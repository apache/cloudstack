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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.ClusterVMMetaDataSyncCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.OvsCreateGreTunnelCommand;
import com.cloud.agent.api.OvsCreateTunnelCommand;
import com.cloud.agent.api.OvsDeleteFlowCommand;
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.agent.api.OvsDestroyTunnelCommand;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.agent.api.OvsSetTagAndFlowCommand;
import com.cloud.agent.api.OvsSetupBridgeCommand;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.agent.api.PerformanceMonitorCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UpdateHostPasswordCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.hypervisor.xenserver.resource.XenServer56FP1Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.RequestWrapper;
import com.cloud.resource.ServerResource;

public class CitrixRequestWrapper extends RequestWrapper {

    private static final Logger s_logger = Logger.getLogger(CitrixRequestWrapper.class);

    private static CitrixRequestWrapper instance;

    static {
        instance = new CitrixRequestWrapper();
    }

    @SuppressWarnings("rawtypes")
    private final Hashtable<Class<? extends ServerResource>, Hashtable<Class<? extends Command>, CommandWrapper>> resources;

    @SuppressWarnings("rawtypes")
    private CitrixRequestWrapper() {
        resources = new Hashtable<Class<? extends ServerResource>, Hashtable<Class<? extends Command>, CommandWrapper>>();
        init();
    }

    @SuppressWarnings("rawtypes")
    private void init() {
        // CitrixResourceBase commands
        final Hashtable<Class<? extends Command>, CommandWrapper> citrixCommands = new Hashtable<Class<? extends Command>, CommandWrapper>();
        citrixCommands.put(RebootRouterCommand.class, new CitrixRebootRouterCommandWrapper());
        citrixCommands.put(CreateCommand.class, new CitrixCreateCommandWrapper());
        citrixCommands.put(CheckConsoleProxyLoadCommand.class, new CitrixCheckConsoleProxyLoadCommandWrapper());
        citrixCommands.put(WatchConsoleProxyLoadCommand.class, new CitrixWatchConsoleProxyLoadCommandWrapper());
        citrixCommands.put(ReadyCommand.class, new CitrixReadyCommandWrapper());
        citrixCommands.put(GetHostStatsCommand.class, new CitrixGetHostStatsCommandWrapper());
        citrixCommands.put(GetVmStatsCommand.class, new CitrixGetVmStatsCommandWrapper());
        citrixCommands.put(GetVmDiskStatsCommand.class, new CitrixGetVmDiskStatsCommandWrapper());
        citrixCommands.put(CheckHealthCommand.class, new CitrixCheckHealthCommandWrapper());
        citrixCommands.put(StopCommand.class, new CitrixStopCommandWrapper());
        citrixCommands.put(RebootCommand.class, new CitrixRebootCommandWrapper());
        citrixCommands.put(CheckVirtualMachineCommand.class, new CitrixCheckVirtualMachineCommandWrapper());
        citrixCommands.put(PrepareForMigrationCommand.class, new CitrixPrepareForMigrationCommandWrapper());
        citrixCommands.put(MigrateCommand.class, new CitrixMigrateCommandWrapper());
        citrixCommands.put(DestroyCommand.class, new CitrixDestroyCommandWrapper());
        citrixCommands.put(CreateStoragePoolCommand.class, new CitrixCreateStoragePoolCommandWrapper());
        citrixCommands.put(ModifyStoragePoolCommand.class, new CitrixModifyStoragePoolCommandWrapper());
        citrixCommands.put(DeleteStoragePoolCommand.class, new CitrixDeleteStoragePoolCommandWrapper());
        citrixCommands.put(ResizeVolumeCommand.class, new CitrixResizeVolumeCommandWrapper());
        citrixCommands.put(AttachVolumeCommand.class, new CitrixAttachVolumeCommandWrapper());
        citrixCommands.put(AttachIsoCommand.class, new CitrixAttachIsoCommandWrapper());
        citrixCommands.put(UpgradeSnapshotCommand.class, new CitrixUpgradeSnapshotCommandWrapper());
        citrixCommands.put(GetStorageStatsCommand.class, new CitrixGetStorageStatsCommandWrapper());
        citrixCommands.put(PrimaryStorageDownloadCommand.class, new CitrixPrimaryStorageDownloadCommandWrapper());
        citrixCommands.put(GetVncPortCommand.class, new CitrixGetVncPortCommandWrapper());
        citrixCommands.put(SetupCommand.class, new CitrixSetupCommandWrapper());
        citrixCommands.put(MaintainCommand.class, new CitrixMaintainCommandWrapper());
        citrixCommands.put(PingTestCommand.class, new CitrixPingTestCommandWrapper());
        citrixCommands.put(CheckOnHostCommand.class, new CitrixCheckOnHostCommandWrapper());
        citrixCommands.put(ModifySshKeysCommand.class, new CitrixModifySshKeysCommandWrapper());
        citrixCommands.put(StartCommand.class, new CitrixStartCommandWrapper());
        citrixCommands.put(OvsSetTagAndFlowCommand.class, new CitrixOvsSetTagAndFlowCommandWrapper());
        citrixCommands.put(CheckSshCommand.class, new CitrixCheckSshCommandWrapper());
        citrixCommands.put(SecurityGroupRulesCmd.class, new CitrixSecurityGroupRulesCommandWrapper());
        citrixCommands.put(OvsFetchInterfaceCommand.class, new CitrixOvsFetchInterfaceCommandWrapper());
        citrixCommands.put(OvsCreateGreTunnelCommand.class, new CitrixOvsCreateGreTunnelCommandWrapper());
        citrixCommands.put(OvsDeleteFlowCommand.class, new CitrixOvsDeleteFlowCommandWrapper());
        citrixCommands.put(OvsVpcPhysicalTopologyConfigCommand.class, new CitrixOvsVpcPhysicalTopologyConfigCommandWrapper());
        citrixCommands.put(OvsVpcRoutingPolicyConfigCommand.class, new CitrixOvsVpcRoutingPolicyConfigCommandWrapper());
        citrixCommands.put(CleanupNetworkRulesCmd.class, new CitrixCleanupNetworkRulesCmdWrapper());
        citrixCommands.put(NetworkRulesSystemVmCommand.class, new CitrixNetworkRulesSystemVmCommandWrapper());
        citrixCommands.put(OvsCreateTunnelCommand.class, new CitrixOvsCreateTunnelCommandWrapper());
        citrixCommands.put(OvsSetupBridgeCommand.class, new CitrixOvsSetupBridgeCommandWrapper());
        citrixCommands.put(OvsDestroyBridgeCommand.class, new CitrixOvsDestroyBridgeCommandWrapper());
        citrixCommands.put(OvsDestroyTunnelCommand.class, new CitrixOvsDestroyTunnelCommandWrapper());
        citrixCommands.put(UpdateHostPasswordCommand.class, new CitrixUpdateHostPasswordCommandWrapper());
        citrixCommands.put(ClusterVMMetaDataSyncCommand.class, new CitrixClusterVMMetaDataSyncCommandWrapper());
        citrixCommands.put(CheckNetworkCommand.class, new CitrixCheckNetworkCommandWrapper());
        citrixCommands.put(PlugNicCommand.class, new CitrixPlugNicCommandWrapper());
        citrixCommands.put(UnPlugNicCommand.class, new CitrixUnPlugNicCommandWrapper());
        citrixCommands.put(CreateVMSnapshotCommand.class, new CitrixCreateVMSnapshotCommandWrapper());
        citrixCommands.put(DeleteVMSnapshotCommand.class, new CitrixDeleteVMSnapshotCommandWrapper());
        citrixCommands.put(RevertToVMSnapshotCommand.class, new CitrixRevertToVMSnapshotCommandWrapper());
        citrixCommands.put(NetworkRulesVmSecondaryIpCommand.class, new CitrixNetworkRulesVmSecondaryIpCommandWrapper());
        citrixCommands.put(ScaleVmCommand.class, new CitrixScaleVmCommandWrapper());
        citrixCommands.put(PvlanSetupCommand.class, new CitrixPvlanSetupCommandWrapper());
        citrixCommands.put(PerformanceMonitorCommand.class, new CitrixPerformanceMonitorCommandWrapper());
        citrixCommands.put(NetworkElementCommand.class, new CitrixNetworkElementCommandWrapper());
        resources.put(CitrixResourceBase.class, citrixCommands);

        // XenServer56Resource commands
        final Hashtable<Class<? extends Command>, CommandWrapper> xenServer56Commands = new Hashtable<Class<? extends Command>, CommandWrapper>();
        xenServer56Commands.put(CheckOnHostCommand.class, new XenServer56CheckOnHostCommandWrapper());
        xenServer56Commands.put(FenceCommand.class, new XenServer56FenceCommandWrapper());
        xenServer56Commands.put(NetworkUsageCommand.class, new XenServer56NetworkUsageCommandWrapper());
        resources.put(XenServer56Resource.class, xenServer56Commands);

        // XenServer56FP1Resource commands
        final Hashtable<Class<? extends Command>, CommandWrapper> xenServer56P1Commands = new Hashtable<Class<? extends Command>, CommandWrapper>();
        xenServer56P1Commands.put(FenceCommand.class, new XenServer56FP1FenceCommandWrapper());
        resources.put(XenServer56FP1Resource.class, xenServer56P1Commands);
    }

    public static CitrixRequestWrapper getInstance() {
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Hashtable<Class<? extends Command>, CommandWrapper> retrieveResource(final Command command, final Class<? extends ServerResource> resourceClass) {
        Class<? extends ServerResource> keepResourceClass = resourceClass;
        Hashtable<Class<? extends Command>, CommandWrapper> resource = resources.get(keepResourceClass);
        while (resource == null) {
            try {
                final Class<? extends ServerResource> keepResourceClass2 = (Class<? extends ServerResource>) keepResourceClass.getSuperclass();
                resource = resources.get(keepResourceClass2);

                keepResourceClass = keepResourceClass2;
            } catch (final ClassCastException e) {
                throw new NullPointerException("No key found for '" + command.getClass() + "' in the Map!");
            }
        }
        return resource;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected CommandWrapper<Command, Answer, ServerResource> retrieveCommands(final Class<? extends Command> commandClass,
            final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands) {

        Class<? extends Command> keepCommandClass = commandClass;
        CommandWrapper<Command, Answer, ServerResource> commandWrapper = resourceCommands.get(keepCommandClass);
        while (commandWrapper == null) {
            try {
                final Class<? extends Command> commandClass2 = (Class<? extends Command>) keepCommandClass.getSuperclass();

                if (commandClass2 == null) {
                    throw new NullPointerException("All the COMMAND hierarchy tree has been visited but no compliant key has been found for '" + commandClass +"'.");
                }

                commandWrapper = resourceCommands.get(commandClass2);

                keepCommandClass = commandClass2;
            } catch (final NullPointerException e) {
                // Will now traverse all the resource hierarchy. Returning null is not a problem.
                // It is all being nicely checked and in case we do not have a resource, an Unsupported answer will be thrown by the base class.
                return null;
            }
        }
        return commandWrapper;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected CommandWrapper<Command, Answer, ServerResource> retryWhenAllFails(final Command command, final Class<? extends ServerResource> resourceClass,
            final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands) {

        Class<? extends ServerResource> keepResourceClass = resourceClass;
        CommandWrapper<Command, Answer, ServerResource> commandWrapper = resourceCommands.get(command.getClass());
        while (commandWrapper == null) {
            //Could not find the command in the given resource, will traverse the family tree.
            try {
                final Class<? extends ServerResource> resourceClass2 = (Class<? extends ServerResource>) keepResourceClass.getSuperclass();

                if (resourceClass2 == null) {
                    throw new NullPointerException("All the SERVER-RESOURCE hierarchy tree has been visited but no compliant key has been found for '" + command.getClass() +"'.");
                }

                final Hashtable<Class<? extends Command>, CommandWrapper> resourceCommands2 = retrieveResource(command, (Class<? extends ServerResource>) keepResourceClass.getSuperclass());
                keepResourceClass = resourceClass2;

                commandWrapper = retrieveCommands(command.getClass(), resourceCommands2);
            } catch (final NullPointerException e) {
                throw e;
            }
        }
        return commandWrapper;
    }
}