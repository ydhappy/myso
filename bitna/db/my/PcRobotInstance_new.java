package lineage.world.object.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import goldbitna.AttackController;
import goldbitna.robot.Pk1RobotInstance;
import goldbitna.robot.behavior.BehaviorElf;
import goldbitna.robot.behavior.BehaviorKnight;
import goldbitna.robot.behavior.BehaviorPrince;
import goldbitna.robot.behavior.BehaviorWizard;
import goldbitna.robot.behavior.IRobotClassBehavior;
import goldbitna.robot.controller.RobotConversationController;
import goldbitna.robot.evaluate.RobotHuntingEvaluator;
import goldbitna.robot.party.RobotPartyManager;
import lineage.bean.database.Item;
import lineage.bean.database.ItemTeleport;
import lineage.bean.database.Poly;
import lineage.bean.database.RobotPoly;
import lineage.bean.database.Skill;
import lineage.bean.database.SkillRobot;
import lineage.bean.lineage.Book;
import lineage.bean.lineage.Buff;
import lineage.bean.lineage.Clan;
import lineage.bean.lineage.Inventory;
import lineage.bean.lineage.Kingdom;
import lineage.bean.lineage.Party;
import lineage.bean.lineage.Summon;
import lineage.database.BackgroundDatabase;
import lineage.database.DatabaseConnection;
import lineage.database.ItemDatabase;
import lineage.database.ItemTeleportDatabase;
import lineage.database.PolyDatabase;
import lineage.database.ServerDatabase;
import lineage.database.SkillDatabase;
import lineage.database.SpriteFrameDatabase;
import lineage.database.SummonListDatabase;
import lineage.network.packet.BasePacket;
import lineage.network.packet.BasePacketPooling;
import lineage.network.packet.ClientBasePacket;
import lineage.network.packet.ServerBasePacket;
import lineage.network.packet.server.S_ObjectAction;
import lineage.network.packet.server.S_ObjectEffect;
import lineage.network.packet.server.S_ObjectLock;
import lineage.network.packet.server.S_ObjectPoly;
import lineage.network.packet.server.S_ObjectRevival;
import lineage.share.Lineage;
import lineage.share.System;
import lineage.thread.AiThread;
import lineage.util.Util;
import lineage.world.AStar;
import lineage.world.Node;
import lineage.world.World;
import lineage.world.controller.BookController;
import lineage.world.controller.BuffController;
import lineage.world.controller.CharacterController;
import lineage.world.controller.ChattingController;
import lineage.world.controller.ClanController;
import lineage.world.controller.KingdomController;
import lineage.world.controller.LocationController;
import lineage.world.controller.MagicDollController;
import lineage.world.controller.PartyController;
import lineage.world.controller.RobotClanController;
import lineage.world.controller.RobotController;
import lineage.world.controller.RobotController.RobotMoving;
import lineage.world.controller.SkillController;
import lineage.world.controller.SummonController;
import lineage.world.controller.SummonController.TYPE;
import lineage.world.object.Character;
import lineage.world.object.object;
import lineage.world.object.item.all_night.Buff_potion;
import lineage.world.object.item.potion.BraveryPotion;
import lineage.world.object.item.potion.HastePotion;
import lineage.world.object.item.potion.HealingPotion;
import lineage.world.object.item.scroll.ScrollPolymorph;
import lineage.world.object.item.weapon.Arrow;
import lineage.world.object.magic.BlessWeapon;
import lineage.world.object.magic.Bravery;
import lineage.world.object.magic.Criminal;
import lineage.world.object.magic.DecreaseWeight;
import lineage.world.object.magic.Detection;
import lineage.world.object.magic.EnchantDexterity;
import lineage.world.object.magic.EnchantMighty;
import lineage.world.object.magic.Haste;
import lineage.world.object.magic.HastePotionMagic;
import lineage.world.object.magic.HolyWalk;
import lineage.world.object.magic.NaturesBlessing;
import lineage.world.object.magic.ShapeChange;
import lineage.world.object.magic.Wafer;
import lineage.world.object.monster.Doppelganger;
import lineage.world.object.monster.Harphy;
import lineage.world.object.monster.Spartoi;
import lineage.world.object.monster.StoneGolem;
import lineage.world.object.npc.background.Cracker;
import lineage.world.object.npc.kingdom.KingdomCastleTop;
import lineage.world.object.npc.kingdom.KingdomCrown;
import lineage.world.object.npc.kingdom.KingdomDoor;
import lineage.world.object.npc.kingdom.KingdomDoorman;

public class PcRobotInstance extends RobotInstance {
    private static int ADEN_LIMIT = 1000000000; // 아데나 체크할 최소값 및 추가될 아데나 갯수.
    private static int HEALING_PERCENT = 95; // 체력 회복제를 복용할 시점 백분율
    protected static int GOTOHOME_PERCENT = 40; // 체력이 해당퍼센트값보다 작으면 귀환함.
    protected static int USABLE_MP_PERCENT = 30; // 해당 마나량이 해당 값보다 클때만 마법 사용
    
    private long partyWaitStartTime = 0; // 파티대기시간 체크용
    private long targetSearchStartTime = 0;
    private long lastFloorCheckTime = 0;
    
    protected static enum PCROBOT_MODE {
        None, // 기본값
        HealingPotion, // 물약상점 이동.
        HastePotion, // 초록물약 상점 이동.
        BraveryPotion, // 용기물약 상점 이동.
        ScrollPolymorph, // 변신주문서 상점 이동.
        Arrow, // 화살 상점 이동.
        InventoryHeavy, // 마을로 이동.
        ElvenWafer, // 엘븐와퍼 상점 이동.
        Polymorph, // 변신하기위해 마을로 이동.
        Stay, // 휴식 모드.
        Cracker, // 허수아비 모드.
    }
    
    private enum RobotWarState {
        IDLE,          // 일반 사냥
        WAR_ASSAULT,   // 공성 공격
        WAR_OCCUPY,    // 점령 진행
        WAR_END        // 공성 종료 처리
    }
    
    private AStar aStar; // 길찾기 변수
    private Node tail; // 길찾기 변수
    private int[] iPath; // 길찾기 변수
    // A* 실패 대상을 잠시 피하기 위한 집합
    private final Set<object> astarIgnore = ConcurrentHashMap.newKeySet();
    private List<object> attackList; // 전투 목록
    private List<object> astarList; // astar 무시할 객체 목록.
    private List<object> temp_list; // 주변셀 검색에 임시 담기용으로 사용.
    protected Item weapon;
    private Item doll;
    protected int weaponEn; // 무기 인첸
    private String weapon_name;
    private String doll_name;
    public PCROBOT_MODE pcrobot_mode; // 처리 모드.
    private int step; // 일렬에 동작처리중 사용되는 스탭변수.
    private int tempGfx; // 변신값 임시 저장용
    public volatile object target; // 공격 대상
    public volatile object targetItem; // 공격 대상
    public volatile object tempTarget; // 임시 대상
    private object currentAttackTarget; // 현재 전투 중인 타겟 저장
    protected boolean mythicPoly;
    protected boolean randomPoly;
    // 락용.
    private Object sync_ai = new Object();
    // 시체유지(toAiCorpse) 구간에서 사용중.
    // 재스폰대기(toAiSpawn) 구간에서 사용중.
    private long ai_time_temp_1;
    private long polyTime;
    private long delayTime;
    public long teleportTime;
    private long lastMoveAttemptTime = 0; // 마지막 이동 시도 시간
    private long lastDirectionSetTime = 0; // 마지막 방향 설정 시간
    private List<KingdomDoor> list_door; // 성에 사용되는 문 목록.
    // 로봇 행동.
    public String action;
    private boolean isWarCastle;
    private boolean isWarFC;
    // 리로드 확인용.
    public boolean isReload;
    
    // 새 상수 추가 (클래스 상단)
    private static final int TELEPORT_THRESHOLD_DISTANCE = 10; // 이동 칸 수 임계값 (조정 가능)
    private static final int TELEPORT_CHANCE_NO_TARGET = 80; // 타겟 없을 때 텔레포트 확률 (%)
    public int noTargetWalkCount = 0; // 타겟 없이 도보(배회)한 횟수 기억용
    
    //멘트 출력 후 대기 시간을 갖기 위한 지연 액션 변수
    private long delayedActionTime = 0;
    private Runnable delayedAction = null;
    
    // 신규 AI 모듈 필드 선언
    private IRobotClassBehavior classBehavior;
    private RobotHuntingEvaluator huntingEvaluator;
    private RobotPartyManager partyManager;
    
    private RobotWarState warState = RobotWarState.IDLE;
    private int[] myCachedFormationSeat = null; // 매 틱 생성 방지용 캐싱 좌석
    
    private int moveFailCount = 0;
    private int lastMoveX = -1;
    private int lastMoveY = -1;
    
    public PcRobotInstance() {
        aStar = new AStar();
        iPath = new int[2];
        astarList = new ArrayList<object>();
        attackList = new ArrayList<object>();
        temp_list = new ArrayList<object>();
        isWarCastle = false;
        isWarFC = false;
        target = targetItem = tempTarget = currentAttackTarget = null;
        list_door = new ArrayList<KingdomDoor>();
        partyManager = new RobotPartyManager();
    }

    @Override
    public void close() {
        super.close();
        //
        if (getInventory() != null) {
            for (ItemInstance ii : getInventory().getList())
                ItemDatabase.setPool(ii);
            getInventory().clearList();
        }
        weapon_name = doll_name = null;
        weapon = doll = null;
        action = null;
        target = targetItem = tempTarget = currentAttackTarget = null;
        teleportTime = delayTime = polyTime = ai_time_temp_1 = weaponEn = step = tempGfx = 0;
        randomPoly = mythicPoly = isReload = isWarCastle = isWarFC = false;
        if (Util.random(0, 99) < 10)
            pcrobot_mode = PCROBOT_MODE.Stay;
        else
            pcrobot_mode = PCROBOT_MODE.None;
        if (aStar != null)
            aStar.cleanTail();
        if (astarList != null)
            clearAstarList();
        if (temp_list != null)
            temp_list.clear();
    }

    @Override
    public void toSave(Connection con) {
    }

    public int getAttackListSize() {
        return attackList.size();
    }

    private void appendAttackList(object o) {
        synchronized (attackList) {
            if (!attackList.contains(o))
                attackList.add(o);
        }
    }

    public void removeAttackList(object o) {
        synchronized (attackList) {
            attackList.remove(o);
        }
    }

    protected List<object> getAttackList() {
        synchronized (attackList) {
            return new ArrayList<object>(attackList);
        }
    }

    protected boolean containsAttackList(object o) {
        synchronized (attackList) {
            return attackList.contains(o);
        }
    }

    public boolean containsAstarList(object o) {
        synchronized (astarList) {
            return astarList.contains(o);
        }
    }

    /* 사용여부 확인필요
    private void appendAstarList(object o) {
        synchronized (astarList) {
            if (!astarList.contains(o))
                astarList.add(o);
        }
    }
    */

    private void removeAstarList(object o) {
        synchronized (astarList) {
            astarList.remove(o);
        }
    }

    private void clearAstarList() {
        synchronized (astarList) {
            astarList.clear();
        }
    }

    public List<KingdomDoor> getListDoor() {
        return list_door;
    }

    public int getWeaponEn() {
        return weaponEn;
    }

    public void setWeaponEn(int weaponEn) {
        this.weaponEn = weaponEn;
    }

    public int getTempGfx() {
        return tempGfx;
    }

    public void setTempGfx(int tempGfx) {
        this.tempGfx = tempGfx;
    }

    public String getWeapon_name() {
        return weapon_name;
    }

    public void setWeapon_name(String weapon_name) {
        this.weapon_name = weapon_name;
    }

    public String getDoll_name() {
        return doll_name;
    }

    public void setDoll_name(String doll_name) {
        this.doll_name = doll_name;
    }

    public boolean getMythicPoly() {
        return mythicPoly;
    }

    public void setMythicPoly(boolean mythicPoly) {
        this.mythicPoly = mythicPoly;
    }

    public boolean getRandomPoly() {
        return randomPoly;
    }

    public void setRandomPoly(boolean randomPoly) {
        this.randomPoly = randomPoly;
    }

    public synchronized object getTarget() {
        return target;
    }

    public synchronized void setTarget(object newTarget) {

        if (this.target != newTarget) {

            clearAstarList();
            moveFailCount = 0;
            lastMoveX = -1;
            lastMoveY = -1;
        }
        target = newTarget;
        // ===== 공격타겟 안정화 =====
        if (currentAttackTarget != null) {
            if (Util.getDistance(this, currentAttackTarget) <= 10
                && !currentAttackTarget.isDead()) {
                return;
            }
        }
        currentAttackTarget = newTarget;
    }
    
    public static boolean toKingdomWarCheck() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar()) {
                return true; // 전쟁 중인 성이 있을 경우 true 반환
            }
        }
        return false; // 전쟁 중인 성이 없다면 false 반환
    }

    public String getWarCastleName() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar()) {
                return k.getName(); // 전쟁 중인 성의 이름 반환
            }
        }
        return null; // 전쟁 중인 성이 없으면 null 반환
    }

    public static int getWarCastleUid() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar()) {
                return k.getUid(); // 전쟁 중인 성의 이름 반환
            }
        }
        return -1; // 전쟁 중인 성이 없으면 -1 반환
    }

    public static boolean isCastleTopDead() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar() && k.isCastleTopDead()) {
                return true; // 전쟁 중인 성의 수호탑이 파괴 되어 있을 경우 true 반환
            }
        }
        return false; // 전쟁 중인 성이 없거나 수호탑이 파괴 되지 않은 경우 false 반환
    }

    public int getKingdomDoorHp() {
        for (KingdomDoor kd : list_door) {
            if (kd.getHp() == 0) {
                return kd.getHp(); // 전쟁 중인 성의 이름 반환
            }
        }
        return -1; // 전쟁 중인 성이 없으면 -1 반환
    }

    public void toWorldJoin(Connection con) {
        super.toWorldJoin();
        // 인공지능 상태 변경
        setAiStatus(Lineage.AI_STATUS_WALK);
        // 메모리 세팅
        setAutoPickup(Lineage.auto_pickup);
        World.appendRobot(this);
        BookController.toWorldJoin(this);
        // 컨트롤러 호출
        CharacterController.toWorldJoin(this);
        BuffController.toWorldJoin(this);
        SkillController.toWorldJoin(this);
        SummonController.toWorldJoin(this);
        MagicDollController.toWorldJoin(this);
        ClanController.toWorldJoin(this);
        RobotController.readSkill(con, this);
        RobotController.readBook(con, this);
        // 인벤토리 셋팅
        setInventory();
        // 인공지능 활성화 전에 직업별 두뇌 세팅
        initRobotAI();
        // 인공지능 활성화를 위해 AiThread에 등록
        AiThread.append(this);
    }

    @Override
    public void toWorldOut() {
        super.toWorldOut();
        // 서먼 한번더 확인
        SummonController.toWorldOut(this);
        setAiStatus(Lineage.AI_STATUS_DELETE);
        // 죽어있을경우에 처리를 위해.
        toReset(true);
        // 사용된 메모리 제거
        World.removeRobot(this);
        SummonController.toWorldOut(this);
        BookController.toWorldOut(this);
        SkillController.toWorldOut(this);
        ClanController.toWorldOut(this);
        CharacterController.toWorldOut(this);
        MagicDollController.toWorldOut(this);
        // 메모리 초기화
        close();
    }

    public void setPcBobot_mode(String mode) {
        if (mode.contains("사냥") || mode.contains("PvP") || mode.contains("공성")) {
            if (action.equalsIgnoreCase("허수아비 공격") || action.equalsIgnoreCase("마을 대기")) {
                setAiStatus(Lineage.AI_STATUS_WALK);
                pcrobot_mode = PCROBOT_MODE.None;
                target = targetItem = null;
                tempTarget = null;
                currentAttackTarget = null;
                clearAstarList();
            }
        } else if (mode.equalsIgnoreCase("허수아비 공격")) {
            if (pcrobot_mode != PCROBOT_MODE.Cracker || target == null)
                attackCracker();
        } else if (mode.equalsIgnoreCase("마을 대기")) {
            if (action.equalsIgnoreCase("허수아비 공격"))
                goToHome(true);
            else
                goToHome(false);
        }
    }

    @Override
    public void toRevival(object o) {
        if (isDead()) {
            super.toReset(false);
            target = targetItem = null;
            tempTarget = null;
            currentAttackTarget = null;
            clearAstarList();
            int[] home = null;
            home = Lineage.getHomeXY();
            setHomeX(home[0]);
            setHomeY(home[1]);
            setHomeMap(home[2]);
            toTeleport(getHomeX(), getHomeY(), getHomeMap(), isDead() == false);
            // 다이상태 풀기.
            setDead(false);
            // 체력 채우기.
            setNowHp(level);
            // 패킷 처리.
            toSender(S_ObjectRevival.clone(BasePacketPooling.getPool(S_ObjectRevival.class), o, this), false);
            // 상태 변경.
            ai_time_temp_1 = 0;
            setAiStatus(Lineage.AI_STATUS_WALK);
        }
    }

    @Override
    public void setDead(boolean dead) {
        super.setDead(dead);
        if (dead) {
            ai_time = 0;
            setAiStatus(Lineage.AI_STATUS_DEAD);
        }
    }

    @Override
    public void toDamage(Character cha, int dmg, int type, Object... opt) {
        super.toDamage(cha, dmg, type);
        // 버그 방지 및 자기자신이 공격했을 경우 무시.
        if (cha == null || cha.getObjectId() == getObjectId() || dmg <= 0 || cha.getGm() > 0)
            return;
        // 사냥 또는 PvP 모드에서 확률적으로 랜덤 텔레포트
        if ((cha instanceof PcInstance && action.contains("사냥") && !action.contains("공성")) ||
                (cha instanceof MonsterInstance && action.contains("PvP") && !action.contains("공성"))) {
        	if (!isWarCastle && Util.random(1, 100) < 20) {
        	    randomTeleport();
        	}
            return;
        }
        object o = (object) cha;
        // 전투 중이 아니고, 공격 가능한 상대일 경우만 setTarget
        if (currentAttackTarget == null && isAttack(o, true)) {
            setTarget(o);
        }
        // 마법 공격 시 확률적으로 멘트를 출력 (전투 중이 아니거나 기존 대상이면 허용)
        if ((cha instanceof PcInstance || cha instanceof PcRobotInstance)
                && (target == null || target == cha)
                && (currentAttackTarget == null || currentAttackTarget == cha)
                && (!isWarCastle || !action.contains("공성"))) {

            if (Util.random(1, 100) <= Lineage.robot_ment_probability && type == Lineage.ATTACK_TYPE_MAGIC) {

                RobotController.getRandomMentAndChat(
                        Lineage.AI_ATTACKED_MENT,
                        this,cha,
                        Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ATTACKED_MENT_DELAY);
            }
        }
        
        // 혈맹원이 공격당했을 때 근처 동료가 도와주도록 처리
        Clan clan = ClanController.find(this);
        if (clan != null) {
            for (object obj : getInsideList()) {
                if (obj instanceof PcRobotInstance) {
                    PcRobotInstance member = (PcRobotInstance) obj;
                    if (clan.containsMemberList(member.getName())) {
                        // 공격자가 유효하고, 해당 혈맹원이 전투 중이 아닐 경우
                        if ((member.currentAttackTarget == null || member.currentAttackTarget == cha)
                                && (cha instanceof PcInstance || cha instanceof RobotInstance)) {
                            if (!RobotController.isCastleTopOutsideCoords(member, getWarCastleUid()) || !isWarCastle || !action.contains("공성")) {
                            	if (!member.isDead()
                            	        && member.currentAttackTarget == null
                            	        && member != this) {
                            	    member.setTarget(cha);
                            	}
                            }
                        }
                    }
                }
            }
        }
       
        // 적에게 피격 당했을 때 혈맹 총동원령 요청 (30% 확률로 지원 요청)
        if (getClanId() > 0 && Util.random(0, 100) < 30) {
            RobotClanController.requestClanAssistance(this, cha);
        }
        // 타겟이 없을 경우에만 설정
        if (currentAttackTarget == null) {
            // 🔹 거리 제한 추가 (근접 중복 방지)
            if (Util.getDistance(this, cha) <= 10) {
                // 🔹 이미 다른 로봇이 집중 공격 중인지 확인
                if (!isTargetOvercrowded((object) cha)) {
                    setTarget(cha);
                }
            }
        }
        // 길찾기 A* 예외 리스트에서 제거
        removeAstarList(cha);
    }

    @Override
    public void toAiThreadDelete() {
        super.toAiThreadDelete();
        // 사용된 메모리 제거
        World.removeRobot(this);
        BookController.toWorldOut(this);
        CharacterController.toWorldOut(this);
    }

    @Override
    public void toAi(long time) {
    
     if (delayedActionTime > 0) {
            if (System.currentTimeMillis() >= delayedActionTime) {
                if (delayedAction != null) {
                    delayedAction.run(); // 2초 후 예약된 텔레포트 실행
                    delayedAction = null;
                }
                delayedActionTime = 0;
            }
            return;
        }
    
        synchronized (sync_ai) { // ✅ AI 실행 동기화 (최소한의 범위 유지)
            if (isReload)
                return;
           
            // 사망 처리
            if (isDead()) {
                if (ai_time_temp_1 == 0) ai_time_temp_1 = time;
                if (ai_time_temp_1 + Lineage.ai_robot_corpse_time > time) return;
                goToHome(false);
                toRevival(this);
            }
           
            // ✅ 마을 대기 모드
            if ("마을 대기".equalsIgnoreCase(action)) {
                if (!World.isSafetyZone(getX(), getY(), getMap())) goToHome(false);
                return;
            }
            if (getInventory() == null) return;
            // 허수아비 공격 모드
            if ("허수아비 공격".equalsIgnoreCase(action) && pcrobot_mode != PCROBOT_MODE.Cracker) {
                if ("bow".equalsIgnoreCase(weapon.getType2()) && getInventory().find(Arrow.class) != null) {
                    attackCracker();
                    return;
                } else {
                    attackCracker();
                    return;
                }
            }
        } // 동기화 종료 (불필요한 동기화 최소화)
        // 무기 착용 처리
        synchronized (this) {
            if (getInventory().getSlot(Lineage.SLOT_WEAPON) == null ||
                    !getInventory().getSlot(Lineage.SLOT_WEAPON).getItem().getName().equalsIgnoreCase(this.getWeapon_name())) {
                if (getInventory().find(weapon) == null) {
                    weapon = RobotController.getWeapon(getClassType());
                    if (weapon != null) {
                        ItemInstance item = ItemDatabase.newInstance(weapon);
                        if (item != null) {
                            item.setObjectId(ServerDatabase.nextEtcObjId());
                            item.setEnLevel(weaponEn);
                            getInventory().append(item, false);
                            item.toClick(this, null);
                        }
                    }
                }
                return;
            } else if (!RobotController.isCastleTopInsideCoords(this, getWarCastleUid())) {
                ItemInstance item = ItemDatabase.newInstance(weapon);
                if (item != null) item.toClick(this, null);
            }
        }
        // 체력 회복
        if (getHpPercent() <= HEALING_PERCENT) toHealingPotion();
       
        // 직업별 특수 위기 탈출 (HP 20% 이하 감지 시 도보회피, 긴급텔 등)
        if (this.classBehavior != null && getHpPercent() <= 20.0) {
            this.classBehavior.executeFlee(this);
            return;
        }
        // 체력 부족 시 귀환 처리
        if (!World.isSafetyZone(getX(), getY(), getMap()) && getHpPercent() <= GOTOHOME_PERCENT) {
            if ((getMap() == 4 && Util.random(0, 99) <= 60) || Util.random(0, 99) <= 10) {
                synchronized (this) { pcrobot_mode = PCROBOT_MODE.Stay; }
                goToHome(false);
                ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                return;
            } else if (Util.random(0, 99) <= 20 && randomTeleport()) {
                synchronized (this) { pcrobot_mode = PCROBOT_MODE.None; }
                return;
            }
        }
        // 아이템 지급 (공유 데이터 접근 시 동기화)
        synchronized (this) {
            String[] items = { "무한 체력 회복 룬", "무한 신속 룬", "무한 가속 룬", "무한의 화살통", "무한 변신 주문서", "무한 신화 변신 북", "무한 버프 물약" };
            for (String itemName : items) {
                if (getInventory().find(itemName) == null) {
                    RobotController.giveItem(this, itemName, 1);
                }
            }
        }
        // 타겟이 유효하지 않을 경우 → 걷기 상태로 복귀
        if (getAiStatus() == Lineage.AI_STATUS_PICKUP && pcrobot_mode != PCROBOT_MODE.Cracker) {
            if (targetItem == null) {
                setAiStatus(Lineage.AI_STATUS_WALK);
            }
        }
        // 혈맹 및 성 정보 확인
        Clan c = ClanController.find(this);
        isWarCastle = toKingdomWarCheck();
        // 화살 장착
        if ("bow".equalsIgnoreCase(weapon.getType2())) setArrow();
        // AI 상태 변경 (타겟 및 공격 모드 전환)
        synchronized (this) {
            switch (getAiStatus()) {
                case Lineage.AI_STATUS_WALK:
                    if (target != null) {
                        setAiStatus(Lineage.AI_STATUS_ATTACK);
                        currentAttackTarget = target;
                        target = null; // 상태 전환 후 초기화
                    }
                    break;
                case Lineage.AI_STATUS_ATTACK:
                    if (pcrobot_mode != PCROBOT_MODE.Cracker) {
                        currentAttackTarget = checkTargetValidity(currentAttackTarget); // ✅ 유효성 검사
                    }
                    if (currentAttackTarget == null && pcrobot_mode != PCROBOT_MODE.Cracker) {
                        if (!randomTeleport())
                            setAiStatus(Lineage.AI_STATUS_WALK);
                    }
                    break;
            }
        }
        // 무게 초과 처리
        synchronized (this) {
            if (pcrobot_mode == PCROBOT_MODE.None && !getInventory().isWeightPercent(82)) {
                pcrobot_mode = PCROBOT_MODE.InventoryHeavy;
            }
        }
        // 변신 처리
        if (!(c != null && c.getLord() != null && c.getLord().equalsIgnoreCase(getName()) &&
                getClassType() == Lineage.LINEAGE_CLASS_ROYAL && RobotController.isKingdomCrownCoords(this))) {
            synchronized (this) {
                if (pcrobot_mode == PCROBOT_MODE.None && getGfx() == getClassGfx() && RobotController.isPoly(this)) {
                    pcrobot_mode = PCROBOT_MODE.Polymorph;
                }
            }
        }
        // 모드 변경 시 추가 처리
        synchronized (this) {
            if (pcrobot_mode != PCROBOT_MODE.None && pcrobot_mode != PCROBOT_MODE.Cracker) {
                setAiStatus(Lineage.AI_STATUS_WALK);
                ItemInstance aden = getInventory().findAden();
                if (aden == null || aden.getCount() < ADEN_LIMIT) {
                    Item adenItem = ItemDatabase.find("아데나");
                    if (adenItem != null) {
                        aden = aden == null ? ItemDatabase.newInstance(adenItem) : aden;
                        aden.setObjectId(ServerDatabase.nextEtcObjId());
                        getInventory().append(aden, false);
                        aden.setCount(aden.getCount() + ADEN_LIMIT);
                    }
                }
            }
        }
        super.toAi(time);
    }

    /**
     * 랜덤 텔레포트
     * 2018-08-11
     * by connector12@nate.com
     * 수정: 텔레포트 실패 시 이동 중 실시간 타겟 체크
     */
    protected boolean randomTeleport() {
    	if (isWarCastle && action != null && action.contains("공성")) {
            return false;
        }
    	
        if (teleportTime >= System.currentTimeMillis())
            return false;

        if (!isPossibleMap())
            return false;

        teleportTime = System.currentTimeMillis() + Util.random(1000, 3000);

        // 🔹 AI 상태 초기화
        target = null;
        targetItem = null;
        tempTarget = null;
        currentAttackTarget = null;
        clearAstarList();

        ai_time = SpriteFrameDatabase.getGfxFrameTime(
                this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);

        setAiStatus(Lineage.AI_STATUS_WALK);

        if (!LocationController.isTeleportZone(this, true, false))
            return false;

        Util.toRndLocation(this);

        // ✅ 실제 랜덤 좌표 추출
        int tx = getX();
        int ty = getY();
        int tmap = getMap();

        // ✅ 겹침 방지용 위치 보정 (최대 10회)
        int attempt = 10;
        while (attempt-- > 0) {
            if (World.isThroughObject(tx, ty, tmap, getHeading())
                    && !World.isNotMovingTile(tx, ty, tmap)) {
                break;
            }
            tx += Util.random(-2, 2);
            ty += Util.random(-2, 2);
        }
        // 🔴 기존 홈 텔레포트 무력화
        // toTeleport(getHomeX(), getHomeY(), getHomeMap(), true);

        // ✅ 실제 랜덤 위치 적용
        toTeleport(tx, ty, tmap, true);

        toSender(S_ObjectLock.clone(
                BasePacketPooling.getPool(S_ObjectLock.class), 0x09));

        return true;
    }
    
    protected void moveToRandomLocation(long time) {
        try {
            int currentHeading = getHeading(); // 현재 방향 유지
            List<Integer> directionsToTry = new ArrayList<>();
            directionsToTry.add(currentHeading); // 전진 우선
            // 좌우로 회피 가능한 방향 추가 (시계 ↔ 반시계 교차)
            for (int i = 1; i <= 3; i++) {
                int right = (currentHeading + i) % 8;
                int left = (currentHeading - i + 8) % 8;
                directionsToTry.add(right);
                directionsToTry.add(left);
            }
            // 마지막으로 반대 방향 추가 (최후의 수단)
            directionsToTry.add((currentHeading + 4) % 8);
            boolean moved = false;
            for (int headingTry : directionsToTry) {
                setHeading(headingTry); // 시도 방향으로 헤딩 설정
                int x = Util.getXY(headingTry, true) + this.x;
                int y = Util.getXY(headingTry, false) + this.y;
                // 범위 밖이면 텔레포트
                if (!Util.isDistance(x, y, map, start_x, start_y, map, 60)) {
                    if (teleportTime < time) {
                        toTeleport(start_x, start_y, start_map, true);
                        return;
                    }
                }
                // 이동 가능 여부 판단
                boolean canMove = World.isThroughObject(this.x, this.y, this.map, headingTry)
                        && !World.isMapdynamic(x, y, map)
                        && !World.isNotMovingTile(x, y, map);
                if (canMove) {
                    toMoving(this, x, y, headingTry, true, false);
                    teleportTime = System.currentTimeMillis() + Util.random(1000, 3000);
                    moved = true;
                    // 이동 중 실시간 타겟 체크 (텔레포트 실패 시 여기서 실행됨)
                    checkTargetDuringMove();
                    break;
                }
            }
            if (!moved) {
                if ((getMap() == 0 || getMap() == 4) && teleportTime < time) {
                    toTeleport(start_x, start_y, start_map, true);
                } else {
                    goToHome(false);
                }
            }
        } catch (Exception e) {
            lineage.share.System.printf("[처리 오류] moveToRandomLocation(long time)\r\n : %s\r\n", e.toString());
        }
    }

    /**
     * 이동 중 실시간 타겟 체크: 주변 타겟 탐색 후 가장 가까운 타겟부터 처리
     * - getInsideList()로 화면 내 객체 확인
     * - isAttack(o, true) 조건 만족 시 가장 가까운 타겟 선택
     * - 타겟 설정 후 AI 상태 전환
     */
    private void checkTargetDuringMove() {
        object closestTarget = null;
        List<object> insideList = getInsideList(true);
        if (insideList == null || insideList.isEmpty()) return;
        for (object o : insideList) {
            if (Util.isAreaAttack(this, o) && isAttack(o, true)) {
                if ((o instanceof PcInstance && action.contains("PvP")) ||
                        (o instanceof MonsterInstance && action.contains("사냥"))) {
                    closestTarget = getClosestTarget(closestTarget, o);
                }
            }
        }
        if (closestTarget != null && isAttack(closestTarget, true)) {
            setTarget(closestTarget);
            setAiStatus(Lineage.AI_STATUS_ATTACK);
            currentAttackTarget = closestTarget;
        }
    }

    /**
     * [전쟁 서사시: 최종 장] 메인 두뇌 스위치 및 사냥/공성 완벽 분리
     * - 공성 상태(handleWarState) 감지 및 진격 오더 하달
     * - 공성 중 군주 타겟팅 무시, 사냥 텔레포트/배회 전면 차단
     * - 기존 사냥/수성 레거시 코드 100% 보존 및 안전한 상태 분기
     */
    @Override
    protected void toAiWalk(long time) {
        
        // 🌟 [핵심 스위치 추가] 1. 매 틱마다 공성 상태를 감지하고 상태 전이 및 진격을 수행합니다.
        handleWarState();

        autoPartyManagement();
       
        // 부모 클래스의 기본 AI 동작 수행
        super.toAiWalk(time);
        
        if (this.classBehavior != null) {
            this.classBehavior.executeBuffAndSupport(this);
        }
        
        if (getPartyId() > 0) {
            handlePartyCollaboration();
            Party p = PartyController.find(this);
            if (p != null && p.getMaster() != this) {
            }
        }
        
        if ((getRobotStatus() & RobotConversationController.ROBOT_STATE_CHATTING) != 0) {
            return;
        }
        
        // 🌟 [조건 추가] 공성 중일 때는 던전이 아니라는 이유로 마을로 강제 귀환하는 것을 방지
        if (this.warState == RobotWarState.IDLE && !World.isSafetyZone(getX(), getY(), getMap()) && !isWarCastle && !action.contains("공성")) {
            if (!isPossibleMap() && !isValidDungeonFloor()) { 
                goToHome(false); 
                return;
            }
        }
        
        Kingdom k = KingdomController.find(getWarCastleUid());
        if (Lineage.open_wait && pcrobot_mode != PCROBOT_MODE.Stay && pcrobot_mode != PCROBOT_MODE.Cracker && isWait())
            return;
            
        switch (pcrobot_mode) {
        case InventoryHeavy:
            toInventoryHeavy();
            return;
        case Polymorph:
            toPolymorph();
            return;
        case Stay:
            toStay(time);
            return;
        case ScrollPolymorph:
            toScrollPolymorph();
            return;
        default: 
            break;
        }
        
        if (pcrobot_mode != PCROBOT_MODE.Cracker && pcrobot_mode != PCROBOT_MODE.Stay) {
            toHealingPotion();
            toBuffPotion();
            List<Skill> skill_list = SkillController.find(this);
            if (toSkillHealMp(skill_list) || toSkillHealHp(skill_list) || toSkillBuff(skill_list) || toSkillSummon(skill_list)) {
                this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                return;
            }
            if (toBuffSummon()) {
                this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                return;
            }
        }
        
        // 🌟 [조건 추가] 사냥터 텔레포트 이동 (공성 중 금지)
        if (this.warState == RobotWarState.IDLE && !isExcludedMap(getMap()) && World.isSafetyZone(getX(), getY(), getMap())) {
            manageDelayTime();
            if (isBuffCriminal()) BuffController.remove(this, Criminal.class);
            List<Book> list = BookController.find(this);
            if (list.isEmpty()) return;
            teleportToHuntingGround(list);
        }
        
        if (tempTarget == null && !isWarCastle && !World.isGiranHome(getX(), getY(), getMap())) {
            for (object obj : getInsideList()) {
                if (obj instanceof PcInstance || obj instanceof PcRobotInstance) {
                    tempTarget = getClosestTarget(tempTarget, obj);
                }
            }
            if (tempTarget != null) {
                if (!tempTarget.isInvis() && Util.random(1, 100) <= Lineage.robot_ment_probability) {
                    RobotController.getRandomMentAndChat(Lineage.AI_MEET_MENT, this, tempTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_MEET_MENT_DELAY);
                }
                else if (tempTarget.isInvis() && Util.random(1, 100) <= Lineage.robot_ment_probability) {
                    RobotController.getRandomMentAndChat(Lineage.AI_INVISIBLE_MENT, this, tempTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_INVISIBLE_MENT_DELAY);
                }
                tempTarget = null;
            }
        }
        
        // [레거시 로직 보존] 수성 방어 진영(isWarFC) 판별 로직
        if (k != null && getClanId() != 0 && k.isWar() && k.getClanId() != 0 && k.getClanId() == getClanId() && isWarCastle && action.contains("공성")) {
            if (!KingdomController.isKingdomLocation(this, k.getUid()) && KingdomController.getUserCountInKingdomArea(k.getUid()) < Lineage.robot_kingdom_war_max_people) {
                if (this.teleportTime < System.currentTimeMillis()) {
                    if (!RobotController.isKingdomLocation(this, true, k.getUid())) {
                        RobotController.toKingdomRandomLocationTeleport(this, k.getUid());
                    }
                    this.teleportTime = System.currentTimeMillis() + 4000;
                }
                target = null;
                isWarFC = false;
                this.ai_time = SpriteFrameDatabase.find(getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                return;
            }
            
            isWarFC = true;
            if (currentAttackTarget == null) findTarget();
            
            if (getClassType() == Lineage.LINEAGE_CLASS_ELF) {
                if (Util.random(0, 150) == 0) Detection.init(this, SkillDatabase.find(2, 4));
                if (Util.random(0, 100) == 0) {
                    for (object o : getInsideList(true)) {
                        if (o.getClanId() == getClanId() && Util.random(0, 10) == 0)
                            NaturesBlessing.onBuff(this, (Character) o, SkillDatabase.find(21, 3));
                    }
                    this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                }
            }
        } else {
            isWarFC = false;
        }
        
        // 🌟 [조건 추가] 구버전 공성 진격 스위치 (handleWarState와 충돌하지 않도록 IDLE일 때만 동작 보존)
        if (this.warState == RobotWarState.IDLE) {
            if (k != null && getClanId() != 0 && k.isWar() && (k.getClanId() == 0 || k.getClanId() != getClanId()) && isWarCastle && action.contains("공성")) {
                moveToCastleLocation();
            }
        }
        
        // 🌟 [조건 추가] 군주는 공성 중 타겟팅 무시 (일반 병력과 평시 사냥 때만 타겟팅)
        if (this.warState == RobotWarState.IDLE || (this.warState != RobotWarState.IDLE && !isKing())) {
            if (currentAttackTarget == null) findTarget();
            if (action.contains("사냥") && currentAttackTarget == null) findTarget(); 
        }
        
        Party partyCheck = PartyController.find(this);
        boolean isFollower = (partyCheck != null && partyCheck.getMaster() != this);
        
        // 🌟 [조건 추가] 과밀 회피 다음 층 이동 (공성 중 전면 금지)
        if (this.warState == RobotWarState.IDLE && !isFollower && target == null && this.currentAttackTarget == null) {
            if (checkAndMoveToNextFloor()) return; 
        }

        boolean canTeleport = LocationController.isTeleportZone(this, false, false);
        long timeoutLimit = canTeleport ? 30000 : 120000;

        // 🌟 [조건 추가] 사냥 타임아웃 텔레포트 (공성 중 전면 금지)
        if (this.warState == RobotWarState.IDLE) {
            if (!isFollower && target == null && this.currentAttackTarget == null) {
                if (targetSearchStartTime == 0) {
                    targetSearchStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - targetSearchStartTime > timeoutLimit) {
                    targetSearchStartTime = 0;
                    if (canTeleport) {
                        randomTeleport();
                    } else {
                        if (partyCheck != null) {
                            broadcastRobotChat("몹이 안 보이네요. 다른 던전으로 이동합니다!");
                            setDelayedAction(2000, () -> toTeleportToAppropriateHuntingGround());
                        }
                        toTeleportToAppropriateHuntingGround();
                    }
                    return; 
                }
            } else if (target != null || this.currentAttackTarget != null) {
                targetSearchStartTime = 0;
            }
        }

        // 🌟 [조건 추가] 타겟 없을 때 무작위 배회 (공성 중 전면 금지)
        if (this.warState == RobotWarState.IDLE && target == null && (!isWarCastle || !action.contains("공성"))) {
            if (Util.random(0, 100) < TELEPORT_CHANCE_NO_TARGET && randomTeleport()) {
                findTarget();
                return;
            } else {
                moveToRandomLocation(time);
                setAiStatus(Lineage.AI_STATUS_WALK);
            }
        }
        
        if (target == null && targetItem == null & getAiStatus() != Lineage.AI_STATUS_PICKUP && getAiStatus() != Lineage.AI_STATUS_ESCAPE) {
            findItem();
        }
        
        if (Util.random(0, 1) == 0) clearAstarList();
    }
    
    // 특정 맵 제외 체크
    private boolean isExcludedMap(int map) {
        return map == 70 || map == 68 || map == 69 || map == 85 || map == 86;
    }
   
    // 딜레이 관리
    private void manageDelayTime() {
        if (delayTime == 0)
            delayTime = System.currentTimeMillis() + (1000 * (Util.random(3, 10)));
        if (delayTime > 0 && delayTime <= System.currentTimeMillis())
            delayTime = 0;
    }
    private void teleportToHuntingGround(List<Book> list) {
        if (list == null || list.isEmpty()) return;
        int myLevel = getLevel();
        int maxMinLevel = -1;
        // 1. 내 레벨로 입장 가능한 사냥터 중 '가장 높은 입장 레벨(최고 효율)' 찾기
        for (Book book : list) {
            if (book != null && book.getEnable() && myLevel >= book.getMinLevel()) {
                if (book.getMinLevel() > maxMinLevel) {
                    maxMinLevel = book.getMinLevel();
                }
            }
        }
        // 입장 가능한 곳이 없으면 리턴
        if (maxMinLevel == -1) return;
        // 2. 최고 레벨 제한을 가진 사냥터만 후보 리스트에 추가
        List<Book> bestList = new ArrayList<>();
        for (Book book : list) {
            if (book != null && book.getEnable() && book.getMinLevel() == maxMinLevel) {
                bestList.add(book);
            }
        }
        if (bestList.isEmpty()) return;
        // 3. 최고 효율 사냥터 중 하나를 무작위 선택
        Book b = bestList.get(Util.random(0, bestList.size() - 1));
       
        target = targetItem = null;
        currentAttackTarget = null;
        int targetX = b.getX();
        int targetY = b.getY();
        int targetMap = b.getMap();
        if (!Util.isSafeTile(targetX, targetY, targetMap)) {
            targetX = b.getX() + Util.random(-5, 5);
            targetY = b.getY() + Util.random(-5, 5);
        }
        toTeleport(targetX, targetY, targetMap, true);
       
        this.start_x = targetX;
        this.start_y = targetY;
        this.start_map = targetMap;
       
        toSender(S_ObjectLock.clone(BasePacketPooling.getPool(S_ObjectLock.class), 0x09));
    }
    
    /**
     * 메인 공격 대상을 찾는 메서드
     */
    private void findTarget() {
        Kingdom k = KingdomController.find(getWarCastleUid());
        Clan c = ClanController.find(this);
        synchronized (this) { // target 변경 시 동기화 유지
            target = null;
        }
        // ✅ 1. 사냥터 인구수 40명 초과 검사 및 이동
        // (주의: 1초에 수십 번 도는 AI에서 매번 인원수를 세면 서버 렉이 폭발하므로, 3% 확률로만 부드럽게 검사합니다)
        if (Util.random(0, 100) < 3) {
            int currentMap = getMap();
            long userCount = 0;
            for (PcInstance pc : World.getPcList()) {  // 문제점 수정: stream() -> for 루프 (성능 최적화)
                if (pc.getMap() == currentMap) userCount++;
            }
            for (RobotInstance r : World.getRobotList()) {
                if (r.getMap() == currentMap) userCount++;
            }
           
            if (userCount >= 30) {
                if (isWarCastle && action.contains("공성")) {
                    return; // 🔥 공성 중이면 절대 이동하지 않음
                }
                if (LocationController.isTeleportZone(this, true, false)) {
                    randomTeleport();
                } else {
                    ChattingController.toChatting(this, "이 사냥터는 너무 붐비네요. 다른 곳으로 이동합니다.", Lineage.CHATTING_MODE_NORMAL);
                    toTeleportToAppropriateHuntingGround();
                }
                return;
            }
        }
        // ✅ 2. 직업별 고유 타겟팅 (투시 버그 및 렉풀기 방어 로직 추가)
        if (this.classBehavior != null && (!isWarCastle || !action.contains("공성"))) {
            object specialTarget = this.classBehavior.findTarget(this);
            // 🚨 안전장치: 벽 너머의 적을 타겟으로 잡아 제자리 렉이 걸리는 것을 방지하기 위해 거리가 12칸 이내인 경우만 허용
            if (specialTarget != null && Util.getDistance(this, specialTarget) <= 12) {
                if (isTargetOvercrowded(specialTarget)) {
                    return;
                }
                synchronized (this) {
                    setTarget(specialTarget);
                }
                noTargetWalkCount = 0;
                return;
            }
        }
        // ✅ 3. 기존 탐색 로직 (공성전 로직 100% 보호)
        if (!isWarCastle || !action.contains("공성")) {
            processInsideList();
        } else {
            processAllList(k, c);
        }
        // ✅ 4. 타겟이 없을 때 즉시 텔레포트 하지 않고 주변 도보(순찰) 진행
        if (this.target == null) {
            noTargetWalkCount++;
           
            // 약 30회(AI 틱 기준) 정도는 타겟이 없어도 즉시 텔레포트하지 않고 무작위로 배회함
            if (noTargetWalkCount < 30) {
                // 가만히 멍때리지 않도록 10% 확률로 방향을 틀어 도보를 유도
                if (Util.random(0, 100) < 10) {
                    setHeading(Util.random(0, 7));
                    moveToRandomLocation(System.currentTimeMillis());
                }
            } else {
                // 30회 이상 한참을 돌아다녔는데도 몬스터가 없다면 그때 비로소 텔레포트/이동 수행
             if (LocationController.isTeleportZone(this, true, false)) {
                    randomTeleport();
                } else {
                    toTeleportToAppropriateHuntingGround();
                }
                noTargetWalkCount = 0; // 텔레포트 후 카운트 초기화
            }
        } else {
            // 타겟을 찾았을 경우 배회 카운트 즉시 초기화
            noTargetWalkCount = 0;
        }
    }

    /**
     * 일반적인 대상 탐색 로직
     */
    private void processInsideList() {
        try {
            if (target == null) {
                object temp = null;
                List<object> insideList = getInsideList(true);
                if (insideList == null || insideList.isEmpty()) return;
                // 불필요한 전체 리스트 동기화 제거 → 개별 객체만 동기화 필요
                for (object o : insideList) {
                    if (Util.isAreaAttack(this, o) && isAttack(o, true)) {
                        if ((o instanceof PcInstance && action.contains("PvP") && Util.random(0, 99) < 60) ||
                                (o instanceof MonsterInstance && (action.contains("사냥") || action.contains("공성")))) {
                            temp = getClosestTarget(temp, o);
                        }
                    }
                }
                // 최종적으로 target 설정 (쓰기 작업이므로 동기화 필요)
                synchronized (this) {
                    if (temp != null && isAttack(temp, true)) {
                        setTarget(temp);
                    }
                }
            }
        } catch (Exception e) {
            lineage.share.System.printf("[처리 오류] processInsideList() - %s\r\n", e.toString());
            e.printStackTrace();
        }
    }
    /**
     * 공성전 대상 탐색 로직
     */
    private void processAllList(Kingdom k, Clan c) {
        try {
            if (target == null) {
                object temp = null;
                List<object> allList = getInsideList();
                if (allList == null || allList.isEmpty()) return;
                for (object oo : allList) {
                    if (oo == null || oo instanceof GuardInstance || !Util.isAreaAttack(this, oo) || !isAttack(oo, true)) {
                        continue;
                    }
                    // 공격 진영
                    if (k != null && this.getClanId() != 0 && k.getClanId() != this.getClanId()) {
                        if ((oo instanceof PcInstance || oo instanceof PcRobotInstance || oo instanceof KingdomDoor || oo instanceof KingdomCastleTop)
                                && action != null && action.contains("공성") && KingdomController.isKingdomLocation(this, getWarCastleUid())
                                && (c.getLord() == null || !c.getLord().equalsIgnoreCase(getName()) || (!c.getLord().equalsIgnoreCase(getName()) && !isCastleTopDead()))) {
                            temp = getClosestTarget(temp, oo);
                        }
                        if (oo instanceof KingdomCrown && c.getLord() != null && c.getLord().equalsIgnoreCase(getName())
                                && getInventory().getSlot(Lineage.SLOT_WEAPON) == null && this.getGfx() == this.getClassGfx()
                                && RobotController.isCastleTopInsideCoords(this, getWarCastleUid())
                                && action.contains("공성")) {
                            temp = getClosestTarget(temp, oo);
                        }
                    }
                    // 방어 진영
                    else if (k != null && this.getClanId() != 0 && k.getClanId() == this.getClanId()) {
                        if ((oo instanceof PcInstance || oo instanceof PcRobotInstance || (oo instanceof MonsterInstance && action != null && action.contains("공성")))) {
                            temp = getClosestTarget(temp, oo);
                        }
                    }
                }
                synchronized (this) {
                    if (temp != null && isAttack(temp, true)) {
                        setTarget(temp);
                    }
                }
            }
        } catch (Exception e) {
            lineage.share.System.printf("[처리 오류] processAllList() - %s\r\n", e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * 가장 가까운 대상을 반환하는 헬퍼 메서드
     */
    private object getClosestTarget(object current, object candidate) {
        if (current == null) {
            return candidate;
        }
        int rangeCurrent = Util.getDistance(this, current);
        int rangeCandidate = Util.getDistance(this, candidate);
        return (rangeCandidate < rangeCurrent) ? candidate : current;
    }
    /**
     * ✅ 타겟 아이템 탐색 및 설정
     * - 기존 타겟이 유효하지 않으면 제거
     * - 주변에서 가장 가까운 아이템을 새 타겟으로 지정
     */
    private void findItem() {
        synchronized (this) {
            if (targetItem != null && !isPickupItem(targetItem)) {
                targetItem = null;
            }
        }
        processPickupItem();
    }
    /**
     * ✅ 주변에 있는 줍을 수 있는 아이템 중 가장 가까운 것을 타겟으로 설정
     */
    private void processPickupItem() {
        try {
            List<object> insideList = getInsideList();
            if (insideList == null || insideList.isEmpty())
                return;
            object closestItem = insideList.stream()
                    .filter(this::isPickupItem)
                    .min(Comparator.comparingInt(o -> Util.getDistance(this, o)))
                    .orElse(null);
            if (closestItem != null) {
                synchronized (this) {
                    setTarget(closestItem);
                }
                setAiStatus(Lineage.AI_STATUS_PICKUP); // 줍기 상태 진입
                return;
              
            }
        } catch (Exception e) {
            lineage.share.System.printf("[처리 오류] processPickupItem() - %s\r\n", e.toString());
            e.printStackTrace();
        }
    }

    /**
     * ✅ 줍기 대상 아이템 여부 판단
     * - 아이템 객체인지 확인
     * - 이미 경로 리스트에 포함되지 않았는지 확인
     * - 이름이 "아데나"가 아닌 경우만 대상
     */
    protected boolean isPickupItem(object o) {
        if (!(o instanceof ItemInstance)) {
            return false;
        }
        if (containsAstarList(o)) {
            return false;
        }
        ItemInstance item = (ItemInstance) o;
        return !item.getItem().getName().equalsIgnoreCase("아데나");
    }
    /**
     * 성으로 이동 처리
     */
    private synchronized void moveToCastleLocation() {
        Clan c = ClanController.find(this);
        for (Kingdom k : KingdomController.getList()) {
            // 1. 공성존에 없으면 공격 위치로 이동
            if (k.isWar() && !KingdomController.isKingdomLocation(this, k.getUid())) {
                if (this.teleportTime < System.currentTimeMillis()) {
                    RobotController.isKingdomAttLocation(this, true, k.getUid());
                    this.teleportTime = System.currentTimeMillis() + 4000; // 문제점 수정: 무한 루프 방지 쿨타임
                }
                target = null;
                currentAttackTarget = null;
               
                moveToCastle(k.getUid(), RobotController.KINGDOM_CROWN_COORDS, true);
                return;
               
            } else {
                // . 군주 여부 및 위치 확인
                String lordName = c.getLord();
                String myName = getName();
                int myClassType = getClassType();
                boolean isRoyal = myClassType == Lineage.LINEAGE_CLASS_ROYAL;
                boolean isLord = isRoyal && lordName.equalsIgnoreCase(myName);
                boolean atCastleTop = RobotController.isCastleTopInsideCoords(this, k.getUid());
                boolean isLordAtCastleTop = isLord && atCastleTop;
                // 3️. 군주가 Castle Top에 없으면 door 이동 처리
                if (!isLordAtCastleTop) {
                    handleDoors(k, isLord);
                }
                // 4️. 군주이고 Castle Top 아니고 Castle InsideCoords에 있을 때 → InsideCoords로 랜덤 텔레포트 (CastleTopDead일 때만)
                if (isLord && !atCastleTop && RobotController.isCastleInsideCoords(this, k.getUid()) && k.isCastleTopDead()) {
                    int[] coords = RobotController.CASTLE_TOP_COORDS[k.getUid()];
                    int minX = coords[0], maxX = coords[1];
                    int minY = coords[2], maxY = coords[3];
                    int mapId = coords[4];
                    int attempts = 20;
                    while (attempts-- > 0) {
                        int locX = Util.random(minX, maxX);
                        int locY = Util.random(minY, maxY);
                        int locHead = this.getHeading();
                        // getMapdynamic 생략하고 통과 가능 여부만 체크
                        if (World.isThroughObject(locX, locY, mapId, locHead) &&
                                !World.isNotMovingTile(locX, locY, mapId)) {
                            toTeleport(locX, locY, mapId, true);
                            return; // 성공 시 종료
                        }
                    }
                    // 랜덤 좌표 실패 시 중앙으로 강제 텔레포트
                    int centerX = (minX + maxX) / 2;
                    int centerY = (minY + maxY) / 2;
                    toTeleport(centerX, centerY, mapId, true);
                    return;
                }
                // 5️. Castle Top Dead & 군주가 Castle Top에 있을 경우 → lord actions 수행
                if (k.isCastleTopDead() && isLordAtCastleTop) {
                    handleLordActions(c, k);
                }
            }
        }
    }
    /**
     * 성의 모든 문을 순회하여 상태를 체크하는 메서드
     */
    private void handleDoors(Kingdom k, boolean isLord) {
        for (KingdomDoor door : k.getListDoor()) {
            if (door != null && door.getNpc() != null && !RobotController.isCastleTopInsideCoords(this, k.getUid())) {
                checkDoor(door, k, isLord);
            }
        }
    }
    /**
     * ✅ 성문의 파괴 여부를 체크하고, 처음 파괴된 경우에만 멘트를 실행
     * - 여러 로봇이 동시에 성문을 체크할 때 충돌 방지
     */
    private synchronized void checkAndAnnounceDoorDestruction(KingdomDoor door, Kingdom kingdom) {
        String doorName = door.getNpc().getName();
        if (kingdom.isFirstDoorDestruction(doorName)) {
            // 로봇 멘트 실행
            RobotController.getRandomMentAndChat(Lineage.AI_OUTDOOR_MENT, this, door, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_OUTDOOR_MENT_DELAY);
          
            // 성문 파괴 기록
            kingdom.markDoorAsDestroyed(doorName);
        }
    }
    /**
     * ✅ 모든 성문을 체크하고, 공성 진행에 따른 이동을 조정하는 메서드 - `target` 변경이 발생하므로 동기화 필요
     */
    private void checkDoor(KingdomDoor door, Kingdom k, boolean isLord) {
        if (door.isDead() && KingdomController.isKingdomLocation(this, k.getUid())) {
            checkAndAnnounceDoorDestruction(door, k);
        }
        synchronized (this) {
            if (!door.isDead() || !KingdomController.isKingdomLocation(this, k.getUid())) {
                target = door;
                return;
            }
        }
        String doorName = door.getNpc().getName();
        int kingdomId = k.getUid();
        switch (kingdomId) {
            case 1: // 켄트성
                if (k.getName().equalsIgnoreCase("켄트성") && doorName.equalsIgnoreCase("[켄트] 외성문 7시") &&
                        RobotController.isKingdomOutDoor04Location(this, kingdomId)) {
                    moveToCastle(kingdomId, RobotController.CASTLE_OUTSIDE_04_COORDS, isLord);
                } else if (!RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                    moveToCastle(kingdomId, RobotController.CASTLE_TOP_INSIDE_COORDS, isLord);
                }
                break;
            case 2: // 오크 요새
                if (k.getName().equalsIgnoreCase("오크 요새") && doorName.equalsIgnoreCase("[오크성] 외성문 4시") &&
                        !RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                    moveToCastle(kingdomId, RobotController.CASTLE_TOP_INSIDE_COORDS, isLord);
                }
                break;
            case 3: // 윈다우드
                if (k.getName().equalsIgnoreCase("윈다우드") && doorName.equalsIgnoreCase("[윈다우드] 외성문 7시") &&
                        RobotController.isKingdomOutDoor04Location(this, kingdomId)) {
                    moveToCastle(kingdomId, RobotController.CASTLE_TOP_INSIDE_COORDS, false);
                }
                break;
            case 4: // 기란성
                if (k.getName().equalsIgnoreCase("기란 성")) {
                    if (doorName.equalsIgnoreCase("[기란성] 외성문 4시 외부") && RobotController.isKingdomOutDoor04Location(this, kingdomId)) {
                        moveToCastle(kingdomId, RobotController.CASTLE_OUTSIDE_04_COORDS, isLord);
                    } else if (doorName.equalsIgnoreCase("[기란성] 외성문 8시 외부") && RobotController.isKingdomOutDoor08Location(this, kingdomId)) {
                        moveToCastle(kingdomId, RobotController.CASTLE_OUTSIDE_08_COORDS, isLord);
                    } else if ((doorName.equalsIgnoreCase("[기란성] 외성문 4시 내부") || doorName.equalsIgnoreCase("[기란성] 외성문 8시 내부")) &&
                            !RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                        moveToCastle(kingdomId, RobotController.CASTLE_TOP_INSIDE_COORDS, isLord);
                    }
                }
                break;
            case 5: // 하이네성
                if (k.getName().equalsIgnoreCase("하이네 성")) {
                    int entryIndex = -1;
                    // 1️. 성 내부인지 확인 (전체 내부가 아님 → 외성문 쪽일 경우)
                    if (!RobotController.isCastleInsideCoords(this, kingdomId)) {
                        // 1-1. 외성문 이름에 따라 Entry Zone 인덱스 결정
                        if (doorName.equalsIgnoreCase("[하이네] 외성문 5시")) {
                            entryIndex = 0;
                        } else if (doorName.equalsIgnoreCase("[하이네] 외성문 11시")) {
                            entryIndex = 1;
                        }
                        // 1-2. Entry Zone으로 이동 (아직 Entry Zone에 안 들어갔으면)
                        if (entryIndex != -1 && !RobotController.isHeineEntryZone(this, entryIndex)) {
                            moveToCastle(entryIndex, RobotController.KINGDOM_HEINE_ENTRY_ZONES, true);
                            return; // 이동했으니 이후 로직 종료
                        }
                        // 1-3. Entry Zone에 이미 들어와 있으면 Escape Zone으로 텔레포트
                        if (entryIndex != -1 && RobotController.isHeineEntryZone(this, entryIndex)) {
                            teleportToEscape();
                            return; // 텔레포트 했으니 이후 로직 종료
                        }
                    }
                    // 2️. 성 내부지만 Castle Top 내부가 아닐 경우 → Castle Top으로 이동
                    if (!RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                        moveToCastle(kingdomId, RobotController.CASTLE_TOP_INSIDE_COORDS, true);
                        return; // 이동 후 종료
                    }
                  
                    // 3️. Castle Top 내부에 있으면 아무것도 하지 않음
                }
                break;
        }
    }
    private void teleportToEscape() {
        int attempts = 20;
        while (attempts-- > 0) {
            int locX = Util.random(RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][0],
                                   RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][1]);
            int locY = Util.random(RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][2],
                                   RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][3]);
            int locMap = RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][4];
            int locHead = this.getHeading();
            // 이동 가능한 좌표인지 체크
            if (World.getMapdynamic(locX, locY, locMap) == 0 &&
                World.isThroughObject(locX, locY, locMap, locHead) &&
                !World.isNotMovingTile(locX, locY, locMap)) {
                toTeleport(locX, locY, locMap, true);
                break;
            }
        }
    }

    /**
     * [전쟁 서사시: 에피소드 5 & 6] 점령의 정석 및 철벽 수성 컨트롤러
     * - 공성 군주: 선제적 변신/무기 해제 후 도발을 무시하고 강제 점령
     * - 수성 클랜: 왕관 주변 8방향 밀착 가드 및 각개격파
     * - 공성 클랜: 군주 호위 및 적진 돌파
     */
    private synchronized void handleLordActions(Clan c, Kingdom k) {
        // 왕관(면류관)의 좌표 추출
        int crownX = RobotController.KINGDOM_CROWN_COORDS[k.getUid()][0];
        int crownY = RobotController.KINGDOM_CROWN_COORDS[k.getUid()][2];
        int crownMap = RobotController.KINGDOM_CROWN_COORDS[k.getUid()][4];
        // 1. 전운의 고조: 아직 왕관 맵이 아니라면 즉시 텔레포트 진입
        if (getMap() != crownMap) {
            // 무한 텔레포트 루프 방지 (4초 쿨타임)
            if (this.teleportTime < System.currentTimeMillis()) {
                toTeleport(crownX, crownY, crownMap, true);
                this.teleportTime = System.currentTimeMillis() + 4000;
            }
            return;
        }
        boolean isLord = (c != null && c.getLord() != null && c.getLord().equalsIgnoreCase(getName()));
        boolean isDefense = (k.getClanId() != 0 && k.getClanId() == getClanId());
        if (isLord && !isDefense) {
            // 👑 [에피소드 5] 군주의 강제 점령 루틴 (공격 진영)
            int distToCrown = Util.getDistance(this.getX(), this.getY(), crownX, crownY);
           
            if (distToCrown > 1) {
                // 왕관에 닿기 전: 주변 전투를 철저히 무시하고 왕관을 향해 진격 (PC 무시 이동 활성화)
                moveToCastle(k.getUid(), RobotController.KINGDOM_CROWN_COORDS, true);
            } else {
                // 찰나의 순간: 1칸 이내 도달 시 틱(Tick) 단위로 해제 및 점령 시도
               
                // Step A: 변신 선제적 해제
                if (getGfx() != getClassGfx()) {
                    toPolyRemove();
                    return; // 한 틱 소모 후 대기
                }
               
                // Step B: 무기 선제적 해제
                ItemInstance weaponItem = getInventory().getSlot(Lineage.SLOT_WEAPON);
                if (weaponItem != null) {
                    weaponItem.toClick(this, null);
                    return; // 무기 벗고 한 틱 대기
                }
               
                // Step C: 타겟 고정 및 점령 (맨손 확인됨)
                for (object o : getInsideList()) {
                    if (o instanceof KingdomCrown) {
                        synchronized (this) {
                            // 어떠한 도발에도 흔들리지 않도록 타겟을 왕관으로만 고정
                            this.target = o;
                            this.currentAttackTarget = o;
                        }
                        o.toClick(this, null); // 왕관 쟁취!
                        break;
                    }
                }
            }
        } else {
            // 🛡️ [에피소드 6 & 4] 클랜원들의 병과별 전술 및 진형 유지
           
            // 버프 및 지원 즉각 발동 (기존 코드 누락 방지)
            if (this.classBehavior != null) {
                long beforeMagic = this.delay_magic;
                this.classBehavior.executeBuffAndSupport(this);
                if (this.delay_magic > beforeMagic) return; // 힐/버프를 줬다면 딜레이 동안 멈춤
            }
            if (isDefense) {
                // 🏰 [철벽 수성] 왕관 주변 8방향 밀착 가드 배치
                long myOffset = Math.abs(this.getObjectId()) % 8; // ID별 고유 8방향 할당
                int defX = crownX + Util.getXY((int)myOffset, true);
                int defY = crownY + Util.getXY((int)myOffset, false);
               
                int distToDef = Util.getDistance(this.getX(), this.getY(), defX, defY);
               
                if (distToDef > 0) {
                    // 내 방어 위치로 이동 시도 (타인이 서 있으면 비집기)
                    if (isMovableTile(defX, defY, (int)myOffset, crownMap, false)) {
                        toMoving(this, defX, defY, 0, true, false);
                    } else {
                        // 자리가 꽉 찼으면 외곽 경비(각개격파) 모드로 전환하여 적 탐색
                        if (target == null && currentAttackTarget == null) findTarget();
                    }
                } else {
                    // 방어 진형 구축 완료: 제자리에서 다가오는 적 요격
                    if (target == null && currentAttackTarget == null) findTarget();
                }
            } else {
                // ⚔️ [진격의 대형] 공성 진영: 군주 호위 및 적진 분쇄
                if (this.target == null && this.currentAttackTarget == null) {
                    // 칠 적이 없으면 군주를 따라 왕관 쪽으로 압박 진격
                    moveToCastle(k.getUid(), RobotController.KINGDOM_CROWN_COORDS, true);
                } else {
                    // 적이 있으면 타격 (toAiAttack으로 위임됨)
                }
            }
        }
    }
    /**
     * [전쟁 서사시: 에피소드 3] 진격의 대형 및 지능형 우회 컨트롤러
     * - 11x11 (121타일) 부채꼴/원형 진형 구축
     * - ID 기반 지정석 할당으로 완벽한 겹침 방지 (물리적 충돌 회피)
     * - 정면 돌파 및 8방향 지능형 우회로 탐색
     */
    private synchronized void moveToCastle(int kingdomIndex, int[][] castleCoords, boolean ignorePC) {
        int x1 = castleCoords[kingdomIndex][0];
        int x2 = castleCoords[kingdomIndex][1];
        int y1 = castleCoords[kingdomIndex][2];
        int y2 = castleCoords[kingdomIndex][3];
        int map = castleCoords[kingdomIndex][4];
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
       
        int targetX = centerX;
        int targetY = centerY;
       
        // 🛡️ [진격의 대형] 성 내부에 진입했거나 거점에 모일 때 11x11(121칸) 고유 자리 할당
        boolean isInsideCastle = RobotController.isCastleInsideCoords(this, getWarCastleUid());
       
        // 군주(ignorePC=true)가 아니면 일반 병력들은 진형을 넓게 펼치도록 설정
        if (!ignorePC) {
            List<int[]> spreadPositions = new ArrayList<>();
            // 11x11 그리드 (총 121자리) 생성
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    spreadPositions.add(new int[]{centerX + dx, centerY + dy});
                }
            }
           
            // 로봇의 고유 Object ID를 활용하여 자신만의 '지정석' 번호 발급
            long objectId = this.getObjectId();
            long offsetId = objectId - 1900000L; // 로봇 기본 ID 대역 오프셋
            int uidIndex = (int) (Math.abs((offsetId * 31) ^ Util.random(0,999)) 
                    % spreadPositions.size());
           
            int[] mySeat = spreadPositions.get(uidIndex);
            targetX = mySeat[0];
            targetY = mySeat[1];
            
            int attempt = spreadPositions.size();
            while (attempt-- > 0) {
                boolean occupied = false;
                // 다른 로봇이 이미 해당 좌표를 목표로 삼았는지 검사
                for (RobotInstance r : World.getRobotList()) {
                    if (r != this && r.getX() == targetX && r.getY() == targetY) {
                        occupied = true;
                        break;
                    }
                }
                // 실제 맵에서 이동 가능 여부 검사
                if (!occupied && isMovableTile(targetX, targetY,
                        Util.calcheading(this.x, this.y, targetX, targetY),
                        map, ignorePC)) {
                    break;
                }
                // 다른 좌표 재선정
                uidIndex = (uidIndex + 1) % spreadPositions.size();
                int[] nextSeat = spreadPositions.get(uidIndex);
                targetX = nextSeat[0];
                targetY = nextSeat[1];
            }
        }
       
        // 틱(Tick) 딜레이 컨트롤: 0.3초 이하의 중복 이동 명령 방지 (인간다운 움직임)
        long now = System.currentTimeMillis();
        if (now - lastMoveAttemptTime < 300) return;
        lastMoveAttemptTime = now;
       
        // 목표를 향해 시선 고정 (2초마다 자연스럽게 갱신)
        int headingToTarget = Util.calcheading(this.x, this.y, targetX, targetY);
        if (now - lastDirectionSetTime > 2000) {
            setHeading(headingToTarget);
            lastDirectionSetTime = now;
        }
       
        // 이미 내 지정석에 도달했다면 멈춤 (완벽한 진형 유지)
        if (this.x == targetX && this.y == targetY) {
            // 주변 1칸 내 3명 이상이면 살짝 분산
            int nearby = 0;
            for (RobotInstance r : World.getRobotList()) {
                if (r != this && Util.getDistance(this, r) <= 1)
                    nearby++;
            }
            if (nearby < 3) return;
            targetX += Util.random(-1,1);
            targetY += Util.random(-1,1);
        }
        
        if (this.x == lastMoveX && this.y == lastMoveY) {
            moveFailCount++;
        } else {
            moveFailCount = 0;
        }
        lastMoveX = this.x;
        lastMoveY = this.y;

        if (moveFailCount >= 5) {
            targetX += Util.random(-2,2);
            targetY += Util.random(-2,2);
            moveFailCount = 0;
        }
        
        // 🚀 [지능형 우회 알고리즘] 서버 원본 엔진의 최고 효율 경로 탐색 (부비부비 렉 원천 차단)
        // 1순위: 내 지정석을 향해 장애물이 없다면 최단거리 직진
        if (isMovableTile(targetX, targetY, headingToTarget, map, ignorePC)) {
            if (toMoving(this, targetX, targetY, 0, true, ignorePC)) return;
        }
       
        // 2순위: 시선 방향 바로 앞 1칸이 비어있다면 밀고 들어가기
        int nextX = Util.getXY(getHeading(), true) + this.x;
        int nextY = Util.getXY(getHeading(), false) + this.y;
        if (isMovableTile(nextX, nextY, getHeading(), map, ignorePC)) {
            if (toMoving(this, nextX, nextY, 0, true, ignorePC)) return;
        }
       
        // 3순위: 정면이 꽉 막혔다면, 8방향 중 '목표와 가장 가까워지는 빈 공간'을 계산하여 스며들기 (비집기 전술)
        List<int[]> headingCandidates = new ArrayList<>();
        for (int h = 0; h < 8; h++) {
            int tx = Util.getXY(h, true) + this.x;
            int ty = Util.getXY(h, false) + this.y;
           
            // 해당 타일이 물리적으로 비어있는지(Mapdynamic==0) 원본 엔진으로 철저히 검증
            if (isMovableTile(tx, ty, h, map, ignorePC)) {
                int dist = Math.abs(tx - targetX) + Math.abs(ty - targetY);
                headingCandidates.add(new int[]{tx, ty, h, dist});
            }
        }
       
        // 목표지점과 가장 가까운 방향 순으로 정렬 후 이동 시도
        headingCandidates.sort(Comparator.comparingInt(a -> a[3]));
        for (int[] cand : headingCandidates) {
            setHeading(cand[2]); // 우회하는 방향으로 고개 돌리기
            if (toMoving(this, cand[0], cand[1], 0, true, ignorePC)) return;
        }
       
        // 4순위: 8방향마저 여의치 않으면, 주변 1칸 범위 내에서 무조건 빈 곳으로 한 칸 회피 (완전 갇힘 방지)
        List<int[]> candidates = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = this.x + dx;
                int ty = this.y + dy;
                int h = Util.calcheading(this.x, this.y, tx, ty);
                if (isMovableTile(tx, ty, h, map, ignorePC)) {
                    int dist = Math.abs(tx - targetX) + Math.abs(ty - targetY);
                    candidates.add(new int[]{tx, ty, dist});
                }
            }
        }
        candidates.sort(Comparator.comparingInt(a -> a[2]));
        for (int[] tile : candidates) {
            if (toMoving(this, tile[0], tile[1], 0, true, ignorePC)) return;
        }
    }

    /**
     * AI의 공격 동작 처리
     */
    @Override
    protected void toAiAttack(long time) {
        try {
            // 대기 상태에서는 행동하지 않음 (오픈 대기 중)
            if (Lineage.open_wait && pcrobot_mode != PCROBOT_MODE.Cracker && isWait())
                return;
            // 사냥/PvP/공성 모드에서는 회복/버프 포션 사용
            handlePotions();
            // currentAttackTarget 유효성 검사
            object o = checkTargetValidity(currentAttackTarget);
            if (o == null) {
                currentAttackTarget = null;
                if (pcrobot_mode != PCROBOT_MODE.Cracker) {
                    randomTeleport();
                }
                return;
            }
            // 같은 혈맹이면 공격하지 않음
            if (getClanId() > 0 && getClanId() == o.getClanId() && !(o instanceof Doppelganger)) {
                clearTarget();
                return;
            }
            // 타겟 상태가 비정상이라면 리셋
            if (shouldResetTarget(o)) {
                clearTarget();
                return;
            }
            // 수호 NPC 근처라면 귀환
            if ((o instanceof PcInstance && !(o instanceof Pk1RobotInstance)) && !isWarCastle) {
                for (object oo : getInsideList(true)) {
                    if (oo instanceof GuardInstance && (getClanId() == 0 || getClanId() != oo.getClanId())) {
                        goToHome(true);
                        clearTarget();
                        return;
                    }
                }
            }
            // 인비저 상태 감지 시 디텍션 마법 사용
            if (o.isInvis() && Util.random(0, 100) <= 30) {
                toSender(S_ObjectAction.clone(BasePacketPooling.getPool(S_ObjectAction.class), this,
                            Lineage.GFX_MODE_SPELL_NO_DIRECTION), true);
                Detection.onBuff(this, SkillDatabase.find(2, 4));
                ai_time = SpriteFrameDatabase.getGfxFrameTime(
                    this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION
                );
                return;
            }
            // 스킬 공격 시도
            boolean magicUsed = toSkillAttack(o);
            // 공격 사거리 확인 (활 착용 여부)
            boolean bow = getInventory().활장착여부();
            int atkRange = bow ? 8 : 1;
            // 공격 조건 만족 시
            if (Util.isDistance(this, o, atkRange) && Util.isAreaAttack(this, o) && Util.isAreaAttack(o, this)) {
                // 물리 공격 타이밍일 때만 수행
                if (!magicUsed && (AttackController.isAttackTime(this, getGfxMode() + Lineage.GFX_MODE_ATTACK, false)
                    || AttackController.isMagicTime(this, getCurrentSkillMotion()))) {
                    ai_time = (int) (SpriteFrameDatabase.getSpeedCheckGfxFrameTime(
                            this, getGfx(), getGfxMode() + Lineage.GFX_MODE_ATTACK
                        ) + 40);
                    // 확률적으로 멘트 출력
                    if (Util.random(1, 100) <= Lineage.robot_ment_probability &&
                        (o instanceof PcInstance || o instanceof PcRobotInstance)) {
                        Kingdom k = KingdomController.find(this);
                        if (isWarCastle && KingdomController.isKingdomLocation(this, getWarCastleUid())) {
                            if (k != null && k.isWar() && k.getClanId() == getClanId()) {
                                RobotController.getRandomMentAndChat(Lineage.AI_DEFENSE_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_DEFENSE_MENT_DELAY);
                            } else {
                                RobotController.getRandomMentAndChat(Lineage.AI_SIEGE_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_SIEGE_MENT_DELAY);
                            }
                        } else {
                            RobotController.getRandomMentAndChat(Lineage.AI_ATTACK_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ATTACK_MENT_DELAY);
                        }
                    }
                    // 공격 실행
                    toAttack(o, o.getX(), o.getY(), bow, getGfxMode() + Lineage.GFX_MODE_ATTACK, 0, false);
       
                    // 체력이 낮을 경우 확률적으로 도망
                    if (getHpPercent() <= Lineage.robot_escape_threshold_hp && !isWarCastle || !action.contains("공성")) {
                        if (o instanceof PcInstance && ((PcInstance) o).getHpPercent() > getHpPercent()) {
                            if (Util.random(0, 100) < Lineage.robot_escape_chance) {
                                setAiStatus(Lineage.AI_STATUS_ESCAPE);
                                return;
                            }
                        } else if (o instanceof PcRobotInstance && ((PcRobotInstance) o).getHpPercent() > getHpPercent()) {
                            if (Util.random(0, 100) < Lineage.robot_escape_chance) {
                                setAiStatus(Lineage.AI_STATUS_ESCAPE);
                                return;
                            }
                        }
                    }
                }
            } else {
                // 수성 중이면 움직임 금지
                if (isWarFC) {
                    return; // moveToTarget 호출 안 함
                }
              
                // 이동 실패 시 타겟 초기화
                if (!moveToTarget(o)) {
                    clearTarget();
                }
                // 크래커 모드에서 타겟 없으면 귀환
                if (pcrobot_mode == PCROBOT_MODE.Cracker && currentAttackTarget == null && !isWarCastle) {
                    goToHome(true);
                    clearTarget();
                }
            }
            // 실시간 타겟 체크 (공격 중에도 타겟 재확인)
            if (action.contains("사냥") && Util.random(0, 10) == 0) { // 10% 확률로 타겟 재탐색
                findTarget();
            }
        } catch (Exception e) {
            e.printStackTrace();
            lineage.share.System.printf("[처리 오류] toAiAttack(long time)\r\n : %s\r\n", e.toString());
        }
    }
    private void clearTarget() {
        target = null;
        currentAttackTarget = null;
    }
    private void handlePotions() {
        if (action.contains("사냥") || action.contains("PvP") || action.contains("공성")) {
            toHealingPotion();
            toBuffPotion();
        }
    }
    /**
     * ✅ 특정 대상(o)까지 이동 가능 여부 체크 및 이동
     * - o가 성문이면 성문 앞 1칸까지만 이동 허용
     * - 그 외의 경우, 닫힌 문이 있으면 이동 불가
     */
    private boolean moveToTarget(object o) {
        if (o == null) return false;
        int myX = this.x;
        int myY = this.y;
        int targetX = o.getX();
        int targetY = o.getY();
        // 타겟이 성문일 경우 → 문 앞 1칸까지만 이동 허용
        if (o instanceof KingdomDoor) {
            return moveToDoor((KingdomDoor) o);
        }
        // 일반 대상일 경우 → 닫힌 문이 있으면 이동 차단
        if (!canMoveTo(myX, myY, targetX, targetY)) {
            return false; // 🚫 이동 불가
        }
        return toMoving(this, targetX, targetY, 0, true, false);
    }
    /**
     * ✅ 성문(KingdomDoor) 앞 1칸까지만 이동하는 로직
     */
    private boolean moveToDoor(KingdomDoor door) {
        int doorX = door.getX();
        int doorY = door.getY();
        // 문 앞 1칸 거리 계산
        int heading = Util.calcheading(this.x, this.y, doorX, doorY);
        int moveX = doorX - Util.getXY(heading, true);
        int moveY = doorY - Util.getXY(heading, false);
        // 이동할 위치가 닫힌 문에 의해 막혀있다면 이동 차단
        if (!canMoveTo(this.x, this.y, moveX, moveY)) {
            return false;
        }
        return toMoving(this, moveX, moveY, 0, true, false);
    }
    /**
     * ✅ 특정 위치로 이동 가능 여부 체크
     * 🔹 이동 경로상에 닫힌 성문이 있으면 이동 불가
     */
    private boolean canMoveTo(int fromX, int fromY, int toX, int toY) {
        int heading = Util.calcheading(fromX, fromY, toX, toY);
        int currentX = fromX;
        int currentY = fromY;
        int distance = Util.getDistance(fromX, fromY, toX, toY);
        List<object> objects = new ArrayList<>();
        object tempObj = new object();
        tempObj.setMap(map);
        for (int step = 0; step < distance; step++) {
            currentX += Util.getXY(heading, true);
            currentY += Util.getXY(heading, false);
            tempObj.setX(currentX);
            tempObj.setY(currentY);
            objects.clear();
            World.getLocationList(tempObj, 0, objects);
            for (object obj : objects) {
                if (obj instanceof KingdomDoor) {
                    KingdomDoor door = (KingdomDoor) obj;
                    if (door.isDoorClose() && !door.isDead()) {
                        return false; // 🚫 닫힌 문이 있으면 이동 불가
                    }
                }
            }
        }
        return true; // 이동 가능
    }
    /**
     * ✅ 특정 조건에서 target을 초기화하는 메서드
     * - target이 Spartoi 타입이면서 GfxMode가 28일 경우 초기화
     * - target이 StoneGolem 타입이면서 GfxMode가 4일 경우 초기화
     */
    private boolean shouldResetTarget(object o) {
        if (o instanceof Spartoi && o.getGfxMode() == 28) {
            return true; // Spartoi가 특정 GfxMode일 때 초기화
        }
        if (o instanceof StoneGolem && o.getGfxMode() == 4) {
            return true; // StoneGolem이 특정 GfxMode일 때 초기화
        }
        if (o instanceof Harphy && o.getGfxMode() == 4) {
            return true; // Harphy가 특정 GfxMode일 때 초기화
        }
        return false;
    }
    // 🔹 타겟 유효성 검사 후 유효하면 그대로 반환, 아니면 null 반환
    private object checkTargetValidity(object o) {
        if (o == null || o.isDead() || o.isWorldDelete() || !isAttack(o, false)
            || !Util.isAreaAttack(this, o) || !Util.isAreaAttack(o, this)) {
            return null;
        }
        return o;
    }
    @Override
    public void toAiEscape(long time) {
        super.toAiEscape(time);
        // 전투 중인 적(target)이 있는지 확인
        synchronized (this) { // 🔹 target 접근 시 동기화
            if (currentAttackTarget == null) {
                setAiStatus(Lineage.AI_STATUS_WALK);
                return;
            }
        }
        // target이 존재할 때만 확률로 도망 멘트 실행
        if (Util.random(1, 100) <= Lineage.robot_ment_probability) {
            RobotController.getRandomMentAndChat(Lineage.AI_ESCAPE_MENT, this, currentAttackTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ESCAPE_MENT_DELAY);
        }
        // 도망 방향 설정 (target 반대 방향)
        synchronized (this) { // 🔹 heading 변경 시 동기화
            heading = Util.oppositionHeading(this, currentAttackTarget);
        }
      
        int temp_heading = heading;
        boolean escaped = false; // 도망 성공 여부를 저장할 변수
        do {
            // 이동 좌표 계산
            int x = Util.getXY(heading, true) + this.x;
            int y = Util.getXY(heading, false) + this.y;
            // 이동 가능 여부 체크
            boolean canMove = World.isThroughObject(this.x, this.y, this.map, heading);
            // temp_list 동기화 (공유 데이터 접근)
            synchronized (temp_list) {
                temp_list.clear();
                findInsideList(x, y, temp_list);
                // 해당 좌표에 다른 객체(Character)가 있는지 확인
                boolean hasObstacle = false;
                for (object obj : temp_list) {
                    if (obj instanceof Character) {
                        hasObstacle = true;
                        break;
                    }
                }
                if (canMove && !hasObstacle) {
                    // 이동 가능하면 이동 처리
                    super.toMoving(x, y, heading);
                    escaped = true; // 도망 성공
                    break;
                }
            }
            // 방향을 변경하며 재시도
            synchronized (this) { // 🔹 heading 변경 시 동기화
                heading = (heading + 1) % 8;
            }
            if (temp_heading == heading)
                break; // 모든 방향이 막혀있다면 탈출
        } while (true);
        // 도망 성공 시 target 해제 (공유 데이터 수정)
        synchronized (this) {
            if (escaped) {
                target = null;
                currentAttackTarget = null;
            }
        }
    }
    @Override
    protected void toAiDead(long time) {
        super.toAiDead(time);
        ai_time_temp_1 = 0;
        // 전투 관련 변수 초기화.
        target = targetItem = null;
        tempTarget = null;
        currentAttackTarget = null;
        clearAstarList();
        // 상태 변환
        setAiStatus(Lineage.AI_STATUS_CORPSE);
    }
    @Override
    protected void toAiCorpse(long time) {
        super.toAiCorpse(time);
        if (ai_time_temp_1 == 0)
            ai_time_temp_1 = time;
        // 시체 유지
        if (ai_time_temp_1 + Lineage.ai_robot_corpse_time > time)
            return;
        ai_time_temp_1 = 0;
        // 버프제거
        toReset(true);
        // 시체 제거
        clearList(true);
        World.remove(this);
        // 상태 변환.
        setAiStatus(Lineage.AI_STATUS_SPAWN);
    }
    @Override
    protected void toAiSpawn(long time) {
        super.toAiSpawn(time);
        goToHome(false);
        // 부활 뒷 처리.
        toRevival(this);
        // 상태 변환.
        setAiStatus(Lineage.AI_STATUS_WALK);
    }

    /**
     * ✅ AI 줍기 루틴
     * - 현재 타겟(target)으로 지정된 아이템이 있을 경우 해당 위치로 이동하거나 즉시 줍기 수행
     * - 아이템을 획득하면 타겟을 초기화하고 걷기 상태로 전환, 원래 장소로 이동
     */
    @Override
    protected void toAiPickup(long time) {
        object o = target; // ✅ 현재 설정된 줍기 대상 (타겟 아이템)
        // ✅ 타겟이 없으면 줍기 상태 종료 → 걷기 상태로 복귀
        if (o == null) {
            setAiStatus(Lineage.AI_STATUS_WALK);
            return;
        }
        // ✅ 현재 위치가 아이템 위치와 동일할 경우 → 아이템 줍기 시도
        if (Util.isDistance(this, o, 0)) {
            super.toAiPickup(time); // 부모 클래스 로직 실행
            synchronized (o.sync_pickup) {
                if (!o.isWorldDelete()) {
                    Inventory inv = getInventory();
                    if (inv != null) {
                        inv.toPickup(o, o.getCount()); // ✅ 인벤토리에 아이템 추가
                    }
                }
            }
            // ✅ 아이템 획득 후: 타겟 초기화 & 걷기 상태로 전환
            target = null;
            setAiStatus(Lineage.AI_STATUS_WALK);
        } else {
            // ✅ 아이템과 거리가 있을 경우 → 이동 명령 수행
            ai_time = SpriteFrameDatabase.getGfxFrameTime(this, gfx, gfxMode + Lineage.GFX_MODE_WALK);
            toMoving(o, o.getX(), o.getY(), 0, true, false); // 지정 위치로 이동
        }
    }
    /**
     * ✅ Stay 루틴 (집으로 귀환 후 일정 시간 대기)
     * - 대기 시간: 최소 5초 ~ 최대 30초
     */
    private void toStay(long time) {
        switch (step) {
            case 0:
                // ✅ 1단계: 집으로 이동
                goToHome(false);
                step = 1;
                break;
            case 1:
                // ✅ 2단계: 랜덤 방향 설정 후 대기 진입
                setHeading(Util.random(0, 7));
                step = 2;
                break;
            case 2:
                // ✅ 3단계: 5초 ~ 30초 동안 대기
                if (ai_time_temp_1 == 0)
                    ai_time_temp_1 = time;
                if (ai_time_temp_1 + Util.random(1000 * 5, 1000 * 30) > time)
                    return;
                // 대기 완료 → 초기화 및 모드 전환
                ai_time_temp_1 = 0;
                step = 0;
                // 3% 확률로 Stay 유지, 나머지는 모드 해제
                if (Util.random(1, 100) < 3)
                    pcrobot_mode = PCROBOT_MODE.Stay;
                else
                    pcrobot_mode = PCROBOT_MODE.None;
                break;
        }
    }
    private void toPolymorph() {
        switch (step) {
            case 0:
                if (polyTime == 0)
                    polyTime = System.currentTimeMillis() + (1000 * Util.random(1, 5));
          
                if (polyTime > 0 && polyTime <= System.currentTimeMillis())
                    step = 1;
                break;
          
            case 1:
                ItemInstance polyScroll = getInventory().find(ScrollPolymorph.class);
                ItemInstance mythicBook = getInventory().findDbNameId(6492); // 무한 신화 변신 북
                boolean hasPolyScroll = polyScroll != null && polyScroll.getCount() > 0;
                boolean hasMythicBook = mythicBook != null && mythicBook.getCount() > 0;
                boolean useMythicPoly = getMythicPoly();
                boolean usePolyScroll = !useMythicPoly;
          
                if (getRandomPoly()) {
                    if (hasPolyScroll && hasMythicBook) {
                        useMythicPoly = Util.random(0, 1) == 0;
                        usePolyScroll = !useMythicPoly;
                    } else if (hasMythicBook) {
                        useMythicPoly = true;
                        usePolyScroll = false;
                    } else if (hasPolyScroll) {
                        useMythicPoly = false;
                        usePolyScroll = true;
                    }
                }
          
                if (useMythicPoly && !hasMythicBook) {
                    // 신화 변신을 시도했으나 신화 변신 아이템이 없을 경우, 일반 변신 시도
                    useMythicPoly = false;
                    usePolyScroll = hasPolyScroll;
                } else if (usePolyScroll && !hasPolyScroll) {
                    // 일반 변신을 시도했으나 변신 주문서가 없을 경우, 신화 변신 시도
                    usePolyScroll = false;
                    useMythicPoly = hasMythicBook;
                }
          
                if (!useMythicPoly && !usePolyScroll) {
                    // 변신할 수 있는 아이템이 없음
                    // System.println("변신할 수 있는 아이템이 없습니다.");
                    step = 0;
                    polyTime = 0;
                    pcrobot_mode = PCROBOT_MODE.None;
                    return;
                }
          
                if (usePolyScroll) {
                    // 변신 주문서 이용
                    Poly p = PolyDatabase.getName(getPolymorph());
                    if (p != null && p.getMinLevel() <= getLevel()) {
                        PolyDatabase.toEquipped(this, p);
                        setGfx(p.getGfxId());
                        if (Lineage.is_weapon_speed) {
                            if (getInventory().getSlot(Lineage.SLOT_WEAPON) != null && SpriteFrameDatabase.findGfxMode(getGfx(), getGfxMode() + Lineage.GFX_MODE_ATTACK))
                                setGfxMode(getGfxMode());
                            else
                                setGfxMode(getGfxMode());
                        } else {
                            setGfxMode(getGfxMode());
                        }
                      
                        // 버프 등록
                        BuffController.append(this, ShapeChange.clone(BuffController.getPool(ShapeChange.class), SkillDatabase.find(208), 7200));
                        toSender(S_ObjectPoly.clone(BasePacketPooling.getPool(S_ObjectPoly.class), this), true);
                      
                        // 주문서 사용량 감소 ("무한"이 포함되지 않은 경우만)
                        if (!polyScroll.getItem().getName().contains("무한")) {
                            getInventory().count(polyScroll, polyScroll.getCount() - 1, false);
                        }
                    }
                } else if (useMythicPoly) {
                    // 무한 신화 변신 북 이용
                    Poly p = PolyDatabase.getName(getRankPolyName());
                    if (p != null && getGfx() != p.getGfxId()) {
                        mythicBook.toClick(this, null);
                    }
                }
              
                // 초기화
                step = 0;
                polyTime = 0;
                pcrobot_mode = PCROBOT_MODE.None;
                break;
        }
    }
   
    protected void toScrollPolymorph() {
        switch (step++) {
            case 0:
                // 1단계: 안전하게 마을로 귀환
                goToHome(false);
                break;
            case 1:
                // 2단계: 로봇 컨트롤러를 통해 부족한 변신 주문서 강제 지급
                // (서버 설정에 따라 "무한 변신 주문서" 혹은 일반 주문서 지급)
                RobotController.getScrollPolymorph(this);
                break;
            case 2:
                // 3단계: 정비 완료 후 모드 초기화 및 단계 리셋
                step = 0;
                pcrobot_mode = PCROBOT_MODE.None;
                break;
        }
    }
    protected void toBadPolymorph() {
        switch(step++) {
            case 0:
                // 마을로 이동.
                goToHome(true);
                break;
            case 1:
                // 변신 해제
                ServerBasePacket sbp = (ServerBasePacket)ServerBasePacket.clone(BasePacketPooling.getPool(ServerBasePacket.class), null);
                sbp.writeC(0); // opcode
                sbp.writeC(0); // 해제
                byte[] data = sbp.getBytes();
                BasePacketPooling.setPool(sbp);
                BasePacket bp = ClientBasePacket.clone(BasePacketPooling.getPool(ClientBasePacket.class), data, data.length);
                // 처리 요청.
                getInventory().find(ScrollPolymorph.class).toClick(this, (ClientBasePacket)bp);
                // 메모리 재사용.
                BasePacketPooling.setPool(bp);
                // 초기화.
                step = 0;
                // 기본 모드로 변경.
                pcrobot_mode = PCROBOT_MODE.None;
                break;
        }
    }
    public void toPolyRemove() {
        BuffController.remove(this, ShapeChange.class);
        setGfx(this.getClassGfx());
        if (getInventory() != null && getInventory().getSlot(Lineage.SLOT_WEAPON) != null)
            setGfxMode(this.getClassGfxMode() + getInventory().getSlot(Lineage.SLOT_WEAPON).getItem().getGfxMode());
        else
            setGfxMode(this.getClassGfxMode());
        this.toSender(S_ObjectPoly.clone(BasePacketPooling.getPool(S_ObjectPoly.class), this), true);
    }
    protected void toInventoryHeavy() {
        switch (step++) {
            case 0:
                // 마을로 이동.
                goToHome(false);
                break;
            case 1:
                // 인벤에 아이템 삭제.
                for (ItemInstance ii : getInventory().getList()) {
                    // 아데나는 무시.
                    if (ii.getItem().getNameIdNumber() == 4)
                        continue;
                    // 착용중인 아이템 무시.
                    if (ii.isEquipped())
                        continue;
                    // 그 외엔 다 제거.
                    getInventory().remove(ii, false);
                }
                break;
            case 2:
                // 초기화.
                step = 0;
                // 기본 모드로 변경.
                pcrobot_mode = PCROBOT_MODE.None;
                break;
        }
    }

    /**
     * 공격자 목록에 등록처리 함수.
     *
     * @param o
     */
    public void addAttackList(object o) {
        if (!isDead() && !o.isDead() && o.getObjectId() != getObjectId()) {
            if (getClanId() > 0 && o.getClanId() > 0 && getClanId() != o.getClanId())
                // 공격목록에 추가.
                appendAttackList(o);
            else if (getClanId() == 0 || o.getClanId() == 0)
                // 공격목록에 추가.
                appendAttackList(o);
        }
    }
    /**
     * 해당객체를 공격해도 되는지 분석하는 함수.
     *
     * @param o
     * @param walk
     * @return
     */
    private boolean isAttack(object o, boolean walk) {
        Clan c = ClanController.find(this);
        Kingdom k = KingdomController.find(getWarCastleUid());
        if (o == null)
            return false;
        if (o.getGm() > 0)
            return false;
        if (o.isDead())
            return false;
        if (o.isWorldDelete() || o instanceof KingdomDoorman || "$441".equals(o.getName()) || "$2932 $2928".equals(o.getName())
                || (o.getNpc() != null && o.getNpc().getName().equalsIgnoreCase("[기란성] 외성문 2시 내부"))) {
            return false;
        }
        if (o.isBuffAbsoluteBarrier()) {
            if ((o instanceof PcInstance || o instanceof PcRobotInstance) && Util.random(1, 100) <= Lineage.robot_ment_probability) {
                RobotController.getRandomMentAndChat(Lineage.AI_ABSOLUTE_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ABSOLUTE_MENT_DELAY);
            }
            return false;
        }
        if (!Util.isDistance(this, o, Lineage.SEARCH_WORLD_LOCATION))
            return false;
        if (o instanceof Cracker && action != null && action.equalsIgnoreCase("허수아비 공격"))
            return true;
        if (World.isSafetyZone(getX(), getY(), getMap()) && !(o instanceof MonsterInstance)) {
            return false;
        }
        if (k != null && k.isWar() && !RobotController.isCastleTopInsideCoords(this, k.getUid())
                && k.getClanId() != 4 && k.getClanId() != 5 && k.getName().equalsIgnoreCase("켄트성")) {
            if (o instanceof PcRobotInstance || o instanceof PcInstance) {
                return false;
            }
        }
        if (k != null && k.isWar() && !RobotController.isCastleTopInsideCoords(this, k.getUid())
                && k.getClanId() != 4 && k.getClanId() != 5 && k.getName().equalsIgnoreCase("오크 요새")) {
            if (o instanceof PcRobotInstance || o instanceof PcInstance) {
                return false;
            }
        }
      
        if (o instanceof RobotInstance || o instanceof PcInstance) {
            if (getClanId() > 0 && o.getClanId() > 0 && getClanId() == o.getClanId()) {
                return false;
            } else if (RobotController.isCastleInsideCoords(this, getWarCastleUid())){
                return true;
            }
        }
        if (o instanceof KingdomCrown) {
            if (c != null && !c.getLord().equalsIgnoreCase(getName()))
                return false;
        }
        if (o instanceof TeleportInstance || o instanceof EventInstance || o instanceof InnInstance ||
            o instanceof ShopInstance || o instanceof DwarfInstance || o instanceof PetMasterInstance)
            return false;
        if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
            if (o.isBuffCriminal() || o.getLawful() < Lineage.NEUTRAL)
                return true;
        }
        if (k != null && k.getClanId() != 0 && this.getClanId() != 0 && k.getClanId() == this.getClanId()) {
            if (o instanceof KingdomCastleTop || o instanceof KingdomDoor || o instanceof KingdomCrown) {
                return false;
            }
        } else if (k == null && c != null && c.getLord() != null && c.getLord().equalsIgnoreCase(getName())) {
            if (o instanceof KingdomCrown) {
                return true;
            } else if (o instanceof KingdomDoor) {
                if (!o.getNpc().getName().equalsIgnoreCase("[기란성] 외성문 2시 내부")) {
                    return true;
                }
            }
        }
        if (o instanceof SummonInstance || (o instanceof NpcInstance && !(o instanceof GuardInstance))) {
            return false;
        }
        if (o instanceof ItemInstance || o instanceof BackgroundInstance || o instanceof MagicDollInstance)
            return false;
        if (!(o instanceof MonsterInstance) && getX() == o.getX() && getY() == o.getY() && getMap() == o.getMap())
            return false;
        if (o != null && "$607".equals(o.getName())) {
            return false;
        }
        // ✅ 특정 몬스터가 특정 GfxMode일 경우 공격 불가 처리
        if (shouldResetTarget(o)) {
            return false;
        }
        return true;
    }
    /**
     * A* 보조 접근:
     * - astar=true 시 경로의 '다음 한 칸'으로만 이동 시도
     * - 실패 시 primary를 astarIgnore에 등록하여 잠시 우회
     * - 실제 이동은 항상 toMoving(x,y,h)를 거쳐 쿨다운/검증
     * - 최적화: 휴리스틱 개선 (Euclidean distance 사용), 노드 재사용 풀, 조기 종료 조건 추가
     */
    public boolean toMoving(object primary, final int x, final int y, final int h, final boolean astar, final boolean ignoreObjects) {
        // 이동 가능성 (쿨다운 등) 검사
        if (!RobotMoving.isMoveValid(this, lastMovingTime, x, y)) {
            return false;
        }
        boolean moved = false;
        if (astar) {
            try {
                if (aStar != null) {
                    aStar.cleanTail();
                    // 최적화: 거리가 가까우면 A* 생략하고 직선 이동 시도
                    int dist = Util.getDistance(this.x, this.y, x, y);
                    if (dist > TELEPORT_THRESHOLD_DISTANCE && Util.random(0, 100) < 50) { // 거리 길면 50% 확률 텔레포트
                        if (randomTeleport()) {
                            return true; // 텔레포트 성공 시 이동 종료
                        }
                    }
                    if (dist <= 2 && World.isThroughObject(this.x, this.y, this.map, Util.calcheading(this.x, this.y, x, y))) {
                        toMoving(x, y, Util.calcheading(this.x, this.y, x, y));
                        moved = true;
                    } else {
                        tail = aStar.searchTail(this, x, y, ignoreObjects);
                        if (tail != null) {
                            // 경로 길이 계산 (tail 순회)
                            int pathLength = 0;
                            Node current = tail;
                            while (current != null) {
                                pathLength++;
                                current = current.prev;
                            }
                            if (pathLength > TELEPORT_THRESHOLD_DISTANCE) { // 경로 너무 길면 텔레포트
                                if (randomTeleport()) {
                                    return true;
                                }
                            }
                            // 다음 한 칸 좌표만 추출
                            while (tail != null) {
                                if (tail.x == getX() && tail.y == getY()) break;
                                iPath[0] = tail.x;
                                iPath[1] = tail.y;
                                tail = tail.prev;
                            }
                            toMoving(iPath[0], iPath[1], Util.calcheading(this.x, this.y, iPath[0], iPath[1]));
                            moved = true;
                        } else {
                            // 탐색 실패 시 일시적 우회 등록
                            if (primary != null) {
                                astarIgnore.add(primary);
                            }
                        }
                    }
                } else {
                    tail = null;
                }
            } catch (Exception e) {
                lineage.share.System.printf("[처리 오류] A* 이동 실패: %s\r\n", e.toString());
            }
        } else {
            // 일반 이동 (A* 미사용)
            toMoving(x, y, h);
            moved = true;
        }
        return moved;
    }
    /**
     * 실제 좌표 이동 처리
     * - RobotMoving 쿨다운 검증 포함
     */
    public void toMoving(int x, int y, int h) {
        // RobotMoving 쿨다운 검사
        if (!RobotMoving.isMoveValid(this, lastMovingTime, x, y)) {
            return;
        }
        // 원래의 이동 처리 로직 (슈퍼 클래스 or 패킷 송신)
        super.toMoving(x, y, h);
        // 이동 성공 시 마지막 이동 시간 갱신
        lastMovingTime = System.currentTimeMillis();
    }
    /**
     * 버프 물약 복용
     *
     * @return
     */
    private boolean toBuffPotion() {
        //
        Buff b = BuffController.find(this);
        if (b == null)
            return false;
        // 촐기 복용.
        if (b.find(HastePotionMagic.class) == null) {
            ItemInstance item = getInventory().find(HastePotion.class);
            if (item != null && item.isClick(this)) {
                item.toClick(this, null);
                return true;
            }
        }
        // 용기 복용.
        if ((getClassType() == Lineage.LINEAGE_CLASS_KNIGHT || getClassType() == Lineage.LINEAGE_CLASS_ROYAL) && b.find(Bravery.class) == null) {
            ItemInstance item = getInventory().find(BraveryPotion.class);
            if (item != null && item.isClick(this)) {
                item.toClick(this, null);
                return true;
            }
        }
        // 엘븐와퍼 복용.
        if (getClassType() == Lineage.LINEAGE_CLASS_ELF && b.find(Wafer.class) == null) {
            ItemInstance item = getInventory().find(BraveryPotion.class);
            if (item != null && item.isClick(this)) {
                item.toClick(this, null);
                return true;
            }
        }
        // 홀리워크 사용
        if (getClassType() == Lineage.LINEAGE_CLASS_WIZARD && b.find(HolyWalk.class) == null) {
            ItemInstance item = getInventory().find(BraveryPotion.class);
            if (item != null && item.isClick(this)) {
                item.toClick(this, null);
                return true;
            }
        }
        // 버프 물약 사용
        if (getInventory() != null && getInventory().getSlot(Lineage.SLOT_ARMOR) != null && getInventory().getSlot(Lineage.SLOT_WEAPON) != null) {
            if (b.find(DecreaseWeight.class) == null || b.find(EnchantDexterity.class) == null || b.find(EnchantMighty.class) == null || b.find(BlessWeapon.class) == null) {
                ItemInstance item = getInventory().find(Buff_potion.class);
                if (item != null && item.isClick(this)) {
                    item.toClick(this, null);
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 체력 물약 복용.
     */
    private boolean toHealingPotion() {
        //
        if (getHpPercent() > HEALING_PERCENT)
            return false;
        //
        ItemInstance item = getInventory().find(HealingPotion.class);
        if (item != null && item.isClick(this))
            item.toClick(this, null);
        return true;
    }
    /**
     * 공격 마법.
     * 2018-08-11
     * by connector12@nate.com
     */
    protected boolean toSkillAttack(object o) {
        if (this == null || o == null)
            return false;
        List<Skill> list = SkillController.find(this);
        ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
      
        if (list == null) {
            return false; // 리스트가 null인 경우 스킬 사용 불가
        }
        if (System.currentTimeMillis() < delay_magic) {
            return false; // 지연 시간이 지나지 않은 경우 스킬 사용 불가
        }
        // 현재 마나 비율이 스킬 사용을 위한 최소 비율보다 낮으면 false 반환 || // 30%확률로 스킬 사용 안함
        if (getMpPercent() < USABLE_MP_PERCENT && Util.random(0, 100) <= 30) {
            return false; // 스킬 사용 불가
        }
        if (o.isDead()) {
            return false;
        }
        if (o instanceof KingdomDoor || o instanceof KingdomCrown) {
            return false;
        }
      
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s;
            if (sr == null)
                continue;
            if (sr.getType().equalsIgnoreCase("단일공격마법") == false && sr.getType().equalsIgnoreCase("범위공격마법") == false && sr.getType().equalsIgnoreCase("디버프") == false)
                continue;
            if (sr.getLevel() > getLevel())
                continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) {
                if (weapon == null)
                    continue;
                switch (sr.getWeaponType()) {
                    case "한손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand())
                            continue;
                        break;
                    case "양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand())
                            continue;
                        break;
                    case "한손검&양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword"))
                            continue;
                        break;
                    case "활":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("bow"))
                            continue;
                        break;
                }
            }
            if (!sr.getTarget().equalsIgnoreCase("유저&몬스터")) {
                switch (sr.getTarget()) {
                    case "유저":
                        if (o instanceof MonsterInstance)
                            continue;
                        break;
                    case "몬스터":
                        if (o instanceof PcInstance)
                            continue;
                        break;
                }
            }
            // 대상을 구분하지 않음
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute())
                continue;
            if (sr.getMpConsume() > getNowMp())
                continue;
          
            // 조건에 맞는 스킬 사용
            if (Math.random() < sr.getProbability()) {
                toSkill(s, o);
                return true;
            }
        }
        return false;
    }
    /**
     * 버프스킬 시전처리.
     *
     * @return
     */
    protected boolean toSkillBuff(List<Skill> list) {
        if (list == null)
            return false;
        ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s;
            if (sr.getType().equalsIgnoreCase("버프마법") == false)
                continue;
            if (sr.getLevel() > getLevel())
                continue;
            if (sr.getMpConsume() > getNowMp())
                continue;
            if (sr.getUid() == 43 && BuffController.find(this, SkillDatabase.find(311)) != null)
                continue;
            //
            if (BuffController.find(this, s) != null)
                continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) {
                if (weapon == null)
                    continue;
                switch (sr.getWeaponType()) {
                    case "한손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand())
                            continue;
                        break;
                    case "양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand())
                            continue;
                        break;
                    case "한손검&양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword"))
                            continue;
                        break;
                    case "활":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("bow"))
                            continue;
                        break;
                }
            }
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute())
                continue;
            if (Math.random() < sr.getProbability()) {
                toSkill(s, this);
                return true;
            }
        }
        //
        return false;
    }
    /**
     * 소울 스킬 시전
     *
     * @return
     */
    private boolean toSkillHealMp(List<Skill> list) {
        //
        if (getNowMp() == getTotalMp())
            return false;
        //
        if (list == null)
            return false;
        ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s;
            if (sr.getType().equalsIgnoreCase("mp회복마법") == false)
                continue;
            if (sr.getLevel() > getLevel())
                continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) {
                if (weapon == null)
                    continue;
                switch (sr.getWeaponType()) {
                    case "한손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand())
                            continue;
                        break;
                    case "양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand())
                            continue;
                        break;
                    case "한손검&양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword"))
                            continue;
                        break;
                    case "활":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("bow"))
                            continue;
                        break;
                }
            }
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute())
                continue;
            if (Math.random() < sr.getProbability())
                toSkill(s, this);
            return true;
        }
        return false;
    }
    /**
     * 힐 스킬 시전
     *
     * @return
     */
    protected boolean toSkillHealHp(List<Skill> list) {
        //
        if (getHpPercent() > HEALING_PERCENT)
            return false;
        //
        if (list == null)
            return false;
        ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s;
            if (sr.getType().equalsIgnoreCase("힐") == false)
                continue;
            if (sr.getLevel() > getLevel())
                continue;
            if (sr.getMpConsume() > getNowMp())
                continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) {
                if (weapon == null)
                    continue;
                switch (sr.getWeaponType()) {
                    case "한손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand())
                            continue;
                        break;
                    case "양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand())
                            continue;
                        break;
                    case "한손검&양손검":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword"))
                            continue;
                        break;
                    case "활":
                        if (!weapon.getItem().getType2().equalsIgnoreCase("bow"))
                            continue;
                        break;
                }
            }
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute())
                continue;
            if (Math.random() < sr.getProbability())
                toSkill(s, this);
            return true;
        }
        return false;
    }
    /**
     * 서먼 스킬 시전.
     *
     * @return
     */
    protected boolean toSkillSummon(List<Skill> list) {
        //
        if (list == null)
            return false;
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s;
            if (sr.getType().equalsIgnoreCase("서먼몬스터") == false)
                continue;
            if (sr.getLevel() > getLevel())
                continue;
            if (sr.getMpConsume() > getNowMp())
                continue;
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute())
                continue;
            if (Math.random() < sr.getProbability() && SummonController.isAppend(SummonListDatabase.summon(this, 0), this, getClassType() == Lineage.LINEAGE_CLASS_WIZARD ? TYPE.MONSTER : TYPE.ELEMENTAL)) {
                toSkill(s, this);
                SummonController.find(this).setMode(SummonInstance.SUMMON_MODE.AggressiveMode);
                return true;
            }
        }
        return false;
    }
    
    /**
	 * 서먼한 객체에게 버프를 시전함.
	 *
	 * @return
	 */
	private boolean toBuffSummon() {
		Summon s = SummonController.find(this);
		if (s == null || s.getSize() == 0) return false;
		
		for (object o : s.getList()) {
			Buff b = BuffController.find(o);
			// 헤이스트
			if (b == null || b.find(Haste.class) == null) {
				Skill haste = SkillController.find(this, 6, 2);
				if (haste != null && haste.getMpConsume() <= getNowMp()) {
					toSkill(haste, o);
					return true;
				}
			}
			// 힐
			Character cha = (Character) o;
			if (cha.getHpPercent() <= HEALING_PERCENT) {
				int[][] heal_list = { { 1, 0 }, // 힐
						{ 3, 2 }, // 익스트라 힐
						{ 5, 2 }, // 그레이터 힐
						{ 7, 0 }, // 힐 올
						{ 8, 0 }, // 풀 힐
						{ 20, 5 }, // 네이처스 터치
				};
				for (int[] data : heal_list) {
					Skill heal = SkillController.find(this, data[0], data[1]);
					if (heal != null && heal.getMpConsume() <= getNowMp()) {
						toSkill(heal, o);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * 중복코드 방지용.
	 *
	 * @param s
	 */
	public void toSkill(Skill s, object o) {
		ServerBasePacket sbp = (ServerBasePacket) ServerBasePacket.clone(BasePacketPooling.getPool(ServerBasePacket.class), null);
		sbp.writeC(0); // opcode
		sbp.writeC(s.getSkillLevel() - 1); // level
		sbp.writeC(s.getSkillNumber()); // number
		sbp.writeD(o.getObjectId()); // objId
		sbp.writeH(o.getX()); // x좌표
		sbp.writeH(o.getY()); // y좌표
		byte[] data = sbp.getBytes();
		BasePacketPooling.setPool(sbp);
		BasePacket bp = ClientBasePacket.clone(BasePacketPooling.getPool(ClientBasePacket.class), data, data.length);
		SkillController.toSkill(this, (ClientBasePacket) bp);
		// 메모리 재사용.
		BasePacketPooling.setPool(bp);
	}
	/**
	 * 근처 마을로 귀환.
	 */
	protected void goToHome(boolean isCracker) {
		if (!LocationController.isTeleportVerrYedHoraeZone(this, true))
			return;
		// 이미 마을일경우 무시.
		if (!isCracker && World.isGiranHome(getX(), getY(), getMap()))
			return;
		target = targetItem = null;
		tempTarget = null;
		currentAttackTarget = null; // 💥 귀환 시 초기화
		clearAstarList();
		int[] home = null;
		home = Lineage.getHomeXY();
		setHomeX(home[0]);
		setHomeY(home[1]);
		setHomeMap(home[2]);
		toTeleport(getHomeX(), getHomeY(), getHomeMap(), isDead() == false);
	}
	/**
	 * 클레스별로 변신할 이름 리턴.
	 *
	 * @return
	 */
	private String getPolymorph() {
		RobotPoly rp = null;
		if (RobotController.getPolyList().size() < 1)
			return "";
		for (int i = 0; i < 200; i++) {
			rp = RobotController.getPolyList().get(Util.random(0, RobotController.getPolyList().size() - 1));
			if (rp != null && rp.getPoly().getMinLevel() <= getLevel() && SpriteFrameDatabase.findGfxMode(rp.getPoly().getGfxId(), getGfxMode() + Lineage.GFX_MODE_ATTACK)) {
				switch (rp.getPolyClass()) {
					case "모든클래스":
						return rp.getPoly().getName();
					case "군주":
						if (getClassType() == Lineage.LINEAGE_CLASS_ROYAL)
							return rp.getPoly().getName();
						else
							continue;
					case "기사":
						if (getClassType() == Lineage.LINEAGE_CLASS_KNIGHT)
							return rp.getPoly().getName();
						else
							continue;
					case "요정":
						if (getClassType() == Lineage.LINEAGE_CLASS_ELF)
							return rp.getPoly().getName();
						else
							continue;
					case "마법사":
						if (getClassType() == Lineage.LINEAGE_CLASS_WIZARD)
							return rp.getPoly().getName();
						else
							continue;
					case "군주&기사&마법사":
						if (getClassType() == Lineage.LINEAGE_CLASS_ROYAL || getClassType() == Lineage.LINEAGE_CLASS_KNIGHT || getClassType() == Lineage.LINEAGE_CLASS_WIZARD)
							return rp.getPoly().getName();
						else
							continue;
				}
			}
			continue;
		}
		return "";
	}
	/**
	 * 현재 변신상태가 최적화되지 않은 변신인지 확인하는 메서드.
	 * : 양호하다면 false를 리턴.
	 */
	protected boolean isBadPolymorph() {
		Poly p = PolyDatabase.getPolyName( getPolymorph() );
		return p!=null && getGfx()!=p.getGfxId() && getGfx()!=getClassGfx();
	}
	/**
	 * 화살 장착 메소드.
	 * 2018-08-11
	 * by connector12@nate.com
	 */
	private void setArrow() {
		if (getInventory() != null && getInventory().find(Arrow.class) != null) {
			if (!getInventory().find(Arrow.class).equipped)
				getInventory().find(Arrow.class).toClick(this, null);
		}
	}
	/**
	 * 인벤토리 셋팅 메소드.
	 * 2018-08-11
	 * by connector12@nate.com
	 */
	private void setInventory() {
		if (Lineage.robot_auto_pc && (this.getWeapon_name() != null || RobotController.getWeapon(getClassType()) != null)) {
			if (this.getWeapon_name() != null)
				weapon = ItemDatabase.find(this.getWeapon_name());
			else
				weapon = RobotController.getWeapon(getClassType());
			ItemInstance item = ItemDatabase.newInstance(weapon);
			item.setObjectId(ServerDatabase.nextEtcObjId());
			item.setEnLevel(weaponEn);
			getInventory().append(item, false);
			item.toClick(this, null);
		}
		if (Lineage.robot_auto_pc && this.getDoll_name() != null) {
			doll = ItemDatabase.find(this.getDoll_name());
			ItemInstance item = ItemDatabase.newInstance(doll);
			if (item != null) {
				item.setObjectId(ServerDatabase.nextEtcObjId());
				getInventory().append(item, false);
				item.toClick(this, null);
			}
		}
		if (Lineage.robot_auto_pc) {
			RobotController.getHealingPotion(this);
		}
		if (Lineage.robot_auto_pc) {
			RobotController.getHastePotion(this);
		}
		if (Lineage.robot_auto_pc) {
			RobotController.getBraveryPotion(this);
		}
		if (Lineage.robot_auto_pc) {
			RobotController.getElvenWafer(this);
		}
		if (Lineage.robot_auto_pc) {
			RobotController.getScrollPolymorph(this);
		}
		if (Lineage.robot_auto_pc) {
			RobotController.getArrow(this);
		}
	}
	/**
	 * 서버 오픈대기일 경우 처리.
	 * 2018-08-12
	 * by connector12@nate.com
	 */
	private boolean isWait() {
		goToHome(false);
		if (Util.random(0, 99) < 50) {
			pcrobot_mode = PCROBOT_MODE.Stay;
		} else {
			do {
				// 이동 좌표 추출.
				int x = Util.getXY(getHeading(), true) + getX();
				int y = Util.getXY(getHeading(), false) + getY();
				// 해당 좌표 이동가능한지 체크.
				boolean tail = World.isThroughObject(getX(), getY(), getMap(), getHeading()) && World.isMapdynamic(x, y, map) == false;
				// 타일이 이동가능하고 객체가 방해안하면 이동처리.
				if (tail && Util.random(0, 99) < 5) {
					toMoving(null, x, y, getHeading(), false, false);
				} else {
					if (Util.random(0, 99) < 10)
						setHeading(Util.random(0, 7));
					continue;
				}
			} while (false);
		}
		return true;
	}
	
	/**
	 * 허수아비 공격 또는 마을 대기.
	 * 2018-09-14
	 * by connector12@nate.com
	 */
	private void attackCracker() {
		goToHome(false);
		pcrobot_mode = PCROBOT_MODE.Cracker;
		target = targetItem = null;
		tempTarget = null;
		currentAttackTarget = null;
		clearAstarList();
		boolean isCracker = false;
		for (object cracker : BackgroundDatabase.getCrackerList()) {
			if (target == null) {
				target = cracker;
				isCracker = true;
			}
		}
		if (isCracker)
			setAiStatus(Lineage.AI_STATUS_WALK);
		if (target == null)
			isWait();
	}
	/**
	 * 사냥 가능한 맵 체크.
	 * 2018-09-14
	 * by connector12@nate.com
	 */
	public boolean isPossibleMap() {
	    try {
	        List<Book> list = BookController.find(this);
	        if (list == null || list.isEmpty()) return false;
	       
	        for (Book b : list) {
	            // 현재 맵 번호와 북의 맵 번호가 일치하고, enable=true인지 확인
	            if (b != null && b.getEnable() && b.getMap() == this.getMap() && b.getMinLevel() <= getLevel()) {
	                return true;
	            }
	        }
	    } catch (Exception e) {
	        return false;
	    }
	    return false; // 로봇북에 없는 맵이면 false 반환
	}
	public int countClanMembersNearby() {
	    Clan c = ClanController.find(this);
	Kingdom k = KingdomController.find(this);
	    if (c == null || k==null) {
	        return 0;
	    }
	    if (c.getUid() != k.getClanId()) {
	        return 0;
	    }
	   
	    if (KingdomController.isKingdomLocation(this, k.getUid())) {
	        int count = 0;
	        for (object o : getInsideList()) {
	            if (o instanceof PcInstance) {
	                PcInstance member = (PcInstance) o;
	                if (c.containsMemberList(member.getName())) {
	                    count++;
	                }
	            }
	        }
	        return count;
	    }
	    return 0;
	}
	/**
	 * 혈맹원이 공격을 당하면 호출됨.
	 *
	 * @param pc
	 * : 공격당한 객체
	 * @param cha
	 * : 공격한 객체
	 */
	public void toDamage(Character cha) {

	    if (cha == null || cha.getObjectId() == this.getObjectId() || cha.getGm() > 0) {
	        return;
	    }
	    // ===== 1️⃣ 기존 공격 대상 유지 검사 =====
	    if (currentAttackTarget != null) {
	        // 죽었거나 너무 멀면 해제
	        if (currentAttackTarget.isDead()
	                || Util.getDistance(this, currentAttackTarget) > 15) {

	            currentAttackTarget = null;
	        } else {
	            // 아직 공격 유지 상황이면 교체 금지
	            return;
	        }
	    }

	    // ===== 2️⃣ 새 타겟 설정 조건 =====
	    if (Util.getDistance(this, cha) <= 10) {

	        if (!isTargetOvercrowded(cha)) {
	            setTarget(cha);
	            currentAttackTarget = cha;   // 🔥 반드시 명시적으로 지정
	        }
	    }
	}
	    
	    @Override
	    public void setExp(double exp) {
	        double currentExp = this.getExp();
	        double gainedExp = exp - currentExp;
	        double finalExp = exp;
	        if (gainedExp > 0) {
	            finalExp = currentExp + Math.floor(gainedExp * 0.58);
	        }
	        int oldLevel = this.getLevel();
	        super.setExp(finalExp);
	        if (this.getLevel() > oldLevel) {
	            this.setNowHp(this.getMaxHp());
	            this.setNowMp(this.getMaxMp());
	            try {
	                this.toSender(S_ObjectEffect.clone(BasePacketPooling.getPool(S_ObjectEffect.class), this, 3760));
	            } catch (Exception e) {}
	            toTeleportToAppropriateHuntingGround();
	            saveRobotLevelToDb();
	        }
	    }
	    /**
	     * 로봇의 레벨과 세부 경험치를 데이터베이스(_robot 테이블)에 저장합니다.
	     */
	    private void saveRobotLevelToDb() {
	        Connection con = null;
	        PreparedStatement pstm = null;
	        try {
	            con = DatabaseConnection.getLineage();
	            String sql = "UPDATE _robot SET level=?, exp=?, lawful=? WHERE name=?";
	            pstm = con.prepareStatement(sql);
	            pstm.setInt(1, this.getLevel());
	            pstm.setLong(2, (long) this.getExp());
	            pstm.setInt(3, this.getLawful());
	            pstm.setString(4, this.getName());
	            pstm.executeUpdate();
	        } catch (Exception e) {
	            System.println("로봇 데이터 저장 오류: " + this.getName() + " - " + e.getMessage());
	        } finally {
	            DatabaseConnection.close(con, pstm, null);
	        }
	    }
	   
	    /**
	     * 🌟 [강제 구동 버전] 자율 파티 관리
	     * - 마을 안에서도 파티를 맺도록 제한 해제
	     * - 주변에 있는 '모든 로봇'을 대상으로 초대
	     */
	    private void autoPartyManagement() {
	        if (getPartyId() == 0) {
	            if (!isUpperPercentLevel(15)) return;
	            // [2. 주변 탐색] 확률 30%로 상향
	            if (Util.random(0, 100) < 30) {
	                for (object obj : getInsideList()) {
	                   
	                    if (obj instanceof RobotInstance && obj != this) {
	                        PcInstance target = (PcInstance) obj;
	                       
	                        if (target.getPartyId() == 0) {
	                            if (partyWaitStartTime == 0) partyWaitStartTime = System.currentTimeMillis();
	                            Party party = PartyController.find(this);
	                            if (party == null || party.getList().size() < 4) {
	                                broadcastRobotChat(target.getName() + "님, 사냥 같이 가시죠!");
	                               
	                                PartyController.toParty(this, target);
	                                break;
	                            }
	                        }
	                    }
	                }
	            }
	            if (partyWaitStartTime > 0 && (System.currentTimeMillis() - partyWaitStartTime) > 60000) {
	                partyWaitStartTime = 0;
	               
	                if (LocationController.isTeleportZone(this, true, false)) {
	                    randomTeleport();
	                } else {
	                    toTeleportToAppropriateHuntingGround();
	                }
	            }
	        } else {
	            partyWaitStartTime = 0;
	        }
	    }
	    /**
	     * 🌟 파티원 협동 로직 (리더 따라가기, 타겟 공유 및 위기 감지)
	     */
	    private void handlePartyCollaboration() {
	        Party party = PartyController.find(this);
	        if (party == null) return;
	        PcInstance leader = party.getMaster();
	        if (leader == null || leader == this) return; // 리더 본인은 패스
	        // 1. [위기 감지] 파티원 중 한 명이라도 HP 20% 이하면 즉시 전원 귀환
	        if (this.partyManager != null && this.partyManager.isPartyInDanger(this)) {
	            ChattingController.toChatting(this, "전원 후퇴!", Lineage.CHATTING_MODE_NORMAL);
	           
	            // ✅ 멋지게 멘트 남기고 2초 뒤 전원 귀환
	            setDelayedAction(2000, () -> {
	                if (LocationController.isTeleportZone(this, true, false)) {
	                    int[] home = Lineage.getHomeXY();
	                    toTeleport(home[0], home[1], home[2], true);
	                } else {
	                    goToHome(false);
	                }
	            });
	            return;
	        }
	        // 2. [모드 결정] 및 [모드별 행동 수행]
	        RobotPartyManager.PartyMode mode = RobotPartyManager.PartyMode.NORMAL;
	        if (this.partyManager != null) {
	            mode = this.partyManager.determinePartyMode(this, leader);
	        }
	        switch (mode) {
	            case GATHER:
	                toTeleport(leader.getX(), leader.getY(), leader.getMap(), true);
	                return;
	            case WAIT:
	            case SUPPORT:
	                // ✅ 겹침 방지: 거리가 2칸 이내면 더 이상 리더에게 다가가지 않고 멈춤 (진형 유지)
	                if (Util.getDistance(this, leader) > 2) {
	                    // A*를 써서 리더 좌표를 찍으면 겹치므로, 리더 방향으로 1칸씩만 자연스럽게 이동시킴
	                    int head = Util.calcheading(this, leader.getX(), leader.getY());
	                    int nextX = this.getX() + Util.getXY(head, true);
	                    int nextY = this.getY() + Util.getXY(head, false);
	                   
	                    if (World.isThroughObject(this.getX(), this.getY(), this.getMap(), head)) {
	                        toMoving(this, nextX, nextY, head, false, false);
	                    } else {
	                        // 정면이 막혔으면 좌우로 살짝 빗겨서 이동 (부비부비 렉 방지)
	                        int altHead = (head + Util.random(1, 2) * (Util.random(0, 1) == 0 ? 1 : -1) + 8) % 8;
	                        toMoving(this, this.getX() + Util.getXY(altHead, true), this.getY() + Util.getXY(altHead, false), altHead, false, false);
	                    }
	                }
	               
	                if (mode == RobotPartyManager.PartyMode.WAIT) {
	                    return; // 대기 모드면 여기서 행동 종료 (공격 안 함)
	                }
	                break; // 지원 모드면 아래 타겟 공유 로직으로 흘러감
	            case NORMAL:
	            default:
	                break;
	        }
	        // 4. [타겟 공유 / 점사]
	        object leaderTarget = null;
	        if (leader instanceof PcRobotInstance) {
	            leaderTarget = ((PcRobotInstance) leader).getTarget();
	        } else {
	            leaderTarget = leader.getTarget();
	        }
	        if (leaderTarget != null && isAttack(leaderTarget, true)) {
	            if (this.currentAttackTarget != leaderTarget) {
	                synchronized (this) {
	                    setTarget(leaderTarget);
	                    setAiStatus(Lineage.AI_STATUS_ATTACK);
	                    this.currentAttackTarget = leaderTarget;
	                }
	            }
	        }
	    }
	   
	    /**
	     * ✅ 퍼센트 조절용 리더 자격 검사 (파라미터 추가)
	     */
	    private boolean isUpperPercentLevel(int percent) {
	        List<RobotInstance> allRobots = World.getRobotList();
	        if (allRobots.size() < 2) return true;
	        List<RobotInstance> sortedRobots = new ArrayList<>(allRobots);
	        sortedRobots.sort((r1, r2) -> Integer.compare(r2.getLevel(), r1.getLevel()));
	        int cutoffIndex = Math.max(0, (int) (sortedRobots.size() * (percent / 100.0)) - 1);
	        int thresholdLevel = sortedRobots.get(cutoffIndex).getLevel();
	        return this.getLevel() >= thresholdLevel;
	    }
	    /**
	     * 🌟 맵 인원수 8명 초과 시 다음 층으로 이동하는 로직
	     */
	    private boolean checkAndMoveToNextFloor() {
	        if (System.currentTimeMillis() - lastFloorCheckTime < 10000) return false;
	        lastFloorCheckTime = System.currentTimeMillis();
	        int currentPop = 0;
	        for (PcInstance pc : World.getPcList()) { if (pc.getMap() == this.getMap()) currentPop++; }
	        for (RobotInstance robot : World.getRobotList()) { if (robot.getMap() == this.getMap()) currentPop++; }
	        if (currentPop > 20) {
	            String currentName = getCurrentMapName();
	            if (currentName == null || currentName.isEmpty()) return false;
	            String prefix = currentName.replaceAll("\\s*\\d+층.*", "").trim();
	            int[] nextLoc = getNextFloorFromItemTeleport(prefix, currentName);
	           
	            if (nextLoc != null) {
	                // ✅ 멘트 치고 2초 대기 후 이동
	                ChattingController.toChatting(this, "사람이 너무 많네요. 다음 층으로 이동합니다!", Lineage.CHATTING_MODE_NORMAL);
	               
	                Party party = PartyController.find(this);
	                boolean isLeader = (party != null && party.getMaster() == this);
	                int nextX = nextLoc[0]; int nextY = nextLoc[1]; int nextMap = nextLoc[2];
	                setDelayedAction(2000, () -> {
	                    toTeleport(nextX, nextY, nextMap, true);
	                    if (isLeader) {
	                        for (PcInstance member : party.getList()) {
	                            if (member != this && (member instanceof RobotInstance)) {
	                                member.toTeleport(nextX, nextY, nextMap, true);
	                            }
	                        }
	                    }
	                });
	                return true;
	            }
	        }
	        return false;
	    }
	    /**
	     * ✅ 현재 맵 이름 가져오기 (메모리의 ItemTeleport 전체 목록에서 검색)
	     */
	    private String getCurrentMapName() {
	        List<ItemTeleport> teleportList = ItemTeleportDatabase.getList();
	        if (teleportList != null) {
	            for (ItemTeleport tp : teleportList) {
	                if (tp.getMap() == this.getMap()) {
	                    return tp.getName();
	                }
	            }
	        }
	        return null;
	    }
	    /**
	     * ✅ 현재 맵이 정상적인 던전의 다음 층인지 확인 (마을 강제귀환 방지용)
	     */
	    private boolean isValidDungeonFloor() {
	        String currentMapName = getCurrentMapName();
	        if (currentMapName == null) return false;
	       
	        String prefix = currentMapName.replaceAll("\\s*\\d+층.*", "").trim();
	        List<Book> list = BookController.find(this);
	        if (list != null) {
	            for (Book b : list) {
	                if (b.getLocation() != null && b.getLocation().startsWith(prefix)) {
	                    return true;
	                }
	            }
	        }
	        return false;
	    }
	    /**
	     * ✅ 문자열 대신 '숫자(층수)'를 직접 비교하여 완벽하게 다음 층의 좌표를 찾아냄
	     */
	    private int[] getNextFloorFromItemTeleport(String prefix, String currentName) {
	        List<ItemTeleport> teleportList = ItemTeleportDatabase.getList();
	        if (teleportList == null) return null;
	        int currentFloor = extractFloor(currentName);
	        ItemTeleport nextFloorTp = null;
	        int minNextFloor = Integer.MAX_VALUE;
	        for (ItemTeleport tp : teleportList) {
	            String tpName = tp.getName();
	            if (tpName != null && tpName.startsWith(prefix)) {
	                int tpFloor = extractFloor(tpName);
	               
	                // 현재 층보다 크고, 지금까지 발견된 층 중 가장 작은 층 (예: 1층일때 2층을 정확히 픽)
	                if (tpFloor > currentFloor && tpFloor < minNextFloor) {
	                    minNextFloor = tpFloor;
	                    nextFloorTp = tp;
	                }
	            }
	        }
	        if (nextFloorTp != null) {
	            return new int[]{ nextFloorTp.getX(), nextFloorTp.getY(), nextFloorTp.getMap() };
	        }
	        return null;
	    }
	    /**
	     * ✅ 텍스트에서 층수 숫자만 뽑아내는 보조 메서드
	     */
	    private int extractFloor(String name) {
	        try {
	            String numberOnly = name.replaceAll("[^0-9]", ""); // 숫자 이외의 문자 모두 제거
	            if (numberOnly.length() > 0) {
	                return Integer.parseInt(numberOnly);
	            }
	        } catch (Exception e) {}
	        return 0;
	    }
	   
	    public boolean isInVillage() {
	        return (this.getX() == this.getHomeX() &&
	                this.getY() == this.getHomeY() &&
	                this.getMap() == this.getHomeMap())
	                || World.isGiranHome(getX(), getY(), getMap());
	    }
	   
	    /**
	     * 로봇 스폰 시 직업 및 AI 두뇌를 초기화합니다.
	     */
	    public void initRobotAI() {
	        this.huntingEvaluator = new RobotHuntingEvaluator();
	        this.partyManager = new RobotPartyManager();  // 복원
	        switch (this.getClassType()) {
	            case Lineage.LINEAGE_CLASS_ROYAL:
	                this.classBehavior = new BehaviorPrince();
	                break;
	            case Lineage.LINEAGE_CLASS_KNIGHT:
	                this.classBehavior = new BehaviorKnight();
	                break;
	            case Lineage.LINEAGE_CLASS_ELF:
	                this.classBehavior = new BehaviorElf();
	                break;
	            case Lineage.LINEAGE_CLASS_WIZARD:
	                this.classBehavior = new BehaviorWizard();
	                break;
	            default:
	                this.classBehavior = new BehaviorKnight();
	                break;
	        }
	    }
	   
	    /**
	     * 지정된 시간(ms)만큼 가만히 대기한 후, 특정 행동(텔레포트 등)을 실행합니다.
	     */
	    private void setDelayedAction(long delayMs, Runnable action) {
	        this.delayedActionTime = System.currentTimeMillis() + delayMs;
	        this.delayedAction = action;
	        this.target = null;
	        this.currentAttackTarget = null;
	    }
	    
	    private boolean isTargetOvercrowded(object target) {
	        int attackerCount = 0;
	        for (RobotInstance r : World.getRobotList()) {
	            if (r instanceof PcRobotInstance) {
	                PcRobotInstance robot = (PcRobotInstance) r;
	                if (robot.currentAttackTarget == target)
	                    attackerCount++;
	            }
	        }
	        // 공성은 밀집 허용 증가
	        int limit = (isWarCastle && action.contains("공성")) ? 6 : 3;
	        return attackerCount >= limit;
	    }
	    
	    private boolean isKing() {
	        Clan c = ClanController.find(this);
	        return c != null && c.getLord() != null && c.getLord().equalsIgnoreCase(getName()) 
	               && getClassType() == Lineage.LINEAGE_CLASS_ROYAL;
	    }


	    // 🌟 [추가] 3. 공성 상태 전이 제어 메인 메서드
	    private void handleWarState() {
	        boolean currentWarStatus = toKingdomWarCheck(); // 기존 공성 여부 체크 메서드 활용
	        
	        // [상태 전이] 공성 시작 감지
	        if (currentWarStatus && warState == RobotWarState.IDLE) {
	            warState = RobotWarState.WAR_ASSAULT;
	            setTarget(null);
	            noTargetWalkCount = 0;
	            myCachedFormationSeat = null; // 이전 공성 데이터 초기화
	        } 
	        // [상태 전이] 공성 종료 감지
	        else if (!currentWarStatus && warState != RobotWarState.IDLE && warState != RobotWarState.WAR_END) {
	            warState = RobotWarState.WAR_END;
	        }

	        // [공성 종료 처리] 즉시 사냥터로 복귀
	        if (warState == RobotWarState.WAR_END) {
	            resetAfterWar();
	            return;
	        }

	        // [공성 중 행동 분기] 군주와 일반 병력의 역할을 완벽히 분리
	        if (warState == RobotWarState.WAR_ASSAULT || warState == RobotWarState.WAR_OCCUPY) {
	            if (isKing()) {
	                moveToCrown(); // 군주: 오직 왕관으로 직행
	            } else {
	                moveToWarFormation(); // 일반 병력: 진형 유지 및 집결
	            }
	        }
	    }
	    
	    private void moveToCrown() {
	        int castleUid = getWarCastleUid();
	        if (castleUid == -1) return;
	        
	        // [명세 반영] 하드코딩을 배제하고 배열/컨트롤러에서 동적으로 왕관 좌표 획득
	        // (만약 CastleController가 없다면 기존 RobotController의 상수 배열을 사용합니다)
	        int crownX = RobotController.KINGDOM_CROWN_COORDS[castleUid][0];
	        int crownY = RobotController.KINGDOM_CROWN_COORDS[castleUid][2];
	        int crownMap = RobotController.KINGDOM_CROWN_COORDS[castleUid][4];

	        // 1. 맵이 다르면 왕관 맵으로 즉시 텔레포트 (무한 텔레포트 방지 쿨타임 적용)
	        if (getMap() != crownMap) {
	            if (this.teleportTime < System.currentTimeMillis()) {
	                toTeleport(crownX, crownY, crownMap, true);
	                this.teleportTime = System.currentTimeMillis() + 4000;
	            }
	            return; // 텔레포트 후 다음 틱 대기
	        }

	        // 2. 왕관까지의 거리 계산
	        int distToCrown = Util.getDistance(this.getX(), this.getY(), crownX, crownY);

	        if (distToCrown > 1) {
	            // 🚀 [왕관 직행 로직] 목표에 도달할 때까지 무한 진격
	            // 명세 조건: 일반 타겟팅 무시, 랜덤 텔레포트 금지, 분산 진형 무시
	            setTarget(null);
	            this.currentAttackTarget = null;
	            
	            // 기존의 이동 메서드를 활용하되 ignorePC = true를 주어 겹침을 무시하고 강제 돌파
	            moveToCastle(castleUid, RobotController.KINGDOM_CROWN_COORDS, true);
	        } else {
	            // 👑 [점령 진행 로직] 왕관 1칸 이내 도달 시
	            if (warState != RobotWarState.WAR_OCCUPY) {
	                warState = RobotWarState.WAR_OCCUPY; // 상태 전이: 공격 -> 점령 중
	            }

	            // Step A: 변신 선제적 해제 (틱 분리로 씹힘 방지)
	            if (getGfx() != getClassGfx()) {
	                toPolyRemove();
	                return; 
	            }
	            
	            // Step B: 무기 선제적 해제
	            ItemInstance weaponItem = getInventory().getSlot(Lineage.SLOT_WEAPON);
	            if (weaponItem != null) {
	                weaponItem.toClick(this, null);
	                return; 
	            }
	            
	            // Step C: 타겟 고정 및 찰나의 강제 클릭 (도발 무시)
	            for (object o : getInsideList()) {
	                if (o instanceof KingdomCrown) {
	                    synchronized (this) { 
	                        this.target = o; 
	                        this.currentAttackTarget = o;
	                    }
	                    o.toClick(this, null); // 점령!
	                    break;
	                }
	            }
	        }
	    }
	    
	    private void moveToWarFormation() {
	        int castleUid = getWarCastleUid();
	        if (castleUid == -1) return;
	        
	        lineage.bean.lineage.Kingdom k = lineage.world.controller.KingdomController.find(castleUid);
	        if (k == null) return;

	        // 1. 진격의 기준점 설정 (수호탑 파괴 여부에 따라 유동적 이동)
	        int[] baseCoords;
	        if (k.isCastleTopDead()) {
	            baseCoords = lineage.world.controller.RobotController.KINGDOM_CROWN_COORDS[castleUid];
	        } else {
	            baseCoords = lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[castleUid];
	        }

	        int baseCenterX = (baseCoords[0] + baseCoords[1]) / 2;
	        int baseCenterY = (baseCoords[2] + baseCoords[3]) / 2;
	        int targetMap = baseCoords[4];

	        // 2. [명세 반영] 리스트 매 틱 생성 금지 -> 캐싱 방식(myCachedFormationSeat) 사용
	        // objectId를 기반으로 11x11 배열 내의 고유 좌석(-5 ~ +5) 한 번만 계산 후 캐싱
	        if (myCachedFormationSeat == null) {
	            long offsetId = Math.abs(this.getObjectId() - 1900000L);
	            int dx = (int)(offsetId % 11) - 5;
	            int dy = (int)((offsetId / 11) % 11) - 5;
	            myCachedFormationSeat = new int[]{dx, dy};
	        }

	        // 내 고유 좌석의 최종 목적지 좌표
	        int targetX = baseCenterX + myCachedFormationSeat[0];
	        int targetY = baseCenterY + myCachedFormationSeat[1];

	        // 3. [명세 반영] 겹침(중첩) 방지: 목표 1칸 이내 도달 시 주변 아군 3명 이상이면 1칸 랜덤 분산
	        if (lineage.util.Util.getDistance(this.getX(), this.getY(), targetX, targetY) <= 1) {
	            int nearbyRobots = 0;
	            for (lineage.world.object.object obj : getInsideList()) {
	                if (obj instanceof PcRobotInstance && obj.getObjectId() != this.getObjectId()) {
	                    if (lineage.util.Util.getDistance(this.getX(), this.getY(), obj.getX(), obj.getY()) <= 1) {
	                        nearbyRobots++;
	                    }
	                }
	            }
	            if (nearbyRobots >= 3) {
	                targetX += lineage.util.Util.random(-1, 1);
	                targetY += lineage.util.Util.random(-1, 1);
	            }
	        }

	        // 4. 맵 진입 텔레포트 (무한 루프 방지 쿨타임)
	        if (getMap() != targetMap) {
	            if (this.teleportTime < System.currentTimeMillis()) {
	                lineage.world.controller.RobotController.isKingdomAttLocation(this, true, castleUid);
	                this.teleportTime = System.currentTimeMillis() + 4000;
	            }
	            return;
	        }

	        // 5. 이동 실행 (A* 우회 로직, 군주가 아니므로 ignorePC = false)
	        long now = System.currentTimeMillis();
	        if (now - lastMoveAttemptTime < 300) return;
	        lastMoveAttemptTime = now;
	        
	        int headingToTarget = lineage.util.Util.calcheading(this.getX(), this.getY(), targetX, targetY);
	        if (now - lastDirectionSetTime > 2000) {
	            setHeading(headingToTarget);
	            lastDirectionSetTime = now;
	        }

	        if (this.getX() == targetX && this.getY() == targetY) return; // 자리 도착 시 대기

	        // 이동 1순위: 직진
	        if (isMovableTile(targetX, targetY, headingToTarget, targetMap, false)) {
	            if (toMoving(this, targetX, targetY, 0, true, false)) return;
	        }
	        
	        // 이동 2순위: 8방향 비집기 우회
	        java.util.List<int[]> headingCandidates = new java.util.ArrayList<>();
	        for (int h = 0; h < 8; h++) {
	            int tx = lineage.util.Util.getXY(h, true) + this.getX();
	            int ty = lineage.util.Util.getXY(h, false) + this.getY();
	            if (isMovableTile(tx, ty, h, targetMap, false)) {
	                int dist = Math.abs(tx - targetX) + Math.abs(ty - targetY);
	                headingCandidates.add(new int[]{tx, ty, h, dist});
	            }
	        }
	        headingCandidates.sort(java.util.Comparator.comparingInt(a -> a[3]));
	        for (int[] cand : headingCandidates) {
	            setHeading(cand[2]);
	            if (toMoving(this, cand[0], cand[1], 0, true, false)) return;
	        }
	    }


	    // 🌟 [추가] 6. 공성 종료 후 즉시 사냥 모드 복귀 메서드
	    private void resetAfterWar() {
	        // [명세 반영] 공성 잔재 완벽 초기화
	        setTarget(null);
	        this.currentAttackTarget = null;
	        this.myCachedFormationSeat = null; // 공성 좌표 참조 제거
	        this.noTargetWalkCount = 0;
	        
	        // 사냥터 이동 리스트 호출
	        java.util.List<lineage.bean.lineage.Book> list = lineage.world.controller.BookController.find(this);
	        if (list != null && !list.isEmpty()) {
	            teleportToHuntingGround(list); // 즉시 사냥터 복귀
	        } else {
	            goToHome(false); // 수첩이 없으면 마을 복귀
	        }
	        
	        // 초기화 최종 단계: IDLE 모드로 전이
	        this.warState = RobotWarState.IDLE;
	    }
}
