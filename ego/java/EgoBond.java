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
 * 통합 DB 기준:
 * - ego.bond
 * - ego.bond_reason
 *
 * ego_bond 물리 테이블은 사용하지 않는다.
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
        Integer value = bondMap.get(Long.valueOf(weapon.getObjectId()));
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
        Long key = Long.valueOf(weapon.getObjectId());
        Long last = talkDelayMap.get(key);
        if (last != null && now - last.longValue() < TALK_ADD_DELAY_MS)
            return;
        talkDelayMap.put(key, Long.valueOf(now));
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
        bondMap.remove(Long.valueOf(weapon.getObjectId()));
        talkDelayMap.remove(Long.valueOf(weapon.getObjectId()));

        Connection con = null;
        PreparedStatement st = null;
        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE ego SET bond=0, bond_reason='', mod_date=NOW() WHERE item_id=?");
            st.setLong(1, weapon.getObjectId());
            st.executeUpdate();
        } catch (Exception e) {
        } finally {
            DatabaseConnection.close(con, st);
        }
    }

    private static void add(ItemInstance weapon, int add, String reason) {
        if (weapon == null || add <= 0)
            return;
        int oldValue = get(weapon);
        int newValue = Math.min(MAX_BOND, oldValue + add);
        if (newValue == oldValue)
            return;
        bondMap.put(Long.valueOf(weapon.getObjectId()), Integer.valueOf(newValue));
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
            if (con == null)
                return;
            st = con.prepareStatement("SELECT item_id, bond FROM ego WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next())
                bondMap.put(Long.valueOf(rs.getLong("item_id")), Integer.valueOf(Math.max(0, rs.getInt("bond"))));
        } catch (Exception e) {
            // ego 테이블 미적용 상태에서는 메모리 유대감만 유지한다.
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
            st = con.prepareStatement("UPDATE ego SET bond=?, bond_reason=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
            st.setInt(1, value);
            st.setString(2, reason == null ? "" : reason);
            st.setLong(3, weapon.getObjectId());
            st.executeUpdate();
        } catch (Exception e) {
            // DB 미적용 상태에서는 메모리 유대감만 유지한다.
        } finally {
            DatabaseConnection.close(con, st);
        }
    }
}
