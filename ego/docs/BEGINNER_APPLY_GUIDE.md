# 에고무기 초보자 적용 가이드

이 문서는 `ego/` 폴더에 준비된 에고무기 기능을 실제 서버 소스에 적용하는 방법입니다.

목표:

- 자바 파일 1개 복사
- 기존 `ChattingController.java`에 연결 코드 몇 줄 추가
- SQL 적용
- 게임에서 `에고` 채팅으로 테스트

## 0. 작업 전 백업

반드시 아래 파일과 DB를 백업하세요.

```text
bitna/src/lineage/world/controller/ChattingController.java
DB 전체 또는 최소 characters_inventory 관련 테이블
```

## 1. SQL 적용

아래 파일을 DB에 실행합니다.

```text
ego/sql/ego_weapon.sql
```

1차 자바 코드는 DB 없이도 동작하지만, 나중에 에고 이름/성격/레벨을 저장하려면 SQL을 먼저 넣어두는 것이 좋습니다.

## 2. 자바 파일 복사

아래 파일을 복사합니다.

원본:

```text
ego/java/EgoWeaponControlController.java
```

복사 위치:

```text
bitna/src/lineage/world/controller/EgoWeaponControlController.java
```

패키지 선언이 이미 아래처럼 되어 있어야 합니다.

```java
package lineage.world.controller;
```

## 3. ChattingController.java 수정

수정 파일:

```text
bitna/src/lineage/world/controller/ChattingController.java
```

### 3-1. 일반 채팅 처리 위치 찾기

`ChattingController.java`에서 아래 코드를 찾습니다.

```java
if (!CommandController.toCommand(o, msg)) {
```

이 코드는 일반 채팅이 명령어가 아닐 때 실제 채팅으로 처리하는 구간입니다.

### 3-2. 에고 연결 코드 추가

위 코드를 찾은 직후, 블록 안쪽 가장 위에 아래 코드를 추가합니다.

```java
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoWeaponControlController.onNormalChat((PcInstance) o, msg)) {
        return;
    }
}
```

적용 예시는 아래와 같습니다.

```java
if (!CommandController.toCommand(o, msg)) {

    // 에고무기 대화/제어 처리
    // 에고 이름으로 시작하는 일반 채팅은 여기서 소비하고 일반 채팅으로 퍼뜨리지 않습니다.
    if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
        if (EgoWeaponControlController.onNormalChat((PcInstance) o, msg)) {
            return;
        }
    }

    if (o != null && o.isBuffChattingClose()) {
        o.toSender(S_Message.clone(BasePacketPooling.getPool(S_Message.class), 242));
        return;
    }

    // 기존 일반 채팅 처리 계속...
}
```

`ChattingController`와 `EgoWeaponControlController`는 같은 패키지입니다.
그래서 보통 import 추가는 필요 없습니다.

그래도 IDE가 import를 요구하면 아래를 import 영역에 추가하세요.

```java
import lineage.world.controller.EgoWeaponControlController;
```

## 4. 컴파일 오류 대응

### 4-1. RobotInstance 오류

`ChattingController.java`에는 이미 `RobotInstance` import가 있는 구조입니다.
없다면 import 영역에 추가합니다.

```java
import lineage.world.object.instance.RobotInstance;
```

### 4-2. getInsideList 오류

`EgoWeaponControlController`는 아래 메서드를 사용합니다.

```java
pc.getInsideList()
```

현재 서버의 `object.java`에 주변 객체 리스트가 있으므로 보통 존재합니다.
만약 컴파일 오류가 나면 `object.java`에서 실제 getter 이름을 확인한 뒤 아래 부분을 바꿉니다.

```java
List<object> inside = pc.getInsideList();
```

예를 들어 서버가 `getInsideList(true)` 형태라면:

```java
List<object> inside = pc.getInsideList(true);
```

### 4-3. resetAutoAttack 오류

컨트롤러 코드에서는 아래처럼 try-catch로 보호되어 있습니다.

```java
try {
    pc.resetAutoAttack();
} catch (Throwable e) {
}
```

그래도 서버 버전에 따라 컴파일 단계에서 오류가 나면 아래 줄만 삭제해도 됩니다.

```java
pc.resetAutoAttack();
```

삭제 후에도 아래 필드 초기화만으로 중지는 됩니다.

```java
pc.isAutoAttack = false;
pc.autoAttackTarget = null;
pc.setTarget(null);
```

## 5. 게임 내 테스트

서버를 실행하고 캐릭터가 무기를 착용한 상태에서 일반 채팅으로 입력합니다.

```text
에고
```

정상 반응:

```text
[에고] 부르셨습니까, 주인님.
```

상태 확인:

```text
에고 상태
```

예상 반응:

```text
[에고] Lv.52 / HP 88%(720/820) / MP 40%(120/300) / 무기 +7 싸울아비 장검
```

조언:

```text
에고 조언
```

선공 확인:

```text
에고 선공
```

공격:

```text
에고 공격
```

중지:

```text
에고 멈춰
```

## 6. 무기별 에고 이름 사용하기

1차 기본 이름은 코드에 고정되어 있습니다.

파일:

```text
bitna/src/lineage/world/controller/EgoWeaponControlController.java
```

기본값:

```java
private static final String DEFAULT_EGO_NAME = "에고";
```

예를 들어 기본 호출명을 `카르마`로 바꾸려면:

```java
private static final String DEFAULT_EGO_NAME = "카르마";
```

그러면 게임에서 아래처럼 호출합니다.

```text
카르마 상태
카르마 공격
카르마 멈춰
```

## 7. 나중에 진짜 무기별 이름으로 확장하는 법

`ItemInstance.java`에 필드를 추가합니다.

```java
protected boolean egoEnabled;
protected String egoName;
protected String egoPersonality;
protected int egoLevel;
protected long egoExp;
protected long egoMaxExp;
```

getter/setter를 추가합니다.

```java
public boolean isEgoEnabled() {
    return egoEnabled;
}

public void setEgoEnabled(boolean egoEnabled) {
    this.egoEnabled = egoEnabled;
}

public String getEgoName() {
    return egoName;
}

public void setEgoName(String egoName) {
    this.egoName = egoName;
}

public String getEgoPersonality() {
    return egoPersonality;
}

public void setEgoPersonality(String egoPersonality) {
    this.egoPersonality = egoPersonality;
}

public int getEgoLevel() {
    return egoLevel;
}

public void setEgoLevel(int egoLevel) {
    this.egoLevel = egoLevel;
}

public long getEgoExp() {
    return egoExp;
}

public void setEgoExp(long egoExp) {
    this.egoExp = egoExp;
}

public long getEgoMaxExp() {
    return egoMaxExp;
}

public void setEgoMaxExp(long egoMaxExp) {
    this.egoMaxExp = egoMaxExp;
}
```

`close()` 초기화 구간에 추가합니다.

```java
egoEnabled = false;
egoName = null;
egoPersonality = null;
egoLevel = 0;
egoExp = 0;
egoMaxExp = 0;
```

그 다음 `EgoWeaponControlController.getEgoName(...)`를 아래처럼 바꿉니다.

```java
private static String getEgoName(ItemInstance weapon) {
    if (weapon.getEgoName() != null && weapon.getEgoName().length() > 0)
        return weapon.getEgoName();
    return DEFAULT_EGO_NAME;
}
```

단, 이 단계는 DB 로드/저장까지 연결해야 완전합니다.
초보자 1차 적용에서는 고정 이름 방식만 사용하세요.

## 8. 자동 경고 연결은 2차에서 진행

`EgoWeaponControlController`에는 아래 메서드가 준비되어 있습니다.

```java
EgoWeaponControlController.checkAutoWarning(pc);
```

이 메서드를 캐릭터 틱이나 주기 스레드에 연결하면, 사용자가 말하지 않아도 선공 몬스터가 접근할 때 경고할 수 있습니다.

하지만 1차에서는 추천하지 않습니다.

이유:

- 너무 많은 메시지가 발생할 수 있음
- 기존 AI/자동사냥/로봇 시스템과 충돌 가능
- 초보자 디버깅 난이도 상승

1차는 반드시 수동 호출 방식으로 테스트하세요.

## 9. 선공 몬스터가 감지되지 않을 때

`monster` 테이블의 `atk_type` 값을 확인하세요.

현재 기준:

```java
mon.getMonster().getAtkType() > 0
```

만약 서버 DB에서 `atk_type = 1`만 선공이라면 아래처럼 바꾸세요.

```java
if (mon.getMonster().getAtkType() != 1)
    continue;
```

만약 특정 몬스터 이름으로 테스트하려면 임시로 아래처럼 조건을 넣을 수 있습니다.

```java
String monName = mon.getMonster().getName();
if (!monName.contains("오크"))
    continue;
```

테스트 후에는 반드시 제거하세요.

## 10. 초보자용 최종 확인표

- [ ] SQL 적용 완료
- [ ] `EgoWeaponControlController.java` 복사 완료
- [ ] `ChattingController.java`에 연결 코드 추가 완료
- [ ] 서버 컴파일 성공
- [ ] 무기 미착용 시 에고 반응 없음 확인
- [ ] 무기 착용 후 `에고` 반응 확인
- [ ] `에고 상태` 정상 출력 확인
- [ ] `에고 선공` 정상 출력 확인
- [ ] 선공 몬스터 근처에서 `에고 공격` 동작 확인
- [ ] `에고 멈춰`로 자동공격 중지 확인

## 11. 원복 방법

문제가 생기면 아래 순서로 원복합니다.

1. `ChattingController.java`에 추가한 에고 연결 코드 삭제
2. `bitna/src/lineage/world/controller/EgoWeaponControlController.java` 삭제
3. 서버 재빌드

SQL 테이블은 남아 있어도 서버 동작에는 영향이 없습니다.
원하면 아래처럼 삭제할 수 있습니다.

```sql
DROP TABLE IF EXISTS ego_talk_template;
DROP TABLE IF EXISTS ego_personality_template;
DROP TABLE IF EXISTS character_item_ego;
```
