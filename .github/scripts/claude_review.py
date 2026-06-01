"""
PR diff를 가져와 Claude로 한국어 코드 리뷰를 수행하고
GitHub PR에 코멘트로 게시합니다.
"""

import os
import sys
import requests
import anthropic

GITHUB_TOKEN = os.environ["GITHUB_TOKEN"]
ANTHROPIC_API_KEY = os.environ["ANTHROPIC_API_KEY"]
PR_NUMBER = os.environ["PR_NUMBER"]
REPO = os.environ["REPO"]
PR_TITLE = os.environ.get("PR_TITLE", "")
PR_BODY = os.environ.get("PR_BODY", "")

# Claude에 전달할 diff 최대 길이 (컨텍스트 절약)
MAX_DIFF_CHARS = 80_000


def get_pr_diff() -> str:
    url = f"https://api.github.com/repos/{REPO}/pulls/{PR_NUMBER}"
    res = requests.get(
        url,
        headers={
            "Authorization": f"token {GITHUB_TOKEN}",
            "Accept": "application/vnd.github.v3.diff",
        },
        timeout=30,
    )
    res.raise_for_status()
    return res.text


def post_comment(body: str) -> None:
    url = f"https://api.github.com/repos/{REPO}/issues/{PR_NUMBER}/comments"
    res = requests.post(
        url,
        json={"body": body},
        headers={
            "Authorization": f"token {GITHUB_TOKEN}",
            "Accept": "application/vnd.github.v3+json",
        },
        timeout=30,
    )
    res.raise_for_status()


def build_prompt(diff: str, truncated: bool) -> str:
    truncation_notice = (
        "\n> ⚠️ diff가 너무 커서 일부만 포함되었습니다. 전체 리뷰가 아닐 수 있습니다.\n"
        if truncated
        else ""
    )

    return f"""당신은 숙련된 백엔드 개발자이자 코드 리뷰어입니다.
아래 Pull Request를 한국어로 리뷰해주세요.

## PR 정보
- 제목: {PR_TITLE}
- 설명: {PR_BODY or "(없음)"}
{truncation_notice}
## Diff
```
{diff}
```

---

다음 형식으로 리뷰해주세요. 해당하는 섹션만 포함하고 없으면 생략합니다.

### 🔴 버그 / 심각한 문제
(잠재적 런타임 오류, 데이터 유실, 보안 취약점 등)

### 🟠 성능 / 설계 / 보안
(N+1 쿼리, 경쟁 조건, 불필요한 중복, 잘못된 트랜잭션 등)

### 🟡 마이너 개선사항
(컨벤션, 네이밍, 사소한 코드 스타일)

### 잘 된 부분
(명확한 설계 결정, 좋은 패턴 등)

### 총평
(전반적인 평가와 머지 가능 여부)

각 지적사항에는 파일명과 관련 코드를 포함해주세요."""


def review_with_claude(diff: str) -> str:
    truncated = len(diff) > MAX_DIFF_CHARS
    if truncated:
        diff = diff[:MAX_DIFF_CHARS]

    client = anthropic.Anthropic(api_key=ANTHROPIC_API_KEY)
    message = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=4096,
        messages=[{"role": "user", "content": build_prompt(diff, truncated)}],
    )

    review_body = message.content[0].text
    return (
        "## 🤖 Claude 코드 리뷰\n\n"
        + review_body
        + "\n\n---\n*[Claude Sonnet 4.6](https://anthropic.com) 자동 리뷰*"
    )


def main() -> None:
    print(f"PR #{PR_NUMBER} 리뷰 시작 (repo: {REPO})")

    diff = get_pr_diff()
    if not diff.strip():
        print("diff가 비어 있어 리뷰를 건너뜁니다.")
        return

    print(f"diff 크기: {len(diff):,} 문자")
    comment = review_with_claude(diff)
    post_comment(comment)
    print("✅ 리뷰 코멘트 게시 완료")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"❌ 오류 발생: {e}", file=sys.stderr)
        sys.exit(1)
