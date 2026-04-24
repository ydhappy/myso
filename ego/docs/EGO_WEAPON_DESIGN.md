# 에고무기 시스템 설계서

## 1. 개요

에고무기 시스템은 착용 중인 무기에 인격을 부여하여, 플레이어가 무기 이름을 부르면 상태를 분석하고 간단한 전투 제어를 수행하는 기능입니다.

1차 목표는 다음입니다.

- 일반 채팅에서 에고 이름 감지
- 에고 자동 반응
- 캐릭터 상태 인식
- 주변 선공 몬스터 감지
- 선공 몬스터 경고
- 가장 가까운 선공 몬스터 자동공격 지정
- 자동공격 중지

## 2. 현재 서버 구조와 연결 지점

### 2.1 채팅 처리

`ChattingController.toChatting(...)`에서 일반 채팅이 처리됩니다.

에고는 명령어가 아닌 일반 채팅으로 호출하는 방식이 자연스럽습니다.

예:

```text
에고
에고 상태
에고 선공
에고 공격
```

따라서 `ChattingController`의 일반 채팅 처리 중, 실제 주변 전파 전에 `EgoWeaponControlController.onNormalChat(pc, msg)`를 호출합니다.

### 2.2 자동공격 처리

현재 서버에는 `AutoAttackThread`가 존재하며, 아래 필드를 기준으로 자동공격합니다.

```java
pc.isAutoAttack
pc.autoAttackTarget
```

에고는 직접 공격 루프를 새로 만들지 않고, 기존 자동공격 구조를 사용합니다.

```java
pc.autoAttackTarget = target;
pc.isAutoAttack = true;
pc.setTarget(target);
```

중지 시:

```java
pc.isAutoAttack = false;
pc.autoAttackTarget = null;
pc.setTarget(null);
pc.resetAutoAttack();
```

### 2.3 몬스터 선공 판단

몬스터 DB는 `monster.atk_type`, `monster.atk_range`, `monster.atk_invis`, `monster.atk_poly` 값을 로딩합니다.

1차 기준:

```java
mon.getMonster().getAtkType() > 0
```

위 조건을 선공/공격형 몬스터로 판단합니다.

서버 DB에서 `atk_type` 의미가 다르면 아래 부분만 수정하면 됩니다.

```java
if (mon.getMonster().getAtkType() <= 0)
    continue;
```

## 3. 대화 흐름

```text
일반 채팅 입력
    ↓
ChattingController.toChatting
    ↓
CommandController.toCommand 검사
    ↓
EgoWeaponControlController.onNormalChat(pc, msg)
    ↓
착용 무기 확인
    ↓
에고 이름 호출 여부 확인
    ↓
명령어 해석
    ↓
상태/조언/선공감지/공격/중지 처리
```

## 4. 1차 명령어

| 입력 | 동작 |
|---|---|
| 에고 | 기본 반응 |
| 에고 상태 | HP/MP/레벨/무기 정보 출력 |
| 에고 조언 | 상태와 선공 몬스터 기준으로 조언 |
| 에고 선공 | 주변 선공 몬스터 감지 |
| 에고 공격 | 가장 가까운 선공 몬스터를 자동공격 대상으로 지정 |
| 에고 멈춰 | 자동공격 중지 |
| 에고 도움 | 사용법 출력 |

## 5. 캐릭터 상태 인식 항목

1차 자바 코드에서 바로 사용하는 항목입니다.

```java
pc.getLevel()
pc.getNowHp()
pc.getTotalHp()
pc.getNowMp()
pc.getTotalMp()
pc.getTarget()
pc.getInventory().getSlot(Lineage.SLOT_WEAPON)
```

추가 확장 가능 항목:

```java
pc.getDynamicReduction()
pc.getDynamicAddDmg()
pc.getDynamicAddPvpDmg()
pc.getDynamicStunResist()
pc.isBuffImmuneToHarm()
pc.isBuffHaste()
pc.isBuffBravery()
pc.getMap()
pc.getX()
pc.getY()
```

## 6. 주변 몬스터 인식

`pc.getInsideList()`에서 주변 객체를 순회합니다.

```java
for (object o : pc.getInsideList()) {
    if (!(o instanceof MonsterInstance))
        continue;

    MonsterInstance mon = (MonsterInstance) o;
}
```

필터 조건:

```java
mon.isDead() == false
mon.getMonster() != null
mon.getMap() == pc.getMap()
mon.getMonster().getAtkType() > 0
Util.isDistance(pc, mon, Lineage.SEARCH_LOCATIONRANGE)
```

## 7. 안전 정책

1차 버전에서 일부 기능을 일부러 제한했습니다.

허용:

- 대화 반응
- 상태 분석
- 선공 몬스터 감지
- 자동공격 대상 지정
- 자동공격 중지

제한:

- 자동 이동
- 자동 귀환
- 자동 물약 난사
- 자동 스킬 난사
- 완전 자동사냥 루프

이유:

- 기존 AI/자동사냥 시스템과 충돌 방지
- 초보자 적용 시 디버깅 난이도 감소
- 캐릭터가 원하지 않는 행동을 반복하는 문제 방지

## 8. DB 설계

`character_item_ego` 테이블은 아이템 objectId 기준으로 에고 정보를 저장합니다.

주요 컬럼:

```sql
ego_enabled
ego_name
ego_personality
ego_level
ego_exp
ego_max_exp
ego_talk_level
ego_control_level
ego_last_talk_time
ego_last_warning_time
```

1차 자바 파일은 DB 로딩 없이 고정 이름 `에고`를 사용합니다.
2차 구현에서 `EgoWeaponDatabase`를 만들어 `character_item_ego`를 로딩하면 무기별 이름/성격을 사용할 수 있습니다.

## 9. 2차 확장 설계

### 9.1 ItemInstance 필드 추가

나중에 진짜 에고무기 필드를 아이템 인스턴스에 붙일 수 있습니다.

```java
protected boolean egoEnabled;
protected String egoName;
protected String egoPersonality;
protected int egoLevel;
protected long egoExp;
protected long egoMaxExp;
```

getter/setter 추가:

```java
public boolean isEgoEnabled() { return egoEnabled; }
public void setEgoEnabled(boolean egoEnabled) { this.egoEnabled = egoEnabled; }

public String getEgoName() { return egoName; }
public void setEgoName(String egoName) { this.egoName = egoName; }

public int getEgoLevel() { return egoLevel; }
public void setEgoLevel(int egoLevel) { this.egoLevel = egoLevel; }
```

### 9.2 에고 경험치

몬스터 사망 처리 시 착용 무기에 경험치를 줄 수 있습니다.

```java
ItemInstance weapon = pc.getInventory().getSlot(Lineage.SLOT_WEAPON);
if (weapon != null && weapon.isEgoEnabled()) {
    EgoWeaponDatabase.addExp(weapon, mon.getMonster().getExp());
}
```

### 9.3 에고 레벨 효과

`DamageController.getDamage(...)` 또는 `ItemWeaponInstance.toDamage(...)`에 레벨 효과를 붙일 수 있습니다.

예:

```java
if (weapon != null && weapon.isEgoEnabled()) {
    dmg += weapon.getEgoLevel();
}
```

또는 확률 발동:

```java
if (Util.random(1, 100) <= weapon.getEgoLevel()) {
    dmg += Util.random(1, weapon.getEgoLevel() * 2);
}
```

## 10. 추천 적용 단계

### 1단계

- `EgoWeaponControlController.java` 적용
- `ChattingController` 연결
- 고정명 `에고`로 테스트

### 2단계

- SQL 적용
- 무기별 에고 이름 저장
- `.에고이름 카르마` 명령어 추가

### 3단계

- 에고 경험치/레벨업 추가
- 몬스터 처치 시 성장

### 4단계

- 에고 성격별 대화
- 위험 자동 경고

### 5단계

- LLM/AI 멘트 연동
- 단, 서버 성능 보호를 위해 캐시/쿨타임 필수

## 11. 테스트 체크리스트

- 무기를 착용하지 않고 `에고` 입력 시 반응하지 않는가?
- 무기 착용 후 `에고` 입력 시 반응하는가?
- `에고 상태`에서 HP/MP가 정상 표시되는가?
- 주변에 선공 몬스터가 없을 때 `에고 선공`이 정상 안내하는가?
- 주변에 선공 몬스터가 있을 때 이름/거리/수량이 표시되는가?
- HP 25% 이하에서 `에고 공격` 명령을 거부하는가?
- `에고 공격` 후 캐릭터가 자동공격을 시작하는가?
- `에고 멈춰` 후 자동공격이 중지되는가?
- 일반 채팅이 에고 호출이 아닐 때 기존 채팅 기능이 유지되는가?

## 12. 문제 발생 시 확인

### 컴파일 오류: getInsideList 없음

서버 버전에 따라 접근자가 없을 수 있습니다.
이 경우 `object.java`에 public getter가 있는지 확인합니다.
없다면 기존 코드에서 사용하는 주변 객체 조회 메서드명으로 바꿔야 합니다.

### 컴파일 오류: resetAutoAttack 없음

`EgoWeaponControlController`는 try-catch로 감싸 두었기 때문에 보통 문제 없습니다.
그래도 오류가 나면 아래 줄을 삭제해도 됩니다.

```java
pc.resetAutoAttack();
```

### 선공 몬스터가 감지되지 않음

DB의 `monster.atk_type` 의미가 다를 수 있습니다.
아래 조건을 서버 DB 기준에 맞게 수정합니다.

```java
if (mon.getMonster().getAtkType() <= 0)
    continue;
```

예를 들어 `atk_type == 1`만 선공이라면:

```java
if (mon.getMonster().getAtkType() != 1)
    continue;
```
