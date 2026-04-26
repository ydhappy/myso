package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import lineage.database.DatabaseConnection;

/**
 * 에고 레벨별 전투 보너스 DB 캐시.
 *
 * ego_level_bonus 테이블이 있으면 DB 값을 사용한다.
 * 테이블/값이 없으면 Java 기본값으로 fallback한다.
 */
public final class EgoLevelBonus {

    public static final int MAX_LEVEL = 10;

    private static final int[] DEFAULT_PROC_BONUS = {0, 0, 1, 2, 3, 4, 6, 8, 10, 12, 15};
    private static final int[] DEFAULT_CRITICAL_CHANCE = {0, 1, 2, 3, 4, 6, 9, 12, 15, 18, 25};
    private static final int[] DEFAULT_CRITICAL_DAMAGE = {0, 1, 2, 3, 4, 6, 8, 10, 12, 15, 20};
    private static final int[] DEFAULT_COUNTER_CHANCE = {0, 0, 0, 0, 0, 35, 100, 100, 100, 100, 100};
    private static final int[] DEFAULT_COUNTER_POWER = {0, 0, 0, 0, 0, 18, 24, 30, 38, 46, 60};
    private static final int[] DEFAULT_COUNTER_CRITICAL = {0, 0, 0, 0, 0, 8, 12, 16, 20, 25, 35};

    private static final int[] procBonus = new int[MAX_LEVEL + 1];
    private static final int[] criticalChance = new int[MAX_LEVEL + 1];
    private static final int[] criticalDamage = new int[MAX_LEVEL + 1];
    private static final int[] counterChance = new int[MAX_LEVEL + 1];
    private static final int[] counterPower = new int[MAX_LEVEL + 1];
    private static final int[] counterCritical = new int[MAX_LEVEL + 1];

    private EgoLevelBonus() {
    }

    public static void reload(Connection con) {
        resetDefault();
        load(con);
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
            // ego_level_bonus 미적용 서버에서는 Java 기본값으로 동작한다.
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
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
