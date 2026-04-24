# CONFIG — quality-config.yml 설정 가이드

> `.claude/quality-config.yml` 한 파일로 code-quality-suite 의 주요 임계값을 팀별로 커스터마이징합니다.
> `.md` 오버라이드보다 진입 장벽이 낮고, 비개발자도 수치만 수정하면 됩니다.

---

## 파일 위치

| 위치 | 적용 범위 | 우선순위 |
|---|---|---|
| `~/.claude/quality-config.yml` | 사용자 전역 (모든 프로젝트) | 최고 |
| `<project>/.claude/quality-config.yml` | 해당 프로젝트만 | 중간 |
| 플러그인 기본값 (하드코딩) | 미설정 항목 기본 | 최저 |

우선순위 높은 설정이 낮은 설정의 동일 키를 **덮어씁니다**.

---

## 전체 스키마

```yaml
# code-quality-suite 규칙 임계값 오버라이드
# 미설정 항목은 플러그인 기본값 사용

refactor:
  method.max-lines: 50          # 메서드 최대 줄 수 (기본 50)
  cc.threshold: 10              # Cyclomatic Complexity 임계값 (기본 10)
  cognitive.threshold: 15       # Cognitive Complexity 임계값 (기본 15)
  params.max: 3                 # 메서드 파라미터 최대 개수 (기본 3)
  fields.max: 10                # 클래스 필드 최대 개수 (기본 10)
  class.max-lines: 400          # 클래스 최대 줄 수 (기본 400)

quality:
  jacoco.threshold: 80          # JaCoCo 라인 커버리지 임계값 (기본 80)
  jacoco.skip: false            # JaCoCo 검사 건너뜀 여부 (기본 false)

architecture:
  layer.strict: true            # 레이어 의존 엄격 모드 (기본 true)
  pkg.allowed-extra: []         # 추가 허용 패키지명 목록 (기본 빈 배열)
```

---

## 키 상세 설명

### refactor 섹션

| 키 | 기본값 | 설명 |
|---|---|---|
| `method.max-lines` | `50` | 이 줄 수를 초과하는 메서드는 Medium 위반. Refactor Agent 가 분리 제안 |
| `cc.threshold` | `10` | Cyclomatic Complexity 이 이 값을 초과하면 Medium (11 이상 High) |
| `cognitive.threshold` | `15` | Cognitive Complexity 임계값 |
| `params.max` | `3` | 파라미터 개수가 이 값을 초과하면 Medium |
| `fields.max` | `10` | 클래스 필드 개수가 이 값을 초과하면 God Class 후보 (High) |
| `class.max-lines` | `400` | 클래스 전체 줄 수 임계값 |

### quality 섹션

| 키 | 기본값 | 설명 |
|---|---|---|
| `jacoco.threshold` | `80` | 변경 메서드 기준 JaCoCo 라인 커버리지 %. 미달 시 High |
| `jacoco.skip` | `false` | `true` 설정 시 JaCoCo 커버리지 검사 건너뜀 (CI 환경 등) |

### architecture 섹션

| 키 | 기본값 | 설명 |
|---|---|---|
| `layer.strict` | `true` | `false` 시 레이어 위반을 High → Medium 으로 완화 |
| `pkg.allowed-extra` | `[]` | 기본 허용 패키지 목록 외 추가 허용할 패키지 세그먼트 (예: `["infra", "adapter"]`) |

---

## 사용 예시

### 스타트업 — 빠른 개발 주기 (느슨한 기준)

```yaml
refactor:
  method.max-lines: 80
  cc.threshold: 15
  params.max: 5

quality:
  jacoco.threshold: 60

architecture:
  layer.strict: false
```

### 엔터프라이즈 — 엄격한 품질 기준

```yaml
refactor:
  method.max-lines: 30
  cc.threshold: 7
  params.max: 2
  fields.max: 7
  class.max-lines: 250

quality:
  jacoco.threshold: 90

architecture:
  layer.strict: true
  pkg.allowed-extra: ["adapter", "port", "infra"]
```

### Hexagonal Architecture 프로젝트

```yaml
architecture:
  layer.strict: true
  pkg.allowed-extra:
    - adapter
    - port
    - infra
    - application
    - usecase
```

---

## 로드 순서 (Agent 동작)

각 Agent 시작 시:

```bash
# 1. 플러그인 기본값 (하드코딩)
# 2. 프로젝트 설정 (있으면 덮어씀)
cat "${CLAUDE_PROJECT_DIR}/.claude/quality-config.yml" 2>/dev/null || true
# 3. 사용자 전역 설정 (있으면 덮어씀)
cat "${HOME}/.claude/quality-config.yml" 2>/dev/null || true
```

YAML 파싱 후 각 임계값을 내부 변수로 설정. 미설정 키는 플러그인 기본값 유지.

---

## 참고

- 규칙 파일 오버라이드: `~/.claude/rules/*.md` / `<project>/.claude/rules/*.md`
- @suppress 인라인 억제: `rules/suppress-policy.md`
- Baseline 관리: `/baseline create` · `/baseline show`
