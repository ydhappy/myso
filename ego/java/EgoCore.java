package lineage.world.controller;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoDB;
import lineage.database.EgoWeaponDatabase;
import lineage.share.Lineage;
import lineage.world.object.Character;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;
import lineage.world.object.instance.RobotInstance;

/**
 * 에고 시스템 통합 진입점.
 *
 * 외부 연결은 이 클래스 중심으로 사용한다.
 * - init/reload: 서버 시작/관리자 리로드
 * - chat/tick: 대화/상태 주기 처리
 * - attack/defense/kill: 전투 보정/처치 경험치
 * - useOrb: 에고 구슬 사용 처리
 */
public final class EgoCore {

    private EgoCore() {
    }

    public static void init(Connection con) {
        EgoSchema.silentCheck(con);
        EgoDB.init(con);
    }

    public static void reload(Connection con) {
        EgoSchema.silentCheck(con);
        EgoDB.reload(con);
    }

    public static String schemaReport(Connection con) {
        return EgoSchema.report(con);
    }

    public static boolean schemaOk(Connection con) {
        return EgoSchema.isValid(con);
    }

    public static boolean useOrb(PcInstance pc, ItemInstance orb) {
        return EgoOrbController.use(pc, orb);
    }

    public static boolean chat(object o, String msg) {
        if (!(o instanceof PcInstance) || o instanceof RobotInstance)
            return false;
        PcInstance pc = (PcInstance) o;
        EgoOwnerRecognition.recognize(pc);
        return EgoTalk.chat(pc, msg);
    }

    public static void tick(object o) {
        if (!(o instanceof PcInstance) || o instanceof RobotInstance)
            return;
        PcInstance pc = (PcInstance) o;
        EgoOwnerRecognition.recognize(pc);
        EgoTalk.warning(pc);
    }

    public static int attack(Character cha, object target, ItemInstance weapon, int damage) {
        if (cha instanceof PcInstance)
            EgoOwnerRecognition.recognize((PcInstance) cha, weapon);
        return EgoWeaponAbilityController.applyAttackAbility(cha, target, weapon, damage);
    }

    public static int defense(Character defender, Character attacker, int damage) {
        if (defender instanceof PcInstance)
            EgoOwnerRecognition.recognize((PcInstance) defender);
        return EgoWeaponAbilityController.applyDefenseAbility(defender, attacker, damage);
    }

    public static void kill(PcInstance pc, lineage.world.object.instance.MonsterInstance mon) {
        EgoWeaponAbilityController.addKillExp(pc, mon);
    }

    public static boolean isEgo(ItemInstance weapon) {
        return EgoWeaponAbilityController.isEgoWeapon(weapon);
    }

    public static int level(ItemInstance weapon) {
        return EgoWeaponAbilityController.getEgoLevel(weapon);
    }
}

final class EgoOrbController {

    static final int DEFAULT_ITEM_CODE = 900001;
    static final String ITEM_NAME = "에고 구슬";

    private static final long USE_LOCK_MS = 1000L;
    private static final Random random = new Random();
    private static final Map<Long, Long> useLockMap = new ConcurrentHashMap<Long, Long>();
    private static final String[] TONES = { "예의", "예의반대" };
    private static final String[] ABILITIES = {
        "BALANCE",
        "BLOOD",
        "MANA",
        "CRIT",
        "SHIELD",
        "AREA",
        "EXECUTE",
        "FIRE",
        "FROST"
    };

    private EgoOrbController() {
    }

    static boolean use(PcInstance pc, ItemInstance orb) {
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
        String egoName = defaultEgoName(weapon);
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

    private static String defaultEgoName(ItemInstance weapon) {
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

    static String randomAbility(ItemInstance weapon) {
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

    static String randomTone() {
        return TONES[random.nextInt(TONES.length)];
    }
}

final class EgoOwnerRecognition {

    private EgoOwnerRecognition() {
    }

    static void recognize(PcInstance pc) {
        if (pc == null)
            return;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return;
        recognize(pc, inv.getSlot(Lineage.SLOT_WEAPON));
    }

    static void recognize(PcInstance pc, ItemInstance weapon) {
        if (pc == null || weapon == null)
            return;
        if (!EgoWeaponDatabase.isEgoWeapon(weapon))
            return;
        EgoWeaponDatabase.recognizeOwner(pc, weapon);
    }
}

final class EgoWeaponTypeUtil {

    private static final String FISHING_ROD = "fishing_rod";

    private EgoWeaponTypeUtil() {
    }

    static String getOriginalType2(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return "";
        return normalize(item.getItem().getType2());
    }

    static String getType2(ItemInstance item) {
        return getOriginalType2(item);
    }

    static boolean isWeaponSlot(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return false;
        try {
            return item.getItem().getSlot() == Lineage.SLOT_WEAPON;
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean isFishingRod(ItemInstance item) {
        return FISHING_ROD.equals(getOriginalType2(item));
    }

    static boolean isValidEgoBaseWeapon(ItemInstance item) {
        return isWeaponSlot(item) && !isFishingRod(item);
    }

    static boolean isAbilityAllowed(String ability, ItemInstance item) {
        return normalizeAbility(ability).length() > 0 && isValidEgoBaseWeapon(item);
    }

    static String getDefaultAbilityType(ItemInstance item) {
        String name = safeItemName(item).toLowerCase();
        if (name.indexOf("피") >= 0 || name.indexOf("blood") >= 0 || name.indexOf("흡혈") >= 0)
            return "BLOOD_DRAIN";
        if (name.indexOf("마나") >= 0 || name.indexOf("지식") >= 0 || name.indexOf("mana") >= 0)
            return "MANA_DRAIN";
        if (name.indexOf("화염") >= 0 || name.indexOf("불") >= 0 || name.indexOf("flame") >= 0 || name.indexOf("fire") >= 0)
            return "FLAME_BRAND";
        if (name.indexOf("얼음") >= 0 || name.indexOf("서리") >= 0 || name.indexOf("frost") >= 0 || name.indexOf("ice") >= 0)
            return "FROST_BIND";
        if (name.indexOf("수호") >= 0 || name.indexOf("가디언") >= 0 || name.indexOf("guardian") >= 0)
            return "GUARDIAN_SHIELD";
        return "EGO_BALANCE";
    }

    static String getDisplayTypeName(ItemInstance item) {
        if (isFishingRod(item))
            return "낚싯대";
        if (isWeaponSlot(item))
            return "무기";
        return "무기 아님";
    }

    static String getAbilityDenyReason(String ability, ItemInstance item) {
        if (item == null)
            return "착용 무기가 없습니다.";
        if (!isWeaponSlot(item))
            return "무기 슬롯 아이템이 아닙니다.";
        if (isFishingRod(item))
            return "낚싯대는 에고무기로 사용할 수 없습니다.";
        return "에고 대상으로 사용할 수 있습니다.";
    }

    static String getSupportedWeaponTypesText() {
        return "지원 대상: 무기 슬롯 전체 / 제외: 낚싯대(fishing_rod)";
    }

    private static String safeItemName(ItemInstance item) {
        if (item == null)
            return "";
        try {
            String name = item.getName();
            return name == null ? "" : name.trim();
        } catch (Throwable e) {
            return "";
        }
    }

    private static String normalizeAbility(String ability) {
        return ability == null ? "" : ability.trim().toUpperCase();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}

final class EgoSchema {

    static final String T_EGO = "ego";
    static final String T_EGO_SKILL = "ego_skill";
    static final String T_EGO_SKILL_BASE = "ego_skill_base";
    static final String T_EGO_LOG = "ego_log";
    static final String T_EGO_TALK_PACK = "ego_talk_pack";
    static final String T_EGO_CONFIG = "ego_config";
    static final String T_EGO_LEVEL = "ego_level";
    static final String T_EGO_BOND = "ego_bond";
    static final String T_EGO_ITEM_TEMPLATE = "ego_item_template";

    private static final Map<String, String[]> REQUIRED = new LinkedHashMap<String, String[]>();

    static {
        REQUIRED.put(T_EGO, new String[] { "item_id", "char_id", "use_yn", "ego_name", "ego_type", "ego_lv", "ego_exp", "need_exp", "talk_lv", "ctrl_lv", "last_talk", "last_warn", "bond", "bond_reason", "reg_date", "mod_date" });
        REQUIRED.put(T_EGO_SKILL, new String[] { "id", "item_id", "skill", "skill_lv", "rate_bonus", "dmg_bonus", "last_proc", "use_yn", "reg_date", "mod_date" });
        REQUIRED.put(T_EGO_SKILL_BASE, new String[] { "skill", "label", "memo", "base_rate", "lv_rate", "max_rate", "min_lv", "cool_ms", "effect", "use_yn" });
        REQUIRED.put(T_EGO_LOG, new String[] { "id", "item_id", "char_id", "char_name", "target_name", "skill", "base_dmg", "final_dmg", "add_dmg", "reg_date" });
        REQUIRED.put(T_EGO_TALK_PACK, new String[] { "id", "genre", "tone", "keyword", "message", "use_yn", "reg_date", "mod_date" });
        REQUIRED.put(T_EGO_CONFIG, new String[] { "config_key", "config_value", "memo", "use_yn", "reg_date", "mod_date" });
        REQUIRED.put(T_EGO_LEVEL, new String[] { "ego_lv", "need_exp", "proc_bonus", "critical_chance", "critical_damage", "counter_chance", "counter_power", "counter_critical", "memo", "use_yn", "reg_date", "mod_date" });
        REQUIRED.put(T_EGO_BOND, new String[] { "item_id", "bond", "last_reason", "reg_date", "mod_date" });
        REQUIRED.put(T_EGO_ITEM_TEMPLATE, new String[] { "item_code", "item_name", "java_class", "item_type1", "item_type2", "name_id", "inv_gfx", "ground_gfx", "stackable", "memo", "use_yn", "reg_date", "mod_date" });
    }

    private EgoSchema() {
    }

    static boolean isValid(Connection con) {
        return validate(con).ok;
    }

    static String report(Connection con) {
        return validate(con).message;
    }

    static void silentCheck(Connection con) {
        try {
            validate(con);
        } catch (Throwable e) {
        }
    }

    private static Result validate(Connection con) {
        Result result = new Result();
        StringBuilder sb = new StringBuilder();
        sb.append("[에고 스키마 연결성 점검]\n");
        sb.append("기준 SQL: ego/sql/ego_schema.sql\n");

        if (con == null) {
            result.ok = false;
            sb.append("FAIL: Connection is null\n");
            result.message = sb.toString();
            return result;
        }

        try {
            DatabaseMetaData meta = con.getMetaData();
            boolean allOk = true;

            for (Map.Entry<String, String[]> e : REQUIRED.entrySet()) {
                String table = e.getKey();
                if (!tableExists(meta, table)) {
                    allOk = false;
                    sb.append("MISSING TABLE: ").append(table).append('\n');
                    continue;
                }
                sb.append("OK TABLE: ").append(table).append('\n');

                String[] cols = e.getValue();
                for (int i = 0; i < cols.length; i++) {
                    String col = cols[i];
                    if (!columnExists(meta, table, col)) {
                        allOk = false;
                        sb.append("  MISSING COLUMN: ").append(table).append('.').append(col).append('\n');
                    }
                }
            }

            if (allOk)
                sb.append("RESULT: OK\n");
            else
                sb.append("RESULT: FAIL - ego/sql/ego_schema.sql 적용 후 .에고리로드 필요\n");

            result.ok = allOk;
            result.message = sb.toString();
            return result;
        } catch (Exception ex) {
            result.ok = false;
            sb.append("FAIL: ").append(ex.getMessage()).append('\n');
            result.message = sb.toString();
            return result;
        }
    }

    private static boolean tableExists(DatabaseMetaData meta, String table) throws Exception {
        ResultSet rs = null;
        try {
            rs = meta.getTables(null, null, table, null);
            if (rs != null && rs.next())
                return true;
        } finally {
            close(rs);
        }
        try {
            rs = meta.getTables(null, null, table.toUpperCase(), null);
            return rs != null && rs.next();
        } finally {
            close(rs);
        }
    }

    private static boolean columnExists(DatabaseMetaData meta, String table, String column) throws Exception {
        ResultSet rs = null;
        try {
            rs = meta.getColumns(null, null, table, column);
            if (rs != null && rs.next())
                return true;
        } finally {
            close(rs);
        }
        try {
            rs = meta.getColumns(null, null, table, column.toUpperCase());
            return rs != null && rs.next();
        } finally {
            close(rs);
        }
    }

    private static void close(ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
        } catch (Exception e) {
        }
    }

    private static final class Result {
        boolean ok;
        String message;
    }
}
