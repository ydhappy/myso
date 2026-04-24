# 에고무기 시스템

`ego/` 폴더는 `ydhappy/myso` 서버에 에고무기 기능을 적용하기 위한 **자바 소스, SQL, 적용 문서**를 모아둔 작업 폴더입니다.

기존 서버 코어를 바로 수정하지 않고, 필요한 파일을 복사한 뒤 기존 컨트롤러에 연결하는 방식으로 구성했습니다.

---

## 1. 먼저 볼 문서

처음 적용하거나 전체 구조를 확인할 때는 아래 순서로 보세요.

```text
1. README.md
2. docs/EGO_SYSTEM_MANUAL.md
3. docs/EGO_WEAPON_APPLY_CHECKLIST.md
```

상세 문서:

```text
docs/EGO_OPPONENT_SCAN_GUIDE.md              상대 캐릭터 감지 상세
docs/EGO_WEAPON_BUGCHECK_AND_WEAPON_TYPE.md  오류/무기종류/능력제한 점검
```

기존 세부 문서들은 참고용으로 유지합니다.

---

## 2. 기능 요약

에고무기는 착용 무기에 인격을 부여하여 캐릭터 상태와 주변 상황을 인식하는 시스템입니다.

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
```

---

## 3. 현재 폴더 구성

```text
ego/
├─ README.md
├─ docs/
│  ├─ EGO_SYSTEM_MANUAL.md
│  ├─ EGO_WEAPON_APPLY_CHECKLIST.md
│  ├─ EGO_OPPONENT_SCAN_GUIDE.md
│  ├─ EGO_WEAPON_BUGCHECK_AND_WEAPON_TYPE.md
│  ├─ BEGINNER_APPLY_GUIDE.md
│  ├─ EGO_WEAPON_ABILITY_GUIDE.md
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
└─ sql/
   ├─ ego_weapon.sql
   └─ ego_weapon_ability.sql
```

---

## 4. 빠른 적용 순서

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
11. .에고정보 / .에고능력 / 카르마 상태 / 카르마 상대 테스트
```

자세한 코드는 아래 문서를 보세요.

```text
docs/EGO_SYSTEM_MANUAL.md
```

---

## 5. SQL 파일

```text
sql/ego_weapon.sql
sql/ego_weapon_ability.sql
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

---

## 6. 자바 파일 복사 위치

### world/controller

```text
java/EgoWeaponTypeUtil.java
java/EgoWeaponControlController.java
java/EgoWeaponAbilityController.java
java/EgoWeaponCommand.java
java/EgoWeaponDiagnostics.java
java/EgoOpponentScanController.java
```

복사 위치:

```text
bitna/src/lineage/world/controller/
```

### database

```text
java/EgoWeaponDatabase.java
```

복사 위치:

```text
bitna/src/lineage/database/
```

---

## 7. 게임 명령어

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

## 8. 일반 채팅 명령

에고 이름이 `카르마`라면:

```text
카르마
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
카르마 상대
카르마 주변캐릭
카르마 타겟분석
```

---

## 9. 지원 무기 종류

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

제외:

```text
fishing_rod  낚싯대
방어구/주문서/포션/기타 비무기
```

---

## 10. 특별 능력

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

---

## 11. 상대 감지 정보 범위

표시:

```text
이름, 호칭, 혈맹, 클래스, 성향, PK수, 거리, HP구간, 무기종류, 위험도, 에고 조언
```

숨김:

```text
계정, IP, 정확한 HP 숫자, 전체 인벤토리, 정확 스탯, 전체 장비명, 숨김 버프 전체
```

---

## 12. 안전장치

```text
- 정상 전투 무기만 에고 생성 가능
- 낚싯대 제외
- 무기별 능력 제한
- 능력 교체 시 기존 능력 비활성화
- 데미지 0 이하일 때 능력 발동 차단
- 능력 발동 메시지 쿨타임 적용
- 광역 피해는 몬스터에만 제한
- .에고검사로 현장 진단 가능
- 상대 캐릭터 정확 스탯/정확 HP/인벤토리/IP 비공개
```

---

## 13. 운영 전 주의

`EgoWeaponAbilityController.java`의 테스트 모드를 확인하세요.

```java
private static final boolean ENABLE_TEST_MODE = true;
```

운영 전에는 DB 기반 판정으로 전환하고 아래처럼 변경하는 것을 권장합니다.

```java
private static final boolean ENABLE_TEST_MODE = false;
```

자세한 내용은 `docs/EGO_SYSTEM_MANUAL.md`의 운영 전 필수 확인 항목을 참고하세요.
