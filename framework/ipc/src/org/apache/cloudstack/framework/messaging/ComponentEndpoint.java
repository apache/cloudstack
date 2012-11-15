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

public class ComponentEndpoint implements RpcEndpoint, Subscriber {
	private TransportEndpoint transportEndpoint;
	private RpcProvider rpcProvider;
	
	public ComponentEndpoint() {
	}
	
	public TransportEndpoint getTransportEndpoint() {
		return transportEndpoint;
	}

	public void setTransportEndpoint(TransportEndpoint transportEndpoint) {
		this.transportEndpoint = transportEndpoint;
	}

	public RpcProvider getRpcProvider() {
		return rpcProvider;
	}

	public void setRpcProvider(RpcProvider rpcProvider) {
		this.rpcProvider = rpcProvider;
	}
	
	public void initialize() {
		rpcProvider.registerRpcEndpoint(this);
	}

	// it will throw RpcRuntimeException in case of transport
	public String call(RpcCallContext callContext, String targetAddress, String command, Object cmdArg)
	{
		return rpcProvider.call(this, callContext, targetAddress, command, cmdArg);
	}
	
	public RpcClientCall asyncCall(RpcCallContext callContext, String targetAddress, String command, Object cmdArg) {
		return rpcProvider.asyncCall(this, callContext, targetAddress, command, cmdArg);
	}

	@Override
	public void onCallReceive(RpcServerCall call) {
		// TODO Auto-generated method stub
		// implement annotation based call dispatching
	}
	
	@Override
	public void onCallReturn(RpcClientCall call, Object returnObject, RpcException e) {
		// ???
	}
	
	@Override
	public void onPublishEvent(String subject, String senderAddress, Object args) {
		// TODO
	}
}
