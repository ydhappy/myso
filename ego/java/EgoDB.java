package lineage.database;

import java.sql.Connection;
import java.util.List;

import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.world.controller.EgoBond;
import lineage.world.controller.EgoConfig;
import lineage.world.controller.EgoTalkPack;
import lineage.world.controller.EgoView;
import lineage.world.controller.EgoWeaponAbilityController;
import lineage.world.controller.EgoWeaponRule;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 DB 클래스.
 *
 * 로드 순서:
 * 1) EgoConfig           공통 설정
 * 2) EgoWeaponRule       무기 규칙
 * 3) EgoWeaponDatabase   에고 기본/스킬 + EgoLevel 통합 로드 + EgoView 로드
 * 4) EgoWeaponAbilityController 스킬베이스/전투 설정
 * 5) EgoBond             유대감, ego.bond 우선
 * 6) EgoTalkPack         DB 대사팩
 */
public final class EgoDB {

    private EgoDB() {
    }

    public static void init(Connection con) {
        reload(con);
    }

    public static void reload(Connection con) {
        EgoConfig.reload(con);
        EgoWeaponRule.reload(con);
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
}
