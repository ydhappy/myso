# 에고무기 보강 및 기능 제안 검토

이 문서는 현재 에고무기 구현 기준으로 바로 보강한 내용, 추가하면 좋은 기능, 보류해야 할 기능을 구분해서 정리합니다.

---

## 1. 현재 구현 유지 원칙

```text
기존 서버 코어 유지
PcInstance 공격 로직 변경 금지
DamageController 무기 타입/데미지 공식 변경 금지
원본 item.type2 변경 금지
EgoCombat 추가 금지
에고는 기존 코어 위에 보조 기능으로만 추가
```

에고 형태는 실제 무기 타입 변형이 아닙니다.

```text
ego.form = 표시형태, 대화형태, 이미지형태
data item.type2 = 실제 전투 무기 타입
```

---

## 2. 이번 보강 반영 내용

### 2.1 능력 보너스 컬럼 실제 적용

기존 `ego_skill.rate_bonus`, `ego_skill.dmg_bonus`, `ego_skill.skill_lv` 컬럼은 DB에는 있었지만 전투 보조능력 계산에서 충분히 활용되지 않았습니다.

보강 후:

```text
skill_lv     → 에고 레벨에 더해 실질 능력 레벨 계산
rate_bonus   → 에고 능력 발동 확률 추가
-dmg_bonus   → 능력 발동 시 추가 피해 보정
```

실질 레벨 계산:

```text
effectiveLevel = ego_lv + (skill_lv - 1)
최대 30 제한
```

발동 확률 계산:

```text
기본확률 3%
+ 에고/능력 실질 레벨당 1%
+ rate_bonus
최대 25%
최소 1%
```

피해 보정:

```text
능력 발동 후 결과 피해에 dmg_bonus 추가
단, dmg_bonus가 0 이하이면 무시
```

수정 파일:

```text
ego/java/EgoWeaponAbilityController.java
```

---

## 3. 바로 추가 추천 기능

### 3.1 에고 경험치 배율 설정

현재 경험치:

```text
공격 경험치: 3초마다 +1
일반 몬스터 처치 hook 연결 시: +5
보스 처치 hook 연결 시: +55
```

추천:

```text
ego_setting 테이블 추가
attack_exp
kill_exp
boss_exp
exp_delay_ms
max_level
```

효과:

```text
운영자가 Java 수정 없이 성장 속도 조절 가능
테스트 서버/본서버 배율 분리 가능
```

우선순위: 높음
위험도: 낮음

---

### 3.2 에고 능력 기본값 DB화 완성

현재 `ego_skill_base` 테이블은 설치되지만 Java 전투 계산은 아직 상수 중심입니다.

추천:

```text
ego_skill_base.base_rate
ego_skill_base.lv_rate
ego_skill_base.max_rate
ego_skill_base.cool_ms
ego_skill_base.effect
```

을 `EgoWeaponAbilityController`에서 읽게 변경.

효과:

```text
능력별 발동률, 쿨타임, 이펙트, 최소레벨을 DB에서 조절 가능
운영 밸런스 조정 쉬움
```

우선순위: 높음
위험도: 중간
주의: 캐시 구조와 .에고리로드 연동 필요

---

### 3.3 에고 레벨별 해금 기능

추천 해금 예시:

```text
Lv.1  기본 대화/상태 인식
Lv.3  선공 몬스터 경고 강화
Lv.5  상대 캐릭터 감지 상세화
Lv.7  능력 발동률 소폭 증가
Lv.10 에고 전용 대사 추가
Lv.15 보스 감지 대사 추가
Lv.20 고급 조언 추가
Lv.30 최종 각성 표시
```

효과:

```text
성장 체감 강화
에고 레벨업 의미 증가
```

우선순위: 중간
위험도: 낮음

---

### 3.4 에고 잠금 기능

추천 명령어:

```text
.에고잠금
.에고잠금해제
```

추천 컬럼:

```text
ego.lock_yn TINYINT(1) DEFAULT 0
```

효과:

```text
실수로 삭제/비활성화/형태변경 방지
운영 보정 시 안전성 증가
```

우선순위: 중간
위험도: 낮음

---

### 3.5 에고 삭제/분리 기능

추천 명령어:

```text
.에고삭제 확인
```

정책 제안:

```text
삭제 시 ego.use_yn = 0
데이터는 보존
ego_skill도 use_yn = 0
완전 삭제는 운영 SQL로만 처리
```

효과:

```text
유저가 실수로 삭제해도 복구 가능
운영 로그 추적 가능
```

우선순위: 중간
위험도: 낮음

---

### 3.6 에고 로그 저장

현재 `ego_log` 테이블은 존재하지만 능력 발동 기록 저장이 미구현입니다.

추천 저장 시점:

```text
능력 발동 성공 시
레벨업 시
운영 명령으로 능력/이름 변경 시
```

추천 기록:

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

효과:

```text
밸런스 점검
버그 추적
어뷰징 확인
```

우선순위: 높음
위험도: 낮음~중간
주의: 전투마다 저장하면 DB 부하 발생. 능력 발동 성공 시만 저장 권장.

---

## 4. 보류 추천 기능

### 4.1 실제 무기 타입 변형

보류 사유:

```text
PcInstance 공격 방식 변경 필요
DamageController 무기 공식 변경 가능성
화살/사거리/모션/자동사냥/스킬 조건이 모두 흔들림
기존 서버 코어 유지 원칙과 충돌
```

판정:

```text
보류 또는 별도 독립 브랜치에서만 실험
```

---

### 4.2 자동 전투 제어 강화

예시:

```text
에고가 자동으로 스킬 사용
에고가 자동으로 물약 사용
에고가 자동으로 귀환 판단
```

보류 사유:

```text
봇/자동사냥 경계가 모호해짐
서버 정책 문제 가능성
기존 AI/자동사냥 로직과 충돌 가능
```

판정:

```text
상태 조언/경고까지만 권장
직접 자동 조작은 신중히 적용
```

---

## 5. 최우선 다음 작업 제안

### 1순위: ego_log 발동 기록 저장

이유:

```text
현재 능력 발동/레벨업이 실제로 얼마나 발생하는지 추적 가능
밸런스 조정 근거 확보
버그 발생 시 추적 쉬움
```

---

### 2순위: ego_skill_base DB 캐시화

이유:

```text
운영자가 능력 밸런스를 SQL로 조절 가능
Java 재빌드 없이 확률/쿨타임/이펙트 조정 가능
```

---

### 3순위: 에고 잠금/삭제 기능

이유:

```text
운영 안정성 증가
유저 실수 방지
복구 편의성 증가
```

---

## 6. 현재 적용 상태 체크리스트

```text
[완료] EgoCombat 없음
[완료] type2 변형 제거
[완료] +0 무기만 에고 생성 가능
[완료] 인벤토리 [에고] 색상 표식
[완료] 에고 경험치 DB 저장
[완료] 에고 레벨업
[완료] rate_bonus / dmg_bonus / skill_lv 일부 활용
[필요] ego_log 기록 저장
[필요] ego_skill_base 전투 계산 연동
[선택] 에고 잠금/삭제 명령
[선택] 에고 레벨별 대화/기능 해금
```

---

## 7. 운영 권장값

초기 운영 추천:

```text
attack_exp = 1
attack_exp_delay_ms = 3000
kill_exp = 5
boss_exp = 55
max_level = 30
max_proc_chance = 25
```

보수적 운영 추천:

```text
attack_exp = 1
attack_exp_delay_ms = 5000
kill_exp = 3
boss_exp = 30
max_level = 20
max_proc_chance = 15
```

빠른 테스트 추천:

```text
attack_exp = 10
attack_exp_delay_ms = 1000
kill_exp = 50
boss_exp = 500
max_level = 30
max_proc_chance = 50
```

테스트용 값은 본서버 적용 전 반드시 원복하세요.
