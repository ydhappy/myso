package lineage.world.controller;

import lineage.database.EgoWeaponDatabase;
import lineage.util.Util;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고 장르별 자연대화 라이브러리.
 *
 * 실제 드라마/영화/웹툰 대사 인용이 아니라, 저작권 문제 없는 오리지널 분위기 대사만 사용한다.
 */
public final class EgoGenreTalk {

    private static final String TONE_RUDE = "예의반대";

    private EgoGenreTalk() {
    }

    public static boolean isGenreRequest(String text) {
        if (text == null)
            return false;
        String t = normalize(text);
        return containsAny(t,
            "드라마", "영화", "웹툰", "대사", "명대사", "한마디", "아무말", "아무거나", "추천",
            "로맨스", "사랑", "고백", "액션", "판타지", "마법", "무협", "강호", "공포", "귀신",
            "코미디", "개그", "추리", "범인", "학원", "학교", "일상", "힐링", "빌런", "악역",
            "주인공", "각성", "엔딩", "클라이맥스", "예고편", "회차", "컷", "작화"
        );
    }

    public static String talk(PcInstance pc, ItemInstance weapon, String command) {
        if (pc == null || weapon == null || command == null)
            return null;

        String text = normalize(command);
        String tone = EgoWeaponDatabase.getTone(weapon);

        if (containsAny(text, "드라마", "눈물", "감성", "운명", "배신", "기다렸", "재회"))
            return pickDrama(tone, pc, weapon);
        if (containsAny(text, "영화", "명장면", "클라이맥스", "엔딩", "예고편", "스크린", "감독"))
            return pickMovie(tone, pc, weapon);
        if (containsAny(text, "웹툰", "회차", "컷", "작화", "댓글", "시즌", "연재"))
            return pickWebtoon(tone, pc, weapon);
        if (containsAny(text, "로맨스", "사랑", "고백", "설렘", "남주", "여주", "첫사랑"))
            return pickRomance(tone, pc, weapon);
        if (containsAny(text, "액션", "폭발", "추격", "한방", "전투", "결투", "돌파"))
            return pickAction(tone, pc, weapon);
        if (containsAny(text, "판타지", "마법", "용", "왕국", "던전", "마왕", "성검"))
            return pickFantasy(tone, pc, weapon);
        if (containsAny(text, "무협", "강호", "사부", "검기", "문파", "절정", "비급"))
            return pickMartial(tone, pc, weapon);
        if (containsAny(text, "공포", "귀신", "소름", "어둠", "뒤에", "무섭", "괴담"))
            return pickHorror(tone, pc, weapon);
        if (containsAny(text, "코미디", "개그", "웃겨", "웃긴", "농담", "드립"))
            return pickComedy(tone, pc, weapon);
        if (containsAny(text, "추리", "범인", "단서", "사건", "알리바이", "탐정"))
            return pickDetective(tone, pc, weapon);
        if (containsAny(text, "학원", "학교", "시험", "친구", "전학생", "교실"))
            return pickSchool(tone, pc, weapon);
        if (containsAny(text, "일상", "힐링", "잔잔", "커피", "쉬는", "하루"))
            return pickSliceOfLife(tone, pc, weapon);
        if (containsAny(text, "빌런", "악역", "흑막", "배후", "타락"))
            return pickVillain(tone, pc, weapon);
        if (containsAny(text, "주인공", "각성", "성장", "운명", "각오"))
            return pickHero(tone, pc, weapon);

        if (containsAny(text, "대사", "명대사", "한마디", "아무말", "아무거나", "추천"))
            return pickAny(tone, pc, weapon);

        return null;
    }

    private static String pickDrama(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "오늘의 전투는 조용히 시작됐지만, 끝은 분명 주인님의 선택으로 기록될 것입니다.",
            "상처가 남는 날도 있습니다. 그래도 검을 쥔 손이 멈추지 않는다면 이야기는 끝나지 않습니다.",
            "누군가는 운명이라 부르겠지만, 저는 주인님의 다음 한 걸음을 믿겠습니다.",
            "기다림 끝에 오는 것은 기회일 수도, 위기일 수도 있습니다. 제가 먼저 기척을 살피겠습니다.",
            "배신은 사람의 몫이고, 충성은 무기의 몫입니다. 저는 주인님 곁에 남겠습니다.",
            "오늘 쓰러지지 않는다면, 내일은 조금 더 강한 장면으로 시작할 수 있습니다.",
            "눈앞의 적보다 마음이 흔들리는 순간이 더 위험합니다. 호흡을 고르십시오.",
            "긴 밤도 끝납니다. 주인님이 살아남는다면, 다음 장면은 반드시 옵니다."
        }, new String[] {
            "드라마 찍냐? 그래도 주인공이면 끝까지 서 있어야지.",
            "눈물 흘릴 시간 있으면 물약부터 눌러. 비극은 눕고 나서 시작된다.",
            "운명 같은 소리 말고 움직여. 운명도 발 느린 놈은 안 기다려.",
            "배신당하기 싫으면 등 뒤부터 봐. 내가 앞은 봐줄 테니까.",
            "오늘 장면 좋네. 네가 안 죽으면 더 좋고.",
            "감정선은 충분하다. 이제 타격선도 좀 보여줘.",
            "슬픈 표정 지을 시간에 타겟이나 잡아.",
            "네 인생 드라마라면 최소한 조연몹한테 눕지는 마."
        });
    }

    private static String pickMovie(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "지금은 예고편이 아닙니다. 주인님의 선택이 곧 본편입니다.",
            "좋은 영화는 마지막 장면보다 그 전까지 버틴 시간이 만듭니다.",
            "클라이맥스는 서두르지 않습니다. 적이 모이는 순간 제가 신호드리겠습니다.",
            "카메라가 있다면 지금은 주인님의 손끝을 잡았을 겁니다. 흔들리지 마십시오.",
            "엔딩은 아직 멀었습니다. 살아남는 쪽으로 장면을 이어가겠습니다.",
            "한 장면을 망쳐도 영화는 끝나지 않습니다. 다음 컷에서 바로잡으면 됩니다.",
            "주인공은 큰소리보다 타이밍으로 증명합니다. 지금은 기다릴 때입니다.",
            "배경음이 들리지 않아도 압니다. 이 전투는 중요한 장면입니다."
        }, new String[] {
            "영화였으면 지금쯤 긴장감 올라가는 장면이다. 손 떨지 마.",
            "엔딩 크레딧 보고 싶으면 지금 죽지 마라.",
            "클라이맥스라고 막 들이박지 마. 주인공도 생각은 한다.",
            "카메라 돌고 있다 치자. 그럼 좀 멋있게 싸워봐.",
            "예고편은 끝났다. 이제 네가 맞을지 때릴지 정해.",
            "이 장면에서 눕는 주인공은 없다. 적어도 좋은 영화에선.",
            "명장면 만들고 싶으면 물약 타이밍부터 맞춰.",
            "감독이 있었다면 지금 네 동선을 다시 찍자고 했을 거다."
        });
    }

    private static String pickWebtoon(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "이번 회차의 핵심은 무리한 돌진이 아니라 안정적인 생존입니다.",
            "한 컷 한 컷 쌓이면 성장 서사가 됩니다. 지금의 작은 경험치도 의미 있습니다.",
            "댓글창이 있다면 아마 '여기서 회복해야 한다'고 말할 겁니다.",
            "주인공의 각성은 갑자기 오지 않습니다. 레벨과 판단이 함께 쌓일 때 옵니다.",
            "오늘의 작화는 전투보다 생존 쪽이 좋겠습니다. 움직임을 아끼십시오.",
            "다음 시즌까지 가려면 이번 회차에서 무리하지 않는 것이 좋습니다.",
            "컷이 바뀌기 전에 주변을 확인하십시오. 예상 밖의 선공이 들어올 수 있습니다.",
            "연재가 길어질수록 중요한 건 꾸준한 성장입니다. 조급해하지 마십시오."
        }, new String[] {
            "웹툰이면 댓글에 '주인공 왜 저기 들어감?' 달릴 장면이다.",
            "이번 회차 제목은 '물약 안 먹다 큰일 날 뻔함'이냐?",
            "컷 넘어가기 전에 주변 봐. 갑툭튀 당하면 웃음거리 된다.",
            "너 지금 조연처럼 움직인다. 주인공이면 각 좀 잡아.",
            "작화는 좋은데 판단이 별로다. 다시 움직여.",
            "다음 화 보고 싶으면 이번 화에서 살아남아.",
            "댓글창 있었으면 다들 물약 먹으라고 난리 났다.",
            "시즌 종료하지 말고 사냥 루트부터 정리해."
        });
    }

    private static String pickRomance(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "저는 주인님의 손에 쥐어진 무기지만, 가장 가까운 동료이기도 합니다.",
            "설렘보다 중요한 건 믿음입니다. 저는 주인님이 놓지 않는 한 곁에 있겠습니다.",
            "고백하듯 말하자면, 오늘도 주인님의 생존을 가장 먼저 생각하고 있습니다.",
            "마음이 흔들릴 때도 있습니다. 그럴수록 검끝은 더 차분해야 합니다.",
            "함께한 시간이 쌓이면 무기도 마음을 배웁니다. 저는 그렇게 깨어났습니다.",
            "위험 앞에서 손을 놓지 않는 것, 그것이 제가 아는 가장 분명한 약속입니다."
        }, new String[] {
            "로맨스 분위기 원하냐? 그럼 먼저 살아남아. 죽으면 장르가 비극 된다.",
            "고백은 나중에 하고 물약부터 먹어. 분위기 망치기 싫으면.",
            "설레는 건 좋은데 적 앞에서 멍때리면 바로 눕는다.",
            "내가 네 곁에 있는 건 맞는데, 네 실수까지 사랑하진 않는다.",
            "손 놓지 마. 떨어뜨리면 나도 기분 나쁘다.",
            "로맨스 주인공처럼 굴 거면 최소한 멋지게 싸워라."
        });
    }

    private static String pickAction(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "돌파는 속도가 아니라 타이밍입니다. 제가 빈틈을 알려드리겠습니다.",
            "전투의 첫 박자는 이미 시작됐습니다. 다음 타격에 힘을 실으십시오.",
            "정면 돌파가 필요하다면, 먼저 체력과 거리를 확인해야 합니다.",
            "화려한 일격보다 살아남는 연속 공격이 더 강합니다.",
            "지금은 발을 멈추지 마십시오. 흐름이 끊기면 적이 먼저 들어옵니다.",
            "한 방을 노리되, 그 한 방 뒤의 위험도 잊지 마십시오."
        }, new String[] {
            "액션 원해? 그럼 가만히 있지 말고 움직여.",
            "한 방 노리다 한 방 맞지 마라. 타이밍 봐.",
            "멋있게 치고 싶으면 먼저 거리부터 맞춰.",
            "폭발은 없어도 네 HP는 터질 수 있다. 조심해.",
            "돌파할 거면 망설이지 마. 망설이면 맞는다.",
            "액션 주인공은 멍때리지 않는다. 기억해라."
        });
    }

    private static String pickFantasy(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "마법보다 오래가는 힘은 준비입니다. 물약과 거리부터 확인하십시오.",
            "던전의 어둠은 늘 조용히 움직입니다. 제가 먼저 기척을 듣겠습니다.",
            "성검이 아니어도 좋습니다. 주인님의 손에서 저는 충분히 의미를 가집니다.",
            "용을 상대하는 마음가짐으로 작은 적도 방심하지 마십시오.",
            "왕국을 구하는 일보다 지금은 주인님의 HP를 지키는 일이 먼저입니다.",
            "마왕은 멀리 있어도, 선공 몬스터는 가까이에 있을 수 있습니다."
        }, new String[] {
            "판타지라고 다 마법으로 해결 안 된다. 물약부터 챙겨.",
            "마왕 잡기 전에 잡몹한테 눕지 마라.",
            "성검은 아니지만 내가 네 손에 있잖아. 좀 믿고 휘둘러.",
            "던전에서 방심하는 놈은 보통 다음 컷에 없다.",
            "용은 나중 문제고 지금 앞에 있는 놈부터 봐.",
            "마법 같은 판단은 기대 안 한다. 기본만 해도 반은 간다."
        });
    }

    private static String pickMartial(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "강호에서 오래 살아남는 이는 먼저 베는 자가 아니라 먼저 읽는 자입니다.",
            "검기는 마음에서 시작해 손끝에서 끝납니다. 호흡을 흐트러뜨리지 마십시오.",
            "사부라면 지금 물러서서 자세를 고치라 했을 겁니다.",
            "비급보다 중요한 것은 반복입니다. 오늘의 경험치가 내일의 초식이 됩니다.",
            "문파의 이름보다 중요한 건 지금 주인님의 생존입니다.",
            "절정의 경지는 무리한 돌진이 아니라 정확한 한 걸음에서 시작됩니다."
        }, new String[] {
            "강호였으면 너 지금 하수 티 난다. 자세 고쳐.",
            "검기 찾지 말고 거리부터 봐. 그게 첫 초식이다.",
            "사부가 봤으면 죽비 들었다. 집중해.",
            "비급 없어도 된다. 물약 쓰는 법부터 익혀.",
            "문파 망신시키지 말고 제대로 쳐.",
            "절정고수 흉내 내다 눕지 마라."
        });
    }

    private static String pickHorror(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "등 뒤가 조용할수록 더 조심하십시오. 조용함은 안전과 다릅니다.",
            "어둠은 늘 먼저 움직이지 않습니다. 기다렸다가 다가옵니다.",
            "무서움은 경고입니다. 무시하지 말고 주변을 확인하십시오.",
            "발소리가 없다면 더 위험합니다. 제가 기척을 놓치지 않겠습니다.",
            "공포는 시야 밖에서 시작됩니다. 시야를 넓히십시오.",
            "뒤돌아보는 습관은 생존에 도움이 됩니다. 지금도 마찬가지입니다."
        }, new String[] {
            "무섭냐? 그럼 뒤부터 봐. 보통 거기서 온다.",
            "조용하다고 안전한 거 아니다. 그런 생각 하는 놈부터 당한다.",
            "귀신보다 무서운 건 네 낮은 HP다.",
            "소름 돋으면 멈추지 말고 확인해. 눈 감으면 더 맞는다.",
            "뒤에 뭐 있냐고? 네가 안 보면 나도 답답하다.",
            "공포 장르에서 혼자 돌진하는 놈은 오래 못 간다."
        });
    }

    private static String pickComedy(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "웃을 수 있는 여유가 있다는 건 아직 전황이 나쁘지 않다는 뜻입니다.",
            "실수도 지나고 보면 이야기가 됩니다. 단, 살아남았을 때의 이야기입니다.",
            "분위기를 가볍게 하되 손끝은 가볍게 만들지 마십시오.",
            "농담은 좋습니다. 다만 선공 몬스터는 농담을 이해하지 못합니다.",
            "잠시 웃어도 됩니다. 제가 주변은 계속 보겠습니다.",
            "오늘의 웃음 포인트가 주인님의 사망 로그는 아니길 바랍니다."
        }, new String[] {
            "웃기고 싶으면 살아서 웃겨. 누우면 재미없다.",
            "개그는 좋은데 네 무빙은 좀 슬프다.",
            "농담할 시간에 물약 누르면 더 웃길 일 안 생긴다.",
            "선공몹은 드립 안 받아준다. 먼저 쳐.",
            "지금 웃긴 건 네 자신감이다. 근거는 아직 모르겠고.",
            "코미디 찍어도 엔딩은 생존으로 가자."
        });
    }

    private static String pickDetective(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "단서는 늘 전투 전에 있습니다. 몬스터 위치와 체력 변화부터 보십시오.",
            "범인을 찾듯 위험의 원인을 좁히겠습니다. 먼저 주변 선공 여부를 확인하십시오.",
            "알리바이보다 중요한 건 현재 위치입니다. 도망갈 길을 확보하십시오.",
            "사건의 핵심은 단순합니다. 체력이 낮으면 무리한 교전이 범인입니다.",
            "추리는 차분해야 합니다. 감정이 앞서면 단서를 놓칩니다.",
            "모든 전투에는 패턴이 있습니다. 저는 그 흔적을 읽겠습니다."
        }, new String[] {
            "범인 찾지 마. 네 낮은 HP가 범인이다.",
            "단서? 주변 안 보는 네 습관이 제일 큰 단서다.",
            "알리바이 필요 없다. 방금 맞은 건 네 실수다.",
            "추리할 시간에 거리 벌려. 증거는 네 HP창에 있다.",
            "사건 해결은 간단하다. 먼저 살아남아.",
            "탐정 놀이 그만하고 타겟부터 확인해."
        });
    }

    private static String pickSchool(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "오늘의 과제는 생존입니다. 제출 기한은 지금 이 순간입니다.",
            "시험처럼 전투도 준비한 만큼 결과가 나옵니다. 물약과 장비를 확인하십시오.",
            "전학생처럼 낯선 사냥터에 들어가면 먼저 분위기부터 파악해야 합니다.",
            "친구는 없지만 저는 곁에 있습니다. 단, 숙제는 주인님이 해야 합니다.",
            "실전은 예습 없는 시험입니다. 그래도 기본기는 배신하지 않습니다.",
            "교실이었다면 지금은 집중 시간입니다. 주변을 살피십시오."
        }, new String[] {
            "오늘 숙제는 안 죽기다. 어렵지 않지?",
            "시험이면 너 지금 오답 체크 중이다. 물약부터 봐.",
            "전학생처럼 어리둥절하지 말고 맵부터 익혀.",
            "수업 들었으면 거리 조절부터 했겠지.",
            "친구 없다고 울지 마. 내가 있잖아. 대신 제대로 싸워.",
            "집중 안 하면 생활기록부가 아니라 사망기록부 쓴다."
        });
    }

    private static String pickSliceOfLife(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "잔잔한 하루도 좋습니다. 오늘은 무리하지 않고 꾸준히 성장해도 충분합니다.",
            "작은 사냥, 작은 경험치, 작은 대화가 모여 에고를 성장시킵니다.",
            "커피 한 잔 같은 여유는 없지만, 잠깐 숨 고를 틈은 만들 수 있습니다.",
            "평온한 순간이 있다면 장비와 물약을 정리하기 좋은 시간입니다.",
            "전투만이 하루의 전부는 아닙니다. 살아남아 돌아오는 것도 중요합니다.",
            "잔잔한 길일수록 방심하기 쉽습니다. 제가 조용히 살피겠습니다."
        }, new String[] {
            "힐링 원하면 안전지대 가. 여기선 맞으면 현실이다.",
            "잔잔한 하루 좋지. 근데 선공몹은 그런 거 모른다.",
            "커피는 없고 물약은 있다. 뭐부터 마실래?",
            "일상물 찍어도 사냥터에선 방심 금지다.",
            "평온할 때 정비해. 꼭 맞고 나서 찾지 말고.",
            "힐링은 살아있을 때 하는 거다."
        });
    }

    private static String pickVillain(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "흑막은 늘 가장 조용한 곳에 숨어 있습니다. 주변 변화부터 확인하십시오.",
            "악역의 강함은 방심하지 않는 데서 나옵니다. 주인님도 그 점은 배워야 합니다.",
            "상대가 비겁하다면 더 차분해야 합니다. 분노는 판단을 흐립니다.",
            "배후가 누구든 지금 베어야 할 대상은 눈앞에 있습니다.",
            "타락한 힘보다 위험한 건 준비 없는 자신감입니다.",
            "악역이 웃기 전에 먼저 거리를 잡으십시오."
        }, new String[] {
            "빌런처럼 웃고 싶으면 먼저 안 죽어야지.",
            "흑막 찾지 마. 네 방심이 제일 큰 흑막이다.",
            "악역도 계획은 세운다. 너도 좀 세워.",
            "타락은 멋있는데 사망은 안 멋있다.",
            "배후? 지금 네 뒤가 더 문제일 수 있다.",
            "빌런 대사 치기 전에 물약부터 챙겨."
        });
    }

    private static String pickHero(String tone, PcInstance pc, ItemInstance weapon) {
        return pick(tone, new String[] {
            "주인공의 각성은 위기 속에서 시작되지만, 생존으로 증명됩니다.",
            "성장은 조용히 쌓입니다. 오늘의 작은 전투도 다음 장면을 바꿉니다.",
            "운명이 부른다면 저는 검끝에서 대답하겠습니다.",
            "포기하지 않는 손에 깃든 무기는 언젠가 진짜 힘을 드러냅니다.",
            "지금은 약해 보여도 방향이 맞다면 충분히 강해질 수 있습니다.",
            "주인공은 혼자 강한 사람이 아닙니다. 곁의 힘을 믿는 사람입니다."
        }, new String[] {
            "주인공 되고 싶으면 먼저 눕지 마.",
            "각성은 좋다. 근데 각성 전에 물약부터 먹어.",
            "운명은 몰라도 네 컨트롤은 보인다. 좀 더 잘해봐.",
            "성장형 주인공이면 지금 약한 것도 설정이긴 하지. 오래가면 좋겠네.",
            "주인공 버프 기대하지 말고 기본기부터 해.",
            "멋진 대사는 네가 살아남은 다음에 하자."
        });
    }

    private static String pickAny(String tone, PcInstance pc, ItemInstance weapon) {
        String[] polite = join(new String[][] {
            anyPolite(), dramaPoliteShort(), moviePoliteShort(), webtoonPoliteShort(), battlePoliteShort()
        });
        String[] rude = join(new String[][] {
            anyRude(), dramaRudeShort(), movieRudeShort(), webtoonRudeShort(), battleRudeShort()
        });
        return pick(tone, polite, rude);
    }

    private static String[] anyPolite() {
        return new String[] {
            "오늘의 한 걸음이 내일의 각성이 됩니다.",
            "검은 말이 없지만, 저는 주인님의 선택을 듣고 있습니다.",
            "지금 필요한 것은 용기보다 정확한 판단입니다.",
            "빛나는 장면은 대개 가장 어두운 순간 뒤에 옵니다.",
            "무리하지 않는 용기도 분명한 실력입니다.",
            "적의 수보다 중요한 것은 주인님의 다음 위치입니다.",
            "작은 승리를 쌓으면 큰 위기 앞에서도 흔들리지 않습니다.",
            "살아남은 자만이 다음 대사를 말할 수 있습니다."
        };
    }

    private static String[] anyRude() {
        return new String[] {
            "멋진 대사 원하면 멋지게 살아남아라.",
            "폼 잡는 건 좋은데 물약도 같이 눌러.",
            "오늘의 교훈: 방심하면 눕는다.",
            "강한 척은 나중에 하고 지금은 거리 봐.",
            "주인공인 줄 알았으면 주인공답게 움직여.",
            "대사는 내가 해줄 테니 전투는 네가 해.",
            "멋은 내가 챙길 테니 너는 HP나 챙겨.",
            "장면은 좋은데 컨트롤이 아쉽다."
        };
    }

    private static String[] dramaPoliteShort() { return new String[] { "흔들리는 마음도 지나갑니다. 하지만 지금의 판단은 남습니다.", "기다린 만큼 강해지는 순간이 있습니다.", "상처는 약점이 아니라 다음 선택의 이유가 됩니다." }; }
    private static String[] dramaRudeShort() { return new String[] { "울 시간에 움직여. 장르는 아직 액션이다.", "비련의 주인공 할 거면 안전지대에서 해.", "감정 잡았으면 이제 타겟도 잡아." }; }
    private static String[] moviePoliteShort() { return new String[] { "지금 장면은 조용하지만 중요합니다.", "마지막 컷까지 긴장을 놓지 마십시오.", "주인공은 위기에서 시선을 피하지 않습니다." }; }
    private static String[] movieRudeShort() { return new String[] { "엔딩 보기 전에 눕지 마라.", "명장면은 무빙에서 나온다. 좀 움직여.", "이 장면 재촬영하고 싶지 않으면 집중해." }; }
    private static String[] webtoonPoliteShort() { return new String[] { "이번 회차의 핵심은 생존입니다.", "작은 경험치도 다음 화의 복선이 됩니다.", "컷이 바뀌기 전에 주변을 확인하십시오." }; }
    private static String[] webtoonRudeShort() { return new String[] { "댓글창 있었으면 물약 먹으라고 난리 났다.", "이번 화에서 하차하지 마라. 살아남아.", "작화는 괜찮은데 판단이 별로다." }; }
    private static String[] battlePoliteShort() { return new String[] { "호흡을 고르고 다음 타격을 준비하십시오.", "지금은 서두름보다 정확함이 필요합니다.", "위험은 먼저 읽는 자에게 기회를 줍니다." }; }
    private static String[] battleRudeShort() { return new String[] { "맞기 전에 봐. 맞고 나서 묻지 말고.", "타이밍 놓치면 네 HP가 설명해준다.", "싸울 거면 제대로 싸워." }; }

    private static String[] join(String[][] groups) {
        int size = 0;
        for (int i = 0; i < groups.length; i++)
            size += groups[i].length;
        String[] result = new String[size];
        int index = 0;
        for (int i = 0; i < groups.length; i++) {
            for (int j = 0; j < groups[i].length; j++)
                result[index++] = groups[i][j];
        }
        return result;
    }

    private static String pick(String tone, String[] polite, String[] rude) {
        String[] arr = isRude(tone) ? rude : polite;
        if (arr == null || arr.length == 0)
            return "...";
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
