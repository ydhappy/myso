package lineage.world.object.item;

import lineage.network.packet.ClientBasePacket;
import lineage.world.object.Character;
import lineage.world.controller.EgoOrbController;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고 구슬.
 *
 * 사용 조건:
 * - 무기를 착용한 상태여야 한다.
 * - 착용 무기가 에고 대상 무기여야 한다.
 *
 * 동작:
 * - 에고가 없는 무기: 에고를 최초 생성한다.
 * - 이미 에고무기인 무기: 능력/대화/레벨을 변경하지 않고 현재 캐릭터를 주인으로 재인식한다.
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
        EgoOrbController.use((PcInstance) cha, this);
    }
}
