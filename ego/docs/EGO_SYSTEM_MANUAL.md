# 에고무기 통합 매뉴얼

이 문서 하나로 에고무기 설치, 적용, 운영, 다른 서버코어 포팅, Java 8 주의사항을 모두 확인할 수 있습니다.

핵심 원칙:

```text
Java 파일은 전부 ego/java/ 한 곳에 둡니다.
설치는 ego/install/ 원클릭 스크립트로 진행합니다.
문서는 README.md와 이 문서만 봅니다.
```

---

## 1. 최종 구조

```text
ego/
├─ README.md
├─ docs/
│  └─ EGO_SYSTEM_MANUAL.md
├─ install/
│  ├─ install_ego_windows.bat
│  └─ install_ego_linux.sh
├─ java/
│  ├─ EgoWeaponTypeUtil.java              # myso 무기종류 판정
│  ├─ EgoWeaponControlController.java     # myso 대화/상태/선공/제어
│  ├─ EgoWeaponAbilityController.java     # myso 특별능력
│  ├─ EgoWeaponDatabase.java              # myso DB 로드/저장
│  ├─ EgoWeaponCommand.java               # myso 명령어
│  ├─ EgoWeaponDiagnostics.java           # myso 진단
│  ├─ EgoOpponentScanController.java      # myso 상대감지
│  ├─ EgoCoreAdapter.java                 # 다른 서버코어 포팅용
│  └─ EgoPortableRules.java               # 다른 서버코어 공통 규칙
└─ sql/
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

---

## 2. 원클릭 설치

### Windows

```text
ego/install/install_ego_windows.bat
```

더블클릭 후 DB 정보를 입력합니다.

### Linux/macOS

```bash
chmod +x ego/install/install_ego_linux.sh
./ego/install/install_ego_linux.sh
```

설치 스크립트는 내부적으로 아래 SQL을 실행합니다.

```text
ego/sql/ego_oneclick_install.sql
```

설치 후 서버가 DB 캐시를 쓰면 아래 중 하나가 필요합니다.

```text
.에고리로드
또는 서버 재시작
```

---

## 3. Java 8 기준

권장 컴파일 옵션:

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

금지 문법:

```text
var, record, sealed, switch expression, text block, List.of, Map.of, Set.of, Stream.toList, module-info.java
```

---

## 4. 어떤 Java 파일을 써야 하나?

### 4.1 myso 서버에 적용할 때

아래 7개가 myso 기준 구현입니다.

```text
ego/java/EgoWeaponTypeUtil.java
ego/java/EgoWeaponControlController.java
ego/java/EgoWeaponAbilityController.java
ego/java/EgoWeaponDatabase.java
ego/java/EgoWeaponCommand.java
ego/java/EgoWeaponDiagnostics.java
ego/java/EgoOpponentScanController.java
```

복사 위치:

```text
EgoWeaponTypeUtil.java
EgoWeaponControlController.java
EgoWeaponAbilityController.java
EgoWeaponCommand.java
EgoWeaponDiagnostics.java
EgoOpponentScanController.java
→ bitna/src/lineage/world/controller/

EgoWeaponDatabase.java
→ bitna/src/lineage/database/
```

### 4.2 다른 서버코어에 적용할 때

먼저 아래 2개만 봅니다.

```text
ego/java/EgoCoreAdapter.java
ego/java/EgoPortableRules.java
```

초보자 기준 설명:

```text
EgoCoreAdapter.java   = 서버마다 다른 클래스명/메서드명을 맞추는 연결표
EgoPortableRules.java = 서버와 무관한 무기/능력/HP/위험도 규칙
```

다른 서버코어에서는 `EgoWeapon*.java`를 그대로 복사하면 `lineage.*`, `PcInstance`, `ItemInstance` 오류가 날 수 있습니다.
먼저 `EgoCoreAdapter` 방식으로 맞추는 것이 안전합니다.

---

## 5. 기능 요약

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
- Java 수정 없이 SQL로 생성/편집
- 다른 서버코어 포팅용 Adapter/Rules 제공
```

---

## 6. myso 적용 순서

```text
1. DB 백업
2. ego/install/install_ego_windows.bat 또는 install_ego_linux.sh 실행
3. myso용 java 파일 7개 복사
4. ChattingController 연결
5. CommandController 연결
6. DamageController 연결
7. EgoWeaponDatabase.init(con) 연결 또는 .에고리로드
8. 서버 빌드
9. 게임 접속 후 .에고검사
10. .에고생성 카르마
11. .에고정보 / 카르마 상태 / 카르마 상대 테스트
```

---

## 7. myso 기존 자바 연결 코드

### 7.1 ChattingController.java

`if (!CommandController.toCommand(o, msg)) {` 바로 아래:

```java
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoWeaponControlController.onNormalChat((PcInstance) o, msg)) {
        return;
    }
}
```

필요 시 import:

```java
import lineage.world.object.instance.PcInstance;
import lineage.world.object.instance.RobotInstance;
```

### 7.2 CommandController.java

`PluginController.init(...)` 이후:

```java
if (EgoWeaponCommand.toCommand(o, key, st)) {
    return true;
}
```

### 7.3 DamageController.java

최종 데미지 반환 직전:

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

실제 변수명은 서버 파일에 맞게 조정하세요.

### 7.4 서버 시작 시 DB 로드

```java
EgoWeaponDatabase.init(con);
```

임시 대체:

```text
.에고리로드
```

---

## 8. 다른 서버코어 초보자 적용 방법

다른 서버코어는 클래스명이 다릅니다.

예:

```text
myso: PcInstance, ItemInstance, MonsterInstance
다른 서버: L1PcInstance, L1ItemInstance, L1MonsterInstance
또 다른 서버: Player, Item, Npc
```

그래서 바로 `EgoWeaponControlController.java`를 복사하면 오류가 날 수 있습니다.
아래 순서로 진행하세요.

### 8.1 1단계: DB만 먼저 설치

```text
ego/install/install_ego_windows.bat
또는
ego/install/install_ego_linux.sh
```

### 8.2 2단계: 서버 클래스명 확인

찾을 것:

```text
플레이어 클래스명
아이템 클래스명
몬스터 클래스명
채팅 메시지 보내는 메서드
착용 무기 가져오는 메서드
주변 객체 가져오는 메서드
데미지 계산 위치
```

자주 나오는 이름:

```text
플레이어: PcInstance, L1PcInstance, Player, PlayerInstance
아이템: ItemInstance, L1ItemInstance, Item
몬스터: MonsterInstance, L1MonsterInstance, NpcInstance
주변객체: getInsideList, getKnownObjects, getVisibleObjects, World.getVisibleObjects
```

### 8.3 3단계: EgoCoreAdapter 구현

파일:

```text
ego/java/EgoCoreAdapter.java
```

이 인터페이스를 대상 서버에 맞게 구현합니다.

예시:

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
    public void sendSystemMessage(Object player, String message) {
        ((L1PcInstance) player).sendPackets(new S_SystemMessage(message));
    }

    // 나머지 메서드도 서버코어에 맞게 연결
}
```

### 8.4 4단계: EgoPortableRules 확인

파일:

```text
ego/java/EgoPortableRules.java
```

서버의 무기 type2 이름이 다르면 여기에 추가합니다.

예:

```text
myso: tohandsword
다른 서버: twohand_sword, two_handed_sword
```

### 8.5 5단계: 단계별 테스트

한 번에 다 붙이지 말고 아래 순서로 켭니다.

```text
1. 에고 대화
2. 에고 상태
3. 선공 몬스터 감지
4. 상대 캐릭터 감지
5. 특별 능력
6. DB 저장/로드
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
.에고리로드      DB 캐시 리로드
```

일반 채팅:

```text
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

## 10. 무기 종류와 능력

지원 무기:

```text
dagger, sword, tohandsword, axe, spear, bow, staff, wand
```

다른 서버 호환용 추가 지원:

```text
twohand_sword, two_handed_sword, crossbow
```

제외:

```text
fishing_rod, 방어구, 주문서, 포션, 기타 비무기
```

능력:

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

## 11. Java 수정 없이 생성/편집

이미 에고 시스템이 서버에 연결되어 있다면 운영 중 생성/편집은 SQL만으로 가능합니다.

사용 파일:

```text
ego/sql/ego_no_java_admin.sql
```

지원 작업:

```text
에고 생성, 이름변경, 성격변경, 능력변경, 레벨/경험치 보정, 비활성화, 삭제, 전체 조회, 이상 데이터 보정
```

DB 수정 후:

```text
.에고리로드
또는 서버 재시작
```

---

## 12. 상대 감지 정보 범위

표시:

```text
이름, 호칭, 혈맹, 클래스, 성향, PK수, 거리, HP구간, 무기종류, 위험도, 에고 조언
```

숨김:

```text
계정, IP, 정확한 HP 숫자, 전체 인벤토리, 정확 스탯, 전체 장비명, 숨김 버프 전체
```

---

## 13. 오류 대응

```text
EgoWeaponTypeUtil 없음       → myso용 java 파일 복사 누락
EgoWeaponDatabase 없음       → database 경로에 복사해야 함
getInsideList 없음           → 서버 주변객체 메서드명 확인
ATTACK_TYPE_WEAPON 없음      → Lineage 공격 타입 상수명 확인
lineage 패키지 오류          → 다른 서버는 EgoCoreAdapter/EgoPortableRules 기준으로 포팅
한글 깨짐                   → UTF-8 저장/컴파일 필요
```

---

## 14. 원복

자바 연결 제거:

```text
ChattingController 연결 코드 삭제
CommandController 연결 코드 삭제
DamageController 연결 코드 삭제
복사한 에고 java 파일 삭제
```

DB 테이블 삭제:

```sql
DROP TABLE IF EXISTS ego_ability_proc_log;
DROP TABLE IF EXISTS character_item_ego_ability;
DROP TABLE IF EXISTS ego_ability_template;
DROP TABLE IF EXISTS ego_talk_template;
DROP TABLE IF EXISTS ego_personality_template;
DROP TABLE IF EXISTS character_item_ego;
```
