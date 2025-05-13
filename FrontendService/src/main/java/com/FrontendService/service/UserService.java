package com.FrontendService.service;

import com.FrontendService.model.User;
import com.FrontendService.model.dto.UpdateProfileDto;
import com.FrontendService.repository.UserRepository;
import com.FrontendService.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // Возвращает jwt токен где в claims обновлено имя пользователя
    public Map<String, String> updateProfileInfo(UpdateProfileDto dto) throws IllegalArgumentException {
        User userInDb = userRepository.findById(dto.getId()).orElseThrow(
                () -> new IllegalArgumentException("Пользователь с указанным ID не найден!")
        );

        Map<String, String> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        if (!userInDb.getUsername().equals(dto.getUsername())) {
            User checkUsername = userRepository.findByUsername(dto.getUsername()).orElse(null);

            if (checkUsername == null) {
                userInDb.setUsername(dto.getUsername());
            } else {
                errors.add("Имя пользователя уже занято!\n");
            }
        }

        if (!userInDb.getEmail().equals(dto.getEmail())) {
            boolean existByEmail = userRepository.existsByEmail(dto.getEmail());

            if (!existByEmail) {
                userInDb.setEmail(dto.getEmail());
            } else {
                errors.add("Указанная почта уже занята!");
            }
        }

        /* Тут Имя и Фамилию не проверяем так как они не уникальны */
        userInDb.setFirstName(dto.getFirstName());
        userInDb.setLastName(dto.getLastName());

        userRepository.save(userInDb);

        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();

            for (String str: errors) {
                errorMessage.append(str);
            }

            result.put("errorMessage", errorMessage.toString());
        }

        // Генерируем токен
        String accessToken = jwtService.generateToken(userInDb);
        result.put("accessToken", accessToken);

        String refreshToken = refreshTokenService.generateRefreshToken(userInDb.getId());
        result.put("refreshToken", refreshToken);

        return result;
    }

    public User findById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
