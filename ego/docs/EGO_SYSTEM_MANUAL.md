# 에고무기 현재 기준 매뉴얼

이 문서는 구버전 변형/이미지/한글테이블 구조를 제거한 뒤의 현재 기준만 정리합니다.

## 유지 기능

```text
기존 무기 그대로 사용
type2 변형 없음
인벤/바닥 이미지 변경 없음
에고 이름표 표시
일반채팅 실시간 에고 대화
짧은 답변 말풍선
긴 답변 egoletter 편지창
에고 성장 Lv.0~10
에고 보조능력
ego_log 기록
.에고삭제 확인 완전삭제
```

## 최종 DB 테이블

```text
ego
ego_skill
ego_skill_base
ego_log
```

삭제/미사용 구조:

```text
ego_view
ego_type
ego_talk
에고모양
에고성격
에고대화
character_item_ego
character_item_ego_ability
ego_personality_template
ego_talk_template
ego_ability_template
ego_ability_proc_log
form
prev_shield
ego_form_type
prev_shield_objid
inv_gfx
ground_gfx
```

## 설치 SQL

신규 설치:

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

기존 서버 정리/보정:

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
```

## Java 연결

서버 시작 DB 로드:

```java
EgoDB.init(con);
```

명령어 연결:

```java
if (EgoCmd.run(o, key, st)) {
    return true;
}
```

일반채팅 연결:

```java
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoTalk.chat((PcInstance) o, msg)) {
        return;
    }
}
```

공격 훅:

```java
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoSkill.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

피격 훅:

```java
o.toDamage(cha, dmg, type);

if (o instanceof Character) {
    dmg = EgoSkill.defense((Character) o, cha, dmg);
    if (dmg <= 0)
        return;
}

o.setNowHp(o.getNowHp() - dmg);
```

## 에고 레벨

```text
Lv.0 -> Lv.1  : 100
Lv.1 -> Lv.2  : 250
Lv.2 -> Lv.3  : 500
Lv.3 -> Lv.4  : 900
Lv.4 -> Lv.5  : 1500
Lv.5 -> Lv.6  : 2400
Lv.6 -> Lv.7  : 3600
Lv.7 -> Lv.8  : 5200
Lv.8 -> Lv.9  : 7500
Lv.9 -> Lv.10 : 10000
Lv.10         : 0
```

## 전투 규칙

```text
Lv.0  전투능력 없음
Lv.1  기본 스킬/치명 시작
Lv.5  피격 반격 시작, PC 포함
Lv.6  피격 자동반격 시작, PC 포함
Lv.10 스턴 50% 성공, PC 포함
```

## 명령어

```text
.에고도움
.에고생성 이름
.에고삭제 확인
.에고정보
.에고이름 이름
.에고능력 코드
.에고상대
.에고주변
.에고리로드
```

일반채팅:

```text
에고이름 상태
에고이름 조언
에고이름 선공
에고이름 공격
에고이름 멈춰
에고이름 상대
에고이름 주변캐릭
에고이름 타겟분석
```

## 금지

```text
무기변형 재추가 금지
PcInstance 공격거리/활공격/화살소비 변경 금지
DamageController type2 공식 변경 금지
인벤/바닥 이미지 변경 기능 재추가 금지
구버전 SQL 실행 금지
```

자세한 최신 설명은 `ego/README.md`를 기준으로 합니다.
