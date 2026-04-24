# 에고무기 시스템 작업 폴더

이 폴더는 `ydhappy/myso` 서버에 **에고무기 대화/상태인식/제어/특별능력/DB저장/무기종류검사/진단 기능**을 적용하기 위한 준비 자료입니다.

본 폴더는 기존 서버 코어를 직접 수정하지 않고, 초보자도 단계별로 복사/붙여넣기 할 수 있도록 자바 코드, SQL, 적용 문서를 분리합니다.

## 목표

에고무기는 단순한 무기 옵션이 아니라, 착용 중인 무기에 깃든 인격처럼 동작합니다.

가능 기능:

- 에고 이름을 일반 채팅으로 부르면 자동 반응
- 내 캐릭터 HP, MP, 레벨, 무기, 현재 타겟 상태 인식
- 주변 선공 몬스터 감지
- 선공 몬스터가 보이면 경고
- 명령에 따라 가장 가까운 선공 몬스터를 자동공격 대상으로 지정
- 명령에 따라 자동공격 중지
- 에고 이름/성격/레벨/경험치 DB 저장
- 에고 특별 능력 발동
- 무기 종류별 능력 제한
- 낚싯대/비무기 에고화 방지
- `.에고검사`로 착용무기/DB/능력/선공감지 진단
- 게임 안에서 `.에고생성`, `.에고정보`, `.에고이름`, `.에고능력` 명령으로 관리

## 폴더 구성

```text
ego/
├─ README.md
├─ docs/
│  ├─ BEGINNER_APPLY_GUIDE.md
│  ├─ EGO_WEAPON_ABILITY_GUIDE.md
│  ├─ EGO_WEAPON_APPLY_CHECKLIST.md
│  ├─ EGO_WEAPON_BUGCHECK_AND_WEAPON_TYPE.md
│  ├─ EGO_WEAPON_DATABASE_COMMAND_GUIDE.md
│  └─ EGO_WEAPON_DESIGN.md
├─ java/
│  ├─ EgoWeaponAbilityController.java
│  ├─ EgoWeaponCommand.java
│  ├─ EgoWeaponControlController.java
│  ├─ EgoWeaponDatabase.java
│  ├─ EgoWeaponDiagnostics.java
│  └─ EgoWeaponTypeUtil.java
└─ sql/
   ├─ ego_weapon.sql
   └─ ego_weapon_ability.sql
```

## 적용 순서

### 1단계: 대화/상태인식/간단제어

1. `ego/sql/ego_weapon.sql`을 DB에 적용합니다.
2. `ego/java/EgoWeaponTypeUtil.java`를 `bitna/src/lineage/world/controller/`로 복사합니다.
3. `ego/java/EgoWeaponControlController.java`를 `bitna/src/lineage/world/controller/`로 복사합니다.
4. `ego/docs/BEGINNER_APPLY_GUIDE.md`를 보면서 `ChattingController.java`에 연결 코드를 추가합니다.
5. 서버를 빌드합니다.
6. 게임에서 착용 무기를 들고 아래처럼 테스트합니다.

```text
에고
에고 상태
에고 조언
에고 선공
에고 공격
에고 멈춰
```

### 2단계: 특별 능력

1. `ego/sql/ego_weapon_ability.sql`을 DB에 적용합니다.
2. `ego/java/EgoWeaponTypeUtil.java`가 복사되어 있는지 확인합니다.
3. `ego/java/EgoWeaponAbilityController.java`를 `bitna/src/lineage/world/controller/`로 복사합니다.
4. `ego/docs/EGO_WEAPON_ABILITY_GUIDE.md`를 보면서 `DamageController.java`에 연결 코드를 추가합니다.
5. 몬스터 공격 시 특별 능력 발동을 테스트합니다.

### 3단계: DB 저장/게임 내 명령어/진단

1. `ego/java/EgoWeaponDatabase.java`를 `bitna/src/lineage/database/`로 복사합니다.
2. `ego/java/EgoWeaponTypeUtil.java`, `EgoWeaponDiagnostics.java`, `EgoWeaponCommand.java`를 `bitna/src/lineage/world/controller/`로 복사합니다.
3. `ego/docs/EGO_WEAPON_DATABASE_COMMAND_GUIDE.md`를 보면서 `CommandController.java`에 연결합니다.
4. 서버 로딩 시 `EgoWeaponDatabase.init(con)`을 연결하거나, GM으로 `.에고리로드`를 실행합니다.
5. 게임에서 아래 명령을 테스트합니다.

```text
.에고도움
.에고검사
.에고생성 카르마
.에고정보
.에고이름 루나
.에고능력 BLOOD_DRAIN
.에고리로드
```

## 지원 무기 종류

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
방어구/소모품/주문서/포션/기타 비무기
```

## 특별 능력 목록

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

## 상세 점검 문서

```text
ego/docs/EGO_WEAPON_APPLY_CHECKLIST.md
```

복사 파일, SQL, ChattingController, CommandController, DamageController, DB 로드, `.에고검사`까지 전체 적용 체크리스트입니다.

## 오류/버그 점검 문서

```text
ego/docs/EGO_WEAPON_BUGCHECK_AND_WEAPON_TYPE.md
```

무기 종류 확인, 낚싯대 차단, 능력 중복 버그, 무기별 능력 제한이 정리되어 있습니다.

## 1차 동작 방식

1차 버전은 안전성을 위해 완전 자동사냥이 아닙니다.

- 사용자가 에고 이름을 말해야 반응합니다.
- `에고 공격` 명령 시 주변 선공 몬스터 중 가장 가까운 대상만 자동공격 대상으로 지정합니다.
- `에고 멈춰` 명령 시 자동공격을 중지합니다.
- 자동 귀환, 자동 이동, 자동 물약 난사는 포함하지 않았습니다.

## 보강된 안전장치

- 정상 전투 무기만 에고 생성 가능
- 낚싯대 제외
- 무기별 능력 제한
- 능력 교체 시 기존 능력 비활성화
- 데미지 0 이하일 때 능력 발동 차단
- 능력 발동 메시지 쿨타임 적용
- 광역 피해는 몬스터에만 제한
- `.에고검사`로 현장 진단 가능

## 나중에 확장할 기능

- 몬스터 처치 시 에고 경험치 완전 자동 지급
- 에고 레벨업 시 능력 강화
- 에고 성격별 말투 변경
- 위험 상황 자동 경고 주기 제한
- HTML 상태창 출력
- LLM/AI 멘트 연동
- 에고 전용 퀘스트/각성 시스템

## 주의

이 폴더의 자바 파일은 바로 서버 경로에 들어간 상태가 아니라, 적용용 원본입니다.
실제 적용 시에는 각 문서의 순서대로 복사하고 기존 파일에 연결 코드를 추가해야 합니다.

특히 `EgoWeaponAbilityController.java`의 테스트 모드 값을 확인하세요.
운영 서버에서는 모든 무기 발동을 막기 위해 DB 기반 에고무기 판정으로 바꾸는 것을 권장합니다.
