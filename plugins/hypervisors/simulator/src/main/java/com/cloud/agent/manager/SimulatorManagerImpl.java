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
package com.cloud.agent.manager;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.ca.SetupCertificateCommand;
import org.apache.cloudstack.ca.SetupKeyStoreCommand;
import org.apache.cloudstack.diagnostics.DiagnosticsCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.command.UploadStatusCommand;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.ReplugNicCommand;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.SecStorageVMSetupCommand;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.AggregationControlCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.GetRouterAlertsCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetIpv6FirewallRulesCommand;
import com.cloud.agent.api.routing.SetMonitorServiceCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.storage.ListVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.api.commands.CleanupSimulatorMockCmd;
import com.cloud.api.commands.ConfigureSimulatorCmd;
import com.cloud.api.commands.ConfigureSimulatorHAProviderState;
import com.cloud.api.commands.ListSimulatorHAStateTransitions;
import com.cloud.api.commands.QuerySimulatorMockCmd;
import com.cloud.resource.SimulatorStorageProcessor;
import com.cloud.serializer.GsonHelper;
import com.cloud.simulator.MockConfigurationVO;
import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockVMVO;
import com.cloud.simulator.dao.MockConfigurationDao;
import com.cloud.simulator.dao.MockHostDao;
import com.cloud.storage.resource.StorageSubsystemCommandHandler;
import com.cloud.storage.resource.StorageSubsystemCommandHandlerBase;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.PowerState;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

@Component
public class SimulatorManagerImpl extends ManagerBase implements SimulatorManager, PluggableService {
    private static final Gson s_gson = GsonHelper.getGson();
    @Inject
    MockVmManager _mockVmMgr;
    @Inject
    MockStorageManager _mockStorageMgr;
    @Inject
    MockAgentManager _mockAgentMgr;
    @Inject
    MockNetworkManager _mockNetworkMgr;
    @Inject
    MockConfigurationDao _mockConfigDao;
    @Inject
    MockHostDao _mockHost = null;
    protected StorageSubsystemCommandHandler storageHandler;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        final SimulatorStorageProcessor processor = new SimulatorStorageProcessor(this);
        storageHandler = new StorageSubsystemCommandHandlerBase(processor);
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public MockVmManager getVmMgr() {
        return _mockVmMgr;
    }

    @Override
    public MockStorageManager getStorageMgr() {
        return _mockStorageMgr;
    }

    @Override
    public MockAgentManager getAgentMgr() {
        return _mockAgentMgr;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ConfigureSimulatorCmd.class);
        cmdList.add(QuerySimulatorMockCmd.class);
        cmdList.add(CleanupSimulatorMockCmd.class);
        cmdList.add(ConfigureSimulatorHAProviderState.class);
        cmdList.add(ListSimulatorHAStateTransitions.class);
        return cmdList;
    }

    @DB
    @Override
    public Answer simulate(final Command cmd, final String hostGuid) {
        logger.debug("Simulate command " + cmd);
        Answer answer = null;
        Exception exception = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            final MockHost host = _mockHost.findByGuid(hostGuid);
            String cmdName = cmd.toString();
            final int index = cmdName.lastIndexOf(".");
            if (index != -1) {
                cmdName = cmdName.substring(index + 1);
            }

            final SimulatorInfo info = new SimulatorInfo();
            info.setHostUuid(hostGuid);

            final MockConfigurationVO config = _mockConfigDao.findByNameBottomUP(host.getDataCenterId(), host.getPodId(), host.getClusterId(), host.getId(), cmdName);
            if (config != null && (config.getCount() == null || config.getCount().intValue() > 0)) {
                final Map<String, String> configParameters = config.getParameters();
                for (final Map.Entry<String, String> entry : configParameters.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("enabled")) {
                        info.setEnabled(Boolean.parseBoolean(entry.getValue()));
                    } else if (entry.getKey().equalsIgnoreCase("timeout")) {
                        try {
                            info.setTimeout(Integer.valueOf(entry.getValue()));
                        } catch (final NumberFormatException e) {
                            logger.debug("invalid timeout parameter: " + e.toString());
                        }
                    }

                    if (entry.getKey().equalsIgnoreCase("wait")) {
                        try {
                            final int wait = Integer.valueOf(entry.getValue());
                            Thread.sleep(wait);
                        } catch (final NumberFormatException e) {
                            logger.debug("invalid wait parameter: " + e.toString());
                        } catch (final InterruptedException e) {
                            logger.debug("thread is interrupted: " + e.toString());
                        }
                    }

                    if (entry.getKey().equalsIgnoreCase("result")) {
                        final String value = entry.getValue();
                        if (value.equalsIgnoreCase("fail")) {
                            answer = new Answer(cmd, false, "Simulated failure");
                        } else if (value.equalsIgnoreCase("fault")) {
                            exception = new Exception("Simulated fault");
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                if (answer == null) {
                    final String message = config.getJsonResponse();
                    if (message != null) {
                        // json response looks like {"<Type>":....}
                        final String objectType = message.split(":")[0].substring(2).replace("\"", "");
                        final String objectData = message.substring(message.indexOf(':') + 1, message.length() - 1);
                        if (objectType != null) {
                            Class<?> clz = null;
                            try {
                                clz = Class.forName(objectType);
                            } catch (final ClassNotFoundException e) {
                            }
                            if (clz != null) {
                                final StringReader reader = new StringReader(objectData);
                                final JsonReader jsonReader = new JsonReader(reader);
                                jsonReader.setLenient(true);
                                answer = (Answer)s_gson.fromJson(jsonReader, clz);
                            }
                        }
                    }
                }
            }

            if (answer == null) {
                if (cmd instanceof GetHostStatsCommand) {
                    answer = _mockAgentMgr.getHostStatistic((GetHostStatsCommand)cmd);
                } else if (cmd instanceof CheckHealthCommand) {
                    answer = _mockAgentMgr.checkHealth((CheckHealthCommand)cmd);
                } else if (cmd instanceof PingTestCommand) {
                    answer = _mockAgentMgr.pingTest((PingTestCommand)cmd);
                } else if (cmd instanceof SetupKeyStoreCommand) {
                    answer = _mockAgentMgr.setupKeyStore((SetupKeyStoreCommand) cmd);
                }else if (cmd instanceof DiagnosticsCommand) {
                    answer = _mockAgentMgr.runDiagnostics((DiagnosticsCommand)cmd);
                } else if (cmd instanceof SetupCertificateCommand) {
                    answer = _mockAgentMgr.setupCertificate((SetupCertificateCommand)cmd);
                } else if (cmd instanceof PrepareForMigrationCommand) {
                    answer = _mockVmMgr.prepareForMigrate((PrepareForMigrationCommand)cmd);
                } else if (cmd instanceof MigrateCommand) {
                    answer = _mockVmMgr.migrate((MigrateCommand)cmd, info);
                } else if (cmd instanceof StartCommand) {
                    answer = _mockVmMgr.startVM((StartCommand)cmd, info);
                } else if (cmd instanceof CheckSshCommand) {
                    answer = _mockVmMgr.checkSshCommand((CheckSshCommand)cmd);
                } else if (cmd instanceof CheckVirtualMachineCommand) {
                    answer = _mockVmMgr.checkVmState((CheckVirtualMachineCommand)cmd);
                } else if (cmd instanceof SetStaticNatRulesCommand) {
                    answer = _mockNetworkMgr.SetStaticNatRules((SetStaticNatRulesCommand)cmd);
                } else if (cmd instanceof SetFirewallRulesCommand) {
                    answer = _mockNetworkMgr.SetFirewallRules((SetFirewallRulesCommand)cmd);
                } else if (cmd instanceof SetIpv6FirewallRulesCommand) {
                    answer = _mockNetworkMgr.SetIpv6FirewallRules((SetIpv6FirewallRulesCommand)cmd);
                } else if (cmd instanceof SetPortForwardingRulesCommand) {
                    answer = _mockNetworkMgr.SetPortForwardingRules((SetPortForwardingRulesCommand)cmd);
                } else if (cmd instanceof NetworkUsageCommand) {
                    answer = _mockNetworkMgr.getNetworkUsage((NetworkUsageCommand)cmd);
                } else if (cmd instanceof IpAssocCommand) {
                    answer = _mockNetworkMgr.IpAssoc((IpAssocCommand)cmd);
                } else if (cmd instanceof LoadBalancerConfigCommand) {
                    answer = _mockNetworkMgr.LoadBalancerConfig((LoadBalancerConfigCommand)cmd);
                } else if (cmd instanceof DhcpEntryCommand) {
                    answer = _mockNetworkMgr.AddDhcpEntry((DhcpEntryCommand)cmd);
                } else if (cmd instanceof VmDataCommand) {
                    answer = _mockVmMgr.setVmData((VmDataCommand)cmd);
                } else if (cmd instanceof CleanupNetworkRulesCmd) {
                    answer = _mockVmMgr.cleanupNetworkRules((CleanupNetworkRulesCmd)cmd, info);
                } else if (cmd instanceof CheckNetworkCommand) {
                    answer = _mockAgentMgr.checkNetworkCommand((CheckNetworkCommand)cmd);
                } else if (cmd instanceof StopCommand) {
                    answer = _mockVmMgr.stopVM((StopCommand)cmd);
                } else if (cmd instanceof RebootCommand) {
                    answer = _mockVmMgr.rebootVM((RebootCommand)cmd);
                } else if (cmd instanceof GetVncPortCommand) {
                    answer = _mockVmMgr.getVncPort((GetVncPortCommand)cmd);
                } else if (cmd instanceof CheckConsoleProxyLoadCommand) {
                    answer = _mockVmMgr.checkConsoleProxyLoad((CheckConsoleProxyLoadCommand)cmd);
                } else if (cmd instanceof WatchConsoleProxyLoadCommand) {
                    answer = _mockVmMgr.watchConsoleProxyLoad((WatchConsoleProxyLoadCommand)cmd);
                } else if (cmd instanceof SecurityGroupRulesCmd) {
                    answer = _mockVmMgr.addSecurityGroupRules((SecurityGroupRulesCmd)cmd, info);
                } else if (cmd instanceof SavePasswordCommand) {
                    answer = _mockVmMgr.savePassword((SavePasswordCommand)cmd);
                } else if (cmd instanceof PrimaryStorageDownloadCommand) {
                    answer = _mockStorageMgr.primaryStorageDownload((PrimaryStorageDownloadCommand)cmd);
                } else if (cmd instanceof CreateCommand) {
                    answer = _mockStorageMgr.createVolume((CreateCommand)cmd);
                } else if (cmd instanceof AttachIsoCommand) {
                    answer = _mockStorageMgr.AttachIso((AttachIsoCommand)cmd);
                } else if (cmd instanceof DeleteStoragePoolCommand) {
                    answer = _mockStorageMgr.DeleteStoragePool((DeleteStoragePoolCommand)cmd);
                } else if (cmd instanceof ModifyStoragePoolCommand) {
                    answer = _mockStorageMgr.ModifyStoragePool((ModifyStoragePoolCommand)cmd);
                } else if (cmd instanceof CreateStoragePoolCommand) {
                    answer = _mockStorageMgr.CreateStoragePool((CreateStoragePoolCommand)cmd);
                } else if (cmd instanceof SecStorageSetupCommand) {
                    answer = _mockStorageMgr.SecStorageSetup((SecStorageSetupCommand)cmd);
                } else if (cmd instanceof ListTemplateCommand) {
                    answer = _mockStorageMgr.ListTemplates((ListTemplateCommand)cmd);
                } else if (cmd instanceof ListVolumeCommand) {
                    answer = _mockStorageMgr.ListVolumes((ListVolumeCommand)cmd);
                } else if (cmd instanceof DestroyCommand) {
                    answer = _mockStorageMgr.Destroy((DestroyCommand)cmd);
                } else if (cmd instanceof DownloadProgressCommand) {
                    answer = _mockStorageMgr.DownloadProcess((DownloadProgressCommand)cmd);
                } else if (cmd instanceof DownloadCommand) {
                    answer = _mockStorageMgr.Download((DownloadCommand)cmd);
                } else if (cmd instanceof GetStorageStatsCommand) {
                    answer = _mockStorageMgr.GetStorageStats((GetStorageStatsCommand)cmd);
                } else if (cmd instanceof GetVolumeStatsCommand) {
                    answer = _mockStorageMgr.getVolumeStats((GetVolumeStatsCommand)cmd);
                } else if (cmd instanceof ManageSnapshotCommand) {
                    answer = _mockStorageMgr.ManageSnapshot((ManageSnapshotCommand)cmd);
                } else if (cmd instanceof BackupSnapshotCommand) {
                    answer = _mockStorageMgr.BackupSnapshot((BackupSnapshotCommand)cmd, info);
                } else if (cmd instanceof CreateVolumeFromSnapshotCommand) {
                    answer = _mockStorageMgr.CreateVolumeFromSnapshot((CreateVolumeFromSnapshotCommand)cmd);
                } else if (cmd instanceof DeleteCommand) {
                    answer = _mockStorageMgr.Delete((DeleteCommand)cmd);
                } else if (cmd instanceof SecStorageVMSetupCommand) {
                    answer = _mockStorageMgr.SecStorageVMSetup((SecStorageVMSetupCommand)cmd);
                } else if (cmd instanceof CreatePrivateTemplateFromSnapshotCommand) {
                    answer = _mockStorageMgr.CreatePrivateTemplateFromSnapshot((CreatePrivateTemplateFromSnapshotCommand)cmd);
                } else if (cmd instanceof ComputeChecksumCommand) {
                    answer = _mockStorageMgr.ComputeChecksum((ComputeChecksumCommand)cmd);
                } else if (cmd instanceof CreatePrivateTemplateFromVolumeCommand) {
                    answer = _mockStorageMgr.CreatePrivateTemplateFromVolume((CreatePrivateTemplateFromVolumeCommand)cmd);
                } else if (cmd instanceof UploadStatusCommand) {
                    answer = _mockStorageMgr.getUploadStatus((UploadStatusCommand)cmd);
                } else if (cmd instanceof MaintainCommand) {
                    answer = _mockAgentMgr.maintain((MaintainCommand)cmd);
                } else if (cmd instanceof GetVmStatsCommand) {
                    answer = _mockVmMgr.getVmStats((GetVmStatsCommand)cmd);
                } else if (cmd instanceof CheckRouterCommand) {
                    answer = _mockVmMgr.checkRouter((CheckRouterCommand)cmd);
                } else if (cmd instanceof GetDomRVersionCmd) {
                    answer = _mockVmMgr.getDomRVersion((GetDomRVersionCmd)cmd);
                } else if (cmd instanceof CopyVolumeCommand) {
                    answer = _mockStorageMgr.CopyVolume((CopyVolumeCommand)cmd);
                } else if (cmd instanceof PlugNicCommand) {
                    answer = _mockNetworkMgr.plugNic((PlugNicCommand)cmd);
                } else if (cmd instanceof UnPlugNicCommand) {
                    answer = _mockNetworkMgr.unplugNic((UnPlugNicCommand)cmd);
                } else if (cmd instanceof ReplugNicCommand) {
                    answer = _mockNetworkMgr.replugNic((ReplugNicCommand)cmd);
                } else if (cmd instanceof IpAssocVpcCommand) {
                    answer = _mockNetworkMgr.ipAssoc((IpAssocVpcCommand)cmd);
                } else if (cmd instanceof SetSourceNatCommand) {
                    answer = _mockNetworkMgr.setSourceNat((SetSourceNatCommand)cmd);
                } else if (cmd instanceof SetNetworkACLCommand) {
                    answer = _mockNetworkMgr.setNetworkAcl((SetNetworkACLCommand)cmd);
                } else if (cmd instanceof SetupGuestNetworkCommand) {
                    answer = _mockNetworkMgr.setUpGuestNetwork((SetupGuestNetworkCommand)cmd);
                } else if (cmd instanceof SetPortForwardingRulesVpcCommand) {
                    answer = _mockNetworkMgr.setVpcPortForwards((SetPortForwardingRulesVpcCommand)cmd);
                } else if (cmd instanceof SetStaticNatRulesCommand) {
                    answer = _mockNetworkMgr.setVPCStaticNatRules((SetStaticNatRulesCommand)cmd);
                } else if (cmd instanceof SetStaticRouteCommand) {
                    answer = _mockNetworkMgr.setStaticRoute((SetStaticRouteCommand)cmd);
                } else if (cmd instanceof Site2SiteVpnCfgCommand) {
                    answer = _mockNetworkMgr.siteToSiteVpn((Site2SiteVpnCfgCommand)cmd);
                } else if (cmd instanceof CheckS2SVpnConnectionsCommand) {
                    answer = _mockNetworkMgr.checkSiteToSiteVpnConnection((CheckS2SVpnConnectionsCommand)cmd);
                } else if (cmd instanceof CreateVMSnapshotCommand) {
                    answer = _mockVmMgr.createVmSnapshot((CreateVMSnapshotCommand)cmd);
                } else if (cmd instanceof DeleteVMSnapshotCommand) {
                    answer = _mockVmMgr.deleteVmSnapshot((DeleteVMSnapshotCommand)cmd);
                } else if (cmd instanceof RevertToVMSnapshotCommand) {
                    answer = _mockVmMgr.revertVmSnapshot((RevertToVMSnapshotCommand)cmd);
                } else if (cmd instanceof NetworkRulesVmSecondaryIpCommand) {
                    answer = _mockVmMgr.plugSecondaryIp((NetworkRulesVmSecondaryIpCommand)cmd);
                } else if (cmd instanceof ScaleVmCommand) {
                    answer = _mockVmMgr.scaleVm((ScaleVmCommand)cmd);
                } else if (cmd instanceof PvlanSetupCommand) {
                    answer = _mockNetworkMgr.setupPVLAN((PvlanSetupCommand)cmd);
                } else if (cmd instanceof StorageSubSystemCommand) {
                    answer = storageHandler.handleStorageCommands((StorageSubSystemCommand)cmd);
                } else if (cmd instanceof FenceCommand) {
                    answer = _mockVmMgr.fence((FenceCommand)cmd);
                } else if (cmd instanceof HandleConfigDriveIsoCommand) {
                    answer = _mockStorageMgr.handleConfigDriveIso((HandleConfigDriveIsoCommand)cmd);
                } else if (cmd instanceof ResizeVolumeCommand) {
                    answer = _mockStorageMgr.handleResizeVolume((ResizeVolumeCommand)cmd);
                } else if (cmd instanceof GetRouterAlertsCommand
                        || cmd instanceof VpnUsersCfgCommand
                        || cmd instanceof RemoteAccessVpnCfgCommand
                        || cmd instanceof SetMonitorServiceCommand
                        || cmd instanceof AggregationControlCommand
                        || cmd instanceof SecStorageFirewallCfgCommand) {
                    answer = new Answer(cmd);
                } else {
                    logger.error("Simulator does not implement command of type " + cmd.toString());
                    answer = Answer.createUnsupportedCommandAnswer(cmd);
                }
            }

            if (config != null && config.getCount() != null && config.getCount().intValue() > 0) {
                if (answer != null) {
                    config.setCount(config.getCount().intValue() - 1);
                    _mockConfigDao.update(config.getId(), config);
                }
            }

            logger.debug("Finished simulate command " + cmd);

            return answer;
        } catch (final Exception e) {
            logger.error("Failed execute cmd: ", e);
            txn.rollback();
            return new Answer(cmd, false, e.toString());
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public StoragePoolInfo getLocalStorage(final String hostGuid) {
        return _mockStorageMgr.getLocalStorage(hostGuid);
    }

    @Override
    public Map<String, PowerState> getVmStates(final String hostGuid) {
        return _mockVmMgr.getVmStates(hostGuid);
    }

    @Override
    public Map<String, MockVMVO> getVms(final String hostGuid) {
        return _mockVmMgr.getVms(hostGuid);
    }

    @Override
    public HashMap<String, Pair<Long, Long>> syncNetworkGroups(final String hostGuid) {
        final SimulatorInfo info = new SimulatorInfo();
        info.setHostUuid(hostGuid);
        return _mockVmMgr.syncNetworkGroups(info);
    }

    @Override
    public Long configureSimulator(final Long zoneId, final Long podId, final Long clusterId, final Long hostId, final String command, final String values, final Integer count, final String jsonResponse) {
        Long id = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            MockConfigurationVO config = _mockConfigDao.findByCommand(zoneId, podId, clusterId, hostId, command);
            if (config == null) {
                config = new MockConfigurationVO();
                config.setClusterId(clusterId);
                config.setDataCenterId(zoneId);
                config.setPodId(podId);
                config.setHostId(hostId);
                config.setName(command);
                config.setValues(values);
                config.setCount(count);
                config.setJsonResponse(jsonResponse);
                config = _mockConfigDao.persist(config);
                txn.commit();
            } else {
                config.setValues(values);
                config.setCount(count);
                config.setJsonResponse(jsonResponse);
                _mockConfigDao.update(config.getId(), config);
                txn.commit();
            }
            id = config.getId();
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to configure simulator mock because of " + ex.getMessage(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return id;
    }

    @Override
    public MockConfigurationVO querySimulatorMock(final Long id) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            return _mockConfigDao.findById(id);
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to query simulator mock because of " + ex.getMessage(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public boolean clearSimulatorMock(final Long id) {
        boolean status = false;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final MockConfigurationVO config = _mockConfigDao.findById(id);
            if (config != null) {
                config.setRemoved(new Date());
                _mockConfigDao.update(config.getId(), config);
                status = true;
                txn.commit();
            }
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to cleanup simulator mock because of " + ex.getMessage(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return status;
    }

    @Override
    public MockConfigurationDao getMockConfigurationDao() {
        return _mockConfigDao;
    }
}
