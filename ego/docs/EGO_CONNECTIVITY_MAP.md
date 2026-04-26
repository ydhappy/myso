# 에고 연결성 맵

목적:

```text
DB 테이블
DB 컬럼
Java 로더
Java 사용 메서드
외부 연결 진입점
병합/호환 구조
```

위 항목을 한눈에 연결하기 위한 문서입니다.

---

## 1. 병합 결과 요약

### 병합 완료

```text
ego_level_exp + ego_level_bonus -> ego_level
ego_bond -> ego.bond / ego.bond_reason
```

### 구버전 fallback 유지

```text
ego_level_exp      구버전 경험치표 fallback
ego_level_bonus    구버전 전투보너스 fallback
ego_bond           구버전 유대감 fallback
```

### 신규 우선 구조

```text
ego_level          레벨 경험치 + 전투 보너스 통합
ego.bond           유대감
ego.bond_reason    마지막 유대감 증가 사유
```

---

## 2. 외부 연결 진입점

외부 서버 코드에서는 가능하면 아래만 연결합니다.

```text
EgoCore.java
```

| 기존 서버 위치 | 호출 메서드 | 내부 연결 |
|---|---|---|
| 서버 시작 | EgoCore.init(con) | EgoSchema.silentCheck → EgoDB.init |
| 운영자 리로드 | EgoCore.reload(con) | EgoSchema.silentCheck → EgoDB.reload |
| CommandController | EgoCore.command(o,key,st) | EgoCmd.run |
| ChattingController | EgoCore.chat(o,msg) | EgoTalk.chat |
| 캐릭터 루프 | EgoCore.tick(pc) | EgoTalk.warning → EgoAutoTalk |
| DamageController 공격 | EgoCore.attack(...) | EgoSkill.attack |
| DamageController 피격 | EgoCore.defense(...) | EgoSkill.defense |
| 스키마 진단 | EgoCore.schemaReport(con) | EgoSchema.report |

---

## 3. DB Facade

짧은 이름용 DB Facade:

```text
EgoDb.java
```

역할:

```text
EgoDB / EgoWeaponDatabase 직접 접근을 줄이기 위한 래퍼
외부 코드에서 DB 접근이 필요하면 EgoDb 사용 권장
```

---

## 4. 스키마 중앙 검증

```text
EgoSchema.java
```

검증 대상:

```text
ego
ego_skill
ego_skill_base
ego_log
ego_talk_pack
ego_config
ego_level
ego_weapon_rule
```

구버전 테이블은 fallback용이므로 필수 검증 대상에서 제외합니다.

```text
ego_bond
ego_level_exp
ego_level_bonus
```

사용:

```java
EgoCore.schemaOk(con);
EgoCore.schemaReport(con);
```

서버 시작/리로드 시:

```java
EgoSchema.silentCheck(con);
```

실패해도 서버를 중단하지 않습니다. 보정 SQL 적용 여부를 빠르게 확인하기 위한 장치입니다.

---

## 5. 테이블별 연결성

### 5.1 ego

| 컬럼 | 의미 | Java 로드/사용 |
|---|---|---|
| item_id | 아이템 objectId | EgoWeaponDatabase.loadEgoInfo / find |
| char_id | 소유 캐릭터 objectId | EgoWeaponDatabase.enableEgo |
| use_yn | 사용 여부 | EgoWeaponDatabase.loadEgoInfo |
| ego_name | 호출 이름 | EgoWeaponDatabase.getEgoName / setEgoName |
| ego_type | 말투 예의/예의반대 | EgoWeaponDatabase.getTone / setTone |
| ego_lv | 레벨 | EgoWeaponDatabase.getEgoLevel / addExp |
| ego_exp | 현재 경험치 | EgoWeaponDatabase.addExp |
| need_exp | 다음 필요 경험치 | EgoWeaponDatabase.addExp |
| talk_lv | 대화 단계 | EgoWeaponDatabase.loadEgoInfo |
| ctrl_lv | 제어 단계 | EgoWeaponDatabase.loadEgoInfo |
| last_talk | 마지막 대화 시간 | 보존 컬럼 |
| last_warn | 마지막 경고 시간 | 보존 컬럼 |
| bond | 유대감 | EgoBond.loadMerged / saveMerged |
| bond_reason | 마지막 유대감 증가 사유 | EgoBond.saveMerged |

주요 메서드:

```text
EgoDb.create
EgoDb.delete
EgoDb.rename
EgoDb.level
EgoDb.name
EgoWeaponDatabase.enableEgo
EgoWeaponDatabase.disableEgo
EgoWeaponDatabase.addExp
EgoBond.get
EgoBond.addTalk
EgoBond.addLevelUp
EgoBond.addCounter
EgoBond.addStun
```

### 5.2 ego_level

병합 테이블입니다.

| 컬럼 | 의미 | Java 사용 |
|---|---|---|
| ego_lv | 레벨 0~10 | EgoLevel.reload |
| need_exp | 다음 레벨 필요 경험치 | EgoLevel.needExp / EgoWeaponDatabase.getNeedExp |
| proc_bonus | 스킬 발동률 추가 | EgoLevel.procBonus |
| critical_chance | 치명률 추가 | EgoLevel.criticalChance |
| critical_damage | 치명 추가 피해 | EgoLevel.criticalDamage |
| counter_chance | 피격 반격 확률 | EgoLevel.counterChance |
| counter_power | 반격 피해 비율 | EgoLevel.counterPower |
| counter_critical | 반격 치명 확률 | EgoLevel.counterCritical |
| use_yn | 사용 여부 | EgoLevel.reload |

사용처:

```text
EgoWeaponDatabase.getNeedExp
EgoWeaponDatabase.addExp
EgoWeaponAbilityController.getProcChance
EgoWeaponAbilityController.tryCounter
EgoWeaponAbilityController.applyAttackByType
```

### 5.3 ego_skill

| 컬럼 | 의미 | Java 로드/사용 |
|---|---|---|
| id | 능력 row id | EgoWeaponDatabase.loadAbilityInfo |
| item_id | 에고 아이템 id | EgoWeaponDatabase.getAbilities |
| skill | 능력 코드 | EgoWeaponAbilityController.getAbilityType |
| skill_lv | 능력 레벨 | EgoWeaponAbilityController.applyAttackAbility |
| rate_bonus | 추가 발동률 | EgoWeaponAbilityController.getProcChance |
| dmg_bonus | 추가 피해 | EgoWeaponAbilityController.applyAttackAbility |
| last_proc | 마지막 발동 시간 | EgoWeaponAbilityController.markProc |
| use_yn | 사용 여부 | EgoWeaponDatabase.loadAbilityInfo |

### 5.4 ego_skill_base

| 컬럼 | 의미 | Java 로드/사용 |
|---|---|---|
| skill | 능력 코드 | EgoWeaponAbilityController.loadSkillBase |
| label | 표시명 | 로드 보존 |
| memo | 설명 | DB 관리용 |
| base_rate | 기본 발동률 | getProcChance |
| lv_rate | 레벨당 발동률 | getProcChance |
| max_rate | 최대 발동률 | getProcChance |
| min_lv | 최소 레벨 | applyAttackAbility |
| cool_ms | 쿨타임 | checkCooldown |
| effect | 이펙트 | applyAttackByType |
| use_yn | 사용 여부 | loadSkillBase |

### 5.5 ego_log

| 컬럼 | 의미 | Java 사용 |
|---|---|---|
| item_id | 에고 아이템 id | EgoWeaponAbilityController.writeLog |
| char_id | 캐릭터 id | writeLog |
| char_name | 캐릭터 이름 | writeLog |
| target_name | 대상 이름 | writeLog |
| skill | 발동 능력 | writeLog |
| base_dmg | 기본 피해 | writeLog |
| final_dmg | 최종 피해 | writeLog |
| add_dmg | 추가 피해 | writeLog |

### 5.6 ego_talk_pack

| 컬럼 | 의미 | Java 사용 |
|---|---|---|
| id | 대사 id | EgoTalkPack.reload |
| genre | 장르 | EgoTalkPack.find |
| tone | 말투 | EgoTalkPack.find |
| keyword | 예비 키워드 | 보존 컬럼 |
| message | 출력 대사 | EgoTalkHistory.pick |
| use_yn | 사용 여부 | EgoTalkPack.reload |

### 5.7 ego_config

| config_key | Java 사용 |
|---|---|
| genre_talk_delay_ms | EgoTalk.checkGenreDelay |
| auto_talk_hp_warn_rate | EgoAutoTalk.warning |
| auto_talk_mp_warn_rate | EgoAutoTalk.warning |
| auto_talk_idle_hp_rate | EgoAutoTalk.warning |
| auto_talk_idle_mp_rate | EgoAutoTalk.warning |
| auto_talk_hp_warn_delay_ms | EgoAutoTalk.check |
| auto_talk_mp_warn_delay_ms | EgoAutoTalk.check |
| auto_talk_boss_warn_delay_ms | EgoAutoTalk.check |
| auto_talk_idle_delay_ms | EgoAutoTalk.check |
| attack_ego_exp | EgoWeaponAbilityController.gainAttackExp |
| attack_exp_delay_ms | EgoWeaponAbilityController.gainAttackExp |
| kill_ego_exp | EgoWeaponAbilityController.addKillExp |
| boss_kill_ego_exp | EgoWeaponAbilityController.addKillExp |
| exp_message_rate | EgoWeaponAbilityController.addExp |
| proc_message_delay_ms | EgoWeaponAbilityController.say |
| counter_unlock_level | EgoWeaponAbilityController.applyDefenseAbility |
| auto_counter_unlock_level | EgoWeaponAbilityController.applyDefenseAbility |
| auto_counter_cool_ms | EgoWeaponAbilityController.tryCounter |
| auto_counter_chance | EgoWeaponAbilityController.tryCounter |
| stun_level | EgoWeaponAbilityController.getStunLevel |
| stun_success_rate | EgoWeaponAbilityController.tryEgoStun |
| stun_time | EgoWeaponAbilityController.tryEgoStun |
| stun_effect | EgoWeaponAbilityController.tryEgoStun |
| stun_cool_ms | EgoWeaponAbilityController.tryEgoStun |
| guardian_shield_hp_rate | EgoWeaponAbilityController.applyAttackByType |
| execution_target_hp_rate | EgoWeaponAbilityController.applyAttackByType |
| area_range | EgoWeaponAbilityController.applyAttackByType |
| area_max_target | EgoWeaponAbilityController.applyAttackByType |

### 5.8 ego_weapon_rule

| 컬럼 | Java 사용 |
|---|---|
| type2 | EgoWeaponRule.get/hasRule |
| display_name | EgoWeaponTypeUtil.getDisplayTypeName |
| default_ability | EgoWeaponTypeUtil.getDefaultAbilityType |
| allowed_abilities | EgoWeaponTypeUtil.isAbilityAllowed |
| use_yn | EgoWeaponTypeUtil.isSupportedType |

---

## 6. 구버전 fallback 연결

### ego_level_exp

```text
EgoLevel.loadLegacyExp()
```

### ego_level_bonus

```text
EgoLevel.loadLegacyBonus()
```

### ego_bond

```text
EgoBond.loadLegacy()
EgoBond.saveLegacy()
```

---

## 7. 병합/최소화 결론

```text
DB 실행 파일:
- 신규: ego_install_euckr.sql
- 기존: ego_update_euckr.sql

외부 Java 연결:
- EgoCore.java

DB 접근 Facade:
- EgoDb.java

스키마 검증:
- EgoSchema.java

레벨 통합:
- EgoLevel.java

기능 내부 파일:
- public class 제약 때문에 기능별 분리 유지
```

---

## 8. 연결성 점검 방법

Java에서:

```java
String report = EgoCore.schemaReport(con);
boolean ok = EgoCore.schemaOk(con);
```

DB에서:

```sql
SHOW TABLES LIKE 'ego%';
SELECT item_id, ego_name, ego_lv, ego_exp, need_exp, bond, bond_reason FROM ego;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_weapon_rule ORDER BY type2;
```
