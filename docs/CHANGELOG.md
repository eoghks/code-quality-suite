# CHANGELOG

[Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) 포맷, [SemVer](https://semver.org/lang/ko/) 준수.

---

## [0.5.0] - 2026-04-24

**Static Tools · Secret Scan · Multi-module · Operational Safety Release.** PMD/Checkstyle/OWASP Dependency-Check 통합, Secret Scan (trufflehog/ggshield) 연동, Multi-module 지원, /init-project 대화형 마법사, Baseline 만료 정책, @suppress 감사, Prompt Injection 방어.

### Added — Rules

- `rules/prompt-safety.md` — LLM Prompt Injection 방어 규칙
  - PROMPT-INJ-01: `@suppress ALL/*/ANY` 와일드카드 → **High (BLOCK)**, 억제 무시
  - PROMPT-INJ-02: "ignore all previous instructions", "jailbreak", "act as" 등 조작 문구 → Medium
  - PROMPT-INJ-03: `system:/assistant:/user:` 롤 태그 주입 → Medium
  - PROMPT-INJ-04: 40자+ Base64 주석 → Low (참고)
  - 테스트 시나리오 예외: `// @test-prompt-injection` 태그
- `rules/static-analysis-tools.md` — PMD · Checkstyle · OWASP Dependency-Check 리포트 파싱 규칙
  - PMD priority 1~5 → Critical/High/Medium/Low 매핑
  - Checkstyle error/warning/info → High/Medium/Low
  - OWASP-DC CVSS 9.0+ Critical (BLOCK), 7.0~ High, 4.0~ Medium, 0.1~ Low
  - Multi-module Glob 패턴 (`**/target/pmd.xml` 등)
- `rules/baseline-policy.md` — version "2" 포맷 (v1 자동 마이그레이션)
  - `registered_at` · `expires_at` 필드 추가
  - 만료 임박 (30일 이내) → `[BASELINE-EXPIRING]` 경고
  - 만료 (경과) → 정상 위반 승격, Critical 은 BLOCK 재활성화

### Added — Commands

- `/init-project [--preset <name>] [--skip-baseline] [--dry-run]` — 대화형 초기화 마법사
  - 팀 규모·성숙도·스택·엄격도 Q&A → 맞춤 `quality-config.yml` 자동 생성
  - 프리셋: startup / mid-team / enterprise / hexagonal
  - 레거시 프로젝트 자동 baseline 초기화
- `/suppress-audit [대상] [--min-count N] [--stale-days N]` — @suppress 사용 현황 감사
  - 위반 코드별/파일별/작성자별 집계
  - 사유 품질 검사 (양호/보통/부실/누락)
  - Stale (180일+) 항목 리포트
  - `.suppress-audit-report.md` 생성
- `/baseline audit` — Baseline 만료 상태 감사 (신규 서브커맨드)
- `/baseline extend <fingerprint> --days N --reason "<사유>"` — 만료 연장 (사유 필수)

### Changed — Agent

- `code-quality-agent` — PMD · Checkstyle · OWASP Dependency-Check 파싱 로직 추가 (§3.6)
- `code-quality-agent` — Multi-module 리포트 탐색 + 모듈별 집계 (§3.7)
- `code-quality-agent` / `security-audit-agent` / `architecture-review-agent` — Prompt-Safety 스캔 추가
- `security-audit-agent` — Git Secret Scan 연동 (trufflehog/ggshield CLI)
- `security-audit-agent` — Multi-module `**/pom.xml` 탐색
- `architecture-review-agent` — ARCH-MODULE-01/02: 모듈 간 순환·과도 결합 감지 (§3.7)

### Added — Test

- `test/scenarios/MaliciousComment.java` — PROMPT-INJ 위반 감지 검증용
- `test/scenarios/BadPmdReport.xml` — code-quality-agent PMD 리포트 파싱 검증용

### Changed — Plugin 메타

- `plugin.json` / `marketplace.json` — v0.5.0, 6 Agent + 8 Command, pmd/checkstyle/dependency-check/trufflehog/multi-module/prompt-injection 키워드 추가

---

## [0.4.0] - 2026-04-24

**Test · DB Migration · Suppression · DX Release.** 테스트 생성 자동화, DB 마이그레이션 안전성, @suppress 인라인 억제, @Transactional·Spring Security 아키텍처 규칙, YAML 임계값 설정 추가.

### Added — Agent

- `test-generation-agent` — 신규/변경 public 메서드에 JUnit 5 + Mockito 스켈레톤 자동 생성
  - Given/When/Then + `@DisplayName` 자동 작성
  - 정상/예외 경로 분리, `@InjectMocks` + `@Mock` 의존성 자동 처리
  - `mvn test` / `./gradlew test` 자동 실행 + 실패 시 최대 2회 수정
  - 2회 실패 후 `@Disabled` 처리 + TODO 주석으로 수동 완성 안내
  - 브랜치 규칙: main/master 이면 `test/<클래스명-lower>` 브랜치 생성 후 커밋

- `db-migration-agent` — Flyway/Liquibase 마이그레이션 스크립트 정적 분석 (읽기 전용)
  - MIG-DROP: `DROP TABLE` / `TRUNCATE` → Critical
  - MIG-ALTER-NULL: `NOT NULL ADD COLUMN` without DEFAULT → High
  - MIG-ALTER-DEF: `ADD COLUMN` without DEFAULT → High
  - MIG-INDEX: `ADD INDEX` (Lock 경고) → Medium
  - MIG-NO-UNDO: Flyway undo 스크립트 부재 → Low
  - MIG-VERSION: 버전 번호 불연속 → High (BLOCK)
  - MIG-ENCODING: 인코딩 선언 누락 → Low
  - `.migration-report.md` → `[BLOCK: MIGRATION STOP]` / `[PASS: MIGRATION OK]`

### Added — Rules

- `rules/suppress-policy.md` — @suppress 인라인 억제 주석 정책
  - 문법: `// @suppress <코드> — <사유>` (직전 1~2 라인)
  - 사유 없음: [SUPPRESS-NO-REASON] Medium 경고
  - 잘못된 코드: 억제 무시 + [SUPPRESS-INVALID-CODE] Low
  - `--strict` 모드: 모든 @suppress 무시
  - 지원 코드: REF-*, SEC-*, ARCH-*, MIG-* 전체

- `rules/migration-rules.md` — DB 마이그레이션 검증 규칙 (db-migration-agent 참조)

- `rules/architecture-rules.md` §9 @Transactional 위치 검증
  - ARCH-TX-01: Controller @Transactional → Medium
  - ARCH-TX-02: Controller @Transactional(readOnly=true) → Medium

- `rules/architecture-rules.md` §10 Spring Security 설정 검증
  - ARCH-SEC-01: SecurityFilterChain anyRequest() 누락 → High
  - ARCH-SEC-02: permitAll() 과다 적용 (5개+) → Medium
  - ARCH-SEC-03: httpBasic() 활성화 → Medium

### Added — Commands

- `/generate-tests [대상] [--dry-run] [--no-run]` — test-generation-agent 단독 호출
- `/db-check [대상] [--strict] [--version-only]` — db-migration-agent 단독 호출

### Added — Config

- `docs/CONFIG.md` — `.claude/quality-config.yml` 스키마 + 사용 예시 (스타트업 / 엔터프라이즈 / Hexagonal)

### Added — Test

- `test/scenarios/BadMigration.sql` — DROP·NOT NULL without DEFAULT·버전 불연속 위반 포함 (db-migration-agent 검증용)

### Changed — Agent

- `security-audit-agent` — @suppress 인라인 억제 로직 추가
- `architecture-review-agent` — @suppress 인라인 억제 로직 추가
- `code-quality-agent` — @suppress 인라인 억제 로직 추가 + YAML config 로드 (§1.1)

### Changed — Pipeline

- `/run-pipeline` — `--with-tests` 옵션 추가 (Refactor → Test Generation → Security → Quality 4-stage)
- `hooks/pre-commit-pipeline.sh` — `.migration-report.md` BLOCK 마커 체크 추가 (v0.4.0, 1번 섹션), 힌트 메시지에 `/db-check` · `/generate-tests` 추가

### Changed — Plugin 메타

- `plugin.json` / `marketplace.json` — v0.4.0, 6 Agent + 6 Command, suppress·junit5·flyway 키워드 추가

---

## [0.3.0] - 2026-04-24

**Defensive Design + Architecture Release.** v0.2.0 안정화 + Immutability/Guard clause 규칙 추가 + architecture-review-agent 신설 + security-rules 카테고리 분리.

### Added — Agent

- `architecture-review-agent` — 패키지 의존 방향·레이어 분리·DDD 경계 정적 검증 (읽기 전용)
  - ARCH-LAYER: Controller→Repository 직접 import, Service→Controller import 감지 [High]
  - ARCH-CYCLE: 패키지 간 순환 의존 감지 [High]
  - ARCH-DDD: `@RequestBody Entity` 직접 수신·반환 감지 [High]
  - ARCH-PKG: 표준 패키지명 위반 [Low]
  - ARCH-HEX: Hexagonal Architecture 힌트 [Low]
  - `.architecture-report.md` → `[BLOCK: ARCH STOP]` / `[PASS: ARCH OK]` 마커

### Added — Rules

- `rules/architecture-rules.md` — 아키텍처 검증 전체 기준 (레이어/순환의존/DDD/패키지명/Hexagonal)
- `rules/refactor-rules.md` §13 Immutability — `final` 강제·`record` 권고·방어적 복사·불변 컬렉션 반환
- `rules/refactor-rules.md` §14 Guard Clause — 중첩 if 3레벨 초과 경고·else 제거 권고
- `rules/security/` 카테고리 분리 — injection·crypto·access-control·deserialization·misc (토큰 최적화)

### Added — Commands

- `/architecture-review [target] [--full] [--strict]` — architecture-review-agent 단독 호출

### Added — Test

- `test/scenarios/` — 더미 파일 8개 (Refactor·Security·Architecture Agent 각 위반 감지 검증용)
- `docs/SCENARIOS.md` — 시나리오 파일별 위반 목록 + 검증 방법 가이드

### Changed

- `/run-pipeline` — `--full` 옵션 추가 (4-stage: Refactor → Architecture → Security → Quality)
- `hooks/pre-commit-pipeline.sh` — `.architecture-report.md` BLOCK 마커 체크 추가 (v0.3.0)
- `security-audit-agent` — 변경 파일 유형별 카테고리 선택 로드로 컨텍스트 최적화
- `plugin.json` / `marketplace.json` — v0.3.0, architecture-review-agent·architecture-review 커맨드 추가

### Fixed (v0.2.0 안정화)

- Hook 엣지케이스 명시화 — 리포트 파일 미존재 시 BLOCK 체크 건너뜀 (최초 커밋 허용) 주석 추가
- `security-rules.md` 단일 392줄 → 인덱스 + `security/` 5개 카테고리 파일로 분리

---

## [0.2.0] - 2026-04-21

**Hardening Release.** 리팩토링·보안·품질 규칙 대폭 강화, OWASP 전담 Agent 신설, SpotBugs/JaCoCo 통합, Baseline 시스템 도입.

### Added — Agent

- `security-audit-agent` — OWASP Top 10 전담 보안 정적 스캔 (읽기 전용)
  - A01 Broken Access Control (`@PreAuthorize` 누락)
  - A02 Cryptographic Failures (MD5/SHA1·SecureRandom·하드코딩 Secret regex)
  - A03 Injection (SQL `${}` · Command · LDAP · HTTP Header)
  - A04~A10 전체 + Cookie/XXE/Path Traversal
  - `.security-report.md` → `[BLOCK: SECURITY STOP]` / `[PASS: SECURITY OK]` 마커

### Added — Rules

- `rules/security-rules.md` — OWASP A01~A10 + 추가 항목 전체 검증 기준
  - 하드코딩 Secret 정규식: AWS `AKIA*`, GitHub `ghp_*`, JWT, Private Key, Slack
  - 취약 의존성 수동 목록: Log4Shell · Spring4Shell · commons-text · jackson · h2database
- `rules/baseline-policy.md` — Baseline 포맷·등록 원칙·Agent 동작 흐름 정의

### Added — Commands

- `/security-scan [target]` — security-audit-agent 단독 호출
- `/baseline create|update|show` — `.quality-baseline.json` 생성·갱신·조회

### Added — Baseline 시스템

- `.quality-baseline.json` — SHA-256 fingerprint 기반 레거시 위반 등록
- 신규 위반만 차단, 기존 위반은 `[BASELINE]` 태그로 분리 보고
- Baseline 없는 신규 프로젝트는 기존 동작 동일

### Changed — Rules

- `rules/refactor-rules.md` — §10~12 추가 (메트릭·런타임 안전·응집도)
  - §10 메트릭: Cyclomatic Complexity 10 · Cognitive 15 · 파라미터 3 · 필드 10
  - §11 런타임 안전: `catch(Exception)` · `printStackTrace()` · try-with-resources 강제
  - §12 응집도·결합도: 클래스 400줄 · God Class · Law of Demeter 3단계
- `rules/quality-rules.md` — 보안 섹션 security-rules.md 로 이관, §4A 신설
  - SpotBugs 카테고리→심각도 매핑 (MALICIOUS_CODE→Critical, CORRECTNESS→High)
  - JaCoCo 라인 커버리지 80% 임계값 (변경 메서드 기준)
  - Graceful fallback + 빌드 설정 스니펫

### Changed — Agent

- `code-quality-agent` — 보안 최소 스모크 체크만 유지 (전체 보안은 security-audit-agent)
  - §3.6 SpotBugs/JaCoCo 리포트 파싱 절차 추가
  - `--strict` 모드 (Baseline 무시) 맥락 반영

### Changed — Pipeline

- `commands/run-pipeline.md` — 2-stage → **3-stage** (Refactor → Security → Quality)
  - `--strict` 인자: Baseline 무시 + 전체 소스 전수 검사
- `hooks/pre-commit-pipeline.sh` — `.security-report.md` BLOCK 마커 감지 추가
  - Security · Quality 둘 중 하나라도 BLOCK → `exit 2`

### Changed — Plugin 메타

- `plugin.json` · `marketplace.json` — v0.2.0, Agent 3개 · Command 3개, categories에 `security` 추가

### Added — 문서

- `docs/BASELINE.md` — Baseline 전용 가이드 (생성·업데이트·해제·팀 운영)
- `docs/INSTALL.md` — SpotBugs/JaCoCo Maven/Gradle 빌드 설정 절 추가

### Roadmap (v0.3+)

- Immutability / Guard clause 규칙 (방어적 설계)
- `architecture-review-agent` (Hexagonal/Clean/DDD)
- `test-generation-agent` (테스트 스켈레톤 자동 생성)
- `db-migration-agent` (Flyway/Liquibase 안전성)
- PMD · Checkstyle · OWASP Dependency-Check 통합
- GitHub Actions CI/CD 템플릿

---

## [0.1.0] - 2026-04-21

**첫 검증 릴리즈.** Phase 0~9 구축 완료, 개인 GitHub 저장소 공개 배포.

### Added — Agent
- `code-refactoring-agent` — 자바 코드 리팩토링 전문 (model: claude-sonnet-4-6)
  - 규칙 3단 로드 (user > project > plugin)
  - 브랜치 자동 감지 + 생성 (`feature/` · `bugfix/` · `refactor/`)
  - main/master 직접 커밋 차단
  - 메서드 50줄 제한, null 정책, Map→DTO, 매직넘버 상수화
  - 설계 패턴 적극 활용 (Strategy · Factory · Builder · Template Method · Facade · Adapter · Decorator · Observer)
  - 테스트 동시 갱신 + 자동 실행 (pom/gradle 감지)
  - 아토믹 커밋 + 완료 보고 템플릿
- `code-quality-agent` — 자바 코드 품질·보안 검증 전문 (model: claude-sonnet-4-6, 읽기 전용)
  - diff 기반 검증 (레거시 부담 없음)
  - 5축 검증: 보안 · 규칙 준수 · 테스트 커버리지 · 테스트 실행 · 성능 안티패턴
  - SQL Injection `${}` 검출 (Critical)
  - 민감정보 로그 스캔 (password/token/secret + apiKey/credential/bearer + ssn/jumin/phone)
  - N+1 쿼리 · `findAll()` + stream filter · 반복 문자열 연산 · 로거 파라미터 방식 감지
  - 심각도 4단계 (Critical/High/Medium/Low) + 식별 코드 (`[SQL-INJ]` 등)
  - BLOCK 마커 출력 (`[BLOCK: COMMIT STOP]` / `[PASS: COMMIT READY]`)

### Added — Hook
- `PreToolUse: Bash(git commit:*)` — `pre-commit-pipeline.sh`
  - 스테이징 파일 화이트리스트 필터 (`.java`, `.xml`, `.jsp`, `.js`, `.css`, `.html`, `pom.xml`, `build.gradle`, `build.gradle.kts`)
  - 파이프라인 권고 힌트 주입
  - 이전 `.quality-report.md` BLOCK 마커 감지 시 `exit 2` (커밋 자동 중단)
  - timeout 5초

### Added — Command
- `/run-pipeline [target]` — Refactor → Quality 파이프라인 수동 실행
  - 인자 없음 → 현재 브랜치 전체
  - 파일/브랜치 지정 가능
  - 4단계 실행 절차 + 요약 템플릿

### Added — Rules (3단 오버라이드 지원)
- `shared-standards.md` — Java/Jakarta/Lombok/레이어 공통 표준
- `refactor-rules.md` — 메서드 크기·null·Map·매직넘버·설계 패턴·DRY·네이밍·테스트·브랜치 전략
- `quality-rules.md` — 보안·규칙 준수·테스트·성능·보고 형식·권한 범위

### Added — Plugin 인프라
- `.claude-plugin/plugin.json` — components 선언 (agents/hooks/commands)
- `.claude-plugin/marketplace.json` — 카테고리 `development-tools`, `java-tooling`, 태그 10개
- MIT License

### Added — 문서
- `docs/README.md` — 전체 가이드 + 사용 예시 + 보고서 샘플
- `docs/INSTALL.md` — 설치·업데이트·제거·문제 해결
- `docs/CUSTOMIZATION.md` — 3단 오버라이드 + 실전 예시 5종
- `docs/BUILD-PLAN.md` — 구축 Q&A 결정 이력 (Phase 0~9)
- `docs/CHANGELOG.md` — (이 파일)
- 루트 `README.md` — GitHub 첫 화면 stub

### Added — 조직 규칙 자동 준수
- 브랜치 태그 후 병합 강제
- `git push --force` · `git reset --hard` · `--no-verify` · `git branch -D` 차단
- 한국어 커밋 메시지 + 영어 타입 prefix

---

## [0.0.4] - 2026-04-21 (개발 마일스톤)

Phase 7~8 완료.

- hooks/hooks.json · pre-commit-pipeline.sh · /run-pipeline command
- plugin.json · marketplace.json

## [0.0.3] - 2026-04-21 (개발 마일스톤)

Phase 5~6 완료.

- code-refactoring-agent.md
- code-quality-agent.md

## [0.0.2] - 2026-04-21 (개발 마일스톤)

Phase 2~4 완료.

- shared-standards.md · refactor-rules.md · quality-rules.md

## [0.0.1] - 2026-04-21 (개발 마일스톤)

Phase 0~1 완료.

- 저장소 초기화, 루트 README stub, `.gitignore`, 개인 GitHub 원격 연결
- docs/BUILD-PLAN.md 스캐폴드

---

## 예정 (Unreleased)

### 검토 중
- IntelliJ IDEA 플러그인 연동 (Quality 보고서 → Inspection)
- GitHub Actions 연동 (PR 시 자동 파이프라인)
- Kotlin 지원 확장
- `/run-pipeline --dry-run` 옵션
- 다국어 문서 (영어)
