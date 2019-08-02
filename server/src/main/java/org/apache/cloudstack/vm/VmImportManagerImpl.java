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

package org.apache.cloudstack.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.ingestion.ImportUnmanageInstanceCmd;
import org.apache.cloudstack.api.command.admin.ingestion.ListUnmanagedInstancesCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceDiskResponse;
import org.apache.cloudstack.api.response.UnmanagedInstanceResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.serializer.GsonHelper;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;
import com.google.gson.Gson;

public class VmImportManagerImpl implements VmImportService {
    private static final Logger LOGGER = Logger.getLogger(VmImportManagerImpl.class);

    @Inject
    private AgentManager agentManager;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private AccountService accountService;
    @Inject
    private UserDao userDao;
    @Inject
    private VMTemplateDao templateDao;
    @Inject
    private VMTemplatePoolDao templatePoolDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private UserVmService userVmService;
    @Inject
    public ResponseGenerator responseGenerator;
    @Inject
    private VolumeOrchestrationService volumeManager;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private NetworkOrchestrationService networkOrchestrationService;
    @Inject
    protected VMInstanceDao _vmDao;
    @Inject
    protected VlanDao vlanDao;

    protected Gson gson;

    public VmImportManagerImpl() {
        gson = GsonHelper.getGsonLogger();
    }

    private UnmanagedInstanceResponse createUnmanagedInstanceResponse(UnmanagedInstance instance, Cluster cluster, Host host) {
        UnmanagedInstanceResponse response = new UnmanagedInstanceResponse();
        response.setName(instance.getName());
        if (cluster != null) {
            response.setClusterId(cluster.getUuid());
        }
        if (host != null) {
            response.setHostId(host.getUuid());
        }
        response.setPowerState(instance.getPowerState());
        response.setCpuCores(instance.getCpuCores());
        response.setCpuSpeed(instance.getCpuSpeed());
        response.setCpuCoresPerSocket(instance.getCpuCoresPerSocket());
        response.setMemory(instance.getMemory());
        response.setOperatingSystem(instance.getOperatingSystem());
        response.setObjectName(UnmanagedInstance.class.getSimpleName().toLowerCase());

        if (instance.getDisks() != null) {
            for (UnmanagedInstance.Disk disk : instance.getDisks()) {
                UnmanagedInstanceDiskResponse diskResponse = new UnmanagedInstanceDiskResponse();
                diskResponse.setDiskId(disk.getDiskId());
                if (!Strings.isNullOrEmpty(disk.getLabel())) {
                    diskResponse.setLabel(disk.getLabel());
                }
                diskResponse.setCapacity(disk.getCapacity());
                diskResponse.setController(disk.getController());
                diskResponse.setControllerUnit(disk.getControllerUnit());
                diskResponse.setPosition(disk.getPosition());
                diskResponse.setImagePath(disk.getImagePath());
                response.addDisk(diskResponse);
            }
        }

        if (instance.getNics() != null) {
            for (UnmanagedInstance.Nic nic : instance.getNics()) {
                NicResponse nicResponse = new NicResponse();
                nicResponse.setId(nic.getNicId());
                nicResponse.setNetworkName(nic.getNetwork());
                nicResponse.setMacAddress(nic.getMacAddress());
                //nicResponse.setIpaddress(nic.getIpAddress());
                nicResponse.setVlanId(nic.getVlan());
                response.addNic(nicResponse);
            }
        }
        return response;
    }

    private List<String> getHostManagedVms(Host host) {
        List<String> managedVms = new ArrayList<>();
        List<VMInstanceVO> instances = _vmDao.listByHostId(host.getId());
        for (VMInstanceVO instance : instances) {
            managedVms.add(instance.getInstanceName());
        }
        instances = _vmDao.listByLastHostId(host.getId());
        for (VMInstanceVO instance : instances) {
            managedVms.add(instance.getInstanceName());
        }
        return managedVms;
    }

    private DiskProfile importDisk(UnmanagedInstance.Disk disk, VirtualMachine vm, DiskOffering diskOffering,
                                   Volume.Type type, String name, Long diskSize, VirtualMachineTemplate template,
                                   Account owner, Long deviceId) {
        VirtualMachineDiskInfo diskInfo = new VirtualMachineDiskInfo();
        diskInfo.setDiskDeviceBusName(String.format("%s%d:%d", disk.getController(), disk.getControllerUnit(), disk.getPosition()));
        diskInfo.setDiskChain(new String[]{disk.getImagePath()});
        String path = disk.getImagePath();
        long poolId = 0;
        if (vm.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
            String[] splits = path.split(" ");
            String poolUuid = splits[0];
            poolUuid = poolUuid.replace("[", "").replace("]", "");
            StoragePool storagePool = null;
            if (poolUuid.length() == 32) {
                poolUuid = String.format("%s-%s-%s-%s-%s", poolUuid.substring(0, 8),
                        poolUuid.substring(8, 12), poolUuid.substring(12, 16),
                        poolUuid.substring(16, 20), poolUuid.substring(20, 32));
                storagePool = primaryDataStoreDao.findPoolByUUID(poolUuid);
            }
            if (storagePool != null) {
                poolId = storagePool.getId();
            }
            path = String.join(" ", Arrays.copyOfRange(splits, 1, splits.length));
            splits = path.split("/");
            path = splits[splits.length - 1];
            splits = path.split("\\.");
            path = splits[0];
        }
        return volumeManager.importVolume(type, name, diskOffering, diskSize,
                diskOffering.getMinIops(), diskOffering.getMaxIops(), vm, template, owner, deviceId, poolId, path, gson.toJson(diskInfo));
    }

    private NicProfile importNic(UnmanagedInstance.Nic nic, VirtualMachine vm, Network network, String ipAddress, boolean isDefaultNic) {
        Pair<NicProfile, Integer> result = networkOrchestrationService.importNic(nic.getMacAddress(), 0, network, isDefaultNic, vm, ipAddress);
        if (result == null) {
            return null;
        }
        return result.first();
    }

    @Override
    public ListResponse<UnmanagedInstanceResponse> listUnmanagedInstances(ListUnmanagedInstancesCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, Calling account is not root admin: %s", caller.getUuid()));
        }
        final Long clusterId = cmd.getClusterId();
        if (clusterId == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID cannot be null!"));
        }
        final Cluster cluster = clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID: %d cannot be found!", clusterId));
        }
        if (cluster.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new InvalidParameterValueException(String.format("VM ingestion is currently not supported for hypervisor: %s", cluster.getHypervisorType().toString()));
        }

        List<HostVO> hosts = resourceManager.listHostsInClusterByStatus(clusterId, Status.Up);

        List<String> templatesFilterList = new ArrayList<>();

        if (cluster.getHypervisorType() == Hypervisor.HypervisorType.VMware) { // Add filter for templates for VMware
            List<VMTemplateStoragePoolVO> templates = templatePoolDao.listAll();
            for (VMTemplateStoragePoolVO template : templates) {
                templatesFilterList.add(template.getInstallPath());
            }
        }

        List<UnmanagedInstanceResponse> responses = new ArrayList<>();
        for (HostVO host : hosts) {
            List<String> managedVms = new ArrayList<>();
            managedVms.addAll(templatesFilterList);
            managedVms.addAll(getHostManagedVms(host));

            GetUnmanagedInstancesCommand command = new GetUnmanagedInstancesCommand();
            command.setInstanceName(cmd.getName());
            command.setManagedInstancesNames(managedVms);
            Answer answer = agentManager.easySend(host.getId(), command);
            if (answer instanceof GetUnmanagedInstancesAnswer) {
                GetUnmanagedInstancesAnswer unmanagedInstancesAnswer = (GetUnmanagedInstancesAnswer) answer;
                HashMap<String, UnmanagedInstance> unmanagedInstances = new HashMap<>();
                unmanagedInstances.putAll(unmanagedInstancesAnswer.getUnmanagedInstances());
                Set<String> keys = unmanagedInstances.keySet();
                for (String key : keys) {
                    responses.add(createUnmanagedInstanceResponse(unmanagedInstances.get(key), cluster, host));
                }
            }
        }
        ListResponse<UnmanagedInstanceResponse> listResponses = new ListResponse<>();
        listResponses.setResponses(responses, responses.size());
        return listResponses;
    }

    @Override
    public UserVmResponse importUnmanagedInstance(ImportUnmanageInstanceCmd cmd) {
        final Account caller = CallContext.current().getCallingAccount();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new PermissionDeniedException(String.format("Cannot perform this operation, Calling account is not root admin: %s", caller.getUuid()));
        }
        final Long clusterId = cmd.getClusterId();
        if (clusterId == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID cannot be null!"));
        }
        final Cluster cluster = clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException(String.format("Cluster ID: %d cannot be found!", clusterId));
        }
        if (cluster.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new InvalidParameterValueException(String.format("VM ingestion is currently not supported for hypervisor: %s", cluster.getHypervisorType().toString()));
        }
        final DataCenter zone = dataCenterDao.findById(cluster.getDataCenterId());
        final String instanceName = cmd.getName();
        if (Strings.isNullOrEmpty(instanceName)) {
            throw new InvalidParameterValueException(String.format("Instance name cannot be empty!"));
        }
        final Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());

        Long userId = null;
        List<UserVO> userVOs = userDao.listByAccount(owner.getAccountId());
        if (!userVOs.isEmpty()) {
            userId = userVOs.get(0).getId();
        }
        final Long templateId = cmd.getTemplateId();
        if (templateId == null) {
            throw new InvalidParameterValueException(String.format("Template ID cannot be null!"));
        }
        final VirtualMachineTemplate template = templateDao.findById(templateId);
        if (template == null) {
            throw new InvalidParameterValueException(String.format("Template ID: %d cannot be found!", templateId));
        }
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        if (serviceOfferingId == null) {
            throw new InvalidParameterValueException(String.format("Service offering ID cannot be null!"));
        }
        final ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException(String.format("Service offering ID: %d cannot be found!", serviceOfferingId));
        }
        final Long diskOfferingId = cmd.getDiskOfferingId();
        if (diskOfferingId == null) {
            throw new InvalidParameterValueException(String.format("Service offering ID cannot be null!"));
        }
        final DiskOffering diskOffering = diskOfferingDao.findById(diskOfferingId);
        if (diskOffering == null) {
            throw new InvalidParameterValueException(String.format("Disk offering ID: %d cannot be found!", diskOfferingId));
        }
        String displayName = cmd.getDisplayName();
        if (Strings.isNullOrEmpty(displayName)) {
            displayName = instanceName;
        }
        String hostName = cmd.getHostName();
        if (Strings.isNullOrEmpty(hostName)) {
            if (!NetUtils.verifyDomainNameLabel(instanceName, true)) {
                throw new InvalidParameterValueException(String.format("Please provide hostname for the VM. VM name contains unsupported characters for it to be used as hostname"));
            }
            hostName = instanceName;
        }
        if (!NetUtils.verifyDomainNameLabel(hostName, true)) {
            throw new InvalidParameterValueException("Invalid VM hostname. VM hostname can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                    + "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
        }

        final Map<String, Long> nicNetworkMap = cmd.getNicNetworkList();
        final Map<String, String> nicIpAddressMap = cmd.getNicIpAddressList();
        final Map<String, Long> dataDiskOfferingMap = cmd.getDataDiskToDiskOfferingList();

        List<HostVO> hosts = resourceManager.listHostsInClusterByStatus(clusterId, Status.Up);

        UserVm userVm = null;

        List<String> templatesFilterList = new ArrayList<>();

        if (cluster.getHypervisorType() == Hypervisor.HypervisorType.VMware) { // Add filter for templates for VMware
            List<VMTemplateStoragePoolVO> templates = templatePoolDao.listAll();
            for (VMTemplateStoragePoolVO templateStoragePoolVO : templates) {
                templatesFilterList.add(templateStoragePoolVO.getInstallPath());
            }
        }

        for (HostVO host : hosts) {
            List<String> managedVms = new ArrayList<>();
            managedVms.addAll(templatesFilterList);
            managedVms.addAll(getHostManagedVms(host));
            GetUnmanagedInstancesCommand command = new GetUnmanagedInstancesCommand(instanceName);
            command.setManagedInstancesNames(managedVms);
            Answer answer = agentManager.easySend(host.getId(), command);
            if (answer instanceof GetUnmanagedInstancesAnswer) {
                GetUnmanagedInstancesAnswer unmanagedInstancesAnswer = (GetUnmanagedInstancesAnswer) answer;
                HashMap<String, UnmanagedInstance> unmanagedInstances = unmanagedInstancesAnswer.getUnmanagedInstances();
                if (unmanagedInstances != null && !unmanagedInstances.isEmpty()) {
                    Set<String> names = unmanagedInstances.keySet();
                    for (String name : names) {
                        if (name.equals(instanceName)) {
                            UnmanagedInstance unmanagedInstance = unmanagedInstances.get(name);
                            if (unmanagedInstance.getDisks() == null || unmanagedInstance.getDisks().isEmpty()) {
                                throw new InvalidParameterValueException(String.format("No attached disks found for the unmanaged VM: %s", name));
                            }
                            final UnmanagedInstance.Disk rootDisk = unmanagedInstance.getDisks().get(0);
                            final long rootDiskSize = diskOffering.isCustomized() ? (rootDisk.getCapacity() / (1024 * 1024)) : diskOffering.getDiskSize();
                            VirtualMachine.PowerState powerState = VirtualMachine.PowerState.PowerOff;
                            if (unmanagedInstance.getPowerState().equalsIgnoreCase("PowerOn") ||
                                    unmanagedInstance.getPowerState().equalsIgnoreCase("POWERED_ON")) {
                                powerState = VirtualMachine.PowerState.PowerOn;
                            }
                            try {
                                userVm = userVmService.importVM(zone, host, template, instanceName, displayName, owner,
                                        null, caller, true, null, owner.getAccountId(), userId,
                                        serviceOffering, diskOffering, null, hostName,
                                        cluster.getHypervisorType(), cmd.getDetails(), powerState);
                            } catch (InsufficientCapacityException ice) {
                                throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ice.getMessage());
                            }
                            if (userVm != null) {
                                try {
                                    importDisk(rootDisk, userVm, diskOffering, Volume.Type.ROOT, String.format("ROOT-%d", userVm.getId()), rootDiskSize, template, owner, null);
                                    Set<String> disks = dataDiskOfferingMap.keySet();
                                    for (String diskId : disks) {
                                        for (UnmanagedInstance.Disk unmanagedDisk : unmanagedInstance.getDisks()) {
                                            if (unmanagedDisk.getDiskId().equals(diskId)) {
                                                DiskOffering offering = diskOfferingDao.findById(dataDiskOfferingMap.get(diskId));
                                                importDisk(unmanagedDisk, userVm, diskOffering, Volume.Type.DATADISK, String.format("DATA-%d-%s", userVm.getId(), unmanagedDisk.getDiskId()), offering.isCustomized() ? (unmanagedDisk.getCapacity() / (1024 * 1024)) : offering.getDiskSize(), template, owner, null);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import volumes while ingesting vm: %s. %s", instanceName, e.getMessage()));
                                }

                                try {
                                    Set<String> nics = nicNetworkMap.keySet();
                                    int i = 0;
                                    for (String nicId : nics) {
                                        Network network = networkDao.findById(nicNetworkMap.get(nicId));
                                        for (UnmanagedInstance.Nic unmanagedNic : unmanagedInstance.getNics()) {
                                            if (unmanagedNic.getNicId().equals(nicId) &&
                                                    network.getBroadcastUri().toString().equals(String.format("vlan://%d", unmanagedNic.getVlan()))) {
                                                String ipAddress = nicIpAddressMap.get(nicId);
                                                if (Strings.isNullOrEmpty(ipAddress)) {
                                                    ipAddress = unmanagedNic.getIpAddress();
                                                }
                                                importNic(unmanagedNic, userVm, network, ipAddress, i == 0);
                                            }
                                        }
                                        i++;
                                    }
                                } catch (Exception e) {
                                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import NICs while ingesting vm: %s. %s", instanceName, e.getMessage()));
                                }
                            } else {
                                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to import vm name: %s", instanceName));
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (userVm == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to find unmanaged vm with name: %s", instanceName));
        }
        UserVmResponse response = responseGenerator.createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVm).get(0);
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListUnmanagedInstancesCmd.class);
        cmdList.add(ImportUnmanageInstanceCmd.class);
        return cmdList;
    }
}
