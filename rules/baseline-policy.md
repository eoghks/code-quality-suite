# Baseline 정책 (baseline-policy)

> Baseline 은 **레거시 프로젝트에 Plugin 을 점진적으로 도입**하기 위한 장치입니다.
> 기존에 이미 존재하는 위반을 `.quality-baseline.json` 에 기록해 두면, Quality · Security Agent 가 신규·재발 위반만 차단하고 기존 위반은 `[BASELINE]` 태그로 분리 보고합니다.

---

## 1. `.quality-baseline.json` 포맷

프로젝트 루트에 커밋. 팀 전체 공유.

```json
{
  "version": "1",
  "created": "2026-04-21T10:00:00Z",
  "updated": "2026-04-21T10:00:00Z",
  "generator": "code-quality-suite@0.2.0",
  "violations": [
    {
      "file": "src/main/java/com/example/UserService.java",
      "line": 102,
      "code": "METHOD-LEN",
      "message": "73줄 (50줄 초과)",
      "fingerprint": "a3f8c2d1e5b9704f..."
    },
    {
      "file": "src/main/resources/mapper/LegacyMapper.xml",
      "line": 45,
      "code": "SQL-INJ",
      "message": "${legacyParam} 레거시 동적 쿼리 — 마이그레이션 예정 2026-Q3",
      "fingerprint": "b2e7a1f4c8d03592..."
    }
  ]
}
```

### 1.1 필드 정의

| 필드 | 타입 | 설명 |
|---|---|---|
| `version` | string | 포맷 버전 (`"1"` 고정) |
| `created` | ISO8601 | 최초 생성 시각 |
| `updated` | ISO8601 | 마지막 업데이트 시각 |
| `generator` | string | 생성 Plugin 버전 |
| `violations[].file` | string | 프로젝트 루트 기준 상대 경로 |
| `violations[].line` | int | 위반 시작 라인 (이동해도 fingerprint 로 추적) |
| `violations[].code` | string | 식별 코드 (`SQL-INJ`, `METHOD-LEN`, `N+1` 등) |
| `violations[].message` | string | 위반 요약 + 해제 예정 사유 권장 |
| `violations[].fingerprint` | string | SHA-256 (파일경로 + code + 정규화 메시지) 16진수 앞 16자 |

### 1.2 Fingerprint 계산

라인 번호는 제외 (리팩토링 시 라인 이동해도 안정적 매칭).

```
SHA-256( violations[].file + "|" + violations[].code + "|" + normalize(violations[].message) )
```

`normalize`: 공백 정규화, 숫자 토큰 제거 (라인 번호, 측정값 제거로 동일 위반 재감지 시 동일 fingerprint 생성).

---

## 2. Agent 동작 흐름

### 2.1 기본 모드 (Baseline 적용)

1. `.quality-baseline.json` 파일 탐색 (프로젝트 루트)
2. 존재 시 `violations` 배열 로드 → fingerprint Set 생성
3. 검증 후 위반 목록 생성
4. 각 위반의 fingerprint 계산 → Set 대조
   - **매칭 (기존 위반)** → `[BASELINE]` 태그 추가, 보고서 하단 별도 섹션 배치
   - **비매칭 (신규 위반)** → 일반 심각도 보고
5. **BLOCK 마커** 판정은 **신규 위반** Critical 기준

### 2.2 Strict 모드 (`--strict`)

- Baseline 완전 무시
- 모든 위반을 신규 취급 → 전수 검사
- BLOCK 마커 판정에 기존 위반도 포함

### 2.3 Baseline 없는 신규 프로젝트

`.quality-baseline.json` 미존재 시 모든 위반을 신규 취급 (기존 동작과 동일).

---

## 3. Baseline 관리 원칙

### 3.1 허용 기준

Baseline 에 등록 가능한 위반 유형:

| 가능 | 불가능 |
|---|---|
| `METHOD-LEN` (레거시 장문 메서드) | **Critical SQL-INJ** (주석 화이트리스트 처리 후 등록 가능) |
| `NULL-RETURN` (레거시 코드) | 테스트 실패 (`TEST-FAIL`) — 즉시 수정 필요 |
| `MAP-PARAM` (레거시 인터페이스) | Hardcoded Secret (`HARDCODED-SECRET`) |
| `N+1` (마이그레이션 예정) | RCE 위험 (`CMD-INJ`) |
| `LAYER` (레거시 아키텍처) | |

**원칙:** 보안 Critical 중 주석 없는 항목은 Baseline 에 등록해도 신규 파일에서 재발 시 반드시 차단.

### 3.2 해제 조건 명시 권장

```json
{
  "code": "SQL-INJ",
  "message": "${legacyParam} — 레거시 매퍼, 2026-Q3 #{} 마이그레이션 예정",
  "fingerprint": "..."
}
```

메시지에 **해제 예정 일정 또는 조건** 을 명시하면 `/baseline show` 로 현황 파악 용이.

### 3.3 팀 합의

- `.quality-baseline.json` 은 git 에 커밋 → PR 리뷰 대상
- 신규 위반을 Baseline 에 무분별 추가하는 행위 = **기술 부채 은닉** → 리뷰어 주의

---

## 4. `/baseline` 커맨드 동작 (요약)

| 서브 | 설명 |
|---|---|
| `/baseline create` | 현재 모든 위반 스캔 → `.quality-baseline.json` 생성 (최초 도입 시) |
| `/baseline update` | 해결된 fingerprint 자동 제거 + `updated` 타임스탬프 갱신 |
| `/baseline show` | 카테고리별 건수, 해제 예정 항목, 최고령 항목 출력 |

상세 사용법: `docs/BASELINE.md` 참조.

---

## 5. 보고서 표시 형식

```
## Quality Report — feature/user-login

### Critical (신규 — 차단)
- [SQL-INJ] LegacyMapper.xml:88 — ${newParam} 직접 치환 (신규 추가됨)

### Baseline (기존 위반 — 비차단)
- [BASELINE][SQL-INJ] LegacyMapper.xml:45 — ${legacyParam} (등록일: 2026-04-21)
- [BASELINE][METHOD-LEN] UserService.java:102 — 73줄
- [BASELINE][N+1] OrderService.java:78 — lazy 루프

### 요약
- 신규 Critical: 1건 (BLOCK)
- Baseline 제외: 3건

[BLOCK: COMMIT STOP]
```
