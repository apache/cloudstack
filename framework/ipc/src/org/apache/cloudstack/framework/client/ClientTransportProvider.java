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
package org.apache.cloudstack.framework.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.serializer.MessageSerializer;
import org.apache.cloudstack.framework.transport.TransportEndpoint;
import org.apache.cloudstack.framework.transport.TransportEndpointSite;
import org.apache.cloudstack.framework.transport.TransportProvider;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.utils.concurrency.NamedThreadFactory;

public class ClientTransportProvider implements TransportProvider {
    final static Logger s_logger = Logger.getLogger(ClientTransportProvider.class);
    public static final int DEFAULT_WORKER_POOL_SIZE = 5;

    private final Map<Integer, ClientTransportEndpointSite> _endpointSites = new HashMap<Integer, ClientTransportEndpointSite>();
    private final Map<String, ClientTransportEndpointSite> _attachedMap = new HashMap<String, ClientTransportEndpointSite>();

    private MessageSerializer _messageSerializer;

    private ClientTransportConnection _connection;
    private String _serverAddress;
    private int _serverPort;

    private int _poolSize = DEFAULT_WORKER_POOL_SIZE;
    private ExecutorService _executor;

    private int _nextProviderKey = 1;

    public ClientTransportProvider() {
    }

    public ClientTransportProvider setPoolSize(int poolSize) {
        _poolSize = poolSize;
        return this;
    }

    public void initialize(String serverAddress, int serverPort) {
        _serverAddress = serverAddress;
        _serverPort = serverPort;

        _executor = Executors.newFixedThreadPool(_poolSize, new NamedThreadFactory("Transport-Worker"));
        _connection = new ClientTransportConnection(this);

        _executor.execute(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    _connection.connect(_serverAddress, _serverPort);
                } catch (Throwable e) {
                    s_logger.info("[ignored]"
                            + "error during ipc client initialization: " + e.getLocalizedMessage());
                }
            }
        });
    }

    @Override
    public TransportEndpointSite attach(TransportEndpoint endpoint, String predefinedAddress) {

        ClientTransportEndpointSite endpointSite;
        synchronized (this) {
            endpointSite = getEndpointSite(endpoint);
            if (endpointSite != null) {
                // already attached
                return endpointSite;
            }

            endpointSite = new ClientTransportEndpointSite(this, endpoint, predefinedAddress, getNextProviderKey());
            _endpointSites.put(endpointSite.getProviderKey(), endpointSite);
        }

        return endpointSite;
    }

    @Override
    public boolean detach(TransportEndpoint endpoint) {
        // TODO Auto-generated method stub

        return false;
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

    @Override
    public void requestSiteOutput(TransportEndpointSite site) {
        // ???
    }

    @Override
    public void sendMessage(String soureEndpointAddress, String targetEndpointAddress, String multiplexier, String message) {
        // TODO
    }

    private ClientTransportEndpointSite getEndpointSite(TransportEndpoint endpoint) {
        synchronized (this) {
            for (ClientTransportEndpointSite endpointSite : _endpointSites.values()) {
                if (endpointSite.getEndpoint() == endpoint)
                    return endpointSite;
            }
        }

        return null;
    }

    public int getNextProviderKey() {
        synchronized (this) {
            return _nextProviderKey++;
        }
    }
}
