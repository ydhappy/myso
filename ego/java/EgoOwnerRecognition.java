package lineage.world.controller;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.share.Lineage;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 주인 자동 재인식 헬퍼.
 *
 * 에고무기가 다른 캐릭터에게 이동되어 착용되면 능력/대화/레벨/경험치는 유지하고
 * ego.char_id만 현재 캐릭터로 갱신한다.
 */
public final class EgoOwnerRecognition {

    private EgoOwnerRecognition() {
    }

    public static void recognize(PcInstance pc) {
        if (pc == null)
            return;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return;
        recognize(pc, inv.getSlot(Lineage.SLOT_WEAPON));
    }

    public static void recognize(PcInstance pc, ItemInstance weapon) {
        if (pc == null || weapon == null)
            return;
        if (!EgoWeaponDatabase.isEgoWeapon(weapon))
            return;
        EgoWeaponDatabase.recognizeOwner(pc, weapon);
    }
}
