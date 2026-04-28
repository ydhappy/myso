package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.network.packet.BasePacketPooling;
import lineage.network.packet.server.S_InventoryAdd;
import lineage.network.packet.server.S_InventoryDelete;
import lineage.network.packet.server.S_InventoryEquipped;
import lineage.network.packet.server.S_InventoryStatus;
import lineage.world.World;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 표시 전용 유틸.
 *
 * 최종 정책:
 * - type2 변형 없음.
 * - 인벤토리/바닥 이미지 변경 없음.
 * - 원본 아이템 템플릿의 inv_gfx / ground_gfx 그대로 사용.
 * - 인벤토리 이름에는 아이템명 뒤 [Lv.x 에고이름]만 추가한다.
 */
public final class EgoView {

    private EgoView() {
    }

    /** DB 이미지 캐시 없음. 호환용 no-op. */
    public static void reload(java.sql.Connection con) {
    }

    public static boolean isEgo(ItemInstance item) {
        return item != null && EgoWeaponDatabase.isEgoWeapon(item);
    }

    public static String form(ItemInstance item) {
        return EgoWeaponTypeUtil.getType2(item);
    }

    /** 원본 인벤토리 이미지 그대로 사용. */
    public static int invGfx(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return 0;
        return item.getItem().getInvGfx();
    }

    /** 원본 바닥 이미지 그대로 사용. */
    public static int groundGfx(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return 0;
        return item.getItem().getGroundGfx();
    }

    /** 원본 바닥 이미지 그대로 사용하므로 별도 적용하지 않는다. */
    public static void applyGroundGfx(ItemInstance item) {
    }

    /**
     * 에고 정보/상태/생성 메시지에서 사용할 실제 아이템명.
     * ItemInstance.clone() 구조상 item.getName()이 nameId($1234)일 수 있어 템플릿 name을 우선 사용한다.
     */
    public static String displayName(ItemInstance item) {
        if (item == null)
            return "";
        return displayName(item, item.getName());
    }

    public static String displayName(ItemInstance item, String baseName) {
        if (item == null)
            return baseName == null ? "" : baseName;
        return fixNameIdBaseName(item, baseName == null ? item.getName() : baseName);
    }

    public static String name(ItemInstance item, String baseName) {
        if (item == null || baseName == null)
            return baseName;
        if (!isEgo(item))
            return baseName;
        if (baseName.indexOf("[Lv.") >= 0)
            return EgoMessageUtil.clientColor(baseName);

        String fixedName = displayName(item, baseName);
        EgoWeaponInfo ego = EgoWeaponDatabase.find(item);
        int level = ego == null ? 1 : Math.max(1, ego.level);
        String egoName = ego == null ? "에고" : safe(ego.egoName);
        if (egoName.length() == 0)
            egoName = "에고";

        StringBuilder sb = new StringBuilder(fixedName);
        sb.append(" ").append(EgoMessageUtil.COLOR_INFO);
        sb.append("[Lv.").append(level).append(" ").append(egoName).append("]");
        sb.append(EgoMessageUtil.COLOR_WHITE);
        return EgoMessageUtil.clientColor(sb.toString());
    }

    public static String info(ItemInstance item) {
        if (item == null || !isEgo(item))
            return "";
        EgoWeaponInfo ego = EgoWeaponDatabase.find(item);
        int level = ego == null ? 1 : Math.max(1, ego.level);
        long exp = ego == null ? 0 : Math.max(0, ego.exp);
        long need = ego == null ? 100 : Math.max(1, ego.maxExp);
        String skill = skillName(item);
        String egoName = ego == null ? "에고" : safe(ego.egoName);
        if (egoName.length() == 0)
            egoName = "에고";

        StringBuilder sb = new StringBuilder();
        sb.append("에고명: ").append(egoName);
        sb.append(" / 레벨: ").append(level);
        sb.append(" / 경험치: ").append(exp).append("/").append(need);
        if (skill.length() > 0)
            sb.append(" / 능력: ").append(skill);
        return sb.toString();
    }

    public static void refreshInventory(PcInstance pc, ItemInstance item) {
        if (pc == null || item == null)
            return;
        try {
            pc.toSender(S_InventoryDelete.clone(BasePacketPooling.getPool(S_InventoryDelete.class), item));
            pc.toSender(S_InventoryAdd.clone(BasePacketPooling.getPool(S_InventoryAdd.class), item));
            pc.toSender(S_InventoryStatus.clone(BasePacketPooling.getPool(S_InventoryStatus.class), item));
            if (item.isEquipped())
                pc.toSender(S_InventoryEquipped.clone(BasePacketPooling.getPool(S_InventoryEquipped.class), item));
        } catch (Throwable e) {
            EgoMessageUtil.danger(pc, "에고 인벤토리 표시 갱신 중 오류가 발생했습니다.");
        }
    }

    public static int refreshPcInventory(PcInstance pc) {
        if (pc == null)
            return 0;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return 0;

        int count = 0;
        for (ItemInstance item : inv.getList()) {
            if (isEgo(item)) {
                refreshInventory(pc, item);
                count++;
            }
        }
        return count;
    }

    public static int refreshOnlineInventories(PcInstance caller) {
        int count = 0;
        List<PcInstance> pcs = new ArrayList<PcInstance>();
        try {
            pcs.addAll(World.getPcList());
        } catch (Throwable e) {
            if (caller != null)
                pcs.add(caller);
        }
        for (PcInstance pc : pcs)
            count += refreshPcInventory(pc);
        return count;
    }

    public static String label(ItemInstance item) {
        return EgoWeaponTypeUtil.getDisplayTypeName(item);
    }

    private static String fixNameIdBaseName(ItemInstance item, String baseName) {
        if (item == null || item.getItem() == null)
            return baseName == null ? "" : baseName;

        String realName = safe(item.getItem().getName());
        if (realName.length() == 0)
            return baseName == null ? "" : baseName;

        String nameId = safe(item.getItem().getNameId());
        String result = baseName == null ? "" : baseName;

        if (result.length() == 0 || result.startsWith("$"))
            result = realName;

        if (nameId.length() > 0 && result.indexOf(nameId) >= 0)
            result = result.replace(nameId, realName);

        String runtimeName = safe(item.getName());
        if (runtimeName.length() > 0 && runtimeName.startsWith("$") && result.indexOf(runtimeName) >= 0)
            result = result.replace(runtimeName, realName);

        return result;
    }

    private static String skillName(ItemInstance item) {
        EgoAbilityInfo skill = EgoWeaponDatabase.getFirstAbility(item);
        if (skill == null || skill.abilityType == null)
            return "";
        String type = skill.abilityType;
        if ("EGO_BALANCE".equals(type)) return "공명";
        if ("BLOOD_DRAIN".equals(type)) return "흡혈";
        if ("MANA_DRAIN".equals(type)) return "흡마";
        if ("CRITICAL_BURST".equals(type)) return "치명";
        if ("GUARDIAN_SHIELD".equals(type)) return "수호";
        if ("AREA_SLASH".equals(type)) return "광역";
        if ("EXECUTION".equals(type)) return "처형";
        if ("FLAME_BRAND".equals(type)) return "화염";
        if ("FROST_BIND".equals(type)) return "서리";
        if ("EGO_COUNTER".equals(type)) return "반격";
        if ("EGO_REVENGE".equals(type)) return "복수";
        return type;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
