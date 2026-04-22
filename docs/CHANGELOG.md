# CHANGELOG

[Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) 포맷, [SemVer](https://semver.org/lang/ko/) 준수.

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
