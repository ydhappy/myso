# 에고무기 시스템

Java 8 / UTF-8 기준 에고무기 모듈입니다.

최종 방향은 단순합니다.

```text
기존 무기는 그대로 사용
type2 변형 없음
인벤/바닥 이미지 변경 없음
에고는 대화, 성장, 보조능력, 로그만 추가
```

---

## 1. 최종 원칙

```text
PcInstance 공격 로직 변경 금지
DamageController 무기 공식 변경 금지
item.type2 변경 금지
item template 변경 금지
인벤토리 이미지 변경 금지
바닥 이미지 변경 금지
EgoCombat 없음
무기변형 없음
```

예시:

```text
원본 활  → 계속 활 공격
원본 검  → 계속 검 공격
원본 창  → 계속 창 공격
에고     → 기존 무기에 이름표, 대화, 경험치, 보조능력, 로그만 추가
```

---

## 2. 폴더 구조

```text
ego/
├─ README.md
├─ html/
│  └─ egoletter.htm
├─ java/
│  ├─ EgoMsg.java
│  ├─ EgoType.java
│  ├─ EgoView.java
│  ├─ EgoTalk.java
│  ├─ EgoSkill.java
│  ├─ EgoDB.java
│  ├─ EgoCmd.java
│  ├─ EgoScan.java
│  ├─ EgoMessageUtil.java
│  ├─ EgoWeaponTypeUtil.java
│  ├─ EgoWeaponControlController.java
│  ├─ EgoWeaponAbilityController.java
│  ├─ EgoWeaponDatabase.java
│  ├─ EgoWeaponCommand.java
│  ├─ EgoOpponentScanController.java
│  ├─ EgoCoreAdapter.java
│  └─ EgoPortableRules.java
└─ sql/
   ├─ ego_install_euckr.sql
   ├─ ego_cleanup_unused.sql
   ├─ ego_install_korean.sql
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

삭제된 기능 파일:

```text
EgoForm.java
EgoWeaponFormController.java
```

---

## 3. DB 최종 테이블

신규 설치 기준 테이블은 4개입니다.

```text
ego
ego_skill
ego_skill_base
ego_log
```

삭제된 테이블:

```text
ego_view
ego_type
ego_talk
에고모양
```

삭제된 컬럼:

```text
ego.form
ego.prev_shield
```

기존 설치 서버는 아래 SQL을 실행합니다.

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
```

주의: 구버전 MySQL은 `DROP INDEX`, `DROP COLUMN`에 `IF EXISTS`가 없어 이미 삭제된 컬럼이면 에러가 날 수 있습니다. 그런 경우 해당 줄은 건너뛰면 됩니다.

---

## 4. 설치 SQL

신규 설치:

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

설치 후 확인:

```sql
SHOW TABLES LIKE 'ego';
SHOW TABLES LIKE 'ego_skill';
SHOW TABLES LIKE 'ego_skill_base';
SHOW TABLES LIKE 'ego_log';
```

---

## 5. Java 연결

### 5.1 서버 시작 DB 로드

서버 DB 초기화 지점에 추가합니다.

```java
EgoDB.init(con);
```

### 5.2 CommandController 연결

기존 명령어 처리 전 또는 초반에 추가합니다.

```java
if (EgoCmd.run(o, key, st)) {
    return true;
}
```

### 5.3 ChattingController 실시간 대화 연결

`ChattingController.toNormal(...)`에서 일반 채팅이 주변에 방송되기 전에 추가합니다.

권장 위치:

```java
// 명령어 확인 처리.
if (!CommandController.toCommand(o, msg)) {
```

위 코드보다 먼저 아래를 넣습니다.

```java
// [에고] 일반채팅 실시간 대화 처리
// 에고 호출 채팅은 주변에 보이지 않게 consume 처리한다.
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoTalk.chat((PcInstance) o, msg)) {
        return;
    }
}
```

이렇게 하면 사용자가 일반 채팅으로 `카르마 상태`, `카르마 조언`처럼 입력해도 주변 유저에게는 보이지 않고 에고만 반응합니다.

### 5.4 DamageController 공격 훅

`DamageController.getDamage(...)` 최종 return 직전, 기존 데미지 계산이 끝난 뒤 추가합니다.

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoSkill.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

### 5.5 DamageController 피격 훅

`DamageController.toDamage(...)`에서 HP 감소 직전에 추가합니다.

찾을 코드:

```java
// 데미지 입었다는거 알리기.
o.toDamage(cha, dmg, type);
// hp 처리
o.setNowHp(o.getNowHp() - dmg);
```

변경:

```java
// 데미지 입었다는거 알리기.
o.toDamage(cha, dmg, type);

// [에고] 피격자 레벨별 해금 능력: 방어본능 / 반격 / 복수
if (o instanceof Character) {
    dmg = EgoSkill.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}

// hp 처리
o.setNowHp(o.getNowHp() - dmg);
```

---

## 6. 에고 대화 출력 방식

### 짧은 답변

짧은 에고 답변은 말풍선으로 출력됩니다.

```text
카르마: [에고] 듣고 있습니다. 근처에 선공 몬스터 기척이 있습니다.
```

구현 방식:

```text
S_ObjectChatting 패킷을 본인에게만 전송
주변 캐릭터에게는 방송하지 않음
```

### 긴 답변 / 커맨드 결과

상태, 도움말, 상세 정보처럼 긴 메시지는 편지창처럼 출력됩니다.

```text
ego/html/egoletter.htm
```

이 파일을 클라이언트 HTML 폴더에 복사해야 합니다.

일반적으로 다음 위치 중 서버/클라 구조에 맞는 곳에 넣습니다.

```text
html/egoletter.htm
data/html/egoletter.htm
client/html/egoletter.htm
```

템플릿이 없으면 시스템 메시지 fallback으로 내용은 볼 수 있습니다.

---

## 7. 명령어

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
```

제거된 명령:

```text
카르마 활
카르마 양검
카르마 한검
카르마 단검
카르마 창
카르마 도끼
카르마 지팡이
카르마 완드
```

---

## 8. 에고 생성 조건

```text
착용 중인 무기 필요
지원 type2 필요
이미 에고 생성된 무기 재생성 불가
강화된 무기도 생성 가능
```

지원 type2:

```text
dagger
sword
tohandsword
axe
spear
bow
staff
wand
```

제외:

```text
fishing_rod
무기 슬롯이 아닌 아이템
지원하지 않는 type2
```

---

## 9. 에고 경험치 / 레벨업

```text
생성 시 레벨: 1
생성 시 경험치: 0
생성 시 필요 경험치: 100
최대 레벨: 30
```

경험치:

```text
공격 중 3초마다 +1
몬스터 처치 hook 연결 시 +5
보스 처치 hook 연결 시 +55
```

레벨업:

```text
현재 경험치 >= 필요 경험치 → 레벨 +1
남은 경험치 이월
다음 필요 경험치 = 기존 필요 경험치 + 현재 레벨 * 100
```

---

## 10. 레벨별 해금

```text
Lv.5  방어본능
      피격 시 받는 피해 소폭 감소

Lv.10 반격
      피격 시 일정 확률로 공격자에게 반격 피해
      skill = EGO_COUNTER

Lv.20 복수
      체력 35% 이하에서 피격 시 강한 반격 + HP 회복
      skill = EGO_REVENGE
```

`EGO_COUNTER`, `EGO_REVENGE`는 일반 설정용 주력 능력이 아니라 레벨별 자동 해금 능력입니다.

---

## 11. 에고 능력 계산

`ego_skill_base` 기준으로 계산합니다.

```text
base_rate  기본 발동률
lv_rate    레벨당 발동률
max_rate   최대 발동률
min_lv     최소 실질 레벨
cool_ms    쿨타임
effect     S_ObjectEffect 이펙트 번호
```

수정 예:

```sql
UPDATE ego_skill_base
SET base_rate = 3,
    lv_rate = 1,
    max_rate = 25,
    cool_ms = 3000,
    effect = 8150
WHERE skill = 'BLOOD_DRAIN';
```

수정 후:

```text
.에고리로드
```

---

## 12. 에고 로그

능력 발동/레벨업은 `ego_log`에 기록됩니다.

확인:

```sql
SELECT *
FROM ego_log
ORDER BY reg_date DESC
LIMIT 50;
```

로그 컬럼:

```text
item_id
char_id
char_name
target_name
skill
base_dmg
final_dmg
add_dmg
reg_date
```

---

## 13. 컴파일

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

---

## 14. 최종 점검

```text
[확인] type2 변형 없음
[확인] 인벤 이미지 변경 없음
[확인] 바닥 이미지 변경 없음
[확인] 에고 이름표만 인벤 이름에 추가
[확인] 짧은 응답은 말풍선
[확인] 긴 응답은 egoletter 편지창
[확인] 에고 호출 채팅은 주변에 보이지 않음
[확인] ego_log 기록
[확인] ego_skill_base 전투 계산 연동
[확인] Lv.5/Lv.10/Lv.20 해금 능력
```
