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

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.cloud.agent.api.Command;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class ArrayTypeAdaptor<T> implements JsonDeserializer<T[]>, JsonSerializer<T[]> {

	static final GsonBuilder s_gBuilder;
    static {
        s_gBuilder = new GsonBuilder();
        final Type listType = new TypeToken<List<VolumeVO>>() {}.getType();
        s_gBuilder.registerTypeAdapter(listType, new VolListTypeAdaptor());
    }
	
	
    private static final String s_pkg = Command.class.getPackage().getName() + ".";
    public ArrayTypeAdaptor() {
    }

    public JsonElement serialize(T[] src, Type typeOfSrc, JsonSerializationContext context) {
        Gson gson = s_gBuilder.create();
        JsonArray array = new JsonArray();
        for (T cmd : src) {
            String result = gson.toJson(cmd);
            array.add(new JsonPrimitive(cmd.getClass().getName().substring(s_pkg.length())));
            array.add(new JsonPrimitive(result));
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    public T[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        Iterator<JsonElement> it = array.iterator();
        ArrayList<T> cmds = new ArrayList<T>();
        Gson gson = Request.initBuilder().create();
        while (it.hasNext()) {
            JsonElement element = it.next();
            String name = s_pkg + element.getAsString();
            Class<?> clazz;
            try {
                clazz = Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new CloudRuntimeException("can't find " + name);
            }
            T cmd = (T)gson.fromJson(it.next().getAsString(), clazz);
            cmds.add(cmd);
        }
        Class<?> type = ((Class<?>)typeOfT).getComponentType();
        T[] ts = (T[])Array.newInstance(type, cmds.size());
        return cmds.toArray(ts);
    }
}