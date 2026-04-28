package lineage.world.object.item;

import lineage.network.packet.ClientBasePacket;
import lineage.world.controller.EgoCore;
import lineage.world.object.Character;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고 구슬.
 *
 * - 에고가 없는 착용 무기: 에고 최초 생성
 * - 이미 에고무기인 착용 무기: 능력/대화/레벨 변경 없이 현재 캐릭터를 주인으로 재인식
 */
public class EgoOrb extends ItemInstance {

    static synchronized public ItemInstance clone(ItemInstance item) {
        if (item == null)
            item = new EgoOrb();
        return item;
    }

    @Override
    public void toClick(Character cha, ClientBasePacket cbp) {
        if (!(cha instanceof PcInstance))
            return;
        EgoCore.useOrb((PcInstance) cha, this);
    }
}
