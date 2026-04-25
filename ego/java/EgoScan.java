package lineage.world.controller;

import lineage.world.object.instance.PcInstance;

/**
 * 짧은 이름용 에고 상대감지 클래스.
 */
public final class EgoScan {

    private EgoScan() {
    }

    public static boolean chat(PcInstance pc, String command) {
        return EgoOpponentScanController.handleTalk(pc, command);
    }

    public static void target(PcInstance pc) {
        EgoOpponentScanController.scanTargetOrNearest(pc);
    }

    public static void around(PcInstance pc) {
        EgoOpponentScanController.scanNearbyPlayers(pc);
    }
}
