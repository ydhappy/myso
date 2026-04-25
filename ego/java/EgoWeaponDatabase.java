package lineage.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.world.controller.EgoView;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 DB 헬퍼.
 *
 * 최종 정책:
 * - 영문 단순 테이블 ego / ego_skill 사용.
 * - 무기변형 제거로 form, prev_shield 컬럼/필드/메서드 제거.
 * - 한글 구버전 테이블 fallback 제거.
 */
public final class EgoWeaponDatabase {

    private static final Map<Long, EgoWeaponInfo> egoMap = new ConcurrentHashMap<Long, EgoWeaponInfo>();
    private static final Map<Long, List<EgoAbilityInfo>> abilityMap = new ConcurrentHashMap<Long, List<EgoAbilityInfo>>();

    private EgoWeaponDatabase() {
    }

    public static void init(Connection con) {
        reload(con);
    }

    public static void reload(Connection con) {
        egoMap.clear();
        abilityMap.clear();
        loadEgoInfo(con);
        loadAbilityInfo(con);
        EgoView.reload(con);
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

            if (!tableExists(con, "ego"))
                return;

            st = con.prepareStatement("SELECT * FROM ego WHERE use_yn=1");
            rs = st.executeQuery();

            while (rs.next()) {
                EgoWeaponInfo info = new EgoWeaponInfo();
                info.itemObjId = rs.getLong("item_id");
                info.chaObjId = rs.getLong("char_id");
                info.enabled = rs.getBoolean("use_yn");
                info.egoName = rs.getString("ego_name");
                info.personality = rs.getString("ego_type");
                info.level = Math.max(1, rs.getInt("ego_lv"));
                info.exp = Math.max(0, rs.getLong("ego_exp"));
                info.maxExp = Math.max(100, rs.getLong("need_exp"));
                info.talkLevel = Math.max(1, rs.getInt("talk_lv"));
                info.controlLevel = Math.max(1, rs.getInt("ctrl_lv"));
                info.lastTalkTime = rs.getLong("last_talk");
                info.lastWarningTime = rs.getLong("last_warn");
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

            if (!tableExists(con, "ego_skill"))
                return;

            st = con.prepareStatement("SELECT * FROM ego_skill WHERE use_yn=1");
            rs = st.executeQuery();

            while (rs.next()) {
                EgoAbilityInfo info = new EgoAbilityInfo();
                info.uid = rs.getLong("id");
                info.itemObjId = rs.getLong("item_id");
                info.abilityType = rs.getString("skill");
                info.abilityLevel = Math.max(1, rs.getInt("skill_lv"));
                info.procChanceBonus = rs.getInt("rate_bonus");
                info.damageBonus = rs.getInt("dmg_bonus");
                info.lastProcTime = rs.getLong("last_proc");
                info.enabled = rs.getBoolean("use_yn");

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
        if (info != null && info.egoName != null && info.egoName.trim().length() > 0)
            return info.egoName.trim();
        return defaultName;
    }

    public static int getEgoLevel(ItemInstance item, int defaultLevel) {
        EgoWeaponInfo info = find(item);
        if (info != null && info.level > 0)
            return info.level;
        return Math.max(1, defaultLevel);
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

    public static boolean enableEgo(PcInstance pc, ItemInstance item, String egoName, String personality) {
        if (pc == null || item == null)
            return false;

        if (egoName == null || egoName.trim().length() == 0)
            egoName = "에고";
        egoName = egoName.trim();

        if (personality == null || personality.trim().length() == 0)
            personality = "수호";
        personality = personality.trim();

        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement(
                "INSERT INTO ego " +
                "(item_id, char_id, use_yn, ego_name, ego_type, ego_lv, ego_exp, need_exp, talk_lv, ctrl_lv, last_talk, last_warn) " +
                "VALUES (?, ?, 1, ?, ?, 1, 0, 100, 1, 1, 0, 0) " +
                "ON DUPLICATE KEY UPDATE char_id=?, use_yn=1, ego_name=?, ego_type=?"
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

    /**
     * 에고 삭제는 데이터 완전삭제가 아니라 use_yn=0 비활성화로 처리한다.
     * 로그와 과거 성장 기록은 운영 추적을 위해 보존한다.
     */
    public static boolean disableEgo(ItemInstance item) {
        if (item == null)
            return false;

        Connection con = null;
        PreparedStatement ego = null;
        PreparedStatement skill = null;

        try {
            con = DatabaseConnection.getLineage();
            con.setAutoCommit(false);

            ego = con.prepareStatement("UPDATE ego SET use_yn=0, mod_date=NOW() WHERE item_id=? AND use_yn=1");
            ego.setLong(1, item.getObjectId());
            int count = ego.executeUpdate();

            skill = con.prepareStatement("UPDATE ego_skill SET use_yn=0, mod_date=NOW() WHERE item_id=?");
            skill.setLong(1, item.getObjectId());
            skill.executeUpdate();

            con.commit();

            egoMap.remove(item.getObjectId());
            abilityMap.remove(item.getObjectId());
            return count > 0;
        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            lineage.share.System.printf("%s : disableEgo(ItemInstance item)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (Exception ignore) {}
            DatabaseConnection.close(ego);
            DatabaseConnection.close(con, skill);
        }
        return false;
    }

    public static boolean setEgoName(ItemInstance item, String egoName) {
        if (item == null || egoName == null || egoName.trim().length() == 0)
            return false;

        egoName = egoName.trim();
        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE ego SET ego_name=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
            st.setString(1, egoName);
            st.setLong(2, item.getObjectId());
            int count = st.executeUpdate();

            EgoWeaponInfo info = find(item.getObjectId());
            if (info != null)
                info.egoName = egoName;

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
        PreparedStatement off = null;
        PreparedStatement upsert = null;

        try {
            con = DatabaseConnection.getLineage();
            con.setAutoCommit(false);

            off = con.prepareStatement("UPDATE ego_skill SET use_yn=0, mod_date=NOW() WHERE item_id=?");
            off.setLong(1, item.getObjectId());
            off.executeUpdate();

            upsert = con.prepareStatement(
                "INSERT INTO ego_skill (item_id, skill, skill_lv, use_yn, mod_date) " +
                "VALUES (?, ?, 1, 1, NOW()) " +
                "ON DUPLICATE KEY UPDATE skill_lv=GREATEST(skill_lv, 1), use_yn=1, mod_date=NOW()"
            );
            upsert.setLong(1, item.getObjectId());
            upsert.setString(2, abilityType);
            upsert.executeUpdate();

            con.commit();

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
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            lineage.share.System.printf("%s : setAbility(ItemInstance item, String abilityType)\r\n", EgoWeaponDatabase.class.toString());
            lineage.share.System.println(e);
        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (Exception ignore) {}
            DatabaseConnection.close(off);
            DatabaseConnection.close(con, upsert);
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
            st = con.prepareStatement("UPDATE ego SET ego_lv=?, ego_exp=?, need_exp=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
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

    private static boolean tableExists(Connection con, String table) {
        ResultSet rs = null;
        try {
            rs = con.getMetaData().getTables(null, null, table, null);
            if (rs != null && rs.next())
                return true;
        } catch (SQLException e) {
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
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
