package lineage.world.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.share.Lineage;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 형태변환 컨트롤러.
 *
 * 핵심:
 * - 인벤토리의 다른 무기로 교체하지 않는다.
 * - 현재 착용 중인 에고무기 자체의 "에고 형태"를 DB/캐시에 저장한다.
 * - EgoWeaponTypeUtil은 이 형태값을 우선 무기종류로 인식한다.
 * - 활/양손검/창/지팡이 형태는 방패를 자동 해제한다.
 * - 한손검/단검/완드 형태는 이전에 해제한 방패를 자동 복구한다.
 *
 * 일반채팅 예:
 * - 카르마 활
 * - 카르마 양검
 * - 카르마 양손검
 * - 카르마 한검
 * - 카르마 한손검
 * - 카르마 단검
 * - 카르마 창
 * - 카르마 도끼
 * - 카르마 지팡이
 * - 카르마 완드
 */
public final class EgoWeaponFormController {

    private static final long FORM_DELAY_MS = 1000L;
    private static final Map<Long, Long> delayMap = new ConcurrentHashMap<Long, Long>();

    private EgoWeaponFormController() {
    }

    public static boolean handleTalk(PcInstance pc, ItemInstance egoWeapon, String command) {
        if (pc == null || egoWeapon == null || command == null)
            return false;

        String formType = parseFormType(command);
        if (formType == null)
            return false;

        if (!checkDelay(pc))
            return true;

        transform(pc, egoWeapon, formType);
        return true;
    }

    public static void transform(PcInstance pc, ItemInstance egoWeapon, String formType) {
        if (pc == null || egoWeapon == null || formType == null)
            return;

        if (!EgoWeaponDatabase.isEgoWeapon(egoWeapon)) {
            EgoMessageUtil.danger(pc, "먼저 .에고생성 이름 으로 에고무기를 활성화해야 합니다.");
            return;
        }

        if (!EgoWeaponTypeUtil.isSupportedType(formType)) {
            EgoMessageUtil.danger(pc, "지원하지 않는 에고 형태입니다: " + formType);
            return;
        }

        Inventory inv = pc.getInventory();
        if (inv == null) {
            EgoMessageUtil.danger(pc, "인벤토리를 확인할 수 없어 형태변환을 중단합니다.");
            return;
        }

        ItemInstance equippedWeapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (equippedWeapon == null || equippedWeapon.getObjectId() != egoWeapon.getObjectId()) {
            EgoMessageUtil.danger(pc, "현재 착용 중인 에고무기만 변신할 수 있습니다.");
            return;
        }

        long rememberedShieldObjId = 0;
        ItemInstance shield = inv.getSlot(Lineage.SLOT_SHIELD);
        if (isNoShieldForm(formType) && shield != null) {
            rememberedShieldObjId = shield.getObjectId();
            safeClick(pc, shield);
            if (inv.getSlot(Lineage.SLOT_SHIELD) != null) {
                EgoMessageUtil.danger(pc, "방패 해제에 실패했습니다. 저주/고정 착용 상태를 확인하세요.");
                return;
            }
            EgoMessageUtil.info(pc, String.format("%s 형태 변신을 위해 방패를 해제했습니다.", displayForm(formType)));
        }

        if (!EgoWeaponDatabase.setForm(egoWeapon, formType, rememberedShieldObjId)) {
            EgoMessageUtil.danger(pc, "에고 형태 저장에 실패했습니다. DB 컬럼 적용 여부를 확인하세요.");
            return;
        }

        if (!isNoShieldForm(formType)) {
            restoreShieldIfPossible(pc, inv, egoWeapon);
        }

        EgoMessageUtil.normal(pc, String.format("에고가 %s 형태로 변신했습니다.", displayForm(formType)));
    }

    private static void restoreShieldIfPossible(PcInstance pc, Inventory inv, ItemInstance egoWeapon) {
        if (pc == null || inv == null || egoWeapon == null)
            return;

        if (inv.getSlot(Lineage.SLOT_SHIELD) != null)
            return;

        long shieldObjId = EgoWeaponDatabase.getPrevShieldObjId(egoWeapon);
        if (shieldObjId <= 0)
            return;

        ItemInstance shield = inv.findByObjId(shieldObjId);
        if (shield == null)
            return;

        safeClick(pc, shield);
        if (inv.getSlot(Lineage.SLOT_SHIELD) != null && inv.getSlot(Lineage.SLOT_SHIELD).getObjectId() == shield.getObjectId()) {
            EgoMessageUtil.info(pc, String.format("이전에 사용하던 방패 %s 를 다시 착용했습니다.", getItemName(shield)));
            EgoWeaponDatabase.setForm(egoWeapon, EgoWeaponDatabase.getFormType(egoWeapon), 0);
        }
    }

    private static String parseFormType(String command) {
        String text = normalize(command);
        if (text.length() == 0)
            return null;

        text = text.replace("으로", "").replace("로", "").replace("변신", "").replace("형태", "").replace("모드", "").replace("바꿔", "").replace("바꿔줘", "").replace("변경", "").trim();

        if (containsAny(text, "활", "보우", "bow"))
            return EgoWeaponTypeUtil.TYPE_BOW;
        if (containsAny(text, "양검", "양손검", "대검", "투핸드", "twohand", "tohandsword"))
            return EgoWeaponTypeUtil.TYPE_TWO_HAND_SWORD;
        if (containsAny(text, "한검", "한손검", "검", "sword"))
            return EgoWeaponTypeUtil.TYPE_SWORD;
        if (containsAny(text, "단검", "대거", "dagger"))
            return EgoWeaponTypeUtil.TYPE_DAGGER;
        if (containsAny(text, "도끼", "axe"))
            return EgoWeaponTypeUtil.TYPE_AXE;
        if (containsAny(text, "창", "spear"))
            return EgoWeaponTypeUtil.TYPE_SPEAR;
        if (containsAny(text, "지팡이", "스태프", "staff"))
            return EgoWeaponTypeUtil.TYPE_STAFF;
        if (containsAny(text, "완드", "wand"))
            return EgoWeaponTypeUtil.TYPE_WAND;

        return null;
    }

    public static boolean isNoShieldForm(String formType) {
        String type = normalize(formType);
        return EgoWeaponTypeUtil.TYPE_BOW.equals(type)
            || EgoWeaponTypeUtil.TYPE_TWO_HAND_SWORD.equals(type)
            || EgoWeaponTypeUtil.TYPE_AXE.equals(type)
            || EgoWeaponTypeUtil.TYPE_SPEAR.equals(type)
            || EgoWeaponTypeUtil.TYPE_STAFF.equals(type);
    }

    public static String displayForm(String formType) {
        String type = normalize(formType);
        if (EgoWeaponTypeUtil.TYPE_DAGGER.equals(type))
            return "단검";
        if (EgoWeaponTypeUtil.TYPE_SWORD.equals(type))
            return "한손검";
        if (EgoWeaponTypeUtil.TYPE_TWO_HAND_SWORD.equals(type))
            return "양손검";
        if (EgoWeaponTypeUtil.TYPE_AXE.equals(type))
            return "도끼";
        if (EgoWeaponTypeUtil.TYPE_SPEAR.equals(type))
            return "창";
        if (EgoWeaponTypeUtil.TYPE_BOW.equals(type))
            return "활";
        if (EgoWeaponTypeUtil.TYPE_STAFF.equals(type))
            return "지팡이";
        if (EgoWeaponTypeUtil.TYPE_WAND.equals(type))
            return "완드";
        return type;
    }

    private static void safeClick(PcInstance pc, ItemInstance item) {
        if (pc == null || item == null)
            return;
        try {
            item.toClick(pc, null);
        } catch (Throwable e) {
            EgoMessageUtil.danger(pc, String.format("%s 착용/해제 처리 중 오류가 발생했습니다.", getItemName(item)));
        }
    }

    private static boolean checkDelay(PcInstance pc) {
        long now = java.lang.System.currentTimeMillis();
        Long last = delayMap.get(pc.getObjectId());
        if (last != null && now - last.longValue() < FORM_DELAY_MS)
            return false;
        delayMap.put(pc.getObjectId(), now);
        return true;
    }

    private static boolean containsAny(String text, String... keys) {
        if (text == null)
            return false;
        for (String key : keys) {
            if (text.contains(key))
                return true;
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null)
            return "";
        return value.trim().toLowerCase();
    }

    private static String getItemName(ItemInstance item) {
        if (item == null)
            return "알 수 없는 아이템";
        if (item.getName() != null)
            return item.getName();
        if (item.getItem() != null && item.getItem().getName() != null)
            return item.getItem().getName();
        return "알 수 없는 아이템";
    }
}
