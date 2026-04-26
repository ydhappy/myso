# 에고 기능 DB화 정밀검토 및 반영 현황

## 1. DB화 기준

에고 기능 중 DB화 우선순위는 다음 기준으로 정했습니다.

```text
1순위: 운영 중 자주 바꾸는 값
2순위: 서버마다 밸런스가 달라질 수 있는 값
3순위: 대사/텍스트처럼 운영자가 직접 편집하고 싶은 값
4순위: 로그/상태처럼 누적 조회가 필요한 값
5순위: 서버 코어별 메서드 시그니처가 달라질 수 있는 직접 제어 로직
```

---

## 2. 현재 DB화 완료 테이블

```text
ego                      에고 기본 정보
ego_skill                에고 무기별 능력
ego_skill_base           에고 스킬 기본 발동률/쿨타임/이펙트
ego_log                  에고 전투 로그
ego_bond                 에고 유대감
ego_talk_pack            에고 장르/자연대화 DB 대사팩
ego_config               에고 공통 설정값
ego_level_exp            에고 레벨별 필요 경험치
ego_level_bonus          에고 레벨별 전투 보너스
ego_weapon_rule          에고 무기 타입/능력 허용 규칙
```

---

## 3. 현재 DB화 완료 Java 파일

```text
EgoConfig.java
- ego_config 설정 로더

EgoLevelBonus.java
- ego_level_bonus 레벨별 전투 보너스 로더

EgoWeaponRule.java
- ego_weapon_rule 무기 타입/능력 허용 규칙 로더

EgoTalkPack.java
- ego_talk_pack 대사팩 로더

EgoBond.java
- ego_bond 유대감 로더/저장
```

수정 연결 파일:

```text
EgoDB.java
- EgoConfig.reload(con)
- EgoLevelBonus.reload(con)
- EgoWeaponRule.reload(con)
- EgoBond.reload(con)
- EgoTalkPack.reload(con)

EgoWeaponDatabase.java
- ego_level_exp 우선 사용
- 없으면 Java fallback

EgoWeaponAbilityController.java
- ego_level_bonus 우선 사용
- ego_config 전투 설정 우선 사용
- ego_skill_base 스킬 기본값 사용

EgoWeaponTypeUtil.java
- ego_weapon_rule 우선 사용
- 없으면 Java fallback

EgoTalk.java
- genre_talk_delay_ms 사용

EgoAutoTalk.java
- 자동대사 기준/딜레이를 ego_config에서 사용
```

---

## 4. 설치 SQL

### 신규 서버

이 파일 하나만 실행하면 기본 DB화 테이블까지 생성됩니다.

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

포함 내용:

```text
ego
ego_skill
ego_skill_base
ego_log
ego_bond
ego_talk_pack
ego_config
ego_level_exp
ego_level_bonus
ego_weapon_rule
```

### 기존 서버

기존 서버는 먼저 정리/보정 SQL을 실행합니다.

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
```

그다음 DB화 보강 SQL을 실행합니다.

```sql
SOURCE ego/sql/ego_db_config.sql;
```

대사팩 중복이 의심되면 추가 실행합니다.

```sql
SOURCE ego/sql/ego_talk_pack_dedupe.sql;
```

### 서버 내 즉시 반영

```text
.에고리로드
```

---

## 5. ego_config 설정 키

```text
genre_talk_delay_ms
장르대화 연속 입력 방지 딜레이 ms

auto_talk_hp_warn_rate
HP 자동 위험 대사 발동 기준 퍼센트 이하

auto_talk_mp_warn_rate
MP 자동 안내 대사 발동 기준 퍼센트 이하

auto_talk_idle_hp_rate
auto_talk_idle_mp_rate
안정 상태 자동 대사 기준

auto_talk_hp_warn_delay_ms
auto_talk_mp_warn_delay_ms
auto_talk_boss_warn_delay_ms
auto_talk_idle_delay_ms
상황별 자동 대사 재출력 딜레이

attack_ego_exp
공격 중 주기적으로 획득하는 에고 경험치

attack_exp_delay_ms
공격 경험치 획득 딜레이 ms

kill_ego_exp
몬스터 처치 시 에고 경험치

boss_kill_ego_exp
보스 처치 시 추가 에고 경험치

exp_message_rate
일반 경험치 획득 메시지 출력 확률

proc_message_delay_ms
능력 발동 메시지 출력 딜레이 ms

counter_unlock_level
피격 반격 해금 레벨

auto_counter_unlock_level
자동반격 해금 레벨

auto_counter_cool_ms
자동반격 쿨타임 ms

auto_counter_chance
자동반격 발동 확률

stun_level
에고 스턴 해금 레벨

stun_success_rate
에고 스턴 성공 확률

stun_time
에고 스턴 시간 초

stun_effect
에고 스턴 이펙트 번호

stun_cool_ms
에고 스턴 쿨타임 ms

guardian_shield_hp_rate
수호 의지 발동 HP 기준 이하

execution_target_hp_rate
처형 발동 대상 HP 기준 이하

area_range
광역 능력 범위

area_max_target
광역 능력 최대 대상 수
```

---

## 6. ego_level_exp 경험치표

```text
Lv.0 -> Lv.1  : 100
Lv.1 -> Lv.2  : 250
Lv.2 -> Lv.3  : 500
Lv.3 -> Lv.4  : 900
Lv.4 -> Lv.5  : 1500
Lv.5 -> Lv.6  : 2400
Lv.6 -> Lv.7  : 3600
Lv.7 -> Lv.8  : 5200
Lv.8 -> Lv.9  : 7500
Lv.9 -> Lv.10 : 10000
Lv.10         : 0
```

운영 중 변경 예시:

```sql
UPDATE ego_level_exp
SET need_exp=2000, mod_date=NOW()
WHERE ego_lv=4;
```

변경 후:

```text
.에고리로드
```

---

## 7. ego_level_bonus 전투 보너스

컬럼:

```text
ego_lv
proc_bonus
critical_chance
critical_damage
counter_chance
counter_power
counter_critical
```

운영 중 변경 예시:

```sql
UPDATE ego_level_bonus
SET critical_chance=30, critical_damage=25, mod_date=NOW()
WHERE ego_lv=10;
```

변경 후:

```text
.에고리로드
```

---

## 8. ego_weapon_rule 무기 규칙

컬럼:

```text
type2
원본 item.type2 값

display_name
표시명

default_ability
기본 에고 능력

allowed_abilities
허용 능력 콤마 구분

use_yn
에고 생성 허용 여부
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

예시: 활에 FROST_BIND 제외하기

```sql
UPDATE ego_weapon_rule
SET allowed_abilities='EGO_BALANCE,CRITICAL_BURST,GUARDIAN_SHIELD,EGO_COUNTER,EGO_REVENGE', mod_date=NOW()
WHERE type2='bow';
```

변경 후:

```text
.에고리로드
```

---

## 9. 운영 중 설정 변경 예시

장르대화 딜레이를 2초로 변경:

```sql
UPDATE ego_config
SET config_value='2000', mod_date=NOW()
WHERE config_key='genre_talk_delay_ms';
```

HP 자동 경고 기준을 30%로 변경:

```sql
UPDATE ego_config
SET config_value='30', mod_date=NOW()
WHERE config_key='auto_talk_hp_warn_rate';
```

스턴 성공률을 40%로 변경:

```sql
UPDATE ego_config
SET config_value='40', mod_date=NOW()
WHERE config_key='stun_success_rate';
```

자동반격 해금 레벨을 7로 변경:

```sql
UPDATE ego_config
SET config_value='7', mod_date=NOW()
WHERE config_key='auto_counter_unlock_level';
```

변경 후 공통:

```text
.에고리로드
```

---

## 10. fallback 정책

모든 DB화 기능은 안전 fallback 구조입니다.

```text
ego_config 없음          → Java 기본값 사용
ego_level_exp 없음       → Java 기본 경험치표 사용
ego_level_bonus 없음     → Java 기본 전투 보너스 사용
ego_weapon_rule 없음     → Java 기본 무기 규칙 사용
ego_talk_pack 없음       → Java 기본 대사 사용
ego_bond 없음            → 메모리 유대감만 일부 동작
```

---

## 11. 아직 DB화하지 않은 항목

아래는 DB화보다 서버 코어 연동 확인이 먼저 필요합니다.

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
[완료] EgoConfig.java 추가
[완료] EgoLevelBonus.java 추가
[완료] EgoWeaponRule.java 추가
[완료] EgoDB.init/reload에서 설정/보너스/무기규칙 로드
[완료] EgoWeaponDatabase 경험치표 DB 우선화
[완료] EgoWeaponAbilityController 전투 설정/보너스 DB 우선화
[완료] EgoWeaponTypeUtil 무기 규칙 DB 우선화
[완료] EgoTalk 장르대화 딜레이 DB화
[완료] EgoAutoTalk 자동대사 임계값/딜레이 DB화
[완료] ego_db_config.sql 최신화
[완료] ego_install_euckr.sql 통합 설치 SQL 최신화
[완료] DB 미적용 서버 fallback 유지
[완료] 테스트 명령 추가 없음
```
