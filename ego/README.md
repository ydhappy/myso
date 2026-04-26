# 에고무기 시스템

Java 8 / UTF-8 기준 에고무기 모듈입니다.

최종 방향은 단순합니다.

```text
기존 무기는 그대로 사용
type2 변형 없음
인벤/바닥 이미지 변경 없음
에고는 대화, 성장, 보조능력, 로그만 추가
에고 레벨은 0~10 고정
실시간 대화 말투는 예의 / 예의반대 2종
명령어가 아닌 자연대화도 반응
드라마/영화/웹툰 등 장르별 오리지널 대사 지원
주요 수치/경험치/전투 보너스/무기 규칙 DB화 완료
레벨 경험치 + 전투 보너스는 ego_level로 병합 완료
유대감은 ego.bond / ego.bond_reason으로 병합 완료
DB 실행 파일 신규 1개 / 기존 1개로 최소화
기존 서버 Java 연결 진입점 EgoCore 1개로 최소화
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
실제 작품 대사 복사 금지
```

에고 정보/상태/생성 메시지에서 아이템 이름은 `item.getItem().getName()` 기준으로 보정합니다. `ItemInstance.getName()`이 `$1234` 같은 nameid로 들어와도 `EgoView.displayName(item)`을 사용하면 실제 아이템명으로 출력됩니다.

---

## 2. 최종 DB 테이블

### 신규 우선 테이블

```text
ego                      에고 기본 정보 + 유대감 병합
ego_skill                에고 무기별 능력
ego_skill_base           에고 스킬 기본 발동률/쿨타임/이펙트
ego_log                  에고 전투 로그
ego_talk_pack            에고 장르/자연대화 DB 대사팩
ego_config               에고 공통 설정값
ego_level                에고 레벨 경험치 + 전투 보너스 통합
ego_weapon_rule          에고 무기 타입/능력 허용 규칙
```

### 구버전 fallback 테이블

아래 테이블은 기존 서버 호환을 위해 fallback으로만 유지합니다.

```text
ego_bond                 구버전 유대감 fallback
ego_level_exp            구버전 레벨 경험치 fallback
ego_level_bonus          구버전 레벨 전투 보너스 fallback
```

`ego.ego_type`은 현재 성격 테이블이 아니라 실시간 대화 말투 저장소로 사용합니다.

```text
예의      공손한 존댓말
예의반대  건방진 반말/도발형
```

---

## 3. SQL 최소 적용

### 신규 서버

신규 서버는 아래 1개만 실행합니다.

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

### 기존 서버

기존 서버는 아래 1개만 실행합니다.

```sql
SOURCE ego/sql/ego_update_euckr.sql;
```

`SOURCE`가 안 되는 DB 툴이면 아래 4개를 순서대로 직접 실행합니다.

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
SOURCE ego/sql/ego_db_config.sql;
SOURCE ego/sql/ego_merge_schema_euckr.sql;
SOURCE ego/sql/ego_talk_pack_dedupe.sql;
```

병합만 별도 적용하려면 아래만 실행합니다.

```sql
SOURCE ego/sql/ego_merge_schema_euckr.sql;
```

### 서버 내 즉시 반영

```text
.에고리로드
```

---

## 4. Java 최소 연결

기존 서버 코드에는 가능하면 아래 클래스만 연결합니다.

```text
EgoCore.java
```

### 서버 시작 DB 로드

```java
EgoCore.init(con);
```

### 서버 리로드

```java
EgoCore.reload(con);
```

### CommandController 연결

```java
if (EgoCore.command(o, key, st)) {
    return true;
}
```

`return void` 구조라면:

```java
if (EgoCore.command(o, key, st)) {
    return;
}
```

### ChattingController 실시간 대화 연결

일반 채팅이 주변에 방송되기 전에 추가합니다.

```java
if (EgoCore.chat(o, msg)) {
    return;
}
```

### 자동 상황 대사 연결

서버 루프 또는 캐릭터 AI/상태 갱신 루프에서 주기적으로 호출합니다.

```java
EgoCore.tick(pc);
```

### DamageController 공격 훅

`DamageController.getDamage(...)` 최종 return 직전:

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoCore.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

### DamageController 피격 훅

`DamageController.toDamage(...)`에서 HP 감소 직전:

```java
// 데미지 입었다는거 알리기.
o.toDamage(cha, dmg, type);

// [에고] 피격자 레벨별 능력: Lv.5 반격 / Lv.6 자동반격 / Lv.10 스턴
if (o instanceof Character) {
    dmg = EgoCore.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}

// hp 처리
o.setNowHp(o.getNowHp() - dmg);
```

---

## 5. Java 파일 그룹

### 외부 연결 그룹

```text
EgoCore.java
```

### 연결성/Facade 그룹

```text
EgoDb.java
EgoSchema.java
```

### DB/설정 그룹

```text
EgoDB.java
EgoConfig.java
EgoLevel.java
EgoLevelBonus.java   구버전 메서드명 호환 Facade
EgoWeaponRule.java
EgoTalkPack.java
EgoBond.java
```

### 전투 그룹

```text
EgoSkill.java
EgoWeaponAbilityController.java
EgoWeaponTypeUtil.java
```

### 대화 그룹

```text
EgoTalk.java
EgoGenreTalk.java
EgoGenreGuide.java
EgoWeaponControlController.java
EgoAutoTalk.java
EgoTalkHistory.java
EgoMessageUtil.java
```

### 표시/명령 그룹

```text
EgoCmd.java
EgoView.java
```

---

## 6. 명령어

```text
.에고도움
.에고생성 이름
.에고삭제 확인
.에고정보
.에고이름 이름
.에고말투 예의
.에고말투 예의반대
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
ego.bond 초기화
ego_bond fallback row 삭제
```

삭제 후 복구할 수 없습니다. 다시 사용하려면 `.에고생성 이름`으로 새로 생성해야 합니다.

---

## 7. 일반채팅 대화

기본 대화:

```text
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
카르마 상대
카르마 주변캐릭
카르마 타겟분석
카르마 말투 예의
카르마 말투 예의반대
```

자연대화:

```text
카르마 안녕
카르마 고마워
카르마 미안해
카르마 나 피곤해
카르마 무서워
카르마 심심해
카르마 너 누구야
카르마 강해지고 있어?
카르마 물약 먹어야 할까
카르마 어디서 사냥할까
카르마 그래
```

장르 대화:

```text
카르마 장르목록
카르마 대사목록
카르마 대화 추천
카르마 드라마 대사 해줘
카르마 영화 한마디
카르마 웹툰 느낌으로 말해줘
카르마 로맨스 대사
카르마 액션 대사
카르마 판타지 대사
카르마 무협 대사
카르마 공포 대사
카르마 코미디 대사
카르마 추리 대사
카르마 학원물 대사
카르마 일상 힐링 대사
카르마 빌런 대사
카르마 주인공 각성 대사
카르마 아무 대사나 해줘
```

---

## 8. DB화된 주요 설정

### ego_config

```text
genre_talk_delay_ms
auto_talk_hp_warn_rate
auto_talk_mp_warn_rate
auto_talk_idle_hp_rate
auto_talk_idle_mp_rate
auto_talk_hp_warn_delay_ms
auto_talk_mp_warn_delay_ms
auto_talk_boss_warn_delay_ms
auto_talk_idle_delay_ms
attack_ego_exp
attack_exp_delay_ms
kill_ego_exp
boss_kill_ego_exp
exp_message_rate
proc_message_delay_ms
counter_unlock_level
auto_counter_unlock_level
auto_counter_cool_ms
auto_counter_chance
stun_level
stun_success_rate
stun_time
stun_effect
stun_cool_ms
guardian_shield_hp_rate
execution_target_hp_rate
area_range
area_max_target
```

예시: 스턴 성공률 변경

```sql
UPDATE ego_config
SET config_value='40', mod_date=NOW()
WHERE config_key='stun_success_rate';
```

예시: 자동반격 해금 레벨 변경

```sql
UPDATE ego_config
SET config_value='7', mod_date=NOW()
WHERE config_key='auto_counter_unlock_level';
```

변경 후:

```text
.에고리로드
```

### ego_level

에고 레벨별 필요 경험치와 전투 보너스를 통합 관리합니다.

```sql
UPDATE ego_level
SET need_exp=2000, mod_date=NOW()
WHERE ego_lv=4;
```

```sql
UPDATE ego_level
SET critical_chance=30, critical_damage=25, mod_date=NOW()
WHERE ego_lv=10;
```

### ego_weapon_rule

무기 타입별 기본 능력/허용 능력을 DB에서 조정합니다.

```sql
UPDATE ego_weapon_rule
SET allowed_abilities='EGO_BALANCE,CRITICAL_BURST,GUARDIAN_SHIELD,EGO_COUNTER,EGO_REVENGE', mod_date=NOW()
WHERE type2='bow';
```

---

## 9. 에고 생성 조건

```text
착용 중인 무기 필요
지원 type2 필요
이미 에고 생성된 무기 재생성 불가
강화된 무기도 생성 가능
```

기본 지원 type2:

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

제외 type2:

```text
fishing_rod
```

무기 규칙은 `ego_weapon_rule`에서 변경 가능합니다.

---

## 10. 에고 경험치 / 레벨업

```text
생성 시 레벨: 0
생성 시 경험치: 0
생성 시 필요 경험치: ego_level.ego_lv=0 기준
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

기본 레벨별 필요 경험치:

```text
Lv.0 -> Lv.1  : 100
Lv.1 -> Lv.2  : 250
Lv.2 -> Lv.3  : 500
Lv.3 -> Lv.4  : 900
Lv.4 -> Lv.5  : 1,500  피격 반격 개방
Lv.5 -> Lv.6  : 2,400  자동반격 개방
Lv.6 -> Lv.7  : 3,600
Lv.7 -> Lv.8  : 5,200
Lv.8 -> Lv.9  : 7,500
Lv.9 -> Lv.10 : 10,000 스턴 50% 개방
Lv.10         : 0       만렙
```

경험치 획득 기본값:

```text
공격 중 3초마다 +1
몬스터 처치 +5
보스 처치 추가 +50
```

위 수치는 `ego_config`에서 변경 가능합니다.

---

## 11. 레벨별 전투 규칙

레벨별 세부 수치는 `ego_level`에서 관리합니다.

```text
Lv.0  모든 전투 능력 없음
Lv.1  기본 에고 스킬/치명 보정 시작
Lv.5  피격 반격 시작
Lv.6  자동반격 시작
Lv.10 스턴 시도
```

전투 공식 주요 기준:

```text
스킬 발동률 = ego_skill_base.base_rate + 레벨 보정 + ego_skill.rate_bonus
레벨 보정 = ego_level.proc_bonus
치명률 보정 = ego_level.critical_chance
치명 피해 보정 = ego_level.critical_damage
반격 확률 = ego_level.counter_chance
반격 피해 = ego_level.counter_power
반격 치명 = ego_level.counter_critical
```

---

## 12. 장르별 대화 라이브러리

장르 대화는 파일과 DB를 함께 사용합니다.

```text
EgoGenreTalk.java   Java fallback 오리지널 대사
EgoGenreGuide.java  장르목록/대화추천 안내
EgoTalkPack.java    ego_talk_pack DB 대사팩
```

처리 흐름:

```text
EgoCore.chat(o, msg)
→ EgoTalk.chat()
→ EgoGenreGuide.isGuideRequest(command)
→ EgoTalkPack.find(pc, weapon, command)
→ EgoGenreTalk.talk(pc, weapon, command)
→ 매칭 없으면 EgoWeaponControlController.onNormalChat()
```

스팸 방지:

```text
장르대화 전용 딜레이: ego_config.genre_talk_delay_ms
연속 입력 시 채팅은 주변에 방송하지 않고 소비
```

---

## 13. 이름 표시 규칙

에고 코드에서 무기명을 보여줄 때는 직접 `weapon.getName()`을 쓰지 말고 아래를 사용합니다.

```java
EgoView.displayName(weapon)
```

인벤토리 표식용 이름은 아래를 사용합니다.

```java
EgoView.name(item, baseName)
```

---

## 14. 에고 대화 출력 방식

```text
짧은 답변 → 본인에게만 보이는 말풍선
긴 답변/커맨드 결과 → egoletter 편지창
장르/감성 대사 → EgoMessageUtil.genre()
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

## 15. 에고 로그

확인:

```sql
SELECT *
FROM ego_log
ORDER BY reg_date DESC
LIMIT 50;
```

주의: `.에고삭제 확인` 실행 시 해당 무기의 `ego_log`도 삭제됩니다.

---

## 16. 연결성 점검

Java에서:

```java
boolean ok = EgoCore.schemaOk(con);
String report = EgoCore.schemaReport(con);
```

DB에서:

```sql
SHOW TABLES LIKE 'ego%';
SELECT item_id, ego_name, ego_lv, ego_exp, need_exp, bond, bond_reason FROM ego;
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_weapon_rule ORDER BY type2;
```

---

## 17. 참고 문서

```text
ego/docs/EGO_MINIMAL_APPLY.md
ego/docs/EGO_DBIZATION.md
ego/docs/EGO_CONNECTIVITY_MAP.md
ego/docs/EGO_IMPLEMENTED_NO_TEST_COMMANDS.md
ego/docs/EGO_SUGGESTIONS.md
```

---

## 18. 컴파일

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

---

## 19. 최종 점검

```text
[확인] DB 실행 파일 신규 1개 / 기존 1개 최소화
[확인] 기존 서버 Java 연결 진입점 EgoCore 1개 최소화
[확인] ego_level_exp + ego_level_bonus -> ego_level 병합
[확인] ego_bond -> ego.bond / ego.bond_reason 병합
[확인] 구버전 fallback 유지
[확인] 반복 실행 안전 SQL 보강
[확인] 최대레벨 10 고정
[확인] 생성 레벨 0
[확인] Lv.0 전투능력 없음
[확인] Lv.1부터 스킬/치명 동작
[확인] 실시간 대화 예의/예의반대 2종
[확인] 명령어가 아닌 자연대화 반응
[확인] DB 대사팩 ego_talk_pack
[확인] 장르대화 스팸 방지 DB화
[확인] 레벨별 경험치/전투 보너스 ego_level 통합
[확인] 주요 전투 설정 ego_config DB화
[확인] 무기 타입/능력 허용 규칙 DB화
[확인] Lv.5부터 피격 반격/공격성공/공격력/치명 보정
[확인] Lv.6부터 피격 자동반격
[확인] Lv.10 스턴 기본 50% 성공
[확인] PC 대상 포함
[확인] .에고삭제 확인 완전삭제
[확인] type2 변형 없음
[확인] 인벤/바닥 이미지 변경 없음
[확인] nameid 대신 실제 아이템명 표시
```
