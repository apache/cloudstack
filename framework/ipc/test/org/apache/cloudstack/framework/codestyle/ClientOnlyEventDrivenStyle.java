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
package org.apache.cloudstack.framework.codestyle;

import org.apache.cloudstack.framework.rpc.RpcCallbackHandler;
import org.apache.cloudstack.framework.rpc.RpcClientCall;
import org.apache.cloudstack.framework.rpc.RpcException;
import org.apache.cloudstack.framework.rpc.RpcIOException;
import org.apache.cloudstack.framework.rpc.RpcProvider;
import org.apache.cloudstack.framework.rpc.RpcTimeoutException;

public class ClientOnlyEventDrivenStyle {
	RpcProvider _rpcProvider;
	
	public void AsyncCallRpcService() {
		String cmd = new String();
		_rpcProvider.newCall("host-2").setCommand("TestCommand").setCommandArg(cmd).setTimeout(10000)
			.setCallbackDispatcherTarget(this)
			.setContextParam("origCmd", cmd)		// save context object for callback handler
			.apply();
	}
	
	@RpcCallbackHandler(command="TestCommand")
	public void OnAsyncCallRpcServiceCallback(RpcClientCall call) {
		try {
			String origCmd = call.getContextParam("origCmd");	// restore calling context at callback handler	

			String answer = call.get();
			
		} catch(RpcTimeoutException e) {
			
		} catch(RpcIOException e) {
			
		} catch(RpcException e) {
		}
	}
}
