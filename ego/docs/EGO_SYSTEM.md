# EGO System Manual

## 1. 목표

EGO 시스템은 기존 무기 구조를 변형하지 않고, 장착 무기에 성장형 대화/전투 보조/로그 기능을 추가하는 Java 8 기반 모듈입니다.

핵심 원칙은 다음과 같습니다.

```text
item.type2 변경 없음
아이템 템플릿 변경 없음
인벤토리/바닥 이미지 변경 없음
DamageController 기본 공식 유지
외부 연결 진입점은 EgoCore 중심
SQL은 ego/sql/ego_schema.sql 1개로 통합
문서는 ego/docs/EGO_SYSTEM.md 1개로 통합
원클릭 전체삭제/전체초기화 제공 없음
```

## 2. 파일 구조

```text
ego/
  java/      Java 보강 코드
  sql/       ego_schema.sql 단일 통합 SQL
  docs/      EGO_SYSTEM.md 단일 통합 문서
  html/      egoletter.htm 안내창 템플릿
  install/   운영체제별 설치 보조 스크립트
  README.md  빠른 적용 가이드
```

## 3. SQL 적용

신규/기존 서버 모두 아래 1개만 적용합니다.

```sql
SOURCE ego/sql/ego_schema.sql;
```

DB 툴이 `SOURCE`를 지원하지 않으면 `ego/sql/ego_schema.sql` 내용을 전체 복사해서 실행합니다.

SQL은 반복 실행을 고려해 `CREATE TABLE IF NOT EXISTS`, `ON DUPLICATE KEY UPDATE`, `INFORMATION_SCHEMA` 기반 컬럼 보강을 사용합니다.

## 4. 주요 DB 테이블

```text
ego                 에고 기본 정보, 레벨, 경험치, 유대감
ego_skill           무기별 에고 능력
ego_skill_base      능력별 발동률/레벨/쿨타임/이펙트
ego_log             발동/레벨업/반격 로그
ego_talk_pack       장르/말투 기반 DB 대사팩
ego_config          공통 설정값
ego_level           레벨별 경험치와 전투 보너스 통합
ego_weapon_rule     무기 type2별 기본 능력/허용 능력
```

구버전 호환을 위해 `ego_bond`, `ego_level_exp`, `ego_level_bonus`는 fallback 테이블로 생성됩니다. Java는 `ego_level`, `ego.bond`를 우선 사용합니다.

## 5. Java 연결

기존 서버에는 가능하면 `EgoCore`만 연결합니다.

### 서버 시작

```java
EgoCore.init(con);
```

### 리로드

```java
EgoCore.reload(con);
```

### 명령어

```java
if (EgoCore.command(o, key, st)) {
    return true;
}
```

`void` 메서드라면:

```java
if (EgoCore.command(o, key, st)) {
    return;
}
```

### 일반 채팅

주변 채팅 방송 전에 호출합니다.

```java
if (EgoCore.chat(o, msg)) {
    return;
}
```

### 주기 처리

```java
EgoCore.tick(pc);
```

### 공격 보정

`DamageController.getDamage(...)` 최종 반환 직전에 연결합니다.

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoCore.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

### 피격 보정

HP 감소 직전 연결합니다.

```java
if (o instanceof Character) {
    dmg = EgoCore.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}
```

## 6. 명령어

```text
.에고도움
.에고생성 이름
.에고삭제 확인
.에고정보
.에고이름 이름
.에고말투 예의
.에고말투 예의반대
.에고능력 코드
.에고상대
.에고주변
.에고리로드
```

`.에고삭제 확인`은 현재 착용 중인 해당 에고무기 1개만 삭제합니다. 전체 DB 삭제, 전체 테이블 삭제, 전체 초기화 명령은 제공하지 않습니다.

## 7. 대화 예시

```text
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
카르마 상대
카르마 주변캐릭
카르마 말투 예의
카르마 말투 예의반대
카르마 드라마 대사 해줘
카르마 영화 한마디
카르마 무협 대사
카르마 아무 대사나 해줘
```

## 8. 성장 규칙

```text
생성 레벨: 0
최대 레벨: 10
Lv.0: 전투능력 없음
Lv.1: 기본 능력 발동 시작
Lv.5: 피격 반격 시작
Lv.6: 자동반격 시작
Lv.10: 스턴 연동
```

기본 경험치:

```text
공격 중 3초마다 +1
몬스터 처치 +5
보스 처치 추가 +50
```

수치는 `ego_config`와 `ego_level`에서 조정 후 `.에고리로드`로 반영합니다.

## 9. 능력 코드

```text
EGO_BALANCE
BLOOD_DRAIN
MANA_DRAIN
CRITICAL_BURST
GUARDIAN_SHIELD
AREA_SLASH
EXECUTION
FLAME_BRAND
FROST_BIND
EGO_COUNTER
EGO_REVENGE
```

무기별 허용 능력은 `ego_weapon_rule.allowed_abilities`에서 관리합니다.

## 10. 운영 점검

DB 점검:

```sql
SHOW TABLES LIKE 'ego%';
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_weapon_rule ORDER BY type2;
SELECT item_id, ego_name, ego_lv, ego_exp, need_exp, bond, bond_reason FROM ego;
```

Java 점검:

```java
boolean ok = EgoCore.schemaOk(con);
String report = EgoCore.schemaReport(con);
```

컴파일:

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

## 11. 삭제 정책

원클릭 전체삭제/전체초기화 기능은 제공하지 않습니다.

허용되는 삭제는 아래 1가지입니다.

```text
.에고삭제 확인
```

이 명령은 착용 중인 해당 에고무기 1개에 대해 `ego`, `ego_skill`, `ego_log`, `ego_bond` 관련 정보만 정리합니다.

## 12. 최종 체크리스트

```text
[OK] SQL 1개 통합: ego/sql/ego_schema.sql
[OK] Docs 1개 통합: ego/docs/EGO_SYSTEM.md
[OK] README 빠른 적용 중심 재작성
[OK] 원클릭 전체삭제 제공 제거
[OK] Java 외부 연결 EgoCore 중심
[OK] DB 설정/레벨/무기규칙 통합
[OK] 구버전 fallback 유지
[OK] UTF-8 파일 기준
```
