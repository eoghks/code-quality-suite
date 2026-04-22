#!/usr/bin/env bash
#
# pre-commit-pipeline.sh
#
# PreToolUse Hook for `Bash(git commit:*)` — code-quality-suite plugin v0.2.0
#
# 목적:
#   1. 스테이징된 변경 파일 중 화이트리스트 확장자가 있으면
#      "Refactor → Security → Quality 3-stage 파이프라인" 힌트를 컨텍스트에 주입
#   2. Security 보고서(.security-report.md)에 [BLOCK: SECURITY STOP] 마커 → exit 2
#   3. Quality 보고서(.quality-report.md)에 [BLOCK: COMMIT STOP] 마커 → exit 2
#
# 동작 원칙:
#   - Subagent 를 직접 호출하지 않음 (Hook 제약상 불가)
#   - stdout 출력은 메인 Claude 컨텍스트에 주입됨
#   - exit 0 = 진행, exit 2 = 커밋 중단 신호 (Claude 가 git commit 취소)
#
# v0.2.0 변경점:
#   - Security 보고서(.security-report.md) BLOCK 마커 체크 추가
#   - 힌트 메시지에 security-audit-agent 3단계 포함

set -u

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$PWD}"

# -------------------------------------------------------------------
# 1. Security 보고서 BLOCK 마커 확인 (v0.2.0 신규)
# -------------------------------------------------------------------
SECURITY_REPORT="${PROJECT_DIR}/.security-report.md"

if [ -f "$SECURITY_REPORT" ]; then
    SECURITY_LAST=$(tail -n 1 "$SECURITY_REPORT" | tr -d '[:space:]')
    if [ "$SECURITY_LAST" = "[BLOCK:SECURITYSTOP]" ]; then
        echo "[code-quality-suite] ❌ Security Agent 보고서에 Critical 보안 취약점이 있습니다."
        echo "[code-quality-suite] .security-report.md 를 확인하고 취약점 수정 후 재커밋하세요."
        echo "[code-quality-suite] 재스캔: /security-scan"
        exit 2
    fi
fi

# -------------------------------------------------------------------
# 2. Quality 보고서 BLOCK 마커 확인
# -------------------------------------------------------------------
QUALITY_REPORT="${PROJECT_DIR}/.quality-report.md"

if [ -f "$QUALITY_REPORT" ]; then
    QUALITY_LAST=$(tail -n 1 "$QUALITY_REPORT" | tr -d '[:space:]')
    if [ "$QUALITY_LAST" = "[BLOCK:COMMITSTOP]" ]; then
        echo "[code-quality-suite] ❌ Quality Agent 보고서에 Critical 위반이 있습니다."
        echo "[code-quality-suite] .quality-report.md 를 확인하고 해결 후 재커밋하세요."
        echo "[code-quality-suite] 재실행: /run-pipeline"
        exit 2
    fi
fi

# -------------------------------------------------------------------
# 3. 스테이징된 변경 파일 수집 + 화이트리스트 필터
# -------------------------------------------------------------------
STAGED=$(git diff --cached --name-only 2>/dev/null || true)

if [ -z "$STAGED" ]; then
    exit 0
fi

# 화이트리스트: .java .xml .jsp .js .css .html + pom.xml · build.gradle·kts
MATCHED=""
while IFS= read -r FILE; do
    case "$FILE" in
        *.java|*.xml|*.jsp|*.js|*.css|*.html)
            MATCHED="${MATCHED}${FILE}\n"
            ;;
        *pom.xml|*build.gradle|*build.gradle.kts)
            MATCHED="${MATCHED}${FILE}\n"
            ;;
    esac
done <<< "$STAGED"

if [ -z "$MATCHED" ]; then
    exit 0
fi

# -------------------------------------------------------------------
# 4. 힌트 메시지 컨텍스트 주입 (3-stage)
# -------------------------------------------------------------------
cat <<'EOF'
[code-quality-suite] 커밋 직전 3-stage 파이프라인 권고

스테이징된 변경에 자바/프론트엔드/빌드 관련 파일이 포함되어 있습니다.

권장 순서 (커밋 전 수행):
  1. /agent code-refactoring-agent   → 구조 개선 + 테스트 동시 갱신 + 아토믹 커밋
  2. /agent security-audit-agent     → OWASP Top 10 보안 취약점 스캔 → .security-report.md
  3. /agent code-quality-agent       → 규칙·테스트·성능·SpotBugs/JaCoCo 검증 → .quality-report.md
  4. 두 보고서 모두 PASS 확인 후 커밋

전체 파이프라인 한 번에: /run-pipeline
보안만 빠르게:           /security-scan
EOF

echo ""
echo "[code-quality-suite] 변경 파일 (최대 20개):"
echo -e "$MATCHED" | head -n 20

exit 0
