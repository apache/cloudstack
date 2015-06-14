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
package org.apache.cloudstack.framework.server;

import java.util.HashMap;
import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.serializer.MessageSerializer;
import org.apache.cloudstack.framework.transport.TransportAddress;
import org.apache.cloudstack.framework.transport.TransportDataPdu;
import org.apache.cloudstack.framework.transport.TransportEndpoint;
import org.apache.cloudstack.framework.transport.TransportEndpointSite;
import org.apache.cloudstack.framework.transport.TransportPdu;
import org.apache.cloudstack.framework.transport.TransportProvider;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.utils.concurrency.NamedThreadFactory;

public class ServerTransportProvider implements TransportProvider {
    private static final Logger s_logger = Logger.getLogger(ServerTransportProvider.class);

    public static final int DEFAULT_WORKER_POOL_SIZE = 5;

    private String _nodeId;

    private Map<String, TransportEndpointSite> _endpointMap = new HashMap<String, TransportEndpointSite>();
    private int _poolSize = DEFAULT_WORKER_POOL_SIZE;
    private ExecutorService _executor;
    private final SecureRandom randomGenerator;
    private int _nextEndpointId;

    private MessageSerializer _messageSerializer;

    public ServerTransportProvider() {
       randomGenerator=new SecureRandom();
       _nextEndpointId=randomGenerator.nextInt();
    }

    public String getNodeId() {
        return _nodeId;
    }

    public ServerTransportProvider setNodeId(String nodeId) {
        _nodeId = nodeId;
        return this;
    }

    public int getWorkerPoolSize() {
        return _poolSize;
    }

    public ServerTransportProvider setWorkerPoolSize(int poolSize) {
        assert (poolSize > 0);

        _poolSize = poolSize;
        return this;
    }

    @Override
    public void setMessageSerializer(MessageSerializer messageSerializer) {
        assert (messageSerializer != null);
        _messageSerializer = messageSerializer;
    }

    @Override
    public MessageSerializer getMessageSerializer() {
        return _messageSerializer;
    }

    public void initialize() {
        _executor = Executors.newFixedThreadPool(_poolSize, new NamedThreadFactory("Transport-Worker"));
    }

    @Override
    public TransportEndpointSite attach(TransportEndpoint endpoint, String predefinedAddress) {

        TransportAddress transportAddress;
        String endpointId;
        if (predefinedAddress != null && !predefinedAddress.isEmpty()) {
            endpointId = predefinedAddress;
            transportAddress = new TransportAddress(_nodeId, TransportAddress.LOCAL_SERVICE_CONNECTION, endpointId, 0);
        } else {
            endpointId = String.valueOf(getNextEndpointId());
            transportAddress = new TransportAddress(_nodeId, TransportAddress.LOCAL_SERVICE_CONNECTION, endpointId);
        }

        TransportEndpointSite endpointSite;
        synchronized (this) {
            endpointSite = _endpointMap.get(endpointId);
            if (endpointSite != null) {
                // already attached
                return endpointSite;
            }
            endpointSite = new TransportEndpointSite(this, endpoint, transportAddress);
            _endpointMap.put(endpointId, endpointSite);
        }

        endpoint.onAttachConfirm(true, transportAddress.toString());
        return endpointSite;
    }

    @Override
    public boolean detach(TransportEndpoint endpoint) {
        synchronized (this) {
            for (Map.Entry<String, TransportEndpointSite> entry : _endpointMap.entrySet()) {
                if (entry.getValue().getEndpoint() == endpoint) {
                    _endpointMap.remove(entry.getKey());
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void requestSiteOutput(final TransportEndpointSite site) {
        _executor.execute(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    site.processOutput();
                    site.ackOutputProcessSignal();
                } catch (Throwable e) {
                    s_logger.error("Unhandled exception", e);
                }
            }
        });
    }

    @Override
    public void sendMessage(String sourceEndpointAddress, String targetEndpointAddress, String multiplexier, String message) {

        TransportDataPdu pdu = new TransportDataPdu();
        pdu.setSourceAddress(sourceEndpointAddress);
        pdu.setDestAddress(targetEndpointAddress);
        pdu.setMultiplexier(multiplexier);
        pdu.setContent(message);

        dispatchPdu(pdu);
    }

    private void dispatchPdu(TransportPdu pdu) {

        TransportAddress transportAddress = TransportAddress.fromAddressString(pdu.getDestAddress());

        if (isLocalAddress(transportAddress)) {
            TransportEndpointSite endpointSite = null;
            synchronized (this) {
                endpointSite = _endpointMap.get(transportAddress.getEndpointId());
            }

            if (endpointSite != null)
                endpointSite.addOutputPdu(pdu);
        } else {
            // do cross-node forwarding
            // ???
        }
    }

    private boolean isLocalAddress(TransportAddress address) {
        if (address.getNodeId().equals(_nodeId) || address.getNodeId().equals(TransportAddress.LOCAL_SERVICE_NODE))
            return true;

        return false;
    }

    private synchronized int getNextEndpointId() {
        return _nextEndpointId++;
    }
}
