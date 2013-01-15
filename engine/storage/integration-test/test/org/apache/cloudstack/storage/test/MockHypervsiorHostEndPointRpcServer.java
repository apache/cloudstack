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
package org.apache.cloudstack.storage.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.HostEndpointRpcServer;
import org.apache.cloudstack.storage.HypervisorHostEndPoint;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class MockHypervsiorHostEndPointRpcServer implements HostEndpointRpcServer {
    private ScheduledExecutorService executor;
    public MockHypervsiorHostEndPointRpcServer() {
        executor = Executors.newScheduledThreadPool(10);
    }
    
    protected class MockRpcCallBack implements Runnable {
        private final Command cmd;
        private final AsyncCompletionCallback<Answer> callback; 
        public MockRpcCallBack(Command cmd, final AsyncCompletionCallback<Answer> callback) {
            this.cmd = cmd;
            this.callback = callback;
        }
        @Override
        public void run() {
            try {
            Answer answer = new Answer(cmd, false, "unknown command");
            /*if (cmd instanceof CopyTemplateToPrimaryStorageCmd) {
                answer = new CopyTemplateToPrimaryStorageAnswer(cmd, UUID.randomUUID().toString());
            } else if (cmd instanceof CreateVolumeFromBaseImageCommand) {
                answer = new CreateVolumeAnswer(cmd, UUID.randomUUID().toString());
            }*/
            
           callback.complete(answer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    public void sendCommandAsync(HypervisorHostEndPoint host, final Command command, final AsyncCompletionCallback<Answer> callback) {
        executor.schedule(new MockRpcCallBack(command, callback), 10, TimeUnit.SECONDS);
    }
    
    @Override
    public Answer sendCommand(HypervisorHostEndPoint host, Command command) {
        // TODO Auto-generated method stub
        return null;
    }
}
