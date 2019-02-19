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
package com.cloud.hypervisor;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.gpu.GPU;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.StoragePool;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public abstract class HypervisorGuruBase extends AdapterBase implements HypervisorGuru {
    public static final Logger s_logger = Logger.getLogger(HypervisorGuruBase.class);

    @Inject
    private NicDao _nicDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Inject
    private VMInstanceDao _virtualMachineDao;
    @Inject
    private UserVmDetailsDao _userVmDetailsDao;
    @Inject
    private NicSecondaryIpDao _nicSecIpDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;

    @Override
    public NicTO toNicTO(NicProfile profile) {
        NicTO to = new NicTO();
        to.setDeviceId(profile.getDeviceId());
        to.setBroadcastType(profile.getBroadcastType());
        to.setType(profile.getTrafficType());
        to.setIp(profile.getIPv4Address());
        to.setNetmask(profile.getIPv4Netmask());
        to.setMac(profile.getMacAddress());
        to.setDns1(profile.getIPv4Dns1());
        to.setDns2(profile.getIPv4Dns2());
        to.setGateway(profile.getIPv4Gateway());
        to.setDefaultNic(profile.isDefaultNic());
        to.setMtu(profile.getMtu());
        to.setBroadcastUri(profile.getBroadCastUri());
        to.setIsolationuri(profile.getIsolationUri());
        to.setNetworkRateMbps(profile.getNetworkRate());
        to.setName(profile.getName());
        to.setSecurityGroupEnabled(profile.isSecurityGroupEnabled());
        to.setIp6Address(profile.getIPv6Address());
        to.setIp6Cidr(profile.getIPv6Cidr());

        NetworkVO network = _networkDao.findById(profile.getNetworkId());
        to.setNetworkUuid(network.getUuid());

        // Workaround to make sure the TO has the UUID we need for Nicira integration
        NicVO nicVO = _nicDao.findById(profile.getId());
        if (nicVO != null) {
            to.setUuid(nicVO.getUuid());
            // disable pxe on system vm nics to speed up boot time
            if (nicVO.getVmType() != VirtualMachine.Type.User) {
                to.setPxeDisable(true);
            }
            List<String> secIps = null;
            if (nicVO.getSecondaryIp()) {
                secIps = _nicSecIpDao.getSecondaryIpAddressesForNic(nicVO.getId());
            }
            to.setNicSecIps(secIps);
        } else {
            s_logger.warn("Unabled to load NicVO for NicProfile " + profile.getId());
            //Workaround for dynamically created nics
            //FixMe: uuid and secondary IPs can be made part of nic profile
            to.setUuid(UUID.randomUUID().toString());
        }

        //check whether the this nic has secondary ip addresses set
        //set nic secondary ip address in NicTO which are used for security group
        // configuration. Use full when vm stop/start
        return to;
    }

    protected VirtualMachineTO toVirtualMachineTO(VirtualMachineProfile vmProfile) {
        ServiceOffering offering = _serviceOfferingDao.findById(vmProfile.getId(), vmProfile.getServiceOfferingId());
        VirtualMachine vm = vmProfile.getVirtualMachine();
        Long minMemory = (long)(offering.getRamSize() / vmProfile.getMemoryOvercommitRatio());
        int minspeed = (int)(offering.getSpeed() / vmProfile.getCpuOvercommitRatio());
        int maxspeed = (offering.getSpeed());
        VirtualMachineTO to = new VirtualMachineTO(vm.getId(), vm.getInstanceName(), vm.getType(), offering.getCpu(), minspeed, maxspeed, minMemory * 1024l * 1024l,
                offering.getRamSize() * 1024l * 1024l, null, null, vm.isHaEnabled(), vm.limitCpuUse(), vm.getVncPassword());
        to.setBootArgs(vmProfile.getBootArgs());

        List<NicProfile> nicProfiles = vmProfile.getNics();
        NicTO[] nics = new NicTO[nicProfiles.size()];
        int i = 0;
        for (NicProfile nicProfile : nicProfiles) {
            if (vm.getType() == VirtualMachine.Type.NetScalerVm) {
                nicProfile.setBroadcastType(BroadcastDomainType.Native);
            }
            NicTO nicTo = toNicTO(nicProfile);
            final NetworkVO network = _networkDao.findByUuid(nicTo.getNetworkUuid());
            if (network != null) {
                final Map<NetworkOffering.Detail, String> details = networkOfferingDetailsDao.getNtwkOffDetails(network.getNetworkOfferingId());
                if (details != null) {
                    details.putIfAbsent(NetworkOffering.Detail.PromiscuousMode, NetworkOrchestrationService.PromiscuousMode.value().toString());
                    details.putIfAbsent(NetworkOffering.Detail.MacAddressChanges, NetworkOrchestrationService.MacAddressChanges.value().toString());
                    details.putIfAbsent(NetworkOffering.Detail.ForgedTransmits, NetworkOrchestrationService.ForgedTransmits.value().toString());
                }
                nicTo.setDetails(details);
            }
            nics[i++] = nicTo;
        }

        to.setNics(nics);
        to.setDisks(vmProfile.getDisks().toArray(new DiskTO[vmProfile.getDisks().size()]));

        if (vmProfile.getTemplate().getBits() == 32) {
            to.setArch("i686");
        } else {
            to.setArch("x86_64");
        }

        Map<String, String> detailsInVm = _userVmDetailsDao.listDetailsKeyPairs(vm.getId());
        if (detailsInVm != null) {
            to.setDetails(detailsInVm);
        }

        // Set GPU details
        ServiceOfferingDetailsVO offeringDetail = null;
        if ((offeringDetail = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.vgpuType.toString())) != null) {
            ServiceOfferingDetailsVO groupName = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.pciDevice.toString());
            to.setGpuDevice(_resourceMgr.getGPUDevice(vm.getHostId(), groupName.getValue(), offeringDetail.getValue()));
        }

        // Workaround to make sure the TO has the UUID we need for Niciri integration
        VMInstanceVO vmInstance = _virtualMachineDao.findById(to.getId());
        // check if XStools/VMWare tools are present in the VM and dynamic scaling feature is enabled (per zone/global)
        Boolean isDynamicallyScalable = vmInstance.isDynamicallyScalable() && UserVmManager.EnableDynamicallyScaleVm.valueIn(vm.getDataCenterId());
        to.setEnableDynamicallyScaleVm(isDynamicallyScalable);
        to.setUuid(vmInstance.getUuid());

        to.setVmData(vmProfile.getVmData());
        to.setConfigDriveLabel(vmProfile.getConfigDriveLabel());
        to.setConfigDriveIsoRootFolder(vmProfile.getConfigDriveIsoRootFolder());
        to.setConfigDriveIsoFile(vmProfile.getConfigDriveIsoFile());
        to.setState(vm.getState());

        return to;
    }

    @Override
    /**
     * The basic implementation assumes that the initial "host" defined to execute the command is the host that is in fact going to execute it.
     * However, subclasses can extend this behavior, changing the host that is going to execute the command in runtime.
     * The first element of the 'Pair' indicates if the hostId has been changed; this means, if you change the hostId, but you do not inform this action in the return 'Pair' object, we will use the original "hostId".
     *
     * Side note: it seems that the 'hostId' received here is normally the ID of the SSVM that has an entry at the host table. Therefore, this methods gives the opportunity to change from the SSVM to a real host to execute a command.
     */
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
    }

    @Override
    public List<Command> finalizeExpunge(VirtualMachine vm) {
        return null;
    }

    @Override
    public List<Command> finalizeExpungeNics(VirtualMachine vm, List<NicProfile> nics) {
        return null;
    }

    @Override
    public List<Command> finalizeExpungeVolumes(VirtualMachine vm) {
        return null;
    }

    @Override
    public Map<String, String> getClusterSettings(long vmId) {
        return null;
    }

    @Override
    public List<Command> finalizeMigrate(VirtualMachine vm, StoragePool destination) {
        return null;
    }
}
