package com.FrontendService.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class GsonByteArrayToBase64 implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Base64.getDecoder().decode(json.getAsString());
    }

    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
        try {
            return new JsonPrimitive(new String(Base64.getEncoder().encode(src), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}