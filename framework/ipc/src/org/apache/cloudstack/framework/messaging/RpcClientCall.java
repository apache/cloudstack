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

import java.util.concurrent.TimeUnit;

public interface RpcClientCall {
	String getCommand();
	RpcClientCall setCommand(String cmd);
	RpcClientCall setTimeout(TimeUnit timeout);
	
	RpcClientCall setCommandArg(Object arg);
	Object getCommandArg();
	
	RpcClientCall setContextParam(String key, Object param);
	Object getContextParam(String key);
	
	<T> RpcClientCall addCallbackListener(RpcCallbackListener<T> listener);
	
	void apply();
	void cancel();
	
	/**
	 * @return the result object， it may also throw RpcException to indicate RPC failures 
	 */
	<T> T get();
}
