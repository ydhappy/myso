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

import lineage.world.controller.EgoLevel;
import lineage.world.controller.EgoView;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 DB 헬퍼.
 *
 * 최종 정책:
 * - 에고 레벨/경험치/전투보너스는 EgoLevel 통합 캐시를 우선 사용.
 * - 에고 레벨은 0~10 고정.
 * - Lv.0은 스킬/치명/반격/스턴 없음.
 * - ego_type은 현재 말투 저장소로 사용: 예의 / 예의반대.
 * - 에고삭제는 ego/ego_skill/ego_log 완전삭제.
 */
public final class EgoWeaponDatabase {

    public static final int MIN_EGO_LEVEL = 0;
    public static final int MAX_EGO_LEVEL = 10;

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
        EgoLevel.reload(con);
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
                info.personality = normalizeTone(rs.getString("ego_type"));
                info.level = clampLevel(rs.getInt("ego_lv"));
                info.exp = Math.max(0, rs.getLong("ego_exp"));
                info.maxExp = getNeedExp(info.level);
                if (info.maxExp > 0 && info.exp >= info.maxExp)
                    info.exp = info.maxExp - 1;
                if (info.level >= MAX_EGO_LEVEL)
                    info.exp = 0;
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

    public static String getTone(ItemInstance item) {
        EgoWeaponInfo info = find(item);
        if (info == null)
            return "예의";
        return normalizeTone(info.personality);
    }

    public static int getEgoLevel(ItemInstance item, int defaultLevel) {
        EgoWeaponInfo info = find(item);
        if (info != null)
            return clampLevel(info.level);
        return clampLevel(defaultLevel);
    }

    public static long getNeedExp(int level) {
        return EgoLevel.needExp(clampLevel(level));
    }

    public static long getTotalNeedExpToLevel(int targetLevel) {
        int lv = clampLevel(targetLevel);
        long total = 0L;
        for (int i = 0; i < lv && i < MAX_EGO_LEVEL; i++)
            total += getNeedExp(i);
        return total;
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
        personality = normalizeTone(personality);

        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement(
                "INSERT INTO ego " +
                "(item_id, char_id, use_yn, ego_name, ego_type, ego_lv, ego_exp, need_exp, talk_lv, ctrl_lv, last_talk, last_warn, bond, bond_reason) " +
                "VALUES (?, ?, 1, ?, ?, 0, 0, ?, 1, 1, 0, 0, 0, '') " +
                "ON DUPLICATE KEY UPDATE char_id=?, use_yn=1, ego_name=?, ego_type=?, ego_lv=0, ego_exp=0, need_exp=?, bond=0, bond_reason=''"
            );
            st.setLong(1, item.getObjectId());
            st.setLong(2, pc.getObjectId());
            st.setString(3, egoName);
            st.setString(4, personality);
            st.setLong(5, getNeedExp(0));
            st.setLong(6, pc.getObjectId());
            st.setString(7, egoName);
            st.setString(8, personality);
            st.setLong(9, getNeedExp(0));
            st.executeUpdate();

            EgoWeaponInfo info = new EgoWeaponInfo();
            info.itemObjId = item.getObjectId();
            info.chaObjId = pc.getObjectId();
            info.enabled = true;
            info.egoName = egoName;
            info.personality = personality;
            info.level = 0;
            info.exp = 0;
            info.maxExp = getNeedExp(0);
            info.talkLevel = 1;
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

    /** 에고삭제: ego / ego_skill / ego_log 완전삭제. */
    public static boolean disableEgo(ItemInstance item) {
        if (item == null)
            return false;

        Connection con = null;
        PreparedStatement log = null;
        PreparedStatement skill = null;
        PreparedStatement ego = null;

        try {
            con = DatabaseConnection.getLineage();
            con.setAutoCommit(false);

            log = con.prepareStatement("DELETE FROM ego_log WHERE item_id=?");
            log.setLong(1, item.getObjectId());
            log.executeUpdate();

            skill = con.prepareStatement("DELETE FROM ego_skill WHERE item_id=?");
            skill.setLong(1, item.getObjectId());
            skill.executeUpdate();

            ego = con.prepareStatement("DELETE FROM ego WHERE item_id=?");
            ego.setLong(1, item.getObjectId());
            int count = ego.executeUpdate();

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
            DatabaseConnection.close(log);
            DatabaseConnection.close(skill);
            DatabaseConnection.close(con, ego);
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

    public static boolean setTone(ItemInstance item, String tone) {
        if (item == null)
            return false;
        tone = normalizeTone(tone);

        Connection con = null;
        PreparedStatement st = null;
        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE ego SET ego_type=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
            st.setString(1, tone);
            st.setLong(2, item.getObjectId());
            int count = st.executeUpdate();

            EgoWeaponInfo info = find(item.getObjectId());
            if (info != null)
                info.personality = tone;
            return count > 0;
        } catch (Exception e) {
            lineage.share.System.printf("%s : setTone(ItemInstance item, String tone)\r\n", EgoWeaponDatabase.class.toString());
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

        if (info.level >= MAX_EGO_LEVEL)
            return false;

        info.level = clampLevel(info.level);
        info.maxExp = getNeedExp(info.level);
        info.exp = Math.max(0, info.exp + addExp);
        boolean levelUp = false;

        while (info.level < MAX_EGO_LEVEL) {
            long need = getNeedExp(info.level);
            if (need <= 0 || info.exp < need)
                break;
            info.exp -= need;
            info.level++;
            info.maxExp = getNeedExp(info.level);
            levelUp = true;
        }

        if (info.level >= MAX_EGO_LEVEL) {
            info.level = MAX_EGO_LEVEL;
            info.exp = 0;
            info.maxExp = 0;
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

    public static String normalizeTone(String tone) {
        if (tone == null)
            return "예의";
        String t = tone.trim();
        if (t.length() == 0)
            return "예의";
        if ("예의반대".equals(t) || "반말".equals(t) || "막말".equals(t) || "싸가지".equals(t))
            return "예의반대";
        return "예의";
    }

    private static int clampLevel(int level) {
        if (level < MIN_EGO_LEVEL)
            return MIN_EGO_LEVEL;
        if (level > MAX_EGO_LEVEL)
            return MAX_EGO_LEVEL;
        return level;
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
