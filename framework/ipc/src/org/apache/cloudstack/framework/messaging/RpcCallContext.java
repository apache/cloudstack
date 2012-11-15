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
package org.apache.cloudstack.framework.messaging;

import java.util.HashMap;
import java.util.Map;

public class RpcCallContext {
	private final static int DEFAULT_RPC_TIMEOUT = 10000;
	
	Map<String, Object> _contextMap = new HashMap<String, Object>();
	int _timeoutMilliSeconds = DEFAULT_RPC_TIMEOUT;
	
	public RpcCallContext() {
	}
	
	public int getTimeoutMilliSeconds() {
		return _timeoutMilliSeconds;
	}
	
	public void setTimeoutMilliSeconds(int timeoutMilliseconds) {
		_timeoutMilliSeconds = timeoutMilliseconds;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getContextParameter(String key) {
		return (T)_contextMap.get(key);
	}
	
	public void setContextParameter(String key, Object object) {
		_contextMap.put(key, object);
	}
}
