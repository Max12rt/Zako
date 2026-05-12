package zako.monolith.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zako.monolith.auth.dto.LoginResponse;
import zako.monolith.config.JwtService;
import zako.monolith.user.UserRepository;
import zako.monolith.user.dto.LoginRequest;
import zako.monolith.user.dto.UserResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return new LoginResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
