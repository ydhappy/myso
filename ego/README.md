# 에고무기 시스템

Java 8 / UTF-8 기준 에고무기 모듈입니다.

최종 방향은 단순합니다.

```text
기존 무기는 그대로 사용
type2 변형 없음
인벤/바닥 이미지 변경 없음
에고는 대화, 성장, 보조능력, 로그만 추가
에고 레벨은 0~10 고정
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

에고 정보/상태/생성 메시지에서 아이템 이름은 `item.getItem().getName()` 기준으로 보정합니다. `ItemInstance.getName()`이 `$1234` 같은 nameid로 들어와도 `EgoView.displayName(item)`을 사용하면 실제 아이템명으로 출력됩니다.

---

## 2. DB 최종 테이블

```text
ego
ego_skill
ego_skill_base
ego_log
```

기존 설치 서버 정리/보정:

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
```

신규 설치:

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

---

## 3. Java 연결

### 서버 시작 DB 로드

```java
EgoDB.init(con);
```

### CommandController 연결

```java
if (EgoCmd.run(o, key, st)) {
    return true;
}
```

### ChattingController 실시간 대화 연결

일반 채팅이 주변에 방송되기 전에 추가합니다.

```java
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoTalk.chat((PcInstance) o, msg)) {
        return;
    }
}
```

### DamageController 공격 훅

`DamageController.getDamage(...)` 최종 return 직전:

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoSkill.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

### DamageController 피격 훅

`DamageController.toDamage(...)`에서 HP 감소 직전:

```java
// 데미지 입었다는거 알리기.
o.toDamage(cha, dmg, type);

// [에고] 피격자 레벨별 능력: Lv.5 반격 / Lv.6 자동반격 / Lv.10 스턴
if (o instanceof Character) {
    dmg = EgoSkill.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}

// hp 처리
o.setNowHp(o.getNowHp() - dmg);
```

---

## 4. 명령어

```text
.에고도움
.에고생성 이름
.에고삭제 확인
.에고정보
.에고이름 이름
.에고능력 코드
.에고상대
.에고주변
.에고리로드
```

`.에고삭제 확인`은 완전삭제입니다.

```text
ego 삭제
ego_skill 삭제
ego_log 삭제
```

삭제 후 복구할 수 없습니다. 다시 사용하려면 `.에고생성 이름`으로 새로 생성해야 합니다.

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

## 5. 에고 생성 조건

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

---

## 6. 이름 표시 규칙

에고 코드에서 무기명을 보여줄 때는 직접 `weapon.getName()`을 쓰지 말고 아래를 사용합니다.

```java
EgoView.displayName(weapon)
```

인벤토리 표식용 이름은 아래를 사용합니다.

```java
EgoView.name(item, baseName)
```

---

## 7. 에고 경험치 / 레벨업

```text
생성 시 레벨: 0
생성 시 경험치: 0
생성 시 필요 경험치: 100
최대 레벨: 10
```

Lv.0 규칙:

```text
스킬 발동 없음
치명 발동 없음
피격 반격 없음
자동반격 없음
스턴 없음
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
최대 Lv.10 도달 시 경험치 0 고정
```

---

## 8. 레벨별 전투 규칙

```text
Lv.0  모든 전투 능력 없음

Lv.1  기본 에고 스킬/치명 보정 시작
Lv.2  스킬 발동률 +1, 치명률 +2
Lv.3  스킬 발동률 +2, 치명률 +3
Lv.4  스킬 발동률 +3, 치명률 +4

Lv.5  피격 시 확률 반격 시작
      반격 공격성공/공격력/치명타 보정 시작
      PC 대상 포함

Lv.6  피격 시 자동반격 시작
      PC 대상 포함

Lv.7  자동반격 강화
Lv.8  자동반격/치명 강화
Lv.9  자동반격/치명 강화

Lv.10 최대 레벨
      스턴 스킬 50% 성공
      공격/반격 시 스턴 시도
      PC 대상 포함
```

세부 수치표는 `EgoWeaponAbilityController` 내부 배열 기준입니다.

```java
LEVEL_PROC_BONUS
LEVEL_CRITICAL_CHANCE
LEVEL_CRITICAL_DAMAGE
LEVEL_COUNTER_CHANCE
LEVEL_COUNTER_POWER
LEVEL_COUNTER_CRITICAL
```

---

## 9. 에고 능력 계산

`ego_skill_base` 기본 공식:

```text
발동률 = base_rate + (레벨 - 1) * lv_rate + ego_skill.rate_bonus + 레벨별 보너스
```

Lv.0은 공식 계산 전에 차단됩니다.

```text
Lv.0이면 발동률 0
```

치명 능력:

```text
CRITICAL_BURST는 Lv.1부터 치명 보정 적용
레벨이 오를수록 치명 발동률/치명 추가피해 증가
```

반격:

```text
Lv.5부터 EGO_COUNTER 사용
Lv.6부터 EGO_AUTO_COUNTER 사용
Lv.10부터 EGO_STUN 시도
```

스턴:

```text
Lv.10 전용
성공률 50%
쿨타임 6000ms
ShockStun 버프 재사용
```

---

## 10. 에고 대화 출력 방식

```text
짧은 답변 → 본인에게만 보이는 말풍선
긴 답변/커맨드 결과 → egoletter 편지창
```

HTML 템플릿:

```text
ego/html/egoletter.htm
```

클라이언트 HTML 폴더에 복사합니다.

```text
html/egoletter.htm
data/html/egoletter.htm
client/html/egoletter.htm
```

---

## 11. 에고 로그

확인:

```sql
SELECT *
FROM ego_log
ORDER BY reg_date DESC
LIMIT 50;
```

주의: `.에고삭제 확인` 실행 시 해당 무기의 `ego_log`도 삭제됩니다.

---

## 12. 컴파일

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

---

## 13. 최종 점검

```text
[확인] 최대레벨 10 고정
[확인] 생성 레벨 0
[확인] Lv.0 전투능력 없음
[확인] Lv.1부터 스킬/치명 동작
[확인] Lv.5부터 피격 반격/공격성공/공격력/치명 보정
[확인] Lv.6부터 피격 자동반격
[확인] Lv.10 스턴 50% 성공
[확인] PC 대상 포함
[확인] .에고삭제 확인 완전삭제
[확인] type2 변형 없음
[확인] 인벤/바닥 이미지 변경 없음
[확인] nameid 대신 실제 아이템명 표시
```
