package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoDB;
import lineage.database.EgoWeaponDatabase;
import lineage.share.Lineage;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고 변경구슬 사용 처리.
 *
 * 정책:
 * - 에고 생성/삭제는 무료.
 * - 에고 능력/대화 성향 변경은 명령어가 아니라 이 아이템 사용으로만 처리한다.
 * - 사용 시 착용 중인 에고무기의 능력과 대화 성향을 랜덤 재선택한다.
 */
public final class EgoChangeOrbController {

    public static final int DEFAULT_ITEM_CODE = 900001;
    public static final String ITEM_NAME = "에고 변경구슬";

    private static final Random random = new Random();
    private static final String[] TONES = { "예의", "예의반대" };
    private static final String[] ABILITIES = {
        "EGO_BALANCE",
        "BLOOD_DRAIN",
        "MANA_DRAIN",
        "CRITICAL_BURST",
        "GUARDIAN_SHIELD",
        "AREA_SLASH",
        "EXECUTION",
        "FLAME_BRAND",
        "FROST_BIND"
    };

    private EgoChangeOrbController() {
    }

    public static boolean use(PcInstance pc, ItemInstance orb) {
        if (pc == null)
            return false;

        Inventory inv = pc.getInventory();
        if (inv == null) {
            EgoMessageUtil.danger(pc, "인벤토리 정보를 찾을 수 없습니다.");
            return false;
        }

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null) {
            EgoMessageUtil.danger(pc, "에고 변경구슬은 에고무기를 착용한 상태에서만 사용할 수 있습니다.");
            return false;
        }
        if (!EgoWeaponDatabase.isEgoWeapon(weapon)) {
            EgoMessageUtil.danger(pc, "현재 착용 무기는 에고무기가 아닙니다.");
            return false;
        }
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            EgoMessageUtil.danger(pc, "현재 착용 무기는 에고 변경 대상이 아닙니다.");
            return false;
        }
        if (orb == null || orb.getCount() <= 0) {
            EgoMessageUtil.danger(pc, "에고 변경구슬이 없습니다.");
            return false;
        }

        String ability = randomAbility(weapon);
        String tone = randomTone();
        if (ability.length() == 0) {
            EgoMessageUtil.danger(pc, "현재 무기에 적용 가능한 에고 능력을 찾지 못했습니다.");
            return false;
        }

        boolean abilityOk = EgoWeaponDatabase.setAbility(weapon, ability);
        boolean toneOk = EgoWeaponDatabase.setTone(weapon, tone);
        if (!abilityOk || !toneOk) {
            EgoMessageUtil.danger(pc, "에고 변경에 실패했습니다. DB 상태를 확인하세요.");
            return false;
        }

        inv.count(orb, orb.getCount() - 1, true);
        EgoDB.reload(null);
        EgoView.refreshInventory(pc, weapon);
        EgoMessageUtil.info(pc, String.format("에고가 새롭게 반응합니다. 능력: %s / 대화: %s", ability, tone));
        return true;
    }

    public static String randomAbility(ItemInstance weapon) {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < ABILITIES.length; i++) {
            String ability = ABILITIES[i];
            if (EgoWeaponTypeUtil.isAbilityAllowed(ability, weapon))
                list.add(ability);
        }
        if (list.isEmpty()) {
            String def = EgoWeaponTypeUtil.getDefaultAbilityType(weapon);
            if (def != null && def.length() > 0)
                return def;
            return "";
        }
        return list.get(random.nextInt(list.size()));
    }

    public static String randomTone() {
        return TONES[random.nextInt(TONES.length)];
    }
}
