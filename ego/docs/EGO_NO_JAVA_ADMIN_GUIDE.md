# 에고무기 무자바 생성/편집 가이드

이 문서는 Java 파일을 추가 수정하지 않고, DB/SQL만으로 에고무기를 생성·편집·조회·비활성화하는 운영 편의 문서입니다.

## 1. 전제

이 문서는 다음 상황을 위한 문서입니다.

```text
- 서버코어 Java는 더 이상 건드리고 싶지 않다.
- 이미 에고 DB 테이블은 적용했다.
- 이미 서버코어가 에고 DB를 읽는 연결은 되어 있다.
- 운영자는 DB 툴에서 에고무기 생성/편집을 하고 싶다.
```

주의:

```text
Java 연결 자체가 전혀 없는 서버에서는 SQL만 넣어도 게임 안에서 기능이 발동하지 않습니다.
SQL은 데이터 생성/편집용이고, 실제 기능 사용은 서버코어가 해당 테이블을 읽어야 가능합니다.
```

## 2. 사용할 SQL 파일

```text
ego/sql/ego_no_java_admin.sql
```

이 파일은 아래 작업을 지원합니다.

```text
- 에고 상태 조회
- 에고 생성/재활성화
- 에고 이름 변경
- 에고 성격 변경
- 에고 능력 설정
- 에고 레벨/경험치 보정
- 에고 능력 레벨/확률/피해 보정
- 에고 비활성화
- 에고 완전 삭제
- 전체 에고 목록 조회
- 이상 데이터 점검
- 이상 데이터 자동 보정
```

## 3. 기본 작업 순서

1. DB 백업
2. `ego_weapon.sql` 적용 여부 확인
3. `ego_weapon_ability.sql` 적용 여부 확인
4. 캐릭터 `cha_objid` 확인
5. 무기 `item_objid` 확인
6. `ego_no_java_admin.sql` 상단 변수 수정
7. 필요한 SQL 섹션 실행
8. 서버에서 에고 DB 캐시 리로드
9. 게임에서 확인

## 4. 변수 설정

`ego_no_java_admin.sql` 상단에 있습니다.

```sql
SET @CHA_OBJID := 100001;
SET @ITEM_OBJID := 123456789;
SET @EGO_NAME := '카르마';
SET @EGO_PERSONALITY := 'guardian';
SET @EGO_ABILITY := 'EGO_BALANCE';
```

여기만 바꾸고 실행하면 됩니다.

## 5. cha_objid 찾는 방법

서버마다 캐릭터 테이블명이 다릅니다.

후보 테이블명:

```text
characters
character
character_info
pc
player
players
```

후보 컬럼명:

```text
objid
object_id
cha_objid
id
name
char_name
```

예시 SQL:

```sql
SELECT *
FROM characters
WHERE name = '캐릭터명';
```

또는:

```sql
SELECT objid, name, level
FROM characters
WHERE name LIKE '%캐릭터명%';
```

myso/리니지 계열 서버에서 자주 쓰는 값은 보통 캐릭터 고유 objectId입니다.

## 6. item_objid 찾는 방법

서버마다 인벤토리 테이블명이 다릅니다.

후보 테이블명:

```text
characters_inventory
character_items
character_item
inventory
items
warehouse
```

후보 컬럼명:

```text
item_objid
object_id
objid
item_id
item_name
name
cha_objid
char_objid
owner_id
equipped
is_equipped
```

예시 SQL 1:

```sql
SELECT *
FROM characters_inventory
WHERE cha_objid = 100001;
```

예시 SQL 2:

```sql
SELECT *
FROM characters_inventory
WHERE cha_objid = 100001
  AND equipped = 1;
```

예시 SQL 3:

```sql
SELECT *
FROM characters_inventory
WHERE cha_objid = 100001
  AND item_name LIKE '%검%';
```

정확한 테이블명과 컬럼명은 서버코어 DB 구조에 맞게 바꿔야 합니다.

## 7. 에고 생성

`ego_no_java_admin.sql`에서 아래 섹션을 실행합니다.

```text
2. 에고 생성 또는 재활성화
3. 능력 설정
13. 적용 후 확인
```

게임 안에서는 서버 DB 캐시 리로드 후 확인합니다.

```text
.에고리로드
.에고정보
카르마 상태
```

## 8. 에고 이름 변경

```sql
SET @ITEM_OBJID := 123456789;
SET @EGO_NAME := '루나';

UPDATE character_item_ego
SET ego_name = @EGO_NAME
WHERE item_objid = @ITEM_OBJID
  AND ego_enabled = 1;
```

그 다음:

```text
.에고리로드
루나 상태
```

## 9. 에고 능력 변경

```sql
SET @ITEM_OBJID := 123456789;
SET @EGO_ABILITY := 'BLOOD_DRAIN';

UPDATE character_item_ego_ability
SET enabled = 0
WHERE item_objid = @ITEM_OBJID;

INSERT INTO character_item_ego_ability
(
    item_objid,
    ability_type,
    ability_level,
    proc_chance_bonus,
    damage_bonus,
    last_proc_time,
    enabled
)
VALUES
(
    @ITEM_OBJID,
    @EGO_ABILITY,
    1,
    0,
    0,
    0,
    1
)
ON DUPLICATE KEY UPDATE
    enabled = 1,
    ability_level = IF(ability_level < 1, 1, ability_level);
```

그 다음:

```text
.에고리로드
.에고정보
```

## 10. 추천 능력

```text
한손검: EGO_BALANCE, FLAME_BRAND, BLOOD_DRAIN
양손검: CRITICAL_BURST, EXECUTION, AREA_SLASH
도끼: CRITICAL_BURST, EXECUTION, AREA_SLASH
창: AREA_SLASH, FROST_BIND
활: CRITICAL_BURST, FROST_BIND, EGO_BALANCE
지팡이: MANA_DRAIN, FROST_BIND, FLAME_BRAND
완드: MANA_DRAIN, FROST_BIND, FLAME_BRAND
수호 컨셉: GUARDIAN_SHIELD
```

## 11. 에고 비활성화

데이터는 남기고 기능만 끕니다.

```sql
SET @ITEM_OBJID := 123456789;

UPDATE character_item_ego
SET ego_enabled = 0
WHERE item_objid = @ITEM_OBJID;

UPDATE character_item_ego_ability
SET enabled = 0
WHERE item_objid = @ITEM_OBJID;
```

## 12. 에고 완전 삭제

신중히 사용하세요.

```sql
SET @ITEM_OBJID := 123456789;

DELETE FROM character_item_ego_ability
WHERE item_objid = @ITEM_OBJID;

DELETE FROM character_item_ego
WHERE item_objid = @ITEM_OBJID;
```

## 13. 운영 편의 팁

### 13.1 DB 툴에서 즐겨찾기 쿼리로 등록

아래 파일을 HeidiSQL, DBeaver, Navicat 등에 즐겨찾기 쿼리로 등록하면 편합니다.

```text
ego/sql/ego_no_java_admin.sql
```

### 13.2 실수 방지

작업 전 항상 현재 상태를 조회하세요.

```sql
SELECT * FROM character_item_ego WHERE item_objid = @ITEM_OBJID;
SELECT * FROM character_item_ego_ability WHERE item_objid = @ITEM_OBJID;
```

### 13.3 캐시 리로드

DB에서 수정한 뒤 서버가 캐시를 쓰고 있으면 바로 반영되지 않을 수 있습니다.

게임 안에서:

```text
.에고리로드
```

또는 서버 재시작.

## 14. 이상 데이터 점검

### 14.1 에고는 켜져 있는데 능력이 없음

```sql
SELECT e.*
FROM character_item_ego e
LEFT JOIN character_item_ego_ability a
       ON a.item_objid = e.item_objid
      AND a.enabled = 1
WHERE e.ego_enabled = 1
  AND a.uid IS NULL;
```

### 14.2 활성 능력이 2개 이상

```sql
SELECT item_objid, COUNT(*) AS enabled_ability_count
FROM character_item_ego_ability
WHERE enabled = 1
GROUP BY item_objid
HAVING COUNT(*) > 1;
```

### 14.3 레벨/경험치 이상

```sql
SELECT *
FROM character_item_ego
WHERE ego_level < 1
   OR ego_max_exp < 1
   OR ego_exp < 0;
```

## 15. 자바를 건드리지 않는다는 의미

이 문서의 의미는 다음과 같습니다.

```text
- 새 Java 기능을 추가하지 않는다.
- 기존 Java 파일을 재수정하지 않는다.
- 에고 생성/편집은 DB에서 처리한다.
- 서버가 DB 캐시를 쓰면 .에고리로드 또는 재시작으로 반영한다.
```

단, 최초 1회 에고 시스템 자체를 서버코어에 붙이는 작업은 별도입니다.
이미 붙어 있다는 전제에서 이 문서를 사용하세요.
