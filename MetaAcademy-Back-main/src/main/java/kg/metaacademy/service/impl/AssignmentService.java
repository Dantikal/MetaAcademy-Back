package kg.metaacademy.service.impl;

import kg.metaacademy.dto.request.AssignmentRequest;
import kg.metaacademy.dto.request.GradeSubmissionRequest;
import kg.metaacademy.dto.request.SubmitAssignmentRequest;
import kg.metaacademy.dto.response.AssignmentResponse;
import kg.metaacademy.dto.response.SubmissionResponse;
import kg.metaacademy.entity.*;
import kg.metaacademy.enums.SubmissionStatus;
import kg.metaacademy.exception.BadRequestException;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository           assignmentRepo;
    private final AssignmentSubmissionRepository submissionRepo;
    private final CourseRepository               courseRepo;
    private final UserRepository                 userRepo;
    private final AssignmentReviewService        reviewService;

    // ── Преподаватель создаёт задание ─────────────────────────────────────────
    @Transactional
    public AssignmentResponse create(Long teacherId, AssignmentRequest req) {
        Course course = courseRepo.findById(req.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));

        User teacher = userRepo.getReferenceById(teacherId);

        User targetStudent = null;
        if (req.getTargetStudentId() != null) {
            targetStudent = userRepo.findById(req.getTargetStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        }

        Assignment a = Assignment.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .deadline(req.getDeadline())
                .type(req.getType())
                .targetStudent(targetStudent)
                .course(course)
                .teacher(teacher)
                .build();

        return toResponse(assignmentRepo.save(a));
    }

    // ── Задания для студента ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getForStudent(Long courseId, Long studentId) {
        return assignmentRepo.findForStudent(courseId, studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Все задания курса (для преподавателя) ─────────────────────────────────
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getByCourse(Long courseId) {
        return assignmentRepo.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Студент сдаёт задание (GitHub ссылка) ────────────────────────────────
    @Transactional
    public SubmissionResponse submit(Long assignmentId, Long studentId, SubmitAssignmentRequest req) {
        Assignment assignment = findOrThrow(assignmentId);
        User student = userRepo.getReferenceById(studentId);

        AssignmentSubmission sub = submissionRepo
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElse(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(student)
                        .build());

        sub.setGithubUrl(req.getGithubUrl());
        sub.setStatus(SubmissionStatus.SUBMITTED);

        return toSubmissionResponse(submissionRepo.save(sub));
    }

    @Transactional
    public SubmissionResponse submitFile(Long assignmentId, Long studentId, MultipartFile file) {
        Assignment assignment = findOrThrow(assignmentId);
        User student = userRepo.getReferenceById(studentId);

        AssignmentSubmission sub = submissionRepo
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElse(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(student)
                        .build());

        sub.setGithubUrl(null);
        sub.setStatus(SubmissionStatus.SUBMITTED);

        AssignmentReviewService.ReviewResult result = reviewService.review(sub, file);

        if (result.isSuccess()) {
            User currentStudent = userRepo.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
            currentStudent.setPoints(currentStudent.getPoints() + 10);
            userRepo.save(currentStudent);
        }

        return toSubmissionResponse(submissionRepo.save(sub));
    }

    // ── Преподаватель оценивает работу ────────────────────────────────────────
    @Transactional
    public SubmissionResponse grade(Long assignmentId, Long studentId, GradeSubmissionRequest req) {
        int pts = req.getPoints();
        if (pts != 3 && pts != 5 && pts != 10)
            throw new BadRequestException("Баллы должны быть 3, 5 или 10");

        AssignmentSubmission sub = submissionRepo
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Работа не найдена"));

        sub.setPointsAwarded(pts);
        sub.setStatus(SubmissionStatus.GRADED);
        sub.setGradedAt(LocalDateTime.now());

        // Начисляем баллы студенту
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        student.setPoints(student.getPoints() + pts);
        userRepo.save(student);

        return toSubmissionResponse(submissionRepo.save(sub));
    }

    // ── Удалить задание ───────────────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        assignmentRepo.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Assignment findOrThrow(Long id) {
        return assignmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задание не найдено: " + id));
    }

    private AssignmentResponse toResponse(Assignment a) {
        List<SubmissionResponse> subs = a.getSubmissions().stream()
                .map(this::toSubmissionResponse)
                .toList();

        return AssignmentResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .deadline(a.getDeadline())
                .type(a.getType().name())
                .targetStudentName(a.getTargetStudent() != null
                        ? a.getTargetStudent().getFirstName() + " " + a.getTargetStudent().getLastName()
                        : null)
                .createdAt(a.getCreatedAt())
                .submissions(subs)
                .build();
    }

    private SubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        return SubmissionResponse.builder()
                .studentId(s.getStudent().getId())
                .studentName(s.getStudent().getFirstName() + " " + s.getStudent().getLastName())
                .githubUrl(s.getGithubUrl())
                .fileName(s.getFileName())
                .fileType(s.getFileType())
                .status(s.getStatus().name())
                .pointsAwarded(s.getPointsAwarded())
                .attemptsToday(s.getAttemptsToday())
                .feedback(s.getFeedback())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
