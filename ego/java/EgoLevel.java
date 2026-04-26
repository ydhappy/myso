package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import lineage.database.DatabaseConnection;

/**
 * 에고 레벨 통합 캐시.
 *
 * 병합 전:
 * - ego_level_exp    : 레벨별 필요 경험치
 * - ego_level_bonus  : 레벨별 전투 보너스
 *
 * 병합 후:
 * - ego_level        : 필요 경험치 + 전투 보너스 통합
 *
 * 동작:
 * 1순위 ego_level 사용
 * 2순위 구버전 ego_level_exp + ego_level_bonus 사용
 * 3순위 Java 기본값 사용
 */
public final class EgoLevel {

    public static final int MAX_LEVEL = 10;

    private static final long[] DEFAULT_NEED_EXP = {100L, 250L, 500L, 900L, 1500L, 2400L, 3600L, 5200L, 7500L, 10000L, 0L};
    private static final int[] DEFAULT_PROC_BONUS = {0, 0, 1, 2, 3, 4, 6, 8, 10, 12, 15};
    private static final int[] DEFAULT_CRITICAL_CHANCE = {0, 1, 2, 3, 4, 6, 9, 12, 15, 18, 25};
    private static final int[] DEFAULT_CRITICAL_DAMAGE = {0, 1, 2, 3, 4, 6, 8, 10, 12, 15, 20};
    private static final int[] DEFAULT_COUNTER_CHANCE = {0, 0, 0, 0, 0, 35, 100, 100, 100, 100, 100};
    private static final int[] DEFAULT_COUNTER_POWER = {0, 0, 0, 0, 0, 18, 24, 30, 38, 46, 60};
    private static final int[] DEFAULT_COUNTER_CRITICAL = {0, 0, 0, 0, 0, 8, 12, 16, 20, 25, 35};

    private static final long[] needExp = new long[MAX_LEVEL + 1];
    private static final int[] procBonus = new int[MAX_LEVEL + 1];
    private static final int[] criticalChance = new int[MAX_LEVEL + 1];
    private static final int[] criticalDamage = new int[MAX_LEVEL + 1];
    private static final int[] counterChance = new int[MAX_LEVEL + 1];
    private static final int[] counterPower = new int[MAX_LEVEL + 1];
    private static final int[] counterCritical = new int[MAX_LEVEL + 1];

    private EgoLevel() {
    }

    public static void reload(Connection con) {
        resetDefault();
        if (!loadMerged(con)) {
            loadLegacyExp(con);
            loadLegacyBonus(con);
        }
        needExp[MAX_LEVEL] = 0L;
    }

    public static long needExp(int level) {
        return needExp[clamp(level)];
    }

    public static int procBonus(int level) {
        return procBonus[clamp(level)];
    }

    public static int criticalChance(int level) {
        return criticalChance[clamp(level)];
    }

    public static int criticalDamage(int level) {
        return criticalDamage[clamp(level)];
    }

    public static int counterChance(int level) {
        return counterChance[clamp(level)];
    }

    public static int counterPower(int level) {
        return counterPower[clamp(level)];
    }

    public static int counterCritical(int level) {
        return counterCritical[clamp(level)];
    }

    private static void resetDefault() {
        for (int i = 0; i <= MAX_LEVEL; i++) {
            needExp[i] = DEFAULT_NEED_EXP[i];
            procBonus[i] = DEFAULT_PROC_BONUS[i];
            criticalChance[i] = DEFAULT_CRITICAL_CHANCE[i];
            criticalDamage[i] = DEFAULT_CRITICAL_DAMAGE[i];
            counterChance[i] = DEFAULT_COUNTER_CHANCE[i];
            counterPower[i] = DEFAULT_COUNTER_POWER[i];
            counterCritical[i] = DEFAULT_COUNTER_CRITICAL[i];
        }
    }

    private static boolean loadMerged(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        boolean loaded = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            if (!tableExists(con, "ego_level"))
                return false;
            st = con.prepareStatement("SELECT * FROM ego_level WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next()) {
                int lv = clamp(rs.getInt("ego_lv"));
                needExp[lv] = Math.max(0L, rs.getLong("need_exp"));
                procBonus[lv] = clampPercent(rs.getInt("proc_bonus"));
                criticalChance[lv] = clampPercent(rs.getInt("critical_chance"));
                criticalDamage[lv] = Math.max(0, rs.getInt("critical_damage"));
                counterChance[lv] = clampPercent(rs.getInt("counter_chance"));
                counterPower[lv] = Math.max(0, rs.getInt("counter_power"));
                counterCritical[lv] = clampPercent(rs.getInt("counter_critical"));
                loaded = true;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
        return loaded;
    }

    private static void loadLegacyExp(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            if (!tableExists(con, "ego_level_exp"))
                return;
            st = con.prepareStatement("SELECT ego_lv, need_exp FROM ego_level_exp WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next()) {
                int lv = clamp(rs.getInt("ego_lv"));
                needExp[lv] = Math.max(0L, rs.getLong("need_exp"));
            }
        } catch (Exception e) {
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    private static void loadLegacyBonus(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            if (!tableExists(con, "ego_level_bonus"))
                return;
            st = con.prepareStatement("SELECT * FROM ego_level_bonus WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next()) {
                int lv = clamp(rs.getInt("ego_lv"));
                procBonus[lv] = clampPercent(rs.getInt("proc_bonus"));
                criticalChance[lv] = clampPercent(rs.getInt("critical_chance"));
                criticalDamage[lv] = Math.max(0, rs.getInt("critical_damage"));
                counterChance[lv] = clampPercent(rs.getInt("counter_chance"));
                counterPower[lv] = Math.max(0, rs.getInt("counter_power"));
                counterCritical[lv] = clampPercent(rs.getInt("counter_critical"));
            }
        } catch (Exception e) {
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
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

    private static int clamp(int level) {
        if (level < 0)
            return 0;
        if (level > MAX_LEVEL)
            return MAX_LEVEL;
        return level;
    }

    private static int clampPercent(int value) {
        if (value < 0)
            return 0;
        if (value > 100)
            return 100;
        return value;
    }
}
