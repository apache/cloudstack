//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.host.HostVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.concurrency.NamedThreadFactory;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.logging.log4j.ThreadContext;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BackupCompressionServiceJobController extends InternalBackupServiceJobController implements Configurable {

    private static final String LOCK = "compression_lock";
    protected ConfigKey<Integer> backupCompressionMaxConcurrentOperationsPerHost = new ConfigKey<>("Advanced", Integer.class,
            "backup.compression.max.concurrent.operations.per.host", "5", "Determines the maximum number of concurrent backup compressions per host. Values lower than 0 remove" +
            " the limit, meaning that as many compressions as possible will be done at the same time.", true, ConfigKey.Scope.Cluster);

    protected ConfigKey<Integer> backupCompressionMaxConcurrentOperations = new ConfigKey<>("Advanced", Integer.class,
            "backup.compression.max.concurrent.operations", "10", "Determines the maximum number of concurrent backup compressions in the zone. Values lower than 1 remove" +
            " the limit, meaning that as many compressions as possible will be done at the same time.", true, ConfigKey.Scope.Zone);

    protected ConfigKey<Integer> backupCompressionMaxJobRetries = new ConfigKey<>("Advanced", Integer.class,
            "backup.compression.max.job.retries", "2", "Determines the maximum number of retries for backup compression jobs. This includes both start compression jobs and " +
            "finalize compression jobs.", true, ConfigKey.Scope.Cluster);

    protected ConfigKey<Integer> backupCompressionRetryInterval = new ConfigKey<>("Advanced", Integer.class,
            "backup.compression.retry.interval", "60", "Determines the minimum amount of time (in minutes) to retry a backup compression job. This includes both start " +
            "compression jobs and finalize compression jobs.", true, ConfigKey.Scope.Cluster);

    protected ConfigKey<Boolean> backupCompressionTaskEnabled = new ConfigKey<>("Advanced", Boolean.class, "backup.compression.task.enabled", "true", "Whether the backup " +
            "compression task should be running or not. Please set this to false and wait for any compression jobs to finish before restarting the Management Server.", true,
            ConfigKey.Scope.Account);

    @Inject
    private InternalBackupService internalBackupService;

    private ExecutorService executor;

    private ScheduledExecutorService scheduledExecutor;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        executor = Executors.newCachedThreadPool(new NamedThreadFactory("BackupCompressionTask"));
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("BackupCompressionScheduler"));
        scheduledExecutor.scheduleAtFixedRate(this::run, 60, 60, TimeUnit.SECONDS);
        this.controllerType = "compression";
        return true;
    }

    /**
     * For each zone, get the jobs that should be started and distribute them through the hosts.
     * Will lock the backup compression table so that only one management server executes this task at a time.
     * Catches all exceptions and only sends them to the log. If we throw an exception, the task will stop running until the server is restarted.
     * */
    @Override
    protected void searchAndDispatchJobs() {
        try {
            List<DataCenterVO> zones = dataCenterDao.listEnabledZones();
            if (!internalBackupServiceJobDao.lockInLockTable(LOCK, 300)) {
                logger.warn("Unable to get lock for compression jobs.");
                return;
            }

            rescheduleLostJobs();

            if (!backupCompressionTaskEnabled.value()) {
                logger.debug("Backup compression task is disabled. Not running.");
                return;
            }

            for (DataCenterVO zone : zones) {
                if (!isFrameworkEnabledForZone(zone)) {
                    logger.debug("Backup framework is not enabled for zone [{}], will not run the backup compression task for this zone.", zone.getUuid());
                    continue;
                }
                List<InternalBackupServiceJobVO> jobsToStart = internalBackupServiceJobDao.listWaitingJobsAndScheduledToBeforeNow(zone.getId(),
                        InternalBackupServiceJobType.StartCompression, InternalBackupServiceJobType.FinalizeCompression);
                jobsToStart = filterJobsOfDomainsAndAccountsWithDisabledCompressionTask(jobsToStart);
                if (jobsToStart.isEmpty()) {
                    continue;
                }
                logger.debug("Found [{}] compression jobs to submit.", jobsToStart.size());
                Pair<HashMap<HostVO, Long>, Integer> hostToNumberOfExecutingJobsAndTotalExecutingJobs = getHostToNumberOfExecutingJobsAndTotalExecutingJobs(zone, InternalBackupServiceJobType.StartCompression);
                List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = filterHostsWithTooManyJobs(hostToNumberOfExecutingJobsAndTotalExecutingJobs.first(),
                        backupCompressionMaxConcurrentOperationsPerHost);
                HashSet<Long> busyInstances = submitFinalizeJobsForExecution(jobsToStart, hostAndNumberOfJobsPairList, zone.getId());
                busyInstances.addAll(internalBackupServiceJobDao.listExecutingJobsByZoneIdAndJobType(zone.getId(), InternalBackupServiceJobType.BackupValidation).stream().
                        map(InternalBackupServiceJobVO::getInstanceId).collect(Collectors.toSet()));

                jobsToStart = thinJobsToStartList(zone, jobsToStart, hostToNumberOfExecutingJobsAndTotalExecutingJobs.second(), backupCompressionMaxConcurrentOperations);
                submitQueuedJobsForExecution(jobsToStart, hostAndNumberOfJobsPairList, busyInstances, backupCompressionMaxConcurrentOperationsPerHost, zone.getId());
            }

            ThreadContext.pop();
        } catch (Exception e) {
            logger.error("Caught exception [{}] while trying to search and dispatch backup compression jobs.", e.getMessage(), e);
        } finally {
            internalBackupServiceJobDao.unlockFromLockTable(LOCK);
        }
    }

    @Override
    protected List<InternalBackupServiceJobVO> getLostJobs(ClusterVO clusterVO, Calendar date, List<HostVO> hostVOS) {
        date.add(Calendar.SECOND, (int)Math.round(InternalBackupProvider.backupCompressionTimeout.valueIn(clusterVO.getId()) * -RESCHEDULE_TO_TIMEOUT_RATIO));
        List<InternalBackupServiceJobVO> result = internalBackupServiceJobDao.listExecutingJobsByHostsAndStartTimeBeforeAndTypeIn(hostVOS.stream().map(HostVO::getId).toArray(),
                date.getTime(), InternalBackupServiceJobType.StartCompression, InternalBackupServiceJobType.FinalizeCompression);
        logger.info("Got [{}] lost backup compression jobs.", result.size());
        if (result.isEmpty()) {
            return result;
        }
        logger.debug("Lost backups compression jobs found: {}", result);
        return result;
    }

    @Override
    protected void submitQueuedJob(InternalBackupServiceJobVO job, long zoneId, String logId) {
        executor.submit(() -> startBackupCompression(job, zoneId, logId));
    }

    /**
     * Submit FinalizeCompression jobs, this should be called before submitStartJobsForExecution.
     * */
    protected HashSet<Long> submitFinalizeJobsForExecution(List<InternalBackupServiceJobVO> jobsToExecute, List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList, long zoneId) {
        List<InternalBackupServiceJobVO> submittedJobs = new ArrayList<>();
        HashSet<Long> setOfInstancesWithExecutingCompressionJobs = new HashSet<>();
        for (InternalBackupServiceJobVO job : jobsToExecute) {
            if (job.getType() != InternalBackupServiceJobType.FinalizeCompression) {
                continue;
            }
            submittedJobs.add(job);
            String logId = UuidUtils.first(UUID.randomUUID().toString());
            logger.debug("Dispatching backup compression job [{}{}] with logid:{} for backup [{}].", BACKUP_JOB, job.getId(), logId, job.getBackupId());

            Pair<HostVO, Long> hostAndNumberOfJobs = hostAndNumberOfJobsPairList.get((int) (Math.random()*hostAndNumberOfJobsPairList.size()));
            job.setHostId(hostAndNumberOfJobs.first().getId());
            job.setStartTime(DateUtil.now());
            internalBackupServiceJobDao.update(job);

            setOfInstancesWithExecutingCompressionJobs.add(job.getInstanceId());
            executor.submit(() -> finalizeBackupCompression(job, zoneId, logId));
        }
        jobsToExecute.removeAll(submittedJobs);
        return setOfInstancesWithExecutingCompressionJobs;
    }

    private void startBackupCompression(InternalBackupServiceJobVO job, long zoneId, String logId) {
        boolean result = false;
        try {
            ThreadContext.push(BACKUP_JOB + job.getId());
            ThreadContext.put(LOGCONTEXTID, logId);
            result = internalBackupService.startBackupCompression(job.getBackupId(), job.getHostId(), zoneId);
        } catch (Exception e) {
            logger.error("Caught exception [{}] while trying to compress backup [{}].", e.getMessage(), job.getBackupId(), e);
        } finally {
            processJobResult(job, result);
            ThreadContext.clearAll();
        }
    }

    private void finalizeBackupCompression(InternalBackupServiceJobVO job, long zoneId, String logId) {
        boolean result = false;
        try {
            ThreadContext.push(BACKUP_JOB + job.getId());
            ThreadContext.put(LOGCONTEXTID, logId);
            result = internalBackupService.finalizeBackupCompression(job.getBackupId(), job.getHostId(), zoneId);
        } catch (Exception e) {
            logger.error("Caught exception [{}] while trying to finalize backup compression [{}].", e.getMessage(), job.getBackupId(), e);
        } finally {
            processJobResult(job, result);
            ThreadContext.clearAll();
        }
    }

    protected List<InternalBackupServiceJobVO> filterJobsOfDomainsAndAccountsWithDisabledCompressionTask(List<InternalBackupServiceJobVO> jobsToFilter) {
        ArrayList<InternalBackupServiceJobVO> filteredJobs = new ArrayList<>();
        for (InternalBackupServiceJobVO job : jobsToFilter) {
            if (backupCompressionTaskEnabled.valueIn(job.getAccountId())) {
                filteredJobs.add(job);
            }
        }
        return filteredJobs;
    }

    @Override
    protected int getMaxAttempts(InternalBackupServiceJobVO jobVo) {
        HostVO hostVO = hostDao.findById(jobVo.getHostId());
        return backupCompressionMaxJobRetries.valueIn(hostVO.getClusterId());
    }

    @Override
    protected int getRetryInterval(InternalBackupServiceJobVO jobVo) {
        HostVO hostVO = hostDao.findById(jobVo.getHostId());
        return backupCompressionRetryInterval.valueIn(hostVO.getClusterId());
    }

    @Override
    public String getConfigComponentName() {
        return BackupCompressionServiceJobController.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {backupCompressionMaxConcurrentOperationsPerHost, backupCompressionMaxJobRetries, backupCompressionRetryInterval, backupCompressionTaskEnabled, backupCompressionMaxConcurrentOperations};
    }
}
