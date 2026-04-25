package ego.portable;

import java.util.Locale;

/**
 * 타 서버코어 공통 사용 가능한 에고무기 순수 규칙 유틸.
 *
 * Java 8 호환 기준:
 * - var 사용 안 함
 * - List.of/Map.of/Set.of 사용 안 함
 * - module-info.java 사용 안 함
 * - Stream/Lambda 의존 안 함
 * - switch expression 사용 안 함
 * - 한글 문자열은 UTF-8 컴파일 옵션 필요
 *
 * 이 파일은 lineage.* 의존성이 없습니다.
 * 다른 서버코어에서도 package만 맞추면 거의 그대로 사용할 수 있습니다.
 */
public final class EgoPortableRules {

    public static final String ABILITY_BALANCE = "EGO_BALANCE";
    public static final String ABILITY_BLOOD_DRAIN = "BLOOD_DRAIN";
    public static final String ABILITY_MANA_DRAIN = "MANA_DRAIN";
    public static final String ABILITY_CRITICAL_BURST = "CRITICAL_BURST";
    public static final String ABILITY_GUARDIAN_SHIELD = "GUARDIAN_SHIELD";
    public static final String ABILITY_AREA_SLASH = "AREA_SLASH";
    public static final String ABILITY_EXECUTION = "EXECUTION";
    public static final String ABILITY_FLAME_BRAND = "FLAME_BRAND";
    public static final String ABILITY_FROST_BIND = "FROST_BIND";

    private EgoPortableRules() {
    }

    public static boolean isSupportedWeaponType(String type2) {
        String t = normalize(type2);
        return isMelee(t) || isBow(t) || isMagicWeapon(t);
    }

    public static boolean isFishingRod(String type2) {
        return "fishing_rod".equals(normalize(type2));
    }

    public static boolean isMelee(String type2) {
        String t = normalize(type2);
        return "dagger".equals(t)
            || "sword".equals(t)
            || "tohandsword".equals(t)
            || "twohand_sword".equals(t)
            || "two_handed_sword".equals(t)
            || "axe".equals(t)
            || "spear".equals(t);
    }

    public static boolean isBow(String type2) {
        String t = normalize(type2);
        return "bow".equals(t) || "crossbow".equals(t);
    }

    public static boolean isMagicWeapon(String type2) {
        String t = normalize(type2);
        return "staff".equals(t) || "wand".equals(t);
    }

    public static boolean isHeavyMelee(String type2) {
        String t = normalize(type2);
        return "tohandsword".equals(t)
            || "twohand_sword".equals(t)
            || "two_handed_sword".equals(t)
            || "axe".equals(t);
    }

    public static String getDisplayWeaponType(String type2) {
        String t = normalize(type2);

        if ("dagger".equals(t))
            return "단검";
        if ("sword".equals(t))
            return "한손검";
        if ("tohandsword".equals(t) || "twohand_sword".equals(t) || "two_handed_sword".equals(t))
            return "양손검";
        if ("axe".equals(t))
            return "도끼";
        if ("spear".equals(t))
            return "창";
        if ("bow".equals(t) || "crossbow".equals(t))
            return "활";
        if ("staff".equals(t))
            return "지팡이";
        if ("wand".equals(t))
            return "완드";
        if ("fishing_rod".equals(t))
            return "낚싯대";
        if (t.length() == 0)
            return "알 수 없음";
        return t;
    }

    public static String getDefaultAbility(String type2, String itemName) {
        String name = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);

        if (name.contains("피") || name.contains("blood") || name.contains("흡혈"))
            return ABILITY_BLOOD_DRAIN;
        if (name.contains("마나") || name.contains("지식") || name.contains("mana"))
            return ABILITY_MANA_DRAIN;
        if (name.contains("화염") || name.contains("불") || name.contains("flame") || name.contains("fire"))
            return ABILITY_FLAME_BRAND;
        if (name.contains("얼음") || name.contains("서리") || name.contains("frost") || name.contains("ice"))
            return ABILITY_FROST_BIND;
        if (name.contains("수호") || name.contains("가디언") || name.contains("guardian"))
            return ABILITY_GUARDIAN_SHIELD;

        if (isMagicWeapon(type2))
            return ABILITY_MANA_DRAIN;
        if ("spear".equals(normalize(type2)))
            return ABILITY_AREA_SLASH;
        if (isHeavyMelee(type2))
            return ABILITY_CRITICAL_BURST;
        return ABILITY_BALANCE;
    }

    public static boolean isAbilityAllowed(String abilityType, String type2) {
        String ability = abilityType == null ? "" : abilityType.trim().toUpperCase(Locale.ROOT);
        String t = normalize(type2);

        if (!isSupportedWeaponType(t))
            return false;
        if (ABILITY_BALANCE.equals(ability))
            return true;
        if (ABILITY_BLOOD_DRAIN.equals(ability))
            return isMelee(t);
        if (ABILITY_MANA_DRAIN.equals(ability))
            return isMagicWeapon(t) || "dagger".equals(t) || "sword".equals(t);
        if (ABILITY_CRITICAL_BURST.equals(ability))
            return isMelee(t) || isBow(t);
        if (ABILITY_GUARDIAN_SHIELD.equals(ability))
            return true;
        if (ABILITY_AREA_SLASH.equals(ability))
            return "spear".equals(t) || isHeavyMelee(t);
        if (ABILITY_EXECUTION.equals(ability))
            return "dagger".equals(t) || "sword".equals(t) || isHeavyMelee(t);
        if (ABILITY_FLAME_BRAND.equals(ability))
            return isMelee(t) || isMagicWeapon(t);
        if (ABILITY_FROST_BIND.equals(ability))
            return isMagicWeapon(t) || "spear".equals(t) || isBow(t);
        return false;
    }

    public static int hpRate(int nowHp, int maxHp) {
        return nowHp * 100 / Math.max(1, maxHp);
    }

    public static String hpBand(int nowHp, int maxHp) {
        int rate = hpRate(nowHp, maxHp);
        if (rate <= 25)
            return "위험";
        if (rate <= 50)
            return "낮음";
        if (rate <= 75)
            return "보통";
        return "높음";
    }

    public static String riskGrade(int score) {
        if (score >= 7)
            return "매우위험";
        if (score >= 4)
            return "위험";
        if (score >= 2)
            return "주의";
        return "낮음";
    }

    public static String normalize(String value) {
        if (value == null)
            return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
