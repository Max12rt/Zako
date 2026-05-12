package zako.monolith.auth.dto;

import zako.monolith.user.dto.UserResponse;

public record LoginResponse(String token, UserResponse user) {}
