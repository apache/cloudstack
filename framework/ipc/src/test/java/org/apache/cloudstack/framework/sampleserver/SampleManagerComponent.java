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
package org.apache.cloudstack.framework.sampleserver;

import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDispatcher;
import org.apache.cloudstack.framework.messagebus.MessageHandler;
import org.apache.cloudstack.framework.rpc.RpcCallbackListener;
import org.apache.cloudstack.framework.rpc.RpcException;
import org.apache.cloudstack.framework.rpc.RpcProvider;
import org.apache.cloudstack.framework.rpc.RpcServerCall;
import org.apache.cloudstack.framework.rpc.RpcServiceDispatcher;
import org.apache.cloudstack.framework.rpc.RpcServiceHandler;

@Component
public class SampleManagerComponent {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private MessageBus _eventBus;

    @Inject
    private RpcProvider _rpcProvider;

    private Timer _timer = new Timer();

    public SampleManagerComponent() {
    }

    @PostConstruct
    public void init() {
        _rpcProvider.registerRpcServiceEndpoint(RpcServiceDispatcher.getDispatcher(this));

        // subscribe to all network events (for example)
        _eventBus.subscribe("network", MessageDispatcher.getDispatcher(this));

        _timer.schedule(new TimerTask() {
            @Override
            public void run() {
                testRpc();
            }
        }, 3000);
    }

    @RpcServiceHandler(command = "NetworkPrepare")
    void onStartCommand(RpcServerCall call) {
        call.completeCall("NetworkPrepare completed");
    }

    @MessageHandler(topic = "network.prepare")
    void onPrepareNetwork(String sender, String topic, Object args) {
    }

    void testRpc() {
        SampleStoragePrepareCommand cmd = new SampleStoragePrepareCommand();
        cmd.setStoragePool("Pool1");
        cmd.setVolumeId("vol1");

        _rpcProvider.newCall()
            .setCommand("StoragePrepare")
            .setCommandArg(cmd)
            .setTimeout(10000)
            .addCallbackListener(new RpcCallbackListener<SampleStoragePrepareAnswer>() {
                @Override
                public void onSuccess(SampleStoragePrepareAnswer result) {
                    logger.info("StoragePrepare return result: " + result.getResult());
                }

                @Override
                public void onFailure(RpcException e) {
                    logger.info("StoragePrepare failed");
                }
            })
            .apply();
    }
}
