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
package org.apache.cloudstack.framework.rpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class RpcCallbackDispatcher {

	private static Map<Class<?>, Map<String, Method>> s_handlerCache = new HashMap<Class<?>, Map<String, Method>>();
	
	public static boolean dispatch(Object target, RpcClientCall clientCall) {
		assert(clientCall != null);
		assert(target != null);
		
		Method handler = resolveHandler(target.getClass(), clientCall.getCommand());
		if(handler == null)
			return false;
		
		try {
			handler.invoke(target, clientCall);
		} catch (IllegalArgumentException e) {
			throw new RpcException("IllegalArgumentException when invoking RPC callback for command: " + clientCall.getCommand());
		} catch (IllegalAccessException e) {
			throw new RpcException("IllegalAccessException when invoking RPC callback for command: " + clientCall.getCommand());
		} catch (InvocationTargetException e) {
			throw new RpcException("InvocationTargetException when invoking RPC callback for command: " + clientCall.getCommand());
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
				RpcCallbackHandler annotation = method.getAnnotation(RpcCallbackHandler.class);
				if(annotation != null) {
					if(annotation.command().equals(command)) {
						method.setAccessible(true);
						handlerMap.put(command, method);
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
