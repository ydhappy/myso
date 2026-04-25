package lineage.world.controller;

import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 변형 컨트롤러 - 기능 제거 버전.
 *
 * 정책:
 * - 무기변형 완전 삭제.
 * - ego.form 값을 전투/표시/방패처리에 사용하지 않는다.
 * - PcInstance / DamageController / item.type2 / 방패 장착 상태를 건드리지 않는다.
 * - 기존 코드 호환을 위해 클래스와 메서드만 유지한다.
 */
public final class EgoWeaponFormController {

    private EgoWeaponFormController() {
    }

    /**
     * 무기변형 명령은 더 이상 처리하지 않는다.
     * 항상 false를 반환하여 일반 대화 처리로 넘긴다.
     */
    public static boolean handleTalk(PcInstance pc, ItemInstance egoWeapon, String command) {
        return false;
    }

    /**
     * 외부 호환용. 실제 변형은 수행하지 않는다.
     */
    public static void transform(PcInstance pc, ItemInstance egoWeapon, String formType) {
        if (pc != null)
            EgoMessageUtil.info(pc, "에고 무기변형 기능은 제거되었습니다. 실제 무기 타입과 표시 타입은 원본 무기 기준입니다.");
    }

    public static boolean isNoShieldForm(String formType) {
        return false;
    }

    public static String displayForm(String formType) {
        return formType == null ? "" : formType;
    }
}
