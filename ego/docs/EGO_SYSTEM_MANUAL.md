# 에고무기 통합 매뉴얼

이 문서는 `ego/` 폴더의 에고무기 기능을 하나로 통합 정리한 공식 매뉴얼입니다.

기존에 나뉘어 있던 적용 가이드, 설계서, 특별능력, DB/명령어, 상대감지, 오류점검 내용을 이 문서 하나에서 확인할 수 있도록 재정리했습니다.

다른 서버코어에 적용하려면 `docs/EGO_PORTING_GUIDE.md`와 `portable/` 폴더를 먼저 확인하세요. `ego/java/`는 myso 서버 기준 참고 구현입니다.

---

## 1. 기능 요약

에고무기는 착용 중인 무기에 인격을 부여하여 캐릭터 상태와 주변 상황을 인식하고, 대화·조언·전투 보조·특별 능력을 수행하는 시스템입니다.

지원 기능:

```text
- 에고 이름 호출 대화
- 내 캐릭터 HP/MP/무기/타겟 상태 인식
- 주변 선공 몬스터 감지
- 주변 상대 캐릭터 감지
- 상대 캐릭터 위험도 분석
- 간단 자동공격 제어
- 특별 능력 발동
- 에고 이름/레벨/경험치/능력 DB 저장
- .에고검사 진단 명령
- 타 서버코어 포팅용 Adapter/Rules 제공
```

---

## 2. 최종 파일 구성

```text
ego/
├─ README.md
├─ docs/
│  ├─ EGO_SYSTEM_MANUAL.md              # 통합 매뉴얼
│  ├─ EGO_PORTING_GUIDE.md              # 타 서버코어 포팅 가이드
│  ├─ EGO_WEAPON_APPLY_CHECKLIST.md     # 실전 적용 체크리스트
│  ├─ EGO_OPPONENT_SCAN_GUIDE.md        # 상대감지 상세 설명
│  ├─ EGO_WEAPON_BUGCHECK_AND_WEAPON_TYPE.md
│  ├─ BEGINNER_APPLY_GUIDE.md           # 구 문서, 참고용
│  ├─ EGO_WEAPON_ABILITY_GUIDE.md       # 구 문서, 참고용
│  ├─ EGO_WEAPON_DATABASE_COMMAND_GUIDE.md
│  └─ EGO_WEAPON_DESIGN.md
├─ java/
│  ├─ EgoWeaponTypeUtil.java
│  ├─ EgoWeaponControlController.java
│  ├─ EgoWeaponAbilityController.java
│  ├─ EgoWeaponDatabase.java
│  ├─ EgoWeaponCommand.java
│  ├─ EgoWeaponDiagnostics.java
│  └─ EgoOpponentScanController.java
├─ portable/
│  ├─ EgoCoreAdapter.java
│  └─ EgoPortableRules.java
└─ sql/
   ├─ ego_weapon.sql
   └─ ego_weapon_ability.sql
```

---

## 3. myso 적용 순서 한눈에 보기

```text
1. DB 백업
2. SQL 2개 적용
3. 자바 파일 7개 복사
4. ChattingController 연결
5. CommandController 연결
6. DamageController 연결
7. 서버 시작 시 EgoWeaponDatabase.init(con) 연결 또는 .에고리로드
8. 서버 빌드
9. 게임 접속 후 .에고검사
10. .에고생성 카르마
11. .에고정보 / .에고능력 / 에고 상태 / 에고 상대 테스트
```

---

## 4. 다른 서버코어 적용 순서

```text
1. docs/EGO_PORTING_GUIDE.md 확인
2. portable/EgoCoreAdapter.java를 대상 서버 클래스에 맞게 구현
3. portable/EgoPortableRules.java의 무기 type2 규칙 확인/수정
4. 대상 서버의 채팅 처리부에 에고 호출 연결
5. 대상 서버의 명령어 처리부에 에고 명령 연결
6. 대상 서버의 데미지 계산부에 특별 능력 연결
7. DB 테이블명/컬럼명을 대상 서버에 맞게 조정
8. 대화 → 상태 → 감지 → 능력 → DB 순서로 단계별 테스트
```

핵심 구분:

```text
ego/java/      myso 전용 참고 구현
ego/portable/  타 서버코어 포팅 기반
```

---

## 5. SQL 적용

적용 순서:

```text
ego/sql/ego_weapon.sql
ego/sql/ego_weapon_ability.sql
```

생성 테이블:

```text
character_item_ego
character_item_ego_ability
ego_personality_template
ego_talk_template
ego_ability_template
ego_ability_proc_log
```

운영 DB에는 반드시 백업 후 적용하세요.

---

## 6. myso 자바 파일 복사 위치

### world/controller 경로

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

ego/java/EgoOpponentScanController.java
→ bitna/src/lineage/world/controller/EgoOpponentScanController.java
```

### database 경로

```text
ego/java/EgoWeaponDatabase.java
→ bitna/src/lineage/database/EgoWeaponDatabase.java
```

---

## 7. 기존 자바 연결 코드

### 7.1 ChattingController.java

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

---

### 7.2 CommandController.java

파일:

```text
bitna/src/lineage/world/controller/CommandController.java
```

`PluginController.init(...)` 이후에 추가:

```java
if (EgoWeaponCommand.toCommand(o, key, st)) {
    return true;
}
```

---

### 7.3 DamageController.java

파일:

```text
bitna/src/lineage/world/controller/DamageController.java
```

최종 데미지 반환 직전에 추가합니다.

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

실제 DamageController의 변수명이 다를 수 있으므로 `cha`, `target`, `weapon`, `dmg` 이름은 현재 파일에 맞춰 조정하세요.

---

### 7.4 서버 시작 시 DB 로드

서버 DB 초기화 구간에 추가 권장:

```java
EgoWeaponDatabase.init(con);
```

import:

```java
import lineage.database.EgoWeaponDatabase;
```

처음 위치를 못 찾으면 GM으로 접속 후 아래 명령으로 임시 리로드할 수 있습니다.

```text
.에고리로드
```

---

## 8. 게임 명령어

```text
.에고도움        명령어 안내
.에고검사        착용무기/DB/능력/선공감지 진단
.에고생성 이름   착용 무기를 에고무기로 활성화
.에고정보        착용 에고무기 정보 확인
.에고이름 이름   에고 호출 이름 변경
.에고능력 코드   특별 능력 설정
.에고상대        타겟 또는 가까운 상대 캐릭터 분석
.에고주변        주변 캐릭터 목록/위험도 감지
.에고리로드      DB 캐시 리로드, GM 권장
```

---

## 9. 일반 채팅 사용법

에고 이름이 `에고`일 때:

```text
에고
에고 상태
에고 조언
에고 선공
에고 공격
에고 멈춰
에고 상대
에고 주변캐릭
에고 타겟분석
```

에고 이름을 `카르마`로 만들었다면:

```text
카르마 상태
카르마 조언
카르마 상대
카르마 주변캐릭
카르마 공격
카르마 멈춰
```

---

## 10. 지원 무기 종류

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

portable 규칙에는 아래 변형도 포함되어 있습니다.

```text
twohand_sword
two_handed_sword
crossbow
```

제외:

```text
fishing_rod  낚싯대
방어구
주문서
포션
화살 단독
기타 비무기
```

최종 판정 함수:

```java
EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)
```

portable 기준:

```java
EgoPortableRules.isSupportedWeaponType(type2)
```

---

## 11. 특별 능력

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

무기별 제한:

```text
EGO_BALANCE      모든 에고무기
BLOOD_DRAIN      근접 무기
MANA_DRAIN       지팡이/완드/단검/한손검
CRITICAL_BURST   근접 무기/활
GUARDIAN_SHIELD  모든 에고무기
AREA_SLASH       창/양손검/도끼
EXECUTION        단검/한손검/양손검/도끼
FLAME_BRAND      근접 무기/마법무기
FROST_BIND       지팡이/완드/창/활
```

---

## 12. 상대 캐릭터 감지

명령:

```text
.에고상대
.에고주변
에고 상대
에고 주변캐릭
에고 타겟분석
```

표시 정보:

```text
이름
호칭
혈맹
클래스
성향
PK수
거리
HP 구간
상대 무기 종류
위험도
에고 조언
```

숨기는 정보:

```text
계정
IP
정확한 HP 숫자
정확한 스탯
전체 인벤토리
전체 장비명
숨김 버프 전체
운영자 내부 정보
```

---

## 13. 진단 명령

적용 후 가장 먼저 실행할 명령:

```text
.에고검사
```

확인 항목:

```text
- 캐릭터 기본 상태
- 착용 무기 objectId
- 무기 type2
- 무기 slot
- 에고 가능 무기 여부
- DB 캐시 로드 여부
- 활성 능력 여부
- 능력과 무기 종류 호환 여부
- 주변 선공 몬스터 감지 여부
```

---

## 14. 첫 테스트 순서

```text
.에고검사
.에고생성 카르마
.에고정보
.에고능력 BLOOD_DRAIN
.에고검사
카르마 상태
카르마 조언
카르마 선공
카르마 상대
카르마 주변캐릭
카르마 공격
카르마 멈춰
```

---

## 15. 운영 전 필수 확인

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

그리고 `isEgoWeapon(...)` 내부를 DB 기반으로 변경합니다.

```java
return EgoWeaponDatabase.isEgoWeapon(weapon);
```

---

## 16. 자주 나는 오류

### EgoWeaponTypeUtil 찾을 수 없음

`ego/java/EgoWeaponTypeUtil.java` 복사 누락입니다.

### EgoWeaponDatabase 찾을 수 없음

`EgoWeaponDatabase.java`는 `world/controller`가 아니라 `lineage/database` 경로입니다.

### getInsideList 오류

서버 버전에 따라 주변 객체 getter 이름이 다를 수 있습니다. `object.java`에서 실제 메서드명을 확인하세요.

### ATTACK_TYPE_WEAPON 오류

`Lineage.java`에서 실제 공격 타입 상수명을 확인하고 맞춰야 합니다.

### 타 서버코어에서 lineage 패키지 오류

`ego/java/`를 그대로 복사했기 때문입니다. `docs/EGO_PORTING_GUIDE.md`와 `ego/portable/` 구조로 포팅하세요.

---

## 17. 원복 방법

1. `ChattingController.java`에 추가한 에고 채팅 연결 코드 삭제
2. `CommandController.java`에 추가한 에고 명령 연결 코드 삭제
3. `DamageController.java`에 추가한 특별능력 연결 코드 삭제
4. 복사한 에고 자바 파일 삭제
5. 서버 재빌드

SQL 테이블 삭제:

```sql
DROP TABLE IF EXISTS ego_ability_proc_log;
DROP TABLE IF EXISTS character_item_ego_ability;
DROP TABLE IF EXISTS ego_ability_template;
DROP TABLE IF EXISTS ego_talk_template;
DROP TABLE IF EXISTS ego_personality_template;
DROP TABLE IF EXISTS character_item_ego;
```

---

## 18. 권장 문서 사용법

myso 적용:

```text
README.md → EGO_SYSTEM_MANUAL.md → EGO_WEAPON_APPLY_CHECKLIST.md
```

타 서버코어 적용:

```text
README.md → EGO_PORTING_GUIDE.md → portable/EgoCoreAdapter.java → portable/EgoPortableRules.java
```

문제 해결:

```text
.에고검사 실행 → EGO_WEAPON_BUGCHECK_AND_WEAPON_TYPE.md 확인
```

상대감지 상세:

```text
EGO_OPPONENT_SCAN_GUIDE.md
```
