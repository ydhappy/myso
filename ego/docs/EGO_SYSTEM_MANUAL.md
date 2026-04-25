# 에고무기 통합 매뉴얼

이 문서 하나로 에고무기 설치, 적용, 운영, 다른 서버코어 포팅, Java 8 주의사항을 확인합니다.

핵심 원칙:

```text
Java 파일은 전부 ego/java/ 한 곳에 둡니다.
설치는 ego/install/ 원클릭 스크립트로 진행합니다.
문서는 README.md와 이 문서만 봅니다.
에고 호출은 일반채팅으로 입력하지만, 호출/응답은 다른 캐릭터에게 보이지 않게 처리합니다.
에고무기는 다른 아이템으로 교체되는 것이 아니라, 자기 자신이 형태변환합니다.
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
│  ├─ EgoMessageUtil.java
│  ├─ EgoWeaponTypeUtil.java
│  ├─ EgoWeaponFormController.java
│  ├─ EgoWeaponControlController.java
│  ├─ EgoWeaponAbilityController.java
│  ├─ EgoWeaponDatabase.java
│  ├─ EgoWeaponCommand.java
│  ├─ EgoOpponentScanController.java
│  ├─ EgoCoreAdapter.java
│  └─ EgoPortableRules.java
└─ sql/
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

---

## 2. 원클릭 설치

Windows:

```text
ego/install/install_ego_windows.bat
```

Linux/macOS:

```bash
chmod +x ego/install/install_ego_linux.sh
./ego/install/install_ego_linux.sh
```

설치 SQL:

```text
ego/sql/ego_oneclick_install.sql
```

설치 후:

```text
.에고리로드
또는 서버 재시작
```

---

## 3. Java 8 기준

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

금지:

```text
var, record, sealed, switch expression, text block, List.of, Map.of, Set.of, Stream.toList, module-info.java
```

---

## 4. myso 적용 파일

아래 8개를 `bitna/src/lineage/world/controller/`로 복사합니다.

```text
EgoMessageUtil.java
EgoWeaponTypeUtil.java
EgoWeaponFormController.java
EgoWeaponControlController.java
EgoWeaponAbilityController.java
EgoWeaponCommand.java
EgoOpponentScanController.java
```

아래 1개는 `bitna/src/lineage/database/`로 복사합니다.

```text
EgoWeaponDatabase.java
```

다른 서버코어는 먼저 아래 2개를 기준으로 포팅합니다.

```text
EgoCoreAdapter.java
EgoPortableRules.java
```

---

## 5. 에고 형태변환 구조

에고무기는 인벤토리에서 다른 무기로 교체되지 않습니다.
착용 중인 에고무기 1개가 DB 값으로 형태만 바뀝니다.

```text
원본 type2      : 아이템 DB 템플릿 값, 변경 금지
에고 형태값     : character_item_ego.ego_form_type
방패 복구값     : character_item_ego.prev_shield_objid
```

예시:

```text
카르마 활       → 에고 활 형태, 방패 자동 해제
카르마 양검     → 에고 양손검 형태, 방패 자동 해제
카르마 한검     → 에고 한손검 형태, 이전 방패 자동 복구 시도
카르마 단검     → 에고 단검 형태, 이전 방패 자동 복구 시도
카르마 지팡이   → 에고 지팡이 형태, 방패 자동 해제
카르마 완드     → 에고 완드 형태, 이전 방패 자동 복구 시도
```

지원 형태:

```text
dagger, sword, tohandsword, axe, spear, bow, staff, wand
```

---

## 6. 대화 표시 정책

```text
입력: 일반 채팅으로 "카르마 활" 입력
처리: EgoWeaponControlController.onNormalChat(...)가 true 반환
효과: 일반 채팅 방송 차단
출력: EgoMessageUtil이 Lineage.CHATTING_MODE_MESSAGE로 본인에게만 전송
결과: 다른 캐릭터에게 에고 호출/응답이 보이지 않음
```

색상:

```text
일반/정상: \fY
위험/실패: \fR
정보/안내: \fS
```

ChattingController 연결 위치는 일반채팅 방송보다 앞이어야 합니다.

```java
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoWeaponControlController.onNormalChat((PcInstance) o, msg)) {
        return;
    }
}
```

---

## 7. 기능 요약

```text
- 일반채팅 자연어 대화
- 일반채팅 호출 consume 처리
- 본인 전용 개인 메시지 출력
- 색상 자동 주입
- 에고무기 자체 형태변환
- 활/양손검/창/도끼/지팡이 형태 시 방패 자동 해제
- 한손검/단검/완드 형태 시 직전 방패 자동 복구 시도
- 캐릭터 HP/MP/타겟 상태 인식
- 선공 몬스터 감지
- 상대 캐릭터 위험도 분석
- 간단 자동공격 제어
- 특별 능력 발동
- 에고 이름/레벨/경험치/능력/형태 DB 저장
- SQL로 운영 생성/편집
```

---

## 8. myso 적용 순서

```text
1. DB 백업
2. install_ego_windows.bat 또는 install_ego_linux.sh 실행
3. myso용 java 파일 복사
4. ChattingController 연결
5. CommandController 연결
6. DamageController 연결
7. EgoWeaponDatabase.init(con) 연결 또는 .에고리로드
8. 서버 빌드
9. .에고생성 카르마
10. 카르마 상태
11. 카르마 활
12. 카르마 양검
13. 카르마 한검
14. 카르마 상대
15. .에고정보
```

---

## 9. 기존 자바 연결 코드

### ChattingController.java

일반 채팅 방송 처리보다 먼저:

```java
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoWeaponControlController.onNormalChat((PcInstance) o, msg)) {
        return;
    }
}
```

### CommandController.java

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

### DB 로드

```java
EgoWeaponDatabase.init(con);
```

---

## 10. 일반채팅 명령

```text
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
카르마 상대
카르마 주변캐릭
카르마 타겟분석
카르마 활
카르마 양검
카르마 양손검
카르마 한검
카르마 한손검
카르마 단검
카르마 창
카르마 도끼
카르마 지팡이
카르마 완드
```

점 명령어:

```text
.에고도움
.에고생성 이름
.에고정보
.에고이름 이름
.에고능력 코드
.에고상대
.에고주변
.에고리로드
```

---

## 11. Java 수정 없이 생성/편집

사용 파일:

```text
ego/sql/ego_no_java_admin.sql
```

지원 작업:

```text
에고 생성, 이름변경, 성격변경, 능력변경, 형태변경, 레벨/경험치 보정, 비활성화, 삭제, 전체 조회, 이상 데이터 보정
```

DB 수정 후:

```text
.에고리로드
또는 서버 재시작
```

---

## 12. 다른 서버코어 포팅

서버마다 클래스명이 다릅니다.

```text
플레이어: PcInstance, L1PcInstance, Player
아이템: ItemInstance, L1ItemInstance, Item
몬스터: MonsterInstance, L1MonsterInstance, NpcInstance
주변객체: getInsideList, getKnownObjects, getVisibleObjects, World.getVisibleObjects
```

다른 서버코어에서는 먼저 `EgoCoreAdapter.java`와 `EgoPortableRules.java`를 기준으로 클래스명과 메서드명을 맞추세요.

개인 메시지는 반드시 본인 전용 메서드로 보내야 합니다.

```text
권장: sendSystemMessage, sendPackets(new S_SystemMessage(msg))
금지: World.broadcastPacket, 주변 전체 일반채팅 패킷
```

---

## 13. 오류 대응

```text
EgoMessageUtil 없음          → 개인 메시지 유틸 복사 누락
EgoWeaponFormController 없음 → 형태변환 컨트롤러 복사 누락
EgoWeaponDatabase 없음       → database 경로에 복사해야 함
ego_form_type 오류           → ego_oneclick_install.sql 재실행 또는 컬럼 추가 필요
에고 호출이 주변에 보임      → ChattingController 연결 위치가 일반채팅 방송보다 늦음
방패 복구 안 됨              → prev_shield_objid 대상 방패가 인벤에 없거나 착용 불가 상태
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
