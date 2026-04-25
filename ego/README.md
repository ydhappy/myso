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
상대 감지
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
ego.item_id
ego.char_id
ego.use_yn
ego.ego_name
ego.ego_type
ego.ego_lv
ego.ego_exp
ego.need_exp
ego.form
ego.prev_shield

ego_view.form
ego_view.label
ego_view.inv_gfx
ego_view.ground_gfx
ego_view.memo
```

---

## 인벤토리/바닥 이미지/아이템정보 처리

원본 아이템 템플릿은 절대 직접 수정하지 않습니다.

```text
원본 item.InvGfx      변경 안 함
원본 item.GroundGfx   변경 안 함
원본 item.Type2       변경 안 함
원본 item.NameId      변경 안 함
```

대신 `ego_view` 테이블에서 형태별 표시값을 관리합니다.

```text
ego_view.inv_gfx     = 인벤토리 아이콘 gfx
ego_view.ground_gfx  = 바닥에 떨어졌을 때 gfx
ego_view.label       = 표시 형태명
ego_view.memo        = 아이템정보 보조 설명
```

값이 `0`이면 원본 이미지를 그대로 사용합니다.

```text
inv_gfx = 0     → 원본 InvGfx 사용
ground_gfx = 0  → 원본 GroundGfx 사용
```

아이템정보 표시는 기존 옵션 구조를 크게 건드리지 않고 이름 뒤에 에고 정보를 붙입니다.

```text
+9 무기명 [에고:활 Lv.3 공명]
+9 무기명 [에고:양손검 Lv.5 치명]
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

방패 처리도 기존 장착 시스템을 호출하는 보조 기능일 뿐, 공격 타입을 바꾸지 않습니다.

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
9. .에고생성 카르마
10. 카르마 상태 / 카르마 활 / 카르마 양검 / 카르마 한검 테스트
11. 인벤토리 아이콘/이름 변경 확인
12. 실제 공격 타입은 원본 무기 기준인지 확인
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
if (cha instanceof PcInstance && weapon != null) {
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

## Java 수정 없이 운영 생성/편집

형태별 이미지 변경 예시:

```sql
UPDATE ego_view
SET inv_gfx = 1001,
    ground_gfx = 1002,
    memo = '에고 활 전용 이미지'
WHERE form = 'bow';
```

수정 후:

```text
.에고리로드
또는 서버 재시작
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

## 주의

```text
- SQL 설치만으로 게임 기능이 자동 발동되는 것은 아닙니다.
- 최초 1회는 서버코어에 Java 연결이 필요합니다.
- Java 8 / UTF-8 기준으로 컴파일하세요.
- 에고는 기존 서버 전투 코어를 변경하지 않습니다.
- 인벤이미지/바닥이미지 값이 0이면 원본 아이템 이미지를 사용합니다.
```
