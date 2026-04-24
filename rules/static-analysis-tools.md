# 정적 분석 툴 통합 규칙 (static-analysis-tools)

> `code-quality-agent` 가 참조하는 **PMD · Checkstyle · OWASP Dependency-Check 리포트 파싱 기준**.
> SpotBugs / JaCoCo 는 `quality-rules.md §4A` 에 정의. 이 파일은 v0.5.0 추가분.

---

## 0. 심각도 매핑 공통 원칙

외부 툴 우선순위를 코드-품질 Agent 의 4단계 심각도로 정규화:

| 외부 툴 심각도 | 통합 심각도 | BLOCK |
|---|---|---|
| Blocker / Error / CVE HIGH+ | Critical | ✅ |
| Critical / CVE MEDIUM | High | ❌ (보고) |
| Major / Warning / CVE LOW | Medium | ❌ |
| Minor / Info | Low | ❌ |

---

## 1. PMD (Java 코드 스타일·복잡도·중복)

### 1.1 리포트 경로

```
Maven:  target/pmd.xml
Gradle: build/reports/pmd/main.xml
        build/reports/pmd/test.xml (테스트는 선택 파싱)
```

### 1.2 파싱 방법

```xml
<!-- pmd.xml 예시 -->
<pmd>
  <file name="src/main/java/com/example/UserService.java">
    <violation beginline="42" priority="1" rule="GodClass" ruleset="Design">
      God Class detected
    </violation>
  </file>
</pmd>
```

- `<violation>` 당 1건 보고
- `priority` 매핑: 1 → Critical · 2 → High · 3 → Medium · 4~5 → Low
- `rule` 이름은 그대로 `[PMD-<rule>]` 태그로 사용 (예: `[PMD-GodClass]`)

### 1.3 중복 검출 (`pmd-cpd`)

```
Maven:  target/cpd.xml
Gradle: build/reports/pmd/main-cpd.xml
```

- 50줄 이상 중복 → High
- 20~49줄 중복 → Medium
- 10~19줄 중복 → Low

### 1.4 부재 시 동작

- PMD 리포트 파일 미존재 → `[PMD-MISSING]` Low + Maven/Gradle 설정 스니펫 첨부

```xml
<!-- pom.xml 스니펫 -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-pmd-plugin</artifactId>
  <version>3.21.2</version>
  <configuration>
    <format>xml</format>
    <printFailingErrors>false</printFailingErrors>
  </configuration>
</plugin>
```

---

## 2. Checkstyle (Java Style)

### 2.1 리포트 경로

```
Maven:  target/checkstyle-result.xml
Gradle: build/reports/checkstyle/main.xml
```

### 2.2 파싱 방법

```xml
<checkstyle version="10.12.4">
  <file name="UserService.java">
    <error line="15" severity="warning" message="Line is longer than 120 characters"
           source="com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck"/>
  </file>
</checkstyle>
```

- `severity` 매핑: `error` → High · `warning` → Medium · `info` → Low
- `source` 에서 마지막 `Check` 제거 → 태그: `[CS-LineLength]`

### 2.3 부재 시 동작

- `[CS-MISSING]` Low 보고 + `checkstyle-maven-plugin` 설정 스니펫

---

## 3. OWASP Dependency-Check (CVE DB 기반 의존성 취약점)

### 3.1 리포트 경로

```
Maven:  target/dependency-check-report.json
Gradle: build/reports/dependency-check-report.json
```

### 3.2 파싱 방법

```json
{
  "dependencies": [
    {
      "fileName": "log4j-core-2.14.1.jar",
      "vulnerabilities": [
        {
          "name": "CVE-2021-44228",
          "severity": "CRITICAL",
          "cvssv3": { "baseScore": 10.0 },
          "description": "Apache Log4j2 JNDI features..."
        }
      ]
    }
  ]
}
```

- `severity` 매핑:
  - `CRITICAL` / CVSS 9.0+ → **Critical** (BLOCK)
  - `HIGH` / CVSS 7.0~8.9 → **High**
  - `MEDIUM` / CVSS 4.0~6.9 → **Medium**
  - `LOW` / CVSS 0.1~3.9 → **Low**
- 태그 형식: `[CVE-2021-44228]` (CVE ID 자체 사용)

### 3.3 부재 시 동작

- `[DC-MISSING]` Low 보고 + OWASP Dependency-Check 설정 스니펫

```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>10.0.3</version>
  <configuration>
    <format>JSON</format>
    <failBuildOnCVSS>9</failBuildOnCVSS>
  </configuration>
</plugin>
```

---

## 4. Multi-module 프로젝트 지원

### 4.1 리포트 탐색 Glob

Maven/Gradle 단일 모듈 기준에서 **Glob 기반 다중 모듈 자동 탐색** 으로 확장:

```
target/**/pmd.xml
**/target/pmd.xml
build/reports/pmd/**/*.xml

target/**/checkstyle-result.xml
**/target/checkstyle-result.xml

target/**/dependency-check-report.json
**/build/reports/dependency-check-report.json

target/**/spotbugsXml.xml
**/target/spotbugsXml.xml

target/**/jacoco.xml
**/build/reports/jacoco/**/jacoco.xml
```

### 4.2 모듈별 집계

- 리포트 발견 시 모듈 경로 추출 (`<root>/order-service/target/pmd.xml` → `order-service`)
- 위반 리포트에 `[<module>]` prefix 추가 (예: `[order-service][PMD-GodClass]`)
- 요약 섹션에 모듈별 위반 건수 표시

### 4.3 부모 POM / 루트 settings.gradle 인식

- `pom.xml` `<modules>` 섹션 파싱 → 하위 모듈 목록 구축
- `settings.gradle` `include` 구문 파싱 → 하위 프로젝트 목록
- 모듈 간 의존 관계는 `architecture-review-agent` 가 ARCH-MODULE-01/02 코드로 검증

---

## 5. 보고서 통합 예시

```
## Quality Report — feature/payment-migration

### Critical
- [CVE-2021-44228] log4j-core-2.14.1.jar — Log4Shell (CVSS 10.0)
- [PMD-GodClass] OrderService.java:1 — God Class detected (27 methods)

### High
- [order-service][CVE-2022-22965] spring-core-5.3.17.jar — Spring4Shell (CVSS 9.8)
- [CS-LineLength] UserService.java:15 — Line > 120 chars
- [SB-NP] AuthService.java:33 — NP_NULL_DEREFERENCE

### Medium
- [PMD-CPD] UserMapper.java ↔ OrderMapper.java — 34줄 중복

### 요약 (모듈별)
- order-service: Critical 1 · High 2
- user-service: Critical 1 · Medium 1
- 총합: Critical 2 · High 3 · Medium 1
```
