package lineage.world.controller;

import java.util.StringTokenizer;

import lineage.world.object.object;

/**
 * 에고 점 명령어 호환용 클래스.
 *
 * 에고 생성 흐름은 에고 구슬 아이템 사용으로 일원화한다.
 * 기존 CommandController 연결부가 남아 있어도 이 클래스는 어떤 점명령도 처리하지 않는다.
 */
public final class EgoWeaponCommand {

    private EgoWeaponCommand() {
    }

    public static boolean toCommand(object o, String key, StringTokenizer st) {
        return false;
    }
}
