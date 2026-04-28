package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;
import lineage.share.Lineage;

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

    private EgoChangeOrbController() {
    }

    public static boolean use(PcInstance pc, ItemInstance orb) {
        if (pc == null)
            return false;

        long pcId = pc.getObjectId();
        if (!acquireUseLock(pcId)) {
            EgoMessageUtil.danger(pc, "에고 변경구슬은 잠시 후 다시 사용할 수 있습니다.");
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

        String oldAbility = currentAbility(weapon);
        String oldTone = EgoWeaponDatabase.getTone(weapon);
        String ability = randomDifferentAbility(weapon, oldAbility);
        String tone = randomDifferentTone(oldTone);

        if (ability.length() == 0) {
            EgoMessageUtil.danger(pc, "현재 무기에 적용 가능한 에고 능력을 찾지 못했습니다.");
            return false;
        }

        boolean abilityOk = false;
        boolean toneOk = false;
        try {
            abilityOk = EgoWeaponDatabase.setAbility(weapon, ability);
            if (!abilityOk)
                throw new RuntimeException("ability update failed");

            toneOk = EgoWeaponDatabase.setTone(weapon, tone);
            if (!toneOk)
                throw new RuntimeException("tone update failed");
        } catch (Throwable e) {
            rollbackChange(weapon, oldAbility, oldTone);
            EgoMessageUtil.danger(pc, "에고 변경에 실패했습니다. 변경값을 복구했습니다.");
            return false;
        }

        try {
            inv.count(orb, orb.getCount() - 1, true);
        } catch (Throwable e) {
            rollbackChange(weapon, oldAbility, oldTone);
            EgoMessageUtil.danger(pc, "에고 변경구슬 소모에 실패하여 변경값을 복구했습니다.");
            return false;
        }

        EgoView.refreshInventory(pc, weapon);
        EgoMessageUtil.info(pc, String.format("에고가 새롭게 반응합니다. 능력: %s / 대화: %s", ability, tone));
        return true;
    }

    private static boolean acquireUseLock(long pcId) {
        long now = java.lang.System.currentTimeMillis();
        Long last = useLockMap.get(Long.valueOf(pcId));
        if (last != null && now - last.longValue() < USE_LOCK_MS)
            return false;
        useLockMap.put(Long.valueOf(pcId), Long.valueOf(now));
        return true;
    }

    private static void releaseUseLockLater(long pcId) {
        long now = java.lang.System.currentTimeMillis();
        useLockMap.put(Long.valueOf(pcId), Long.valueOf(now));
    }

    private static void rollbackChange(ItemInstance weapon, String oldAbility, String oldTone) {
        try {
            if (oldAbility != null && oldAbility.length() > 0)
                EgoWeaponDatabase.setAbility(weapon, oldAbility);
            if (oldTone != null && oldTone.length() > 0)
                EgoWeaponDatabase.setTone(weapon, oldTone);
        } catch (Throwable ignore) {
        }
    }

    private static String currentAbility(ItemInstance weapon) {
        EgoAbilityInfo info = EgoWeaponDatabase.getFirstAbility(weapon);
        if (info == null || info.abilityType == null || info.abilityType.trim().length() == 0)
            return EgoWeaponTypeUtil.getDefaultAbilityType(weapon);
        return info.abilityType.trim().toUpperCase();
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

    private static String randomDifferentAbility(ItemInstance weapon, String oldAbility) {
        List<String> list = allowedAbilities(weapon);
        if (list.isEmpty())
            return randomAbility(weapon);
        if (list.size() <= 1)
            return list.get(0);

        String old = oldAbility == null ? "" : oldAbility.trim().toUpperCase();
        List<String> candidates = new ArrayList<String>();
        for (String ability : list) {
            if (!ability.equals(old))
                candidates.add(ability);
        }
        if (candidates.isEmpty())
            candidates = list;
        return candidates.get(random.nextInt(candidates.size()));
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

    private static String randomDifferentTone(String oldTone) {
        if (TONES.length <= 1)
            return randomTone();
        String old = EgoWeaponDatabase.normalizeTone(oldTone);
        for (int i = 0; i < 5; i++) {
            String next = randomTone();
            if (!next.equals(old))
                return next;
        }
        return "예의".equals(old) ? "예의반대" : "예의";
    }
}
