import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * PR diff를 가져와 Claude로 한국어 코드 리뷰를 수행하고
 * GitHub PR에 코멘트로 게시합니다.
 *
 * 의존성: Java 11+ 표준 라이브러리만 사용 (외부 라이브러리 불필요)
 * 실행: javac ReviewPR.java && java ReviewPR
 */
public class ReviewPR {

    private static final String GITHUB_TOKEN    = requireEnv("GITHUB_TOKEN");
    private static final String ANTHROPIC_KEY   = requireEnv("ANTHROPIC_API_KEY");
    private static final String PR_NUMBER       = requireEnv("PR_NUMBER");
    private static final String REPO            = requireEnv("REPO");
    private static final String PR_TITLE        = getEnv("PR_TITLE", "");
    private static final String PR_BODY         = getEnv("PR_BODY", "(없음)");

    private static final int MAX_DIFF_CHARS = 80_000;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public static void main(String[] args) throws Exception {
        System.out.printf("PR #%s 리뷰 시작 (repo: %s)%n", PR_NUMBER, REPO);

        String diff = fetchDiff();
        if (diff == null || diff.isBlank()) {
            System.out.println("diff가 비어 있어 리뷰를 건너뜁니다.");
            return;
        }

        System.out.printf("diff 크기: %,d 문자%n", diff.length());
        String comment = reviewWithClaude(diff);
        postComment(comment);
        System.out.println("✅ 리뷰 코멘트 게시 완료");
    }

    // ── GitHub: diff 조회 ──────────────────────────────────────────────────

    private static String fetchDiff() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + REPO + "/pulls/" + PR_NUMBER))
                .header("Authorization", "token " + GITHUB_TOKEN)
                .header("Accept", "application/vnd.github.v3.diff")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertOk(res, "GitHub diff 조회");
        return res.body();
    }

    // ── GitHub: 코멘트 게시 ────────────────────────────────────────────────

    private static void postComment(String body) throws Exception {
        String json = "{\"body\":" + toJsonString(body) + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + REPO + "/issues/" + PR_NUMBER + "/comments"))
                .header("Authorization", "token " + GITHUB_TOKEN)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertOk(res, "GitHub 코멘트 게시");
    }

    // ── Claude: 코드 리뷰 ─────────────────────────────────────────────────

    private static String reviewWithClaude(String diff) throws Exception {
        boolean truncated = diff.length() > MAX_DIFF_CHARS;
        if (truncated) diff = diff.substring(0, MAX_DIFF_CHARS);

        String prompt = buildPrompt(diff, truncated);
        String reqBody = "{" +
                "\"model\":\"claude-sonnet-4-6\"," +
                "\"max_tokens\":4096," +
                "\"messages\":[{\"role\":\"user\",\"content\":" + toJsonString(prompt) + "}]" +
                "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("x-api-key", ANTHROPIC_KEY)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(reqBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertOk(res, "Claude API 호출");

        String reviewText = extractText(res.body());
        return "## 🤖 Claude 코드 리뷰\n\n" + reviewText
                + "\n\n---\n*[Claude Sonnet 4.6](https://anthropic.com) 자동 리뷰*";
    }

    private static String buildPrompt(String diff, boolean truncated) {
        String notice = truncated
                ? "\n> ⚠️ diff가 너무 커서 일부만 포함되었습니다. 전체 리뷰가 아닐 수 있습니다.\n"
                : "";
        return "당신은 숙련된 백엔드 개발자이자 코드 리뷰어입니다.\n"
                + "아래 Pull Request를 한국어로 리뷰해주세요.\n\n"
                + "## PR 정보\n"
                + "- 제목: " + PR_TITLE + "\n"
                + "- 설명: " + PR_BODY + "\n"
                + notice + "\n"
                + "## Diff\n```\n" + diff + "\n```\n\n---\n\n"
                + "다음 형식으로 리뷰해주세요. 해당하는 섹션만 포함하고 없으면 생략합니다.\n\n"
                + "### 🔴 버그 / 심각한 문제\n"
                + "(잠재적 런타임 오류, 데이터 유실, 보안 취약점 등)\n\n"
                + "### 🟠 성능 / 설계 / 보안\n"
                + "(N+1 쿼리, 경쟁 조건, 불필요한 중복, 잘못된 트랜잭션 등)\n\n"
                + "### 🟡 마이너 개선사항\n"
                + "(컨벤션, 네이밍, 사소한 코드 스타일)\n\n"
                + "### 잘 된 부분\n"
                + "(명확한 설계 결정, 좋은 패턴 등)\n\n"
                + "### 총평\n"
                + "(전반적인 평가와 머지 가능 여부)\n\n"
                + "각 지적사항에는 파일명과 관련 코드를 포함해주세요.";
    }

    // ── JSON 유틸 ─────────────────────────────────────────────────────────

    /**
     * Anthropic API 응답에서 content[0].text 값을 추출합니다.
     * 표준 라이브러리만 쓰기 위해 문자열 파싱으로 처리합니다.
     */
    private static String extractText(String json) {
        String marker = "\"text\":\"";
        int start = json.indexOf(marker);
        if (start == -1) throw new RuntimeException("응답에서 text 필드를 찾을 수 없습니다: " + json);
        start += marker.length();

        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> { sb.append('\\'); sb.append(next); }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    /** 문자열을 JSON 문자열 리터럴로 변환합니다. */
    private static String toJsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append("\"").toString();
    }

    // ── 공통 헬퍼 ────────────────────────────────────────────────────────

    private static void assertOk(HttpResponse<String> res, String label) {
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new RuntimeException(label + " 실패 (" + res.statusCode() + "): " + res.body());
        }
    }

    private static String requireEnv(String name) {
        String val = System.getenv(name);
        if (val == null || val.isBlank()) throw new RuntimeException("환경변수 필요: " + name);
        return val;
    }

    private static String getEnv(String name, String defaultVal) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val : defaultVal;
    }
}
