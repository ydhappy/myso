package lineage.world.object.item;

import lineage.network.packet.ClientBasePacket;
import lineage.world.Character;
import lineage.world.controller.EgoChangeOrbController;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고 변경구슬.
 *
 * 사용 조건:
 * - 착용 중인 무기가 에고무기여야 한다.
 * - 사용 시 구슬 1개를 소모한다.
 * - 에고 능력과 대화 성향이 랜덤으로 재선택된다.
 */
public class EgoChangeOrb extends ItemInstance {

    static synchronized public ItemInstance clone(ItemInstance item) {
        if (item == null)
            item = new EgoChangeOrb();
        return item;
    }

    @Override
    public void toClick(Character cha, ClientBasePacket cbp) {
        if (!(cha instanceof PcInstance))
            return;
        EgoChangeOrbController.use((PcInstance) cha, this);
    }
}
