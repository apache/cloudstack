//
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
//

package com.cloud.hypervisor.xenserver.resource.wrapper;

import java.util.Hashtable;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.RequestWrapper;
import com.cloud.resource.ServerResource;

public class CitrixRequestWrapper extends RequestWrapper {

    private static CitrixRequestWrapper instance;

    static {
        instance = new CitrixRequestWrapper();
    }

    @SuppressWarnings("rawtypes")
    private final Hashtable<Class<? extends Command>, CommandWrapper> map;

    @SuppressWarnings("rawtypes")
    private CitrixRequestWrapper() {
        map = new Hashtable<Class<? extends Command>, CommandWrapper>();
        init();
    }

    private void init() {
        map.put(RebootRouterCommand.class, new CitrixRebootRouterCommandWrapper());
        map.put(CreateCommand.class, new CitrixCreateCommandWrapper());
        map.put(CheckConsoleProxyLoadCommand.class, new CitrixCheckConsoleProxyLoadCommandWrapper());
        map.put(WatchConsoleProxyLoadCommand.class, new CitrixWatchConsoleProxyLoadCommandWrapper());
        map.put(ReadyCommand.class, new CitrixReadyCommandWrapper());
        map.put(GetHostStatsCommand.class, new CitrixGetHostStatsCommandWrapper());
        map.put(GetVmStatsCommand.class, new CitrixGetVmStatsCommandWrapper());
        map.put(GetVmDiskStatsCommand.class, new CitrixGetVmDiskStatsCommandWrapper());
        map.put(CheckHealthCommand.class, new CitrixCheckHealthCommandWrapper());
        map.put(StopCommand.class, new CitrixStopCommandWrapper());
        map.put(RebootCommand.class, new CitrixRebootCommandWrapper());
    }

    public static CitrixRequestWrapper getInstance() {
        return instance;
    }

    @Override
    public Answer execute(final Command command, final ServerResource serverResource) {
        @SuppressWarnings("unchecked")
        final CommandWrapper<Command, Answer, ServerResource> commandWrapper = map.get(command.getClass());

        if (commandWrapper == null) {
            throw new NullPointerException("No key found for '" + command.getClass() + "' in the Map!");
        }

        return commandWrapper.execute(command, serverResource);
    }
}