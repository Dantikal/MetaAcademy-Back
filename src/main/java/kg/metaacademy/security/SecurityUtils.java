package kg.metaacademy.security;

import kg.metaacademy.entity.User;
import kg.metaacademy.exception.ResourceNotFoundException;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepo;

    /** Возвращает текущего авторизованного пользователя */
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
