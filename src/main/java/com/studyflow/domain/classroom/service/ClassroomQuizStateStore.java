package com.studyflow.domain.classroom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.global.redis.RedisStateLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 강의실 실시간 문제풀이 상태 저장소.
 *
 * <p>세션 중에만 필요한 퀴즈이므로 DB 영속화 대신 Redis JSON으로 보관한다.
 * 늦게 들어온 학생은 REST 스냅샷으로 현재 문제/제출 여부/종료 결과를 복원하고,
 * 실시간 시작·제출·종료 이벤트는 WebSocket으로 동기화한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassroomQuizStateStore {

    private static final Duration TTL = Duration.ofHours(12);
    private static final int MIN_DURATION_SEC = 5;
    private static final int MAX_DURATION_SEC = 60 * 30;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisStateLock lock;

    private String key(Long sessionId) {
        return "classroom-quiz:" + sessionId;
    }

    private String lockName(Long sessionId) {
        return "classroom-quiz-lock:" + sessionId;
    }

    public Map<String, Object> start(Long sessionId, Long teacherUserId, String question, String answer, Integer durationSec) {
        String normalizedQuestion = requiredText(question, "문제는 필수입니다.");
        String normalizedAnswer = requiredText(answer, "정답은 필수입니다.");
        int seconds = normalizeDuration(durationSec);
        long now = System.currentTimeMillis();

        return lock.withLock(lockName(sessionId), () -> {
            SessionQuizState state = load(sessionId);
            Quiz prev = currentQuiz(state);
            int nextSeq = (prev.quizId == null) ? 1 : prev.sequence + 1;

            Quiz quiz = new Quiz();
            quiz.quizId = UUID.randomUUID().toString();
            quiz.sequence = nextSeq;
            quiz.teacherUserId = teacherUserId;
            quiz.question = normalizedQuestion;
            quiz.answer = normalizedAnswer;
            quiz.durationSec = seconds;
            quiz.startedAtMs = now;
            quiz.endsAtMs = now + seconds * 1000L;

            state.quizzes.add(quiz);
            save(sessionId, state);
            return publicSnapshot(quiz, now);
        });
    }

    public Map<String, Object> submit(Long sessionId, Long studentUserId, String quizId, String answer) {
        return lock.withLock(lockName(sessionId), () -> {
            SessionQuizState state = load(sessionId);
            Quiz quiz = findQuiz(state, quizId);
            long now = System.currentTimeMillis();
            if (quiz == null) {
                throw new IllegalArgumentException("진행 중인 문제가 아닙니다.");
            }
            if (now > quiz.endsAtMs) {
                throw new IllegalArgumentException("제출 시간이 종료되었습니다.");
            }
            if (quiz.submissions.containsKey(String.valueOf(studentUserId))) {
                throw new IllegalStateException("이미 답을 제출했습니다.");
            }

            String submittedAnswer = requiredText(answer, "답안은 필수입니다.");
            Submission submission = new Submission();
            submission.userId = studentUserId;
            submission.answer = submittedAnswer;
            submission.correct = isCorrect(submittedAnswer, quiz.answer);
            submission.submittedAtMs = now;
            quiz.submissions.put(String.valueOf(studentUserId), submission);
            save(sessionId, state);
            return userSubmissionSnapshot(quiz, submission, now, false);
        });
    }

    public Map<String, Object> snapshotFor(Long sessionId, Long userId, boolean host) {
        SessionQuizState state = load(sessionId);
        Quiz current = currentQuiz(state);
        long now = System.currentTimeMillis();
        
        Map<String, Object> out = new LinkedHashMap<>();
        if (current.quizId == null) {
            out.put("type", "idle");
            out.put("serverNowMs", now);
        } else {
            out.putAll(host ? hostSnapshot(current, now) : publicSnapshot(current, now));
            Submission submission = current.submissions.get(String.valueOf(userId));
            if (submission != null) {
                out.put("mySubmission", userSubmissionSnapshot(current, submission, now, isEnded(current, now)));
            }
            if (isEnded(current, now)) {
                out.put("type", "ended");
                out.put("answer", current.answer);
                if (submission == null && !host) {
                    out.put("mySubmission", Map.of(
                            "submitted", false,
                            "correct", false,
                            "message", "조금 더 분발하세요"
                    ));
                }
            }
        }

        java.util.List<Map<String, Object>> history = new java.util.ArrayList<>();
        for (int i = 0; i < state.quizzes.size(); i++) {
            Quiz q = state.quizzes.get(i);
            if (Objects.equals(q.quizId, current.quizId) && !isEnded(current, now)) continue;
            Map<String, Object> h = host ? hostSnapshot(q, now) : publicSnapshot(q, now);
            h.put("type", "ended");
            h.put("answer", q.answer);
            Submission sub = q.submissions.get(String.valueOf(userId));
            if (sub != null) {
                h.put("mySubmission", userSubmissionSnapshot(q, sub, now, true));
            } else if (!host) {
                h.put("mySubmission", Map.of(
                        "submitted", false,
                        "correct", false,
                        "message", "제출 안 함"
                ));
            }
            history.add(h);
        }
        out.put("history", history);
        return out;
    }

    public Map<String, Object> end(Long sessionId) {
        return lock.withLock(lockName(sessionId), () -> {
            SessionQuizState state = load(sessionId);
            Quiz quiz = currentQuiz(state);
            if (quiz.quizId == null) {
                return Map.of("type", "idle", "serverNowMs", System.currentTimeMillis());
            }
            long now = System.currentTimeMillis();
            if (quiz.endsAtMs > now) {
                quiz.endsAtMs = now;
                save(sessionId, state);
            }
            return endedPublicSnapshot(quiz, now);
        });
    }

    public Map<String, Object> endIfSame(Long sessionId, String quizId) {
        return lock.withLock(lockName(sessionId), () -> {
            SessionQuizState state = load(sessionId);
            Quiz quiz = findQuiz(state, quizId);
            if (quiz == null) {
                return null;
            }
            long now = System.currentTimeMillis();
            if (quiz.endsAtMs > now) {
                quiz.endsAtMs = now;
                save(sessionId, state);
            }
            return endedPublicSnapshot(quiz, now);
        });
    }

    public Map<String, Object> toggleCorrect(Long sessionId, String quizId, Long studentUserId) {
        return lock.withLock(lockName(sessionId), () -> {
            SessionQuizState state = load(sessionId);
            Quiz quiz = findQuiz(state, quizId);
            if (quiz == null) {
                throw new IllegalArgumentException("진행 중인 문제가 아닙니다.");
            }
            Submission submission = quiz.submissions.get(String.valueOf(studentUserId));
            if (submission == null) {
                throw new IllegalArgumentException("해당 학생의 제출 내역이 없습니다.");
            }
            submission.correct = !submission.correct;
            save(sessionId, state);
            return Map.of("type", "submissionUpdate", "quizId", quiz.quizId, "serverNowMs", System.currentTimeMillis());
        });
    }

    public void clear(Long sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private Map<String, Object> publicSnapshot(Quiz quiz, long now) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", isEnded(quiz, now) ? "ended" : "started");
        out.put("quizId", quiz.quizId);
        out.put("sequence", quiz.sequence);
        out.put("question", quiz.question);
        out.put("durationSec", quiz.durationSec);
        out.put("startedAtMs", quiz.startedAtMs);
        out.put("endsAtMs", quiz.endsAtMs);
        out.put("serverNowMs", now);
        out.put("submissionCount", quiz.submissions.size());
        return out;
    }

    private Map<String, Object> hostSnapshot(Quiz quiz, long now) {
        Map<String, Object> out = publicSnapshot(quiz, now);
        out.put("answer", quiz.answer);
        out.put("correctCount", quiz.submissions.values().stream().filter(s -> s.correct).count());
        out.put("wrongCount", quiz.submissions.values().stream().filter(s -> !s.correct).count());
        
        java.util.List<Map<String, Object>> list = quiz.submissions.values().stream()
                .map(s -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("userId", s.userId);
                    detail.put("answer", s.answer);
                    detail.put("correct", s.correct);
                    detail.put("submittedAtMs", s.submittedAtMs);
                    return detail;
                })
                .toList();
        out.put("submissionsList", list);
        
        return out;
    }

    private Map<String, Object> endedPublicSnapshot(Quiz quiz, long now) {
        Map<String, Object> out = publicSnapshot(quiz, now);
        out.put("type", "ended");
        out.put("answer", quiz.answer);
        out.put("correctCount", quiz.submissions.values().stream().filter(s -> s.correct).count());
        out.put("wrongCount", quiz.submissions.values().stream().filter(s -> !s.correct).count());
        return out;
    }

    private Map<String, Object> userSubmissionSnapshot(Quiz quiz, Submission submission, long now, boolean revealResult) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("submitted", true);
        out.put("answer", submission.answer);
        out.put("submittedAtMs", submission.submittedAtMs);
        out.put("serverNowMs", now);
        if (revealResult || isEnded(quiz, now)) {
            out.put("correct", submission.correct);
            out.put("message", submission.correct ? "축하합니다. 맞았습니다!" : "조금 더 분발하세요");
        }
        return out;
    }

    private boolean isEnded(Quiz quiz, long now) {
        return quiz.endsAtMs > 0 && now >= quiz.endsAtMs;
    }

    private void save(Long sessionId, SessionQuizState state) {
        try {
            redisTemplate.opsForValue().set(key(sessionId), objectMapper.writeValueAsString(toMap(state)), TTL);
        } catch (Exception e) {
            log.warn("[classroom-quiz] 상태 저장 실패(sessionId={})", sessionId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private SessionQuizState load(Long sessionId) {
        String json = redisTemplate.opsForValue().get(key(sessionId));
        if (json == null) return new SessionQuizState();
        try {
            return fromMap(objectMapper.readValue(json, Map.class));
        } catch (Exception e) {
            log.warn("[classroom-quiz] 상태 역직렬화 실패(sessionId={})", sessionId, e);
            return new SessionQuizState();
        }
    }

    private Map<String, Object> toMap(SessionQuizState state) {
        Map<String, Object> out = new LinkedHashMap<>();
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (Quiz q : state.quizzes) {
            list.add(toMap(q));
        }
        out.put("quizzes", list);
        return out;
    }

    @SuppressWarnings("unchecked")
    private SessionQuizState fromMap(Map<String, Object> map) {
        SessionQuizState state = new SessionQuizState();
        if (map == null) return state;
        Object rawQuizzes = map.get("quizzes");
        if (rawQuizzes instanceof java.util.List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    state.quizzes.add(quizFromMap((Map<String, Object>) m));
                }
            }
        }
        return state;
    }

    private Quiz currentQuiz(SessionQuizState state) {
        return state.quizzes.isEmpty() ? new Quiz() : state.quizzes.get(state.quizzes.size() - 1);
    }

    private Quiz findQuiz(SessionQuizState state, String quizId) {
        for (int i = state.quizzes.size() - 1; i >= 0; i--) {
            Quiz q = state.quizzes.get(i);
            if (Objects.equals(q.quizId, quizId)) return q;
        }
        return null;
    }

    private Map<String, Object> toMap(Quiz quiz) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("quizId", quiz.quizId);
        out.put("sequence", quiz.sequence);
        out.put("teacherUserId", quiz.teacherUserId);
        out.put("question", quiz.question);
        out.put("answer", quiz.answer);
        out.put("durationSec", quiz.durationSec);
        out.put("startedAtMs", quiz.startedAtMs);
        out.put("endsAtMs", quiz.endsAtMs);
        Map<String, Object> submissions = new LinkedHashMap<>();
        quiz.submissions.forEach((key, value) -> submissions.put(key, toMap(value)));
        out.put("submissions", submissions);
        return out;
    }

    @SuppressWarnings("unchecked")
    private Quiz quizFromMap(Map<String, Object> map) {
        Quiz quiz = new Quiz();
        quiz.quizId = str(map.get("quizId"));
        quiz.sequence = intVal(map.get("sequence"));
        quiz.teacherUserId = longVal(map.get("teacherUserId"));
        quiz.question = str(map.get("question"));
        quiz.answer = str(map.get("answer"));
        quiz.durationSec = intVal(map.get("durationSec"));
        quiz.startedAtMs = longOrZero(map.get("startedAtMs"));
        quiz.endsAtMs = longOrZero(map.get("endsAtMs"));
        Object rawSubmissions = map.get("submissions");
        if (rawSubmissions instanceof Map<?, ?> submissions) {
            submissions.forEach((key, value) -> {
                if (key != null && value instanceof Map<?, ?> item) {
                    quiz.submissions.put(String.valueOf(key), submissionFromMap((Map<String, Object>) item));
                }
            });
        }
        return quiz;
    }

    private Map<String, Object> toMap(Submission submission) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", submission.userId);
        out.put("answer", submission.answer);
        out.put("correct", submission.correct);
        out.put("submittedAtMs", submission.submittedAtMs);
        return out;
    }

    private Submission submissionFromMap(Map<String, Object> map) {
        Submission submission = new Submission();
        submission.userId = longVal(map.get("userId"));
        submission.answer = str(map.get("answer"));
        submission.correct = boolVal(map.get("correct"));
        submission.submittedAtMs = longOrZero(map.get("submittedAtMs"));
        return submission;
    }

    private static boolean isCorrect(String submitted, String answer) {
        return normalizeAnswer(submitted).equals(normalizeAnswer(answer));
    }

    private static String normalizeAnswer(String value) {
        return Normalizer.normalize(Objects.requireNonNullElse(value, ""), Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private static String requiredText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static int normalizeDuration(Integer durationSec) {
        int value = durationSec == null ? 60 : durationSec;
        if (value < MIN_DURATION_SEC || value > MAX_DURATION_SEC) {
            throw new IllegalArgumentException("풀이 시간은 5초 이상 1800초 이하로 설정할 수 있습니다.");
        }
        return value;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longVal(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return null;
        try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private static long longOrZero(Object value) {
        Long parsed = longVal(value);
        return parsed != null ? parsed : 0L;
    }

    private static int intVal(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value == null) return 0;
        try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return 0; }
    }

    private static boolean boolVal(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    static final class SessionQuizState {
        final java.util.List<Quiz> quizzes = new java.util.ArrayList<>();
    }

    static final class Quiz {
        String quizId;
        int sequence;
        Long teacherUserId;
        String question;
        String answer;
        int durationSec;
        long startedAtMs;
        long endsAtMs;
        final Map<String, Submission> submissions = new LinkedHashMap<>();
    }

    static final class Submission {
        Long userId;
        String answer;
        boolean correct;
        long submittedAtMs;
    }
}
