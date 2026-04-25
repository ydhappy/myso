# 에고무기 시스템

에고무기 기능을 적용하기 위한 최소 구성 폴더입니다. Java 파일은 전부 `ego/java/` 한 곳에 모았습니다. 적용할 때 쓰는 Java 이름은 짧게 단순화했습니다.

Java 8 / UTF-8 기준입니다.

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

---

## 가장 중요한 적용 원칙

```text
기존 서버 코어 유지
PcInstance 공격 로직 변경 금지
DamageController 무기 타입/데미지 공식 변경 금지
원본 item.type2 변경 금지
원본 아이템 템플릿 변경 금지
EgoCombat 없음, 만들지 않음
에고는 별도 모듈로만 추가
```

에고 형태는 실제 무기 타입 교체가 아닙니다.

```text
활 원본 무기  → 계속 활 공격
검 원본 무기  → 계속 검 공격
에고 활/양검/한검 → 에고 표시형태, 대화형태, 이미지, 보조능력 상태
```

즉, 에고는 기존 전투 코어를 갈아엎지 않고 아래만 추가합니다.

```text
일반채팅 대화
점 명령어
인벤토리 표시
아이템정보 표시
에고 전용 DB 상태
에고 특별능력 보조 발동
에고 경험치/레벨업
상대 감지
```

---

## 생성 조건

에고 생성은 착용 중인 무기에만 가능합니다.

```text
착용 무기 필요
무기 슬롯 아이템 필요
원본 type2 지원 필요
강화 수치 +0 필요
이미 에고가 생성된 무기 재생성 불가
```

강화된 무기는 생성 불가입니다.

```text
+0 무기     → 에고 생성 가능
+1 이상 무기 → 에고 생성 불가
-1 등 특수 강화값 → 에고 생성 불가
```

이유:

```text
강화 무기까지 허용하면 에고 성장/기존 강화/운영 보정이 섞여 추적이 어려워집니다.
에고 시스템은 +0 무기를 성장시키는 별도 성장축으로 사용합니다.
```

---

## 최종 구조

```text
ego/
├─ README.md
├─ docs/
│  └─ EGO_SYSTEM_MANUAL.md
├─ install/
│  ├─ install_ego_windows.bat
│  └─ install_ego_linux.sh
├─ java/
│  ├─ EgoMsg.java       # 메시지, 색상, 개인출력
│  ├─ EgoType.java      # 에고 표시형태/분류
│  ├─ EgoForm.java      # 에고 표시형태 변경
│  ├─ EgoView.java      # 인벤토리/바닥 이미지, 아이템정보 표시
│  ├─ EgoTalk.java      # 일반채팅 대화
│  ├─ EgoSkill.java     # 전투 보조능력
│  ├─ EgoDB.java        # DB 연결
│  ├─ EgoCmd.java       # 점 명령어
│  ├─ EgoScan.java      # 상대감지
│  ├─ EgoMessageUtil.java
│  ├─ EgoWeaponTypeUtil.java
│  ├─ EgoWeaponFormController.java
│  ├─ EgoWeaponControlController.java
│  ├─ EgoWeaponAbilityController.java
│  ├─ EgoWeaponDatabase.java
│  ├─ EgoWeaponCommand.java
│  ├─ EgoOpponentScanController.java
│  ├─ EgoCoreAdapter.java
│  └─ EgoPortableRules.java
└─ sql/
   ├─ ego_install_euckr.sql
   ├─ ego_install_korean.sql
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

---

## Java 명칭 단순화

초보자는 아래 짧은 이름만 기억하면 됩니다.

```text
EgoMsg    메시지/색상/본인전용 출력
EgoType   에고 표시형태/분류 판정
EgoForm   에고 표시형태 변경
EgoView   인벤토리/바닥 이미지, 아이템정보 표시
EgoTalk   일반채팅 대화 처리
EgoSkill  전투 보조능력 발동
EgoDB     DB 로드/저장
EgoCmd    점 명령어 처리
EgoScan   상대 캐릭터 감지
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
ego.form          표시형태
ego.prev_shield   표시형태 변경 때문에 해제한 방패 objectId

ego_skill.item_id
ego_skill.skill
ego_skill.skill_lv
ego_skill.rate_bonus
ego_skill.dmg_bonus

ego_view.form
ego_view.label
ego_view.inv_gfx
ego_view.ground_gfx
ego_view.memo
```

---

## 인벤토리 표시

에고 생성 후 인벤토리 이름 뒤에 색상 표식이 붙습니다.

```text
무기명 \fY[에고]\fW \fS(활 Lv.1 공명)\fW
무기명 \fY[에고]\fW \fS(한손검 Lv.3 흡혈)\fW
```

실제 클라이언트에서는 색상코드가 적용되어 `[에고]`가 강조됩니다.

아이템정보에는 에고 상태가 표시됩니다.

```text
에고형태: 활 / 레벨: 3 / 경험치: 15/300 / 능력: 공명
```

원본 아이템 템플릿은 절대 직접 수정하지 않습니다.

```text
원본 item.InvGfx      변경 안 함
원본 item.GroundGfx   변경 안 함
원본 item.Type2       변경 안 함
원본 item.NameId      변경 안 함
```

---

## 에고 표시형태

에고 표시형태는 DB와 인벤 표시, 대화 반응, 보조능력 분류에만 사용합니다.

```text
카르마 활      → 에고 표시형태 활
카르마 양검    → 에고 표시형태 양손검
카르마 한검    → 에고 표시형태 한손검
카르마 단검    → 에고 표시형태 단검
```

실제 공격 방식은 원본 무기 기준입니다.

```text
원본 활이면 계속 활 공격
원본 검이면 계속 검 공격
원본 창이면 계속 창 공격
```

---

## 에고 경험치/레벨업

에고 경험치와 레벨은 `ego` 테이블에 저장됩니다.

```text
ego.ego_lv   현재 에고 레벨
ego.ego_exp  현재 누적 경험치
ego.need_exp 다음 레벨 필요 경험치
```

기본값:

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
현재 경험치 >= 필요 경험치가 되면 레벨 +1
남은 경험치는 다음 레벨 경험치로 이월
다음 필요 경험치 = 기존 필요 경험치 + 현재 레벨 * 100
최대 레벨 30에서 더 이상 레벨업하지 않음
```

예시:

```text
Lv.1 필요 100
Lv.2 필요 300
Lv.3 필요 600
Lv.4 필요 1000
```

레벨업 시:

```text
개인 메시지: [에고] 의식이 성장했습니다. Lv.N
이펙트 출력 시도
인벤토리 이름/아이템정보 즉시 갱신
```

주의:

```text
경험치/레벨은 무기 강화수치와 별개입니다.
무기 강화수치가 오르는 기능이 아닙니다.
에고 레벨만 올라갑니다.
```

---

## 원클릭 설치

Windows:

```text
ego/install/install_ego_windows.bat
```

Linux/macOS:

```bash
chmod +x ego/install/install_ego_linux.sh
./ego/install/install_ego_linux.sh
```

EUC-KR 서버 권장 SQL:

```text
ego/sql/ego_install_euckr.sql
```

설치 후 서버가 DB 캐시를 쓰면 아래 중 하나를 실행합니다.

```text
.에고리로드
또는 서버 재시작
```

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
9. +0 무기 착용
10. .에고생성 카르마
11. 인벤토리 [에고] 색상 표식 확인
12. .에고정보 확인
13. 카르마 상태 / 카르마 활 / 카르마 양검 / 카르마 한검 테스트
14. 실제 공격 타입은 원본 무기 기준인지 확인
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

금지:

```text
PcInstance의 bow 값 변경 금지
PcInstance의 공격 사거리 변경 금지
PcInstance의 화살 소비 로직 변경 금지
DamageController의 weapon.getItem().getType2() 교체 금지
EgoCombat 같은 전투 타입 우회 클래스 추가 금지
```

---

## 게임 명령어

점 명령어는 최소화하고, 실제 조작은 일반채팅 중심입니다.

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

일반 채팅 예시:

```text
카르마 상태
카르마 조언
카르마 선공
카르마 공격
카르마 멈춰
카르마 상대
카르마 주변캐릭
카르마 타겟분석
카르마 활
카르마 양검
카르마 양손검
카르마 한검
카르마 한손검
카르마 단검
카르마 창
카르마 도끼
카르마 지팡이
카르마 완드
```

---

## 운영 SQL 예시

형태별 이미지 변경:

```sql
UPDATE ego_view
SET inv_gfx = 1001,
    ground_gfx = 1002,
    memo = '에고 활 전용 이미지'
WHERE form = 'bow';
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

수정 후:

```text
.에고리로드
또는 서버 재시작
```

---

## 주의

```text
- SQL 설치만으로 게임 기능이 자동 발동되는 것은 아닙니다.
- 최초 1회는 서버코어에 Java 연결이 필요합니다.
- Java 8 / UTF-8 기준으로 컴파일하세요.
- 에고는 기존 서버 전투 코어를 변경하지 않습니다.
- 인벤이미지/바닥이미지 값이 0이면 원본 아이템 이미지를 사용합니다.
- 강화된 무기에는 에고를 생성할 수 없습니다.
```
