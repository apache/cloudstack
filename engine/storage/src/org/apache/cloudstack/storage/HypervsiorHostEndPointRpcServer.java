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
package org.apache.cloudstack.storage;

import javax.inject.Inject;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.rpc.RpcCallbackListener;
import org.apache.cloudstack.framework.rpc.RpcException;
import org.apache.cloudstack.framework.rpc.RpcProvider;
import org.apache.cloudstack.framework.rpc.RpcServiceDispatcher;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

@Component
public class HypervsiorHostEndPointRpcServer implements HostEndpointRpcServer {
    
    private RpcProvider _rpcProvider;
    @Inject
    public HypervsiorHostEndPointRpcServer(RpcProvider rpcProvider) {
        _rpcProvider = rpcProvider;
        _rpcProvider.registerRpcServiceEndpoint(RpcServiceDispatcher.getDispatcher(this));
    }
    
    @Override
    public void sendCommandAsync(String host, final Command command, final AsyncCompletionCallback<Answer> callback) {
        _rpcProvider.newCall(host).addCallbackListener(new RpcCallbackListener<Answer>() {
            @Override
            public void onSuccess(Answer result) {
                callback.complete(result);
            }

            @Override
            public void onFailure(RpcException e) {
                Answer answer = new Answer(command, false, e.toString());
                callback.complete(answer);
            }
        }).apply();
    }
}
