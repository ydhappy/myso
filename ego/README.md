# EGO Weapon System

Java 8 / UTF-8 기준 에고무기 배포 패키지입니다.

## 기준

이 패키지는 `ego/` 폴더 자체가 배포 단위입니다. `bitna/빛나` 폴더는 서버 구조와 기존 코드 스타일을 확인하기 위한 참고용이며, 배포 파일을 `bitna/src`에 직접 생성하지 않습니다.

## 실제 구성

```text
ego/sql/ego_schema.sql      SQL 통합 1개
ego/docs/사용방법.md         실제 적용 문서
ego/docs/업데이트내용.md     실제 변경 이력
ego/java/                   Java 배포 코드
ego/html/egoletter.htm      장문 안내창 템플릿
ego/README.md               빠른 안내
```

## 핵심 원칙

```text
ego 패키지 배포 기준
bitna/빛나는 참고용
무기 분류/템플릿 변경 없음
무기 슬롯 아이템이면 에고 가능
낚싯대는 제외
인벤/바닥 이미지 변경 없음
DamageController 기본 공식 유지
외부 전투/대화 연결은 EgoCore 중심
DB/아이템 Hook 연결은 EgoDB 중심
SQL은 ego_schema.sql 1개만 사용
문서는 사용방법.md, 업데이트내용.md 2개만 유지
자동 설치/전체 초기화 관련 파일 없음
점명령 생성/삭제/변경 없음
에고 구슬로만 최초 생성
에고무기 이동 시 주인 자동 재인식
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

`ego/java`의 배포 코드를 서버 소스 구조에 맞게 복사한 뒤 기존 서버에는 가능하면 `EgoCore`와 `EgoDB`만 연결합니다.

서버 시작/리로드:

```java
EgoCore.init(con);
EgoCore.reload(con);
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

몬스터 처치 경험치:

```java
EgoCore.kill(pc, mon);
```

아이템 생성 Hook:

```java
ItemInstance egoItem = EgoDB.newInstance(i, item);
if (egoItem != null)
    return egoItem;
```

풀 객체 변수가 없다면 아래처럼 연결합니다.

```java
ItemInstance egoItem = EgoDB.newInstance(i);
if (egoItem != null)
    return egoItem;
```

## 에고 구슬

```text
아이템명: 에고 구슬
기본 item_code: 900001
Java 클래스: lineage.world.object.item.EgoOrb
```

동작:

```text
에고가 없는 착용 무기: 에고 최초 생성, 능력/대화 랜덤 최초 선택, 구슬 1개 소모
이미 에고무기: 능력/대화/레벨/경험치 변경 없음, 현재 캐릭터를 주인으로 재인식
```

## 점명령 정책

```text
.에고생성 없음
.에고삭제 없음
.에고능력 변경 없음
.에고말투 변경 없음
```

생성은 `에고 구슬` 아이템 사용으로만 처리합니다. 삭제와 변경은 제공하지 않습니다.

## 현재 병합/삭제된 파일

```text
EgoItemDatabaseHook.java -> EgoDB.java에 병합 후 삭제
EgoSkill.java            -> EgoCore.java에 병합 후 삭제
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
