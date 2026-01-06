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

package org.apache.cloudstack.resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.resource.PurgeExpungedResourcesCmd;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.dao.VmWorkJobDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.OpRouterMonitorServiceDao;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ItWorkDao;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.ConsoleSessionDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.NicExtraDhcpOptionDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

public class ResourceCleanupServiceImpl extends ManagerBase implements ResourceCleanupService, PluggableService,
        Configurable {

    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VolumeDetailsDao volumeDetailsDao;
    @Inject
    VolumeDataStoreDao volumeDataStoreDao;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    SnapshotDetailsDao snapshotDetailsDao;
    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    NicDao nicDao;
    @Inject
    NicDetailsDao nicDetailsDao;
    @Inject
    NicExtraDhcpOptionDao nicExtraDhcpOptionDao;
    @Inject
    InlineLoadBalancerNicMapDao inlineLoadBalancerNicMapDao;
    @Inject
    UserVmDetailsDao userVmDetailsDao;
    @Inject
    VMSnapshotDao vmSnapshotDao;
    @Inject
    VMSnapshotDetailsDao vmSnapshotDetailsDao;
    @Inject
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;
    @Inject
    CommandExecLogDao commandExecLogDao;
    @Inject
    NetworkOrchestrationService networkOrchestrationService;
    @Inject
    LoadBalancerVMMapDao loadBalancerVMMapDao;
    @Inject
    NicSecondaryIpDao nicSecondaryIpDao;
    @Inject
    HighAvailabilityManager highAvailabilityManager;
    @Inject
    ItWorkDao itWorkDao;
    @Inject
    OpRouterMonitorServiceDao opRouterMonitorServiceDao;
    @Inject
    PortForwardingRulesDao portForwardingRulesDao;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    VmWorkJobDao vmWorkJobDao;
    @Inject
    ConsoleSessionDao consoleSessionDao;
    @Inject
    ManagementServerHostDao managementServerHostDao;
    @Inject
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    private ScheduledExecutorService expungedResourcesCleanupExecutor;
    private ExecutorService purgeExpungedResourcesJobExecutor;

    protected void purgeLinkedSnapshotEntities(final List<Long> snapshotIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(snapshotIds)) {
            return;
        }
        snapshotDetailsDao.batchExpungeForResources(snapshotIds, batchSize);
        snapshotDataStoreDao.expungeBySnapshotList(snapshotIds, batchSize);
        // Snapshot policies are using ON DELETE CASCADE
    }

    protected long purgeVolumeSnapshots(final List<Long> volumeIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(volumeIds)) {
            return 0;
        }
        SearchBuilder<SnapshotVO> sb = snapshotDao.createSearchBuilder();
        sb.and("volumeIds", sb.entity().getVolumeId(), SearchCriteria.Op.IN);
        sb.and("removed", sb.entity().getRemoved(), SearchCriteria.Op.NNULL);
        SearchCriteria<SnapshotVO> sc = sb.create();
        sc.setParameters("volumeIds", volumeIds.toArray());
        int removed = 0;
        long totalRemoved = 0;
        Filter filter = new Filter(SnapshotVO.class, "id", true, 0L, batchSize);
        final long batchSizeFinal = ObjectUtils.defaultIfNull(batchSize, 0L);
        do {
            List<SnapshotVO> snapshots = snapshotDao.searchIncludingRemoved(sc, filter, null, false);
            List<Long> snapshotIds = snapshots.stream().map(SnapshotVO::getId).collect(Collectors.toList());
            purgeLinkedSnapshotEntities(snapshotIds, batchSize);
            removed = snapshotDao.expungeList(snapshotIds);
            totalRemoved += removed;
        } while (batchSizeFinal > 0 && removed >= batchSizeFinal);
        return totalRemoved;
    }

    protected void purgeLinkedVolumeEntities(final List<Long> volumeIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(volumeIds)) {
            return;
        }
        volumeDetailsDao.batchExpungeForResources(volumeIds, batchSize);
        volumeDataStoreDao.expungeByVolumeList(volumeIds, batchSize);
        purgeVolumeSnapshots(volumeIds, batchSize);
    }

    protected long purgeVMVolumes(final List<Long> vmIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return 0;
        }
        int removed = 0;
        long totalRemoved = 0;
        final long batchSizeFinal = ObjectUtils.defaultIfNull(batchSize, 0L);
        do {
            List<VolumeVO> volumes = volumeDao.searchRemovedByVms(vmIds, batchSize);
            List<Long> volumeIds = volumes.stream().map(VolumeVO::getId).collect(Collectors.toList());
            purgeLinkedVolumeEntities(volumeIds, batchSize);
            removed = volumeDao.expungeList(volumeIds);
            totalRemoved += removed;
        } while (batchSizeFinal > 0 && removed >= batchSizeFinal);
        return totalRemoved;
    }

    protected void purgeLinkedNicEntities(final List<Long> nicIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(nicIds)) {
            return;
        }
        nicDetailsDao.batchExpungeForResources(nicIds, batchSize);
        nicExtraDhcpOptionDao.expungeByNicList(nicIds, batchSize);
        inlineLoadBalancerNicMapDao.expungeByNicList(nicIds, batchSize);
    }

    protected long purgeVMNics(final List<Long> vmIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return 0;
        }
        int removed = 0;
        long totalRemoved = 0;
        final long batchSizeFinal = ObjectUtils.defaultIfNull(batchSize, 0L);
        do {
            List<NicVO> nics = nicDao.searchRemovedByVms(vmIds, batchSize);
            List<Long> nicIds = nics.stream().map(NicVO::getId).collect(Collectors.toList());
            purgeLinkedNicEntities(nicIds, batchSize);
            removed = nicDao.expungeList(nicIds);
            totalRemoved += removed;
        } while (batchSizeFinal > 0 && removed >= batchSizeFinal);
        return totalRemoved;
    }

    protected long purgeVMSnapshots(final List<Long> vmIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return 0;
        }
        int removed = 0;
        long totalRemoved = 0;
        final long batchSizeFinal = ObjectUtils.defaultIfNull(batchSize, 0L);
        do {
            List<VMSnapshotVO> vmSnapshots = vmSnapshotDao.searchRemovedByVms(vmIds, batchSize);
            List<Long> ids = vmSnapshots.stream().map(VMSnapshotVO::getId).collect(Collectors.toList());
            vmSnapshotDetailsDao.batchExpungeForResources(ids, batchSize);
            removed = vmSnapshotDao.expungeList(ids);
            totalRemoved += removed;
        } while (batchSizeFinal > 0 && removed >= batchSizeFinal);
        return totalRemoved;
    }

    protected void purgeLinkedVMEntities(final List<Long> vmIds, final Long batchSize) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return;
        }
        purgeVMVolumes(vmIds, batchSize);
        purgeVMNics(vmIds, batchSize);
        userVmDetailsDao.batchExpungeForResources(vmIds, batchSize);
        purgeVMSnapshots(vmIds, batchSize);
        autoScaleVmGroupVmMapDao.expungeByVmList(vmIds, batchSize);
        commandExecLogDao.expungeByVmList(vmIds, batchSize);
        networkOrchestrationService.expungeLbVmRefs(vmIds, batchSize);
        loadBalancerVMMapDao.expungeByVmList(vmIds, batchSize);
        nicSecondaryIpDao.expungeByVmList(vmIds, batchSize);
        highAvailabilityManager.expungeWorkItemsByVmList(vmIds, batchSize);
        itWorkDao.expungeByVmList(vmIds, batchSize);
        opRouterMonitorServiceDao.expungeByVmList(vmIds, batchSize);
        portForwardingRulesDao.expungeByVmList(vmIds, batchSize);
        ipAddressDao.expungeByVmList(vmIds, batchSize);
        vmWorkJobDao.expungeByVmList(vmIds, batchSize);
        consoleSessionDao.expungeByVmList(vmIds, batchSize);
    }

    protected HashSet<Long> getVmIdsWithActiveVolumeSnapshots(List<Long> vmIds) {
        if (CollectionUtils.isEmpty(vmIds)) {
            return new HashSet<>();
        }
        List<VolumeVO> volumes = volumeDao.searchRemovedByVms(vmIds, null);
        List<Long> volumeIds = volumes.stream().map(VolumeVO::getId).collect(Collectors.toList());
        List<SnapshotVO> activeSnapshots = snapshotDao.searchByVolumes(volumeIds);
        HashSet<Long> activeSnapshotVolumeIds =
                activeSnapshots.stream().map(SnapshotVO::getVolumeId).collect(Collectors.toCollection(HashSet::new));
        List<VolumeVO> volumesWithActiveSnapshots =
                volumes.stream().filter(v -> activeSnapshotVolumeIds.contains(v.getId())).collect(Collectors.toList());
        return volumesWithActiveSnapshots.stream().map(VolumeVO::getInstanceId)
                        .collect(Collectors.toCollection(HashSet::new));
    }

    protected Pair<List<Long>, List<Long>> getFilteredVmIdsForSnapshots(List<Long> vmIds) {
        HashSet<Long> currentSkippedVmIds = new HashSet<>();
        List<VMSnapshotVO> activeSnapshots = vmSnapshotDao.searchByVms(vmIds);
        if (CollectionUtils.isNotEmpty(activeSnapshots)) {
            HashSet<Long> vmIdsWithActiveSnapshots = activeSnapshots.stream().map(VMSnapshotVO::getVmId)
                    .collect(Collectors.toCollection(HashSet::new));
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Skipping purging VMs with IDs %s as they have active " +
                        "VM snapshots", StringUtils.join(vmIdsWithActiveSnapshots)));
            }
            currentSkippedVmIds.addAll(vmIdsWithActiveSnapshots);
        }
        HashSet<Long> vmIdsWithActiveVolumeSnapshots = getVmIdsWithActiveVolumeSnapshots(vmIds);
        if (CollectionUtils.isNotEmpty(vmIdsWithActiveVolumeSnapshots)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Skipping purging VMs with IDs %s as they have volumes with active " +
                        "snapshots", StringUtils.join(vmIdsWithActiveVolumeSnapshots)));
            }
            currentSkippedVmIds.addAll(vmIdsWithActiveVolumeSnapshots);
        }
        if (CollectionUtils.isNotEmpty(currentSkippedVmIds)) {
            vmIds.removeAll(currentSkippedVmIds);
        }
        return new Pair<>(vmIds, new ArrayList<>(currentSkippedVmIds));
    }

    protected Pair<List<Long>, List<Long>> getVmIdsWithNoActiveSnapshots(final Date startDate, final Date endDate,
                 final Long batchSize, final List<Long> skippedVmIds) {
        List<VMInstanceVO> vms = vmInstanceDao.searchRemovedByRemoveDate(startDate, endDate, batchSize, skippedVmIds);
        if (CollectionUtils.isEmpty(vms)) {
            return new Pair<>(new ArrayList<>(), new ArrayList<>());
        }
        List<Long> vmIds = vms.stream().map(VMInstanceVO::getId).collect(Collectors.toList());
        return getFilteredVmIdsForSnapshots(vmIds);
    }

    protected long purgeVMEntities(final Long batchSize, final Date startDate, final Date endDate) {
        return Transaction.execute((TransactionCallbackWithException<Long, CloudRuntimeException>) status -> {
            int count;
            long totalRemoved = 0;
            final long batchSizeFinal = ObjectUtils.defaultIfNull(batchSize, 0L);
            List<Long> skippedVmIds = new ArrayList<>();
            do {
                Pair<List<Long>, List<Long>> allVmIds =
                        getVmIdsWithNoActiveSnapshots(startDate, endDate, batchSize, skippedVmIds);
                List<Long> vmIds = allVmIds.first();
                List<Long> currentSkippedVmIds = allVmIds.second();
                count = vmIds.size() + currentSkippedVmIds.size();
                skippedVmIds.addAll(currentSkippedVmIds);
                purgeLinkedVMEntities(vmIds, batchSize);
                totalRemoved += vmInstanceDao.expungeList(vmIds);
            } while (batchSizeFinal > 0 && count >= batchSizeFinal);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Purged total %d VM records", totalRemoved));
            }
            return totalRemoved;
        });
    }

    protected boolean purgeVMEntity(final long vmId) {
        return Transaction.execute((TransactionCallbackWithException<Boolean, CloudRuntimeException>) status -> {
            final Long batchSize = ExpungedResourcesPurgeBatchSize.value().longValue();
            List<Long> vmIds = new ArrayList<>();
            vmIds.add(vmId);
            Pair<List<Long>, List<Long>> allVmIds = getFilteredVmIdsForSnapshots(vmIds);
            if (CollectionUtils.isEmpty(allVmIds.first())) {
                return false;
            }
            purgeLinkedVMEntities(vmIds, batchSize);
            return vmInstanceDao.expunge(vmId);
        });
    }

    protected long purgeEntities(final List<ResourceType> resourceTypes, final Long batchSize,
             final Date startDate, final Date endDate) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Expunging entities with parameters - resourceType: %s, batchSize: %d, " +
                            "startDate: %s, endDate: %s", StringUtils.join(resourceTypes), batchSize, startDate, endDate));
        }
        long totalPurged = 0;
        if (CollectionUtils.isEmpty(resourceTypes) || resourceTypes.contains(ResourceType.VirtualMachine)) {
            totalPurged += purgeVMEntities(batchSize, startDate, endDate);
        }
        return totalPurged;
    }

    protected Void purgeExpungedResourcesCallback(
            AsyncCallbackDispatcher<ResourceCleanupServiceImpl, PurgeExpungedResourcesResult> callback,
            PurgeExpungedResourcesContext<PurgeExpungedResourcesResult> context) {
        PurgeExpungedResourcesResult result = callback.getResult();
        context.future.complete(result);
        return null;
    }

    protected ResourceType getResourceTypeAndValidatePurgeExpungedResourcesCmdParams(final String resourceTypeStr,
               final Date startDate, final Date endDate, final Long batchSize) {
        ResourceType resourceType = null;
        if (StringUtils.isNotBlank(resourceTypeStr)) {
            resourceType = EnumUtils.getEnumIgnoreCase(ResourceType.class, resourceTypeStr, null);
            if (resourceType == null) {
                throw new InvalidParameterValueException("Invalid resource type specified");
            }
        }
        if (batchSize != null && batchSize <= 0) {
            throw new InvalidParameterValueException(String.format("Invalid %s specified", ApiConstants.BATCH_SIZE));
        }
        if (endDate != null && startDate != null && endDate.before(startDate)) {
            throw new InvalidParameterValueException(String.format("Invalid %s specified", ApiConstants.END_DATE));
        }
        return resourceType;
    }

    protected long purgeExpungedResourceUsingJob(final ResourceType resourceType, final Long batchSize,
                 final Date startDate, final Date endDate) {
        AsyncCallFuture<PurgeExpungedResourcesResult> future = new AsyncCallFuture<>();
        PurgeExpungedResourcesContext<PurgeExpungedResourcesResult> context =
                new PurgeExpungedResourcesContext<>(null, future);
        AsyncCallbackDispatcher<ResourceCleanupServiceImpl, PurgeExpungedResourcesResult> caller =
                AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().purgeExpungedResourcesCallback(null, null))
                .setContext(context);
        PurgeExpungedResourceThread job = new PurgeExpungedResourceThread(resourceType, batchSize, startDate, endDate,
                caller);
        purgeExpungedResourcesJobExecutor.submit(job);
        long expungedCount;
        try {
            PurgeExpungedResourcesResult result = future.get();
            if (result.isFailed()) {
                throw new CloudRuntimeException(String.format("Failed to purge expunged resources due to: %s", result.getResult()));
            }
            expungedCount = result.getPurgedCount();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(String.format("Failed to purge expunged resources due to: %s", e.getMessage()), e);
            throw new CloudRuntimeException("Failed to purge expunged resources");
        }
        return expungedCount;
    }

    protected boolean isVmOfferingPurgeResourcesEnabled(long vmServiceOfferingId) {
        String detail =
                serviceOfferingDetailsDao.getDetail(vmServiceOfferingId, ServiceOffering.PURGE_DB_ENTITIES_KEY);
        return StringUtils.isNotBlank(detail) && Boolean.parseBoolean(detail);
    }

    protected boolean purgeExpungedResource(long resourceId, ResourceType resourceType) {
        if (!ResourceType.VirtualMachine.equals(resourceType)) {
            return false;
        }
        return purgeVMEntity(resourceId);
    }

    protected void purgeExpungedResourceLater(long resourceId, ResourceType resourceType) {
        AsyncCallFuture<PurgeExpungedResourcesResult> future = new AsyncCallFuture<>();
        PurgeExpungedResourcesContext<PurgeExpungedResourcesResult> context =
                new PurgeExpungedResourcesContext<>(null, future);
        AsyncCallbackDispatcher<ResourceCleanupServiceImpl, PurgeExpungedResourcesResult> caller =
                AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().purgeExpungedResourcesCallback(null, null))
                .setContext(context);
        PurgeExpungedResourceThread job = new PurgeExpungedResourceThread(resourceId, resourceType, caller);
        purgeExpungedResourcesJobExecutor.submit(job);
    }

    protected Date parseDateFromConfig(String configKey, String configValue) {
        if (StringUtils.isBlank(configValue)) {
            return null;
        }
        final List<String> dateFormats = List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd");
        Date date = null;
        for (String format : dateFormats) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            try {
                date =  dateFormat.parse(configValue);
                break;
            } catch (ParseException e) {
                logger.trace(String.format("Unable to parse value for config %s: %s with date " +
                        "format: %s due to %s", configKey, configValue, format, e.getMessage()));
            }
        }
        if (date == null) {
            throw new CloudRuntimeException(String.format("Unable to parse value for config %s: %s with date " +
                    "formats: %s", configKey, configValue, StringUtils.join(dateFormats)));
        }
        return date;
    }

    protected Date getStartDateFromConfig() {
        return parseDateFromConfig(ExpungedResourcesPurgeStartTime.key(), ExpungedResourcesPurgeStartTime.value());
    }

    protected Date calculatePastDateFromConfig(String configKey, Integer configValue) {
        if (configValue == null || configValue == 0) {
            return null;
        }
        if (configValue < 0) {
            throw new CloudRuntimeException(String.format("Unable to retrieve a valid value for config %s: %s",
                    configKey, configValue));
        }
        Calendar cal = Calendar.getInstance();
        Date endDate = new Date();
        cal.setTime(endDate);
        cal.add(Calendar.DATE, -1 * configValue);
        return cal.getTime();
    }

    protected Date getEndDateFromConfig() {
        return calculatePastDateFromConfig(ExpungedResourcesPurgeKeepPastDays.key(),
                ExpungedResourcesPurgeKeepPastDays.value());
    }

    protected List<ResourceType> getResourceTypesFromConfig() {
        String resourceTypesConfig = ExpungedResourcePurgeResources.value();
        if (StringUtils.isBlank(resourceTypesConfig)) {
            return null;
        }
        List<ResourceType> resourceTypes = new ArrayList<>();
        for (String type : resourceTypesConfig.split(",")) {
            ResourceType resourceType = EnumUtils.getEnum(ResourceType.class, type.trim(), null);
            if (resourceType == null) {
                throw new CloudRuntimeException(String.format("Invalid resource type: '%s' specified in " +
                        "the config: %s", type, ExpungedResourcePurgeResources.key()));
            }
            resourceTypes.add(resourceType);
        }
        return resourceTypes;
    }

    protected long getBatchSizeFromConfig() {
        Integer batchSize = ExpungedResourcesPurgeBatchSize.value();
        if (batchSize == null || batchSize <= 0) {
            throw new CloudRuntimeException(String.format("Unable to retrieve a valid value for config %s: %s",
                    ExpungedResourcesPurgeBatchSize.key(), batchSize));
        }
        return batchSize.longValue();
    }

    @Override
    public long purgeExpungedResources(PurgeExpungedResourcesCmd cmd) {
        final String resourceTypeStr = cmd.getResourceType();
        final Date startDate = cmd.getStartDate();
        final Date endDate = cmd.getEndDate();
        Long batchSize = cmd.getBatchSize();
        ResourceType resourceType = getResourceTypeAndValidatePurgeExpungedResourcesCmdParams(resourceTypeStr,
                startDate, endDate, batchSize);
        Integer globalBatchSize = ExpungedResourcesPurgeBatchSize.value();
        if (batchSize == null && globalBatchSize > 0) {
            batchSize = globalBatchSize.longValue();
        }
        long expungedCount = purgeExpungedResourceUsingJob(resourceType, batchSize, startDate, endDate);
        if (expungedCount <= 0) {
            logger.debug("No resource expunged during purgeExpungedResources execution");
        }
        return expungedCount;
    }

    @Override
    public void purgeExpungedVmResourcesLaterIfNeeded(VirtualMachine vm) {
        if (!isVmOfferingPurgeResourcesEnabled(vm.getServiceOfferingId())) {
            return;
        }
        purgeExpungedResourceLater(vm.getId(), ResourceType.VirtualMachine);
    }

    @Override
    public boolean start() {
        if (Boolean.TRUE.equals(ExpungedResourcePurgeEnabled.value())) {
            expungedResourcesCleanupExecutor = new ScheduledThreadPoolExecutor(1,
                    new NamedThreadFactory("ExpungedResourceCleanupWorker"));
            expungedResourcesCleanupExecutor.scheduleWithFixedDelay(new ExpungedResourceCleanupWorker(),
                    ExpungedResourcesPurgeDelay.value(), ExpungedResourcesPurgeInterval.value(), TimeUnit.SECONDS);
        }
        purgeExpungedResourcesJobExecutor = Executors.newFixedThreadPool(3,
                new NamedThreadFactory("Purge-Expunged-Resources-Job-Executor"));
        return true;
    }

    @Override
    public boolean stop() {
        purgeExpungedResourcesJobExecutor.shutdown();
        if (expungedResourcesCleanupExecutor != null) {
            expungedResourcesCleanupExecutor.shutdownNow();
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(PurgeExpungedResourcesCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return ResourceCleanupService.class.getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                ExpungedResourcePurgeEnabled,
                ExpungedResourcePurgeResources,
                ExpungedResourcesPurgeInterval,
                ExpungedResourcesPurgeDelay,
                ExpungedResourcesPurgeBatchSize,
                ExpungedResourcesPurgeStartTime,
                ExpungedResourcesPurgeKeepPastDays,
                ExpungedResourcePurgeJobDelay
        };
    }

    public class ExpungedResourceCleanupWorker extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("Expunged.Resource.Cleanup.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        runCleanupForLongestRunningManagementServer();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        protected void runCleanupForLongestRunningManagementServer() {
            ManagementServerHostVO msHost = managementServerHostDao.findOneByLongestRuntime();
            if (msHost == null || (msHost.getMsid() != ManagementServerNode.getManagementServerId())) {
                logger.debug("Skipping the expunged resource cleanup task on this management server");
                return;
            }
            reallyRun();
        }

        public void reallyRun() {
            try {
                Date startDate = getStartDateFromConfig();
                Date endDate = getEndDateFromConfig();
                List<ResourceType> resourceTypes = getResourceTypesFromConfig();
                long batchSize = getBatchSizeFromConfig();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Purging resources: %s as part of cleanup with start date: %s, " +
                            "end date: %s and batch size: %d", StringUtils.join(resourceTypes), startDate, endDate, batchSize));
                }
                purgeEntities(resourceTypes, batchSize, startDate, endDate);
            } catch (Exception e) {
                logger.warn("Caught exception while running expunged resources cleanup task: ", e);
            }
        }
    }

    protected class PurgeExpungedResourceThread extends ManagedContextRunnable {
        ResourceType resourceType;
        Long resourceId;
        Long batchSize;
        Date startDate;
        Date endDate;
        AsyncCompletionCallback<PurgeExpungedResourcesResult> callback;
        long taskTimestamp;

        public PurgeExpungedResourceThread(final ResourceType resourceType, final Long batchSize,
                   final Date startDate, final Date endDate,
                   AsyncCompletionCallback<PurgeExpungedResourcesResult> callback) {
            this.resourceType = resourceType;
            this.batchSize = batchSize;
            this.startDate = startDate;
            this.endDate = endDate;
            this.callback = callback;
        }

        public PurgeExpungedResourceThread(final Long resourceId, final ResourceType resourceType,
                   AsyncCompletionCallback<PurgeExpungedResourcesResult> callback) {
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.callback = callback;
            this.taskTimestamp = System.currentTimeMillis();
        }

        @Override
        protected void runInContext() {
            logger.trace(String.format("Executing purge for resource type: %s with batch size: %d start: %s, end: %s",
                    resourceType, batchSize, startDate, endDate));
            reallyRun();
        }

        protected void waitForPurgeSingleResourceDelay(String resourceAsString) throws InterruptedException {
            long jobDelayConfig = ExpungedResourcePurgeJobDelay.value();
            if (jobDelayConfig < MINIMUM_EXPUNGED_RESOURCE_PURGE_JOB_DELAY_IN_SECONDS) {
                logger.debug(String.format("Value: %d for config: %s is lesser than the minimum value: %d, " +
                                "using minimum value",
                        jobDelayConfig,
                        ExpungedResourcePurgeJobDelay.key(),
                        MINIMUM_EXPUNGED_RESOURCE_PURGE_JOB_DELAY_IN_SECONDS));
                jobDelayConfig = MINIMUM_EXPUNGED_RESOURCE_PURGE_JOB_DELAY_IN_SECONDS;
            }
            long delay = (jobDelayConfig * 1000) -
                    (System.currentTimeMillis() - taskTimestamp);

            if (delay > 0) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Waiting for %d before purging %s", delay, resourceAsString));
                }
                Thread.sleep(delay);
            }
        }

        protected void purgeSingleResource() {
            String resourceAsString = String.format("resource [type: %s, ID: %d]", resourceType, resourceId);
            try {
                waitForPurgeSingleResourceDelay(resourceAsString);
                if (!purgeExpungedResource(resourceId, resourceType)) {
                    throw new CloudRuntimeException(String.format("Failed to purge %s", resourceAsString));
                }
                if (logger.isDebugEnabled()) {
                    logger.info(String.format("Purged %s", resourceAsString));
                }
                callback.complete(new PurgeExpungedResourcesResult(resourceId, resourceType, null));
            } catch (CloudRuntimeException e) {
                logger.error(String.format("Caught exception while purging %s: ", resourceAsString), e);
                callback.complete(new PurgeExpungedResourcesResult(resourceId, resourceType, e.getMessage()));
            } catch (InterruptedException e) {
                logger.error(String.format("Caught exception while waiting for purging %s: ", resourceAsString), e);
                callback.complete(new PurgeExpungedResourcesResult(resourceId, resourceType, e.getMessage()));
            }
        }

        protected void purgeMultipleResources() {
            try {
                long purged = purgeEntities(resourceType == null ? null : List.of(resourceType),
                        batchSize, startDate, endDate);
                callback.complete(new PurgeExpungedResourcesResult(resourceType, batchSize, startDate, endDate, purged));
            } catch (CloudRuntimeException e) {
                logger.error("Caught exception while expunging resources: ", e);
                callback.complete(new PurgeExpungedResourcesResult(resourceType, batchSize, startDate, endDate, e.getMessage()));
            }
        }

        public void reallyRun() {
            if (resourceId != null) {
                purgeSingleResource();
                return;
            }
            purgeMultipleResources();
        }
    }

    public static class PurgeExpungedResourcesResult extends CommandResult {
        ResourceType resourceType;
        Long resourceId;
        Long batchSize;
        Date startDate;
        Date endDate;
        Long purgedCount;

        public PurgeExpungedResourcesResult(final ResourceType resourceType, final Long batchSize,
                    final Date startDate, final Date endDate, final long purgedCount) {
            super();
            this.resourceType = resourceType;
            this.batchSize = batchSize;
            this.startDate = startDate;
            this.endDate = endDate;
            this.purgedCount = purgedCount;
            this.setSuccess(true);
        }

        public PurgeExpungedResourcesResult(final ResourceType resourceType, final Long batchSize,
                    final Date startDate, final Date endDate, final String error) {
            super();
            this.resourceType = resourceType;
            this.batchSize = batchSize;
            this.startDate = startDate;
            this.endDate = endDate;
            this.setResult(error);
        }

        public PurgeExpungedResourcesResult(final Long resourceId, final ResourceType resourceType,
                    final String error) {
            super();
            this.resourceId = resourceId;
            this.resourceType = resourceType;
            if (error != null) {
                this.setResult(error);
            } else {
                this.purgedCount = 1L;
                this.setSuccess(true);
            }
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public Long getResourceId() {
            return resourceId;
        }

        public Long getBatchSize() {
            return batchSize;
        }

        public Date getStartDate() {
            return startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public Long getPurgedCount() {
            return purgedCount;
        }
    }

    public static class PurgeExpungedResourcesContext<T> extends AsyncRpcContext<T> {
        final AsyncCallFuture<PurgeExpungedResourcesResult> future;

        public PurgeExpungedResourcesContext(AsyncCompletionCallback<T> callback,
                 AsyncCallFuture<PurgeExpungedResourcesResult> future) {
            super(callback);
            this.future = future;
        }

    }
}
