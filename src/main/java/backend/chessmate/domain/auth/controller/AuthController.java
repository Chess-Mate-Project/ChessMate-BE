package backend.chessmate.domain.auth.controller;

import backend.chessmate.domain.auth.config.UserPrincipal;
import backend.chessmate.domain.auth.dto.request.OAuthValueRequest;
import backend.chessmate.domain.auth.service.AuthService;
import backend.chessmate.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<?>> login(@RequestBody OAuthValueRequest req, HttpServletResponse res) {

       authService.login(req, res);
        return ResponseEntity.ok(
            new SuccessResponse<>("로그인 성공", null)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Object>> logout(@AuthenticationPrincipal UserPrincipal u, HttpServletResponse res) {
        authService.logout(u.getUser(), res);
        return ResponseEntity.ok(
            new SuccessResponse<>("로그아웃 성공", null)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<Object>> get(@AuthenticationPrincipal UserPrincipal u) {
        return ResponseEntity.ok(
                new SuccessResponse<>("인증됨", null)
        );
    }
}
