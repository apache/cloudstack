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

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.MigrateVmToPoolCommand;
import com.cloud.agent.api.UnregisterNicCommand;
import com.cloud.agent.api.UnregisterVMCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.CreateVolumeOVACommand;
import com.cloud.agent.api.storage.PrepareOVAPackingCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Resource;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.hypervisor.vmware.VmwareDatacenterVO;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMapVO;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.NetworkMO;
import com.cloud.hypervisor.vmware.mo.VirtualDiskManagerMO;
import com.cloud.hypervisor.vmware.mo.VirtualMachineDiskInfoBuilder;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.resource.VmwareContextFactory;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.secstorage.CommandExecLogVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.Pair;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualE1000;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineConfigSummary;
import com.vmware.vim25.VirtualMachineRuntimeInfo;

public class VMwareGuru extends HypervisorGuruBase implements HypervisorGuru, Configurable {
    private static final Logger s_logger = Logger.getLogger(VMwareGuru.class);
    private static final Gson GSON = GsonHelper.getGson();


    @Inject VmwareVmImplementer vmwareVmImplementer;
    @Inject GuestOSDao _guestOsDao;
    @Inject HostDetailsDao _hostDetailsDao;
    @Inject ClusterDetailsDao _clusterDetailsDao;
    @Inject CommandExecLogDao _cmdExecLogDao;
    @Inject VmwareManager _vmwareMgr;
    @Inject VMInstanceDao vmDao;
    @Inject SecondaryStorageVmManager _secStorageMgr;
    @Inject PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;
    @Inject VirtualMachineManager vmManager;
    @Inject ClusterManager _clusterMgr;
    @Inject VolumeDao _volumeDao;
    @Inject ResourceLimitService _resourceLimitService;
    @Inject PrimaryDataStoreDao _storagePoolDao;
    @Inject VolumeDataFactory _volFactory;
    @Inject VmwareDatacenterDao vmwareDatacenterDao;
    @Inject VmwareDatacenterZoneMapDao vmwareDatacenterZoneMapDao;
    @Inject VMTemplatePoolDao templateStoragePoolDao;
    @Inject VMTemplateDao vmTemplateDao;
    @Inject UserVmDao userVmDao;
    @Inject DiskOfferingDao diskOfferingDao;
    @Inject PhysicalNetworkDao physicalNetworkDao;
    @Inject StoragePoolHostDao storagePoolHostDao;

    protected VMwareGuru() {
        super();
    }

    public static final ConfigKey<Boolean> VmwareReserveCpu = new ConfigKey<Boolean>(Boolean.class, "vmware.reserve.cpu", "Advanced", "false",
            "Specify whether or not to reserve CPU when deploying an instance.", true, ConfigKey.Scope.Cluster, null);

    public static final ConfigKey<Boolean> VmwareReserveMemory = new ConfigKey<Boolean>(Boolean.class, "vmware.reserve.mem", "Advanced", "false",
            "Specify whether or not to reserve memory when deploying an instance.", true, ConfigKey.Scope.Cluster, null);

    public static final ConfigKey<Boolean> VmwareEnableNestedVirtualization = new ConfigKey<Boolean>(Boolean.class, "vmware.nested.virtualization", "Advanced", "false",
            "When set to true this will enable nested virtualization when this is supported by the hypervisor", true, ConfigKey.Scope.Global, null);

    public static final ConfigKey<Boolean> VmwareEnableNestedVirtualizationPerVM = new ConfigKey<Boolean>(Boolean.class, "vmware.nested.virtualization.perVM", "Advanced", "false",
            "When set to true this will enable nested virtualization per vm", true, ConfigKey.Scope.Global, null);

    @Override public HypervisorType getHypervisorType() {
        return HypervisorType.VMware;
    }

    @Override public VirtualMachineTO implement(VirtualMachineProfile vm) {
        vmwareVmImplementer.setGlobalNestedVirtualisationEnabled(VmwareEnableNestedVirtualization.value());
        vmwareVmImplementer.setGlobalNestedVPerVMEnabled(VmwareEnableNestedVirtualizationPerVM.value());
        return vmwareVmImplementer.implement(vm, toVirtualMachineTO(vm), vmManager.findClusterAndHostIdForVm(vm.getId()).first());
    }

    @Override @DB public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("Finding delegation for command of type %s to host %d.", cmd.getClass(), hostId));
        }

        boolean needDelegation = false;
        if (cmd instanceof StorageSubSystemCommand) {
            StorageSubSystemCommand c = (StorageSubSystemCommand)cmd;
            c.setExecuteInSequence(StorageManager.shouldExecuteInSequenceOnVmware());
        }
        if (cmd instanceof DownloadCommand) {
            cmd.setContextParam(VmwareManager.s_vmwareOVAPackageTimeout.key(), String.valueOf(VmwareManager.s_vmwareOVAPackageTimeout.value()));
        }
        //NOTE: the hostid can be a hypervisor host, or a ssvm agent. For copycommand, if it's for volume upload, the hypervisor
        //type is empty, so we need to check the format of volume at first.
        if (cmd instanceof CopyCommand) {
            CopyCommand cpyCommand = (CopyCommand)cmd;
            DataTO srcData = cpyCommand.getSrcTO();
            DataStoreTO srcStoreTO = srcData.getDataStore();
            DataTO destData = cpyCommand.getDestTO();
            DataStoreTO destStoreTO = destData.getDataStore();

            boolean inSeq = true;
            if (parallelExecutionAllowed(srcData, destData, srcStoreTO, destStoreTO)) {
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

            if (destData.getObjectType() == DataObjectType.VOLUME && destStoreTO.getRole() == DataStoreRole.Primary && srcData.getObjectType() == DataObjectType.TEMPLATE
                    && srcStoreTO.getRole() == DataStoreRole.Primary) {
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

        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("Command of type %s is going to be executed in sequence? %b", cmd.getClass(), cmd.executeInSequence()));
            s_logger.trace(String.format("Command of type %s is going to need delegation? %b", cmd.getClass(), needDelegation));
        }

        if (!needDelegation) {
            return new Pair<Boolean, Long>(Boolean.FALSE, new Long(hostId));
        }
        HostVO host = hostDao.findById(hostId);
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

            if (cmd instanceof BackupSnapshotCommand || cmd instanceof CreatePrivateTemplateFromVolumeCommand || cmd instanceof CreatePrivateTemplateFromSnapshotCommand || cmd instanceof CopyVolumeCommand || cmd instanceof CopyCommand || cmd instanceof CreateVolumeOVACommand || cmd instanceof PrepareOVAPackingCommand
                    || cmd instanceof CreateVolumeFromSnapshotCommand) {
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

    private boolean parallelExecutionAllowed(DataTO srcData, DataTO destData, DataStoreTO srcStoreTO, DataStoreTO destStoreTO) {
        Long srcId = srcStoreTO == null || srcStoreTO.getUuid() == null ? null : _storagePoolDao.findByUuid(srcStoreTO.getUuid()).getId();
        Long dstId = destStoreTO == null || destStoreTO.getUuid() == null ? null : _storagePoolDao.findByUuid(destStoreTO.getUuid()).getId();
        return (srcData.getObjectType() == DataObjectType.SNAPSHOT)
                || (destData.getObjectType() == DataObjectType.SNAPSHOT)
                || (destStoreTO != null && destStoreTO.getRole() == DataStoreRole.Image)
                || (destStoreTO != null && destStoreTO.getRole() == DataStoreRole.ImageCache)
                || !StorageManager.shouldExecuteInSequenceOnVmware(srcId, dstId);
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

    @Override public List<Command> finalizeExpungeNics(VirtualMachine vm, List<NicProfile> nics) {
        List<Command> commands = new ArrayList<Command>();
        List<NicVO> nicVOs = nicDao.listByVmId(vm.getId());
        for (NicVO nic : nicVOs) {
            NetworkVO network = networkDao.findById(nic.getNetworkId());
            if (network.getBroadcastDomainType() == BroadcastDomainType.Lswitch) {
                s_logger.debug("Nic " + nic.toString() + " is connected to an lswitch, cleanup required");
                NetworkVO networkVO = networkDao.findById(nic.getNetworkId());
                // We need the traffic label to figure out which vSwitch has the
                // portgroup
                PhysicalNetworkTrafficTypeVO trafficTypeVO = _physicalNetworkTrafficTypeDao.findBy(networkVO.getPhysicalNetworkId(), networkVO.getTrafficType());
                UnregisterNicCommand unregisterNicCommand = new UnregisterNicCommand(vm.getInstanceName(), trafficTypeVO.getVmwareNetworkLabel(), UUID.fromString(nic.getUuid()));
                commands.add(unregisterNicCommand);
            }
        }
        return commands;
    }

    @Override public String getConfigComponentName() {
        return VMwareGuru.class.getSimpleName();
    }

    @Override public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {VmwareReserveCpu, VmwareReserveMemory, VmwareEnableNestedVirtualization, VmwareEnableNestedVirtualizationPerVM};
    }

    @Override public List<Command> finalizeExpungeVolumes(VirtualMachine vm) {
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

    @Override public Map<String, String> getClusterSettings(long vmId) {
        Map<String, String> details = new HashMap<String, String>();
        Long clusterId = vmManager.findClusterAndHostIdForVm(vmId).first();
        if (clusterId != null) {
            details.put(VmwareReserveCpu.key(), VmwareReserveCpu.valueIn(clusterId).toString());
            details.put(VmwareReserveMemory.key(), VmwareReserveMemory.valueIn(clusterId).toString());
        }
        return details;
    }

    /**
     * Get vmware datacenter mapped to the zoneId
     */
    private VmwareDatacenterVO getVmwareDatacenter(long zoneId) {
        VmwareDatacenterZoneMapVO zoneMap = vmwareDatacenterZoneMapDao.findByZoneId(zoneId);
        long vmwareDcId = zoneMap.getVmwareDcId();
        return vmwareDatacenterDao.findById(vmwareDcId);
    }

    /**
     * Get Vmware datacenter MO
     */
    private DatacenterMO getDatacenterMO(long zoneId) throws Exception {
        VmwareDatacenterVO vmwareDatacenter = getVmwareDatacenter(zoneId);
        VmwareContext context = VmwareContextFactory.getContext(vmwareDatacenter.getVcenterHost(), vmwareDatacenter.getUser(), vmwareDatacenter.getPassword());
        DatacenterMO dcMo = new DatacenterMO(context, vmwareDatacenter.getVmwareDatacenterName());
        ManagedObjectReference dcMor = dcMo.getMor();
        if (dcMor == null) {
            String msg = "Error while getting Vmware datacenter " + vmwareDatacenter.getVmwareDatacenterName();
            s_logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }
        return dcMo;
    }

    /**
     * Get guest OS ID for VM being imported.
     * If it cannot be found it is mapped to: "Other (64-bit)" ID
     */
    private Long getImportingVMGuestOs(VirtualMachineConfigSummary configSummary) {
        String guestFullName = configSummary.getGuestFullName();
        GuestOSVO os = _guestOsDao.listByDisplayName(guestFullName);
        return os != null ? os.getId() : _guestOsDao.listByDisplayName("Other (64-bit)").getId();
    }

    /**
     * Create and persist service offering
     */
    private ServiceOfferingVO createServiceOfferingForVMImporting(Integer cpus, Integer memory, Integer maxCpuUsage) {
        String name = "Imported-" + cpus + "-" + memory;

        DiskOfferingVO diskOfferingVO = new DiskOfferingVO(name, name, Storage.ProvisioningType.THIN, false, null, false, false, true);
        diskOfferingVO = diskOfferingDao.persistDefaultDiskOffering(diskOfferingVO);

        ServiceOfferingVO vo = new ServiceOfferingVO(name, cpus, memory, maxCpuUsage, null, null, false, name, false, Type.User,
                false);
        vo.setDiskOfferingId(diskOfferingVO.getId());
        return serviceOfferingDao.persist(vo);
    }

    /**
     * Get service offering ID for VM being imported.
     * If it cannot be found it creates one and returns its ID
     */
    private Long getImportingVMServiceOffering(VirtualMachineConfigSummary configSummary, VirtualMachineRuntimeInfo runtimeInfo) {
        Integer numCpu = configSummary.getNumCpu();
        Integer memorySizeMB = configSummary.getMemorySizeMB();
        Integer maxCpuUsage = runtimeInfo.getMaxCpuUsage();
        List<ServiceOfferingVO> offerings = serviceOfferingDao.listPublicByCpuAndMemory(numCpu, memorySizeMB);
        return CollectionUtils.isEmpty(offerings) ? createServiceOfferingForVMImporting(numCpu, memorySizeMB, maxCpuUsage).getId() : offerings.get(0).getId();
    }

    /**
     * Check if disk is ROOT disk
     */
    private boolean isRootDisk(VirtualDisk disk, Map<VirtualDisk, VolumeVO> disksMapping, Backup backup) {
        if (!disksMapping.containsKey(disk)) {
            return false;
        }
        VolumeVO volumeVO = disksMapping.get(disk);
        if (volumeVO == null) {
            final VMInstanceVO vm = virtualMachineDao.findByIdIncludingRemoved(backup.getVmId());
            if (vm == null) {
                throw new CloudRuntimeException("Failed to find the volumes details from the VM backup");
            }
            List<Backup.VolumeInfo> backedUpVolumes = vm.getBackupVolumeList();
            for (Backup.VolumeInfo backedUpVolume : backedUpVolumes) {
                if (backedUpVolume.getSize().equals(disk.getCapacityInBytes())) {
                    return backedUpVolume.getType().equals(Volume.Type.ROOT);
                }
            }
        } else {
            return volumeVO.getVolumeType().equals(Volume.Type.ROOT);
        }
        throw new CloudRuntimeException("Could not determinate ROOT disk for VM to import");
    }

    /**
     * Check backing info
     */
    private void checkBackingInfo(VirtualDeviceBackingInfo backingInfo) {
        if (!(backingInfo instanceof VirtualDiskFlatVer2BackingInfo)) {
            String errorMessage = String.format("Unsupported backing info. Expected: [%s], but received: [%s].", VirtualDiskFlatVer2BackingInfo.class.getSimpleName(), backingInfo.getClass().getSimpleName());
            s_logger.error(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }
    }

    /**
     * Get pool ID from datastore UUID
     */
    private Long getPoolIdFromDatastoreUuid(String datastoreUuid) {
        String poolUuid = UuidUtils.normalize(datastoreUuid);
        StoragePoolVO pool = _storagePoolDao.findByUuid(poolUuid);
        if (pool == null) {
            throw new CloudRuntimeException("Couldn't find storage pool " + poolUuid);
        }
        return pool.getId();
    }

    /**
     * Get pool ID for disk
     */
    private Long getPoolId(VirtualDisk disk) {
        VirtualDeviceBackingInfo backing = disk.getBacking();
        checkBackingInfo(backing);
        VirtualDiskFlatVer2BackingInfo info = (VirtualDiskFlatVer2BackingInfo)backing;
        String[] fileNameParts = info.getFileName().split(" ");
        String datastoreUuid = StringUtils.substringBetween(fileNameParts[0], "[", "]");
        return getPoolIdFromDatastoreUuid(datastoreUuid);
    }

    /**
     * Get volume name from filename
     */
    private String getVolumeNameFromFileName(String fileName) {
        String[] fileNameParts = fileName.split(" ");
        String volumePath = fileNameParts[1];
        return volumePath.split("/")[1].replaceFirst(".vmdk", "");
    }

    /**
     * Get root disk template path
     */
    private String getRootDiskTemplatePath(VirtualDisk rootDisk) {
        VirtualDeviceBackingInfo backing = rootDisk.getBacking();
        checkBackingInfo(backing);
        VirtualDiskFlatVer2BackingInfo info = (VirtualDiskFlatVer2BackingInfo)backing;
        VirtualDiskFlatVer2BackingInfo parent = info.getParent();
        return (parent != null) ? getVolumeNameFromFileName(parent.getFileName()) : getVolumeNameFromFileName(info.getFileName());
    }

    /**
     * Get template MO
     */
    private VirtualMachineMO getTemplate(DatacenterMO dcMo, String templatePath) throws Exception {
        VirtualMachineMO template = dcMo.findVm(templatePath);
        if (!template.isTemplate()) {
            throw new CloudRuntimeException(templatePath + " is not a template");
        }
        return template;
    }

    /**
     * Get template pool ID
     */
    private Long getTemplatePoolId(VirtualMachineMO template) throws Exception {
        VirtualMachineConfigSummary configSummary = template.getConfigSummary();
        String vmPathName = configSummary.getVmPathName();
        String[] pathParts = vmPathName.split(" ");
        String dataStoreUuid = pathParts[0].replace("[", "").replace("]", "");
        return getPoolIdFromDatastoreUuid(dataStoreUuid);
    }

    /**
     * Get template size
     */
    private Long getTemplateSize(VirtualMachineMO template, String vmInternalName, Map<VirtualDisk, VolumeVO> disksMapping, Backup backup) throws Exception {
        List<VirtualDisk> disks = template.getVirtualDisks();
        if (CollectionUtils.isEmpty(disks)) {
            throw new CloudRuntimeException("Couldn't find VM template size");
        }
        return disks.get(0).getCapacityInBytes();
    }

    /**
     * Create a VM Template record on DB
     */
    private VMTemplateVO createVMTemplateRecord(String vmInternalName, long guestOsId, long accountId) {
        Long nextTemplateId = vmTemplateDao.getNextInSequence(Long.class, "id");
        VMTemplateVO templateVO = new VMTemplateVO(nextTemplateId, "Imported-from-" + vmInternalName, Storage.ImageFormat.OVA, false, false, false, Storage.TemplateType.USER, null,
                false, 64, accountId, null, "Template imported from VM " + vmInternalName, false, guestOsId, false, HypervisorType.VMware, null, null, false, false, false, false);
        return vmTemplateDao.persist(templateVO);
    }

    /**
     * Retrieve the template ID matching the template on templatePath. There are 2 cases:
     * - There are no references on DB for primary storage -> create a template DB record and return its ID
     * - There are references on DB for primary storage -> return template ID for any of those references
     */
    private long getTemplateId(String templatePath, String vmInternalName, Long guestOsId, long accountId) {
        List<VMTemplateStoragePoolVO> poolRefs = templateStoragePoolDao.listByTemplatePath(templatePath);
        return CollectionUtils.isNotEmpty(poolRefs) ? poolRefs.get(0).getTemplateId() : createVMTemplateRecord(vmInternalName, guestOsId, accountId).getId();
    }

    /**
     * Update template reference on primary storage, if needed
     */
    private void updateTemplateRef(long templateId, Long poolId, String templatePath, Long templateSize) {
        VMTemplateStoragePoolVO templateRef = templateStoragePoolDao.findByPoolPath(poolId, templatePath);
        if (templateRef == null) {
            templateRef = new VMTemplateStoragePoolVO(poolId, templateId, null, 100, VMTemplateStorageResourceAssoc.Status.DOWNLOADED, templatePath, null, null, templatePath,
                    templateSize, null);
            templateRef.setState(ObjectInDataStoreStateMachine.State.Ready);
            templateStoragePoolDao.persist(templateRef);
        }
    }

    /**
     * Get template ID for VM being imported. If it is not found, it is created
     */
    private Long getImportingVMTemplate(List<VirtualDisk> virtualDisks, DatacenterMO dcMo, String vmInternalName, Long guestOsId, long accountId, Map<VirtualDisk, VolumeVO> disksMapping, Backup backup) throws Exception {
        for (VirtualDisk disk : virtualDisks) {
            if (isRootDisk(disk, disksMapping, backup)) {
                VolumeVO volumeVO = disksMapping.get(disk);
                if (volumeVO == null) {
                    String templatePath = getRootDiskTemplatePath(disk);
                    VirtualMachineMO template = getTemplate(dcMo, templatePath);
                    Long poolId = getTemplatePoolId(template);
                    Long templateSize = getTemplateSize(template, vmInternalName, disksMapping, backup);
                    long templateId = getTemplateId(templatePath, vmInternalName, guestOsId, accountId);
                    updateTemplateRef(templateId, poolId, templatePath, templateSize);
                    return templateId;
                } else {
                    return volumeVO.getTemplateId();
                }
            }
        }
        throw new CloudRuntimeException("Could not find ROOT disk for VM " + vmInternalName);
    }

    /**
     * If VM does not exist: create and persist VM
     * If VM exists: update VM
     */
    private VMInstanceVO getVM(String vmInternalName, long templateId, long guestOsId, long serviceOfferingId, long zoneId, long accountId, long userId, long domainId) {
        s_logger.debug(String.format("Trying to get VM with specs: [vmInternalName: %s, templateId: %s, guestOsId: %s, serviceOfferingId: %s].", vmInternalName,
                templateId, guestOsId, serviceOfferingId));
        VMInstanceVO vm = virtualMachineDao.findVMByInstanceNameIncludingRemoved(vmInternalName);
        if (vm != null) {
            s_logger.debug(String.format("Found an existing VM [id: %s, removed: %s] with internalName: [%s].", vm.getUuid(), vm.getRemoved() != null ? "yes" : "no", vmInternalName));
            vm.setState(VirtualMachine.State.Stopped);
            vm.setPowerState(VirtualMachine.PowerState.PowerOff);
            virtualMachineDao.update(vm.getId(), vm);
            if (vm.getRemoved() != null) {
                virtualMachineDao.unremove(vm.getId());
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, accountId, vm.getDataCenterId(), vm.getId(), vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(),
                        vm.getHypervisorType().toString(), VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplayVm());
            }
            return virtualMachineDao.findById(vm.getId());
        } else {
            long id = userVmDao.getNextInSequence(Long.class, "id");
            s_logger.debug(String.format("Can't find an existing VM with internalName: [%s]. Creating a new VM with: [id: %s, name: %s, templateId: %s, guestOsId: %s, serviceOfferingId: %s].",
                    vmInternalName, id, vmInternalName, templateId, guestOsId, serviceOfferingId));

            UserVmVO vmInstanceVO = new UserVmVO(id, vmInternalName, vmInternalName, templateId, HypervisorType.VMware, guestOsId, false, false, domainId, accountId, userId,
                    serviceOfferingId, null, null, null, vmInternalName);
            vmInstanceVO.setDataCenterId(zoneId);
            return userVmDao.persist(vmInstanceVO);
        }
    }

    /**
     * Create and persist volume
     */
    private VolumeVO createVolumeRecord(Volume.Type type, String volumeName, long zoneId, long domainId, long accountId, long diskOfferingId, Storage.ProvisioningType provisioningType,
            Long size, long instanceId, Long poolId, long templateId, Integer unitNumber, VirtualMachineDiskInfo diskInfo) {
        VolumeVO volumeVO = new VolumeVO(type, volumeName, zoneId, domainId, accountId, diskOfferingId, provisioningType, size, null, null, null);
        volumeVO.setFormat(Storage.ImageFormat.OVA);
        volumeVO.setPath(volumeName);
        volumeVO.setState(Volume.State.Ready);
        volumeVO.setInstanceId(instanceId);
        volumeVO.setPoolId(poolId);
        volumeVO.setTemplateId(templateId);
        volumeVO.setAttached(new Date());
        volumeVO.setRemoved(null);
        volumeVO.setChainInfo(GSON.toJson(diskInfo));
        if (unitNumber != null) {
            volumeVO.setDeviceId(unitNumber.longValue());
        }
        return _volumeDao.persist(volumeVO);
    }

    /**
     * Get volume name from VM disk
     */
    private String getVolumeName(VirtualDisk disk, VirtualMachineMO vmToImport) throws Exception {
        return vmToImport.getVmdkFileBaseName(disk);
    }

    /**
     * Get provisioning type for VM disk info
     */
    private Storage.ProvisioningType getProvisioningType(VirtualDiskFlatVer2BackingInfo backing) {
        Boolean thinProvisioned = backing.isThinProvisioned();
        if (BooleanUtils.isTrue(thinProvisioned)) {
            return Storage.ProvisioningType.THIN;
        }
        return Storage.ProvisioningType.SPARSE;
    }

    /**
     * Get disk offering ID for volume being imported. If it is not found it is mapped to "Custom" ID
     */
    private long getDiskOfferingId(long size, Storage.ProvisioningType provisioningType) {
        List<DiskOfferingVO> offerings = diskOfferingDao.listAllBySizeAndProvisioningType(size, provisioningType);
        return CollectionUtils.isNotEmpty(offerings) ? offerings.get(0).getId() : diskOfferingDao.findByUniqueName("Cloud.Com-Custom").getId();
    }

    protected VolumeVO updateVolume(VirtualDisk disk, Map<VirtualDisk, VolumeVO> disksMapping, VirtualMachineMO vmToImport, Long poolId, VirtualMachine vm) throws Exception {
        VolumeVO volume = disksMapping.get(disk);
        String volumeName = getVolumeName(disk, vmToImport);
        volume.setPath(volumeName);
        volume.setPoolId(poolId);
        VirtualMachineDiskInfo diskInfo = getDiskInfo(vmToImport, poolId, volumeName);
        volume.setChainInfo(GSON.toJson(diskInfo));
        volume.setInstanceId(vm.getId());
        volume.setState(Volume.State.Ready);
        volume.setAttached(new Date());
        _volumeDao.update(volume.getId(), volume);
        if (volume.getRemoved() != null) {
            s_logger.debug(String.format("Marking volume [uuid: %s] of restored VM [%s] as non removed.", volume.getUuid(),
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm, "uuid", "instanceName")));
            _volumeDao.unremove(volume.getId());
            if (vm.getType() == Type.User) {
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), volume.getDiskOfferingId(), null, volume.getSize(),
                        Volume.class.getName(), volume.getUuid(), volume.isDisplayVolume());
                _resourceLimitService.incrementResourceCount(vm.getAccountId(), Resource.ResourceType.volume, volume.isDisplayVolume());
                _resourceLimitService.incrementResourceCount(vm.getAccountId(), Resource.ResourceType.primary_storage, volume.isDisplayVolume(), volume.getSize());
            }
        }
        return volume;
    }

    /**
     * Get volumes for VM being imported
     */
    private void syncVMVolumes(VMInstanceVO vmInstanceVO, List<VirtualDisk> virtualDisks, Map<VirtualDisk, VolumeVO> disksMapping, VirtualMachineMO vmToImport, Backup backup)
            throws Exception {
        long zoneId = vmInstanceVO.getDataCenterId();
        long accountId = vmInstanceVO.getAccountId();
        long domainId = vmInstanceVO.getDomainId();
        long templateId = vmInstanceVO.getTemplateId();
        long instanceId = vmInstanceVO.getId();

        String operation = "";
        for (VirtualDisk disk : virtualDisks) {
            Long poolId = getPoolId(disk);
            Volume volume = null;
            if (disksMapping.containsKey(disk) && disksMapping.get(disk) != null) {
                volume = updateVolume(disk, disksMapping, vmToImport, poolId, vmInstanceVO);
                operation = "updated";
            } else {
                volume = createVolume(disk, vmToImport, domainId, zoneId, accountId, instanceId, poolId, templateId, backup, true);
                operation = "created";
            }
            s_logger.debug(String.format("VM [id: %s, instanceName: %s] backup restore operation %s volume [id: %s].", instanceId, vmInstanceVO.getInstanceName(),
                    operation, volume.getUuid()));
        }
    }

    private VirtualMachineDiskInfo getDiskInfo(VirtualMachineMO vmMo, Long poolId, String volumeName) throws Exception {
        VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();
        String poolName = _storagePoolDao.findById(poolId).getUuid().replace("-", "");
        return diskInfoBuilder.getDiskInfoByBackingFileBaseName(volumeName, poolName);
    }

    private VolumeVO createVolume(VirtualDisk disk, VirtualMachineMO vmToImport, long domainId, long zoneId, long accountId, long instanceId, Long poolId, long templateId, Backup backup, boolean isImport) throws Exception {
        VMInstanceVO vm = virtualMachineDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("Failed to find the backup volume information from the VM backup");
        }
        List<Backup.VolumeInfo> backedUpVolumes = vm.getBackupVolumeList();
        Volume.Type type = Volume.Type.DATADISK;
        Long size = disk.getCapacityInBytes();
        if (isImport) {
            for (Backup.VolumeInfo volumeInfo : backedUpVolumes) {
                if (volumeInfo.getSize().equals(disk.getCapacityInBytes())) {
                    type = volumeInfo.getType();
                }
            }
        }
        VirtualDeviceBackingInfo backing = disk.getBacking();
        checkBackingInfo(backing);
        VirtualDiskFlatVer2BackingInfo info = (VirtualDiskFlatVer2BackingInfo)backing;
        String volumeName = getVolumeName(disk, vmToImport);
        Storage.ProvisioningType provisioningType = getProvisioningType(info);
        long diskOfferingId = getDiskOfferingId(size, provisioningType);
        Integer unitNumber = disk.getUnitNumber();
        VirtualMachineDiskInfo diskInfo = getDiskInfo(vmToImport, poolId, volumeName);
        return createVolumeRecord(type, volumeName, zoneId, domainId, accountId, diskOfferingId, provisioningType, size, instanceId, poolId, templateId, unitNumber, diskInfo);
    }

    protected String createVolumeInfoFromVolumes(List<VolumeVO> vmVolumes) {
        try {
            List<Backup.VolumeInfo> list = new ArrayList<>();
            for (VolumeVO vol : vmVolumes) {
                list.add(new Backup.VolumeInfo(vol.getUuid(), vol.getPath(), vol.getVolumeType(), vol.getSize()));
            }
            return GSON.toJson(list.toArray(), Backup.VolumeInfo[].class);
        } catch (Exception e) {
            if (CollectionUtils.isEmpty(vmVolumes) || vmVolumes.get(0).getInstanceId() == null) {
                s_logger.error(String.format("Failed to create VolumeInfo of VM [id: null] volumes due to: [%s].", e.getMessage()), e);
            } else {
                s_logger.error(String.format("Failed to create VolumeInfo of VM [id: %s] volumes due to: [%s].", vmVolumes.get(0).getInstanceId(), e.getMessage()), e);
            }
            throw e;
        }
    }

    /**
     * Get physical network ID from zoneId and Vmware label
     */
    private long getPhysicalNetworkId(Long zoneId, String tag) {
        List<PhysicalNetworkVO> physicalNetworks = physicalNetworkDao.listByZone(zoneId);
        for (PhysicalNetworkVO physicalNetwork : physicalNetworks) {
            PhysicalNetworkTrafficTypeVO vo = _physicalNetworkTrafficTypeDao.findBy(physicalNetwork.getId(), TrafficType.Guest);
            if (vo == null) {
                continue;
            }
            String vmwareNetworkLabel = vo.getVmwareNetworkLabel();
            if (!vmwareNetworkLabel.startsWith(tag)) {
                throw new CloudRuntimeException("Vmware network label does not start with: " + tag);
            }
            return physicalNetwork.getId();
        }
        throw new CloudRuntimeException("Could not find guest physical network matching tag: " + tag + " on zone " + zoneId);
    }

    /**
     * Create and persist network
     */
    private NetworkVO createNetworkRecord(Long zoneId, String tag, String vlan, long accountId, long domainId) {
        Long physicalNetworkId = getPhysicalNetworkId(zoneId, tag);
        final long id = networkDao.getNextInSequence(Long.class, "id");
        NetworkVO networkVO = new NetworkVO(id, TrafficType.Guest, Networks.Mode.Dhcp, BroadcastDomainType.Vlan, 9L, domainId, accountId, id, "Imported-network-" + id,
                "Imported-network-" + id, null, Network.GuestType.Isolated, zoneId, physicalNetworkId, ControlledEntity.ACLType.Account, false, null, false);
        networkVO.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlan));
        networkVO.setGuruName("ExternalGuestNetworkGuru");
        networkVO.setState(Network.State.Implemented);
        return networkDao.persist(networkVO);
    }

    /**
     * Get network from VM network name
     */
    private NetworkVO getGuestNetworkFromNetworkMorName(String name, long accountId, Long zoneId, long domainId) {
        String prefix = "cloud.guest.";
        String nameWithoutPrefix = name.replace(prefix, "");
        String[] parts = nameWithoutPrefix.split("\\.");
        String vlan = parts[0];
        String tag = parts[parts.length - 1];
        String[] tagSplit = tag.split("-");
        tag = tagSplit[tagSplit.length - 1];
        NetworkVO networkVO = networkDao.findByVlan(vlan);
        if (networkVO == null) {
            networkVO = createNetworkRecord(zoneId, tag, vlan, accountId, domainId);
        }
        return networkVO;
    }

    /**
     * Get map between VM networks and its IDs on CloudStack
     */
    private Map<String, NetworkVO> getNetworksMapping(String[] vmNetworkNames, long accountId, long zoneId, long domainId) {
        Map<String, NetworkVO> mapping = new HashMap<>();
        for (String networkName : vmNetworkNames) {
            NetworkVO networkVO = getGuestNetworkFromNetworkMorName(networkName, accountId, zoneId, domainId);
            mapping.put(networkName, networkVO);
        }
        return mapping;
    }

    /**
     * Get network MO from VM NIC
     */
    private NetworkMO getNetworkMO(VirtualE1000 nic, VmwareContext context) {
        VirtualDeviceConnectInfo connectable = nic.getConnectable();
        VirtualEthernetCardNetworkBackingInfo info = (VirtualEthernetCardNetworkBackingInfo)nic.getBacking();
        ManagedObjectReference networkMor = info.getNetwork();
        if (networkMor == null) {
            throw new CloudRuntimeException("Could not find network for NIC on: " + nic.getMacAddress());
        }
        return new NetworkMO(context, networkMor);
    }

    private Pair<String, String> getNicMacAddressAndNetworkName(VirtualDevice nicDevice, VmwareContext context) throws Exception {
        VirtualE1000 nic = (VirtualE1000)nicDevice;
        String macAddress = nic.getMacAddress();
        NetworkMO networkMO = getNetworkMO(nic, context);
        String networkName = networkMO.getName();
        return new Pair<>(macAddress, networkName);
    }

    private void syncVMNics(VirtualDevice[] nicDevices, DatacenterMO dcMo, Map<String, NetworkVO> networksMapping, VMInstanceVO vm) throws Exception {
        VmwareContext context = dcMo.getContext();
        List<NicVO> allNics = nicDao.listByVmId(vm.getId());
        for (VirtualDevice nicDevice : nicDevices) {
            Pair<String, String> pair = getNicMacAddressAndNetworkName(nicDevice, context);
            String macAddress = pair.first();
            String networkName = pair.second();
            NetworkVO networkVO = networksMapping.get(networkName);
            NicVO nicVO = nicDao.findByNetworkIdAndMacAddress(networkVO.getId(), macAddress);
            if (nicVO != null) {
                allNics.remove(nicVO);
            }
        }
        for (final NicVO unMappedNic : allNics) {
            vmManager.removeNicFromVm(vm, unMappedNic);
        }
    }

    private Map<VirtualDisk, VolumeVO> getDisksMapping(Backup backup, List<VirtualDisk> virtualDisks) {
        final VMInstanceVO vm = virtualMachineDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException("Failed to find the volumes details from the VM backup");
        }

        List<Backup.VolumeInfo> backedUpVolumes = vm.getBackupVolumeList();
        Map<String, Boolean> usedVols = new HashMap<>();
        Map<VirtualDisk, VolumeVO> map = new HashMap<>();

        for (Backup.VolumeInfo backedUpVol : backedUpVolumes) {
            VolumeVO volumeExtra = _volumeDao.findByUuid(backedUpVol.getUuid());
            if (volumeExtra != null) {
                s_logger.debug(String.format("Marking volume [id: %s] of VM [%s] as removed for the backup process.", backedUpVol.getUuid(), ReflectionToStringBuilderUtils.reflectOnlySelectedFields(vm, "uuid", "instanceName")));
                _volumeDao.remove(volumeExtra.getId());

                if (vm.getType() == Type.User) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DETACH, volumeExtra.getAccountId(), volumeExtra.getDataCenterId(), volumeExtra.getId(),
                            volumeExtra.getName(), volumeExtra.getDiskOfferingId(), null, volumeExtra.getSize(), Volume.class.getName(),
                            volumeExtra.getUuid(), volumeExtra.isDisplayVolume());
                    _resourceLimitService.decrementResourceCount(vm.getAccountId(), Resource.ResourceType.volume, volumeExtra.isDisplayVolume());
                    _resourceLimitService.decrementResourceCount(vm.getAccountId(), Resource.ResourceType.primary_storage, volumeExtra.isDisplayVolume(), volumeExtra.getSize());
                }
            }
            for (VirtualDisk disk : virtualDisks) {
                if (!map.containsKey(disk) && backedUpVol.getSize().equals(disk.getCapacityInBytes()) && !usedVols.containsKey(backedUpVol.getUuid())) {
                    String volId = backedUpVol.getUuid();
                    VolumeVO vol = _volumeDao.findByUuidIncludingRemoved(volId);
                    usedVols.put(backedUpVol.getUuid(), true);
                    map.put(disk, vol);
                    s_logger.debug("VM restore mapping for disk " + disk.getBacking() + " (capacity: " + toHumanReadableSize(disk.getCapacityInBytes()) + ") with volume ID" + vol.getId());
                }
            }
        }
        return map;
    }

    /**
     * Find VM on datacenter
     */
    private VirtualMachineMO findVM(DatacenterMO dcMo, String path) throws Exception {
        VirtualMachineMO vm = dcMo.findVm(path);
        if (vm == null) {
            throw new CloudRuntimeException("Error finding VM: " + path);
        }
        return vm;
    }

    /**
     * Find restored volume based on volume info
     */
    private VirtualDisk findRestoredVolume(Backup.VolumeInfo volumeInfo, VirtualMachineMO vm) throws Exception {
        List<VirtualDisk> virtualDisks = vm.getVirtualDisks();
        for (VirtualDisk disk : virtualDisks) {
            if (disk.getCapacityInBytes().equals(volumeInfo.getSize())) {
                return disk;
            }
        }
        throw new CloudRuntimeException("Volume to restore could not be found");
    }

    /**
     * Get volume full path
     */
    private String getVolumeFullPath(VirtualDisk disk) {
        VirtualDeviceBackingInfo backing = disk.getBacking();
        checkBackingInfo(backing);
        VirtualDiskFlatVer2BackingInfo info = (VirtualDiskFlatVer2BackingInfo)backing;
        return info.getFileName();
    }

    /**
     * Get dest volume full path
     */
    private String getDestVolumeFullPath(VirtualDisk restoredDisk, VirtualMachineMO restoredVm, VirtualMachineMO vmMo) throws Exception {
        VirtualDisk vmDisk = vmMo.getVirtualDisks().get(0);
        String vmDiskPath = vmMo.getVmdkFileBaseName(vmDisk);
        String vmDiskFullPath = getVolumeFullPath(vmMo.getVirtualDisks().get(0));
        String restoredVolumePath = restoredVm.getVmdkFileBaseName(restoredDisk);
        return vmDiskFullPath.replace(vmDiskPath, restoredVolumePath);
    }

    /**
     * Get dest datastore mor
     */
    private ManagedObjectReference getDestStoreMor(VirtualMachineMO vmMo) throws Exception {
        VirtualDisk vmDisk = vmMo.getVirtualDisks().get(0);
        VirtualDeviceBackingInfo backing = vmDisk.getBacking();
        checkBackingInfo(backing);
        VirtualDiskFlatVer2BackingInfo info = (VirtualDiskFlatVer2BackingInfo)backing;
        return info.getDatastore();
    }

    @Override
    public VirtualMachine importVirtualMachineFromBackup(long zoneId, long domainId, long accountId, long userId, String vmInternalName, Backup backup) throws Exception {
        s_logger.debug(String.format("Trying to import VM [vmInternalName: %s] from Backup [%s].", vmInternalName,
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backup, "id", "uuid", "vmId", "externalId", "backupType")));
        DatacenterMO dcMo = getDatacenterMO(zoneId);
        VirtualMachineMO vmToImport = dcMo.findVm(vmInternalName);
        if (vmToImport == null) {
            throw new CloudRuntimeException("Error finding VM: " + vmInternalName);
        }
        VirtualMachineConfigSummary configSummary = vmToImport.getConfigSummary();
        VirtualMachineRuntimeInfo runtimeInfo = vmToImport.getRuntimeInfo();
        List<VirtualDisk> virtualDisks = vmToImport.getVirtualDisks();
        String[] vmNetworkNames = vmToImport.getNetworks();
        VirtualDevice[] nicDevices = vmToImport.getNicDevices();

        Map<VirtualDisk, VolumeVO> disksMapping = getDisksMapping(backup, virtualDisks);
        Map<String, NetworkVO> networksMapping = getNetworksMapping(vmNetworkNames, accountId, zoneId, domainId);

        long guestOsId = getImportingVMGuestOs(configSummary);
        long serviceOfferingId = getImportingVMServiceOffering(configSummary, runtimeInfo);
        long templateId = getImportingVMTemplate(virtualDisks, dcMo, vmInternalName, guestOsId, accountId, disksMapping, backup);

        VMInstanceVO vm = getVM(vmInternalName, templateId, guestOsId, serviceOfferingId, zoneId, accountId, userId, domainId);
        syncVMVolumes(vm, virtualDisks, disksMapping, vmToImport, backup);
        syncVMNics(nicDevices, dcMo, networksMapping, vm);

        return vm;
    }

    @Override
    public boolean attachRestoredVolumeToVirtualMachine(long zoneId, String location, Backup.VolumeInfo volumeInfo, VirtualMachine vm, long poolId, Backup backup)
            throws Exception {
        DatacenterMO dcMo = getDatacenterMO(zoneId);
        VirtualMachineMO vmRestored = findVM(dcMo, location);
        VirtualMachineMO vmMo = findVM(dcMo, vm.getInstanceName());
        VirtualDisk restoredDisk = findRestoredVolume(volumeInfo, vmRestored);
        String diskPath = vmRestored.getVmdkFileBaseName(restoredDisk);

        s_logger.debug("Restored disk size=" + toHumanReadableSize(restoredDisk.getCapacityInKB()) + " path=" + diskPath);

        // Detach restored VM disks
        vmRestored.detachAllDisks();

        String srcPath = getVolumeFullPath(restoredDisk);
        String destPath = getDestVolumeFullPath(restoredDisk, vmRestored, vmMo);

        VirtualDiskManagerMO virtualDiskManagerMO = new VirtualDiskManagerMO(dcMo.getContext());

        // Copy volume to the VM folder
        virtualDiskManagerMO.moveVirtualDisk(srcPath, dcMo.getMor(), destPath, dcMo.getMor(), true);

        try {
            // Attach volume to VM
            vmMo.attachDisk(new String[] {destPath}, getDestStoreMor(vmMo));
        } catch (Exception e) {
            s_logger.error("Failed to attach the restored volume: " + diskPath, e);
            return false;
        } finally {
            // Destroy restored VM
            vmRestored.destroy();
        }

        VirtualDisk attachedDisk = getAttachedDisk(vmMo, diskPath);
        if (attachedDisk == null) {
            s_logger.error("Failed to get the attached the (restored) volume " + diskPath);
            return false;
        }
        createVolume(attachedDisk, vmMo, vm.getDomainId(), vm.getDataCenterId(), vm.getAccountId(), vm.getId(), poolId, vm.getTemplateId(), backup, false);

        if (vm.getBackupOfferingId() == null) {
            return true;
        }
        VMInstanceVO vmVO = (VMInstanceVO)vm;
        vmVO.setBackupVolumes(createVolumeInfoFromVolumes(_volumeDao.findByInstance(vm.getId())));
        vmDao.update(vmVO.getId(), vmVO);
        return true;
    }

    private VirtualDisk getAttachedDisk(VirtualMachineMO vmMo, String diskPath) throws Exception {
        for (VirtualDisk disk : vmMo.getVirtualDisks()) {
            if (vmMo.getVmdkFileBaseName(disk).equals(diskPath)) {
                return disk;
            }
        }
        return null;
    }

    private boolean isInterClusterMigration(Long srcClusterId, Long destClusterId) {
        return srcClusterId != null && destClusterId != null && ! srcClusterId.equals(destClusterId);
    }

    private String getHostGuidForLocalStorage(StoragePool pool) {
        List<StoragePoolHostVO> storagePoolHostVOs = storagePoolHostDao.listByPoolId(pool.getId());
        StoragePoolHostVO storagePoolHostVO = storagePoolHostVOs.get(0);
        HostVO hostVO = hostDao.findById(storagePoolHostVO.getHostId());
        return hostVO.getGuid();
    }

    private String getHostGuidInTargetCluster(boolean isInterClusterMigration, Long destClusterId) {
        String hostGuidInTargetCluster = null;
        if (isInterClusterMigration) {
            Host hostInTargetCluster = null;
            // Without host vMotion might fail between non-shared storages with error similar to,
            // https://kb.vmware.com/s/article/1003795
            // As this is offline migration VM won't be started on this host
            List<HostVO> hosts = hostDao.findHypervisorHostInCluster(destClusterId);
            if (CollectionUtils.isNotEmpty(hosts)) {
                hostInTargetCluster = hosts.get(0);
            }
            if (hostInTargetCluster == null) {
                throw new CloudRuntimeException("Migration failed, unable to find suitable target host for VM placement while migrating between storage pools of different clusters without shared storages");
            }
            hostGuidInTargetCluster = hostInTargetCluster.getGuid();
        }
        return hostGuidInTargetCluster;
    }

    @Override
    public List<Command> finalizeMigrate(VirtualMachine vm, Map<Volume, StoragePool> volumeToPool) {
        List<Command> commands = new ArrayList<>();

        // OfflineVmwareMigration: specialised migration command
        List<Pair<VolumeTO, StorageFilerTO>> volumeToFilerTo = new ArrayList<>();
        Long poolClusterId = null;
        StoragePool targetLocalPoolForVM = null;
        for (Map.Entry<Volume, StoragePool> entry : volumeToPool.entrySet()) {
            Volume volume = entry.getKey();
            StoragePool pool = entry.getValue();
            VolumeTO volumeTo = new VolumeTO(volume, _storagePoolDao.findById(pool.getId()));
            StorageFilerTO filerTo = new StorageFilerTO(pool);
            if (pool.getClusterId() != null) {
                poolClusterId = pool.getClusterId();
            }
            if (volume.getVolumeType().equals(Volume.Type.ROOT) && pool.isLocal()) {
                targetLocalPoolForVM = pool;
            }
            volumeToFilerTo.add(new Pair<>(volumeTo, filerTo));
        }
        final Long destClusterId = poolClusterId;
        final Long srcClusterId = vmManager.findClusterAndHostIdForVm(vm, true).first();
        final boolean isInterClusterMigration = isInterClusterMigration(destClusterId, srcClusterId);
        String targetHostGuid = getTargetHostGuid(targetLocalPoolForVM, destClusterId, isInterClusterMigration);

        MigrateVmToPoolCommand migrateVmToPoolCommand = new MigrateVmToPoolCommand(vm.getInstanceName(),
                volumeToFilerTo, targetHostGuid, true);
        commands.add(migrateVmToPoolCommand);

        // OfflineVmwareMigration: cleanup if needed
        if (isInterClusterMigration) {
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

    private String getTargetHostGuid(StoragePool targetLocalPoolForVM, Long destClusterId, boolean isInterClusterMigration) {
        String targetHostGuid = null;
        if (targetLocalPoolForVM != null) {
            // Get the target host for local storage migration
            targetHostGuid = getHostGuidForLocalStorage(targetLocalPoolForVM);
        } else {
            targetHostGuid = getHostGuidInTargetCluster(isInterClusterMigration, destClusterId);
        }
        return targetHostGuid;
    }

    @Override
    protected VirtualMachineTO toVirtualMachineTO(VirtualMachineProfile vmProfile) {
        return super.toVirtualMachineTO(vmProfile);
    }
}
