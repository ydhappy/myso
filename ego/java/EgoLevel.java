package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import lineage.database.DatabaseConnection;

/**
 * 에고 레벨 캐시.
 *
 * DB/Java 1:1 기준:
 * - Java는 ego_level 테이블만 읽는다.
 * - ego_level_exp / ego_level_bonus fallback은 사용하지 않는다.
 * - DB 값이 없거나 로드 실패 시 Java 기본값으로만 보정한다.
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
        load(con);
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

    private static void load(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            if (!tableExists(con, "ego_level"))
                return;
            st = con.prepareStatement("SELECT ego_lv, need_exp, proc_bonus, critical_chance, critical_damage, counter_chance, counter_power, counter_critical FROM ego_level WHERE use_yn=1");
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
            return rs != null && rs.next();
        } catch (SQLException e) {
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
        }
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
