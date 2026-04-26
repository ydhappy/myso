# 에고 제안 기능 반영 완료 - 테스트 명령 제외

요청 기준: 테스트 명령은 제외하고 실제 기능만 반영.

## 1. 반영 완료 기능

```text
대사 반복 방지
유대감/호감도 시스템
DB 대사팩 선택 지원
상황별 자동 대사
장르대화 색상 분리
기존 리로드 흐름 연결
전투 이벤트 유대감 증가 연결
대사팩 중복 정리 SQL 추가
```

## 2. 테스트 명령 제외 확인

아래 명령은 만들지 않았습니다.

```text
.에고대사테스트
.에고대사목록
.에고호감도테스트
.에고유대감테스트
```

## 3. 추가 Java 파일

```text
EgoTalkHistory.java
- 캐릭터별 최근 대사 5개 메모리 저장
- 같은 대사 반복 체감 감소

EgoBond.java
- 에고 유대감 0~1000
- 대화, 레벨업, 반격, 스턴, 위험 생존 상황에서 자동 증가
- 에고삭제 시 DB/메모리 유대감 삭제

EgoTalkPack.java
- ego_talk_pack 테이블이 있으면 DB 대사 우선 사용
- 없으면 기존 Java 대사로 자동 fallback

EgoAutoTalk.java
- HP 부족, MP 부족, 보스 감지, 안정 상태 자동 대사
- EgoTalk.warning(pc) 호출 흐름에 연결
```

## 4. 수정 Java 파일

```text
EgoTalk.java
- DB 대사팩 우선 처리
- 장르대화 반복 방지 연결
- 대화 시 유대감 증가 연결
- 자동 상황 대사 EgoAutoTalk.warning(pc) 연결

EgoMessageUtil.java
- 장르/감성 대사용 genre() 색상 메서드 추가

EgoDB.java
- EgoBond.reload(con) 연결
- EgoTalkPack.reload(con) 연결
- 에고삭제 시 EgoBond.delete(item) 연결

EgoWeaponAbilityController.java
- 레벨업 시 유대감 +10
- 반격 성공 시 유대감 +2
- 스턴 성공 시 유대감 +3
```

## 5. 추가 SQL

```text
ego/sql/ego_talk_pack_dedupe.sql
- DB 대사팩 중복 제거
- genre + tone + message 유니크 기준 정리
```

## 6. 수정 SQL

```text
ego/sql/ego_install_euckr.sql
- ego_bond 생성
- ego_talk_pack 생성
- 기본 장르 대사 일부 삽입

ego/sql/ego_cleanup_unused.sql
- 기존 서버에 ego_bond 생성
- 기존 서버에 ego_talk_pack 생성
- 기존 에고 아이템용 ego_bond 기본 행 생성
```

## 7. 적용 순서

신규 서버:

```sql
SOURCE ego/sql/ego_install_euckr.sql;
```

기존 서버:

```sql
SOURCE ego/sql/ego_cleanup_unused.sql;
```

대사팩 중복이 의심되면 추가 실행:

```sql
SOURCE ego/sql/ego_talk_pack_dedupe.sql;
```

서버 내에서 리로드:

```text
.에고리로드
```

## 8. 누락 확인

```text
[완료] 테스트 명령 제외
[완료] 대사 반복 방지 파일 추가
[완료] 유대감 파일 추가
[완료] DB 대사팩 파일 추가
[완료] 상황별 자동 대사 파일 추가
[완료] 메시지 색상 보강
[완료] EgoTalk 연결
[완료] EgoDB 리로드 연결
[완료] 전투 이벤트 유대감 연결
[완료] 신규 설치 SQL 반영
[완료] 기존 서버 보정 SQL 반영
[완료] 대사팩 중복 정리 SQL 추가
```

## 9. 남은 수동 확인 필요

실제 서버 전체 소스와 함께 컴파일해야 최종 확인 가능한 항목입니다.

```text
PcInstance.resetAutoAttack() 존재 여부
S_ObjectChatting 패킷 시그니처 서버별 차이
클라이언트 색상코드 \fU 지원 여부
DatabaseConnection.close(...) 오버로드 서버별 차이
```

위 항목은 서버 코어마다 다를 수 있으므로 실제 컴파일 로그 기준으로 조정하면 됩니다.
