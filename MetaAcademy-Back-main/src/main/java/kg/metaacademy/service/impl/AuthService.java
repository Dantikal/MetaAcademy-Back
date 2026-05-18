package kg.metaacademy.service.impl;

import kg.metaacademy.dto.request.LoginRequest;
import kg.metaacademy.dto.request.RegisterRequest;
import kg.metaacademy.dto.response.AuthResponse;
import kg.metaacademy.entity.User;
import kg.metaacademy.enums.Role;
import kg.metaacademy.exception.AlreadyExistsException;
import kg.metaacademy.exception.BadRequestException;
import kg.metaacademy.repository.UserRepository;
import kg.metaacademy.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository    userRepo;
    private final PasswordEncoder   encoder;
    private final JwtUtils          jwtUtils;
    private final AuthenticationManager authManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new AlreadyExistsException("Email уже зарегистрирован: " + req.getEmail());

        if (req.getNickname() != null && userRepo.existsByNickname(req.getNickname()))
            throw new AlreadyExistsException("Никнейм уже занят: " + req.getNickname());

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .nickname(req.getNickname() != null ? req.getNickname()
                        : req.getFirstName().toLowerCase() + "_" + System.currentTimeMillis() % 10000)
                .age(req.getAge())
                .region(req.getRegion())
                .role(Role.STUDENT)
                .streak(1)
                .daysOnPlatform(1)
                .lastVisitDate(LocalDate.now())
                .build();

        user = userRepo.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        // Обновляем стрик при входе
        updateStreak(user);

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtils.isValid(refreshToken))
            throw new BadRequestException("Невалидный refresh token");

        String email = jwtUtils.extractEmail(refreshToken);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        return buildAuthResponse(user);
    }

    @Transactional
    public void updateStreak(User user) {
        LocalDate today = LocalDate.now();

        // Защита от NULL (пользователи добавленные вручную через SQL)
        if (user.getLastVisitDate() == null) {
            user.setLastVisitDate(today);
            user.setStreak(1);
            user.setDaysOnPlatform(1);
            user.setPoints(user.getPoints() != null ? user.getPoints() : 0);
            userRepo.save(user);
            return;
        }

        if (today.equals(user.getLastVisitDate())) return;

        LocalDate yesterday = today.minusDays(1);
        int currentStreak = user.getStreak() != null ? user.getStreak() : 0;
        int newStreak = yesterday.equals(user.getLastVisitDate())
                ? currentStreak + 1
                : 1;

        int currentDays = user.getDaysOnPlatform() != null ? user.getDaysOnPlatform() : 0;
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;

        user.setStreak(newStreak);
        user.setDaysOnPlatform(currentDays + 1);
        user.setLastVisitDate(today);

        // +5 баллов за каждые 10 дней подряд
        if (newStreak % 10 == 0) {
            user.setPoints(currentPoints + 5);
        } else {
            user.setPoints(currentPoints);
        }

        userRepo.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String access  = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String refresh = jwtUtils.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .role(user.getRole().name())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .englishLevel(user.getEnglishLevel() != null
                        ? user.getEnglishLevel().name() : null)
                .build();
    }
}
