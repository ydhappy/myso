# 에고무기 Java 8 호환성 가이드

이 문서는 `ego/` 에고무기 시스템을 Java 8 환경에서 사용하기 위한 기준 문서입니다.

## 1. 결론

에고무기 소스는 Java 8 기준으로 맞춥니다.

권장 컴파일 옵션:

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8
```

Maven 사용 시:

```xml
<properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

Gradle 사용 시:

```gradle
sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8
compileJava.options.encoding = 'UTF-8'
```

## 2. Java 8에서 사용 가능한 문법

현재 에고 시스템에서 허용하는 문법입니다.

```text
- 일반 class/interface
- enum
- generic
- try-catch-finally
- try-with-resources 사용 가능, 단 기존 서버 스타일에 맞춰 현재는 일반 finally 사용
- interface default method 사용 가능
- ConcurrentHashMap
- Collections.synchronizedList
- String switch 사용 가능, 단 현재 코드는 if 기반 위주
```

## 3. 금지 문법

Java 8 서버에서는 아래 문법을 사용하면 안 됩니다.

```text
- var
- record
- sealed class/interface
- switch expression
- text block
- List.of / Map.of / Set.of
- Stream.toList()
- Optional.isEmpty()
- module-info.java
- private interface method
```

## 4. 주의할 API

### 4.1 Locale.ROOT

Java 8에서 지원됩니다.

사용 이유:

```java
value.toLowerCase(Locale.ROOT)
```

서버 OS 언어 설정에 따라 대소문자 변환이 이상해지는 문제를 방지합니다.

### 4.2 interface default method

`EgoCoreAdapter.java`에는 Java 8 default method가 있습니다.

```java
default boolean isPvpAllowed(Object player, Object target) {
    return true;
}
```

Java 8에서는 정상입니다.

만약 서버가 Java 7 이하라면 default method를 제거하고 구현 클래스에서 직접 구현해야 합니다.

## 5. 한글 깨짐 방지

에고 시스템은 한글 메시지를 포함합니다.

반드시 UTF-8로 저장하고 컴파일하세요.

```bash
javac -encoding UTF-8 -source 1.8 -target 1.8 ...
```

Eclipse 사용 시:

```text
Project Properties
→ Resource
→ Text file encoding
→ UTF-8
```

IntelliJ 사용 시:

```text
Settings
→ Editor
→ File Encodings
→ Global Encoding: UTF-8
→ Project Encoding: UTF-8
```

## 6. Java 8 서버에서 특히 확인할 파일

```text
ego/portable/EgoCoreAdapter.java
ego/portable/EgoPortableRules.java
ego/java/EgoWeaponAbilityController.java
ego/java/EgoWeaponCommand.java
ego/java/EgoWeaponControlController.java
ego/java/EgoWeaponDatabase.java
ego/java/EgoWeaponDiagnostics.java
ego/java/EgoOpponentScanController.java
ego/java/EgoWeaponTypeUtil.java
```

## 7. Java 8 포팅 체크리스트

- [ ] JDK 8로 빌드하는지 확인
- [ ] 소스 인코딩 UTF-8 확인
- [ ] `var` 사용 없음
- [ ] `List.of`, `Map.of`, `Set.of` 사용 없음
- [ ] `record`, `sealed`, `module-info.java` 사용 없음
- [ ] `switch ->` 문법 없음
- [ ] `Stream.toList()` 사용 없음
- [ ] 한글 메시지 깨짐 없음
- [ ] 서버 코어의 실제 클래스명에 맞게 import 수정
- [ ] DB 드라이버가 Java 8과 호환되는지 확인

## 8. Java 8용 포팅 권장 방식

다른 서버코어가 Java 8이면 아래 파일을 우선 사용하세요.

```text
ego/portable/EgoCoreAdapter.java
ego/portable/EgoPortableRules.java
```

`ego/java/`는 myso 기준 구현입니다.
다른 서버코어에서는 `ego/java/`를 그대로 복사하기보다 `portable` 기반으로 어댑터를 구현하는 것이 안전합니다.

## 9. 빌드 오류 대응

### default method 오류

오류 예:

```text
default methods are not supported in -source 1.7
```

원인:

```text
컴파일 옵션이 Java 7 이하입니다.
```

해결:

```text
-source 1.8 -target 1.8 로 변경
```

### 한글 깨짐

오류 또는 현상:

```text
인코딩 오류
채팅 메시지 깨짐
```

해결:

```text
-encoding UTF-8 추가
IDE 파일 인코딩 UTF-8 설정
```

### List.of 오류

현재 에고 소스에는 사용하지 않습니다.
다른 코드와 합치는 과정에서 사용했다면 Java 8에서는 아래처럼 바꾸세요.

Java 9+ 코드:

```java
List<String> list = List.of("A", "B");
```

Java 8 코드:

```java
List<String> list = new ArrayList<String>();
list.add("A");
list.add("B");
```

## 10. 운영 권장

Java 8 환경에서는 다음 기준을 권장합니다.

```text
JDK: 1.8
Encoding: UTF-8
Source: 1.8
Target: 1.8
DB Driver: Java 8 호환 버전
IDE Encoding: UTF-8
```
