#!/usr/bin/env bash
#
# pre-commit-pipeline.sh
#
# PreToolUse Hook for `Bash(git commit:*)` — code-quality-suite plugin
#
# 목적:
#   1. 스테이징된 변경 파일 중 화이트리스트 확장자가 있으면
#      "Refactor Agent → Quality Agent 파이프라인 실행" 힌트를 컨텍스트에 주입
#   2. 이전 Quality 보고서(`.quality-report.md`)에 [BLOCK: COMMIT STOP] 마커가
#      있으면 exit 2 로 커밋 중단 신호 반환
#
# 동작 원칙:
#   - Subagent 를 직접 호출하지 않음 (Hook 제약)
#   - stdout 출력은 메인 Claude 컨텍스트에 주입됨
#   - exit 0 = 진행, exit 2 = 커밋 중단 신호
#
# 범위: 힌트 주입 레이어. 최종 판단은 메인 Claude / 개발자.

set -u

# -------------------------------------------------------------------
# 1. 이전 Quality 보고서 BLOCK 마커 확인
# -------------------------------------------------------------------
REPORT_FILE="${CLAUDE_PROJECT_DIR:-$PWD}/.quality-report.md"

if [ -f "$REPORT_FILE" ]; then
    LAST_LINE=$(tail -n 1 "$REPORT_FILE" | tr -d '[:space:]')
    if [ "$LAST_LINE" = "[BLOCK:COMMITSTOP]" ]; then
        echo "[code-quality-suite] ❌ Quality Agent 보고서에 Critical 위반이 있습니다."
        echo "[code-quality-suite] .quality-report.md 를 확인하고 해결 후 재커밋하세요."
        echo "[code-quality-suite] 무시하려면 보고서를 삭제하거나 재실행해 [PASS: COMMIT READY] 로 갱신하세요."
        exit 2
    fi
fi

# -------------------------------------------------------------------
# 2. 스테이징된 변경 파일 수집 + 화이트리스트 필터
# -------------------------------------------------------------------
STAGED=$(git diff --cached --name-only 2>/dev/null || true)

if [ -z "$STAGED" ]; then
    # 스테이징된 변경이 없으면 힌트 없이 통과
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
    # 대상 확장자 없음 → 파이프라인 권고 안 함
    exit 0
fi

# -------------------------------------------------------------------
# 3. 힌트 메시지 컨텍스트 주입
# -------------------------------------------------------------------
cat <<EOF
[code-quality-suite] 커밋 직전 파이프라인 권고

스테이징된 변경에 자바/프론트엔드/빌드 관련 파일이 포함되어 있습니다:
$(echo -e "$MATCHED" | head -n 20)

권장 순서 (커밋 실행 전 수행):
  1. /agent code-refactoring-agent  → 구조 개선 + 테스트 동시 갱신 + 아토믹 커밋
  2. /agent code-quality-agent      → 보안·규칙·테스트·성능 검증 보고서 생성
  3. 보고서 [PASS: COMMIT READY] 확인 후 커밋

수동 실행: /run-pipeline
EOF

exit 0
