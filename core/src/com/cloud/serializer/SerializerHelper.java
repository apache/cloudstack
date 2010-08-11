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

package com.cloud.serializer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.google.gson.Gson;

/**
 * Note: toPairList and appendPairList only support simple POJO objects currently
 */
public class SerializerHelper {
    public static final Logger s_logger = Logger.getLogger(SerializerHelper.class.getName());
	
	public static String toSerializedString(Object result) {
		if(result != null) {
			Class<?> clz = result.getClass();
	    	Gson gson = GsonHelper.getBuilder().create();
	    	
			return clz.getName() + "/" + gson.toJson(result); 
		} 
		return null;
	}
	
	public static Object fromSerializedString(String result) {
		try {
			if(result != null && !result.isEmpty()) {
				int seperatorPos = result.indexOf('/');
				if(seperatorPos < 0)
					return null;
				
				String clzName = result.substring(0, seperatorPos);
				String content = result.substring(seperatorPos + 1);
				Class<?> clz;
				try {
					clz = Class.forName(clzName);
				} catch (ClassNotFoundException e) {
					return null;
				}
				
		    	Gson gson = GsonHelper.getBuilder().create();
		    	return gson.fromJson(content, clz);
			}
			return null;
		} catch(RuntimeException e) {
			s_logger.error("Caught runtime exception when doing GSON descrialization on: " + result);
			throw e; 
		}
	}
    
	public static List<Pair<String, Object>> toPairList(Object o, String name) {
		List<Pair<String, Object>> l = new ArrayList<Pair<String, Object>>();
		return appendPairList(l, o, name);
	}
	
	public static List<Pair<String, Object>> appendPairList(List<Pair<String, Object>> l, Object o, String name) {
		if(o != null) {
			Class<?> clz = o.getClass();
			
			if(clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
				l.add(new Pair<String, Object>(name, o.toString()));
				return l;
			}
			
			for(Field f : clz.getDeclaredFields()) {
				if((f.getModifiers() & Modifier.STATIC) != 0)
					continue;
				
				Param param = f.getAnnotation(Param.class);
				if(param == null)
					continue;
				
				String propName = f.getName();
				if(!param.propName().isEmpty())
					propName = param.propName();
				
				String paramName = param.name();
				if(paramName.isEmpty())
					paramName = propName;
				
				Method method = getGetMethod(o, propName);
				if(method != null) {
					try {
						Object fieldValue = method.invoke(o);
						if(fieldValue != null) {
						    if (f.getType() == Date.class) {
	                            l.add(new Pair<String, Object>(paramName, DateUtil.getOutputString((Date)fieldValue)));
						    } else {
	                            l.add(new Pair<String, Object>(paramName, fieldValue.toString()));
						    }
						}
						//else
						//	l.add(new Pair<String, Object>(paramName, ""));
					} catch (IllegalArgumentException e) {
						s_logger.error("Illegal argument exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);
						
					} catch (IllegalAccessException e) {
						s_logger.error("Illegal access exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);
					} catch (InvocationTargetException e) {
						s_logger.error("Invocation target exception when calling POJO " + o.getClass().getName() + " get method for property: " + propName);
					}
				}
			}
		}
		return l;
	}
	
	private static Method getGetMethod(Object o, String propName) {
		Method method = null;
		String methodName = getGetMethodName("get", propName);
		try {
			method = o.getClass().getMethod(methodName);
		} catch (SecurityException e1) {
			s_logger.error("Security exception in getting POJO " + o.getClass().getName() + " get method for property: " + propName);
		} catch (NoSuchMethodException e1) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("POJO " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName + ", will check is-prefixed method to see if it is boolean property");
		}
		
		if(method != null)
			return method;
		
		methodName = getGetMethodName("is", propName);
		try {
			method = o.getClass().getMethod(methodName);
		} catch (SecurityException e1) {
			s_logger.error("Security exception in getting POJO " + o.getClass().getName() + " get method for property: " + propName);
		} catch (NoSuchMethodException e1) {
			s_logger.warn("POJO " + o.getClass().getName() + " does not have " + methodName + "() method for property: " + propName);
		}
		return method;
	}
	
	private static String getGetMethodName(String prefix, String fieldName) {
		StringBuffer sb = new StringBuffer(prefix);
		
		if(fieldName.length() >= prefix.length() && fieldName.substring(0, prefix.length()).equals(prefix)) {
			return fieldName;
		} else {
			sb.append(fieldName.substring(0, 1).toUpperCase());
			sb.append(fieldName.substring(1));
		}
		
		return sb.toString();
	}
}
