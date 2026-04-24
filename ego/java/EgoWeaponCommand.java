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
 * 적용 위치:
 * - 이 파일을 bitna/src/lineage/world/controller/EgoWeaponCommand.java 로 복사한다.
 * - CommandController.toCommand(...)의 PluginController 호출 이후 또는 유저 명령어 분기 초반에 연결한다.
 *
 * 지원 명령:
 * - .에고도움
 * - .에고생성 [이름]
 * - .에고정보
 * - .에고이름 [새이름]
 * - .에고능력 [능력코드]
 * - .에고리로드    GM 전용 권장
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
        msg(pc, "\\fY[에고무기] 명령어 안내");
        msg(pc, Lineage.command + "에고생성 [이름] : 착용 무기를 에고무기로 활성화");
        msg(pc, Lineage.command + "에고정보 : 착용 에고무기 정보 확인");
        msg(pc, Lineage.command + "에고이름 [새이름] : 에고 호출 이름 변경");
        msg(pc, Lineage.command + "에고능력 [능력코드] : 에고 특별 능력 설정");
        msg(pc, "능력코드: EGO_BALANCE, BLOOD_DRAIN, MANA_DRAIN, CRITICAL_BURST, GUARDIAN_SHIELD, AREA_SLASH, EXECUTION, FLAME_BRAND, FROST_BIND");
        msg(pc, "일반 채팅 사용: 에고 상태 / 에고 조언 / 에고 선공 / 에고 공격 / 에고 멈춰");
    }

    private static void create(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            msg(pc, "\\fR[에고무기] 무기를 착용한 뒤 사용하세요.");
            return;
        }

        String egoName = "에고";
        if (st != null && st.hasMoreTokens())
            egoName = st.nextToken().trim();

        if (egoName.length() < 1 || egoName.length() > 20) {
            msg(pc, "\\fR[에고무기] 이름은 1~20자만 가능합니다.");
            return;
        }

        boolean ok = EgoWeaponDatabase.enableEgo(pc, weapon, egoName, "guardian");
        if (ok) {
            msg(pc, String.format("\\fY[에고무기] +%d %s 에고가 깨어났습니다. 호출명: %s", weapon.getEnLevel(), weapon.getName(), egoName));
            msg(pc, String.format("일반 채팅으로 '%s 상태' 를 입력해보세요.", egoName));
        } else {
            msg(pc, "\\fR[에고무기] 에고 생성에 실패했습니다. DB 적용 여부를 확인하세요.");
        }
    }

    private static void info(PcInstance pc) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            msg(pc, "\\fR[에고무기] 무기를 착용한 뒤 사용하세요.");
            return;
        }

        EgoWeaponInfo info = EgoWeaponDatabase.find(weapon);
        if (info == null || !info.enabled) {
            msg(pc, "\\fR[에고무기] 현재 착용 무기는 에고무기가 아닙니다.");
            msg(pc, Lineage.command + "에고생성 [이름] 으로 활성화할 수 있습니다.");
            return;
        }

        msg(pc, "\\fY========== 에고무기 정보 ==========");
        msg(pc, String.format("무기: +%d %s", weapon.getEnLevel(), weapon.getName()));
        msg(pc, String.format("이름: %s / 성격: %s", safe(info.egoName), safe(info.personality)));
        msg(pc, String.format("레벨: %d / 경험치: %,d / 다음: %,d", info.level, info.exp, info.maxExp));
        msg(pc, String.format("대화단계: %d / 제어단계: %d", info.talkLevel, info.controlLevel));

        List<EgoAbilityInfo> abilityList = EgoWeaponDatabase.getAbilities(weapon);
        if (abilityList.isEmpty()) {
            msg(pc, "능력: 없음");
        } else {
            for (EgoAbilityInfo ai : abilityList) {
                msg(pc, String.format("능력: %s Lv.%d / 추가확률 %+d / 추가피해 %+d", safe(ai.abilityType), ai.abilityLevel, ai.procChanceBonus, ai.damageBonus));
            }
        }
    }

    private static void rename(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            msg(pc, "\\fR[에고무기] 무기를 착용한 뒤 사용하세요.");
            return;
        }

        if (!EgoWeaponDatabase.isEgoWeapon(weapon)) {
            msg(pc, "\\fR[에고무기] 에고무기만 이름을 변경할 수 있습니다.");
            return;
        }

        if (st == null || !st.hasMoreTokens()) {
            msg(pc, Lineage.command + "에고이름 새이름");
            return;
        }

        String name = st.nextToken().trim();
        if (name.length() < 1 || name.length() > 20) {
            msg(pc, "\\fR[에고무기] 이름은 1~20자만 가능합니다.");
            return;
        }

        if (EgoWeaponDatabase.setEgoName(weapon, name)) {
            msg(pc, String.format("\\fY[에고무기] 에고 이름이 '%s' 로 변경되었습니다.", name));
        } else {
            msg(pc, "\\fR[에고무기] 이름 변경에 실패했습니다.");
        }
    }

    private static void ability(PcInstance pc, StringTokenizer st) {
        ItemInstance weapon = getWeapon(pc);
        if (weapon == null) {
            msg(pc, "\\fR[에고무기] 무기를 착용한 뒤 사용하세요.");
            return;
        }

        if (!EgoWeaponDatabase.isEgoWeapon(weapon)) {
            msg(pc, "\\fR[에고무기] 먼저 " + Lineage.command + "에고생성 [이름] 으로 활성화하세요.");
            return;
        }

        if (st == null || !st.hasMoreTokens()) {
            msg(pc, Lineage.command + "에고능력 능력코드");
            msg(pc, "예: " + Lineage.command + "에고능력 BLOOD_DRAIN");
            return;
        }

        String type = st.nextToken().trim().toUpperCase();
        if (!isValidAbility(type)) {
            msg(pc, "\\fR[에고무기] 알 수 없는 능력코드입니다.");
            msg(pc, "가능: EGO_BALANCE, BLOOD_DRAIN, MANA_DRAIN, CRITICAL_BURST, GUARDIAN_SHIELD, AREA_SLASH, EXECUTION, FLAME_BRAND, FROST_BIND");
            return;
        }

        if (EgoWeaponDatabase.setAbility(weapon, type)) {
            msg(pc, String.format("\\fY[에고무기] 특별 능력이 %s 로 설정되었습니다.", type));
        } else {
            msg(pc, "\\fR[에고무기] 능력 설정에 실패했습니다.");
        }
    }

    private static void reload(PcInstance pc) {
        if (pc.getGm() == 0) {
            msg(pc, "\\fR[에고무기] 운영자만 사용할 수 있습니다.");
            return;
        }
        EgoWeaponDatabase.reload(null);
        msg(pc, "\\fY[에고무기] DB 캐시를 리로드했습니다.");
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
        return type.equals("EGO_BALANCE") ||
               type.equals("BLOOD_DRAIN") ||
               type.equals("MANA_DRAIN") ||
               type.equals("CRITICAL_BURST") ||
               type.equals("GUARDIAN_SHIELD") ||
               type.equals("AREA_SLASH") ||
               type.equals("EXECUTION") ||
               type.equals("FLAME_BRAND") ||
               type.equals("FROST_BIND");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static void msg(PcInstance pc, String msg) {
        ChattingController.toChatting(pc, msg, Lineage.CHATTING_MODE_MESSAGE);
    }
}
