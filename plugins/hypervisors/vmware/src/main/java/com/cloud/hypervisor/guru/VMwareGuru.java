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
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.agent.api.MigrateVmToPoolCommand;
import com.cloud.agent.api.UnregisterVMCommand;
import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.TemplateOVFPropertyVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.dao.TemplateOVFPropertiesDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
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
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
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
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class VMwareGuru extends HypervisorGuruBase implements HypervisorGuru, Configurable {
    private static final Logger s_logger = Logger.getLogger(VMwareGuru.class);

    @Inject
    private NetworkDao _networkDao;
    @Inject
    private GuestOSDao _guestOsDao;
    @Inject
    private GuestOSHypervisorDao _guestOsHypervisorDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private HostDetailsDao _hostDetailsDao;
    @Inject
    private ClusterDetailsDao _clusterDetailsDao;
    @Inject
    private CommandExecLogDao _cmdExecLogDao;
    @Inject
    private VmwareManager _vmwareMgr;
    @Inject
    private SecondaryStorageVmManager _secStorageMgr;
    @Inject
    private NetworkModel _networkMgr;
    @Inject
    private NicDao _nicDao;
    @Inject
    private DomainRouterDao _domainRouterDao;
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
    @Inject
    private VMTemplatePoolDao templateSpoolDao;
    @Inject
    private TemplateOVFPropertiesDao templateOVFPropertiesDao;

    protected VMwareGuru() {
        super();
    }

    public static final ConfigKey<Boolean> VmwareReserveCpu = new ConfigKey<Boolean>(Boolean.class, "vmware.reserve.cpu", "Advanced", "false",
        "Specify whether or not to reserve CPU when deploying an instance.", true, ConfigKey.Scope.Cluster,
        null);

    public static final ConfigKey<Boolean> VmwareReserveMemory = new ConfigKey<Boolean>(Boolean.class, "vmware.reserve.mem", "Advanced", "false",
        "Specify whether or not to reserve memory when deploying an instance.", true,
        ConfigKey.Scope.Cluster, null);

    protected ConfigKey<Boolean> VmwareEnableNestedVirtualization = new ConfigKey<Boolean>(Boolean.class, "vmware.nested.virtualization", "Advanced", "false",
            "When set to true this will enable nested virtualization when this is supported by the hypervisor", true, ConfigKey.Scope.Global, null);

    protected ConfigKey<Boolean> VmwareEnableNestedVirtualizationPerVM = new ConfigKey<Boolean>(Boolean.class, "vmware.nested.virtualization.perVM", "Advanced", "false",
            "When set to true this will enable nested virtualization per vm", true, ConfigKey.Scope.Global, null);

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

        details.put(VmDetailConstants.BOOT_MODE, to.getBootType());
        String diskDeviceType = details.get(VmDetailConstants.ROOT_DISK_CONTROLLER);
        if (userVm) {
            if (diskDeviceType == null) {
                details.put(VmDetailConstants.ROOT_DISK_CONTROLLER, _vmwareMgr.getRootDiskController());
            }
        }
        String diskController = details.get(VmDetailConstants.DATA_DISK_CONTROLLER);
        if (userVm) {
            if (diskController == null) {
                details.put(VmDetailConstants.DATA_DISK_CONTROLLER, DiskControllerType.lsilogic.toString());
            }
        }

        if (vm.getType() == VirtualMachine.Type.NetScalerVm) {
            details.put(VmDetailConstants.ROOT_DISK_CONTROLLER, "scsi");
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

                VirtualMachine router = vm.getVirtualMachine();
                DomainRouterVO routerVO = _domainRouterDao.findById(router.getId());
                if (routerVO != null && routerVO.getIsRedundantRouter()) {
                    Long peerRouterId = _nicDao.getPeerRouterId(publicNicProfile.getMacAddress(), router.getId());
                    DomainRouterVO peerRouterVO = null;
                    if (peerRouterId != null) {
                        peerRouterVO = _domainRouterDao.findById(peerRouterId);
                        if (peerRouterVO != null) {
                            details.put("PeerRouterInstanceName", peerRouterVO.getInstanceName());
                        }
                    }
                }
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
            configureNestedVirtualization(details, to);
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

        List<OVFPropertyTO> ovfProperties = new ArrayList<>();
        for (String detailKey : details.keySet()) {
            if (detailKey.startsWith(ApiConstants.OVF_PROPERTIES)) {
                String ovfPropKey = detailKey.replace(ApiConstants.OVF_PROPERTIES + "-", "");
                TemplateOVFPropertyVO templateOVFPropertyVO = templateOVFPropertiesDao.findByTemplateAndKey(vm.getTemplateId(), ovfPropKey);
                if (templateOVFPropertyVO == null) {
                    s_logger.warn(String.format("OVF property %s not found on template, discarding", ovfPropKey));
                    continue;
                }
                String ovfValue = details.get(detailKey);
                boolean isPassword = templateOVFPropertyVO.isPassword();
                OVFPropertyTO propertyTO = new OVFPropertyTO(ovfPropKey, ovfValue, isPassword);
                ovfProperties.add(propertyTO);
            }
        }

        if (CollectionUtils.isNotEmpty(ovfProperties)) {
            removeOvfPropertiesFromDetails(ovfProperties, details);
            String templateInstallPath = null;
            List<DiskTO> rootDiskList = vm.getDisks().stream().filter(x -> x.getType() == Volume.Type.ROOT).collect(Collectors.toList());
            if (rootDiskList.size() != 1) {
                throw new CloudRuntimeException("Did not find only one root disk for VM " + vm.getHostName());
            }

            DiskTO rootDiskTO = rootDiskList.get(0);
            DataStoreTO dataStore = rootDiskTO.getData().getDataStore();
            StoragePoolVO storagePoolVO = _storagePoolDao.findByUuid(dataStore.getUuid());
            long dataCenterId = storagePoolVO.getDataCenterId();
            List<StoragePoolVO> pools = _storagePoolDao.listByDataCenterId(dataCenterId);
            for (StoragePoolVO pool : pools) {
                VMTemplateStoragePoolVO ref = templateSpoolDao.findByPoolTemplate(pool.getId(), vm.getTemplateId());
                if (ref != null && ref.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                    templateInstallPath = ref.getInstallPath();
                    break;
                }
            }

            if (templateInstallPath == null) {
                throw new CloudRuntimeException("Did not find the template install path for template " +
                        vm.getTemplateId() + " on zone " + dataCenterId);
            }

            Pair<String, List<OVFPropertyTO>> pair = new Pair<>(templateInstallPath, ovfProperties);
            to.setOvfProperties(pair);
        }

        return to;
    }

    /*
    Remove OVF properties from details to be sent to hypervisor (avoid duplicate data)
     */
    private void removeOvfPropertiesFromDetails(List<OVFPropertyTO> ovfProperties, Map<String, String> details) {
        for (OVFPropertyTO propertyTO : ovfProperties) {
            String key = propertyTO.getKey();
            details.remove(ApiConstants.OVF_PROPERTIES + "-" + key);
        }
    }

    /**
     * Decide in which cases nested virtualization should be enabled based on (1){@code globalNestedV}, (2){@code globalNestedVPerVM}, (3){@code localNestedV}<br/>
     * Nested virtualization should be enabled when one of this cases:
     * <ul>
     * <li>(1)=TRUE, (2)=TRUE, (3) is NULL (missing)</li>
     * <li>(1)=TRUE, (2)=TRUE, (3)=TRUE</li>
     * <li>(1)=TRUE, (2)=FALSE</li>
     * <li>(1)=FALSE, (2)=TRUE, (3)=TRUE</li>
     * </ul>
     * In any other case, it shouldn't be enabled
     * @param globalNestedV value of {@code 'vmware.nested.virtualization'} global config
     * @param globalNestedVPerVM value of {@code 'vmware.nested.virtualization.perVM'} global config
     * @param localNestedV value of {@code 'nestedVirtualizationFlag'} key in vm details if present, null if not present
     * @return "true" for cases in which nested virtualization is enabled, "false" if not
     */
    protected Boolean shouldEnableNestedVirtualization(Boolean globalNestedV, Boolean globalNestedVPerVM, String localNestedV){
        if (globalNestedV == null || globalNestedVPerVM == null) {
            return false;
        }
        boolean globalNV = globalNestedV.booleanValue();
        boolean globalNVPVM = globalNestedVPerVM.booleanValue();

        if (globalNVPVM){
            return (localNestedV == null && globalNV) || BooleanUtils.toBoolean(localNestedV);
        }
        return globalNV;
    }

    /**
     * Adds {@code 'nestedVirtualizationFlag'} value to {@code details} due to if it should be enabled or not
     * @param details vm details
     * @param to vm to
     */
    protected void configureNestedVirtualization(Map<String, String> details, VirtualMachineTO to) {
        Boolean globalNestedV = VmwareEnableNestedVirtualization.value();
        Boolean globalNestedVPerVM = VmwareEnableNestedVirtualizationPerVM.value();
        String localNestedV = details.get(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG);

        Boolean shouldEnableNestedVirtualization = shouldEnableNestedVirtualization(globalNestedV, globalNestedVPerVM, localNestedV);
        s_logger.debug("Nested virtualization requested, adding flag to vm configuration");
        details.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, Boolean.toString(shouldEnableNestedVirtualization));
        to.setDetails(details);
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
        if (cmd instanceof DownloadCommand) {
          cmd.setContextParam(VmwareManager.s_vmwareOVAPackageTimeout.key(), String.valueOf(VmwareManager.s_vmwareOVAPackageTimeout.value()));
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
            cmd.setContextParam(VmwareManager.s_vmwareOVAPackageTimeout.key(), String.valueOf(VmwareManager.s_vmwareOVAPackageTimeout.value()));

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
                cmd.setContextParam("searchexludefolders", _vmwareMgr.s_vmwareSearchExcludeFolder.value());
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
        return new ConfigKey<?>[] {VmwareReserveCpu, VmwareReserveMemory, VmwareEnableNestedVirtualization, VmwareEnableNestedVirtualizationPerVM};
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

    @Override
    public List<Command> finalizeMigrate(VirtualMachine vm, StoragePool destination) {
        List<Command> commands = new ArrayList<Command>();

        // OfflineVmwareMigration: specialised migration command
        List<VolumeVO> volumes = _volumeDao.findByInstance(vm.getId());
        List<VolumeTO> vols = new ArrayList<>();
        for (Volume volume : volumes) {
            VolumeTO vol = new VolumeTO(volume,destination);
            vols.add(vol);
        }
        MigrateVmToPoolCommand migrateVmToPoolCommand = new MigrateVmToPoolCommand(vm.getInstanceName(), vols, destination.getUuid(), true);
        commands.add(migrateVmToPoolCommand);

        // OfflineVmwareMigration: cleanup if needed
        final Long destClusterId = destination.getClusterId();
        final Long srcClusterId = getClusterId(vm.getId());

        if (srcClusterId != null && destClusterId != null && ! srcClusterId.equals(destClusterId)) {
            final String srcDcName = _clusterDetailsDao.getVmwareDcName(srcClusterId);
            final String destDcName = _clusterDetailsDao.getVmwareDcName(destClusterId);
            if (srcDcName != null && destDcName != null && !srcDcName.equals(destDcName)) {
                final UnregisterVMCommand unregisterVMCommand = new UnregisterVMCommand(vm.getInstanceName(), true);
                unregisterVMCommand.setCleanupVmFiles(true);

                commands.add(unregisterVMCommand);
            }
        }
        return commands;
    }
}
