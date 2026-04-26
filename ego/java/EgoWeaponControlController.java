package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

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

/**
 * 에고무기 대화/상태인식/간단 제어 컨트롤러.
 *
 * 대화 말투:
 * - 예의: 공손/존댓말
 * - 예의반대: 건방진 반말/도발형
 *
 * 자연대화:
 * - 명령어가 아닌 일반 문장도 인사/감정/칭찬/걱정/사냥잡담/에고정체성/이어말로 해석한다.
 */
public final class EgoWeaponControlController {

    private static final String DEFAULT_EGO_NAME = "에고";
    private static final String TONE_POLITE = "예의";
    private static final String TONE_RUDE = "예의반대";
    private static final long TALK_DELAY_MS = 800L;
    private static final long WARNING_DELAY_MS = 5000L;

    private static final java.util.Map<Long, Long> talkDelayMap = new java.util.concurrent.ConcurrentHashMap<Long, Long>();
    private static final java.util.Map<Long, Long> warningDelayMap = new java.util.concurrent.ConcurrentHashMap<Long, Long>();
    private static final java.util.Map<Long, String> lastTopicMap = new java.util.concurrent.ConcurrentHashMap<Long, String>();

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
            setLastTopic(pc, "greeting");
            say(pc, buildGreeting(pc, weapon, tone));
            return;
        }

        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, phrase(tone, "이 장비는 에고무기로 사용할 수 없습니다. %s", "이건 에고무기로 못 써. 이유: %s", EgoWeaponTypeUtil.getAbilityDenyReason("", weapon)));
            return;
        }

        if (EgoOpponentScanController.handleTalk(pc, command)) {
            setLastTopic(pc, "opponent");
            return;
        }

        if (containsAny(command, "말투", "예의", "예의반대", "반말", "막말")) {
            handleToneTalk(pc, weapon, command);
            setLastTopic(pc, "tone");
            return;
        }

        if (containsAny(command, "상태", "정보", "내상태")) {
            setLastTopic(pc, "status");
            say(pc, buildStatus(pc, weapon, tone));
            return;
        }

        if (containsAny(command, "조언", "판단", "어때", "위험")) {
            setLastTopic(pc, "advice");
            sayByContent(pc, buildAdvice(pc, weapon, tone));
            return;
        }

        if (containsAny(command, "선공", "몹", "몬스터", "감지")) {
            setLastTopic(pc, "aggro");
            sayByContent(pc, buildAggroStatus(pc, tone));
            return;
        }

        if (containsAny(command, "공격", "쳐", "잡아", "처리")) {
            setLastTopic(pc, "attack");
            controlAttackNearestAggro(pc, weapon, tone);
            return;
        }

        if (containsAny(command, "멈춰", "중지", "정지", "스톱", "그만")) {
            setLastTopic(pc, "stop");
            stopControl(pc, tone);
            return;
        }

        if (containsAny(command, "도움", "명령", "사용법")) {
            setLastTopic(pc, "help");
            say(pc, phrase(tone, "사용법: 에고 상태 / 에고 조언 / 에고 선공 / 에고 상대 / 에고 주변캐릭 / 에고 공격 / 에고 멈춰 / 에고 말투 예의 / 에고 말투 예의반대", "쓸 말은 이거다: 상태 / 조언 / 선공 / 상대 / 주변캐릭 / 공격 / 멈춰 / 말투 예의 / 말투 예의반대"));
            info(pc, EgoWeaponTypeUtil.getSupportedWeaponTypesText());
            return;
        }

        if (containsAny(command, "활", "양검", "양손검", "한검", "한손검", "단검", "창", "도끼", "지팡이", "완드", "변신", "변형")) {
            setLastTopic(pc, "weapon_form");
            info(pc, phrase(tone, "무기변형 기능은 제거되었습니다. 에고는 원본 무기 타입 그대로 성장/보조능력만 적용합니다.", "무기변형은 없어. 원본 무기로만 싸우는 거다."));
            return;
        }

        String natural = buildNaturalTalk(pc, weapon, tone, command);
        if (natural != null && natural.length() > 0) {
            sayByContent(pc, natural);
            return;
        }

        setLastTopic(pc, "unknown");
        say(pc, phrase(tone, "말씀은 들었습니다. 지금은 상태, 조언, 선공, 상대, 공격, 멈춰 중 하나로 이어가면 바로 도와드릴 수 있습니다.", "듣긴 했는데 애매하다. 상태, 조언, 선공, 상대, 공격, 멈춰 중에 제대로 말해."));
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

    private static String buildNaturalTalk(PcInstance pc, ItemInstance weapon, String tone, String command) {
        String text = normalize(command);
        String topic = getLastTopic(pc);
        int hpRate = getHpRate(pc);
        int mpRate = getMpRate(pc);
        EgoWeaponInfo ego = EgoWeaponDatabase.find(weapon);
        int egoLevel = ego == null ? 0 : ego.level;
        String egoName = getEgoName(weapon);
        String playerName = safePcName(pc);

        if (containsAny(text, "안녕", "하이", "ㅎㅇ", "반가", "왔어", "나야", "부름", "불렀")) {
            setLastTopic(pc, "greeting");
            return pick(tone,
                new String[] {
                    String.format("어서 오십시오, %s님. 오늘도 곁에서 전투 흐름을 살피겠습니다.", playerName),
                    "부르셨습니까. 상태를 살피고 필요하면 바로 알려드리겠습니다.",
                    String.format("반갑습니다. %s은 아직 조용하지만, 제 감각은 깨어 있습니다.", egoName)
                },
                new String[] {
                    String.format("왔냐, %s. 늦지는 않았네.", playerName),
                    "불렀으면 말해. 멍하니 서 있지 말고.",
                    String.format("그래, %s 깨어 있다. 이번엔 좀 제대로 움직여 봐.", egoName)
                }
            );
        }

        if (containsAny(text, "고마", "감사", "수고", "잘했", "최고", "든든", "멋지")) {
            setLastTopic(pc, "thanks");
            return pick(tone,
                new String[] {
                    "도움이 되었다니 다행입니다. 계속 주인님의 흐름을 보조하겠습니다.",
                    "감사합니다. 저는 주인님과 함께 성장하는 무기입니다.",
                    "그 말씀만으로도 충분합니다. 다음 전투도 맡겨주십시오."
                },
                new String[] {
                    "이제야 내 가치를 알아보네. 그래, 계속 잘 써먹어.",
                    "칭찬은 됐고, 다음 전투나 제대로 해.",
                    "당연하지. 내가 평범한 쇳덩이로 보였냐?"
                }
            );
        }

        if (containsAny(text, "미안", "죄송", "실수", "잘못")) {
            setLastTopic(pc, "sorry");
            return pick(tone,
                new String[] {
                    "괜찮습니다. 실수는 기록보다 다음 판단이 중요합니다.",
                    "사과는 받겠습니다. 다음에는 제가 더 빠르게 경고하겠습니다.",
                    "흔들리지 마십시오. 아직 바로잡을 수 있습니다."
                },
                new String[] {
                    "알면 됐다. 다음엔 같은 실수 하지 마.",
                    "미안할 시간에 자세부터 고쳐.",
                    "그래, 실수했다. 근데 아직 끝난 건 아니잖아."
                }
            );
        }

        if (containsAny(text, "힘들", "피곤", "졸려", "지침", "쉬고", "휴식", "잠깐")) {
            setLastTopic(pc, "tired");
            if (hpRate <= 40)
                return "DANGER:" + phrase(tone, "체력도 낮습니다. 무리하지 말고 안전한 위치에서 회복하십시오.", "피도 낮은데 피곤하다고? 안전한 데 가서 쉬어. 쓰러지지 말고.");
            return pick(tone,
                new String[] {
                    "무리한 전투는 판단을 흐립니다. 잠시 정비하는 것도 좋은 선택입니다.",
                    "쉬어도 괜찮습니다. 제가 주변 기척은 계속 살피겠습니다.",
                    "지금은 크게 위험하지 않습니다. 장비와 물약을 정리하며 숨을 고르십시오."
                },
                new String[] {
                    "피곤하면 쉬어. 억지로 하다 눕는 것보단 낫다.",
                    "멈출 줄 아는 것도 실력이다. 오늘은 그 정도는 알겠지?",
                    "쉬어도 된다. 대신 멍때리다 선공몹한테 맞지는 마."
                }
            );
        }

        if (containsAny(text, "무서", "불안", "위험할까", "죽을", "살려", "도와")) {
            setLastTopic(pc, "fear");
            if (hpRate <= 35)
                return "DANGER:" + phrase(tone, "위험합니다. 지금은 대화보다 회복과 거리 확보가 우선입니다.", "진짜 위험하다. 말 그만하고 피부터 채워.");
            return buildAdvice(pc, weapon, tone);
        }

        if (containsAny(text, "심심", "재미", "말해", "얘기", "대화", "놀자", "뭐해")) {
            setLastTopic(pc, "smalltalk");
            return pick(tone,
                new String[] {
                    String.format("저는 %s 안에서 주인님의 박자와 적의 기척을 듣고 있습니다.", EgoView.displayName(weapon)),
                    "대화는 가능합니다. 다만 전투 중이면 제 말보다 발밑을 먼저 보십시오.",
                    "저는 말상대이면서 전투 보조자입니다. 지금은 주변이 비교적 조용합니다."
                },
                new String[] {
                    String.format("뭐하긴. %s 안에서 네 삽질을 감시 중이지.", EgoView.displayName(weapon)),
                    "심심하면 몹이나 찾아. 나는 말동무도 되지만 장식품은 아니다.",
                    "대화는 해준다. 대신 맞고 나서 징징대진 마."
                }
            );
        }

        if (containsAny(text, "너 누구", "정체", "뭐야", "에고가 뭐", "너는", "너 뭐")) {
            setLastTopic(pc, "identity");
            return phrase(tone,
                "저는 주인님의 무기에 깃든 에고입니다. 전투 상황을 읽고, 성장하며, 필요할 때 조언과 보조능력을 발동합니다.",
                "나는 네 무기에 깃든 에고다. 네 전투를 보고, 판단하고, 가끔 네 목숨도 건져주는 존재지."
            );
        }

        if (containsAny(text, "강해", "성장", "레벨", "경험치", "얼마나", "언제 커")) {
            setLastTopic(pc, "growth");
            long exp = ego == null ? 0 : ego.exp;
            long need = ego == null ? EgoWeaponDatabase.getNeedExp(0) : ego.maxExp;
            if (egoLevel >= EgoWeaponDatabase.MAX_EGO_LEVEL)
                return phrase(tone, "저는 이미 최대 레벨입니다. 이제 성장보다 발동 타이밍이 중요합니다.", "나 만렙이다. 이제 문제는 내가 아니라 네 손이다.");
            return phrase(tone,
                "현재 에고 레벨은 %d이고 경험치는 %,d/%,d입니다. 전투를 계속하면 조금씩 성장합니다.",
                "지금 에고 레벨 %d, 경험치 %,d/%,d다. 더 키우고 싶으면 더 싸워.",
                egoLevel, exp, need
            );
        }

        if (containsAny(text, "배고", "물약", "회복", "피 없어", "마나 없어", "mp", "hp")) {
            setLastTopic(pc, "resource");
            if (hpRate <= 35)
                return "DANGER:" + phrase(tone, "HP가 낮습니다. 즉시 회복하거나 안전 거리로 이동하십시오. 현재 HP %d%%입니다.", "HP %d%%다. 지금 장난하냐? 물약 먹어.", hpRate);
            if (mpRate <= 20)
                return phrase(tone, "MP가 낮습니다. 스킬 사용을 줄이고 평타 위주로 전투하십시오. 현재 MP %d%%입니다.", "MP %d%%밖에 없다. 스킬 낭비하지 말고 평타 쳐.", mpRate);
            return phrase(tone, "현재 HP %d%%, MP %d%%입니다. 당장은 큰 문제 없습니다.", "HP %d%%, MP %d%%다. 아직은 멀쩡하네.", hpRate, mpRate);
        }

        if (containsAny(text, "사냥", "어디", "가자", "갈까", "잡을까", "계속")) {
            setLastTopic(pc, "hunt");
            List<MonsterInstance> list = findAggressiveMonsters(pc);
            if (!list.isEmpty())
                return buildAggroStatus(pc, tone);
            if (hpRate <= 35)
                return "DANGER:" + phrase(tone, "사냥보다 회복이 먼저입니다. HP가 %d%%입니다.", "사냥은 무슨. HP %d%%다. 먼저 살아남아.", hpRate);
            return phrase(tone, "지금은 주변 선공 몬스터가 없습니다. 이동하면서 대상을 찾으면 제가 알려드리겠습니다.", "주변 선공몹 없다. 움직여. 있으면 내가 말해줄게.");
        }

        if (containsAny(text, "좋아", "그래", "오케이", "ㅇㅋ", "알겠", "그럼", "그래서", "왜")) {
            setLastTopic(pc, "follow");
            return buildFollowUp(pc, weapon, tone, topic);
        }

        if (containsAny(text, "바보", "멍청", "싫어", "짜증", "꺼져", "시끄")) {
            setLastTopic(pc, "complaint");
            return pick(tone,
                new String[] {
                    "불편하셨다면 말투를 조정하겠습니다. 그래도 전투 판단은 계속 보조하겠습니다.",
                    "감정은 이해했습니다. 필요하시면 '말투 예의반대' 또는 '말투 예의'로 바꿀 수 있습니다.",
                    "저는 주인님을 방해하려는 것이 아닙니다. 생존에 필요한 말만 하겠습니다."
                },
                new String[] {
                    "싫어도 들을 건 들어. 네가 누우면 나도 조용해지거든.",
                    "짜증나면 말투 바꿔. 그래도 위험하면 말할 거다.",
                    "시끄러워도 필요한 말은 한다. 살아있을 때 투덜대."
                }
            );
        }

        setLastTopic(pc, "free");
        return pick(tone,
            new String[] {
                "알겠습니다. 계속 듣고 있습니다. 필요하면 상태나 조언이라고 말씀해주십시오.",
                "그 말씀은 기억해두겠습니다. 지금은 전투 흐름도 함께 살피겠습니다.",
                "대화는 이어가겠습니다. 다만 주변이 바뀌면 먼저 경고드리겠습니다."
            },
            new String[] {
                "그래, 듣고 있다. 근데 위험하면 내가 먼저 끼어든다.",
                "말은 알겠다. 이제 주변도 좀 봐라.",
                "응, 계속 말해. 대신 몹 오면 대화 끊고 싸워야 한다."
            }
        );
    }

    private static String buildFollowUp(PcInstance pc, ItemInstance weapon, String tone, String topic) {
        if ("advice".equals(topic) || "fear".equals(topic))
            return buildAdvice(pc, weapon, tone);
        if ("aggro".equals(topic) || "hunt".equals(topic))
            return buildAggroStatus(pc, tone);
        if ("status".equals(topic) || "resource".equals(topic))
            return buildStatus(pc, weapon, tone);
        if ("attack".equals(topic))
            return phrase(tone, "공격 흐름은 유지 중입니다. 체력이 떨어지면 즉시 회복하십시오.", "공격은 계속 중이다. 피 떨어지면 바로 물약 먹어.");
        if ("stop".equals(topic))
            return phrase(tone, "멈춘 상태입니다. 다시 공격하려면 공격이라고 말씀하십시오.", "멈춰 있다. 다시 치려면 공격이라고 해.");
        return phrase(tone, "네, 계속 듣고 있습니다. 원하시면 상태, 조언, 선공 중 하나로 바로 확인해드리겠습니다.", "그래, 듣고 있다. 상태, 조언, 선공 중 하나 말하면 바로 봐준다.");
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

    private static String pick(String tone, String[] polite, String[] rude) {
        String[] arr = isRude(tone) ? rude : polite;
        if (arr == null || arr.length == 0)
            return "...";
        return arr[Util.random(0, arr.length - 1)];
    }

    private static String normalize(String value) {
        if (value == null)
            return "";
        return value.trim().replace("?", "").replace("!", "").replace("~", "").replace(".", "").toLowerCase();
    }

    private static void setLastTopic(PcInstance pc, String topic) {
        if (pc != null && topic != null)
            lastTopicMap.put(pc.getObjectId(), topic);
    }

    private static String getLastTopic(PcInstance pc) {
        if (pc == null)
            return "";
        String topic = lastTopicMap.get(pc.getObjectId());
        return topic == null ? "" : topic;
    }

    private static String safePcName(PcInstance pc) {
        if (pc == null || pc.getName() == null || pc.getName().trim().length() == 0)
            return "주인님";
        return pc.getName();
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
