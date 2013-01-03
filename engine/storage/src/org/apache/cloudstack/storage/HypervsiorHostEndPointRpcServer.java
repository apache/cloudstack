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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.framework.rpc.RpcCallbackListener;
import org.apache.cloudstack.framework.rpc.RpcException;
import org.apache.cloudstack.framework.rpc.RpcProvider;
import org.apache.cloudstack.framework.rpc.RpcServiceDispatcher;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class HypervsiorHostEndPointRpcServer implements HostEndpointRpcServer {
    private static final Logger s_logger = Logger.getLogger(HypervsiorHostEndPointRpcServer.class);
    
    @Inject
    private RpcProvider _rpcProvider;
    
    public HypervsiorHostEndPointRpcServer() {
    }
    
    public HypervsiorHostEndPointRpcServer(RpcProvider rpcProvider) {
        _rpcProvider = rpcProvider;
        _rpcProvider.registerRpcServiceEndpoint(RpcServiceDispatcher.getDispatcher(this));
    }
    
    @PostConstruct
    public void Initialize() {
        _rpcProvider.registerRpcServiceEndpoint(RpcServiceDispatcher.getDispatcher(this));
    }
    
    @Override
    public void sendCommandAsync(HypervisorHostEndPoint host, final Command command, final AsyncCompletionCallback<Answer> callback) {
        _rpcProvider.newCall(host.getHostAddr()).addCallbackListener(new RpcCallbackListener<Answer>() {
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
    
    private class SendCommandContext<T> extends AsyncRpcConext<T> {
        private T answer;
       
        public SendCommandContext(AsyncCompletionCallback<T> callback) {
            super(callback);
        }
        
        public void setAnswer(T answer) {
            this.answer = answer;
        }
        
        public T getAnswer() {
            return this.answer;
        }
        
    }

    @Override
    public Answer sendCommand(HypervisorHostEndPoint host, Command command) {
        SendCommandContext<Answer> context = new SendCommandContext<Answer>(null);
        AsyncCallbackDispatcher<HypervsiorHostEndPointRpcServer, Answer> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().sendCommandCallback(null, null))
        .setContext(context);
        
        this.sendCommandAsync(host, command, caller);
        
        synchronized (context) {
            try {
                context.wait();
            } catch (InterruptedException e) {
                s_logger.debug(e.toString());
                throw new CloudRuntimeException("wait on context is interrupted", e);
            }
        }
        
        return context.getAnswer();
    }
    
    protected Object sendCommandCallback(AsyncCallbackDispatcher<HypervsiorHostEndPointRpcServer, Answer> callback, SendCommandContext<Answer> context) {
        context.setAnswer((Answer)callback.getResult());
        synchronized(context) {
            context.notify();
        }
        return null;
    }
}
