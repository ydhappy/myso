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
 * 외부 연결은 아래 5개만 사용한다.
 * - init/reload: 서버 시작/관리자 리로드
 * - chat: 일반 채팅 처리
 * - tick: 캐릭터 주기 처리
 * - attack/defense: 전투 보정
 *
 * 에고 생성은 점명령이 아니라 EgoOrb 아이템 사용으로만 처리한다.
 */
public final class EgoCore {

    private EgoCore() {
    }

    /** 서버 시작 시 1회 호출. */
    public static void init(Connection con) {
        EgoSchema.silentCheck(con);
        EgoDB.init(con);
    }

    /** 서버 관리 리로드 시 호출. 점명령 연결은 제공하지 않는다. */
    public static void reload(Connection con) {
        EgoSchema.silentCheck(con);
        EgoDB.reload(con);
    }

    /** DB 테이블/컬럼 연결성 리포트. 운영자 진단/문서 확인용. */
    public static String schemaReport(Connection con) {
        return EgoSchema.report(con);
    }

    /** DB 테이블/컬럼 연결성이 현재 Java 기준을 만족하는지 확인. */
    public static boolean schemaOk(Connection con) {
        return EgoSchema.isValid(con);
    }

    /** ChattingController 일반채팅 처리. true면 주변 방송 중단. */
    public static boolean chat(object o, String msg) {
        if (!(o instanceof PcInstance) || o instanceof RobotInstance)
            return false;
        PcInstance pc = (PcInstance) o;
        EgoOwnerRecognition.recognize(pc);
        return EgoTalk.chat(pc, msg);
    }

    /** 캐릭터 상태 루프/AI 루프에서 자동 경고 및 자동 대사 처리. */
    public static void tick(object o) {
        if (!(o instanceof PcInstance) || o instanceof RobotInstance)
            return;
        PcInstance pc = (PcInstance) o;
        EgoOwnerRecognition.recognize(pc);
        EgoTalk.warning(pc);
    }

    /** DamageController.getDamage 최종 return 직전 공격 보정. */
    public static int attack(Character cha, object target, ItemInstance weapon, int damage) {
        if (cha instanceof PcInstance)
            EgoOwnerRecognition.recognize((PcInstance) cha, weapon);
        return EgoSkill.attack(cha, target, weapon, damage);
    }

    /** DamageController.toDamage HP 감소 직전 피격 보정. */
    public static int defense(Character defender, Character attacker, int damage) {
        if (defender instanceof PcInstance)
            EgoOwnerRecognition.recognize((PcInstance) defender);
        return EgoSkill.defense(defender, attacker, damage);
    }
}
