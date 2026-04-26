package lineage.world.controller;

import java.sql.Connection;

/**
 * 에고 레벨별 전투 보너스 호환 Facade.
 *
 * 실제 로드는 EgoLevel에서 통합 처리한다.
 * 기존 코드 호환을 위해 메서드명은 유지한다.
 */
public final class EgoLevelBonus {

    private EgoLevelBonus() {
    }

    public static void reload(Connection con) {
        EgoLevel.reload(con);
    }

    public static int procBonus(int level) {
        return EgoLevel.procBonus(level);
    }

    public static int criticalChance(int level) {
        return EgoLevel.criticalChance(level);
    }

    public static int criticalDamage(int level) {
        return EgoLevel.criticalDamage(level);
    }

    public static int counterChance(int level) {
        return EgoLevel.counterChance(level);
    }

    public static int counterPower(int level) {
        return EgoLevel.counterPower(level);
    }

    public static int counterCritical(int level) {
        return EgoLevel.counterCritical(level);
    }
}
