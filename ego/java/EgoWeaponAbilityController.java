package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.DatabaseConnection;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
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
 * 기존 전투 공식은 변경하지 않고, DamageController에서 계산된 최종 피해에 보조능력만 더한다.
 * 공격 훅: DamageController.getDamage 최종 return 직전 EgoSkill.attack(...)
 * 피격 훅: DamageController.toDamage HP 감소 직전 EgoSkill.defense(...)
 */
public final class EgoWeaponAbilityController {

    private static final int DEFAULT_MAX_EGO_LEVEL = 30;
    private static final int DEFAULT_BASE_PROC_CHANCE = 3;
    private static final int DEFAULT_LV_PROC_CHANCE = 1;
    private static final int DEFAULT_MAX_PROC_CHANCE = 25;
    private static final int AREA_RANGE = 2;
    private static final int AREA_MAX_TARGET = 4;

    private static final long ATTACK_EGO_EXP = 1L;
    private static final long ATTACK_EXP_DELAY_MS = 3000L;
    private static final long DEFAULT_KILL_EGO_EXP = 5L;
    private static final long BOSS_KILL_EGO_EXP = 50L;
    private static final long PROC_MESSAGE_DELAY_MS = 1500L;

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

    /** 공격 시 에고 보조능력. */
    public static int applyAttackAbility(Character cha, object target, ItemInstance weapon, int damage) {
        if (!(cha instanceof PcInstance))
            return damage;
        if (damage <= 0 || target == null || target.isDead())
            return damage;
        if (weapon == null || !isEgoWeapon(weapon))
            return damage;
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
            return damage;

        PcInstance pc = (PcInstance) cha;
        gainAttackExp(pc, weapon);

        EgoAbilityInfo abilityInfo = EgoWeaponDatabase.getFirstAbility(weapon);
        EgoAbilityType type = getAbilityType(weapon);
        if (!EgoWeaponTypeUtil.isAbilityAllowed(type.name(), weapon))
            type = EgoAbilityType.EGO_BALANCE;

        SkillBaseInfo base = getSkillBase(type.name());
        int egoLevel = getEgoLevel(weapon);
        int abilityLevel = abilityInfo == null ? 1 : Math.max(1, abilityInfo.abilityLevel);
        int effectiveLevel = Math.min(DEFAULT_MAX_EGO_LEVEL, egoLevel + Math.max(0, abilityLevel - 1));

        if (effectiveLevel < Math.max(1, base.minLevel))
            return damage;
        if (!checkCooldown(weapon, type.name(), base.coolMs))
            return damage;

        int chance = getProcChance(effectiveLevel, abilityInfo, base);
        if (Util.random(1, 100) > chance)
            return damage;

        markProc(weapon, type.name());

        int result = applyAttackByType(pc, target, damage, effectiveLevel, base.effect, type);
        int bonusDamage = abilityInfo == null ? 0 : Math.max(0, abilityInfo.damageBonus);
        if (bonusDamage > 0 && result > 0)
            result += bonusDamage;

        writeLog(pc, target, weapon, type.name(), damage, result);
        return result;
    }

    /**
     * 피격 시 에고 레벨별 해금 능력.
     * Lv.5  : 에고 방어본능, 받는 피해 소폭 감소
     * Lv.10 : 에고 반격, 공격자에게 반격 피해
     * Lv.20 : 에고 복수, 저체력 피격 시 강한 반격 + 소량 회복
     */
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
        int newDamage = damage;

        // Lv.5 해금: 방어본능. 피격 피해 소폭 감소.
        if (egoLevel >= 5) {
            int reduce = Math.max(1, Math.min(10, egoLevel / 3));
            newDamage = Math.max(1, newDamage - reduce);
        }

        // Lv.10 해금: 반격.
        if (egoLevel >= 10) {
            SkillBaseInfo counter = getSkillBase("EGO_COUNTER");
            if (checkCooldown(weapon, "EGO_COUNTER", counter.coolMs)) {
                int chance = Math.min(counter.maxRate, counter.baseRate + (egoLevel - 10) * Math.max(0, counter.levelRate));
                if (Util.random(1, 100) <= Math.max(1, chance)) {
                    int reflect = Math.max(1, newDamage * (10 + egoLevel) / 100);
                    markProc(weapon, "EGO_COUNTER");
                    sendEffect(attacker, counter.effect > 0 ? counter.effect : 10710);
                    safeCounterDamage(pc, attacker, reflect);
                    say(pc, "EGO_COUNTER", String.format("\fY[에고] 반격 발동. 반격 피해 +%d", reflect));
                    writeLog(pc, attacker, weapon, "EGO_COUNTER", damage, newDamage);
                }
            }
        }

        // Lv.20 해금: 저체력 복수 특수스킬.
        if (egoLevel >= 20) {
            int hpRate = pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp());
            SkillBaseInfo revenge = getSkillBase("EGO_REVENGE");
            if (hpRate <= 35 && checkCooldown(weapon, "EGO_REVENGE", revenge.coolMs)) {
                int chance = Math.min(revenge.maxRate, revenge.baseRate + (egoLevel - 20) * Math.max(0, revenge.levelRate));
                if (Util.random(1, 100) <= Math.max(1, chance)) {
                    int revengeDmg = Math.max(5, egoLevel * 2);
                    int heal = Math.max(3, egoLevel / 2);
                    markProc(weapon, "EGO_REVENGE");
                    sendEffect(pc, revenge.effect > 0 ? revenge.effect : 6321);
                    safeCounterDamage(pc, attacker, revengeDmg);
                    pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + heal));
                    say(pc, "EGO_REVENGE", String.format("\fR[에고] 복수 각성. 반격 +%d / HP +%d", revengeDmg, heal));
                    writeLog(pc, attacker, weapon, "EGO_REVENGE", damage, newDamage);
                }
            }
        }

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
        return EgoWeaponDatabase.getEgoLevel(weapon, 1);
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

    private static int applyAttackByType(PcInstance pc, object target, int damage, int egoLevel, int effect, EgoAbilityType type) {
        switch (type) {
            case BLOOD_DRAIN:
                int heal = Math.max(1, damage * (3 + egoLevel / 3) / 100);
                pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + heal));
                sendEffect(target, effect > 0 ? effect : 8150);
                say(pc, "BLOOD_DRAIN", String.format("\fR[에고] 생명 흡수 발동. HP +%d", heal));
                return damage + Math.max(1, egoLevel / 2);
            case MANA_DRAIN:
                int mp = Math.max(1, 1 + egoLevel / 4);
                pc.setNowMp(Math.min(pc.getTotalMp(), pc.getNowMp() + mp));
                sendEffect(target, effect > 0 ? effect : 7300);
                say(pc, "MANA_DRAIN", String.format("\fY[에고] 정신 흡수 발동. MP +%d", mp));
                return damage;
            case CRITICAL_BURST:
                int criticalAdd = Math.max(2, egoLevel);
                sendEffect(target, effect > 0 ? effect : 12487);
                say(pc, "CRITICAL_BURST", String.format("\fY[에고] 치명 폭발 발동. 추가 피해 +%d", criticalAdd));
                return damage + criticalAdd;
            case GUARDIAN_SHIELD:
                int hpRate = pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp());
                if (hpRate <= 40) {
                    int shieldHeal = Math.max(5, egoLevel * 2);
                    pc.setNowHp(Math.min(pc.getTotalHp(), pc.getNowHp() + shieldHeal));
                    sendEffect(pc, effect > 0 ? effect : 6321);
                    say(pc, "GUARDIAN_SHIELD", String.format("\fY[에고] 수호 의지 발동. HP +%d", shieldHeal));
                }
                return damage + Math.max(1, egoLevel / 3);
            case AREA_SLASH:
                int splashDamage = Math.max(1, damage * (10 + egoLevel) / 100);
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
                        int add = Math.max(3, egoLevel * 2);
                        sendEffect(target, effect > 0 ? effect : 8683);
                        say(pc, "EXECUTION", String.format("\fR[에고] 처형 발동. 약화된 적에게 추가 피해 +%d", add));
                        return damage + add;
                    }
                }
                return damage;
            case FLAME_BRAND:
                int fireAdd = Math.max(2, 3 + egoLevel);
                sendEffect(target, effect > 0 ? effect : 1811);
                say(pc, "FLAME_BRAND", String.format("\fR[에고] 화염 각인 발동. 추가 피해 +%d", fireAdd));
                return damage + fireAdd;
            case FROST_BIND:
                int frostAdd = Math.max(1, egoLevel / 2);
                sendEffect(target, effect > 0 ? effect : 3684);
                say(pc, "FROST_BIND", "\fY[에고] 서리 충격 발동. 적의 움직임을 흔듭니다.");
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
        int beforeLevel = before == null ? 1 : before.level;
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

    private static int getProcChance(int egoLevel, EgoAbilityInfo abilityInfo, SkillBaseInfo base) {
        int chance = base.baseRate + Math.max(0, egoLevel - 1) * base.levelRate;
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
        def.baseRate = DEFAULT_BASE_PROC_CHANCE;
        def.levelRate = DEFAULT_LV_PROC_CHANCE;
        def.maxRate = DEFAULT_MAX_PROC_CHANCE;
        def.minLevel = 1;
        def.coolMs = 0;
        def.effect = 3940;
        return def;
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
