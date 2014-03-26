package com.cloud.bridge.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * @author Dmitry Batkovich
 *
 * For more complex cases (JsonArrays or other) it can be rewrite to matcher pattern
 */
public final class JsonElementUtil {

    private JsonElementUtil() {}

    public static JsonElement getAsJsonElement(final JsonElement jsonElement, final String... path) {
        JsonElement currentElement = jsonElement;
        for (final String propertyName : path) {
            if (currentElement == null) {
                return null;
            }
            if (jsonElement.isJsonObject()) {
                currentElement = ((JsonObject) currentElement).get(propertyName);
            } else {
                return null;
            }
        }
        return currentElement;
    }

    public static Integer getAsInt(final JsonElement jsonElement, final String... path) {
        final JsonElement targetElement = getAsJsonElement(jsonElement, path);
        if (targetElement == null || !targetElement.isJsonPrimitive()) {
            return null;
        }
        final JsonPrimitive asPrimitive = (JsonPrimitive) targetElement;
        return asPrimitive.getAsInt();
    }

    public static String getAsString(final JsonElement jsonElement, final String... path) {
        final JsonElement targetElement = getAsJsonElement(jsonElement, path);
        return targetElement == null ? null : targetElement.getAsString();
    }

}
