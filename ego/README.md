# 에고무기 시스템

에고무기 기능을 적용하기 위한 최소 구성 폴더입니다. **Java 파일은 전부 `ego/java/` 한 곳에 모았습니다.** 설치는 원클릭 SQL/스크립트 중심입니다.

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
│  ├─ EgoMessageUtil.java                # 개인 메시지/색상 통합
│  ├─ EgoWeaponTypeUtil.java             # 에고 원본/변신 형태 판정
│  ├─ EgoWeaponFormController.java       # 에고무기 자체 형태변환
│  ├─ EgoWeaponControlController.java    # 대화/상태/선공/제어
│  ├─ EgoWeaponAbilityController.java    # 특별능력
│  ├─ EgoWeaponDatabase.java             # DB 로드/저장/형태 저장
│  ├─ EgoWeaponCommand.java              # 점 명령어
│  ├─ EgoOpponentScanController.java     # 상대감지
│  ├─ EgoCoreAdapter.java                # 다른 서버코어 포팅용
│  └─ EgoPortableRules.java              # 다른 서버코어 공통 규칙
└─ sql/
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

---

## 핵심 구현 방향

```text
무기 교체가 아닙니다.
에고무기 자체가 활/양손검/한손검/단검/창/도끼/지팡이/완드 형태로 변신합니다.
```

안전상 원본 아이템 템플릿의 type2는 직접 바꾸지 않습니다.
대신 DB의 `character_item_ego.ego_form_type`에 현재 에고 형태를 저장하고, 에고 로직에서는 이 값을 현재 무기종류처럼 인식합니다.

```text
원본 type2: 실제 아이템 DB 값, 공유 템플릿이라 변경 금지
에고 형태: ego_form_type, 해당 에고무기 1개에만 적용
```

방패 처리:

```text
카르마 활      → 방패 자동 해제, 에고 활 형태
카르마 양검    → 방패 자동 해제, 에고 양손검 형태
카르마 한검    → 에고 한손검 형태, 직전 방패 자동 복구 시도
카르마 단검    → 에고 단검 형태, 직전 방패 자동 복구 시도
```

---

## 초보자 기준: 무엇을 써야 하나?

### myso 서버에 적용

```text
ego/java/EgoMessageUtil.java
ego/java/EgoWeaponTypeUtil.java
ego/java/EgoWeaponFormController.java
ego/java/EgoWeaponControlController.java
ego/java/EgoWeaponAbilityController.java
ego/java/EgoWeaponDatabase.java
ego/java/EgoWeaponCommand.java
ego/java/EgoOpponentScanController.java
ego/sql/ego_oneclick_install.sql
```

### 다른 서버코어에 적용

```text
ego/java/EgoCoreAdapter.java
ego/java/EgoPortableRules.java
ego/sql/ego_oneclick_install.sql
```

다른 서버코어에서는 `EgoWeapon*.java`를 그대로 복사하지 말고, `EgoCoreAdapter.java`를 서버 클래스명에 맞게 구현하는 방식이 안전합니다.

---

## 원클릭 설치

### Windows

```text
ego/install/install_ego_windows.bat
```

더블클릭 후 DB 정보를 입력합니다.

### Linux/macOS

```bash
chmod +x ego/install/install_ego_linux.sh
./ego/install/install_ego_linux.sh
```

원클릭 설치 스크립트는 아래 SQL을 실행합니다.

```text
ego/sql/ego_oneclick_install.sql
```

설치 후 서버가 DB 캐시를 쓰면 아래 중 하나를 실행합니다.

```text
.에고리로드
또는 서버 재시작
```

---

## 기능 요약

```text
- 에고 이름 호출 대화
- 일반채팅으로 호출하되 호출 채팅은 주변에 방송하지 않음
- 에고 응답은 본인에게만 보이는 개인 메시지로 출력
- 에고 응답 색상 자동 주입
- 에고무기 자체 형태변환
- 활/양손검 형태 시 방패 자동 해제
- 한손검/단검/완드 형태 시 직전 방패 자동 복구 시도
- 캐릭터 HP/MP/무기/타겟 상태 인식
- 선공 몬스터 감지
- 상대 캐릭터 위험도 분석
- 간단 자동공격 제어
- 특별 능력 발동
- 에고 이름/레벨/경험치/능력/형태 DB 저장
- Java 수정 없이 SQL로 생성/편집
- 다른 서버코어 포팅용 Adapter/Rules 제공
```

---

## 대화 표시 정책

```text
입력: 일반 채팅으로 "카르마 활" 입력
처리: EgoWeaponControlController가 true를 반환하여 일반 채팅 방송 차단
출력: EgoMessageUtil이 CHATTING_MODE_MESSAGE로 본인에게만 전송
결과: 다른 캐릭터에게 에고 호출/응답이 보이지 않음
```

색상:

```text
일반/정상: \fY
위험/실패: \fR
정보/안내: \fS
```

---

## myso 빠른 적용 순서

```text
1. DB 백업
2. install/install_ego_windows.bat 또는 install/install_ego_linux.sh 실행
3. java 파일 8개 복사
4. ChattingController 연결
5. CommandController 연결
6. DamageController 연결
7. EgoWeaponDatabase.init(con) 연결 또는 .에고리로드
8. 서버 빌드
9. .에고생성 카르마
10. 카르마 상태 / 카르마 활 / 카르마 양검 / 카르마 한검 테스트
11. 카르마 상대 / .에고정보 테스트
```

자세한 내용은 아래 문서 하나만 보면 됩니다.

```text
docs/EGO_SYSTEM_MANUAL.md
```

---

## Java 수정 없이 운영 생성/편집

이미 서버코어에 에고 기능이 연결되어 있다면, 운영 중 생성/편집은 SQL만으로 가능합니다.

```text
sql/ego_no_java_admin.sql
```

지원 작업:

```text
에고 생성, 이름변경, 성격변경, 능력변경, 형태변경, 레벨/경험치 보정, 비활성화, 삭제, 전체 조회, 이상 데이터 보정
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
- 이후 에고 생성/편집은 sql/ego_no_java_admin.sql로 처리할 수 있습니다.
- Java 8 / UTF-8 기준으로 컴파일하세요.
- 운영 전 EgoWeaponAbilityController의 ENABLE_TEST_MODE 확인이 필요합니다.
- 다른 서버코어는 EgoCoreAdapter/EgoPortableRules를 기준으로 포팅하세요.
- ChattingController 연결 위치가 잘못되면 호출 채팅이 주변에 보일 수 있습니다.
- 에고 형태변환은 원본 아이템 DB type2를 바꾸지 않습니다.
```
