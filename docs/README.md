# code-quality-suite — 전체 가이드

Claude Code 용 **자바 코드 리팩토링 + 아키텍처 + 보안 + 품질 + 테스트 생성 + DB 마이그레이션 검증 전문 Agent** 번들 Plugin 입니다 (v0.5.0). 6개 Agent + 8개 Command + @suppress 인라인 억제 + YAML 임계값 설정 + **PMD/Checkstyle/OWASP Dependency-Check 통합 + trufflehog/ggshield Secret Scan + Multi-module 지원 + Prompt Injection 방어 + Baseline 만료 정책 + `/init-project` 대화형 마법사** 를 통해 Java/Spring Boot 프로젝트의 코드 품질을 전방위로 자동 검증합니다.

---

## 구성 요소

| 종류 | 이름 | 역할 |
|---|---|---|
| Agent | `code-refactoring-agent` | 구조 개선 · CC/Cognitive/파라미터 메트릭 · Immutability · Guard Clause · 예외/Resource 안전 · 브랜치/커밋 자동화 |
| Agent | `architecture-review-agent` | 레이어 역방향 의존 · 순환 의존 · DDD Entity 노출 · @Transactional 위치 · Spring Security 설정 · `.architecture-report.md` 출력 (읽기 전용) |
| Agent | `security-audit-agent` | OWASP Top 10 보안 스캔 · 하드코딩 Secret · 취약 의존성 · @suppress 처리 · `.security-report.md` 출력 (읽기 전용) |
| Agent | `code-quality-agent` | 규칙 준수 · 테스트 커버리지 · 성능 안티패턴 · SpotBugs/JaCoCo 파싱 · @suppress 처리 · YAML 임계값 적용 · `.quality-report.md` 출력 (읽기 전용) |
| Agent | `test-generation-agent` | 신규/변경 public 메서드 JUnit 5 + Mockito 스켈레톤 자동 생성 · mvn/gradlew test 자동 실행 · @Disabled fallback |
| Agent | `db-migration-agent` | Flyway/Liquibase SQL 정적 분석 · DROP/NOT NULL/버전 불연속 감지 · `.migration-report.md` 출력 (읽기 전용) |
| Hook | `PreToolUse: Bash(git commit:*)` | Migration + Architecture + Security + Quality BLOCK 마커 감지 시 exit 2 · 파이프라인 권고 |
| Command | `/run-pipeline [--full\|--with-tests] [--strict]` | 3~5-stage 파이프라인 수동 실행 |
| Command | `/architecture-review [target]` | architecture-review-agent 단독 호출 |
| Command | `/security-scan [target]` | security-audit-agent 단독 호출 |
| Command | `/generate-tests [target]` | test-generation-agent 단독 호출 |
| Command | `/db-check [target]` | db-migration-agent 단독 호출 |
| Command | `/baseline create\|update\|show\|audit\|extend` | `.quality-baseline.json` 관리 + 만료 감사 · 연장 |
| Command | `/init-project [--preset]` | 대화형 초기화 마법사 (quality-config.yml + baseline 자동 생성) |
| Command | `/suppress-audit [대상]` | @suppress 사용 현황 감사 리포트 |
| Rules | `shared-standards.md` · `refactor-rules.md` · `quality-rules.md` · `security-rules.md` · `architecture-rules.md` · `migration-rules.md` · `suppress-policy.md` · `baseline-policy.md` · `static-analysis-tools.md` · `prompt-safety.md` | 3단 오버라이드 (사용자 > 프로젝트 > Plugin) |

---

## 빠른 설치

```bash
/plugin marketplace add https://github.com/eoghks/code-quality-suite.git
/plugin install code-quality-suite
/plugin list   # 활성 확인
```

자세한 절차는 [INSTALL.md](INSTALL.md) 참고.

---

## 기본 사용법

### 1. 전체 파이프라인 (3-stage)

```bash
# Refactor → Security → Quality 전체 실행
/run-pipeline

# 특정 파일 지정
/run-pipeline src/main/java/com/example/UserService.java

# Baseline 무시 + 전수 검사 (정기 감사)
/run-pipeline --strict
```

### 2. 개별 호출

```bash
# 보안 스캔만
/security-scan
/security-scan src/main/java/com/example/AdminController.java

# 리팩토링만
/agent code-refactoring-agent UserService.java

# 품질 검증만
/agent code-quality-agent

# 테스트 스켈레톤 자동 생성
/generate-tests
/generate-tests src/main/java/com/example/UserService.java

# DB 마이그레이션 안전성 검증
/db-check
/db-check db/migration/V5__add_payment.sql
/db-check --all
```

### 5. YAML 임계값 설정

```yaml
# <project>/.claude/quality-config.yml
refactor:
  method.max-lines: 60   # 기본 50
  cc.threshold: 12       # 기본 10
quality:
  jacoco.threshold: 70   # 기본 80
```

### 6. @suppress 인라인 억제

```java
// @suppress REF-CC — 레거시 분기 로직, 리팩토링 일정 2026-Q3
public String processLegacy(String type) { ... }
```

```xml
<!-- @suppress SQL-INJ — sortColumn Enum 화이트리스트 검증 완료 -->
<select id="findAllSorted">SELECT * FROM user ORDER BY ${sortColumn}</select>
```

### 3. 레거시 프로젝트 — Baseline 도입

```bash
# 1. 기존 위반 전체를 baseline 으로 등록
/baseline create

# 2. .quality-baseline.json 커밋
git add .quality-baseline.json
git commit -m "chore: 초기 품질 baseline 등록"

# 3. 이후 신규 위반만 차단
/run-pipeline   # baseline 자동 적용
```

### 4. 커밋 직전 자동 파이프라인 (Hook)

```
사용자: UserService 변경사항 커밋해줘

→ Claude 가 git commit 실행 시도
→ Hook 발화 (Bash(git commit:*) 매치)
→ .security-report.md / .quality-report.md BLOCK 마커 확인
→ BLOCK 있으면 exit 2 → 커밋 자동 중단
→ BLOCK 없으면 3-stage 파이프라인 권고 힌트 주입
```

---

## 보고서 샘플

### Security 보고서 (`.security-report.md`)

```
## Security Audit Report — feature/user-login

### Critical (즉시 조치)
- [SQL-INJ] UserMapper.xml:45 — ${name} 직접 치환 (A03)
- [HARDCODED-SECRET] application.yml:12 — AWS Access Key 패턴 (A02)

### High
- [NO-AUTHZ] AdminController.java:32 — @PreAuthorize 누락 (A01)
- [TIMING-ATTACK] LoginService.java:78 — password.equals() 사용 (A07)

### 요약
- Critical: 2건 · High: 2건 · Baseline 제외: 0건

[BLOCK: SECURITY STOP]
```

### Quality 보고서 (`.quality-report.md`)

```
## Quality Report — feature/user-login

### Critical (차단 권고)
- [TEST-FAIL] UserServiceTest — 통과 41건 / 실패 1건

### High
- [LAYER] UserController.java:32 — Repository 직접 주입
- [N+1] OrderService.java:78 — @OneToMany lazy + 루프 접근
- [SB-BUG] UserService.java:55 — NP_NULL_ON_SOME_PATH (SpotBugs)
- [COV-LOW] UserService#findById — 라인 커버리지 58% (권장 80%)

### Medium
- [CC-HIGH] OrderService.java:102 — CC=13 (권장 10 이하)
- [RESOURCE-LEAK] FileService.java:33 — FileInputStream try-with-resources 권고

### Baseline (기존 위반)
- [BASELINE][METHOD-LEN] LegacyService.java:200 — 73줄

### 요약
- Critical: 1건 · High: 4건 · Medium: 2건 · Baseline 제외: 1건

[BLOCK: COMMIT STOP]
```

---

## Rules 커스터마이징

기본 규칙이 조직/개인 환경에 맞지 않으면 **3단 오버라이드**로 보완:

```bash
# 개인 전역 규칙 (최우선)
mkdir -p ~/.claude/rules
vi ~/.claude/rules/refactor-rules.md   # CC 임계값 조정 등

# 프로젝트별 규칙 (중간)
mkdir -p <project>/.claude/rules
vi <project>/.claude/rules/security-rules.md  # 추가 Secret 키워드 등
```

자세한 가이드는 [CUSTOMIZATION.md](CUSTOMIZATION.md) 참고.

---

## 조직 규칙 자동 준수

- ❌ main / master 직접 커밋 → Refactor Agent 가 **자동 차단** 후 `feature/`·`bugfix/`·`refactor/` 브랜치 생성·이동
- ❌ `git push --force`, `git reset --hard`, `--no-verify` → Agent 권한 설정으로 차단
- ✅ 브랜치명 자동 감지 (작업 유형별 prefix)
- ✅ 아토믹 커밋 (리팩토링 단위 분리)
- ✅ 한국어 커밋 메시지 + 영어 타입 prefix (`feat`, `fix`, `refactor`, ...)

---

## 지원 스택

- **Java** 17+
- **Jakarta EE** (`jakarta.*`, `javax.*` 금지)
- **Spring Boot** + **Spring Security**
- **MyBatis** (Controller → Service → Dao) / **JPA** (Controller → Service → Repository)
- **Lombok** 핵심 어노테이션 (`@Getter` / `@Setter` / `@Slf4j`, `@Data` 금지)
- **빌드 도구** Maven / Gradle 자동 감지
- **SpotBugs** / **JaCoCo** 리포트 파싱 (선택적 통합)

---

## 문서

- [INSTALL.md](INSTALL.md) — 설치·업데이트·제거 절차 + SpotBugs/JaCoCo 설정
- [CUSTOMIZATION.md](CUSTOMIZATION.md) — Rules 3단 오버라이드 가이드
- [CONFIG.md](CONFIG.md) — `.claude/quality-config.yml` YAML 임계값 설정 가이드
- [BASELINE.md](BASELINE.md) — Baseline 생성·업데이트·팀 운영 가이드
- [SCENARIOS.md](SCENARIOS.md) — 시나리오 파일별 위반 목록 + 검증 방법
- [BUILD-PLAN.md](BUILD-PLAN.md) — 구축 Q&A 결정 이력
- [CHANGELOG.md](CHANGELOG.md) — 버전별 변경 이력

---

## 라이선스

MIT License — 상업/개인 사용, 수정, 배포 자유.

## 저자

daehwan · [github.com/eoghks](https://github.com/eoghks)
