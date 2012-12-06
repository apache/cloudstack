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

import java.util.HashMap;
import java.util.Map;

public class AsyncCompletionCallback {	
	private Map<String, Object> _contextMap = new HashMap<String, Object>();
	private String _operationName;
	private Object _targetObject;
	
	public AsyncCompletionCallback(Object target) {
		_targetObject = target;
	}
	
	public AsyncCompletionCallback setContextParam(String key, Object param) {
		// ???
		return this;
	}
	
	public AsyncCompletionCallback attachDriver(AsyncCallbackDriver driver) {
		// ???
		return this;
	}
	
	public AsyncCompletionCallback setOperationName(String name) {
		_operationName = name;
		return this;
	}
	
	public String getOperationName() {
		return _operationName;
	}
	
	public <T> T getContextParam(String key) {
		// ???
		return null;
	}
	
	public void complete(Object resultObject) {
		///
	}
	
	public <T> T getResult() {
		
		// ???
		return null;
	}
}
