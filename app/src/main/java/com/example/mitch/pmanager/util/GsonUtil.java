package com.example.mitch.pmanager.util;

import static com.example.mitch.pmanager.util.StringsUtil.bytesToChars;
import static com.example.mitch.pmanager.util.StringsUtil.charsToBytes;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Utility class for working with Gson
 *
 * @author mitch
 */
public class GsonUtil {

    /**
     * Util class, private constructor
     */
    private GsonUtil() {
    }

    /**
     * GSON singleton
     */
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(char[].class, new CharArrayTypeAdapter())
            .create();

    /**
     * Gets the gson singleton instance
     *
     * @return GSON instance
     */
    public static Gson gson() {
        return GSON;
    }

    /**
     * TypeAdapter for char[]
     * Used to protect data in memory
     *
     * @author mitch
     */
    private static class CharArrayTypeAdapter implements JsonSerializer<char[]>, JsonDeserializer<char[]> {
        @Override
        public char[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return bytesToChars(Base64.decode(json.getAsString(), Base64.NO_WRAP));
        }

        @Override
        public JsonElement serialize(char[] src, Type typeOfSrc, JsonSerializationContext context) {
            JsonPrimitive primitive = new JsonPrimitive(Base64.encodeToString(charsToBytes(src), Base64.NO_WRAP));
            Arrays.fill(src, (char) 0);
            return primitive;
        }
    }
}
