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
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
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
import com.cloud.storage.TemplateOVFPropertyVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.TemplateOVFPropertiesDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.Pair;
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
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VmwareVmImplementer {
    private static final Logger LOG = Logger.getLogger(VmwareVmImplementer.class);
    private final VMwareGuru guru;

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
    TemplateOVFPropertiesDao templateOVFPropertiesDao;
    @Inject
    VMTemplatePoolDao templateStoragePoolDao;
    @Inject
    VmwareManager vmwareMgr;

    public VmwareVmImplementer(VMwareGuru guru) {
        this.guru = guru;
    }

    VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = guru.toVirtualMachineTO(vm);
        to.setBootloader(VirtualMachineTemplate.BootloaderType.HVM);

        Map<String, String> details = to.getDetails();
        if (details == null)
            details = new HashMap<String, String>();

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
                    LOG.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
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
                    LOG.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
                    details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
                }
            }
        }

        details.put(VmDetailConstants.BOOT_MODE, to.getBootMode());
        if (vm.getParameter(VirtualMachineProfile.Param.BootIntoSetup) != null && (Boolean)vm.getParameter(VirtualMachineProfile.Param.BootIntoSetup) == true) {
            to.setEnterHardwareSetup(true);
        }
// there should also be
//        details.put(VmDetailConstants.BOOT_TYPE, to.getBootType());
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

        List<NicProfile> nicProfiles = vm.getNics();

        for (NicProfile nicProfile : nicProfiles) {
            if (nicProfile.getTrafficType() == Networks.TrafficType.Guest) {
                if (networkMgr.isProviderSupportServiceInNetwork(nicProfile.getNetworkId(), Network.Service.Firewall, Network.Provider.CiscoVnmc)) {
                    details.put("ConfigureVServiceInNexus", Boolean.TRUE.toString());
                }
                break;
            }
        }

        long clusterId = guru.getClusterId(vm.getId());
        details.put(com.cloud.hypervisor.guru.VMwareGuru.VmwareReserveCpu.key(), com.cloud.hypervisor.guru.VMwareGuru.VmwareReserveCpu.valueIn(clusterId).toString());
        details.put(com.cloud.hypervisor.guru.VMwareGuru.VmwareReserveMemory.key(), com.cloud.hypervisor.guru.VMwareGuru.VmwareReserveMemory.valueIn(clusterId).toString());
        to.setDetails(details);

        if (vmType.equals(VirtualMachine.Type.DomainRouter)) {

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
            for (NicTO nicTo : guru.sortNicsByDeviceId(to.getNics())) {
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
            guru.configureNestedVirtualization(details, to);
        }
        // Determine the VM's OS description
        GuestOSVO guestOS = guestOsDao.findByIdIncludingRemoved(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        to.setHostName(vm.getHostName());
        HostVO host = hostDao.findById(vm.getVirtualMachine().getHostId());
        GuestOSHypervisorVO guestOsMapping = null;
        if (host != null) {
            guestOsMapping = guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), guru.getHypervisorType().toString(), host.getHypervisorVersion());
        }
        if (guestOsMapping == null || host == null) {
            to.setPlatformEmulator(null);
        } else {
            to.setPlatformEmulator(guestOsMapping.getGuestOsName());
        }

        List<OVFPropertyTO> ovfProperties = new ArrayList<OVFPropertyTO>();
        for (String detailKey : details.keySet()) {
            if (detailKey.startsWith(ApiConstants.OVF_PROPERTIES)) {
                String ovfPropKey = detailKey.replace(ApiConstants.OVF_PROPERTIES + "-", "");
                TemplateOVFPropertyVO templateOVFPropertyVO = templateOVFPropertiesDao.findByTemplateAndKey(vm.getTemplateId(), ovfPropKey);
                if (templateOVFPropertyVO == null) {
                    LOG.warn(String.format("OVF property %s not found on template, discarding", ovfPropKey));
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
            StoragePoolVO storagePoolVO = storagePoolDao.findByUuid(dataStore.getUuid());
            long dataCenterId = storagePoolVO.getDataCenterId();
            List<StoragePoolVO> pools = storagePoolDao.listByDataCenterId(dataCenterId);
            for (StoragePoolVO pool : pools) {
                VMTemplateStoragePoolVO ref = templateStoragePoolDao.findByPoolTemplate(pool.getId(), vm.getTemplateId());
                if (ref != null && ref.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                    templateInstallPath = ref.getInstallPath();
                    break;
                }
            }

            if (templateInstallPath == null) {
                throw new CloudRuntimeException("Did not find the template install path for template " + vm.getTemplateId() + " on zone " + dataCenterId);
            }

            Pair<String, List<OVFPropertyTO>> pair = new Pair<String, List<OVFPropertyTO>>(templateInstallPath, ovfProperties);
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
}