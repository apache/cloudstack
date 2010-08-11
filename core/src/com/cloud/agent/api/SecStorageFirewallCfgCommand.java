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

package com.cloud.agent.api;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.cloud.agent.transport.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class SecStorageFirewallCfgCommand extends Command {
	


	public static class PortConfigListTypeAdaptor implements JsonDeserializer<List<PortConfig>>, JsonSerializer<List<PortConfig>> {
		static final GsonBuilder s_gBuilder;
	    static {
	        s_gBuilder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
	    }

	    Type listType = new TypeToken<List<PortConfig>>() {}.getType();

	    public PortConfigListTypeAdaptor() {
	    }

	    public JsonElement serialize(List<PortConfig> src, Type typeOfSrc, JsonSerializationContext context) {
	        Gson json = s_gBuilder.create();
	        String result = json.toJson(src, listType);
	        return new JsonPrimitive(result);
	    }

	    public List<PortConfig> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
	            throws JsonParseException {
	        String jsonString = json.toString();
	        Gson jsonp = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	        List<PortConfig> pcs = jsonp.fromJson(jsonString, listType);
	        return pcs;
	    }

	}
	public static class PortConfig {
		@Expose boolean add;
		@Expose String sourceIp;
		@Expose String port;
		@Expose String intf;
		public PortConfig(String sourceIp, String port, boolean add, String intf) {
			this.add = add;
			this.sourceIp = sourceIp;
			this.port = port;
			this.intf = intf;
		}
		public PortConfig() {
			
		}
		public boolean isAdd() {
			return add;
		}
		public String getSourceIp() {
			return sourceIp;
		}
		public String getPort() {
			return port;
		}
		public String getIntf() {
			return intf;
		}
	}
	
	static {//works since Request's static initializer is run before this one.
        GsonBuilder s_gBuilder = Request.initBuilder();
        final Type listType = new TypeToken<List<PortConfig>>() {}.getType();
        s_gBuilder.registerTypeAdapter(listType, new PortConfigListTypeAdaptor());
    }
	
	@Expose
	private List<PortConfig> portConfigs = new ArrayList<PortConfig>();
	
	public SecStorageFirewallCfgCommand() {
		
	}
    
    
    public void addPortConfig(String sourceIp, String port, boolean add, String intf) {
    	PortConfig pc = new PortConfig(sourceIp, port, add, intf);
    	this.portConfigs.add(pc);
    	
    }

	@Override
    public boolean executeInSequence() {
        return false;
    }


	public List<PortConfig> getPortConfigs() {
		return portConfigs;
	}
}
