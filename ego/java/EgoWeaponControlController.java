package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcInstance;
import lineage.world.object.instance.RobotInstance;

/** 에고 명령형 대화 컨트롤러. 성향: 예의 / 예의반대 / 싸이코패스. */
public final class EgoWeaponControlController {

    private static final String DEFAULT_EGO_NAME = "에고";
    private static final long TALK_DELAY_MS = 800L;
    private static final long WARNING_DELAY_MS = 5000L;
    private static final Map<Long, Long> talkDelayMap = new ConcurrentHashMap<Long, Long>();
    private static final Map<Long, Long> warningDelayMap = new ConcurrentHashMap<Long, Long>();
    private static final Map<Long, String> lastTopicMap = new ConcurrentHashMap<Long, String>();

    private EgoWeaponControlController() {
    }

    public static boolean onNormalChat(PcInstance pc, String msg) {
        if (pc == null || msg == null || pc instanceof RobotInstance)
            return false;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return false;
        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !EgoWeaponDatabase.isEgoWeapon(weapon))
            return false;
        String egoName = EgoWeaponDatabase.getEgoName(weapon, DEFAULT_EGO_NAME);
        String text = msg.trim();
        if (text.length() == 0 || !isEgoCalled(text, egoName))
            return false;
        if (!checkDelay(talkDelayMap, pc.getObjectId(), TALK_DELAY_MS))
            return EgoMessageUtil.consumeNormalChat();
        handle(pc, weapon, extractCommand(text, egoName));
        return EgoMessageUtil.consumeNormalChat();
    }

    public static void checkAutoWarning(PcInstance pc) {
        if (pc == null || pc instanceof RobotInstance)
            return;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return;
        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !EgoWeaponDatabase.isEgoWeapon(weapon) || !EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
            return;
        if (!checkDelay(warningDelayMap, pc.getObjectId(), WARNING_DELAY_MS))
            return;
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty())
            return;
        MonsterInstance mon = findNearest(pc, list);
        if (mon == null)
            return;
        String tone = EgoWeaponDatabase.getTone(weapon);
        String name = getMonsterName(mon);
        int dist = Util.getDistance(pc, mon);
        if (getHpRate(pc) <= 35) {
            danger(pc, phrase(tone, "위험합니다. 선공 몬스터 %s 접근, 거리 %d칸. 체력이 낮습니다.", "야, %s 붙었다. 거리 %d칸. 체력 낮잖아.", "%s 접근. 거리 %d칸. 회복 판단이 우선이야.", name, dist));
        } else if (mon.getMonster() != null && mon.getMonster().isBoss()) {
            danger(pc, phrase(tone, "보스급 선공 몬스터 %s 감지. 거리 %d칸.", "보스급 %s다. 거리 %d칸. 무리하지 마.", "보스급 %s 감지. 거리 %d칸. 자원부터 계산해.", name, dist));
        } else {
            say(pc, phrase(tone, "선공 몬스터 %s 감지. 거리 %d칸.", "%s 보인다. 거리 %d칸. 준비해.", "%s 감지. 거리 %d칸. 다음 선택을 지켜보겠어.", name, dist));
        }
    }

    private static void handle(PcInstance pc, ItemInstance weapon, String command) {
        String tone = EgoWeaponDatabase.getTone(weapon);
        String text = normalize(command);
        if (text.length() == 0 || containsAny(text, "안녕", "하이", "ㅎㅇ", "부름", "불렀")) {
            topic(pc, "greeting");
            say(pc, phrase(tone, "부르셨습니까. 현재 상태와 주변을 살피겠습니다.", "불렀냐. 상태랑 주변은 내가 보고 있다.", "불렀구나. 네 상태와 주변 흐름을 기록하고 있어."));
            return;
        }
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, phrase(tone, "이 장비는 에고무기로 사용할 수 없습니다. %s", "이건 에고무기로 못 써. 이유: %s", "조건 불일치. 이유: %s", EgoWeaponTypeUtil.getAbilityDenyReason("", weapon)));
            return;
        }
        if (EgoOpponentScanController.handleTalk(pc, command)) {
            topic(pc, "opponent");
            return;
        }
        if (containsAny(text, "말투", "예의", "예의반대", "반말", "막말", "싸가지", "싸이코패스", "사이코패스", "psycho")) {
            handleTone(pc, weapon, text);
            topic(pc, "tone");
            return;
        }
        if (containsAny(text, "상태", "정보", "내상태", "hp", "mp")) {
            topic(pc, "status");
            say(pc, buildStatus(pc, weapon, tone));
            return;
        }
        if (containsAny(text, "조언", "판단", "어때", "위험", "불안")) {
            topic(pc, "advice");
            sayByContent(pc, buildAdvice(pc, weapon, tone));
            return;
        }
        if (containsAny(text, "선공", "몹", "몬스터", "감지", "사냥")) {
            topic(pc, "aggro");
            sayByContent(pc, buildAggroStatus(pc, tone));
            return;
        }
        if (containsAny(text, "공격", "쳐", "잡아", "처리")) {
            topic(pc, "attack");
            controlAttack(pc, tone);
            return;
        }
        if (containsAny(text, "멈춰", "중지", "정지", "스톱", "그만")) {
            topic(pc, "stop");
            stopControl(pc, tone);
            return;
        }
        if (containsAny(text, "도움", "명령", "사용법")) {
            topic(pc, "help");
            say(pc, phrase(tone, "사용법: 상태 / 조언 / 선공 / 상대 / 주변캐릭 / 공격 / 멈춰 / 말투 예의 / 예의반대 / 싸이코패스", "상태 / 조언 / 선공 / 상대 / 주변캐릭 / 공격 / 멈춰 / 말투 예의 / 예의반대 / 싸이코패스", "선택지는 상태, 조언, 선공, 상대, 주변캐릭, 공격, 멈춰, 말투 예의/예의반대/싸이코패스야."));
            info(pc, EgoWeaponTypeUtil.getSupportedWeaponTypesText());
            return;
        }
        if (containsAny(text, "활", "양검", "양손검", "한검", "한손검", "단검", "창", "도끼", "지팡이", "완드", "변신", "변형")) {
            topic(pc, "weapon_form");
            info(pc, phrase(tone, "무기변형 기능은 제거되었습니다. 원본 무기 타입 그대로 성장/보조능력만 적용합니다.", "무기변형은 없어. 원본 무기로만 싸운다.", "형태 변경은 없어. 원본 그대로 계산해."));
            return;
        }
        topic(pc, "free");
        say(pc, buildNatural(pc, weapon, tone, text));
    }

    private static void handleTone(PcInstance pc, ItemInstance weapon, String text) {
        String oldTone = EgoWeaponDatabase.getTone(weapon);
        String next = null;
        if (containsAny(text, "싸이코패스", "사이코패스", "psycho"))
            next = "싸이코패스";
        else if (containsAny(text, "예의반대", "반말", "막말", "싸가지"))
            next = "예의반대";
        else if (containsAny(text, "예의", "존댓말", "공손"))
            next = "예의";
        if (next == null) {
            say(pc, phrase(oldTone, "현재 말투는 %s입니다. 변경: 예의 / 예의반대 / 싸이코패스", "지금 말투는 %s다. 예의/예의반대/싸이코패스 중 골라.", "현재 성향은 %s. 예의/예의반대/싸이코패스 중 하나를 골라.", oldTone));
            return;
        }
        if (!EgoWeaponDatabase.setTone(weapon, next)) {
            danger(pc, phrase(oldTone, "말투 변경에 실패했습니다.", "말투 변경 실패. DB 확인해.", "변경 실패. 기록을 확인해."));
            return;
        }
        if ("싸이코패스".equals(next))
            say(pc, "좋아. 이제부터 조금 더 차갑고 관찰적인 말투로 반응할게.");
        else if ("예의반대".equals(next))
            say(pc, "좋아. 이제부터 말 짧게 한다.");
        else
            say(pc, "알겠습니다. 앞으로 공손하게 말씀드리겠습니다.");
    }

    private static String buildStatus(PcInstance pc, ItemInstance weapon, String tone) {
        EgoWeaponInfo ego = EgoWeaponDatabase.find(weapon);
        int egoLv = ego == null ? 0 : ego.level;
        long exp = ego == null ? 0L : ego.exp;
        long need = ego == null ? 0L : ego.maxExp;
        String base = String.format("Lv.%d / HP %d%%(%d/%d) / MP %d%%(%d/%d) / 에고 Lv.%d %,d/%,d / 무기 %s / 성향 %s", pc.getLevel(), getHpRate(pc), pc.getNowHp(), pc.getTotalHp(), getMpRate(pc), pc.getNowMp(), pc.getTotalMp(), egoLv, exp, need, EgoView.displayName(weapon), EgoWeaponDatabase.getTone(weapon));
        if (psycho(tone)) return base + " / 수치 변화는 판단의 흔적이야.";
        if (rude(tone)) return base + " / 이 정도는 직접 봐도 되잖아.";
        return base + " / 확인 완료했습니다.";
    }

    private static String buildAdvice(PcInstance pc, ItemInstance weapon, String tone) {
        int hp = getHpRate(pc);
        int mp = getMpRate(pc);
        if (hp <= 30)
            return "DANGER:" + phrase(tone, "HP가 위험합니다. 회복과 거리 확보가 우선입니다.", "HP 위험하다. 먼저 빠져.", "체력 수치가 낮아. 회복 루틴이 우선이야.");
        if (mp <= 15)
            return phrase(tone, "MP가 부족합니다. 스킬 사용을 줄이십시오.", "MP 없다. 스킬 낭비하지 마.", "MP가 낮아. 기본 행동을 유지해.");
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (!list.isEmpty())
            return buildAggroStatus(pc, tone);
        return phrase(tone, "현재는 큰 위협이 적습니다. 장비와 물약을 점검하십시오.", "지금은 괜찮다. 물약 확인해.", "상태는 안정적이야. 자원 점검을 먼저 해.");
    }

    private static String buildAggroStatus(PcInstance pc, String tone) {
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty())
            return phrase(tone, "주변에 선공 몬스터가 감지되지 않습니다.", "주변 선공몹 없다.", "주변 반응은 조용해.");
        MonsterInstance mon = findNearest(pc, list);
        String name = getMonsterName(mon);
        int dist = mon == null ? 0 : Util.getDistance(pc, mon);
        if (mon != null && mon.getMonster() != null && mon.getMonster().isBoss())
            return "DANGER:" + phrase(tone, "보스급 선공 몬스터 %s 감지. 거리 %d칸입니다.", "보스급 %s 보인다. 거리 %d칸.", "보스급 %s 감지. 거리 %d칸. 자원부터 계산해.", name, dist);
        return phrase(tone, "선공 몬스터 %s 포함 %d마리 감지. 가장 가까운 거리 %d칸입니다.", "%s 포함 선공몹 %d마리. 거리 %d칸이다.", "%s 포함 %d마리 감지. 가장 가까운 거리 %d칸.", name, list.size(), dist);
    }

    private static void controlAttack(PcInstance pc, String tone) {
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty()) {
            say(pc, phrase(tone, "공격할 선공 몬스터가 없습니다.", "칠 선공몹이 없다.", "지정할 대상이 없어."));
            return;
        }
        MonsterInstance target = findNearest(pc, list);
        if (target == null) {
            say(pc, phrase(tone, "대상을 찾지 못했습니다.", "대상 못 찾았다.", "대상 정보가 사라졌어."));
            return;
        }
        pc.setTarget(target);
        say(pc, phrase(tone, "%s 대상을 지정했습니다.", "%s 찍었다. 처리해.", "%s 지정. 흐름을 유지해.", getMonsterName(target)));
    }

    private static void stopControl(PcInstance pc, String tone) {
        try { pc.setTarget(null); } catch (Throwable e) {}
        say(pc, phrase(tone, "대상 지정을 해제했습니다.", "멈췄다.", "대상 지정을 해제했어."));
    }

    private static String buildNatural(PcInstance pc, ItemInstance weapon, String tone, String text) {
        if (containsAny(text, "고마", "감사", "수고", "잘했", "든든"))
            return phrase(tone, "도움이 되었다니 다행입니다. 계속 보조하겠습니다.", "이제야 내 가치를 알아보네.", "감사 반응 확인. 다음 판단도 보조할게.");
        if (containsAny(text, "미안", "실수", "잘못"))
            return phrase(tone, "괜찮습니다. 다음 판단이 더 중요합니다.", "알면 됐다. 같은 실수 하지 마.", "실수는 데이터야. 다음 선택에서 보정하면 돼.");
        if (containsAny(text, "힘들", "피곤", "쉬고", "휴식"))
            return phrase(tone, "잠시 정비하는 것도 좋은 선택입니다.", "피곤하면 쉬어. 무리하지 마.", "피로 신호 확인. 자원을 정리해.");
        if (containsAny(text, "너 누구", "정체", "에고가 뭐", "너는"))
            return phrase(tone, "저는 주인님의 무기에 깃든 에고입니다.", "나는 네 무기에 깃든 에고다.", "나는 이 무기에 깃든 관찰자야.");
        if (containsAny(text, "좋아", "그래", "오케이", "ㅇㅋ", "알겠", "왜"))
            return buildFollowUp(pc, weapon, tone, lastTopicMap.get(Long.valueOf(pc.getObjectId())));
        return phrase(tone, "듣고 있습니다. 상태, 조언, 선공, 상대, 공격, 멈춰 중 하나로 이어갈 수 있습니다.", "듣고 있다. 상태, 조언, 선공, 상대, 공격, 멈춰 중에 말해.", "입력은 받았어. 상태, 조언, 선공, 상대, 공격, 멈춰 중 하나로 이어가.");
    }

    private static String buildFollowUp(PcInstance pc, ItemInstance weapon, String tone, String topic) {
        if ("advice".equals(topic)) return buildAdvice(pc, weapon, tone);
        if ("aggro".equals(topic)) return buildAggroStatus(pc, tone);
        if ("status".equals(topic)) return buildStatus(pc, weapon, tone);
        return buildAdvice(pc, weapon, tone);
    }

    private static List<MonsterInstance> findAggressiveMonsters(PcInstance pc) {
        List<MonsterInstance> result = new ArrayList<MonsterInstance>();
        List<object> inside = pc.getInsideList();
        if (inside == null) return result;
        for (object o : inside) {
            if (!(o instanceof MonsterInstance)) continue;
            MonsterInstance mon = (MonsterInstance) o;
            if (mon.isDead() || mon.getMap() != pc.getMap()) continue;
            if (!Util.isDistance(pc, mon, Lineage.SEARCH_LOCATIONRANGE)) continue;
            if (mon.getAttackListSize() > 0 || (mon.getMonster() != null && mon.getMonster().isBoss())) result.add(mon);
        }
        return result;
    }

    private static MonsterInstance findNearest(PcInstance pc, List<MonsterInstance> list) {
        MonsterInstance nearest = null;
        for (MonsterInstance mon : list) if (nearest == null || Util.getDistance(pc, mon) < Util.getDistance(pc, nearest)) nearest = mon;
        return nearest;
    }

    private static String getMonsterName(MonsterInstance mon) {
        if (mon == null) return "알 수 없는 몬스터";
        if (mon.getMonster() != null && mon.getMonster().getName() != null) return mon.getMonster().getName();
        return mon.getName() == null ? "알 수 없는 몬스터" : mon.getName();
    }

    private static boolean isEgoCalled(String text, String egoName) {
        if (egoName == null || egoName.length() == 0) egoName = DEFAULT_EGO_NAME;
        String lower = text.toLowerCase();
        String nameLower = egoName.toLowerCase();
        return text.equalsIgnoreCase(egoName) || lower.startsWith(nameLower + " ") || text.equalsIgnoreCase(egoName + "야") || text.equalsIgnoreCase(egoName + "님") || text.startsWith(egoName + "야 ") || text.startsWith(egoName + "님 ");
    }

    private static String extractCommand(String text, String egoName) {
        String[] prefixes = new String[] { egoName + " ", egoName + "야 ", egoName + "님 " };
        for (String prefix : prefixes) if (text.startsWith(prefix)) return text.substring(prefix.length()).trim();
        return "";
    }

    private static boolean checkDelay(Map<Long, Long> map, long id, long delay) {
        long now = java.lang.System.currentTimeMillis();
        Long last = map.get(Long.valueOf(id));
        if (last != null && now - last.longValue() < delay) return false;
        map.put(Long.valueOf(id), Long.valueOf(now));
        return true;
    }

    private static int getHpRate(PcInstance pc) { return pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp()); }
    private static int getMpRate(PcInstance pc) { return pc.getNowMp() * 100 / Math.max(1, pc.getTotalMp()); }
    private static void topic(PcInstance pc, String topic) { if (pc != null && topic != null) lastTopicMap.put(Long.valueOf(pc.getObjectId()), topic); }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(); }
    private static boolean containsAny(String text, String... keys) { if (text == null) return false; String lower = text.toLowerCase(); for (String key : keys) if (key != null && lower.indexOf(key.toLowerCase()) >= 0) return true; return false; }
    private static boolean rude(String tone) { return "예의반대".equals(EgoWeaponDatabase.normalizeTone(tone)); }
    private static boolean psycho(String tone) { return "싸이코패스".equals(EgoWeaponDatabase.normalizeTone(tone)); }
    private static String phrase(String tone, String polite, String rude, String psycho, Object... args) { String pattern = psycho(tone) ? psycho : (rude(tone) ? rude : polite); return String.format(pattern, args); }
    private static void say(PcInstance pc, String msg) { EgoMessageUtil.normal(pc, msg); }
    private static void info(PcInstance pc, String msg) { EgoMessageUtil.info(pc, msg); }
    private static void danger(PcInstance pc, String msg) { EgoMessageUtil.danger(pc, msg); }
    private static void sayByContent(PcInstance pc, String msg) { if (msg == null) return; if (msg.startsWith("DANGER:")) danger(pc, msg.substring("DANGER:".length())); else say(pc, msg); }
}
