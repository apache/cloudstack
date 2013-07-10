
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
package com.cloud.hypervisor.guru;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.UnregisterNicCommand;
import com.cloud.agent.api.UnregisterVMCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateVolumeOVACommand;
import com.cloud.agent.api.storage.PrepareOVAPackingCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.secstorage.CommandExecLogVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;

import org.apache.cloudstack.storage.command.CopyCommand;

@Local(value=HypervisorGuru.class)
public class VMwareGuru extends HypervisorGuruBase implements HypervisorGuru {
    private static final Logger s_logger = Logger.getLogger(VMwareGuru.class);

    @Inject NetworkDao _networkDao;
    @Inject GuestOSDao _guestOsDao;
    @Inject HostDao _hostDao;
    @Inject HostDetailsDao _hostDetailsDao;
    @Inject CommandExecLogDao _cmdExecLogDao;
    @Inject ClusterManager _clusterMgr;
    @Inject VmwareManager _vmwareMgr;
    @Inject SecondaryStorageVmManager _secStorageMgr;
    @Inject NetworkModel _networkMgr;
    @Inject ConfigurationDao _configDao;
    @Inject
    NicDao _nicDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;

    protected VMwareGuru() {
        super();
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.VMware;
    }

    @Override
    public <T extends VirtualMachine> VirtualMachineTO implement(VirtualMachineProfile<T> vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        to.setBootloader(BootloaderType.HVM);

        Map<String, String> details = to.getDetails();
        if(details == null)
            details = new HashMap<String, String>();

        String nicDeviceType = details.get(VmDetailConstants.NIC_ADAPTER);
        if(vm.getVirtualMachine() instanceof DomainRouterVO || vm.getVirtualMachine() instanceof ConsoleProxyVO
                || vm.getVirtualMachine() instanceof SecondaryStorageVmVO) {

            if(nicDeviceType == null) {
                details.put(VmDetailConstants.NIC_ADAPTER, _vmwareMgr.getSystemVMDefaultNicAdapterType());
            } else {
                try {
                    VirtualEthernetCardType.valueOf(nicDeviceType);
                } catch (Exception e) {
                    s_logger.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
                    details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
                }
            }
        } else {
            // for user-VM, use E1000 as default
            if(nicDeviceType == null) {
                details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
            } else {
                try {
                    VirtualEthernetCardType.valueOf(nicDeviceType);
                } catch (Exception e) {
                    s_logger.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
                    details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
                }
            }
        }

        String diskDeviceType = details.get(VmDetailConstants.ROOK_DISK_CONTROLLER);
        if (!(vm.getVirtualMachine() instanceof DomainRouterVO || vm.getVirtualMachine() instanceof ConsoleProxyVO
                || vm.getVirtualMachine() instanceof SecondaryStorageVmVO)){
            // user vm
            if (diskDeviceType == null){
                details.put(VmDetailConstants.ROOK_DISK_CONTROLLER, _vmwareMgr.getRootDiskController());
            }
        }

        List<NicProfile> nicProfiles = vm.getNics();

        for(NicProfile nicProfile : nicProfiles) {
            if(nicProfile.getTrafficType() == TrafficType.Guest) {
                if(_networkMgr.isProviderSupportServiceInNetwork(nicProfile.getNetworkId(), Service.Firewall, Provider.CiscoVnmc)) {
                    details.put("ConfigureVServiceInNexus", Boolean.TRUE.toString());
                }
                break;
            }
        }

        to.setDetails(details);

        if(vm.getVirtualMachine() instanceof DomainRouterVO) {

            NicProfile publicNicProfile = null;
            for(NicProfile nicProfile : nicProfiles) {
                if(nicProfile.getTrafficType() == TrafficType.Public) {
                    publicNicProfile = nicProfile;
                    break;
                }
            }

            if(publicNicProfile != null) {
                NicTO[] nics = to.getNics();

                // reserve extra NICs
                NicTO[] expandedNics = new NicTO[nics.length + _vmwareMgr.getRouterExtraPublicNics()];
                int i = 0;
                int deviceId = -1;
                for(i = 0; i < nics.length; i++) {
                    expandedNics[i] = nics[i];
                    if(nics[i].getDeviceId() > deviceId)
                        deviceId = nics[i].getDeviceId();
                }
                deviceId++;

                long networkId = publicNicProfile.getNetworkId();
                NetworkVO network = _networkDao.findById(networkId);

                for(; i < nics.length + _vmwareMgr.getRouterExtraPublicNics(); i++) {
                    NicTO nicTo = new NicTO();

                    nicTo.setDeviceId(deviceId++);
                    nicTo.setBroadcastType(publicNicProfile.getBroadcastType());
                    nicTo.setType(publicNicProfile.getTrafficType());
                    nicTo.setIp("0.0.0.0");
                    nicTo.setNetmask("255.255.255.255");

                    try {
                        String mac = _networkMgr.getNextAvailableMacAddressInNetwork(networkId);
                        nicTo.setMac(mac);
                    } catch (InsufficientAddressCapacityException e) {
                        throw new CloudRuntimeException("unable to allocate mac address on network: " + networkId);
                    }
                    nicTo.setDns1(publicNicProfile.getDns1());
                    nicTo.setDns2(publicNicProfile.getDns2());
                    if (publicNicProfile.getGateway() != null) {
                        nicTo.setGateway(publicNicProfile.getGateway());
                    } else {
                        nicTo.setGateway(network.getGateway());
                    }
                    nicTo.setDefaultNic(false);
                    nicTo.setBroadcastUri(publicNicProfile.getBroadCastUri());
                    nicTo.setIsolationuri(publicNicProfile.getIsolationUri());

                    Integer networkRate = _networkMgr.getNetworkRate(network.getId(), null);
                    nicTo.setNetworkRateMbps(networkRate);

                    expandedNics[i] = nicTo;
                }

                to.setNics(expandedNics);
            }

            StringBuffer sbMacSequence = new StringBuffer();
            for(NicTO nicTo : sortNicsByDeviceId(to.getNics())) {
                sbMacSequence.append(nicTo.getMac()).append("|");
            }
            sbMacSequence.deleteCharAt(sbMacSequence.length() - 1);
            String bootArgs = to.getBootArgs();
            to.setBootArgs(bootArgs + " nic_macs=" + sbMacSequence.toString());

        }

        // Don't do this if the virtual machine is one of the special types
        // Should only be done on user machines
        if(!(vm.getVirtualMachine() instanceof DomainRouterVO || vm.getVirtualMachine() instanceof ConsoleProxyVO
                || vm.getVirtualMachine() instanceof SecondaryStorageVmVO)) {
            String nestedVirt = _configDao.getValue(Config.VmwareEnableNestedVirtualization.key());
            if (nestedVirt != null) {
                s_logger.debug("Nested virtualization requested, adding flag to vm configuration");
                details.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, nestedVirt);
                to.setDetails(details);

            }
        }
        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        return to;
    }

    private NicTO[] sortNicsByDeviceId(NicTO[] nics) {

        List<NicTO> listForSort = new ArrayList<NicTO>();
        for (NicTO nic : nics) {
            listForSort.add(nic);
        }
        Collections.sort(listForSort, new Comparator<NicTO>() {

            @Override
            public int compare(NicTO arg0, NicTO arg1) {
                if (arg0.getDeviceId() < arg1.getDeviceId()) {
                    return -1;
                } else if (arg0.getDeviceId() == arg1.getDeviceId()) {
                    return 0;
                }

                return 1;
            }
        });

        return listForSort.toArray(new NicTO[0]);
    }

    @Override @DB
    public long getCommandHostDelegation(long hostId, Command cmd) {
        boolean needDelegation = false;

        if(cmd instanceof PrimaryStorageDownloadCommand ||
                cmd instanceof BackupSnapshotCommand ||
                cmd instanceof CreatePrivateTemplateFromVolumeCommand ||
                cmd instanceof CreatePrivateTemplateFromSnapshotCommand ||
                cmd instanceof CopyVolumeCommand ||
                cmd instanceof CreateVolumeOVACommand ||
                cmd instanceof PrepareOVAPackingCommand ||
                cmd instanceof CreateVolumeFromSnapshotCommand ||
                cmd instanceof CopyCommand) {
            if (cmd instanceof CopyCommand) {
                CopyCommand cpyCommand = (CopyCommand)cmd;
                DataTO srcData = cpyCommand.getSrcTO();
                DataStoreTO srcStoreTO = srcData.getDataStore();
                DataTO destData = cpyCommand.getDestTO();
                DataStoreTO destStoreTO = destData.getDataStore();

                if (destData.getObjectType() == DataObjectType.VOLUME && destStoreTO.getRole() == DataStoreRole.Primary &&
                        srcData.getObjectType() == DataObjectType.TEMPLATE && srcStoreTO.getRole() == DataStoreRole.Primary) {
                    needDelegation = false;
                } else {
                    needDelegation = true;
                }
            } else {
                needDelegation = true;
            }

        }
        /* Fang: remove this before checking in */
        // needDelegation = false;

        if (cmd instanceof PrepareOVAPackingCommand ||
                cmd instanceof CreateVolumeOVACommand	) {
            cmd.setContextParam("hypervisor", HypervisorType.VMware.toString());
        }
        if(needDelegation) {
            HostVO host = _hostDao.findById(hostId);
            assert(host != null);
            assert(host.getHypervisorType() == HypervisorType.VMware);
            long dcId = host.getDataCenterId();

            Pair<HostVO, SecondaryStorageVmVO> cmdTarget = _secStorageMgr.assignSecStorageVm(dcId, cmd);
            if(cmdTarget != null) {
                // TODO, we need to make sure agent is actually connected too
                cmd.setContextParam("hypervisor", HypervisorType.VMware.toString());
                Map<String, String> hostDetails = _hostDetailsDao.findDetails(hostId);
                cmd.setContextParam("guid", resolveNameInGuid(hostDetails.get("guid")));
                cmd.setContextParam("username", hostDetails.get("username"));
                cmd.setContextParam("password", hostDetails.get("password"));
                cmd.setContextParam("serviceconsole", _vmwareMgr.getServiceConsolePortGroupName());
                cmd.setContextParam("manageportgroup", _vmwareMgr.getManagementPortGroupName());

                CommandExecLogVO execLog = new CommandExecLogVO(cmdTarget.first().getId(), cmdTarget.second().getId(), cmd.getClass().getSimpleName(), 1);
                _cmdExecLogDao.persist(execLog);
                cmd.setContextParam("execid", String.valueOf(execLog.getId()));

                if(cmd instanceof BackupSnapshotCommand ||
                        cmd instanceof CreatePrivateTemplateFromVolumeCommand ||
                        cmd instanceof CreatePrivateTemplateFromSnapshotCommand ||
                        cmd instanceof CopyVolumeCommand ||
                        cmd instanceof CreateVolumeOVACommand ||
                        cmd instanceof PrepareOVAPackingCommand ||
                        cmd instanceof CreateVolumeFromSnapshotCommand) {

                    String workerName = _vmwareMgr.composeWorkerName();
                    long checkPointId = 1;
                    // FIXME: Fix                    long checkPointId = _checkPointMgr.pushCheckPoint(new VmwareCleanupMaid(hostDetails.get("guid"), workerName));
                    cmd.setContextParam("worker", workerName);
                    cmd.setContextParam("checkpoint", String.valueOf(checkPointId));

                    // some commands use 2 workers
                    String workerName2 = _vmwareMgr.composeWorkerName();
                    long checkPointId2 = 1;
                    // FIXME: Fix                    long checkPointId2 = _checkPointMgr.pushCheckPoint(new VmwareCleanupMaid(hostDetails.get("guid"), workerName2));
                    cmd.setContextParam("worker2", workerName2);
                    cmd.setContextParam("checkpoint2", String.valueOf(checkPointId2));
                }

                return cmdTarget.first().getId();
            }
        }

        return hostId;
    }

    @Override
    public boolean trackVmHostChange() {
        return true;
    }

    private static String resolveNameInGuid(String guid) {
        String tokens[] = guid.split("@");
        assert(tokens.length == 2);

        String vCenterIp = NetUtils.resolveToIp(tokens[1]);
        if(vCenterIp == null) {
            s_logger.error("Fatal : unable to resolve vCenter address " + tokens[1] + ", please check your DNS configuration");
            return guid;
        }

        if(vCenterIp.equals(tokens[1]))
            return guid;

        return tokens[0] + "@" + vCenterIp;
    }

    @Override
    public List<Command> finalizeExpunge(VirtualMachine vm) {
        UnregisterVMCommand unregisterVMCommand = new UnregisterVMCommand(vm.getInstanceName());
        List<Command> commands = new ArrayList<Command>();
        commands.add(unregisterVMCommand);
        return commands;
    }

    @Override
    public List<Command> finalizeExpungeNics(VirtualMachine vm, List<NicProfile> nics) {
        List<Command> commands = new ArrayList<Command>();
        List<NicVO> nicVOs = _nicDao.listByVmId(vm.getId());
        for (NicVO nic : nicVOs) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            if (network.getBroadcastDomainType() == BroadcastDomainType.Lswitch) {
                s_logger.debug("Nic " + nic.toString() + " is connected to an lswitch, cleanup required");
                NetworkVO networkVO = _networkDao.findById(nic.getNetworkId());
                // We need the traffic label to figure out which vSwitch has the
                // portgroup
                PhysicalNetworkTrafficTypeVO trafficTypeVO = _physicalNetworkTrafficTypeDao.findBy(
                        networkVO.getPhysicalNetworkId(), networkVO.getTrafficType());
                UnregisterNicCommand unregisterNicCommand = new UnregisterNicCommand(vm.getInstanceName(),
                        trafficTypeVO.getVmwareNetworkLabel(), UUID.fromString(nic.getUuid()));
                commands.add(unregisterNicCommand);
            }
        }
        return commands;
    }
}
