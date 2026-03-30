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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.agent.api.to.VirtualMachineMetadataTO;
import com.cloud.cpu.CPU;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.gpu.VgpuProfileVO;
import com.cloud.gpu.dao.VgpuProfileDao;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.ResourceTag;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.gpu.GPU;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
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
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

public abstract class HypervisorGuruBase extends AdapterBase implements HypervisorGuru, Configurable {

    @Inject
    protected
    NicDao nicDao;
    @Inject
    protected
    NetworkDao networkDao;
    @Inject
    protected VpcDao vpcDao;
    @Inject
    protected AccountManager accountManager;
    @Inject
    protected DomainDao domainDao;
    @Inject
    private DataCenterDao dcDao;
    @Inject
    private NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Inject
    protected
    VMInstanceDao virtualMachineDao;
    @Inject
    private VMInstanceDetailsDao _vmInstanceDetailsDao;
    @Inject
    private NicSecondaryIpDao _nicSecIpDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    protected ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    protected VgpuProfileDao vgpuProfileDao;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    private NetworkDetailsDao networkDetailsDao;
    @Inject
    protected
    HostDao hostDao;
    @Inject
    private UserVmManager userVmManager;
    @Inject
    protected UserVmDao userVmDao;
    @Inject
    protected ProjectDao projectDao;
    @Inject
    protected ClusterDao clusterDao;
    @Inject
    protected DataCenterDao dataCenterDao;
    @Inject
    protected HostPodDao hostPodDao;
    @Inject
    private ConfigurationManager configurationManager;
    @Inject
    ResourceTagDao tagsDao;

    public static ConfigKey<Boolean> VmMinMemoryEqualsMemoryDividedByMemOverprovisioningFactor = new ConfigKey<Boolean>("Advanced", Boolean.class, "vm.min.memory.equals.memory.divided.by.mem.overprovisioning.factor", "true",
            "If we set this to 'true', a minimum memory (memory/ mem.overprovisioning.factor) will be set to the VM, independent of using a scalable service offering or not.", true, ConfigKey.Scope.Cluster);

    public static ConfigKey<Boolean> VmMinCpuSpeedEqualsCpuSpeedDividedByCpuOverprovisioningFactor = new ConfigKey<Boolean>("Advanced", Boolean.class, "vm.min.cpu.speed.equals.cpu.speed.divided.by.cpu.overprovisioning.factor", "true",
            "If we set this to 'true', a minimum CPU speed (cpu speed/ cpu.overprovisioning.factor) will be set on the VM, independent of using a scalable service offering or not.", true, ConfigKey.Scope.Cluster);

    private Map<NetworkOffering.Detail, String> getNicDetails(Network network) {
        if (network == null) {
            logger.debug("Unable to get NIC details as the network is null");
            return null;
        }
        Map<NetworkOffering.Detail, String> details = networkOfferingDetailsDao.getNtwkOffDetails(network.getNetworkOfferingId());
        if (details != null) {
            details.putIfAbsent(NetworkOffering.Detail.PromiscuousMode, NetworkOrchestrationService.PromiscuousMode.value().toString());
            details.putIfAbsent(NetworkOffering.Detail.MacAddressChanges, NetworkOrchestrationService.MacAddressChanges.value().toString());
            details.putIfAbsent(NetworkOffering.Detail.ForgedTransmits, NetworkOrchestrationService.ForgedTransmits.value().toString());
            details.putIfAbsent(NetworkOffering.Detail.MacLearning, NetworkOrchestrationService.MacLearning.value().toString());
        }
        NetworkDetailVO pvlantypeDetail = networkDetailsDao.findDetail(network.getId(), ApiConstants.ISOLATED_PVLAN_TYPE);
        if (pvlantypeDetail != null) {
            details.putIfAbsent(NetworkOffering.Detail.pvlanType, pvlantypeDetail.getValue());
        }
        return details;
    }

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
        to.setBroadcastUri(profile.getBroadCastUri());
        to.setIsolationuri(profile.getIsolationUri());
        to.setNetworkRateMbps(profile.getNetworkRate());
        to.setName(profile.getName());
        to.setSecurityGroupEnabled(profile.isSecurityGroupEnabled());
        to.setIp6Address(profile.getIPv6Address());
        to.setIp6Gateway(profile.getIPv6Gateway());
        to.setIp6Cidr(profile.getIPv6Cidr());
        to.setMtu(profile.getMtu());
        to.setIp6Dns1(profile.getIPv6Dns1());
        to.setIp6Dns2(profile.getIPv6Dns2());
        to.setNetworkId(profile.getNetworkId());

        NetworkVO network = networkDao.findById(profile.getNetworkId());
        to.setNetworkUuid(network.getUuid());
        Account account = accountManager.getAccount(network.getAccountId());
        Domain domain = domainDao.findById(network.getDomainId());
        DataCenter zone = dcDao.findById(network.getDataCenterId());
        if (Objects.isNull(zone)) {
            throw new CloudRuntimeException(String.format("Failed to find zone with ID: %s", network.getDataCenterId()));
        }
        if (Objects.isNull(account)) {
            throw new CloudRuntimeException(String.format("Failed to find account with ID: %s", network.getAccountId()));
        }
        if (Objects.isNull(domain)) {
            throw new CloudRuntimeException(String.format("Failed to find domain with ID: %s", network.getDomainId()));
        }
        VpcVO vpc = null;
        if (Objects.nonNull(network.getVpcId())) {
            vpc = vpcDao.findById(network.getVpcId());
        }
        to.setNetworkSegmentName(getNetworkName(zone.getId(), domain.getId(), account.getId(), vpc, network.getId()));

        // Workaround to make sure the TO has the UUID we need for Nicira integration
        NicVO nicVO = nicDao.findById(profile.getId());
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
            logger.warn("Unable to load NicVO for NicProfile {}", profile);
            //Workaround for dynamically created nics
            //FixMe: uuid and secondary IPs can be made part of nic profile
            to.setUuid(UUID.randomUUID().toString());
        }
        to.setDetails(getNicDetails(network));

        //check whether the this nic has secondary ip addresses set
        //set nic secondary ip address in NicTO which are used for security group
        // configuration. Use full when vm stop/start
        return to;
    }

    private String getNetworkName(long zoneId, long domainId, long accountId, VpcVO vpc, long networkId) {
        String prefix = String.format("D%s-A%s-Z%s", domainId, accountId, zoneId);
        if (Objects.isNull(vpc)) {
            return prefix + "-S" + networkId;
        }
        return prefix + "-V" + vpc.getId() + "-S" + networkId;
    }


    /**
     * Add extra configuration from VM details. Extra configuration is stored as details starting with 'extraconfig'
     */
    private void addExtraConfig(Map<String, String> details, VirtualMachineTO to, long accountId, Hypervisor.HypervisorType hypervisorType) {
        for (String key : details.keySet()) {
            if (key.startsWith(ApiConstants.EXTRA_CONFIG)) {
                String extraConfig = details.get(key);
                userVmManager.validateExtraConfig(accountId, hypervisorType, extraConfig);
                to.addExtraConfig(key, extraConfig);
            }
        }
    }

    /**
     * Add extra configurations from service offering to the VM TO.
     * Extra configuration keys are expected in formats:
     * - "extraconfig-N"
     * - "extraconfig-CONFIG_NAME"
     */
    protected void addServiceOfferingExtraConfiguration(ServiceOffering offering, VirtualMachineTO to) {
        List<ServiceOfferingDetailsVO> details = _serviceOfferingDetailsDao.listDetails(offering.getId());
        if (CollectionUtils.isNotEmpty(details)) {
            for (ServiceOfferingDetailsVO detail : details) {
                if (detail.getName().startsWith(ApiConstants.EXTRA_CONFIG)) {
                    configurationManager.validateExtraConfigInServiceOfferingDetail(detail.getName());
                    to.addExtraConfig(detail.getName(), detail.getValue());
                }
            }
        }
    }

    protected VirtualMachineTO toVirtualMachineTO(VirtualMachineProfile vmProfile) {
        ServiceOffering offering = serviceOfferingDao.findById(vmProfile.getId(), vmProfile.getServiceOfferingId());
        VirtualMachine vm = vmProfile.getVirtualMachine();
        Long clusterId = findClusterOfVm(vm);
        boolean divideMemoryByOverprovisioning = true;
        boolean divideCpuByOverprovisioning = true;

        if (clusterId != null) {
            divideMemoryByOverprovisioning = VmMinMemoryEqualsMemoryDividedByMemOverprovisioningFactor.valueIn(clusterId);
            divideCpuByOverprovisioning = VmMinCpuSpeedEqualsCpuSpeedDividedByCpuOverprovisioningFactor.valueIn(clusterId);
        }

        Long minMemory = (long)(offering.getRamSize() / (divideMemoryByOverprovisioning ? vmProfile.getMemoryOvercommitRatio() : 1));
        int minspeed = (int)(offering.getSpeed() / (divideCpuByOverprovisioning ? vmProfile.getCpuOvercommitRatio() : 1));
        int maxspeed = (offering.getSpeed());
        VirtualMachineTO to = new VirtualMachineTO(vm.getId(), vm.getInstanceName(), vm.getType(), offering.getCpu(), minspeed, maxspeed, minMemory * 1024l * 1024l,
                offering.getRamSize() * 1024l * 1024l, null, null, vm.isHaEnabled(), vm.limitCpuUse(), vm.getVncPassword());
        to.setBootArgs(vmProfile.getBootArgs());

        Map<VirtualMachineProfile.Param, Object> map = vmProfile.getParameters();
        if (MapUtils.isNotEmpty(map)) {
            if (map.containsKey(VirtualMachineProfile.Param.BootMode)) {
                if (StringUtils.isNotBlank((String) map.get(VirtualMachineProfile.Param.BootMode))) {
                    to.setBootMode((String) map.get(VirtualMachineProfile.Param.BootMode));
                }
            }

            if (map.containsKey(VirtualMachineProfile.Param.BootType)) {
                if (StringUtils.isNotBlank((String) map.get(VirtualMachineProfile.Param.BootType))) {
                    to.setBootType((String) map.get(VirtualMachineProfile.Param.BootType));
                }
            }
        }

        List<NicProfile> nicProfiles = vmProfile.getNics();
        NicTO[] nics = new NicTO[nicProfiles.size()];
        int i = 0;
        for (NicProfile nicProfile : nicProfiles) {
            if (vm.getType() == VirtualMachine.Type.NetScalerVm) {
                nicProfile.setBroadcastType(BroadcastDomainType.Native);
            }
            NicTO nicTo = toNicTO(nicProfile);
            nics[i++] = nicTo;
        }

        to.setNics(nics);
        to.setDisks(vmProfile.getDisks().toArray(new DiskTO[vmProfile.getDisks().size()]));

        CPU.CPUArch templateArch = vmProfile.getTemplate().getArch();
        if (templateArch != null) {
            to.setArch(templateArch.getType());
        } else {
            if (vmProfile.getTemplate().getBits() == 32) {
                to.setArch(CPU.CPUArch.x86.getType());
            } else if("s390x".equals(System.getProperty("os.arch"))) {
                to.setArch("s390x");
            } else {
                to.setArch(CPU.CPUArch.amd64.getType());
            }
        }

        Map<String, String> detailsInVm = _vmInstanceDetailsDao.listDetailsKeyPairs(vm.getId());
        if (detailsInVm != null) {
            to.setDetails(detailsInVm);
            addExtraConfig(detailsInVm, to, vm.getAccountId(), vm.getHypervisorType());
        }

        addServiceOfferingExtraConfiguration(offering, to);

        // Set GPU details
        ServiceOfferingDetailsVO offeringDetail = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.vgpuType.toString());
        if (offering.getVgpuProfileId() != null || offeringDetail != null) {
                to.setGpuDevice(getGpuDevice(offering, offeringDetail, vm, vmProfile.getHostId()));
        }

        // Workaround to make sure the TO has the UUID we need for Niciri integration
        VMInstanceVO vmInstance = virtualMachineDao.findById(to.getId());
        to.setEnableDynamicallyScaleVm(vmInstance.isDynamicallyScalable());
        to.setUuid(vmInstance.getUuid());

        to.setVmData(vmProfile.getVmData());
        to.setConfigDriveLabel(vmProfile.getConfigDriveLabel());
        to.setConfigDriveIsoRootFolder(vmProfile.getConfigDriveIsoRootFolder());
        to.setConfigDriveIsoFile(vmProfile.getConfigDriveIsoFile());
        to.setConfigDriveLocation(vmProfile.getConfigDriveLocation());
        to.setState(vm.getState());

        return to;
    }

    private GPUDeviceTO getGpuDevice(ServiceOffering offering, ServiceOfferingDetailsVO offeringDetail, VirtualMachine vm, long hostId) {
        if (offering.getVgpuProfileId() != null) {
            VgpuProfileVO vgpuProfile = vgpuProfileDao.findById(offering.getVgpuProfileId());
            if (vgpuProfile != null) {
                int gpuCount = offering.getGpuCount() != null ? offering.getGpuCount() : 1;
                return _resourceMgr.getGPUDevice(vm, hostId, vgpuProfile, gpuCount);
            }
        } else if (offeringDetail != null) {
            ServiceOfferingDetailsVO groupName = _serviceOfferingDetailsDao.findDetail(offering.getId(), GPU.Keys.pciDevice.toString());
            return _resourceMgr.getGPUDevice(vm.getHostId(), groupName.getValue(), offeringDetail.getValue());
        }
        return null;
    }


    protected Long findClusterOfVm(VirtualMachine vm) {
        HostVO host = hostDao.findById(vm.getHostId());
        if (host != null) {
            return host.getClusterId();
        }

        logger.debug(String.format("VM [%s] does not have a host id. Trying the last host.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm, "instanceName", "id", "uuid")));
        host = hostDao.findById(vm.getLastHostId());
        if (host != null) {
            return host.getClusterId();
        }

        logger.debug(String.format("VM [%s] does not have a last host id.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm, "instanceName", "id", "uuid")));
        return null;
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
    public VirtualMachine importVirtualMachineFromBackup(long zoneId, long domainId, long accountId, long userId,
                                                         String vmInternalName, Backup backup) throws Exception {
        return null;
    }

    @Override
    public boolean attachRestoredVolumeToVirtualMachine(long zoneId, String location, Backup.VolumeInfo volumeInfo,
                                                        VirtualMachine vm, long poolId, Backup backup) throws Exception {
        return false;
    }

    public List<Command> finalizeMigrate(VirtualMachine vm, Map<Volume, StoragePool> volumeToPool) {
        return null;
    }

     @Override
    public String getConfigComponentName() {
        return HypervisorGuruBase.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {VmMinMemoryEqualsMemoryDividedByMemOverprovisioningFactor,
                VmMinCpuSpeedEqualsCpuSpeedDividedByCpuOverprovisioningFactor,
                HypervisorCustomDisplayName
        };
    }

    @Override
    public Pair<UnmanagedInstanceTO, Boolean> getHypervisorVMOutOfBandAndCloneIfRequired(String hostIp, String vmName, Map<String, String> params) {
        logger.error("Unsupported operation: cannot clone external VM");
        return null;
    }

    @Override
    public boolean removeClonedHypervisorVMOutOfBand(String hostIp, String vmName, Map<String, String> params) {
        logger.error("Unsupported operation: cannot remove external VM");
        return false;
    }

    @Override
    public String createVMTemplateOutOfBand(String hostIp, String vmName, Map<String, String> params, DataStoreTO templateLocation, int threadsCountToExportOvf) {
        logger.error("Unsupported operation: cannot create template file");
        return null;
    }

    @Override
    public boolean removeVMTemplateOutOfBand(DataStoreTO templateLocation, String templateDir) {
        logger.error("Unsupported operation: cannot remove template file");
        return false;
    }

    /**
     * Generates VirtualMachineMetadataTO object from VirtualMachineProfile
     * It is a helper function to be used in the inherited classes to avoid repetition
     * while generating metadata for multiple Guru implementations
     *
     * @param  vmProfile  virtual machine profile object
     * @return      A VirtualMachineMetadataTO ready to be appended to VirtualMachineTO object
     * @see         KVMGuru
     */
    protected VirtualMachineMetadataTO makeVirtualMachineMetadata(VirtualMachineProfile vmProfile) {
        String vmName = "unknown",
                instanceName = "unknown",
                displayName = "unknown",
                instanceUuid = "unknown",
                clusterName = "unknown",
                clusterUuid = "unknown",
                zoneUuid = "unknown",
                zoneName = "unknown",
                podUuid = "unknown",
                podName = "unknown",
                domainUuid = "unknown",
                domainName = "unknown",
                accountUuid = "unknown",
                accountName = "unknown",
                projectName = "", // the project can be empty
                projectUuid = "", // the project can be empty
                serviceOfferingName = "unknown";
        long created = 0L;
        Integer cpuCores = -1, memory = -1;
        List<String> serviceOfferingTags = new ArrayList<>();
        HashMap<String, String> resourceTags = new HashMap<>();

        UserVmVO vmVO = userVmDao.findById(vmProfile.getVirtualMachine().getId());
        if (vmVO != null) {
            instanceUuid = vmVO.getUuid();
            vmName = vmVO.getHostName(); // this returns the VM name field
            instanceName = vmVO.getInstanceName();
            displayName = vmVO.getDisplayName();
            created = vmVO.getCreated().getTime() / 1000L;

            HostVO host = hostDao.findById(vmVO.getHostId());
            if (host != null) {
                // Find zone and cluster
                Long clusterId = host.getClusterId();
                ClusterVO cluster = clusterDao.findById(clusterId);

                if (cluster != null) {
                    clusterName = cluster.getName();
                    clusterUuid = cluster.getUuid();

                    DataCenterVO zone = dataCenterDao.findById(cluster.getDataCenterId());
                    if (zone != null) {
                        zoneUuid = zone.getUuid();
                        zoneName = zone.getName();
                    }

                    HostPodVO pod = hostPodDao.findById(cluster.getPodId());
                    if (pod != null) {
                        podUuid = pod.getUuid();
                        podName = pod.getName();
                    }
                }
            } else {
                logger.warn("Could not find the Host object for the virtual machine (null value returned). Libvirt metadata for cluster, pod, zone will not be populated.");
            }

            DomainVO domain = domainDao.findById(vmVO.getDomainId());
            if (domain != null) {
                domainUuid = domain.getUuid();
                domainName = domain.getName();
            } else {
                logger.warn("Could not find the Domain object for the virtual machine (null value returned). Libvirt metadata for domain will not be populated.");
            }

            Account account = accountManager.getAccount(vmVO.getAccountId());
            if (account != null) {
                accountUuid = account.getUuid();
                accountName = account.getName();

                ProjectVO project = projectDao.findByProjectAccountId(account.getId());
                if (project != null) {
                    projectName = project.getName();
                    projectUuid = project.getUuid();
                }
            } else {
                logger.warn("Could not find the Account object for the virtual machine (null value returned). Libvirt metadata for account and project will not be populated.");
            }

            List<? extends ResourceTag> resourceTagsList = tagsDao.listBy(vmVO.getId(), ResourceTag.ResourceObjectType.UserVm);
            if (resourceTagsList != null) {
                for (ResourceTag tag : resourceTagsList) {
                    resourceTags.put(tag.getKey(), tag.getValue());
                }
            }
        } else {
            logger.warn("Could not find the VirtualMachine object by its profile (null value returned). Libvirt metadata will not be populated.");
        }

        ServiceOffering serviceOffering = vmProfile.getServiceOffering();
        if (serviceOffering != null) {
            serviceOfferingName = serviceOffering.getName();
            cpuCores = serviceOffering.getCpu();
            memory = serviceOffering.getRamSize();

            String hostTagsCommaSeparated = serviceOffering.getHostTag();
            if (hostTagsCommaSeparated != null) { // when service offering has no host tags, this value is null
                serviceOfferingTags = Arrays.asList(hostTagsCommaSeparated.split(","));
            }
        } else {
            logger.warn("Could not find the ServiceOffering object by its profile (null value returned). Libvirt metadata for service offering will not be populated.");
        }


        return new VirtualMachineMetadataTO(
                vmName, // name
                instanceName, // internalName
                displayName, // displayName
                instanceUuid , // instanceUUID
                cpuCores, // cpuCores
                memory, // memory
                created, // created, unix epoch in seconds
                System.currentTimeMillis() / 1000L, // started, unix epoch in seconds
                domainUuid, // ownerDomainUUID
                domainName, // ownerDomainName
                accountUuid, // ownerAccountUUID
                accountName, // ownerAccountName
                projectUuid,
                projectName,
                serviceOfferingName,
                serviceOfferingTags, // serviceOfferingTags
                zoneName,
                zoneUuid,
                podName,
                podUuid,
                clusterName,
                clusterUuid,
                resourceTags
        );
    }
}
