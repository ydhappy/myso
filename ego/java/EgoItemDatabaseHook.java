package lineage.database;

import lineage.bean.database.Item;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.item.EgoChangeOrb;

/**
 * 에고 전용 아이템 생성 Hook.
 *
 * ItemDatabase.newInstance(...) 내부의 일반 ItemInstance 생성 전에 아래처럼 연결한다.
 *
 * ItemInstance egoItem = EgoItemDatabaseHook.newInstance(i, item);
 * if (egoItem != null)
 *     return egoItem;
 *
 * 목적:
 * - 서버마다 item 테이블 구조가 달라도 Java 아이템 클래스 연결 누락을 줄인다.
 * - 에고 변경구슬 DB 등록 후 실제 더블클릭 로직이 EgoChangeOrb로 연결되도록 한다.
 */
public final class EgoItemDatabaseHook {

    private EgoItemDatabaseHook() {
    }

    public static ItemInstance newInstance(Item itemTemplate, ItemInstance pooledItem) {
        if (!isEgoChangeOrb(itemTemplate))
            return null;
        return EgoChangeOrb.clone(pooledItem);
    }

    public static ItemInstance newInstance(Item itemTemplate) {
        return newInstance(itemTemplate, null);
    }

    public static boolean isEgoChangeOrb(Item itemTemplate) {
        if (itemTemplate == null)
            return false;
        try {
            if (itemTemplate.getItemCode() == 900001)
                return true;
        } catch (Throwable e) {
        }
        try {
            String name = itemTemplate.getName();
            if (name != null && "에고 변경구슬".equals(name.trim()))
                return true;
        } catch (Throwable e) {
        }
        return false;
    }
}
