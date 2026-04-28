# EGO Weapon System

Java 8 / UTF-8 기준 에고무기 시스템입니다.

## 최종 구조

```text
ego/sql/ego_schema.sql      SQL 통합 1개
ego/docs/사용방법.md         사용/적용 문서
ego/docs/업데이트내용.md     변경 이력 문서
ego/java/                   Java 보강 코드
ego/html/egoletter.htm      장문 안내창 템플릿
ego/README.md               빠른 안내
```

## 핵심 원칙

```text
기존 무기 type2 변경 없음
아이템 템플릿 변경 없음
인벤/바닥 이미지 변경 없음
DamageController 기본 공식 유지
외부 연결은 EgoCore 중심
SQL은 ego_schema.sql 1개만 사용
문서는 사용방법.md, 업데이트내용.md 2개만 유지
원클릭 관련 파일/기능/안내 없음
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

```java
EgoCore.init(con);
EgoCore.reload(con);
```

명령어:

```java
if (EgoCore.command(o, key, st)) {
    return true;
}
```

일반 채팅:

```java
if (EgoCore.chat(o, msg)) {
    return;
}
```

상태 루프:

```java
EgoCore.tick(pc);
```

공격 보정:

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoCore.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

피격 보정:

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

## 문서

```text
ego/docs/사용방법.md
ego/docs/업데이트내용.md
```

## 컴파일

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```
