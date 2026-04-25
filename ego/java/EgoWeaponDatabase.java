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
 * DB 명칭 단순화:
 * - 테이블: 에고, 에고능력
 * - 컬럼: 아이템번호, 캐릭터번호, 사용, 이름, 성격, 레벨, 경험치, 필요경험치, 형태, 이전방패 등 한글명 사용
 *
 * 주의:
 * - Java 소스는 UTF-8로 저장/컴파일해야 한다.
 * - MySQL/MariaDB에서 한글 테이블/컬럼명을 사용하므로 SQL에는 백틱(`)을 사용한다.
 */
public final class EgoWeaponDatabase {

    private static final String T_EGO = "`에고`";
    private static final String T_ABILITY = "`에고능력`";

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

            st = con.prepareStatement("SELECT * FROM " + T_EGO + " WHERE `사용`=1");
            rs = st.executeQuery();

            while (rs.next()) {
                EgoWeaponInfo info = new EgoWeaponInfo();
                info.itemObjId = rs.getLong("아이템번호");
                info.chaObjId = rs.getLong("캐릭터번호");
                info.enabled = rs.getBoolean("사용");
                info.egoName = rs.getString("이름");
                info.personality = rs.getString("성격");
                info.level = Math.max(1, rs.getInt("레벨"));
                info.exp = Math.max(0, rs.getLong("경험치"));
                info.maxExp = Math.max(100, rs.getLong("필요경험치"));
                info.talkLevel = Math.max(1, rs.getInt("대화단계"));
                info.controlLevel = Math.max(1, rs.getInt("제어단계"));
                info.lastTalkTime = rs.getLong("마지막대화");
                info.lastWarningTime = rs.getLong("마지막경고");
                info.formType = safe(rs.getString("형태"));
                info.prevShieldObjId = Math.max(0, rs.getLong("이전방패"));
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

            st = con.prepareStatement("SELECT * FROM " + T_ABILITY + " WHERE `사용`=1");
            rs = st.executeQuery();

            while (rs.next()) {
                EgoAbilityInfo info = new EgoAbilityInfo();
                info.uid = rs.getLong("번호");
                info.itemObjId = rs.getLong("아이템번호");
                info.abilityType = rs.getString("능력");
                info.abilityLevel = Math.max(1, rs.getInt("레벨"));
                info.procChanceBonus = rs.getInt("확률보너스");
                info.damageBonus = rs.getInt("피해보너스");
                info.lastProcTime = rs.getLong("마지막발동");
                info.enabled = rs.getBoolean("사용");

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

    public static String getFormType(ItemInstance item) {
        EgoWeaponInfo info = find(item);
        if (info != null && info.formType != null)
            return info.formType.trim();
        return "";
    }

    public static long getPrevShieldObjId(ItemInstance item) {
        EgoWeaponInfo info = find(item);
        if (info != null)
            return info.prevShieldObjId;
        return 0;
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
                "INSERT INTO " + T_EGO + " " +
                "(`아이템번호`, `캐릭터번호`, `사용`, `이름`, `성격`, `레벨`, `경험치`, `필요경험치`, `대화단계`, `제어단계`, `마지막대화`, `마지막경고`, `형태`, `이전방패`) " +
                "VALUES (?, ?, 1, ?, ?, 1, 0, 100, 1, 1, 0, 0, '', 0) " +
                "ON DUPLICATE KEY UPDATE `캐릭터번호`=?, `사용`=1, `이름`=?, `성격`=?"
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
            if (info.formType == null)
                info.formType = "";
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

        egoName = egoName.trim();
        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE " + T_EGO + " SET `이름`=? WHERE `아이템번호`=? AND `사용`=1");
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

    public static boolean setForm(ItemInstance item, String formType, long prevShieldObjId) {
        if (item == null || formType == null)
            return false;

        formType = formType.trim().toLowerCase();

        Connection con = null;
        PreparedStatement st = null;

        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE " + T_EGO + " SET `형태`=?, `이전방패`=? WHERE `아이템번호`=? AND `사용`=1");
            st.setString(1, formType);
            st.setLong(2, Math.max(0, prevShieldObjId));
            st.setLong(3, item.getObjectId());

            int count = st.executeUpdate();
            if (count <= 0)
                return false;

            EgoWeaponInfo info = find(item.getObjectId());
            if (info != null) {
                info.formType = formType;
                info.prevShieldObjId = Math.max(0, prevShieldObjId);
            }
            return true;
        } catch (Exception e) {
            lineage.share.System.printf("%s : setForm(ItemInstance item, String formType, long prevShieldObjId)\r\n", EgoWeaponDatabase.class.toString());
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

            off = con.prepareStatement("UPDATE " + T_ABILITY + " SET `사용`=0 WHERE `아이템번호`=?");
            off.setLong(1, item.getObjectId());
            off.executeUpdate();

            upsert = con.prepareStatement(
                "INSERT INTO " + T_ABILITY + " (`아이템번호`, `능력`, `레벨`, `사용`) " +
                "VALUES (?, ?, 1, 1) " +
                "ON DUPLICATE KEY UPDATE `레벨`=GREATEST(`레벨`, 1), `사용`=1"
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
            st = con.prepareStatement("UPDATE " + T_EGO + " SET `레벨`=?, `경험치`=?, `필요경험치`=? WHERE `아이템번호`=? AND `사용`=1");
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

    @SuppressWarnings("unused")
    private static boolean hasColumn(Connection con, String table, String column) {
        ResultSet rs = null;
        try {
            rs = con.getMetaData().getColumns(null, null, table, column);
            if (rs != null && rs.next())
                return true;
        } catch (SQLException e) {
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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
        public String formType;
        public long prevShieldObjId;
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
