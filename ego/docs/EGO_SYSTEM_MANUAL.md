# 에고무기 통합 매뉴얼

이 문서 하나로 에고무기 설치, 적용, 운영, 포팅, Java 8 주의사항을 모두 확인할 수 있습니다.

---

## 1. 최종 축소 구조

```text
ego/
├─ README.md
├─ docs/
│  └─ EGO_SYSTEM_MANUAL.md
├─ install/
│  ├─ install_ego_windows.bat
│  └─ install_ego_linux.sh
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
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

기존 분산 문서는 이 매뉴얼로 통합했습니다.
기존 분산 SQL은 `ego_oneclick_install.sql`로 통합했습니다.

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

에고 소스는 Java 8 기준으로 작성합니다.

---

## 4. 기능 요약

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
- 타 서버코어 포팅용 portable 제공
```

---

## 5. myso 적용 순서

```text
1. DB 백업
2. ego/install/install_ego_windows.bat 또는 install_ego_linux.sh 실행
3. java 파일 7개 복사
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

## 6. 자바 파일 복사 위치

### world/controller

```text
EgoWeaponTypeUtil.java
EgoWeaponControlController.java
EgoWeaponAbilityController.java
EgoWeaponCommand.java
EgoWeaponDiagnostics.java
EgoOpponentScanController.java
```

복사 위치:

```text
bitna/src/lineage/world/controller/
```

### database

```text
EgoWeaponDatabase.java
```

복사 위치:

```text
bitna/src/lineage/database/
```

---

## 7. 기존 자바 연결 코드

### ChattingController.java

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

### CommandController.java

`PluginController.init(...)` 이후:

```java
if (EgoWeaponCommand.toCommand(o, key, st)) {
    return true;
}
```

### DamageController.java

최종 데미지 반환 직전:

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

실제 변수명은 서버 파일에 맞게 조정하세요.

### 서버 시작 시 DB 로드

```java
EgoWeaponDatabase.init(con);
```

임시 대체:

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

## 9. 무기 종류와 능력

지원 무기:

```text
dagger, sword, tohandsword, axe, spear, bow, staff, wand
```

portable 추가 지원:

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

## 10. Java 수정 없이 생성/편집

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

## 12. 타 서버코어 포팅

다른 서버코어에서는 `ego/java/`를 그대로 복사하지 말고 아래 파일을 기준으로 포팅하세요.

```text
ego/portable/EgoCoreAdapter.java
ego/portable/EgoPortableRules.java
```

순서:

```text
1. EgoCoreAdapter를 대상 서버 클래스에 맞게 구현
2. EgoPortableRules의 type2 규칙 확인
3. 채팅 처리부 연결
4. 명령어 처리부 연결
5. 데미지 계산부 연결
6. DB 테이블/컬럼명 조정
7. 대화 → 상태 → 감지 → 능력 → DB 순서로 테스트
```

---

## 13. 오류 대응

```text
EgoWeaponTypeUtil 없음       → java 파일 복사 누락
EgoWeaponDatabase 없음       → database 경로에 복사해야 함
getInsideList 없음           → 서버 주변객체 메서드명 확인
ATTACK_TYPE_WEAPON 없음      → Lineage 공격 타입 상수명 확인
lineage 패키지 오류          → 타 서버는 portable 기반 포팅 필요
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
