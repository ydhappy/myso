package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;
import lineage.share.Lineage;

/**
 * 에고 구슬 사용 처리.
 *
 * 최종 정책:
 * - 명령어로 에고 생성/삭제/변경하지 않는다.
 * - 에고 구슬 사용 시 착용 무기에 에고가 없으면 최초 생성한다.
 * - 이미 에고무기라면 능력/대화/레벨은 변경하지 않고 현재 캐릭터를 주인으로 재인식한다.
 */
public final class EgoOrbController {

    public static final int DEFAULT_ITEM_CODE = 900001;
    public static final String ITEM_NAME = "에고 구슬";

    private static final long USE_LOCK_MS = 1000L;
    private static final Random random = new Random();
    private static final Map<Long, Long> useLockMap = new ConcurrentHashMap<Long, Long>();
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

    private EgoOrbController() {
    }

    public static boolean use(PcInstance pc, ItemInstance orb) {
        if (pc == null)
            return false;

        long pcId = pc.getObjectId();
        if (!acquireUseLock(pcId)) {
            EgoMessageUtil.danger(pc, "에고 구슬은 잠시 후 다시 사용할 수 있습니다.");
            return false;
        }

        try {
            return useInternal(pc, orb);
        } finally {
            releaseUseLockLater(pcId);
        }
    }

    private static boolean useInternal(PcInstance pc, ItemInstance orb) {
        Inventory inv = pc.getInventory();
        if (inv == null) {
            EgoMessageUtil.danger(pc, "인벤토리 정보를 찾을 수 없습니다.");
            return false;
        }

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null) {
            EgoMessageUtil.danger(pc, "에고 구슬은 무기를 착용한 상태에서만 사용할 수 있습니다.");
            return false;
        }
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            EgoMessageUtil.danger(pc, "현재 착용 무기는 에고 생성 대상이 아닙니다.");
            return false;
        }
        if (orb == null || orb.getCount() <= 0) {
            EgoMessageUtil.danger(pc, "에고 구슬이 없습니다.");
            return false;
        }

        if (EgoWeaponDatabase.isEgoWeapon(weapon))
            return recognizeOwner(pc, weapon);

        return createEgo(pc, inv, weapon, orb);
    }

    private static boolean createEgo(PcInstance pc, Inventory inv, ItemInstance weapon, ItemInstance orb) {
        String egoName = defaultEgoName(pc, weapon);
        String tone = randomTone();
        String ability = randomAbility(weapon);
        if (ability.length() == 0)
            ability = EgoWeaponTypeUtil.getDefaultAbilityType(weapon);
        if (ability == null || ability.length() == 0) {
            EgoMessageUtil.danger(pc, "현재 무기에 적용 가능한 에고 능력을 찾지 못했습니다.");
            return false;
        }

        boolean created = EgoWeaponDatabase.enableEgo(pc, weapon, egoName, tone);
        if (!created) {
            EgoMessageUtil.danger(pc, "에고 생성에 실패했습니다. DB 상태를 확인하세요.");
            return false;
        }

        boolean abilityOk = EgoWeaponDatabase.setAbility(weapon, ability);
        if (!abilityOk) {
            EgoWeaponDatabase.disableEgo(weapon);
            EgoMessageUtil.danger(pc, "에고 능력 부여에 실패하여 생성을 취소했습니다.");
            return false;
        }

        try {
            inv.count(orb, orb.getCount() - 1, true);
        } catch (Throwable e) {
            EgoWeaponDatabase.disableEgo(weapon);
            EgoMessageUtil.danger(pc, "에고 구슬 소모에 실패하여 생성을 취소했습니다.");
            return false;
        }

        EgoView.refreshInventory(pc, weapon);
        EgoMessageUtil.info(pc, String.format("%s 에고가 깨어났습니다. 이름: %s", EgoView.displayName(weapon), egoName));
        EgoMessageUtil.info(pc, String.format("능력: %s / 대화: %s", ability, tone));
        return true;
    }

    private static boolean recognizeOwner(PcInstance pc, ItemInstance weapon) {
        boolean ok = EgoWeaponDatabase.recognizeOwner(pc, weapon);
        if (!ok) {
            EgoMessageUtil.danger(pc, "에고무기 주인 재인식에 실패했습니다.");
            return false;
        }
        EgoView.refreshInventory(pc, weapon);
        EgoMessageUtil.info(pc, String.format("%s 이 현재 캐릭터를 주인으로 다시 인식했습니다.", EgoWeaponDatabase.getEgoName(weapon, "에고")));
        EgoMessageUtil.info(pc, "능력, 대화 성향, 레벨은 변경되지 않습니다.");
        return true;
    }

    private static String defaultEgoName(PcInstance pc, ItemInstance weapon) {
        String base = "에고";
        try {
            String weaponName = EgoView.displayName(weapon);
            if (weaponName != null && weaponName.trim().length() > 0)
                base = weaponName.trim();
        } catch (Throwable e) {
        }
        if (base.length() > 20)
            base = base.substring(0, 20);
        return base;
    }

    private static boolean acquireUseLock(long pcId) {
        long now = java.lang.System.currentTimeMillis();
        Long key = Long.valueOf(pcId);
        Long last = useLockMap.get(key);
        if (last != null && now - last.longValue() < USE_LOCK_MS)
            return false;
        useLockMap.put(key, Long.valueOf(now));
        return true;
    }

    private static void releaseUseLockLater(long pcId) {
        useLockMap.put(Long.valueOf(pcId), Long.valueOf(java.lang.System.currentTimeMillis()));
    }

    public static String randomAbility(ItemInstance weapon) {
        List<String> list = allowedAbilities(weapon);
        if (list.isEmpty()) {
            String def = EgoWeaponTypeUtil.getDefaultAbilityType(weapon);
            if (def != null && def.length() > 0)
                return def;
            return "";
        }
        return list.get(random.nextInt(list.size()));
    }

    private static List<String> allowedAbilities(ItemInstance weapon) {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < ABILITIES.length; i++) {
            String ability = ABILITIES[i];
            if (EgoWeaponTypeUtil.isAbilityAllowed(ability, weapon))
                list.add(ability);
        }
        return list;
    }

    public static String randomTone() {
        return TONES[random.nextInt(TONES.length)];
    }
}
