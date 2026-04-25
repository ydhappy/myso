package lineage.world.controller;

import java.lang.reflect.Field;
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
 * 처리 범위:
 * - 인벤토리 아이콘: 에고모양.인벤이미지
 * - 바닥 이미지: 에고모양.바닥이미지
 * - 인벤토리 이름/아이템정보: [에고:형태 Lv.n 능력] suffix
 * - 형태변신 직후 인벤토리 아이콘 재전송
 * - 형태변신 직후 바닥 표시 gfx 동기화
 * - 리로드 직후 접속자 인벤토리 에고 아이템 즉시 재전송
 *
 * 안전 원칙:
 * - 원본 Item 템플릿의 InvGfx/GroundGfx/NameId/type2는 변경하지 않는다.
 * - 에고 아이템 1개에 대해서만 표시값을 계산한다.
 * - 에고모양 테이블에 이미지값이 0이면 원본 이미지를 사용한다.
 */
public final class EgoView {

    private static final Map<String, ViewInfo> viewMap = new ConcurrentHashMap<String, ViewInfo>();

    static {
        resetDefaults();
    }

    private EgoView() {
    }

    public static void reload(Connection con) {
        resetDefaults();

        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;

        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }

            st = con.prepareStatement("SELECT * FROM `에고모양` WHERE `사용`=1");
            rs = st.executeQuery();

            while (rs.next()) {
                ViewInfo info = new ViewInfo();
                info.form = normalize(rs.getString("형태"));
                info.label = safe(rs.getString("표시"));
                info.invGfx = Math.max(0, rs.getInt("인벤이미지"));
                info.groundGfx = Math.max(0, rs.getInt("바닥이미지"));
                info.info = safe(rs.getString("설명"));

                if (info.form.length() > 0)
                    viewMap.put(info.form, info);
            }
        } catch (Exception e) {
            // 에고모양 테이블이 없어도 서버는 원본 이미지로 동작해야 한다.
            lineage.share.System.println("EgoView reload skip: " + e.getMessage());
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    public static boolean isEgo(ItemInstance item) {
        return item != null && EgoWeaponDatabase.isEgoWeapon(item);
    }

    public static String form(ItemInstance item) {
        if (item == null)
            return "";

        String form = EgoWeaponDatabase.getFormType(item);
        if (form != null && form.trim().length() > 0)
            return normalize(form);

        return EgoWeaponTypeUtil.getType2(item);
    }

    public static int invGfx(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return 0;
        if (!isEgo(item))
            return item.getItem().getInvGfx();

        ViewInfo info = getView(item);
        if (info != null && info.invGfx > 0)
            return info.invGfx;

        return item.getItem().getInvGfx();
    }

    public static int groundGfx(ItemInstance item) {
        if (item == null || item.getItem() == null)
            return 0;
        if (!isEgo(item))
            return item.getItem().getGroundGfx();

        ViewInfo info = getView(item);
        if (info != null && info.groundGfx > 0)
            return info.groundGfx;

        return item.getItem().getGroundGfx();
    }

    /**
     * 아이템 객체의 바닥 표시 gfx를 현재 에고 형태에 맞춰 동기화한다.
     * object.gfx 필드가 protected라 reflection으로 안전하게 접근한다.
     */
    public static void applyGroundGfx(ItemInstance item) {
        if (item == null)
            return;
        try {
            Field field = lineage.world.object.object.class.getDeclaredField("gfx");
            field.setAccessible(true);
            field.setInt(item, groundGfx(item));
        } catch (Throwable e) {
            // 바닥 이미지 동기화 실패가 게임 진행을 막으면 안 된다.
        }
    }

    public static String name(ItemInstance item, String baseName) {
        if (item == null || baseName == null)
            return baseName;
        if (!isEgo(item))
            return baseName;
        if (baseName.indexOf("[에고:") >= 0)
            return baseName;

        EgoWeaponInfo ego = EgoWeaponDatabase.find(item);
        String label = label(item);
        String skill = skillName(item);
        int level = ego == null ? 1 : Math.max(1, ego.level);

        StringBuilder sb = new StringBuilder(baseName);
        sb.append(" [에고:").append(label).append(" Lv.").append(level);
        if (skill.length() > 0)
            sb.append(" ").append(skill);
        sb.append("]");
        return sb.toString();
    }

    public static String info(ItemInstance item) {
        if (item == null || !isEgo(item))
            return "";
        ViewInfo view = getView(item);
        String label = label(item);
        String text = view == null ? "" : safe(view.info);
        EgoWeaponInfo ego = EgoWeaponDatabase.find(item);
        int level = ego == null ? 1 : Math.max(1, ego.level);
        String skill = skillName(item);

        StringBuilder sb = new StringBuilder();
        sb.append("에고형태: ").append(label).append(" / 레벨: ").append(level);
        if (skill.length() > 0)
            sb.append(" / 능력: ").append(skill);
        if (text.length() > 0)
            sb.append(" / ").append(text);
        return sb.toString();
    }

    /**
     * 단일 에고 아이템의 인벤토리 아이콘/이름/상태와 바닥 gfx를 즉시 갱신한다.
     */
    public static void refreshInventory(PcInstance pc, ItemInstance item) {
        if (pc == null || item == null)
            return;
        try {
            applyGroundGfx(item);
            pc.toSender(S_InventoryDelete.clone(BasePacketPooling.getPool(S_InventoryDelete.class), item));
            pc.toSender(S_InventoryAdd.clone(BasePacketPooling.getPool(S_InventoryAdd.class), item));
            pc.toSender(S_InventoryStatus.clone(BasePacketPooling.getPool(S_InventoryStatus.class), item));
            if (item.isEquipped())
                pc.toSender(S_InventoryEquipped.clone(BasePacketPooling.getPool(S_InventoryEquipped.class), item));
        } catch (Throwable e) {
            EgoMessageUtil.danger(pc, "에고 인벤토리 표시 갱신 중 오류가 발생했습니다.");
        }
    }

    /**
     * 한 캐릭터 인벤토리 안의 모든 에고 아이템 표시를 갱신한다.
     * .에고리로드 후 서버 재시작 없이 즉시 반영하기 위한 메서드다.
     */
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

    /**
     * 접속 중인 전체 유저 인벤토리의 에고 아이템 표시를 갱신한다.
     * World.pc_list가 private라 reflection으로 읽는다. 실패 시 caller만 갱신한다.
     */
    @SuppressWarnings("unchecked")
    public static int refreshOnlineInventories(PcInstance caller) {
        int count = 0;
        List<PcInstance> pcs = new ArrayList<PcInstance>();

        try {
            Field field = World.class.getDeclaredField("pc_list");
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof List<?>) {
                synchronized (value) {
                    pcs.addAll((List<PcInstance>) value);
                }
            }
        } catch (Throwable e) {
            if (caller != null)
                pcs.add(caller);
        }

        for (PcInstance pc : pcs) {
            count += refreshPcInventory(pc);
        }
        return count;
    }

    public static String label(ItemInstance item) {
        ViewInfo info = getView(item);
        if (info != null && info.label.length() > 0)
            return info.label;
        return EgoWeaponTypeUtil.getDisplayTypeName(item);
    }

    private static String skillName(ItemInstance item) {
        EgoAbilityInfo skill = EgoWeaponDatabase.getFirstAbility(item);
        if (skill == null || skill.abilityType == null)
            return "";
        String type = skill.abilityType;
        if ("EGO_BALANCE".equals(type))
            return "공명";
        if ("BLOOD_DRAIN".equals(type))
            return "흡혈";
        if ("MANA_DRAIN".equals(type))
            return "흡마";
        if ("CRITICAL_BURST".equals(type))
            return "치명";
        if ("GUARDIAN_SHIELD".equals(type))
            return "수호";
        if ("AREA_SLASH".equals(type))
            return "광역";
        if ("EXECUTION".equals(type))
            return "처형";
        if ("FLAME_BRAND".equals(type))
            return "화염";
        if ("FROST_BIND".equals(type))
            return "서리";
        return type;
    }

    private static ViewInfo getView(ItemInstance item) {
        String form = form(item);
        if (form == null || form.length() == 0)
            return null;
        return viewMap.get(normalize(form));
    }

    private static void resetDefaults() {
        viewMap.clear();
        putDefault("dagger", "단검");
        putDefault("sword", "한손검");
        putDefault("tohandsword", "양손검");
        putDefault("axe", "도끼");
        putDefault("spear", "창");
        putDefault("bow", "활");
        putDefault("staff", "지팡이");
        putDefault("wand", "완드");
    }

    private static void putDefault(String form, String label) {
        ViewInfo info = new ViewInfo();
        info.form = form;
        info.label = label;
        info.info = "";
        viewMap.put(form, info);
    }

    private static String normalize(String value) {
        if (value == null)
            return "";
        value = value.trim().toLowerCase();
        if ("twohand_sword".equals(value) || "two_handed_sword".equals(value))
            return "tohandsword";
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ViewInfo {
        String form;
        String label;
        String info;
        int invGfx;
        int groundGfx;
    }
}
