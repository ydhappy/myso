package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

import lineage.network.packet.BasePacketPooling;
import lineage.network.packet.server.S_Html;
import lineage.network.packet.server.S_ObjectChatting;
import lineage.share.Lineage;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 메시지 유틸.
 *
 * 최종 출력 정책:
 * - 사용자의 에고 호출 일반채팅은 외부 방송하지 않고 소비한다.
 * - 짧은 에고 대답은 말풍선 패킷으로 본인에게만 보낸다.
 * - 긴 상태/도움말/커맨드 결과는 편지처럼 보이도록 HTML 창으로 보낸다.
 * - HTML 템플릿이 없을 경우에도 시스템 메시지 fallback으로 내용은 확인 가능하다.
 */
public final class EgoMessageUtil {

    public static final String COLOR_NORMAL = "\fY";
    public static final String COLOR_DANGER = "\fR";
    public static final String COLOR_INFO = "\fS";
    public static final String COLOR_WHITE = "\fW";
    public static final String COLOR_GENRE = "\fU";

    private static final String PREFIX = "[에고] ";
    private static final int BUBBLE_MAX_LENGTH = 72;
    private static final String HTML_LETTER = "egoletter";

    private EgoMessageUtil() {
    }

    public static void normal(PcInstance pc, String message) {
        reply(pc, COLOR_NORMAL, message);
    }

    public static void danger(PcInstance pc, String message) {
        reply(pc, COLOR_DANGER, message);
    }

    public static void info(PcInstance pc, String message) {
        reply(pc, COLOR_INFO, message);
    }

    /** 장르/감성 대사용 색상. 클라이언트가 \fU를 지원하지 않으면 일반색처럼 표시된다. */
    public static void genre(PcInstance pc, String message) {
        reply(pc, COLOR_GENRE, message);
    }

    /** 짧으면 말풍선, 길면 편지형 HTML로 출력. */
    public static void reply(PcInstance pc, String color, String message) {
        if (pc == null || message == null)
            return;
        String msg = message.trim();
        if (msg.length() == 0)
            return;

        if (isLongMessage(msg)) {
            letter(pc, "에고의 답장", msg);
        } else {
            bubble(pc, color, msg);
        }
    }

    /** 본인에게만 보이는 에고 말풍선. 주변에는 방송하지 않는다. */
    public static void bubble(PcInstance pc, String color, String message) {
        if (pc == null || message == null)
            return;
        String msg = withPrefix(color, message.trim());
        pc.toSender(S_ObjectChatting.clone(BasePacketPooling.getPool(S_ObjectChatting.class), pc, Lineage.CHATTING_MODE_NORMAL, msg));
    }

    /** 편지형 긴 메시지. 클라이언트 html/egoletter.htm 필요. */
    public static void letter(PcInstance pc, String title, String message) {
        if (pc == null || message == null)
            return;

        List<String> list = new ArrayList<String>();
        list.add(title == null || title.trim().length() == 0 ? "에고의 답장" : title.trim());
        list.addAll(splitLines(stripColor(message), 46, 18));

        try {
            pc.toSender(S_Html.clone(BasePacketPooling.getPool(S_Html.class), pc, HTML_LETTER, null, list));
        } catch (Throwable e) {
            raw(pc, withPrefix(COLOR_INFO, message));
        }
    }

    /** 시스템 메시지 fallback. */
    public static void raw(PcInstance pc, String message) {
        if (pc == null || message == null)
            return;
        ChattingController.toChatting(pc, message, Lineage.CHATTING_MODE_MESSAGE);
    }

    /** 일반 채팅으로 입력된 에고 호출을 외부 방송하지 않고 소비한다. */
    public static boolean consumeNormalChat() {
        return true;
    }

    private static boolean isLongMessage(String msg) {
        return msg.length() > BUBBLE_MAX_LENGTH || msg.indexOf('\n') >= 0 || msg.indexOf(" / ") >= 0 || msg.indexOf("==========") >= 0;
    }

    private static String withPrefix(String color, String message) {
        String msg = message == null ? "" : message.trim();
        String c = color == null || color.length() == 0 ? COLOR_NORMAL : color;
        if (!hasColor(msg))
            msg = c + msg;
        if (!msg.contains("[에고]")) {
            if (hasColor(msg))
                msg = msg.substring(0, 3) + PREFIX + msg.substring(3);
            else
                msg = c + PREFIX + msg;
        }
        return msg;
    }

    private static boolean hasColor(String message) {
        return message != null && message.startsWith("\f") && message.length() >= 3;
    }

    private static String stripColor(String value) {
        if (value == null)
            return "";
        return value.replaceAll("\\f.", "").replace("[에고] ", "").trim();
    }

    private static List<String> splitLines(String text, int width, int maxLines) {
        List<String> out = new ArrayList<String>();
        if (text == null)
            return out;
        String[] rawLines = text.replace("\r", "").split("\n");
        for (String raw : rawLines) {
            String line = raw == null ? "" : raw.trim();
            while (line.length() > width) {
                out.add(line.substring(0, width));
                line = line.substring(width).trim();
                if (out.size() >= maxLines)
                    return out;
            }
            if (line.length() > 0)
                out.add(line);
            if (out.size() >= maxLines)
                return out;
        }
        if (out.isEmpty())
            out.add("...");
        return out;
    }
}
