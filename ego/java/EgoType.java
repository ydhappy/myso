package lineage.world.controller;

import lineage.world.object.instance.ItemInstance;

/**
 * 짧은 이름용 에고 형태/무기종류 클래스.
 *
 * 적용 코드에서는 EgoWeaponTypeUtil 대신 EgoType 사용을 권장합니다.
 */
public final class EgoType {

    public static final String DAGGER = EgoWeaponTypeUtil.TYPE_DAGGER;
    public static final String SWORD = EgoWeaponTypeUtil.TYPE_SWORD;
    public static final String TWO_HAND = EgoWeaponTypeUtil.TYPE_TWO_HAND_SWORD;
    public static final String AXE = EgoWeaponTypeUtil.TYPE_AXE;
    public static final String SPEAR = EgoWeaponTypeUtil.TYPE_SPEAR;
    public static final String BOW = EgoWeaponTypeUtil.TYPE_BOW;
    public static final String STAFF = EgoWeaponTypeUtil.TYPE_STAFF;
    public static final String WAND = EgoWeaponTypeUtil.TYPE_WAND;

    private EgoType() {
    }

    public static String original(ItemInstance item) {
        return EgoWeaponTypeUtil.getOriginalType2(item);
    }

    public static String type(ItemInstance item) {
        return EgoWeaponTypeUtil.getType2(item);
    }

    public static String name(ItemInstance item) {
        return EgoWeaponTypeUtil.getDisplayTypeName(item);
    }

    public static boolean valid(ItemInstance item) {
        return EgoWeaponTypeUtil.isValidEgoBaseWeapon(item);
    }

    public static boolean supported(String type) {
        return EgoWeaponTypeUtil.isSupportedType(type);
    }

    public static String defaultSkill(ItemInstance item) {
        return EgoWeaponTypeUtil.getDefaultAbilityType(item);
    }

    public static boolean skillAllowed(String skill, ItemInstance item) {
        return EgoWeaponTypeUtil.isAbilityAllowed(skill, item);
    }

    public static String reason(String skill, ItemInstance item) {
        return EgoWeaponTypeUtil.getAbilityDenyReason(skill, item);
    }
}
