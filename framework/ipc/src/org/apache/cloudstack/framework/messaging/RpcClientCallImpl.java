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

public class RpcClientCallImpl implements RpcClientCall {

	private String _command;
	private Object _commandArg;
	
	private int _timeoutMilliseconds;
	
	private Map<String, Object> _contextParams = new HashMap<String, Object>();
	
	public RpcClientCallImpl() {
	}
	
	@Override
	public String getCommand() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RpcClientCall setCommand(String cmd) {
		_command = cmd;
		return this;
	}

	@Override
	public RpcClientCall setTimeout(int timeoutMilliseconds) {
		_timeoutMilliseconds = timeoutMilliseconds;
		return this;
	}

	@Override
	public RpcClientCall setCommandArg(Object arg) {
		_commandArg = arg;
		return this;
	}

	@Override
	public Object getCommandArg() {
		return _commandArg;
	}

	@Override
	public RpcClientCall setContextParam(String key, Object param) {
		assert(key != null);
		_contextParams.put(key, param);
		return this;
	}

	@Override
	public Object getContextParam(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> RpcClientCall addCallbackListener(RpcCallbackListener<T> listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RpcClientCall setOneway() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void apply() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T get() {
		// TODO Auto-generated method stub
		return null;
	}
}
