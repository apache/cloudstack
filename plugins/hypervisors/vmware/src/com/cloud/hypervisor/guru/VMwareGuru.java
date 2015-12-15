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

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.UnregisterNicCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.CreateVolumeOVACommand;
import com.cloud.agent.api.storage.PrepareOVAPackingCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.host.Host;
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
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.secstorage.CommandExecLogVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = HypervisorGuru.class)
public class VMwareGuru extends HypervisorGuruBase implements HypervisorGuru, Configurable {
    private static final Logger s_logger = Logger.getLogger(VMwareGuru.class);

    @Inject
    private NetworkDao _networkDao;
    @Inject
    private GuestOSDao _guestOsDao;
    @Inject
    GuestOSHypervisorDao _guestOsHypervisorDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private HostDetailsDao _hostDetailsDao;
    @Inject
    private CommandExecLogDao _cmdExecLogDao;
    @Inject
    private VmwareManager _vmwareMgr;
    @Inject
    private SecondaryStorageVmManager _secStorageMgr;
    @Inject
    private NetworkModel _networkMgr;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private NicDao _nicDao;
    @Inject
    private PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private ClusterManager _clusterMgr;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    VolumeDataFactory _volFactory;

    protected VMwareGuru() {
        super();
    }

    public static final ConfigKey<Boolean> VmwareReserveCpu = new ConfigKey<Boolean>(Boolean.class, "vmware.reserve.cpu", "Advanced", "false",
        "Specify whether or not to reserve CPU when not overprovisioning, In case of cpu overprovisioning we will always reserve cpu.", true, ConfigKey.Scope.Cluster,
        null);

    public static final ConfigKey<Boolean> VmwareReserveMemory = new ConfigKey<Boolean>(Boolean.class, "vmware.reserve.mem", "Advanced", "false",
        "Specify whether or not to reserve memory when not overprovisioning, In case of memory overprovisioning we will always reserve memory.", true,
        ConfigKey.Scope.Cluster, null);

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.VMware;
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        to.setBootloader(BootloaderType.HVM);

        Map<String, String> details = to.getDetails();
        if (details == null)
            details = new HashMap<String, String>();

        Type vmType = vm.getType();
        boolean userVm = !(vmType.equals(VirtualMachine.Type.DomainRouter) || vmType.equals(VirtualMachine.Type.ConsoleProxy)
                || vmType.equals(VirtualMachine.Type.SecondaryStorageVm));

        String nicDeviceType = details.get(VmDetailConstants.NIC_ADAPTER);
        if (!userVm) {

            if (nicDeviceType == null) {
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
            if (nicDeviceType == null) {
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
        if (userVm) {
            if (diskDeviceType == null) {
                details.put(VmDetailConstants.ROOK_DISK_CONTROLLER, _vmwareMgr.getRootDiskController());
            }
        }

        List<NicProfile> nicProfiles = vm.getNics();

        for (NicProfile nicProfile : nicProfiles) {
            if (nicProfile.getTrafficType() == TrafficType.Guest) {
                if (_networkMgr.isProviderSupportServiceInNetwork(nicProfile.getNetworkId(), Service.Firewall, Provider.CiscoVnmc)) {
                    details.put("ConfigureVServiceInNexus", Boolean.TRUE.toString());
                }
                break;
            }
        }

        long clusterId = getClusterId(vm.getId());
        details.put(VmwareReserveCpu.key(), VmwareReserveCpu.valueIn(clusterId).toString());
        details.put(VmwareReserveMemory.key(), VmwareReserveMemory.valueIn(clusterId).toString());
        to.setDetails(details);

        if (vmType.equals(VirtualMachine.Type.DomainRouter)) {

            NicProfile publicNicProfile = null;
            for (NicProfile nicProfile : nicProfiles) {
                if (nicProfile.getTrafficType() == TrafficType.Public) {
                    publicNicProfile = nicProfile;
                    break;
                }
            }

            if (publicNicProfile != null) {
                NicTO[] nics = to.getNics();

                // reserve extra NICs
                NicTO[] expandedNics = new NicTO[nics.length + _vmwareMgr.getRouterExtraPublicNics()];
                int i = 0;
                int deviceId = -1;
                for (i = 0; i < nics.length; i++) {
                    expandedNics[i] = nics[i];
                    if (nics[i].getDeviceId() > deviceId)
                        deviceId = nics[i].getDeviceId();
                }
                deviceId++;

                long networkId = publicNicProfile.getNetworkId();
                NetworkVO network = _networkDao.findById(networkId);

                for (; i < nics.length + _vmwareMgr.getRouterExtraPublicNics(); i++) {
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
                    nicTo.setDns1(publicNicProfile.getIPv4Dns1());
                    nicTo.setDns2(publicNicProfile.getIPv4Dns2());
                    if (publicNicProfile.getIPv4Gateway() != null) {
                        nicTo.setGateway(publicNicProfile.getIPv4Gateway());
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
            for (NicTO nicTo : sortNicsByDeviceId(to.getNics())) {
                sbMacSequence.append(nicTo.getMac()).append("|");
            }
            if (!sbMacSequence.toString().isEmpty()) {
                sbMacSequence.deleteCharAt(sbMacSequence.length() - 1);
                String bootArgs = to.getBootArgs();
                to.setBootArgs(bootArgs + " nic_macs=" + sbMacSequence.toString());
            }

        }

        // Don't do this if the virtual machine is one of the special types
        // Should only be done on user machines
        if (userVm) {
            String nestedVirt = _configDao.getValue(Config.VmwareEnableNestedVirtualization.key());
            if (nestedVirt != null) {
                s_logger.debug("Nested virtualization requested, adding flag to vm configuration");
                details.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, nestedVirt);
                to.setDetails(details);

            }
        }
        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findByIdIncludingRemoved(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        to.setHostName(vm.getHostName());
        HostVO host = _hostDao.findById(vm.getVirtualMachine().getHostId());
        GuestOSHypervisorVO guestOsMapping = null;
        if (host != null) {
            guestOsMapping = _guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), getHypervisorType().toString(), host.getHypervisorVersion());
        }
        if (guestOsMapping == null || host == null) {
            to.setPlatformEmulator(null);
        } else {
            to.setPlatformEmulator(guestOsMapping.getGuestOsName());
        }
        return to;
    }

    private long getClusterId(long vmId) {
        long clusterId;
        Long hostId;

        hostId = _vmDao.findById(vmId).getHostId();
        if (hostId == null) {
            // If VM is in stopped state then hostId would be undefined. Hence read last host's Id instead.
            hostId = _vmDao.findById(vmId).getLastHostId();
        }
        clusterId = _hostDao.findById(hostId).getClusterId();

        return clusterId;
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

    @Override
    @DB
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        boolean needDelegation = false;

        if (cmd instanceof StorageSubSystemCommand) {
            Boolean fullCloneEnabled = VmwareFullClone.value();
            StorageSubSystemCommand c = (StorageSubSystemCommand)cmd;
            c.setExecuteInSequence(fullCloneEnabled);
        }

        //NOTE: the hostid can be a hypervisor host, or a ssvm agent. For copycommand, if it's for volume upload, the hypervisor
        //type is empty, so we need to check the format of volume at first.
        if (cmd instanceof CopyCommand) {
            CopyCommand cpyCommand = (CopyCommand) cmd;
            DataTO srcData = cpyCommand.getSrcTO();
            DataStoreTO srcStoreTO = srcData.getDataStore();
            DataTO destData = cpyCommand.getDestTO();
            DataStoreTO destStoreTO = destData.getDataStore();

            boolean inSeq = true;
            if ((srcData.getObjectType() == DataObjectType.SNAPSHOT) || (destData.getObjectType() == DataObjectType.SNAPSHOT)) {
                inSeq = false;
            } else if ((destStoreTO.getRole() == DataStoreRole.Image) || (destStoreTO.getRole() == DataStoreRole.ImageCache)) {
                inSeq = false;
            } else if (!VmwareFullClone.value()) {
                inSeq = false;
            }
            cpyCommand.setExecuteInSequence(inSeq);

            if (srcData.getObjectType() == DataObjectType.VOLUME) {
                VolumeObjectTO volumeObjectTO = (VolumeObjectTO)srcData;
                if (Storage.ImageFormat.OVA == volumeObjectTO.getFormat()) {
                    needDelegation = true;
                }
            }

            if (!needDelegation && !(HypervisorType.VMware == srcData.getHypervisorType() || HypervisorType.VMware == destData.getHypervisorType())) {
                return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
            }

            if (destData.getObjectType() == DataObjectType.VOLUME && destStoreTO.getRole() == DataStoreRole.Primary &&
                srcData.getObjectType() == DataObjectType.TEMPLATE && srcStoreTO.getRole() == DataStoreRole.Primary) {
                needDelegation = false;
            } else {
                needDelegation = true;
            }
        } else if (cmd instanceof CreateEntityDownloadURLCommand) {
            DataTO srcData = ((CreateEntityDownloadURLCommand)cmd).getData();
            if ((HypervisorType.VMware == srcData.getHypervisorType())) {
                needDelegation = true;
            }
            if (srcData.getObjectType() == DataObjectType.VOLUME) {
                VolumeObjectTO volumeObjectTO = (VolumeObjectTO)srcData;
                if (Storage.ImageFormat.OVA == volumeObjectTO.getFormat()) {
                    needDelegation = true;
                }
            }
        }

        if (!needDelegation) {
            return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
        }
        HostVO host = _hostDao.findById(hostId);
        long dcId = host.getDataCenterId();
        Pair<HostVO, SecondaryStorageVmVO> cmdTarget = _secStorageMgr.assignSecStorageVm(dcId, cmd);
        if (cmdTarget != null) {
            // TODO, we need to make sure agent is actually connected too

            cmd.setContextParam("hypervisor", HypervisorType.VMware.toString());
            if (host.getType() == Host.Type.Routing) {
                Map<String, String> hostDetails = _hostDetailsDao.findDetails(hostId);
                cmd.setContextParam("guid", resolveNameInGuid(hostDetails.get("guid")));
                cmd.setContextParam("username", hostDetails.get("username"));
                cmd.setContextParam("password", hostDetails.get("password"));
                cmd.setContextParam("serviceconsole", _vmwareMgr.getServiceConsolePortGroupName());
                cmd.setContextParam("manageportgroup", _vmwareMgr.getManagementPortGroupName());
            }

            CommandExecLogVO execLog = new CommandExecLogVO(cmdTarget.first().getId(), cmdTarget.second().getId(), cmd.getClass().getSimpleName(), 1);
            _cmdExecLogDao.persist(execLog);
            cmd.setContextParam("execid", String.valueOf(execLog.getId()));
            cmd.setContextParam("noderuninfo", String.format("%d-%d", _clusterMgr.getManagementNodeId(), _clusterMgr.getCurrentRunId()));
            cmd.setContextParam("vCenterSessionTimeout", String.valueOf(_vmwareMgr.getVcenterSessionTimeout()));

            if (cmd instanceof BackupSnapshotCommand || cmd instanceof CreatePrivateTemplateFromVolumeCommand ||
                cmd instanceof CreatePrivateTemplateFromSnapshotCommand || cmd instanceof CopyVolumeCommand || cmd instanceof CopyCommand ||
                cmd instanceof CreateVolumeOVACommand || cmd instanceof PrepareOVAPackingCommand || cmd instanceof CreateVolumeFromSnapshotCommand) {

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

            return new Pair<Boolean, Long>(Boolean.TRUE, cmdTarget.first().getId());

        }
        return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
    }

    @Override
    public boolean trackVmHostChange() {
        return true;
    }

    private static String resolveNameInGuid(String guid) {
        String tokens[] = guid.split("@");
        assert (tokens.length == 2);

        String vCenterIp = NetUtils.resolveToIp(tokens[1]);
        if (vCenterIp == null) {
            s_logger.error("Fatal : unable to resolve vCenter address " + tokens[1] + ", please check your DNS configuration");
            return guid;
        }

        if (vCenterIp.equals(tokens[1]))
            return guid;

        return tokens[0] + "@" + vCenterIp;
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
                PhysicalNetworkTrafficTypeVO trafficTypeVO = _physicalNetworkTrafficTypeDao.findBy(networkVO.getPhysicalNetworkId(), networkVO.getTrafficType());
                UnregisterNicCommand unregisterNicCommand =
                    new UnregisterNicCommand(vm.getInstanceName(), trafficTypeVO.getVmwareNetworkLabel(), UUID.fromString(nic.getUuid()));
                commands.add(unregisterNicCommand);
            }
        }
        return commands;
    }

    @Override
    public String getConfigComponentName() {
        return VMwareGuru.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {VmwareReserveCpu, VmwareReserveMemory};
    }

    @Override
    public List<Command> finalizeExpungeVolumes(VirtualMachine vm) {
        List<Command> commands = new ArrayList<Command>();

        List<VolumeVO> volumes = _volumeDao.findByInstance(vm.getId());

        if (volumes != null) {
            for (VolumeVO volume : volumes) {
                StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());

                // storagePool should be null if we are expunging a volume that was never
                // attached to a VM that was started (the "trick" for storagePool to be null
                // is that none of the VMs this volume may have been attached to were ever started,
                // so the volume was never assigned to a storage pool)
                if (storagePool != null && storagePool.isManaged() && volume.getVolumeType() == Volume.Type.ROOT) {
                    VolumeInfo volumeInfo = _volFactory.getVolume(volume.getId());
                    PrimaryDataStore primaryDataStore = (PrimaryDataStore)volumeInfo.getDataStore();
                    Map<String, String> details = primaryDataStore.getDetails();

                    if (details == null) {
                        details = new HashMap<String, String>();

                        primaryDataStore.setDetails(details);
                    }

                    details.put(DiskTO.MANAGED, Boolean.TRUE.toString());

                    DeleteCommand cmd = new DeleteCommand(volumeInfo.getTO());

                    commands.add(cmd);

                    break;
                }
            }
        }

        return commands;
    }

    @Override
    public Map<String, String> getClusterSettings(long vmId) {
        Map<String, String> details = new HashMap<String, String>();
        long clusterId = getClusterId(vmId);
        details.put(VmwareReserveCpu.key(), VmwareReserveCpu.valueIn(clusterId).toString());
        details.put(VmwareReserveMemory.key(), VmwareReserveMemory.valueIn(clusterId).toString());
        return details;
    }
}
