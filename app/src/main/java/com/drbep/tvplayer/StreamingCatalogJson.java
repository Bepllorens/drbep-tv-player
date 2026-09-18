package com.drbep.tvplayer;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/** Keeps the object tree, never a second complete encoded catalogue. */
final class StreamingCatalogJson {
    static JSONObject read(InputStream input) throws Exception {
        try (JsonReader reader = new JsonReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            Object value = readValue(reader, 0);
            if (!(value instanceof JSONObject) || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalStateException("catalogo JSON invalido");
            }
            return (JSONObject) value;
        }
    }

    private static Object readValue(JsonReader reader, int depth) throws Exception {
        if (depth > 64) throw new IllegalStateException("catalogo demasiado anidado");
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JSONObject object = new JSONObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw new IllegalStateException("campo JSON duplicado");
                    object.put(name, readValue(reader, depth + 1));
                }
                reader.endObject();
                return object;
            case BEGIN_ARRAY:
                JSONArray array = new JSONArray();
                reader.beginArray();
                while (reader.hasNext()) array.put(readValue(reader, depth + 1));
                reader.endArray();
                return array;
            case STRING: return reader.nextString();
            case BOOLEAN: return reader.nextBoolean();
            case NULL: reader.nextNull(); return JSONObject.NULL;
            case NUMBER:
                String number = reader.nextString();
                try { return Long.valueOf(number); }
                catch (NumberFormatException ignored) { return Double.valueOf(number); }
            default: throw new IllegalStateException("valor JSON invalido");
        }
    }

    static void write(OutputStream output, JSONObject payload) throws Exception {
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writeValue(writer, payload);
        }
    }

    private static void writeValue(JsonWriter writer, Object value) throws Exception {
        if (value == null || value == JSONObject.NULL) writer.nullValue();
        else if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            writer.beginObject();
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                writer.name(key);
                writeValue(writer, object.get(key));
            }
            writer.endObject();
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            writer.beginArray();
            for (int i = 0; i < array.length(); i++) writeValue(writer, array.get(i));
            writer.endArray();
        } else if (value instanceof Number) writer.value((Number) value);
        else if (value instanceof Boolean) writer.value((Boolean) value);
        else writer.value(String.valueOf(value));
    }
}
