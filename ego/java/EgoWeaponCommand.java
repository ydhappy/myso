package lineage.world.controller;

import java.util.List;
import java.util.StringTokenizer;

import lineage.bean.lineage.Inventory;
import lineage.database.EgoDB;
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
import lineage.database.EgoWeaponDatabase.EgoWeaponInfo;
import lineage.share.Lineage;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;

/**
 * 에고무기 유저/운영 명령 헬퍼.
 *
 * 정책:
 * - 생성/삭제는 무료 명령어로 처리한다.
 * - 생성 시 능력과 대화 성향은 랜덤 선택 후 유지된다.
 * - 능력/대화 성향 변경은 명령어가 아니라 에고 변경구슬 아이템 사용으로만 처리한다.
 */
public final class EgoWeaponCommand {

    private EgoWeaponCommand() {
    }

    public static boolean toCommand(object o, String key, StringTokenizer st) {
        if (!(o instanceof PcInstance))
            return false;

        PcInstance pc = (PcInstance) o;

        if (key.equalsIgnoreCase(Lineage.command + "에고도움")) {
            help(pc);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고상대")) {
            EgoOpponentScanController.scanTargetOrNearest(pc);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고주변")) {
            EgoOpponentScanController.scanNearbyPlayers(pc);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고생성")) {
            create(pc, st);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고삭제")) {
            delete(pc, st);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고정보")) {
            info(pc);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고이름")) {
            rename(pc, st);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고말투")) {
            tone(pc, st);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고능력")) {
            ability(pc, st);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고리로드")) {
            reload(pc);
            return true;
        }

        return false;
    }

    private static void help(PcInstance pc) {
        info(pc, "========== 에고무기 명령어 ==========");
        msg(pc, Lineage.command + "에고생성 [이름] : 착용 중인 무기를 에고무기로 활성화. 능력/대화는 랜덤 결정");
        msg(pc, Lineage.command + "에고삭제 확인 : 착용 에고무기 정보 삭제. 무료");
        msg(pc, Lineage.command + "에고정보 : 착용 에고무기 정보 확인");
        msg(pc, Lineage.command + "에고이름 [새이름] : 에고 호출 이름 변경");
        msg(pc, Lineage.command + "에고상대 : 타겟 또는 가장 가까운 상대 캐릭터 분석");
        msg(pc, Lineage.command + "에고주변 : 주변 캐릭터 목록/위험도 감지");
        msg(pc, Lineage.command + "에고리로드 : 에고 DB/스킬베이스 캐시 리로드 및 온라인 인벤토리 표식 갱신");
        msg(pc, "능력/대화 성향 변경: 에고 변경구슬 아이템 사용");
        msg(pc, "일반 채팅: 에고 상태 / 에고 조언 / 에고 선공 / 에고 상대 / 에고 주변캐릭 / 에고 공격 / 에고 멈춰");
        info(pc, EgoWeaponTypeUtil.getSupportedWeaponTypesText());
    }

    private static void create(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            danger(pc, "무기를 착용한 뒤 사용하세요.");
            return;
        }

        if (EgoWeaponDatabase.isEgoWeapon(weapon)) {
            danger(pc, "이미 에고가 생성된 무기입니다.");
            return;
        }

        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, "생성 불가: " + EgoWeaponTypeUtil.getAbilityDenyReason("", weapon));
            info(pc, EgoWeaponTypeUtil.getSupportedWeaponTypesText());
            return;
        }

        String egoName = "에고";
        if (st != null && st.hasMoreTokens())
            egoName = st.nextToken().trim();

        if (egoName.length() < 1 || egoName.length() > 20) {
            danger(pc, "이름은 1~20자만 가능합니다.");
            return;
        }

        String randomTone = EgoChangeOrbController.randomTone();
        String randomAbility = EgoChangeOrbController.randomAbility(weapon);
        if (randomAbility.length() == 0)
            randomAbility = EgoWeaponTypeUtil.getDefaultAbilityType(weapon);

        boolean ok = EgoWeaponDatabase.enableEgo(pc, weapon, egoName, randomTone);
        if (ok) {
            EgoWeaponDatabase.setAbility(weapon, randomAbility);
            EgoDB.reload(null);
            EgoView.refreshInventory(pc, weapon);
            msg(pc, String.format("%s 에고가 깨어났습니다. 호출명: %s", EgoView.displayName(weapon), egoName));
            msg(pc, String.format("랜덤 능력: %s / 랜덤 대화: %s", randomAbility, randomTone));
            msg(pc, "이후 능력/대화는 유지됩니다. 변경은 에고 변경구슬 사용으로만 가능합니다.");
        } else {
            danger(pc, "에고 생성에 실패했습니다. DB 적용 여부를 확인하세요.");
        }
    }

    private static void delete(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            danger(pc, "무기를 착용한 뒤 사용하세요.");
            return;
        }
        if (!EgoWeaponDatabase.isEgoWeapon(weapon)) {
            danger(pc, "현재 착용 무기는 에고무기가 아닙니다.");
            return;
        }
        if (st == null || !st.hasMoreTokens() || !"확인".equals(st.nextToken().trim())) {
            danger(pc, Lineage.command + "에고삭제 확인 을 입력해야 삭제됩니다.");
            info(pc, "삭제는 무료입니다. 착용 중인 해당 에고무기 정보만 정리합니다.");
            return;
        }

        String weaponName = EgoView.displayName(weapon);
        if (EgoWeaponDatabase.disableEgo(weapon)) {
            EgoDB.reload(null);
            EgoView.refreshInventory(pc, weapon);
            msg(pc, String.format("%s 의 에고가 삭제되었습니다.", weaponName));
        } else {
            danger(pc, "에고 삭제에 실패했습니다. DB 상태를 확인하세요.");
        }
    }

    private static void info(PcInstance pc) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            danger(pc, "무기를 착용한 뒤 사용하세요.");
            return;
        }

        EgoWeaponInfo egoInfo = EgoWeaponDatabase.find(weapon);
        if (egoInfo == null || !egoInfo.enabled) {
            danger(pc, "현재 착용 무기는 에고무기가 아닙니다.");
            info(pc, String.format("착용 무기 원본: %s / originalType2=%s / 강화 %+d", EgoWeaponTypeUtil.getDisplayTypeName(weapon), EgoWeaponTypeUtil.getOriginalType2(weapon), weapon.getEnLevel()));
            msg(pc, Lineage.command + "에고생성 [이름] 으로 활성화할 수 있습니다.");
            return;
        }

        info(pc, "========== 에고무기 정보 ==========");
        msg(pc, String.format("무기: %s", EgoView.displayName(weapon)));
        msg(pc, String.format("표시: %s", EgoView.info(weapon)));
        msg(pc, String.format("이름: %s / 대화: %s", safe(egoInfo.egoName), EgoWeaponDatabase.normalizeTone(egoInfo.personality)));
        msg(pc, String.format("원본 type2: %s / 원본 타입: %s / 강화 %+d", EgoWeaponTypeUtil.getOriginalType2(weapon), EgoWeaponTypeUtil.getDisplayTypeName(weapon), weapon.getEnLevel()));
        msg(pc, String.format("레벨: %d / 경험치: %,d / 다음: %,d", egoInfo.level, egoInfo.exp, egoInfo.maxExp));
        msg(pc, String.format("대화단계: %d / 제어단계: %d", egoInfo.talkLevel, egoInfo.controlLevel));

        List<EgoAbilityInfo> abilityList = EgoWeaponDatabase.getAbilities(weapon);
        if (abilityList.isEmpty()) {
            msg(pc, "능력: 없음");
        } else {
            for (EgoAbilityInfo ai : abilityList)
                msg(pc, String.format("능력: %s Lv.%d / 추가확률 %+d / 추가피해 %+d", safe(ai.abilityType), ai.abilityLevel, ai.procChanceBonus, ai.damageBonus));
        }
        msg(pc, "능력/대화 변경은 에고 변경구슬 사용으로만 가능합니다.");
    }

    private static void rename(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            danger(pc, "무기를 착용한 뒤 사용하세요.");
            return;
        }
        if (!EgoWeaponDatabase.isEgoWeapon(weapon)) {
            danger(pc, "에고무기만 이름을 변경할 수 있습니다.");
            return;
        }
        if (st == null || !st.hasMoreTokens()) {
            msg(pc, Lineage.command + "에고이름 새이름");
            return;
        }
        String name = st.nextToken().trim();
        if (name.length() < 1 || name.length() > 20) {
            danger(pc, "이름은 1~20자만 가능합니다.");
            return;
        }
        if (EgoWeaponDatabase.setEgoName(weapon, name)) {
            EgoDB.reload(null);
            EgoView.refreshInventory(pc, weapon);
            msg(pc, String.format("에고 이름이 '%s' 로 변경되었습니다.", name));
        } else {
            danger(pc, "이름 변경에 실패했습니다.");
        }
    }

    private static void tone(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            danger(pc, "무기를 착용한 뒤 사용하세요.");
            return;
        }
        if (!EgoWeaponDatabase.isEgoWeapon(weapon)) {
            danger(pc, "현재 착용 무기는 에고무기가 아닙니다.");
            return;
        }
        info(pc, String.format("현재 대화: %s", EgoWeaponDatabase.getTone(weapon)));
        danger(pc, "대화 성향 변경은 명령어가 아니라 에고 변경구슬 아이템 사용으로만 가능합니다.");
    }

    private static void ability(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            danger(pc, "무기를 착용한 뒤 사용하세요.");
            return;
        }
        if (!EgoWeaponDatabase.isEgoWeapon(weapon)) {
            danger(pc, "먼저 " + Lineage.command + "에고생성 [이름] 으로 활성화하세요.");
            return;
        }
        List<EgoAbilityInfo> abilityList = EgoWeaponDatabase.getAbilities(weapon);
        if (abilityList.isEmpty())
            info(pc, "현재 능력: 없음");
        else
            info(pc, String.format("현재 능력: %s", safe(abilityList.get(0).abilityType)));
        danger(pc, "능력 변경은 명령어가 아니라 에고 변경구슬 아이템 사용으로만 가능합니다.");
    }

    private static void reload(PcInstance pc) {
        if (pc.getGm() == 0) {
            danger(pc, "운영자만 사용할 수 있습니다.");
            return;
        }
        EgoDB.reload(null);
        int refreshed = EgoView.refreshOnlineInventories(pc);
        msg(pc, String.format("에고 DB/스킬베이스 캐시를 리로드하고 온라인 에고 아이템 %,d개를 갱신했습니다.", refreshed));
    }

    private static ItemInstance getWeapon(PcInstance pc) {
        if (pc == null)
            return null;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return null;
        return inv.getSlot(Lineage.SLOT_WEAPON);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static void msg(PcInstance pc, String msg) {
        EgoMessageUtil.normal(pc, msg);
    }

    private static void danger(PcInstance pc, String msg) {
        EgoMessageUtil.danger(pc, msg);
    }

    private static void info(PcInstance pc, String msg) {
        EgoMessageUtil.info(pc, msg);
    }
}
