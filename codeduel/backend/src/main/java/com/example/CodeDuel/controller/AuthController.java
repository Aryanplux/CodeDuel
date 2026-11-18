package com.example.CodeDuel.controller;

import com.example.CodeDuel.entity.UserAccount;
import com.example.CodeDuel.repository.UserAccountRepository;
import com.example.CodeDuel.service.EloService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    public static final String SESSION_USER_ID = "CODEDUEL_USER_ID";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody(required = false) Map<String, Object> body,
                                    HttpSession session) {
        if (body == null) return bad("Invalid signup request");
        String username = text(body, "username");
        String email = text(body, "email");
        String password = text(body, "password");
        if (username == null || username.length() < 3 || username.length() > 32)
            return bad("Username must be 3-32 characters");
        if (!username.matches("[A-Za-z0-9_-]+"))
            return bad("Username may contain only letters, numbers, underscores and hyphens");
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            return bad("Enter a valid email address");
        if (password == null || password.length() < 8)
            return bad("Password must be at least 8 characters");
        if (users.existsByUsernameIgnoreCase(username))
            return conflict("That username is already taken");
        if (users.existsByEmailIgnoreCase(email))
            return conflict("An account already exists for that email");

        UserAccount user = UserAccount.builder()
                .username(username.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .elo(1250)
                .matchesPlayed(0)
                .wins(0)
                .losses(0)
                .build();
        users.save(user);
        session.setAttribute(SESSION_USER_ID, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(publicUser(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) Map<String, Object> body,
                                   HttpSession session) {
        if (body == null) return bad("Invalid login request");
        String login = text(body, "username");
        String password = text(body, "password");
        if (login == null || login.isBlank() || password == null)
            return bad("Username and password are required");

        UserAccount user = users.findByUsernameIgnoreCase(login.trim())
                .orElseGet(() -> users.findByEmailIgnoreCase(login.trim()).orElse(null));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));

        session.setAttribute(SESSION_USER_ID, user.getId());
        return ResponseEntity.ok(publicUser(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        String userId = (String) session.getAttribute(SESSION_USER_ID);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        return users.findById(userId)
                .map(user -> ResponseEntity.ok(publicUser(user)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Session expired")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private Map<String, Object> publicUser(UserAccount user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "elo", user.getElo(),
                "rank", EloService.getRank(user.getElo()),
                "matchesPlayed", user.getMatchesPlayed(),
                "wins", user.getWins(),
                "losses", user.getLosses()
        );
    }

    private ResponseEntity<Map<String, String>> bad(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, String>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
    }

    private String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }
}
