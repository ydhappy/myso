# 에고무기 상세 적용 체크리스트

이 문서는 에고무기 적용 중 빠뜨리기 쉬운 부분을 확인하는 실전 체크리스트입니다.

## 1. 복사해야 하는 자바 파일

### world/controller 경로

아래 파일은 모두 같은 경로로 복사합니다.

```text
ego/java/EgoWeaponTypeUtil.java
→ bitna/src/lineage/world/controller/EgoWeaponTypeUtil.java

ego/java/EgoWeaponControlController.java
→ bitna/src/lineage/world/controller/EgoWeaponControlController.java

ego/java/EgoWeaponAbilityController.java
→ bitna/src/lineage/world/controller/EgoWeaponAbilityController.java

ego/java/EgoWeaponCommand.java
→ bitna/src/lineage/world/controller/EgoWeaponCommand.java

ego/java/EgoWeaponDiagnostics.java
→ bitna/src/lineage/world/controller/EgoWeaponDiagnostics.java
```

### database 경로

아래 파일만 database 경로로 복사합니다.

```text
ego/java/EgoWeaponDatabase.java
→ bitna/src/lineage/database/EgoWeaponDatabase.java
```

## 2. SQL 적용 체크

아래 순서대로 적용합니다.

```text
1. ego/sql/ego_weapon.sql
2. ego/sql/ego_weapon_ability.sql
```

적용 후 DB에 아래 테이블이 있어야 합니다.

```text
character_item_ego
ego_personality_template
ego_talk_template
ego_ability_template
character_item_ego_ability
ego_ability_proc_log
```

## 3. ChattingController 연결 체크

파일:

```text
bitna/src/lineage/world/controller/ChattingController.java
```

찾을 코드:

```java
if (!CommandController.toCommand(o, msg)) {
```

바로 아래에 추가:

```java
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoWeaponControlController.onNormalChat((PcInstance) o, msg)) {
        return;
    }
}
```

필요 import가 없으면 추가:

```java
import lineage.world.object.instance.PcInstance;
import lineage.world.object.instance.RobotInstance;
```

`EgoWeaponControlController`는 같은 패키지라 보통 import가 필요 없습니다.

## 4. CommandController 연결 체크

파일:

```text
bitna/src/lineage/world/controller/CommandController.java
```

`PluginController.init(...)` 호출 이후에 추가합니다.

```java
if (EgoWeaponCommand.toCommand(o, key, st)) {
    return true;
}
```

적용 후 사용 가능한 명령:

```text
.에고도움
.에고검사
.에고생성 이름
.에고정보
.에고이름 새이름
.에고능력 능력코드
.에고리로드
```

## 5. DamageController 연결 체크

파일:

```text
bitna/src/lineage/world/controller/DamageController.java
```

최종 데미지 반환 직전에 추가합니다.

예시:

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

주의:

실제 파일의 변수명이 다를 수 있습니다.

```text
cha     공격자 변수명
object  대상 변수명, target 또는 o일 수 있음
weapon  착용 무기 변수명
Dmg     데미지 변수명, dmg 또는 damage일 수 있음
```

컴파일 오류가 나면 실제 변수명에 맞춰 수정해야 합니다.

## 6. 서버 시작 시 DB 로드 체크

서버 시작 로딩 구간에서 아래 호출을 추가하는 것이 가장 좋습니다.

```java
EgoWeaponDatabase.init(con);
```

import:

```java
import lineage.database.EgoWeaponDatabase;
```

초기 연결이 어렵다면 GM으로 게임 접속 후 아래 명령을 실행합니다.

```text
.에고리로드
```

## 7. 첫 테스트 순서

무기를 착용하고 아래 순서대로 실행합니다.

```text
.에고검사
.에고생성 카르마
.에고정보
.에고능력 BLOOD_DRAIN
.에고검사
```

일반 채팅 테스트:

```text
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
```

## 8. 무기 종류 테스트

성공해야 하는 무기:

```text
dagger       단검
sword        한손검
tohandsword  양손검
axe          도끼
spear        창
bow          활
staff        지팡이
wand         완드
```

실패해야 정상인 항목:

```text
fishing_rod 낚싯대
armor       방어구
scroll      주문서
potion      포션
arrow       화살 단독
etc         기타 아이템
```

## 9. 능력별 무기 제한 테스트

### 창

```text
.에고능력 AREA_SLASH
```

성공해야 정상.

### 활

```text
.에고능력 AREA_SLASH
```

실패해야 정상.

### 지팡이

```text
.에고능력 MANA_DRAIN
```

성공해야 정상.

### 지팡이

```text
.에고능력 EXECUTION
```

실패해야 정상.

## 10. 자주 나는 컴파일 오류

### EgoWeaponTypeUtil 찾을 수 없음

원인:

```text
EgoWeaponTypeUtil.java 복사 누락
```

해결:

```text
ego/java/EgoWeaponTypeUtil.java
→ bitna/src/lineage/world/controller/EgoWeaponTypeUtil.java
```

### EgoWeaponDiagnostics 찾을 수 없음

원인:

```text
EgoWeaponDiagnostics.java 복사 누락
```

해결:

```text
ego/java/EgoWeaponDiagnostics.java
→ bitna/src/lineage/world/controller/EgoWeaponDiagnostics.java
```

### EgoWeaponDatabase 찾을 수 없음

원인:

```text
EgoWeaponDatabase.java를 controller 경로에 잘못 복사했거나 복사 누락
```

해결:

```text
ego/java/EgoWeaponDatabase.java
→ bitna/src/lineage/database/EgoWeaponDatabase.java
```

### getInsideList 오류

원인:

```text
서버 버전에 따라 주변 객체 리스트 getter 이름이 다를 수 있음
```

확인:

```text
object.java 안에서 insideList getter 이름 확인
```

### ATTACK_TYPE_WEAPON 오류

원인:

```text
Lineage.ATTACK_TYPE_WEAPON 상수가 서버 버전에 없거나 이름이 다름
```

확인:

```text
lineage/share/Lineage.java
```

대체:

기존 코드에서 `toDamage(..., Lineage.ATTACK_TYPE_...)`를 사용하는 이름을 찾아 동일하게 맞춥니다.

## 11. 운영 전 필수 변경

파일:

```text
bitna/src/lineage/world/controller/EgoWeaponAbilityController.java
```

테스트 모드:

```java
private static final boolean ENABLE_TEST_MODE = true;
```

운영 전 권장:

```java
private static final boolean ENABLE_TEST_MODE = false;
```

그리고 `isEgoWeapon(...)` 안에서 DB 기반 판정을 사용합니다.

```java
return EgoWeaponDatabase.isEgoWeapon(weapon);
```

## 12. 최종 확인

- [ ] SQL 2개 적용 완료
- [ ] 자바 6개 파일 복사 완료
- [ ] ChattingController 연결 완료
- [ ] CommandController 연결 완료
- [ ] DamageController 연결 완료
- [ ] 서버 시작 시 DB 로드 또는 `.에고리로드` 실행
- [ ] `.에고검사` 정상 출력
- [ ] `.에고생성` 정상 동작
- [ ] `.에고능력` 무기 제한 정상 동작
- [ ] 일반 채팅 호출 정상 동작
- [ ] 몬스터 공격 시 특별 능력 발동 확인
- [ ] 운영 전 테스트 모드 해제 검토
