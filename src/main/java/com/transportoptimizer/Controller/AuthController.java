package com.transportoptimizer.Controller;

import com.transportoptimizer.entity.User;
import com.transportoptimizer.Services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> req) {
        authService.signup(req.get("username"), req.get("password"));
        return ResponseEntity.ok("Signup successful");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

        User user = authService.login(
                req.get("username"),
                req.get("password")
        );

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "userId", user.getId() // Mongo _id
        ));
    }
}
