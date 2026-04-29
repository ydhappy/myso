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
 * - 리로드는 새 Snapshot을 만든 뒤 한 번에 교체한다.
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

    private static volatile Snapshot snapshot = Snapshot.defaults();

    private EgoLevel() {
    }

    public static void reload(Connection con) {
        Snapshot next = Snapshot.defaults();
        load(con, next);
        next.needExp[MAX_LEVEL] = 0L;
        snapshot = next;
    }

    public static long needExp(int level) {
        return snapshot.needExp[clamp(level)];
    }

    public static int procBonus(int level) {
        return snapshot.procBonus[clamp(level)];
    }

    public static int criticalChance(int level) {
        return snapshot.criticalChance[clamp(level)];
    }

    public static int criticalDamage(int level) {
        return snapshot.criticalDamage[clamp(level)];
    }

    public static int counterChance(int level) {
        return snapshot.counterChance[clamp(level)];
    }

    public static int counterPower(int level) {
        return snapshot.counterPower[clamp(level)];
    }

    public static int counterCritical(int level) {
        return snapshot.counterCritical[clamp(level)];
    }

    private static void load(Connection con, Snapshot target) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            if (con == null || !tableExists(con, "ego_level"))
                return;
            st = con.prepareStatement("SELECT ego_lv, need_exp, proc_bonus, critical_chance, critical_damage, counter_chance, counter_power, counter_critical FROM ego_level WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next()) {
                int lv = clamp(rs.getInt("ego_lv"));
                target.needExp[lv] = Math.max(0L, rs.getLong("need_exp"));
                target.procBonus[lv] = clampPercent(rs.getInt("proc_bonus"));
                target.criticalChance[lv] = clampPercent(rs.getInt("critical_chance"));
                target.criticalDamage[lv] = Math.max(0, rs.getInt("critical_damage"));
                target.counterChance[lv] = clampPercent(rs.getInt("counter_chance"));
                target.counterPower[lv] = Math.max(0, rs.getInt("counter_power"));
                target.counterCritical[lv] = clampPercent(rs.getInt("counter_critical"));
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

    private static final class Snapshot {
        final long[] needExp = new long[MAX_LEVEL + 1];
        final int[] procBonus = new int[MAX_LEVEL + 1];
        final int[] criticalChance = new int[MAX_LEVEL + 1];
        final int[] criticalDamage = new int[MAX_LEVEL + 1];
        final int[] counterChance = new int[MAX_LEVEL + 1];
        final int[] counterPower = new int[MAX_LEVEL + 1];
        final int[] counterCritical = new int[MAX_LEVEL + 1];

        static Snapshot defaults() {
            Snapshot s = new Snapshot();
            for (int i = 0; i <= MAX_LEVEL; i++) {
                s.needExp[i] = DEFAULT_NEED_EXP[i];
                s.procBonus[i] = DEFAULT_PROC_BONUS[i];
                s.criticalChance[i] = DEFAULT_CRITICAL_CHANCE[i];
                s.criticalDamage[i] = DEFAULT_CRITICAL_DAMAGE[i];
                s.counterChance[i] = DEFAULT_COUNTER_CHANCE[i];
                s.counterPower[i] = DEFAULT_COUNTER_POWER[i];
                s.counterCritical[i] = DEFAULT_COUNTER_CRITICAL[i];
            }
            return s;
        }
    }
}
