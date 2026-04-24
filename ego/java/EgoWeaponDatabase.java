package lineage.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 DB 헬퍼.
 *
 * 적용 위치:
 * - 이 파일을 bitna/src/lineage/database/EgoWeaponDatabase.java 로 복사한다.
 *
 * 역할:
 * - character_item_ego 로드/저장
 * - character_item_ego_ability 로드/저장
 * - 아이템 objectId 기준 메모리 캐시 제공
 *
 * 주의:
 * - 이 클래스는 ItemInstance에 에고 필드를 직접 추가하지 않아도 동작하도록 설계했다.
 * - 컨트롤러에서는 EgoWeaponDatabase.find(item.getObjectId())로 에고 정보를 조회하면 된다.
 */
public final class EgoWeaponDatabase {

    private static final Map<Long, EgoWeaponInfo> egoMap = new ConcurrentHashMap<Long, EgoWeaponInfo>();
    private static final Map<Long, List<EgoAbilityInfo>> abilityMap = new ConcurrentHashMap<Long, List<EgoAbilityInfo>>();

    private EgoWeaponDatabase() {
    }

    /**
     * 서버 로딩 시 전체 에고 정보를 캐시에 올린다.
     * Main 로딩 구간 또는 별도 리로드 명령에서 호출한다.
     */
    public static void init(Connection con) {
        reload(con);
    }

    public static void reload(Connection con) {
        egoMap.clear();
        abilityMap.clear();

        loadEgoInfo(con);
        loadAbilityInfo(con);
    }

    private static void loadEgoInfo(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;

        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }

            st = con.prepareStatement("SELECT * FROM character_item_ego WHERE ego_enabled=1");
            rs = st.executeQuery();

            while (rs.next()) {
                EgoWeaponInfo info = new EgoWeaponInfo();
                info.itemObjId = rs.getLong("item_objid");
                info.chaObjId = rs.getLong("cha_objid");
                info.enabled = rs.getBoolean("ego_enabled");
                info.egoName = rs.getString("ego_name");
                info.personality = rs.getString("ego_personality");
                info.level = rs.getInt("ego_level");
                info.exp = rs.getLong("ego_exp");
                info.maxExp = rs.getLong("ego_max_exp");
                info.talkLevel = rs.getInt("ego_talk_level");
                info.controlLevel = rs.getInt("ego_control_level");
                info.lastTalkTime = rs.getLong("ego_last_talk_time");
                info.lastWarningTime = rs.getLong("ego_last_warning_time");

                egoMap.put(info.itemObjId, info);
            }
        } catch (Exception e) {
            lineage.share.System.printf("%s : loadEgoInfo(Connection con)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    private static void loadAbilityInfo(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;

        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }

            st = con.prepareStatement("SELECT * FROM character_item_ego_ability WHERE enabled=1");
            rs = st.executeQuery();

            while (rs.next()) {
                EgoAbilityInfo info = new EgoAbilityInfo();
                info.uid = rs.getLong("uid");
                info.itemObjId = rs.getLong("item_objid");
                info.abilityType = rs.getString("ability_type");
                info.abilityLevel = rs.getInt("ability_level");
                info.procChanceBonus = rs.getInt("proc_chance_bonus");
                info.damageBonus = rs.getInt("damage_bonus");
                info.lastProcTime = rs.getLong("last_proc_time");
                info.enabled = rs.getBoolean("enabled");

                List<EgoAbilityInfo> list = abilityMap.get(info.itemObjId);
                if (list == null) {
                    list = Collections.synchronizedList(new ArrayList<EgoAbilityInfo>());
                    abilityMap.put(info.itemObjId, list);
                }
                list.add(info);
            }
        } catch (Exception e) {
            lineage.share.System.printf("%s : loadAbilityInfo(Connection con)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    public static EgoWeaponInfo find(long itemObjId) {
        return egoMap.get(itemObjId);
    }

    public static EgoWeaponInfo find(ItemInstance item) {
        if (item == null)
            return null;
        return find(item.getObjectId());
    }

    public static boolean isEgoWeapon(ItemInstance item) {
        EgoWeaponInfo info = find(item);
        return info != null && info.enabled;
    }

    public static String getEgoName(ItemInstance item, String defaultName) {
        EgoWeaponInfo info = find(item);
        if (info != null && info.egoName != null && info.egoName.length() > 0)
            return info.egoName;
        return defaultName;
    }

    public static int getEgoLevel(ItemInstance item, int defaultLevel) {
        EgoWeaponInfo info = find(item);
        if (info != null && info.level > 0)
            return info.level;
        return defaultLevel;
    }

    public static List<EgoAbilityInfo> getAbilities(ItemInstance item) {
        if (item == null)
            return new ArrayList<EgoAbilityInfo>();
        List<EgoAbilityInfo> list = abilityMap.get(item.getObjectId());
        if (list == null)
            return new ArrayList<EgoAbilityInfo>();
        synchronized (list) {
            return new ArrayList<EgoAbilityInfo>(list);
        }
    }

    public static EgoAbilityInfo getFirstAbility(ItemInstance item) {
        List<EgoAbilityInfo> list = getAbilities(item);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 에고 활성화/생성.
     */
    public static boolean enableEgo(PcInstance pc, ItemInstance item, String egoName, String personality) {
        if (pc == null || item == null)
            return false;

        if (egoName == null || egoName.trim().length() == 0)
            egoName = "에고";
        if (personality == null || personality.trim().length() == 0)
            personality = "guardian";

        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement(
                "INSERT INTO character_item_ego " +
                "(item_objid, cha_objid, ego_enabled, ego_name, ego_personality, ego_level, ego_exp, ego_max_exp, ego_talk_level, ego_control_level) " +
                "VALUES (?, ?, 1, ?, ?, 1, 0, 100, 1, 1) " +
                "ON DUPLICATE KEY UPDATE cha_objid=?, ego_enabled=1, ego_name=?, ego_personality=?"
            );
            st.setLong(1, item.getObjectId());
            st.setLong(2, pc.getObjectId());
            st.setString(3, egoName);
            st.setString(4, personality);
            st.setLong(5, pc.getObjectId());
            st.setString(6, egoName);
            st.setString(7, personality);
            st.executeUpdate();

            EgoWeaponInfo info = find(item.getObjectId());
            if (info == null)
                info = new EgoWeaponInfo();
            info.itemObjId = item.getObjectId();
            info.chaObjId = pc.getObjectId();
            info.enabled = true;
            info.egoName = egoName;
            info.personality = personality;
            if (info.level <= 0)
                info.level = 1;
            if (info.maxExp <= 0)
                info.maxExp = 100;
            if (info.talkLevel <= 0)
                info.talkLevel = 1;
            if (info.controlLevel <= 0)
                info.controlLevel = 1;
            egoMap.put(info.itemObjId, info);

            return true;
        } catch (Exception e) {
            lineage.share.System.printf("%s : enableEgo(PcInstance pc, ItemInstance item, String egoName, String personality)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            DatabaseConnection.close(con, st);
        }
        return false;
    }

    public static boolean setEgoName(ItemInstance item, String egoName) {
        if (item == null || egoName == null || egoName.trim().length() == 0)
            return false;

        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE character_item_ego SET ego_name=? WHERE item_objid=?");
            st.setString(1, egoName.trim());
            st.setLong(2, item.getObjectId());
            int count = st.executeUpdate();

            EgoWeaponInfo info = find(item.getObjectId());
            if (info != null)
                info.egoName = egoName.trim();

            return count > 0;
        } catch (Exception e) {
            lineage.share.System.printf("%s : setEgoName(ItemInstance item, String egoName)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            DatabaseConnection.close(con, st);
        }
        return false;
    }

    public static boolean setAbility(ItemInstance item, String abilityType) {
        if (item == null || abilityType == null || abilityType.trim().length() == 0)
            return false;

        abilityType = abilityType.trim().toUpperCase();

        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement(
                "INSERT INTO character_item_ego_ability (item_objid, ability_type, ability_level, enabled) " +
                "VALUES (?, ?, 1, 1) " +
                "ON DUPLICATE KEY UPDATE ability_type=?, enabled=1"
            );
            st.setLong(1, item.getObjectId());
            st.setString(2, abilityType);
            st.setString(3, abilityType);
            st.executeUpdate();

            List<EgoAbilityInfo> list = Collections.synchronizedList(new ArrayList<EgoAbilityInfo>());
            EgoAbilityInfo info = new EgoAbilityInfo();
            info.itemObjId = item.getObjectId();
            info.abilityType = abilityType;
            info.abilityLevel = 1;
            info.enabled = true;
            list.add(info);
            abilityMap.put(item.getObjectId(), list);
            return true;
        } catch (Exception e) {
            lineage.share.System.printf("%s : setAbility(ItemInstance item, String abilityType)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            DatabaseConnection.close(con, st);
        }
        return false;
    }

    public static boolean addExp(ItemInstance item, long addExp) {
        if (item == null || addExp <= 0)
            return false;

        EgoWeaponInfo info = find(item);
        if (info == null || !info.enabled)
            return false;

        info.exp += addExp;
        boolean levelUp = false;

        while (info.level < 30 && info.exp >= info.maxExp) {
            info.exp -= info.maxExp;
            info.level++;
            info.maxExp = Math.max(100, info.maxExp + (info.level * 100));
            levelUp = true;
        }

        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE character_item_ego SET ego_level=?, ego_exp=?, ego_max_exp=? WHERE item_objid=?");
            st.setInt(1, info.level);
            st.setLong(2, info.exp);
            st.setLong(3, info.maxExp);
            st.setLong(4, item.getObjectId());
            st.executeUpdate();
            return levelUp;
        } catch (Exception e) {
            lineage.share.System.printf("%s : addExp(ItemInstance item, long addExp)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            DatabaseConnection.close(con, st);
        }
        return false;
    }

    public static final class EgoWeaponInfo {
        public long itemObjId;
        public long chaObjId;
        public boolean enabled;
        public String egoName;
        public String personality;
        public int level;
        public long exp;
        public long maxExp;
        public int talkLevel;
        public int controlLevel;
        public long lastTalkTime;
        public long lastWarningTime;
    }

    public static final class EgoAbilityInfo {
        public long uid;
        public long itemObjId;
        public String abilityType;
        public int abilityLevel;
        public int procChanceBonus;
        public int damageBonus;
        public long lastProcTime;
        public boolean enabled;
    }
}
