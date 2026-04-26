package lineage.world.controller;

import lineage.database.EgoWeaponDatabase;
import lineage.util.Util;
import lineage.world.object.instance.ItemInstance;

/**
 * 에고 장르대화 안내/추천 헬퍼.
 */
public final class EgoGenreGuide {

    private static final String TONE_RUDE = "예의반대";

    private EgoGenreGuide() {
    }

    public static boolean isGuideRequest(String command) {
        if (command == null)
            return false;
        String t = normalize(command);
        return containsAny(t, "장르목록", "대사목록", "대화목록", "장르 뭐", "장르 알려", "대사 뭐", "뭐 가능", "무슨 대사", "대화 추천");
    }

    public static String guide(ItemInstance weapon) {
        String tone = EgoWeaponDatabase.getTone(weapon);
        if (isRude(tone)) {
            return "가능한 장르는 이거다: 드라마, 영화, 웹툰, 로맨스, 액션, 판타지, 무협, 공포, 코미디, 추리, 학원, 일상/힐링, 빌런, 주인공각성. 예: '드라마 대사 해줘', '아무 대사나 해줘'.";
        }
        return "가능한 장르는 드라마, 영화, 웹툰, 로맨스, 액션, 판타지, 무협, 공포, 코미디, 추리, 학원, 일상/힐링, 빌런, 주인공각성입니다. 예: '드라마 대사 해줘', '아무 대사나 해줘'.";
    }

    public static String suggest(ItemInstance weapon) {
        String tone = EgoWeaponDatabase.getTone(weapon);
        String[] polite = new String[] {
            "오늘은 드라마풍 대사로 분위기를 잡아보셔도 좋겠습니다.",
            "전투가 많다면 액션 또는 무협 대사가 잘 어울립니다.",
            "긴 사냥 중이라면 일상/힐링 대사로 잠시 호흡을 고르는 것도 좋습니다.",
            "위험한 사냥터라면 공포풍 대사가 의외로 잘 맞습니다.",
            "성장 구간이라면 주인공각성 대사를 추천드립니다."
        };
        String[] rude = new String[] {
            "오늘은 액션이나 무협으로 가라. 멍때리는 분위기는 아니니까.",
            "사냥 지루하면 웹툰풍이나 코미디 대사 시켜. 졸다 눕지 말고.",
            "피곤하면 힐링 대사나 들어. 그래도 몹 오면 바로 움직여라.",
            "각성 대사 듣고 싶으면 먼저 성장부터 해. 그래도 분위기는 내줄게.",
            "공포 대사? 네 HP 낮을 때가 제일 공포다."
        };
        String[] arr = isRude(tone) ? rude : polite;
        return arr[Util.random(0, arr.length - 1)];
    }

    private static boolean isRude(String tone) {
        return TONE_RUDE.equals(EgoWeaponDatabase.normalizeTone(tone));
    }

    private static String normalize(String value) {
        if (value == null)
            return "";
        return value.trim().replace("?", "").replace("!", "").replace("~", "").replace(".", "").toLowerCase();
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
