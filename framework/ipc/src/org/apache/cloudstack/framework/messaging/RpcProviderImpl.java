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

import java.util.ArrayList;
import java.util.List;

public class RpcProviderImpl implements RpcProvider {
	
	private MessageSerializer _messageSerializer;
	private List<RpcServiceEndpoint> _serviceEndpoints = new ArrayList<RpcServiceEndpoint>();
	private TransportProvider _transportProvider;
	
	public RpcProviderImpl() {
	}
	
	public TransportProvider getTransportProvider() {
		return _transportProvider;
	}
	
	public void setTransportProvider(TransportProvider transportProvider) {
		_transportProvider = transportProvider;
	}
	
	@Override
	public void onTransportMessage(String senderEndpointAddress,
		String targetEndpointAddress, String multiplexer, String message) {

		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMessageSerializer(MessageSerializer messageSerializer) {
		_messageSerializer = messageSerializer;
	}

	@Override
	public MessageSerializer getMessageSerializer() {
		return _messageSerializer;
	}

	@Override
	public void registerRpcServiceEndpoint(RpcServiceEndpoint rpcEndpoint) {
		synchronized(_serviceEndpoints) {
			_serviceEndpoints.add(rpcEndpoint);
		}
	}

	@Override
	public void unregisteRpcServiceEndpoint(RpcServiceEndpoint rpcEndpoint) {
		synchronized(_serviceEndpoints) {
			_serviceEndpoints.remove(rpcEndpoint);
		}
	}

	@Override
	public RpcClientCall target(String target) {
		// TODO Auto-generated method stub
		return null;
	}
}
