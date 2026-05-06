# 에고 폴더 파일맵

이 문서는 `ego` 폴더의 실사용 파일을 역할별로 정리합니다.

---

## 1. 최상위

```text
ego/README.md
```

역할:

```text
에고 시스템 전체 설명
신규/기존 서버 적용 순서
최종 DB/Java 구조 안내
```

---

## 2. Java 외부 연결 그룹

```text
ego/java/EgoCore.java
```

역할:

```text
기존 서버 코드가 연결할 단일 진입점
서버 시작/리로드/명령/채팅/전투 훅을 한곳에서 받음
```

외부 연결은 가능하면 `EgoCore`만 사용합니다.

---

## 3. Java 짧은 Facade 그룹

```text
ego/java/EgoCmd.java
ego/java/EgoSkill.java
ego/java/EgoDb.java
ego/java/EgoScan.java
```

역할:

```text
EgoCmd    명령어 짧은 Facade, 내부적으로 EgoWeaponCommand 호출
EgoSkill  전투 훅 짧은 Facade, 내부적으로 EgoWeaponAbilityController 호출
EgoDb     DB 접근 짧은 Facade, 내부적으로 EgoDB/EgoWeaponDatabase 호출
EgoScan   상대/주변 스캔 짧은 Facade, 내부적으로 EgoOpponentScanController 호출
```

삭제 금지:

```text
Facade지만 외부 서버 코드와 문서 연결을 단순화하기 위한 완충층입니다.
```

---

## 4. Java DB/설정 그룹

```text
ego/java/EgoDB.java
ego/java/EgoConfig.java
ego/java/EgoLevel.java
ego/java/EgoWeaponRule.java
ego/java/EgoTalkPack.java
ego/java/EgoBond.java
ego/java/EgoSchema.java
ego/java/EgoWeaponDatabase.java
```

역할:

```text
EgoDB              전체 DB 로드 허브
EgoConfig          ego_config 설정 로드
EgoLevel           ego_level 통합 레벨/경험치/전투 보너스 로드
EgoWeaponRule      ego_weapon_rule 무기 규칙 로드
EgoTalkPack        ego_talk_pack 대사팩 로드
EgoBond            ego.bond 우선 유대감 처리, ego_bond fallback
EgoSchema          테이블/컬럼 연결성 검증
EgoWeaponDatabase  ego/ego_skill 기본 데이터 로드 및 생성/삭제/경험치 처리
```

---

## 5. Java 명령/표시 그룹

```text
ego/java/EgoWeaponCommand.java
ego/java/EgoView.java
```

역할:

```text
EgoWeaponCommand  .에고생성/.에고삭제/.에고정보/.에고리로드 등 실제 명령 처리 본체
EgoView           nameid 보정, 인벤토리 [에고] 표시, 정보 표시
```

주의:

```text
EgoWeaponCommand는 구버전 파일처럼 보여도 실제 명령 본체입니다. 삭제하면 안 됩니다.
```

---

## 6. Java 전투 그룹

```text
ego/java/EgoWeaponAbilityController.java
ego/java/EgoWeaponTypeUtil.java
```

역할:

```text
EgoWeaponAbilityController  공격/피격/경험치/스턴/반격/이펙트/로그 처리
EgoWeaponTypeUtil           기존 item.type2 기준 무기 지원 여부/기본 능력 판단
```

정책:

```text
무기변형 없음
type2 변경 없음
기존 무기 그대로 사용
```

---

## 7. Java 대화 그룹

```text
ego/java/EgoTalk.java
ego/java/EgoAutoTalk.java
ego/java/EgoGenreTalk.java
ego/java/EgoGenreGuide.java
ego/java/EgoTalkHistory.java
ego/java/EgoMessageUtil.java
ego/java/EgoWeaponControlController.java
```

역할:

```text
EgoTalk                  일반채팅 실시간 대화 진입
EgoAutoTalk              HP/MP/보스/안정 상태 자동 대사
EgoGenreTalk             Java fallback 장르별 오리지널 대사
EgoGenreGuide            장르목록/대화추천 안내
EgoTalkHistory           최근 대화 중복 방지/대사 선택
EgoMessageUtil           말풍선/일반메시지/편지창 출력 공통 유틸
EgoWeaponControlController 일반채팅 명령성 대화 처리
```

---

## 8. Java 상대 감지 그룹

```text
ego/java/EgoOpponentScanController.java
```

역할:

```text
상대 캐릭터 정보 감지
타겟/주변 캐릭터 분석
.에고상대 / .에고주변 처리
```

---

## 9. SQL 실행 파일

### 신규 서버

```text
ego/sql/ego_install_euckr.sql
```

### 기존 서버

```text
ego/sql/ego_update_euckr.sql
```

### 기존 서버 내부 보조 SQL

```text
ego/sql/ego_cleanup_unused.sql
ego/sql/ego_db_config.sql
ego/sql/ego_merge_schema_euckr.sql
ego/sql/ego_talk_pack_dedupe.sql
```

역할:

```text
ego_cleanup_unused.sql      구버전 무기변형/이미지 관련 잔여 정리, 반복 실행 안전
ego_db_config.sql           ego_config/ego_level/ego_weapon_rule 보강
ego_merge_schema_euckr.sql  ego_level 병합, ego.bond 컬럼 병합, 반복 실행 안전
ego_talk_pack_dedupe.sql    대사팩 중복 정리
```

---

## 10. HTML

```text
ego/html/egoletter.htm
```

역할:

```text
긴 에고 응답/명령 결과를 편지창처럼 보여주는 HTML 템플릿
```

복사 대상:

```text
html/egoletter.htm
data/html/egoletter.htm
client/html/egoletter.htm
```

---

## 11. Docs

```text
ego/docs/EGO_MINIMAL_APPLY.md
ego/docs/EGO_DBIZATION.md
ego/docs/EGO_CONNECTIVITY_MAP.md
ego/docs/EGO_FINAL_AUDIT.md
ego/docs/EGO_FILE_MAP.md
ego/docs/EGO_IMPLEMENTED_NO_TEST_COMMANDS.md
ego/docs/EGO_SUGGESTIONS.md
```

역할:

```text
EGO_MINIMAL_APPLY.md                 초보자 최소 적용 문서
EGO_DBIZATION.md                     DB화 최종 현황
EGO_CONNECTIVITY_MAP.md              테이블/컬럼/메서드 연결성 맵
EGO_FINAL_AUDIT.md                   최종 점검표와 실사용 순서
EGO_FILE_MAP.md                      ego 폴더 전체 파일맵
EGO_IMPLEMENTED_NO_TEST_COMMANDS.md  테스트 명령 제외 구현 내역
EGO_SUGGESTIONS.md                   보강/제안 내역
```

---

## 12. 삭제 완료 파일

```text
ego/java/EgoLevelBonus.java
```

삭제 이유:

```text
ego_level 통합 후 EgoLevel.java가 직접 처리
EgoWeaponAbilityController도 EgoLevel 직접 호출로 변경
```

---

## 13. 삭제 보류 파일

```text
ego_bond
ego_level_exp
ego_level_bonus
```

위 항목은 파일이 아니라 DB fallback 테이블입니다.

보류 이유:

```text
기존 서버 데이터 이관 및 구버전 DB 호환
운영 안정화 후 별도 DROP SQL로 제거 가능
```
