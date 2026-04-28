package lineage.world.controller;

import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 구버전 호환 wrapper.
 *
 * 기존 에고 변경구슬 개념은 폐기한다.
 * 실제 처리는 EgoOrbController가 담당한다.
 */
public final class EgoChangeOrbController {

    public static final int DEFAULT_ITEM_CODE = EgoOrbController.DEFAULT_ITEM_CODE;
    public static final String ITEM_NAME = EgoOrbController.ITEM_NAME;

    private EgoChangeOrbController() {
    }

    public static boolean use(PcInstance pc, ItemInstance orb) {
        return EgoOrbController.use(pc, orb);
    }

    public static String randomAbility(ItemInstance weapon) {
        return EgoOrbController.randomAbility(weapon);
    }

    public static String randomTone() {
        return EgoOrbController.randomTone();
    }
}
