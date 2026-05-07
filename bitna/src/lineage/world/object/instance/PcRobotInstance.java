package lineage.world.object.instance;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import goldbitna.AttackController;
import goldbitna.robot.Pk1RobotInstance;
import goldbitna.robot.controller.RobotConversationController;
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
import lineage.world.controller.ClanController;
import lineage.world.controller.KingdomController;
import lineage.world.controller.LocationController;
import lineage.world.controller.MagicDollController;
import lineage.world.controller.PartyController;
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
    private int whaleTier = 0;
    private long whaleMonthlyCash = 0;
    private long whaleSinglePurchase = 0;
    private int whaleMeetChance = 0;
    private long whaleChatCooldownMillis = 0;
    private long whaleLastChatTime = 0;
    
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
    private int moveFailCount = 0;
    private long moveFailFirstTime = 0L;
    private long moveFailLastTime = 0L;
    private int moveFailTargetX = Integer.MIN_VALUE;
    private int moveFailTargetY = Integer.MIN_VALUE;
    private int moveFailTargetMap = Integer.MIN_VALUE;
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
    private static final int STUCK_FAIL_COUNT_THRESHOLD = 6;
    private static final long STUCK_FAIL_WINDOW_MS = 4500L;
    private static final int FORCE_RELOCATE_SCAN_RADIUS = 16;
    private static final int CONTINUOUS_MOVE_STEP = 2;
    
    public enum RobotState {
        IDLE,       // 대기 및 탐색 모드
        HUNTING,    // 사냥터 이동 (네비게이션)
        COMBAT,     // 전투 중
        FLEE,       // 도망/회피 중
        TOWN        // 마을 귀환 및 정비
    }

    // 신규 AI 모듈 필드 선언
    private goldbitna.robot.behavior.IRobotClassBehavior classBehavior;
    private goldbitna.robot.evaluate.RobotHuntingEvaluator huntingEvaluator;
    private RobotState currentState = RobotState.IDLE;
    
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
        resetMovementFailure();
        randomPoly = mythicPoly = isReload = isWarCastle = isWarFC = false;
        clearWhaleProfile();
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

    public void setWhaleProfile(int tier, long monthlyCash, long singlePurchase, int meetChance, int cooldownSec) {
        this.whaleTier = Math.max(0, tier);
        this.whaleMonthlyCash = Math.max(0L, monthlyCash);
        this.whaleSinglePurchase = Math.max(0L, singlePurchase);
        this.whaleMeetChance = Math.max(0, Math.min(100, meetChance));
        this.whaleChatCooldownMillis = Math.max(0L, cooldownSec) * 1000L;
    }

    public void clearWhaleProfile() {
        this.whaleTier = 0;
        this.whaleMonthlyCash = 0L;
        this.whaleSinglePurchase = 0L;
        this.whaleMeetChance = 0;
        this.whaleChatCooldownMillis = 0L;
        this.whaleLastChatTime = 0L;
    }

    public int getWhaleTier() {
        return whaleTier;
    }

    public long getWhaleMonthlyCash() {
        return whaleMonthlyCash;
    }

    public long getWhaleSinglePurchase() {
        return whaleSinglePurchase;
    }

    public boolean isWhaleProfileEnabled() {
        return whaleTier > 0;
    }

    private void tryWhaleMent(String triggerType, object target) {
        if (!isWhaleProfileEnabled()) {
            return;
        }

        String event = triggerType == null ? "" : triggerType.trim().toUpperCase();
        long now = System.currentTimeMillis();
        boolean ignoreCooldown = ("KILL".equalsIgnoreCase(event) && RobotController.getWhaleOptionInt("kill_cooldown_ignore", 1) > 0)
            || "SPAWN".equalsIgnoreCase(event);
        if (!ignoreCooldown && whaleChatCooldownMillis > 0 && now - whaleLastChatTime < whaleChatCooldownMillis) {
            return;
        }

        int chance = whaleMeetChance + RobotController.getWhaleOptionInt("chance_global_bonus", 0);
        if ("IDLE".equalsIgnoreCase(event)) {
            int divider = Math.max(1, RobotController.getWhaleOptionInt("idle_divider", 3));
            chance = Math.max(3, chance / divider);
        } else if ("ATTACK".equalsIgnoreCase(event)) {
            chance += RobotController.getWhaleOptionInt("attack_bonus", 10);
        } else if ("DAMAGE".equalsIgnoreCase(event)) {
            chance += RobotController.getWhaleOptionInt("damage_bonus", 8);
        } else if ("KILL".equalsIgnoreCase(event)) {
            chance += RobotController.getWhaleOptionInt("kill_bonus", 25);
        } else if ("SPAWN".equalsIgnoreCase(event)) {
            chance += RobotController.getWhaleOptionInt("spawn_bonus", 50);
        }
        int chanceCap = Math.max(1, RobotController.getWhaleOptionInt("chance_cap", 95));
        chance = Math.max(1, Math.min(chanceCap, chance));

        if (chance <= 0 || Util.random(1, 100) > chance) {
            return;
        }

        String ment = RobotController.getWhaleMent(this, event, target);
        if (ment == null || ment.length() == 0) {
            return;
        }

        broadcastRobotChat(ment);
        whaleLastChatTime = now;
    }

    public synchronized object getTarget() {
        return target;
    }

    public synchronized void setTarget(object newTarget) {
        target = newTarget;
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
            if (Util.random(1, 100) < 20) {
                randomTeleport();
            }
            return;
        }
        object o = (object) cha;
        // 전투 중이 아니고, 공격 가능한 상대일 경우만 setTarget
        if (currentAttackTarget == null && isAttack(o, true)) {
            setTarget(o); // 👉 이 시점에 AI 상태가 WALK 상태에서 ATTACK으로 전환되며 currentAttackTarget이 설정됨
        }
        // 마법 공격 시 확률적으로 멘트를 출력 (전투 중이 아니거나 기존 대상이면 허용)
        if ((cha instanceof PcInstance || cha instanceof PcRobotInstance)
                && (target == null || target == cha)
                && (currentAttackTarget == null || currentAttackTarget == cha)
                && (!isWarCastle || !action.contains("공성"))) {
            if (Util.random(1, 100) <= Lineage.robot_ment_probability && type == Lineage.ATTACK_TYPE_MAGIC) {
                RobotController.getRandomMentAndChat(Lineage.AI_ATTACKED_MENT, this, cha, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ATTACKED_MENT_DELAY);
                tryWhaleMent("DAMAGE", (object) cha);
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
                                member.toDamage(cha); // 👉 재귀 호출로 혈맹 동료에게도 위협 전달
                            }
                        }
                    }
                }
            }
        }
        // 🔹 길찾기 A* 예외 리스트에서 제거
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
        if (teleportTime < System.currentTimeMillis()) {
            if (isPossibleMap()) {
                teleportTime = System.currentTimeMillis() + Util.random(1000, 3000);
                target = targetItem = null;
                tempTarget = null;
                currentAttackTarget = null;
                clearAstarList();
                ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                setAiStatus(Lineage.AI_STATUS_WALK);
                if (!LocationController.isTeleportZone(this, true, false) || (getMap() == 4 && !World.isSafetyZone(getX(), getY(), getMap())))
                    return false;
                // 랜덤 텔레포트
                Util.toRndLocation(this);
                toTeleport(getHomeX(), getHomeY(), getHomeMap(), true);
                toSender(S_ObjectLock.clone(BasePacketPooling.getPool(S_ObjectLock.class), 0x09));
                resetMovementFailure();
                return true;
            }
        }
        return false;
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
                if (canMove && toMoving(this, x, y, headingTry, true, false)) {
                    teleportTime = System.currentTimeMillis() + Util.random(1000, 3000);
                    moved = true;
                    resetMovementFailure();
                    // 이동 중 실시간 타겟 체크 (텔레포트 실패 시 여기서 실행됨)
                    checkTargetDuringMove();
                    break;
                }
            }
            if (!moved) {
                registerMovementFailure(null, start_x, start_y, start_map);
                if (tryForceRelocationForStuck()) {
                    return;
                }
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
            if (action != null && Util.isAreaAttack(this, o) && isAttack(o, true)) {
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

    private void registerMovementFailure(object primary, int targetX, int targetY, int targetMap) {
        boolean sameTarget = (moveFailTargetX == targetX && moveFailTargetY == targetY && moveFailTargetMap == targetMap);
        if (!sameTarget) {
            moveFailCount = 0;
            moveFailFirstTime = 0L;
        }

        long now = System.currentTimeMillis();
        if (moveFailFirstTime == 0L) {
            moveFailFirstTime = now;
        }
        moveFailLastTime = now;
        moveFailCount++;
        moveFailTargetX = targetX;
        moveFailTargetY = targetY;
        moveFailTargetMap = targetMap;

        if (primary != null && moveFailCount >= 2) {
            astarIgnore.add(primary);
        }
    }

    private void resetMovementFailure() {
        moveFailCount = 0;
        moveFailFirstTime = 0L;
        moveFailLastTime = 0L;
        moveFailTargetX = Integer.MIN_VALUE;
        moveFailTargetY = Integer.MIN_VALUE;
        moveFailTargetMap = Integer.MIN_VALUE;
    }

    private boolean isStuckByMovementFailure() {
        if (moveFailCount >= STUCK_FAIL_COUNT_THRESHOLD) {
            return true;
        }
        if (moveFailFirstTime == 0L) {
            return false;
        }
        return (System.currentTimeMillis() - moveFailFirstTime) >= STUCK_FAIL_WINDOW_MS;
    }

    private boolean tryForceRelocationForStuck() {
        if (!isStuckByMovementFailure()) {
            return false;
        }

        int[] localSafe = findNearestPassableTile(getX(), getY(), getMap(), FORCE_RELOCATE_SCAN_RADIUS);
        if (localSafe != null) {
            toTeleport(localSafe[0], localSafe[1], localSafe[2], true);
            target = null;
            targetItem = null;
            currentAttackTarget = null;
            resetMovementFailure();
            return true;
        }

        int[] homeSafe = findNearestPassableTile(getHomeX(), getHomeY(), getHomeMap(), FORCE_RELOCATE_SCAN_RADIUS);
        if (homeSafe != null) {
            toTeleport(homeSafe[0], homeSafe[1], homeSafe[2], true);
        } else {
            toTeleport(getHomeX(), getHomeY(), getHomeMap(), true);
        }

        target = null;
        targetItem = null;
        currentAttackTarget = null;
        resetMovementFailure();
        return true;
    }

    private int[] findNearestPassableTile(int baseX, int baseY, int mapId, int radiusLimit) {
        if (radiusLimit <= 0) {
            return null;
        }
        if (isTilePassable(baseX, baseY, mapId)) {
            return new int[] { baseX, baseY, mapId };
        }

        for (int radius = 1; radius <= radiusLimit; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                        continue;
                    }
                    int tx = baseX + dx;
                    int ty = baseY + dy;
                    if (isTilePassable(tx, ty, mapId)) {
                        return new int[] { tx, ty, mapId };
                    }
                }
            }
        }
        return null;
    }

    private boolean isTilePassable(int x, int y, int mapId) {
        if (World.isMapdynamic(x, y, mapId)) {
            return false;
        }
        if (World.isNotMovingTile(x, y, mapId)) {
            return false;
        }
        for (int heading = 0; heading < 8; heading++) {
            if (World.isThroughObject(x, y, mapId, heading)) {
                return true;
            }
        }
        return false;
    }

    private int[] pickContinuousStepPoint(Node tailNode) {
        if (tailNode == null) {
            return null;
        }

        List<Node> route = new ArrayList<Node>();
        Node cursor = tailNode;
        while (cursor != null) {
            route.add(cursor);
            cursor = cursor.prev;
        }

        int stepBudget = shouldUseContinuousMove() ? CONTINUOUS_MOVE_STEP : 1;
        int currentX = getX();
        int currentY = getY();
        int moved = 0;

        for (int idx = route.size() - 1; idx >= 0 && moved < stepBudget; idx--) {
            Node step = route.get(idx);
            if (step == null) {
                continue;
            }
            if (step.x == currentX && step.y == currentY) {
                continue;
            }

            int heading = Util.calcheading(currentX, currentY, step.x, step.y);
            if (!World.isThroughObject(currentX, currentY, getMap(), heading)) {
                break;
            }
            if (World.isMapdynamic(step.x, step.y, getMap()) || World.isNotMovingTile(step.x, step.y, getMap())) {
                break;
            }

            currentX = step.x;
            currentY = step.y;
            moved++;
        }

        if (moved <= 0) {
            return null;
        }
        return new int[] { currentX, currentY };
    }

    private boolean shouldUseContinuousMove() {
        return isBrave() || getSpeed() >= 1 || getAiStatus() == Lineage.AI_STATUS_ATTACK;
    }

    private boolean moveToPointAndCheck(int tx, int ty, int heading) {
        int oldX = getX();
        int oldY = getY();
        int oldMap = getMap();
        toMoving(tx, ty, heading);
        return oldX != getX() || oldY != getY() || oldMap != getMap();
    }

    @Override
    protected void toAiWalk(long time) {
    	
    	autoPartyManagement();
       
        // 부모 클래스의 toAiWalk 메서드를 호출하여 기본 AI 동작 수행
        super.toAiWalk(time);
        
        if (getPartyId() > 0) {
            handlePartyCollaboration();
            
            Party p = PartyController.find(this);
            // 내가 파티원(리더가 아님)이면 혼자 맘대로 사냥터로 안 가도록 중지
            if (p != null && p.getMaster() != this) {
                return; 
            }
        }
        
        if ((getRobotStatus() & RobotConversationController.ROBOT_STATE_CHATTING) != 0) {
            return;
        }
        if (!World.isSafetyZone(getX(), getY(), getMap()) && !isWarCastle && !action.contains("공성")) {
            // ✅ 수정: 수첩에 없는 층(2층, 3층)이어도 정상적인 던전이면 마을로 도망가지 않음!
            if (!isPossibleMap() && !isValidDungeonFloor()) { 
                goToHome(false); // 즉시 마을로 강제 귀환
                return;
            }
        }
        // 현재 객체가 속한 성 정보를 가져옴
        Kingdom k = KingdomController.find(getWarCastleUid());
        // 오픈 대기 상태 및 특정 모드 체크
        if (Lineage.open_wait && pcrobot_mode != PCROBOT_MODE.Stay && pcrobot_mode != PCROBOT_MODE.Cracker && isWait())
            return;
        // 현재 객체의 로봇 모드에 따른 처리
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
        default: // ✅ 안전을 위한 기본 처리
            break;
        }
        
        // 물약 복용 처리
        if (pcrobot_mode != PCROBOT_MODE.Cracker && pcrobot_mode != PCROBOT_MODE.Stay) {
            toHealingPotion();
            toBuffPotion();
            // 버프 스킬 사용
            List<Skill> skill_list = SkillController.find(this);
            if (toSkillHealMp(skill_list) || toSkillHealHp(skill_list) || toSkillBuff(skill_list) || toSkillSummon(skill_list)) {
                ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                return;
            }
            // 서먼 객체에게 버프 시전
            if (toBuffSummon()) {
                ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                return;
            }
        }
        // 특정 맵에서 제외 (잊섬 등)
        if (!isExcludedMap(getMap()) && World.isSafetyZone(getX(), getY(), getMap())) {
            manageDelayTime();
            // 보라돌이 상태 제거
            if (isBuffCriminal())
                BuffController.remove(this, Criminal.class);
            // 사냥터 이동
            List<Book> list = BookController.find(this);
            if (list.isEmpty())
                return;
            teleportToHuntingGround(list);
        }
        if (tempTarget == null && !isWarCastle && !World.isGiranHome(getX(), getY(), getMap())) {
            // 가장 가까운 플레이어 또는 로봇 찾기
            for (object obj : getInsideList()) {
                if (obj instanceof PcInstance || obj instanceof PcRobotInstance) {
                    tempTarget = getClosestTarget(tempTarget, obj);
                }
            }
            // tempTarget이 있을 경우, 적절한 멘트 실행
            if (tempTarget != null) {
                if (!tempTarget.isInvis() && Util.random(1, 100) <= Lineage.robot_ment_probability) {
                    RobotController.getRandomMentAndChat(Lineage.AI_MEET_MENT, this, tempTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_MEET_MENT_DELAY);
                    tryWhaleMent("MEET", tempTarget);
                }
                else if (tempTarget.isInvis() && Util.random(1, 100) <= Lineage.robot_ment_probability) {
                    RobotController.getRandomMentAndChat(Lineage.AI_INVISIBLE_MENT, this, tempTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_INVISIBLE_MENT_DELAY);
                    tryWhaleMent("MEET", tempTarget);
                }
                // tempTarget 초기화 → 새로운 대상과 대화 가능
                tempTarget = null;
            }
        }
       
        // 공성 중인 경우 처리
        if (k != null && getClanId() != 0 && k.isWar() && k.getClanId() != 0 && k.getClanId() == getClanId() && isWarCastle && action.contains("공성")) {
            // 아직 외성내부에 존재하지 않는다면 랜덤워킹할 수 잇게 특정 위치에 스폰처리.
            if (!KingdomController.isKingdomLocation(this, k.getUid()) && KingdomController.getUserCountInKingdomArea(k.getUid()) < Lineage.robot_kingdom_war_max_people) {
                if (!RobotController.isKingdomLocation(this, true, k.getUid())) {
                    RobotController.toKingdomRandomLocationTeleport(this, k.getUid());
                    target = null;
                    isWarFC = false;
                } else {
                    isWarFC = true;
                }
                ai_time = SpriteFrameDatabase.find(getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                return;
            }
            if (currentAttackTarget == null) {
                findTarget();
            }
            // 요정 클래스의 공성 행동 처리
            if (isWarFC) {
                if (getClassType() == Lineage.LINEAGE_CLASS_ELF) {
                    // 디텍션(은신 해제)
                    if (Util.random(0, 150) == 0)
                        Detection.init(this, SkillDatabase.find(2, 4));
                    ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                    // 블래싱 시전
                    if (Util.random(0, 100) == 0) {
                        for (object o : getInsideList(true)) {
                            if (o.getClanId() == getClanId() && Util.random(0, 10) == 0)
                                NaturesBlessing.onBuff(this, (Character) o, SkillDatabase.find(21, 3));
                            ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                        }
                    }
                }
                return;
            }
        }
        isWarFC = false;
        if (k != null && getClanId() != 0 && k.isWar() && (k.getClanId() == 0 || k.getClanId() != getClanId()) && isWarCastle && action.contains("공성")) {
            moveToCastleLocation();
        }
        if (currentAttackTarget == null) {
            findTarget();
        }
        // 실시간 타겟 체크 (자동 사냥 강화)
        if (action.contains("사냥") && currentAttackTarget == null) {
            findTarget(); // 주기적으로 타겟 재탐색
        }
        
        // 🌟 1.5. 사냥터 인구 초과(8명) 시 다음 층으로 내려가는 지능형 로직 🌟
        Party partyCheck = PartyController.find(this);
        boolean isFollower = (partyCheck != null && partyCheck.getMaster() != this);
        
        // 파티원이 아니며(파티장 혹은 솔로), 공격할 타겟이 없을 때 여유를 갖고 인원수 체크
        if (!isFollower && target == null && this.currentAttackTarget == null) {
            if (checkAndMoveToNextFloor()) {
                return; // 다음 층으로 이동했으면 아래 로직 스킵
            }
        }

        // 🌟 2. 사냥터 특성(텔레포트 가능 여부)에 따른 지능형 타임아웃 판단 🌟
        boolean canTeleport = LocationController.isTeleportZone(this, false, false);
        long timeoutLimit = canTeleport ? 30000 : 120000;

        // 파티원이 아니며, 타겟이 없을 때만 타임아웃 타이머 작동
        if (!isFollower && target == null && this.currentAttackTarget == null) {
            if (Util.random(0, 99) < 8) {
                tryWhaleMent("IDLE", null);
            }
            if (targetSearchStartTime == 0) {
                targetSearchStartTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - targetSearchStartTime > timeoutLimit) {
                // 타임아웃 도달 -> 다른 사냥터로 짐 싸서 이동!
                targetSearchStartTime = 0;
                String msg = canTeleport 
                    ? "이 사냥터에는 몹이 없네요. 다른 곳으로 옮깁니다!" 
                    : "이 던전은 한참 걸어도 몹이 안 보이네요. 다른 던전으로 이동합니다!";
                broadcastRobotChat(msg);
                toTeleportToAppropriateHuntingGround(); 
                return; // 완전히 다른 곳으로 이동했으므로 아래 걷기 로직 스킵
            }
        } else if (target != null || this.currentAttackTarget != null) {
            // 몹을 발견하면 타이머 즉시 초기화 (안심하고 사냥 시작)
            targetSearchStartTime = 0;
        }

        // 🌟 2. 맵 특성에 맞춘 내부 수색 로직 (실제 유저처럼 행동) 🌟
        // 랜덤 이동 또는 성으로 이동
        if (target == null && (!isWarCastle || !action.contains("공성"))) {
            // 타겟 없을 때 (화면 내 타겟 X) 텔레포트 확률 증가
            if (Util.random(0, 100) < TELEPORT_CHANCE_NO_TARGET && randomTeleport()) {
                // 텔레포트 성공 시 타겟 재탐색
                findTarget();
                return;
            } else {
                moveToRandomLocation(time);
                setAiStatus(Lineage.AI_STATUS_WALK);
            }
        }
        // 타겟 탐색: 피격 대상 없고, 루팅/도주 상태가 아닐 경우만
        if (target == null && targetItem == null && getAiStatus() != Lineage.AI_STATUS_PICKUP && getAiStatus() != Lineage.AI_STATUS_ESCAPE) {
            findItem();
        }
        // 특정 주기로 A* 경로 리스트를 정리하여 길막힌 객체 제거
        if (Util.random(0, 1) == 0)
            clearAstarList();
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

        final int LEVEL_GAP_LIMIT = 15; 

        List<Book> possibleList = new ArrayList<>();
        int myLevel = getLevel();

        for (Book book : list) {
            if (book != null && book.getEnable()) {
                int minLevel = book.getMinLevel();
                
                if (myLevel >= minLevel) {
                    if ((myLevel - minLevel) <= LEVEL_GAP_LIMIT) {
                        possibleList.add(book);
                    }
                }
            }
        }

        if (possibleList.isEmpty()) {
            for (Book book : list) {
                if (book != null && book.getEnable() && myLevel >= book.getMinLevel()) {
                    possibleList.add(book);
                }
            }
        }

        if (possibleList.isEmpty()) return;
        Book b = possibleList.get(Util.random(0, possibleList.size() - 1));
        
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
        if (!isWarCastle || !action.contains("공성")) {
            processInsideList();
        } else {
            processAllList(k, c);
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
                RobotController.isKingdomAttLocation(this, true, k.getUid());
                target = null;
                currentAttackTarget = null;
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
     * 군주 행동 처리
     */
    private synchronized void handleLordActions(Clan c, Kingdom k) {
        if (!RobotController.isKingdomCrownCoords(this, k.getUid())) {
            // 군주가 왕관 좌표로 이동할 때는 PC 무시 이동 활성화
            moveToCastle(k.getUid(), RobotController.KINGDOM_CROWN_COORDS, true);
        } else {
            toPolyRemove();
            if (getInventory().getSlot(Lineage.SLOT_WEAPON) != null) {
                getInventory().getSlot(Lineage.SLOT_WEAPON).toClick(this, null);
                if (getInventory().getSlot(Lineage.SLOT_WEAPON) == null) {
                    if (target != null && !target.getName().equalsIgnoreCase("면류관")) {
                        target = null;
                        currentAttackTarget = null;
                    }
                }
            }
        }
    }

    /**
     * 성 중심 또는 왕관으로 이동 (좁은 공간 대응 + 장애물 우회 + PC 무시 조건 포함)
     */
    private synchronized void moveToCastle(int kingdomIndex, int[][] castleCoords, boolean ignorePC) {
        int x1 = castleCoords[kingdomIndex][0];
        int x2 = castleCoords[kingdomIndex][1];
        int y1 = castleCoords[kingdomIndex][2];
        int y2 = castleCoords[kingdomIndex][3];
        int map = castleCoords[kingdomIndex][4];
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        boolean isInsideCastle = RobotController.isCastleInsideCoords(this, getWarCastleUid());
        int targetX = centerX;
        int targetY = centerY;
        if (isInsideCastle && !ignorePC) {
            List<int[]> spreadPositions = new ArrayList<>();
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    spreadPositions.add(new int[]{centerX + dx, centerY + dy});
                }
            }
            long objectId = this.getObjectId();
            long offsetId = objectId - 1900000L;
            int uidIndex = (int) (offsetId % spreadPositions.size());
            int[] spread = spreadPositions.get(uidIndex);
            targetX = spread[0];
            targetY = spread[1];
        }
        long now = System.currentTimeMillis();
        if (now - lastMoveAttemptTime < 300) return;
        lastMoveAttemptTime = now;
        int heading = Util.calcheading(this.x, this.y, targetX, targetY);
        if (now - lastDirectionSetTime > 2000) {
            setHeading(heading);
            lastDirectionSetTime = now;
        }
        // 1. 직진 가능하면 이동
        if (isMovableTile(targetX, targetY, heading, map, ignorePC)) {
            if (toMoving(this, targetX, targetY, 0, true, ignorePC)) return;
        }
        // 2. 현재 heading 방향으로 한 칸 전진
        int nextX = Util.getXY(getHeading(), true) + this.x;
        int nextY = Util.getXY(getHeading(), false) + this.y;
        if (isMovableTile(nextX, nextY, getHeading(), map, ignorePC)) {
            if (toMoving(this, nextX, nextY, 0, true, ignorePC)) return;
        }
        // 3. 중심 방향 기준 8방향 후보
        List<int[]> headingCandidates = new ArrayList<>();
        for (int h = 0; h < 8; h++) {
            int tx = Util.getXY(h, true) + this.x;
            int ty = Util.getXY(h, false) + this.y;
            if (isMovableTile(tx, ty, h, map, ignorePC)) {
                int dist = Math.abs(tx - targetX) + Math.abs(ty - targetY);
                headingCandidates.add(new int[]{tx, ty, h, dist});
            }
        }
        headingCandidates.sort(Comparator.comparingInt(a -> a[3]));
        for (int[] cand : headingCandidates) {
            setHeading(cand[2]);
            if (toMoving(this, cand[0], cand[1], 0, true, ignorePC)) return;
        }
        // 4. 주변 ±1 타일 탐색
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
                    if (o instanceof PcInstance || o instanceof PcRobotInstance) {
                        tryWhaleMent("ATTACK", o);
                    }
	                    toAttack(o, o.getX(), o.getY(), bow, getGfxMode() + Lineage.GFX_MODE_ATTACK, 0, false);
	                    if (o.isDead()) {
	                        tryWhaleMent("KILL", o);
	                    }
        
                    // 체력이 낮을 경우 확률적으로 도망
                    if (getHpPercent() <= Lineage.robot_escape_threshold_hp && (!isWarCastle || !action.contains("공성"))) {
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
                    registerMovementFailure(o, o.getX(), o.getY(), o.getMap());
                    if (tryForceRelocationForStuck()) {
                        return;
                    }
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
        tryWhaleMent("SPAWN", null);
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
                        moved = moveToPointAndCheck(x, y, Util.calcheading(this.x, this.y, x, y));
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
                            int[] nextPoint = pickContinuousStepPoint(tail);
                            if (nextPoint != null) {
                                moved = moveToPointAndCheck(nextPoint[0], nextPoint[1], Util.calcheading(this.x, this.y, nextPoint[0], nextPoint[1]));
                            }
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
            moved = moveToPointAndCheck(x, y, h);
        }
        if (moved) {
            resetMovementFailure();
        } else {
            registerMovementFailure(primary, x, y, getMap());
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
        resetMovementFailure();
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
		//
		Summon s = SummonController.find(this);
		if (s == null || s.getSize() == 0)
			return false;
		//
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
	 * 	: 양호하다면 false를 리턴.
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
	 *            : 공격당한 객체
	 * @param cha
	 *            : 공격한 객체
	 */
    public void toDamage(Character cha) {
        // ✅ 유효성 검사
        if (cha == null || cha.getObjectId() == this.getObjectId() || cha.getGm() > 0) {
            return;
        }

        // ✅ 타겟이 없을 경우에만 설정
        if (currentAttackTarget == null) {
            setTarget(cha);
        }
    }

    @Override
    public void setExp(double exp) {
        // ✅ Math.floor()를 사용하여 곱하기 연산 후 발생하는 소수점을 아예 버림(내림) 처리합니다.
        double finalExp = Math.floor(exp * 1.1);

        int oldLevel = this.getLevel();
        super.setExp(finalExp);

        if (this.getLevel() > oldLevel) {
            this.setNowHp(this.getMaxHp());
            this.setNowMp(this.getMaxMp());

            try {
                this.toSender(lineage.network.packet.server.S_ObjectEffect.clone(lineage.network.packet.BasePacketPooling.getPool(lineage.network.packet.server.S_ObjectEffect.class), this, 3760));
            } catch (Exception e) {}

            toTeleportToAppropriateHuntingGround();
        }
        saveRobotLevelToDb();
    }

    /**
     * 로봇의 레벨과 세부 경험치를 데이터베이스(_robot 테이블)에 저장합니다.
     */
    private void saveRobotLevelToDb() {
        java.sql.Connection con = null;
        java.sql.PreparedStatement pstm = null;

        try {
            con = lineage.database.DatabaseConnection.getLineage();

            String sql = "UPDATE _robot SET level=?, exp=?, lawful=? WHERE name=?";

            pstm = con.prepareStatement(sql);
            pstm.setInt(1, this.getLevel());     // 1번 ? : 레벨
            pstm.setLong(2, (long) this.getExp()); // 2번 ? : 소수점 뗀 경험치 정수값
            pstm.setInt(3, this.getLawful());    // 3번 ? : lawful
            pstm.setString(4, this.getName());   // 4번 ? : 로봇 이름

            pstm.executeUpdate();

        } catch (Exception e) {
            lineage.share.System.println("로봇 데이터 저장 오류: " + this.getName() + " - " + e.getMessage());
        } finally {
            lineage.database.DatabaseConnection.close(con, pstm, null);
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
                    
                    if (obj instanceof lineage.world.object.instance.RobotInstance && obj != this) {
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
                broadcastRobotChat("파티가 안구해지네요. 혼자 출발합니다.");
                partyWaitStartTime = 0;
                toTeleportToAppropriateHuntingGround(); 
            }
        } else {
            partyWaitStartTime = 0;
        }
    }

    /**
     * 🌟 파티원 협동 로직 (리더 따라가기 & 타겟 공유)
     */
    private void handlePartyCollaboration() {
        Party party = PartyController.find(this);
        if (party == null) return;

        PcInstance leader = party.getMaster();
        if (leader == null || leader == this) return; // 리더 본인은 패스

        // 리더와 거리가 멀어지면 따라감
        int dist = Util.getDistance(this, leader);
        if (dist > 2 && dist < 15) {
            toMoving(leader, leader.getX(), leader.getY(), 0, true, false);
        } else if (dist >= 15 || getMap() != leader.getMap()) {
            toTeleport(leader.getX(), leader.getY(), leader.getMap(), true);
        }

        // 리더가 때리는 타겟을 같이 때림(점사)
        object leaderTarget = null;
        if (leader instanceof PcRobotInstance) {
            leaderTarget = ((PcRobotInstance) leader).getTarget();
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
        for (PcInstance pc : World.getPcList()) {
            if (pc.getMap() == this.getMap()) currentPop++;
        }
        for (RobotInstance robot : World.getRobotList()) {
            if (robot.getMap() == this.getMap()) currentPop++;
        }

        // 8명 초과 시 다음 층 탐색
        if (currentPop > 8) {
            String currentName = getCurrentMapName();
            if (currentName == null || currentName.isEmpty()) return false;

            // "기란 감옥 1층" -> "기란 감옥" 추출
            String prefix = currentName.replaceAll("\\s*\\d+층.*", "").trim();

            int[] nextLoc = getNextFloorFromItemTeleport(prefix, currentName);
            
            if (nextLoc != null) {
                broadcastRobotChat("사람이 너무 많네요. 다음 층으로 이동합니다!");
                
                Party party = PartyController.find(this);
                boolean isLeader = (party != null && party.getMaster() == this);
                
                int nextX = nextLoc[0];
                int nextY = nextLoc[1];
                int nextMap = nextLoc[2];

                toTeleport(nextX, nextY, nextMap, true);
                
                if (isLeader) {
                    for (PcInstance member : party.getList()) {
                        if (member != this && (member instanceof RobotInstance)) {
                            member.toTeleport(nextX, nextY, nextMap, true);
                        }
                    }
                }
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
        this.huntingEvaluator = new goldbitna.robot.evaluate.RobotHuntingEvaluator();
        
        // 직업(Class)에 따른 행동 패턴 장착
        switch (this.getClassType()) {
            case lineage.share.Lineage.LINEAGE_CLASS_ROYAL:
                this.classBehavior = new goldbitna.robot.behavior.BehaviorPrince();
                break;
            case lineage.share.Lineage.LINEAGE_CLASS_KNIGHT:
                this.classBehavior = new goldbitna.robot.behavior.BehaviorKnight();
                break;
            case lineage.share.Lineage.LINEAGE_CLASS_ELF:
                this.classBehavior = new goldbitna.robot.behavior.BehaviorElf();
                break;
            case lineage.share.Lineage.LINEAGE_CLASS_WIZARD:
                this.classBehavior = new goldbitna.robot.behavior.BehaviorWizard();
                break;
            default: // 다크엘프 등 미구현 직업은 기본 기사 패턴으로 처리
                this.classBehavior = new goldbitna.robot.behavior.BehaviorKnight();
                break;
        }
        this.currentState = RobotState.IDLE;
    }
}
