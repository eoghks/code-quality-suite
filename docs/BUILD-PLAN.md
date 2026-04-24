# BUILD-PLAN — code-quality-suite 구축 이력

> 이 문서는 Plugin 구축 과정의 **Q&A 결정 이력**을 Phase 별로 누적 기록합니다.
> 각 Phase 완료 시 결정된 항목·근거·산출물을 append 합니다.

**구축 목표:** Claude Code 용 Refactor + Quality 전문 Agent 를 묶은 Plugin 배포

**기본 정보:**
- Plugin 이름: `code-quality-suite`
- 저장소: https://github.com/eoghks/code-quality-suite
- 시작일: 2026-04-21
- 저자: daehwan (yjchoi@rathontech.com)

---

## Phase 0: 저장소 초기화 ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 작업 디렉터리 | `C:\Users\daehwan\Desktop\claudWorkSpace\agent\code-quality-suite` | agent 작업 공간 하위 통일 |
| GitHub 계정 | `eoghks` | 사용자 지정 |
| 저장소명 | `code-quality-suite` | Plugin 이름과 일치 |
| Visibility | Public | 공개 공유 |
| 인증 방식 | HTTPS + PAT | SSH 키 분리 이슈 회피 |
| 기본 브랜치 | `main` | GitHub 기본값 |
| 원격 URL | `https://github.com/eoghks/code-quality-suite.git` | - |
| `.gitignore` | Plugin 친화형 | OS/편집기/로컬 파일 포괄 |

**산출물:**
- [x] 디렉터리 스켈레톤 생성 (`.claude-plugin/`, `agents/`, `hooks/`, `rules/`, `commands/`, `docs/`)
- [x] 루트 `README.md` stub (GitHub 첫 화면용)
- [x] `.gitignore` 작성
- [x] `git init -b main` + 초기 커밋 (`52904e3`)
- [ ] GitHub 웹에서 빈 저장소 수동 생성 (사용자 작업 대기)
- [ ] `git remote add origin` + `git push -u origin main`

---

## Phase 1: BUILD-PLAN 초안 🔄

**결정 사항:**
- 문서 위치: `docs/BUILD-PLAN.md` (루트 아님)
- 기록 방식: Phase 별 Q&A 결과 + 체크리스트 누적 append
- 업데이트 시점: 각 Phase 완료 직후

---

## Phase 2: rules/shared-standards.md (공통 표준) ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 언어·FW 스택 | Java + Jakarta EE + Spring Boot | 전역 CLAUDE.md 와 일치 |
| Java 버전 | 17 이상 권장 | LTS |
| Lombok 범위 | 핵심만 (`@Getter`, `@Setter`, `@Slf4j`) | 명시성 유지, `@Data` 금지 |
| 레이어 패턴 | **조건부** — MyBatis: `Controller→Service→Dao` / JPA: `Controller→Service→Repository` | Stack 별 관용 준수 |
| 데이터 응답 | `ResponseEntity` 사용 | Spring 관용 |
| 주석 언어 | 한국어 권장 (식별자는 영어) | 전역 정책 |
| 커밋 메시지 | 한국어 본문 + 영어 타입 prefix | `feat:`, `fix:` 등 관용 병행 |

**금지 패턴 명시:**
- `javax.*` / `@Data` / `@EqualsAndHashCode` 무분별 사용
- Controller 내 비즈니스 로직 / Service 에서 DB 직접 접근
- null 반환 / `Map<String, Object>` 인자·반환 / SQL `${}` 치환

**산출물:**
- [x] `rules/shared-standards.md` 작성

---

## Phase 3: rules/refactor-rules.md ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 메서드 최대 줄 수 | **50줄** | Claude 추천 (Spring+MyBatis/JPA 현실적 균형점) |
| 테스트 줄 수 제한 | 동일 적용 (50줄) | 테스트도 복잡도 관리 |
| null 대체 | 단일→`Optional`, 컨테이너→빈 컨테이너, 배열→길이 0 배열 | 타입별 명확 구분 |
| `Map<String,Object>` | 최대한 금지, DTO/VO 강제. 불가피 시 주석 필수 | 타입 안전성·가독성 |
| 매직넘버 | `0`, `1`, `-1` 제외 전부 상수화 | 관용적 값만 예외 |
| 설계 패턴 | **최대한 활용** — Strategy / Factory / Builder / Template Method 우선 + Facade/Adapter/Decorator/Observer 권장 | 사용자 지시 "디자인 패턴 최대한 활용" |
| 중복 제거 | 3회 이상 → 메서드 추출, 2개 클래스 이상 → 공통 유틸 | DRY |
| 네이밍 | 동사+명사, is/has/can 접두사, 약어 금지 | 가독성 |
| 테스트 동시 갱신 | Refactor Agent 책임 | CLAUDE.md 규칙 |
| 브랜치·커밋 | `feature/*` · `bugfix/*` · `refactor/*` 강제, 아토믹 커밋 | 조직 규칙 반영 |

**산출물:**
- [x] `rules/refactor-rules.md` 작성

---

## Phase 4: rules/quality-rules.md ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| SQL `${}` 대응 | **차단 (Critical)** | SQL Injection 심각도 최상, 조건부 주석 명시 시만 허용 |
| 테스트 커버리지 범위 | **신규/변경 메서드만** | 레거시 부담 없는 실용적 검증, diff 기반 |
| 민감정보 키워드 | 기본 3종(`password`/`token`/`secret`) + 인증 파생(`apiKey`/`accessKey`/`privateKey`/`credential`/`authorization`/`bearer`) + PII(`ssn`/`jumin`/`주민등록번호`/`phone`/`연락처`/`email`) | 국내 개인정보보호법 + API 인증 맥락 |
| 테스트 실행 명령 | **자동 감지** — `pom.xml` → `mvn test`, `build.gradle` → `./gradlew test` | 두 빌드 도구 혼용 환경 지원 |
| 성능 규칙 범위 | N+1 쿼리 + 인덱스 힌트 + 반복 문자열 연산 + **로거 파라미터 방식** + **`findAll()` 후 필터링 안티패턴** | 정적 분석 가능한 실용적 항목만 |

**심각도 체계:**
- **Critical** (차단 권고) — SQL Injection, 테스트 실패
- **High** (검토 강력 권고) — 레이어 위반, 민감정보 노출, N+1, `findAll()` 필터
- **Medium** (다음 리팩토링) — 50줄 초과, null 반환, 로거 방식, 반복 문자열
- **Low** (참고) — 인덱스 힌트, 미확정 Lombok

**산출물:**
- [x] `rules/quality-rules.md` 작성 (보안 · 규칙 준수 · 테스트 커버리지 · 성능 · 보고 형식 · 권한 범위)

---

## Phase 5: agents/code-refactoring-agent.md ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 모델 | `claude-sonnet-4-6` | 빠르고 저렴, 일반 리팩토링에 충분 |
| 툴 권한 | **풀 권한** — Read/Grep/Glob + Edit/Write + git add/commit/checkout -b/status/diff/branch/log + mvn·gradle test | 반자동 리팩토링·테스트 실행·커밋 자동화 |
| 브랜치 전략 | **자동 감지 + 생성** — feature/bugfix/refactor 구분 후 자동 분기, main/master 직접 커밋 차단 | 조직 규칙 자동 준수 |
| description 톤 | **적극적 위임 트리거** — "자바 코드 작업 직후 호출" 문구로 메인 Claude 자동 위임 유도 | 개발자 호출 누락 방지 |
| 규칙 로드 순서 | Plugin → 프로젝트 → 사용자 (뒤가 앞을 보완) | 3단 오버라이드 준수 |

**금지 사항 명시:**
- main/master 직접 커밋, `git push`, `git push --force`, `git reset --hard`, `git branch -D`, `--no-verify`, `--amend`(push 된 커밋)
- 테스트 실패 상태 커밋 종료
- 한 커밋에 기능+리팩토링+스타일 혼합

**산출물:**
- [x] `agents/code-refactoring-agent.md` 작성 (규칙 로드 · 브랜치 자동화 · 리팩토링 절차 · 테스트 병행 · 아토믹 커밋 · 완료 보고 · 금지 사항)

---

## Phase 6: agents/code-quality-agent.md ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 모델 | `claude-sonnet-4-6` | Refactor 와 동일, 비용 일관성 |
| 테스트 실행 | **자동 실행** — pom/gradle 감지 후 `mvn test` 또는 `./gradlew test` | 실제 실패 감지 필수 |
| 실패 시 동작 | **Critical 만 자동 차단 (exit 2)** | SQL Injection/테스트 실패는 보안 사고 직결, High 이하는 개발자 재량 |
| 리포트 형식 | **체크리스트 + 심각도 그룹화** (Critical/High/Medium/Low) + 식별 코드 `[SQL-INJ]` · `[LAYER]` 등 | 사람 가독성 + 파싱 용이 |
| 자동 차단 구현 | Agent 는 보고서 마지막 줄에 `[BLOCK: COMMIT STOP]` 또는 `[PASS: COMMIT READY]` 마커 출력 → Hook 스크립트(Phase 7)가 파싱해 exit 2 반환 | Agent 권한 제약 우회 (프로세스 exit code 제어 불가) |
| 권한 | 읽기 전용 — Read/Grep/Glob + git diff/log/status/branch + mvn·gradle test | Edit/Write/git add/commit 금지 |

**`quality-rules.md` 동기화:**
- 5.3 절 "차단하지 않음" → "BLOCK 마커 + Hook exit 2 구조" 로 개정

**산출물:**
- [x] `agents/code-quality-agent.md` 작성 (규칙 로드 · diff 기반 검증 · 5축 검증 · BLOCK 마커 · 권한 제약 · 호출 맥락별 동작)
- [x] `rules/quality-rules.md` 5.3 절 업데이트

---

## Phase 7: hooks & commands ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| Hook 매처 | `Bash(git commit:*)` 직전만 | 가장 흔한 진입점, 단순·이해 쉬움 |
| 화이트리스트 | `.java`, `.xml`, `.jsp`, `.js`, `.css`, `.html`, `pom.xml`, `build.gradle`, `build.gradle.kts` | 자바 + 프론트엔드 + 빌드 스크립트 |
| Hook 엄격도 | **힌트 주입 + BLOCK 마커 감지 시 exit 2** | 유연함 + Critical 자동 차단 |
| `/run-pipeline` 인자 | 없으면 현재 브랜치 전체, 파일/브랜치 지정 가능 | 중간 점검부터 타겟 검증까지 |
| BLOCK 연동 | `.quality-report.md` 마지막 줄 `[BLOCK: COMMIT STOP]` 감지 → exit 2 | Agent exit code 제약 우회, 이전 Agent 결과 재사용 |

**산출물:**
- [x] `hooks/hooks.json` — PreToolUse 매처 설정 (timeout 5s)
- [x] `hooks/pre-commit-pipeline.sh` — 화이트리스트 필터 + 파이프라인 권고 + BLOCK 감지
- [x] `commands/run-pipeline.md` — 슬래시 커맨드 (인자 규칙 + 4단계 실행 절차 + 요약 템플릿)

---

## Phase 8: plugin.json & marketplace.json ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 이름 | `code-quality-suite` | Phase 0 결정, 저장소명 일치 |
| 버전 | **0.1.0** | SemVer 초기, 검증 단계 |
| 라이선스 | **MIT** | 가장 허용적, 공유 친화적 |
| author | `daehwan` (이메일 미포함) | 개인정보 보호, GitHub 계정 `eoghks` 로 연결 |
| 카테고리 | `development-tools`, `java-tooling` | 개발 도구 + 자바 필터링 노출 |
| 키워드 | java, spring-boot, jakarta, refactoring, code-quality, code-review, sql-injection, mybatis, jpa, lombok | 검색 최적화 |
| components | agents(2) + hooks(1) + commands(1) 경로 명시 | Plugin 로더가 자동 인식 |
| compatibility | `claude-code: >=1.0.0` | 최소 호환 버전 명시 |

**산출물:**
- [x] `.claude-plugin/plugin.json` — Plugin 매니페스트 (Agent/Hook/Command 컴포넌트 선언)
- [x] `.claude-plugin/marketplace.json` — 마켓플레이스 등록 (카테고리·태그·문서 링크)

---

## Phase 9: docs/* 일괄 작성 & 최종 검증 ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 문서 언어 | **한국어 단독** | BUILD-PLAN·rules 와 일관, 국내 팀 타겟 |
| 문서 분리 | README / INSTALL / CUSTOMIZATION / CHANGELOG 4개 | 도달 속도 최적화 |
| CHANGELOG 시작 | 0.0.1 → 0.0.4 단계적 + 0.1.0 첫 정식 릴리즈 | Phase 별 이력 보존 |
| 루트 README | GitHub stub (1줄 소개 + 설치 커맨드 + docs/ 링크) | GitHub 첫 화면 최적 |
| 데모 수준 | 기본 사용법 예제 + 보고서 샘플 | 실질적 도움 |

**산출물:**
- [x] `docs/README.md` — 전체 가이드 (구성 · 설치 · 사용법 · 보고서 샘플 · 커스터마이징 · 조직 규칙 · 지원 스택)
- [x] `docs/INSTALL.md` — 설치·검증·업데이트·제거·문제 해결·환경별 주의사항·회사 GitLab 공존
- [x] `docs/CUSTOMIZATION.md` — 3단 오버라이드 · 실전 예시 5종 (메서드 줄 수 / Lombok / 민감정보 / 레이어 / 원복)
- [x] `docs/CHANGELOG.md` — 0.0.1~0.0.4 개발 마일스톤 + 0.1.0 정식 릴리즈 + 예정 항목

---

## Phase 10: v0.2.0 Hardening Release ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| 리팩토링 줄 수 | 50줄 유지 | 사용자 확인 |
| 리팩토링 강화 축 | 메트릭(CC/Cognitive/파라미터/필드) + 런타임 안전(예외/Resource) + 응집도(클래스 크기/LoD) | 사용자 선택 3축 |
| 보안 검증 | security-audit-agent 신설 (OWASP Top 10) | 사용자 선택 |
| 외부 툴 | SpotBugs + JaCoCo 통합 | 사용자 선택 |
| 레거시 대응 | Baseline 도입 | 사용자 선택 |

**산출물:**
- [x] `rules/refactor-rules.md` — §10(메트릭) · §11(런타임 안전) · §12(응집도) 추가
- [x] `rules/security-rules.md` 신설 — OWASP A01~A10 + Cookie/XXE/PathTraversal + Secret regex + 취약 의존성
- [x] `rules/baseline-policy.md` 신설 — fingerprint 포맷, 등록 원칙, 보고 형식
- [x] `rules/quality-rules.md` — 보안 분리, §4A SpotBugs/JaCoCo 파싱 추가
- [x] `agents/security-audit-agent.md` 신설 — OWASP 전담, 읽기 전용, `.security-report.md` 출력
- [x] `agents/code-quality-agent.md` — 보안 최소 스모크 체크, §3.6 SpotBugs/JaCoCo, --strict 맥락
- [x] `commands/security-scan.md` 신설 — `/security-scan [target]`
- [x] `commands/baseline.md` 신설 — `/baseline create|update|show`
- [x] `commands/run-pipeline.md` — 3-stage, `--strict` 옵션
- [x] `hooks/pre-commit-pipeline.sh` — `.security-report.md` BLOCK 마커 감지 추가
- [x] `.claude-plugin/plugin.json` · `marketplace.json` — v0.2.0
- [x] `docs/BASELINE.md` 신설
- [x] `docs/CHANGELOG.md` — [0.2.0] 섹션
- [x] `docs/README.md` — 3 Agent + 3 Command, 보고서 샘플 갱신
- [x] `docs/INSTALL.md` — §8 SpotBugs/JaCoCo 설정 절
- [x] `docs/BUILD-PLAN.md` — Phase 10 추가 (이 항목)
- [x] `README.md` (루트) — v0.2.0 소개 업데이트

---

## 진행 현황 요약

| Phase | 상태 | 완료일 |
|---|---|---|
| 0. 저장소 초기화 | ✅ 완료 (원격 push 완료) | 2026-04-21 |
| 1. BUILD-PLAN 초안 | ✅ 완료 | 2026-04-21 |
| 2. shared-standards | ✅ 완료 | 2026-04-21 |
| 3. refactor-rules | ✅ 완료 | 2026-04-21 |
| 4. quality-rules | ✅ 완료 | 2026-04-21 |
| 5. refactoring-agent | ✅ 완료 | 2026-04-21 |
| 6. quality-agent | ✅ 완료 | 2026-04-21 |
| 7. hooks & commands | ✅ 완료 | 2026-04-21 |
| 8. plugin 매니페스트 | ✅ 완료 | 2026-04-21 |
| 9. 문서 & 최종 검증 | ✅ 완료 (v0.1.0) | 2026-04-21 |
| 10. v0.2.0 Hardening Release | ✅ 완료 | 2026-04-21 |
| 11. v0.3.0 Defensive Design + Architecture | ✅ 완료 | 2026-04-24 |

> **범례:** ✅ 완료 · 🟡 진행 중 · ⏳ 대기

---

## Phase 11: v0.3.0 Defensive Design + Architecture Release ✅

**결정 사항:**

| 항목 | 값 | 근거 |
|---|---|---|
| architecture-review-agent 파이프라인 포함 여부 | `--full` 옵션 전용 (기본 제외) | 매 커밋 import 전수 분석은 사용성 저하 |
| Immutability 규칙 | §13 추가 (final·record·방어적 복사·불변 컬렉션) | v0.3.0 로드맵 항목 |
| Guard Clause 규칙 | §14 추가 (중첩 if 3레벨·else 제거) | v0.3.0 로드맵 항목 |
| security-rules.md 분리 | 인덱스 + security/ 5개 카테고리 파일 | 토큰 최적화 — 파일 유형별 선택 로드 |
| 시나리오 테스트 | test/scenarios/ 8개 더미 파일 | v0.2.0 미검증 항목 보완 |

**산출물:**
- [x] `agents/architecture-review-agent.md` 신설 — 레이어/순환의존/DDD 검증, `.architecture-report.md` 출력
- [x] `rules/architecture-rules.md` 신설 — ARCH-LAYER/ARCH-CYCLE/ARCH-DDD/ARCH-PKG/ARCH-HEX
- [x] `rules/refactor-rules.md` — §13(Immutability) · §14(Guard Clause) 추가
- [x] `rules/security-rules.md` — 인덱스 파일로 축소 재작성
- [x] `rules/security/injection.md` · `crypto.md` · `access-control.md` · `deserialization.md` · `misc.md` 신설
- [x] `agents/security-audit-agent.md` — 카테고리 선택 로드 전략 반영
- [x] `commands/architecture-review.md` 신설 — `/architecture-review [target] [--full] [--strict]`
- [x] `commands/run-pipeline.md` — `--full` 옵션 추가 (4-stage)
- [x] `hooks/pre-commit-pipeline.sh` — `.architecture-report.md` BLOCK 마커 체크 추가, v0.3.0
- [x] `.claude-plugin/plugin.json` · `marketplace.json` — v0.3.0
- [x] `test/scenarios/` — BadService · GodClass · MutableDto · NestedIfController · VulnerableMapper.xml · InsecureService · LayerViolation · EntityExposed
- [x] `docs/SCENARIOS.md` 신설 — 시나리오 검증 가이드
- [x] `docs/CHANGELOG.md` — [0.3.0] 섹션
- [x] `docs/README.md` — 4 Agent + 4 Command 표 갱신
- [x] `docs/BUILD-PLAN.md` — Phase 11 추가 (이 항목)
- [x] `docs/ROADMAP.md` — v0.3.0 ✅ 릴리즈 마킹

---

## Phase 12: v0.4.0 — 테스트·DB·억제·DX 개선 ✅

**목표:** 테스트 생성 자동화 + DB 마이그레이션 안전성 + @suppress 인라인 억제 + 아키텍처 규칙 확장 + YAML 임계값 설정.

| 항목 | 값 | 근거 |
|---|---|---|
| test-generation-agent 구현 | JUnit 5 + Mockito 스켈레톤 자동 생성 | v0.4.0 로드맵 항목 |
| db-migration-agent 구현 | Flyway/Liquibase 정적 분석, 읽기 전용 | v0.4.0 로드맵 항목 |
| @suppress 인라인 억제 | 직전 1~2 라인 주석, 사유 필수 (없으면 Medium) | Baseline 보다 가벼운 단건 억제 수단 |
| ARCH-TX-01/02 규칙 | Controller @Transactional → Medium | 레이어 책임 명확화 |
| ARCH-SEC-01/02/03 규칙 | anyRequest 누락 High, permitAll 과다/httpBasic Medium | Spring Security 설정 강화 |
| YAML config 로드 | .claude/quality-config.yml 3단 오버라이드 | .md 오버라이드보다 낮은 진입 장벽 |
| --with-tests 파이프라인 | Refactor → Test Gen → Security → Quality | 신규 메서드 자동 테스트 생성 포함 |
| migration-report BLOCK hook | .migration-report.md 체크를 Hook 1번 섹션에 추가 | DB 변경 안전 커밋 강제 |

**산출물:**
- [x] `agents/test-generation-agent.md` 신설
- [x] `agents/db-migration-agent.md` 신설
- [x] `rules/migration-rules.md` 신설 — MIG-DROP/ALTER-NULL/ALTER-DEF/INDEX/NO-UNDO/VERSION/ENCODING
- [x] `rules/suppress-policy.md` 신설 — @suppress 문법·위치·동작 규칙·지원 코드 목록
- [x] `rules/architecture-rules.md` — §9 @Transactional + §10 Spring Security 추가
- [x] `commands/generate-tests.md` 신설 — `/generate-tests [target] [--dry-run] [--no-run]`
- [x] `commands/db-check.md` 신설 — `/db-check [target] [--strict] [--version-only]`
- [x] `commands/run-pipeline.md` — `--with-tests` 옵션 추가
- [x] `agents/security-audit-agent.md` — @suppress 로직 추가 (§5)
- [x] `agents/architecture-review-agent.md` — @suppress 로직 추가 (§5)
- [x] `agents/code-quality-agent.md` — @suppress 로직 추가 (§3-B) + YAML config 로드 (§1.1)
- [x] `hooks/pre-commit-pipeline.sh` — .migration-report.md BLOCK 체크 추가 (v0.4.0, 섹션 1)
- [x] `docs/CONFIG.md` 신설 — quality-config.yml 스키마 + 사용 예시
- [x] `test/scenarios/BadMigration.sql` 신설 — DB 마이그레이션 위반 시나리오
- [x] `.claude-plugin/plugin.json` · `marketplace.json` — v0.4.0, 6 Agent, 6 Command
- [x] `docs/CHANGELOG.md` — [0.4.0] 섹션
- [x] `docs/README.md` — 6 Agent + 6 Command 표 갱신
- [x] `docs/BUILD-PLAN.md` — Phase 12 추가 (이 항목)
- [x] `docs/ROADMAP.md` — v0.4.0 ✅ 릴리즈 마킹
