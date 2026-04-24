# 에고무기 DB/명령어 적용 가이드

이 문서는 에고무기 정보를 DB에 저장하고, 게임 안에서 명령어로 에고를 생성/관리하는 방법입니다.

## 1. 추가된 파일

```text
ego/java/EgoWeaponDatabase.java
ego/java/EgoWeaponCommand.java
ego/docs/EGO_WEAPON_DATABASE_COMMAND_GUIDE.md
```

## 2. SQL 적용 순서

아래 SQL을 순서대로 적용합니다.

```text
ego/sql/ego_weapon.sql
ego/sql/ego_weapon_ability.sql
```

## 3. 자바 파일 복사

### 3-1. DB 헬퍼 복사

원본:

```text
ego/java/EgoWeaponDatabase.java
```

복사 위치:

```text
bitna/src/lineage/database/EgoWeaponDatabase.java
```

주의: 이 파일의 패키지는 `lineage.database`입니다.

```java
package lineage.database;
```

### 3-2. 명령어 헬퍼 복사

원본:

```text
ego/java/EgoWeaponCommand.java
```

복사 위치:

```text
bitna/src/lineage/world/controller/EgoWeaponCommand.java
```

주의: 이 파일의 패키지는 `lineage.world.controller`입니다.

```java
package lineage.world.controller;
```

## 4. 서버 시작 시 DB 로드 연결

`EgoWeaponDatabase`는 서버 시작 시 한 번 로드되어야 합니다.

가장 쉬운 방식은 서버 DB 로딩 구간에서 아래를 호출하는 것입니다.

```java
EgoWeaponDatabase.init(con);
```

예상 위치 후보:

```text
lineage/Main.java
또는 DB 초기화 메서드들이 모여 있는 로딩 구간
```

import 필요:

```java
import lineage.database.EgoWeaponDatabase;
```

만약 정확한 로딩 위치를 모르면, 일단 `.에고리로드` 명령어로 수동 리로드할 수 있습니다.
단, 서버 재시작 후 자동 로드가 안 되면 `.에고정보`가 DB 값을 못 볼 수 있으므로 최종적으로는 로딩 연결을 권장합니다.

## 5. CommandController.java 연결

수정 파일:

```text
bitna/src/lineage/world/controller/CommandController.java
```

### 5-1. 위치 찾기

`toCommand(object o, String cmd)` 안에서 아래 줄을 찾습니다.

```java
Object is_check = PluginController.init(CommandController.class, "toCommand", o, key, st);
if (is_check != null)
    return (Boolean) is_check;
```

### 5-2. 바로 아래에 추가

```java
if (EgoWeaponCommand.toCommand(o, key, st)) {
    return true;
}
```

적용 예시:

```java
Object is_check = PluginController.init(CommandController.class, "toCommand", o, key, st);
if (is_check != null)
    return (Boolean) is_check;

// 에고무기 명령어 처리
if (EgoWeaponCommand.toCommand(o, key, st)) {
    return true;
}
```

같은 패키지라 import는 보통 필요 없습니다.
필요하면 추가하세요.

```java
import lineage.world.controller.EgoWeaponCommand;
```

## 6. 지원 명령어

| 명령어 | 설명 |
|---|---|
| `.에고도움` | 명령어 안내 |
| `.에고생성 [이름]` | 착용 무기를 에고무기로 활성화 |
| `.에고정보` | 착용 에고무기 정보 확인 |
| `.에고이름 [새이름]` | 에고 호출 이름 변경 |
| `.에고능력 [능력코드]` | 특별 능력 설정 |
| `.에고리로드` | DB 캐시 리로드, GM 전용 |

## 7. 게임 내 사용 예시

무기를 착용한 후:

```text
.에고생성 카르마
```

그러면 일반 채팅으로 아래처럼 호출할 수 있습니다.

```text
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
```

이름 변경:

```text
.에고이름 루나
```

특별 능력 설정:

```text
.에고능력 BLOOD_DRAIN
```

정보 확인:

```text
.에고정보
```

## 8. 능력 코드 목록

```text
EGO_BALANCE      공명 타격
BLOOD_DRAIN      생명 흡수
MANA_DRAIN       정신 흡수
CRITICAL_BURST   치명 폭발
GUARDIAN_SHIELD  수호 의지
AREA_SLASH       공명 베기
EXECUTION        처형
FLAME_BRAND      화염 각인
FROST_BIND       서리 충격
```

## 9. 기존 컨트롤러를 DB 기반으로 바꾸기

### 9-1. EgoWeaponControlController.java 수정

현재 기본 호출명은 고정값입니다.

```java
private static final String DEFAULT_EGO_NAME = "에고";
```

아래 import 추가:

```java
import lineage.database.EgoWeaponDatabase;
```

`getEgoName(...)` 메서드를 아래처럼 교체합니다.

```java
private static String getEgoName(ItemInstance weapon) {
    return EgoWeaponDatabase.getEgoName(weapon, DEFAULT_EGO_NAME);
}
```

그리고 `onNormalChat(...)`에서 에고무기만 반응시키려면 착용 무기 확인 후 아래 조건을 추가합니다.

```java
if (!EgoWeaponDatabase.isEgoWeapon(weapon))
    return false;
```

### 9-2. EgoWeaponAbilityController.java 수정

아래 import 추가:

```java
import lineage.database.EgoWeaponDatabase;
import lineage.database.EgoWeaponDatabase.EgoAbilityInfo;
```

`ENABLE_TEST_MODE`를 false로 변경합니다.

```java
private static final boolean ENABLE_TEST_MODE = false;
```

`isEgoWeapon(...)`을 아래처럼 교체합니다.

```java
public static boolean isEgoWeapon(ItemInstance weapon) {
    return EgoWeaponDatabase.isEgoWeapon(weapon);
}
```

`getEgoLevel(...)`을 아래처럼 교체합니다.

```java
public static int getEgoLevel(ItemInstance weapon) {
    return EgoWeaponDatabase.getEgoLevel(weapon, 1);
}
```

`getAbilityType(...)`의 맨 위에 아래를 추가합니다.

```java
EgoAbilityInfo info = EgoWeaponDatabase.getFirstAbility(weapon);
if (info != null && info.abilityType != null) {
    try {
        return EgoAbilityType.valueOf(info.abilityType);
    } catch (Exception e) {
    }
}
```

그러면 `.에고능력 BLOOD_DRAIN`으로 설정한 값이 실제 능력으로 사용됩니다.

## 10. 에고 경험치 저장 연결

`EgoWeaponAbilityController.addKillExp(...)` 안의 주석 부분을 아래처럼 교체합니다.

기존:

```java
// EgoWeaponDatabase.addExp(weapon, pc, addExp);
```

교체:

```java
boolean levelUp = EgoWeaponDatabase.addExp(weapon, addExp);
if (levelUp) {
    ChattingController.toChatting(pc, "\\fY[에고] 내 의식이 성장했습니다. 에고 레벨이 상승했습니다.", Lineage.CHATTING_MODE_MESSAGE);
}
```

## 11. 초보자 추천 적용 순서

1. SQL 2개 적용
2. `EgoWeaponDatabase.java` 복사
3. `EgoWeaponCommand.java` 복사
4. `CommandController.java`에 명령어 연결
5. 서버 시작 시 `EgoWeaponDatabase.init(con)` 연결 또는 게임 안에서 `.에고리로드`
6. 무기 착용 후 `.에고생성 카르마`
7. `.에고정보` 확인
8. `.에고능력 BLOOD_DRAIN` 설정
9. 일반 채팅 `카르마 상태` 테스트
10. 특별 능력까지 연결했다면 몬스터 공격 테스트

## 12. 문제 해결

### .에고생성 실패

확인:

- SQL 적용 여부
- character_item_ego 테이블 존재 여부
- 착용 무기 objectId가 정상인지
- DB 접속 권한

### .에고정보가 안 나옴

확인:

- `.에고생성`을 먼저 했는지
- `EgoWeaponDatabase.reload(null)`이 호출되었는지
- `.에고리로드`를 GM으로 실행했는지

### 에고 이름을 바꿨는데 일반 채팅 호출이 안 됨

확인:

- `EgoWeaponControlController.getEgoName(...)`를 DB 기반으로 교체했는지
- `EgoWeaponDatabase.init(con)` 또는 `.에고리로드`를 했는지

### 능력 설정은 되는데 발동하지 않음

확인:

- `DamageController.java`에 `EgoWeaponAbilityController.applyAttackAbility(...)` 연결 여부
- `EgoWeaponAbilityController.ENABLE_TEST_MODE=false` 후 DB 기반 `isEgoWeapon`으로 교체했는지
- `.에고능력 능력코드`가 정상 저장되었는지

## 13. 원복 방법

1. `CommandController.java`에 추가한 `EgoWeaponCommand.toCommand(...)` 코드 삭제
2. `EgoWeaponControlController.java`의 DB 기반 수정 부분을 원래 고정 이름 방식으로 되돌림
3. `EgoWeaponAbilityController.java`의 DB 기반 수정 부분을 되돌림
4. 아래 파일 삭제

```text
bitna/src/lineage/database/EgoWeaponDatabase.java
bitna/src/lineage/world/controller/EgoWeaponCommand.java
```

5. 서버 재빌드

SQL 테이블은 남아 있어도 서버 동작에는 영향이 없습니다.
