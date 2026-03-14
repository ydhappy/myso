package goldbitna.robot.behavior;

import lineage.database.SkillDatabase;
import lineage.database.SpriteFrameDatabase;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.controller.LocationController;
import lineage.world.object.Character;
import lineage.world.object.object;
import lineage.world.object.instance.PcRobotInstance;

/**
 * 리니지 로봇 클래스별(군주, 기사, 요정, 법사) 고유 행동 패턴 인터페이스
 * Java 1.8 환경 최적화
 */
public interface IRobotClassBehavior {
    
    /**
     * 1. 몬스터 타겟 지정 및 탐색
     * @param robot 행동을 수행할 로봇 객체
     * @return 탐색된 타겟 객체 (없을 경우 null)
     */
    object findTarget(PcRobotInstance robot);
    
    /**
     * 2. 전투 수행 (클래스별 거리 유지 및 고유 스킬/물리 공격 혼합)
     * @param robot 행동을 수행할 로봇 객체
     * @param target 공격할 대상
     */
    void executeAttack(PcRobotInstance robot, object target);
    
    /**
     * 3. 버프 및 파티원 지원 (군주/법사/요정 특화, 기사는 자가 버프 위주)
     * @param robot 행동을 수행할 로봇 객체
     */
    default void executeBuffAndSupport(PcRobotInstance robot) {
        if (System.currentTimeMillis() < robot.delay_magic) return;

        int classType = robot.getClassType();
        long myClanId = robot.getClanId();

        switch (classType) {
            case Lineage.LINEAGE_CLASS_ROYAL: // 군주
                if (myClanId > 0 && Util.random(0, 100) < 5) { 
                    lineage.bean.database.Skill aura = SkillDatabase.find(113); 
                    if (aura != null && aura.getMpConsume() <= robot.getNowMp()) {
                        robot.toSkill(aura, robot);
                        // 에러 해결: 기존에 있는 setAiTime 메서드 사용
                        robot.setAiTime(SpriteFrameDatabase.getGfxFrameTime(robot, robot.getGfx(), robot.getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION));
                    }
                }
                break;

            case Lineage.LINEAGE_CLASS_KNIGHT: // 기사
                break;

            case Lineage.LINEAGE_CLASS_ELF: // 요정
                for (object o : robot.getInsideList()) {
                    if (o instanceof Character) {
                        Character member = (Character) o;
                        if (!member.isDead() && member.getClanId() == myClanId && member.getHpPercent() < 70.0) {
                            lineage.bean.database.Skill blessing = SkillDatabase.find(21, 3); 
                            if (blessing != null && blessing.getMpConsume() <= robot.getNowMp()) {
                                robot.setHeading(Util.calcheading(robot, member.getX(), member.getY()));
                                robot.toSkill(blessing, member);
                                // 에러 해결: 기존에 있는 setAiTime 메서드 사용
                                robot.setAiTime(SpriteFrameDatabase.getGfxFrameTime(robot, robot.getGfx(), robot.getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION));
                                return; 
                            }
                        }
                    }
                }
                break;

            case Lineage.LINEAGE_CLASS_WIZARD: // 법사
                if (robot.getNowMp() < 20) return; 
                for (object o : robot.getInsideList()) {
                    if (o instanceof Character) {
                        Character targetCha = (Character) o;
                        if (!targetCha.isDead() && targetCha.getClanId() == myClanId && targetCha.getHpPercent() < 60.0) {
                            lineage.bean.database.Skill heal = SkillDatabase.find(5, 2); 
                            if (heal != null && heal.getMpConsume() <= robot.getNowMp()) {
                                robot.setHeading(Util.calcheading(robot, targetCha.getX(), targetCha.getY()));
                                robot.toSkill(heal, targetCha);
                                // 에러 해결: 기존에 있는 setAiTime 메서드 사용
                                robot.setAiTime(SpriteFrameDatabase.getGfxFrameTime(robot, robot.getGfx(), robot.getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION));
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }
    
    /**
     * 4. 위기 상황 회피 (HP 20% 이하 귀환 또는 텔레포트 불가 맵 도보 회피)
     * @param robot 행동을 수행할 로봇 객체
     */
    default void executeFlee(PcRobotInstance robot) {
        double hpPercent = ((double) robot.getNowHp() / robot.getMaxHp()) * 100;
        
        if (hpPercent <= 20.0) {
            if (LocationController.isTeleportZone(robot, true, false)) {
                int[] home = Lineage.getHomeXY();
                robot.toTeleport(home[0], home[1], home[2], true);
            } else {
                object target = robot.getTarget();
                if (target != null) {
                    robot.setHeading(Util.oppositionHeading(robot, target));
                } else {
                    robot.setHeading(Util.random(0, 7));
                }
            }
        }
    }
}