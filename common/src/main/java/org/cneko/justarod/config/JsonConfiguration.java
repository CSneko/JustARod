package org.cneko.justarod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Gson 的扁平键 JSON 配置存储。
 * 结构与 toNeko 的 JsonConfiguration 一致（键即 JSON 顶层字段，支持点号分隔的语义分组），
 * 所有访问均加锁，读写强制 UTF-8。
 */
public class JsonConfiguration {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String original;
    private final JsonObject jsonObject;
    private Path filePath;

    private final Object lock = new Object();

    public JsonConfiguration(String jsonString) {
        this.original = jsonString;
        Gson gson = new Gson();
        // 尝试解析，如果失败则创建空对象，防止损坏的文件导致启动失败
        JsonObject temp;
        try {
            temp = gson.fromJson(jsonString, JsonObject.class);
        } catch (JsonSyntaxException e) {
            temp = new JsonObject();
        }
        this.jsonObject = temp != null ? temp : new JsonObject();
    }

    public JsonConfiguration(JsonObject jsonObject) {
        this.original = jsonObject.toString();
        this.jsonObject = jsonObject;
    }

    public JsonConfiguration(Path filePath) throws IOException {
        String content = "{}";
        try {
            if (Files.exists(filePath)) {
                // 强制使用 UTF-8 读取
                byte[] bytes = Files.readAllBytes(filePath);
                content = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read config file {}", filePath, e);
        }

        if (content.trim().isEmpty()) content = "{}";
        this.original = content;

        Gson gson = new Gson();
        JsonObject temp;
        try {
            temp = gson.fromJson(content, JsonObject.class);
        } catch (Exception e) {
            temp = new JsonObject();
        }
        this.jsonObject = temp != null ? temp : new JsonObject();
        this.filePath = filePath;
    }

    // --- 所有访问 jsonObject 的方法加锁 ---

    public JsonElement get(String path) {
        synchronized (lock) {
            return jsonObject.get(path);
        }
    }

    public JsonPrimitive getJsonPrimitive(String path) {
        synchronized (lock) {
            JsonElement element = get(path);
            if (element != null && element.isJsonPrimitive()) {
                return element.getAsJsonPrimitive();
            }
            return new JsonPrimitive("");
        }
    }

    public JsonArray getJsonArray(String path) {
        synchronized (lock) {
            JsonElement element = get(path);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray();
            }
            return new JsonArray();
        }
    }

    public void set(String path, Object value) {
        synchronized (lock) {
            if (value instanceof JsonElement) {
                jsonObject.add(path, (JsonElement) value);
                return;
            }
            if (value instanceof String) {
                jsonObject.addProperty(path, (String) value);
                return;
            }
            if (value instanceof Number) {
                jsonObject.addProperty(path, (Number) value);
                return;
            }
            if (value instanceof Boolean) {
                jsonObject.addProperty(path, (Boolean) value);
                return;
            }
            if (value instanceof Character) {
                jsonObject.addProperty(path, (Character) value);
                return;
            }
            if (value instanceof List) {
                processList(path, (List<?>) value);
                return;
            }
            if (value != null) {
                this.jsonObject.addProperty(path, value.toString());
            }
        }
    }

    private void processList(String path, List<?> list) {
        JsonArray jsonArray = new JsonArray();
        for (Object o : list) {
            if (o instanceof JsonElement) {
                jsonArray.add((JsonElement) o);
            } else if (o instanceof String) {
                jsonArray.add((String) o);
            } else if (o instanceof Number) {
                jsonArray.add((Number) o);
            } else if (o instanceof Boolean) {
                jsonArray.add((Boolean) o);
            } else if (o instanceof Character) {
                jsonArray.add((Character) o);
            } else if (o != null) {
                jsonArray.add(o.toString());
            }
        }
        jsonObject.add(path, jsonArray);
    }

    public void save(Path filePath) {
        synchronized (lock) {
            if (filePath != null) {
                try {
                    // 确保父目录存在
                    if (filePath.getParent() != null) {
                        Files.createDirectories(filePath.getParent());
                    }

                    // 格式化输出，否则配置文件挤在一行很难看
                    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                    String content = gson.toJson(this.jsonObject);

                    // 强制使用 UTF-8 写入
                    try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        writer.write(content);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to save config file {}", filePath, e);
                }
            }
        }
    }

    public void save() {
        save(this.filePath);
    }

    public String getString(String path) {
        try {
            return getJsonPrimitive(path).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    public float getFloat(String path) { try { return getJsonPrimitive(path).getAsFloat(); } catch (Exception e) { return 0; } }
    public double getDouble(String path) { try { return getJsonPrimitive(path).getAsDouble(); } catch (Exception e) { return 0; } }
    public int getInt(String path) { try { return getJsonPrimitive(path).getAsInt(); } catch (Exception e) { return 0; } }
    public boolean getBoolean(String path) { try { return getJsonPrimitive(path).getAsBoolean(); } catch (Exception e) { return false; } }

    public boolean contains(String key) {
        synchronized (lock) {
            return jsonObject.has(key);
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return jsonObject.toString();
        }
    }

    // Static methods
    public static JsonConfiguration of(String jsonString) { return new JsonConfiguration(jsonString); }
    public static JsonConfiguration fromFile(File file) throws IOException { return new JsonConfiguration(file.toPath()); }
    public static JsonConfiguration fromFile(Path filePath) throws IOException { return new JsonConfiguration(filePath); }
}
