# 에고무기 통합 매뉴얼

이 문서는 에고무기 설치, 적용, 운영 기준을 정리합니다.

---

## 1. 최우선 원칙

```text
기존 서버 코어 유지
PcInstance 공격 로직 변경 금지
DamageController 무기 타입/데미지 공식 변경 금지
원본 item.type2 변경 금지
원본 아이템 템플릿 변경 금지
에고는 별도 모듈로만 추가
```

에고는 기존 전투 시스템을 대체하지 않습니다.

```text
활 원본 무기  → 계속 활 공격
검 원본 무기  → 계속 검 공격
창 원본 무기  → 계속 창 공격
```

에고 형태는 실제 무기 타입 변환이 아니라 다음 용도입니다.

```text
인벤토리 표시
바닥 표시
아이템정보 표시
에고 대화 반응
에고 보조능력 상태
방패 해제/복구 보조 처리
```

---

## 2. 금지 작업

절대 하지 마세요.

```text
PcInstance의 bow 값 변경
PcInstance의 공격 사거리 변경
PcInstance의 화살 소비 로직 변경
PcInstance의 공격 모션 로직을 에고 기준으로 변경
DamageController의 weapon.getItem().getType2()를 에고 기준으로 교체
DamageController의 데미지 공식/무기 타입 판정 변경
EgoCombat 같은 전투 타입 우회 클래스 추가
원본 Item 템플릿 type2/gfx/name 변경
```

DamageController에 넣을 수 있는 것은 최종 데미지 계산 이후의 에고 보조능력 호출 1개뿐입니다.

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoSkill.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

이 코드는 기존 데미지 계산이 끝난 뒤 에고 보조능력을 추가하는 용도입니다.
무기 타입 공식 자체를 바꾸면 안 됩니다.

---

## 3. 최종 구조

```text
ego/
├─ README.md
├─ docs/
│  └─ EGO_SYSTEM_MANUAL.md
├─ install/
│  ├─ install_ego_windows.bat
│  └─ install_ego_linux.sh
├─ java/
│  ├─ EgoMsg.java
│  ├─ EgoType.java
│  ├─ EgoForm.java
│  ├─ EgoView.java
│  ├─ EgoTalk.java
│  ├─ EgoSkill.java
│  ├─ EgoDB.java
│  ├─ EgoCmd.java
│  ├─ EgoScan.java
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
   ├─ ego_install_euckr.sql
   ├─ ego_install_korean.sql
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

---

## 4. Java 8 기준

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

금지:

```text
var, record, sealed, switch expression, text block, List.of, Map.of, Set.of, Stream.toList, module-info.java
```

---

## 5. DB 설치

EUC-KR 서버 권장 SQL:

```text
ego/sql/ego_install_euckr.sql
```

테이블:

```text
ego
ego_skill
ego_view
ego_type
ego_talk
ego_skill_base
ego_log
```

설치 후:

```text
.에고리로드
또는 서버 재시작
```

---

## 6. 에고 표시형태 구조

에고 표시형태는 DB 값으로 저장됩니다.

```text
ego.form
```

지원값:

```text
dagger, sword, tohandsword, axe, spear, bow, staff, wand
```

예시:

```text
카르마 활       → 에고 표시형태 활
카르마 양검     → 에고 표시형태 양손검
카르마 한검     → 에고 표시형태 한손검
카르마 단검     → 에고 표시형태 단검
```

중요:

```text
ego.form은 원본 item.type2를 대체하지 않습니다.
ego.form은 PcInstance 공격 타입을 바꾸지 않습니다.
ego.form은 DamageController 데미지 공식을 바꾸지 않습니다.
```

---

## 7. 인벤토리/바닥 이미지/아이템정보

원본 아이템 템플릿은 수정하지 않습니다.

```text
원본 item.InvGfx      변경 안 함
원본 item.GroundGfx   변경 안 함
원본 item.Type2       변경 안 함
원본 item.NameId      변경 안 함
```

표시는 `ego_view`에서 관리합니다.

```text
ego_view.form
ego_view.label
ego_view.inv_gfx
ego_view.ground_gfx
ego_view.memo
```

값이 0이면 원본 이미지를 사용합니다.

```text
inv_gfx = 0     → 원본 InvGfx 사용
ground_gfx = 0  → 원본 GroundGfx 사용
```

아이템정보 예시:

```text
+9 무기명 [에고:활 Lv.3 공명]
+9 무기명 [에고:양손검 Lv.5 치명]
```

---

## 8. 서버 연결 코드

### ChattingController.java

일반채팅 방송 직전에만 넣습니다.

```java
if (mode == Lineage.CHATTING_MODE_NORMAL
        && o instanceof PcInstance
        && !(o instanceof RobotInstance)) {
    if (EgoTalk.chat((PcInstance) o, msg)) {
        return;
    }
}
```

주의:

```text
PcInstance 공격 메서드에는 넣지 않습니다.
일반채팅 외 모드에는 넣지 않습니다.
```

### CommandController.java

기존 명령어 처리 전에 넣습니다.

```java
if (EgoCmd.run(o, key, st)) {
    return true;
}
```

### DamageController.java

최종 return 직전, 기존 데미지 계산이 끝난 뒤에만 넣습니다.

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoSkill.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

주의:

```text
DamageController 내부의 type2 판정은 그대로 둡니다.
DamageController 내부의 bow 판정은 그대로 둡니다.
DamageController 내부 공식은 그대로 둡니다.
```

### DB 로드

서버 시작 시 1회:

```java
EgoDB.init(con);
```

리로드 명령:

```text
.에고리로드
```

---

## 9. 적용 순서

```text
1. DB 백업
2. ego/sql/ego_install_euckr.sql 실행
3. ego/java 파일 복사
4. ChattingController에 EgoTalk.chat(...) 추가
5. CommandController에 EgoCmd.run(...) 추가
6. DamageController 최종 return 직전에 EgoSkill.attack(...) 추가
7. DB 시작 연결 EgoDB.init(con) 추가
8. 서버 빌드
9. .에고생성 카르마
10. 카르마 상태 / 카르마 활 / 카르마 한검 테스트
11. 인벤토리 표시 변경 확인
12. 공격 타입은 원본 무기 기준으로 유지되는지 확인
```

---

## 10. 운영 SQL 예시

형태별 이미지 변경:

```sql
UPDATE ego_view
SET inv_gfx = 1001,
    ground_gfx = 1002,
    memo = '에고 활 전용 이미지'
WHERE form = 'bow';
```

에고 형태 변경:

```sql
UPDATE ego
SET form = 'bow'
WHERE item_id = 123456789
  AND use_yn = 1;
```

능력 변경:

```sql
UPDATE ego_skill
SET use_yn = 0
WHERE item_id = 123456789;

INSERT INTO ego_skill (item_id, skill, skill_lv, use_yn)
VALUES (123456789, 'BLOOD_DRAIN', 1, 1)
ON DUPLICATE KEY UPDATE use_yn = 1;
```

변경 후:

```text
.에고리로드
```

---

## 11. 게임 명령어

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

---

## 12. 오류 대응

```text
에고 호출이 주변에 보임
→ ChattingController 연결 위치가 일반채팅 방송보다 늦음

에고 명령이 귓말/파티말에서도 반응함
→ mode == Lineage.CHATTING_MODE_NORMAL 조건 누락

에고 활로 바꿨는데 실제 활 공격이 안 됨
→ 정상입니다. 에고 형태는 실제 무기 타입 변경이 아닙니다.

원본 활인데 에고 한검 후에도 활 공격함
→ 정상입니다. 기존 코어 유지 정책입니다.

DamageController 공식이 바뀜
→ 잘못 적용한 것입니다. type2/bow 판정 변경분을 원복하세요.

PcInstance 자동사냥 사거리가 바뀜
→ 잘못 적용한 것입니다. PcInstance 공격 로직 변경분을 원복하세요.
```

---

## 13. 원복 기준

에고 관련 연결만 제거하면 기존 서버로 돌아가야 합니다.

```text
ChattingController의 EgoTalk.chat(...) 제거
CommandController의 EgoCmd.run(...) 제거
DamageController의 EgoSkill.attack(...) 제거
서버 시작부 EgoDB.init(con) 제거
복사한 ego java 파일 제거
```

PcInstance와 DamageController 내부 공식이 바뀌어 있으면 에고 원복이 아닙니다.
해당 변경은 별도로 되돌려야 합니다.
