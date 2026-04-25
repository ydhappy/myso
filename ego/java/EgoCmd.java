package lineage.world.controller;

import java.util.StringTokenizer;

import lineage.world.object.object;

/**
 * 짧은 이름용 에고 점 명령어 클래스.
 *
 * CommandController 연결 시 EgoWeaponCommand 대신 EgoCmd 사용을 권장합니다.
 */
public final class EgoCmd {

    private EgoCmd() {
    }

    public static boolean run(object o, String key, StringTokenizer st) {
        return EgoWeaponCommand.toCommand(o, key, st);
    }
}
