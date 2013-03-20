// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.utils.component;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

public class ComponentMethodProxyCache {

	private static WeakHashMap<TargetKey, WeakReference<Method>> s_cache = new WeakHashMap<TargetKey, WeakReference<Method>>();
	
	public ComponentMethodProxyCache() {
	}
	
	public static Method getTargetMethod(Method method, Object target) {
		synchronized(s_cache) {
			WeakReference<Method> targetMethod = s_cache.get(new TargetKey(method, target));
			if(targetMethod != null && targetMethod.get() != null)
				return targetMethod.get();

        	Class<?> clazz = target.getClass();
        	for(Method m : clazz.getMethods()) {
        		if(isMethodMatched(method, m)) {
        			s_cache.put(new TargetKey(method, target), new WeakReference<Method>(m));
        			return m;
        		}
        	}
			
        	return method;
		}
	}
	
	private static boolean isMethodMatched(Method m1, Method m2) {
		if(!m1.getName().equals(m2.getName()))
			return false;
		
		Class<?>[] params1 = m1.getParameterTypes();
		Class<?>[] params2 = m2.getParameterTypes();
		
		if(params1.length != params2.length)
			return false;
		
		for(int i = 0; i < params1.length; i++) {
			if(!params1[i].isAssignableFrom(params2[i]))
				return false;
		}
		
		return true;
	}
	
	public static class TargetKey {
		Method _method;
		Object _target;
		
		public TargetKey(Method method, Object target) {
			_method = method;
			_target = target;
		}
		
		@Override
	    public boolean equals(Object obj) {
			if(!(obj instanceof TargetKey))
				return false;
			
			// for target object, we just check the reference
			return _method.equals(((TargetKey)obj)._method) &&
					_target == ((TargetKey)obj)._target;
		}
		
	    public int hashCode() {
	    	return _target.hashCode() ^ _target.hashCode();
	    }
	}
}
