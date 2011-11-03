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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class IdentityTypeAdapter implements JsonSerializer<IdentityProxy>, JsonDeserializer<IdentityProxy> {
	@Override
	public JsonElement serialize(IdentityProxy src, Type srcType, JsonSerializationContext context) {
		assert(src != null);
		assert(src.getTableName() != null);
		
		if(src.getValue() == null)
			return null;

		IdentityDao identityDao = new IdentityDaoImpl();
		if(src.getValue() == null)
			return context.serialize(null);
		
		return new JsonPrimitive(identityDao.getIdentityUuid(src.getTableName(), String.valueOf(src.getValue())));
	}

	@Override
	public IdentityProxy deserialize(JsonElement json, Type type,
		JsonDeserializationContext context) throws JsonParseException {
		
		// this is a place holder implementation to guard our assumption - IdentityProxy is only used
		// on one-direction 
		assert(false);
		return null;
	}
}
