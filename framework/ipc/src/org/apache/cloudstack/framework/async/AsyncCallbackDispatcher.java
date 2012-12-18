/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.framework.async;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("rawtypes")
public class AsyncCallbackDispatcher implements AsyncCompletionCallback {
	private static Map<Class<?>, Map<String, Method>> s_handlerCache = new HashMap<Class<?>, Map<String, Method>>();
	private final String parentCallbackKey = "parentCallback";
	private Map<String, Object> _contextMap = new HashMap<String, Object>();
	private String _operationName;
	private Object _targetObject;
	private Object _resultObject;
	private AsyncCallbackDriver _driver = new InplaceAsyncCallbackDriver(); 
	
	public AsyncCallbackDispatcher(Object target) {
		assert(target != null);
		_targetObject = target;
	}
	
	public AsyncCallbackDispatcher setContextParam(String key, Object param) {
		_contextMap.put(key, param);
		return this;
	}
	
	public <T> AsyncCallbackDispatcher setParentCallback(AsyncCompletionCallback<T> parentCallback) {
	    _contextMap.put(parentCallbackKey, parentCallback);
	    return this;
	}
	
	public AsyncCallbackDispatcher getParentCallback() {
	   return (AsyncCallbackDispatcher)_contextMap.get(parentCallbackKey);
	}
	
	public AsyncCallbackDispatcher attachDriver(AsyncCallbackDriver driver) {
		assert(driver != null);
		_driver = driver;
		
		return this;
	}
	
	public AsyncCallbackDispatcher setOperationName(String name) {
		_operationName = name;
		return this;
	}
	
	public String getOperationName() {
		return _operationName;
	}
	
	public Object getTargetObject() {
		return _targetObject;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getContextParam(String key) {
		return (T)_contextMap.get(key);
	}
	
	public void complete(Object resultObject) {
		_resultObject = resultObject;
		_driver.performCompletionCallback(this);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getResult() {
		return (T)_resultObject;
	}
	
	public static boolean dispatch(Object target, AsyncCallbackDispatcher callback) {
		assert(callback != null);
		assert(target != null);
		
		Method handler = resolveHandler(target.getClass(), callback.getOperationName());
		if(handler == null)
			return false;
		
		try {
			handler.invoke(target, callback);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("IllegalArgumentException when invoking RPC callback for command: " + callback.getOperationName());
		} catch (IllegalAccessException e) {
			throw new RuntimeException("IllegalAccessException when invoking RPC callback for command: " + callback.getOperationName());
		} catch (InvocationTargetException e) {
			throw new RuntimeException("InvocationTargetException when invoking RPC callback for command: " + callback.getOperationName(), e);
		}
		
		return true;
	}
	
	public static Method resolveHandler(Class<?> handlerClz, String command) {
		synchronized(s_handlerCache) {
			Map<String, Method> handlerMap = getAndSetHandlerMap(handlerClz);
				
			Method handler = handlerMap.get(command);
			if(handler != null)
				return handler;
			
			for(Method method : handlerClz.getDeclaredMethods()) {
				AsyncCallbackHandler annotation = method.getAnnotation(AsyncCallbackHandler.class);
				if(annotation != null) {
					if(annotation.operationName().equals(command)) {
						handlerMap.put(command, method);
						method.setAccessible(true);
						return method;
					}
				}
			}
		}
		
		return null;
	}
	
	private static Map<String, Method> getAndSetHandlerMap(Class<?> handlerClz) {
		Map<String, Method> handlerMap;
		synchronized(s_handlerCache) {
			handlerMap = s_handlerCache.get(handlerClz);
			
			if(handlerMap == null) {
				handlerMap = new HashMap<String, Method>();
				s_handlerCache.put(handlerClz, handlerMap);
			}
		}
		
		return handlerMap;
	}
}
