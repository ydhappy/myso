# 에고무기 특별 능력 적용 가이드

이 문서는 에고무기에 특별한 전투 능력을 부여하는 방법입니다.

## 1. 추가된 파일

```text
ego/java/EgoWeaponAbilityController.java
ego/sql/ego_weapon_ability.sql
ego/docs/EGO_WEAPON_ABILITY_GUIDE.md
```

## 2. 특별 능력 목록

| 코드 | 이름 | 효과 |
|---|---|---|
| EGO_BALANCE | 공명 타격 | 소량 추가 피해 |
| BLOOD_DRAIN | 생명 흡수 | 피해 일부만큼 HP 회복 |
| MANA_DRAIN | 정신 흡수 | 공격 시 MP 회복 |
| CRITICAL_BURST | 치명 폭발 | 높은 추가 피해 |
| GUARDIAN_SHIELD | 수호 의지 | HP 낮을 때 회복 |
| AREA_SLASH | 공명 베기 | 주변 몬스터 광역 피해 |
| EXECUTION | 처형 | HP 낮은 적에게 추가 피해 |
| FLAME_BRAND | 화염 각인 | 화염 이펙트 + 추가 피해 |
| FROST_BIND | 서리 충격 | 서리 이펙트 + 소량 피해 |

## 3. SQL 적용

먼저 기본 SQL을 적용합니다.

```text
ego/sql/ego_weapon.sql
```

그 다음 특별 능력 SQL을 적용합니다.

```text
ego/sql/ego_weapon_ability.sql
```

## 4. 자바 파일 복사

원본:

```text
ego/java/EgoWeaponAbilityController.java
```

복사 위치:

```text
bitna/src/lineage/world/controller/EgoWeaponAbilityController.java
```

## 5. DamageController.java 연결

수정 파일:

```text
bitna/src/lineage/world/controller/DamageController.java
```

### 5-1. 위치 찾기

`getDamage(...)` 메서드 안에서 최종 데미지가 반환되기 전 위치를 찾습니다.

비슷한 구간이 있습니다.

```java
return (int) Math.round(dmg);
```

또는 최종 데미지 계산 후 `return` 하는 부분을 찾습니다.

### 5-2. 적용 코드

최종 반환 직전에 아래 코드를 넣습니다.

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

예시:

```java
// 기존 최종 데미지 계산 완료 후
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}

return (int) Math.round(dmg);
```

`DamageController`와 `EgoWeaponAbilityController`는 같은 패키지입니다.
보통 import는 필요 없습니다.

## 6. ItemWeaponInstance.java 연결 방식

특별 능력은 `DamageController`에 연결하는 것을 추천합니다.

하지만 무기 자체 발동 스킬처럼 처리하고 싶으면 아래 파일에 연결할 수도 있습니다.

```text
bitna/src/lineage/world/object/instance/ItemWeaponInstance.java
```

`toDamage(Character cha, object o)` 메서드 안에서 기존 무기 스킬 발동 전후에 연결할 수 있습니다.

단, 이 방식은 `DamageController` 최종 데미지와 분리되어 있기 때문에 초보자는 추천하지 않습니다.

## 7. 1차 테스트 방식

`EgoWeaponAbilityController.java`에는 테스트 모드가 켜져 있습니다.

```java
private static final boolean ENABLE_TEST_MODE = true;
```

이 상태에서는 착용 중인 모든 무기가 에고무기처럼 특별 능력을 발동할 수 있습니다.

운영 적용 시 반드시 아래처럼 바꾸는 것을 권장합니다.

```java
private static final boolean ENABLE_TEST_MODE = false;
```

그리고 `isEgoWeapon(...)`을 DB 또는 `ItemInstance.isEgoEnabled()` 기반으로 바꿔야 합니다.

## 8. 능력 자동 분류 기준

1차 코드는 무기 이름과 타입으로 능력을 자동 분류합니다.

```text
피/blood 포함: BLOOD_DRAIN
마나/지식/지팡이/완드: MANA_DRAIN
화염/불/flame 포함: FLAME_BRAND
얼음/서리/frost 포함: FROST_BIND
양손검/도끼: CRITICAL_BURST
창: AREA_SLASH
수호/가디언 포함: GUARDIAN_SHIELD
기본: EGO_BALANCE
```

## 9. DB 기반 능력으로 확장하기

운영용으로는 `character_item_ego_ability` 테이블에서 능력을 읽어오는 방식이 좋습니다.

예시 SQL:

```sql
INSERT INTO character_item_ego_ability
(item_objid, ability_type, ability_level, enabled)
VALUES
(123456789, 'BLOOD_DRAIN', 1, 1)
ON DUPLICATE KEY UPDATE ability_level=1, enabled=1;
```

그 다음 `EgoWeaponAbilityController.getAbilityType(...)` 안을 DB 조회 방식으로 바꾸면 됩니다.

## 10. 에고 경험치 연결

`EgoWeaponAbilityController`에는 아래 메서드가 준비되어 있습니다.

```java
EgoWeaponAbilityController.addKillExp(pc, mon);
```

몬스터 사망 처리 구간에서 호출하면 됩니다.

추천 위치:

```text
MonsterInstance 사망 처리 또는 DamageController.toDead(...)
```

예시:

```java
if (cha instanceof PcInstance && o instanceof MonsterInstance) {
    EgoWeaponAbilityController.addKillExp((PcInstance) cha, (MonsterInstance) o);
}
```

단, 현재 1차 구현은 DB 저장 없이 멘트만 준비된 상태입니다.
실제 레벨업 저장은 별도 `EgoWeaponDatabase` 구현이 필요합니다.

## 11. 밸런스 조정 위치

파일:

```text
bitna/src/lineage/world/controller/EgoWeaponAbilityController.java
```

주요 값:

```java
private static final int MAX_EGO_LEVEL = 30;
private static final int BASE_PROC_CHANCE = 3;
private static final int ADD_PROC_CHANCE_PER_LEVEL = 1;
private static final int MAX_PROC_CHANCE = 25;
private static final int AREA_RANGE = 2;
private static final int AREA_MAX_TARGET = 4;
```

초기 추천값:

```text
발동확률: 3~10%
최대확률: 15~20%
광역범위: 2칸
광역대상: 최대 3~4명
흡혈량: 피해량의 3~8%
```

## 12. 주의사항

### 12-1. 광역 피해 주의

`AREA_SLASH`는 주변 몬스터에게 `mon.toDamage(...)`를 직접 호출합니다.
너무 강하게 설정하면 사냥 밸런스가 무너질 수 있습니다.

### 12-2. PvP 주의

현재 광역 피해는 `MonsterInstance`만 대상으로 제한했습니다.
PvP 대상에게 광역 피해를 넣지 않기 위한 안전장치입니다.

### 12-3. 이펙트 ID 주의

서버 클라이언트 버전에 따라 이펙트 ID가 다를 수 있습니다.
이펙트가 안 보이거나 튕기면 `sendEffect(...)` 호출의 effect 값을 바꾸거나 0으로 비활성화하세요.

### 12-4. 테스트 모드 주의

`ENABLE_TEST_MODE = true`는 모든 무기를 에고무기로 취급합니다.
운영 서버에서는 반드시 false로 바꾸고 DB 기반으로 제한하세요.

## 13. 테스트 체크리스트

- [ ] `EgoWeaponAbilityController.java` 복사 완료
- [ ] `DamageController.java` 연결 완료
- [ ] 서버 컴파일 성공
- [ ] 무기 착용 후 몬스터 공격 시 특별 능력 발동 확인
- [ ] HP 흡수/MP 흡수 수치가 과하지 않은지 확인
- [ ] 광역 피해가 몬스터에게만 들어가는지 확인
- [ ] PvP에서 과도한 피해가 발생하지 않는지 확인
- [ ] 이펙트 출력 시 클라이언트 오류가 없는지 확인
- [ ] 운영 전 `ENABLE_TEST_MODE=false` 전환 여부 확인

## 14. 원복 방법

1. `DamageController.java`에 추가한 아래 코드 삭제

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

2. 아래 파일 삭제

```text
bitna/src/lineage/world/controller/EgoWeaponAbilityController.java
```

3. 서버 재빌드

SQL 테이블은 남아 있어도 서버 동작에는 영향이 없습니다.
삭제하려면:

```sql
DROP TABLE IF EXISTS ego_ability_proc_log;
DROP TABLE IF EXISTS character_item_ego_ability;
DROP TABLE IF EXISTS ego_ability_template;
```
