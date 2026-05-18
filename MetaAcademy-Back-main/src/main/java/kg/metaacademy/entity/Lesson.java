package kg.metaacademy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer orderIndex;

    private String videoUrl;
    private String videoPublicId;   // Cloudinary public_id для удаления

    @Column(columnDefinition = "TEXT")
    private String practiceTask;    // текст задания для студента (ИИ проверяет)

    // "text" — студент пишет в редакторе, "file" — студент загружает файл/zip
    @Builder.Default
    @Column(nullable = false)
    private String practiceMode = "text";

    // Для английских уроков — уровень (Beginner/Elementary/Pre-Intermediate/Intermediate)
    private String engLevel;

    // AI-разговорная практика — вопросы от препода (JSON-строка или текст)
    @Column(columnDefinition = "TEXT")
    private String aiQuestions;   // вопросы через перенос строки

    @Builder.Default
    private Integer aiQuestionCount = 3; // сколько вопросов задаёт ИИ (1-10)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LessonProgress> progresses = new ArrayList<>();
}
