package lineage.world.controller;

import java.util.List;
import java.util.StringTokenizer;

import lineage.bean.lineage.Inventory;
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
 * .에고검사 진단은 제거한다.
 * 핵심 조작은 일반채팅 자연어로 처리한다.
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
        if (key.equalsIgnoreCase(Lineage.command + "에고정보")) {
            info(pc);
            return true;
        }
        if (key.equalsIgnoreCase(Lineage.command + "에고이름")) {
            rename(pc, st);
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
        msg(pc, Lineage.command + "에고생성 [이름] : 착용 무기를 에고무기로 활성화");
        msg(pc, Lineage.command + "에고정보 : 착용 에고무기 정보 확인");
        msg(pc, Lineage.command + "에고이름 [새이름] : 에고 호출 이름 변경");
        msg(pc, Lineage.command + "에고능력 [능력코드] : 에고 특별 능력 설정");
        msg(pc, Lineage.command + "에고상대 : 타겟 또는 가장 가까운 상대 캐릭터 분석");
        msg(pc, Lineage.command + "에고주변 : 주변 캐릭터 목록/위험도 감지");
        msg(pc, Lineage.command + "에고리로드 : DB/이미지 캐시 리로드 및 온라인 인벤토리 즉시반영");
        msg(pc, "일반 채팅: 에고 상태 / 에고 조언 / 에고 선공 / 에고 상대 / 에고 주변캐릭 / 에고 활 / 에고 양검 / 에고 한검 / 에고 공격 / 에고 멈춰");
        msg(pc, "형태변환: 활, 양검, 양손검, 한검, 한손검, 단검, 창, 도끼, 지팡이, 완드");
        msg(pc, "능력코드: EGO_BALANCE, BLOOD_DRAIN, MANA_DRAIN, CRITICAL_BURST, GUARDIAN_SHIELD, AREA_SLASH, EXECUTION, FLAME_BRAND, FROST_BIND");
        info(pc, EgoWeaponTypeUtil.getSupportedWeaponTypesText());
    }

    private static void create(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            danger(pc, "무기를 착용한 뒤 사용하세요.");
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

        boolean ok = EgoWeaponDatabase.enableEgo(pc, weapon, egoName, "guardian");
        if (ok) {
            String defaultAbility = EgoWeaponTypeUtil.getDefaultAbilityType(weapon);
            EgoWeaponDatabase.setAbility(weapon, defaultAbility);
            EgoView.refreshInventory(pc, weapon);
            msg(pc, String.format("+%d %s [%s] 에고가 깨어났습니다. 호출명: %s", weapon.getEnLevel(), weapon.getName(), EgoWeaponTypeUtil.getDisplayTypeName(weapon), egoName));
            msg(pc, String.format("기본 능력: %s", defaultAbility));
            msg(pc, String.format("일반 채팅 예: '%s 상태', '%s 활', '%s 양검', '%s 한검'", egoName, egoName, egoName, egoName));
        } else {
            danger(pc, "에고 생성에 실패했습니다. DB 적용 여부를 확인하세요.");
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
            info(pc, String.format("착용 무기 원본: %s / originalType2=%s", EgoWeaponTypeUtil.getDisplayTypeName(weapon), EgoWeaponTypeUtil.getOriginalType2(weapon)));
            msg(pc, Lineage.command + "에고생성 [이름] 으로 활성화할 수 있습니다.");
            return;
        }

        info(pc, "========== 에고무기 정보 ==========");
        msg(pc, String.format("무기: +%d %s", weapon.getEnLevel(), weapon.getName()));
        msg(pc, String.format("표시: %s", EgoView.info(weapon)));
        msg(pc, String.format("이름: %s / 성격: %s", safe(egoInfo.egoName), safe(egoInfo.personality)));
        msg(pc, String.format("원본 type2: %s / 현재 에고형태: %s", EgoWeaponTypeUtil.getOriginalType2(weapon), EgoWeaponTypeUtil.getDisplayTypeName(weapon)));
        msg(pc, String.format("레벨: %d / 경험치: %,d / 다음: %,d", egoInfo.level, egoInfo.exp, egoInfo.maxExp));
        msg(pc, String.format("대화단계: %d / 제어단계: %d", egoInfo.talkLevel, egoInfo.controlLevel));

        List<EgoAbilityInfo> abilityList = EgoWeaponDatabase.getAbilities(weapon);
        if (abilityList.isEmpty()) {
            msg(pc, "능력: 없음");
        } else {
            for (EgoAbilityInfo ai : abilityList)
                msg(pc, String.format("능력: %s Lv.%d / 추가확률 %+d / 추가피해 %+d", safe(ai.abilityType), ai.abilityLevel, ai.procChanceBonus, ai.damageBonus));
        }
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
            EgoView.refreshInventory(pc, weapon);
            msg(pc, String.format("에고 이름이 '%s' 로 변경되었습니다.", name));
        } else {
            danger(pc, "이름 변경에 실패했습니다.");
        }
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
        if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
            danger(pc, "능력 설정 불가: " + EgoWeaponTypeUtil.getAbilityDenyReason("", weapon));
            return;
        }
        if (st == null || !st.hasMoreTokens()) {
            msg(pc, Lineage.command + "에고능력 능력코드");
            info(pc, String.format("현재 형태 추천 능력: %s", EgoWeaponTypeUtil.getDefaultAbilityType(weapon)));
            return;
        }

        String type = st.nextToken().trim().toUpperCase();
        if (!isValidAbility(type)) {
            danger(pc, "알 수 없는 능력코드입니다.");
            msg(pc, "가능: EGO_BALANCE, BLOOD_DRAIN, MANA_DRAIN, CRITICAL_BURST, GUARDIAN_SHIELD, AREA_SLASH, EXECUTION, FLAME_BRAND, FROST_BIND");
            return;
        }
        if (!EgoWeaponTypeUtil.isAbilityAllowed(type, weapon)) {
            danger(pc, EgoWeaponTypeUtil.getAbilityDenyReason(type, weapon));
            info(pc, String.format("현재 형태 추천 능력: %s", EgoWeaponTypeUtil.getDefaultAbilityType(weapon)));
            return;
        }
        if (EgoWeaponDatabase.setAbility(weapon, type)) {
            EgoView.refreshInventory(pc, weapon);
            msg(pc, String.format("특별 능력이 %s 로 설정되었습니다.", type));
        } else {
            danger(pc, "능력 설정에 실패했습니다.");
        }
    }

    private static void reload(PcInstance pc) {
        if (pc.getGm() == 0) {
            danger(pc, "운영자만 사용할 수 있습니다.");
            return;
        }
        EgoWeaponDatabase.reload(null);
        int refreshed = EgoView.refreshOnlineInventories(pc);
        msg(pc, String.format("에고 DB/이미지 캐시를 리로드하고 온라인 에고 아이템 %,d개를 즉시 갱신했습니다.", refreshed));
    }

    private static ItemInstance getWeapon(PcInstance pc) {
        if (pc == null)
            return null;
        Inventory inv = pc.getInventory();
        if (inv == null)
            return null;
        return inv.getSlot(Lineage.SLOT_WEAPON);
    }

    private static boolean isValidAbility(String type) {
        if (type == null)
            return false;
        return type.equals("EGO_BALANCE") || type.equals("BLOOD_DRAIN") || type.equals("MANA_DRAIN") || type.equals("CRITICAL_BURST") || type.equals("GUARDIAN_SHIELD") || type.equals("AREA_SLASH") || type.equals("EXECUTION") || type.equals("FLAME_BRAND") || type.equals("FROST_BIND");
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
