package com.FrontendService.controller;

import com.FrontendService.model.User;
import com.FrontendService.model.auth.AuthRequest;
import com.FrontendService.model.auth.RegisterRequest;
import com.FrontendService.service.AuthService;
import com.FrontendService.service.JwtService;
import com.FrontendService.service.TaskKafkaService;
import com.FrontendService.service.UserService;
import com.FrontendService.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtService jwtService;
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                          @RequestParam(required = false) String registered,
                          Model model) {
        model.addAttribute("authRequest", new AuthRequest());
        if (error != null) {
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }
        if (registered != null) {
            model.addAttribute("success", "Регистрация успешна! Теперь вы можете войти.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("authRequest") AuthRequest request,
                       HttpServletResponse response,
                       Model model) {
        try {
            var authResponse = authService.login(request);
            // Сохраняем токены в cookie
            cookieUtil.addTokenCookie(response, authResponse.getToken(), 3600 * 24);
            cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 86400 * 7);
            return "redirect:/dashboard";
        } catch (Exception e) {
            if (e.getMessage().equals("Bad credentionals")) {
                model.addAttribute("Имя пользователя или пароль неправильный!");
            } else {
                model.addAttribute("error", e.getMessage());
            }
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                          @RequestParam(required = false) String error,
                          HttpServletResponse response,
                          Model model) {
        log.info("request to register!");
        try {
            var authResponse = authService.register(request);
            cookieUtil.addTokenCookie(response, authResponse.getToken(), 3600); // 1 час
            cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 86400); // 24 часа
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/welcome")
    public String homePage() {
        return "index";
    }

    @GetMapping("/user-info")
    public String getUserInfoPage(HttpServletRequest request, Model model) {
        String token = jwtService.getRefreshByRequest(request);
        Long userId = jwtService.getUserIdFromToken(token);

        User user = userService.findById(userId);
        model.addAttribute("firstName", user.getFirstName());
        model.addAttribute("lastName", user.getLastName());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("id", userId);

        return "user-profile";
    }
} 