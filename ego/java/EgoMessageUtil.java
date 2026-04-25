package lineage.world.controller;

import lineage.share.Lineage;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 개인 메시지/색상 유틸.
 *
 * 목적:
 * - 사용자는 일반 채팅으로 에고를 호출한다.
 * - 에고 응답은 일반 채팅으로 방송하지 않고 본인에게만 보낸다.
 * - 다른 캐릭터에게 에고 대화가 보이지 않게 한다.
 * - 색상 코드를 한 곳에서 관리한다.
 *
 * 중요:
 * - CHATTING_MODE_NORMAL 또는 일반채팅 패킷으로 보내면 주변 캐릭터에게 보일 수 있다.
 * - 반드시 CHATTING_MODE_MESSAGE 계열의 개인 시스템 메시지로 보낸다.
 */
public final class EgoMessageUtil {

    public static final String COLOR_NORMAL = "\\fY";
    public static final String COLOR_DANGER = "\\fR";
    public static final String COLOR_INFO = "\\fS";
    public static final String COLOR_WHITE = "\\fW";

    private static final String PREFIX = "[에고] ";

    private EgoMessageUtil() {
    }

    /**
     * 에고 기본 개인 메시지.
     */
    public static void normal(PcInstance pc, String message) {
        privateMessage(pc, COLOR_NORMAL, message);
    }

    /**
     * 위험/경고 개인 메시지.
     */
    public static void danger(PcInstance pc, String message) {
        privateMessage(pc, COLOR_DANGER, message);
    }

    /**
     * 정보/진단 개인 메시지.
     */
    public static void info(PcInstance pc, String message) {
        privateMessage(pc, COLOR_INFO, message);
    }

    /**
     * 이미 색상코드가 포함된 메시지를 본인에게만 보낸다.
     */
    public static void raw(PcInstance pc, String message) {
        if (pc == null || message == null)
            return;
        ChattingController.toChatting(pc, message, Lineage.CHATTING_MODE_MESSAGE);
    }

    /**
     * 색상 자동 주입 + [에고] 접두어 자동 보정 + 개인 메시지 전송.
     */
    public static void privateMessage(PcInstance pc, String color, String message) {
        if (pc == null || message == null)
            return;

        String msg = message.trim();
        if (msg.length() == 0)
            return;

        String c = color == null || color.length() == 0 ? COLOR_NORMAL : color;

        if (!hasColor(msg))
            msg = c + msg;

        if (!msg.contains("[에고]")) {
            if (hasColor(msg))
                msg = msg.substring(0, 3) + PREFIX + msg.substring(3);
            else
                msg = c + PREFIX + msg;
        }

        ChattingController.toChatting(pc, msg, Lineage.CHATTING_MODE_MESSAGE);
    }

    /**
     * 일반 채팅으로 입력된 에고 호출을 외부 방송하지 않고 소비한다는 의미를 명확히 하기 위한 메서드.
     */
    public static boolean consumeNormalChat() {
        return true;
    }

    private static boolean hasColor(String message) {
        return message.startsWith("\\f") && message.length() >= 3;
    }
}
