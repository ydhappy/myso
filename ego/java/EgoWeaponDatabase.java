package lineage.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
 * 정책:
 * - 에고 런타임 정보는 ego 1개 테이블 중심이다.
 * - 무기별 능력은 ego.ability_* 컬럼에 저장한다.
 * - ego_skill 테이블은 더 이상 런타임 기준으로 사용하지 않는다.
 * - 에고 레벨은 ego_level 1개 테이블 기준이다.
 * - 에고 레벨은 0~10 고정한다.
 * - Lv.0은 스킬/치명/반격/스턴 없음이다.
 * - ego_type은 현재 말투 저장소로 사용한다: 예의 / 예의반대 / 싸이코패스.
 * - 에고 구슬은 최초 생성과 주인 재인식만 담당한다.
 * - 단순 능력명 입력은 런타임 enum명으로 안전 정규화한다.
 */
public final class EgoWeaponDatabase {

    public static final int MIN_EGO_LEVEL = 0;
    public static final int MAX_EGO_LEVEL = 10;
    private static final int MAX_EGO_NAME_LENGTH = 20;

    private static final Map<Long, EgoWeaponInfo> egoMap = new ConcurrentHashMap<Long, EgoWeaponInfo>();
    private static final Map<Long, EgoAbilityInfo> abilityMap = new ConcurrentHashMap<Long, EgoAbilityInfo>();
    private static final Map<Long, Object> itemLocks = new ConcurrentHashMap<Long, Object>();

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
            boolean hasAbilityColumns = columnExists(con, "ego", "ability_type");
            st = con.prepareStatement("SELECT * FROM ego WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next()) {
                EgoWeaponInfo info = new EgoWeaponInfo();
                info.itemObjId = rs.getLong("item_id");
                info.chaObjId = rs.getLong("char_id");
                info.enabled = rs.getBoolean("use_yn");
                info.egoName = normalizeName(rs.getString("ego_name"));
                info.personality = normalizeTone(rs.getString("ego_type"));
                info.level = clampLevel(rs.getInt("ego_lv"));
                info.exp = Math.max(0L, rs.getLong("ego_exp"));
                info.maxExp = getNeedExp(info.level);
                if (info.maxExp > 0L && info.exp >= info.maxExp)
                    info.exp = info.maxExp - 1L;
                if (info.level >= MAX_EGO_LEVEL) {
                    info.level = MAX_EGO_LEVEL;
                    info.exp = 0L;
                    info.maxExp = 0L;
                }
                info.talkLevel = Math.max(1, rs.getInt("talk_lv"));
                info.controlLevel = Math.max(1, rs.getInt("ctrl_lv"));
                info.lastTalkTime = Math.max(0L, rs.getLong("last_talk"));
                info.lastWarningTime = Math.max(0L, rs.getLong("last_warn"));
                egoMap.put(Long.valueOf(info.itemObjId), info);

                if (hasAbilityColumns) {
                    EgoAbilityInfo ability = new EgoAbilityInfo();
                    ability.uid = info.itemObjId;
                    ability.itemObjId = info.itemObjId;
                    ability.abilityType = normalizeAbility(rs.getString("ability_type"));
                    ability.abilityLevel = Math.max(1, rs.getInt("ability_lv"));
                    ability.procChanceBonus = rs.getInt("ability_rate_bonus");
                    ability.damageBonus = Math.max(0, rs.getInt("ability_dmg_bonus"));
                    ability.lastProcTime = Math.max(0L, rs.getLong("ability_last_proc"));
                    ability.enabled = ability.abilityType.length() > 0;
                    if (ability.enabled)
                        abilityMap.put(Long.valueOf(ability.itemObjId), ability);
                }
            }
        } catch (Exception e) {
            log("loadEgoInfo(Connection con)", e);
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    public static EgoWeaponInfo find(long itemObjId) {
        return egoMap.get(Long.valueOf(itemObjId));
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
        List<EgoAbilityInfo> list = new ArrayList<EgoAbilityInfo>();
        EgoAbilityInfo info = getFirstAbility(item);
        if (info != null)
            list.add(info);
        return list;
    }

    public static EgoAbilityInfo getFirstAbility(ItemInstance item) {
        if (item == null)
            return null;
        return abilityMap.get(Long.valueOf(item.getObjectId()));
    }

    public static boolean enableEgo(PcInstance pc, ItemInstance item, String egoName, String personality) {
        if (pc == null || item == null)
            return false;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return false;
        synchronized (itemLock(itemObjId)) {
            String name = normalizeName(egoName);
            String tone = normalizeTone(personality);
            long needExp = getNeedExp(0);
            Connection con = null;
            PreparedStatement st = null;
            boolean oldAutoCommit = true;
            try {
                con = DatabaseConnection.getLineage();
                oldAutoCommit = con.getAutoCommit();
                con.setAutoCommit(false);
                st = con.prepareStatement("INSERT INTO ego (item_id, char_id, use_yn, ego_name, ego_type, ego_lv, ego_exp, need_exp, talk_lv, ctrl_lv, ability_type, ability_lv, ability_rate_bonus, ability_dmg_bonus, ability_last_proc, last_talk, last_warn, bond, bond_reason) VALUES (?, ?, 1, ?, ?, 0, 0, ?, 1, 1, '', 1, 0, 0, 0, 0, 0, 0, '') ON DUPLICATE KEY UPDATE char_id=?, use_yn=1, ego_name=?, ego_type=?, ego_lv=0, ego_exp=0, need_exp=?, talk_lv=1, ctrl_lv=1, ability_type='', ability_lv=1, ability_rate_bonus=0, ability_dmg_bonus=0, ability_last_proc=0, last_talk=0, last_warn=0, bond=0, bond_reason='', mod_date=NOW()");
                st.setLong(1, itemObjId);
                st.setLong(2, pc.getObjectId());
                st.setString(3, name);
                st.setString(4, tone);
                st.setLong(5, needExp);
                st.setLong(6, pc.getObjectId());
                st.setString(7, name);
                st.setString(8, tone);
                st.setLong(9, needExp);
                st.executeUpdate();
                con.commit();

                EgoWeaponInfo info = new EgoWeaponInfo();
                info.itemObjId = itemObjId;
                info.chaObjId = pc.getObjectId();
                info.enabled = true;
                info.egoName = name;
                info.personality = tone;
                info.level = 0;
                info.exp = 0L;
                info.maxExp = needExp;
                info.talkLevel = 1;
                info.controlLevel = 1;
                info.lastTalkTime = 0L;
                info.lastWarningTime = 0L;
                egoMap.put(Long.valueOf(itemObjId), info);
                abilityMap.remove(Long.valueOf(itemObjId));
                return true;
            } catch (Exception e) {
                rollback(con);
                log("enableEgo(PcInstance pc, ItemInstance item, String egoName, String personality)", e);
            } finally {
                restoreAutoCommit(con, oldAutoCommit);
                DatabaseConnection.close(con, st);
            }
            return false;
        }
    }

    public static boolean recognizeOwner(PcInstance pc, ItemInstance item) {
        if (pc == null || item == null)
            return false;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return false;
        synchronized (itemLock(itemObjId)) {
            EgoWeaponInfo info = find(itemObjId);
            if (info == null || !info.enabled)
                return false;
            long charId = pc.getObjectId();
            if (info.chaObjId == charId)
                return true;
            Connection con = null;
            PreparedStatement st = null;
            try {
                con = DatabaseConnection.getLineage();
                st = con.prepareStatement("UPDATE ego SET char_id=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
                st.setLong(1, charId);
                st.setLong(2, itemObjId);
                int count = st.executeUpdate();
                if (count <= 0)
                    return false;
                info.chaObjId = charId;
                return true;
            } catch (Exception e) {
                log("recognizeOwner(PcInstance pc, ItemInstance item)", e);
            } finally {
                DatabaseConnection.close(con, st);
            }
            return false;
        }
    }

    public static boolean disableEgo(ItemInstance item) {
        if (item == null)
            return false;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return false;
        synchronized (itemLock(itemObjId)) {
            Connection con = null;
            PreparedStatement log = null;
            PreparedStatement ego = null;
            boolean oldAutoCommit = true;
            try {
                con = DatabaseConnection.getLineage();
                oldAutoCommit = con.getAutoCommit();
                con.setAutoCommit(false);
                log = con.prepareStatement("DELETE FROM ego_log WHERE item_id=?");
                log.setLong(1, itemObjId);
                log.executeUpdate();
                DatabaseConnection.close(log);
                log = null;
                ego = con.prepareStatement("DELETE FROM ego WHERE item_id=?");
                ego.setLong(1, itemObjId);
                int count = ego.executeUpdate();
                con.commit();
                egoMap.remove(Long.valueOf(itemObjId));
                abilityMap.remove(Long.valueOf(itemObjId));
                return count > 0;
            } catch (Exception e) {
                rollback(con);
                log("disableEgo(ItemInstance item)", e);
            } finally {
                restoreAutoCommit(con, oldAutoCommit);
                DatabaseConnection.close(log);
                DatabaseConnection.close(con, ego);
            }
            return false;
        }
    }

    public static boolean setEgoName(ItemInstance item, String egoName) {
        if (item == null)
            return false;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return false;
        synchronized (itemLock(itemObjId)) {
            String name = normalizeName(egoName);
            Connection con = null;
            PreparedStatement st = null;
            try {
                con = DatabaseConnection.getLineage();
                st = con.prepareStatement("UPDATE ego SET ego_name=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
                st.setString(1, name);
                st.setLong(2, itemObjId);
                int count = st.executeUpdate();
                if (count <= 0)
                    return false;
                EgoWeaponInfo info = find(itemObjId);
                if (info != null)
                    info.egoName = name;
                return true;
            } catch (Exception e) {
                log("setEgoName(ItemInstance item, String egoName)", e);
            } finally {
                DatabaseConnection.close(con, st);
            }
            return false;
        }
    }

    public static boolean setTone(ItemInstance item, String tone) {
        if (item == null)
            return false;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return false;
        synchronized (itemLock(itemObjId)) {
            String normalizedTone = normalizeTone(tone);
            Connection con = null;
            PreparedStatement st = null;
            try {
                con = DatabaseConnection.getLineage();
                st = con.prepareStatement("UPDATE ego SET ego_type=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
                st.setString(1, normalizedTone);
                st.setLong(2, itemObjId);
                int count = st.executeUpdate();
                if (count <= 0)
                    return false;
                EgoWeaponInfo info = find(itemObjId);
                if (info != null)
                    info.personality = normalizedTone;
                return true;
            } catch (Exception e) {
                log("setTone(ItemInstance item, String tone)", e);
            } finally {
                DatabaseConnection.close(con, st);
            }
            return false;
        }
    }

    public static boolean setAbility(ItemInstance item, String abilityType) {
        if (item == null)
            return false;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return false;
        synchronized (itemLock(itemObjId)) {
            String ability = normalizeAbility(abilityType);
            if (ability.length() == 0)
                return false;
            Connection con = null;
            PreparedStatement st = null;
            try {
                con = DatabaseConnection.getLineage();
                st = con.prepareStatement("UPDATE ego SET ability_type=?, ability_lv=1, ability_rate_bonus=0, ability_dmg_bonus=0, ability_last_proc=0, mod_date=NOW() WHERE item_id=? AND use_yn=1");
                st.setString(1, ability);
                st.setLong(2, itemObjId);
                int count = st.executeUpdate();
                if (count <= 0)
                    return false;
                EgoAbilityInfo info = new EgoAbilityInfo();
                info.uid = itemObjId;
                info.itemObjId = itemObjId;
                info.abilityType = ability;
                info.abilityLevel = 1;
                info.procChanceBonus = 0;
                info.damageBonus = 0;
                info.lastProcTime = 0L;
                info.enabled = true;
                abilityMap.put(Long.valueOf(itemObjId), info);
                return true;
            } catch (Exception e) {
                log("setAbility(ItemInstance item, String abilityType)", e);
            } finally {
                DatabaseConnection.close(con, st);
            }
            return false;
        }
    }

    public static void updateAbilityLastProc(ItemInstance item, String abilityType, long lastProc) {
        if (item == null || abilityType == null || lastProc <= 0L)
            return;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return;
        String ability = normalizeAbility(abilityType);
        if (ability.length() == 0)
            return;
        EgoAbilityInfo info = abilityMap.get(Long.valueOf(itemObjId));
        if (info != null && ability.equals(info.abilityType))
            info.lastProcTime = lastProc;
        Connection con = null;
        PreparedStatement st = null;
        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE ego SET ability_last_proc=?, mod_date=NOW() WHERE item_id=? AND ability_type=? AND use_yn=1");
            st.setLong(1, lastProc);
            st.setLong(2, itemObjId);
            st.setString(3, ability);
            st.executeUpdate();
        } catch (Exception e) {
            log("updateAbilityLastProc(ItemInstance item, String abilityType, long lastProc)", e);
        } finally {
            DatabaseConnection.close(con, st);
        }
    }

    public static boolean addExp(ItemInstance item, long addExp) {
        if (item == null || addExp <= 0L)
            return false;
        final long itemObjId = item.getObjectId();
        if (itemObjId <= 0L)
            return false;
        synchronized (itemLock(itemObjId)) {
            EgoWeaponInfo info = find(itemObjId);
            if (info == null || !info.enabled || info.level >= MAX_EGO_LEVEL)
                return false;
            int oldLevel = clampLevel(info.level);
            long oldExp = Math.max(0L, info.exp);
            long oldMaxExp = Math.max(0L, info.maxExp);
            int newLevel = oldLevel;
            long newExp = safeAdd(oldExp, addExp);
            boolean levelUp = false;
            while (newLevel < MAX_EGO_LEVEL) {
                long need = getNeedExp(newLevel);
                if (need <= 0L || newExp < need)
                    break;
                newExp -= need;
                newLevel++;
                levelUp = true;
            }
            long newMaxExp = getNeedExp(newLevel);
            if (newLevel >= MAX_EGO_LEVEL) {
                newLevel = MAX_EGO_LEVEL;
                newExp = 0L;
                newMaxExp = 0L;
            }
            Connection con = null;
            PreparedStatement st = null;
            try {
                con = DatabaseConnection.getLineage();
                st = con.prepareStatement("UPDATE ego SET ego_lv=?, ego_exp=?, need_exp=?, mod_date=NOW() WHERE item_id=? AND use_yn=1");
                st.setInt(1, newLevel);
                st.setLong(2, newExp);
                st.setLong(3, newMaxExp);
                st.setLong(4, itemObjId);
                int count = st.executeUpdate();
                if (count <= 0)
                    return false;
                info.level = newLevel;
                info.exp = newExp;
                info.maxExp = newMaxExp;
                return levelUp;
            } catch (Exception e) {
                info.level = oldLevel;
                info.exp = oldExp;
                info.maxExp = oldMaxExp;
                log("addExp(ItemInstance item, long addExp)", e);
            } finally {
                DatabaseConnection.close(con, st);
            }
            return false;
        }
    }

    public static String normalizeTone(String tone) {
        if (tone == null)
            return "예의";
        String t = tone.trim();
        if (t.length() == 0)
            return "예의";
        if ("싸이코패스".equals(t) || "사이코패스".equals(t) || "psycho".equalsIgnoreCase(t) || "psychopath".equalsIgnoreCase(t))
            return "싸이코패스";
        if ("예의반대".equals(t) || "반말".equals(t) || "막말".equals(t) || "싸가지".equals(t))
            return "예의반대";
        return "예의";
    }

    private static String normalizeName(String name) {
        if (name == null)
            return "에고";
        String n = name.trim();
        if (n.length() == 0)
            return "에고";
        if (n.length() > MAX_EGO_NAME_LENGTH)
            n = n.substring(0, MAX_EGO_NAME_LENGTH);
        return n;
    }

    private static String normalizeAbility(String ability) {
        if (ability == null)
            return "";
        String a = ability.trim().toUpperCase();
        if (a.length() == 0)
            return "";
        if ("BALANCE".equals(a)) return "EGO_BALANCE";
        if ("BLOOD".equals(a)) return "BLOOD_DRAIN";
        if ("MANA".equals(a)) return "MANA_DRAIN";
        if ("CRIT".equals(a)) return "CRITICAL_BURST";
        if ("SHIELD".equals(a)) return "GUARDIAN_SHIELD";
        if ("AREA".equals(a)) return "AREA_SLASH";
        if ("EXECUTE".equals(a)) return "EXECUTION";
        if ("FIRE".equals(a)) return "FLAME_BRAND";
        if ("FROST".equals(a)) return "FROST_BIND";
        if ("COUNTER".equals(a)) return "EGO_COUNTER";
        if ("REVENGE".equals(a)) return "EGO_REVENGE";
        return a;
    }

    private static int clampLevel(int level) {
        if (level < MIN_EGO_LEVEL)
            return MIN_EGO_LEVEL;
        if (level > MAX_EGO_LEVEL)
            return MAX_EGO_LEVEL;
        return level;
    }

    private static long safeAdd(long a, long b) {
        if (b <= 0L)
            return a;
        if (Long.MAX_VALUE - a < b)
            return Long.MAX_VALUE;
        return a + b;
    }

    private static Object itemLock(long itemObjId) {
        Long key = Long.valueOf(itemObjId);
        Object lock = itemLocks.get(key);
        if (lock != null)
            return lock;
        Object newLock = new Object();
        Object old = itemLocks.putIfAbsent(key, newLock);
        return old == null ? newLock : old;
    }

    private static boolean tableExists(Connection con, String table) {
        ResultSet rs = null;
        try {
            rs = con.getMetaData().getTables(null, null, table, null);
            return rs != null && rs.next();
        } catch (SQLException e) {
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
        }
    }

    private static boolean columnExists(Connection con, String table, String column) {
        ResultSet rs = null;
        try {
            rs = con.getMetaData().getColumns(null, null, table, column);
            return rs != null && rs.next();
        } catch (Exception e) {
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
        }
    }

    private static void rollback(Connection con) {
        try { if (con != null) con.rollback(); } catch (Exception e) {}
    }

    private static void restoreAutoCommit(Connection con, boolean oldAutoCommit) {
        try { if (con != null) con.setAutoCommit(oldAutoCommit); } catch (Exception e) {}
    }

    private static void log(String where, Exception e) {
        try {
            lineage.share.System.printf("%s : %s\r\n", EgoWeaponDatabase.class.toString(), where);
            lineage.share.System.println(e);
        } catch (Throwable ignore) {
        }
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
