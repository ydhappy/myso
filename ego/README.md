# 에고무기 시스템

에고무기 기능을 적용하기 위한 최소 구성 폴더입니다. Java 파일은 전부 `ego/java/` 한 곳에 모았습니다. Java 8 / UTF-8 기준입니다.

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

---

## 최종 적용 원칙

```text
기존 서버 코어 유지
PcInstance 공격 로직 변경 금지
DamageController 무기 타입/데미지 공식 변경 금지
원본 item.type2 변경 금지
원본 아이템 템플릿 변경 금지
EgoCombat 없음, 만들지 않음
무기변형 기능 완전 제거
에고는 별도 모듈로만 추가
```

에고는 실제 무기 타입을 바꾸지 않습니다.

```text
원본 활  → 계속 활 공격
원본 검  → 계속 검 공격
원본 창  → 계속 창 공격
에고     → 대화, 표시, 경험치, 보조능력, 로그만 추가
```

---

## 생성 조건

에고 생성은 착용 중인 무기에 가능합니다.

```text
착용 무기 필요
무기 슬롯 아이템 필요
원본 type2 지원 필요
이미 에고가 생성된 무기 재생성 불가
강화된 무기도 에고 생성 가능
```

---

## DB 기준

EUC-KR 서버는 영문 단순 테이블을 권장합니다.

```text
ego
ego_skill
ego_view
ego_type
ego_talk
ego_skill_base
ego_log
```

핵심 컬럼:

```text
ego.item_id       에고가 생성된 아이템 objectId
ego.char_id       소유 캐릭터 objectId
ego.use_yn        사용 여부
ego.ego_name      호출 이름
ego.ego_type      성격
ego.ego_lv        에고 레벨
ego.ego_exp       현재 경험치
ego.need_exp      다음 레벨 필요 경험치

ego.form          미사용 호환 컬럼, 무기변형 기능 제거로 사용하지 않음
ego.prev_shield   미사용 호환 컬럼, 무기변형 기능 제거로 사용하지 않음

ego_skill.skill       능력 코드
ego_skill.skill_lv    능력 레벨 보정
ego_skill.rate_bonus  발동 확률 보정
ego_skill.dmg_bonus   피해 보정
ego_skill.last_proc   마지막 발동 시간

ego_skill_base.base_rate  기본 발동률
ego_skill_base.lv_rate    레벨당 발동률
ego_skill_base.max_rate   최대 발동률
ego_skill_base.min_lv     최소 에고 실질 레벨
ego_skill_base.cool_ms    능력별 쿨타임
ego_skill_base.effect     이펙트 번호

ego_log                  능력 발동/레벨업 기록
```

---

## 인벤토리 표시

에고 생성 후 인벤토리 이름 뒤에 색상 표식이 붙습니다.

```text
무기명 \fY[에고]\fW \fS(원본무기타입 Lv.1 공명)\fW
```

아이템정보에는 에고 상태가 표시됩니다.

```text
에고: 활 / 레벨: 3 / 경험치: 15/300 / 능력: 공명
```

`활/검/창` 표시는 원본 item.type2 기준입니다. 무기변형이 아닙니다.

---

## 에고 경험치/레벨업

에고 경험치와 레벨은 `ego` 테이블에 저장됩니다.

```text
생성 시 레벨: 1
생성 시 경험치: 0
생성 시 필요 경험치: 100
최대 레벨: 30
```

경험치 획득:

```text
전투 중 에고 보조능력이 연결된 공격 → 3초마다 +1 경험치
몬스터 처치 hook을 addKillExp에 연결한 경우 → 일반 몬스터 +5 경험치
보스 몬스터 처치 hook을 addKillExp에 연결한 경우 → +55 경험치
```

레벨업 공식:

```text
현재 경험치 >= 필요 경험치 → 레벨 +1
남은 경험치는 다음 레벨 경험치로 이월
다음 필요 경험치 = 기존 필요 경험치 + 현재 레벨 * 100
```

---

## 에고 스킬 계산

`ego_skill_base`가 실제 전투 계산에 연결됩니다.

```text
base_rate  → 기본 발동률
lv_rate    → 에고/능력 실질 레벨당 추가 발동률
max_rate   → 최대 발동률
min_lv     → 발동 최소 실질 레벨
cool_ms    → 능력별 쿨타임
effect     → S_ObjectEffect 이펙트 번호
```

서버의 기존 스킬 DB 기준:

```text
skill.이팩트          → 일반 스킬 이펙트 컬럼
monster_skill.effect  → 몬스터 스킬 이펙트 컬럼
ego_skill_base.effect → 에고 보조능력 이펙트 컬럼
```

즉 `ego_skill_base.effect`에는 기존 skill/monster_skill에서 확인한 사용 가능한 이펙트 번호를 넣으면 됩니다.

---

## ego_log

에고 능력 발동 성공 시 `ego_log`에 기록됩니다.

```text
item_id
char_id
char_name
target_name
skill
base_dmg
final_dmg
add_dmg
reg_date
```

레벨업도 `skill = LEVEL_UP`으로 기록됩니다.

---

## myso 빠른 적용 순서

```text
1. DB 백업
2. ego/sql/ego_install_euckr.sql 직접 실행 또는 설치 스크립트 실행
3. ego/java 파일 복사
4. ChattingController 연결: EgoTalk.chat(...)
5. CommandController 연결: EgoCmd.run(...)
6. DamageController 연결: EgoSkill.attack(...) 단 1회만 추가
7. DB 시작 연결: EgoDB.init(con)
8. 서버 빌드
9. 무기 착용
10. .에고생성 카르마
11. 인벤토리 [에고] 색상 표식 확인
12. .에고정보 확인
13. 카르마 상태 / 카르마 조언 / 카르마 선공 테스트
14. ego_log 기록 확인
```

연결 예시:

```java
// ChattingController: 일반채팅 방송 직전
if (mode == Lineage.CHATTING_MODE_NORMAL
        && o instanceof PcInstance
        && !(o instanceof RobotInstance)) {
    if (EgoTalk.chat((PcInstance) o, msg)) {
        return;
    }
}
```

```java
// CommandController: 기존 명령어 처리 전
if (EgoCmd.run(o, key, st)) {
    return true;
}
```

```java
// DamageController: 최종 return 직전, 기존 dmg 계산 이후 보조능력만 추가
if (cha instanceof PcInstance && weapon != null && dmg > 0) {
    dmg = EgoSkill.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

```java
// 서버 시작 DB 로드
EgoDB.init(con);
```

---

## 게임 명령어

```text
.에고도움
.에고생성 이름
.에고정보
.에고이름 이름
.에고능력 코드
.에고상대
.에고주변
.에고리로드
```

일반 채팅:

```text
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
카르마 상대
카르마 주변캐릭
카르마 타겟분석
```

제거된 명령:

```text
카르마 활
카르마 양검
카르마 한검
카르마 단검
카르마 창
카르마 도끼
카르마 지팡이
카르마 완드
```

---

## 금지 작업

```text
PcInstance의 bow 값 변경 금지
PcInstance의 공격 사거리 변경 금지
PcInstance의 화살 소비 로직 변경 금지
DamageController의 weapon.getItem().getType2() 교체 금지
EgoCombat 같은 전투 타입 우회 클래스 추가 금지
무기변형 기능 재추가 금지
```

---

## 운영 SQL 예시

능력 이펙트/확률 변경:

```sql
UPDATE ego_skill_base
SET base_rate = 3,
    lv_rate = 1,
    max_rate = 25,
    cool_ms = 3000,
    effect = 8150
WHERE skill = 'BLOOD_DRAIN';
```

에고 경험치 보정:

```sql
UPDATE ego
SET ego_lv = 5,
    ego_exp = 0,
    need_exp = 1000
WHERE item_id = 123456789
  AND use_yn = 1;
```

로그 확인:

```sql
SELECT *
FROM ego_log
ORDER BY reg_date DESC
LIMIT 50;
```

수정 후:

```text
.에고리로드
또는 서버 재시작
```
