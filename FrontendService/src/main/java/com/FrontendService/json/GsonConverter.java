package com.FrontendService.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

public class GsonConverter {

    protected final Gson gson;

    public GsonConverter() {
        final GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
                .registerTypeHierarchyAdapter(byte[].class, new GsonByteArrayToBase64())
                .registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTime());
        this.gson = builder.create();
    }

    public <T> String toJson(T obj) {
        return gson.toJson(obj);
    }

    public <T> T fromJson(String json, Class<T> valueType) {
        return gson.fromJson(json, valueType);
    }
}