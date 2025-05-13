package com.FrontendService.controller;

import com.FrontendService.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import com.FrontendService.model.dto.UpdateProfileDto;
import com.FrontendService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final CookieUtil cookieUtil;

    @PostMapping("/updateInfo")
    public String updateProfileInfo(@ModelAttribute UpdateProfileDto dto, RedirectAttributes redirectAttributes, HttpServletResponse response) {
        try {
            Map<String, String> result = userService.updateProfileInfo(dto);
            String accessToken = result.get("accessToken");
            String errorMessage = result.get("errorMessage");
            String refreshToken = result.get("refreshToken");

            cookieUtil.addTokenCookie(response, accessToken, 3600 * 24);
            cookieUtil.addRefreshTokenCookie(response, refreshToken, 86400 * 7);

            if (errorMessage != null) {
                redirectAttributes.addFlashAttribute("error", errorMessage);
            } else {
                redirectAttributes.addFlashAttribute("success", "Данные успешно обновлены!");
            }

            return "redirect:/user-info";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user-info";
        }
    }
}
