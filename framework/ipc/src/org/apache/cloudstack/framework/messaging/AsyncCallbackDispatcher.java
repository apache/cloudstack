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

package org.apache.cloudstack.framework.messaging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AsyncCallbackDispatcher {
	private static Map<Class<?>, Method> s_handlerCache = new HashMap<Class<?>, Method>();
	
	public static boolean dispatch(Object target, AsyncCompletionCallback callback) {
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
			throw new RuntimeException("InvocationTargetException when invoking RPC callback for command: " + callback.getOperationName());
		}
		
		return true;
	}
	
	public static Method resolveHandler(Class<?> handlerClz, String operationName) {
		synchronized(s_handlerCache) {
			Method handler = s_handlerCache.get(handlerClz);
			if(handler != null)
				return handler;
			
			for(Method method : handlerClz.getMethods()) {
				AsyncCallbackHandler annotation = method.getAnnotation(AsyncCallbackHandler.class);
				if(annotation != null) {
					if(annotation.operationName().equals(operationName)) {
						s_handlerCache.put(handlerClz, method);
						return method;
					}
				}
			}
		}
		
		return null;
	}
}
