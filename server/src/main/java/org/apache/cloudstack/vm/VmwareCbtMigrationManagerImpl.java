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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.CancelVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.CutoverVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVmwareCbtMigrationsCmd;
import org.apache.cloudstack.api.command.admin.vm.RegisterVmwareCbtMigrationTargetCmd;
import org.apache.cloudstack.api.command.admin.vm.StartVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.SyncVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationCycleResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationDiskResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.VmwareCbtCleanupCommand;
import com.cloud.agent.api.VmwareCbtCutoverCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtSyncCommand;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.agent.api.to.VmwareCbtDiskSyncResultTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VmwareDatacenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VmwareDatacenterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmwareCbtMigrationCycleVO;
import com.cloud.vm.VmwareCbtMigrationDiskVO;
import com.cloud.vm.VmwareCbtMigrationVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VmwareCbtMigrationCycleDao;
import com.cloud.vm.dao.VmwareCbtMigrationDao;
import com.cloud.vm.dao.VmwareCbtMigrationDiskDao;

public class VmwareCbtMigrationManagerImpl implements VmwareCbtMigrationManager, Configurable {

    private static final String OBJECT_NAME = "vmwarecbtmigration";
    private static final String DETAIL_VDDK_TRANSPORTS = "vddk.transports";
    private static final String DETAIL_VDDK_THUMBPRINT = "vddk.thumbprint";
    private static final Logger LOGGER = LogManager.getLogger(VmwareCbtMigrationManagerImpl.class);

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
    private UserVmDao userVmDao;
    @Inject
    private VmwareCbtMigrationDiskDao vmwareCbtMigrationDiskDao;
    @Inject
    private VmwareCbtMigrationCycleDao vmwareCbtMigrationCycleDao;
    @Inject
    private AgentManager agentManager;
    @Autowired(required = false)
    private VmwareCbtMigrationService vmwareCbtMigrationService;

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(StartVmwareCbtMigrationCmd.class);
        cmdList.add(ListVmwareCbtMigrationsCmd.class);
        cmdList.add(SyncVmwareCbtMigrationCmd.class);
        cmdList.add(RegisterVmwareCbtMigrationTargetCmd.class);
        cmdList.add(CutoverVmwareCbtMigrationCmd.class);
        cmdList.add(CancelVmwareCbtMigrationCmd.class);
        return cmdList;
    }

    @Override
    public VmwareCbtMigrationResponse startVmwareCbtMigration(StartVmwareCbtMigrationCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        if (caller == null) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Unable to determine calling account");
        }

        DataCenterVO zone = getZone(cmd.getZoneId());
        ClusterVO destinationCluster = getDestinationCluster(cmd.getClusterId(), zone.getId());
        HostVO convertHost = selectCbtHost(cmd.getConvertInstanceHostId(), destinationCluster);
        StoragePoolVO storagePool = getStoragePool(cmd.getStoragePoolId(), zone, destinationCluster);

        String sourceVmName = StringUtils.trimToNull(cmd.getSourceVmName());
        if (sourceVmName == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Source VM name is required");
        }

        VmwareSource source = resolveVmwareSource(cmd);
        List<VmwareCbtDiskInfo> sourceDisks = discoverSourceDisks(source, sourceVmName);
        String displayName = StringUtils.defaultIfBlank(cmd.getDisplayName(), sourceVmName);

        VmwareCbtMigrationVO migration = new VmwareCbtMigrationVO(zone.getId(), caller.getAccountId(), CallContext.current().getCallingUserId(),
                destinationCluster.getId(), displayName, source.vcenter, source.datacenterName, cmd.getSourceHost(), cmd.getSourceCluster(), sourceVmName);
        migration.setExistingVcenterId(source.existingVcenterId);
        if (convertHost != null) {
            migration.setConvertHostId(convertHost.getId());
        }
        if (storagePool != null) {
            migration.setStoragePoolId(storagePool.getId());
        }
        applyVddkDetails(migration, cmd.getDetails());
        migration.setState(VmwareCbtMigration.State.InitialSync);
        migration.setCurrentStep(String.format("Discovered %s source disk(s); waiting for initial VDDK full sync", sourceDisks.size()));
        migration.setUpdated(new Date());
        migration = vmwareCbtMigrationDao.persist(migration);
        persistSourceDisks(migration, sourceDisks);
        return createVmwareCbtMigrationResponse(migration);
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
                    createRemoteInstance(source, migration), getDiskTransferObjects(migration),
                    changedBlockQuery.changedBlocks, cycleNumber, snapshot.getSnapshotMor(), false);
            applyVddkDetails(syncCommand, migration);
            syncCommand.setWait(3600);

            VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, syncCommand, "synchronize",
                    migration.getUuid());
            if (!answer.getResult()) {
                markCycleFailed(cycle, answer.getDetails());
                markMigrationFailed(migration, "CBT delta synchronization failed", answer.getDetails());
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
            cycle.setDescription(answer.getDetails());
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
            String error = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            markCycleFailed(cycle, error);
            markMigrationFailed(migration, "CBT delta synchronization failed", error);
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        } finally {
            removeDeltaSnapshotIfPossible(source, migration, snapshot);
        }
    }

    @Override
    public VmwareCbtMigrationResponse registerVmwareCbtMigrationTarget(RegisterVmwareCbtMigrationTargetCmd cmd) {
        VmwareCbtMigrationVO migration = getMigration(cmd.getId());
        rejectTerminalMigration(migration, "register target disks for");
        requireMigrationState(migration, "register target disks for", VmwareCbtMigration.State.Created,
                VmwareCbtMigration.State.InitialSync);

        int registeredDiskCount = registerTargetDisks(migration, cmd.getTargetDisks());
        int diskCount = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId()).size();
        boolean allTargetDisksRegistered = registeredDiskCount == diskCount;

        migration.setState(allTargetDisksRegistered ? VmwareCbtMigration.State.Replicating :
                VmwareCbtMigration.State.InitialSync);
        migration.setCurrentStep(allTargetDisksRegistered ?
                "Initial full sync target disks registered; ready for CBT delta synchronization" :
                String.format("Registered %s of %s initial full sync target disk(s)", registeredDiskCount, diskCount));
        migration.setLastError(null);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);
        return createVmwareCbtMigrationResponse(migration);
    }

    @Override
    public VmwareCbtMigrationResponse cutoverVmwareCbtMigration(CutoverVmwareCbtMigrationCmd cmd) {
        VmwareCbtMigrationVO migration = getMigration(cmd.getId());
        rejectTerminalMigration(migration, "cut over");
        requireMigrationState(migration, "cut over", VmwareCbtMigration.State.ReadyForCutover);
        VmwareSource source = resolveVmwareSource(migration, cmd.getUsername(), cmd.getPassword());
        validateInitialSyncTargetDisks(migration);
        HostVO cbtHost = getCbtHostForMigration(migration);

        migration.setState(VmwareCbtMigration.State.CuttingOver);
        migration.setCurrentStep("Running final CBT cutover");
        migration.setLastError(null);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);

        VmwareCbtCutoverCommand cutoverCommand = new VmwareCbtCutoverCommand(migration.getUuid(), createRemoteInstance(source, migration),
                getDiskTransferObjects(migration), migration.getCompletedCycles() + 1, true);
        applyVddkDetails(cutoverCommand, migration);
        cutoverCommand.setWait(3600);

        VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, cutoverCommand, "cut over", migration.getUuid());
        if (!answer.getResult()) {
            markMigrationFailed(migration, "CBT cutover failed", answer.getDetails());
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        }

        applyDiskResults(migration, answer.getDiskResults());
        migration.setState(VmwareCbtMigration.State.Completed);
        migration.setCurrentStep("Completed");
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);
        return createVmwareCbtMigrationResponse(migration);
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
        return createVmwareCbtMigrationResponse(migration);
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

    private HostVO selectCbtHost(Long hostId, ClusterVO destinationCluster) {
        if (hostId == null) {
            List<HostVO> hosts = hostDao.listByClusterHypervisorTypeAndHostCapability(destinationCluster.getId(),
                    destinationCluster.getHypervisorType(), Host.HOST_VMWARE_CBT_SUPPORT);
            if (CollectionUtils.isEmpty(hosts)) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        String.format("Could not find an enabled KVM host in cluster %s that reports VMware CBT migration support", destinationCluster.getName()));
            }
            return hosts.get(0);
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
        if (!Boolean.parseBoolean(host.getDetail(Host.HOST_VMWARE_CBT_SUPPORT))) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    String.format("CBT migration host %s does not report VMware CBT migration support", host.getName()));
        }
        return host;
    }

    private StoragePoolVO getStoragePool(Long storagePoolId, DataCenterVO zone, ClusterVO destinationCluster) {
        if (storagePoolId == null) {
            return null;
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

    private VmwareSource resolveVmwareSource(StartVmwareCbtMigrationCmd cmd) {
        if ((cmd.getExistingVcenterId() == null && StringUtils.isBlank(cmd.getVcenter())) ||
                (cmd.getExistingVcenterId() != null && StringUtils.isNotBlank(cmd.getVcenter()))) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Please provide an existing vCenter ID or a vCenter IP/Name, parameters are mutually exclusive");
        }

        if (cmd.getExistingVcenterId() != null) {
            VmwareDatacenterVO existingDc = vmwareDatacenterDao.findById(cmd.getExistingVcenterId());
            if (existingDc == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        String.format("Cannot find any existing VMware datacenter with ID %s", cmd.getExistingVcenterId()));
            }
            return new VmwareSource(existingDc.getId(), existingDc.getVcenterHost(), existingDc.getVmwareDatacenterName(),
                    existingDc.getUser(), existingDc.getPassword(), cmd.getSourceHost());
        }

        if (StringUtils.isAnyBlank(cmd.getVcenter(), cmd.getDatacenterName(), cmd.getUsername(), cmd.getPassword())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please set all the information for a vCenter IP/Name, datacenter, username and password");
        }
        return new VmwareSource(null, cmd.getVcenter(), cmd.getDatacenterName(), cmd.getUsername(), cmd.getPassword(),
                cmd.getSourceHost());
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

        if (StringUtils.isAnyBlank(migration.getVcenter(), migration.getDatacenter(), username, password)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please provide source vCenter username and password for this VMware CBT migration");
        }
        return new VmwareSource(null, migration.getVcenter(), migration.getDatacenter(), username, password,
                migration.getSourceHost());
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
        }
    }

    private int registerTargetDisks(VmwareCbtMigrationVO migration, List<VmwareCbtTargetDiskInfo> targetDisks) {
        if (CollectionUtils.isEmpty(targetDisks)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "At least one target disk must be provided");
        }

        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        Map<String, VmwareCbtMigrationDiskVO> disksBySourceDiskId = new HashMap<>();
        for (VmwareCbtMigrationDiskVO disk : disks) {
            disksBySourceDiskId.put(disk.getSourceDiskId(), disk);
        }

        for (VmwareCbtTargetDiskInfo targetDisk : targetDisks) {
            VmwareCbtMigrationDiskVO disk = disksBySourceDiskId.get(targetDisk.getSourceDiskId());
            if (disk == null) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                        String.format("Source disk %s is not tracked by VMware CBT migration %s",
                                targetDisk.getSourceDiskId(), migration.getUuid()));
            }
            disk.setTargetPath(targetDisk.getTargetPath());
            disk.setTargetFormat(StringUtils.defaultIfBlank(targetDisk.getTargetFormat(), "qcow2"));
            if (StringUtils.isNotBlank(targetDisk.getChangeId())) {
                disk.setChangeId(targetDisk.getChangeId());
            }
            if (StringUtils.isNotBlank(targetDisk.getSnapshotMor())) {
                disk.setSnapshotMor(targetDisk.getSnapshotMor());
            }
            disk.setState(VmwareCbtMigrationDisk.State.Ready);
            disk.setUpdated(new Date());
            vmwareCbtMigrationDiskDao.update(disk.getId(), disk);
        }

        int registeredDiskCount = 0;
        for (VmwareCbtMigrationDiskVO disk : vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId())) {
            if (StringUtils.isNotBlank(disk.getTargetPath())) {
                registeredDiskCount++;
            }
        }
        return registeredDiskCount;
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
            LOGGER.warn(String.format("Unable to remove VMware CBT snapshot %s for migration %s: %s",
                    snapshot.getSnapshotMor(), migration.getUuid(), e.getMessage()), e);
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
        if (migration.getConvertHostId() != null) {
            return selectCbtHost(migration.getConvertHostId(), destinationCluster);
        }
        return selectCbtHost(null, destinationCluster);
    }

    private RemoteInstanceTO createRemoteInstance(VmwareSource source, VmwareCbtMigrationVO migration) {
        return new RemoteInstanceTO(migration.getSourceVmName(), null, source.vcenter, source.username, source.password,
                source.datacenterName, migration.getSourceCluster(), migration.getSourceHost());
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

    private void applyVddkDetails(VmwareCbtCutoverCommand command, VmwareCbtMigrationVO migration) {
        command.setVddkLibDir(migration.getVddkLibDir());
        command.setVddkTransports(migration.getVddkTransports());
        command.setVddkThumbprint(migration.getVddkThumbprint());
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

    private void sendCleanupCommandIfPossible(VmwareCbtMigrationVO migration) {
        try {
            HostVO cbtHost = getCbtHostForMigration(migration);
            VmwareCbtCleanupCommand cleanupCommand = new VmwareCbtCleanupCommand(migration.getUuid(), getDiskTransferObjects(migration),
                    true, true, true);
            cleanupCommand.setWait(300);
            sendVmwareCbtCommand(cbtHost, cleanupCommand, "clean up", migration.getUuid());
        } catch (ServerApiException e) {
            migration.setLastError(e.getDescription());
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
                VmwareCbtMigrationQuietDirtyRate
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
}
