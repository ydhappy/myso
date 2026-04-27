# 에고 최종 정밀 점검표

## 1. 현재 실사용 기준

### 신규 서버

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

### 기존 서버

```sql
SOURCE ego/sql/ego_update_euckr.sql;
```

`SOURCE`가 안 되는 DB 툴은 아래 순서로 직접 실행합니다.

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
SOURCE ego/sql/ego_db_config.sql;
SOURCE ego/sql/ego_merge_schema_euckr.sql;
SOURCE ego/sql/ego_talk_pack_dedupe.sql;
```

서버 안에서는 아래로 즉시 반영합니다.

```text
.에고리로드
```

---

## 2. Java 외부 연결 기준

외부 서버 코드에서는 아래 클래스만 연결합니다.

```text
EgoCore.java
```

### 서버 시작

```java
EgoCore.init(con);
```

### 리로드

```java
EgoCore.reload(con);
```

### 명령어

서버별로 아래 셋 중 맞는 것을 사용합니다.

```java
EgoCore.command(o, key, st);
EgoCore.command(o, key, args);
EgoCore.command(o, key);
```

### 일반 채팅

```java
if (EgoCore.chat(o, msg)) {
    return;
}
```

### 자동 상황 대사

```java
EgoCore.tick(pc);
```

### 공격 보정

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoCore.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

### 피격 보정

```java
if (o instanceof Character) {
    dmg = EgoCore.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}
```

---

## 3. 삭제 완료

```text
ego/java/EgoLevelBonus.java
```

삭제 이유:

```text
ego_level_exp + ego_level_bonus 병합 후 EgoLevel.java가 통합 처리
전투 컨트롤러는 EgoLevel 직접 사용
호환 Facade만 남아 있어 제거
```

---

## 4. 삭제하면 안 되는 파일

### Java

```text
EgoWeaponCommand.java
```

이유:

```text
EgoCmd.java가 위임하는 실제 명령 처리 본체
삭제 시 .에고생성/.에고삭제/.에고정보 등 명령어 전체가 깨짐
```

```text
EgoSkill.java
```

이유:

```text
DamageController 연결용 짧은 Facade
EgoCore.attack/defense 내부에서 사용
```

```text
EgoDb.java
```

이유:

```text
DB 접근용 짧은 Facade
외부 코드가 EgoDB/EgoWeaponDatabase에 직접 의존하지 않게 하기 위한 완충층
```

### SQL

```text
ego_cleanup_unused.sql
ego_db_config.sql
ego_merge_schema_euckr.sql
ego_talk_pack_dedupe.sql
```

이유:

```text
ego_update_euckr.sql이 내부에서 순서대로 호출
SOURCE 미지원 DB 툴에서는 직접 실행해야 하는 보조 파일
```

---

## 5. 최종 우선 테이블

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

## 6. fallback 테이블

```text
ego_bond
ego_level_exp
ego_level_bonus
```

fallback 유지 이유:

```text
기존 서버 데이터 이관 및 구버전 DB 즉시 호환
운영 안정화 후 별도 DROP SQL로 제거 가능
```

---

## 7. 버그 방지 보강 완료

```text
[완료] EgoCore.command 인자 타입 불일치 수정
[완료] EgoBond 병합 컬럼 bond/bond_reason 둘 다 확인
[완료] EgoWeaponDatabase 에고 재생성 시 병합/legacy 유대감 초기화
[완료] ego_cleanup_unused.sql 반복 실행 안전화
[완료] ego_db_config.sql ego_level 중심으로 재정리
[완료] ego_merge_schema_euckr.sql 반복 실행 안전화
[완료] EgoLevelBonus 삭제 후 참조 제거
[완료] 무기변형/type2 변형/EgoCombat 흔적 제거 확인
```

---

## 8. 최종 확인 SQL

```sql
SHOW TABLES LIKE 'ego%';
SELECT item_id, ego_name, ego_type, ego_lv, ego_exp, need_exp, bond, bond_reason FROM ego;
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_weapon_rule ORDER BY type2;
SELECT * FROM ego_skill_base ORDER BY skill;
SELECT * FROM ego_talk_pack ORDER BY genre, tone, id;
```

---

## 9. 최종 확인 명령

```text
.에고리로드
.에고도움
.에고생성 카르마
.에고정보
카르마 상태
카르마 장르목록
카르마 대화 추천
.에고삭제 확인
```

---

## 10. 아직 서버별 컴파일 로그가 필요한 부분

아래는 서버 코어마다 시그니처가 다를 수 있습니다.

```text
DatabaseConnection.close(...) 오버로드
S_ObjectEffect.clone(...) 시그니처
ShockStun.clone(...) 시그니처
DamageController.toDamage(...) 시그니처
PcInstance.getInsideList() 반환 타입
ItemInstance.getObjectId() / getEnLevel() 이름
```

컴파일 오류가 나면 위 항목부터 확인합니다.
