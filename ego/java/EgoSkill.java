package lineage.world.controller;

import lineage.world.object.Character;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 능력 클래스.
 *
 * DamageController 연결 시 EgoWeaponAbilityController 대신 EgoSkill 사용을 권장합니다.
 */
public final class EgoSkill {

    private EgoSkill() {
    }

    public static int attack(Character cha, object target, ItemInstance weapon, int damage) {
        return EgoWeaponAbilityController.applyAttackAbility(cha, target, weapon, damage);
    }

    public static void exp(PcInstance pc, MonsterInstance mon) {
        EgoWeaponAbilityController.addKillExp(pc, mon);
    }

    public static boolean isEgo(ItemInstance weapon) {
        return EgoWeaponAbilityController.isEgoWeapon(weapon);
    }

    public static int level(ItemInstance weapon) {
        return EgoWeaponAbilityController.getEgoLevel(weapon);
    }
}
