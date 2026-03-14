package goldbitna.robot.behavior;

import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.World;
import lineage.world.object.object;
import lineage.world.object.instance.MonsterInstance;
import lineage.world.object.instance.PcRobotInstance;

public class BehaviorWizard implements IRobotClassBehavior {

    private static final int MAGIC_ATTACK_RANGE = 8;

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

        if (dist > MAGIC_ATTACK_RANGE) {
            robot.setHeading(Util.calcheading(robot, target.getX(), target.getY()));
            return;
        }

        double mpPercent = ((double) robot.getNowMp() / robot.getMaxMp()) * 100;
        
        if (mpPercent > 30.0) {
            robot.toAttack(target, robot.getX(), robot.getY(), false, robot.getGfxMode() + Lineage.GFX_MODE_ATTACK, 0, false);
        } else {
            if (dist > 1) {
                robot.setHeading(Util.calcheading(robot, target.getX(), target.getY()));
            } else {
                robot.toAttack(target, robot.getX(), robot.getY(), false, robot.getGfxMode() + Lineage.GFX_MODE_ATTACK, 0, false);
            }
        }
    }
}