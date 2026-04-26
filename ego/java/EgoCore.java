package lineage.world.controller;

import java.sql.Connection;

import lineage.database.EgoDB;
import lineage.world.object.Character;
import lineage.world.object.object;
import lineage.world.object.instance.ItemInstance;
import lineage.world.object.instance.PcInstance;
import lineage.world.object.instance.RobotInstance;

/**
 * 에고 시스템 단일 진입점.
 *
 * 초보자 적용 시 가능하면 이 클래스만 기존 서버 코드에 연결하세요.
 * 내부 구현 파일들은 기능별로 나뉘어 있지만, 외부 연결은 EgoCore로 최소화합니다.
 */
public final class EgoCore {

    private EgoCore() {
    }

    /** 서버 시작 시 1회 호출. */
    public static void init(Connection con) {
        EgoDB.init(con);
    }

    /** .에고리로드 등 운영자 리로드 시 호출. */
    public static void reload(Connection con) {
        EgoDB.reload(con);
    }

    /** CommandController에서 명령어 처리. true면 기존 명령 처리 중단. */
    public static boolean command(object o, String key, String args) {
        return EgoCmd.run(o, key, args);
    }

    /** ChattingController 일반채팅 처리. true면 주변 방송 중단. */
    public static boolean chat(object o, String msg) {
        if (!(o instanceof PcInstance) || o instanceof RobotInstance)
            return false;
        return EgoTalk.chat((PcInstance) o, msg);
    }

    /** 캐릭터 상태 루프/AI 루프에서 자동 경고 및 자동 대사 처리. */
    public static void tick(object o) {
        if (!(o instanceof PcInstance) || o instanceof RobotInstance)
            return;
        EgoTalk.warning((PcInstance) o);
    }

    /** DamageController.getDamage 최종 return 직전 공격 보정. */
    public static int attack(Character cha, object target, ItemInstance weapon, int damage) {
        return EgoSkill.attack(cha, target, weapon, damage);
    }

    /** DamageController.toDamage HP 감소 직전 피격 보정. */
    public static int defense(Character defender, Character attacker, int damage) {
        return EgoSkill.defense(defender, attacker, damage);
    }
}
