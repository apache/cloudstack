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

import org.apache.cloudstack.framework.serializer.MessageSerializer;
import org.apache.cloudstack.framework.transport.TransportAddressMapper;
import org.apache.cloudstack.framework.transport.TransportMultiplexier;

public interface RpcProvider extends TransportMultiplexier {
    final static String RPC_MULTIPLEXIER = "rpc";

    void setMessageSerializer(MessageSerializer messageSerializer);

    MessageSerializer getMessageSerializer();

    boolean initialize();

    void registerRpcServiceEndpoint(RpcServiceEndpoint rpcEndpoint);

    void unregisteRpcServiceEndpoint(RpcServiceEndpoint rpcEndpoint);

    RpcClientCall newCall();

    RpcClientCall newCall(String targetAddress);

    RpcClientCall newCall(TransportAddressMapper targetAddress);

    //
    // low-level public API
    //
    void registerCall(RpcClientCall call);

    void cancelCall(RpcClientCall call);

    void sendRpcPdu(String sourceAddress, String targetAddress, String serializedPdu);
}
