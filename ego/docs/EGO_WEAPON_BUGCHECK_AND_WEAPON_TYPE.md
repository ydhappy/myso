# 에고무기 오류/버그 점검 및 무기 종류 구현 문서

## 1. 점검 결과 요약

이번 점검에서 확인한 주요 위험 지점과 보완 내용입니다.

| 구분 | 문제 가능성 | 조치 |
|---|---|---|
| 무기 종류 판정 | `type2` 문자열 비교가 여러 파일에 흩어질 가능성 | `EgoWeaponTypeUtil.java` 추가 |
| 낚싯대 처리 | `fishing_rod`가 무기 슬롯이지만 에고무기로 처리될 수 있음 | `isValidEgoBaseWeapon()`에서 제외 |
| 비무기 처리 | 방어구/소모품/기타 아이템이 잘못 에고화될 가능성 | `item.getItem().getSlot() == Lineage.SLOT_WEAPON` 검사 추가 |
| 능력 중복 | `.에고능력` 여러 번 사용 시 기존 능력이 남아 예전 능력이 먼저 발동 가능 | `setAbility()`에서 기존 능력 비활성화 후 신규 능력만 활성화 |
| 음수/0 데미지 | 빗맞음/무효 데미지에도 능력 발동 가능 | `damage <= 0`이면 발동 차단 |
| 무기별 능력 제한 | 활에 창 전용 광역기, 지팡이에 처형 등 부자연스러운 조합 가능 | `isAbilityAllowed()`로 제한 |
| 상태 출력 부족 | 실제 무기 종류를 확인하기 어려움 | `.에고정보`, `에고 상태`에 무기 종류/type2 표시 |

## 2. 새로 추가된 파일

```text
ego/java/EgoWeaponTypeUtil.java
```

실제 적용 위치:

```text
bitna/src/lineage/world/controller/EgoWeaponTypeUtil.java
```

## 3. 지원 무기 종류

현재 지원하는 `item.구분2(type2)` 값입니다.

| type2 | 표시명 | 에고무기 가능 |
|---|---|---|
| dagger | 단검 | 가능 |
| sword | 한손검 | 가능 |
| tohandsword | 양손검 | 가능 |
| axe | 도끼 | 가능 |
| spear | 창 | 가능 |
| bow | 활 | 가능 |
| staff | 지팡이 | 가능 |
| wand | 완드 | 가능 |
| fishing_rod | 낚싯대 | 불가 |

## 4. 무기 여부 확인 로직

`EgoWeaponTypeUtil.isValidEgoBaseWeapon(item)`이 최종 기준입니다.

```java
public static boolean isValidEgoBaseWeapon(ItemInstance item) {
    if (!isWeaponSlot(item))
        return false;

    String type = getType2(item);
    if (type.length() == 0)
        return false;

    if (isFishingRod(item))
        return false;

    return isMelee(item) || isBow(item) || isMagicWeapon(item);
}
```

즉 아래 조건을 모두 만족해야 합니다.

```text
1. item != null
2. item.getItem() != null
3. item.getItem().getSlot() == Lineage.SLOT_WEAPON
4. type2가 지원 무기 종류
5. fishing_rod 아님
```

## 5. 무기별 능력 제한

| 능력 | 허용 무기 |
|---|---|
| EGO_BALANCE | 모든 에고무기 |
| BLOOD_DRAIN | 근접 무기 |
| MANA_DRAIN | 지팡이, 완드, 단검, 한손검 |
| CRITICAL_BURST | 근접 무기, 활 |
| GUARDIAN_SHIELD | 모든 에고무기 |
| AREA_SLASH | 창, 양손검, 도끼 |
| EXECUTION | 단검, 한손검, 양손검, 도끼 |
| FLAME_BRAND | 근접 무기, 지팡이, 완드 |
| FROST_BIND | 지팡이, 완드, 창, 활 |

## 6. 수정된 파일

```text
ego/java/EgoWeaponAbilityController.java
ego/java/EgoWeaponCommand.java
ego/java/EgoWeaponControlController.java
ego/java/EgoWeaponDatabase.java
```

## 7. 적용 시 추가 복사 필요

이제 에고 시스템 적용 시 아래 파일도 반드시 복사해야 합니다.

```text
ego/java/EgoWeaponTypeUtil.java
→ bitna/src/lineage/world/controller/EgoWeaponTypeUtil.java
```

## 8. 주요 수정 내용

### 8-1. EgoWeaponAbilityController.java

추가 검사:

```java
if (damage <= 0)
    return damage;

if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon))
    return damage;
```

능력 발동 전 무기별 허용 여부 검사:

```java
if (!EgoWeaponTypeUtil.isAbilityAllowed(type.name(), weapon)) {
    type = EgoAbilityType.EGO_BALANCE;
}
```

### 8-2. EgoWeaponCommand.java

`.에고생성` 시 무기 종류 검사:

```java
if (!EgoWeaponTypeUtil.isValidEgoBaseWeapon(weapon)) {
    msg(pc, "\\fR[에고무기] 생성 불가: " + EgoWeaponTypeUtil.getAbilityDenyReason("", weapon));
    return;
}
```

`.에고능력` 시 무기별 허용 능력 검사:

```java
if (!EgoWeaponTypeUtil.isAbilityAllowed(type, weapon)) {
    msg(pc, "\\fR[에고무기] " + EgoWeaponTypeUtil.getAbilityDenyReason(type, weapon));
    return;
}
```

### 8-3. EgoWeaponDatabase.java

기존 문제:

```text
.에고능력 BLOOD_DRAIN
.에고능력 FLAME_BRAND
```

이렇게 여러 번 사용하면 DB에는 두 능력이 모두 enabled=1로 남을 수 있고, `getFirstAbility()`가 예전 능력을 선택할 수 있었습니다.

수정 후:

```java
UPDATE character_item_ego_ability SET enabled=0 WHERE item_objid=?
```

그 다음 신규 능력만 활성화합니다.

```java
INSERT INTO character_item_ego_ability ... ON DUPLICATE KEY UPDATE ... enabled=1
```

## 9. 테스트 체크리스트

### 9-1. 무기 종류 테스트

아래 무기를 착용하고 `.에고생성 테스트` 실행:

- [ ] 단검
- [ ] 한손검
- [ ] 양손검
- [ ] 도끼
- [ ] 창
- [ ] 활
- [ ] 지팡이
- [ ] 완드

아래는 실패해야 정상:

- [ ] 낚싯대
- [ ] 방어구
- [ ] 주문서
- [ ] 포션
- [ ] 화살 단독

### 9-2. 능력 제한 테스트

창 착용:

```text
.에고능력 AREA_SLASH
```

성공해야 정상.

활 착용:

```text
.에고능력 AREA_SLASH
```

실패해야 정상.

지팡이 착용:

```text
.에고능력 MANA_DRAIN
```

성공해야 정상.

지팡이 착용:

```text
.에고능력 EXECUTION
```

실패해야 정상.

### 9-3. 능력 교체 테스트

```text
.에고능력 BLOOD_DRAIN
.에고정보
.에고능력 FLAME_BRAND
.에고정보
```

정상 결과:

```text
능력: FLAME_BRAND
```

`BLOOD_DRAIN`이 계속 표시되면 DB 캐시 리로드 또는 setAbility 적용 여부를 확인해야 합니다.

## 10. 남은 주의사항

### 10-1. getInsideList 컴파일 확인

현재 에고 코드가 주변 객체 조회에 사용합니다.

```java
pc.getInsideList()
```

서버 버전에 따라 접근자가 다르면 실제 메서드명에 맞춰 수정해야 합니다.
기존 서버 코드에서 `getInsideList()` 사용 흔적이 있으므로 현재 저장소 기준으로는 가능성이 높습니다.

### 10-2. DamageController 연결 변수명 확인

문서의 예시는 다음 형태입니다.

```java
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoWeaponAbilityController.applyAttackAbility((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

실제 `DamageController.getDamage(...)`의 변수명이 `target`, `o`, `weapon`, `dmg` 등 다를 수 있으므로, 실제 파일의 변수명에 맞춰 넣어야 합니다.

### 10-3. 운영 서버 테스트 모드 해제

`EgoWeaponAbilityController.java`:

```java
private static final boolean ENABLE_TEST_MODE = true;
```

운영 전에는 DB 기반으로 전환하고 `false`를 권장합니다.
