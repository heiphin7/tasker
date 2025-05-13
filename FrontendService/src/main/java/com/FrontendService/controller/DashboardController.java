package com.FrontendService.controller;

import com.FrontendService.model.Task;
import com.FrontendService.model.Team;
import com.FrontendService.model.User;
import com.FrontendService.service.JwtService;
import com.FrontendService.service.TaskKafkaService;
import com.FrontendService.service.UserKafkaService;
import com.FrontendService.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserKafkaService userKafkaService;
    private final TaskKafkaService taskKafkaService;
    private final CookieUtil cookieUtil;
    private final JwtService jwtService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request, @CookieValue("refreshToken") String token) {
        if (token == null) {
            return "redirect:/login";
        }
        Long userId = jwtService.getUserIdFromToken(token);
        Integer notifications = taskKafkaService.getUserNotifications(userId);
        model.addAttribute("notifications", notifications);

        return "dashboard";
    }
}