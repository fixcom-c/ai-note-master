package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.dto.LoginRequest;
import com.ainote.dto.LoginResponse;
import com.ainote.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody LoginRequest request) {
        return Result.success(authService.register(request));
    }
}