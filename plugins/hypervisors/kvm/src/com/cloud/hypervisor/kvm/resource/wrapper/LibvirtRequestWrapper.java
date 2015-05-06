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
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.agent.api.OvsDestroyTunnelCommand;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.agent.api.OvsSetupBridgeCommand;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
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
        linbvirtCommands.put(PrimaryStorageDownloadCommand.class, new LibvirtPrimaryStorageDownloadCommandWrapper());
        linbvirtCommands.put(GetStorageStatsCommand.class, new LibvirtGetStorageStatsCommandWrapper());
        linbvirtCommands.put(UpgradeSnapshotCommand.class, new LibvirtUpgradeSnapshotCommandWrapper());
        linbvirtCommands.put(DeleteStoragePoolCommand.class, new LibvirtDeleteStoragePoolCommandWrapper());
        linbvirtCommands.put(OvsSetupBridgeCommand.class, new LibvirtOvsSetupBridgeCommandWrapper());
        linbvirtCommands.put(OvsDestroyBridgeCommand.class, new LibvirtOvsDestroyBridgeCommandWrapper());
        linbvirtCommands.put(OvsFetchInterfaceCommand.class, new LibvirtOvsFetchInterfaceCommandWrapper());
        linbvirtCommands.put(OvsVpcPhysicalTopologyConfigCommand.class, new LibvirtOvsVpcPhysicalTopologyConfigCommandWrapper());
        linbvirtCommands.put(OvsVpcRoutingPolicyConfigCommand.class, new LibvirtOvsVpcRoutingPolicyConfigCommandWrapper());
        linbvirtCommands.put(CreateStoragePoolCommand.class, new LibvirtCreateStoragePoolCommandWrapper());
        linbvirtCommands.put(ModifyStoragePoolCommand.class, new LibvirtModifyStoragePoolCommandWrapper());
        linbvirtCommands.put(CleanupNetworkRulesCmd.class, new LibvirtCleanupNetworkRulesCommandWrapper());
        linbvirtCommands.put(NetworkRulesVmSecondaryIpCommand.class, new LibvirtNetworkRulesVmSecondaryIpCommandWrapper());
        linbvirtCommands.put(NetworkRulesSystemVmCommand.class, new LibvirtNetworkRulesSystemVmCommandWrapper());
        linbvirtCommands.put(CheckSshCommand.class, new LibvirtCheckSshCommandWrapper());
        linbvirtCommands.put(CheckNetworkCommand.class, new LibvirtCheckNetworkCommandWrapper());
        linbvirtCommands.put(OvsDestroyTunnelCommand.class, new LibvirtOvsDestroyTunnelCommandWrapper());
        linbvirtCommands.put(CheckOnHostCommand.class, new LibvirtCheckOnHostCommandWrapper());
        linbvirtCommands.put(OvsCreateTunnelCommand.class, new LibvirtOvsCreateTunnelCommandWrapper());
        linbvirtCommands.put(CreateVolumeFromSnapshotCommand.class, new LibvirtCreateVolumeFromSnapshotCommandWrapper());
        linbvirtCommands.put(FenceCommand.class, new LibvirtFenceCommandWrapper());
        linbvirtCommands.put(SecurityGroupRulesCmd.class, new LibvirtSecurityGroupRulesCommandWrapper());
        linbvirtCommands.put(PlugNicCommand.class, new LibvirtPlugNicCommandWrapper());
        linbvirtCommands.put(UnPlugNicCommand.class, new LibvirtUnPlugNicCommandWrapper());
        linbvirtCommands.put(NetworkUsageCommand.class, new LibvirtNetworkUsageCommandWrapper());
        linbvirtCommands.put(CreatePrivateTemplateFromVolumeCommand.class, new LibvirtCreatePrivateTemplateFromVolumeCommandWrapper());
        linbvirtCommands.put(ManageSnapshotCommand.class, new LibvirtManageSnapshotCommandWrapper());
        linbvirtCommands.put(BackupSnapshotCommand.class, new LibvirtBackupSnapshotCommandWrapper());
        linbvirtCommands.put(CreatePrivateTemplateFromSnapshotCommand.class, new LibvirtCreatePrivateTemplateFromSnapshotCommandWrapper());
        linbvirtCommands.put(CopyVolumeCommand.class, new LibvirtCopyVolumeCommandWrapper());

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