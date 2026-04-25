package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.network.packet.BasePacketPooling;
import lineage.network.packet.server.S_ObjectEffect;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.object.Character;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 특별 능력 컨트롤러.
 *
 * 기존 서버 전투 코어는 변경하지 않는다.
 * DamageController의 기존 계산이 끝난 뒤 보조능력만 추가한다.
 * 에고 여부/레벨/경험치는 DB 기준으로만 처리한다.
 */
public final class EgoWeaponAbilityController {

    private static final int MAX_EGO_LEVEL = 30;
    private static final int BASE_PROC_CHANCE = 3;
    private static final int ADD_PROC_CHANCE_PER_LEVEL = 1;
    private static final int MAX_PROC_CHANCE = 25;
    private static final int AREA_RANGE = 2;
    private static final int AREA_MAX_TARGET = 4;

    private static final long ATTACK_EGO_EXP = 1L;
    private static final long ATTACK_EXP_DELAY_MS = 3000L;
    private static final long DEFAULT_KILL_EGO_EXP = 5L;
    private static final long BOSS_KILL_EGO_EXP = 50L;

    /** 능력 발동 메시지 최소 간격. 전투 중 채팅창 도배 방지. */
    private static final long PROC_MESSAGE_DELAY_MS = 1500L;

    /** objectId + abilityType 기준 마지막 메시지 시간. */
    private static final Map<String, Long> procMessageDelayMap = new ConcurrentHashMap<String, Long>();

    /** 캐릭터 objectId + 무기 objectId 기준 경험치 지급 딜레이. */
    private static final Map<String, Long> expDelayMap = new ConcurrentHashMap<String, Long>();

    private EgoWeaponAbilityController() {
    }

    public static int applyAttackAbility(Character cha, object target, ItemInstance weapon, int damage) {
        if (!(cha instanceof PcInstance))
            return damage;

        if (damage <= 0)
            return damage;

        if (target == null || target.isDead())
            return damage;

        if (weapon == null || !isEgoWeapon(weapon))
            return damage;

        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
            return damage;

        PcInstance pc = (PcInstance) cha;
        gainAttackExp(pc, weapon);

        int egoLevel = getEgoLevel(weapon);
        int chance = getProcChance(egoLevel);

        if (Util.random(1, 100) > chance)
            return damage;

        EgoAbilityType type = getAbilityType(weapon);
        if (!EgoWeaponTypeUtil.isAbilityAllowed(type.name(), weapon))
            type = EgoAbilityType.EGO_BALANCE;

        switch (type) {
            case BLOOD_DRAIN:
                return applyBloodDrain(pc, target, weapon, damage, egoLevel);
            case MANA_DRAIN:
                return applyManaDrain(pc, target, weapon, damage, egoLevel);
            case CRITICAL_BURST:
                return applyCriticalBurst(pc, target, weapon, damage, egoLevel);
            case GUARDIAN_SHIELD:
                return applyGuardianShield(pc, target, weapon, damage, egoLevel);
            case AREA_SLASH:
                return applyAreaSlash(pc, target, weapon, damage, egoLevel);
            case EXECUTION:
                return applyExecution(pc, target, weapon, damage, egoLevel);
            case FLAME_BRAND:
                return applyFlameBrand(pc, target, weapon, damage, egoLevel);
            case FROST_BIND:
                return applyFrostBind(pc, target, weapon, damage, egoLevel);
            case EGO_BALANCE:
            default:
                return applyBalanced(pc, target, weapon, damage, egoLevel);
        }
    }

    public static void addKillExp(PcInstance pc, MonsterInstance mon) {
        if (pc == null || mon == null)
            return;

        Inventory inv = pc.getInventory();
        if (inv == null)
            return;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !isEgoWeapon(weapon))
            return;

        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
            return;

        long addExp = DEFAULT_KILL_EGO_EXP;
        if (mon.getMonster() != null && mon.getMonster().isBoss())
            addExp += BOSS_KILL_EGO_EXP;

        addExp(pc, weapon, addExp, true);
    }

    public static boolean isEgoWeapon(ItemInstance weapon) {
        if (weapon == null)
            return false;

        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
            return false;

        return EgoWeaponDatabase.isEgoWeapon(weapon);
    }

    public static int getEgoLevel(ItemInstance weapon) {
        return EgoWeaponDatabase.getEgoLevel(weapon, 1);
    }

    public static EgoAbilityType getAbilityType(ItemInstance weapon) {
        if (weapon == null || weapon.getItem() == null)
            return EgoAbilityType.EGO_BALANCE;

        try {
            if (EgoWeaponDatabase.getFirstAbility(weapon) != null) {
                String ability = EgoWeaponDatabase.getFirstAbility(weapon).abilityType;
                if (ability != null && ability.trim().length() > 0)
                    return EgoAbilityType.valueOf(ability.trim().toUpperCase());
            }
        } catch (Exception e) {
            return EgoAbilityType.EGO_BALANCE;
        }

        String ability = EgoWeaponTypeUtil.getDefaultAbilityType(weapon);
        try {
            return EgoAbilityType.valueOf(ability);
        } catch (Exception e) {
            return EgoAbilityType.EGO_BALANCE;
        }
    }

    private static void gainAttackExp(PcInstance pc, ItemInstance weapon) {
        if (pc == null || weapon == null)
            return;

        long now = java.lang.System.currentTimeMillis();
        String key = pc.getObjectId() + ":" + weapon.getObjectId();
        Long last = expDelayMap.get(key);
        if (last != null && now - last.longValue() < ATTACK_EXP_DELAY_MS)
            return;

        expDelayMap.put(key, now);
        addExp(pc, weapon, ATTACK_EGO_EXP, false);
    }

    private static void addExp(PcInstance pc, ItemInstance weapon, long addExp, boolean forceMessage) {
        if (pc == null || weapon == null || addExp <= 0)
            return;

        EgoWeaponInfo before = EgoWeaponDatabase.find(weapon);
        int beforeLevel = before == null ? 1 : before.level;
        boolean levelUp = EgoWeaponDatabase.addExp(weapon, addExp);
        EgoWeaponInfo after = EgoWeaponDatabase.find(weapon);
        int afterLevel = after == null ? beforeLevel : after.level;

        if (levelUp || afterLevel > beforeLevel) {
            sendEffect(pc, 3944);
            say(pc, "LEVEL_UP", String.format("\\fY[에고] 의식이 성장했습니다. Lv.%d", afterLevel));
            EgoView.refreshInventory(pc, weapon);
            return;
        }

        if (forceMessage || Util.random(1, 100) <= 3) {
            EgoWeaponInfo info = EgoWeaponDatabase.find(weapon);
            if (info != null)
                say(pc, "EXP", String.format("\\fS[에고] 경험치 +%d (%d/%d)", addExp, info.exp, info.maxExp));
        }
    }

    private static int getProcChance(int egoLevel) {
        int chance = BASE_PROC_CHANCE + Math.max(0, egoLevel - 1) * ADD_PROC_CHANCE_PER_LEVEL;
        return Math.min(MAX_PROC_CHANCE, Math.max(1, chance));
    }

    private static int applyBloodDrain(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int heal = Math.max(1, damage * (3 + egoLevel / 3) / 100);
        pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + heal));
        sendEffect(target, 8150);
        say(pc, "BLOOD_DRAIN", String.format("\\fR[에고] 생명 흡수 발동. HP +%d", heal));
        return damage + Math.max(1, egoLevel / 2);
    }

    private static int applyManaDrain(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int mp = Math.max(1, 1 + egoLevel / 4);
        pc.setNowMp(Math.min(pc.getTotalMp(), pc.getNowMp() + mp));
        sendEffect(target, 7300);
        say(pc, "MANA_DRAIN", String.format("\\fY[에고] 정신 흡수 발동. MP +%d", mp));
        return damage;
    }

    private static int applyCriticalBurst(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(2, egoLevel);
        sendEffect(target, 12487);
        say(pc, "CRITICAL_BURST", String.format("\\fY[에고] 치명 폭발 발동. 추가 피해 +%d", add));
        return damage + add;
    }

    private static int applyGuardianShield(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int hpRate = pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp());
        if (hpRate <= 40) {
            int heal = Math.max(5, egoLevel * 2);
            pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + heal));
            sendEffect(pc, 6321);
            say(pc, "GUARDIAN_SHIELD", String.format("\\fY[에고] 수호 의지 발동. HP +%d", heal));
        }
        return damage + Math.max(1, egoLevel / 3);
    }

    private static int applyAreaSlash(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int splashDamage = Math.max(1, damage * (10 + egoLevel) / 100);
        int count = 0;

        for (MonsterInstance mon : findNearbyMonsters(pc, target, AREA_RANGE)) {
            if (count >= AREA_MAX_TARGET)
                break;
            if (mon.getObjectId() == target.getObjectId())
                continue;

            safeSplashDamage(pc, mon, splashDamage);
            sendEffect(mon, 12248);
            count++;
        }

        if (count > 0)
            say(pc, "AREA_SLASH", String.format("\\fY[에고] 공명 베기 발동. 주변 %d명에게 피해", count));

        return damage;
    }

    private static int applyExecution(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        if (!(target instanceof Character))
            return damage;

        Character t = (Character) target;
        int hpRate = t.getNowHp() * 100 / Math.max(1, t.getTotalHp());
        if (hpRate <= 20) {
            int add = Math.max(3, egoLevel * 2);
            sendEffect(target, 8683);
            say(pc, "EXECUTION", String.format("\\fR[에고] 처형 발동. 약화된 적에게 추가 피해 +%d", add));
            return damage + add;
        }

        return damage;
    }

    private static int applyFlameBrand(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(2, 3 + egoLevel);
        sendEffect(target, 1811);
        say(pc, "FLAME_BRAND", String.format("\\fR[에고] 화염 각인 발동. 추가 피해 +%d", add));
        return damage + add;
    }

    private static int applyFrostBind(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(1, egoLevel / 2);
        sendEffect(target, 3684);
        say(pc, "FROST_BIND", "\\fY[에고] 서리 충격 발동. 적의 움직임을 흔듭니다.");
        return damage + add;
    }

    private static int applyBalanced(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(1, egoLevel / 2);
        sendEffect(target, 3940);
        say(pc, "EGO_BALANCE", String.format("\\fY[에고] 공명 타격 발동. 추가 피해 +%d", add));
        return damage + add;
    }

    private static void safeSplashDamage(PcInstance pc, MonsterInstance mon, int splashDamage) {
        if (pc == null || mon == null || splashDamage <= 0)
            return;
        if (mon.isDead())
            return;
        try {
            mon.toDamage(pc, splashDamage, Lineage.ATTACK_TYPE_WEAPON);
        } catch (Throwable e) {
            // 서버 버전별 toDamage 시그니처/상태 차이 대비.
        }
    }

    private static List<MonsterInstance> findNearbyMonsters(PcInstance pc, object center, int range) {
        List<MonsterInstance> list = new ArrayList<MonsterInstance>();
        if (pc == null || center == null)
            return list;

        List<object> inside = pc.getInsideList();
        if (inside == null)
            return list;

        for (object o : inside) {
            if (!(o instanceof MonsterInstance))
                continue;

            MonsterInstance mon = (MonsterInstance) o;
            if (mon.isDead())
                continue;

            if (mon.getMap() != pc.getMap())
                continue;

            if (!Util.isDistance(center, mon, range))
                continue;

            list.add(mon);
        }

        return list;
    }

    private static void sendEffect(object o, int effect) {
        if (o == null || effect <= 0)
            return;

        try {
            o.toSender(S_ObjectEffect.clone(BasePacketPooling.getPool(S_ObjectEffect.class), o, effect), true);
        } catch (Throwable e) {
            // 서버/클라 버전에 따라 이펙트가 없을 수 있으므로 무시.
        }
    }

    private static void say(PcInstance pc, String abilityKey, String msg) {
        if (pc == null || msg == null)
            return;

        long now = java.lang.System.currentTimeMillis();
        String key = pc.getObjectId() + ":" + abilityKey;
        Long last = procMessageDelayMap.get(key);
        if (last != null && now - last.longValue() < PROC_MESSAGE_DELAY_MS)
            return;

        procMessageDelayMap.put(key, now);
        ChattingController.toChatting(pc, msg, Lineage.CHATTING_MODE_MESSAGE);
    }

    public enum EgoAbilityType {
        EGO_BALANCE,
        BLOOD_DRAIN,
        MANA_DRAIN,
        CRITICAL_BURST,
        GUARDIAN_SHIELD,
        AREA_SLASH,
        EXECUTION,
        FLAME_BRAND,
        FROST_BIND
    }
}
