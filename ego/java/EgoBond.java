package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.database.DatabaseConnection;
import lineage.database.EgoWeaponDatabase;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고 유대감 시스템.
 *
 * 테스트 명령 없음.
 * 대화/레벨업/전투상황에서 자동으로 유대감을 올린다.
 */
public final class EgoBond {

    private static final int MAX_BOND = 1000;
    private static final long TALK_ADD_DELAY_MS = 30000L;

    private static final Map<Long, Integer> bondMap = new ConcurrentHashMap<Long, Integer>();
    private static final Map<Long, Long> talkDelayMap = new ConcurrentHashMap<Long, Long>();

    private EgoBond() {
    }

    public static void reload(Connection con) {
        bondMap.clear();
        load(con);
    }

    public static int get(ItemInstance weapon) {
        if (weapon == null)
            return 0;
        Integer value = bondMap.get(weapon.getObjectId());
        return value == null ? 0 : value.intValue();
    }

    public static String grade(ItemInstance weapon) {
        int value = get(weapon);
        if (value >= 800)
            return "혼연일체";
        if (value >= 500)
            return "신뢰";
        if (value >= 250)
            return "친밀";
        if (value >= 100)
            return "관심";
        return "낯섦";
    }

    public static void addTalk(PcInstance pc, ItemInstance weapon) {
        if (pc == null || weapon == null || !EgoWeaponDatabase.isEgoWeapon(weapon))
            return;
        long now = java.lang.System.currentTimeMillis();
        Long last = talkDelayMap.get(weapon.getObjectId());
        if (last != null && now - last.longValue() < TALK_ADD_DELAY_MS)
            return;
        talkDelayMap.put(weapon.getObjectId(), now);
        add(weapon, 1, "TALK");
    }

    public static void addLevelUp(ItemInstance weapon) {
        add(weapon, 10, "LEVEL_UP");
    }

    public static void addDangerSurvive(ItemInstance weapon) {
        add(weapon, 20, "DANGER_SURVIVE");
    }

    public static void addStun(ItemInstance weapon) {
        add(weapon, 3, "STUN");
    }

    public static void addCounter(ItemInstance weapon) {
        add(weapon, 2, "COUNTER");
    }

    public static void delete(ItemInstance weapon) {
        if (weapon == null)
            return;
        bondMap.remove(weapon.getObjectId());
    }

    private static void add(ItemInstance weapon, int add, String reason) {
        if (weapon == null || add <= 0)
            return;
        int oldValue = get(weapon);
        int newValue = Math.min(MAX_BOND, oldValue + add);
        if (newValue == oldValue)
            return;
        bondMap.put(weapon.getObjectId(), newValue);
        save(weapon, newValue, reason);
    }

    private static void load(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            st = con.prepareStatement("SELECT item_id, bond FROM ego_bond");
            rs = st.executeQuery();
            while (rs.next())
                bondMap.put(rs.getLong("item_id"), rs.getInt("bond"));
        } catch (Exception e) {
            // ego_bond 미적용 서버에서도 에고 기본 기능은 계속 동작해야 한다.
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    private static void save(ItemInstance weapon, int value, String reason) {
        Connection con = null;
        PreparedStatement st = null;
        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement(
                "INSERT INTO ego_bond (item_id, bond, last_reason, mod_date) VALUES (?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE bond=?, last_reason=?, mod_date=NOW()"
            );
            st.setLong(1, weapon.getObjectId());
            st.setInt(2, value);
            st.setString(3, reason == null ? "" : reason);
            st.setInt(4, value);
            st.setString(5, reason == null ? "" : reason);
            st.executeUpdate();
        } catch (Exception e) {
            // DB 미적용 상태에서는 메모리 유대감만 유지한다.
        } finally {
            DatabaseConnection.close(con, st);
        }
    }
}
