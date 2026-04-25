package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcInstance;
import lineage.world.object.instance.RobotInstance;

/**
 * 에고무기 대화/상태인식/간단 제어 컨트롤러.
 *
 * 대화 말투:
 * - 예의: 공손/존댓말
 * - 예의반대: 건방진 반말/도발형
 */
public final class EgoWeaponControlController {

    private static final String DEFAULT_EGO_NAME = "에고";
    private static final String TONE_POLITE = "예의";
    private static final String TONE_RUDE = "예의반대";
    private static final long TALK_DELAY_MS = 800L;
    private static final long WARNING_DELAY_MS = 5000L;

    private static final java.util.Map<Long, Long> talkDelayMap = new java.util.concurrent.ConcurrentHashMap<Long, Long>();
    private static final java.util.Map<Long, Long> warningDelayMap = new java.util.concurrent.ConcurrentHashMap<Long, Long>();

    private EgoWeaponControlController() {
    }

    public static boolean onNormalChat(PcInstance pc, String msg) {
        if (pc == null || msg == null)
            return false;
        if (pc instanceof RobotInstance)
            return false;

        Inventory inv = pc.getInventory();
        if (inv == null)
            return false;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null)
            return false;

        String egoName = getEgoName(weapon);
        String text = msg.trim();
        if (text.length() == 0)
            return false;

        if (!isEgoCalled(text, egoName))
            return false;

        if (!checkTalkDelay(pc))
            return EgoMessageUtil.consumeNormalChat();

        String command = extractCommand(text, egoName);
        handle(pc, weapon, command);
        return EgoMessageUtil.consumeNormalChat();
    }

    public static void checkAutoWarning(PcInstance pc) {
        if (pc == null || pc instanceof RobotInstance)
            return;

        Inventory inv = pc.getInventory();
        if (inv == null)
            return;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
            return;

        if (!checkWarningDelay(pc))
            return;

        List<MonsterInstance> aggroList = findAggressiveMonsters(pc);
        if (aggroList.isEmpty())
            return;

        int hpRate = getHpRate(pc);
        MonsterInstance nearest = findNearest(pc, aggroList);
        if (nearest == null)
            return;

        String name = getMonsterName(nearest);
        int dist = Util.getDistance(pc, nearest);
        String tone = getTone(weapon);

        if (hpRate <= 35)
            danger(pc, phrase(tone, "위험합니다. 선공 몬스터 %s 접근, 거리 %d칸. 체력이 낮습니다.", "야, %s 붙었다. 거리 %d칸. 피 낮잖아, 정신 차려.", name, dist));
        else if (nearest.getMonster() != null && nearest.getMonster().isBoss())
            danger(pc, phrase(tone, "보스급 선공 몬스터 %s 감지. 거리 %d칸.", "보스급 %s다. 거리 %d칸. 까불다 죽는다.", name, dist));
        else
            say(pc, phrase(tone, "선공 몬스터 %s 감지. 거리 %d칸.", "%s 보인다. 거리 %d칸. 먼저 칠 준비해.", name, dist));
    }

    private static boolean isEgoCalled(String text, String egoName) {
        if (egoName == null || egoName.length() == 0)
            egoName = DEFAULT_EGO_NAME;

        if (text.equalsIgnoreCase(egoName))
            return true;
        if (text.toLowerCase().startsWith(egoName.toLowerCase() + " "))
            return true;
        if (text.equalsIgnoreCase(egoName + "야") || text.equalsIgnoreCase(egoName + "님"))
            return true;
        if (text.startsWith(egoName + "야 ") || text.startsWith(egoName + "님 "))
            return true;

        return false;
    }

    private static String extractCommand(String text, String egoName) {
        if (text.equalsIgnoreCase(egoName))
            return "";

        String[] prefixes = new String[] { egoName + " ", egoName + "야 ", egoName + "님 " };
        for (String prefix : prefixes) {
            if (text.startsWith(prefix))
                return text.substring(prefix.length()).trim();
        }

        if (text.equalsIgnoreCase(egoName + "야") || text.equalsIgnoreCase(egoName + "님"))
            return "";

        return "";
    }

    private static boolean checkTalkDelay(PcInstance pc) {
        long now = java.lang.System.currentTimeMillis();
        Long last = talkDelayMap.get(pc.getObjectId());
        if (last != null && now - last.longValue() < TALK_DELAY_MS)
            return false;
        talkDelayMap.put(pc.getObjectId(), now);
        return true;
    }

    private static boolean checkWarningDelay(PcInstance pc) {
        long now = java.lang.System.currentTimeMillis();
        Long last = warningDelayMap.get(pc.getObjectId());
        if (last != null && now - last.longValue() < WARNING_DELAY_MS)
            return false;
        warningDelayMap.put(pc.getObjectId(), now);
        return true;
    }

    private static String getEgoName(ItemInstance weapon) {
        return EgoWeaponDatabase.getEgoName(weapon, DEFAULT_EGO_NAME);
    }

    private static String getTone(ItemInstance weapon) {
        return EgoWeaponDatabase.getTone(weapon);
    }

    private static boolean isRude(String tone) {
        return TONE_RUDE.equals(EgoWeaponDatabase.normalizeTone(tone));
    }

    private static void handle(PcInstance pc, ItemInstance weapon, String command) {
        if (weapon == null) {
            danger(pc, "착용 중인 무기가 없습니다.");
            return;
        }

        String tone = getTone(weapon);

        if (command == null || command.length() == 0) {
            say(pc, buildGreeting(pc, weapon, tone));
            return;
        }

        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, phrase(tone, "이 장비는 에고무기로 사용할 수 없습니다. %s", "이건 에고무기로 못 써. 이유: %s", EgoWeaponTypeUtil.getAbilityDenyReason("", weapon)));
            return;
        }

        if (EgoOpponentScanController.handleTalk(pc, command))
            return;

        if (containsAny(command, "말투", "예의", "예의반대", "반말", "막말")) {
            handleToneTalk(pc, weapon, command);
            return;
        }

        if (containsAny(command, "상태", "정보", "내상태")) {
            say(pc, buildStatus(pc, weapon, tone));
            return;
        }

        if (containsAny(command, "조언", "판단", "어때", "위험")) {
            sayByContent(pc, buildAdvice(pc, weapon, tone));
            return;
        }

        if (containsAny(command, "선공", "몹", "몬스터", "감지")) {
            sayByContent(pc, buildAggroStatus(pc, tone));
            return;
        }

        if (containsAny(command, "공격", "쳐", "잡아", "처리")) {
            controlAttackNearestAggro(pc, weapon, tone);
            return;
        }

        if (containsAny(command, "멈춰", "중지", "정지", "스톱", "그만")) {
            stopControl(pc, tone);
            return;
        }

        if (containsAny(command, "도움", "명령", "사용법")) {
            say(pc, phrase(tone, "사용법: 에고 상태 / 에고 조언 / 에고 선공 / 에고 상대 / 에고 주변캐릭 / 에고 공격 / 에고 멈춰 / 에고 말투 예의 / 에고 말투 예의반대", "쓸 말은 이거다: 상태 / 조언 / 선공 / 상대 / 주변캐릭 / 공격 / 멈춰 / 말투 예의 / 말투 예의반대"));
            info(pc, EgoWeaponTypeUtil.getSupportedWeaponTypesText());
            return;
        }

        if (containsAny(command, "활", "양검", "양손검", "한검", "한손검", "단검", "창", "도끼", "지팡이", "완드", "변신", "변형")) {
            info(pc, phrase(tone, "무기변형 기능은 제거되었습니다. 에고는 원본 무기 타입 그대로 성장/보조능력만 적용합니다.", "무기변형은 없어. 원본 무기로만 싸우는 거다."));
            return;
        }

        say(pc, phrase(tone, "명령을 이해하지 못했습니다. '상태', '조언', '선공', '상대', '공격', '멈춰'로 말씀하십시오.", "뭔 소린지 모르겠다. 상태, 조언, 선공, 상대, 공격, 멈춰 중에 골라."));
    }

    private static void handleToneTalk(PcInstance pc, ItemInstance weapon, String command) {
        String tone = getTone(weapon);
        String nextTone = null;
        if (containsAny(command, "예의반대", "반말", "막말", "싸가지"))
            nextTone = TONE_RUDE;
        else if (containsAny(command, "예의", "존댓말", "공손"))
            nextTone = TONE_POLITE;

        if (nextTone == null) {
            say(pc, phrase(tone, "현재 말투는 %s입니다. 변경: '말투 예의' 또는 '말투 예의반대'", "지금 말투는 %s다. 바꾸려면 '말투 예의'나 '말투 예의반대'라고 해.", tone));
            return;
        }

        if (EgoWeaponDatabase.setTone(weapon, nextTone)) {
            if (TONE_RUDE.equals(nextTone))
                say(pc, "좋아. 이제부터 말 짧게 한다. 불만 없지?");
            else
                say(pc, "알겠습니다. 앞으로 공손하게 말씀드리겠습니다.");
        } else {
            danger(pc, phrase(tone, "말투 변경에 실패했습니다.", "말투 변경 실패했다. DB부터 확인해."));
        }
    }

    private static boolean containsAny(String text, String... keys) {
        if (text == null)
            return false;
        for (String key : keys) {
            if (text.contains(key))
                return true;
        }
        return false;
    }

    private static String buildGreeting(PcInstance pc, ItemInstance weapon, String tone) {
        int hpRate = getHpRate(pc);

        if (hpRate <= 30)
            return "DANGER:" + phrase(tone, "부름보다 회복이 먼저입니다. 체력이 위험합니다.", "부를 시간에 물약부터 먹어. 피 위험하다.");

        List<MonsterInstance> aggroList = findAggressiveMonsters(pc);
        if (!aggroList.isEmpty())
            return phrase(tone, "듣고 있습니다. 근처에 선공 몬스터 기척이 있습니다.", "듣고 있다. 근처에 선공몹 있다, 눈 좀 떠.");

        return phrase(tone, "부르셨습니까, 주인님. 현재 무기는 %s, 원본 타입은 %s입니다.", "왜 불렀냐. 무기는 %s, 원본 타입은 %s다.", EgoView.displayName(weapon), EgoWeaponTypeUtil.getDisplayTypeName(weapon));
    }

    private static String buildStatus(PcInstance pc, ItemInstance weapon, String tone) {
        int hpRate = getHpRate(pc);
        int mpRate = getMpRate(pc);
        String base = String.format("Lv.%d / HP %d%%(%d/%d) / MP %d%%(%d/%d) / 에고 +%d %s / 원본 %s",
            pc.getLevel(), hpRate, pc.getNowHp(), pc.getTotalHp(), mpRate, pc.getNowMp(), pc.getTotalMp(), weapon.getEnLevel(), EgoView.displayName(weapon), EgoWeaponTypeUtil.getOriginalType2(weapon));
        if (isRude(tone))
            return base + " / 이 정도는 직접 봐도 되잖아.";
        return base + " / 확인 완료했습니다.";
    }

    private static String buildAdvice(PcInstance pc, ItemInstance weapon, String tone) {
        int hpRate = getHpRate(pc);
        int mpRate = getMpRate(pc);
        List<MonsterInstance> aggroList = findAggressiveMonsters(pc);

        if (hpRate <= 30 && !aggroList.isEmpty())
            return "DANGER:" + phrase(tone, "체력이 낮고 선공 몬스터가 있습니다. 교전보다 후퇴와 회복이 우선입니다.", "피 낮은데 선공몹까지 있다. 싸우지 말고 빠져, 답답하게 굴지 말고.");
        if (hpRate <= 30)
            return "DANGER:" + phrase(tone, "체력이 낮습니다. 물약 사용 또는 후퇴를 권합니다.", "피 낮다. 물약 먹거나 빠져. 버티다 눕는다.");
        if (mpRate <= 20 && EgoWeaponTypeUtil.isMagicWeapon(weapon))
            return phrase(tone, "마나가 부족합니다. 지팡이/완드 계열 원본 무기 능력 효율이 낮아질 수 있습니다.", "마나 없다. 지팡이/완드 들고 그러면 효율 안 나온다.");
        if (mpRate <= 20)
            return phrase(tone, "마나가 부족합니다. 스킬 사용은 아끼고 평타 위주로 싸우십시오.", "마나 아껴. 스킬 막 쓰지 말고 평타 쳐.");
        if (!aggroList.isEmpty()) {
            MonsterInstance nearest = findNearest(pc, aggroList);
            String name = nearest == null ? "알 수 없는 몬스터" : getMonsterName(nearest);
            return phrase(tone, "선공 몬스터 %s가 있습니다. 먼저 정리하는 것이 좋습니다.", "선공몹 %s 있다. 먼저 잡아, 안 그러면 귀찮아진다.", name);
        }
        if (pc.getTarget() != null)
            return phrase(tone, "현재 대상이 잡혀 있습니다. 전투 흐름은 유지 가능합니다.", "타겟 잡혀 있다. 그냥 계속 쳐.");
        return phrase(tone, "상태는 안정적입니다. 사냥을 계속해도 좋습니다.", "상태 괜찮다. 계속 사냥해도 안 죽겠다.");
    }

    private static String buildAggroStatus(PcInstance pc, String tone) {
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty())
            return phrase(tone, "주변 선공 몬스터는 감지되지 않았습니다.", "주변 선공몹 없다. 겁먹지 마.");
        MonsterInstance nearest = findNearest(pc, list);
        if (nearest == null)
            return phrase(tone, "선공 몬스터 기척은 있으나 대상을 특정하지 못했습니다.", "뭔가 있긴 한데 대상 특정은 안 된다. 움직여 봐.");
        String name = getMonsterName(nearest);
        int dist = Util.getDistance(pc, nearest);
        if (nearest.getMonster() != null && nearest.getMonster().isBoss())
            return "DANGER:" + phrase(tone, "보스급 선공 몬스터 %s 감지. 거리 %d칸. 위험합니다.", "보스급 %s다. 거리 %d칸. 객기 부리지 마.", name, dist);
        return phrase(tone, "선공 몬스터 %s 감지. 거리 %d칸. 주변 선공 수: %d", "선공몹 %s 감지. 거리 %d칸. 주변 선공 %d마리다.", name, dist, list.size());
    }

    private static void controlAttackNearestAggro(PcInstance pc, ItemInstance weapon, String tone) {
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, phrase(tone, "공격 제어 불가: %s", "공격 제어 못 한다. 이유: %s", EgoWeaponTypeUtil.getAbilityDenyReason("", weapon)));
            return;
        }
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty()) {
            say(pc, phrase(tone, "공격할 선공 몬스터가 없습니다.", "칠 선공몹 없다. 허공에 칼질할래?"));
            return;
        }
        MonsterInstance target = findNearest(pc, list);
        if (target == null) {
            say(pc, phrase(tone, "공격 대상을 찾지 못했습니다.", "타겟 못 찾았다. 위치부터 제대로 잡아."));
            return;
        }
        int hpRate = getHpRate(pc);
        if (hpRate <= 25) {
            danger(pc, phrase(tone, "체력이 너무 낮습니다. 공격 명령을 거부합니다. 먼저 회복하십시오.", "피가 너무 낮다. 공격 거부. 물약부터 먹어."));
            return;
        }
        pc.autoAttackTarget = target;
        pc.isAutoAttack = true;
        pc.setTarget(target);
        say(pc, phrase(tone, "%s 공격을 시작합니다. 원본 무기 타입: %s", "%s 친다. 원본 무기 타입은 %s다.", getMonsterName(target), EgoWeaponTypeUtil.getDisplayTypeName(weapon)));
    }

    private static void stopControl(PcInstance pc, String tone) {
        pc.isAutoAttack = false;
        pc.autoAttackTarget = null;
        pc.setTarget(null);
        try { pc.resetAutoAttack(); } catch (Throwable e) {}
        say(pc, phrase(tone, "전투 제어를 중지했습니다.", "멈췄다. 이제 네가 알아서 해."));
    }

    private static List<MonsterInstance> findAggressiveMonsters(PcInstance pc) {
        List<MonsterInstance> result = new ArrayList<MonsterInstance>();
        if (pc == null)
            return result;
        List<object> inside = pc.getInsideList();
        if (inside == null)
            return result;
        for (object o : inside) {
            if (!(o instanceof MonsterInstance))
                continue;
            MonsterInstance mon = (MonsterInstance) o;
            if (mon.isDead() || mon.getMonster() == null)
                continue;
            if (mon.getMap() != pc.getMap())
                continue;
            if (mon.getMonster().getAtkType() <= 0)
                continue;
            if (!Util.isDistance(pc, mon, Lineage.SEARCH_LOCATIONRANGE))
                continue;
            result.add(mon);
        }
        return result;
    }

    private static MonsterInstance findNearest(PcInstance pc, List<MonsterInstance> list) {
        MonsterInstance nearest = null;
        if (pc == null || list == null)
            return null;
        for (MonsterInstance mon : list) {
            if (mon == null)
                continue;
            if (nearest == null || Util.getDistance(pc, mon) < Util.getDistance(pc, nearest))
                nearest = mon;
        }
        return nearest;
    }

    private static String getMonsterName(MonsterInstance mon) {
        if (mon == null)
            return "알 수 없는 몬스터";
        if (mon.getMonster() != null && mon.getMonster().getName() != null)
            return mon.getMonster().getName();
        if (mon.getName() != null)
            return mon.getName();
        return "알 수 없는 몬스터";
    }

    private static int getHpRate(PcInstance pc) {
        return pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp());
    }

    private static int getMpRate(PcInstance pc) {
        return pc.getNowMp() * 100 / Math.max(1, pc.getTotalMp());
    }

    private static void sayByContent(PcInstance pc, String msg) {
        if (msg != null && msg.startsWith("DANGER:")) {
            danger(pc, msg.substring("DANGER:".length()));
            return;
        }
        say(pc, msg);
    }

    private static String phrase(String tone, String polite, String rude, Object... args) {
        String pattern = isRude(tone) ? rude : polite;
        if (args == null || args.length == 0)
            return pattern;
        return String.format(pattern, args);
    }

    private static String phrase(String tone, String polite, String rude) {
        return isRude(tone) ? rude : polite;
    }

    private static void say(PcInstance pc, String msg) {
        EgoMessageUtil.normal(pc, msg);
    }

    private static void danger(PcInstance pc, String msg) {
        EgoMessageUtil.danger(pc, msg);
    }

    private static void info(PcInstance pc, String msg) {
        EgoMessageUtil.info(pc, msg);
    }
}
