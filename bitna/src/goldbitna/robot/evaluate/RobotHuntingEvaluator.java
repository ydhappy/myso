package goldbitna.robot.evaluate;

import lineage.share.Lineage;
import lineage.world.World;
import lineage.world.object.instance.PcRobotInstance;
import lineage.world.controller.LocationController;
import lineage.util.Util;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RobotHuntingEvaluator {
    
    // ✅ int에서 long으로 변경하여 lossy conversion 에러 해결
    private final Map<Long, Double> previousExpMap = new ConcurrentHashMap<>();

    public boolean isMapOvercrowded(PcRobotInstance robot) {
        int currentMapId = robot.getMap();
        long userCountInMap = 0;

        userCountInMap += World.getPcList().stream().filter(pc -> pc.getMap() == currentMapId).count();
        userCountInMap += World.getRobotList().stream().filter(r -> r.getMap() == currentMapId).count();

        return userCountInMap >= 20;
    }

    public boolean needsToChangeHuntingGround(PcRobotInstance robot, double expectedExpLimit) {
        long objId = robot.getObjectId(); // ✅ int -> long 형변환 처리
        double currentExp = robot.getExp();

        if (!previousExpMap.containsKey(objId)) {
            previousExpMap.put(objId, currentExp);
            return false; 
        }

        double prevExp = previousExpMap.get(objId);
        double gainedExp = currentExp - prevExp;

        previousExpMap.put(objId, currentExp);
        return gainedExp <= (expectedExpLimit * 0.5);
    }

    public void exploreNewArea(PcRobotInstance robot) {
        // ✅ 텔레포트 존 검증 및 텔레포트 로직 완벽 교체
        if (LocationController.isTeleportZone(robot, true, false)) {
            Util.toRndLocation(robot);
            int[] home = Lineage.getHomeXY();
            robot.toTeleport(home[0], home[1], home[2], true);
        } else {
            int randomHeading = Util.random(0, 7);
            if (World.isThroughObject(robot.getX(), robot.getY(), robot.getMap(), randomHeading)) {
                robot.setHeading(randomHeading);
            } else {
                robot.setHeading(Util.oppositionHeading(robot, robot));
            }
        }
    }
    
    public void clearRobotData(long objId) {
        previousExpMap.remove(objId);
    }
}