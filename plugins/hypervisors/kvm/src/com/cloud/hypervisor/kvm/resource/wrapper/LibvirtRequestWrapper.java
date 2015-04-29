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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Hashtable;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.RequestWrapper;
import com.cloud.resource.ServerResource;

public class LibvirtRequestWrapper extends RequestWrapper {

    private static LibvirtRequestWrapper instance;

    static {
        instance = new LibvirtRequestWrapper();
    }

    private LibvirtRequestWrapper() {
        init();
    }

    @SuppressWarnings("rawtypes")
    private void init() {
        // LibvirtComputingResource commands
        final Hashtable<Class<? extends Command>, CommandWrapper> linbvirtCommands = new Hashtable<Class<? extends Command>, CommandWrapper>();

        linbvirtCommands.put(StopCommand.class, new LibvirtStopCommandWrapper());
        linbvirtCommands.put(GetVmStatsCommand.class, new LibvirtGetVmStatsCommandWrapper());
        linbvirtCommands.put(GetVmDiskStatsCommand.class, new LibvirtGetVmDiskStatsCommandWrapper());
        linbvirtCommands.put(RebootRouterCommand.class, new LibvirtRebootRouterCommandWrapper());
        linbvirtCommands.put(RebootCommand.class, new LibvirtRebootCommandWrapper());
        linbvirtCommands.put(GetHostStatsCommand.class, new LibvirtGetHostStatsCommandWrapper());
        linbvirtCommands.put(CheckHealthCommand.class, new LibvirtCheckHealthCommandWrapper());
        linbvirtCommands.put(PrepareForMigrationCommand.class, new LibvirtPrepareForMigrationCommandWrapper());
        linbvirtCommands.put(MigrateCommand.class, new LibvirtMigrateCommandWrapper());
        linbvirtCommands.put(PingTestCommand.class, new LibvirtPingTestCommandWrapper());
        linbvirtCommands.put(CheckVirtualMachineCommand.class, new LibvirtCheckVirtualMachineCommandWrapper());
        linbvirtCommands.put(ReadyCommand.class, new LibvirtReadyCommandWrapper());
        linbvirtCommands.put(AttachIsoCommand.class, new LibvirtAttachIsoCommandWrapper());
        linbvirtCommands.put(AttachVolumeCommand.class, new LibvirtAttachVolumeCommandWrapper());
        linbvirtCommands.put(WatchConsoleProxyLoadCommand.class, new LibvirtWatchConsoleProxyLoadCommandWrapper());
        linbvirtCommands.put(CheckConsoleProxyLoadCommand.class, new LibvirtCheckConsoleProxyLoadCommandWrapper());
        linbvirtCommands.put(GetVncPortCommand.class, new LibvirtGetVncPortCommandWrapper());
        linbvirtCommands.put(ModifySshKeysCommand.class, new LibvirtModifySshKeysCommandWrapper());
        linbvirtCommands.put(MaintainCommand.class, new LibvirtMaintainCommandWrapper());
        linbvirtCommands.put(CreateCommand.class, new LibvirtCreateCommandWrapper());
        linbvirtCommands.put(DestroyCommand.class, new LibvirtDestroyCommandWrapper());

        resources.put(LibvirtComputingResource.class, linbvirtCommands);
    }

    public static LibvirtRequestWrapper getInstance() {
        return instance;
    }

    @SuppressWarnings({"rawtypes" })
    @Override
    public Answer execute(final Command command, final ServerResource serverResource) {
        final Class<? extends ServerResource> resourceClass = serverResource.getClass();

        final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands = retrieveResource(command, resourceClass);

        CommandWrapper<Command, Answer, ServerResource> commandWrapper = retrieveCommands(command.getClass(), resourceCommands);

        while (commandWrapper == null) {
            //Could not find the command in the given resource, will traverse the family tree.
            commandWrapper = retryWhenAllFails(command, resourceClass, resourceCommands);
        }

        return commandWrapper.execute(command, serverResource);
    }
}