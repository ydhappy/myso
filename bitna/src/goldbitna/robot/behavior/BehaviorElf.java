package goldbitna.robot.behavior;

import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.World;
import lineage.world.object.object;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcRobotInstance;

public class BehaviorElf implements IRobotClassBehavior {

    private static final int BOW_ATTACK_RANGE = 7;

    @Override
    public object findTarget(PcRobotInstance robot) {
        object bestTarget = null;
        int minDistance = Integer.MAX_VALUE;

        for (object obj : robot.getInsideList()) {
            if (obj instanceof MonsterInstance && !obj.isDead()) {
                if (Util.isAreaAttack(robot, obj) && World.isAttack(robot, obj)) {
                    int dist = Util.getDistance(robot, obj);
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestTarget = obj;
                    }
                }
            }
        }
        return bestTarget;
    }

    @Override
    public void executeAttack(PcRobotInstance robot, object target) {
        if (target == null || target.isDead()) return;

        int dist = Util.getDistance(robot, target);

        if (dist > BOW_ATTACK_RANGE) {
            robot.setHeading(Util.calcheading(robot, target.getX(), target.getY()));
            return;
        }

        robot.toAttack(target, robot.getX(), robot.getY(), true, robot.getGfxMode() + Lineage.GFX_MODE_ATTACK, 0, false);
    }
}