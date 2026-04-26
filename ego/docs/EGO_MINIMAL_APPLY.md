# 에고 최소 적용 가이드

목표:

```text
DB 실행 파일 최소화
Java 기존 서버 연결 지점 최소화
초보자도 순서대로 적용 가능
테이블/컬럼/메서드 연결성 추적 가능
```

---

## 1. DB 최소 실행 파일

### 신규 서버

신규 서버는 아래 1개만 실행합니다.

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

포함 테이블:

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

기존 서버는 아래 1개만 실행합니다.

```sql
SOURCE ego/sql/ego_update_euckr.sql;
```

내부적으로 아래 파일을 순서대로 실행합니다.

```text
ego_cleanup_unused.sql
ego_db_config.sql
ego_talk_pack_dedupe.sql
```

주의:

```text
사용 중인 DB 툴에서 SOURCE 문이 동작하지 않으면 위 3개 파일을 순서대로 직접 실행하세요.
```

---

## 2. Java 최소 연결 파일

기존 서버 코드에 직접 연결할 클래스는 가능하면 1개입니다.

```text
EgoCore.java
```

내부 구현 파일은 많지만 기존 서버에는 `EgoCore`만 연결하면 됩니다.

보조 Facade:

```text
EgoDb.java      DB 접근 짧은 이름 Facade
EgoSchema.java  테이블/컬럼 연결성 검증기
```

---

## 3. 서버 시작 연결

기존:

```java
EgoDB.init(con);
```

최소화 후:

```java
EgoCore.init(con);
```

리로드:

```java
EgoCore.reload(con);
```

`EgoCore.init/reload`는 내부에서 `EgoSchema.silentCheck(con)`를 먼저 호출하고, 이후 DB 캐시를 로드합니다.

---

## 4. CommandController 연결

기존 명령 처리부에 추가합니다.

```java
if (EgoCore.command(o, key, st)) {
    return true;
}
```

서버의 CommandController가 `return void` 구조라면:

```java
if (EgoCore.command(o, key, st)) {
    return;
}
```

---

## 5. ChattingController 연결

일반 채팅이 주변에 방송되기 전에 추가합니다.

```java
if (EgoCore.chat(o, msg)) {
    return;
}
```

효과:

```text
에고 이름으로 시작하는 일반 채팅은 본인에게만 처리
다른 캐릭터에게 보이지 않음
장르대화/자연대화/명령대화 처리
```

---

## 6. 자동 상황 대사 연결

캐릭터 상태 루프, AI 루프, 또는 주기적으로 호출되는 곳에 추가합니다.

```java
EgoCore.tick(pc);
```

효과:

```text
HP 부족 자동 경고
MP 부족 자동 안내
보스 감지 자동 경고
안정 상태 자동 대사
```

---

## 7. DamageController 공격 연결

`DamageController.getDamage(...)` 최종 return 직전에 추가합니다.

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoCore.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

주의:

```text
기존 DamageController 공식은 바꾸지 않습니다.
최종 dmg만 에고 보조능력으로 보정합니다.
```

---

## 8. DamageController 피격 연결

`DamageController.toDamage(...)`에서 HP 감소 직전 추가합니다.

```java
// 데미지 입었다는거 알리기.
o.toDamage(cha, dmg, type);

// 에고 피격 보정
if (o instanceof Character) {
    dmg = EgoCore.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}

// hp 처리
o.setNowHp(o.getNowHp() - dmg);
```

주의:

```text
기존 피격 처리 흐름은 유지합니다.
HP 감소 직전에만 에고 반격/방어 보정을 넣습니다.
```

---

## 9. Java 파일 그룹

### 외부 연결 그룹

```text
EgoCore.java
```

### 연결성/Facade 그룹

```text
EgoDb.java
EgoSchema.java
```

### DB/설정 그룹

```text
EgoDB.java
EgoConfig.java
EgoLevelBonus.java
EgoWeaponRule.java
EgoTalkPack.java
EgoBond.java
```

### 전투 그룹

```text
EgoSkill.java
EgoWeaponAbilityController.java
EgoWeaponTypeUtil.java
```

### 대화 그룹

```text
EgoTalk.java
EgoGenreTalk.java
EgoGenreGuide.java
EgoWeaponControlController.java
EgoAutoTalk.java
EgoTalkHistory.java
EgoMessageUtil.java
```

### 표시/명령 그룹

```text
EgoCmd.java
EgoView.java
```

---

## 10. SQL 파일 그룹

### 실제 실행 최소 파일

```text
신규 서버: ego_install_euckr.sql
기존 서버: ego_update_euckr.sql
```

### 내부 보조 SQL

```text
ego_cleanup_unused.sql
ego_db_config.sql
ego_talk_pack_dedupe.sql
```

---

## 11. 연결성 점검

Java에서 직접 확인:

```java
boolean ok = EgoCore.schemaOk(con);
String report = EgoCore.schemaReport(con);
```

연결성 상세 문서:

```text
ego/docs/EGO_CONNECTIVITY_MAP.md
```

DB 확인:

```sql
SHOW TABLES LIKE 'ego%';
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_level_exp ORDER BY ego_lv;
SELECT * FROM ego_level_bonus ORDER BY ego_lv;
SELECT * FROM ego_weapon_rule ORDER BY type2;
```

---

## 12. 적용 후 확인

게임 안에서:

```text
.에고리로드
.에고도움
.에고생성 카르마
카르마 상태
카르마 장르목록
```

---

## 13. 현재 최소화 결론

```text
DB 실행 파일: 신규 1개 / 기존 1개
기존 Java 연결 클래스: EgoCore 1개
DB 접근 Facade: EgoDb 1개
DB-자바 연결성 검증: EgoSchema 1개
실제 구현 Java 파일: 기능별 public class 제약 때문에 보존
문서 기준 적용 난이도: 기존보다 낮음
```
