package kg.metaacademy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kg.metaacademy.entity.User;
import kg.metaacademy.enums.UserStatus;
import kg.metaacademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtils         jwtUtils;
    private final UserRepository   userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(req);

        if (token != null && jwtUtils.isValid(token)) {
            String email = jwtUtils.extractEmail(token);

            // Один запрос к БД — загружаем пользователя и сразу проверяем статус
            User user = userRepo.findByEmail(email).orElse(null);

            if (user == null) {
                // Токен валиден но пользователь удалён — просто не аутентифицируем
                chain.doFilter(req, res);
                return;
            }

            // ИСПРАВЛЕНО: блокировка — возвращаем 403
            if (user.getStatus() == UserStatus.BLOCKED) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Аккаунт заблокирован\"}");
                return;
            }

            // Строим аутентификацию напрямую из User — без второго запроса к БД
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            var springUser  = new org.springframework.security.core.userdetails.User(
                    user.getEmail(), user.getPassword(), authorities);

            var auth = new UsernamePasswordAuthenticationToken(springUser, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
