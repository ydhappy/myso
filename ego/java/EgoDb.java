package lineage.world.controller;

import java.sql.Connection;
import java.util.List;

import lineage.database.EgoDB;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 DB Facade.
 *
 * 외부 코드에서는 가능하면 EgoDB/EgoWeaponDatabase 직접 접근 대신 EgoDb 또는 EgoCore를 사용합니다.
 * 목적은 DB/Java 연결 명칭을 단순화하는 것입니다.
 */
public final class EgoDb {

    private EgoDb() {
    }

    public static void init(Connection con) {
        EgoCore.init(con);
    }

    public static void reload(Connection con) {
        EgoCore.reload(con);
    }

    public static boolean schemaOk(Connection con) {
        return EgoCore.schemaOk(con);
    }

    public static String schemaReport(Connection con) {
        return EgoCore.schemaReport(con);
    }

    public static EgoWeaponInfo find(long itemObjId) {
        return EgoDB.find(itemObjId);
    }

    public static EgoWeaponInfo find(ItemInstance item) {
        return EgoDB.find(item);
    }

    public static boolean isEgo(ItemInstance item) {
        return EgoDB.isEgo(item);
    }

    public static String name(ItemInstance item, String defaultName) {
        return EgoDB.name(item, defaultName);
    }

    public static int level(ItemInstance item, int defaultLevel) {
        return EgoDB.level(item, defaultLevel);
    }

    public static List<EgoAbilityInfo> skills(ItemInstance item) {
        return EgoDB.skills(item);
    }

    public static EgoAbilityInfo firstSkill(ItemInstance item) {
        return EgoDB.firstSkill(item);
    }

    public static boolean create(PcInstance pc, ItemInstance item, String name, String tone) {
        return EgoDB.create(pc, item, name, tone);
    }

    public static boolean delete(ItemInstance item) {
        return EgoDB.delete(item);
    }

    public static boolean rename(ItemInstance item, String name) {
        return EgoDB.rename(item, name);
    }

    public static boolean setSkill(ItemInstance item, String skill) {
        return EgoDB.setSkill(item, skill);
    }

    public static boolean addExp(ItemInstance item, long exp) {
        return EgoDB.addExp(item, exp);
    }
}
