package lineage.database;

import java.sql.Connection;
import java.util.List;

import lineage.bean.database.Item;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.world.controller.EgoBond;
import lineage.world.controller.EgoConfig;
import lineage.world.controller.EgoTalkPack;
import lineage.world.controller.EgoWeaponAbilityController;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;
import lineage.world.object.item.EgoOrb;

/**
 * 에고 DB/아이템 Hook 통합 Facade.
 *
 * 외부 DB 로드, 에고 조회/생성/성장, 에고 구슬 아이템 Hook을 이 파일 1개로 처리한다.
 *
 * 로드 순서:
 * 1) EgoConfig
 * 2) EgoWeaponDatabase + EgoLevel
 * 3) EgoWeaponAbilityController
 * 4) EgoBond
 * 5) EgoTalkPack
 *
 * ItemDatabase Hook 연결:
 * ItemInstance egoItem = EgoDB.newInstance(i, item);
 * if (egoItem != null)
 *     return egoItem;
 */
public final class EgoDB {

    public static final int EGO_ORB_ITEM_CODE = 900001;
    public static final String EGO_ORB_ITEM_NAME = "에고 구슬";

    private EgoDB() {
    }

    public static void init(Connection con) {
        reload(con);
    }

    public static void reload(Connection con) {
        EgoConfig.reload(con);
        EgoWeaponDatabase.reload(con);
        EgoWeaponAbilityController.reloadConfig();
        EgoBond.reload(con);
        EgoTalkPack.reload(con);
    }

    public static EgoWeaponInfo find(long itemObjId) {
        return EgoWeaponDatabase.find(itemObjId);
    }

    public static EgoWeaponInfo find(ItemInstance item) {
        return EgoWeaponDatabase.find(item);
    }

    public static boolean isEgo(ItemInstance item) {
        return EgoWeaponDatabase.isEgoWeapon(item);
    }

    public static String name(ItemInstance item, String defaultName) {
        return EgoWeaponDatabase.getEgoName(item, defaultName);
    }

    public static int level(ItemInstance item, int defaultLevel) {
        return EgoWeaponDatabase.getEgoLevel(item, defaultLevel);
    }

    public static List<EgoAbilityInfo> skills(ItemInstance item) {
        return EgoWeaponDatabase.getAbilities(item);
    }

    public static EgoAbilityInfo firstSkill(ItemInstance item) {
        return EgoWeaponDatabase.getFirstAbility(item);
    }

    public static boolean create(PcInstance pc, ItemInstance item, String name, String personality) {
        return EgoWeaponDatabase.enableEgo(pc, item, name, personality);
    }

    public static boolean delete(ItemInstance item) {
        EgoBond.delete(item);
        return EgoWeaponDatabase.disableEgo(item);
    }

    public static boolean rename(ItemInstance item, String name) {
        return EgoWeaponDatabase.setEgoName(item, name);
    }

    public static boolean setSkill(ItemInstance item, String skill) {
        return EgoWeaponDatabase.setAbility(item, skill);
    }

    public static boolean addExp(ItemInstance item, long exp) {
        return EgoWeaponDatabase.addExp(item, exp);
    }

    public static ItemInstance newInstance(Item itemTemplate, ItemInstance pooledItem) {
        if (!isEgoOrb(itemTemplate))
            return null;
        return EgoOrb.clone(pooledItem);
    }

    public static ItemInstance newInstance(Item itemTemplate) {
        return newInstance(itemTemplate, null);
    }

    public static boolean isEgoOrb(Item itemTemplate) {
        if (itemTemplate == null)
            return false;
        try {
            if (itemTemplate.getItemCode() == EGO_ORB_ITEM_CODE)
                return true;
        } catch (Throwable e) {
        }
        try {
            String name = itemTemplate.getName();
            if (name == null)
                return false;
            String n = name.trim();
            return EGO_ORB_ITEM_NAME.equals(n);
        } catch (Throwable e) {
            return false;
        }
    }
}
