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

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

@SuppressWarnings("rawtypes")
public class AsyncCallbackDispatcher<T> implements AsyncCompletionCallback {
	private AsyncCallbackDispatcher _parent;
	
	private Method _callbackMethod;
	private T _targetObject;
	private Object _contextObject;
	private Object _resultObject;
	private AsyncCallbackDriver _driver = new InplaceAsyncCallbackDriver(); 
	
	private AsyncCallbackDispatcher(T target) {
		assert(target != null);
		_targetObject = target;
	}
	
	private AsyncCallbackDispatcher(T target, AsyncCallbackDispatcher parent) {
		assert(target != null);
		_targetObject = target;
		_parent = parent;
	}
	
	public AsyncCallbackDispatcher<T> attachDriver(AsyncCallbackDriver driver) {
		assert(driver != null);
		_driver = driver;
		
		return this;
	}
	
	public Method getCallbackMethod() {
		return _callbackMethod;
	}
	
	@SuppressWarnings("unchecked")
	public T getTarget() {
		return (T)Enhancer.create(_targetObject.getClass(), new MethodInterceptor() {
			@Override
			public Object intercept(Object arg0, Method arg1, Object[] arg2,
				MethodProxy arg3) throws Throwable {
				_callbackMethod = arg1;
				return null;
			}
		});
	}

	public AsyncCallbackDispatcher<T> setCallback(Object useless) {
		return this;
	}
	
	public AsyncCallbackDispatcher<T> setContext(Object context) {
		_contextObject = context;
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <P> P getContext() {
		return (P)_contextObject;
	}
	
	public void complete(Object resultObject) {
		_resultObject = resultObject;
		_driver.performCompletionCallback(this);
	}

	public void deepComplete(Object resultObject) {
		complete(resultObject);
		if(_parent != null)
			_parent.deepComplete(resultObject);
	}
	
	@SuppressWarnings("unchecked")
	public <R> R getResult() {
		return (R)_resultObject;
	}

	// for internal use
	Object getTargetObject() {
		return _targetObject;
	}
	
	public static <P> AsyncCallbackDispatcher<P> create(P target)  {
		return new AsyncCallbackDispatcher<P>(target);
	}
	
	public <P> AsyncCallbackDispatcher<P> chainToCreate(P target) {
		return new AsyncCallbackDispatcher<P>(target, this);
	}
	
	@SuppressWarnings("unchecked")
	public <P> AsyncCallbackDispatcher<P> getParent() {
		return (AsyncCallbackDispatcher<P>)_parent;
	}
	
	public static boolean dispatch(Object target, AsyncCallbackDispatcher callback) {
		assert(callback != null);
		assert(target != null);
		
		try {
			callback.getCallbackMethod().invoke(target, callback, callback.getContext());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("IllegalArgumentException when invoking RPC callback for command: " + callback.getCallbackMethod().getName());
		} catch (IllegalAccessException e) {
			throw new RuntimeException("IllegalAccessException when invoking RPC callback for command: " + callback.getCallbackMethod().getName());
		} catch (InvocationTargetException e) {
			throw new RuntimeException("InvocationTargetException when invoking RPC callback for command: " + callback.getCallbackMethod().getName());
		}
		
		return true;
	}
}
