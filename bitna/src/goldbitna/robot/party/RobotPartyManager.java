package goldbitna.robot.party;

import lineage.bean.lineage.Party;
import lineage.util.Util;
import lineage.world.World;
import lineage.world.controller.PartyController;
import lineage.world.object.Character;
import lineage.world.object.instance.PcInstance;
import lineage.world.object.instance.PcRobotInstance;

/**
 * 설계 문서 3번(파티 및 클랜 규칙)을 관장하는 매니저 클래스입니다.
 */
public class RobotPartyManager {

    // 파티 행동 모드
    public enum PartyMode {
        GATHER,  // 집결 모드 (리더 위치로 강제 이동/텔레포트)
        WAIT,    // 대기 모드 (전투 중이 아니면 안전 지점 대기)
        SUPPORT, // 지원 모드 (가까운 파티원 먼저 지원, 떨어진 파티원 합류 후 공격)
        NORMAL   // 일반 전투 모드
    }

    /**
     * 리더와의 거리 및 현재 상태를 계산하여 파티 모드를 결정합니다.
     */
    public PartyMode determinePartyMode(PcRobotInstance robot, PcInstance leader) {
        if (leader == null || leader.isDead()) {
            return PartyMode.NORMAL;
        }

        // 1. 맵이 다르거나 거리가 15칸 이상이면 무조건 집결(GATHER)
        if (robot.getMap() != leader.getMap() || Util.getDistance(robot, leader) >= 15) {
            return PartyMode.GATHER;
        }

        // 2. 전투 중이 아닐 때, 리더가 안전지대에 있다면 대기(WAIT)
        boolean isCombat = robot.getTarget() != null || leader.getTarget() != null;
        if (!isCombat && World.isSafetyZone(leader.getX(), leader.getY(), leader.getMap())) {
            return PartyMode.WAIT;
        }

        // 3. 거리가 3~14칸 사이라면 합류하면서 지원(SUPPORT)
        if (Util.getDistance(robot, leader) > 2) {
            return PartyMode.SUPPORT;
        }

        return PartyMode.NORMAL;
    }

    /**
     * 설계 문서 규칙: 파티원 중 누군가 HP 20% 이하 시 위험 상황으로 간주 (전체 귀환 트리거)
     */
    public boolean isPartyInDanger(PcRobotInstance robot) {
        Party party = PartyController.find(robot);
        if (party == null) return false;

        for (Character member : party.getList()) {
            if (member == null || member.isDead()) continue;
            
            double hpPercent = ((double) member.getNowHp() / member.getMaxHp()) * 100;
            if (hpPercent <= 20.0) {
                return true;
            }
        }
        return false;
    }
}