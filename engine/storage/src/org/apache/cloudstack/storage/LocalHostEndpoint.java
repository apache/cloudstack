// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ConfigurationDaoImpl;
import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.resource.LocalNfsSecondaryStorageResource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.resource.ServerResource;
import com.cloud.utils.net.NetUtils;

import javax.inject.Inject;

public class LocalHostEndpoint implements EndPoint {
    private ScheduledExecutorService executor;
    protected ServerResource resource;
    @Inject
    ConfigurationDao configDao;

    public LocalHostEndpoint() {
        // get mount parent folder configured in global setting, if set, this will overwrite _parent in NfsSecondaryStorageResource to work
        // around permission issue for default /mnt folder
        String mountParent = configDao.getValue(Config.MountParent.key());

        String path =  mountParent + File.separator + "secStorage";

        LocalNfsSecondaryStorageResource localResource = new LocalNfsSecondaryStorageResource();
        localResource.setParentPath(path);
        resource = localResource;
        executor = Executors.newScheduledThreadPool(10);
    }

    public static EndPoint getEndpoint() {
        LocalHostEndpoint endpoint = ComponentContext.inject(LocalHostEndpoint.class);
        return endpoint;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
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

    @Override
    public Answer sendMessage(Command cmd) {
        if ((cmd instanceof CopyCommand) || (cmd instanceof DownloadCommand)) {
            return resource.executeRequest(cmd);
        }
        // TODO Auto-generated method stub
        return new Answer(cmd, false, "unsupported command:" + cmd.toString());
    }

    private class CmdRunner implements Runnable {
        final Command cmd;
        final AsyncCompletionCallback<Answer> callback;

        public CmdRunner(Command cmd, AsyncCompletionCallback<Answer> callback) {
            this.cmd = cmd;
            this.callback = callback;
        }

        @Override
        public void run() {
            Answer answer = sendMessage(cmd);
            callback.complete(answer);
        }
    }

    @Override
    public void sendMessageAsync(Command cmd, AsyncCompletionCallback<Answer> callback) {
        executor.schedule(new CmdRunner(cmd, callback), 10, TimeUnit.SECONDS);
    }

    public ServerResource getResource() {
        return resource;
    }

    public void setResource(ServerResource resource) {
        this.resource = resource;
    }

}
