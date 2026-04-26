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
 * 에고 상황별 자동 대사.
 *
 * 테스트 명령 없음.
 * 일반 루프/경고 체크에서 EgoTalk.warning(pc)를 호출하면 자동으로 동작한다.
 */
public final class EgoAutoTalk {

    private static final String TONE_RUDE = "예의반대";
    private static final long HP_WARN_DELAY_MS = 15000L;
    private static final long MP_WARN_DELAY_MS = 20000L;
    private static final long IDLE_TALK_DELAY_MS = 180000L;
    private static final long BOSS_WARN_DELAY_MS = 30000L;

    private static final Map<String, Long> delayMap = new ConcurrentHashMap<String, Long>();

    private EgoAutoTalk() {
    }

    public static void warning(PcInstance pc) {
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

        if (hpRate <= 25 && check(pc, "HP", HP_WARN_DELAY_MS)) {
            EgoMessageUtil.danger(pc, phrase(tone,
                "HP가 매우 낮습니다. 즉시 회복하거나 거리를 벌리십시오.",
                "HP 진짜 낮다. 지금 물약 안 먹으면 눕는다."));
            EgoBond.addDangerSurvive(weapon);
            return;
        }

        if (mpRate <= 15 && check(pc, "MP", MP_WARN_DELAY_MS)) {
            EgoMessageUtil.info(pc, phrase(tone,
                "MP가 부족합니다. 스킬 사용을 줄이고 회복 시간을 확보하십시오.",
                "MP 바닥이다. 스킬 낭비 그만하고 숨 좀 돌려."));
            return;
        }

        MonsterInstance boss = findBoss(pc);
        if (boss != null && check(pc, "BOSS", BOSS_WARN_DELAY_MS)) {
            EgoMessageUtil.danger(pc, phrase(tone,
                "보스급 기척이 감지됩니다. 대상: %s. 무리한 교전은 피하십시오.",
                "보스급 %s 보인다. 객기 부리면 바로 눕는다.", getMonsterName(boss)));
            return;
        }

        if (hpRate >= 80 && mpRate >= 50 && check(pc, "IDLE", IDLE_TALK_DELAY_MS)) {
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
