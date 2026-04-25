package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

import lineage.bean.lineage.Inventory;
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
 * 대화 정책:
 * - 사용자는 일반 채팅으로 에고를 호출한다.
 * - 에고 호출 채팅은 이 컨트롤러에서 consume 처리하여 주변 일반채팅으로 방송되지 않게 한다.
 * - 에고 응답은 EgoMessageUtil을 통해 본인에게만 보이는 개인 시스템 메시지로 전송한다.
 * - 에고 응답 색상은 EgoMessageUtil에서 통합 관리한다.
 */
public final class EgoWeaponControlController {

    private static final String DEFAULT_EGO_NAME = "에고";
    private static final long TALK_DELAY_MS = 800L;
    private static final long WARNING_DELAY_MS = 5000L;

    private static final java.util.Map<Long, Long> talkDelayMap = new java.util.concurrent.ConcurrentHashMap<Long, Long>();
    private static final java.util.Map<Long, Long> warningDelayMap = new java.util.concurrent.ConcurrentHashMap<Long, Long>();

    private EgoWeaponControlController() {
    }

    /**
     * 일반 채팅에서 호출된다.
     * true 반환: 해당 일반 채팅을 소비하여 다른 캐릭터에게 보이지 않게 한다.
     * false 반환: 에고 호출이 아니므로 기존 일반 채팅 흐름 유지.
     */
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

        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
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

        if (hpRate <= 35)
            danger(pc, String.format("위험합니다. 선공 몬스터 %s 접근, 거리 %d칸. 체력이 낮습니다.", name, dist));
        else if (nearest.getMonster() != null && nearest.getMonster().isBoss())
            danger(pc, String.format("보스급 선공 몬스터 %s 감지. 거리 %d칸.", name, dist));
        else
            say(pc, String.format("선공 몬스터 %s 감지. 거리 %d칸.", name, dist));
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
        return DEFAULT_EGO_NAME;
    }

    private static void handle(PcInstance pc, ItemInstance weapon, String command) {
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, "이 장비는 에고무기로 사용할 수 없습니다. " + EgoWeaponTypeUtil.getAbilityDenyReason("", weapon));
            return;
        }

        if (command == null || command.length() == 0) {
            say(pc, buildGreeting(pc, weapon));
            return;
        }

        if (EgoOpponentScanController.handleTalk(pc, command))
            return;

        if (containsAny(command, "상태", "정보", "내상태")) {
            say(pc, buildStatus(pc, weapon));
            return;
        }

        if (containsAny(command, "조언", "판단", "어때", "위험")) {
            sayByContent(pc, buildAdvice(pc, weapon));
            return;
        }

        if (containsAny(command, "선공", "몹", "몬스터", "감지")) {
            sayByContent(pc, buildAggroStatus(pc));
            return;
        }

        if (containsAny(command, "공격", "쳐", "잡아", "처리")) {
            controlAttackNearestAggro(pc, weapon);
            return;
        }

        if (containsAny(command, "멈춰", "중지", "정지", "스톱", "그만")) {
            stopControl(pc);
            return;
        }

        if (containsAny(command, "도움", "명령", "사용법")) {
            say(pc, "사용법: 에고 상태 / 에고 조언 / 에고 선공 / 에고 상대 / 에고 주변캐릭 / 에고 공격 / 에고 멈춰");
            info(pc, EgoWeaponTypeUtil.getSupportedWeaponTypesText());
            return;
        }

        say(pc, "명령을 이해하지 못했습니다. '상태', '조언', '선공', '상대', '공격', '멈춰'로 말씀하십시오.");
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

    private static String buildGreeting(PcInstance pc, ItemInstance weapon) {
        int hpRate = getHpRate(pc);

        if (hpRate <= 30)
            return "DANGER:부름보다 회복이 먼저입니다. 체력이 위험합니다.";

        List<MonsterInstance> aggroList = findAggressiveMonsters(pc);
        if (!aggroList.isEmpty())
            return "듣고 있습니다. 근처에 선공 몬스터 기척이 있습니다.";

        return String.format("부르셨습니까, 주인님. 현재 무기 종류는 %s입니다.", EgoWeaponTypeUtil.getDisplayTypeName(weapon));
    }

    private static String buildStatus(PcInstance pc, ItemInstance weapon) {
        int hpRate = getHpRate(pc);
        int mpRate = getMpRate(pc);

        return String.format("Lv.%d / HP %d%%(%d/%d) / MP %d%%(%d/%d) / 무기 +%d %s [%s]",
            pc.getLevel(), hpRate, pc.getNowHp(), pc.getTotalHp(), mpRate, pc.getNowMp(), pc.getTotalMp(), weapon.getEnLevel(), weapon.getName(), EgoWeaponTypeUtil.getDisplayTypeName(weapon));
    }

    private static String buildAdvice(PcInstance pc, ItemInstance weapon) {
        int hpRate = getHpRate(pc);
        int mpRate = getMpRate(pc);
        List<MonsterInstance> aggroList = findAggressiveMonsters(pc);

        if (hpRate <= 30 && !aggroList.isEmpty())
            return "DANGER:체력이 낮고 선공 몬스터가 있습니다. 교전보다 후퇴와 회복이 우선입니다.";
        if (hpRate <= 30)
            return "DANGER:체력이 낮습니다. 물약 사용 또는 후퇴를 권합니다.";
        if (mpRate <= 20 && EgoWeaponTypeUtil.isMagicWeapon(weapon))
            return "마나가 부족합니다. 지팡이/완드 계열 능력 효율이 낮아질 수 있습니다.";
        if (mpRate <= 20)
            return "마나가 부족합니다. 스킬 사용은 아끼고 평타 위주로 싸우십시오.";
        if (!aggroList.isEmpty()) {
            MonsterInstance nearest = findNearest(pc, aggroList);
            String name = nearest == null ? "알 수 없는 몬스터" : getMonsterName(nearest);
            return String.format("선공 몬스터 %s가 있습니다. 먼저 정리하는 것이 좋습니다.", name);
        }
        if (pc.getTarget() != null)
            return "현재 대상이 잡혀 있습니다. 전투 흐름은 유지 가능합니다.";
        return "상태는 안정적입니다. 사냥을 계속해도 좋습니다.";
    }

    private static String buildAggroStatus(PcInstance pc) {
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty())
            return "주변 선공 몬스터는 감지되지 않았습니다.";
        MonsterInstance nearest = findNearest(pc, list);
        if (nearest == null)
            return "선공 몬스터 기척은 있으나 대상을 특정하지 못했습니다.";
        String name = getMonsterName(nearest);
        int dist = Util.getDistance(pc, nearest);
        if (nearest.getMonster() != null && nearest.getMonster().isBoss())
            return String.format("DANGER:보스급 선공 몬스터 %s 감지. 거리 %d칸. 위험합니다.", name, dist);
        return String.format("선공 몬스터 %s 감지. 거리 %d칸. 주변 선공 수: %d", name, dist, list.size());
    }

    private static void controlAttackNearestAggro(PcInstance pc, ItemInstance weapon) {
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, "공격 제어 불가: " + EgoWeaponTypeUtil.getAbilityDenyReason("", weapon));
            return;
        }
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty()) {
            say(pc, "공격할 선공 몬스터가 없습니다.");
            return;
        }
        MonsterInstance target = findNearest(pc, list);
        if (target == null) {
            say(pc, "공격 대상을 찾지 못했습니다.");
            return;
        }
        int hpRate = getHpRate(pc);
        if (hpRate <= 25) {
            danger(pc, "체력이 너무 낮습니다. 공격 명령을 거부합니다. 먼저 회복하십시오.");
            return;
        }
        pc.autoAttackTarget = target;
        pc.isAutoAttack = true;
        pc.setTarget(target);
        say(pc, String.format("%s 공격을 시작합니다. 무기 종류: %s", getMonsterName(target), EgoWeaponTypeUtil.getDisplayTypeName(weapon)));
    }

    private static void stopControl(PcInstance pc) {
        pc.isAutoAttack = false;
        pc.autoAttackTarget = null;
        pc.setTarget(null);
        try { pc.resetAutoAttack(); } catch (Throwable e) {}
        say(pc, "전투 제어를 중지했습니다.");
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
