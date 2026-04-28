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
 * 클라이언트 출력 안정화:
 * - 클라 색상코드는 Java form-feed 문자(\f)가 아니라 실제 문자열 "\\fY" 형식으로 보낸다.
 * - HTML request 값은 null 대신 빈 문자열을 사용한다.
 * - egoletter.htm의 $0~$18 치환값을 항상 채운다.
 * - 패킷 실패 시 시스템 메시지로 fallback한다.
 */
public final class EgoMessageUtil {

    public static final String COLOR_NORMAL = "\\fY";
    public static final String COLOR_DANGER = "\\fR";
    public static final String COLOR_INFO = "\\fS";
    public static final String COLOR_WHITE = "\\fW";
    public static final String COLOR_GENRE = "\\fU";

    private static final String PREFIX = "[에고] ";
    private static final int BUBBLE_MAX_LENGTH = 72;
    private static final int LETTER_LINE_WIDTH = 46;
    private static final int LETTER_MAX_LINES = 18;
    private static final int LETTER_PARAM_COUNT = 19;
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

    /** 직접 시스템 메시지를 보낼 때도 색상코드를 클라이언트 형식으로 보정한다. */
    public static String clientColor(String value) {
        if (value == null)
            return "";
        return value.replace("\r\n", "\n").replace('\r', '\n').replace("\f", "\\f");
    }

    /** 짧으면 말풍선, 길면 편지형 HTML로 출력. */
    public static void reply(PcInstance pc, String color, String message) {
        if (pc == null || message == null)
            return;
        String msg = normalize(message);
        if (msg.length() == 0)
            return;

        if (isLongMessage(msg))
            letter(pc, "에고의 답장", msg);
        else
            bubble(pc, color, msg);
    }

    /** 본인에게만 보이는 에고 말풍선. 주변에는 방송하지 않는다. */
    public static void bubble(PcInstance pc, String color, String message) {
        if (pc == null || message == null)
            return;
        String msg = withPrefix(color, normalize(message));
        if (msg.length() == 0)
            return;
        try {
            pc.toSender(S_ObjectChatting.clone(BasePacketPooling.getPool(S_ObjectChatting.class), pc, Lineage.CHATTING_MODE_NORMAL, msg));
        } catch (Throwable e) {
            raw(pc, msg);
        }
    }

    /** 편지형 긴 메시지. 클라이언트 html/egoletter.htm 필요. */
    public static void letter(PcInstance pc, String title, String message) {
        if (pc == null || message == null)
            return;

        List<String> list = new ArrayList<String>();
        list.add(stripColor(normalize(title).length() == 0 ? "에고의 답장" : normalize(title)));
        list.addAll(splitLines(stripColor(message), LETTER_LINE_WIDTH, LETTER_MAX_LINES));
        padLetterParams(list);

        try {
            pc.toSender(S_Html.clone(BasePacketPooling.getPool(S_Html.class), pc, HTML_LETTER, "", list));
        } catch (Throwable e) {
            raw(pc, withPrefix(COLOR_INFO, message));
        }
    }

    /** 시스템 메시지 fallback. */
    public static void raw(PcInstance pc, String message) {
        if (pc == null || message == null)
            return;
        String msg = normalize(clientColor(message));
        if (msg.length() == 0)
            return;
        try {
            ChattingController.toChatting(pc, msg, Lineage.CHATTING_MODE_MESSAGE);
        } catch (Throwable e) {
        }
    }

    /** 일반 채팅으로 입력된 에고 호출을 외부 방송하지 않고 소비한다. */
    public static boolean consumeNormalChat() {
        return true;
    }

    private static boolean isLongMessage(String msg) {
        return msg.length() > BUBBLE_MAX_LENGTH || msg.indexOf('\n') >= 0 || msg.indexOf(" / ") >= 0 || msg.indexOf("==========") >= 0;
    }

    private static String withPrefix(String color, String message) {
        String msg = normalize(clientColor(message));
        if (msg.length() == 0)
            return "";
        String c = normalizeColor(color);
        if (c.length() == 0)
            c = COLOR_NORMAL;

        if (containsEgoPrefix(msg)) {
            if (hasColor(msg))
                return clientColor(msg);
            return c + msg;
        }

        if (hasColor(msg)) {
            String actualColor = normalizeColor(msg);
            String body = msg.substring(colorLength(msg)).trim();
            return actualColor + PREFIX + body;
        }
        return c + PREFIX + msg;
    }

    private static boolean containsEgoPrefix(String msg) {
        return msg != null && msg.indexOf("[에고]") >= 0;
    }

    private static boolean hasColor(String message) {
        return colorLength(message) > 0;
    }

    private static int colorLength(String value) {
        if (value == null)
            return 0;
        if (value.length() >= 3 && value.charAt(0) == '\\' && value.charAt(1) == 'f')
            return 3;
        if (value.length() >= 2 && value.charAt(0) == '\f')
            return 2;
        return 0;
    }

    private static String normalizeColor(String value) {
        if (value == null)
            return "";
        String v = clientColor(value);
        if (v.length() >= 3 && v.charAt(0) == '\\' && v.charAt(1) == 'f')
            return v.substring(0, 3);
        return "";
    }

    private static String stripColor(String value) {
        if (value == null)
            return "";
        String v = clientColor(value);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length();) {
            if (i + 2 < v.length() && v.charAt(i) == '\\' && v.charAt(i + 1) == 'f') {
                i += 3;
                continue;
            }
            sb.append(v.charAt(i));
            i++;
        }
        return sb.toString().replace("[에고] ", "").replace("[에고]", "").trim();
    }

    private static String normalize(String value) {
        if (value == null)
            return "";
        return clientColor(value).replaceAll("\n{3,}", "\n\n").trim();
    }

    private static void padLetterParams(List<String> list) {
        if (list == null)
            return;
        while (list.size() < LETTER_PARAM_COUNT)
            list.add("");
        while (list.size() > LETTER_PARAM_COUNT)
            list.remove(list.size() - 1);
    }

    private static List<String> splitLines(String text, int width, int maxLines) {
        List<String> out = new ArrayList<String>();
        if (text == null)
            return out;
        if (width <= 0)
            width = LETTER_LINE_WIDTH;
        if (maxLines <= 0)
            maxLines = LETTER_MAX_LINES;

        String[] rawLines = clientColor(text).replace("\r\n", "\n").replace("\r", "").split("\n");
        for (int i = 0; i < rawLines.length; i++) {
            String line = rawLines[i] == null ? "" : rawLines[i].trim();
            while (line.length() > width) {
                int cut = findCutPoint(line, width);
                out.add(line.substring(0, cut).trim());
                line = line.substring(cut).trim();
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

    private static int findCutPoint(String line, int width) {
        if (line == null || line.length() <= width)
            return line == null ? 0 : line.length();
        int cut = line.lastIndexOf(' ', width);
        if (cut <= width / 2)
            cut = width;
        return Math.max(1, cut);
    }
}
