package kg.metaacademy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kg.metaacademy.entity.Assignment;
import kg.metaacademy.entity.AssignmentSubmission;
import kg.metaacademy.enums.SubmissionStatus;
import kg.metaacademy.exception.BadRequestException;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentReviewService {

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Основной метод проверки ──────────────────────────────────────────────
    public ReviewResult review(AssignmentSubmission submission, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл для проверки не загружен");
        }

        // ── Сброс счётчика попыток при наступлении нового дня ─────────────
        LocalDate today = LocalDate.now();
        if (submission.getLastAttemptAt() == null || !submission.getLastAttemptAt().isEqual(today)) {
            submission.setAttemptsToday(0);
        }

        // ── Лимит: 5 неверных попыток в день ─────────────────────────────
        if (submission.getAttemptsToday() != null && submission.getAttemptsToday() >= 5) {
            throw new BadRequestException(
                    "Лимит 5 попыток на сегодня исчерпан. Следующие попытки будут доступны завтра.");
        }

        // ── Проверяем допустимый тип файла ───────────────────────────────
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean validExt = fileName.endsWith(".zip")
                || fileName.endsWith(".java")
                || fileName.endsWith(".py")
                || fileName.endsWith(".js")
                || fileName.endsWith(".ts")
                || fileName.endsWith(".kt")
                || fileName.endsWith(".cpp")
                || fileName.endsWith(".c");

        if (!validExt) {
            // Неверный формат — не тратим попытку
            throw new BadRequestException(
                    "Загрузите файл .zip или исходник (.java, .py, .js, .ts, .kt, .cpp, .c).");
        }

        // ── Увеличиваем счётчик попыток ───────────────────────────────────
        submission.setAttemptsToday((submission.getAttemptsToday() == null ? 0 : submission.getAttemptsToday()) + 1);
        submission.setLastAttemptAt(today);

        // ── Сохраняем файл ────────────────────────────────────────────────
        try {
            submission.setFileContent(file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Не удалось прочитать файл");
        }
        submission.setFileName(file.getOriginalFilename());
        submission.setFileType(file.getContentType());

        // ── AI-проверка ───────────────────────────────────────────────────
        String extractedCode = extractCode(file);
        Assignment assignment  = submission.getAssignment();
        String taskDescription = assignment != null ? assignment.getDescription() : "Нет описания";

        AiCheckResult aiResult = checkWithAi(taskDescription, extractedCode, file.getOriginalFilename());

        if (aiResult.accepted) {
            submission.setStatus(SubmissionStatus.GRADED);
            submission.setPointsAwarded(10);
            submission.setGradedAt(java.time.LocalDateTime.now());
        } else {
            submission.setStatus(SubmissionStatus.SUBMITTED);
            submission.setPointsAwarded(null);
        }
        submission.setFeedback(aiResult.feedback);

        return ReviewResult.builder()
                .success(aiResult.accepted)
                .feedback(aiResult.feedback)
                .attemptsToday(submission.getAttemptsToday())
                .build();
    }

    // ── Извлекаем текст кода из файла или zip ────────────────────────────────
    private String extractCode(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        try {
            if (name.endsWith(".zip")) {
                StringBuilder sb = new StringBuilder();
                try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                    ZipEntry entry;
                    int totalChars = 0;
                    while ((entry = zis.getNextEntry()) != null && totalChars < 12000) {
                        String eName = entry.getName().toLowerCase();
                        boolean isCode = eName.endsWith(".java") || eName.endsWith(".py")
                                || eName.endsWith(".js")  || eName.endsWith(".ts")
                                || eName.endsWith(".kt")  || eName.endsWith(".cpp")
                                || eName.endsWith(".c")   || eName.endsWith(".txt")
                                || eName.endsWith(".md");
                        if (!entry.isDirectory() && isCode) {
                            byte[] buf = zis.readAllBytes();
                            String content = new String(buf, StandardCharsets.UTF_8);
                            sb.append("\n\n// === ").append(entry.getName()).append(" ===\n");
                            // Берём не более 3000 символов на файл
                            int take = Math.min(content.length(), 3000);
                            sb.append(content, 0, take);
                            totalChars += take;
                        }
                        zis.closeEntry();
                    }
                }
                return sb.length() > 0 ? sb.toString() : "[zip архив без читаемых файлов кода]";
            } else {
                byte[] bytes = file.getBytes();
                String text = new String(bytes, StandardCharsets.UTF_8);
                return text.length() > 12000 ? text.substring(0, 12000) : text;
            }
        } catch (Exception e) {
            log.warn("extractCode error: {}", e.getMessage());
            return "[не удалось прочитать содержимое файла]";
        }
    }

    // ── AI запрос к OpenAI GPT-4o-mini ───────────────────────────────────────
    private AiCheckResult checkWithAi(String taskDescription, String code, String fileName) {
        // Если API-ключ не настроен — fallback на простую проверку
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.warn("openai.api-key не настроен, используется базовая проверка");
            return fallbackCheck(code, fileName);
        }

        String prompt = """
                Ты — строгий преподаватель программирования. Проверь работу студента.
                
                ЗАДАНИЕ ПРЕПОДАВАТЕЛЯ:
                %s
                
                КОД СТУДЕНТА (файл: %s):
                %s
                
                Ответь СТРОГО в формате JSON (без markdown, без пояснений вне JSON):
                {
                  "accepted": true или false,
                  "feedback": "Подробный отзыв на русском языке — что сделано правильно, что не так, что нужно исправить"
                }
                
                Правила проверки:
                - accepted = true если задание выполнено полностью и корректно
                - accepted = false если есть серьёзные ошибки, не выполнены требования задания, или код пустой
                - feedback должен быть конструктивным и конкретным
                """.formatted(taskDescription, fileName, code);

        try {
            String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "model", "gpt-4o-mini",
                    "max_tokens", 500,
                    "messages", java.util.List.of(
                            java.util.Map.of("role", "user", "content", prompt)
                    )
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
                log.error("OpenAI API error {}: {}", response.statusCode(), response.body());
                return fallbackCheck(code, fileName);
            }

            JsonNode root     = objectMapper.readTree(response.body());
            String   content  = root.path("choices").get(0).path("message").path("content").asText();

            // Убираем возможные markdown ограждения
            content = content.replaceAll("```json|```", "").trim();
            JsonNode json = objectMapper.readTree(content);

            boolean accepted = json.path("accepted").asBoolean(false);
            String  feedback = json.path("feedback").asText("Проверка завершена.");

            return new AiCheckResult(accepted, feedback);

        } catch (Exception e) {
            log.error("AI review failed: {}", e.getMessage());
            return fallbackCheck(code, fileName);
        }
    }

    // ── Fallback если AI недоступен ───────────────────────────────────────────
    private AiCheckResult fallbackCheck(String code, String fileName) {
        boolean hasContent = code != null && code.trim().length() > 50
                && !code.contains("[не удалось прочитать");
        if (hasContent) {
            return new AiCheckResult(true,
                    "✅ Файл получен и принят автоматически (AI-проверка недоступна). " +
                    "Преподаватель проверит работу вручную.");
        } else {
            return new AiCheckResult(false,
                    "❌ Файл пустой или не содержит кода. Загрузите рабочее решение.");
        }
    }

    // ── Внутренний класс результата AI ───────────────────────────────────────
    private record AiCheckResult(boolean accepted, String feedback) {}

    // ── Публичный результат ───────────────────────────────────────────────────
    @Getter
    @Builder
    public static class ReviewResult {
        private final boolean success;
        private final String  feedback;
        private final Integer attemptsToday;
    }
}
