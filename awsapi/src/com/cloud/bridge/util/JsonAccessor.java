// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloud.bridge.service.exception.InternalErrorException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * JsonAccessor provides the functionality to allow navigating JSON object graph using simple expressions, 
 * for example, following property access expressions are all valid ones
 * 
 * 		rootobj.level1obj[1].property
 * 		this[0].level1obj[1].property
 * 
 */
public class JsonAccessor {
	private JsonElement _json;
	
    Pattern _arrayAccessorMatcher = Pattern.compile("(.*)\\[(\\d+)\\]");
	
	public JsonAccessor(JsonElement json) {
		assert(json != null);
		_json = json;
	}
	
	public BigDecimal getAsBigDecimal(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsBigDecimal();
	}
	
	public BigInteger getAsBigInteger(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsBigInteger();
	}
	
	public boolean getAsBoolean(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsBoolean();
	}
	
	public byte getAsByte(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsByte();
	}
	
	public char getAsCharacter(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsCharacter();
	}
	
	public double getAsDouble(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsDouble();
	}
	
	public float getAsFloat(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsFloat();
	}
	
	public int getAsInt(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsInt();
	}
	
	public long	getAsLong(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsLong();
	}
	
	public Number getAsNumber(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsNumber();
	}
	
	public short getAsShort(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsShort();
	}
	
	public String getAsString(String propPath) {
		JsonElement jsonElement = eval(propPath);
		return jsonElement.getAsString();
	}
	
	public boolean isBoolean(String propPath) {
		JsonElement jsonElement = eval(propPath);
		if(jsonElement instanceof JsonPrimitive)
			return ((JsonPrimitive)jsonElement).isBoolean();
		
		return false;
	}
	
	public boolean isNumber(String propPath) {
		JsonElement jsonElement = eval(propPath);
		
		if(jsonElement instanceof JsonPrimitive)
			return ((JsonPrimitive)jsonElement).isNumber();
		return false;
	}
	
	public boolean isString(String propPath) {
		JsonElement jsonElement = eval(propPath);
		
		if(jsonElement instanceof JsonPrimitive)
			return ((JsonPrimitive)jsonElement).isString();
		return false;
	}
	
	/*
	 * Return
	 * 		-1	:	property expression can not be resolved
	 * 		0	:	match to a null JSON object
	 * 		1+	:	matched, for array element, the count of the elements inside the array
	 */
	public int getMatchCount(String propPath) {
		JsonElement jsonElement = tryEval(propPath);
		if(jsonElement == null)
			return -1;
		
		if(jsonElement.isJsonNull())
			return 0;
		
		if(jsonElement.isJsonArray())
			return ((JsonArray)jsonElement).size();
		
		return 1;
	}
	
	public JsonElement eval(String propPath) {
		JsonElement jsonElement = tryEval(propPath);
		if(jsonElement == null)
			throw new InternalErrorException("Property " + propPath + " is resolved to null JSON element on object: " + _json.toString());

		return jsonElement;
	}
	
	public JsonElement tryEval(String propPath) {
		assert(propPath != null);
		String[] tokens = propPath.split("\\.");

		ArrayList<Resolver> resolverChain = new ArrayList<Resolver>();
		for(String token : tokens) {
	        Matcher matcher = _arrayAccessorMatcher.matcher(token);
	        if(matcher.find()) {
	            String propStr = matcher.group(1);
	            String indexStr = matcher.group(2);
	            
	            resolverChain.add(new ArrayPropertyResolver(propStr, Integer.parseInt(indexStr)));
	        } else {
	        	resolverChain.add(new PropertyResolver(token));
	        }
		}
		
		JsonElement jsonElementToResolveAt = _json;
		for(Resolver resolver : resolverChain) {
			jsonElementToResolveAt = resolver.resolve(jsonElementToResolveAt);
			
			if(jsonElementToResolveAt == null)
				break;
		}
		
		return jsonElementToResolveAt;
	}
	
	//
	// Property resolvers
	//
	private static interface Resolver {
		public JsonElement resolve(JsonElement jsonElementToResolveAt);
	}
	
	private static class PropertyResolver implements Resolver {
		protected String _propName;
		
		public PropertyResolver(String propName) {
			_propName = propName;
		}
		
		public JsonElement resolve(JsonElement jsonElementToResolveAt) {
			if("this".equals(_propName))
				return jsonElementToResolveAt;

			if(jsonElementToResolveAt.isJsonObject())
				return ((JsonObject)jsonElementToResolveAt).get(_propName);

			if(jsonElementToResolveAt.isJsonNull())
				throw new NullPointerException(String.format("Property %s points to a null element on object: %s", _propName, jsonElementToResolveAt.toString()));
			
			throw new InternalErrorException("Unable to evaluate JSON accessor property: " + _propName 
				+ ", on object: " + jsonElementToResolveAt.toString());
		}
	}
	
	private static class ArrayPropertyResolver extends PropertyResolver {
		protected int _index;
		
		public ArrayPropertyResolver(String propName, int index) {
			super(propName);
			_index = index;
		}
		
		public JsonElement resolve(JsonElement jsonElementToResolveAt) {
			if(!"this".equals(_propName)) {
				if(jsonElementToResolveAt.isJsonObject()) {
					jsonElementToResolveAt = ((JsonObject)jsonElementToResolveAt).get(_propName);
				} else {
					if(jsonElementToResolveAt.isJsonNull())
						throw new NullPointerException(String.format("Property %s points to a null element on object: %s", _propName, jsonElementToResolveAt.toString()));
					
					
					throw new InternalErrorException("Unable to evaluate JSON accessor property: " + _propName 
							+ ", on object: " + jsonElementToResolveAt.toString());
				}
			}
			
			if(jsonElementToResolveAt instanceof JsonArray) {
				return ((JsonArray)jsonElementToResolveAt).get(_index);
			}

			if(jsonElementToResolveAt.isJsonNull())
				throw new NullPointerException(String.format("Property %s points to a null element on object: %s", _propName, jsonElementToResolveAt.toString()));
			
			throw new InternalErrorException("Unable to evaluate JSON accessor property: " + _propName 
					+ ", on object: " + jsonElementToResolveAt.toString());
		}
	}
}
