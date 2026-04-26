# 에고 기능 DB화 정밀검토 및 반영 현황

## 1. DB화 기준

에고 기능 중 DB화 우선순위는 다음 기준으로 정했습니다.

```text
1순위: 운영 중 자주 바꾸는 값
2순위: 서버마다 밸런스가 달라질 수 있는 값
3순위: 대사/텍스트처럼 운영자가 직접 편집하고 싶은 값
4순위: 로그/상태처럼 누적 조회가 필요한 값
5순위: 코드 흐름 자체를 바꾸는 위험 기능은 후순위
```

## 2. 이미 DB화된 기능

```text
ego                      에고 기본 정보
ego_skill                에고 무기별 능력
ego_skill_base           에고 스킬 기본 발동률/쿨타임/이펙트
ego_log                  에고 전투 로그
ego_bond                 에고 유대감
ego_talk_pack            에고 장르/자연대화 DB 대사팩
ego_config               에고 공통 설정값
ego_level_exp            에고 레벨별 필요 경험치
```

## 3. 이번에 구현 완료한 DB화

### 3.1 공통 설정 DB화

추가 파일:

```text
ego/java/EgoConfig.java
```

추가 SQL:

```text
ego/sql/ego_db_config.sql
```

DB 테이블:

```text
ego_config
```

현재 DB 설정으로 이동한 값:

```text
genre_talk_delay_ms
장르대화 연속 입력 방지 딜레이

auto_talk_hp_warn_rate
HP 자동 위험 대사 발동 기준

auto_talk_mp_warn_rate
MP 자동 안내 대사 발동 기준

auto_talk_idle_hp_rate
auto_talk_idle_mp_rate
안정 상태 자동 대사 기준

auto_talk_hp_warn_delay_ms
auto_talk_mp_warn_delay_ms
auto_talk_boss_warn_delay_ms
auto_talk_idle_delay_ms
상황별 자동 대사 재출력 딜레이
```

### 3.2 레벨별 경험치 DB화

DB 테이블:

```text
ego_level_exp
```

수정 파일:

```text
ego/java/EgoWeaponDatabase.java
```

동작 방식:

```text
1. .에고리로드 또는 서버 시작 시 ego_level_exp 로드
2. 테이블이 있으면 DB 경험치표 사용
3. 테이블이 없으면 Java fallback 경험치표 사용
4. Lv.10은 항상 need_exp=0 고정
```

기본값:

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

### 3.3 대화/자동대사 DB 설정 연결

수정 파일:

```text
ego/java/EgoTalk.java
ego/java/EgoAutoTalk.java
```

동작 방식:

```text
EgoConfig.getLong(...)
EgoConfig.percent(...)
```

DB 설정이 없으면 기존 Java 기본값으로 동작합니다.

## 4. 적용 방법

기존/신규 서버 공통:

```sql
SOURCE ego/sql/ego_db_config.sql;
```

서버에서 즉시 반영:

```text
.에고리로드
```

## 5. 운영 중 설정 변경 예시

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

Lv.4 -> Lv.5 필요 경험치를 2000으로 변경:

```sql
UPDATE ego_level_exp
SET need_exp=2000, mod_date=NOW()
WHERE ego_lv=4;
```

변경 후:

```text
.에고리로드
```

## 6. DB화 가능하지만 이번 단계에서 보류한 항목

### 6.1 레벨별 전투 배열

현재 Java 배열:

```text
LEVEL_PROC_BONUS
LEVEL_CRITICAL_CHANCE
LEVEL_CRITICAL_DAMAGE
LEVEL_COUNTER_CHANCE
LEVEL_COUNTER_POWER
LEVEL_COUNTER_CRITICAL
```

DB화 후보 테이블:

```text
ego_level_bonus
```

보류 이유:

```text
전투 계산 핵심이라 서버별 DamageController/PcInstance 차이에 영향 큼
잘못된 값 입력 시 밸런스 급변 가능
먼저 경험치/대화/자동대사 DB화를 안정화한 뒤 적용 권장
```

### 6.2 스턴/반격 세부 수치

현재 Java 상수:

```text
STUN_SUCCESS_RATE
STUN_TIME
STUN_COOL_MS
AUTO_COUNTER_COOL_MS
AREA_RANGE
AREA_MAX_TARGET
```

DB화 후보:

```text
ego_config
```

일부는 이미 ego_skill_base.cool_ms와 중복될 수 있어 중복 설계를 피해야 합니다.

### 6.3 무기 타입별 허용 능력

현재 Java 유틸:

```text
EgoWeaponTypeUtil
```

DB화 후보:

```text
ego_weapon_rule
```

보류 이유:

```text
서버 item.type2 값이 코어마다 다를 수 있음
잘못 DB화하면 에고 생성/능력 선택이 막힐 수 있음
현재는 Java fallback 유지가 안전
```

## 7. 다음 추천 순서

```text
1. ego_db_config.sql 적용
2. .에고리로드
3. 경험치표 변경 테스트
4. 자동대사 임계값 변경 테스트
5. 문제 없으면 ego_level_bonus DB화
6. 이후 ego_weapon_rule DB화 검토
```

## 8. 현재 완료 체크

```text
[완료] EgoConfig.java 추가
[완료] EgoDB.init/reload에서 EgoConfig 로드
[완료] EgoWeaponDatabase 경험치표 DB 우선화
[완료] EgoTalk 장르대화 딜레이 DB화
[완료] EgoAutoTalk 자동대사 임계값/딜레이 DB화
[완료] ego_db_config.sql 추가
[완료] DB 미적용 서버 fallback 유지
[완료] 테스트 명령 추가 없음
```
