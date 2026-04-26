# 에고 기능 DB화 최종 현황

## 1. 최종 DB화 기준

```text
운영 중 자주 바꾸는 값은 DB화
레벨 경험치와 전투 보너스는 ego_level로 병합
유대감은 ego 테이블 컬럼으로 병합
구버전 테이블은 fallback으로만 유지
외부 Java 연결은 EgoCore 중심으로 최소화
```

---

## 2. 최종 신규 우선 테이블

```text
ego                      에고 기본 정보 + 유대감 병합
ego_skill                에고 무기별 능력
ego_skill_base           에고 스킬 기본 발동률/쿨타임/이펙트
ego_log                  에고 전투 로그
ego_talk_pack            에고 DB 대사팩
ego_config               에고 공통 설정값
ego_level                에고 레벨 경험치 + 전투 보너스 통합
ego_weapon_rule          에고 무기 타입/능력 허용 규칙
```

---

## 3. 구버전 fallback 테이블

아래 테이블은 기존 서버 호환용으로만 유지합니다.

```text
ego_bond                 구버전 유대감 fallback
ego_level_exp            구버전 레벨 경험치 fallback
ego_level_bonus          구버전 레벨 전투 보너스 fallback
```

신규 코드 우선순위:

```text
유대감       ego.bond / ego.bond_reason 우선, ego_bond fallback
레벨/전투    ego_level 우선, ego_level_exp + ego_level_bonus fallback
```

---

## 4. 최종 Java DB/설정 그룹

```text
EgoConfig.java           ego_config 설정 로더
EgoLevel.java            ego_level 통합 레벨/전투 캐시
EgoWeaponRule.java       ego_weapon_rule 무기 규칙 로더
EgoTalkPack.java         ego_talk_pack 대사팩 로더
EgoBond.java             ego.bond 우선 유대감 로더/저장
EgoSchema.java           테이블/컬럼 연결성 검증기
EgoDb.java               짧은 DB Facade
EgoDB.java               전체 DB 로드 허브
```

삭제 완료:

```text
레벨 보너스 구버전 Facade 파일 제거
```

---

## 5. 로드 순서

```text
EgoCore.init/reload
→ EgoSchema.silentCheck
→ EgoDB.reload
   1. EgoConfig.reload
   2. EgoWeaponRule.reload
   3. EgoWeaponDatabase.reload
      - EgoLevel.reload
      - ego 로드
      - ego_skill 로드
      - EgoView.reload
   4. EgoWeaponAbilityController.reloadConfig
   5. EgoBond.reload
   6. EgoTalkPack.reload
```

---

## 6. 설치 SQL

### 신규 서버

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

### 기존 서버

```sql
SOURCE ego/sql/ego_update_euckr.sql;
```

`SOURCE`가 안 되는 DB 툴이면 아래 순서로 직접 실행합니다.

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
SOURCE ego/sql/ego_db_config.sql;
SOURCE ego/sql/ego_merge_schema_euckr.sql;
SOURCE ego/sql/ego_talk_pack_dedupe.sql;
```

병합만 별도 적용:

```sql
SOURCE ego/sql/ego_merge_schema_euckr.sql;
```

서버 내 즉시 반영:

```text
.에고리로드
```

---

## 7. ego_config 주요 설정 키

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

예시:

```sql
UPDATE ego_config
SET config_value='40', mod_date=NOW()
WHERE config_key='stun_success_rate';
```

변경 후:

```text
.에고리로드
```

---

## 8. ego_level 통합 테이블

컬럼:

```text
ego_lv
need_exp
proc_bonus
critical_chance
critical_damage
counter_chance
counter_power
counter_critical
memo
use_yn
reg_date
mod_date
```

경험치 변경 예시:

```sql
UPDATE ego_level
SET need_exp=2000, mod_date=NOW()
WHERE ego_lv=4;
```

전투 보너스 변경 예시:

```sql
UPDATE ego_level
SET critical_chance=30, critical_damage=25, mod_date=NOW()
WHERE ego_lv=10;
```

변경 후:

```text
.에고리로드
```

---

## 9. ego_weapon_rule 무기 규칙

컬럼:

```text
type2
display_name
default_ability
allowed_abilities
use_yn
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

예시:

```sql
UPDATE ego_weapon_rule
SET allowed_abilities='EGO_BALANCE,CRITICAL_BURST,GUARDIAN_SHIELD,EGO_COUNTER,EGO_REVENGE', mod_date=NOW()
WHERE type2='bow';
```

---

## 10. 스키마 연결성 점검

Java:

```java
boolean ok = EgoCore.schemaOk(con);
String report = EgoCore.schemaReport(con);
```

DB:

```sql
SHOW TABLES LIKE 'ego%';
SELECT item_id, ego_name, ego_lv, ego_exp, need_exp, bond, bond_reason FROM ego;
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_weapon_rule ORDER BY type2;
```

---

## 11. 아직 DB화하지 않는 항목

아래는 서버 코어별 시그니처 차이가 커서 DB화보다 컴파일 로그 기준 보정이 우선입니다.

```text
PcInstance 직접 제어 세부 동작
DamageController 내부 세부 공식
S_ObjectChatting 패킷 시그니처
ShockStun 버프 클래스 시그니처
클라이언트 색상 코드 지원 여부
```

---

## 12. 현재 완료 체크

```text
[완료] EgoConfig.java
[완료] EgoLevel.java
[완료] EgoWeaponRule.java
[완료] EgoTalkPack.java
[완료] EgoBond.java
[완료] EgoSchema.java
[완료] EgoDb.java
[완료] EgoDB 로드 순서 정리
[완료] EgoWeaponDatabase 경험치 ego_level 통합
[완료] EgoWeaponAbilityController 전투 보너스 ego_level 통합
[완료] EgoWeaponTypeUtil 무기 규칙 DB 우선화
[완료] EgoTalk 장르대화 딜레이 DB화
[완료] EgoAutoTalk 자동대사 임계값/딜레이 DB화
[완료] ego_merge_schema_euckr.sql 반복 실행 안전화
[완료] ego_install_euckr.sql 병합 구조 반영
[완료] ego_update_euckr.sql 병합 순서 반영
[완료] 삭제 가능한 구버전 Facade 제거
[완료] 테스트 명령 추가 없음
```
