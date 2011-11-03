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
package com.cloud.api;

import java.lang.reflect.Type;

import com.cloud.Identity.dao.IdentityDao;
import com.cloud.Identity.dao.IdentityDaoImpl;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class IdentityTypeAdapter implements JsonSerializer<IdentityProxy>, JsonDeserializer<IdentityProxy> {
	
	@Override
	public JsonElement serialize(IdentityProxy src, Type srcType, JsonSerializationContext context) {
		if(SerializationContext.current().getUuidTranslation()) {
			assert(src != null);
			if(src.getValue() == null)
				return context.serialize(null);
	
			IdentityDao identityDao = new IdentityDaoImpl();
			if(src.getTableName() != null) {
				String uuid = identityDao.getIdentityUuid(src.getTableName(), String.valueOf(src.getValue()));
				if(uuid == null)
					return context.serialize(null);
				
				return new JsonPrimitive(uuid);
			} else {
				return new JsonPrimitive(String.valueOf(src.getValue()));
			}
		} else {
	        return new Gson().toJsonTree(src);
		}
	}

	@Override
	public IdentityProxy deserialize(JsonElement src, Type srcType,
			JsonDeserializationContext context) throws JsonParseException {

		IdentityProxy obj = new IdentityProxy();
		JsonObject json = src.getAsJsonObject();
		obj.setTableName(json.get("_tableName").getAsString());
		if(json.get("_value") != null)
			obj.setValue(json.get("_value").getAsLong());
		return obj;
	}
}
