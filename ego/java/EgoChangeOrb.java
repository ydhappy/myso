package lineage.world.object.item;

import lineage.world.object.instance.ItemInstance;

/**
 * 구버전 호환 wrapper.
 *
 * 기존 에고 변경구슬 개념은 폐기한다.
 * 실제 아이템 클래스는 EgoOrb를 사용한다.
 */
public class EgoChangeOrb extends EgoOrb {

    static synchronized public ItemInstance clone(ItemInstance item) {
        if (item == null)
            item = new EgoChangeOrb();
        return item;
    }
}
