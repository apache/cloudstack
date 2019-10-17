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
package org.apache.cloudstack.framework.transport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportEndpointSite {
    private TransportProvider _provider;
    private TransportEndpoint _endpoint;
    private TransportAddress _address;

    private List<TransportPdu> _outputQueue = new ArrayList<TransportPdu>();
    private Map<String, TransportMultiplexier> _multiplexierMap = new HashMap<String, TransportMultiplexier>();

    private int _outstandingSignalRequests;

    public TransportEndpointSite(TransportProvider provider, TransportEndpoint endpoint, TransportAddress address) {
        assert (provider != null);
        assert (endpoint != null);
        assert (address != null);

        _provider = provider;
        _endpoint = endpoint;
        _address = address;

        _outstandingSignalRequests = 0;
    }

    public TransportEndpointSite(TransportProvider provider, TransportEndpoint endpoint) {
        assert (provider != null);
        assert (endpoint != null);

        _provider = provider;
        _endpoint = endpoint;

        _outstandingSignalRequests = 0;
    }

    public TransportEndpoint getEndpoint() {
        return _endpoint;
    }

    public TransportAddress getAddress() {
        return _address;
    }

    public void setAddress(TransportAddress address) {
        _address = address;
    }

    public void registerMultiplexier(String name, TransportMultiplexier multiplexier) {
        assert (name != null);
        assert (multiplexier != null);
        assert (_multiplexierMap.get(name) == null);

        _multiplexierMap.put(name, multiplexier);
    }

    public void unregisterMultiplexier(String name) {
        assert (name != null);
        _multiplexierMap.remove(name);
    }

    public void addOutputPdu(TransportPdu pdu) {
        synchronized (this) {
            _outputQueue.add(pdu);
        }

        signalOutputProcessRequest();
    }

    public TransportPdu getNextOutputPdu() {
        synchronized (this) {
            if (_outputQueue.size() > 0)
                return _outputQueue.remove(0);
        }

        return null;
    }

    public void processOutput() {
        TransportPdu pdu;
        TransportEndpoint endpoint = getEndpoint();

        if (endpoint != null) {
            while ((pdu = getNextOutputPdu()) != null) {
                if (pdu instanceof TransportDataPdu) {
                    String multiplexierName = ((TransportDataPdu)pdu).getMultiplexier();
                    TransportMultiplexier multiplexier = getRoutedMultiplexier(multiplexierName);
                    assert (multiplexier != null);
                    multiplexier.onTransportMessage(pdu.getSourceAddress(), pdu.getDestAddress(), multiplexierName, ((TransportDataPdu)pdu).getContent());
                }
            }
        }
    }

    private TransportMultiplexier getRoutedMultiplexier(String multiplexierName) {
        TransportMultiplexier multiplexier = _multiplexierMap.get(multiplexierName);
        if (multiplexier == null)
            multiplexier = _endpoint;

        return multiplexier;
    }

    private void signalOutputProcessRequest() {
        boolean proceed = false;
        synchronized (this) {
            if (_outstandingSignalRequests == 0) {
                _outstandingSignalRequests++;
                proceed = true;
            }
        }

        if (proceed)
            _provider.requestSiteOutput(this);
    }

    public void ackOutputProcessSignal() {
        synchronized (this) {
            assert (_outstandingSignalRequests == 1);
            _outstandingSignalRequests--;
        }
    }
}
