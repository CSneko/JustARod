package org.cneko.justarod.config;

import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置构建器：声明默认值与中英注释，首次运行时生成带注释的配置文件。
 * 结构与 toNeko 的 ConfigBuilder 一致，配置项按声明顺序写出。
 */
public class ConfigBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path path;
    private final JsonConfiguration config;
    private final Map<String, Entry> defaults = new LinkedHashMap<>();

    public ConfigBuilder(Path path) {
        this.path = path;
        // 尝试读取文件
        JsonConfiguration read;
        try {
            read = new JsonConfiguration(path);
        } catch (Exception e) {
            // 出现错误，使用一个空的配置
            read = new JsonConfiguration("{}");
        }
        this.config = read;
    }

    private ConfigBuilder add(String key, Entry defaultValue) {
        defaults.put(key, defaultValue);
        return this;
    }

    public ConfigBuilder addFloat(String key, float value, String url, String comment) {
        return this.add(key, Entry.of(value, comment, url));
    }

    public ConfigBuilder addFloat(String key, float value, String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments), url));
    }

    public ConfigBuilder addInt(String key, int value, String url, String comment) {
        return this.add(key, Entry.of(value, comment, url));
    }

    public ConfigBuilder addInt(String key, int value, String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments), url));
    }

    public ConfigBuilder addBoolean(String key, boolean value, String url, String comment) {
        return this.add(key, Entry.of(value, comment, url));
    }

    public ConfigBuilder addBoolean(String key, boolean value, String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments), url));
    }

    public ConfigBuilder addString(String key, String value, String url, String comment) {
        return this.add(key, Entry.of(value, comment, url));
    }

    public ConfigBuilder addString(String key, String value, String url, String... comments) {
        return this.add(key, Entry.of(value, String.join("\n", comments), url));
    }

    public void setBoolean(String key, boolean value) { config.set(key, value); config.save(path); }
    public void setString(String key, String value) { config.set(key, value); config.save(path); }
    public void setNumber(String key, Number value) { config.set(key, value); config.save(path); }

    /** 获取某个键的默认值条目 */
    public Entry get(String key) {
        return defaults.get(key);
    }

    /**
     * 读取当前生效值（文件里没有时回退到默认值），并带上默认值的注释。
     */
    public Entry getExist(String key) {
        Object value = config.get(key);
        if (value instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                value = primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                value = primitive.getAsNumber();
            } else if (primitive.isString()) {
                value = primitive.getAsString();
            }
        }
        Entry def = get(key);
        if (value == null) {
            value = def != null ? def.value : null;
        }
        if (def == null) {
            LOGGER.warn("Unknown config key: {}", key);
            return Entry.of(value, "", null);
        }
        return Entry.of(value, def.comment, def.url);
    }

    public String getKey(Entry entry) {
        for (String key : defaults.keySet()) {
            if (defaults.get(key).equals(entry)) {
                return key;
            }
        }
        return null;
    }

    public List<String> getKeys() {
        return new ArrayList<>(defaults.keySet());
    }

    /** 把缺失的键补写进文件并保存，返回自身以便链式调用 */
    public ConfigBuilder build() {
        boolean changed = false;
        for (String key : defaults.keySet()) {
            if (!config.contains(key)) {
                config.set(key, defaults.get(key).value);
                changed = true;
            }
        }
        if (changed || !containsAnyKey()) {
            save();
        }
        return this;
    }

    private boolean containsAnyKey() {
        for (String key : defaults.keySet()) {
            if (config.contains(key)) return true;
        }
        return false;
    }

    public void save() {
        config.save(path);
    }

    public JsonConfiguration getConfig() {
        return config;
    }

    public Path getPath() {
        return path;
    }

    /**
     * 一个配置项：值 + 注释 + 可选的参考链接。
     */
    public static class Entry {
        public enum Types { STRING, NUMBER, BOOLEAN }

        public final Object value;
        public final Types type;
        public final String comment;
        public final String url;

        private Entry(Object value, Types type, String comment, String url) {
            this.value = value;
            this.type = type;
            this.comment = comment;
            this.url = url;
        }

        public static Entry of(Object value, String comment, String url) {
            if (value instanceof String v) return new Entry(v, Types.STRING, comment, url);
            if (value instanceof Number v) return new Entry(v, Types.NUMBER, comment, url);
            if (value instanceof Boolean v) return new Entry(v, Types.BOOLEAN, comment, url);
            throw new IllegalArgumentException("Invalid type: " + (value != null ? value.getClass().getName() : "null"));
        }
    }
}
