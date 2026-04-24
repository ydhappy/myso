package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

import lineage.bean.lineage.Inventory;
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
 * 적용 위치:
 * - 이 파일을 bitna/src/lineage/world/controller/EgoWeaponAbilityController.java 로 복사한다.
 * - DamageController 또는 ItemWeaponInstance의 데미지 계산 구간에서 applyAttackAbility(...)를 호출한다.
 * - MonsterInstance 사망/경험치 지급 구간에서 addKillExp(...)를 호출하면 에고 성장까지 가능하다.
 *
 * 1차 안전 정책:
 * - 무기 착용 상태에서만 발동한다.
 * - 확률 발동이며, 과도한 자동제어는 하지 않는다.
 * - HP/MP 회복은 최대치 초과를 방지한다.
 * - 광역 피해는 주변 몬스터에만 제한 적용한다.
 */
public final class EgoWeaponAbilityController {

    /**
     * 1차 기본 활성화 여부.
     * DB/ItemInstance 에고 필드 연결 전까지는 true로 테스트 가능.
     * 실제 운영에서는 isEgoWeapon(weapon) 조건을 DB 기반으로 바꾸는 것을 권장한다.
     */
    private static final boolean ENABLE_TEST_MODE = true;

    /** 에고 최대 레벨 */
    private static final int MAX_EGO_LEVEL = 30;

    /** 기본 발동 확률 */
    private static final int BASE_PROC_CHANCE = 3;

    /** 레벨당 추가 발동 확률 */
    private static final int ADD_PROC_CHANCE_PER_LEVEL = 1;

    /** 발동 확률 상한 */
    private static final int MAX_PROC_CHANCE = 25;

    /** 광역 공격 범위 */
    private static final int AREA_RANGE = 2;

    /** 광역 공격 최대 대상 수 */
    private static final int AREA_MAX_TARGET = 4;

    /** 킬 경험치 기본값. DB 미연동 1차 테스트용 */
    private static final long DEFAULT_KILL_EGO_EXP = 1L;

    private EgoWeaponAbilityController() {
    }

    /**
     * 물리 공격 데미지 계산 후 추가 능력을 적용한다.
     *
     * 권장 호출 위치:
     * - DamageController.getDamage(...)에서 최종 dmg 반환 직전
     *
     * @param cha 공격자
     * @param target 대상
     * @param weapon 착용 무기
     * @param damage 현재 계산된 데미지
     * @return 특별 능력이 반영된 데미지
     */
    public static int applyAttackAbility(Character cha, object target, ItemInstance weapon, int damage) {
        if (!(cha instanceof PcInstance))
            return damage;

        if (target == null || target.isDead())
            return damage;

        if (weapon == null || !isEgoWeapon(weapon))
            return damage;

        PcInstance pc = (PcInstance) cha;
        int egoLevel = getEgoLevel(weapon);
        int chance = getProcChance(egoLevel);

        if (Util.random(1, 100) > chance)
            return damage;

        EgoAbilityType type = getAbilityType(weapon);

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

    /**
     * 몬스터 처치 시 에고 경험치를 지급하는 예비 메서드.
     * 1차 파일에서는 DB 저장 없이 멘트/설계용으로 제공한다.
     */
    public static void addKillExp(PcInstance pc, MonsterInstance mon) {
        if (pc == null || mon == null)
            return;

        Inventory inv = pc.getInventory();
        if (inv == null)
            return;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !isEgoWeapon(weapon))
            return;

        long addExp = DEFAULT_KILL_EGO_EXP;
        if (mon.getMonster() != null && mon.getMonster().isBoss())
            addExp += 10;

        // DB/ItemInstance 연동 후 이 부분을 실제 저장 로직으로 교체한다.
        // EgoWeaponDatabase.addExp(weapon, pc, addExp);

        if (Util.random(1, 100) <= 3) {
            ChattingController.toChatting(pc, "\\fY[에고] 적의 기운을 흡수했습니다. 내 의식이 조금 선명해졌습니다.", Lineage.CHATTING_MODE_MESSAGE);
        }
    }

    /**
     * 현재 무기가 에고무기인지 판단한다.
     *
     * 1차 테스트:
     * - ENABLE_TEST_MODE=true면 모든 착용 무기를 에고무기로 취급한다.
     *
     * 운영 추천:
     * - ItemInstance.isEgoEnabled()
     * - character_item_ego.ego_enabled
     * - 특정 아이템 이름/아이디
     * 중 하나로 교체한다.
     */
    public static boolean isEgoWeapon(ItemInstance weapon) {
        if (weapon == null)
            return false;

        if (ENABLE_TEST_MODE)
            return true;

        // 2차 확장 예시:
        // return weapon.isEgoEnabled();

        return false;
    }

    /**
     * 에고 레벨 조회.
     * 1차는 인챈트 레벨 기반 임시 계산.
     * 2차에서 weapon.getEgoLevel() 또는 DB값으로 교체한다.
     */
    public static int getEgoLevel(ItemInstance weapon) {
        if (weapon == null)
            return 1;

        int level = 1 + Math.max(0, weapon.getEnLevel());
        if (level > MAX_EGO_LEVEL)
            level = MAX_EGO_LEVEL;
        return level;
    }

    /**
     * 에고 능력 타입 조회.
     * 1차는 무기 이름/타입 기반으로 자동 분류한다.
     * 2차에서 DB character_item_ego.ego_ability_type으로 교체한다.
     */
    public static EgoAbilityType getAbilityType(ItemInstance weapon) {
        if (weapon == null || weapon.getItem() == null)
            return EgoAbilityType.EGO_BALANCE;

        String name = weapon.getName() == null ? "" : weapon.getName();
        String type2 = weapon.getItem().getType2() == null ? "" : weapon.getItem().getType2();

        if (name.contains("피") || name.toLowerCase().contains("blood"))
            return EgoAbilityType.BLOOD_DRAIN;

        if (name.contains("마나") || name.contains("지식") || type2.equalsIgnoreCase("staff") || type2.equalsIgnoreCase("wand"))
            return EgoAbilityType.MANA_DRAIN;

        if (name.contains("화염") || name.contains("불") || name.toLowerCase().contains("flame"))
            return EgoAbilityType.FLAME_BRAND;

        if (name.contains("얼음") || name.contains("서리") || name.toLowerCase().contains("frost"))
            return EgoAbilityType.FROST_BIND;

        if (type2.equalsIgnoreCase("tohandsword") || type2.equalsIgnoreCase("axe"))
            return EgoAbilityType.CRITICAL_BURST;

        if (type2.equalsIgnoreCase("spear"))
            return EgoAbilityType.AREA_SLASH;

        if (name.contains("수호") || name.contains("가디언"))
            return EgoAbilityType.GUARDIAN_SHIELD;

        return EgoAbilityType.EGO_BALANCE;
    }

    private static int getProcChance(int egoLevel) {
        int chance = BASE_PROC_CHANCE + Math.max(0, egoLevel - 1) * ADD_PROC_CHANCE_PER_LEVEL;
        return Math.min(MAX_PROC_CHANCE, chance);
    }

    private static int applyBloodDrain(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int heal = Math.max(1, damage * (3 + egoLevel / 3) / 100);
        pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + heal));
        sendEffect(target, 8150);
        ChattingController.toChatting(pc, String.format("\\fR[에고] 생명 흡수 발동. HP +%d", heal), Lineage.CHATTING_MODE_MESSAGE);
        return damage + Math.max(1, egoLevel / 2);
    }

    private static int applyManaDrain(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int mp = Math.max(1, 1 + egoLevel / 4);
        pc.setNowMp(Math.min(pc.getTotalMp(), pc.getNowMp() + mp));
        sendEffect(target, 7300);
        ChattingController.toChatting(pc, String.format("\\fY[에고] 정신 흡수 발동. MP +%d", mp), Lineage.CHATTING_MODE_MESSAGE);
        return damage;
    }

    private static int applyCriticalBurst(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(2, egoLevel + weapon.getEnLevel());
        sendEffect(target, 12487);
        ChattingController.toChatting(pc, String.format("\\fY[에고] 치명 폭발 발동. 추가 피해 +%d", add), Lineage.CHATTING_MODE_MESSAGE);
        return damage + add;
    }

    private static int applyGuardianShield(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int hpRate = pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp());
        if (hpRate <= 40) {
            int heal = Math.max(5, egoLevel * 2);
            pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + heal));
            sendEffect(pc, 6321);
            ChattingController.toChatting(pc, String.format("\\fY[에고] 수호 의지 발동. HP +%d", heal), Lineage.CHATTING_MODE_MESSAGE);
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

            mon.toDamage(pc, splashDamage, Lineage.ATTACK_TYPE_WEAPON);
            sendEffect(mon, 12248);
            count++;
        }

        if (count > 0) {
            ChattingController.toChatting(pc, String.format("\\fY[에고] 공명 베기 발동. 주변 %d명에게 피해", count), Lineage.CHATTING_MODE_MESSAGE);
        }

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
            ChattingController.toChatting(pc, String.format("\\fR[에고] 처형 발동. 약화된 적에게 추가 피해 +%d", add), Lineage.CHATTING_MODE_MESSAGE);
            return damage + add;
        }

        return damage;
    }

    private static int applyFlameBrand(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(2, 3 + egoLevel);
        sendEffect(target, 1811);
        ChattingController.toChatting(pc, String.format("\\fR[에고] 화염 각인 발동. 추가 피해 +%d", add), Lineage.CHATTING_MODE_MESSAGE);
        return damage + add;
    }

    private static int applyFrostBind(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(1, egoLevel / 2);
        sendEffect(target, 3684);
        ChattingController.toChatting(pc, "\\fY[에고] 서리 충격 발동. 적의 움직임을 흔듭니다.", Lineage.CHATTING_MODE_MESSAGE);
        return damage + add;
    }

    private static int applyBalanced(PcInstance pc, object target, ItemInstance weapon, int damage, int egoLevel) {
        int add = Math.max(1, egoLevel / 2);
        sendEffect(target, 3940);
        ChattingController.toChatting(pc, String.format("\\fY[에고] 공명 타격 발동. 추가 피해 +%d", add), Lineage.CHATTING_MODE_MESSAGE);
        return damage + add;
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
            // 서버 버전에 따라 이펙트 패킷 시그니처가 다를 수 있으므로 안전하게 무시한다.
        }
    }

    public enum EgoAbilityType {
        /** 균형형: 소량 추가 피해 */
        EGO_BALANCE,

        /** 생명 흡수: 피해 일부 HP 회복 */
        BLOOD_DRAIN,

        /** 정신 흡수: MP 회복 */
        MANA_DRAIN,

        /** 치명 폭발: 높은 추가 피해 */
        CRITICAL_BURST,

        /** 수호 의지: HP 낮을 때 회복 */
        GUARDIAN_SHIELD,

        /** 공명 베기: 주변 몬스터에게 소량 피해 */
        AREA_SLASH,

        /** 처형: HP 낮은 적에게 추가 피해 */
        EXECUTION,

        /** 화염 각인: 고정 추가 피해 */
        FLAME_BRAND,

        /** 서리 충격: 소량 피해 + 제어형 연출 */
        FROST_BIND
    }
}
