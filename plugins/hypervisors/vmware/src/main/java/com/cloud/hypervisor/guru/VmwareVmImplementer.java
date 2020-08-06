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

import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.agent.api.to.DeployAsIsInfoTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.ImageStore;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class VmwareVmImplementer {
    private static final Logger LOGGER = Logger.getLogger(VmwareVmImplementer.class);

    @Inject
    DomainRouterDao domainRouterDao;
    @Inject
    GuestOSDao guestOsDao;
    @Inject
    GuestOSHypervisorDao guestOsHypervisorDao;
    @Inject
    HostDao hostDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    NetworkModel networkMgr;
    @Inject
    NicDao nicDao;
    @Inject
    PrimaryDataStoreDao storagePoolDao;
    @Inject
    VMTemplatePoolDao templateStoragePoolDao;
    @Inject
    VMTemplateDetailsDao templateDetailsDao;
    @Inject
    VmwareManager vmwareMgr;

    private Boolean globalNestedVirtualisationEnabled;
    private Boolean globalNestedVPerVMEnabled;

    Boolean getGlobalNestedVirtualisationEnabled() {
        return globalNestedVirtualisationEnabled != null ? globalNestedVirtualisationEnabled : false;
    }

    void setGlobalNestedVirtualisationEnabled(Boolean globalNestedVirtualisationEnabled) {
        this.globalNestedVirtualisationEnabled = globalNestedVirtualisationEnabled;
    }

    Boolean getGlobalNestedVPerVMEnabled() {
        return globalNestedVPerVMEnabled != null ? globalNestedVPerVMEnabled : false;
    }

    void setGlobalNestedVPerVMEnabled(Boolean globalNestedVPerVMEnabled) {
        this.globalNestedVPerVMEnabled = globalNestedVPerVMEnabled;
    }

    VirtualMachineTO implement(VirtualMachineProfile vm, VirtualMachineTO to, long clusterId) {
        to.setBootloader(VirtualMachineTemplate.BootloaderType.HVM);
        boolean deployAsIs = vm.getTemplate().isDeployAsIs();
        HostVO host = hostDao.findById(vm.getVirtualMachine().getHostId());
        Map<String, String> details = to.getDetails();
        if (details == null)
            details = new HashMap<>();

        VirtualMachine.Type vmType = vm.getType();
        boolean userVm = !(vmType.equals(VirtualMachine.Type.DomainRouter) || vmType.equals(VirtualMachine.Type.ConsoleProxy) || vmType.equals(VirtualMachine.Type.SecondaryStorageVm));

        String nicDeviceType = details.get(VmDetailConstants.NIC_ADAPTER);
        if (!userVm) {

            if (nicDeviceType == null) {
                details.put(VmDetailConstants.NIC_ADAPTER, vmwareMgr.getSystemVMDefaultNicAdapterType());
            } else {
                try {
                    VirtualEthernetCardType.valueOf(nicDeviceType);
                } catch (Exception e) {
                    LOGGER.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
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
                    LOGGER.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
                    details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
                }
            }
        }

        setBootParameters(vm, to, details);

        setDiskControllers(vm, details, userVm);

        List<NicProfile> nicProfiles = getNicProfiles(vm, details);

        addReservationDetails(clusterId, details);

        if (vmType.equals(VirtualMachine.Type.DomainRouter)) {
            configureDomainRouterNicsAndDetails(vm, to, details, nicProfiles);
        }

        // Don't do this if the virtual machine is one of the special types
        // Should only be done on user machines
        if (userVm) {
            configureNestedVirtualization(details, to);
        }
        // Determine the VM's OS description
        GuestOSVO guestOS = guestOsDao.findByIdIncludingRemoved(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        to.setHostName(vm.getHostName());

        GuestOSHypervisorVO guestOsMapping = null;
        if (host != null) {
            guestOsMapping = guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), Hypervisor.HypervisorType.VMware.toString(), host.getHypervisorVersion());
        }
        if (guestOsMapping == null || host == null) {
            to.setPlatformEmulator(null);
        } else {
            to.setPlatformEmulator(guestOsMapping.getGuestOsName());
        }

        if (deployAsIs) {
            List<OVFPropertyTO> ovfProperties = getOvfPropertyList(vm, details);
            handleOvfProperties(vm, to, details, ovfProperties);
            setDeployAsIsParams(vm, to, details);
        }

        setDetails(to, details);

        return to;
    }

    private void setDeployAsIsParams(VirtualMachineProfile vm, VirtualMachineTO to, Map<String, String> details) {
        DeployAsIsInfoTO info = new DeployAsIsInfoTO();

        String configuration = null;
        if (details.containsKey(VmDetailConstants.DEPLOY_AS_IS_CONFIGURATION)) {
            configuration = details.get(VmDetailConstants.DEPLOY_AS_IS_CONFIGURATION);
            info.setDeploymentConfiguration(configuration);
        }

        // Deploy as-is disks are all allocated to the same storage pool
        String deployAsIsStoreUuid = vm.getDisks().get(0).getData().getDataStore().getUuid();
        StoragePoolVO storagePoolVO = storagePoolDao.findByUuid(deployAsIsStoreUuid);
        VMTemplateStoragePoolVO tmplRef = templateStoragePoolDao.findByPoolTemplate(storagePoolVO.getId(), vm.getTemplate().getId(), configuration);
        if (tmplRef != null) {
            info.setTemplatePath(tmplRef.getInstallPath());
        }

        info.setDeployAsIs(true);
        to.setDeployAsIsInfo(info);
    }

    private void setDetails(VirtualMachineTO to, Map<String, String> details) {
        Map<String, String> detailsToSend = new HashMap<>();
        for (String key: details.keySet()) {
            if (key.startsWith(ImageStore.OVF_EULA_SECTION_PREFIX) ||
                    key.startsWith(ImageStore.OVF_HARDWARE_CONFIGURATION_PREFIX) ||
                    key.startsWith(ImageStore.OVF_HARDWARE_ITEM_PREFIX)) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Discarding detail for VM %s: %s => %s", to.getName(), key, details.get(key)));
                }
                continue;
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Detail for VM %s: %s => %s", to.getName(), key, details.get(key)));
            }
            detailsToSend.put(key, details.get(key));
        }
        to.setDetails(detailsToSend);
    }

    private void configureDomainRouterNicsAndDetails(VirtualMachineProfile vm, VirtualMachineTO to, Map<String, String> details, List<NicProfile> nicProfiles) {
        NicProfile publicNicProfile = null;
        for (NicProfile nicProfile : nicProfiles) {
            if (nicProfile.getTrafficType() == Networks.TrafficType.Public) {
                publicNicProfile = nicProfile;
                break;
            }
        }

        if (publicNicProfile != null) {
            NicTO[] nics = to.getNics();

            // reserve extra NICs
            NicTO[] expandedNics = new NicTO[nics.length + vmwareMgr.getRouterExtraPublicNics()];
            int i = 0;
            int deviceId = -1;
            for (i = 0; i < nics.length; i++) {
                expandedNics[i] = nics[i];
                if (nics[i].getDeviceId() > deviceId)
                    deviceId = nics[i].getDeviceId();
            }
            deviceId++;

            long networkId = publicNicProfile.getNetworkId();
            NetworkVO network = networkDao.findById(networkId);

            for (; i < nics.length + vmwareMgr.getRouterExtraPublicNics(); i++) {
                NicTO nicTo = new NicTO();

                nicTo.setDeviceId(deviceId++);
                nicTo.setBroadcastType(publicNicProfile.getBroadcastType());
                nicTo.setType(publicNicProfile.getTrafficType());
                nicTo.setIp("0.0.0.0");
                nicTo.setNetmask("255.255.255.255");

                try {
                    String mac = networkMgr.getNextAvailableMacAddressInNetwork(networkId);
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

                Integer networkRate = networkMgr.getNetworkRate(network.getId(), null);
                nicTo.setNetworkRateMbps(networkRate);

                expandedNics[i] = nicTo;
            }

            to.setNics(expandedNics);

            VirtualMachine router = vm.getVirtualMachine();
            DomainRouterVO routerVO = domainRouterDao.findById(router.getId());
            if (routerVO != null && routerVO.getIsRedundantRouter()) {
                Long peerRouterId = nicDao.getPeerRouterId(publicNicProfile.getMacAddress(), router.getId());
                DomainRouterVO peerRouterVO = null;
                if (peerRouterId != null) {
                    peerRouterVO = domainRouterDao.findById(peerRouterId);
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

    private void handleOvfProperties(VirtualMachineProfile vm, VirtualMachineTO to, Map<String, String> details, List<OVFPropertyTO> ovfProperties) {
        if (CollectionUtils.isNotEmpty(ovfProperties)) {
            removeOvfPropertiesFromDetails(ovfProperties, details);
            to.setOvfProperties(ovfProperties);
        }
    }

    private DiskTO getRootDiskTOFromVM(VirtualMachineProfile vm) {
        DiskTO rootDiskTO;
        List<DiskTO> rootDiskList;
        rootDiskList = vm.getDisks().stream().filter(x -> x.getType() == Volume.Type.ROOT).collect(Collectors.toList());
        if (rootDiskList.size() != 1) {
            if (vm.getTemplate().isDeployAsIs()) {
                rootDiskList = vm.getDisks().stream().filter(x -> x.getType() == null).collect(Collectors.toList());
                if (rootDiskList.size() < 1) {
                    throw new CloudRuntimeException("Did not find a template to serve as root disk for VM " + vm.getHostName());
                }
            } else {
                throw new CloudRuntimeException("Did not find only one root disk for VM " + vm.getHostName());
            }
        }
        rootDiskTO = rootDiskList.get(0);
        return rootDiskTO;
    }

    private List<OVFPropertyTO> getOvfPropertyList(VirtualMachineProfile vm, Map<String, String> details) {
        List<OVFPropertyTO> ovfProperties = new ArrayList<OVFPropertyTO>();
        for (String detailKey : details.keySet()) {
            if (detailKey.startsWith(ImageStore.ACS_PROPERTY_PREFIX)) {
                OVFPropertyTO propertyTO = templateDetailsDao.findPropertyByTemplateAndKey(vm.getTemplateId(), detailKey);
                String vmPropertyKey = detailKey.replace(ImageStore.ACS_PROPERTY_PREFIX, "");
                if (propertyTO == null) {
                    LOGGER.warn(String.format("OVF property %s not found on template, discarding", vmPropertyKey));
                    continue;
                }
                propertyTO.setKey(vmPropertyKey);
                propertyTO.setValue(details.get(detailKey));
                ovfProperties.add(propertyTO);
            }
        }
        return ovfProperties;
    }

    private void addReservationDetails(long clusterId, Map<String, String> details) {
        details.put(VMwareGuru.VmwareReserveCpu.key(), VMwareGuru.VmwareReserveCpu.valueIn(clusterId).toString());
        details.put(VMwareGuru.VmwareReserveMemory.key(), VMwareGuru.VmwareReserveMemory.valueIn(clusterId).toString());
    }

    private List<NicProfile> getNicProfiles(VirtualMachineProfile vm, Map<String, String> details) {
        List<NicProfile> nicProfiles = vm.getNics();

        for (NicProfile nicProfile : nicProfiles) {
            if (nicProfile.getTrafficType() == Networks.TrafficType.Guest) {
                if (networkMgr.isProviderSupportServiceInNetwork(nicProfile.getNetworkId(), Network.Service.Firewall, Network.Provider.CiscoVnmc)) {
                    details.put("ConfigureVServiceInNexus", Boolean.TRUE.toString());
                }
                break;
            }
        }
        return nicProfiles;
    }

    private void setDiskControllers(VirtualMachineProfile vm, Map<String, String> details, boolean userVm) {
        String diskDeviceType = details.get(VmDetailConstants.ROOT_DISK_CONTROLLER);
        if (userVm) {
            if (diskDeviceType == null) {
                details.put(VmDetailConstants.ROOT_DISK_CONTROLLER,vmwareMgr.getRootDiskController());
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
    }

    private void setBootParameters(VirtualMachineProfile vm, VirtualMachineTO to, Map<String, String> details) {
        details.put(VmDetailConstants.BOOT_MODE, to.getBootMode());
        if (vm.getParameter(VirtualMachineProfile.Param.BootIntoSetup) != null && (Boolean)vm.getParameter(VirtualMachineProfile.Param.BootIntoSetup) == true) {
            to.setEnterHardwareSetup(true);
        }
// there should also be
//        details.put(VmDetailConstants.BOOT_TYPE, to.getBootType());
    }

    /*
        Remove OVF properties from details to be sent to hypervisor (avoid duplicate data)
     */
    private void removeOvfPropertiesFromDetails(List<OVFPropertyTO> ovfProperties, Map<String, String> details) {
        for (OVFPropertyTO propertyTO : ovfProperties) {
            String key = propertyTO.getKey();
            details.remove(ApiConstants.PROPERTIES + "-" + key);
        }
    }

    /**
     * Adds {@code 'nestedVirtualizationFlag'} value to {@code details} due to if it should be enabled or not
     * @param details vm details should not be null
     * @param to vm to
     */
    protected void configureNestedVirtualization(Map<String, String> details, VirtualMachineTO to) {
        String localNestedV = details.get(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG);

        Boolean globalNestedVirtualisationEnabled = getGlobalNestedVirtualisationEnabled();
        Boolean globalNestedVPerVMEnabled = getGlobalNestedVPerVMEnabled();

        Boolean shouldEnableNestedVirtualization = shouldEnableNestedVirtualization(globalNestedVirtualisationEnabled, globalNestedVPerVMEnabled, localNestedV);
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(
                    "Due to '%B'(globalNestedVirtualisationEnabled) and '%B'(globalNestedVPerVMEnabled) I'm adding a flag with value %B to the vm configuration for Nested Virtualisation.",
                    globalNestedVirtualisationEnabled,
                    globalNestedVPerVMEnabled,
                    shouldEnableNestedVirtualization)
            );
        }
        details.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, Boolean.toString(shouldEnableNestedVirtualization));
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
    Boolean shouldEnableNestedVirtualization(Boolean globalNestedV, Boolean globalNestedVPerVM, String localNestedV) {
        if (globalNestedV == null || globalNestedVPerVM == null) {
            return false;
        }
        boolean globalNV = globalNestedV.booleanValue();
        boolean globalNVPVM = globalNestedVPerVM.booleanValue();

        if (globalNVPVM) {
            return (localNestedV == null && globalNV) || BooleanUtils.toBoolean(localNestedV);
        }
        return globalNV;
    }

    private NicTO[] sortNicsByDeviceId(NicTO[] nics) {

        List<NicTO> listForSort = new ArrayList<NicTO>();
        for (NicTO nic : nics) {
            listForSort.add(nic);
        }
        Collections.sort(listForSort, new Comparator<NicTO>() {

            @Override public int compare(NicTO arg0, NicTO arg1) {
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
}