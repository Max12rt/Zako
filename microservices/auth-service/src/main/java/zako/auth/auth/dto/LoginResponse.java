package zako.auth.auth.dto;

import zako.auth.user.dto.UserResponse;

public record LoginResponse(String token, UserResponse user) {}
