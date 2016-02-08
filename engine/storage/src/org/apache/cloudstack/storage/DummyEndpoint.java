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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.net.NetUtils;

public class DummyEndpoint implements EndPoint {
    private static final Logger s_logger = Logger.getLogger(DummyEndpoint.class);

    private ScheduledExecutorService executor;

    public static EndPoint getEndpoint() {
        DummyEndpoint endpoint = ComponentContext.inject(DummyEndpoint.class);
        return endpoint;
    }

    public void configure() {
        executor = Executors.newScheduledThreadPool(10);
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Answer sendMessage(Command cmd) {
        s_logger.info("Handling " + cmd + " with dummy endpoint. We will pretend that the operation succeeded.");
        return new Answer(cmd, true, "Success");
    }

    private class CmdRunner extends ManagedContextRunnable {
        final Command cmd;
        final AsyncCompletionCallback<Answer> callback;

        public CmdRunner(Command cmd, AsyncCompletionCallback<Answer> callback) {
            this.cmd = cmd;
            this.callback = callback;
        }

        @Override
        protected void runInContext() {
            Answer answer = sendMessage(cmd);
            callback.complete(answer);
        }
    }

    @Override
    public void sendMessageAsync(Command cmd, AsyncCompletionCallback<Answer> callback) {
        executor.schedule(new CmdRunner(cmd, callback), 10, TimeUnit.SECONDS);
    }

    @Override
    public String getHostAddr() {
        return "127.0.0.0";
    }

    @Override
    public String getPublicAddr() {
        String hostIp = NetUtils.getDefaultHostIp();
        if (hostIp != null)
            return hostIp;
        else
            return "127.0.0.0";
    }
}
