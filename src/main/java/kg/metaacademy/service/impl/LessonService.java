package kg.metaacademy.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import kg.metaacademy.dto.request.LessonRequest;
import kg.metaacademy.dto.request.SaveQuizRequest;
import kg.metaacademy.dto.response.*;
import kg.metaacademy.entity.*;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository       lessonRepo;
    private final CourseRepository       courseRepo;
    private final QuizQuestionRepository quizRepo;
    private final Cloudinary             cloudinary;

    // ── Создать урок ──────────────────────────────────────────────────────────
    @Transactional
    public LessonResponse create(Long courseId, LessonRequest req) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        Lesson lesson = Lesson.builder()
                .title(req.getTitle())
                .orderIndex(req.getOrderIndex())
                .practiceTask(req.getPracticeTask())
                .practiceMode(req.getPracticeMode() != null ? req.getPracticeMode() : "text")
                .engLevel(req.getEngLevel())
                .aiQuestions(req.getAiQuestions())
                .aiQuestionCount(req.getAiQuestionCount() != null ? req.getAiQuestionCount() : 3)
                .course(course)
                .build();

        return toResponse(lessonRepo.save(lesson));
    }

    // ── Уроки по уровню английского ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LessonResponse> getByEngLevel(Long courseId, String engLevel) {
        return lessonRepo.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .filter(l -> engLevel.equalsIgnoreCase(l.getEngLevel()))
                .map(this::toResponse)
                .toList();
    }

    // ── Загрузить видео ───────────────────────────────────────────────────────
    @Transactional
    public LessonResponse uploadVideo(Long lessonId, MultipartFile file) throws IOException {
        Lesson lesson = findOrThrow(lessonId);

        if (lesson.getVideoPublicId() != null) {
            cloudinary.uploader().destroy(lesson.getVideoPublicId(),
                    ObjectUtils.asMap("resource_type", "video"));
        }

        Map<?, ?> result = cloudinary.uploader().uploadLarge(
                file.getInputStream(),
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder", "metaacademy/lessons",
                        "public_id", "lesson_" + lessonId
                )
        );

        lesson.setVideoUrl((String) result.get("secure_url"));
        lesson.setVideoPublicId((String) result.get("public_id"));
        return toResponse(lessonRepo.save(lesson));
    }

    // ── Сохранить тест ────────────────────────────────────────────────────────
    @Transactional
    public LessonResponse saveQuiz(Long lessonId, List<SaveQuizRequest.Question> questions) {
        // ИСПРАВЛЕНО: сначала удаляем варианты ответов (quiz_options),
        // потом вопросы (quiz_questions) — иначе FK constraint даёт ошибку
        quizRepo.deleteOptionsByLessonId(lessonId);
        quizRepo.deleteByLessonId(lessonId);
        quizRepo.flush();

        // Загружаем урок после удаления
        Lesson lesson = findOrThrow(lessonId);

        // Создаём новые вопросы с вариантами
        List<QuizQuestion> newQuestions = new ArrayList<>();
        for (int qi = 0; qi < questions.size(); qi++) {
            SaveQuizRequest.Question q = questions.get(qi);
            QuizQuestion qq = QuizQuestion.builder()
                    .questionText(q.getQuestionText())
                    .orderIndex(q.getOrderIndex() != null ? q.getOrderIndex() : qi)
                    .lesson(lesson)
                    .build();

            List<QuizOption> options = new ArrayList<>();
            for (int oi = 0; oi < q.getOptions().size(); oi++) {
                SaveQuizRequest.Option o = q.getOptions().get(oi);
                options.add(QuizOption.builder()
                        .optionText(o.getOptionText())
                        .orderIndex(oi)
                        .isCorrect(o.getIsCorrect())
                        .question(qq)
                        .build());
            }
            qq.setOptions(options);
            newQuestions.add(qq);
        }

        quizRepo.saveAll(newQuestions);
        quizRepo.flush();

        // Перезагружаем свежий урок из БД
        return toResponse(findOrThrow(lessonId));
    }

    // ── Сохранить практику ────────────────────────────────────────────────────
    @Transactional
    public LessonResponse savePractice(Long lessonId, String task) {
        Lesson lesson = findOrThrow(lessonId);
        lesson.setPracticeTask(task);
        return toResponse(lessonRepo.save(lesson));
    }

    // ── Сохранить практику с режимом ─────────────────────────────────────────
    @Transactional
    public LessonResponse savePracticeWithMode(Long lessonId, String task, String mode) {
        Lesson lesson = findOrThrow(lessonId);
        lesson.setPracticeTask(task);
        if (mode != null && (mode.equals("text") || mode.equals("file"))) {
            lesson.setPracticeMode(mode);
        }
        return toResponse(lessonRepo.save(lesson));
    }

    // ── Сохранить AI-настройки практики ──────────────────────────────────────
    @Transactional
    public LessonResponse saveAiPractice(Long lessonId, String aiQuestions, Integer aiQuestionCount) {
        Lesson lesson = findOrThrow(lessonId);
        lesson.setAiQuestions(aiQuestions);
        if (aiQuestionCount != null && aiQuestionCount >= 1 && aiQuestionCount <= 10) {
            lesson.setAiQuestionCount(aiQuestionCount);
        }
        return toResponse(lessonRepo.save(lesson));
    }

    // ── Обновить урок ─────────────────────────────────────────────────────────
    @Transactional
    public LessonResponse update(Long lessonId, LessonRequest req) {
        Lesson lesson = findOrThrow(lessonId);
        if (req.getTitle()        != null) lesson.setTitle(req.getTitle());
        if (req.getOrderIndex()   != null) lesson.setOrderIndex(req.getOrderIndex());
        if (req.getPracticeTask() != null) lesson.setPracticeTask(req.getPracticeTask());
        if (req.getPracticeMode() != null) lesson.setPracticeMode(req.getPracticeMode());
        if (req.getEngLevel() != null)     lesson.setEngLevel(req.getEngLevel());
        if (req.getAiQuestions() != null)  lesson.setAiQuestions(req.getAiQuestions());
        if (req.getAiQuestionCount() != null) lesson.setAiQuestionCount(req.getAiQuestionCount());
        return toResponse(lessonRepo.save(lesson));
    }

    // ── Удалить урок ──────────────────────────────────────────────────────────
    @Transactional
    public void delete(Long lessonId) {
        // При удалении урока тоже сначала удаляем options потом questions
        quizRepo.deleteOptionsByLessonId(lessonId);
        quizRepo.deleteByLessonId(lessonId);
        quizRepo.flush();

        Lesson lesson = findOrThrow(lessonId);
        if (lesson.getVideoPublicId() != null) {
            try {
                cloudinary.uploader().destroy(lesson.getVideoPublicId(),
                        ObjectUtils.asMap("resource_type", "video"));
            } catch (IOException ignored) {}
        }
        lessonRepo.deleteById(lessonId);
    }

    // ── Получить уроки курса ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LessonResponse> getByCourse(Long courseId) {
        return lessonRepo.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream().map(this::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Lesson findOrThrow(Long id) {
        return lessonRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Урок не найден: " + id));
    }

    private LessonResponse toResponse(Lesson l) {
        List<QuizQuestionResponse> qr = new ArrayList<>();
        if (l.getQuizQuestions() != null) {
            for (QuizQuestion q : l.getQuizQuestions()) {
                List<QuizOptionResponse> opts = new ArrayList<>();
                if (q.getOptions() != null) {
                    for (QuizOption o : q.getOptions()) {
                        opts.add(QuizOptionResponse.builder()
                                .id(o.getId())
                                .optionText(o.getOptionText())
                                .orderIndex(o.getOrderIndex())
                                .isCorrect(o.getIsCorrect())
                                .build());
                    }
                }
                qr.add(QuizQuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .orderIndex(q.getOrderIndex())
                        .options(opts)
                        .build());
            }
        }

        return LessonResponse.builder()
                .id(l.getId())
                .title(l.getTitle())
                .orderIndex(l.getOrderIndex())
                .videoUrl(l.getVideoUrl())
                .practiceTask(l.getPracticeTask())
                .practiceMode(l.getPracticeMode() != null ? l.getPracticeMode() : "text")
                .engLevel(l.getEngLevel())
                .aiQuestions(l.getAiQuestions())
                .aiQuestionCount(l.getAiQuestionCount())
                .quizQuestions(qr)
                .build();
    }
}
