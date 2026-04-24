# 에고무기 시스템

에고무기 기능을 적용하기 위한 최소 구성 폴더입니다. 파일 수를 줄이고, 설치는 원클릭 SQL/스크립트 중심으로 정리했습니다.

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
│  ├─ EgoWeaponTypeUtil.java
│  ├─ EgoWeaponControlController.java
│  ├─ EgoWeaponAbilityController.java
│  ├─ EgoWeaponDatabase.java
│  ├─ EgoWeaponCommand.java
│  ├─ EgoWeaponDiagnostics.java
│  └─ EgoOpponentScanController.java
├─ portable/
│  ├─ EgoCoreAdapter.java
│  └─ EgoPortableRules.java
└─ sql/
   ├─ ego_oneclick_install.sql
   └─ ego_no_java_admin.sql
```

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
- 캐릭터 HP/MP/무기/타겟 상태 인식
- 선공 몬스터 감지
- 상대 캐릭터 위험도 분석
- 간단 자동공격 제어
- 특별 능력 발동
- 에고 이름/레벨/경험치/능력 DB 저장
- .에고검사 진단 명령
- Java 수정 없이 SQL로 생성/편집
- 타 서버코어 포팅용 portable 제공
```

---

## 빠른 적용 순서

```text
1. DB 백업
2. install/install_ego_windows.bat 또는 install/install_ego_linux.sh 실행
3. java 파일 7개 복사
4. ChattingController 연결
5. CommandController 연결
6. DamageController 연결
7. EgoWeaponDatabase.init(con) 연결 또는 .에고리로드
8. 서버 빌드
9. 게임 접속 후 .에고검사
10. .에고생성 카르마
11. 카르마 상태 / 카르마 상대 / .에고정보 테스트
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
에고 생성, 이름변경, 성격변경, 능력변경, 레벨/경험치 보정, 비활성화, 삭제, 전체 조회, 이상 데이터 보정
```

---

## 타 서버코어 포팅

`ego/java/`는 myso 기준 참고 구현입니다. 다른 서버코어에는 아래 파일을 기준으로 포팅하세요.

```text
portable/EgoCoreAdapter.java
portable/EgoPortableRules.java
```

---

## 게임 명령어

```text
.에고도움
.에고검사
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
```

---

## 주의

```text
- SQL 설치만으로 게임 기능이 자동 발동되는 것은 아닙니다.
- 최초 1회는 서버코어에 Java 연결이 필요합니다.
- 이후 에고 생성/편집은 sql/ego_no_java_admin.sql로 처리할 수 있습니다.
- Java 8 / UTF-8 기준으로 컴파일하세요.
- 운영 전 EgoWeaponAbilityController의 ENABLE_TEST_MODE 확인이 필요합니다.
```
