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

## Phase 4: rules/quality-rules.md ⏳

**대기 중 질문 예시:**
- SQL `${}` 경고 vs 차단?
- 테스트 커버리지 기준?
- 민감정보 키워드?
- 테스트 실행 명령?
- 성능 규칙 범위?

---

## Phase 5: agents/code-refactoring-agent.md ⏳

**대기 중 질문 예시:**
- description 문구 확정?
- 모델 선택?
- `tools` 허용 범위?
- 브랜치 네이밍 규칙?

---

## Phase 6: agents/code-quality-agent.md ⏳

**대기 중 질문 예시:**
- 실패 시 차단 vs 보고?
- 테스트 자동 실행 포함?
- 리포트 출력 형식?

---

## Phase 7: hooks & commands ⏳

**대기 중 질문 예시:**
- PreToolUse 매처 범위?
- 소스 파일 확장자 화이트리스트?
- `/run-pipeline` 인자 기본값?

---

## Phase 8: plugin.json & marketplace.json ⏳

**대기 중 질문 예시:**
- Plugin 이름·버전·라이선스·author?
- Marketplace 카테고리?

---

## Phase 9: docs/* 일괄 작성 & 최종 검증 ⏳

**대기 중 질문 예시:**
- 문서 언어?
- 루트 stub README 포함 항목?
- CHANGELOG 시작 버전?
- INSTALL / CUSTOMIZATION 분리 여부?

---

## 진행 현황 요약

| Phase | 상태 | 완료일 |
|---|---|---|
| 0. 저장소 초기화 | 🟡 진행 중 (원격 연결 대기) | - |
| 1. BUILD-PLAN 초안 | ✅ 완료 | 2026-04-21 |
| 2. shared-standards | ✅ 완료 | 2026-04-21 |
| 3. refactor-rules | ✅ 완료 | 2026-04-21 |
| 4. quality-rules | ⏳ 대기 | - |
| 5. refactoring-agent | ⏳ 대기 | - |
| 6. quality-agent | ⏳ 대기 | - |
| 7. hooks & commands | ⏳ 대기 | - |
| 8. plugin 매니페스트 | ⏳ 대기 | - |
| 9. 문서 & 최종 검증 | ⏳ 대기 | - |

> **범례:** ✅ 완료 · 🟡 진행 중 · ⏳ 대기
