package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

import lineage.bean.lineage.Inventory;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 상대 캐릭터 감지/분석 컨트롤러.
 *
 * 대화 정책:
 * - 일반 채팅 호출은 EgoWeaponControlController에서 consume 처리한다.
 * - 상대감지 결과는 EgoMessageUtil을 통해 본인에게만 보이는 개인 메시지로 출력한다.
 * - 정확 HP/인벤토리/IP/계정 정보는 노출하지 않는다.
 */
public final class EgoOpponentScanController {

    private static final int SCAN_RANGE = Lineage.SEARCH_LOCATIONRANGE;
    private static final long SCAN_DELAY_MS = 1200L;
    private static final int MAX_LIST_COUNT = 5;

    private static final java.util.Map<Long, Long> scanDelayMap = new java.util.concurrent.ConcurrentHashMap<Long, Long>();

    private EgoOpponentScanController() {
    }

    public static boolean handleTalk(PcInstance pc, String command) {
        if (pc == null || command == null)
            return false;

        if (!isOpponentCommand(command))
            return false;

        if (!checkScanDelay(pc))
            return true;

        if (containsAny(command, "주변", "목록", "리스트", "플레이어", "캐릭")) {
            scanNearbyPlayers(pc);
            return true;
        }

        scanTargetOrNearest(pc);
        return true;
    }

    public static void scanTargetOrNearest(PcInstance pc) {
        if (pc == null)
            return;

        PcInstance target = getTargetPc(pc);
        if (target == null)
            target = findNearestPc(pc);

        if (target == null) {
            say(pc, "감지 범위 안에 상대 캐릭터가 없습니다.");
            return;
        }

        printOpponentInfo(pc, target);
    }

    public static void scanNearbyPlayers(PcInstance pc) {
        if (pc == null)
            return;

        List<PcInstance> list = findNearbyPlayers(pc);
        if (list.isEmpty()) {
            say(pc, "주변에 감지된 캐릭터가 없습니다.");
            return;
        }

        info(pc, String.format("주변 캐릭터 %d명 감지", list.size()));

        int count = 0;
        for (PcInstance target : list) {
            if (count >= MAX_LIST_COUNT)
                break;
            count++;

            int dist = Util.getDistance(pc, target);
            String relation = getRelation(pc, target);
            String className = getClassName(target);
            String risk = getRiskGrade(pc, target);

            sayByRisk(pc, risk, String.format("%d) %s %s %s / 거리 %d / 위험도 %s", count, relation, target.getName(), className, dist, risk));
        }

        if (list.size() > MAX_LIST_COUNT)
            info(pc, String.format("외 %d명 추가 감지", list.size() - MAX_LIST_COUNT));
    }

    private static boolean isOpponentCommand(String command) {
        return containsAny(command, "상대", "적", "타겟", "대상", "캐릭", "플레이어", "pvp", "분석", "위험도");
    }

    private static PcInstance getTargetPc(PcInstance pc) {
        if (pc == null || pc.getTarget() == null)
            return null;

        object target = pc.getTarget();
        if (!(target instanceof PcInstance))
            return null;

        PcInstance targetPc = (PcInstance) target;
        if (!isValidTarget(pc, targetPc))
            return null;

        return targetPc;
    }

    private static PcInstance findNearestPc(PcInstance pc) {
        List<PcInstance> list = findNearbyPlayers(pc);
        PcInstance nearest = null;

        for (PcInstance target : list) {
            if (nearest == null || Util.getDistance(pc, target) < Util.getDistance(pc, nearest))
                nearest = target;
        }

        return nearest;
    }

    private static List<PcInstance> findNearbyPlayers(PcInstance pc) {
        List<PcInstance> result = new ArrayList<PcInstance>();
        if (pc == null || pc.getInsideList() == null)
            return result;

        for (object o : pc.getInsideList()) {
            if (!(o instanceof PcInstance))
                continue;

            PcInstance target = (PcInstance) o;
            if (!isValidTarget(pc, target))
                continue;

            result.add(target);
        }

        return result;
    }

    private static boolean isValidTarget(PcInstance pc, PcInstance target) {
        if (pc == null || target == null)
            return false;

        if (pc.getObjectId() == target.getObjectId())
            return false;

        if (target.isDead() || target.isWorldDelete())
            return false;

        if (target.getMap() != pc.getMap())
            return false;

        if (!Util.isDistance(pc, target, SCAN_RANGE))
            return false;

        return true;
    }

    private static void printOpponentInfo(PcInstance pc, PcInstance target) {
        int dist = Util.getDistance(pc, target);
        String relation = getRelation(pc, target);
        String className = getClassName(target);
        String lawful = getLawfulName(target);
        String hpBand = getHpBand(target);
        String weaponType = getWeaponType(target);
        String risk = getRiskGrade(pc, target);
        String clan = target.getClanId() == 0 ? "무혈" : target.getClanName();
        String title = target.getTitle() == null || target.getTitle().length() == 0 ? "" : target.getTitle();

        info(pc, "========== 에고 상대 감지 ==========");
        sayByRisk(pc, risk, String.format("대상: %s %s %s", relation, target.getName(), className));
        if (title.length() > 0)
            info(pc, String.format("호칭: %s", title));
        info(pc, String.format("혈맹: %s / 성향: %s / PK: %d", clan, lawful, target.getPkCount()));
        info(pc, String.format("거리: %d칸 / HP상태: %s / 무기종류: %s", dist, hpBand, weaponType));
        sayByRisk(pc, risk, String.format("위험도: %s / 판단: %s", risk, buildAdvice(pc, target)));
    }

    private static String getRelation(PcInstance pc, PcInstance target) {
        if (pc == null || target == null)
            return "[알수없음]";
        if (pc.getClanId() != 0 && pc.getClanId() == target.getClanId())
            return "[혈맹]";
        if (target.getLawful() < Lineage.NEUTRAL)
            return "[카오]";
        if (target.getPkCount() > 0)
            return "[PK]";
        return "[중립]";
    }

    private static String getClassName(PcInstance target) {
        if (target == null)
            return "[알수없음]";

        switch (target.getClassType()) {
            case Lineage.LINEAGE_CLASS_ROYAL:
                return "[군주]";
            case Lineage.LINEAGE_CLASS_KNIGHT:
                return "[기사]";
            case Lineage.LINEAGE_CLASS_ELF:
                return "[요정]";
            case Lineage.LINEAGE_CLASS_WIZARD:
                return "[마법사]";
            case Lineage.LINEAGE_CLASS_DARKELF:
                return "[다크엘프]";
            default:
                return "[알수없음]";
        }
    }

    private static String getLawfulName(PcInstance target) {
        if (target == null)
            return "알수없음";
        if (target.getLawful() < Lineage.NEUTRAL)
            return "Chaotic";
        if (target.getLawful() < Lineage.NEUTRAL + 500)
            return "Neutral";
        return "Lawful";
    }

    private static String getHpBand(PcInstance target) {
        if (target == null)
            return "알수없음";
        int hpRate = target.getNowHp() * 100 / Math.max(1, target.getTotalHp());
        if (hpRate <= 25)
            return "위험";
        if (hpRate <= 50)
            return "낮음";
        if (hpRate <= 75)
            return "보통";
        return "높음";
    }

    private static String getWeaponType(PcInstance target) {
        if (target == null)
            return "알수없음";

        Inventory inv = target.getInventory();
        if (inv == null)
            return "알수없음";

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null)
            return "맨손";

        return EgoWeaponTypeUtil.getDisplayTypeName(weapon);
    }

    private static String getRiskGrade(PcInstance pc, PcInstance target) {
        if (pc == null || target == null)
            return "알수없음";

        int score = 0;
        int levelDiff = target.getLevel() - pc.getLevel();
        if (levelDiff >= 10)
            score += 4;
        else if (levelDiff >= 5)
            score += 3;
        else if (levelDiff >= 1)
            score += 1;

        int dist = Util.getDistance(pc, target);
        if (dist <= 2)
            score += 2;
        else if (dist <= 5)
            score += 1;

        if (target.getLawful() < Lineage.NEUTRAL)
            score += 2;

        if (target.getPkCount() >= 5)
            score += 2;
        else if (target.getPkCount() > 0)
            score += 1;

        String weaponType = getWeaponType(target);
        if ("양손검".equals(weaponType) || "도끼".equals(weaponType))
            score += 2;
        else if ("활".equals(weaponType) || "지팡이".equals(weaponType) || "완드".equals(weaponType))
            score += 1;

        if (score >= 7)
            return "매우위험";
        if (score >= 4)
            return "위험";
        if (score >= 2)
            return "주의";
        return "낮음";
    }

    private static String buildAdvice(PcInstance pc, PcInstance target) {
        String risk = getRiskGrade(pc, target);
        int dist = Util.getDistance(pc, target);

        if (pc.getClanId() != 0 && pc.getClanId() == target.getClanId())
            return "혈맹원입니다. 전투 대상이 아닐 가능성이 큽니다.";
        if ("매우위험".equals(risk))
            return "정면 교전은 피하고 거리 확보를 권합니다.";
        if ("위험".equals(risk))
            return "버프와 체력을 확인한 뒤 교전하십시오.";
        if (dist <= 2)
            return "근접 거리입니다. 선제 대응 또는 이탈 판단이 필요합니다.";
        return "즉시 위협은 낮지만 움직임을 주시하십시오.";
    }

    private static boolean containsAny(String text, String... keys) {
        if (text == null)
            return false;
        String lower = text.toLowerCase();
        for (String key : keys) {
            if (lower.contains(key.toLowerCase()))
                return true;
        }
        return false;
    }

    private static boolean checkScanDelay(PcInstance pc) {
        long now = java.lang.System.currentTimeMillis();
        Long last = scanDelayMap.get(pc.getObjectId());
        if (last != null && now - last.longValue() < SCAN_DELAY_MS)
            return false;
        scanDelayMap.put(pc.getObjectId(), now);
        return true;
    }

    private static void sayByRisk(PcInstance pc, String risk, String msg) {
        if ("매우위험".equals(risk) || "위험".equals(risk)) {
            EgoMessageUtil.danger(pc, msg);
            return;
        }
        EgoMessageUtil.normal(pc, msg);
    }

    private static void say(PcInstance pc, String msg) {
        EgoMessageUtil.normal(pc, msg);
    }

    private static void info(PcInstance pc, String msg) {
        EgoMessageUtil.info(pc, msg);
    }
}
