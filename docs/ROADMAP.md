# ROADMAP — code-quality-suite

> 이 문서는 v0.2.0 이후 계획된 업그레이드 방향을 기록합니다.
> 우선순위·일정은 사용자 피드백과 실사용 결과에 따라 조정됩니다.

---

## 현재 버전

| 버전 | 상태 | 주요 내용 |
|---|---|---|
| **v0.1.0** | ✅ 릴리즈 | MVP — Refactor + Quality 2-stage, Hook, Rules 3단 오버라이드 |
| **v0.2.0** | ✅ 릴리즈 | Hardening — OWASP Security Agent, SpotBugs/JaCoCo, Baseline, 3-stage 파이프라인 |
| **v0.3.0** | ✅ 릴리즈 | Defensive Design — Immutability/Guard Clause, architecture-review-agent, security-rules 분리, 시나리오 테스트 |
| **v0.4.0** | ✅ 릴리즈 | Test·DB·Suppression·DX — test-generation-agent, db-migration-agent, @suppress, @Transactional/Security 규칙, YAML config |
| **v0.5.0** | ✅ 릴리즈 | 외부 툴·멀티 모듈·운영 안전망 — PMD/Checkstyle/OWASP-DC, Secret Scan, Multi-module, `/init-project`, Baseline 만료, `/suppress-audit`, Prompt Injection 방어 |
| **v0.6.0** | ✅ 릴리즈 | Agent 협업·교육·대용량 처리 — pipeline-state.json, `/agent-explain`, Large Diff Chunk 분할 |
| **v0.6.1** | ✅ 릴리즈 | Rule 파일 세분화 — 4개 대형 규칙 파일을 파일당 200줄 이하 서브디렉터리로 분리 |
| **v0.7.0** | 📝 계획 | Round-trip 피드백 루프·리포트·AI 품질·프론트엔드 — Quality BLOCK → Refactor 자동 재호출, JSON/HTML 리포트, 트렌드, Confidence Score, frontend-rules, Rule Conflict Detection |
| **v0.8.0** | 📝 계획 | 엔터프라이즈 확장 — 팀별 프로파일, Slack/Jira/Datadog 연동, compliance-audit-agent, `/changelog-gen`, 규칙별 WHY 문서 |
| **v1.0.0** | 📝 계획 | 프로덕션 릴리스 — i18n, Kotlin/Groovy/Scala, IDEA 플러그인, WebFlux/Reactive, 장기 안정화 |
| **v1.1.0** | 📝 계획 | CI/CD 통합 — GitHub/GitLab Actions 템플릿, PR 봇, Pre-push Hook |

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

## v0.4.0 — 테스트 · DB 마이그레이션 · 규칙 DX 개선

**목표:** 테스트 생산성, DB 변경 안전성, 규칙 커스터마이징 편의성 확보.

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

### 인라인 억제 주석 (`@suppress`)

- Baseline JSON 없이 코드 라인에 직접 억제 사유 명시
  ```xml
  <!-- @suppress SQL-INJ — sortColumn Enum 화이트리스트 검증 완료 -->
  SELECT * FROM user ORDER BY ${sortColumn}
  ```
- 억제 주석 없는 위반은 기존대로 차단
- 남용 방지: `@suppress` 사유 텍스트 필수 (사유 없으면 경고)

### 아키텍처 규칙 확장

- **`@Transactional` 위치 검증** — Controller 에 `@Transactional` 선언 시 **Medium** (Service 책임 위반)
- **Spring Security 설정 강화** — `SecurityFilterChain` 에서 `anyRequest().authenticated()` 누락, `permitAll()` 과다 적용 감지

### 규칙 YAML 설정 파일

- `.claude/quality-config.yml` 한 파일로 주요 임계값 변경 — `.md` 오버라이드보다 진입 장벽 낮춤
  ```yaml
  method.max-lines: 60
  cc.threshold: 12
  jacoco.threshold: 70
  params.max: 4
  ```
- JaCoCo 임계값 커스터마이징 통합 (기존 `coverage.threshold` 방식 대체)

---

## v0.5.0 — 외부 툴 확장 · 멀티 모듈 · 운영 안전망

**목표:** 정적 분석 커버리지 확대 + 실무 프로젝트 구조 완전 지원 + 도입 장벽 해소 + @suppress/Baseline 운영 안전망.

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

### Multi-module Maven/Gradle 지원

- 현재 단일 모듈 가정 → 멀티 모듈 프로젝트 대응
- 모듈별 SpotBugs/JaCoCo 리포트 자동 탐색 (`**/target/spotbugsXml.xml`, `**/build/reports/jacoco/**`)
- 모듈 간 의존 방향 검증 (`architecture-review-agent` 확장)
- `pom.xml` 계층 구조 파악 (parent → child 모듈 관계 인식)

### 초기 도입 마법사 — `/init-project`

- 대화형 Q&A (팀 규모 · 프로젝트 성숙도 · 기술 스택)
- `.claude/quality-config.yml` 기본값 자동 생성 (스타트업/엔터프라이즈/Hexagonal 프리셋)
- 레거시 프로젝트면 `/baseline create` 자동 수행
- 기존 규칙 오버라이드 디렉터리 (`~/.claude/rules/`, `<project>/.claude/rules/`) 스캐폴딩

### Baseline 만료 정책

- `.quality-baseline.json` 각 violation 에 `registered_at` · `expires_at` 필드 추가
- 만료 임박 (30일 이내) → `.quality-report.md` 에 `[BASELINE-EXPIRING]` 경고
- 만료 초과 → 정상 위반으로 승격 (BLOCK 재활성화)
- `/baseline audit` — 전체 baseline 항목의 등록일·만료일·경과일 집계 리포트 + 90일 초과 stale 목록

### @suppress 감사 — `/suppress-audit`

- 프로젝트 전체 `@suppress` 주석 수집 → 코드별/파일별/작성자별 집계
- 사유 품질 검사 (사유 10자 미만·"임시"·"TODO" 등 불명확 텍스트 → 경고)
- `.suppress-audit-report.md` 생성
- PR 정책 힌트: "이번 PR 에서 @suppress N건 추가 — 리뷰어 재확인 권고"

### Prompt Injection 방어 규칙

- `rules/prompt-safety.md` 신설 — 악성 주석 패턴 블랙리스트
  - `ignore all previous rules`, `disregard instructions`, `act as`, `jailbreak` 등
  - `@suppress ALL`, `@suppress *` 와 같은 와일드카드 시도 차단
- 모든 Agent 가 주석 스캔 시 해당 패턴 감지 → `[PROMPT-INJ]` **Medium** 경고 + 원래 규칙 적용 유지
- 주석 블록 내 "system:", "assistant:" 같은 롤 태그 감지 → Medium

---

## v0.6.0 — Agent 협업 · 교육 · 대용량 처리

**목표:** Agent 간 교차 검증 + 개발자 학습 곡선 단축 + 대용량 PR 대응.

### Agent 협업 — `pipeline-state.json`

- 각 Agent 가 읽고 쓰는 **공유 상태 파일** — 독립 실행 한계 해소
- 기록 항목: Stage 완료 시각 · 수정 파일 목록 · fingerprint · Agent 권고 충돌 현황
- Refactor Agent 수정 → Security Agent 가 수정 범위만 재검증 (Round-trip)
- **Conflict Resolution 정책** — 권고 충돌 시 우선순위: Security(Critical) > Architecture(High) > Refactor(Medium)
- 파이프라인 종료 시 state 파일 비움 (세션 단위)

### 교육 커맨드 — `/agent-explain <CODE>`

- 특정 위반 코드의 **배경·위험·해결책** 자연어 설명
- 예: `/agent-explain SQL-INJ` → "SQL Injection 은 … 해결: #{param} 바인딩 사용"
- 규칙 파일(`rules/*.md`)의 해당 섹션 + 샘플 코드 + 실제 공격 시나리오 출력
- 주니어 개발자 온보딩 · 코드 리뷰 근거 설명에 활용

### Large Diff Chunk 분할 전략

- 변경 파일 50개 초과 시 Agent 가 자동 chunk 분할
- chunk 별 부분 보고서 생성 → 최종 merge 후 단일 `.quality-report.md`
- 토큰 폭증·Context Window 초과 방지
- `--chunk-size` 옵션으로 수동 조정 가능

---

## v0.7.0 — Round-trip 피드백 루프 · 리포트 고도화 · AI 품질 · 프론트엔드

**목표:** Quality BLOCK 시 자동 재수정 루프 + 보고서 가독성·분석성 + False Positive 완화 + 프론트엔드 규칙 확장.

### Round-trip 피드백 루프 (핵심 신규)

현재(v0.6.0)는 Quality Agent 가 BLOCK 을 내면 파이프라인이 멈추고 사용자가 수동으로 재실행해야 한다.
v0.7.0 에서는 **자동 재수정 루프**를 도입해 BLOCK 해소까지 자동으로 반복한다.

#### 동작 흐름

```
Quality Agent → BLOCK 발생
      │
      │ pipeline-state.json 에 block_reasons 기록
      │ {
      │   "block_reasons": [
      │     { "code": "CC-HIGH", "file": "OrderService.java", "line": 42 },
      │     { "code": "NULL-RETURN", "file": "UserService.java", "line": 88 }
      │   ],
      │   "retry": { "count": 0, "max": 2 }
      │ }
      │
      ▼
Refactor Agent 재호출 (block_reasons 타겟만)
      │
      │ CC-HIGH → 메서드 분리
      │ NULL-RETURN → Optional 전환
      │
      ▼
Quality Agent 재실행
      │
      ├─ PASS → 파이프라인 완료 ✅
      └─ BLOCK (retry.count < max) → 재시도
               (retry.count == max) → 사용자에게 보고 ⛔
```

#### pipeline-state.json 확장 필드

```json
{
  "stages": {
    "quality": {
      "block": true,
      "block_reasons": [
        { "code": "CC-HIGH",    "file": "OrderService.java", "line": 42, "severity": "Critical" },
        { "code": "NULL-RETURN","file": "UserService.java",  "line": 88, "severity": "High" }
      ]
    }
  },
  "retry": {
    "count": 1,
    "max": 2,
    "history": [
      { "attempt": 1, "fixed": ["CC-HIGH"], "remaining_blocks": 1 }
    ]
  }
}
```

#### 구현 범위

- `commands/run-pipeline.md` — 재시도 루프 로직 (`max: 2` 기본)
- `agents/code-quality-agent.md` — `block_reasons` 구조화 출력 추가
- `agents/code-refactoring-agent.md` — `block_reasons` 타겟 모드 추가
- `--no-retry` 옵션 — 루프 비활성화 (수동 모드)
- `--max-retry N` 옵션 — 최대 재시도 횟수 조정

---

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

### AI Confidence Score

- 모든 위반에 `confidence: 0.0 ~ 1.0` 필드 추가
- 정규식 기반 감지 → 높음(0.9+), 휴리스틱 추론 → 중간(0.7~0.9), 맥락 추론 → 낮음(0.7 미만)
- **보고서 분리:** 0.7 미만 위반은 `### 검토 권고 (낮은 확신도)` 섹션으로 분리 → False Positive 완화
- `--min-confidence 0.8` 옵션으로 임계값 조정 가능

### 프론트엔드 규칙 — `rules/frontend-rules.md`

- 현재 `.js` 확장자만 화이트리스트, 실제 규칙 없음 → 프론트엔드 전담 규칙 도입
- **XSS 방어** — `innerHTML`, `document.write`, `eval`, `new Function()` → High
- **하드코딩 API 키** — `const API_KEY = "sk-..."` → Critical
- **React 안티패턴** — `dangerouslySetInnerHTML`, 키 없는 `.map()`, `useEffect` deps 배열 누락 → Medium
- **Vue 안티패턴** — `v-html`, `mounted` 내 DOM 직접 조작 → Medium
- **TypeScript** — `any` 과다 사용, `// @ts-ignore` 남발 → Low (통계 기반)

### 규칙 충돌 감지 (Rule Conflict Detection)

- 3단 오버라이드(사용자 > 프로젝트 > Plugin) 에서 규칙끼리 모순 시 경고
- 예: 사용자가 `method.max-lines: 100`, 프로젝트가 `method.max-lines: 30` → 최종 적용 값 명시 + 출처 로그
- `/config show` 커맨드 — 현재 활성 config 전체 출처 추적 리포트

---

## v0.8.0 — 엔터프라이즈 확장

**목표:** 팀·조직 단위 운영 기능 + 외부 시스템 연동 + 컴플라이언스.

### 팀별 규칙 프로파일

- `rules/profiles/backend.md`, `frontend.md`, `security.md`, `infra.md` — 프로파일별 규칙 세트
- `quality-config.yml` 에 `profile: backend` 또는 `profile: [backend, security]` 복수 선택 지원
- 팀 디렉터리 구조 감지 시 자동 프로파일 적용 (`src/frontend/` → frontend, `src/backend/` → backend)

### 외부 시스템 연동

- **Slack 봇** — Critical 발견 시 채널 알림 (`.claude/integrations/slack.yml` 설정)
- **Jira 이슈 자동 생성** — BLOCK 발생 시 Jira 티켓 초안 생성 (사용자 승인 후 실제 생성)
- **Datadog 메트릭 export** — 일일 품질 지표 (`trend.json`) Datadog API 로 전송
- **Microsoft Teams** — Webhook 기반 알림 (Slack 과 동일 구조)

### Compliance Audit Agent — `compliance-audit-agent`

- **PII 하드코딩 감지** — 주민번호 · 전화번호 · 이메일 · 카드번호 패턴 → Critical
- **GDPR 데이터 보관 정책** — 개인정보 로깅 · DB 평문 저장 → High
- **라이선스 검증** — `pom.xml` / `build.gradle` 의존성 라이선스 (GPL 도입 시 High 경고)
- **데이터 마스킹 규칙** — `logger.info("user: {}", user)` 에서 PII 필드 포함 시 Medium
- `.compliance-report.md` → `[BLOCK: COMPLIANCE STOP]` 마커

### 문서 자동화 커맨드

- `/changelog-gen` — 최근 N 커밋 → `CHANGELOG.md` 섹션 초안 자동 생성
- `/release-notes` — 버전 태그 간 변경사항 → Release Notes 작성
- 커밋 메시지 규약(`feat:`, `fix:`, `docs:`) 기반 자동 분류

### 규칙별 WHY 문서

- `docs/rules-why/` — 규칙 코드별 배경·사례·업계 기준 문서
- `/agent-explain <CODE>` 가 이 문서를 읽어 답변
- CVE 참조 · OWASP 링크 · 실제 사고 사례 (Log4Shell, Spring4Shell 등) 포함

---

## v1.0.0 — 프로덕션 릴리스

**목표:** 안정화 + 생태계 확장.

### 다국어 문서 (i18n)

- 영문 문서 (`docs/en/`) 추가
- 마켓플레이스 영문 description

### Kotlin 지원 확장

- `.kt` 파일 화이트리스트 추가
- Kotlin 관용구 규칙 (null-safety, data class, sealed class, coroutine)

### 추가 JVM 언어 지원

- **Groovy** — `.groovy` 확장자 + Gradle 스크립트 규칙 (Spring 테스트에서 흔함)
- **Scala** — `.scala` 확장자 + Akka · Play 프레임워크 패턴
- 각 언어별 최소 기본 규칙 세트 (메서드 길이·null·하드코딩 Secret) 포함

### IntelliJ IDEA 플러그인 연동

- `.quality-report.json` → IDEA Inspection 패널에 자동 표시
- 파일·라인 이동 지원

### WebFlux / Reactive 지원

- `.kt` / Reactor 패턴 규칙 추가 (`Mono`·`Flux` 반환, `WebFilter`, `RouterFunction`)
- `@RestController` + `Mono/Flux` 반환 시 blocking 호출 감지
- `WebClient` 체인 패턴 규칙 (subscribe 없는 cold stream 경고)

### 장기 안정화

- 회귀 테스트 suite (시나리오별 더미 프로젝트로 전체 파이프라인 검증)
- SemVer 기반 Breaking Change 관리
- 커뮤니티 기여 가이드 (`CONTRIBUTING.md`)

---

## v1.1.0 — CI/CD 통합

**목표:** 외부 파이프라인 연동 (사내 CI/CD 활용도에 따라 일정 조정).

### GitHub Actions 템플릿 자동 생성

```
/generate-workflow
```

- `.github/workflows/code-quality.yml` 자동 생성
- SpotBugs + JaCoCo + OWASP Dependency-Check 포함
- PR 시 자동 파이프라인 실행

### GitLab CI/CD 템플릿 자동 생성

- `.gitlab-ci.yml` 자동 생성 (GitHub Actions 와 동일 커버리지)
- GitLab Merge Request 파이프라인 연동
- GitLab Code Quality 리포트 포맷 출력 (`gl-code-quality-report.json`)

### PR 자동 코멘트 봇

- `gh pr comment` 로 Quality / Security 보고서 요약을 PR 코멘트에 자동 게시
- BLOCK 항목은 `❌` 아이콘 + 라인 링크 포함

### Pre-push Hook 연동

- `Bash(git push:*)` 매처 추가 → push 직전에도 Security + Quality 검증
- 로컬 pre-commit 과 다른 레이어에서 이중 안전망

---

## 기여 방법

현재 개인 저장소로 운영 중입니다. 제안·피드백은 GitHub Issues 로:

- [https://github.com/eoghks/code-quality-suite/issues](https://github.com/eoghks/code-quality-suite/issues)

규칙 수정 제안은 저장소에 PR 로 보내주세요. `rules/` 파일 변경은 `BUILD-PLAN.md` 에 결정 이력을 함께 기록해야 합니다.
