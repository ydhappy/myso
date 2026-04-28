package lineage.world.controller;

import lineage.share.Lineage;
import lineage.world.object.instance.ItemInstance;

/**
 * 에고무기 원본 무기종류 판정 유틸.
 *
 * 원칙:
 * - 에고는 원본 item.type2를 변형하지 않는다.
 * - getType2(item)는 항상 원본 item.getItem().getType2()만 반환한다.
 * - PcInstance, DamageController, 공격 사거리, 화살 소비, 무기 공식은 기존 서버 코어를 따른다.
 * - ego_weapon_rule 테이블이 있으면 DB 규칙을 우선 사용하고, 없으면 Java 기본 규칙을 사용한다.
 */
public final class EgoWeaponTypeUtil {

    public static final String TYPE_DAGGER = "dagger";
    public static final String TYPE_SWORD = "sword";
    public static final String TYPE_TWO_HAND_SWORD = "tohandsword";
    public static final String TYPE_AXE = "axe";
    public static final String TYPE_SPEAR = "spear";
    public static final String TYPE_BOW = "bow";
    public static final String TYPE_STAFF = "staff";
    public static final String TYPE_WAND = "wand";
    public static final String TYPE_FISHING_ROD = "fishing_rod";

    private EgoWeaponTypeUtil() {
    }

    public static String getOriginalType2(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return "";
        return normalizeType(item.getItem().getType2());
    }

    public static String getType2(ItemInstance item) {
        return getOriginalType2(item);
    }

    public static boolean isSupportedType(String type2) {
        String type = normalizeType(type2);
        if (type.length() == 0)
            return false;
        if (EgoWeaponRule.hasRule(type))
            return EgoWeaponRule.isSupportedType(type);
        return TYPE_DAGGER.equals(type)
            || TYPE_SWORD.equals(type)
            || TYPE_TWO_HAND_SWORD.equals(type)
            || TYPE_AXE.equals(type)
            || TYPE_SPEAR.equals(type)
            || TYPE_BOW.equals(type)
            || TYPE_STAFF.equals(type)
            || TYPE_WAND.equals(type);
    }

    public static boolean isWeaponSlot(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return false;
        try {
            return item.getItem().getSlot() == Lineage.SLOT_WEAPON;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isValidEgoBaseWeapon(ItemInstance item) {
        if (!isWeaponSlot(item))
            return false;
        String originalType = getOriginalType2(item);
        if (TYPE_FISHING_ROD.equals(originalType))
            return false;
        return isSupportedType(originalType);
    }

    public static boolean isFishingRod(ItemInstance item) {
        return TYPE_FISHING_ROD.equals(getOriginalType2(item));
    }

    public static boolean isDagger(ItemInstance item) {
        return TYPE_DAGGER.equals(getOriginalType2(item));
    }

    public static boolean isOneHandSword(ItemInstance item) {
        return TYPE_SWORD.equals(getOriginalType2(item));
    }

    public static boolean isTwoHandSword(ItemInstance item) {
        return TYPE_TWO_HAND_SWORD.equals(getOriginalType2(item));
    }

    public static boolean isAxe(ItemInstance item) {
        return TYPE_AXE.equals(getOriginalType2(item));
    }

    public static boolean isSpear(ItemInstance item) {
        return TYPE_SPEAR.equals(getOriginalType2(item));
    }

    public static boolean isBow(ItemInstance item) {
        return TYPE_BOW.equals(getOriginalType2(item));
    }

    public static boolean isStaff(ItemInstance item) {
        return TYPE_STAFF.equals(getOriginalType2(item));
    }

    public static boolean isWand(ItemInstance item) {
        return TYPE_WAND.equals(getOriginalType2(item));
    }

    public static boolean isMagicWeapon(ItemInstance item) {
        return isStaff(item) || isWand(item);
    }

    public static boolean isMelee(ItemInstance item) {
        return isDagger(item) || isOneHandSword(item) || isTwoHandSword(item) || isAxe(item) || isSpear(item);
    }

    public static boolean isHeavyMelee(ItemInstance item) {
        return isTwoHandSword(item) || isAxe(item);
    }

    public static String getDisplayTypeName(ItemInstance item) {
        String type = getOriginalType2(item);
        if (EgoWeaponRule.hasRule(type))
            return EgoWeaponRule.displayName(type, type.length() == 0 ? "알 수 없음" : type);
        if (TYPE_DAGGER.equals(type)) return "단검";
        if (TYPE_SWORD.equals(type)) return "한손검";
        if (TYPE_TWO_HAND_SWORD.equals(type)) return "양손검";
        if (TYPE_AXE.equals(type)) return "도끼";
        if (TYPE_SPEAR.equals(type)) return "창";
        if (TYPE_BOW.equals(type)) return "활";
        if (TYPE_STAFF.equals(type)) return "지팡이";
        if (TYPE_WAND.equals(type)) return "완드";
        if (TYPE_FISHING_ROD.equals(type)) return "낚싯대";
        if (type.length() == 0) return "알 수 없음";
        return type;
    }

    public static String getDefaultAbilityType(ItemInstance item) {
        String type2 = getOriginalType2(item);
        String javaDefault = getJavaDefaultAbilityType(item);
        if (EgoWeaponRule.hasRule(type2))
            return EgoWeaponRule.defaultAbility(type2, javaDefault);
        return javaDefault;
    }

    private static String getJavaDefaultAbilityType(ItemInstance item) {
        String name = safeItemName(item).toLowerCase();
        if (name.indexOf("피") >= 0 || name.indexOf("blood") >= 0 || name.indexOf("흡혈") >= 0) return "BLOOD_DRAIN";
        if (name.indexOf("마나") >= 0 || name.indexOf("지식") >= 0 || name.indexOf("mana") >= 0) return "MANA_DRAIN";
        if (name.indexOf("화염") >= 0 || name.indexOf("불") >= 0 || name.indexOf("flame") >= 0 || name.indexOf("fire") >= 0) return "FLAME_BRAND";
        if (name.indexOf("얼음") >= 0 || name.indexOf("서리") >= 0 || name.indexOf("frost") >= 0 || name.indexOf("ice") >= 0) return "FROST_BIND";
        if (name.indexOf("수호") >= 0 || name.indexOf("가디언") >= 0 || name.indexOf("guardian") >= 0) return "GUARDIAN_SHIELD";
        if (isMagicWeapon(item)) return "MANA_DRAIN";
        if (isSpear(item)) return "AREA_SLASH";
        if (isHeavyMelee(item)) return "CRITICAL_BURST";
        return "EGO_BALANCE";
    }

    public static boolean isAbilityAllowed(String abilityType, ItemInstance item) {
        String ability = normalizeAbility(abilityType);
        if (ability.length() == 0 || !isValidEgoBaseWeapon(item))
            return false;
        String type2 = getOriginalType2(item);
        boolean javaFallback = isJavaAbilityAllowed(ability, item);
        if (EgoWeaponRule.hasRule(type2))
            return EgoWeaponRule.isAbilityAllowed(type2, ability, javaFallback);
        return javaFallback;
    }

    private static boolean isJavaAbilityAllowed(String abilityType, ItemInstance item) {
        String type = normalizeAbility(abilityType);
        if (type.length() == 0 || !isValidEgoBaseWeapon(item))
            return false;
        if ("EGO_BALANCE".equals(type)) return true;
        if ("BLOOD_DRAIN".equals(type)) return isMelee(item);
        if ("MANA_DRAIN".equals(type)) return isMagicWeapon(item) || isDagger(item) || isOneHandSword(item);
        if ("CRITICAL_BURST".equals(type)) return isMelee(item) || isBow(item);
        if ("GUARDIAN_SHIELD".equals(type)) return true;
        if ("AREA_SLASH".equals(type)) return isSpear(item) || isTwoHandSword(item) || isAxe(item);
        if ("EXECUTION".equals(type)) return isDagger(item) || isOneHandSword(item) || isTwoHandSword(item) || isAxe(item);
        if ("FLAME_BRAND".equals(type)) return isMelee(item) || isMagicWeapon(item);
        if ("FROST_BIND".equals(type)) return isMagicWeapon(item) || isSpear(item) || isBow(item);
        if ("EGO_COUNTER".equals(type)) return true;
        if ("EGO_REVENGE".equals(type)) return true;
        return false;
    }

    public static String getAbilityDenyReason(String abilityType, ItemInstance item) {
        if (item == null)
            return "착용 무기가 없습니다.";
        if (!isWeaponSlot(item))
            return "무기 슬롯 아이템이 아닙니다.";
        if (isFishingRod(item))
            return "낚싯대는 에고무기로 사용할 수 없습니다.";
        if (!isValidEgoBaseWeapon(item))
            return "지원하지 않는 원본 무기 타입입니다. originalType2=" + getOriginalType2(item);
        return "이 능력은 원본 무기 타입(" + getDisplayTypeName(item) + ")에 사용할 수 없습니다.";
    }

    public static String getSupportedWeaponTypesText() {
        String fallback = "지원 원본 무기 타입: 단검(dagger), 한손검(sword), 양손검(tohandsword), 도끼(axe), 창(spear), 활(bow), 지팡이(staff), 완드(wand) / 원본 낚싯대(fishing_rod)는 제외";
        return EgoWeaponRule.supportedText(fallback);
    }

    private static String safeItemName(ItemInstance item) {
        if (item == null)
            return "";
        try {
            String name = item.getName();
            return name == null ? "" : name.trim();
        } catch (Throwable e) {
            return "";
        }
    }

    private static String normalizeAbility(String ability) {
        if (ability == null)
            return "";
        return ability.trim().toUpperCase();
    }

    private static String normalizeType(String type) {
        if (type == null)
            return "";
        type = type.trim().toLowerCase();
        if ("twohand_sword".equals(type) || "two_handed_sword".equals(type) || "twohandsword".equals(type))
            return TYPE_TWO_HAND_SWORD;
        return type;
    }
}
