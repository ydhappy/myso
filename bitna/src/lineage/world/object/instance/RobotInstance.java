package lineage.world.object.instance;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import lineage.bean.lineage.Book;
import lineage.bean.lineage.Party;
import lineage.network.packet.BasePacketPooling;
import lineage.network.packet.server.S_ObjectChatting;
import lineage.network.packet.server.S_ObjectLock;
import lineage.share.Lineage;
import lineage.util.Util;
import lineage.world.World;
import lineage.world.controller.BookController;
import lineage.world.controller.InventoryController;
import lineage.world.controller.PartyController;
import lineage.world.object.Character;
import lineage.world.object.object;

public class RobotInstance extends PcInstance {

    // Robot 상태 비트 저장용 변수
    private long robotStatus = 0;
    
	public RobotInstance() {
		super(null);
	}

    // Robot 상태 가져오기
    public long getRobotStatus() {
        return robotStatus;
    }
    
    // Robot 상태 설정하기
    public void setRobotStatus(long robotStatus) {
        this.robotStatus = robotStatus;
    }
    
	@Override
	public void toTimer(long time) {

	}

	@Override
	public void toWorldJoin() {
		InventoryController.toWorldJoin(this);
		toTeleport(getX(), getY(), getMap(), false);
	} 

	@Override
	public void toWorldOut() {
		clearList(true);
		World.remove(this);

		InventoryController.toWorldOut(this);
	}

	@Override
	public void toSave(Connection con) {

	}
	
    /**
     * ✅ 로봇 일반 채팅 브로드캐스트 (말풍선 강제 출력용)
     */
	protected void broadcastRobotChat(String msg) {
        S_ObjectChatting chattingPacket = (S_ObjectChatting) S_ObjectChatting.clone(
            BasePacketPooling.getPool(S_ObjectChatting.class), 
            this, Lineage.CHATTING_MODE_NORMAL, msg
        );
        this.toSender(chattingPacket, true); 
    }
	
    /**
     * ✅ 개선된 적정 사냥터 이동 메서드
     * - 파티장 레벨 +15 제한 적용
     * - 사냥터별 인원수를 파악하여 로봇들을 골고루 분산시킴
     */
    public void toTeleportToAppropriateHuntingGround() {
        List<Book> list = BookController.find(this);
        if (list == null || list.isEmpty()) {
            goToHome(false);
            return;
        }

        List<Book> possibleList = new ArrayList<>();
        int myLevel = getLevel();
        
        Party party = PartyController.find(this);
        boolean isParty = (party != null);
        int referenceLevel = myLevel;

        if (isParty && party.getMaster() != null) {
            referenceLevel = party.getMaster().getLevel();
        }

        // 1. 입장 가능한 사냥터 목록 필터링
        for (Book book : list) {
            if (book != null && book.getEnable()) {
                if (isParty) {
                    if (book.getMinLevel() <= (referenceLevel + 15)) possibleList.add(book);
                } else {
                    if (myLevel >= book.getMinLevel()) possibleList.add(book);
                }
            }
        }

        if (!possibleList.isEmpty()) {
            // 🌟 2. 인원 분산 로직: 인원수가 가장 적은 사냥터 찾기
            Book bestLocation = possibleList.get(0);
            int minPops = Integer.MAX_VALUE;

            for (Book b : possibleList) {
                // 해당 맵(b.getMap())에 현재 있는 로봇 수를 카운트
                int currentPops = World.getRobotList().stream()
                    .filter(r -> r.getMap() == b.getMap())
                    .mapToInt(r -> 1).sum();

                if (currentPops < minPops) {
                    minPops = currentPops;
                    bestLocation = b;
                }
            }
            
            // 3. 최종 결정된 사냥터로 이동
            Book b = bestLocation;
            
            String ment = isParty 
                ? "[분산이동] 인원이 적은 " + b.getLocation() + "(으)로 팀 사냥을 갑니다!" 
                : "쾌적한 " + b.getLocation() + "(으)로 사냥을 떠납니다.";
            
            broadcastRobotChat(ment); //
            
         // 좌표 세팅
            setHomeX(b.getX()); setHomeY(b.getY()); setHomeMap(b.getMap());
            this.start_x = b.getX(); this.start_y = b.getY(); this.start_map = b.getMap();

            // 🌟 본인(리더) 텔레포트 (살짝 흩뿌린 좌표로 이동)
            int[] scatteredSelf = getScatteredLocation(b.getX(), b.getY(), b.getMap());
            toTeleport(scatteredSelf[0], scatteredSelf[1], scatteredSelf[2], true);

            // 🌟 5. 파티원 강제 동기화 (파티원들도 리더 주변으로 흩뿌려서 텔레포트)
            if (isParty && party.getMaster() == this) {
                for (PcInstance member : party.getList()) {
                    if (member != this && (member instanceof lineage.world.object.instance.RobotInstance)) {
                        int[] scatteredMember = getScatteredLocation(b.getX(), b.getY(), b.getMap());
                        member.toTeleport(scatteredMember[0], scatteredMember[1], scatteredMember[2], true);
                    }
                }
            }
            
            toSender(S_ObjectLock.clone(BasePacketPooling.getPool(S_ObjectLock.class), 0x09));
        } else {
            broadcastRobotChat("갈 곳이 없네요. 마을로 돌아갑니다.");
            goToHome(false);
        }
    }
    
    /**
     * ✅ 기본 좌표 주변 ±7 칸 이내의 랜덤한 이동 가능한 좌표 반환
     */
    public int[] getScatteredLocation(int originX, int originY, int mapId) {
        int maxAttempts = 20;
        int spread = 7;

        for (int i = 0; i < maxAttempts; i++) {
            // ±spread 범위 내 랜덤 오프셋
            int offsetX = Util.random(-spread, spread);
            int offsetY = Util.random(-spread, spread);

            int newX = originX + offsetX;
            int newY = originY + offsetY;

            // 이동 가능한 지형인지 체크 (0번 방향 가정, ignorePC=true)
            if (isMovableTile(newX, newY, 0, mapId, true)) {
                return new int[] { newX, newY, mapId };
            }
        }

        // 반복 실패 시 원래 위치로
        return new int[] { originX, originY, mapId };
    }
    
    /**
     * ✅ 해당 좌표가 이동 가능한 타일인지 확인
     * 
     * @param ignorePC
     *                     true면 PC가 있어도 이동 가능
     */
    public boolean isMovableTile(int x, int y, int heading, int mapId, boolean ignorePC) {
        if (!World.isThroughObject(this.x, this.y, mapId, heading))
            return false;
        if (World.getMapdynamic(x, y, mapId) != 0)
            return false;
        if (World.isNotMovingTile(x, y, mapId))
            return false;

        if (!ignorePC && isPlayerAt(x, y, mapId))
            return false; // ✅ PC 무시 여부 분기
        if (isOccupiedByRobot(x, y))
            return false;

        return true;
    }
    
    /**
     * ✅ 해당 좌표에 사람이 조종하는 PC가 있는지 확인
     */
    private boolean isPlayerAt(int x, int y, int mapId) {
        for (object obj : getInsideList()) {
            if (obj == this) continue;
            if (!(obj instanceof PcInstance)) continue; // 실제 사람 유저 클래스 (확인 필요)
            if (obj.getX() == x && obj.getY() == y && obj.getMap() == mapId) {
                return true;
            }
        }
        return false;
    }

    /**
     * ✅ 해당 좌표에 로봇 또는 사람(PC)이 있는지 확인
     */
    private boolean isOccupiedByRobot(int x, int y) {
        for (object obj : getInsideList()) {
            if (obj == this) continue;
            if (!(obj instanceof Character)) continue; // Character, PcInstance 등 전체 캐릭터 상위 클래스
            if (obj.getX() == x && obj.getY() == y && obj.getMap() == this.map) {
                return true;
            }
        }
        return false;
    }
    
    protected void goToHome(boolean isCracker) {
    	
    }
}
