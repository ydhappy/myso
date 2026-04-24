# 에고무기 시스템

`ego/` 폴더는 에고무기 기능을 적용하기 위한 **자바 소스, SQL, 적용 문서, 타 서버코어 포팅 자료**를 모아둔 작업 폴더입니다.

기본 구현은 `ydhappy/myso` 구조를 기준으로 작성되어 있지만, 다른 서버코어에서 사용할 수 있도록 `ego/portable/`에 코어 독립 인터페이스와 순수 규칙 유틸을 추가했습니다.

Java 8 환경을 기준으로 정리했습니다.

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

---

## 1. 먼저 볼 문서

myso 서버에 적용할 때:

```text
1. README.md
2. docs/EGO_SYSTEM_MANUAL.md
3. docs/EGO_WEAPON_APPLY_CHECKLIST.md
```

Java를 더 이상 수정하지 않고 DB에서 생성/편집만 할 때:

```text
1. docs/EGO_NO_JAVA_ADMIN_GUIDE.md
2. sql/ego_no_java_admin.sql
```

다른 서버코어에 적용할 때:

```text
1. README.md
2. docs/EGO_PORTING_GUIDE.md
3. docs/EGO_JAVA8_COMPATIBILITY.md
4. portable/EgoCoreAdapter.java
5. portable/EgoPortableRules.java
```

상세 문서:

```text
docs/EGO_NO_JAVA_ADMIN_GUIDE.md             무자바 DB 생성/편집 가이드
docs/EGO_JAVA8_COMPATIBILITY.md             Java 8 호환성 기준
docs/EGO_OPPONENT_SCAN_GUIDE.md             상대 캐릭터 감지 상세
docs/EGO_WEAPON_BUGCHECK_AND_WEAPON_TYPE.md 오류/무기종류/능력제한 점검
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
- Java 수정 없이 DB에서 에고 생성/편집 가능
- .에고검사 진단 명령
- 타 서버코어 포팅용 Adapter/Rules 제공
- Java 8 호환 기준 문서화
```

---

## 3. 현재 폴더 구성

```text
ego/
├─ README.md
├─ docs/
│  ├─ EGO_SYSTEM_MANUAL.md
│  ├─ EGO_NO_JAVA_ADMIN_GUIDE.md
│  ├─ EGO_PORTING_GUIDE.md
│  ├─ EGO_JAVA8_COMPATIBILITY.md
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
├─ portable/
│  ├─ EgoCoreAdapter.java
│  └─ EgoPortableRules.java
└─ sql/
   ├─ ego_weapon.sql
   ├─ ego_weapon_ability.sql
   └─ ego_no_java_admin.sql
```

---

## 4. myso 기준 빠른 적용 순서

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

## 5. Java 수정 없이 DB로 생성/편집

이미 에고 시스템이 서버코어에 연결되어 있다면, 이후 생성/편집은 Java를 건드리지 않고 DB에서 처리할 수 있습니다.

사용 파일:

```text
sql/ego_no_java_admin.sql
docs/EGO_NO_JAVA_ADMIN_GUIDE.md
```

지원 작업:

```text
- 에고 생성/재활성화
- 에고 이름 변경
- 에고 성격 변경
- 에고 능력 변경
- 에고 레벨/경험치 보정
- 에고 능력 레벨/확률/피해 보정
- 에고 비활성화
- 에고 완전 삭제
- 전체 에고 목록 조회
- 이상 데이터 점검/보정
```

주의:

```text
SQL은 데이터 생성/편집용입니다.
서버코어가 에고 테이블을 읽는 Java 연결이 전혀 없다면 SQL만으로 게임 기능이 발동하지 않습니다.
DB 수정 후에는 .에고리로드 또는 서버 재시작이 필요할 수 있습니다.
```

---

## 6. 타 서버코어 적용 순서

다른 서버코어에 적용할 때는 `ego/java/` 파일을 바로 복사하지 말고, 먼저 포팅 가이드를 보세요.

```text
1. docs/EGO_PORTING_GUIDE.md 확인
2. docs/EGO_JAVA8_COMPATIBILITY.md 확인
3. portable/EgoCoreAdapter.java 구현
4. portable/EgoPortableRules.java 규칙 확인/무기 type2 추가
5. 대상 서버의 채팅/명령/데미지/DB 위치에 연결
6. 대화 기능부터 단계별 테스트
```

핵심:

```text
- ego/java/ : myso 전용 참고 구현
- ego/portable/ : 다른 서버코어용 포팅 기반
- Java 기준 : 1.8 / UTF-8
```

---

## 7. SQL 파일

```text
sql/ego_weapon.sql
sql/ego_weapon_ability.sql
sql/ego_no_java_admin.sql
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

다른 서버코어에서는 캐릭터/아이템 objectId 컬럼명에 맞게 SQL을 조정하세요.

---

## 8. myso 자바 파일 복사 위치

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

## 9. 게임 명령어

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

## 10. 일반 채팅 명령

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

## 11. 지원 무기 종류

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

portable 규칙에는 아래 변형도 일부 포함했습니다.

```text
twohand_sword
two_handed_sword
crossbow
```

제외:

```text
fishing_rod  낚싯대
방어구/주문서/포션/기타 비무기
```

---

## 12. 특별 능력

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

## 13. 상대 감지 정보 범위

표시:

```text
이름, 호칭, 혈맹, 클래스, 성향, PK수, 거리, HP구간, 무기종류, 위험도, 에고 조언
```

숨김:

```text
계정, IP, 정확한 HP 숫자, 전체 인벤토리, 정확 스탯, 전체 장비명, 숨김 버프 전체
```

---

## 14. 안전장치

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
- 타 서버코어 포팅 시 Adapter로 코어 종속성 분리
- Java 8 기준 문법만 사용
- 운영 생성/편집은 SQL로 처리 가능
```

---

## 15. 운영 전 주의

`EgoWeaponAbilityController.java`의 테스트 모드를 확인하세요.

```java
private static final boolean ENABLE_TEST_MODE = true;
```

운영 전에는 DB 기반 판정으로 전환하고 아래처럼 변경하는 것을 권장합니다.

```java
private static final boolean ENABLE_TEST_MODE = false;
```

자세한 내용은 `docs/EGO_SYSTEM_MANUAL.md`의 운영 전 필수 확인 항목을 참고하세요.

다른 서버코어에서는 `docs/EGO_PORTING_GUIDE.md`를 먼저 보고 `portable/EgoCoreAdapter.java`를 구현하세요.

Java 8 빌드 문제는 `docs/EGO_JAVA8_COMPATIBILITY.md`를 확인하세요.
