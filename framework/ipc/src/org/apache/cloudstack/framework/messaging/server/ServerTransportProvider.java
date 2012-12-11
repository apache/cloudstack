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
package org.apache.cloudstack.framework.messaging.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.cloudstack.framework.messaging.TransportAddress;
import org.apache.cloudstack.framework.messaging.TransportDataPdu;
import org.apache.cloudstack.framework.messaging.TransportEndpoint;
import org.apache.cloudstack.framework.messaging.TransportEndpointSite;
import org.apache.cloudstack.framework.messaging.TransportPdu;
import org.apache.cloudstack.framework.messaging.TransportProvider;

public class ServerTransportProvider implements TransportProvider {
	private String _nodeId;

	private Map<String, TransportEndpointSite> _endpointMap = new HashMap<String, TransportEndpointSite>();
	
	private int _nextEndpointId = new Random().nextInt();
	
	public ServerTransportProvider() {
	}
	
	public String getNodeId() { return _nodeId; }
	public void setNodeId(String nodeId) {
		_nodeId = nodeId;
	}
	
	@Override
	public boolean attach(TransportEndpoint endpoint, String predefinedAddress) {
		
		TransportAddress transportAddress;
		String endpointId;
		if(predefinedAddress != null && !predefinedAddress.isEmpty()) {
			endpointId = predefinedAddress;
			transportAddress = new TransportAddress(_nodeId, endpointId, 0);
		} else {
			endpointId = String.valueOf(getNextEndpointId());
			transportAddress = new TransportAddress(_nodeId, endpointId);
		}
		
		TransportEndpointSite endpointSite;
		synchronized(this) {
			endpointSite = _endpointMap.get(endpointId);
			if(endpointSite != null) {
				// already attached
				return false;
			}
			endpointSite = new TransportEndpointSite(endpoint, transportAddress);
			_endpointMap.put(endpointId, endpointSite);
		}
		
		endpoint.onAttachConfirm(true, transportAddress.toString());
		return true;
	}

	@Override
	public boolean detach(TransportEndpoint endpoint) {
		TransportAddress transportAddress = TransportAddress.fromAddressString(endpoint.getEndpointAddress());
		if(transportAddress == null)
			return false;
		
		boolean found = false;
		synchronized(this) {
			TransportEndpointSite endpointSite = _endpointMap.get(transportAddress.getEndpointId());
			if(endpointSite.getAddress().equals(transportAddress)) {
				found = true;
				_endpointMap.remove(transportAddress.getEndpointId());
			}
		}
		
		if(found) {
			endpoint.onDetachIndication(endpoint.getEndpointAddress());
			return true;
		}
			
		return false;
	}
	
	@Override
	public void sendMessage(String sourceEndpointAddress, String targetEndpointAddress, 
		String multiplexier, String message) {
		
		TransportDataPdu pdu = new TransportDataPdu();
		pdu.setSourceAddress(sourceEndpointAddress);
		pdu.setDestAddress(targetEndpointAddress);
		pdu.setMultiplexier(multiplexier);
		pdu.setContent(message);
		
		dispatchPdu(pdu);
	}
	
	private void dispatchPdu(TransportPdu pdu) {
		
		TransportAddress transportAddress = TransportAddress.fromAddressString(pdu.getDestAddress());
		
		if(isLocalAddress(transportAddress)) {
			TransportEndpointSite endpointSite = null;
			synchronized(this) {
				endpointSite = _endpointMap.get(transportAddress.getEndpointId());
			}
			
			if(endpointSite != null)
				endpointSite.addOutputPdu(pdu);
		} else {
			// do cross-node forwarding
		}
	}
	
	private boolean isLocalAddress(TransportAddress address) {
		if(address.getNodeId().equals(_nodeId) || address.getNodeId().equals(TransportAddress.LOCAL_SERVICE_NODE))
			return true;
		
		return false;
	}
	
	private synchronized int getNextEndpointId() {
		return _nextEndpointId++;
	}
}
