package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.database.Skill;
import lineage.bean.lineage.Inventory;
import lineage.database.DatabaseConnection;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.database.SkillDatabase;
import lineage.network.packet.BasePacketPooling;
import lineage.network.packet.server.S_ObjectEffect;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.controller.BuffController;
import lineage.world.object.Character;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcInstance;
import lineage.world.object.magic.ShockStun;

/**
 * 에고무기 특별 능력 컨트롤러.
 *
 * 최종 전투 정책:
 * - 에고 최대레벨 10 고정.
 * - Lv.0은 스킬/치명/반격/스턴이 전혀 발동하지 않는다.
 * - Lv.1부터 공격 보조능력과 치명 보정이 레벨별로 동작한다.
 * - Lv.5부터 피격 시 확률 반격, 반격 공격성공/공격력/치명타 보정이 동작한다. PC 대상 포함.
 * - Lv.6부터 피격 시 자동반격이 동작한다. PC 대상 포함.
 * - Lv.10은 에고 스턴을 50% 성공률로 시도한다.
 */
public final class EgoWeaponAbilityController {

    private static final int MAX_EGO_LEVEL = 10;
    private static final int AREA_RANGE = 2;
    private static final int AREA_MAX_TARGET = 4;

    private static final long ATTACK_EGO_EXP = 1L;
    private static final long ATTACK_EXP_DELAY_MS = 3000L;
    private static final long DEFAULT_KILL_EGO_EXP = 5L;
    private static final long BOSS_KILL_EGO_EXP = 50L;
    private static final long PROC_MESSAGE_DELAY_MS = 1500L;

    private static final int[] LEVEL_PROC_BONUS = {0, 0, 1, 2, 3, 4, 6, 8, 10, 12, 15};
    private static final int[] LEVEL_CRITICAL_CHANCE = {0, 1, 2, 3, 4, 6, 9, 12, 15, 18, 25};
    private static final int[] LEVEL_CRITICAL_DAMAGE = {0, 1, 2, 3, 4, 6, 8, 10, 12, 15, 20};
    private static final int[] LEVEL_COUNTER_CHANCE = {0, 0, 0, 0, 0, 35, 100, 100, 100, 100, 100};
    private static final int[] LEVEL_COUNTER_POWER = {0, 0, 0, 0, 0, 18, 24, 30, 38, 46, 60};
    private static final int[] LEVEL_COUNTER_CRITICAL = {0, 0, 0, 0, 0, 8, 12, 16, 20, 25, 35};

    private static final int STUN_LEVEL = 10;
    private static final int STUN_SUCCESS_RATE = 50;
    private static final int STUN_EFFECT = 4183;
    private static final int STUN_TIME = 2;
    private static final int AUTO_COUNTER_COOL_MS = 2500;
    private static final int STUN_COOL_MS = 6000;

    private static final Map<String, Long> procMessageDelayMap = new ConcurrentHashMap<String, Long>();
    private static final Map<String, Long> expDelayMap = new ConcurrentHashMap<String, Long>();
    private static final Map<String, Long> procCoolMap = new ConcurrentHashMap<String, Long>();
    private static final Map<String, SkillBaseInfo> skillBaseMap = new ConcurrentHashMap<String, SkillBaseInfo>();
    private static final ThreadLocal<Boolean> DEFENSE_RECURSION_GUARD = new ThreadLocal<Boolean>();

    private EgoWeaponAbilityController() {
    }

    public static void reloadConfig() {
        skillBaseMap.clear();
        loadSkillBase();
    }

    public static int applyAttackAbility(Character cha, object target, ItemInstance weapon, int damage) {
        if (!(cha instanceof PcInstance))
            return damage;
        if (damage <= 0 || target == null || target.isDead())
            return damage;
        if (weapon == null || !isEgoWeapon(weapon))
            return damage;
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
            return damage;

        int egoLevel = getEgoLevel(weapon);
        if (egoLevel <= 0)
            return damage;

        PcInstance pc = (PcInstance) cha;
        gainAttackExp(pc, weapon);

        EgoAbilityInfo abilityInfo = EgoWeaponDatabase.getFirstAbility(weapon);
        EgoAbilityType type = getAbilityType(weapon);
        if (!EgoWeaponTypeUtil.isAbilityAllowed(type.name(), weapon))
            type = EgoAbilityType.EGO_BALANCE;

        SkillBaseInfo base = getSkillBase(type.name());
        int abilityLevel = abilityInfo == null ? 1 : Math.max(1, abilityInfo.abilityLevel);
        int effectiveLevel = clampLevel(egoLevel + Math.max(0, abilityLevel - 1));

        if (effectiveLevel <= 0 || effectiveLevel < Math.max(1, base.minLevel))
            return damage;
        if (!checkCooldown(weapon, type.name(), base.coolMs))
            return damage;

        int chance = getProcChance(effectiveLevel, abilityInfo, base, type);
        if (Util.random(1, 100) > chance)
            return damage;

        markProc(weapon, type.name());

        int result = applyAttackByType(pc, target, damage, effectiveLevel, base.effect, type);
        int bonusDamage = abilityInfo == null ? 0 : Math.max(0, abilityInfo.damageBonus);
        if (bonusDamage > 0 && result > 0)
            result += bonusDamage;

        if (effectiveLevel >= STUN_LEVEL && target instanceof Character)
            tryEgoStun(pc, (Character) target, weapon, "EGO_STUN_ATTACK");

        writeLog(pc, target, weapon, type.name(), damage, result);
        return result;
    }

    public static int applyDefenseAbility(Character defender, Character attacker, int damage) {
        if (!(defender instanceof PcInstance))
            return damage;
        if (attacker == null || damage <= 0 || attacker.isDead() || defender.isDead())
            return damage;
        if (Boolean.TRUE.equals(DEFENSE_RECURSION_GUARD.get()))
            return damage;

        PcInstance pc = (PcInstance) defender;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return damage;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !isEgoWeapon(weapon))
            return damage;

        int egoLevel = getEgoLevel(weapon);
        if (egoLevel <= 0)
            return damage;

        int newDamage = damage;

        if (egoLevel >= 5) {
            int reduce = Math.max(1, egoLevel / 2);
            newDamage = Math.max(1, newDamage - reduce);
            tryCounter(pc, attacker, weapon, newDamage, egoLevel, false);
        }

        if (egoLevel >= 6)
            tryCounter(pc, attacker, weapon, newDamage, egoLevel, true);

        return newDamage;
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
        return EgoWeaponDatabase.getEgoLevel(weapon, 0);
    }

    public static EgoAbilityType getAbilityType(ItemInstance weapon) {
        if (weapon == null || weapon.getItem() == null)
            return EgoAbilityType.EGO_BALANCE;
        try {
            EgoAbilityInfo info = EgoWeaponDatabase.getFirstAbility(weapon);
            if (info != null && info.abilityType != null && info.abilityType.trim().length() > 0)
                return EgoAbilityType.valueOf(info.abilityType.trim().toUpperCase());
        } catch (Exception e) {
            return EgoAbilityType.EGO_BALANCE;
        }
        try {
            return EgoAbilityType.valueOf(EgoWeaponTypeUtil.getDefaultAbilityType(weapon));
        } catch (Exception e) {
            return EgoAbilityType.EGO_BALANCE;
        }
    }

    private static void tryCounter(PcInstance pc, Character attacker, ItemInstance weapon, int baseDamage, int egoLevel, boolean automatic) {
        if (pc == null || attacker == null || weapon == null || attacker.isDead())
            return;
        String skill = automatic ? "EGO_AUTO_COUNTER" : "EGO_COUNTER";
        int cool = automatic ? AUTO_COUNTER_COOL_MS : getSkillBase("EGO_COUNTER").coolMs;
        if (!checkCooldown(weapon, skill, cool))
            return;

        int chance = automatic ? 100 : LEVEL_COUNTER_CHANCE[clampLevel(egoLevel)];
        if (Util.random(1, 100) > chance)
            return;

        int powerRate = LEVEL_COUNTER_POWER[clampLevel(egoLevel)];
        int counterDamage = Math.max(1, baseDamage * powerRate / 100);
        boolean critical = Util.random(1, 100) <= LEVEL_COUNTER_CRITICAL[clampLevel(egoLevel)];
        if (critical)
            counterDamage += Math.max(1, counterDamage * (20 + egoLevel * 2) / 100);

        markProc(weapon, skill);
        sendEffect(attacker, critical ? 12487 : 10710);
        safeCounterDamage(pc, attacker, counterDamage);
        say(pc, skill, String.format("\fY[에고] %s반격 발동. 피해 +%d%s", automatic ? "자동" : "", counterDamage, critical ? " / 치명" : ""));
        writeLog(pc, attacker, weapon, skill, baseDamage, baseDamage + counterDamage);

        if (egoLevel >= STUN_LEVEL)
            tryEgoStun(pc, attacker, weapon, "EGO_STUN_COUNTER");
    }

    private static boolean tryEgoStun(PcInstance pc, Character target, ItemInstance weapon, String coolKey) {
        if (pc == null || target == null || weapon == null)
            return false;
        if (target.isDead() || target.isLock())
            return false;
        if (!checkCooldown(weapon, coolKey, STUN_COOL_MS))
            return false;
        markProc(weapon, coolKey);

        if (Util.random(1, 100) > STUN_SUCCESS_RATE)
            return false;

        try {
            Skill skill = SkillDatabase.find(5, 1);
            if (skill == null) {
                skill = new Skill(0, "에고 스턴");
                skill.setBuffDuration(STUN_TIME);
                skill.setCastGfx(STUN_EFFECT);
            }
            int time = Math.max(1, Math.min(STUN_TIME, skill.getBuffDuration() > 0 ? skill.getBuffDuration() : STUN_TIME));
            target.toSender(S_ObjectEffect.clone(BasePacketPooling.getPool(S_ObjectEffect.class), target, STUN_EFFECT), true);
            BuffController.append(target, ShockStun.clone(BuffController.getPool(ShockStun.class), skill, time, target, STUN_EFFECT));
            say(pc, "EGO_STUN", "\fR[에고] 스턴 성공.");
            writeLog(pc, target, weapon, "EGO_STUN", 0, 0);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static int applyAttackByType(PcInstance pc, object target, int damage, int egoLevel, int effect, EgoAbilityType type) {
        switch (type) {
            case BLOOD_DRAIN:
                int heal = Math.max(1, damage * (2 + egoLevel) / 100);
                pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + heal));
                sendEffect(target, effect > 0 ? effect : 8150);
                say(pc, "BLOOD_DRAIN", String.format("\fR[에고] 생명 흡수 발동. HP +%d", heal));
                return damage + Math.max(1, egoLevel / 2);
            case MANA_DRAIN:
                int mp = Math.max(1, egoLevel / 2);
                pc.setNowMp(Math.min(pc.getTotalMp(), pc.getNowMp() + mp));
                sendEffect(target, effect > 0 ? effect : 7300);
                say(pc, "MANA_DRAIN", String.format("\fY[에고] 정신 흡수 발동. MP +%d", mp));
                return damage;
            case CRITICAL_BURST:
                int criticalAdd = Math.max(1, egoLevel + LEVEL_CRITICAL_DAMAGE[clampLevel(egoLevel)]);
                sendEffect(target, effect > 0 ? effect : 12487);
                say(pc, "CRITICAL_BURST", String.format("\fY[에고] 치명 폭발 발동. 추가 피해 +%d", criticalAdd));
                return damage + criticalAdd;
            case GUARDIAN_SHIELD:
                int hpRate = pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp());
                if (hpRate <= 40) {
                    int shieldHeal = Math.max(3, egoLevel * 2);
                    pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + shieldHeal));
                    sendEffect(pc, effect > 0 ? effect : 6321);
                    say(pc, "GUARDIAN_SHIELD", String.format("\fY[에고] 수호 의지 발동. HP +%d", shieldHeal));
                }
                return damage + Math.max(1, egoLevel / 3);
            case AREA_SLASH:
                int splashDamage = Math.max(1, damage * (8 + egoLevel) / 100);
                int count = 0;
                for (MonsterInstance mon : findNearbyMonsters(pc, target, AREA_RANGE)) {
                    if (count >= AREA_MAX_TARGET)
                        break;
                    if (mon.getObjectId() == target.getObjectId())
                        continue;
                    safeSplashDamage(pc, mon, splashDamage);
                    sendEffect(mon, effect > 0 ? effect : 12248);
                    count++;
                }
                if (count > 0)
                    say(pc, "AREA_SLASH", String.format("\fY[에고] 공명 베기 발동. 주변 %d명에게 피해", count));
                return damage;
            case EXECUTION:
                if (target instanceof Character) {
                    Character t = (Character) target;
                    int targetHpRate = t.getNowHp() * 100 / Math.max(1, t.getTotalHp());
                    if (targetHpRate <= 20) {
                        int add = Math.max(2, egoLevel * 2);
                        sendEffect(target, effect > 0 ? effect : 8683);
                        say(pc, "EXECUTION", String.format("\fR[에고] 처형 발동. 추가 피해 +%d", add));
                        return damage + add;
                    }
                }
                return damage;
            case FLAME_BRAND:
                int fireAdd = Math.max(1, 2 + egoLevel);
                sendEffect(target, effect > 0 ? effect : 1811);
                say(pc, "FLAME_BRAND", String.format("\fR[에고] 화염 각인 발동. 추가 피해 +%d", fireAdd));
                return damage + fireAdd;
            case FROST_BIND:
                int frostAdd = Math.max(1, egoLevel / 2);
                sendEffect(target, effect > 0 ? effect : 3684);
                say(pc, "FROST_BIND", "\fY[에고] 서리 충격 발동.");
                return damage + frostAdd;
            case EGO_BALANCE:
            default:
                int balanceAdd = Math.max(1, egoLevel / 2);
                sendEffect(target, effect > 0 ? effect : 3940);
                say(pc, "EGO_BALANCE", String.format("\fY[에고] 공명 타격 발동. 추가 피해 +%d", balanceAdd));
                return damage + balanceAdd;
        }
    }

    private static void safeCounterDamage(PcInstance pc, Character attacker, int counterDamage) {
        if (pc == null || attacker == null || counterDamage <= 0 || attacker.isDead())
            return;
        try {
            DEFENSE_RECURSION_GUARD.set(Boolean.TRUE);
            DamageController.toDamage(pc, attacker, counterDamage, Lineage.ATTACK_TYPE_WEAPON);
        } catch (Throwable e) {
        } finally {
            DEFENSE_RECURSION_GUARD.remove();
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
        int beforeLevel = before == null ? 0 : before.level;
        boolean levelUp = EgoWeaponDatabase.addExp(weapon, addExp);
        EgoWeaponInfo after = EgoWeaponDatabase.find(weapon);
        int afterLevel = after == null ? beforeLevel : after.level;

        if (levelUp || afterLevel > beforeLevel) {
            sendEffect(pc, 3944);
            say(pc, "LEVEL_UP", String.format("\fY[에고] 의식이 성장했습니다. Lv.%d", afterLevel));
            writeLog(pc, pc, weapon, "LEVEL_UP", 0, 0);
            EgoView.refreshInventory(pc, weapon);
            return;
        }

        if (forceMessage || Util.random(1, 100) <= 3) {
            EgoWeaponInfo info = EgoWeaponDatabase.find(weapon);
            if (info != null)
                say(pc, "EXP", String.format("\fS[에고] 경험치 +%d (%d/%d)", addExp, info.exp, info.maxExp));
        }
    }

    private static int getProcChance(int egoLevel, EgoAbilityInfo abilityInfo, SkillBaseInfo base, EgoAbilityType type) {
        if (egoLevel <= 0)
            return 0;
        int level = clampLevel(egoLevel);
        int chance = base.baseRate + Math.max(0, level - 1) * base.levelRate + LEVEL_PROC_BONUS[level];
        if (type == EgoAbilityType.CRITICAL_BURST)
            chance += LEVEL_CRITICAL_CHANCE[level];
        if (abilityInfo != null)
            chance += abilityInfo.procChanceBonus;
        return Math.min(base.maxRate, Math.max(1, chance));
    }

    private static boolean checkCooldown(ItemInstance weapon, String skill, int coolMs) {
        if (weapon == null || skill == null || coolMs <= 0)
            return true;
        long now = java.lang.System.currentTimeMillis();
        String key = weapon.getObjectId() + ":" + skill;
        Long last = procCoolMap.get(key);
        if (last != null && now - last.longValue() < coolMs)
            return false;
        return true;
    }

    private static void markProc(ItemInstance weapon, String skill) {
        if (weapon == null || skill == null)
            return;
        long now = java.lang.System.currentTimeMillis();
        procCoolMap.put(weapon.getObjectId() + ":" + skill, now);

        Connection con = null;
        PreparedStatement st = null;
        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("UPDATE ego_skill SET last_proc=?, mod_date=NOW() WHERE item_id=? AND skill=? AND use_yn=1");
            st.setLong(1, now);
            st.setLong(2, weapon.getObjectId());
            st.setString(3, skill);
            st.executeUpdate();
        } catch (Exception e) {
        } finally {
            DatabaseConnection.close(con, st);
        }
    }

    private static void safeSplashDamage(PcInstance pc, MonsterInstance mon, int splashDamage) {
        if (pc == null || mon == null || splashDamage <= 0)
            return;
        if (mon.isDead())
            return;
        try {
            mon.toDamage(pc, splashDamage, Lineage.ATTACK_TYPE_WEAPON);
        } catch (Throwable e) {
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

    private static void loadSkillBase() {
        Connection con = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("SELECT * FROM ego_skill_base WHERE use_yn=1");
            rs = st.executeQuery();
            while (rs.next()) {
                SkillBaseInfo info = new SkillBaseInfo();
                info.skill = rs.getString("skill");
                info.label = rs.getString("label");
                info.baseRate = Math.max(1, rs.getInt("base_rate"));
                info.levelRate = Math.max(0, rs.getInt("lv_rate"));
                info.maxRate = Math.max(1, rs.getInt("max_rate"));
                info.minLevel = Math.max(1, rs.getInt("min_lv"));
                info.coolMs = Math.max(0, rs.getInt("cool_ms"));
                info.effect = Math.max(0, rs.getInt("effect"));
                if (info.skill != null)
                    skillBaseMap.put(info.skill.trim().toUpperCase(), info);
            }
        } catch (Exception e) {
        } finally {
            DatabaseConnection.close(con, st, rs);
        }
    }

    private static SkillBaseInfo getSkillBase(String skill) {
        if (skillBaseMap.isEmpty())
            loadSkillBase();
        SkillBaseInfo info = skill == null ? null : skillBaseMap.get(skill.trim().toUpperCase());
        if (info != null)
            return info;
        SkillBaseInfo def = new SkillBaseInfo();
        def.skill = skill == null ? "EGO_BALANCE" : skill;
        def.baseRate = 3;
        def.levelRate = 1;
        def.maxRate = 25;
        def.minLevel = 1;
        def.coolMs = 0;
        def.effect = 3940;
        return def;
    }

    private static int clampLevel(int level) {
        if (level < 0)
            return 0;
        if (level > MAX_EGO_LEVEL)
            return MAX_EGO_LEVEL;
        return level;
    }

    private static void writeLog(PcInstance pc, object target, ItemInstance weapon, String skill, int baseDmg, int finalDmg) {
        if (pc == null || weapon == null || skill == null)
            return;
        Connection con = null;
        PreparedStatement st = null;
        try {
            con = DatabaseConnection.getLineage();
            st = con.prepareStatement("INSERT INTO ego_log (item_id, char_id, char_name, target_name, skill, base_dmg, final_dmg, add_dmg) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            st.setLong(1, weapon.getObjectId());
            st.setLong(2, pc.getObjectId());
            st.setString(3, safeName(pc));
            st.setString(4, safeName(target));
            st.setString(5, skill);
            st.setInt(6, Math.max(0, baseDmg));
            st.setInt(7, Math.max(0, finalDmg));
            st.setInt(8, Math.max(0, finalDmg - baseDmg));
            st.executeUpdate();
        } catch (Exception e) {
        } finally {
            DatabaseConnection.close(con, st);
        }
    }

    private static String safeName(object o) {
        if (o == null)
            return "";
        try {
            String name = o.getName();
            return name == null ? "" : name;
        } catch (Throwable e) {
            return "";
        }
    }

    private static final class SkillBaseInfo {
        String skill;
        String label;
        int baseRate;
        int levelRate;
        int maxRate;
        int minLevel;
        int coolMs;
        int effect;
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
        FROST_BIND,
        EGO_COUNTER,
        EGO_REVENGE
    }
}
