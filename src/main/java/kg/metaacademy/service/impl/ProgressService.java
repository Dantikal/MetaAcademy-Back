package kg.metaacademy.service.impl;

import kg.metaacademy.dto.request.QuizSubmitRequest;
import kg.metaacademy.dto.response.LessonResponse;
import kg.metaacademy.dto.response.QuizOptionResponse;
import kg.metaacademy.dto.response.QuizQuestionResponse;
import kg.metaacademy.dto.response.QuizResultResponse;
import kg.metaacademy.entity.*;
import kg.metaacademy.exception.AccessDeniedException;
import kg.metaacademy.exception.BadRequestException;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final LessonProgressRepository progressRepo;
    private final LessonRepository         lessonRepo;
    private final UserRepository           userRepo;
    private final EnrollmentService        enrollmentService;
    private final AssignmentReviewService  reviewService;

    @Value("${gamification.points.lesson-attempt-1:10}")
    private int pts1;
    @Value("${gamification.points.lesson-attempt-2:5}")
    private int pts2;
    @Value("${gamification.points.lesson-attempt-3:3}")
    private int pts3;

    // ── Отметить видео просмотренным ──────────────────────────────────────────
    @Transactional
    public void markVideoWatched(Long studentId, Long lessonId) {
        Lesson lesson = findLesson(lessonId);
        checkEnrolled(studentId, lesson.getCourse().getId());

        LessonProgress p = getOrCreate(studentId, lesson);
        p.setVideoWatched(true);
        saveProgress(p);
    }

    // ── Сдать тест ────────────────────────────────────────────────────────────
    @Transactional
    public QuizResultResponse submitQuiz(Long studentId, QuizSubmitRequest req) {
        Lesson lesson = findLesson(req.getLessonId());
        // Мягкая проверка — только существование записи, не статус доступа
        checkEnrolled(studentId, lesson.getCourse().getId());

        LessonProgress p = getOrCreate(studentId, lesson);

        // Если тест уже пройден — просто возвращаем результат
        if (Boolean.TRUE.equals(p.getQuizPassed())) {
            return QuizResultResponse.builder()
                    .passed(true)
                    .correctCount(lesson.getQuizQuestions().size())
                    .totalCount(lesson.getQuizQuestions().size())
                    .attempt(p.getQuizAttempts())
                    .pointsEarned(0)
                    .build();
        }

        List<QuizQuestion> questions = lesson.getQuizQuestions();
        long correct = questions.stream().filter(q -> {
            Long selectedId = req.getAnswers().get(q.getId());
            if (selectedId == null) return false;
            return q.getOptions().stream()
                    .anyMatch(o -> o.getId().equals(selectedId) && Boolean.TRUE.equals(o.getIsCorrect()));
        }).count();

        p.setQuizAttempts(p.getQuizAttempts() + 1);
        boolean passed = correct == questions.size();

        int pointsEarned = 0;
        if (passed) {
            p.setQuizPassed(true);
            pointsEarned = calcPoints(p.getQuizAttempts());
            p.setPointsEarned(pointsEarned);

            User student = userRepo.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
            student.setPoints(student.getPoints() + pointsEarned);
            userRepo.save(student);

            tryCompleteLesson(p);
        }

        saveProgress(p);

        return QuizResultResponse.builder()
                .passed(passed)
                .correctCount((int) correct)
                .totalCount(questions.size())
                .attempt(p.getQuizAttempts())
                .pointsEarned(passed ? pointsEarned : 0)
                .build();
    }

    // ── Завершить практику ────────────────────────────────────────────────────
    @Transactional
    public void completePractice(Long studentId, Long lessonId) {
        Lesson lesson = findLesson(lessonId);
        checkEnrolled(studentId, lesson.getCourse().getId());

        LessonProgress p = getOrCreate(studentId, lesson);

        // Тест проверяется на фронтенде, не блокируем практику
        p.setQuizPassed(true);  // гарантируем что тест помечен
        p.setPracticeDone(true);
        tryCompleteLesson(p);
        saveProgress(p);

        long completedCount = progressRepo.countCompleted(studentId, lesson.getCourse().getId());
        enrollmentService.updateProgress(studentId, lesson.getCourse().getId(), (int) completedCount);
    }

    // ── Сдать практику файлом (режим "file") ─────────────────────────────────
    @Transactional
    public Map<String, Object> submitPracticeFile(Long studentId, Long lessonId, MultipartFile file) {
        Lesson lesson = findLesson(lessonId);
        checkAccess(studentId, lesson.getCourse().getId());

        if (!Boolean.TRUE.equals(progressRepo
                .findByStudentIdAndLessonId(studentId, lessonId)
                .map(LessonProgress::getQuizPassed).orElse(false))) {
            throw new BadRequestException("Сначала пройди тест");
        }

        LessonProgress p = getOrCreate(studentId, lesson);

        // ── Лимит попыток в день (5 неверных) ────────────────────────────
        LocalDate today = LocalDate.now();
        if (p.getPracticeLastAttemptAt() == null || !p.getPracticeLastAttemptAt().isEqual(today)) {
            p.setPracticeAttemptsToday(0);
        }
        if (p.getPracticeAttemptsToday() != null && p.getPracticeAttemptsToday() >= 5) {
            throw new BadRequestException(
                    "Лимит 5 попыток на сегодня исчерпан. Следующие попытки будут доступны завтра.");
        }

        // ── Используем AI-проверку из AssignmentReviewService ────────────
        // Создаём временный объект submission для проверки
        AssignmentSubmission tempSub = new AssignmentSubmission();
        tempSub.setAttemptsToday(p.getPracticeAttemptsToday());
        tempSub.setLastAttemptAt(p.getPracticeLastAttemptAt());
        // Создаём фиктивный Assignment чтобы передать описание задачи
        Assignment fakeAssignment = new Assignment();
        fakeAssignment.setDescription(lesson.getPracticeTask() != null ? lesson.getPracticeTask() : "");
        tempSub.setAssignment(fakeAssignment);

        AssignmentReviewService.ReviewResult result = reviewService.review(tempSub, file);

        // Синхронизируем счётчики попыток обратно в LessonProgress
        p.setPracticeAttemptsToday(tempSub.getAttemptsToday());
        p.setPracticeLastAttemptAt(tempSub.getLastAttemptAt());
        p.setPracticeFeedback(result.getFeedback());

        Map<String, Object> response = new HashMap<>();
        response.put("accepted", result.isSuccess());
        response.put("feedback", result.getFeedback());
        response.put("attemptsToday", result.getAttemptsToday());
        response.put("attemptsLeft", Math.max(0, 5 - (result.getAttemptsToday() != null ? result.getAttemptsToday() : 0)));

        if (result.isSuccess()) {
            p.setPracticeDone(true);
            tryCompleteLesson(p);
            response.put("lessonCompleted", true);

            // Начисляем баллы студенту (+10 за файловую практику)
            User student = userRepo.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
            student.setPoints(student.getPoints() + 10);
            userRepo.save(student);
            response.put("pointsEarned", 10);

            long completedCount = progressRepo.countCompleted(studentId, lesson.getCourse().getId());
            enrollmentService.updateProgress(studentId, lesson.getCourse().getId(), (int) completedCount);
        } else {
            response.put("lessonCompleted", false);
            response.put("pointsEarned", 0);
        }

        saveProgress(p);
        return response;
    }

    // ── Получить прогресс по уроку ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public LessonResponse getLessonWithProgress(Long studentId, Long lessonId) {
        Lesson lesson = findLesson(lessonId);
        LessonProgress p = progressRepo
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(null);
        return toLessonResponse(lesson, p);
    }

    // ── Список уроков курса с прогрессом ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LessonResponse> getCourseWithProgress(Long studentId, Long courseId) {
        List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<LessonProgress> progresses = progressRepo
                .findByStudentIdAndLessonCourseId(studentId, courseId);
        Map<Long, LessonProgress> pMap = progresses.stream()
                .collect(Collectors.toMap(lp -> lp.getLesson().getId(), lp -> lp));
        return lessons.stream()
                .map(l -> toLessonResponse(l, pMap.get(l.getId())))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void tryCompleteLesson(LessonProgress p) {
        // Урок завершён если пройден тест и практика (видео опционально)
        boolean quizOk     = Boolean.TRUE.equals(p.getQuizPassed());
        boolean practiceOk = Boolean.TRUE.equals(p.getPracticeDone());
        if (quizOk && practiceOk) {
            p.setCompleted(true);
        }
    }

    private int calcPoints(int attempts) {
        if (attempts == 1) return pts1;
        if (attempts == 2) return pts2;
        return pts3;
    }

    // ИСПРАВЛЕНО: если запись уже существует (гонка запросов или повторный вызов)
    // ловим DataIntegrityViolationException и перечитываем из БД
    private LessonProgress getOrCreate(Long studentId, Lesson lesson) {
        return progressRepo.findByStudentIdAndLessonId(studentId, lesson.getId())
                .orElseGet(() -> {
                    try {
                        LessonProgress fresh = LessonProgress.builder()
                                .student(userRepo.getReferenceById(studentId))
                                .lesson(lesson)
                                .build();
                        return progressRepo.saveAndFlush(fresh);
                    } catch (DataIntegrityViolationException e) {
                        // Гонка: другой поток уже вставил запись — просто читаем её
                        return progressRepo.findByStudentIdAndLessonId(studentId, lesson.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Прогресс не найден после конфликта вставки"));
                    }
                });
    }

    // ИСПРАВЛЕНО: используем saveAndFlush чтобы сразу видеть ошибки БД,
    // а не при коммите транзакции когда уже поздно обрабатывать
    private LessonProgress saveProgress(LessonProgress p) {
        try {
            return progressRepo.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            // Если вдруг дубликат — перечитываем актуальное состояние
            return progressRepo.findByStudentIdAndLessonId(
                    p.getStudent().getId(), p.getLesson().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Прогресс не найден"));
        }
    }

    private Lesson findLesson(Long id) {
        return lessonRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Урок не найден: " + id));
    }

    private void checkAccess(Long studentId, Long courseId) {
        if (!enrollmentService.hasAccess(studentId, courseId))
            throw new AccessDeniedException("Нет доступа к курсу — купите или продлите подписку");
    }

    // Мягкая проверка — только наличие enrollment (не проверяем срок)
    private void checkEnrolled(Long studentId, Long courseId) {
        boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);
        if (!enrolled)
            throw new AccessDeniedException("Вы не записаны на этот курс");
    }

    private LessonResponse toLessonResponse(Lesson l, LessonProgress p) {
        List<QuizQuestionResponse> questions = l.getQuizQuestions().stream()
                .map(q -> QuizQuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .orderIndex(q.getOrderIndex())
                        .options(q.getOptions().stream()
                                .map(o -> QuizOptionResponse.builder()
                                        .id(o.getId())
                                        .optionText(o.getOptionText())
                                        .orderIndex(o.getOrderIndex())
                                        .isCorrect(o.getIsCorrect())
                                        .build())
                                .toList())
                        .build())
                .toList();

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
                .quizQuestions(questions)
                .videoWatched(p != null ? p.getVideoWatched() : false)
                .quizPassed(p != null ? p.getQuizPassed() : false)
                .practiceDone(p != null ? p.getPracticeDone() : false)
                .completed(p != null ? p.getCompleted() : false)
                .quizAttempts(p != null ? p.getQuizAttempts() : 0)
                .build();
    }
}
