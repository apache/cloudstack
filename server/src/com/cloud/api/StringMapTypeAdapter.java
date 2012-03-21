package com.cloud.api;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

@SuppressWarnings("rawtypes")
public class StringMapTypeAdapter implements JsonDeserializer<Map> {
    @Override
    
    public Map deserialize(JsonElement src, Type srcType,
            JsonDeserializationContext context) throws JsonParseException {

        Map<String, String> obj = new HashMap<String, String>();
        JsonObject json = src.getAsJsonObject();
        
        for(Entry<String, JsonElement> entry : json.entrySet()) {
            obj.put(entry.getKey(), entry.getValue().getAsString());
        }
        
        return obj;
    }
}
