package lineage.world.controller;

import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 메시지 클래스.
 *
 * 적용 코드에서는 EgoMessageUtil 대신 EgoMsg 사용을 권장합니다.
 */
public final class EgoMsg {

    public static final String NORMAL = EgoMessageUtil.COLOR_NORMAL;
    public static final String DANGER = EgoMessageUtil.COLOR_DANGER;
    public static final String INFO = EgoMessageUtil.COLOR_INFO;
    public static final String WHITE = EgoMessageUtil.COLOR_WHITE;

    private EgoMsg() {
    }

    public static void normal(PcInstance pc, String message) {
        EgoMessageUtil.normal(pc, message);
    }

    public static void danger(PcInstance pc, String message) {
        EgoMessageUtil.danger(pc, message);
    }

    public static void info(PcInstance pc, String message) {
        EgoMessageUtil.info(pc, message);
    }

    public static void raw(PcInstance pc, String message) {
        EgoMessageUtil.raw(pc, message);
    }

    public static void privateMessage(PcInstance pc, String color, String message) {
        EgoMessageUtil.privateMessage(pc, color, message);
    }

    public static boolean consume() {
        return EgoMessageUtil.consumeNormalChat();
    }
}
