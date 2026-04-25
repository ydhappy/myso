package lineage.world.controller;

import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 형태변환 클래스.
 */
public final class EgoForm {

    private EgoForm() {
    }

    public static boolean chat(PcInstance pc, ItemInstance weapon, String command) {
        return EgoWeaponFormController.handleTalk(pc, weapon, command);
    }

    public static void change(PcInstance pc, ItemInstance weapon, String form) {
        EgoWeaponFormController.transform(pc, weapon, form);
    }

    public static boolean noShield(String form) {
        return EgoWeaponFormController.isNoShieldForm(form);
    }

    public static String name(String form) {
        return EgoWeaponFormController.displayForm(form);
    }
}
