package lineage.world.controller;

import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 대화 클래스.
 *
 * ChattingController 연결 시 EgoWeaponControlController 대신 EgoTalk 사용을 권장합니다.
 */
public final class EgoTalk {

    private EgoTalk() {
    }

    /**
     * 일반채팅 처리.
     * true 반환 시 해당 채팅은 주변에 방송하지 않고 소비해야 합니다.
     */
    public static boolean chat(PcInstance pc, String msg) {
        return EgoWeaponControlController.onNormalChat(pc, msg);
    }

    /**
     * 자동 경고 체크.
     */
    public static void warning(PcInstance pc) {
        EgoWeaponControlController.checkAutoWarning(pc);
    }
}
