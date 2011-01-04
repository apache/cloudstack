/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

import com.cloud.agent.transport.Request;
import com.cloud.host.Host;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine.State;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class PingRoutingWithNwGroupsCommand extends PingRoutingCommand {
	HashMap<String, Pair<Long, Long>> newGroupStates;

	public static class NwGroupsCommandTypeAdaptor implements JsonDeserializer<Pair<Long, Long>>, JsonSerializer<Pair<Long,Long>> {
		static final GsonBuilder s_gBuilder;
	    static {
	        s_gBuilder = Request.initBuilder();
	    }

	    public NwGroupsCommandTypeAdaptor() {
	    }
	    
		@Override
		public JsonElement serialize(Pair<Long, Long> src,
				java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
	        JsonArray array = new JsonArray();
	        Gson json = s_gBuilder.create();
	        if(src.first() != null) {
	        	array.add(json.toJsonTree(src.first()));
	        } else {
	        	array.add(new JsonNull());
	        }
	        
	        if (src.second() != null) {
	        	array.add(json.toJsonTree(src.second()));
	        } else {
	        	array.add(new JsonNull());
	        }
	        
	        return array;
		}

		@Override
		public Pair<Long, Long> deserialize(JsonElement json,
				java.lang.reflect.Type type, JsonDeserializationContext context)
				throws JsonParseException {
			Pair<Long, Long> pairs = new Pair<Long, Long>(null, null);
			JsonArray array = json.getAsJsonArray();
			if (array.size() != 2) {
				return pairs;
			}
			JsonElement element = array.get(0);
			if (!element.isJsonNull()) {
				pairs.first(element.getAsLong());
			}

			element = array.get(1);
			if (!element.isJsonNull()) {
				pairs.second(element.getAsLong());
			}

			return pairs;
		}
		
	}
	protected PingRoutingWithNwGroupsCommand() {
		super();
	}

	public PingRoutingWithNwGroupsCommand(Host.Type type, long id, Map<String, State> states, HashMap<String, Pair<Long, Long>> nwGrpStates) {
		super(type, id, states);
		newGroupStates = nwGrpStates;
	}

	public HashMap<String, Pair<Long, Long>> getNewGroupStates() {
		return newGroupStates;
	}

	public void setNewGroupStates(HashMap<String, Pair<Long, Long>> newGroupStates) {
		this.newGroupStates = newGroupStates;
	}
}
