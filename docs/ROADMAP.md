# ROADMAP — code-quality-suite

> 이 문서는 v0.2.0 이후 계획된 업그레이드 방향을 기록합니다.
> 우선순위·일정은 사용자 피드백과 실사용 결과에 따라 조정됩니다.

---

## 현재 버전

| 버전 | 상태 | 주요 내용 |
|---|---|---|
| **v0.1.0** | ✅ 릴리즈 | MVP — Refactor + Quality 2-stage, Hook, Rules 3단 오버라이드 |
| **v0.2.0** | ✅ 릴리즈 | Hardening — OWASP Security Agent, SpotBugs/JaCoCo, Baseline, 3-stage 파이프라인 |

---

## v0.3.0 — 방어적 설계 + 아키텍처 검증

**목표:** 코드 설계 품질을 선언적으로 강제. + v0.2.0 검증·안정화 병행.

### v0.2.0 미해결 개선사항

- **시나리오 테스트 프로젝트 구축**
  - CC 11 메서드, 파라미터 4개, God Class, `catch(Exception)`, try-finally, LoD 체인 등 위반 더미 파일 세트 작성
  - `security-audit-agent` 대상: Jackson `enableDefaultTyping`, MD5, 하드코딩 JWT, `Runtime.exec(userInput)`, `String.equals(password)`
  - 각 위반이 올바른 심각도로 감지·보고되는지 수동 검증
  - Baseline create → 신규 위반 추가 → `[BASELINE]` 분리 흐름 end-to-end 확인

- **`security-rules.md` 로드 최적화**
  - 현재 단일 392줄 파일 → 카테고리별 분리 또는 심각도 High 이상 규칙만 기본 로드하는 구조 검토
  - Agent 컨텍스트 토큰 부담 절감

- **JaCoCo 변경 메서드 커버리지 파싱 개선**
  - `git diff` 기반 변경 메서드 추출 → JaCoCo XML 매핑이 실제로 정확히 동작하는지 검증
  - 파싱 실패 시 fallback 로직 명확화

- **Hook 엣지케이스 처리**
  - 리포트 파일 부재(최초 커밋, Agent 미실행) 시 Hook 동작 명세화
  - skip 조건 (`--no-verify` 대안) 공식 문서화

### 리팩토링 규칙 강화

- **Immutability 우선 정책**
  - `final` 필드 기본 강제
  - DTO → `record` 변환 권고
  - `Collections.unmodifiableXxx`, 방어적 복사 강제
- **Guard clause / Early return**
  - 중첩 `if` 3레벨 초과 경고
  - 전제 조건 실패 시 조기 return 권장 패턴 제안

### 신규 Agent: `architecture-review-agent`

- **패키지 의존 방향** — 순환 의존(`a.b → a.c → a.b`) 감지 **[High]**
- **레이어 간 의존 강제** — `web → service → repository` 단방향 검증
- **Hexagonal / Clean Architecture** 패턴 준수 힌트
- **DDD 기초** — Entity·Value Object·Aggregate 경계 감지

---

## v0.4.0 — 테스트 · DB 마이그레이션 Agent

**목표:** 테스트 생산성과 DB 변경 안전성 확보.

### 신규 Agent: `test-generation-agent`

- 신규 `public` 메서드에 대해 **JUnit 5 + Mockito 테스트 스켈레톤 자동 생성**
- Given/When/Then 구조, `@DisplayName` 자동 작성
- 예외 경로(`@Test(expected)`, `assertThrows`) 포함
- 생성 후 `mvn test` / `./gradlew test` 자동 실행 + 실패 시 수정

### 신규 Agent: `db-migration-agent`

- **Flyway / Liquibase** 스크립트 (`*.sql`, `*.xml`, `*.yaml`) 정적 분석
- 위험 패턴 감지:
  - `DROP TABLE` / `TRUNCATE` — **Critical**
  - `ALTER TABLE` 컬럼 추가 시 `DEFAULT` 누락 (기존 데이터 NULL 위험) — **High**
  - `NOT NULL` 컬럼 추가 시 `DEFAULT` 또는 백필 전략 부재 — **High**
  - Long-running DDL (`ADD INDEX` 대용량 테이블) 경고 — **Medium**
  - 롤백 스크립트(`undo`) 부재 — **Low**
- 버전 번호 연속성 검증 (V1 → V2 → V3 순서 누락 감지)

### JaCoCo 임계값 커스터마이징

- 프로젝트 오버라이드로 커버리지 목표 팀별 설정 가능
- `<project>/.claude/rules/quality-rules.md` 에 `coverage.threshold: 70` 형태로 지정

---

## v0.5.0 — 외부 툴 확장

**목표:** 정적 분석 커버리지 확대.

### 추가 정적 분석 툴 통합

| 툴 | 역할 | 리포트 경로 |
|---|---|---|
| **PMD** | 코드 스타일·복잡도·중복 (SpotBugs 보완) | `target/pmd.xml` / `build/reports/pmd/main.xml` |
| **Checkstyle** | Google/Sun Java Style 준수 | `target/checkstyle-result.xml` |
| **OWASP Dependency-Check** | CVE DB 기반 의존성 취약점 (전체 스캔) | `target/dependency-check-report.json` |

### Git Secret Scan 연동

- **trufflehog** / **ggshield** CLI 래퍼 — 커밋 이력 전체에서 Secret 감지
- `.security-report.md` 에 통합 보고
- CI/CD 연동 권고 (pre-push hook)

---

## v0.6.0 — CI/CD 통합

**목표:** Claude Code 외부 파이프라인과 연동.

### GitHub Actions 템플릿 자동 생성

```
/generate-workflow
```

- `.github/workflows/code-quality.yml` 자동 생성
- SpotBugs + JaCoCo + OWASP Dependency-Check 포함
- PR 시 자동 파이프라인 실행

### PR 자동 코멘트 봇

- `gh pr comment` 로 Quality / Security 보고서 요약을 PR 코멘트에 자동 게시
- BLOCK 항목은 `❌` 아이콘 + 라인 링크 포함

### Pre-push Hook 연동

- `Bash(git push:*)` 매처 추가 → push 직전에도 Security + Quality 검증
- 로컬 pre-commit 과 다른 레이어에서 이중 안전망

---

## v0.7.0 — 리포트 고도화

**목표:** 보고서 가독성·분석성 개선.

### 구조화 출력 (JSON)

- `.quality-report.json` / `.security-report.json` — 파싱 가능한 JSON 형식 병렬 저장
- CI 시스템, Slack 봇 등 외부 연동 용이

### HTML 리포트

- 대시보드 형태 HTML (`quality-report.html`)
- 심각도별 필터, 파일별 드릴다운

### 트렌드 추적

- `trend.json` — 커밋별 위반 건수 누적 기록
- `/trend show` 커맨드: 최근 N 커밋의 Critical/High 추이 출력

### Agent 실행 메트릭

- 토큰 사용량, 실행 시간, 감지 위반 건수를 세션별 기록
- `/stats` 커맨드로 조회

---

## v1.0.0 — 프로덕션 릴리스

**목표:** 안정화 + 생태계 확장.

### 다국어 문서 (i18n)

- 영문 문서 (`docs/en/`) 추가
- 마켓플레이스 영문 description

### Kotlin 지원 확장

- `.kt` 파일 화이트리스트 추가
- Kotlin 관용구 규칙 (null-safety, data class, sealed class, coroutine)

### IntelliJ IDEA 플러그인 연동

- `.quality-report.json` → IDEA Inspection 패널에 자동 표시
- 파일·라인 이동 지원

### 장기 안정화

- 회귀 테스트 suite (시나리오별 더미 프로젝트로 전체 파이프라인 검증)
- SemVer 기반 Breaking Change 관리
- 커뮤니티 기여 가이드 (`CONTRIBUTING.md`)

---

## 기여 방법

현재 개인 저장소로 운영 중입니다. 제안·피드백은 GitHub Issues 로:

- [https://github.com/eoghks/code-quality-suite/issues](https://github.com/eoghks/code-quality-suite/issues)

규칙 수정 제안은 저장소에 PR 로 보내주세요. `rules/` 파일 변경은 `BUILD-PLAN.md` 에 결정 이력을 함께 기록해야 합니다.
