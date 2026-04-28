package lineage.world.controller;

import lineage.share.Lineage;
import lineage.world.object.instance.ItemInstance;

/**
 * 에고무기 대상 판정 유틸.
 *
 * 최종 정책:
 * - 무기 슬롯이면 에고 대상이다.
 * - 낚싯대(fishing_rod)는 제외한다.
 * - Java 전투 컨트롤러 런타임 능력명을 기본값으로 반환한다.
 */
public final class EgoWeaponTypeUtil {

    private static final String FISHING_ROD = "fishing_rod";

    private EgoWeaponTypeUtil() {
    }

    public static String getOriginalType2(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return "";
        return normalize(item.getItem().getType2());
    }

    public static String getType2(ItemInstance item) {
        return getOriginalType2(item);
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

    public static boolean isFishingRod(ItemInstance item) {
        return FISHING_ROD.equals(getOriginalType2(item));
    }

    public static boolean isValidEgoBaseWeapon(ItemInstance item) {
        return isWeaponSlot(item) && !isFishingRod(item);
    }

    public static boolean isAbilityAllowed(String ability, ItemInstance item) {
        return normalizeAbility(ability).length() > 0 && isValidEgoBaseWeapon(item);
    }

    public static String getDefaultAbilityType(ItemInstance item) {
        String name = safeItemName(item).toLowerCase();
        if (name.indexOf("피") >= 0 || name.indexOf("blood") >= 0 || name.indexOf("흡혈") >= 0)
            return "BLOOD_DRAIN";
        if (name.indexOf("마나") >= 0 || name.indexOf("지식") >= 0 || name.indexOf("mana") >= 0)
            return "MANA_DRAIN";
        if (name.indexOf("화염") >= 0 || name.indexOf("불") >= 0 || name.indexOf("flame") >= 0 || name.indexOf("fire") >= 0)
            return "FLAME_BRAND";
        if (name.indexOf("얼음") >= 0 || name.indexOf("서리") >= 0 || name.indexOf("frost") >= 0 || name.indexOf("ice") >= 0)
            return "FROST_BIND";
        if (name.indexOf("수호") >= 0 || name.indexOf("가디언") >= 0 || name.indexOf("guardian") >= 0)
            return "GUARDIAN_SHIELD";
        return "EGO_BALANCE";
    }

    public static String getDisplayTypeName(ItemInstance item) {
        if (isFishingRod(item))
            return "낚싯대";
        if (isWeaponSlot(item))
            return "무기";
        return "무기 아님";
    }

    public static String getAbilityDenyReason(String ability, ItemInstance item) {
        if (item == null)
            return "착용 무기가 없습니다.";
        if (!isWeaponSlot(item))
            return "무기 슬롯 아이템이 아닙니다.";
        if (isFishingRod(item))
            return "낚싯대는 에고무기로 사용할 수 없습니다.";
        return "에고 대상으로 사용할 수 있습니다.";
    }

    public static String getSupportedWeaponTypesText() {
        return "지원 대상: 무기 슬롯 전체 / 제외: 낚싯대(fishing_rod)";
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
        return ability == null ? "" : ability.trim().toUpperCase();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
