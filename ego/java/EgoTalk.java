package lineage.world.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * 에고 대화 통합 클래스.
 *
 * 일반 채팅 처리, 장르 대화, 자동 경고 대화를 이 파일에서 처리한다.
 */
public final class EgoTalk {

    private static final long DEFAULT_GENRE_TALK_DELAY_MS = 1200L;
    private static final Map<Long, Long> genreDelayMap = new ConcurrentHashMap<Long, Long>();

    private EgoTalk() {
    }

    /**
     * 일반채팅 처리.
     * true 반환 시 해당 채팅은 주변에 방송하지 않고 소비해야 합니다.
     */
    public static boolean chat(PcInstance pc, String msg) {
        if (tryGenreTalk(pc, msg))
            return true;
        boolean handled = EgoWeaponControlController.onNormalChat(pc, msg);
        if (handled)
            addTalkBond(pc);
        return handled;
    }

    /** 자동 경고 체크. */
    public static void warning(PcInstance pc) {
        EgoAutoTalk.warning(pc);
        EgoWeaponControlController.checkAutoWarning(pc);
    }

    private static boolean tryGenreTalk(PcInstance pc, String msg) {
        if (pc == null || msg == null)
            return false;

        Inventory inv = pc.getInventory();
        if (inv == null)
            return false;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !EgoWeaponDatabase.isEgoWeapon(weapon))
            return false;

        String egoName = EgoWeaponDatabase.getEgoName(weapon, "에고");
        String text = msg.trim();
        if (text.length() == 0)
            return false;

        String command = extractCommand(text, egoName);
        if (command == null || command.length() == 0)
            return false;

        if (EgoGenreGuide.isGuideRequest(command)) {
            if (!checkGenreDelay(pc))
                return EgoMessageUtil.consumeNormalChat();
            String guide;
            if (command.indexOf("추천") >= 0)
                guide = EgoGenreGuide.suggest(weapon);
            else
                guide = EgoGenreGuide.guide(weapon);
            EgoTalkHistory.remember(pc, guide);
            EgoMessageUtil.info(pc, guide);
            EgoBond.addTalk(pc, weapon);
            return EgoMessageUtil.consumeNormalChat();
        }

        if (!EgoGenreTalk.isGenreRequest(command))
            return false;

        if (!checkGenreDelay(pc))
            return EgoMessageUtil.consumeNormalChat();

        String answer = EgoTalkPack.find(pc, weapon, command);
        if (answer == null || answer.length() == 0)
            answer = EgoGenreTalk.talk(pc, weapon, command);
        if (answer == null || answer.length() == 0)
            return false;

        EgoTalkHistory.remember(pc, answer);
        EgoMessageUtil.genre(pc, answer);
        EgoBond.addTalk(pc, weapon);
        return EgoMessageUtil.consumeNormalChat();
    }

    private static void addTalkBond(PcInstance pc) {
        if (pc == null)
            return;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return;
        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !EgoWeaponDatabase.isEgoWeapon(weapon))
            return;
        EgoBond.addTalk(pc, weapon);
    }

    private static String extractCommand(String text, String egoName) {
        if (egoName == null || egoName.length() == 0)
            egoName = "에고";

        if (text.equalsIgnoreCase(egoName))
            return "";
        if (text.toLowerCase().startsWith(egoName.toLowerCase() + " "))
            return text.substring(egoName.length()).trim();
        if (text.equalsIgnoreCase(egoName + "야") || text.equalsIgnoreCase(egoName + "님"))
            return "";
        if (text.startsWith(egoName + "야 "))
            return text.substring((egoName + "야 ").length()).trim();
        if (text.startsWith(egoName + "님 "))
            return text.substring((egoName + "님 ").length()).trim();
        return null;
    }

    private static boolean checkGenreDelay(PcInstance pc) {
        if (pc == null)
            return false;
        long now = java.lang.System.currentTimeMillis();
        long delay = EgoConfig.getLong("genre_talk_delay_ms", DEFAULT_GENRE_TALK_DELAY_MS);
        Long last = genreDelayMap.get(pc.getObjectId());
        if (last != null && now - last.longValue() < delay)
            return false;
        genreDelayMap.put(pc.getObjectId(), now);
        return true;
    }
}

/**
 * 에고 상황별 자동 대사.
 * 일반 루프/경고 체크에서 EgoTalk.warning(pc)를 호출하면 자동으로 동작한다.
 */
final class EgoAutoTalk {

    private static final String TONE_RUDE = "예의반대";
    private static final Map<String, Long> delayMap = new ConcurrentHashMap<String, Long>();

    private EgoAutoTalk() {
    }

    static void warning(PcInstance pc) {
        if (pc == null || pc instanceof RobotInstance)
            return;

        Inventory inv = pc.getInventory();
        if (inv == null)
            return;

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null || !EgoWeaponDatabase.isEgoWeapon(weapon))
            return;

        String tone = EgoWeaponDatabase.getTone(weapon);
        int hpRate = pc.getNowHp() * 100 / Math.max(1, pc.getTotalHp());
        int mpRate = pc.getNowMp() * 100 / Math.max(1, pc.getTotalMp());

        int hpWarnRate = EgoConfig.percent("auto_talk_hp_warn_rate", 25);
        int mpWarnRate = EgoConfig.percent("auto_talk_mp_warn_rate", 15);
        int idleHpRate = EgoConfig.percent("auto_talk_idle_hp_rate", 80);
        int idleMpRate = EgoConfig.percent("auto_talk_idle_mp_rate", 50);

        if (hpRate <= hpWarnRate && check(pc, "HP", EgoConfig.getLong("auto_talk_hp_warn_delay_ms", 15000L))) {
            EgoMessageUtil.danger(pc, phrase(tone,
                "HP가 매우 낮습니다. 즉시 회복하거나 거리를 벌리십시오.",
                "HP 진짜 낮다. 지금 물약 안 먹으면 눕는다."));
            EgoBond.addDangerSurvive(weapon);
            return;
        }

        if (mpRate <= mpWarnRate && check(pc, "MP", EgoConfig.getLong("auto_talk_mp_warn_delay_ms", 20000L))) {
            EgoMessageUtil.info(pc, phrase(tone,
                "MP가 부족합니다. 스킬 사용을 줄이고 회복 시간을 확보하십시오.",
                "MP 바닥이다. 스킬 낭비 그만하고 숨 좀 돌려."));
            return;
        }

        MonsterInstance boss = findBoss(pc);
        if (boss != null && check(pc, "BOSS", EgoConfig.getLong("auto_talk_boss_warn_delay_ms", 30000L))) {
            EgoMessageUtil.danger(pc, phrase(tone,
                "보스급 기척이 감지됩니다. 대상: %s. 무리한 교전은 피하십시오.",
                "보스급 %s 보인다. 객기 부리면 바로 눕는다.", getMonsterName(boss)));
            return;
        }

        if (hpRate >= idleHpRate && mpRate >= idleMpRate && check(pc, "IDLE", EgoConfig.getLong("auto_talk_idle_delay_ms", 180000L))) {
            EgoMessageUtil.genre(pc, phrase(tone,
                "상태는 안정적입니다. 지금은 차분히 성장하기 좋은 흐름입니다.",
                "상태 괜찮다. 지금은 무리만 안 하면 된다."));
        }
    }

    private static boolean check(PcInstance pc, String type, long delay) {
        long now = java.lang.System.currentTimeMillis();
        String key = pc.getObjectId() + ":" + type;
        Long last = delayMap.get(key);
        if (last != null && now - last.longValue() < delay)
            return false;
        delayMap.put(key, now);
        return true;
    }

    private static MonsterInstance findBoss(PcInstance pc) {
        List<object> inside = pc.getInsideList();
        if (inside == null)
            return null;
        for (object o : inside) {
            if (!(o instanceof MonsterInstance))
                continue;
            MonsterInstance mon = (MonsterInstance) o;
            if (mon.isDead() || mon.getMonster() == null)
                continue;
            if (mon.getMap() != pc.getMap())
                continue;
            if (!Util.isDistance(pc, mon, Lineage.SEARCH_LOCATIONRANGE))
                continue;
            if (mon.getMonster().isBoss())
                return mon;
        }
        return null;
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

    private static boolean isRude(String tone) {
        return TONE_RUDE.equals(EgoWeaponDatabase.normalizeTone(tone));
    }

    private static String phrase(String tone, String polite, String rude) {
        return isRude(tone) ? rude : polite;
    }

    private static String phrase(String tone, String polite, String rude, Object... args) {
        String pattern = isRude(tone) ? rude : polite;
        return String.format(pattern, args);
    }
}
