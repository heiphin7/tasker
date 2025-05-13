package com.FrontendService.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter PRIMARY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
    private static final DateTimeFormatter SECONDARY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateText = p.getText();
        try {
            // Пробуем парсить с первым форматтером
            return LocalDateTime.parse(dateText, PRIMARY_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                // Если не получилось, пробуем второй формат
                return LocalDateTime.parse(dateText, SECONDARY_FORMATTER);
            } catch (DateTimeParseException ex) {
                // Если оба варианта не сработали, кидаем исключение с понятным сообщением
                throw new IOException("Не удалось распарсить дату: " + dateText, ex);
            }
        }
    }
}
