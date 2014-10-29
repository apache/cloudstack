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

public class RpcServerCallImpl implements RpcServerCall {

    private RpcProvider _rpcProvider;
    private String _sourceAddress;
    private String _targetAddress;

    private RpcCallRequestPdu _requestPdu;

    public RpcServerCallImpl(RpcProvider provider, String sourceAddress, String targetAddress, RpcCallRequestPdu requestPdu) {

        _rpcProvider = provider;
        _sourceAddress = sourceAddress;
        _targetAddress = targetAddress;
        _requestPdu = requestPdu;
    }

    @Override
    public String getCommand() {
        assert (_requestPdu != null);
        return _requestPdu.getCommand();
    }

    @Override
    public <T> T getCommandArgument() {
        if (_requestPdu.getSerializedCommandArg() == null)
            return null;

        assert (_rpcProvider.getMessageSerializer() != null);
        return _rpcProvider.getMessageSerializer().serializeFrom(_requestPdu.getSerializedCommandArg());
    }

    @Override
    public void completeCall(Object returnObject) {
        assert (_sourceAddress != null);
        assert (_targetAddress != null);

        RpcCallResponsePdu pdu = new RpcCallResponsePdu();
        pdu.setCommand(_requestPdu.getCommand());
        pdu.setRequestTag(_requestPdu.getRequestTag());
        pdu.setRequestStartTick(_requestPdu.getRequestStartTick());
        pdu.setRequestStartTick(RpcCallResponsePdu.RESULT_SUCCESSFUL);
        if (returnObject != null) {
            assert (_rpcProvider.getMessageSerializer() != null);
            pdu.setSerializedResult(_rpcProvider.getMessageSerializer().serializeTo(returnObject.getClass(), returnObject));
        }

        _rpcProvider.sendRpcPdu(_sourceAddress,_targetAddress,_rpcProvider.getMessageSerializer().serializeTo(RpcCallResponsePdu.class, pdu));
    }
}
