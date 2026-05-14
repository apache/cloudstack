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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.CancelVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.CutoverVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVmwareCbtMigrationsCmd;
import org.apache.cloudstack.api.command.admin.vm.StartVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.SyncVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.VmwareCbtCleanupCommand;
import com.cloud.agent.api.VmwareCbtCutoverCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtSyncCommand;
import com.cloud.agent.api.to.RemoteInstanceTO;
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

public class VmwareCbtMigrationManagerImpl implements VmwareCbtMigrationManager {

    private static final String OBJECT_NAME = "vmwarecbtmigration";

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
        if (convertHost != null) {
            migration.setConvertHostId(convertHost.getId());
        }
        if (storagePool != null) {
            migration.setStoragePoolId(storagePool.getId());
        }
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

        VmwareCbtSyncCommand syncCommand = new VmwareCbtSyncCommand(migration.getUuid(), createRemoteInstance(migration),
                getDiskTransferObjects(migration), Collections.emptyList(), cycleNumber, null, false);
        syncCommand.setWait(3600);

        VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, syncCommand, "synchronize", migration.getUuid());
        if (!answer.getResult()) {
            markCycleFailed(cycle, answer.getDetails());
            markMigrationFailed(migration, "CBT delta synchronization failed", answer.getDetails());
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        }

        cycle.setState(VmwareCbtMigrationCycle.State.Completed);
        cycle.setChangedBytes(answer.getChangedBytes());
        cycle.setDirtyRate(answer.getDirtyRateBytesPerSecond());
        cycle.setDuration(answer.getDurationSeconds() * 1000);
        cycle.setDescription(answer.getDetails());
        cycle.setUpdated(new Date());
        vmwareCbtMigrationCycleDao.update(cycle.getId(), cycle);

        migration.setCompletedCycles(cycleNumber);
        migration.setTotalChangedBytes(migration.getTotalChangedBytes() + answer.getChangedBytes());
        migration.setLastChangedBytes(answer.getChangedBytes());
        migration.setLastDirtyRate(answer.getDirtyRateBytesPerSecond());
        migration.setState(answer.getReadyForCutover() ? VmwareCbtMigration.State.ReadyForCutover : VmwareCbtMigration.State.Replicating);
        migration.setCurrentStep(answer.getReadyForCutover() ? "Ready for final cutover" : "CBT delta synchronization completed");
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);
        return createVmwareCbtMigrationResponse(migration);
    }

    @Override
    public VmwareCbtMigrationResponse cutoverVmwareCbtMigration(CutoverVmwareCbtMigrationCmd cmd) {
        VmwareCbtMigrationVO migration = getMigration(cmd.getId());
        rejectTerminalMigration(migration, "cut over");
        HostVO cbtHost = getCbtHostForMigration(migration);

        migration.setState(VmwareCbtMigration.State.CuttingOver);
        migration.setCurrentStep("Running final CBT cutover");
        migration.setLastError(null);
        migration.setUpdated(new Date());
        vmwareCbtMigrationDao.update(migration.getId(), migration);

        VmwareCbtCutoverCommand cutoverCommand = new VmwareCbtCutoverCommand(migration.getUuid(), createRemoteInstance(migration),
                getDiskTransferObjects(migration), migration.getCompletedCycles() + 1, true);
        cutoverCommand.setWait(3600);

        VmwareCbtMigrationAnswer answer = sendVmwareCbtCommand(cbtHost, cutoverCommand, "cut over", migration.getUuid());
        if (!answer.getResult()) {
            markMigrationFailed(migration, "CBT cutover failed", answer.getDetails());
            return createVmwareCbtMigrationResponse(vmwareCbtMigrationDao.findById(migration.getId()));
        }

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
            return new VmwareSource(existingDc.getVcenterHost(), existingDc.getVmwareDatacenterName(),
                    existingDc.getUser(), existingDc.getPassword(), cmd.getSourceHost());
        }

        if (StringUtils.isAnyBlank(cmd.getVcenter(), cmd.getDatacenterName(), cmd.getUsername(), cmd.getPassword())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Please set all the information for a vCenter IP/Name, datacenter, username and password");
        }
        return new VmwareSource(cmd.getVcenter(), cmd.getDatacenterName(), cmd.getUsername(), cmd.getPassword(),
                cmd.getSourceHost());
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

    private List<VmwareCbtDiskInfo> discoverSourceDisks(VmwareSource source, String sourceVmName) {
        if (vmwareCbtMigrationService == null) {
            throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR,
                    "VMware CBT disk discovery service is unavailable. Please enable the VMware hypervisor plugin.");
        }

        List<VmwareCbtDiskInfo> sourceDisks = vmwareCbtMigrationService.listSourceDisks(source.vcenter, source.datacenterName,
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
                    sourceDisk.getSourceDiskPath(), sourceDisk.getDatastoreName(), sourceDisk.getCapacityBytes());
            disk.setChangeId(sourceDisk.getChangeId());
            disk.setTargetFormat("qcow2");
            disk.setUpdated(new Date());
            vmwareCbtMigrationDiskDao.persist(disk);
        }
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

    private RemoteInstanceTO createRemoteInstance(VmwareCbtMigrationVO migration) {
        return new RemoteInstanceTO(migration.getSourceVmName(), null, migration.getVcenter(), null, null,
                migration.getDatacenter(), migration.getSourceCluster(), migration.getSourceHost());
    }

    private List<VmwareCbtDiskTO> getDiskTransferObjects(VmwareCbtMigrationVO migration) {
        List<VmwareCbtMigrationDiskVO> disks = vmwareCbtMigrationDiskDao.listByMigrationId(migration.getId());
        List<VmwareCbtDiskTO> diskTOs = new ArrayList<>();
        for (VmwareCbtMigrationDiskVO disk : disks) {
            diskTOs.add(new VmwareCbtDiskTO(disk.getSourceDiskId(), disk.getSourceDiskPath(), disk.getDatastoreName(),
                    disk.getTargetPath(), disk.getTargetFormat(), disk.getChangeId(), disk.getSnapshotMor(),
                    disk.getCapacityBytes() == null ? 0L : disk.getCapacityBytes()));
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
        response.setCreated(migration.getCreated());
        response.setLastUpdated(migration.getUpdated());
        response.setObjectName(OBJECT_NAME);
        return response;
    }

    private static class VmwareSource {
        private final String vcenter;
        private final String datacenterName;
        private final String username;
        private final String password;
        private final String sourceHost;

        private VmwareSource(String vcenter, String datacenterName, String username, String password, String sourceHost) {
            this.vcenter = vcenter;
            this.datacenterName = datacenterName;
            this.username = username;
            this.password = password;
            this.sourceHost = sourceHost;
        }
    }
}
