package com.pockyl.petrichor.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Typed, range-checked Fabric configuration. Values are published only after the entire file validates. */
public final class ConfigSpec {
    private final Map<String, ConfigValue<?>> values;
    private volatile boolean loaded;

    private ConfigSpec(Map<String, ConfigValue<?>> values) { this.values = Map.copyOf(values); }
    public boolean isLoaded() { return loaded; }

    public synchronized void load(Path file) {
        try {
            JsonObject json = new JsonObject();
            if (Files.exists(file)) {
                try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
            Map<ConfigValue<?>, Object> pending = new LinkedHashMap<>();
            for (var entry : values.entrySet()) {
                ConfigValue<?> value = entry.getValue();
                JsonElement encoded = json.get(entry.getKey());
                Object decoded = encoded == null ? value.getDefault() : value.read(encoded);
                pending.put(value, decoded);
                json.add(entry.getKey(), new com.google.gson.Gson().toJsonTree(decoded));
            }
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), file.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n", StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporary); }
            pending.forEach(ConfigValue::publish);
            loaded = true;
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Cannot load configuration " + file, ex);
        }
    }

    public static class ConfigValue<T> {
        private final T defaultValue;
        private final Function<JsonElement, T> decoder;
        private volatile T value;
        ConfigValue(T value, Function<JsonElement, T> decoder) {
            this.defaultValue = value; this.value = value; this.decoder = decoder;
        }
        public T get() { return value; }
        public T getDefault() { return defaultValue; }
        private T read(JsonElement json) { return decoder.apply(json); }
        @SuppressWarnings("unchecked") private void publish(Object value) { this.value = (T) value; }
    }
    public static final class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(boolean value) { super(value, json -> {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected boolean");
            return json.getAsBoolean();
        }); }
    }
    public static final class IntValue extends ConfigValue<Integer> {
        IntValue(int value, int min, int max) { super(value, json -> {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected integer");
            int number = json.getAsBigDecimal().intValueExact();
            if (number < min || number > max) throw new IllegalArgumentException("Integer outside " + min + ".." + max);
            return number;
        }); }
    }
    public static final class DoubleValue extends ConfigValue<Double> {
        DoubleValue(double value, double min, double max) { super(value, json -> {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected number");
            double number = json.getAsDouble();
            if (!Double.isFinite(number) || number < min || number > max) throw new IllegalArgumentException("Number outside " + min + ".." + max);
            return number;
        }); }
    }
    public static final class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(T value) { super(value, json -> Enum.valueOf(value.getDeclaringClass(), json.getAsString())); }
    }
    public static final class Builder {
        private final ArrayDeque<String> path = new ArrayDeque<>();
        private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();
        public Builder comment(String... lines) { return this; }
        public Builder translation(String key) { return this; }
        public Builder push(String group) { path.addLast(group); return this; }
        public Builder pop() { path.removeLast(); return this; }
        private <T extends ConfigValue<?>> T add(String key, T value) {
            String full = path.isEmpty() ? key : String.join(".", path) + "." + key;
            if (values.putIfAbsent(full, value) != null) throw new IllegalArgumentException("Duplicate config key " + full);
            return value;
        }
        public BooleanValue define(String key, boolean value) { return add(key, new BooleanValue(value)); }
        public IntValue defineInRange(String key, int value, int min, int max) { return add(key, new IntValue(value, min, max)); }
        public DoubleValue defineInRange(String key, double value, double min, double max) { return add(key, new DoubleValue(value, min, max)); }
        public <T extends Enum<T>> EnumValue<T> defineEnum(String key, T value) { return add(key, new EnumValue<>(value)); }
        public ConfigSpec build() {
            if (!path.isEmpty()) throw new IllegalStateException("Unclosed configuration group");
            return new ConfigSpec(values);
        }
    }
}
