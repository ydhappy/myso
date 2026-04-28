# EGO Weapon System

Java 8 / UTF-8 기준 에고무기 시스템입니다.

## 최종 구조

```text
ego/sql/ego_schema.sql      SQL 통합 1개
ego/docs/EGO_SYSTEM.md      문서 통합 1개
ego/java/                   Java 보강 코드
ego/html/egoletter.htm      장문 안내창 템플릿
ego/install/                설치 보조 스크립트
```

원클릭 전체삭제/전체초기화 파일은 제공하지 않습니다. 삭제는 게임 내 `.에고삭제 확인`으로 착용 중인 해당 에고무기 1개만 처리합니다.

## 핵심 원칙

```text
기존 무기 type2 변경 없음
아이템 템플릿 변경 없음
인벤/바닥 이미지 변경 없음
DamageController 기본 공식 유지
외부 연결은 EgoCore 중심
SQL은 ego_schema.sql 1개만 사용
문서는 EGO_SYSTEM.md 1개만 사용
```

## SQL 적용

신규/기존 서버 모두 아래 1개만 실행합니다.

```sql
SOURCE ego/sql/ego_schema.sql;
```

DB 툴이 `SOURCE`를 지원하지 않으면 `ego/sql/ego_schema.sql` 내용을 전체 복사해서 실행합니다.

적용 후 서버에서:

```text
.에고리로드
```

## Java 연결

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

`void` 구조라면:

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

### 상태 루프

```java
EgoCore.tick(pc);
```

### 공격 보정

`DamageController.getDamage(...)` 최종 return 직전에 연결합니다.

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoCore.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

### 피격 보정

HP 감소 직전에 연결합니다.

```java
if (o instanceof Character) {
    dmg = EgoCore.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}
```

## 명령어

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

## 성장/전투 규칙

```text
생성 레벨: 0
최대 레벨: 10
Lv.0: 전투능력 없음
Lv.1: 기본 능력 발동 시작
Lv.5: 피격 반격 시작
Lv.6: 자동반격 시작
Lv.10: 스턴 연동
```

수치는 DB에서 조정합니다.

```text
ego_config       공통 설정
ego_level        레벨별 경험치/전투 보너스
ego_skill_base   능력별 발동률/쿨타임/이펙트
ego_weapon_rule  무기별 허용 능력
```

변경 후 `.에고리로드`를 실행합니다.

## 자세한 문서

```text
ego/docs/EGO_SYSTEM.md
```

## 컴파일

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

## 최종 체크

```text
[OK] SQL 1개 통합
[OK] Docs 1개 통합
[OK] README 빠른 적용 중심 재작성
[OK] 원클릭 전체삭제 제거
[OK] Java 연결 EgoCore 중심
[OK] UTF-8 기준
```
