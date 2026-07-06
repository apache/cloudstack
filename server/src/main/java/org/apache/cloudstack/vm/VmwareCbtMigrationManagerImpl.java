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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.CancelVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.CheckVmwareCbtMigrationPrerequisitesCmd;
import org.apache.cloudstack.api.command.admin.vm.CutoverVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.DeleteVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVmwareCbtMigrationsCmd;
import org.apache.cloudstack.api.command.admin.vm.StartVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.SyncVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationPreflightDiskResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationPreflightFindingResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationPreflightResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationCycleResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationDiskResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckConvertInstanceAnswer;
import com.cloud.agent.api.CheckConvertInstanceCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.VmwareCbtCleanupCommand;
import com.cloud.agent.api.VmwareCbtCutoverCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtPrepareCommand;
import com.cloud.agent.api.VmwareCbtRbdProbeCommand;
import com.cloud.agent.api.VmwareCbtSyncCommand;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.agent.api.to.VmwareCbtDiskSyncResultTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VmwareDatacenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VmwareDatacenterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.network.Network;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.vm.VmDetailConstants;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmwareCbtMigrationCycleVO;
import com.cloud.vm.VmwareCbtMigrationDiskVO;
import com.cloud.vm.VmwareCbtMigrationVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VmwareCbtMigrationCycleDao;
import com.cloud.vm.dao.VmwareCbtMigrationDao;
import com.cloud.vm.dao.VmwareCbtMigrationDiskDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class VmwareCbtMigrationManagerImpl implements VmwareCbtMigrationManager, Configurable {

    private static final String OBJECT_NAME = "vmwarecbtmigration";
    private static final String DETAIL_VDDK_TRANSPORTS = "vddk.transports";
    private static final String DETAIL_VDDK_THUMBPRINT = "vddk.thumbprint";
    private static final String REDACTED_SECRET = "******";
    private static final String DEFAULT_CBT_DISK_BASE_PATH = "/var/lib/libvirt/images/cloudstack-cbt";
    private static final String KVM_STORAGE_POOL_MOUNT_BASE_PATH = "/mnt";
    static final String CBT_FINALIZED_DISK_CONTROLLER = "virtio";
    private static final String RBD_PROBE_IMAGE_PREFIX = "cloudstack-cbt-probe-";
    private static final List<Storage.StoragePoolType> CBT_COMPATIBLE_STORAGE_POOL_TYPES = Arrays.asList(
            Storage.StoragePoolType.NetworkFilesystem,
            Storage.StoragePoolType.Filesystem,
            Storage.StoragePoolType.SharedMountPoint,
            Storage.StoragePoolType.RBD);
    private static final int CLEANUP_WAIT_SECONDS = 300;
    private static final int DELETE_CLEANUP_WAIT_SECONDS = 30;
    private static final Logger LOGGER = LogManager.getLogger(VmwareCbtMigrationManagerImpl.class);
    private static final Gson GSON = new Gson();

    static final ConfigKey<Integer> VmwareCbtMigrationMinCycles = new ConfigKey<>(Integer.class,
            "vmware.cbt.migration.min.cycles",
            "Advanced",
            "1",
            "Minimum number of CBT delta synchronization cycles to run before CloudStack can recommend final VMware to KVM cutover",
            true,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Integer> VmwareCbtMigrationMaxCycles = new ConfigKey<>(Integer.class,
            "vmware.cbt.migration.max.cycles",
            "Advanced",
            "5",
            "Maximum number of CBT delta synchronization cycles to run before CloudStack recommends final VMware to KVM cutover",
            true,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Integer> VmwareCbtMigrationQuietCycles = new ConfigKey<>(Integer.class,
            "vmware.cbt.migration.quiet.cycles",
            "Advanced",
            "2",
            "Number of consecutive quiet CBT delta synchronization cycles required before CloudStack recommends final VMware to KVM cutover",
            true,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Long> VmwareCbtMigrationQuietBytes = new ConfigKey<>(Long.class,
            "vmware.cbt.migration.quiet.bytes",
            "Advanced",
            "1073741824",
            "Maximum changed bytes in a CBT delta synchronization cycle for the cycle to be considered quiet",
            true,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Long> VmwareCbtMigrationQuietDirtyRate = new ConfigKey<>(Long.class,
            "vmware.cbt.migration.quiet.dirty.rate",
            "Advanced",
            "16777216",
            "Maximum changed bytes per second in a CBT delta synchronization cycle for the cycle to be considered quiet",
            true,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Boolean> VmwareCbtAllowNonInPlaceFinalization = new ConfigKey<>(Boolean.class,
            "vmware.cbt.allow.non.inplace.finalization",
            "Advanced",
            "false",
            "If true, VMware CBT cutover may fall back to regular virt-v2v finalization for qcow2 file targets when true in-place finalization is unavailable. The fallback stages temporary data on the selected primary storage and requires additional free space.",
            true,
            ConfigKey.Scope.Global,
            null);

    static final ConfigKey<Integer> VmwareCbtMigrationAgentCommandTimeout = new ConfigKey<>(Integer.class,
            "vmware.cbt.migration.agent.command.timeout",
            "Advanced",
            "86400",
            "Timeout in seconds for long-running VMware CBT data-plane commands dispatched to the KVM agent, including initial full sync, delta sync, final delta sync, and cutover finalization.",
            true,
            ConfigKey.Scope.Global,
            null);

    @Inject
    private VmwareCbtMigrationDao vmwareCbtMigrationDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private VmwareDatacenterDao vmwareDatacenterDao;
    @Inject
    private AccountService accountService;
    @Inject
    private UserDao userDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private VmwareCbtMigrationDiskDao vmwareCbtMigrationDiskDao;
    @Inject
    private VmwareCbtMigrationCycleDao vmwareCbtMigrationCycleDao;
    @Inject
    private AgentManager agentManager;
    @Inject
    private UnmanagedVMsManager unmanagedVMsManager;
    @Autowired(required = false)
    private VmwareCbtMigrationService vmwareCbtMigrationService;

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CheckVmwareCbtMigrationPrerequisitesCmd.class);
        cmdList.add(StartVmwareCbtMigrationCmd.class);
        cmdList.add(ListVmwareCbtMigrationsCmd.class);
        cmdList.add(SyncVmwareCbtMigrationCmd.class);
        cmdList.add(CutoverVmwareCbtMigrationCmd.class);
        cmdList.add(CancelVmwareCbtMigrationCmd.class);
        cmdList.add(DeleteVmwareCbtMigrationCmd.class);
        return cmdList;
    }

    @Override
    public VmwareCbtMigrationPreflightResponse checkVmwareCbtMigrationPrerequisites(CheckVmwareCbtMigrationPrerequisitesCmd cmd) {
        PreflightFindingCollector findings = new PreflightFindingCollector();
        VmwareCbtMigrationPreflightResponse response = new VmwareCbtMigrationPreflightResponse();
        response.setObjectName("vmwarecbtmigrationpreflight");

        DataCenterVO zone = getPreflightZone(cmd.getZoneId(), findings);
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }

        ClusterVO destinationCluster = getPreflightDestinationCluster(cmd.getClusterId(), zone, findings);
        if (destinationCluster != null) {
            response.setClusterId(destinationCluster.getUuid());
            response.setClusterName(destinationCluster.getName());
        }

        StoragePoolVO storagePool = getPreflightStoragePool(cmd.getStoragePoolId(), zone, destinationCluster, findings);
        VmwareCbtStorageTarget storageTarget = null;
        if (storagePool != null) {
            storageTarget = VmwareCbtStorageTarget.forPool(storagePool);
            populateStorageTargetResponse(response, storageTarget);
            addStorageTargetFinding(storageTarget, findings);
        }

        HostVO cbtHost = getPreflightCbtHost(cmd.getConvertInstanceHostId(), destinationCluster, storageTarget, findings);
        if (cbtHost != null) {
            response.setConvertInstanceHostId(cbtHost.getUuid());
            response.setConvertInstanceHostName(cbtHost.getName());
            response.setConvertInstanceHostInPlaceFinalizationSupported(hostSupportsInPlaceFinalization(cbtHost));
        }
        addStorageTargetFinalizationFinding(storageTarget, cbtHost, findings);
        addRbdStorageAccessFinding(storageTarget, cbtHost, findings);

        String sourceVmName = StringUtils.trimToNull(cmd.getSourceVmName());
        if (sourceVmName == null) {
            findings.fail("sourceVm.name.present", "vmware", null, "Source VM name is required.");
        } else {
            response.setSourceVmName(sourceVmName);
        }

        VmwareSource source = getPreflightVmwareSource(cmd, findings);
        if (source != null) {
            response.setVcenter(source.vcenter);
            response.setDatacenterName(source.datacenterName);
            response.setSourceHost(source.sourceHost);
        }

        if (source != null && sourceVmName != null) {
            populateSourceVmPreflight(response, source, sourceVmName, cbtHost, cmd.getServiceOfferingId(),
                    cmd.getDetails(), zone, findings);
        }

        response.setFindings(findings.getFindings());
        response.setReady(!findings.hasFailures());
        return response;
    }

    private VmwareCbtPreflightInfo validateSourceVmPreflightForStart(VmwareSource source, String sourceVmName) {
        VmwareCbtPreflightInfo preflightInfo;
        try {
            preflightInfo = getVmwareCbtMigrationService().getPreflightInfo(source.vcenter, source.datacenterName,
                    source.username, source.password, source.sourceHost, sourceVmName);
        } catch (RuntimeException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Unable to validate VMware CBT prerequisites for source VM %s: %s",
                            sourceVmName, sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source)));
        }

        if (Boolean.FALSE.equals(preflightInfo.getChangeTrackingSupported())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Source VM %s does not report VMware CBT support", sourceVmName));
        }
        if (Boolean.TRUE.equals(preflightInfo.getConsolidationNeeded())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Source VM %s reports pending disk consolidation; consolidate VMware disks before starting CBT migration",
                            sourceVmName));
        }
        if (CollectionUtils.isEmpty(preflightInfo.getDisks())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("No source VMware disks were discovered for VM %s", sourceVmName));
        }
        for (VmwareCbtPreflightDiskInfo disk : preflightInfo.getDisks()) {
            validateSourceDiskPreflightForStart(disk);
        }
        return preflightInfo;
    }

    private void ensureSourceVmChangeTrackingEnabledForStart(VmwareSource source, String sourceVmName,
                                                             VmwareCbtPreflightInfo preflightInfo) {
        if (Boolean.TRUE.equals(preflightInfo.getChangeTrackingEnabled())) {
            return;
        }
        try {
            getVmwareCbtMigrationService().ensureChangeTrackingEnabled(source.vcenter, source.datacenterName,
                    source.username, source.password, source.sourceHost, sourceVmName);
        } catch (RuntimeException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Unable to enable VMware CBT on source VM %s before initial full sync: %s",
                            sourceVmName, sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source)));
        }
    }

    private void validateSourceDiskPreflightForStart(VmwareCbtPreflightDiskInfo disk) {
        String diskId = StringUtils.defaultIfBlank(disk.getSourceDiskId(), disk.getSourceDiskPath());
        if (disk.getSourceDiskDeviceKey() == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Source disk %s does not expose a VMware device key required for CBT delta queries",
                            diskId));
        }
        if (StringUtils.isBlank(disk.getSourceDiskPath())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Source disk %s does not expose a VMware backing path", diskId));
        }
        if (disk.isIndependentDisk()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Source disk %s uses independent disk mode %s, which is not supported for VMware CBT migration",
                            diskId, disk.getDiskMode()));
        }
        if (disk.isPhysicalRdm()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Source disk %s is a physical-mode RDM, which is not supported for VMware CBT migration",
                            diskId));
        }
    }

    private ServiceOfferingVO getServiceOfferingForVmwareCbtMigration(Long serviceOfferingId, Account owner, DataCenterVO zone) {
        if (serviceOfferingId == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Service offering ID cannot be null");
        }
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Service offering ID: %d cannot be found", serviceOfferingId));
        }
        accountService.checkAccess(owner, serviceOffering, zone);
        serviceOfferingDao.loadDetails(serviceOffering);
        return serviceOffering;
    }

    static VmwareCbtOfferingResources resolveRequestedOfferingResources(ServiceOfferingVO serviceOffering, Map<String, String> details) {
        if (serviceOffering == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Service offering cannot be null");
        }
        Map<String, String> callerDetails = details == null ? new HashMap<>() : details;
        Map<String, String> offeringDetails = serviceOffering.getDetails();
        Integer cpu = firstInteger(VmDetailConstants.CPU_NUMBER, callerDetails,
                serviceOffering.getCpu(), ApiConstants.MIN_CPU_NUMBER, offeringDetails);
        Integer cpuSpeed = firstInteger(VmDetailConstants.CPU_SPEED, callerDetails,
                serviceOffering.getSpeed(), null, offeringDetails);
        Integer memory = firstInteger(VmDetailConstants.MEMORY, callerDetails,
                serviceOffering.getRamSize(), ApiConstants.MIN_MEMORY, offeringDetails);
        return new VmwareCbtOfferingResources(cpu, cpuSpeed, memory);
    }

    private static Integer firstInteger(String callerDetailKey, Map<String, String> callerDetails,
                                        Integer offeringValue, String offeringDetailKey,
                                        Map<String, String> offeringDetails) {
        if (offeringValue != null) {
            return offeringValue;
        }
        Integer callerValue = parseIntegerDetail(callerDetails, callerDetailKey);
        if (callerValue != null) {
            return callerValue;
        }
        return StringUtils.isBlank(offeringDetailKey) ? null : parseIntegerDetail(offeringDetails, offeringDetailKey);
    }

    private static Integer parseIntegerDetail(Map<String, String> details, String key) {
        if (MapUtils.isEmpty(details) || StringUtils.isBlank(key) || StringUtils.isBlank(details.get(key))) {
            return null;
        }
        try {
            return Integer.valueOf(details.get(key));
        } catch (NumberFormatException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Please provide a valid integer value for detail '%s'.", key));
        }
    }

    static void validateSelectedServiceOfferingResourcesForSourceVm(VmwareCbtPreflightInfo preflightInfo,
                                                                    ServiceOfferingVO serviceOffering,
                                                                    Map<String, String> details) {
        VmwareCbtOfferingResources requestedResources = resolveRequestedOfferingResources(serviceOffering, details);
        validateRequestedResourceAtLeastSource(preflightInfo.getCpuCores(), requestedResources.cpuNumber, "CPU number");
        validateRequestedResourceAtLeastSource(preflightInfo.getCpuSpeed(), requestedResources.cpuSpeed, "CPU speed");
        validateRequestedResourceAtLeastSource(preflightInfo.getMemoryMb(), requestedResources.memoryMb, "Memory");
    }

    private static void validateRequestedResourceAtLeastSource(Integer sourceResource, Integer requestedResource, String resourceName) {
        if (sourceResource == null || requestedResource == null || sourceResource <= 0 || requestedResource <= 0) {
            return;
        }
        if (requestedResource < sourceResource) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("The requested %s (%d) is less than the source VM %s (%d)",
                            resourceName, requestedResource, resourceName, sourceResource));
        }
    }

    @Override
    public VmwareCbtMigrationResponse startVmwareCbtMigration(StartVmwareCbtMigrationCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        if (caller == null) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Unable to determine calling account");
        }
        Account owner = accountService.getActiveAccountById(cmd.getEntityOwnerId());
        if (owner == null) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Unable to determine target account");
        }

        DataCenterVO zone = getZone(cmd.getZoneId());
        ClusterVO destinationCluster = getDestinationCluster(cmd.getClusterId(), zone.getId());
        StoragePoolVO storagePool = getStoragePool(cmd.getStoragePoolId(), zone, destinationCluster);
        VmwareCbtStorageTarget storageTarget = VmwareCbtStorageTarget.forPool(storagePool);
        if (!storageTarget.isSupported()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, storageTarget.getSupportMessage());
        }
        HostVO convertHost = selectCbtHost(cmd.getConvertInstanceHostId(), destinationCluster, storageTarget);
        validateStorageTargetFinalizationSupport(storageTarget, convertHost);
        validateRbdStorageAccessForStart(storageTarget, convertHost);

        String sourceVmName = StringUtils.trimToNull(cmd.getSourceVmName());
        if (sourceVmName == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Source VM name is required");
        }

        VmwareSource source = resolveVmwareSource(cmd);
        VmwareCbtPreflightInfo preflightInfo = validateSourceVmPreflightForStart(source, sourceVmName);
        ServiceOfferingVO serviceOffering = getServiceOfferingForVmwareCbtMigration(cmd.getServiceOfferingId(), owner, zone);
        validateSelectedServiceOfferingResourcesForSourceVm(preflightInfo, serviceOffering, cmd.getDetails());
        validateWindowsGuestConversionSupportForStart(convertHost, sourceVmName, preflightInfo, cmd.getDetails());
        ensureSourceVmChangeTrackingEnabledForStart(source, sourceVmName, preflightInfo);
        List<VmwareCbtDiskInfo> sourceDisks = discoverSourceDisks(source, sourceVmName);
        String displayName = StringUtils.defaultIfBlank(cmd.getDisplayName(), sourceVmName);

        VmwareCbtMigrationVO migration = new VmwareCbtMigrationVO(zone.getId(), owner.getId(), getUserIdForOwner(owner),
                destinationCluster.getId(), displayName, source.vcenter, source.datacenterName, cmd.getSourceHost(), cmd.getSourceCluster(), sourceVmName);
        migration.setExistingVcenterId(source.existingVcenterId);
        storeExternalVmwareSourceCredentials(migration, source);
        if (convertHost != null) {
            migration.setConvertHostId(convertHost.getId());
        }
        if (storagePool != null) {
            migration.setStoragePoolId(storagePool.getId());
        }
        migration.setHostName(StringUtils.trimToNull(cmd.getHostName()));
        migration.setTemplateId(cmd.getTemplateId());
        migration.setServiceOfferingId(cmd.getServiceOfferingId());
        migration.setGuestOsId(cmd.getGuestOsId());
        migration.setDataDiskOfferingMap(serializeMap(cmd.getDataDiskToDiskOfferingList()));
        migration.setNicNetworkMap(serializeMap(cmd.getNicNetworkList()));
        migration.setNicIpAddressMap(serializeNicIpAddressMap(cmd.getNicIpAddressList()));
        migration.setImportDetails(serializeMap(cmd.getDetails()));
        migration.setForced(cmd.isForced());
        applyVddkDetails(migration, cmd.getDetails());
        migration.setState(VmwareCbtMigration.State.InitialSync);
        migration.setCurrentStep(String.format("Discovered %s source disk(s); preparing initial VDDK full sync", sourceDisks.size()));
        migration.setUpdated(new Date());
        migration = vmwareCbtMigrationDao.persist(migration);
        persistSourceDisks(migration, sourceDisks);
        return runInitialFullSync(source, migration, storageTarget);
    }

    @Override
    public ListResponse<VmwareCbtMigrationResponse> listVmwareCbtMigrations(ListVmwareCbtMigrationsCmd cmd) {
        VmwareCbtMigration.State state = parseState(cmd.getState());
        Pair<List<VmwareCbtMigrationVO>, Integer> result = vmwareCbtMigrationDao.listMigrations(cmd.getId(), cmd.getZoneId(), cmd.getAccountId(),
                cmd.getVcenter(), cmd.getSourceVmName(), state, cmd.getStartIndex(), cmd.getPageSizeVal());

        List<VmwareCbtMigrationResponse> responses = new ArrayList<>();
        for (VmwareCbtMigrationVO migration : result.first()) {
            responses.add(createVmwareCbtMigrationResponse(migration));
        }
        ListResponse<VmwareCbtMigrationResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, result.second());
        return listResponse;
    }

    @Override
    public VmwareCbtMigrationResponse syncVmwareCbtMigration(SyncVmwareCbtMigrationCmd cmd) {
        VmwareCbtMigrationVO migration = getMigration(cmd.getId());
        rejectTerminalMigration(migration, "synchronize");
        requireMigrationState(migration, "synchronize", VmwareCbtMigration.State.Replicating,
                VmwareCbtMigration.State.ReadyForCutover);
        VmwareSource source = resolveVmwareSource(migration, cmd.getUsername(), cmd.getPassword());
        validateInitialSyncTargetDisks(migration);
        HostVO cbtHost = getCbtHostForMigration(migration);
        int cycleNumber = migration.getCompletedCycles() + 1;
        VmwareCbtMigrationCycleVO cycle = new VmwareCbtMigrationCycleVO(migration.getId(), cycleNumber);
        cycle.setState(VmwareCbtMigrationCycle.State.CopyingChangedBlocks);
        cycle.setDescription("Dispatching CBT delta synchronization to KVM agent");
        cycle.setUpdated(new Date());
        cycle = vmwareCbtMigrationCycleDao.persist(cycle);

        migration.setState(VmwareCbtMigration.State.Replicating);
        migration.setCurrentStep(String.format("Running CBT delta synchronization cycle %s", cycleNumber));
        migration.setLastError(null);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);

        VmwareCbtSnapshotInfo snapshot = null;
        try {
            snapshot = createDeltaSnapshot(source, migration, cycleNumber);
            cycle.setState(VmwareCbtMigrationCycle.State.QueryingChangedAreas);
            cycle.setSnapshotMor(snapshot.getSnapshotMor());
            cycle.setDescription("Querying VMware CBT changed disk areas");
            cycle.setUpdated(new Date());
            vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);

            VmwareCbtChangedBlockQueryResult changedBlockQuery = queryChangedBlocks(source, migration,
                    snapshot.getSnapshotMor());

            cycle.setState(VmwareCbtMigrationCycle.State.CopyingChangedBlocks);
            cycle.setDescription(String.format("Dispatching %s VMware CBT changed block range(s) to KVM agent",
                    changedBlockQuery.changedBlocks.size()));
            cycle.setUpdated(new Date());
            vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);

            VmwareCbtSyncCommand syncCommand = new VmwareCbtSyncCommand(migration.getUuid(),
                    createRemoteInstance(source, migration, snapshot.getSourceVmMor()), getDiskTransferObjects(migration),
                    changedBlockQuery.changedBlocks, cycleNumber, snapshot.getSnapshotMor(), false);
            applyVddkDetails(syncCommand, migration);
            applyTargetStorageDetails(syncCommand, migration);
            syncCommand.setWait(getVmwareCbtMigrationAgentCommandTimeout());

            VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, syncCommand, "synchronize",
                    migration.getUuid());
            if (!answer.getResult()) {
                String details = sanitizeSensitiveMessage(answer.getDetails(), source);
                markCycleFailed(cycle, details);
                markMigrationFailed(migration, "CBT delta synchronization failed", details);
                return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
            }

            applyDiskResults(migration, answer.getDiskResults());
            updateDiskChangeIds(migration, changedBlockQuery.changedDisks);
            long changedBytes = answer.getChangedBytes();
            long durationSeconds = answer.getDurationSeconds();
            long dirtyRate = getDirtyRateBytesPerSecond(changedBytes, durationSeconds, answer.getDirtyRateBytesPerSecond());
            VmwareCbtMigrationCutoverPolicy cutoverPolicy = getCutoverPolicy();
            VmwareCbtMigrationCutoverPolicy.Decision cutoverDecision = cutoverPolicy.decide(cycleNumber,
                    migration.getQuietCycles(), changedBytes, durationSeconds);
            int quietCycles = cutoverPolicy.isQuietCycle(changedBytes, durationSeconds) ?
                    migration.getQuietCycles() + 1 : 0;

            cycle.setState(VmwareCbtMigrationCycle.State.Completed);
            cycle.setChangedBytes(changedBytes);
            cycle.setDirtyRate(dirtyRate);
            cycle.setDuration(durationSeconds * 1000);
            cycle.setDescription(sanitizeSensitiveMessage(answer.getDetails(), source));
            cycle.setUpdated(new Date());
            vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);

            migration.setCompletedCycles(cycleNumber);
            migration.setQuietCycles(quietCycles);
            migration.setTotalChangedBytes(migration.getTotalChangedBytes() + changedBytes);
            migration.setLastChangedBytes(changedBytes);
            migration.setLastDirtyRate(dirtyRate);
            migration.setState(cutoverDecision == VmwareCbtMigrationCutoverPolicy.Decision.CONTINUE ?
                    VmwareCbtMigration.State.Replicating : VmwareCbtMigration.State.ReadyForCutover);
            migration.setCurrentStep(getCutoverDecisionStep(cutoverDecision));
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);
            return createVmwareCbtMigrationResponse(migration);
        } catch (RuntimeException e) {
            String error = sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source);
            markCycleFailed(cycle, error);
            markMigrationFailed(migration, "CBT delta synchronization failed", error);
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        } finally {
            removeDeltaSnapshotIfPossible(source, migration, snapshot);
        }
    }

    @Override
    public VmwareCbtMigrationResponse cutoverVmwareCbtMigration(CutoverVmwareCbtMigrationCmd cmd) {
        VmwareCbtMigrationVO migration = getMigration(cmd.getId());
        rejectTerminalMigration(migration, "cut over");
        requireMigrationState(migration, "cut over", VmwareCbtMigration.State.ReadyForCutover,
                VmwareCbtMigration.State.ReadyForImport);
        VmwareSource source = resolveVmwareSource(migration, cmd.getUsername(), cmd.getPassword());
        validateInitialSyncTargetDisks(migration);
        if (migration.getState() == VmwareCbtMigration.State.ReadyForImport) {
            return importCutoverMigration(source, migration);
        }
        HostVO cbtHost = getCbtHostForMigration(migration);
        VmwareCbtStorageTarget storageTarget = getStorageTargetForMigration(migration);
        validateStorageTargetFinalizationSupport(storageTarget, cbtHost);
        requireSourceVmPoweredOff(source, migration);

        migration.setState(VmwareCbtMigration.State.CuttingOver);
        migration.setCurrentStep("Running final CBT delta synchronization before cutover");
        migration.setLastError(null);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);

        int finalCycleNumber = migration.getCompletedCycles() + 1;
        if (!runFinalDeltaSync(source, migration, cbtHost, finalCycleNumber)) {
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        }

        migration = vmwareCbtMigrationDao.findById(migration.getId());
        boolean inPlaceFinalization = hostSupportsInPlaceFinalization(cbtHost);
        String finalizationDescription = inPlaceFinalization ? "in-place conversion" : "virt-v2v fallback conversion";
        migration.setCurrentStep(String.format("Final CBT delta synchronization completed; running %s",
                finalizationDescription));
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);

        VmwareCbtCutoverCommand cutoverCommand = new VmwareCbtCutoverCommand(migration.getUuid(), createRemoteInstance(source, migration),
                getDiskTransferObjects(migration), finalCycleNumber, true);
        applyVddkDetails(cutoverCommand, migration);
        applyTargetStorageDetails(cutoverCommand, migration);
        cutoverCommand.setAllowNonInPlaceFinalization(isNonInPlaceFinalizationFallbackAllowed(storageTarget));
        cutoverCommand.setWait(getVmwareCbtMigrationAgentCommandTimeout());

        VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, cutoverCommand, "cut over", migration.getUuid());
        if (!answer.getResult()) {
            markMigrationFailed(migration, "CBT cutover failed", sanitizeSensitiveMessage(answer.getDetails(), source));
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        }

        applyDiskResults(migration, answer.getDiskResults());
        migration = vmwareCbtMigrationDao.findById(migration.getId());
        migration.setState(VmwareCbtMigration.State.ReadyForImport);
        migration.setCurrentStep(String.format("Final %s completed; importing VM into CloudStack",
                finalizationDescription));
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);
        return importCutoverMigration(source, migration);
    }

    private VmwareCbtMigrationResponse importCutoverMigration(VmwareSource source, VmwareCbtMigrationVO migration) {
        if (migration.getServiceOfferingId() == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Cannot import VMware CBT migration because serviceofferingid was not recorded when the migration was started");
        }
        Account caller = CallContext.current().getCallingAccount();
        Account owner = accountService.getActiveAccountById(migration.getAccountId());
        if (caller == null || owner == null) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Unable to resolve caller or target account for VMware CBT import");
        }
        try {
            UserVm userVm = unmanagedVMsManager.importConvertedVmwareCbtInstanceToKvm(source.vcenter, source.datacenterName,
                    source.username, source.password, migration.getSourceCluster(), migration.getSourceHost(),
                    migration.getSourceVmName(), createConvertedInstanceForImport(migration), getZone(migration.getZoneId()),
                    getDestinationCluster(migration.getDestinationClusterId(), migration.getZoneId()), migration.getDisplayName(),
                    migration.getHostName(), caller, owner, migration.getUserId(), migration.getTemplateId(),
                    migration.getServiceOfferingId(), deserializeLongMap(migration.getDataDiskOfferingMap()),
                    deserializeLongMap(migration.getNicNetworkMap()), deserializeNicIpAddressMap(migration.getNicIpAddressMap()),
                    migration.getGuestOsId(), deserializeStringMap(migration.getImportDetails()), migration.isForced(),
                    migration.getStoragePoolId());

            migration = vmwareCbtMigrationDao.findById(migration.getId());
            migration.setVmId(userVm.getId());
            migration.setState(VmwareCbtMigration.State.Completed);
            migration.setCurrentStep("CloudStack VM import completed");
            migration.setLastError(null);
            clearStoredSourceCredentials(migration);
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);
        } catch (RuntimeException e) {
            String error = sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source);
            migration = vmwareCbtMigrationDao.findById(migration.getId());
            migration.setState(VmwareCbtMigration.State.ReadyForImport);
            migration.setCurrentStep("CloudStack VM import failed; retry cutover to import the converted disks");
            migration.setLastError(error);
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);
        }
        return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
    }

    private UnmanagedInstanceTO createConvertedInstanceForImport(VmwareCbtMigrationVO migration) {
        List<VmwareCbtMigrationDiskVO> migrationDisks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        if (CollectionUtils.isEmpty(migrationDisks)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    String.format("No synchronized disks were found for VMware CBT migration %s", migration.getUuid()));
        }
        UnmanagedInstanceTO convertedInstance = new UnmanagedInstanceTO();
        convertedInstance.setName(migration.getSourceVmName());
        convertedInstance.setHypervisorType(Hypervisor.HypervisorType.KVM.name());
        convertedInstance.setPowerState(UnmanagedInstanceTO.PowerState.PowerOff);

        StoragePoolVO storagePool = primaryDataStoreDao.findById(migration.getStoragePoolId());
        Storage.StoragePoolType storagePoolType = storagePool == null ? null : storagePool.getPoolType();
        List<UnmanagedInstanceTO.Disk> disks = new ArrayList<>();
        int position = 0;
        for (VmwareCbtMigrationDiskVO migrationDisk : migrationDisks) {
            if (StringUtils.isBlank(migrationDisk.getTargetPath())) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                        String.format("Target disk path is missing for source disk %s in VMware CBT migration %s",
                                migrationDisk.getSourceDiskId(), migration.getUuid()));
            }
            UnmanagedInstanceTO.Disk disk = new UnmanagedInstanceTO.Disk();
            disk.setDiskId(migrationDisk.getSourceDiskId());
            disk.setLabel(migrationDisk.getSourceDiskPath());
            disk.setCapacity(migrationDisk.getCapacityBytes());
            disk.setPosition(position);
            disk.setControllerUnit(position);
            disk.setController(CBT_FINALIZED_DISK_CONTROLLER);
            disk.setImagePath(migrationDisk.getTargetPath());
            disk.setFileBaseName(getVolumePathForCbtTarget(migrationDisk.getTargetPath(), migration.getUuid(), storagePoolType));
            if (storagePool != null) {
                disk.setDatastoreName(storagePool.getUuid());
                disk.setDatastoreType(storagePool.getPoolType().name());
            }
            disks.add(disk);
            position++;
        }
        convertedInstance.setDisks(disks);
        convertedInstance.setNics(new ArrayList<>());
        return convertedInstance;
    }

    private String getVolumePathForCbtTarget(String targetPath, String migrationUuid, Storage.StoragePoolType storagePoolType) {
        String normalizedPath = StringUtils.defaultString(targetPath).replace('\\', '/');
        if (storagePoolType == Storage.StoragePoolType.RBD) {
            return StringUtils.contains(normalizedPath, "/") ? StringUtils.substringAfterLast(normalizedPath, "/") : normalizedPath;
        }
        String marker = String.format("/cloudstack-cbt/%s/", migrationUuid);
        int markerIndex = normalizedPath.indexOf(marker);
        if (markerIndex >= 0) {
            return String.format("cloudstack-cbt/%s/%s", migrationUuid,
                    normalizedPath.substring(markerIndex + marker.length()));
        }
        return StringUtils.substringAfterLast(normalizedPath, "/");
    }

    private Map<String, Long> deserializeLongMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new HashMap<>();
        }
        try {
            return GSON.fromJson(json, new TypeToken<Map<String, Long>>() {
            }.getType());
        } catch (RuntimeException e) {
            throw new InvalidParameterValueException("Unable to parse stored VMware CBT import ID map");
        }
    }

    private Map<String, String> deserializeStringMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new HashMap<>();
        }
        try {
            return GSON.fromJson(json, new TypeToken<Map<String, String>>() {
            }.getType());
        } catch (RuntimeException e) {
            throw new InvalidParameterValueException("Unable to parse stored VMware CBT import details");
        }
    }

    private Map<String, Network.IpAddresses> deserializeNicIpAddressMap(String json) {
        Map<String, Network.IpAddresses> nicIpAddressMap = new HashMap<>();
        for (Map.Entry<String, String> entry : deserializeStringMap(json).entrySet()) {
            nicIpAddressMap.put(entry.getKey(), new Network.IpAddresses(entry.getValue(), null));
        }
        return nicIpAddressMap;
    }

    private boolean runFinalDeltaSync(VmwareSource source, VmwareCbtMigrationVO migration, HostVO cbtHost,
                                      int cycleNumber) {
        VmwareCbtMigrationCycleVO cycle = new VmwareCbtMigrationCycleVO(migration.getId(), cycleNumber);
        cycle.setState(VmwareCbtMigrationCycle.State.Created);
        cycle.setDescription("Creating final VMware CBT snapshot for cutover");
        cycle.setUpdated(new Date());
        cycle = vmwareCbtMigrationCycleDao.persist(cycle);

        VmwareCbtSnapshotInfo snapshot = null;
        try {
            snapshot = createDeltaSnapshot(source, migration, cycleNumber);
            cycle.setState(VmwareCbtMigrationCycle.State.QueryingChangedAreas);
            cycle.setSnapshotMor(snapshot.getSnapshotMor());
            cycle.setDescription("Querying final VMware CBT changed disk areas");
            cycle.setUpdated(new Date());
            vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);

            VmwareCbtChangedBlockQueryResult changedBlockQuery = queryChangedBlocks(source, migration,
                    snapshot.getSnapshotMor());

            cycle.setState(VmwareCbtMigrationCycle.State.CopyingChangedBlocks);
            cycle.setDescription(String.format("Dispatching %s final VMware CBT changed block range(s) to KVM agent",
                    changedBlockQuery.changedBlocks.size()));
            cycle.setUpdated(new Date());
            vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);

            VmwareCbtSyncCommand syncCommand = new VmwareCbtSyncCommand(migration.getUuid(),
                    createRemoteInstance(source, migration, snapshot.getSourceVmMor()), getDiskTransferObjects(migration),
                    changedBlockQuery.changedBlocks, cycleNumber, snapshot.getSnapshotMor(), true);
            applyVddkDetails(syncCommand, migration);
            applyTargetStorageDetails(syncCommand, migration);
            syncCommand.setWait(getVmwareCbtMigrationAgentCommandTimeout());

            VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, syncCommand, "perform final synchronization",
                    migration.getUuid());
            if (!answer.getResult()) {
                String details = sanitizeSensitiveMessage(answer.getDetails(), source);
                markCycleFailed(cycle, details);
                markMigrationFailed(migration, "Final CBT delta synchronization failed", details);
                return false;
            }

            applyDiskResults(migration, answer.getDiskResults());
            updateDiskChangeIds(migration, changedBlockQuery.changedDisks);
            long changedBytes = answer.getChangedBytes();
            long durationSeconds = answer.getDurationSeconds();
            long dirtyRate = getDirtyRateBytesPerSecond(changedBytes, durationSeconds, answer.getDirtyRateBytesPerSecond());

            cycle.setState(VmwareCbtMigrationCycle.State.Completed);
            cycle.setChangedBytes(changedBytes);
            cycle.setDirtyRate(dirtyRate);
            cycle.setDuration(durationSeconds * 1000);
            cycle.setDescription(sanitizeSensitiveMessage(answer.getDetails(), source));
            cycle.setUpdated(new Date());
            vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);

            migration.setCompletedCycles(cycleNumber);
            migration.setQuietCycles(0);
            migration.setTotalChangedBytes(migration.getTotalChangedBytes() + changedBytes);
            migration.setLastChangedBytes(changedBytes);
            migration.setLastDirtyRate(dirtyRate);
            migration.setCurrentStep("Final CBT delta synchronization completed");
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);
            return true;
        } catch (RuntimeException e) {
            String error = sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source);
            markCycleFailed(cycle, error);
            markMigrationFailed(migration, "Final CBT delta synchronization failed", error);
            return false;
        } finally {
            removeDeltaSnapshotIfPossible(source, migration, snapshot);
        }
    }

    private void requireSourceVmPoweredOff(VmwareSource source, VmwareCbtMigrationVO migration) {
        UnmanagedInstanceTO.PowerState powerState = getVmwareCbtMigrationService().getPowerState(source.vcenter,
                source.datacenterName, source.username, source.password, source.sourceHost,
                migration.getSourceVmName());
        if (powerState != UnmanagedInstanceTO.PowerState.PowerOff) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Cannot cut over VMware CBT migration %s while source VM %s is in power state %s. Gracefully shut down the source VM, then retry cutover.",
                            migration.getUuid(), migration.getSourceVmName(), powerState));
        }
    }

    @Override
    public VmwareCbtMigrationResponse cancelVmwareCbtMigration(CancelVmwareCbtMigrationCmd cmd) {
        VmwareCbtMigrationVO migration = getMigration(cmd.getId());
        if (!migration.getState().isTerminal()) {
            sendCleanupCommandIfPossible(migration);
            migration.setState(VmwareCbtMigration.State.Cancelled);
            migration.setCurrentStep("Cancelled");
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);
        }
        clearStoredSourceCredentials(migration);
        return createVmwareCbtMigrationResponse(migration);
    }

    @Override
    public boolean deleteVmwareCbtMigration(DeleteVmwareCbtMigrationCmd cmd) {
        VmwareCbtMigrationVO migration = getMigration(cmd.getId());
        if (!canDeleteMigrationState(migration.getState())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Only failed, cancelled or completed VMware CBT migrations can be deleted. Migration %s is in state %s.",
                            migration.getUuid(), migration.getState()));
        }
        clearStoredSourceCredentials(migration);
        if (shouldCleanupDeletedMigration(migration.getState(), cmd.getCleanup())) {
            sendCleanupCommandForDelete(migration);
        } else if (migration.getState() == VmwareCbtMigration.State.Completed && cmd.getCleanup()) {
            LOGGER.debug("Skipping target disk cleanup for completed VMware CBT migration {}. Completed migration deletion is record-only.",
                    migration.getUuid());
        }
        for (VmwareCbtMigrationCycleVO cycle : vmwareCbtMigrationCycleDao.listByMigrationId(migration.getId())) {
            vmwareCbtMigrationCycleDao.remove(cycle.getId());
        }
        for (VmwareCbtMigrationDiskVO disk : vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId())) {
            vmwareCbtMigrationDiskDao.remove(disk.getId());
        }
        return vmwareCbtMigrationDao.remove(migration.getId());
    }

    boolean canDeleteMigrationState(VmwareCbtMigration.State state) {
        return state == VmwareCbtMigration.State.Failed || state == VmwareCbtMigration.State.Cancelled ||
                state == VmwareCbtMigration.State.Completed;
    }

    boolean shouldCleanupDeletedMigration(VmwareCbtMigration.State state, boolean cleanupRequested) {
        return cleanupRequested && (state == VmwareCbtMigration.State.Failed || state == VmwareCbtMigration.State.Cancelled);
    }

    private long getUserIdForOwner(Account owner) {
        long userId = CallContext.current().getCallingUserId();
        List<UserVO> users = userDao.listByAccount(owner.getAccountId());
        if (CollectionUtils.isNotEmpty(users)) {
            userId = users.get(0).getId();
        }
        return userId;
    }

    private String serializeMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return GSON.toJson(map);
    }

    private String serializeNicIpAddressMap(Map<String, Network.IpAddresses> nicIpAddressMap) {
        if (nicIpAddressMap == null || nicIpAddressMap.isEmpty()) {
            return null;
        }
        Map<String, String> ip4AddressMap = new HashMap<>();
        for (Map.Entry<String, Network.IpAddresses> entry : nicIpAddressMap.entrySet()) {
            if (entry.getValue() != null && StringUtils.isNotBlank(entry.getValue().getIp4Address())) {
                ip4AddressMap.put(entry.getKey(), entry.getValue().getIp4Address());
            }
        }
        return serializeMap(ip4AddressMap);
    }

    private DataCenterVO getZone(Long zoneId) {
        DataCenterVO zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Unable to find zone with ID %s", zoneId));
        }
        return zone;
    }

    private ClusterVO getDestinationCluster(Long clusterId, long zoneId) {
        ClusterVO cluster = clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Unable to find cluster with ID %s", clusterId));
        }
        if (cluster.getDataCenterId() != zoneId) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Destination cluster does not belong to the requested zone");
        }
        if (Hypervisor.HypervisorType.KVM != cluster.getHypervisorType()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Destination cluster must be a KVM cluster for VMware CBT migration");
        }
        return cluster;
    }

    private HostVO selectCbtHost(Long hostId, ClusterVO destinationCluster, VmwareCbtStorageTarget storageTarget) {
        boolean requireInPlaceFinalization = requiresInPlaceFinalizationSupport(storageTarget);
        boolean requireRbdSupport = requiresRbdStorageSupport(storageTarget);
        if (hostId == null) {
            String hostCapability = getRequiredHostCapability(requireInPlaceFinalization, requireRbdSupport);
            List<HostVO> hosts = hostDao.listByClusterHypervisorTypeAndHostCapability(destinationCluster.getId(),
                    destinationCluster.getHypervisorType(), hostCapability);
            if (CollectionUtils.isEmpty(hosts)) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        getNoSuitableCbtHostMessage(destinationCluster, requireInPlaceFinalization, requireRbdSupport));
            }
            HostVO host = hosts.get(0);
            hostDao.loadDetails(host);
            return host;
        }
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Unable to find CBT migration host with ID %s", hostId));
        }
        if (host.getClusterId() == null || host.getClusterId() != destinationCluster.getId()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "CBT migration host must belong to the destination KVM cluster");
        }
        if (Hypervisor.HypervisorType.KVM != host.getHypervisorType()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "CBT migration host must be a KVM host");
        }
        if (host.getType() != Host.Type.Routing || host.getStatus() != Status.Up || host.getResourceState() != ResourceState.Enabled) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "CBT migration host must be an enabled, running routing host");
        }
        hostDao.loadDetails(host);
        if (!Boolean.parseBoolean(host.getDetail(Host.HOST_VDDK_BLOCKCOPY_SUPPORT))) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("CBT migration host %s does not report VMware CBT migration support", host.getName()));
        }
        validateStorageTargetFinalizationSupport(storageTarget, host);
        validateRbdHostSupport(storageTarget, host);
        return host;
    }

    private String getRequiredHostCapability(boolean requireInPlaceFinalization, boolean requireRbdSupport) {
        if (requireRbdSupport) {
            return Host.HOST_VDDK_BLOCKCOPY_RBD_SUPPORT;
        }
        if (requireInPlaceFinalization) {
            return Host.HOST_VDDK_BLOCKCOPY_INPLACE_FINALIZATION_SUPPORT;
        }
        return Host.HOST_VDDK_BLOCKCOPY_SUPPORT;
    }

    private String getRequiredHostCapabilityDescription(boolean requireInPlaceFinalization, boolean requireRbdSupport) {
        if (requireRbdSupport) {
            return "VMware CBT migration, in-place finalization and qemu RBD support";
        }
        if (requireInPlaceFinalization) {
            return "VMware CBT migration and in-place finalization support";
        }
        return "VMware CBT migration support";
    }

    private String getNoSuitableCbtHostMessage(ClusterVO destinationCluster, boolean requireInPlaceFinalization,
                                               boolean requireRbdSupport) {
        if (requireRbdSupport) {
            return String.format("Could not find an enabled KVM host in cluster %s with '%s' enabled, which is required to perform VMware CBT migration to RBD storage. " +
                            "This usually means the host is missing VMware CBT migration support, qemu-img RBD support, or in-place virt-v2v support through the virt-v2v-in-place binary or virt-v2v --in-place option.",
                    destinationCluster.getName(), Host.HOST_VDDK_BLOCKCOPY_RBD_SUPPORT);
        }
        if (requireInPlaceFinalization) {
            return String.format("Could not find an enabled KVM host in cluster %s with '%s' enabled, which is required to perform VMware CBT in-place finalization. " +
                            "This usually means the host is missing virt-v2v-in-place or virt-v2v --in-place support.",
                    destinationCluster.getName(), Host.HOST_VDDK_BLOCKCOPY_INPLACE_FINALIZATION_SUPPORT);
        }
        return String.format("Could not find an enabled KVM host in cluster %s that reports %s",
                destinationCluster.getName(), getRequiredHostCapabilityDescription(requireInPlaceFinalization, false));
    }

    void validateStorageTargetFinalizationSupport(VmwareCbtStorageTarget storageTarget, HostVO host) {
        if (storageTarget == null || hostSupportsInPlaceFinalization(host)) {
            return;
        }
        if (isNonInPlaceFinalizationFallbackAllowed(storageTarget)) {
            return;
        }
        String hostName = host == null ? "selected KVM host" : host.getName();
        if (storageTarget.supportsNonInPlaceFinalizationFallback()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Selected KVM host %s cannot finalize VMware CBT migration in-place. Enable virt-v2v in-place support or explicitly allow non-in-place fallback finalization with %s.",
                            hostName, VmwareCbtAllowNonInPlaceFinalization.key()));
        }
        throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                String.format("Destination storage requires virt-v2v in-place finalization, but %s does not report '%s=true'. " +
                                "Ensure virt-v2v-in-place or virt-v2v --in-place is available on the conversion host.",
                        hostName, Host.HOST_VDDK_BLOCKCOPY_INPLACE_FINALIZATION_SUPPORT));
    }

    private boolean hostSupportsInPlaceFinalization(HostVO host) {
        return host != null && Boolean.parseBoolean(host.getDetail(Host.HOST_VDDK_BLOCKCOPY_INPLACE_FINALIZATION_SUPPORT));
    }

    private boolean requiresInPlaceFinalizationSupport(VmwareCbtStorageTarget storageTarget) {
        if (storageTarget == null) {
            return false;
        }
        return storageTarget.requiresInPlaceFinalization() || !isNonInPlaceFinalizationFallbackAllowed(storageTarget);
    }

    private boolean requiresRbdStorageSupport(VmwareCbtStorageTarget storageTarget) {
        return storageTarget != null && storageTarget.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW;
    }

    private void validateRbdHostSupport(VmwareCbtStorageTarget storageTarget, HostVO host) {
        if (!requiresRbdStorageSupport(storageTarget)) {
            return;
        }
        if (host == null || !Boolean.parseBoolean(host.getDetail(Host.HOST_VDDK_BLOCKCOPY_RBD_SUPPORT))) {
            String hostName = host == null ? "selected KVM host" : host.getName();
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Destination RBD storage requires qemu RBD support on the conversion host, but %s does not report '%s=true'. " +
                                    "This usually means the host is missing VMware CBT migration support, qemu-img RBD support, or in-place virt-v2v support through the virt-v2v-in-place binary or virt-v2v --in-place option.",
                            hostName, Host.HOST_VDDK_BLOCKCOPY_RBD_SUPPORT));
        }
    }

    private boolean isNonInPlaceFinalizationFallbackAllowed(VmwareCbtStorageTarget storageTarget) {
        return storageTarget != null && storageTarget.supportsNonInPlaceFinalizationFallback() &&
                Boolean.TRUE.equals(VmwareCbtAllowNonInPlaceFinalization.value());
    }

    private StoragePoolVO getStoragePool(Long storagePoolId, DataCenterVO zone, ClusterVO destinationCluster) {
        if (storagePoolId == null) {
            return getImplicitStoragePool(zone, destinationCluster);
        }
        StoragePoolVO pool = primaryDataStoreDao.findById(storagePoolId);
        if (pool == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Unable to find storage pool with ID %s", storagePoolId));
        }
        if (pool.getDataCenterId() != zone.getId()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Storage pool does not belong to the requested zone");
        }
        if (pool.getClusterId() != null && !pool.getClusterId().equals(destinationCluster.getId())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Cluster-scoped storage pool must belong to the destination KVM cluster");
        }
        return pool;
    }

    private StoragePoolVO getImplicitStoragePool(DataCenterVO zone, ClusterVO destinationCluster) {
        List<StoragePoolVO> pools = listCbtCompatibleStoragePools(zone, destinationCluster);
        if (CollectionUtils.isEmpty(pools)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Cannot find a suitable CBT-compatible primary storage pool in cluster %s for VMware CBT migration. Supported target pool types are NetworkFilesystem, Filesystem, SharedMountPoint and RBD.",
                            destinationCluster.getName()));
        }
        if (pools.size() > 1) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Multiple CBT-compatible primary storage pools are available in cluster %s. Please provide storagepoolid.",
                            destinationCluster.getName()));
        }
        return pools.get(0);
    }

    List<StoragePoolVO> listCbtCompatibleStoragePools(DataCenterVO zone, ClusterVO destinationCluster) {
        Map<Long, StoragePoolVO> poolsById = new LinkedHashMap<>();
        for (Storage.StoragePoolType poolType : CBT_COMPATIBLE_STORAGE_POOL_TYPES) {
            for (StoragePoolVO pool : CollectionUtils.emptyIfNull(primaryDataStoreDao.findClusterWideStoragePoolsByHypervisorAndPoolType(destinationCluster.getId(),
                    Hypervisor.HypervisorType.KVM, poolType))) {
                poolsById.put(pool.getId(), pool);
            }
            for (StoragePoolVO pool : CollectionUtils.emptyIfNull(primaryDataStoreDao.findZoneWideStoragePoolsByHypervisorAndPoolType(zone.getId(),
                    Hypervisor.HypervisorType.KVM, poolType))) {
                poolsById.put(pool.getId(), pool);
            }
        }
        return new ArrayList<>(poolsById.values());
    }

    private DataCenterVO getPreflightZone(Long zoneId, PreflightFindingCollector findings) {
        try {
            DataCenterVO zone = getZone(zoneId);
            findings.pass("destination.zone.exists", "cloudstack", zone.getUuid(),
                    String.format("Destination zone %s was found.", zone.getName()));
            return zone;
        } catch (ServerApiException e) {
            findings.fail("destination.zone.exists", "cloudstack", String.valueOf(zoneId), e.getDescription());
            return null;
        }
    }

    private ClusterVO getPreflightDestinationCluster(Long clusterId, DataCenterVO zone,
                                                     PreflightFindingCollector findings) {
        if (zone == null) {
            return null;
        }
        try {
            ClusterVO cluster = getDestinationCluster(clusterId, zone.getId());
            findings.pass("destination.cluster.kvm", "cloudstack", cluster.getUuid(),
                    String.format("Destination cluster %s is a KVM cluster in the selected zone.", cluster.getName()));
            return cluster;
        } catch (ServerApiException e) {
            findings.fail("destination.cluster.kvm", "cloudstack", String.valueOf(clusterId), e.getDescription());
            return null;
        }
    }

    private HostVO getPreflightCbtHost(Long hostId, ClusterVO destinationCluster, VmwareCbtStorageTarget storageTarget,
                                       PreflightFindingCollector findings) {
        if (destinationCluster == null) {
            return null;
        }
        try {
            HostVO host = selectCbtHost(hostId, destinationCluster, storageTarget);
            findings.pass("destinationHost.vmwareCbtSupport", "kvm", host.getUuid(),
                    String.format("KVM host %s reports VMware CBT migration support.", host.getName()));
            return host;
        } catch (ServerApiException e) {
            findings.fail("destinationHost.vmwareCbtSupport", "kvm",
                    hostId == null ? destinationCluster.getUuid() : String.valueOf(hostId), e.getDescription());
            return null;
        }
    }

    private StoragePoolVO getPreflightStoragePool(Long storagePoolId, DataCenterVO zone, ClusterVO destinationCluster,
                                                  PreflightFindingCollector findings) {
        if (zone == null || destinationCluster == null) {
            return null;
        }
        try {
            StoragePoolVO pool = getStoragePool(storagePoolId, zone, destinationCluster);
            findings.pass("destinationStorage.poolSelected", "storage", pool.getUuid(),
                    String.format("Destination storage pool %s was selected.", pool.getName()));
            return pool;
        } catch (ServerApiException e) {
            findings.fail("destinationStorage.poolSelected", "storage",
                    storagePoolId == null ? destinationCluster.getUuid() : String.valueOf(storagePoolId),
                    e.getDescription());
            return null;
        }
    }

    private VmwareSource getPreflightVmwareSource(CheckVmwareCbtMigrationPrerequisitesCmd cmd,
                                                  PreflightFindingCollector findings) {
        try {
            VmwareSource source = resolveVmwareSource(cmd.getExistingVcenterId(), cmd.getVcenter(),
                    cmd.getDatacenterName(), cmd.getUsername(), cmd.getPassword(), cmd.getSourceHost());
            findings.pass("sourceVcenter.credentialsResolved", "vmware", source.vcenter,
                    "Source vCenter credentials were resolved.");
            return source;
        } catch (ServerApiException e) {
            findings.fail("sourceVcenter.credentialsResolved", "vmware", cmd.getVcenter(), e.getDescription());
            return null;
        }
    }

    private void populateStorageTargetResponse(VmwareCbtMigrationPreflightResponse response,
                                               VmwareCbtStorageTarget storageTarget) {
        if (storageTarget == null) {
            return;
        }
        StoragePoolVO pool = storageTarget.getPool();
        if (pool != null) {
            response.setStoragePoolId(pool.getUuid());
            response.setStoragePoolName(pool.getName());
            response.setStoragePoolType(pool.getPoolType().name());
        }
        response.setStorageWriterType(storageTarget.getTargetStorageType().name());
        response.setStorageWriterSupported(storageTarget.isSupported());
        response.setStorageRequiresInPlaceFinalization(storageTarget.requiresInPlaceFinalization());
        response.setNonInPlaceFinalizationFallbackAllowed(isNonInPlaceFinalizationFallbackAllowed(storageTarget));
        response.setNonInPlaceFinalizationFallbackSupported(storageTarget.supportsNonInPlaceFinalizationFallback());
    }

    private void addStorageTargetFinding(VmwareCbtStorageTarget storageTarget, PreflightFindingCollector findings) {
        if (storageTarget == null) {
            return;
        }
        StoragePoolVO pool = storageTarget.getPool();
        String resource = pool == null ? null : pool.getUuid();
        if (storageTarget.isSupported()) {
            findings.pass("destinationStorage.writerSupported", "storage", resource, storageTarget.getSupportMessage());
        } else {
            findings.fail("destinationStorage.writerSupported", "storage", resource, storageTarget.getSupportMessage());
        }
    }

    private void addStorageTargetFinalizationFinding(VmwareCbtStorageTarget storageTarget, HostVO cbtHost,
                                                     PreflightFindingCollector findings) {
        if (storageTarget == null || !storageTarget.isSupported()) {
            return;
        }
        StoragePoolVO pool = storageTarget.getPool();
        String resource = pool == null ? null : pool.getUuid();
        if (hostSupportsInPlaceFinalization(cbtHost)) {
            findings.pass("destinationStorage.finalizationSupported", "storage", resource,
                    String.format("KVM host %s reports VMware CBT in-place finalization support.",
                            cbtHost.getName()));
            return;
        }
        if (cbtHost == null) {
            findings.fail("destinationStorage.finalizationSupported", "storage", resource,
                    "No suitable KVM host was selected for VMware CBT finalization.");
            return;
        }
        if (isNonInPlaceFinalizationFallbackAllowed(storageTarget)) {
            findings.pass("destinationStorage.finalizationSupported", "storage", resource,
                    String.format("KVM host %s does not report VMware CBT in-place finalization support, but non-in-place fallback is explicitly enabled for qcow2 file targets. CloudStack will stage fallback temporary data on the selected primary storage and verify free space before cutover.",
                            cbtHost.getName()));
            return;
        }
        if (storageTarget.supportsNonInPlaceFinalizationFallback()) {
            findings.fail("destinationStorage.finalizationSupported", "storage", resource,
                    String.format("KVM host %s does not report '%s=true'. Ensure virt-v2v-in-place or virt-v2v --in-place is available, or explicitly allow non-in-place fallback finalization with %s.",
                            cbtHost.getName(), Host.HOST_VDDK_BLOCKCOPY_INPLACE_FINALIZATION_SUPPORT,
                            VmwareCbtAllowNonInPlaceFinalization.key()));
        } else {
            findings.fail("destinationStorage.finalizationSupported", "storage", resource,
                    String.format("Destination storage requires virt-v2v in-place finalization, but KVM host %s does not report '%s=true'. Ensure virt-v2v-in-place or virt-v2v --in-place is available on the conversion host.",
                            cbtHost.getName(), Host.HOST_VDDK_BLOCKCOPY_INPLACE_FINALIZATION_SUPPORT));
        }
    }

    private void addRbdStorageAccessFinding(VmwareCbtStorageTarget storageTarget, HostVO cbtHost,
                                            PreflightFindingCollector findings) {
        if (!requiresRbdStorageSupport(storageTarget)) {
            return;
        }
        StoragePoolVO pool = storageTarget.getPool();
        String resource = pool == null ? null : pool.getUuid();
        if (cbtHost == null) {
            findings.fail("destinationStorage.rbdAccess", "storage", resource,
                    "No suitable KVM host was selected for the RBD access probe.");
            return;
        }
        Answer answer = probeRbdStorageAccess(storageTarget, cbtHost);
        if (answer != null && answer.getResult()) {
            findings.pass("destinationStorage.rbdAccess", "storage", resource,
                    String.format("KVM host %s can create, write, read and delete a temporary RBD image in storage pool %s.",
                            cbtHost.getName(), pool.getName()));
        } else {
            findings.fail("destinationStorage.rbdAccess", "storage", resource, getRbdProbeFailureMessage(answer, cbtHost, pool));
        }
    }

    private void validateRbdStorageAccessForStart(VmwareCbtStorageTarget storageTarget, HostVO cbtHost) {
        if (!requiresRbdStorageSupport(storageTarget)) {
            return;
        }
        Answer answer = probeRbdStorageAccess(storageTarget, cbtHost);
        if (answer == null || !answer.getResult()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    getRbdProbeFailureMessage(answer, cbtHost, storageTarget.getPool()));
        }
    }

    private Answer probeRbdStorageAccess(VmwareCbtStorageTarget storageTarget, HostVO cbtHost) {
        StoragePoolVO pool = storageTarget.getPool();
        VmwareCbtRbdProbeCommand command = new VmwareCbtRbdProbeCommand(pool.getPoolType(), pool.getUuid(),
                RBD_PROBE_IMAGE_PREFIX + UUID.randomUUID());
        command.setWait(getVmwareCbtMigrationAgentCommandTimeout());
        try {
            return agentManager.send(cbtHost.getId(), command);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String details = String.format("Unable to run VMware CBT RBD storage probe on host %s for storage pool %s: %s",
                    cbtHost.getName(), pool.getName(), StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            LOGGER.warn(details, e);
            return new Answer(command, false, details);
        }
    }

    private String getRbdProbeFailureMessage(Answer answer, HostVO cbtHost, StoragePoolVO pool) {
        String details = answer == null ? null : answer.getDetails();
        if (StringUtils.isNotBlank(details)) {
            return details;
        }
        String hostName = cbtHost == null ? "selected KVM host" : cbtHost.getName();
        String poolName = pool == null ? "selected RBD storage pool" : pool.getName();
        return String.format("KVM host %s cannot verify access to RBD storage pool %s. Verify CloudStack RBD primary-storage configuration, Ceph monitor connectivity, qemu RBD block-driver support, librados/librbd client library versions, Java RADOS/RBD bindings and Ceph authentication.",
                hostName, poolName);
    }

    private void populateSourceVmPreflight(VmwareCbtMigrationPreflightResponse response, VmwareSource source,
                                           String sourceVmName, HostVO cbtHost, Long serviceOfferingId,
                                           Map<String, String> details, DataCenterVO zone,
                                           PreflightFindingCollector findings) {
        try {
            VmwareCbtPreflightInfo preflightInfo = getVmwareCbtMigrationService().getPreflightInfo(source.vcenter,
                    source.datacenterName, source.username, source.password, source.sourceHost, sourceVmName);
            response.setSourceVmMor(preflightInfo.getSourceVmMor());
            response.setChangeTrackingSupported(preflightInfo.getChangeTrackingSupported());
            response.setChangeTrackingEnabled(preflightInfo.getChangeTrackingEnabled());
            response.setConsolidationNeeded(preflightInfo.getConsolidationNeeded());
            response.setExistingSnapshotCount(preflightInfo.getExistingSnapshotCount());
            response.setSourceCpuNumber(preflightInfo.getCpuCores());
            response.setSourceCpuSpeed(preflightInfo.getCpuSpeed());
            response.setSourceMemory(preflightInfo.getMemoryMb());
            response.setSourceGuestOsId(preflightInfo.getOperatingSystemId());
            response.setSourceGuestOs(preflightInfo.getOperatingSystem());
            response.setDisks(createPreflightDiskResponses(preflightInfo.getDisks()));
            addSourceVmFindings(preflightInfo, findings);
            addWindowsGuestConversionSupportFinding(cbtHost, sourceVmName, preflightInfo, details, findings);
            addServiceOfferingResourceFindings(preflightInfo, serviceOfferingId, details, zone, findings);
        } catch (RuntimeException e) {
            findings.fail("sourceVm.inspect", "vmware", sourceVmName,
                    sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source));
        }
    }

    private void validateWindowsGuestConversionSupportForStart(HostVO cbtHost, String sourceVmName,
                                                               VmwareCbtPreflightInfo preflightInfo,
                                                               Map<String, String> details) {
        if (!isWindowsSourceVm(preflightInfo)) {
            return;
        }
        CheckConvertInstanceAnswer answer = checkWindowsGuestConversionSupportOnHost(cbtHost, sourceVmName, details);
        if (!answer.getResult()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    getWindowsGuestConversionSupportError(cbtHost, sourceVmName, answer));
        }
    }

    private void addWindowsGuestConversionSupportFinding(HostVO cbtHost, String sourceVmName,
                                                         VmwareCbtPreflightInfo preflightInfo,
                                                         Map<String, String> details,
                                                         PreflightFindingCollector findings) {
        if (!isWindowsSourceVm(preflightInfo)) {
            return;
        }
        if (cbtHost == null) {
            findings.fail("destinationHost.windowsGuestConversionSupport", "host", null,
                    "No suitable KVM host was selected to validate Windows guest conversion support.");
            return;
        }
        try {
            CheckConvertInstanceAnswer answer = checkWindowsGuestConversionSupportOnHost(cbtHost, sourceVmName, details);
            if (answer.getResult()) {
                findings.pass("destinationHost.windowsGuestConversionSupport", "host", cbtHost.getUuid(),
                        String.format("KVM host %s reports Windows guest conversion support.", cbtHost.getName()));
                return;
            }
            findings.fail("destinationHost.windowsGuestConversionSupport", "host", cbtHost.getUuid(),
                    getWindowsGuestConversionSupportError(cbtHost, sourceVmName, answer));
        } catch (ServerApiException e) {
            findings.fail("destinationHost.windowsGuestConversionSupport", "host", cbtHost.getUuid(),
                    e.getDescription());
        }
    }

    private CheckConvertInstanceAnswer checkWindowsGuestConversionSupportOnHost(HostVO cbtHost, String sourceVmName,
                                                                               Map<String, String> details) {
        CheckConvertInstanceCommand command = new CheckConvertInstanceCommand(true, true);
        if (MapUtils.isNotEmpty(details)) {
            command.setVddkLibDir(StringUtils.trimToNull(details.get(Host.HOST_VDDK_LIB_DIR)));
        }
        command.setWait(60);
        try {
            return (CheckConvertInstanceAnswer) agentManager.send(cbtHost.getId(), command);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String error = String.format("Failed to check windows guest conversion support on host %s for VMware CBT migration of source VM %s due to: %s",
                    cbtHost, sourceVmName, e.getMessage());
            LOGGER.error(error, e);
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, error);
        }
    }

    private String getWindowsGuestConversionSupportError(HostVO cbtHost, String sourceVmName,
                                                         CheckConvertInstanceAnswer answer) {
        return String.format("The host %s doesn't support conversion of instance %s from VMware to KVM due to: %s",
                cbtHost, sourceVmName, StringUtils.defaultIfBlank(answer.getDetails(), "unknown reason"));
    }

    static boolean isWindowsSourceVm(VmwareCbtPreflightInfo preflightInfo) {
        return preflightInfo != null &&
                (StringUtils.containsIgnoreCase(preflightInfo.getOperatingSystemId(), "windows") ||
                        StringUtils.containsIgnoreCase(preflightInfo.getOperatingSystem(), "windows"));
    }

    private void addSourceVmFindings(VmwareCbtPreflightInfo preflightInfo, PreflightFindingCollector findings) {
        String sourceVmName = preflightInfo.getSourceVmName();
        findings.pass("sourceVm.found", "vmware", sourceVmName, "Source VM was found in vCenter.");

        if (Boolean.FALSE.equals(preflightInfo.getChangeTrackingSupported())) {
            findings.fail("vmCapability.changeTrackingSupported", "vmware", sourceVmName,
                    "Source VM does not report VMware CBT support.");
        } else if (Boolean.TRUE.equals(preflightInfo.getChangeTrackingSupported())) {
            findings.pass("vmCapability.changeTrackingSupported", "vmware", sourceVmName,
                    "Source VM reports VMware CBT support.");
        } else {
            findings.warn("vmCapability.changeTrackingSupported", "vmware", sourceVmName,
                    "Source VM CBT capability could not be determined.");
        }

        if (Boolean.TRUE.equals(preflightInfo.getChangeTrackingEnabled())) {
            findings.pass("vmConfig.changeTrackingEnabled", "vmware", sourceVmName,
                    "CBT is already enabled on the source VM.");
        } else if (Boolean.FALSE.equals(preflightInfo.getChangeTrackingEnabled())) {
            findings.warn("vmConfig.changeTrackingEnabled", "vmware", sourceVmName,
                    "CBT is not currently enabled; startVmwareCbtMigration will enable it before creating the baseline snapshot.");
        } else {
            findings.warn("vmConfig.changeTrackingEnabled", "vmware", sourceVmName,
                    "CBT enabled state could not be determined.");
        }

        if (Boolean.TRUE.equals(preflightInfo.getConsolidationNeeded())) {
            findings.fail("vmRuntime.consolidationNeeded", "vmware", sourceVmName,
                    "Source VM reports that disk consolidation is needed.");
        } else if (Boolean.FALSE.equals(preflightInfo.getConsolidationNeeded())) {
            findings.pass("vmRuntime.consolidationNeeded", "vmware", sourceVmName,
                    "Source VM does not report pending disk consolidation.");
        } else {
            findings.warn("vmRuntime.consolidationNeeded", "vmware", sourceVmName,
                    "Source VM consolidation state could not be determined.");
        }

        if (preflightInfo.getExistingSnapshotCount() != null && preflightInfo.getExistingSnapshotCount() > 0) {
            findings.warn("vmSnapshot.existingSnapshots", "vmware", sourceVmName,
                    String.format("Source VM has %s existing VMware snapshot(s). Initial full copy may still work, but CBT migration is safer after snapshot consolidation.",
                            preflightInfo.getExistingSnapshotCount()));
        } else {
            findings.pass("vmSnapshot.existingSnapshots", "vmware", sourceVmName,
                    "Source VM has no existing VMware snapshots.");
        }

        if (CollectionUtils.isEmpty(preflightInfo.getDisks())) {
            findings.fail("sourceVm.disks.present", "vmware", sourceVmName,
                    "No source VMware disks were discovered.");
            return;
        }
        for (VmwareCbtPreflightDiskInfo disk : preflightInfo.getDisks()) {
            addSourceDiskFindings(disk, findings);
        }
    }

    private void addServiceOfferingResourceFindings(VmwareCbtPreflightInfo preflightInfo, Long serviceOfferingId,
                                                    Map<String, String> details, DataCenterVO zone,
                                                    PreflightFindingCollector findings) {
        if (serviceOfferingId == null) {
            findings.warn("serviceOffering.present", "serviceoffering", null,
                    "No service offering was provided; source VM CPU and memory sizing was not validated.");
            return;
        }

        Account caller = CallContext.current().getCallingAccount();
        ServiceOfferingVO serviceOffering = getPreflightServiceOffering(serviceOfferingId, caller, zone, findings);
        if (serviceOffering == null) {
            return;
        }

        VmwareCbtOfferingResources requestedResources;
        try {
            requestedResources = resolveRequestedOfferingResources(serviceOffering, details);
        } catch (ServerApiException e) {
            findings.fail("serviceOffering.resources.valid", "serviceoffering", serviceOffering.getUuid(), e.getDescription());
            return;
        }

        addServiceOfferingResourceFinding(preflightInfo.getCpuCores(), requestedResources.getCpuNumber(),
                "serviceOffering.cpuNumber", "CPU number", serviceOffering, findings);
        addServiceOfferingResourceFinding(preflightInfo.getCpuSpeed(), requestedResources.getCpuSpeed(),
                "serviceOffering.cpuSpeed", "CPU speed", serviceOffering, findings);
        addServiceOfferingResourceFinding(preflightInfo.getMemoryMb(), requestedResources.getMemoryMb(),
                "serviceOffering.memory", "Memory", serviceOffering, findings);
    }

    private ServiceOfferingVO getPreflightServiceOffering(Long serviceOfferingId, Account caller, DataCenterVO zone,
                                                          PreflightFindingCollector findings) {
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering == null) {
            findings.fail("serviceOffering.present", "serviceoffering", String.valueOf(serviceOfferingId),
                    String.format("Service offering ID %d cannot be found.", serviceOfferingId));
            return null;
        }
        try {
            if (caller != null && zone != null) {
                accountService.checkAccess(caller, serviceOffering, zone);
            }
            serviceOfferingDao.loadDetails(serviceOffering);
            findings.pass("serviceOffering.present", "serviceoffering", serviceOffering.getUuid(),
                    "Service offering was found.");
            return serviceOffering;
        } catch (RuntimeException e) {
            findings.fail("serviceOffering.access", "serviceoffering", serviceOffering.getUuid(),
                    StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            return null;
        }
    }

    private void addServiceOfferingResourceFinding(Integer sourceResource, Integer requestedResource,
                                                   String code, String resourceName, ServiceOfferingVO serviceOffering,
                                                   PreflightFindingCollector findings) {
        if (sourceResource == null || sourceResource <= 0) {
            findings.warn(code, "serviceoffering", serviceOffering.getUuid(),
                    String.format("Source VM %s could not be determined.", resourceName));
            return;
        }
        if (requestedResource == null || requestedResource <= 0) {
            findings.warn(code, "serviceoffering", serviceOffering.getUuid(),
                    String.format("Selected service offering %s could not be determined.", resourceName));
            return;
        }
        if (requestedResource < sourceResource) {
            findings.fail(code, "serviceoffering", serviceOffering.getUuid(),
                    String.format("The requested %s (%d) is less than the source VM %s (%d).",
                            resourceName, requestedResource, resourceName, sourceResource));
            return;
        }
        findings.pass(code, "serviceoffering", serviceOffering.getUuid(),
                String.format("The requested %s (%d) is sufficient for the source VM %s (%d).",
                        resourceName, requestedResource, resourceName, sourceResource));
    }

    private void addSourceDiskFindings(VmwareCbtPreflightDiskInfo disk, PreflightFindingCollector findings) {
        String diskId = StringUtils.defaultIfBlank(disk.getSourceDiskId(), disk.getSourceDiskPath());
        if (disk.getSourceDiskDeviceKey() == null) {
            findings.fail("disk.deviceKey.present", "vmware", diskId,
                    "Source disk device key is missing; CBT changed-area queries require a disk device key.");
        } else {
            findings.pass("disk.deviceKey.present", "vmware", diskId,
                    "Source disk device key is available.");
        }
        if (StringUtils.isBlank(disk.getSourceDiskPath())) {
            findings.fail("disk.backing.path.present", "vmware", diskId,
                    "Source disk backing path is missing.");
        } else {
            findings.pass("disk.backing.path.present", "vmware", diskId,
                    "Source disk backing path is available.");
        }
        if (disk.isIndependentDisk()) {
            findings.fail("disk.mode.independent", "vmware", diskId,
                    String.format("Source disk mode %s is independent; independent disks are not supported for VMware CBT migration.",
                            disk.getDiskMode()));
        } else {
            findings.pass("disk.mode.independent", "vmware", diskId,
                    "Source disk is not configured as independent.");
        }
        if (disk.isPhysicalRdm()) {
            findings.fail("disk.rdm.physical", "vmware", diskId,
                    "Source disk is a physical-mode RDM; physical RDM disks are not supported for VMware CBT migration.");
        } else {
            findings.pass("disk.rdm.physical", "vmware", diskId,
                    "Source disk is not a physical-mode RDM.");
        }
        if (StringUtils.isBlank(disk.getChangeId())) {
            findings.warn("disk.changeId.present", "vmware", diskId,
                    "A current CBT changeId is not visible for this disk. This can be normal before CloudStack enables CBT and creates the baseline snapshot; startVmwareCbtMigration will validate the baseline snapshot changeId after the initial full sync.");
        } else {
            findings.pass("disk.changeId.present", "vmware", diskId,
                    "A current CBT changeId is visible for this disk.");
        }
    }

    private List<VmwareCbtMigrationPreflightDiskResponse> createPreflightDiskResponses(List<VmwareCbtPreflightDiskInfo> disks) {
        List<VmwareCbtMigrationPreflightDiskResponse> responses = new ArrayList<>();
        if (CollectionUtils.isEmpty(disks)) {
            return responses;
        }
        for (VmwareCbtPreflightDiskInfo disk : disks) {
            VmwareCbtMigrationPreflightDiskResponse response = new VmwareCbtMigrationPreflightDiskResponse();
            response.setObjectName("disk");
            response.setSourceDiskId(disk.getSourceDiskId());
            response.setSourceDiskDeviceKey(disk.getSourceDiskDeviceKey());
            response.setSourceDiskPath(disk.getSourceDiskPath());
            response.setDatastoreName(disk.getDatastoreName());
            response.setCapacityBytes(disk.getCapacityBytes());
            response.setBackingType(disk.getBackingType());
            response.setDiskMode(disk.getDiskMode());
            response.setRdmCompatibilityMode(disk.getRdmCompatibilityMode());
            response.setIndependentDisk(disk.isIndependentDisk());
            response.setPhysicalRdm(disk.isPhysicalRdm());
            response.setChangeIdPresent(StringUtils.isNotBlank(disk.getChangeId()));
            responses.add(response);
        }
        return responses;
    }

    private VmwareSource resolveVmwareSource(StartVmwareCbtMigrationCmd cmd) {
        return resolveVmwareSource(cmd.getExistingVcenterId(), cmd.getVcenter(), cmd.getDatacenterName(),
                cmd.getUsername(), cmd.getPassword(), cmd.getSourceHost());
    }

    private VmwareSource resolveVmwareSource(Long existingVcenterId, String vcenter, String datacenterName,
                                             String username, String password, String sourceHost) {
        if ((existingVcenterId == null && StringUtils.isBlank(vcenter)) ||
                (existingVcenterId != null && StringUtils.isNotBlank(vcenter))) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Please provide an existing vCenter ID or a vCenter IP/Name, parameters are mutually exclusive");
        }

        if (existingVcenterId != null) {
            VmwareDatacenterVO existingDc = vmwareDatacenterDao.findById(existingVcenterId);
            if (existingDc == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        String.format("Cannot find any existing VMware datacenter with ID %s", existingVcenterId));
            }
            return new VmwareSource(existingDc.getId(), existingDc.getVcenterHost(), existingDc.getVmwareDatacenterName(),
                    existingDc.getUser(), existingDc.getPassword(), sourceHost);
        }

        if (StringUtils.isAnyBlank(vcenter, datacenterName, username, password)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please set all the information for a vCenter IP/Name, datacenter, username and password");
        }
        return new VmwareSource(null, vcenter, datacenterName, username, password, sourceHost);
    }

    private VmwareSource resolveVmwareSource(VmwareCbtMigrationVO migration, String username, String password) {
        if (migration.getExistingVcenterId() != null) {
            VmwareDatacenterVO existingDc = vmwareDatacenterDao.findById(migration.getExistingVcenterId());
            if (existingDc == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        String.format("Cannot find any existing VMware datacenter with ID %s",
                                migration.getExistingVcenterId()));
            }
            return new VmwareSource(existingDc.getId(), existingDc.getVcenterHost(), existingDc.getVmwareDatacenterName(),
                    existingDc.getUser(), existingDc.getPassword(), migration.getSourceHost());
        }

        boolean usernameOverrideProvided = StringUtils.isNotBlank(username);
        boolean passwordOverrideProvided = StringUtils.isNotBlank(password);
        if (usernameOverrideProvided != passwordOverrideProvided) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please provide both source vCenter username and password when overriding stored VMware CBT migration credentials");
        }

        String resolvedUsername = usernameOverrideProvided ? username : migration.getSourceUsername();
        String resolvedPassword = passwordOverrideProvided ? password : migration.getSourcePassword();
        if (StringUtils.isAnyBlank(migration.getVcenter(), migration.getDatacenter(), resolvedUsername, resolvedPassword)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please provide source vCenter username and password for this VMware CBT migration");
        }

        if (usernameOverrideProvided) {
            migration.setSourceUsername(resolvedUsername);
            migration.setSourcePassword(resolvedPassword);
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);
        }
        return new VmwareSource(null, migration.getVcenter(), migration.getDatacenter(), resolvedUsername, resolvedPassword,
                migration.getSourceHost());
    }

    private void storeExternalVmwareSourceCredentials(VmwareCbtMigrationVO migration, VmwareSource source) {
        if (source.existingVcenterId != null) {
            return;
        }
        migration.setSourceUsername(source.username);
        migration.setSourcePassword(source.password);
    }

    private void clearStoredSourceCredentials(VmwareCbtMigrationVO migration) {
        if (migration == null ||
                (StringUtils.isBlank(migration.getSourceUsername()) && StringUtils.isBlank(migration.getSourcePassword()))) {
            return;
        }
        migration.setSourceUsername(null);
        migration.setSourcePassword(null);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);
    }

    private VmwareCbtMigration.State parseState(String state) {
        if (StringUtils.isBlank(state)) {
            return null;
        }
        try {
            return VmwareCbtMigration.State.getValue(state);
        } catch (IllegalArgumentException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.getMessage());
        }
    }

    private VmwareCbtMigrationVO getMigration(Long id) {
        VmwareCbtMigrationVO migration = vmwareCbtMigrationDao.findById(id);
        if (migration == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Unable to find VMware CBT migration with ID %s", id));
        }
        return migration;
    }

    private void rejectTerminalMigration(VmwareCbtMigrationVO migration, String action) {
        if (migration.getState().isTerminal()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("Cannot %s VMware CBT migration %s because it is already in state %s",
                            action, migration.getUuid(), migration.getState()));
        }
    }

    private void requireMigrationState(VmwareCbtMigrationVO migration, String action,
                                       VmwareCbtMigration.State... allowedStates) {
        for (VmwareCbtMigration.State allowedState : allowedStates) {
            if (migration.getState() == allowedState) {
                return;
            }
        }
        throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                String.format("Cannot %s VMware CBT migration %s while it is in state %s. Expected state: %s",
                        action, migration.getUuid(), migration.getState(), StringUtils.join(allowedStates, ", ")));
    }

    private List<VmwareCbtDiskInfo> discoverSourceDisks(VmwareSource source, String sourceVmName) {
        List<VmwareCbtDiskInfo> sourceDisks = getVmwareCbtMigrationService().listSourceDisks(source.vcenter, source.datacenterName,
                source.username, source.password, source.sourceHost, sourceVmName);
        if (CollectionUtils.isEmpty(sourceDisks)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    String.format("No source disks were discovered for VMware VM %s", sourceVmName));
        }
        return sourceDisks;
    }

    private void persistSourceDisks(VmwareCbtMigrationVO migration, List<VmwareCbtDiskInfo> sourceDisks) {
        for (VmwareCbtDiskInfo sourceDisk : sourceDisks) {
            VmwareCbtMigrationDiskVO disk = new VmwareCbtMigrationDiskVO(migration.getId(),
                    StringUtils.defaultIfBlank(sourceDisk.getSourceDiskId(), sourceDisk.getLabel()),
                    sourceDisk.getSourceDiskDeviceKey(), sourceDisk.getSourceDiskPath(), sourceDisk.getDatastoreName(),
                    sourceDisk.getCapacityBytes());
            disk.setChangeId(sourceDisk.getChangeId());
            disk.setTargetFormat("qcow2");
            disk.setUpdated(new Date());
            vmwareCbtMigrationDiskDao.persist(disk);
        }
    }

    private void prepareInitialSyncDiskTargets(VmwareCbtMigrationVO migration, VmwareCbtStorageTarget storageTarget) {
        if (storageTarget == null) {
            return;
        }

        String targetBasePath = storageTarget.getTargetStorageType() == VmwareCbtTargetStorageType.QCOW2_FILE ?
                getQcow2InitialSyncTargetBasePath(migration, storageTarget) : null;
        for (VmwareCbtMigrationDiskVO disk : vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId())) {
            String targetFormat = getInitialSyncTargetFormat(storageTarget);
            disk.setTargetFormat(targetFormat);
            if (StringUtils.isBlank(disk.getTargetPath())) {
                disk.setTargetPath(storageTarget.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW ?
                        getRbdInitialSyncTargetImageName(migration, disk) :
                        String.format("%s/%s", targetBasePath, getTargetDiskFileName(disk, targetFormat)));
            }
            disk.setState(VmwareCbtMigrationDisk.State.Syncing);
            disk.setUpdated(new Date());
            vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
        }
    }

    private String getInitialSyncTargetFormat(VmwareCbtStorageTarget storageTarget) {
        if (storageTarget.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW) {
            return "raw";
        }
        return "qcow2";
    }

    private String getQcow2InitialSyncTargetBasePath(VmwareCbtMigrationVO migration, VmwareCbtStorageTarget storageTarget) {
        StoragePoolVO storagePool = storageTarget == null ? null : storageTarget.getPool();
        if (storagePool == null) {
            return String.format("%s/%s", DEFAULT_CBT_DISK_BASE_PATH, migration.getUuid());
        }
        return String.format("%s/%s/cloudstack-cbt/%s", KVM_STORAGE_POOL_MOUNT_BASE_PATH, storagePool.getUuid(),
                migration.getUuid());
    }

    private String getTargetDiskFileName(VmwareCbtMigrationDiskVO disk, String targetFormat) {
        String sourceName = StringUtils.substringAfterLast(StringUtils.defaultIfBlank(disk.getSourceDiskPath(), disk.getSourceDiskId()), "/");
        sourceName = StringUtils.substringBeforeLast(StringUtils.defaultIfBlank(sourceName, disk.getSourceDiskId()), ".");
        return String.format("%s-%s.%s", sanitizeFileName(disk.getSourceDiskId()), sanitizeFileName(sourceName),
                sanitizeFileName(targetFormat));
    }

    private String getRbdInitialSyncTargetImageName(VmwareCbtMigrationVO migration, VmwareCbtMigrationDiskVO disk) {
        String sourceName = StringUtils.substringAfterLast(StringUtils.defaultIfBlank(disk.getSourceDiskPath(), disk.getSourceDiskId()), "/");
        sourceName = StringUtils.substringBeforeLast(StringUtils.defaultIfBlank(sourceName, disk.getSourceDiskId()), ".");
        return String.format("cloudstack-cbt-%s-%s-%s", sanitizeFileName(migration.getUuid()),
                sanitizeFileName(disk.getSourceDiskId()), sanitizeFileName(sourceName));
    }

    private String sanitizeFileName(String value) {
        String sanitized = StringUtils.defaultIfBlank(value, "disk").replaceAll("[^A-Za-z0-9._-]", "-");
        return StringUtils.defaultIfBlank(sanitized, "disk");
    }

    private VmwareCbtMigrationResponse runInitialFullSync(VmwareSource source, VmwareCbtMigrationVO migration,
                                                          VmwareCbtStorageTarget storageTarget) {
        VmwareCbtSnapshotInfo baselineSnapshot = null;
        try {
            migration.setCurrentStep("Creating VMware CBT baseline snapshot");
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);

            baselineSnapshot = createBaselineSnapshot(source, migration);
            prepareInitialSyncDiskTargets(migration, storageTarget);

            migration.setCurrentStep("Initial VDDK full sync is running on KVM agent");
            migration.setLastError(null);
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);

            HostVO cbtHost = getCbtHostForMigration(migration);
            StoragePoolVO storagePool = storageTarget == null ? null : storageTarget.getPool();
            VmwareCbtPrepareCommand prepareCommand = new VmwareCbtPrepareCommand(migration.getUuid(),
                    createRemoteInstance(source, migration, baselineSnapshot.getSourceVmMor()), getDiskTransferObjects(migration),
                    storagePool == null ? null : storagePool.getPoolType(),
                    storagePool == null ? null : storagePool.getUuid(),
                    storageTarget == null ? null : storageTarget.getTargetStorageType(),
                    baselineSnapshot.getSnapshotMor());
            applyVddkDetails(prepareCommand, migration);
            prepareCommand.setWait(getVmwareCbtMigrationAgentCommandTimeout());

            VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, prepareCommand, "prepare",
                    migration.getUuid());
            if (!answer.getResult()) {
                markInitialSyncDisksFailed(migration);
                markMigrationFailed(migration, "Initial VDDK full sync failed", sanitizeSensitiveMessage(answer.getDetails(), source));
                return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
            }

            applyDiskResults(migration, answer.getDiskResults());
            migration.setCurrentStep("Recording VMware CBT baseline metadata");
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);

            refreshBaselineDiskChangeIds(source, migration, baselineSnapshot.getSnapshotMor());
            validateInitialSyncTargetDisks(migration);
            migration = vmwareCbtMigrationDao.findById(migration.getId());
            migration.setState(VmwareCbtMigration.State.Replicating);
            migration.setCurrentStep("Initial VDDK full sync completed; ready for CBT delta synchronization");
            migration.setLastError(null);
            migration.setUpdated(new Date());
            vmwareCbtMigrationDao.update(migration.getId(), migration);
            return createVmwareCbtMigrationResponse(migration);
        } catch (RuntimeException e) {
            String error = sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source);
            markInitialSyncDisksFailed(migration);
            markMigrationFailed(migration, "Initial VDDK full sync failed", error);
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        } finally {
            removeDeltaSnapshotIfPossible(source, migration, baselineSnapshot);
        }
    }

    private VmwareCbtSnapshotInfo createBaselineSnapshot(VmwareSource source, VmwareCbtMigrationVO migration) {
        String snapshotName = String.format("cloudstack-cbt-%s-baseline", migration.getUuid());
        String description = String.format("CloudStack VMware CBT migration %s baseline", migration.getUuid());
        return getVmwareCbtMigrationService().createSnapshot(source.vcenter, source.datacenterName, source.username,
                source.password, source.sourceHost, migration.getSourceVmName(), snapshotName, description, false);
    }

    private void refreshBaselineDiskChangeIds(VmwareSource source, VmwareCbtMigrationVO migration,
                                              String snapshotMor) {
        List<VmwareCbtDiskInfo> sourceDisks = getVmwareCbtMigrationService().listSnapshotDisks(source.vcenter,
                source.datacenterName, source.username, source.password, source.sourceHost,
                migration.getSourceVmName(), snapshotMor);
        if (CollectionUtils.isEmpty(sourceDisks)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    String.format("No source disks were discovered in VMware CBT baseline snapshot %s for VM %s",
                            snapshotMor, migration.getSourceVmName()));
        }
        for (VmwareCbtMigrationDiskVO disk : vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId())) {
            VmwareCbtDiskInfo sourceDisk = findSourceDisk(sourceDisks, disk);
            if (sourceDisk == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                        String.format("Unable to refresh VMware CBT baseline metadata for source disk %s",
                                disk.getSourceDiskId()));
            }
            if (sourceDisk.getSourceDiskDeviceKey() != null) {
                disk.setSourceDiskDeviceKey(sourceDisk.getSourceDiskDeviceKey());
            }
            if (StringUtils.isNotBlank(sourceDisk.getChangeId())) {
                disk.setChangeId(sourceDisk.getChangeId());
            }
            disk.setSnapshotMor(snapshotMor);
            disk.setUpdated(new Date());
            vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
        }
    }

    private VmwareCbtDiskInfo findSourceDisk(List<VmwareCbtDiskInfo> sourceDisks, VmwareCbtMigrationDiskVO disk) {
        for (VmwareCbtDiskInfo sourceDisk : sourceDisks) {
            if (StringUtils.equals(sourceDisk.getSourceDiskId(), disk.getSourceDiskId()) ||
                    StringUtils.equals(sourceDisk.getSourceDiskPath(), disk.getSourceDiskPath()) ||
                    (sourceDisk.getSourceDiskDeviceKey() != null &&
                            sourceDisk.getSourceDiskDeviceKey().equals(disk.getSourceDiskDeviceKey()))) {
                return sourceDisk;
            }
        }
        return null;
    }

    private void validateInitialSyncTargetDisks(VmwareCbtMigrationVO migration) {
        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        if (CollectionUtils.isEmpty(disks)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("VMware CBT migration %s has no discovered source disks", migration.getUuid()));
        }
        for (VmwareCbtMigrationDiskVO disk : disks) {
            if (StringUtils.isBlank(disk.getTargetPath())) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        String.format("VMware CBT migration %s cannot run delta sync before initial full sync registers target disk path for source disk %s",
                                migration.getUuid(), disk.getSourceDiskId()));
            }
            if (StringUtils.isBlank(disk.getChangeId())) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        String.format("VMware CBT migration %s completed initial full sync but cannot run delta sync before CloudStack records a baseline change ID for source disk %s",
                                migration.getUuid(), disk.getSourceDiskId()));
            }
        }
    }

    private VmwareCbtSnapshotInfo createDeltaSnapshot(VmwareSource source, VmwareCbtMigrationVO migration,
                                                      int cycleNumber) {
        String snapshotName = String.format("cloudstack-cbt-%s-%s", migration.getUuid(), cycleNumber);
        String description = String.format("CloudStack VMware CBT migration %s cycle %s", migration.getUuid(),
                cycleNumber);
        return getVmwareCbtMigrationService().createSnapshot(source.vcenter, source.datacenterName, source.username,
                source.password, source.sourceHost, migration.getSourceVmName(), snapshotName, description, false);
    }

    private VmwareCbtChangedBlockQueryResult queryChangedBlocks(VmwareSource source, VmwareCbtMigrationVO migration,
                                                                String snapshotMor) {
        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        List<VmwareCbtDiskInfo> sourceDisks = new ArrayList<>();
        for (VmwareCbtMigrationDiskVO disk : disks) {
            sourceDisks.add(new VmwareCbtDiskInfo(disk.getSourceDiskId(), disk.getSourceDiskDeviceKey(), null,
                    disk.getSourceDiskPath(), disk.getDatastoreName(), disk.getCapacityBytes(), disk.getChangeId()));
            disk.setSnapshotMor(snapshotMor);
            disk.setUpdated(new Date());
            vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
        }

        List<VmwareCbtChangedDiskInfo> changedDisks = getVmwareCbtMigrationService().queryChangedDiskAreas(source.vcenter,
                source.datacenterName, source.username, source.password, source.sourceHost,
                migration.getSourceVmName(), sourceDisks, snapshotMor);
        List<VmwareCbtChangedBlockRangeTO> changedBlocks = new ArrayList<>();
        for (VmwareCbtChangedDiskInfo changedDisk : changedDisks) {
            for (VmwareCbtChangedBlockInfo changedBlock : changedDisk.getChangedBlocks()) {
                changedBlocks.add(new VmwareCbtChangedBlockRangeTO(changedDisk.getSourceDiskId(),
                        changedBlock.getStartOffset(), changedBlock.getLength()));
            }
        }
        return new VmwareCbtChangedBlockQueryResult(changedBlocks, changedDisks);
    }

    private void updateDiskChangeIds(VmwareCbtMigrationVO migration, List<VmwareCbtChangedDiskInfo> changedDisks) {
        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        for (VmwareCbtMigrationDiskVO disk : disks) {
            for (VmwareCbtChangedDiskInfo changedDisk : changedDisks) {
                if (StringUtils.isNotBlank(changedDisk.getNextChangeId()) &&
                        StringUtils.equals(disk.getSourceDiskId(), changedDisk.getSourceDiskId())) {
                    disk.setChangeId(changedDisk.getNextChangeId());
                    disk.setUpdated(new Date());
                    vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
                    break;
                }
            }
        }
    }

    private VmwareCbtMigrationCutoverPolicy getCutoverPolicy() {
        return new VmwareCbtMigrationCutoverPolicy(VmwareCbtMigrationMinCycles.value(),
                VmwareCbtMigrationMaxCycles.value(), VmwareCbtMigrationQuietCycles.value(),
                VmwareCbtMigrationQuietBytes.value(), VmwareCbtMigrationQuietDirtyRate.value());
    }

    private int getVmwareCbtMigrationAgentCommandTimeout() {
        Integer timeout = VmwareCbtMigrationAgentCommandTimeout.value();
        if (timeout == null || timeout <= 0) {
            return Integer.parseInt(VmwareCbtMigrationAgentCommandTimeout.defaultValue());
        }
        return timeout;
    }

    private long getDirtyRateBytesPerSecond(long changedBytes, long durationSeconds, long reportedDirtyRate) {
        if (reportedDirtyRate > 0) {
            return reportedDirtyRate;
        }
        if (durationSeconds <= 0) {
            return changedBytes;
        }
        return changedBytes / durationSeconds;
    }

    private String getCutoverDecisionStep(VmwareCbtMigrationCutoverPolicy.Decision cutoverDecision) {
        switch (cutoverDecision) {
            case READY_FOR_CUTOVER:
                return "Ready for final cutover";
            case READY_FOR_CUTOVER_MAX_CYCLES:
                return "Ready for final cutover after reaching maximum CBT delta cycles";
            case CONTINUE:
            default:
                return "CBT delta synchronization completed; continue replication cycles";
        }
    }

    private void applyDiskResults(VmwareCbtMigrationVO migration, List<VmwareCbtDiskSyncResultTO> diskResults) {
        if (CollectionUtils.isEmpty(diskResults)) {
            return;
        }

        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        for (VmwareCbtDiskSyncResultTO diskResult : diskResults) {
            for (VmwareCbtMigrationDiskVO disk : disks) {
                if (StringUtils.equals(disk.getSourceDiskId(), diskResult.getDiskId())) {
                    applyDiskResult(disk, diskResult);
                    break;
                }
            }
        }
    }

    private void applyDiskResult(VmwareCbtMigrationDiskVO disk, VmwareCbtDiskSyncResultTO diskResult) {
        if (StringUtils.isNotBlank(diskResult.getTargetPath())) {
            disk.setTargetPath(diskResult.getTargetPath());
        }
        if (StringUtils.isNotBlank(diskResult.getChangeId())) {
            disk.setChangeId(diskResult.getChangeId());
        }
        if (StringUtils.isNotBlank(diskResult.getSnapshotMor())) {
            disk.setSnapshotMor(diskResult.getSnapshotMor());
        }
        disk.setState(diskResult.getResult() ? VmwareCbtMigrationDisk.State.Ready : VmwareCbtMigrationDisk.State.Failed);
        disk.setUpdated(new Date());
        vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
    }

    private void removeDeltaSnapshotIfPossible(VmwareSource source, VmwareCbtMigrationVO migration,
                                               VmwareCbtSnapshotInfo snapshot) {
        if (source == null || snapshot == null || StringUtils.isBlank(snapshot.getSnapshotMor())) {
            return;
        }

        try {
            getVmwareCbtMigrationService().removeSnapshot(source.vcenter, source.datacenterName, source.username,
                    source.password, source.sourceHost, migration.getSourceVmName(), snapshot.getSnapshotMor());
            clearDiskSnapshotMor(migration, snapshot.getSnapshotMor());
        } catch (RuntimeException e) {
            String message = sanitizeSensitiveMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()), source);
            LOGGER.warn(String.format("Unable to remove VMware CBT snapshot %s for migration %s: %s",
                    snapshot.getSnapshotMor(), migration.getUuid(), message));
        }
    }

    private void clearDiskSnapshotMor(VmwareCbtMigrationVO migration, String snapshotMor) {
        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        for (VmwareCbtMigrationDiskVO disk : disks) {
            if (StringUtils.equals(disk.getSnapshotMor(), snapshotMor)) {
                disk.setSnapshotMor(null);
                disk.setUpdated(new Date());
                vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
            }
        }
    }

    private VmwareCbtMigrationService getVmwareCbtMigrationService() {
        if (vmwareCbtMigrationService == null) {
            throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR,
                    "VMware CBT migration service is unavailable. Please enable the VMware hypervisor plugin.");
        }
        return vmwareCbtMigrationService;
    }

    private HostVO getCbtHostForMigration(VmwareCbtMigrationVO migration) {
        ClusterVO destinationCluster = clusterDao.findById(migration.getDestinationClusterId());
        if (destinationCluster == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find destination cluster for VMware CBT migration");
        }
        VmwareCbtStorageTarget storageTarget = getStorageTargetForMigration(migration);
        if (migration.getConvertHostId() != null) {
            return selectCbtHost(migration.getConvertHostId(), destinationCluster, storageTarget);
        }
        return selectCbtHost(null, destinationCluster, storageTarget);
    }

    private VmwareCbtStorageTarget getStorageTargetForMigration(VmwareCbtMigrationVO migration) {
        StoragePoolVO storagePool = migration.getStoragePoolId() == null ? null :
                primaryDataStoreDao.findById(migration.getStoragePoolId());
        return VmwareCbtStorageTarget.forPool(storagePool);
    }

    private RemoteInstanceTO createRemoteInstance(VmwareSource source, VmwareCbtMigrationVO migration) {
        return createRemoteInstance(source, migration, null);
    }

    private RemoteInstanceTO createRemoteInstance(VmwareSource source, VmwareCbtMigrationVO migration,
                                                  String vmwareMoref) {
        return new RemoteInstanceTO(migration.getSourceVmName(), null, source.vcenter, source.username, source.password,
                source.datacenterName, migration.getSourceCluster(), migration.getSourceHost(), vmwareMoref);
    }

    private void applyVddkDetails(VmwareCbtMigrationVO migration, Map<String, String> details) {
        if (details == null) {
            return;
        }
        migration.setVddkLibDir(StringUtils.trimToNull(details.get(Host.HOST_VDDK_LIB_DIR)));
        migration.setVddkTransports(StringUtils.trimToNull(details.get(DETAIL_VDDK_TRANSPORTS)));
        migration.setVddkThumbprint(StringUtils.trimToNull(details.get(DETAIL_VDDK_THUMBPRINT)));
    }

    private void applyVddkDetails(VmwareCbtSyncCommand command, VmwareCbtMigrationVO migration) {
        command.setVddkLibDir(migration.getVddkLibDir());
        command.setVddkTransports(migration.getVddkTransports());
        command.setVddkThumbprint(migration.getVddkThumbprint());
    }

    private void applyVddkDetails(VmwareCbtPrepareCommand command, VmwareCbtMigrationVO migration) {
        command.setVddkLibDir(migration.getVddkLibDir());
        command.setVddkTransports(migration.getVddkTransports());
        command.setVddkThumbprint(migration.getVddkThumbprint());
    }

    private void applyVddkDetails(VmwareCbtCutoverCommand command, VmwareCbtMigrationVO migration) {
        command.setVddkLibDir(migration.getVddkLibDir());
        command.setVddkTransports(migration.getVddkTransports());
        command.setVddkThumbprint(migration.getVddkThumbprint());
    }

    private void applyTargetStorageDetails(VmwareCbtSyncCommand command, VmwareCbtMigrationVO migration) {
        VmwareCbtStorageTarget storageTarget = getStorageTargetForMigration(migration);
        StoragePoolVO storagePool = storageTarget == null ? null : storageTarget.getPool();
        command.setDestinationStoragePoolType(storagePool == null ? null : storagePool.getPoolType());
        command.setDestinationStoragePoolUuid(storagePool == null ? null : storagePool.getUuid());
        command.setTargetStorageType(storageTarget == null ? null : storageTarget.getTargetStorageType());
    }

    private void applyTargetStorageDetails(VmwareCbtCutoverCommand command, VmwareCbtMigrationVO migration) {
        VmwareCbtStorageTarget storageTarget = getStorageTargetForMigration(migration);
        StoragePoolVO storagePool = storageTarget == null ? null : storageTarget.getPool();
        command.setDestinationStoragePoolType(storagePool == null ? null : storagePool.getPoolType());
        command.setDestinationStoragePoolUuid(storagePool == null ? null : storagePool.getUuid());
        command.setTargetStorageType(storageTarget == null ? null : storageTarget.getTargetStorageType());
    }

    private void applyTargetStorageDetails(VmwareCbtCleanupCommand command, VmwareCbtMigrationVO migration) {
        VmwareCbtStorageTarget storageTarget = getStorageTargetForMigration(migration);
        StoragePoolVO storagePool = storageTarget == null ? null : storageTarget.getPool();
        command.setDestinationStoragePoolType(storagePool == null ? null : storagePool.getPoolType());
        command.setDestinationStoragePoolUuid(storagePool == null ? null : storagePool.getUuid());
        command.setTargetStorageType(storageTarget == null ? null : storageTarget.getTargetStorageType());
    }

    private List<VmwareCbtDiskTO> getDiskTransferObjects(VmwareCbtMigrationVO migration) {
        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        List<VmwareCbtDiskTO> diskTOs = new ArrayList<>();
        for (VmwareCbtMigrationDiskVO disk : disks) {
            diskTOs.add(new VmwareCbtDiskTO(disk.getSourceDiskId(), disk.getSourceDiskDeviceKey(),
                    disk.getSourceDiskPath(), disk.getDatastoreName(), disk.getTargetPath(), disk.getTargetFormat(),
                    disk.getChangeId(), disk.getSnapshotMor(), disk.getCapacityBytes() == null ? 0L : disk.getCapacityBytes()));
        }
        return diskTOs;
    }

    private VmwareCbtMigrationAnswer sendVmwareCbtCommand(HostVO host, Command command, String action, String migrationUuid) {
        try {
            Answer answer = agentManager.send(host.getId(), command);
            if (answer instanceof VmwareCbtMigrationAnswer) {
                return (VmwareCbtMigrationAnswer) answer;
            }
            return new VmwareCbtMigrationAnswer(command, answer.getResult(), answer.getDetails(), migrationUuid);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            String message = String.format("Failed to %s VMware CBT migration %s on host %s due to: %s",
                    action, migrationUuid, host.getName(), e.getMessage());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, message, e);
        }
    }

    private void markCycleFailed(VmwareCbtMigrationCycleVO cycle, String details) {
        cycle.setState(VmwareCbtMigrationCycle.State.Failed);
        cycle.setDescription(details);
        cycle.setUpdated(new Date());
        vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);
    }

    private void markMigrationFailed(VmwareCbtMigrationVO migration, String currentStep, String details) {
        migration.setState(VmwareCbtMigration.State.Failed);
        migration.setCurrentStep(currentStep);
        migration.setLastError(details);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);
    }

    private void markInitialSyncDisksFailed(VmwareCbtMigrationVO migration) {
        for (VmwareCbtMigrationDiskVO disk : vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId())) {
            if (disk.getState() == VmwareCbtMigrationDisk.State.Syncing || disk.getState() == VmwareCbtMigrationDisk.State.Created) {
                disk.setState(VmwareCbtMigrationDisk.State.Failed);
                disk.setUpdated(new Date());
                vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
            }
        }
    }

    private String sanitizeSensitiveMessage(String message, VmwareSource source) {
        String sanitized = StringUtils.defaultString(message);
        if (source == null) {
            return sanitized;
        }
        return redactValue(sanitized, source.password);
    }

    private String redactValue(String message, String sensitiveValue) {
        if (StringUtils.isAnyBlank(message, sensitiveValue)) {
            return message;
        }
        return StringUtils.replace(message, sensitiveValue, REDACTED_SECRET);
    }

    private void sendCleanupCommandIfPossible(VmwareCbtMigrationVO migration) {
        sendCleanupCommand(migration, false, CLEANUP_WAIT_SECONDS);
    }

    private void sendCleanupCommandForDelete(VmwareCbtMigrationVO migration) {
        if (hasActiveMigrationOnSameConvertHost(migration)) {
            LOGGER.warn("Skipping immediate cleanup for VMware CBT migration {} because conversion host {} is busy with another active VMware CBT migration. " +
                    "The migration record will be removed; any leftover temporary files under cloudstack-cbt can be cleaned later.",
                    migration.getUuid(), migration.getConvertHostId());
            return;
        }
        sendCleanupCommand(migration, true, DELETE_CLEANUP_WAIT_SECONDS);
    }

    private boolean hasActiveMigrationOnSameConvertHost(VmwareCbtMigrationVO migration) {
        if (migration.getConvertHostId() == null) {
            return false;
        }
        for (VmwareCbtMigrationVO otherMigration : vmwareCbtMigrationDao.listByConvertHostId(migration.getConvertHostId())) {
            if (otherMigration.getId() != migration.getId() && !otherMigration.getState().isTerminal()) {
                return true;
            }
        }
        return false;
    }

    private void sendCleanupCommand(VmwareCbtMigrationVO migration, boolean failOnCleanupError, int waitSeconds) {
        if (!hasCleanupTargetDisks(migration)) {
            return;
        }
        try {
            HostVO cbtHost = getCbtHostForMigration(migration);
            VmwareCbtCleanupCommand cleanupCommand = new VmwareCbtCleanupCommand(migration.getUuid(), getDiskTransferObjects(migration),
                    true, true, true);
            applyTargetStorageDetails(cleanupCommand, migration);
            cleanupCommand.setWait(waitSeconds);
            VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, cleanupCommand, "clean up", migration.getUuid());
            if (!answer.getResult()) {
                handleCleanupFailure(migration, failOnCleanupError, answer.getDetails());
            }
        } catch (ServerApiException e) {
            handleCleanupFailure(migration, failOnCleanupError, e.getDescription());
        }
    }

    private boolean hasCleanupTargetDisks(VmwareCbtMigrationVO migration) {
        String migrationMarker = String.format("/cloudstack-cbt/%s/", migration.getUuid());
        String rbdMigrationMarker = String.format("cloudstack-cbt-%s-", migration.getUuid());
        VmwareCbtStorageTarget storageTarget = getStorageTargetForMigration(migration);
        for (VmwareCbtMigrationDiskVO disk : vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId())) {
            String targetPath = StringUtils.defaultString(disk.getTargetPath()).replace('\\', '/');
            if (StringUtils.contains(targetPath, migrationMarker)) {
                return true;
            }
            if (storageTarget != null && storageTarget.getTargetStorageType() == VmwareCbtTargetStorageType.RBD_RAW &&
                    StringUtils.contains(targetPath, rbdMigrationMarker)) {
                return true;
            }
        }
        return false;
    }

    private void handleCleanupFailure(VmwareCbtMigrationVO migration, boolean failOnCleanupError, String details) {
        String error = StringUtils.defaultIfBlank(details,
                String.format("Unable to clean up VMware CBT migration %s target disks", migration.getUuid()));
        migration.setLastError(error);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);
        if (failOnCleanupError) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, error);
        }
    }

    private VmwareCbtMigrationResponse createVmwareCbtMigrationResponse(VmwareCbtMigrationVO migration) {
        VmwareCbtMigrationResponse response = new VmwareCbtMigrationResponse();
        response.setId(migration.getUuid());

        DataCenterVO zone = dataCenterDao.findById(migration.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }

        Account account = accountService.getAccount(migration.getAccountId());
        if (account != null) {
            response.setAccountId(account.getUuid());
            response.setAccountName(account.getAccountName());
        }

        ClusterVO cluster = clusterDao.findById(migration.getDestinationClusterId());
        if (cluster != null) {
            response.setClusterId(cluster.getUuid());
            response.setClusterName(cluster.getName());
        }

        if (migration.getConvertHostId() != null) {
            HostVO host = hostDao.findById(migration.getConvertHostId());
            if (host != null) {
                response.setConvertInstanceHostId(host.getUuid());
                response.setConvertInstanceHostName(host.getName());
            }
        }

        if (migration.getStoragePoolId() != null) {
            StoragePoolVO pool = primaryDataStoreDao.findById(migration.getStoragePoolId());
            if (pool != null) {
                response.setStoragePoolId(pool.getUuid());
                response.setStoragePoolName(pool.getName());
            }
        }

        if (migration.getVmId() != null) {
            UserVmVO vm = userVmDao.findById(migration.getVmId());
            if (vm != null) {
                response.setVirtualMachineId(vm.getUuid());
            }
        }

        response.setDisplayName(migration.getDisplayName());
        response.setVcenter(migration.getVcenter());
        if (migration.getExistingVcenterId() != null) {
            VmwareDatacenterVO existingDc = vmwareDatacenterDao.findById(migration.getExistingVcenterId());
            if (existingDc != null) {
                response.setExistingVcenterId(existingDc.getUuid());
            }
        }
        response.setDatacenterName(migration.getDatacenter());
        response.setSourceHost(migration.getSourceHost());
        response.setSourceCluster(migration.getSourceCluster());
        response.setSourceVmName(migration.getSourceVmName());
        response.setState(migration.getState().name());
        response.setCurrentStep(migration.getCurrentStep());
        response.setCurrentStepDuration(getCurrentStepDuration(migration));
        response.setLastError(migration.getLastError());
        response.setCompletedCycles(migration.getCompletedCycles());
        response.setQuietCycles(migration.getQuietCycles());
        response.setTotalChangedBytes(migration.getTotalChangedBytes());
        response.setLastChangedBytes(migration.getLastChangedBytes());
        response.setLastDirtyRate(migration.getLastDirtyRate());
        response.setDisks(createVmwareCbtMigrationDiskResponses(migration));
        response.setCycles(createVmwareCbtMigrationCycleResponses(migration));
        response.setCreated(migration.getCreated());
        response.setLastUpdated(migration.getUpdated());
        response.setObjectName(OBJECT_NAME);
        return response;
    }

    String getCurrentStepDuration(VmwareCbtMigrationVO migration) {
        if (migration == null || migration.getUpdated() == null || migration.getState() == null ||
                migration.getState().isTerminal()) {
            return null;
        }
        long elapsedSeconds = Math.max(0L,
                TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - migration.getUpdated().getTime()));
        return formatDuration(elapsedSeconds);
    }

    String formatDuration(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        if (seconds < 60L) {
            return String.format("%s sec%s", seconds, seconds == 1L ? "" : "s");
        }
        long minutes = seconds / 60L;
        seconds = seconds % 60L;
        if (minutes < 60L) {
            return String.format("%s min %s sec%s", minutes, seconds, seconds == 1L ? "" : "s");
        }
        long hours = minutes / 60L;
        minutes = minutes % 60L;
        return String.format("%s hr %s min %s sec%s", hours, minutes, seconds, seconds == 1L ? "" : "s");
    }

    private List<VmwareCbtMigrationDiskResponse> createVmwareCbtMigrationDiskResponses(VmwareCbtMigrationVO migration) {
        List<VmwareCbtMigrationDiskResponse> diskResponses = new ArrayList<>();
        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        for (VmwareCbtMigrationDiskVO disk : disks) {
            VmwareCbtMigrationDiskResponse diskResponse = new VmwareCbtMigrationDiskResponse();
            diskResponse.setId(disk.getUuid());
            diskResponse.setSourceDiskId(disk.getSourceDiskId());
            diskResponse.setSourceDiskDeviceKey(disk.getSourceDiskDeviceKey());
            diskResponse.setSourceDiskPath(disk.getSourceDiskPath());
            diskResponse.setDatastoreName(disk.getDatastoreName());
            diskResponse.setCapacityBytes(disk.getCapacityBytes());
            diskResponse.setTargetPath(disk.getTargetPath());
            diskResponse.setTargetFormat(disk.getTargetFormat());
            diskResponse.setChangeId(disk.getChangeId());
            diskResponse.setSnapshotMor(disk.getSnapshotMor());
            diskResponse.setState(disk.getState().name());
            diskResponse.setObjectName("vmwarecbtmigrationdisk");
            diskResponses.add(diskResponse);
        }
        return diskResponses;
    }

    private List<VmwareCbtMigrationCycleResponse> createVmwareCbtMigrationCycleResponses(VmwareCbtMigrationVO migration) {
        List<VmwareCbtMigrationCycleResponse> cycleResponses = new ArrayList<>();
        List<VmwareCbtMigrationCycleVO> cycles = vmwareCbtMigrationCycleDao.listByMigrationId(migration.getId());
        for (VmwareCbtMigrationCycleVO cycle : cycles) {
            VmwareCbtMigrationCycleResponse cycleResponse = new VmwareCbtMigrationCycleResponse();
            cycleResponse.setId(cycle.getUuid());
            cycleResponse.setCycleNumber(cycle.getCycleNumber());
            cycleResponse.setSnapshotMor(cycle.getSnapshotMor());
            cycleResponse.setChangedBytes(cycle.getChangedBytes());
            cycleResponse.setDirtyRate(cycle.getDirtyRate());
            cycleResponse.setDuration(cycle.getDuration());
            cycleResponse.setState(cycle.getState().name());
            cycleResponse.setDescription(cycle.getDescription());
            cycleResponse.setCreated(cycle.getCreated());
            cycleResponse.setLastUpdated(cycle.getUpdated());
            cycleResponse.setObjectName("vmwarecbtmigrationcycle");
            cycleResponses.add(cycleResponse);
        }
        return cycleResponses;
    }

    @Override
    public String getConfigComponentName() {
        return VmwareCbtMigrationManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                VmwareCbtMigrationMinCycles,
                VmwareCbtMigrationMaxCycles,
                VmwareCbtMigrationQuietCycles,
                VmwareCbtMigrationQuietBytes,
                VmwareCbtMigrationQuietDirtyRate,
                VmwareCbtAllowNonInPlaceFinalization,
                VmwareCbtMigrationAgentCommandTimeout
        };
    }

    private static class VmwareSource {
        private final Long existingVcenterId;
        private final String vcenter;
        private final String datacenterName;
        private final String username;
        private final String password;
        private final String sourceHost;

        private VmwareSource(Long existingVcenterId, String vcenter, String datacenterName, String username,
                             String password, String sourceHost) {
            this.existingVcenterId = existingVcenterId;
            this.vcenter = vcenter;
            this.datacenterName = datacenterName;
            this.username = username;
            this.password = password;
            this.sourceHost = sourceHost;
        }
    }

    private static class VmwareCbtChangedBlockQueryResult {
        private final List<VmwareCbtChangedBlockRangeTO> changedBlocks;
        private final List<VmwareCbtChangedDiskInfo> changedDisks;

        private VmwareCbtChangedBlockQueryResult(List<VmwareCbtChangedBlockRangeTO> changedBlocks,
                                                 List<VmwareCbtChangedDiskInfo> changedDisks) {
            this.changedBlocks = changedBlocks;
            this.changedDisks = changedDisks;
        }
    }

    static class VmwareCbtOfferingResources {
        private final Integer cpuNumber;
        private final Integer cpuSpeed;
        private final Integer memoryMb;

        VmwareCbtOfferingResources(Integer cpuNumber, Integer cpuSpeed, Integer memoryMb) {
            this.cpuNumber = cpuNumber;
            this.cpuSpeed = cpuSpeed;
            this.memoryMb = memoryMb;
        }

        Integer getCpuNumber() {
            return cpuNumber;
        }

        Integer getCpuSpeed() {
            return cpuSpeed;
        }

        Integer getMemoryMb() {
            return memoryMb;
        }
    }

    private static class PreflightFindingCollector {
        private final List<VmwareCbtMigrationPreflightFindingResponse> findings = new ArrayList<>();
        private boolean hasFailures;

        void pass(String code, String component, String resource, String message) {
            add("PASS", code, component, resource, message);
        }

        void warn(String code, String component, String resource, String message) {
            add("WARN", code, component, resource, message);
        }

        void fail(String code, String component, String resource, String message) {
            hasFailures = true;
            add("FAIL", code, component, resource, message);
        }

        private void add(String severity, String code, String component, String resource, String message) {
            findings.add(new VmwareCbtMigrationPreflightFindingResponse(severity, code, component,
                    resource, message));
        }

        List<VmwareCbtMigrationPreflightFindingResponse> getFindings() {
            return findings;
        }

        boolean hasFailures() {
            return hasFailures;
        }
    }
}
