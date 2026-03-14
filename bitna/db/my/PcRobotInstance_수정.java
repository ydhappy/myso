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

public class PcRobotInstance extends RobotInstance {
    protected static int ADEN_LIMIT = 1000000000;
    protected static int HEALING_PERCENT = 95;
    protected static int GOTOHOME_PERCENT = 40;
    protected static int USABLE_MP_PERCENT = 30;
    protected static final int MAX_WAR_RECRUIT = 150;
    
    protected long partyWaitStartTime = 0;
    protected long targetSearchStartTime = 0;
    protected long lastFloorCheckTime = 0;
    
    protected static enum PCROBOT_MODE {
        None, HealingPotion, HastePotion, BraveryPotion, ScrollPolymorph, Arrow, InventoryHeavy, ElvenWafer, Polymorph, Stay, Cracker,
    }
    
    protected AStar aStar;
    protected Node tail;
    protected int[] iPath;
    protected final Set<object> astarIgnore = ConcurrentHashMap.newKeySet();
    protected List<object> attackList;
    protected List<object> astarList;
    protected List<object> temp_list;
    protected Item weapon;
    protected Item doll;
    protected int weaponEn;
    protected String weapon_name;
    protected String doll_name;
    public PCROBOT_MODE pcrobot_mode;
    protected int step;
    protected int tempGfx;
    public volatile object target;
    public volatile object targetItem;
    public volatile object tempTarget;
    protected object currentAttackTarget; 
    protected boolean mythicPoly;
    protected boolean randomPoly;
    protected Object sync_ai = new Object();
    protected long ai_time_temp_1;
    protected long polyTime;
    protected long delayTime;
    public long teleportTime;
    protected long lastMoveAttemptTime = 0;
    protected long lastDirectionSetTime = 0;
    public String action;
    public boolean isReload;
    
    protected static final int TELEPORT_THRESHOLD_DISTANCE = 10;
    protected static final int TELEPORT_CHANCE_NO_TARGET = 80;
    public int noTargetWalkCount = 0;
    
    protected long delayedActionTime = 0;
    protected Runnable delayedAction = null;
    
    protected IRobotClassBehavior classBehavior;
    protected RobotHuntingEvaluator huntingEvaluator;
    protected RobotPartyManager partyManager;
    
    protected int moveFailCount = 0;
    protected int lastMoveX = -1;
    protected int lastMoveY = -1;

    // ==========================================
    // ⚔️ 공성 전용 전역 상태 및 변수
    // ==========================================
    protected enum RobotWarState { IDLE, WAR_ASSAULT, WAR_OCCUPY, WAR_END }
    protected RobotWarState warState = RobotWarState.IDLE;
    protected int[] myCachedFormationSeat = null;
    protected boolean isWarFC = false;
    protected long lastWarCheckTime = System.currentTimeMillis() + Util.random(0, 10000); // 분산 스캔
    protected boolean isSelectedForWar = false;
    
    public PcRobotInstance() {
        aStar = new AStar();
        iPath = new int[2];
        astarList = new ArrayList<>();
        attackList = new ArrayList<>();
        temp_list = new ArrayList<>();
        target = targetItem = tempTarget = currentAttackTarget = null;
        partyManager = new RobotPartyManager();
    }

    @Override
    public void close() {
        super.close();
        if (getInventory() != null) {
            for (ItemInstance ii : getInventory().getList()) ItemDatabase.setPool(ii);
            getInventory().clearList();
        }
        weapon_name = doll_name = null;
        weapon = doll = null;
        action = null;
        target = targetItem = tempTarget = currentAttackTarget = null;
        teleportTime = delayTime = polyTime = ai_time_temp_1 = weaponEn = step = tempGfx = 0;
        randomPoly = mythicPoly = isReload = false;
        if (Util.random(0, 99) < 10) pcrobot_mode = PCROBOT_MODE.Stay;
        else pcrobot_mode = PCROBOT_MODE.None;
        if (aStar != null) aStar.cleanTail();
        if (astarList != null) clearAstarList();
        if (temp_list != null) temp_list.clear();
        warState = RobotWarState.IDLE;
        isSelectedForWar = false;
    }

    @Override
    public void toSave(Connection con) {}

    public int getAttackListSize() { return attackList.size(); }
    protected void appendAttackList(object o) { synchronized (attackList) { if (!attackList.contains(o)) attackList.add(o); } }
    public void removeAttackList(object o) { synchronized (attackList) { attackList.remove(o); } }
    protected List<object> getAttackList() { synchronized (attackList) { return new ArrayList<>(attackList); } }
    protected boolean containsAttackList(object o) { synchronized (attackList) { return attackList.contains(o); } }
    public boolean containsAstarList(object o) { synchronized (astarList) { return astarList.contains(o); } }
    protected void removeAstarList(object o) { synchronized (astarList) { astarList.remove(o); } }
    protected void clearAstarList() { synchronized (astarList) { astarList.clear(); } }

    public int getWeaponEn() { return weaponEn; }
    public void setWeaponEn(int weaponEn) { this.weaponEn = weaponEn; }
    public int getTempGfx() { return tempGfx; }
    public void setTempGfx(int tempGfx) { this.tempGfx = tempGfx; }
    public String getWeapon_name() { return weapon_name; }
    public void setWeapon_name(String weapon_name) { this.weapon_name = weapon_name; }
    public String getDoll_name() { return doll_name; }
    public void setDoll_name(String doll_name) { this.doll_name = doll_name; }
    public boolean getMythicPoly() { return mythicPoly; }
    public void setMythicPoly(boolean mythicPoly) { this.mythicPoly = mythicPoly; }
    public boolean getRandomPoly() { return randomPoly; }
    public void setRandomPoly(boolean randomPoly) { this.randomPoly = randomPoly; }

    public synchronized object getTarget() { return target; }
    public synchronized void setTarget(object newTarget) {
        if (this.target != newTarget) {
            clearAstarList();
            moveFailCount = 0;
            lastMoveX = -1;
            lastMoveY = -1;
        }
        target = newTarget;
        if (currentAttackTarget != null) {
            if (Util.getDistance(this, currentAttackTarget) <= 10 && !currentAttackTarget.isDead()) {
                return;
            }
        }
        currentAttackTarget = newTarget;
    }

    public void toWorldJoin(Connection con) {
        super.toWorldJoin();
        setAiStatus(Lineage.AI_STATUS_WALK);
        setAutoPickup(Lineage.auto_pickup);
        World.appendRobot(this);
        BookController.toWorldJoin(this);
        CharacterController.toWorldJoin(this);
        BuffController.toWorldJoin(this);
        SkillController.toWorldJoin(this);
        SummonController.toWorldJoin(this);
        MagicDollController.toWorldJoin(this);
        ClanController.toWorldJoin(this);
        RobotController.readSkill(con, this);
        RobotController.readBook(con, this);
        setInventory();
        initRobotAI();
        AiThread.append(this);
    }

    @Override
    public void toWorldOut() {
        super.toWorldOut();
        SummonController.toWorldOut(this);
        setAiStatus(Lineage.AI_STATUS_DELETE);
        toReset(true);
        World.removeRobot(this);
        SummonController.toWorldOut(this);
        BookController.toWorldOut(this);
        SkillController.toWorldOut(this);
        ClanController.toWorldOut(this);
        CharacterController.toWorldOut(this);
        MagicDollController.toWorldOut(this);
        close();
    }

    public void setPcBobot_mode(String mode) {
        if (mode.contains("사냥") || mode.contains("PvP") || mode.contains("공성")) {
            if (action.equalsIgnoreCase("허수아비 공격") || action.equalsIgnoreCase("마을 대기")) {
                setAiStatus(Lineage.AI_STATUS_WALK);
                pcrobot_mode = PCROBOT_MODE.None;
                target = targetItem = tempTarget = currentAttackTarget = null;
                clearAstarList();
            }
        } else if (mode.equalsIgnoreCase("허수아비 공격")) {
            if (pcrobot_mode != PCROBOT_MODE.Cracker || target == null) attackCracker();
        } else if (mode.equalsIgnoreCase("마을 대기")) {
            if (action.equalsIgnoreCase("허수아비 공격")) goToHome(true);
            else goToHome(false);
        }
    }

    @Override
    public void toRevival(object o) {
        if (isDead()) {
            super.toReset(false);
            target = targetItem = tempTarget = currentAttackTarget = null;
            clearAstarList();
            int[] home = Lineage.getHomeXY();
            setHomeX(home[0]); setHomeY(home[1]); setHomeMap(home[2]);
            toTeleport(getHomeX(), getHomeY(), getHomeMap(), !isDead());
            setDead(false);
            setNowHp(level);
            toSender(S_ObjectRevival.clone(BasePacketPooling.getPool(S_ObjectRevival.class), o, this), false);
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
        if (cha == null || cha.getObjectId() == getObjectId() || dmg <= 0 || cha.getGm() > 0) return;
        
        if (warState == RobotWarState.IDLE) {
            if ((cha instanceof PcInstance && action.contains("사냥")) || (cha instanceof MonsterInstance && action.contains("PvP"))) {
                if (Util.random(1, 100) < 20) { randomTeleport(); }
                return;
            }
        }
        
        object o = (object) cha;
        if (currentAttackTarget == null && isAttack(o, true)) setTarget(o);
        
        if ((cha instanceof PcInstance || cha instanceof PcRobotInstance) && (target == null || target == cha) && (currentAttackTarget == null || currentAttackTarget == cha)) {
            if (Util.random(1, 100) <= Lineage.robot_ment_probability && type == Lineage.ATTACK_TYPE_MAGIC) {
                RobotController.getRandomMentAndChat(Lineage.AI_ATTACKED_MENT, this, cha, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ATTACKED_MENT_DELAY);
            }
        }
        
        Clan clan = ClanController.find(this);
        if (clan != null) {
            for (object obj : getInsideList()) {
                if (obj instanceof PcRobotInstance) {
                    PcRobotInstance member = (PcRobotInstance) obj;
                    if (clan.containsMemberList(member.getName())) {
                        if ((member.currentAttackTarget == null || member.currentAttackTarget == cha) && (cha instanceof PcInstance || cha instanceof RobotInstance)) {
                            if (!member.isDead() && member.currentAttackTarget == null && member != this) {
                                member.setTarget(cha);
                            }
                        }
                    }
                }
            }
        }
       
        if (getClanId() > 0 && Util.random(0, 100) < 30) RobotClanController.requestClanAssistance(this, cha);
        if (currentAttackTarget == null && Util.getDistance(this, cha) <= 10) {
            if (!isTargetOvercrowded(cha)) setTarget(cha);
        }
        removeAstarList(cha);
    }

    @Override
    public void toAiThreadDelete() {
        super.toAiThreadDelete();
        World.removeRobot(this);
        BookController.toWorldOut(this);
        CharacterController.toWorldOut(this);
    }

    @Override
    public void toAi(long time) {
        if (delayedActionTime > 0) {
            if (System.currentTimeMillis() >= delayedActionTime) {
                if (delayedAction != null) { delayedAction.run(); delayedAction = null; }
                delayedActionTime = 0;
            }
            return;
        }
    
        synchronized (sync_ai) {
            if (isReload) return;
            if (isDead()) {
                if (ai_time_temp_1 == 0) ai_time_temp_1 = time;
                if (ai_time_temp_1 + Lineage.ai_robot_corpse_time > time) return;
                goToHome(false);
                toRevival(this);
            }
            if ("마을 대기".equalsIgnoreCase(action)) {
                if (!World.isSafetyZone(getX(), getY(), getMap())) goToHome(false);
                return;
            }
            if (getInventory() == null) return;
            if ("허수아비 공격".equalsIgnoreCase(action) && pcrobot_mode != PCROBOT_MODE.Cracker) {
                attackCracker();
                return;
            }
        } 
        synchronized (this) {
            if (getInventory().getSlot(Lineage.SLOT_WEAPON) == null || !getInventory().getSlot(Lineage.SLOT_WEAPON).getItem().getName().equalsIgnoreCase(this.getWeapon_name())) {
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
            } else {
                ItemInstance item = ItemDatabase.newInstance(weapon);
                if (item != null) item.toClick(this, null);
            }
        }
        if (getHpPercent() <= HEALING_PERCENT) toHealingPotion();
        if (this.classBehavior != null && getHpPercent() <= 20.0) {
            this.classBehavior.executeFlee(this);
            return;
        }
        if (warState == RobotWarState.IDLE && !World.isSafetyZone(getX(), getY(), getMap()) && getHpPercent() <= GOTOHOME_PERCENT) {
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
        synchronized (this) {
            String[] items = { "무한 체력 회복 룬", "무한 신속 룬", "무한 가속 룬", "무한의 화살통", "무한 변신 주문서", "무한 신화 변신 북", "무한 버프 물약" };
            for (String itemName : items) {
                if (getInventory().find(itemName) == null) RobotController.giveItem(this, itemName, 1);
            }
        }
        if (getAiStatus() == Lineage.AI_STATUS_PICKUP && pcrobot_mode != PCROBOT_MODE.Cracker) {
            if (targetItem == null) setAiStatus(Lineage.AI_STATUS_WALK);
        }
        if ("bow".equalsIgnoreCase(weapon.getType2())) setArrow();
        synchronized (this) {
            switch (getAiStatus()) {
                case Lineage.AI_STATUS_WALK:
                    if (target != null) {
                        setAiStatus(Lineage.AI_STATUS_ATTACK);
                        currentAttackTarget = target;
                        target = null;
                    }
                    break;
                case Lineage.AI_STATUS_ATTACK:
                    if (pcrobot_mode != PCROBOT_MODE.Cracker) currentAttackTarget = checkTargetValidity(currentAttackTarget);
                    if (currentAttackTarget == null && pcrobot_mode != PCROBOT_MODE.Cracker) {
                        if (warState == RobotWarState.IDLE) {
                            if (!randomTeleport()) setAiStatus(Lineage.AI_STATUS_WALK);
                        } else {
                            setAiStatus(Lineage.AI_STATUS_WALK);
                        }
                    }
                    break;
            }
        }
        synchronized (this) {
            if (pcrobot_mode == PCROBOT_MODE.None && !getInventory().isWeightPercent(82)) pcrobot_mode = PCROBOT_MODE.InventoryHeavy;
        }
        synchronized (this) {
            if (pcrobot_mode == PCROBOT_MODE.None && getGfx() == getClassGfx() && RobotController.isPoly(this)) pcrobot_mode = PCROBOT_MODE.Polymorph;
        }
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

    protected boolean isKing() {
        Clan c = ClanController.find(this);
        return c != null && c.getLord() != null && c.getLord().equalsIgnoreCase(getName()) 
               && getClassType() == Lineage.LINEAGE_CLASS_ROYAL;
    }

    private void checkWarParticipation(int castleUid) {
        if (getClanId() == 0) {
            isSelectedForWar = false;
            return;
        }
        
        if (isKing()) { 
            isSelectedForWar = true; 
            return; 
        }
        
        Kingdom k = KingdomController.find(castleUid);
        if (k == null) {
            isSelectedForWar = false;
            return;
        }
        if (isKing()) {
            isSelectedForWar = true;
            return;
        }
        List<PcRobotInstance> clanRobots = new ArrayList<>();
        for (RobotInstance r : World.getRobotList()) {
            if (r instanceof PcRobotInstance && r.getClanId() == this.getClanId()) {
                clanRobots.add((PcRobotInstance) r);
            }
        }
        clanRobots.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));

        int myRank = clanRobots.indexOf(this);
        isSelectedForWar = (myRank != -1 && myRank < MAX_WAR_RECRUIT);
    }

    protected void handleWarState() {
        int castleUid = getWarCastleUid();
        boolean isWarActive = (castleUid != -1);
        
        if (isWarActive && System.currentTimeMillis() - lastWarCheckTime > 10000) {
            lastWarCheckTime = System.currentTimeMillis();
            checkWarParticipation(castleUid);
        }

        if (isWarActive && isSelectedForWar) {
            if (warState == RobotWarState.IDLE) {
                warState = RobotWarState.WAR_ASSAULT;
                setTarget(null);
                noTargetWalkCount = 0;
                myCachedFormationSeat = null;
                
                if (getPartyId() > 0) {
                    Party p = PartyController.find(this);
                    if (p != null) {
                        p.getList().remove(this);
                    }
                    setPartyId(0);
                }
            }
        } else {
            if (warState != RobotWarState.IDLE && warState != RobotWarState.WAR_END) {
                warState = RobotWarState.WAR_END;
            }
        }

        if (warState == RobotWarState.WAR_END) {
            resetAfterWar();
            return;
        }

        if (warState == RobotWarState.WAR_ASSAULT || warState == RobotWarState.WAR_OCCUPY) {
            Kingdom k = KingdomController.find(castleUid);
            if (k != null) {
                if (k.getClanId() == getClanId()) {
                    isWarFC = true; 
                } else {
                    isWarFC = false; 
                    if (isKing()) {
                        moveToCrown(castleUid, k);
                    } else {
                        moveToWarFormation(castleUid, k);
                    }
                }
            }
        }
    }

    private boolean processAssault(int castleUid, Kingdom k) {
        // 공성 지역 밖이면 집결지로 텔레포트 (최초 1회)
        if (!KingdomController.isKingdomLocation(this, castleUid)) {
            if (this.teleportTime < System.currentTimeMillis()) {
                teleportToDoorScatter(k);
                this.teleportTime = System.currentTimeMillis() + 4000;
            }
            return false;
        }

        // [수정 포인트] 성문이 살아있을 때만 문을 타겟팅
        KingdomDoor targetDoor = findLivingDoor(k);
        if (targetDoor != null && !RobotController.isCastleTopInsideCoords(this, castleUid)) {
            handleDoors(k, isKing());
            return false;
        }

        return true; 
    }

    private KingdomDoor findLivingDoor(Kingdom k) {
        for (KingdomDoor door : k.getListDoor()) {
            if (door != null && !door.isDead() && door.getNpc() != null && door.getNpc().getName().contains("외성문")) {
                return door;
            }
        }
        return null;
    }

    /**
     * 🌟 살아있는 외성문을 찾아 주변에 산개 스폰합니다.
     * (에러 원인이었던 좌표 매개변수 불일치 문제 해결 및 내부 진입 방지 로직 보강)
     */
    protected void teleportToDoorScatter(Kingdom k) {
        KingdomDoor targetDoor = null;
        
        for (KingdomDoor door : k.getListDoor()) {
            if (door != null && !door.isDead() && door.getNpc() != null) {
                if (door.getNpc().getName().contains("외성문")) {
                    targetDoor = door;
                    break; 
                }
            }
        }
        
        if (targetDoor == null) {
            for (KingdomDoor door : k.getListDoor()) {
                if (door != null && !door.isDead()) { targetDoor = door; break; }
            }
        }
        
        if (targetDoor != null) {
            int tx = targetDoor.getX();
            int ty = targetDoor.getY();
            int tmap = targetDoor.getMap();
            int castleUid = k.getUid();
            
            int finalX = tx;
            int finalY = ty;
            boolean found = false;
            
            for (int i = 0; i < 50; i++) {
                int rx = tx + Util.random(-15, 15);
                int ry = ty + Util.random(-15, 15);
                
                if (World.isThroughObject(rx, ry, tmap, 0) && !World.isNotMovingTile(rx, ry, tmap)) {
                    if (isSafeOutsideTile(rx, ry, tmap, castleUid)) {
                        finalX = rx;
                        finalY = ry;
                        found = true;
                        break;
                    }
                }
            }
            
            if (found) {
                toTeleport(finalX, finalY, tmap, true);
                toSender(S_ObjectLock.clone(BasePacketPooling.getPool(S_ObjectLock.class), 0x09));
            } else {
                RobotController.isKingdomAttLocation(this, true, k.getUid());
            }
        } else {
            RobotController.isKingdomAttLocation(this, true, k.getUid());
        }
    }

    /**
     * 🛡️ [추가] 텔레포트 좌표가 성 내부(수호탑 구역)인지 체크하는 좌표 전용 검증기
     */
    private boolean isSafeOutsideTile(int x, int y, int map, int castleUid) {
        int[] insideCoords = lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[castleUid];
        if (insideCoords != null && map == insideCoords[4]) {
            if (x >= insideCoords[0] && x <= insideCoords[1] && y >= insideCoords[2] && y <= insideCoords[3]) {
                return false; 
            }
        }
        return true; // 성 외부이므로 안전함
    }
    
    private void moveToCrown(int castleUid, Kingdom k) {
        if (!processAssault(castleUid, k)) return;

        int crownX = RobotController.KINGDOM_CROWN_COORDS[castleUid][0];
        int crownY = RobotController.KINGDOM_CROWN_COORDS[castleUid][2];
        
        int distToCrown = Util.getDistance(this.getX(), this.getY(), crownX, crownY);

        if (distToCrown > 1) {
            setTarget(null);
            this.currentAttackTarget = null;
            moveToCastleLogic(crownX, crownY, true);
        } else {
            if (warState != RobotWarState.WAR_OCCUPY) {
                warState = RobotWarState.WAR_OCCUPY;
            }
            if (getGfx() != getClassGfx()) {
                toPolyRemove();
                return; 
            }
            ItemInstance weaponItem = getInventory().getSlot(Lineage.SLOT_WEAPON);
            if (weaponItem != null) {
                weaponItem.toClick(this, null);
                return; 
            }
            for (object o : getInsideList()) {
                if (o instanceof KingdomCrown) {
                    synchronized (this) { 
                        this.target = o; 
                        this.currentAttackTarget = o;
                    }
                    o.toClick(this, null);
                    break;
                }
            }
        }
    }

    private void moveToWarFormation(int castleUid, Kingdom k) {
        if (!processAssault(castleUid, k)) return;

        int[] baseCoords = k.isCastleTopDead() ? RobotController.KINGDOM_CROWN_COORDS[castleUid] : RobotController.CASTLE_TOP_INSIDE_COORDS[castleUid];
        int baseCenterX = (baseCoords[0] + baseCoords[1]) / 2;
        int baseCenterY = (baseCoords[2] + baseCoords[3]) / 2;

        if (myCachedFormationSeat == null) {
            long offsetId = Math.abs(this.getObjectId() - 1900000L);
            int dx = (int)(offsetId % 11) - 5;
            int dy = (int)((offsetId / 11) % 11) - 5;
            myCachedFormationSeat = new int[]{dx, dy};
        }

        int targetX = baseCenterX + myCachedFormationSeat[0];
        int targetY = baseCenterY + myCachedFormationSeat[1];

        if (Util.getDistance(this.getX(), this.getY(), targetX, targetY) <= 1) {
            int nearbyRobots = 0;
            for (object obj : getInsideList()) {
                if (obj instanceof PcRobotInstance && obj.getObjectId() != this.getObjectId()) {
                    if (Util.getDistance(this.getX(), this.getY(), obj.getX(), obj.getY()) <= 1) nearbyRobots++;
                }
            }
            if (nearbyRobots >= 3) {
                targetX += Util.random(-1, 1);
                targetY += Util.random(-1, 1);
            }
        }

        moveToCastleLogic(targetX, targetY, false);
        syncMoveAnimation();
    }

    private void syncMoveAnimation() {
        int frame = SpriteFrameDatabase.getGfxFrameTime(
            this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
        if (frame <= 0) frame = 120;
        this.ai_time = frame;
    }
    
    private void moveToCastleLogic(int targetX, int targetY, boolean ignorePC) {
        navigateTo(targetX, targetY, this.getMap(), ignorePC);
    }

    /*
    private boolean canMoveToDir(int tx, int ty, int h, int mapId, boolean ignorePC) {
        if (!World.isThroughObject(this.getX(), this.getY(), mapId, h)) return false;
        if (World.isNotMovingTile(tx, ty, mapId)) return false;
        if (!ignorePC && World.getMapdynamic(tx, ty, mapId) > 0) return false;
        return true;
    }
    */
    
    private void resetAfterWar() {
        setTarget(null);
        this.currentAttackTarget = null;
        this.myCachedFormationSeat = null;
        this.noTargetWalkCount = 0;
        this.isSelectedForWar = false;
        this.warState = RobotWarState.IDLE;
        
        List<Book> list = BookController.find(this);
        if (list != null && !list.isEmpty()) teleportToHuntingGround(list); 
        else goToHome(false); 
    }

    @Override
    protected void toAiWalk(long time) {
        handleWarState();

        if (warState != RobotWarState.IDLE) {
            if (this.classBehavior != null) {
                long beforeMagicTime = this.delay_magic;
                this.classBehavior.executeBuffAndSupport(this);
                if (this.delay_magic > beforeMagicTime) return;
            }

            if (isWarFC) {
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
            }
            return;
        }

        autoPartyManagement();
        super.toAiWalk(time);
        if (this.classBehavior != null) this.classBehavior.executeBuffAndSupport(this);
        if (getPartyId() > 0) handlePartyCollaboration();
        if ((getRobotStatus() & RobotConversationController.ROBOT_STATE_CHATTING) != 0) return;
        if (!World.isSafetyZone(getX(), getY(), getMap()) && !action.contains("공성")) {
            if (!isPossibleMap() && !isValidDungeonFloor()) { goToHome(false); return; }
        }
        if (Lineage.open_wait && pcrobot_mode != PCROBOT_MODE.Stay && pcrobot_mode != PCROBOT_MODE.Cracker && isWait()) return;
        switch (pcrobot_mode) {
            case InventoryHeavy: toInventoryHeavy(); return;
            case Polymorph: toPolymorph(); return;
            case Stay: toStay(time); return;
            case ScrollPolymorph: toScrollPolymorph(); return;
            default: break;
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
        if (!isExcludedMap(getMap()) && World.isSafetyZone(getX(), getY(), getMap())) {
            manageDelayTime();
            if (isBuffCriminal()) BuffController.remove(this, Criminal.class);
            List<Book> list = BookController.find(this);
            if (list.isEmpty()) return;
            teleportToHuntingGround(list);
        }
        if (tempTarget == null && !World.isGiranHome(getX(), getY(), getMap())) {
            for (object obj : getInsideList()) {
                if (obj instanceof PcInstance || obj instanceof PcRobotInstance) tempTarget = getClosestTarget(tempTarget, obj);
            }
            if (tempTarget != null) {
                if (!tempTarget.isInvis() && Util.random(1, 100) <= Lineage.robot_ment_probability) RobotController.getRandomMentAndChat(Lineage.AI_MEET_MENT, this, tempTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_MEET_MENT_DELAY);
                else if (tempTarget.isInvis() && Util.random(1, 100) <= Lineage.robot_ment_probability) RobotController.getRandomMentAndChat(Lineage.AI_INVISIBLE_MENT, this, tempTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_INVISIBLE_MENT_DELAY);
                tempTarget = null;
            }
        }
        
        if (currentAttackTarget == null) findTarget();
        if (action.contains("사냥") && currentAttackTarget == null) findTarget(); 
        
        Party partyCheck = PartyController.find(this);
        boolean isFollower = (partyCheck != null && partyCheck.getMaster() != this);
        if (!isFollower && target == null && this.currentAttackTarget == null) {
            if (checkAndMoveToNextFloor()) return; 
        }

        boolean canTeleport = LocationController.isTeleportZone(this, false, false);
        long timeoutLimit = canTeleport ? 30000 : 120000;
        if (!isFollower && target == null && this.currentAttackTarget == null) {
            if (targetSearchStartTime == 0) targetSearchStartTime = System.currentTimeMillis();
            else if (System.currentTimeMillis() - targetSearchStartTime > timeoutLimit) {
                targetSearchStartTime = 0;
                if (canTeleport) randomTeleport();
                else {
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

        if (target == null && !action.contains("공성")) {
            if (Util.random(0, 100) < TELEPORT_CHANCE_NO_TARGET && randomTeleport()) {
                findTarget(); return;
            } else {
                moveToRandomLocation(time);
                setAiStatus(Lineage.AI_STATUS_WALK);
            }
        }
        if (target == null && targetItem == null & getAiStatus() != Lineage.AI_STATUS_PICKUP && getAiStatus() != Lineage.AI_STATUS_ESCAPE) findItem();
        if (Util.random(0, 1) == 0) clearAstarList();
    }

    protected boolean isExcludedMap(int map) { return map == 70 || map == 68 || map == 69 || map == 85 || map == 86; }
    protected void manageDelayTime() {
        if (delayTime == 0) delayTime = System.currentTimeMillis() + (1000 * (Util.random(3, 10)));
        if (delayTime > 0 && delayTime <= System.currentTimeMillis()) delayTime = 0;
    }

    protected void teleportToHuntingGround(List<Book> list) {
        if (list == null || list.isEmpty()) return;
        int myLevel = getLevel(); int maxMinLevel = -1;
        for (Book book : list) {
            if (book != null && book.getEnable() && myLevel >= book.getMinLevel()) {
                if (book.getMinLevel() > maxMinLevel) maxMinLevel = book.getMinLevel();
            }
        }
        if (maxMinLevel == -1) return;
        List<Book> bestList = new ArrayList<>();
        for (Book book : list) {
            if (book != null && book.getEnable() && book.getMinLevel() == maxMinLevel) bestList.add(book);
        }
        if (bestList.isEmpty()) return;
        Book b = bestList.get(Util.random(0, bestList.size() - 1));
        
        target = targetItem = currentAttackTarget = null;
        int targetX = b.getX(); 
        int targetY = b.getY(); 
        int targetMap = b.getMap();

        Party party = PartyController.find(this);
        if (party != null && party.getMaster() != null && party.getMaster() != this) {
            PcInstance leader = party.getMaster();
            if (!World.isSafetyZone(leader.getX(), leader.getY(), leader.getMap())) {
                targetX = leader.getX();
                targetY = leader.getY();
                targetMap = leader.getMap();
            }
        }

        toSafeTeleport(targetX, targetY, targetMap);
        this.start_x = targetX; 
        this.start_y = targetY; 
        this.start_map = targetMap;
    }
    
    protected void findTarget() {
        if (warState != RobotWarState.IDLE) {
            if (this.currentAttackTarget instanceof KingdomDoor) return;
            if (warState == RobotWarState.WAR_OCCUPY) return;

            int castleUid = getWarCastleUid();
            Kingdom k = KingdomController.find(castleUid);
            if (k == null) return;
            
            synchronized (this) { target = null; }
            try {
                object temp = null;
                List<object> allList = getInsideList();
                if (allList == null || allList.isEmpty()) return;
                
                for (object oo : allList) {
                    if (oo == null || oo instanceof GuardInstance || !Util.isAreaAttack(this, oo) || !isAttack(oo, true)) continue;

                    if (k.getClanId() != this.getClanId()) {
                        if (oo instanceof PcInstance || oo instanceof PcRobotInstance || oo instanceof KingdomCastleTop) {
                            temp = getClosestTarget(temp, oo);
                        }
                    } else {
                        if (oo instanceof PcInstance || oo instanceof PcRobotInstance) {
                            temp = getClosestTarget(temp, oo);
                        }
                    }
                }
                synchronized (this) {
                    if (temp != null && isAttack(temp, true)) setTarget(temp);
                }
            } catch (Exception e) {}
            return;
        }

        synchronized (this) { target = null; }
        if (Util.random(0, 100) < 3) {
            int currentMap = getMap(); long userCount = 0;
            for (PcInstance pc : World.getPcList()) { if (pc.getMap() == currentMap) userCount++; }
            for (RobotInstance r : World.getRobotList()) { if (r.getMap() == currentMap) userCount++; }
            if (userCount >= 30) {
                if (LocationController.isTeleportZone(this, true, false)) randomTeleport();
                else {
                    ChattingController.toChatting(this, "이 사냥터는 너무 붐비네요. 다른 곳으로 이동합니다.", Lineage.CHATTING_MODE_NORMAL);
                    toTeleportToAppropriateHuntingGround();
                }
                return;
            }
        }
        if (this.classBehavior != null && !action.contains("공성")) {
            object specialTarget = this.classBehavior.findTarget(this);
            if (specialTarget != null && Util.getDistance(this, specialTarget) <= 12) {
                if (isTargetOvercrowded(specialTarget)) return;
                synchronized (this) { setTarget(specialTarget); }
                noTargetWalkCount = 0; return;
            }
        }
        if (!action.contains("공성")) processInsideList();
        
        if (this.target == null) {
            noTargetWalkCount++;
            if (noTargetWalkCount < 30) {
                if (Util.random(0, 100) < 10) { setHeading(Util.random(0, 7)); moveToRandomLocation(System.currentTimeMillis()); }
            } else {
             if (LocationController.isTeleportZone(this, true, false)) randomTeleport();
                else toTeleportToAppropriateHuntingGround();
                noTargetWalkCount = 0;
            }
        } else {
            noTargetWalkCount = 0;
        }
    }

    protected void processInsideList() {
        try {
            if (target == null) {
                object temp = null;
                List<object> insideList = getInsideList(true);
                if (insideList == null || insideList.isEmpty()) return;
                for (object o : insideList) {
                    if (Util.isAreaAttack(this, o) && isAttack(o, true)) {
                        if ((o instanceof PcInstance && action.contains("PvP") && Util.random(0, 99) < 60) ||
                                (o instanceof MonsterInstance && action.contains("사냥"))) {
                            temp = getClosestTarget(temp, o);
                        }
                    }
                }
                synchronized (this) {
                    if (temp != null && isAttack(temp, true)) setTarget(temp);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    protected object getClosestTarget(object current, object candidate) {
        if (current == null) return candidate;
        int rangeCurrent = Util.getDistance(this, current);
        int rangeCandidate = Util.getDistance(this, candidate);
        return (rangeCandidate < rangeCurrent) ? candidate : current;
    }

    protected void findItem() {
        synchronized (this) { if (targetItem != null && !isPickupItem(targetItem)) targetItem = null; }
        processPickupItem();
    }

    protected void processPickupItem() {
        try {
            List<object> insideList = getInsideList();
            if (insideList == null || insideList.isEmpty()) return;
            object closestItem = insideList.stream().filter(this::isPickupItem).min(Comparator.comparingInt(o -> Util.getDistance(this, o))).orElse(null);
            if (closestItem != null) {
                synchronized (this) { setTarget(closestItem); }
                setAiStatus(Lineage.AI_STATUS_PICKUP);
                return;
            }
        } catch (Exception e) {}
    }

    protected boolean isPickupItem(object o) {
        if (!(o instanceof ItemInstance)) return false;
        if (containsAstarList(o)) return false;
        ItemInstance item = (ItemInstance) o;
        return !item.getItem().getName().equalsIgnoreCase("아데나");
    }

    @Override
    protected void toAiAttack(long time) {
        try {
            if (Lineage.open_wait && pcrobot_mode != PCROBOT_MODE.Cracker && isWait()) return;
            handlePotions();
            object o = checkTargetValidity(currentAttackTarget);
            if (o == null) {
                currentAttackTarget = null;
                if (pcrobot_mode != PCROBOT_MODE.Cracker && warState == RobotWarState.IDLE) randomTeleport();
                return;
            }
            if (getClanId() > 0 && getClanId() == o.getClanId() && !(o instanceof Doppelganger)) { clearTarget(); return; }
            if (shouldResetTarget(o)) { clearTarget(); return; }
            
            if (warState == RobotWarState.IDLE && (o instanceof PcInstance && !(o instanceof Pk1RobotInstance))) {
                for (object oo : getInsideList(true)) {
                    if (oo instanceof GuardInstance && (getClanId() == 0 || getClanId() != oo.getClanId())) { goToHome(true); clearTarget(); return; }
                }
            }
            if (o.isInvis() && Util.random(0, 100) <= 30) {
                toSender(S_ObjectAction.clone(BasePacketPooling.getPool(S_ObjectAction.class), this, Lineage.GFX_MODE_SPELL_NO_DIRECTION), true);
                Detection.onBuff(this, SkillDatabase.find(2, 4));
                ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION);
                return;
            }
            boolean magicUsed = toSkillAttack(o);
            boolean bow = getInventory().활장착여부();
            int atkRange = bow ? 8 : (getClassType() == Lineage.LINEAGE_CLASS_WIZARD ? 8 : 1);
            
            if (Util.isDistance(this, o, atkRange) && Util.isAreaAttack(this, o) && Util.isAreaAttack(o, this)) {
                if (!magicUsed && (AttackController.isAttackTime(this, getGfxMode() + Lineage.GFX_MODE_ATTACK, false) || AttackController.isMagicTime(this, getCurrentSkillMotion()))) {
                    ai_time = (int) (SpriteFrameDatabase.getSpeedCheckGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_ATTACK) + 40);
                    if (Util.random(1, 100) <= Lineage.robot_ment_probability && (o instanceof PcInstance || o instanceof PcRobotInstance)) {
                        if (warState != RobotWarState.IDLE) {
                            Kingdom k = KingdomController.find(getWarCastleUid());
                            if (k != null && k.isWar() && k.getClanId() == getClanId()) RobotController.getRandomMentAndChat(Lineage.AI_DEFENSE_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_DEFENSE_MENT_DELAY);
                            else RobotController.getRandomMentAndChat(Lineage.AI_SIEGE_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_SIEGE_MENT_DELAY);
                        } else {
                            RobotController.getRandomMentAndChat(Lineage.AI_ATTACK_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ATTACK_MENT_DELAY);
                        }
                    }
                    toAttack(o, o.getX(), o.getY(), bow, getGfxMode() + Lineage.GFX_MODE_ATTACK, 0, false);
                    if (warState == RobotWarState.IDLE && getHpPercent() <= Lineage.robot_escape_threshold_hp && !action.contains("공성")) {
                        if (o instanceof PcInstance && ((PcInstance) o).getHpPercent() > getHpPercent() && Util.random(0, 100) < Lineage.robot_escape_chance) { setAiStatus(Lineage.AI_STATUS_ESCAPE); return; }
                        else if (o instanceof PcRobotInstance && ((PcRobotInstance) o).getHpPercent() > getHpPercent() && Util.random(0, 100) < Lineage.robot_escape_chance) { setAiStatus(Lineage.AI_STATUS_ESCAPE); return; }
                    }
                }
            } else {
                if (warState != RobotWarState.IDLE) {
                    if (!isWarFC && !moveToTarget(o)) clearTarget();
                    else if (isWarFC && Util.getDistance(this, o) > 8) clearTarget(); 
                } else {
                    if (!moveToTarget(o)) clearTarget();
                }
                
                if (warState == RobotWarState.IDLE && pcrobot_mode == PCROBOT_MODE.Cracker && currentAttackTarget == null) { goToHome(true); clearTarget(); }
            }
            if (warState == RobotWarState.IDLE && action.contains("사냥") && Util.random(0, 10) == 0) findTarget();
        } catch (Exception e) { e.printStackTrace(); }
    }

    protected void clearTarget() { target = currentAttackTarget = null; }
    protected void handlePotions() { if (action.contains("사냥") || action.contains("PvP") || action.contains("공성")) { toHealingPotion(); toBuffPotion(); } }

    protected boolean moveToTarget(object o) {
        if (o == null) return false;
        return navigateTo(o.getX(), o.getY(), o.getMap(), false);
    }

    protected boolean canMoveTo(int fromX, int fromY, int toX, int toY) {
        return true; 
    }

    protected boolean shouldResetTarget(object o) {
        if (o instanceof Spartoi && o.getGfxMode() == 28) return true;
        if (o instanceof StoneGolem && o.getGfxMode() == 4) return true;
        if (o instanceof Harphy && o.getGfxMode() == 4) return true;
        return false;
    }

    protected object checkTargetValidity(object o) {
        if (o == null || o.isDead() || o.isWorldDelete() || !isAttack(o, false) || !Util.isAreaAttack(this, o) || !Util.isAreaAttack(o, this)) return null;
        return o;
    }

    @Override
    public void toAiEscape(long time) {
        super.toAiEscape(time);
        synchronized (this) { if (currentAttackTarget == null) { setAiStatus(Lineage.AI_STATUS_WALK); return; } }
        if (Util.random(1, 100) <= Lineage.robot_ment_probability) RobotController.getRandomMentAndChat(Lineage.AI_ESCAPE_MENT, this, currentAttackTarget, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ESCAPE_MENT_DELAY);
        synchronized (this) { heading = Util.oppositionHeading(this, currentAttackTarget); }
        int temp_heading = heading; boolean escaped = false;
        do {
            int x = Util.getXY(heading, true) + this.x; int y = Util.getXY(heading, false) + this.y;
            boolean canMove = World.isThroughObject(this.x, this.y, this.map, heading);
            synchronized (temp_list) {
                temp_list.clear(); findInsideList(x, y, temp_list);
                boolean hasObstacle = false;
                for (object obj : temp_list) { if (obj instanceof Character) { hasObstacle = true; break; } }
                if (canMove && !hasObstacle) { super.toMoving(x, y, heading); escaped = true; break; }
            }
            synchronized (this) { heading = (heading + 1) % 8; }
            if (temp_heading == heading) break;
        } while (true);
        synchronized (this) { if (escaped) target = currentAttackTarget = null; }
    }

    @Override protected void toAiDead(long time) { super.toAiDead(time); ai_time_temp_1 = 0; target = targetItem = tempTarget = currentAttackTarget = null; clearAstarList(); setAiStatus(Lineage.AI_STATUS_CORPSE); }
    @Override protected void toAiCorpse(long time) { super.toAiCorpse(time); if (ai_time_temp_1 == 0) ai_time_temp_1 = time; if (ai_time_temp_1 + Lineage.ai_robot_corpse_time > time) return; ai_time_temp_1 = 0; toReset(true); clearList(true); World.remove(this); setAiStatus(Lineage.AI_STATUS_SPAWN); }
    @Override protected void toAiSpawn(long time) { super.toAiSpawn(time); goToHome(false); toRevival(this); setAiStatus(Lineage.AI_STATUS_WALK); }
    
    /**
     * 🌟 바닥에 떨어진 아이템 줍기 (네비게이션 연동)
     */
    @Override 
    protected void toAiPickup(long time) {
        object o = target;
        if (o == null) { setAiStatus(Lineage.AI_STATUS_WALK); return; }
        
        if (Util.isDistance(this, o, 0)) { // 아이템 위로 올라갔다면 줍기 시도
            super.toAiPickup(time);
            synchronized (o.sync_pickup) { 
                if (!o.isWorldDelete()) { 
                    Inventory inv = getInventory(); 
                    if (inv != null) inv.toPickup(o, o.getCount()); 
                } 
            }
            target = null; 
            setAiStatus(Lineage.AI_STATUS_WALK);
        } else {
            navigateTo(o.getX(), o.getY(), o.getMap(), false);
        }
    }

    protected void toStay(long time) {
        switch (step) {
            case 0: goToHome(false); step = 1; break;
            case 1: setHeading(Util.random(0, 7)); step = 2; break;
            case 2: if (ai_time_temp_1 == 0) ai_time_temp_1 = time; if (ai_time_temp_1 + Util.random(1000 * 5, 1000 * 30) > time) return; ai_time_temp_1 = 0; step = 0; if (Util.random(1, 100) < 3) pcrobot_mode = PCROBOT_MODE.Stay; else pcrobot_mode = PCROBOT_MODE.None; break;
        }
    }

    protected void toPolymorph() {
        switch (step) {
            case 0: if (polyTime == 0) polyTime = System.currentTimeMillis() + (1000 * Util.random(1, 5)); if (polyTime > 0 && polyTime <= System.currentTimeMillis()) step = 1; break;
            case 1:
                ItemInstance polyScroll = getInventory().find(ScrollPolymorph.class); ItemInstance mythicBook = getInventory().findDbNameId(6492);
                boolean hasPolyScroll = polyScroll != null && polyScroll.getCount() > 0; boolean hasMythicBook = mythicBook != null && mythicBook.getCount() > 0;
                boolean useMythicPoly = getMythicPoly(); boolean usePolyScroll = !useMythicPoly;
                if (getRandomPoly()) { if (hasPolyScroll && hasMythicBook) { useMythicPoly = Util.random(0, 1) == 0; usePolyScroll = !useMythicPoly; } else if (hasMythicBook) { useMythicPoly = true; usePolyScroll = false; } else if (hasPolyScroll) { useMythicPoly = false; usePolyScroll = true; } }
                if (useMythicPoly && !hasMythicBook) { useMythicPoly = false; usePolyScroll = hasPolyScroll; } else if (usePolyScroll && !hasPolyScroll) { usePolyScroll = false; useMythicPoly = hasMythicBook; }
                if (!useMythicPoly && !usePolyScroll) { step = 0; polyTime = 0; pcrobot_mode = PCROBOT_MODE.None; return; }
                if (usePolyScroll) {
                    Poly p = PolyDatabase.getName(getPolymorph());
                    if (p != null && p.getMinLevel() <= getLevel()) {
                        PolyDatabase.toEquipped(this, p); setGfx(p.getGfxId());
                        if (Lineage.is_weapon_speed) { if (getInventory().getSlot(Lineage.SLOT_WEAPON) != null && SpriteFrameDatabase.findGfxMode(getGfx(), getGfxMode() + Lineage.GFX_MODE_ATTACK)) setGfxMode(getGfxMode()); else setGfxMode(getGfxMode()); } else { setGfxMode(getGfxMode()); }
                        BuffController.append(this, ShapeChange.clone(BuffController.getPool(ShapeChange.class), SkillDatabase.find(208), 7200));
                        toSender(S_ObjectPoly.clone(BasePacketPooling.getPool(S_ObjectPoly.class), this), true);
                        if (!polyScroll.getItem().getName().contains("무한")) getInventory().count(polyScroll, polyScroll.getCount() - 1, false);
                    }
                } else if (useMythicPoly) {
                    Poly p = PolyDatabase.getName(getRankPolyName());
                    if (p != null && getGfx() != p.getGfxId()) mythicBook.toClick(this, null);
                }
                step = 0; polyTime = 0; pcrobot_mode = PCROBOT_MODE.None; break;
        }
    }
   
    protected void toScrollPolymorph() { switch (step++) { case 0: goToHome(false); break; case 1: RobotController.getScrollPolymorph(this); break; case 2: step = 0; pcrobot_mode = PCROBOT_MODE.None; break; } }
    protected void toBadPolymorph() { switch(step++) { case 0: goToHome(true); break; case 1: ServerBasePacket sbp = (ServerBasePacket)ServerBasePacket.clone(BasePacketPooling.getPool(ServerBasePacket.class), null); sbp.writeC(0); sbp.writeC(0); byte[] data = sbp.getBytes(); BasePacketPooling.setPool(sbp); BasePacket bp = ClientBasePacket.clone(BasePacketPooling.getPool(ClientBasePacket.class), data, data.length); getInventory().find(ScrollPolymorph.class).toClick(this, (ClientBasePacket)bp); BasePacketPooling.setPool(bp); step = 0; pcrobot_mode = PCROBOT_MODE.None; break; } }
    public void toPolyRemove() { BuffController.remove(this, ShapeChange.class); setGfx(this.getClassGfx()); if (getInventory() != null && getInventory().getSlot(Lineage.SLOT_WEAPON) != null) setGfxMode(this.getClassGfxMode() + getInventory().getSlot(Lineage.SLOT_WEAPON).getItem().getGfxMode()); else setGfxMode(this.getClassGfxMode()); this.toSender(S_ObjectPoly.clone(BasePacketPooling.getPool(S_ObjectPoly.class), this), true); }
    protected void toInventoryHeavy() { switch (step++) { case 0: goToHome(false); break; case 1: for (ItemInstance ii : getInventory().getList()) { if (ii.getItem().getNameIdNumber() == 4) continue; if (ii.isEquipped()) continue; getInventory().remove(ii, false); } break; case 2: step = 0; pcrobot_mode = PCROBOT_MODE.None; break; } }

    public void addAttackList(object o) { if (!isDead() && !o.isDead() && o.getObjectId() != getObjectId()) { if (getClanId() > 0 && o.getClanId() > 0 && getClanId() != o.getClanId()) appendAttackList(o); else if (getClanId() == 0 || o.getClanId() == 0) appendAttackList(o); } }

    public boolean isAttack(object o, boolean walk) {
        if (o == null || o.getGm() > 0 || o.isDead() || o.isWorldDelete() || "$441".equals(o.getName()) || "$2932 $2928".equals(o.getName())) return false;
        
        if (o.isBuffAbsoluteBarrier()) {
            if ((o instanceof PcInstance || o instanceof PcRobotInstance) && Util.random(1, 100) <= Lineage.robot_ment_probability) RobotController.getRandomMentAndChat(Lineage.AI_ABSOLUTE_MENT, this, o, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_ABSOLUTE_MENT_DELAY);
            return false;
        }
        if (!Util.isDistance(this, o, Lineage.SEARCH_WORLD_LOCATION)) return false;

        if (warState != RobotWarState.IDLE) {
            Clan c = ClanController.find(this);
            Kingdom k = KingdomController.find(getWarCastleUid());
            
            if (k != null && k.getClanId() != 0 && this.getClanId() != 0 && k.getClanId() != this.getClanId()) {
                if (o instanceof KingdomDoor) return true;
            }
            
            if (o instanceof RobotInstance || o instanceof PcInstance) { 
                if (getClanId() > 0 && o.getClanId() > 0 && getClanId() == o.getClanId()) return false; 
                if (RobotController.isCastleInsideCoords(this, getWarCastleUid())) return true;
            }

            if (k != null && k.getClanId() != 0 && this.getClanId() != 0 && k.getClanId() == this.getClanId()) {
                if (o instanceof KingdomCastleTop || o instanceof KingdomDoor || o instanceof KingdomCrown) return false;
            } else if (k == null && isKing()) {
                if (c == null) return false; 
                if (o instanceof KingdomCrown) return true;
            }
            return true; 
        }

        if (o instanceof Cracker && action != null && action.equalsIgnoreCase("허수아비 공격")) return true;
        if (World.isSafetyZone(getX(), getY(), getMap()) && !(o instanceof MonsterInstance)) return false;
        if (o instanceof RobotInstance || o instanceof PcInstance) { if (getClanId() > 0 && o.getClanId() > 0 && getClanId() == o.getClanId()) return false; }
        if (o instanceof KingdomCrown) {
            Clan c = ClanController.find(this);
            if (c != null && !c.getLord().equalsIgnoreCase(getName())) return false;
        }
        if (o instanceof TeleportInstance || o instanceof EventInstance || o instanceof InnInstance || o instanceof ShopInstance || o instanceof DwarfInstance || o instanceof PetMasterInstance) return false;
        if (o instanceof PcInstance && !(o instanceof RobotInstance)) { if (o.isBuffCriminal() || o.getLawful() < Lineage.NEUTRAL) return true; }
        if (o instanceof SummonInstance || (o instanceof NpcInstance && !(o instanceof GuardInstance))) return false;
        if (o instanceof ItemInstance || o instanceof BackgroundInstance || o instanceof MagicDollInstance) return false;
        if (!(o instanceof MonsterInstance) && getX() == o.getX() && getY() == o.getY() && getMap() == o.getMap()) return false;
        if (o != null && "$607".equals(o.getName())) return false;
        if (shouldResetTarget(o)) return false;
        return true;
    }

    public boolean toMoving(object primary, final int x, final int y, final int h, final boolean astar, final boolean ignoreObjects) {
        if (!RobotMoving.isMoveValid(this, lastMovingTime, x, y)) return false;
        boolean moved = false;
        if (astar) {
            try {
                if (aStar != null) {
                    aStar.cleanTail();
                    int dist = Util.getDistance(this.x, this.y, x, y);
                    if (dist > TELEPORT_THRESHOLD_DISTANCE && Util.random(0, 100) < 50) { if (randomTeleport()) return true; }
                    if (dist <= 2 && World.isThroughObject(this.x, this.y, this.map, Util.calcheading(this.x, this.y, x, y))) { toMoving(x, y, Util.calcheading(this.x, this.y, x, y)); moved = true; } else {
                        tail = aStar.searchTail(this, x, y, ignoreObjects);
                        if (tail != null) {
                            int pathLength = 0; Node current = tail; while (current != null) { pathLength++; current = current.prev; }
                            if (pathLength > TELEPORT_THRESHOLD_DISTANCE) { if (randomTeleport()) return true; }
                            while (tail != null) { if (tail.x == getX() && tail.y == getY()) break; iPath[0] = tail.x; iPath[1] = tail.y; tail = tail.prev; }
                            toMoving(iPath[0], iPath[1], Util.calcheading(this.x, this.y, iPath[0], iPath[1])); moved = true;
                        } else { if (primary != null) astarIgnore.add(primary); }
                    }
                } else tail = null;
            } catch (Exception e) {}
        } else { toMoving(x, y, h); moved = true; }
        return moved;
    }

    @Override
    public void toMoving(int x, int y, int h) {
        if (!RobotMoving.isMoveValid(this, lastMovingTime, x, y)) {
            this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
            return;
        }

        if (isTileOccupied(x, y, getMap())) {
            int altHead = (h + (Util.random(0, 1) == 0 ? 1 : -1) + 8) % 8;
            setHeading(altHead);
            this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
            return;
        }

        super.toMoving(x, y, h);
        lastMovingTime = System.currentTimeMillis();
        
        this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
    }
    
    protected boolean toBuffPotion() {
        Buff b = BuffController.find(this); if (b == null) return false;
        if (b.find(HastePotionMagic.class) == null) { ItemInstance item = getInventory().find(HastePotion.class); if (item != null && item.isClick(this)) { item.toClick(this, null); return true; } }
        if ((getClassType() == Lineage.LINEAGE_CLASS_KNIGHT || getClassType() == Lineage.LINEAGE_CLASS_ROYAL) && b.find(Bravery.class) == null) { ItemInstance item = getInventory().find(BraveryPotion.class); if (item != null && item.isClick(this)) { item.toClick(this, null); return true; } }
        if (getClassType() == Lineage.LINEAGE_CLASS_ELF && b.find(Wafer.class) == null) { ItemInstance item = getInventory().find(BraveryPotion.class); if (item != null && item.isClick(this)) { item.toClick(this, null); return true; } }
        if (getClassType() == Lineage.LINEAGE_CLASS_WIZARD && b.find(HolyWalk.class) == null) { ItemInstance item = getInventory().find(BraveryPotion.class); if (item != null && item.isClick(this)) { item.toClick(this, null); return true; } }
        if (getInventory() != null && getInventory().getSlot(Lineage.SLOT_ARMOR) != null && getInventory().getSlot(Lineage.SLOT_WEAPON) != null) { if (b.find(DecreaseWeight.class) == null || b.find(EnchantDexterity.class) == null || b.find(EnchantMighty.class) == null || b.find(BlessWeapon.class) == null) { ItemInstance item = getInventory().find(Buff_potion.class); if (item != null && item.isClick(this)) { item.toClick(this, null); return true; } } }
        return false;
    }

    protected boolean toHealingPotion() { if (getHpPercent() > HEALING_PERCENT) return false; ItemInstance item = getInventory().find(HealingPotion.class); if (item != null && item.isClick(this)) item.toClick(this, null); return true; }

    protected boolean toSkillAttack(object o) {
        if (this == null || o == null) return false;
        List<Skill> list = SkillController.find(this); ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
        if (list == null || System.currentTimeMillis() < delay_magic || (getMpPercent() < USABLE_MP_PERCENT && Util.random(0, 100) <= 30) || o.isDead()) return false;
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s; if (sr == null || (sr.getType().equalsIgnoreCase("단일공격마법") == false && sr.getType().equalsIgnoreCase("범위공격마법") == false && sr.getType().equalsIgnoreCase("디버프") == false) || sr.getLevel() > getLevel()) continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) { if (weapon == null) continue; switch (sr.getWeaponType()) { case "한손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand()) continue; break; case "양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand()) continue; break; case "한손검&양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword")) continue; break; case "활": if (!weapon.getItem().getType2().equalsIgnoreCase("bow")) continue; break; } }
            if (!sr.getTarget().equalsIgnoreCase("유저&몬스터")) { switch (sr.getTarget()) { case "유저": if (o instanceof MonsterInstance) continue; break; case "몬스터": if (o instanceof PcInstance) continue; break; } }
            if ((sr.getAttribute() > 0 && getAttribute() != sr.getAttribute()) || sr.getMpConsume() > getNowMp()) continue;
            if (Math.random() < sr.getProbability()) { toSkill(s, o); return true; }
        } return false;
    }

    protected boolean toSkillBuff(List<Skill> list) {
        if (list == null) return false; ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s; if (sr.getType().equalsIgnoreCase("버프마법") == false || sr.getLevel() > getLevel() || sr.getMpConsume() > getNowMp() || (sr.getUid() == 43 && BuffController.find(this, SkillDatabase.find(311)) != null) || BuffController.find(this, s) != null) continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) { if (weapon == null) continue; switch (sr.getWeaponType()) { case "한손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand()) continue; break; case "양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand()) continue; break; case "한손검&양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword")) continue; break; case "활": if (!weapon.getItem().getType2().equalsIgnoreCase("bow")) continue; break; } }
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute()) continue;
            if (Math.random() < sr.getProbability()) { toSkill(s, this); return true; }
        } return false;
    }

    protected boolean toSkillHealMp(List<Skill> list) {
        if (getNowMp() == getTotalMp() || list == null) return false; ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s; if (sr.getType().equalsIgnoreCase("mp회복마법") == false || sr.getLevel() > getLevel()) continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) { if (weapon == null) continue; switch (sr.getWeaponType()) { case "한손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand()) continue; break; case "양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand()) continue; break; case "한손검&양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword")) continue; break; case "활": if (!weapon.getItem().getType2().equalsIgnoreCase("bow")) continue; break; } }
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute()) continue;
            if (Math.random() < sr.getProbability()) { toSkill(s, this); return true; }
        } return false;
    }

    protected boolean toSkillHealHp(List<Skill> list) {
        if (getHpPercent() > HEALING_PERCENT || list == null) return false; ItemInstance weapon = getInventory().getSlot(Lineage.SLOT_WEAPON);
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s; if (sr.getType().equalsIgnoreCase("힐") == false || sr.getLevel() > getLevel() || sr.getMpConsume() > getNowMp()) continue;
            if (!sr.getWeaponType().equalsIgnoreCase("모든무기")) { if (weapon == null) continue; switch (sr.getWeaponType()) { case "한손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") || weapon.getItem().isTohand()) continue; break; case "양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("tohandsword") || !weapon.getItem().isTohand()) continue; break; case "한손검&양손검": if (!weapon.getItem().getType2().equalsIgnoreCase("sword") && !weapon.getItem().getType2().equalsIgnoreCase("tohandsword")) continue; break; case "활": if (!weapon.getItem().getType2().equalsIgnoreCase("bow")) continue; break; } }
            if (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute()) continue;
            if (Math.random() < sr.getProbability()) { toSkill(s, this); return true; }
        } return false;
    }

    protected boolean toSkillSummon(List<Skill> list) {
        if (list == null) return false;
        for (Skill s : list) {
            SkillRobot sr = (SkillRobot) s; if (sr.getType().equalsIgnoreCase("서먼몬스터") == false || sr.getLevel() > getLevel() || sr.getMpConsume() > getNowMp() || (sr.getAttribute() > 0 && getAttribute() != sr.getAttribute())) continue;
            if (Math.random() < sr.getProbability() && SummonController.isAppend(SummonListDatabase.summon(this, 0), this, getClassType() == Lineage.LINEAGE_CLASS_WIZARD ? TYPE.MONSTER : TYPE.ELEMENTAL)) { toSkill(s, this); SummonController.find(this).setMode(SummonInstance.SUMMON_MODE.AggressiveMode); return true; }
        } return false;
    }
    
    protected boolean toBuffSummon() {
        Summon s = SummonController.find(this); if (s == null || s.getSize() == 0) return false;
        for (object o : s.getList()) {
            Buff b = BuffController.find(o);
            if (b == null || b.find(Haste.class) == null) { Skill haste = SkillController.find(this, 6, 2); if (haste != null && haste.getMpConsume() <= getNowMp()) { toSkill(haste, o); return true; } }
            Character cha = (Character) o;
            if (cha.getHpPercent() <= HEALING_PERCENT) { int[][] heal_list = { { 1, 0 }, { 3, 2 }, { 5, 2 }, { 7, 0 }, { 8, 0 }, { 20, 5 } }; for (int[] data : heal_list) { Skill heal = SkillController.find(this, data[0], data[1]); if (heal != null && heal.getMpConsume() <= getNowMp()) { toSkill(heal, o); return true; } } }
        } return false;
    }

    public void toSkill(Skill s, object o) {
        ServerBasePacket sbp = (ServerBasePacket) ServerBasePacket.clone(BasePacketPooling.getPool(ServerBasePacket.class), null);
        sbp.writeC(0); sbp.writeC(s.getSkillLevel() - 1); sbp.writeC(s.getSkillNumber()); sbp.writeD(o.getObjectId()); sbp.writeH(o.getX()); sbp.writeH(o.getY()); byte[] data = sbp.getBytes(); BasePacketPooling.setPool(sbp);
        BasePacket bp = ClientBasePacket.clone(BasePacketPooling.getPool(ClientBasePacket.class), data, data.length); SkillController.toSkill(this, (ClientBasePacket) bp); BasePacketPooling.setPool(bp);
    }
    
    public boolean toSkillExternal(int skillId, object targetObj) {
        if (System.currentTimeMillis() < this.delay_magic) return false;
        Skill s = SkillDatabase.find(skillId);
        if (s != null && s.getMpConsume() <= this.getNowMp()) { this.setHeading(Util.calcheading(this, targetObj.getX(), targetObj.getY())); this.toSkill(s, targetObj); this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, this.getGfx(), this.getGfxMode() + Lineage.GFX_MODE_SPELL_NO_DIRECTION); return true; }
        return false;
    }

    protected void goToHome(boolean isCracker) {
        if (!LocationController.isTeleportVerrYedHoraeZone(this, true) || (!isCracker && World.isGiranHome(getX(), getY(), getMap()))) return;
        target = targetItem = tempTarget = currentAttackTarget = null; clearAstarList();
        int[] home = Lineage.getHomeXY(); 
        setHomeX(home[0]); setHomeY(home[1]); setHomeMap(home[2]);
        toSafeTeleport(getHomeX(), getHomeY(), getHomeMap());
    }

    protected String getPolymorph() {
        RobotPoly rp = null; if (RobotController.getPolyList().size() < 1) return "";
        for (int i = 0; i < 200; i++) {
            rp = RobotController.getPolyList().get(Util.random(0, RobotController.getPolyList().size() - 1));
            if (rp != null && rp.getPoly().getMinLevel() <= getLevel() && SpriteFrameDatabase.findGfxMode(rp.getPoly().getGfxId(), getGfxMode() + Lineage.GFX_MODE_ATTACK)) {
                switch (rp.getPolyClass()) { case "모든클래스": return rp.getPoly().getName(); case "군주": if (getClassType() == Lineage.LINEAGE_CLASS_ROYAL) return rp.getPoly().getName(); else continue; case "기사": if (getClassType() == Lineage.LINEAGE_CLASS_KNIGHT) return rp.getPoly().getName(); else continue; case "요정": if (getClassType() == Lineage.LINEAGE_CLASS_ELF) return rp.getPoly().getName(); else continue; case "마법사": if (getClassType() == Lineage.LINEAGE_CLASS_WIZARD) return rp.getPoly().getName(); else continue; case "군주&기사&마법사": if (getClassType() == Lineage.LINEAGE_CLASS_ROYAL || getClassType() == Lineage.LINEAGE_CLASS_KNIGHT || getClassType() == Lineage.LINEAGE_CLASS_WIZARD) return rp.getPoly().getName(); else continue; }
            }
        } return "";
    }

    protected boolean isBadPolymorph() { Poly p = PolyDatabase.getPolyName( getPolymorph() ); return p!=null && getGfx()!=p.getGfxId() && getGfx()!=getClassGfx(); }
    protected void setArrow() { if (getInventory() != null && getInventory().find(Arrow.class) != null && !getInventory().find(Arrow.class).equipped) getInventory().find(Arrow.class).toClick(this, null); }
    protected void setInventory() {
        if (Lineage.robot_auto_pc && (this.getWeapon_name() != null || RobotController.getWeapon(getClassType()) != null)) {
            weapon = this.getWeapon_name() != null ? ItemDatabase.find(this.getWeapon_name()) : RobotController.getWeapon(getClassType());
            ItemInstance item = ItemDatabase.newInstance(weapon); item.setObjectId(ServerDatabase.nextEtcObjId()); item.setEnLevel(weaponEn); getInventory().append(item, false); item.toClick(this, null);
        }
        if (Lineage.robot_auto_pc && this.getDoll_name() != null) { doll = ItemDatabase.find(this.getDoll_name()); ItemInstance item = ItemDatabase.newInstance(doll); if (item != null) { item.setObjectId(ServerDatabase.nextEtcObjId()); getInventory().append(item, false); item.toClick(this, null); } }
        if (Lineage.robot_auto_pc) { RobotController.getHealingPotion(this); RobotController.getHastePotion(this); RobotController.getBraveryPotion(this); RobotController.getElvenWafer(this); RobotController.getScrollPolymorph(this); RobotController.getArrow(this); }
    }
    protected boolean isWait() {
        goToHome(false);
        if (Util.random(0, 99) < 50) { pcrobot_mode = PCROBOT_MODE.Stay; } else {
            int x = Util.getXY(getHeading(), true) + getX(); int y = Util.getXY(getHeading(), false) + getY();
            boolean tail = World.isThroughObject(getX(), getY(), getMap(), getHeading()) && !World.isMapdynamic(x, y, map);
            if (tail && Util.random(0, 99) < 5) toMoving(null, x, y, getHeading(), false, false); else if (Util.random(0, 99) < 10) setHeading(Util.random(0, 7));
        } return true;
    }
    protected void attackCracker() {
        goToHome(false); pcrobot_mode = PCROBOT_MODE.Cracker; target = targetItem = tempTarget = currentAttackTarget = null; clearAstarList();
        boolean isCracker = false; for (object cracker : BackgroundDatabase.getCrackerList()) { if (target == null) { target = cracker; isCracker = true; } }
        if (isCracker) setAiStatus(Lineage.AI_STATUS_WALK); if (target == null) isWait();
    }
    public boolean isPossibleMap() {
        try { List<Book> list = BookController.find(this); if (list == null || list.isEmpty()) return false; for (Book b : list) { if (b != null && b.getEnable() && b.getMap() == this.getMap() && b.getMinLevel() <= getLevel()) return true; } } catch (Exception e) { return false; } return false;
    }
    public int countClanMembersNearby() {
        Clan c = ClanController.find(this); if (c == null) return 0; int count = 0;
        for (object o : getInsideList()) { if (o instanceof PcInstance && c.containsMemberList(((PcInstance) o).getName())) count++; } return count;
    }
    public void toDamage(Character cha) { if (cha == null || cha.getObjectId() == this.getObjectId() || cha.getGm() > 0) return; if (currentAttackTarget != null) { if (currentAttackTarget.isDead() || Util.getDistance(this, currentAttackTarget) > 15) currentAttackTarget = null; else return; } if (Util.getDistance(this, cha) <= 10 && !isTargetOvercrowded(cha)) { setTarget(cha); currentAttackTarget = cha; } }
    @Override public void setExp(double exp) { double currentExp = this.getExp(); double gainedExp = exp - currentExp; double finalExp = exp; if (gainedExp > 0) finalExp = currentExp + Math.floor(gainedExp * 0.58); int oldLevel = this.getLevel(); super.setExp(finalExp); if (this.getLevel() > oldLevel) { this.setNowHp(this.getMaxHp()); this.setNowMp(this.getMaxMp()); try { this.toSender(S_ObjectEffect.clone(BasePacketPooling.getPool(S_ObjectEffect.class), this, 3760)); } catch (Exception e) {} toTeleportToAppropriateHuntingGround(); saveRobotLevelToDb(); } }
    protected void saveRobotLevelToDb() {
        Connection con = null; PreparedStatement pstm = null;
        try { con = DatabaseConnection.getLineage(); pstm = con.prepareStatement("UPDATE _robot SET level=?, exp=?, lawful=? WHERE name=?"); pstm.setInt(1, this.getLevel()); pstm.setLong(2, (long) this.getExp()); pstm.setInt(3, this.getLawful()); pstm.setString(4, this.getName()); pstm.executeUpdate(); } catch (Exception e) {} finally { DatabaseConnection.close(con, pstm, null); }
    }
    protected void autoPartyManagement() {
        if (getPartyId() == 0) {
            if (!isUpperPercentLevel(15)) return;
            if (Util.random(0, 100) < 30) {
                for (object obj : getInsideList()) { if (obj instanceof RobotInstance && obj != this) { PcInstance target = (PcInstance) obj; if (target.getPartyId() == 0) { if (partyWaitStartTime == 0) partyWaitStartTime = System.currentTimeMillis(); Party party = PartyController.find(this); if (party == null || party.getList().size() < 4) { broadcastRobotChat(target.getName() + "님, 사냥 같이 가시죠!"); PartyController.toParty(this, target); break; } } } }
            }
            if (partyWaitStartTime > 0 && (System.currentTimeMillis() - partyWaitStartTime) > 60000) { partyWaitStartTime = 0; if (LocationController.isTeleportZone(this, true, false)) randomTeleport(); else toTeleportToAppropriateHuntingGround(); }
        } else partyWaitStartTime = 0;
    }
    protected void handlePartyCollaboration() {
        Party party = PartyController.find(this); if (party == null) return; PcInstance leader = party.getMaster(); if (leader == null || leader == this) return;
        if (this.partyManager != null && this.partyManager.isPartyInDanger(this)) { ChattingController.toChatting(this, "전원 후퇴!", Lineage.CHATTING_MODE_NORMAL); setDelayedAction(2000, () -> { if (LocationController.isTeleportZone(this, true, false)) { int[] home = Lineage.getHomeXY(); toSafeTeleport(home[0], home[1], home[2]); } else goToHome(false); }); return; }
        RobotPartyManager.PartyMode mode = RobotPartyManager.PartyMode.NORMAL; if (this.partyManager != null) mode = this.partyManager.determinePartyMode(this, leader);
        switch (mode) { 
            case GATHER: 
                toSafeTeleport(leader.getX(), leader.getY(), leader.getMap()); 
                return; 
            case WAIT: 
            case SUPPORT: 
                if (Util.getDistance(this, leader) > 2) { 
                    int head = Util.calcheading(this, leader.getX(), leader.getY()); 
                    int nextX = this.getX() + Util.getXY(head, true); 
                    int nextY = this.getY() + Util.getXY(head, false); 
                    if (World.isThroughObject(this.getX(), this.getY(), this.getMap(), head)) toMoving(this, nextX, nextY, head, false, false); 
                    else { int altHead = (head + Util.random(1, 2) * (Util.random(0, 1) == 0 ? 1 : -1) + 8) % 8; toMoving(this, this.getX() + Util.getXY(altHead, true), this.getY() + Util.getXY(altHead, false), altHead, false, false); } 
                } 
                if (mode == RobotPartyManager.PartyMode.WAIT) return; 
                break; 
            case NORMAL: 
            default: break; 
        }
        object leaderTarget = leader instanceof PcRobotInstance ? ((PcRobotInstance) leader).getTarget() : leader.getTarget();
        if (leaderTarget != null && isAttack(leaderTarget, true) && this.currentAttackTarget != leaderTarget) { synchronized (this) { setTarget(leaderTarget); setAiStatus(Lineage.AI_STATUS_ATTACK); this.currentAttackTarget = leaderTarget; } }
    }
    
    protected boolean isUpperPercentLevel(int percent) { List<RobotInstance> allRobots = World.getRobotList(); if (allRobots.size() < 2) return true; List<RobotInstance> sortedRobots = new ArrayList<>(allRobots); sortedRobots.sort((r1, r2) -> Integer.compare(r2.getLevel(), r1.getLevel())); int cutoffIndex = Math.max(0, (int) (sortedRobots.size() * (percent / 100.0)) - 1); return this.getLevel() >= sortedRobots.get(cutoffIndex).getLevel(); }
    protected boolean checkAndMoveToNextFloor() {
        if (System.currentTimeMillis() - lastFloorCheckTime < 10000) return false; lastFloorCheckTime = System.currentTimeMillis();
        int currentPop = 0; for (PcInstance pc : World.getPcList()) { if (pc.getMap() == this.getMap()) currentPop++; } for (RobotInstance robot : World.getRobotList()) { if (robot.getMap() == this.getMap()) currentPop++; }
        if (currentPop > 20) { String currentName = getCurrentMapName(); if (currentName == null || currentName.isEmpty()) return false; String prefix = currentName.replaceAll("\\s*\\d+층.*", "").trim(); int[] nextLoc = getNextFloorFromItemTeleport(prefix, currentName); if (nextLoc != null) { ChattingController.toChatting(this, "사람이 너무 많네요. 다음 층으로 이동합니다!", Lineage.CHATTING_MODE_NORMAL); Party party = PartyController.find(this); boolean isLeader = (party != null && party.getMaster() == this); int nextX = nextLoc[0]; int nextY = nextLoc[1]; int nextMap = nextLoc[2]; setDelayedAction(2000, () -> { toTeleport(nextX, nextY, nextMap, true); if (isLeader) { for (PcInstance member : party.getList()) { if (member != this && (member instanceof RobotInstance)) member.toTeleport(nextX, nextY, nextMap, true); } } }); return true; } } return false;
    }
    protected String getCurrentMapName() { List<ItemTeleport> teleportList = ItemTeleportDatabase.getList(); if (teleportList != null) { for (ItemTeleport tp : teleportList) { if (tp.getMap() == this.getMap()) return tp.getName(); } } return null; }
    protected boolean isValidDungeonFloor() { String currentMapName = getCurrentMapName(); if (currentMapName == null) return false; String prefix = currentMapName.replaceAll("\\s*\\d+층.*", "").trim(); List<Book> list = BookController.find(this); if (list != null) { for (Book b : list) { if (b.getLocation() != null && b.getLocation().startsWith(prefix)) return true; } } return false; }
    protected int[] getNextFloorFromItemTeleport(String prefix, String currentName) { List<ItemTeleport> teleportList = ItemTeleportDatabase.getList(); if (teleportList == null) return null; int currentFloor = extractFloor(currentName); ItemTeleport nextFloorTp = null; int minNextFloor = Integer.MAX_VALUE; for (ItemTeleport tp : teleportList) { String tpName = tp.getName(); if (tpName != null && tpName.startsWith(prefix)) { int tpFloor = extractFloor(tpName); if (tpFloor > currentFloor && tpFloor < minNextFloor) { minNextFloor = tpFloor; nextFloorTp = tp; } } } if (nextFloorTp != null) return new int[]{ nextFloorTp.getX(), nextFloorTp.getY(), nextFloorTp.getMap() }; return null; }
    protected int extractFloor(String name) { try { String numberOnly = name.replaceAll("[^0-9]", ""); if (numberOnly.length() > 0) return Integer.parseInt(numberOnly); } catch (Exception e) {} return 0; }
    public boolean isInVillage() { return (this.getX() == this.getHomeX() && this.getY() == this.getHomeY() && this.getMap() == this.getHomeMap()) || World.isGiranHome(getX(), getY(), getMap()); }
    public void initRobotAI() {
        this.huntingEvaluator = new RobotHuntingEvaluator(); this.partyManager = new RobotPartyManager();
        switch (this.getClassType()) { case Lineage.LINEAGE_CLASS_ROYAL: this.classBehavior = new BehaviorPrince(); break; case Lineage.LINEAGE_CLASS_KNIGHT: this.classBehavior = new BehaviorKnight(); break; case Lineage.LINEAGE_CLASS_ELF: this.classBehavior = new BehaviorElf(); break; case Lineage.LINEAGE_CLASS_WIZARD: this.classBehavior = new BehaviorWizard(); break; default: this.classBehavior = new BehaviorKnight(); break; }
    }
    protected void setDelayedAction(long delayMs, Runnable action) { this.delayedActionTime = System.currentTimeMillis() + delayMs; this.delayedAction = action; this.target = null; this.currentAttackTarget = null; }
    protected boolean isTargetOvercrowded(object target) {
        int attackerCount = 0;
        for (RobotInstance r : World.getRobotList()) {
            if (r instanceof PcRobotInstance) {
                PcRobotInstance robot = (PcRobotInstance) r;
                if (robot.currentAttackTarget == target) {
                    attackerCount++;
                }
            }
        }
        return attackerCount >= 3;
    }
    
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
                return true;
            }
        }
        return false;
    }

    public static boolean toKingdomWarCheck() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar()) {return true; }
        } return false; }

    public String getWarCastleName() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar()) { return k.getName(); }
        } return null; }
    
    public static int getWarCastleUid() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar()) {return k.getUid(); }
        } return -1; }

    public static boolean isCastleTopDead() {
        for (Kingdom k : KingdomController.getList()) {
            if (k.isWar() && k.isCastleTopDead()) {
                return true; 
            }
        } return false; }
    
    /**
     * 🌟 평시 사냥터 도보 탐색 (자연스러운 회피 이동)
     */
    protected void moveToRandomLocation(long time) {
        try {
            int currentHeading = getHeading();
            List<Integer> directionsToTry = new ArrayList<>();
            directionsToTry.add(currentHeading);
            for (int i = 1; i <= 3; i++) {
                directionsToTry.add((currentHeading + i) % 8);
                directionsToTry.add((currentHeading - i + 8) % 8);
            }
            directionsToTry.add((currentHeading + 4) % 8);

            boolean moved = false;
            for (int headingTry : directionsToTry) {
                int x = Util.getXY(headingTry, true) + this.x;
                int y = Util.getXY(headingTry, false) + this.y;
                
                if (!Util.isDistance(x, y, map, start_x, start_y, map, 60)) {
                    if (teleportTime < time) {
                        toTeleport(start_x, start_y, start_map, true);
                        return;
                    }
                }
                boolean canMove = World.isThroughObject(this.x, this.y, this.map, headingTry) 
                               && !World.isNotMovingTile(x, y, map) 
                               && !isTileOccupied(x, y, map);
                               
                if (canMove) {
                    setHeading(headingTry);
                    this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                    toMoving(this, x, y, headingTry, true, false);
                    teleportTime = System.currentTimeMillis() + Util.random(1000, 3000);
                    moved = true;
                    checkTargetDuringMove();
                    break;
                }
            }
            
            if (!moved) {
                if ((getMap() == 0 || getMap() == 4) && teleportTime < time) toTeleport(start_x, start_y, start_map, true);
                else {
                    setHeading(Util.random(0, 7)); // 텔레포트 불가 시 제자리에서 방향 전환
                    this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                }
            }
        } catch (Exception e) {}
    }
    
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
    
    protected void handleDoors(Kingdom k, boolean isLord) {
        List<KingdomDoor> doors = k.getListDoor();
        if (doors == null || doors.isEmpty()) return;

        KingdomDoor targetDoor = null;
        int minDist = Integer.MAX_VALUE;
        
        for (KingdomDoor door : doors) {
            if (door != null && !door.isDead() && door.getNpc() != null) {
                int dist = Util.getDistance(this.getX(), this.getY(), door.getX(), door.getY());
                if (dist < minDist) {
                    minDist = dist;
                    targetDoor = door;
                }
            }
        }

        if (targetDoor != null) {
            synchronized (this) {
                this.target = targetDoor;
                this.currentAttackTarget = targetDoor;
            }
            
            if (targetDoor.isDead() && lineage.world.controller.KingdomController.isKingdomLocation(this, k.getUid())) {
                String doorName = targetDoor.getNpc().getName();
                if (k.isFirstDoorDestruction(doorName)) {
                    lineage.world.controller.RobotController.getRandomMentAndChat(Lineage.AI_OUTDOOR_MENT, this, targetDoor, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_OUTDOOR_MENT_DELAY);
                    k.markDoorAsDestroyed(doorName);
                }
            }

            if (minDist > 1) { 
                moveToCastleLogic(targetDoor.getX(), targetDoor.getY(), isLord);
            }
        }
    }

    protected synchronized void checkAndAnnounceDoorDestruction(KingdomDoor door, Kingdom kingdom) {
        String doorName = door.getNpc().getName();
        if (kingdom.isFirstDoorDestruction(doorName)) {
            lineage.world.controller.RobotController.getRandomMentAndChat(Lineage.AI_OUTDOOR_MENT, this, door, Lineage.CHATTING_MODE_NORMAL, Lineage.AI_OUTDOOR_MENT_DELAY);
            kingdom.markDoorAsDestroyed(doorName);
        }
    }

    protected void checkDoor(KingdomDoor door, Kingdom k, boolean isLord) {
        if (door.isDead() && lineage.world.controller.KingdomController.isKingdomLocation(this, k.getUid())) {
            checkAndAnnounceDoorDestruction(door, k);
        }
        synchronized (this) {
            if (!door.isDead() || !lineage.world.controller.KingdomController.isKingdomLocation(this, k.getUid())) {
                target = door;
                return;
            }
        }
        
        String doorName = door.getNpc().getName();
        int kingdomId = k.getUid();
        
        switch (kingdomId) {
            case 1: // 켄트성
                if (k.getName().equalsIgnoreCase("켄트성") && doorName.equalsIgnoreCase("[켄트] 외성문 7시") && lineage.world.controller.RobotController.isKingdomOutDoor04Location(this, kingdomId)) {
                    moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_OUTSIDE_04_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_OUTSIDE_04_COORDS[kingdomId][2], isLord);
                } else if (!lineage.world.controller.RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                    moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][2], isLord);
                }
                break;
            case 2: // 오크 요새
                if (k.getName().equalsIgnoreCase("오크 요새") && doorName.equalsIgnoreCase("[오크성] 외성문 4시") && !lineage.world.controller.RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                    moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][2], isLord);
                }
                break;
            case 3: // 윈다우드
                if (k.getName().equalsIgnoreCase("윈다우드") && doorName.equalsIgnoreCase("[윈다우드] 외성문 7시") && lineage.world.controller.RobotController.isKingdomOutDoor04Location(this, kingdomId)) {
                    moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][2], false);
                }
                break;
            case 4: // 기란성
                if (k.getName().equalsIgnoreCase("기란 성")) {
                    if (doorName.equalsIgnoreCase("[기란성] 외성문 4시 외부") && lineage.world.controller.RobotController.isKingdomOutDoor04Location(this, kingdomId)) {
                        moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_OUTSIDE_04_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_OUTSIDE_04_COORDS[kingdomId][2], isLord);
                    } else if (doorName.equalsIgnoreCase("[기란성] 외성문 8시 외부") && lineage.world.controller.RobotController.isKingdomOutDoor08Location(this, kingdomId)) {
                        moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_OUTSIDE_08_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_OUTSIDE_08_COORDS[kingdomId][2], isLord);
                    } else if ((doorName.equalsIgnoreCase("[기란성] 외성문 4시 내부") || doorName.equalsIgnoreCase("[기란성] 외성문 8시 내부")) && !lineage.world.controller.RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                        moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][2], isLord);
                    }
                }
                break;
            case 5: // 하이네성
                if (k.getName().equalsIgnoreCase("하이네 성")) {
                    int entryIndex = -1;
                    if (!lineage.world.controller.RobotController.isCastleInsideCoords(this, kingdomId)) {
                        if (doorName.equalsIgnoreCase("[하이네] 외성문 5시")) entryIndex = 0;
                        else if (doorName.equalsIgnoreCase("[하이네] 외성문 11시")) entryIndex = 1;

                        if (entryIndex != -1 && !lineage.world.controller.RobotController.isHeineEntryZone(this, entryIndex)) {
                            moveToCastleLogic(lineage.world.controller.RobotController.KINGDOM_HEINE_ENTRY_ZONES[entryIndex][0], lineage.world.controller.RobotController.KINGDOM_HEINE_ENTRY_ZONES[entryIndex][2], true);
                            return; 
                        }
                        if (entryIndex != -1 && lineage.world.controller.RobotController.isHeineEntryZone(this, entryIndex)) {
                            teleportToEscape();
                            return; 
                        }
                    }
                    if (!lineage.world.controller.RobotController.isCastleTopInsideCoords(this, kingdomId)) {
                        moveToCastleLogic(lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][0], lineage.world.controller.RobotController.CASTLE_TOP_INSIDE_COORDS[kingdomId][2], true);
                        return; 
                    }
                }
                break;
        }
    }

    protected void teleportToEscape() {
        int attempts = 20;
        while (attempts-- > 0) {
            int locX = Util.random(lineage.world.controller.RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][0], lineage.world.controller.RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][1]);
            int locY = Util.random(lineage.world.controller.RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][2], lineage.world.controller.RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][3]);
            int locMap = lineage.world.controller.RobotController.KINGDOM_HEINE_ESCAPE_TARGETS[0][4];
            int locHead = this.getHeading();
            if (World.getMapdynamic(locX, locY, locMap) == 0 && World.isThroughObject(locX, locY, locMap, locHead) && !World.isNotMovingTile(locX, locY, locMap)) {
                toTeleport(locX, locY, locMap, true);
                break;
            }
        }
    }
    
    protected boolean isTileOccupied(int tx, int ty, int tmap) {
        if (World.getMapdynamic(tx, ty, tmap) >= 2) return true;
        
        int count = 0;
        for (object obj : getInsideList()) {
            if (obj instanceof Character && !obj.isDead()) {
                if (obj.getX() == tx && obj.getY() == ty && obj.getMap() == tmap) {
                    count++;
                    if (count >= 2) return true;
                }
            }
        }
        return false;
    }
    
    protected void toSafeTeleport(int tx, int ty, int tmap) {
        int finalX = tx;
        int finalY = ty;
        
        if (!World.isThroughObject(tx, ty, tmap, 0) || World.isNotMovingTile(tx, ty, tmap)) {
            boolean found = false;
            for (int r = 1; r <= 15; r++) {
                for (int i = 0; i < 20; i++) {
                    int rx = tx + Util.random(-r, r);
                    int ry = ty + Util.random(-r, r);
                    if (World.isThroughObject(rx, ry, tmap, 0) && !World.isNotMovingTile(rx, ry, tmap)) {
                        finalX = rx;
                        finalY = ry;
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
        }
        
        toTeleport(finalX, finalY, tmap, true);
        toSender(S_ObjectLock.clone(BasePacketPooling.getPool(S_ObjectLock.class), 0x09));
    }
    
    /**
     * 목표 좌표(tx, ty, tmap)까지 최적의 방식으로 이동합니다.
     * @param ignorePC true이면 유저/로봇과 겹침을 무시하고 강제 돌파 (주로 군주 왕관 점령 시 사용)
     */
    protected boolean navigateTo(int tx, int ty, int tmap, boolean ignorePC) {
        long now = System.currentTimeMillis();
        if (now - lastMoveAttemptTime < 300) return false; // 0.3초 내 중복 이동 명령 방지
        lastMoveAttemptTime = now;

        if (this.getMap() != tmap) {
            if (this.teleportTime < now) {
                toSafeTeleport(tx, ty, tmap);
                this.teleportTime = now + 4000;
            }
            return true;
        }

        int dist = Util.getDistance(this.getX(), this.getY(), tx, ty);
        if (dist == 0) return true; // 도착 완료

        if (dist > 18) {
            if (this.teleportTime < now) {
                toSafeTeleport(tx, ty, tmap);
                this.teleportTime = now + 4000;
            }
            return true;
        }

        int headingToTarget = Util.calcheading(this.getX(), this.getY(), tx, ty);
        if (now - lastDirectionSetTime > 2000) {
            setHeading(headingToTarget);
            lastDirectionSetTime = now;
        }

        int nextStraightX = this.getX() + Util.getXY(headingToTarget, true);
        int nextStraightY = this.getY() + Util.getXY(headingToTarget, false);
        if (World.isThroughObject(this.getX(), this.getY(), tmap, headingToTarget) 
            && !World.isNotMovingTile(nextStraightX, nextStraightY, tmap)) {
            
            if (ignorePC || !isTileOccupied(nextStraightX, nextStraightY, tmap)) {
                this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                toMoving(this, nextStraightX, nextStraightY, headingToTarget, false, ignorePC);
                return true;
            }
        }

        try {
            if (this.aStar != null) {
                this.aStar.cleanTail();
                Node path = this.aStar.searchTail(this, tx, ty, ignorePC);
                
                if (path != null) {
                    int nextX = -1, nextY = -1;
                    while (path != null) {
                        if (path.x == this.getX() && path.y == this.getY()) break;
                        nextX = path.x;
                        nextY = path.y;
                        path = path.prev;
                    }

                    if (nextX != -1 && nextY != -1) {
                        int astarHeading = Util.calcheading(this.getX(), this.getY(), nextX, nextY);
                        if (ignorePC || !isTileOccupied(nextX, nextY, tmap)) {
                            this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
                            toMoving(this, nextX, nextY, astarHeading, false, ignorePC);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {}

        return evadeAndMove(tx, ty, tmap, ignorePC);
    }

    /**
     * 막혔을 때 사용하는 지능형 8방향 틈새 비집기 모듈
     */
    protected boolean evadeAndMove(int targetX, int targetY, int targetMap, boolean ignorePC) {
        List<int[]> candidates = new ArrayList<>();
        for (int h = 0; h < 8; h++) {
            int rx = this.getX() + Util.getXY(h, true);
            int ry = this.getY() + Util.getXY(h, false);
            
            if (World.isThroughObject(this.getX(), this.getY(), targetMap, h) && !World.isNotMovingTile(rx, ry, targetMap)) {
                if (ignorePC || !isTileOccupied(rx, ry, targetMap)) {
                    int dist = Util.getDistance(rx, ry, targetX, targetY);
                    candidates.add(new int[]{rx, ry, h, dist});
                }
            }
        }
        
        if (!candidates.isEmpty()) {
            candidates.sort(Comparator.comparingInt(a -> a[3])); // 타겟과 가장 가까워지는 방향 우선
            int[] best = candidates.get(0);
            setHeading(best[2]);
            this.ai_time = SpriteFrameDatabase.getGfxFrameTime(this, getGfx(), getGfxMode() + Lineage.GFX_MODE_WALK);
            toMoving(this, best[0], best[1], best[2], false, ignorePC);
            return true;
        }
        return false;
    }
    
}