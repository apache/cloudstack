/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.transport;

import java.lang.reflect.Type;
import java.util.List;

import com.cloud.storage.VolumeVO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class VolListTypeAdaptor implements JsonDeserializer<List<VolumeVO>>, JsonSerializer<List<VolumeVO>> {
	static final GsonBuilder s_gBuilder;
    static {
        s_gBuilder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
    }

    Type listType = new TypeToken<List<VolumeVO>>() {}.getType();

    public VolListTypeAdaptor() {
    }

    public JsonElement serialize(List<VolumeVO> src, Type typeOfSrc, JsonSerializationContext context) {
        Gson json = s_gBuilder.create();
        String result = json.toJson(src, listType);
        return new JsonPrimitive(result);
    }

    public List<VolumeVO> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String jsonString = json.getAsJsonPrimitive().getAsString();
        Gson jsonp = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        List<VolumeVO> vols = jsonp.fromJson(jsonString, listType);
        return vols;
    }

}