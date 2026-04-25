package lineage.world.controller;

import lineage.share.Lineage;
import lineage.world.object.instance.ItemInstance;

/**
 * 에고무기 원본 무기종류 판정 유틸.
 *
 * 중요 원칙:
 * - 에고는 원본 item.type2를 변형하지 않는다.
 * - getType2(item)는 항상 원본 item.getItem().getType2()만 반환한다.
 * - ego.form 값은 인벤토리 표시/대화/문서용 표시형태일 뿐, 무기 type2가 아니다.
 * - PcInstance, DamageController, 공격 사거리, 화살 소비, 무기 공식은 기존 서버 코어를 따른다.
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
        if (item == null || item.getItem() == null || item.getItem().getType2() == null)
            return "";
        return normalizeType(item.getItem().getType2());
    }

    /**
     * 원본 무기 type2.
     *
     * 이전 버전에서는 에고 표시형태를 type2처럼 우선 반환했지만,
     * 기존 서버 코어 유지 정책에 따라 제거했다.
     */
    public static String getType2(ItemInstance item) {
        return getOriginalType2(item);
    }

    public static boolean isSupportedType(String type2) {
        String type = normalizeType(type2);
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
        return item.getItem().getSlot() == Lineage.SLOT_WEAPON;
    }

    public static boolean isValidEgoBaseWeapon(ItemInstance item) {
        if (!isWeaponSlot(item))
            return false;

        String originalType = getOriginalType2(item);
        if (TYPE_FISHING_ROD.equals(originalType))
            return false;

        if (originalType.length() == 0)
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

        if (TYPE_DAGGER.equals(type))
            return "단검";
        if (TYPE_SWORD.equals(type))
            return "한손검";
        if (TYPE_TWO_HAND_SWORD.equals(type))
            return "양손검";
        if (TYPE_AXE.equals(type))
            return "도끼";
        if (TYPE_SPEAR.equals(type))
            return "창";
        if (TYPE_BOW.equals(type))
            return "활";
        if (TYPE_STAFF.equals(type))
            return "지팡이";
        if (TYPE_WAND.equals(type))
            return "완드";
        if (TYPE_FISHING_ROD.equals(type))
            return "낚싯대";
        if (type.length() == 0)
            return "알 수 없음";
        return type;
    }

    public static String getDefaultAbilityType(ItemInstance item) {
        if (item == null)
            return "EGO_BALANCE";

        String name = item.getName() == null ? "" : item.getName().toLowerCase();

        if (name.contains("피") || name.contains("blood") || name.contains("흡혈"))
            return "BLOOD_DRAIN";
        if (name.contains("마나") || name.contains("지식") || name.contains("mana"))
            return "MANA_DRAIN";
        if (name.contains("화염") || name.contains("불") || name.contains("flame") || name.contains("fire"))
            return "FLAME_BRAND";
        if (name.contains("얼음") || name.contains("서리") || name.contains("frost") || name.contains("ice"))
            return "FROST_BIND";
        if (name.contains("수호") || name.contains("가디언") || name.contains("guardian"))
            return "GUARDIAN_SHIELD";
        if (isMagicWeapon(item))
            return "MANA_DRAIN";
        if (isSpear(item))
            return "AREA_SLASH";
        if (isHeavyMelee(item))
            return "CRITICAL_BURST";
        if (isBow(item))
            return "EGO_BALANCE";
        return "EGO_BALANCE";
    }

    public static boolean isAbilityAllowed(String abilityType, ItemInstance item) {
        if (abilityType == null || !isValidEgoBaseWeapon(item))
            return false;

        String type = abilityType.trim().toUpperCase();

        if ("EGO_BALANCE".equals(type))
            return true;
        if ("BLOOD_DRAIN".equals(type))
            return isMelee(item);
        if ("MANA_DRAIN".equals(type))
            return isMagicWeapon(item) || isDagger(item) || isOneHandSword(item);
        if ("CRITICAL_BURST".equals(type))
            return isMelee(item) || isBow(item);
        if ("GUARDIAN_SHIELD".equals(type))
            return true;
        if ("AREA_SLASH".equals(type))
            return isSpear(item) || isTwoHandSword(item) || isAxe(item);
        if ("EXECUTION".equals(type))
            return isDagger(item) || isOneHandSword(item) || isTwoHandSword(item) || isAxe(item);
        if ("FLAME_BRAND".equals(type))
            return isMelee(item) || isMagicWeapon(item);
        if ("FROST_BIND".equals(type))
            return isMagicWeapon(item) || isSpear(item) || isBow(item);

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
        return "지원 원본 무기 타입: 단검(dagger), 한손검(sword), 양손검(tohandsword), 도끼(axe), 창(spear), 활(bow), 지팡이(staff), 완드(wand) / 원본 낚싯대(fishing_rod)는 제외";
    }

    private static String normalizeType(String type) {
        if (type == null)
            return "";
        type = type.trim().toLowerCase();
        if ("twohand_sword".equals(type) || "two_handed_sword".equals(type))
            return TYPE_TWO_HAND_SWORD;
        return type;
    }
}
