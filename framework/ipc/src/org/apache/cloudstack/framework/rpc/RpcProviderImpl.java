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
package org.apache.cloudstack.framework.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.serializer.MessageSerializer;
import org.apache.cloudstack.framework.transport.TransportAddress;
import org.apache.cloudstack.framework.transport.TransportAddressMapper;
import org.apache.cloudstack.framework.transport.TransportEndpoint;
import org.apache.cloudstack.framework.transport.TransportEndpointSite;
import org.apache.cloudstack.framework.transport.TransportProvider;

public class RpcProviderImpl implements RpcProvider {
    public static final String RPC_MULTIPLEXIER = "rpc";

    private TransportProvider _transportProvider;
    private String _transportAddress;
    private RpcTransportEndpoint _transportEndpoint = new RpcTransportEndpoint();    // transport attachment at RPC layer

    private MessageSerializer _messageSerializer;
    private List<RpcServiceEndpoint> _serviceEndpoints = new ArrayList<RpcServiceEndpoint>();
    private Map<Long, RpcClientCall> _outstandingCalls = new HashMap<Long, RpcClientCall>();

    private long _nextCallTag = System.currentTimeMillis();

    public RpcProviderImpl() {
    }

    public RpcProviderImpl(TransportProvider transportProvider) {
        _transportProvider = transportProvider;
    }

    public TransportProvider getTransportProvider() {
        return _transportProvider;
    }

    public void setTransportProvider(TransportProvider transportProvider) {
        _transportProvider = transportProvider;
    }

    @Override
    public void onTransportMessage(String senderEndpointAddress, String targetEndpointAddress, String multiplexer, String message) {
        assert (_messageSerializer != null);

        Object pdu = _messageSerializer.serializeFrom(message);
        if (pdu instanceof RpcCallRequestPdu) {
            handleCallRequestPdu(senderEndpointAddress, targetEndpointAddress, (RpcCallRequestPdu)pdu);
        } else if (pdu instanceof RpcCallResponsePdu) {
            handleCallResponsePdu(senderEndpointAddress, targetEndpointAddress, (RpcCallResponsePdu)pdu);
        } else {
            assert (false);
        }
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
    public boolean initialize() {
        if (_transportProvider == null)
            return false;
        TransportEndpointSite endpointSite = _transportProvider.attach(_transportEndpoint, "RpcProvider");
        endpointSite.registerMultiplexier(RPC_MULTIPLEXIER, this);
        return true;
    }

    @Override
    public void registerRpcServiceEndpoint(RpcServiceEndpoint rpcEndpoint) {
        synchronized (_serviceEndpoints) {
            _serviceEndpoints.add(rpcEndpoint);
        }
    }

    @Override
    public void unregisteRpcServiceEndpoint(RpcServiceEndpoint rpcEndpoint) {
        synchronized (_serviceEndpoints) {
            _serviceEndpoints.remove(rpcEndpoint);
        }
    }

    @Override
    public RpcClientCall newCall() {
        return newCall(TransportAddress.getLocalPredefinedTransportAddress("RpcProvider").toString());
    }

    @Override
    public RpcClientCall newCall(String targetAddress) {

        long callTag = getNextCallTag();
        RpcClientCallImpl call = new RpcClientCallImpl(this);
        call.setSourceAddress(_transportAddress);
        call.setTargetAddress(targetAddress);
        call.setCallTag(callTag);

        return call;
    }

    @Override
    public RpcClientCall newCall(TransportAddressMapper targetAddress) {
        return newCall(targetAddress.getAddress());
    }

    @Override
    public void registerCall(RpcClientCall call) {
        assert (call != null);
        synchronized (this) {
            _outstandingCalls.put(((RpcClientCallImpl)call).getCallTag(), call);
        }
    }

    @Override
    public void cancelCall(RpcClientCall call) {
        synchronized (this) {
            _outstandingCalls.remove(((RpcClientCallImpl)call).getCallTag());
        }

        ((RpcClientCallImpl)call).complete(new RpcException("Call is cancelled"));
    }

    @Override
    public void sendRpcPdu(String sourceAddress, String targetAddress, String serializedPdu) {
        assert (_transportProvider != null);
        _transportProvider.sendMessage(sourceAddress, targetAddress, RpcProvider.RPC_MULTIPLEXIER, serializedPdu);
    }

    protected synchronized long getNextCallTag() {
        long tag = _nextCallTag++;
        if (tag == 0)
            tag++;

        return tag;
    }

    private void handleCallRequestPdu(String sourceAddress, String targetAddress, RpcCallRequestPdu pdu) {
        try {
            RpcServerCall call = new RpcServerCallImpl(this, sourceAddress, targetAddress, pdu);

            // TODO, we are trying to avoid locking when calling into callbacks
            // this should be optimized later
            List<RpcServiceEndpoint> endpoints = new ArrayList<RpcServiceEndpoint>();
            synchronized (_serviceEndpoints) {
                endpoints.addAll(_serviceEndpoints);
            }

            for (RpcServiceEndpoint endpoint : endpoints) {
                if (endpoint.onCallReceive(call))
                    return;
            }

            RpcCallResponsePdu responsePdu = new RpcCallResponsePdu();
            responsePdu.setCommand(pdu.getCommand());
            responsePdu.setRequestStartTick(pdu.getRequestStartTick());
            responsePdu.setRequestTag(pdu.getRequestTag());
            responsePdu.setResult(RpcCallResponsePdu.RESULT_HANDLER_NOT_EXIST);
            sendRpcPdu(targetAddress, sourceAddress, _messageSerializer.serializeTo(RpcCallResponsePdu.class, responsePdu));

        } catch (Throwable e) {

            RpcCallResponsePdu responsePdu = new RpcCallResponsePdu();
            responsePdu.setCommand(pdu.getCommand());
            responsePdu.setRequestStartTick(pdu.getRequestStartTick());
            responsePdu.setRequestTag(pdu.getRequestTag());
            responsePdu.setResult(RpcCallResponsePdu.RESULT_HANDLER_EXCEPTION);

            sendRpcPdu(targetAddress, sourceAddress, _messageSerializer.serializeTo(RpcCallResponsePdu.class, responsePdu));
        }
    }

    private void handleCallResponsePdu(String sourceAddress, String targetAddress, RpcCallResponsePdu pdu) {
        RpcClientCallImpl call = null;

        synchronized (this) {
            call = (RpcClientCallImpl)_outstandingCalls.remove(pdu.getRequestTag());
        }

        if (call != null) {
            switch (pdu.getResult()) {
                case RpcCallResponsePdu.RESULT_SUCCESSFUL:
                    call.complete(pdu.getSerializedResult());
                    break;

                case RpcCallResponsePdu.RESULT_HANDLER_NOT_EXIST:
                    call.complete(new RpcException("Handler does not exist"));
                    break;

                case RpcCallResponsePdu.RESULT_HANDLER_EXCEPTION:
                    call.complete(new RpcException("Exception in handler"));
                    break;

                default:
                    assert (false);
                    break;
            }
        }
    }

    private class RpcTransportEndpoint implements TransportEndpoint {

        @Override
        public void onTransportMessage(String senderEndpointAddress, String targetEndpointAddress, String multiplexer, String message) {

            // we won't handle generic transport message toward RPC transport endpoint
        }

        @Override
        public void onAttachConfirm(boolean bSuccess, String endpointAddress) {
            if (bSuccess)
                _transportAddress = endpointAddress;

        }

        @Override
        public void onDetachIndication(String endpointAddress) {
            if (_transportAddress != null && _transportAddress.equals(endpointAddress))
                _transportAddress = null;
        }
    }
}
