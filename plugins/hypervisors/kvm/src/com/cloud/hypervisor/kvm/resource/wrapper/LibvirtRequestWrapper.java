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

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;

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
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
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
        final Hashtable<Class<? extends Command>, CommandWrapper> libvirtCommands = new Hashtable<Class<? extends Command>, CommandWrapper>();

        libvirtCommands.put(StopCommand.class, new LibvirtStopCommandWrapper());
        libvirtCommands.put(GetVmStatsCommand.class, new LibvirtGetVmStatsCommandWrapper());
        libvirtCommands.put(GetVmDiskStatsCommand.class, new LibvirtGetVmDiskStatsCommandWrapper());
        libvirtCommands.put(RebootRouterCommand.class, new LibvirtRebootRouterCommandWrapper());
        libvirtCommands.put(RebootCommand.class, new LibvirtRebootCommandWrapper());
        libvirtCommands.put(GetHostStatsCommand.class, new LibvirtGetHostStatsCommandWrapper());
        libvirtCommands.put(CheckHealthCommand.class, new LibvirtCheckHealthCommandWrapper());
        libvirtCommands.put(PrepareForMigrationCommand.class, new LibvirtPrepareForMigrationCommandWrapper());
        libvirtCommands.put(MigrateCommand.class, new LibvirtMigrateCommandWrapper());
        libvirtCommands.put(PingTestCommand.class, new LibvirtPingTestCommandWrapper());
        libvirtCommands.put(CheckVirtualMachineCommand.class, new LibvirtCheckVirtualMachineCommandWrapper());
        libvirtCommands.put(ReadyCommand.class, new LibvirtReadyCommandWrapper());
        libvirtCommands.put(AttachIsoCommand.class, new LibvirtAttachIsoCommandWrapper());
        libvirtCommands.put(AttachVolumeCommand.class, new LibvirtAttachVolumeCommandWrapper());
        libvirtCommands.put(WatchConsoleProxyLoadCommand.class, new LibvirtWatchConsoleProxyLoadCommandWrapper());
        libvirtCommands.put(CheckConsoleProxyLoadCommand.class, new LibvirtCheckConsoleProxyLoadCommandWrapper());
        libvirtCommands.put(GetVncPortCommand.class, new LibvirtGetVncPortCommandWrapper());
        libvirtCommands.put(ModifySshKeysCommand.class, new LibvirtModifySshKeysCommandWrapper());
        libvirtCommands.put(MaintainCommand.class, new LibvirtMaintainCommandWrapper());
        libvirtCommands.put(CreateCommand.class, new LibvirtCreateCommandWrapper());
        libvirtCommands.put(DestroyCommand.class, new LibvirtDestroyCommandWrapper());
        libvirtCommands.put(PrimaryStorageDownloadCommand.class, new LibvirtPrimaryStorageDownloadCommandWrapper());
        libvirtCommands.put(GetStorageStatsCommand.class, new LibvirtGetStorageStatsCommandWrapper());
        libvirtCommands.put(UpgradeSnapshotCommand.class, new LibvirtUpgradeSnapshotCommandWrapper());
        libvirtCommands.put(DeleteStoragePoolCommand.class, new LibvirtDeleteStoragePoolCommandWrapper());
        libvirtCommands.put(OvsSetupBridgeCommand.class, new LibvirtOvsSetupBridgeCommandWrapper());
        libvirtCommands.put(OvsDestroyBridgeCommand.class, new LibvirtOvsDestroyBridgeCommandWrapper());
        libvirtCommands.put(OvsFetchInterfaceCommand.class, new LibvirtOvsFetchInterfaceCommandWrapper());
        libvirtCommands.put(OvsVpcPhysicalTopologyConfigCommand.class, new LibvirtOvsVpcPhysicalTopologyConfigCommandWrapper());
        libvirtCommands.put(OvsVpcRoutingPolicyConfigCommand.class, new LibvirtOvsVpcRoutingPolicyConfigCommandWrapper());
        libvirtCommands.put(CreateStoragePoolCommand.class, new LibvirtCreateStoragePoolCommandWrapper());
        libvirtCommands.put(ModifyStoragePoolCommand.class, new LibvirtModifyStoragePoolCommandWrapper());
        libvirtCommands.put(CleanupNetworkRulesCmd.class, new LibvirtCleanupNetworkRulesCommandWrapper());
        libvirtCommands.put(NetworkRulesVmSecondaryIpCommand.class, new LibvirtNetworkRulesVmSecondaryIpCommandWrapper());
        libvirtCommands.put(NetworkRulesSystemVmCommand.class, new LibvirtNetworkRulesSystemVmCommandWrapper());
        libvirtCommands.put(CheckSshCommand.class, new LibvirtCheckSshCommandWrapper());
        libvirtCommands.put(CheckNetworkCommand.class, new LibvirtCheckNetworkCommandWrapper());
        libvirtCommands.put(OvsDestroyTunnelCommand.class, new LibvirtOvsDestroyTunnelCommandWrapper());
        libvirtCommands.put(CheckOnHostCommand.class, new LibvirtCheckOnHostCommandWrapper());
        libvirtCommands.put(OvsCreateTunnelCommand.class, new LibvirtOvsCreateTunnelCommandWrapper());
        libvirtCommands.put(CreateVolumeFromSnapshotCommand.class, new LibvirtCreateVolumeFromSnapshotCommandWrapper());
        libvirtCommands.put(FenceCommand.class, new LibvirtFenceCommandWrapper());
        libvirtCommands.put(SecurityGroupRulesCmd.class, new LibvirtSecurityGroupRulesCommandWrapper());
        libvirtCommands.put(PlugNicCommand.class, new LibvirtPlugNicCommandWrapper());
        libvirtCommands.put(UnPlugNicCommand.class, new LibvirtUnPlugNicCommandWrapper());
        libvirtCommands.put(NetworkUsageCommand.class, new LibvirtNetworkUsageCommandWrapper());
        libvirtCommands.put(CreatePrivateTemplateFromVolumeCommand.class, new LibvirtCreatePrivateTemplateFromVolumeCommandWrapper());
        libvirtCommands.put(ManageSnapshotCommand.class, new LibvirtManageSnapshotCommandWrapper());
        libvirtCommands.put(BackupSnapshotCommand.class, new LibvirtBackupSnapshotCommandWrapper());
        libvirtCommands.put(CreatePrivateTemplateFromSnapshotCommand.class, new LibvirtCreatePrivateTemplateFromSnapshotCommandWrapper());
        libvirtCommands.put(CopyVolumeCommand.class, new LibvirtCopyVolumeCommandWrapper());
        libvirtCommands.put(PvlanSetupCommand.class, new LibvirtPvlanSetupCommandWrapper());
        libvirtCommands.put(ResizeVolumeCommand.class, new LibvirtResizeVolumeCommandWrapper());
        libvirtCommands.put(NetworkElementCommand.class, new LibvirtNetworkElementCommandWrapper());
        libvirtCommands.put(StorageSubSystemCommand.class, new LibvirtStorageSubSystemCommandWrapper());
        libvirtCommands.put(StartCommand.class, new LibvirtStartCommandWrapper());

        resources.put(LibvirtComputingResource.class, libvirtCommands);
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