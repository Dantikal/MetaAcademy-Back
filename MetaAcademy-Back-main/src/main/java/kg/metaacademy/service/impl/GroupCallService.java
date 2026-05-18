package kg.metaacademy.service.impl;

import kg.metaacademy.dto.request.GroupCallRequest;
import kg.metaacademy.dto.response.GroupCallResponse;
import kg.metaacademy.entity.Course;
import kg.metaacademy.entity.GroupCall;
import kg.metaacademy.entity.User;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.CourseRepository;
import kg.metaacademy.repository.GroupCallRepository;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupCallService {

    private final GroupCallRepository callRepo;
    private final CourseRepository    courseRepo;
    private final UserRepository      userRepo;

    @Transactional
    public GroupCallResponse create(Long teacherId, Long courseId, GroupCallRequest req) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Курс не найден"));
        User teacher = userRepo.getReferenceById(teacherId);

        GroupCall call = GroupCall.builder()
                .callDate(req.getCallDate())
                .callTime(req.getCallTime())
                .zoomLink(req.getZoomLink())
                .course(course)
                .teacher(teacher)
                .build();

        return toResponse(callRepo.save(call));
    }

    @Transactional
    public GroupCallResponse update(Long callId, GroupCallRequest req) {
        GroupCall call = callRepo.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Созвон не найден"));
        if (req.getCallDate() != null) call.setCallDate(req.getCallDate());
        if (req.getCallTime() != null) call.setCallTime(req.getCallTime());
        if (req.getZoomLink() != null) call.setZoomLink(req.getZoomLink());
        return toResponse(callRepo.save(call));
    }

    @Transactional
    public void delete(Long callId) { callRepo.deleteById(callId); }

    @Transactional(readOnly = true)
    public List<GroupCallResponse> getByCourse(Long courseId) {
        return callRepo.findByCourseIdOrderByCallDateAscCallTimeAsc(courseId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupCallResponse> getUpcoming(Long courseId) {
        return callRepo.findByCourseIdAndCallDateGreaterThanEqual(courseId, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    private GroupCallResponse toResponse(GroupCall c) {
        LocalDateTime callDT = LocalDateTime.of(c.getCallDate(), c.getCallTime());
        LocalDateTime now    = LocalDateTime.now();
        boolean isLive = now.isAfter(callDT) && now.isBefore(callDT.plusHours(2));

        return GroupCallResponse.builder()
                .id(c.getId())
                .callDate(c.getCallDate())
                .callTime(c.getCallTime())
                .zoomLink(c.getZoomLink())
                .courseName(c.getCourse().getName())
                .isLive(isLive)
                .build();
    }
}
