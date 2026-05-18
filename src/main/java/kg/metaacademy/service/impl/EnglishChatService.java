package kg.metaacademy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kg.metaacademy.entity.Lesson;
import kg.metaacademy.exception.BadRequestException;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishChatService {

    private final LessonRepository lessonRepo;
    private final ObjectMapper     objectMapper = new ObjectMapper();

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    // Хранение истории сессий: key = "studentId:lessonId"
    private final Map<String, List<Map<String,String>>> sessions = new ConcurrentHashMap<>();
    // Счётчик вопросов AI: key = "studentId:lessonId"
    private final Map<String, Integer> questionCounter = new ConcurrentHashMap<>();

    // ── Отправить сообщение и получить ответ AI ──────────────────────────────
    public Map<String, Object> chat(Long studentId, Long lessonId, String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            throw new BadRequestException("Сообщение не может быть пустым");

        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Урок не найден"));

        String sessionKey = studentId + ":" + lessonId;
        List<Map<String,String>> history = sessions.computeIfAbsent(sessionKey, k -> new ArrayList<>());
        int qCount = questionCounter.getOrDefault(sessionKey, 0);
        int maxQuestions = lesson.getAiQuestionCount() != null ? lesson.getAiQuestionCount() : 3;

        // Добавляем сообщение студента в историю
        history.add(Map.of("role", "user", "content", userMessage));

        // Проверяем — все вопросы уже заданы?
        boolean practiceComplete = qCount >= maxQuestions;
        if (practiceComplete) {
            String finalMsg = buildCompletionMessage(lesson.getEngLevel());
            history.add(Map.of("role", "assistant", "content", finalMsg));
            return Map.of(
                "reply", finalMsg,
                "questionNumber", qCount,
                "maxQuestions", maxQuestions,
                "done", true
            );
        }

        // Строим системный промпт
        String systemPrompt = buildSystemPrompt(lesson);
        String aiReply = callOpenAi(systemPrompt, history, lesson.getEngLevel(), qCount + 1, maxQuestions);

        history.add(Map.of("role", "assistant", "content", aiReply));
        int newCount = qCount + 1;
        questionCounter.put(sessionKey, newCount);

        boolean done = newCount >= maxQuestions;
        if (done) {
            String outro = buildCompletionMessage(lesson.getEngLevel());
            history.add(Map.of("role", "assistant", "content", outro));
            return Map.of(
                "reply", aiReply + "\n\n" + outro,
                "questionNumber", newCount,
                "maxQuestions", maxQuestions,
                "done", true
            );
        }

        return Map.of(
            "reply", aiReply,
            "questionNumber", newCount,
            "maxQuestions", maxQuestions,
            "done", false
        );
    }

    // ── Получить историю ─────────────────────────────────────────────────────
    public List<Map<String, String>> getHistory(Long studentId, Long lessonId) {
        String key = studentId + ":" + lessonId;
        return sessions.getOrDefault(key, new ArrayList<>());
    }

    // ── Сбросить сессию ──────────────────────────────────────────────────────
    public void reset(Long studentId, Long lessonId) {
        String key = studentId + ":" + lessonId;
        sessions.remove(key);
        questionCounter.remove(key);
    }

    // ── Системный промпт ─────────────────────────────────────────────────────
    private String buildSystemPrompt(Lesson lesson) {
        String level = lesson.getEngLevel() != null ? lesson.getEngLevel() : "Beginner";
        String lessonTitle = lesson.getTitle() != null ? lesson.getTitle() : "English";
        String questions = lesson.getAiQuestions() != null ? lesson.getAiQuestions() : "";
        int maxQ = lesson.getAiQuestionCount() != null ? lesson.getAiQuestionCount() : 3;

        String levelDesc = switch (level) {
            case "Beginner"        -> "very simple vocabulary and short sentences. Correct mistakes gently with simple explanations.";
            case "Elementary"      -> "simple vocabulary and basic grammar. Provide friendly corrections.";
            case "Pre-Intermediate"-> "intermediate vocabulary. Correct grammar errors and explain briefly.";
            case "Intermediate"    -> "varied vocabulary and complex sentences. Give detailed grammar feedback.";
            default                -> "appropriate vocabulary for the student's level.";
        };

        String qSection = questions.isBlank() ? "" :
            "\n\nQUESTIONS THE TEACHER WANTS YOU TO ASK (use these as your basis):\n" + questions;

        return """
            You are a friendly English teacher AI assistant for a Kyrgyz online learning platform called MetaAcademy.
            
            STUDENT LEVEL: %s
            LESSON TOPIC: %s
            TOTAL QUESTIONS TO ASK: %d
            
            YOUR BEHAVIOR:
            - Use %s
            - Always respond in English (but you may add brief Russian/Kyrgyz explanations when correcting errors)
            - If the student makes a grammar mistake, FIRST acknowledge what they said, then gently correct with an example
            - Keep responses concise (2-4 sentences max for each turn)
            - Be encouraging and positive
            - Ask ONE question at a time
            - Adapt your language complexity to the student's %s level
            %s
            
            FORMAT: Always end your response with your next question (unless this is the final question).
            """.formatted(level, lessonTitle, maxQ, levelDesc, level, qSection);
    }

    // ── Вызов OpenAI ────────────────────────────────────────────────────────
    private String callOpenAi(String systemPrompt, List<Map<String,String>> history, String level, int currentQ, int maxQ) {
        // Fallback если нет API ключа
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return buildFallbackReply(level, currentQ, maxQ);
        }

        try {
            // Строим messages для API
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // Добавляем историю (без последнего user-сообщения — оно уже в history)
            for (int i = 0; i < history.size(); i++) {
                messages.add(history.get(i));
            }

            String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", "gpt-4o-mini",
                "max_tokens", 300,
                "temperature", 0.7,
                "messages", messages
            ));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI error {}: {}", response.statusCode(), response.body());
                return buildFallbackReply(level, currentQ, maxQ);
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("OpenAI chat error: {}", e.getMessage());
            return buildFallbackReply(level, currentQ, maxQ);
        }
    }

    // ── Fallback ответы (пока нет API) ───────────────────────────────────────
    private String buildFallbackReply(String level, int questionNum, int maxQ) {
        // Простые дефолтные реакции подстроенные под уровень
        String[] responses = switch (level != null ? level : "Beginner") {
            case "Beginner" -> new String[]{
                "Good job! ✓ Your answer shows you're learning well.\n\nNow tell me: what is your favorite color?",
                "Great! ✓ Keep practicing like that!\n\nNext question: how many people are in your family?",
                "Very good! ✓ I can see you understand the basics.\n\nLast question: what did you eat today?",
            };
            case "Elementary" -> new String[]{
                "Well done! ✓ Good use of vocabulary.\n\nCan you tell me about your daily routine?",
                "Nice work! ✓ Your grammar is improving.\n\nWhat do you usually do on weekends?",
                "Excellent! ✓ That's a great sentence.\n\nNow, describe your home or neighborhood.",
            };
            case "Pre-Intermediate" -> new String[]{
                "Good effort! ✓ I like your use of grammar here.\n\nCan you tell me about a memorable trip or experience?",
                "Well structured! ✓ Your vocabulary is growing.\n\nWhat are your plans for the future?",
                "Impressive! ✓ You're using complex structures well.\n\nDescribe something that has changed in your life recently.",
            };
            default -> new String[]{
                "Excellent response! ✓ Your grammar and vocabulary are at a strong intermediate level.\n\nCould you elaborate on that using a more complex structure?",
                "Very well expressed! ✓ Clear and coherent.\n\nHow do you think technology has changed communication?",
                "Outstanding! ✓ That's sophisticated use of the language.\n\nReflect on a global issue that concerns you and why.",
            };
        };

        int idx = Math.min(questionNum - 1, responses.length - 1);
        return responses[idx];
    }

    private String buildCompletionMessage(String level) {
        return switch (level != null ? level : "Beginner") {
            case "Beginner" -> "🎉 Excellent work! You've completed all the conversation practice for this lesson! Your English Beginner skills are improving. Keep it up! ⭐";
            case "Elementary" -> "🎉 Wonderful! You've finished the conversational practice! Your Elementary English is getting stronger every day. Great job! ⭐";
            case "Pre-Intermediate" -> "🎉 Fantastic! Practice complete! Your Pre-Intermediate grammar and vocabulary are developing nicely. You should be proud! ⭐";
            default -> "🎉 Outstanding performance! You've completed the Intermediate conversation practice with impressive language skills. Keep challenging yourself! ⭐";
        };
    }
}
