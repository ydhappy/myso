package lineage.world.controller;

import java.sql.Connection;
import java.util.StringTokenizer;

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
        EgoSchema.silentCheck(con);
        EgoDB.init(con);
    }

    /** 리로드 시 호출. */
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

    /** CommandController 호환용. 에고 생명주기 점명령은 더 이상 처리하지 않는다. */
    public static boolean command(object o, String key, StringTokenizer st) {
        return EgoCmd.run(o, key, st);
    }

    /** CommandController가 남은 인자를 문자열로 넘기는 서버용 편의 오버로드. */
    public static boolean command(object o, String key, String args) {
        return EgoCmd.run(o, key, new StringTokenizer(args == null ? "" : args));
    }

    /** CommandController가 인자 없이 key만 넘기는 서버용 편의 오버로드. */
    public static boolean command(object o, String key) {
        return EgoCmd.run(o, key, new StringTokenizer(""));
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
