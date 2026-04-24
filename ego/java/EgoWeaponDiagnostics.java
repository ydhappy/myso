package lineage.world.controller;

import java.util.ArrayList;
import java.util.List;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 진단 도구.
 *
 * 적용 위치:
 * - bitna/src/lineage/world/controller/EgoWeaponDiagnostics.java
 *
 * 용도:
 * - .에고검사 명령에서 호출한다.
 * - 초보자 적용 후 가장 먼저 문제를 확인할 수 있게 한다.
 */
public final class EgoWeaponDiagnostics {

    private EgoWeaponDiagnostics() {
    }

    public static void printDiagnosis(PcInstance pc) {
        if (pc == null)
            return;

        msg(pc, "\\fY========== 에고무기 진단 시작 ==========");
        checkCharacter(pc);
        ItemInstance weapon = checkWeapon(pc);
        checkDatabase(pc, weapon);
        checkAbility(pc, weapon);
        checkAggressiveMonster(pc);
        msg(pc, "\\fY========== 에고무기 진단 종료 ==========");
    }

    private static void checkCharacter(PcInstance pc) {
        msg(pc, String.format("캐릭터: %s / Lv.%d / HP %d/%d / MP %d/%d", pc.getName(), pc.getLevel(), pc.getNowHp(), pc.getTotalHp(), pc.getNowMp(), pc.getTotalMp()));
        msg(pc, String.format("좌표: %d,%d,%d", pc.getX(), pc.getY(), pc.getMap()));
    }

    private static ItemInstance checkWeapon(PcInstance pc) {
        Inventory inv = pc.getInventory();
        if (inv == null) {
            msg(pc, "\\fR[FAIL] 인벤토리 객체가 null 입니다.");
            return null;
        }

        ItemInstance weapon = inv.getSlot(Lineage.SLOT_WEAPON);
        if (weapon == null) {
            msg(pc, "\\fR[FAIL] 무기를 착용하지 않았습니다.");
            return null;
        }

        msg(pc, String.format("착용무기: +%d %s / objectId=%d", weapon.getEnLevel(), weapon.getName(), weapon.getObjectId()));

        if (weapon.getItem() == null) {
            msg(pc, "\\fR[FAIL] weapon.getItem()이 null 입니다. 아이템 DB 로딩 또는 인스턴스 생성 문제입니다.");
            return weapon;
        }

        msg(pc, String.format("무기종류: %s / type2=%s / slot=%d", EgoWeaponTypeUtil.getDisplayTypeName(weapon), EgoWeaponTypeUtil.getType2(weapon), weapon.getItem().getSlot()));

        if (EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            msg(pc, "\\fY[OK] 에고무기로 사용할 수 있는 전투 무기입니다.");
        } else {
            msg(pc, "\\fR[FAIL] 에고무기로 사용할 수 없습니다: " + EgoWeaponTypeUtil.getAbilityDenyReason("", weapon));
        }

        return weapon;
    }

    private static void checkDatabase(PcInstance pc, ItemInstance weapon) {
        if (weapon == null)
            return;

        EgoWeaponInfo info = EgoWeaponDatabase.find(weapon);
        if (info == null) {
            msg(pc, "\\fR[WARN] DB 캐시에 에고 정보가 없습니다. .에고생성 또는 .에고리로드를 확인하세요.");
            return;
        }

        msg(pc, String.format("\\fY[OK] DB 에고: enabled=%s / name=%s / personality=%s", info.enabled, safe(info.egoName), safe(info.personality)));
        msg(pc, String.format("에고성장: level=%d / exp=%d / maxExp=%d / talk=%d / control=%d", info.level, info.exp, info.maxExp, info.talkLevel, info.controlLevel));
    }

    private static void checkAbility(PcInstance pc, ItemInstance weapon) {
        if (weapon == null)
            return;

        String recommend = EgoWeaponTypeUtil.getDefaultAbilityType(weapon);
        msg(pc, "추천 능력: " + recommend);

        List<EgoAbilityInfo> list = EgoWeaponDatabase.getAbilities(weapon);
        if (list.isEmpty()) {
            msg(pc, "\\fR[WARN] DB 캐시에 활성 능력이 없습니다. .에고능력 " + recommend + " 를 실행할 수 있습니다.");
            return;
        }

        for (EgoAbilityInfo ai : list) {
            boolean allowed = EgoWeaponTypeUtil.isAbilityAllowed(ai.abilityType, weapon);
            if (allowed) {
                msg(pc, String.format("\\fY[OK] 능력: %s Lv.%d 허용됨", safe(ai.abilityType), ai.abilityLevel));
            } else {
                msg(pc, String.format("\\fR[FAIL] 능력: %s 는 현재 무기(%s)에 허용되지 않습니다.", safe(ai.abilityType), EgoWeaponTypeUtil.getDisplayTypeName(weapon)));
            }
        }
    }

    private static void checkAggressiveMonster(PcInstance pc) {
        List<MonsterInstance> list = findAggressiveMonsters(pc);
        if (list.isEmpty()) {
            msg(pc, "선공몬스터: 주변 감지 없음");
            return;
        }

        MonsterInstance nearest = null;
        for (MonsterInstance mon : list) {
            if (nearest == null || Util.getDistance(pc, mon) < Util.getDistance(pc, nearest))
                nearest = mon;
        }

        if (nearest != null) {
            msg(pc, String.format("선공몬스터: %d마리 / 최근접=%s / 거리=%d", list.size(), getMonsterName(nearest), Util.getDistance(pc, nearest)));
        }
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

    private static String getMonsterName(MonsterInstance mon) {
        if (mon == null)
            return "알 수 없음";
        if (mon.getMonster() != null && mon.getMonster().getName() != null)
            return mon.getMonster().getName();
        if (mon.getName() != null)
            return mon.getName();
        return "알 수 없음";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void msg(PcInstance pc, String msg) {
        ChattingController.toChatting(pc, msg, Lineage.CHATTING_MODE_MESSAGE);
    }
}
