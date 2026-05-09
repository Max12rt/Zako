package zako.monolith.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import zako.monolith.user.User;
import zako.monolith.user.UserRepository;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length());
        if (!jwtService.isValid(token)) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }
        String email = jwtService.extractEmail(token);
        if (email == null) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var optUser = userRepository.findByEmail(email);
            if (optUser.isEmpty()) {
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                return;
            }
            User user = optUser.get();
            var auth = new UsernamePasswordAuthenticationToken(
                    user, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
