# 에고무기 타 서버코어 포팅 가이드

이 문서는 `ego/` 시스템을 `ydhappy/myso`가 아닌 다른 리니지/게임 서버코어에 적용하기 위한 가이드입니다.

기존 `ego/java/` 파일들은 myso 서버 구조에 맞춘 적용본입니다.
다른 서버코어에서는 그대로 복사하면 `lineage.*`, `PcInstance`, `ItemInstance`, `MonsterInstance`, `Lineage`, `ChattingController`, `DatabaseConnection` 등의 의존성 때문에 컴파일 오류가 날 수 있습니다.

따라서 다른 서버코어에는 아래 순서로 포팅하는 것을 권장합니다.

---

## 1. 결론

다른 서버코어용으로는 아래 구조를 사용하세요.

```text
ego/portable/EgoCoreAdapter.java      # 서버코어 의존성 흡수용 인터페이스
ego/portable/EgoPortableRules.java    # 순수 규칙 유틸, 거의 그대로 사용 가능
ego/sql/*.sql                        # DB 구조 참고, 테이블명/컬럼명은 서버에 맞게 수정
ego/docs/EGO_SYSTEM_MANUAL.md         # 기능/적용 흐름 참고
```

`ego/java/` 아래 파일들은 myso 전용 예제이자 참고 구현입니다.

---

## 2. 포팅 핵심 개념

에고 기능은 크게 두 부분입니다.

### 2.1 공통 규칙

서버와 무관한 로직입니다.

```text
무기 종류 판정
능력 허용 여부
기본 능력 추천
HP 구간 표시
위험도 등급
```

이 부분은 아래 파일에 들어 있습니다.

```text
ego/portable/EgoPortableRules.java
```

### 2.2 서버코어 종속 로직

서버마다 클래스명과 메서드명이 다릅니다.

```text
플레이어 클래스명
아이템 클래스명
몬스터 클래스명
채팅 전송 방식
데미지 처리 방식
인벤토리/착용무기 조회 방식
주변 객체 조회 방식
자동공격 처리 방식
DB 연결 방식
```

이 차이는 아래 인터페이스로 흡수합니다.

```text
ego/portable/EgoCoreAdapter.java
```

---

## 3. 서버별로 반드시 매핑해야 하는 항목

### 3.1 플레이어

```java
boolean isPlayer(Object obj);
boolean isRobotPlayer(Object obj);
int getLevel(Object player);
int getNowHp(Object player);
int getMaxHp(Object player);
int getNowMp(Object player);
int getMaxMp(Object player);
Object getTarget(Object player);
void setTarget(Object player, Object target);
List<?> getVisibleObjects(Object player);
void sendSystemMessage(Object player, String message);
```

myso 기준 예시:

```text
PcInstance
RobotInstance
pc.getLevel()
pc.getNowHp()
pc.getTotalHp()
pc.getTarget()
pc.getInsideList()
ChattingController.toChatting(...)
```

다른 서버코어에서는 `L1PcInstance`, `L1Character`, `L1Object` 같은 이름일 수 있습니다.

---

### 3.2 무기/아이템

```java
Object getEquippedWeapon(Object player);
boolean isWeaponSlot(Object item);
String getItemName(Object item);
String getItemType2(Object item);
int getEnchantLevel(Object item);
long getItemObjectId(Object item);
```

서버별 확인 포인트:

```text
착용 무기 슬롯 상수
아이템 종류 컬럼명
인챈트 레벨 getter
아이템 고유 objectId getter
```

무기 타입은 서버마다 다릅니다.

myso 기준:

```text
dagger
sword
tohandsword
axe
spear
bow
staff
wand
fishing_rod
```

다른 서버에서는 아래처럼 다를 수 있습니다.

```text
twohand_sword
two_handed_sword
claw
edoryu
crossbow
single_sword
```

이 경우 `EgoPortableRules`에 타입을 추가하세요.

---

### 3.3 몬스터

```java
boolean isMonster(Object obj);
boolean isAggressiveMonster(Object monster);
boolean isBossMonster(Object monster);
long getMonsterExp(Object monster);
```

서버별 확인 포인트:

```text
선공 여부 컬럼/메서드
보스 여부 컬럼/메서드
몬스터 경험치 getter
몬스터 사망 처리 이벤트 위치
```

선공 기준이 없는 서버는 우선 아래처럼 임시 처리할 수 있습니다.

```java
return false;
```

그 다음 몬스터 DB의 `aggressive`, `agro`, `atk_type`, `family`, `first_attack` 같은 값을 찾아 연결하세요.

---

### 3.4 전투/제어

```java
void startAutoAttack(Object player, Object target);
void stopAutoAttack(Object player);
void healHp(Object player, int amount);
void healMp(Object player, int amount);
void damageMonster(Object attacker, Object monster, int damage);
void sendEffect(Object target, int effectId);
```

서버별 위험 포인트:

```text
자동공격 필드가 있는지
데미지 처리에 락/스레드가 필요한지
HP/MP 직접 set이 안전한지
이펙트 ID가 클라이언트와 맞는지
PvP 지역 제한이 필요한지
```

---

## 4. 포팅 단계

### 4.1 1단계: 대화만 붙이기

먼저 전투 기능 없이 대화만 붙입니다.

필요 기능:

```text
일반 채팅 입력 감지
착용 무기 조회
시스템 메시지 출력
```

테스트:

```text
에고
에고 상태
```

### 4.2 2단계: 상태 인식

추가 기능:

```text
HP/MP/레벨/무기종류/인챈트 조회
```

테스트:

```text
에고 상태
```

### 4.3 3단계: 선공 몬스터 감지

추가 기능:

```text
주변 객체 조회
몬스터 판정
선공 판정
거리 계산
```

테스트:

```text
에고 선공
```

### 4.4 4단계: 상대 캐릭터 감지

추가 기능:

```text
주변 플레이어 판정
혈맹/성향/PK/클래스/무기종류 조회
```

테스트:

```text
에고 상대
에고 주변캐릭
```

### 4.5 5단계: 특별 능력

추가 기능:

```text
DamageController 또는 AttackController 연결
최종 데미지 보정
HP/MP 회복
이펙트 출력
```

테스트:

```text
몬스터 공격 시 에고 능력 발동
```

### 4.6 6단계: DB 저장

추가 기능:

```text
에고 이름 저장
에고 레벨/경험치 저장
능력 저장
DB 캐시 리로드
```

테스트:

```text
.에고생성 카르마
.에고정보
.에고능력 BLOOD_DRAIN
서버 재시작 후 유지 확인
```

---

## 5. 포팅 시 가장 많이 나는 오류

### 5.1 패키지 오류

문제:

```text
package lineage.world.controller does not exist
```

원인:

```text
myso 전용 package를 다른 서버에 그대로 복사함
```

해결:

```text
서버코어의 패키지 구조에 맞게 package/import 수정
또는 ego.portable 기반으로 새 컨트롤러 작성
```

---

### 5.2 PcInstance 없음

문제:

```text
cannot find symbol PcInstance
```

해결:

```text
해당 서버의 플레이어 클래스명으로 변경
예: L1PcInstance, Pc, PlayerInstance, Player
```

---

### 5.3 getInsideList 없음

문제:

```text
cannot find symbol getInsideList
```

해결:

서버의 주변 객체 조회 메서드를 찾습니다.

후보 이름:

```text
getKnownObjects()
getNearObjects()
getVisibleObjects()
getKnownPlayers()
World.getVisibleObjects(player)
World.getAroundObjects(player)
```

---

### 5.4 데미지 연결 위치 불명확

후보 위치:

```text
DamageController.getDamage
AttackController.calcDamage
L1Attack.calcDamage
WeaponDamage.calc
PhysicalAttack
onAttack
```

원칙:

```text
최종 데미지 반환 직전
명중/회피/리덕션 계산 후
실제 HP 차감 전
```

---

### 5.5 HP/MP 직접 set 위험

일부 서버는 HP/MP 변경 시 패킷/이벤트/락 처리가 필요합니다.

가능하면 기존 회복 메서드를 사용하세요.

후보:

```text
healHp
setCurrentHp
setCurrentMp
receiveDamage
sendStatusPacket
updateHpBar
```

---

## 6. 포팅용 최소 구현 예시

아래는 구조 예시입니다. 실제 클래스명은 서버에 맞게 바꿔야 합니다.

```java
public final class MyServerEgoAdapter implements EgoCoreAdapter {

    @Override
    public boolean isPlayer(Object obj) {
        return obj instanceof L1PcInstance;
    }

    @Override
    public Object getEquippedWeapon(Object player) {
        L1PcInstance pc = (L1PcInstance) player;
        return pc.getWeapon();
    }

    @Override
    public String getItemType2(Object item) {
        L1ItemInstance weapon = (L1ItemInstance) item;
        return weapon.getItem().getType2();
    }

    @Override
    public void sendSystemMessage(Object player, String message) {
        ((L1PcInstance) player).sendPackets(new S_SystemMessage(message));
    }

    // 나머지 메서드도 서버코어에 맞게 매핑
}
```

---

## 7. 타 서버코어 적용 권장 구조

권장:

```text
src/ego/portable/EgoCoreAdapter.java
src/ego/portable/EgoPortableRules.java
src/ego/core/EgoTalkService.java
src/ego/core/EgoAbilityService.java
src/ego/core/EgoOpponentScanService.java
src/ego/server/MyServerEgoAdapter.java
```

비추천:

```text
myso용 EgoWeaponControlController.java를 그대로 복사해서 import만 대충 수정
```

이 방식은 초기에 빨라 보이지만, 서버코어가 조금만 달라도 컴파일 오류와 런타임 오류가 많이 발생합니다.

---

## 8. 기능별 포팅 난이도

```text
대화 반응                 쉬움
캐릭터 상태 인식          쉬움
무기 종류 확인            쉬움~보통
선공 몬스터 감지          보통
상대 캐릭터 감지          보통
자동공격 제어             보통~어려움
특별 능력 데미지 연결      어려움
DB 저장/로드              보통
LLM/AI 멘트 연동          어려움
```

---

## 9. 안전 권장사항

다른 서버코어에 적용할 때는 아래 기능을 단계적으로 켜세요.

1차:

```text
에고 대화
에고 상태
.에고검사
```

2차:

```text
선공 몬스터 감지
상대 캐릭터 감지
```

3차:

```text
특별 능력 발동
```

4차:

```text
자동공격 제어
에고 경험치/레벨업
```

처음부터 전부 켜면 오류 위치를 찾기 어렵습니다.

---

## 10. 포팅 완료 기준

아래가 모두 통과되면 1차 포팅 완료로 봅니다.

```text
- 서버 컴파일 성공
- 무기 착용 후 에고 호출 가능
- 무기 미착용 시 에고 반응 없음
- 지원 무기/비지원 무기 구분 가능
- 에고 상태 출력 정상
- 선공 몬스터 감지 정상
- 상대 캐릭터 감지 정상
- 특별 능력 발동 시 서버 오류 없음
- .에고검사 결과 OK
- 서버 재시작 후 DB 정보 유지
```
