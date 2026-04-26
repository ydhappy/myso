package lineage.world.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.database.DatabaseConnection;
import lineage.database.EgoWeaponDatabase;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 선택형 DB 대사팩.
 *
 * ego_talk_pack 테이블이 있으면 Java 기본 대사보다 먼저 사용한다.
 * 테이블이 없으면 조용히 기본 Java 대사로 fallback한다.
 */
public final class EgoTalkPack {

    private static final Map<String, List<String>> packMap = new ConcurrentHashMap<String, List<String>>();

    private EgoTalkPack() {
    }

    public static void reload(Connection con) {
        packMap.clear();
        load(con);
    }

    public static String find(PcInstance pc, ItemInstance weapon, String command) {
        if (pc == null || weapon == null || command == null)
            return null;
        if (packMap.isEmpty())
            return null;

        String genre = detectGenre(command);
        if (genre == null)
            return null;

        String tone = EgoWeaponDatabase.getTone(weapon);
        List<String> list = packMap.get(key(genre, tone));
        if (list == null || list.isEmpty())
            list = packMap.get(key(genre, "예의"));
        if (list == null || list.isEmpty())
            return null;

        return EgoTalkHistory.pick(pc, list.toArray(new String[list.size()]));
    }

    private static void load(Connection con) {
        PreparedStatement st = null;
        ResultSet rs = null;
        boolean closeCon = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getLineage();
                closeCon = true;
            }
            st = con.prepareStatement("SELECT genre, tone, message FROM ego_talk_pack WHERE use_yn=1 ORDER BY id ASC");
            rs = st.executeQuery();
            while (rs.next()) {
                String genre = normalizeGenre(rs.getString("genre"));
                String tone = EgoWeaponDatabase.normalizeTone(rs.getString("tone"));
                String message = rs.getString("message");
                if (genre == null || message == null || message.trim().length() == 0)
                    continue;
                String key = key(genre, tone);
                List<String> list = packMap.get(key);
                if (list == null) {
                    list = new ArrayList<String>();
                    packMap.put(key, list);
                }
                list.add(message.trim());
            }
        } catch (Exception e) {
            // ego_talk_pack 미적용 서버에서는 기본 Java 대사로 fallback한다.
        } finally {
            if (closeCon)
                DatabaseConnection.close(con, st, rs);
            else
                DatabaseConnection.close(st, rs);
        }
    }

    public static String detectGenre(String command) {
        if (command == null)
            return null;
        String t = command.trim().toLowerCase();
        if (containsAny(t, "드라마", "눈물", "감성", "운명", "배신", "재회")) return "드라마";
        if (containsAny(t, "영화", "명장면", "클라이맥스", "엔딩", "예고편", "스크린")) return "영화";
        if (containsAny(t, "웹툰", "회차", "컷", "작화", "댓글", "연재")) return "웹툰";
        if (containsAny(t, "로맨스", "사랑", "고백", "설렘", "첫사랑")) return "로맨스";
        if (containsAny(t, "액션", "폭발", "추격", "전투", "결투", "돌파")) return "액션";
        if (containsAny(t, "판타지", "마법", "용", "왕국", "던전", "마왕", "성검")) return "판타지";
        if (containsAny(t, "무협", "강호", "사부", "검기", "문파", "비급")) return "무협";
        if (containsAny(t, "공포", "귀신", "소름", "어둠", "괴담")) return "공포";
        if (containsAny(t, "코미디", "개그", "웃겨", "농담", "드립")) return "코미디";
        if (containsAny(t, "추리", "범인", "단서", "사건", "알리바이", "탐정")) return "추리";
        if (containsAny(t, "학원", "학교", "시험", "친구", "교실")) return "학원";
        if (containsAny(t, "일상", "힐링", "잔잔", "커피", "하루")) return "일상";
        if (containsAny(t, "빌런", "악역", "흑막", "배후", "타락")) return "빌런";
        if (containsAny(t, "주인공", "각성", "성장", "각오")) return "주인공";
        if (containsAny(t, "대사", "명대사", "한마디", "아무말", "아무거나", "추천")) return "아무";
        return null;
    }

    private static String normalizeGenre(String genre) {
        if (genre == null)
            return null;
        String g = genre.trim();
        if (g.length() == 0)
            return null;
        if ("힐링".equals(g))
            return "일상";
        if ("악역".equals(g))
            return "빌런";
        if ("각성".equals(g))
            return "주인공";
        return g;
    }

    private static String key(String genre, String tone) {
        return genre + ":" + tone;
    }

    private static boolean containsAny(String text, String... keys) {
        if (text == null)
            return false;
        for (int i = 0; i < keys.length; i++) {
            if (text.indexOf(keys[i]) >= 0)
                return true;
        }
        return false;
    }
}
