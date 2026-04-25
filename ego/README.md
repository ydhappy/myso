# 에고무기 시스템

에고무기 기능을 적용하기 위한 최소 구성 폴더입니다. Java 파일은 전부 `ego/java/` 한 곳에 모았습니다. **적용할 때 쓰는 Java 이름은 짧게 단순화했습니다.** DB 테이블과 컬럼은 한글명입니다.

Java 8 / UTF-8 기준입니다.

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
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
│  ├─ EgoType.java      # 형태/무기종류
│  ├─ EgoForm.java      # 형태변신
│  ├─ EgoTalk.java      # 일반채팅 대화
│  ├─ EgoSkill.java     # 전투 능력
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
   ├─ ego_install_korean.sql
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

---

## Java 명칭 단순화

초보자는 아래 짧은 이름만 기억하면 됩니다.

```text
EgoMsg    메시지/색상/본인전용 출력
EgoType   에고 형태/무기종류 판정
EgoForm   에고무기 자체 형태변신
EgoTalk   일반채팅 대화 처리
EgoSkill  전투 능력 발동
EgoDB     한글 DB 로드/저장
EgoCmd    점 명령어 처리
EgoScan   상대 캐릭터 감지
```

기존 긴 클래스는 내부 구현용으로 유지합니다.

```text
EgoWeaponControlController → 적용 코드에서는 EgoTalk 사용
EgoWeaponDatabase          → 적용 코드에서는 EgoDB 사용
EgoWeaponAbilityController → 적용 코드에서는 EgoSkill 사용
EgoWeaponTypeUtil          → 적용 코드에서는 EgoType 사용
EgoWeaponFormController    → 적용 코드에서는 EgoForm 사용
EgoWeaponCommand           → 적용 코드에서는 EgoCmd 사용
EgoOpponentScanController  → 적용 코드에서는 EgoScan 사용
EgoMessageUtil             → 적용 코드에서는 EgoMsg 사용
```

---

## DB 명칭 단순화

메인 DB는 한글 테이블/컬럼을 사용합니다.

```text
테이블:
에고
에고능력
에고성격
에고대화
에고능력기본
에고기록
```

핵심 컬럼:

```text
에고.아이템번호
에고.캐릭터번호
에고.사용
에고.이름
에고.성격
에고.레벨
에고.경험치
에고.필요경험치
에고.형태
에고.이전방패
```

---

## 핵심 구현 방향

```text
무기 교체가 아닙니다.
에고무기 자체가 활/양손검/한손검/단검/창/도끼/지팡이/완드 형태로 변신합니다.
```

원본 아이템 템플릿의 type2는 직접 바꾸지 않습니다.
대신 DB의 `에고.형태`에 현재 에고 형태를 저장하고, 에고 로직에서는 이 값을 현재 무기종류처럼 인식합니다.

```text
카르마 활      → 방패 자동 해제, 에고 활 형태
카르마 양검    → 방패 자동 해제, 에고 양손검 형태
카르마 한검    → 에고 한손검 형태, 직전 방패 자동 복구 시도
카르마 단검    → 에고 단검 형태, 직전 방패 자동 복구 시도
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

설치 스크립트는 아래 SQL을 실행합니다.

```text
ego/sql/ego_install_korean.sql
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
2. install/install_ego_windows.bat 또는 install/install_ego_linux.sh 실행
3. ego/java 파일 복사
4. ChattingController 연결: EgoTalk.chat(...)
5. CommandController 연결: EgoCmd.run(...)
6. DamageController 연결: EgoSkill.attack(...)
7. DB 시작 연결: EgoDB.init(con)
8. 서버 빌드
9. .에고생성 카르마
10. 카르마 상태 / 카르마 활 / 카르마 양검 / 카르마 한검 테스트
11. 카르마 상대 / .에고정보 테스트
```

연결 예시:

```java
// ChattingController
if (o instanceof PcInstance && !(o instanceof RobotInstance)) {
    if (EgoTalk.chat((PcInstance) o, msg)) {
        return;
    }
}
```

```java
// CommandController
if (EgoCmd.run(o, key, st)) {
    return true;
}
```

```java
// DamageController
if (cha instanceof PcInstance && weapon != null) {
    dmg = EgoSkill.attack((Character) cha, target, weapon, (int) Math.round(dmg));
}
```

```java
// 서버 시작 DB 로드
EgoDB.init(con);
```

---

## 기능 요약

```text
- 일반채팅으로 에고 호출
- 호출 채팅 주변 방송 차단
- 에고 응답 본인 전용 메시지 출력
- 에고무기 자체 형태변환
- 활/양손검 형태 시 방패 자동 해제
- 한손검/단검/완드 형태 시 직전 방패 자동 복구 시도
- 캐릭터 HP/MP/타겟 상태 인식
- 선공 몬스터 감지
- 상대 캐릭터 위험도 분석
- 간단 자동공격 제어
- 특별 능력 발동
- 한글 DB로 이름/레벨/경험치/능력/형태 저장
```

---

## Java 수정 없이 운영 생성/편집

운영 중 생성/편집은 아래 한글 관리 SQL로 처리합니다.

```text
sql/ego_no_java_admin.sql
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
- 한글 테이블/컬럼명을 사용하므로 DB와 Java 파일 모두 UTF-8로 관리하세요.
- 운영 전 EgoWeaponAbilityController의 ENABLE_TEST_MODE 확인이 필요합니다.
- ChattingController 연결 위치가 잘못되면 호출 채팅이 주변에 보일 수 있습니다.
- 에고 형태변환은 원본 아이템 DB type2를 바꾸지 않습니다.
```
