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
import com.cloud.utils.Pair;
import com.cloud.utils.concurrency.NamedThreadFactory;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.logging.log4j.ThreadContext;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BackupValidationServiceController extends InternalBackupServiceJobController implements Configurable {

    private static final String LOCK = "validation_lock";

    protected ConfigKey<Integer> backupValidationMaxConcurrentOperationsPerHost = new ConfigKey<>("Advanced", Integer.class,
            "backup.validation.max.concurrent.operations.per.host", "1", "Determines the maximum number of concurrent backup validations per host. Values lower than 0 remove" +
            " the limit, meaning that as many validations as possible will be done at the same time.", true, ConfigKey.Scope.Cluster);

    protected ConfigKey<Integer> backupValidationMaxConcurrentOperations = new ConfigKey<>("Advanced", Integer.class,
            "backup.validation.max.concurrent.operations", "10", "Determines the maximum number of concurrent backup validations in the zone. Values lower than 1 remove" +
            " the limit, meaning that as many validations as possible will be done at the same time.", true, ConfigKey.Scope.Zone);

    protected ConfigKey<Integer> backupValidationInterval = new ConfigKey<>("Advanced", Integer.class,
            "backup.validation.interval", "24", "Determines the period (in hours) between two backup validations for the same backup.", true, ConfigKey.Scope.Account);

    protected ConfigKey<Integer> backupValidationMaxJobRetries = new ConfigKey<>("Advanced", Integer.class,
            "backup.validation.max.job.retries", "2", "Determines the maximum number of retries for backup validation jobs. This includes both start validation jobs and " +
            "finalize validation jobs.", true, ConfigKey.Scope.Account);

    protected ConfigKey<Integer> backupValidationRetryInterval = new ConfigKey<>("Advanced", Integer.class,
            "backup.validation.retry.interval", "60", "Determines the minimum amount of time (in minutes) to retry a backup validation job. This includes both start " +
            "validation jobs and finalize validation jobs.", true, ConfigKey.Scope.Account);

    protected ConfigKey<Boolean> backupValidationTaskEnabled = new ConfigKey<>("Advanced", Boolean.class, "backup.validation.task.enabled", "true", "Whether the backup " +
            "validation task should be running or not. Please set this to false and wait for any validation jobs to finish before restarting the Management Server.", true,
            ConfigKey.Scope.Account);

    public static ConfigKey<Integer> backupValidationBootDefaultTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.validation.boot.default.timeout", "240",
            "Default timeout, in seconds, to wait for the validation VM to boot.", true, ConfigKey.Scope.Account);

    public static ConfigKey<Integer> backupValidationScriptDefaultTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.validation.script.default.timeout", "60",
            "Default timeout, in seconds, to wait for the validation script to finish.", true, ConfigKey.Scope.Account);

    public static ConfigKey<Integer> backupValidationScreenshotDefaultWait = new ConfigKey<>("Advanced", Integer.class, "backup.validation.screenshot.default.wait", "60",
            "Default period to wait, in seconds, to wait before taking a screenshot of the validating VM.", true, ConfigKey.Scope.Account);

    public static ConfigKey<Boolean> backupValidationEndChainOnFail = new ConfigKey<>("Advanced", Boolean.class, "backup.validation.end.chain.on.fail", "true",
            "Whether to end the backup chain when the validation fails.", true, ConfigKey.Scope.Account);

    private ExecutorService executor;

    private ScheduledExecutorService scheduledExecutor;

    @Inject
    private InternalBackupService internalBackupService;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        executor = Executors.newCachedThreadPool(new NamedThreadFactory("BackupValidationTask"));
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("BackupValidationScheduler"));
        scheduledExecutor.scheduleAtFixedRate(this::run, 60, 60, TimeUnit.SECONDS);
        this.controllerType = "validation";
        return true;
    }

    @Override
    protected void searchAndDispatchJobs() {
        try {
            List<DataCenterVO> zones = dataCenterDao.listEnabledZones();
            if (!internalBackupServiceJobDao.lockInLockTable(LOCK, 300)) {
                logger.warn("Unable to get lock for validation jobs.");
                return;
            }

            rescheduleLostJobs();

            if (!backupValidationTaskEnabled.value()) {
                logger.debug("Backup validation task is disabled. Not running.");
                return;
            }

            for (DataCenterVO zone : zones) {
                if (!BackupManager.BackupFrameworkEnabled.valueIn(zone.getId())) {
                    logger.debug("Backup framework is not enabled for zone [{}], will not run the backup validation task for this zone.", zone.getUuid());
                    continue;
                }
                List<InternalBackupServiceJobVO> jobsToStart = internalBackupServiceJobDao.listWaitingJobsAndScheduledToBeforeNow(zone.getId(), InternalBackupServiceJobType.BackupValidation);
                jobsToStart = filterJobsOfDomainsAndAccountsWithDisabledValidationTask(jobsToStart);
                if (jobsToStart.isEmpty()) {
                    continue;
                }
                logger.debug("Found [{}] validation jobs to submit.", jobsToStart.size());
                Pair<HashMap<HostVO, Long>, Integer> hostToNumberOfExecutingJobsAndTotalExecutingJobs = getHostToNumberOfExecutingJobsAndTotalExecutingJobs(zone, InternalBackupServiceJobType.BackupValidation);
                jobsToStart = thinJobsToStartList(zone, jobsToStart, hostToNumberOfExecutingJobsAndTotalExecutingJobs.second(), backupValidationMaxConcurrentOperations);

                List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = filterHostsWithTooManyJobs(hostToNumberOfExecutingJobsAndTotalExecutingJobs.first(), backupValidationMaxConcurrentOperationsPerHost);
                Set<Long> busyInstances = internalBackupServiceJobDao.listExecutingJobsByZoneIdAndJobType(zone.getId(), InternalBackupServiceJobType.StartCompression,
                        InternalBackupServiceJobType.FinalizeCompression).stream().map(InternalBackupServiceJobVO::getInstanceId).collect(Collectors.toSet());

                submitQueuedJobsForExecution(jobsToStart, hostAndNumberOfJobsPairList, busyInstances, backupValidationMaxConcurrentOperationsPerHost, zone.getId());
            }

            ThreadContext.pop();
        } catch (Exception e) {
            logger.error("Caught exception [{}] while trying to search and dispatch backup validation jobs.", e.getMessage(), e);
        } finally {
            internalBackupServiceJobDao.unlockFromLockTable(LOCK);
        }
    }



    @Override
    protected void submitQueuedJob(InternalBackupServiceJobVO job, long zoneId, String logId) {
        executor.submit(() -> startBackupValidation(job, zoneId, logId));
    }


    @Override
    protected List<InternalBackupServiceJobVO> getLostJobs(ClusterVO clusterVO, Calendar date, List<HostVO> hostVOS) {
        date.add(Calendar.SECOND, (int)Math.round(InternalBackupProvider.backupValidationTimeout.valueIn(clusterVO.getId()) * -RESCHEDULE_TO_TIMEOUT_RATIO));
        List<InternalBackupServiceJobVO> result = internalBackupServiceJobDao.listExecutingJobsByHostsAndStartTimeBeforeAndTypeIn(hostVOS.stream().map(HostVO::getId).toArray(),
                date.getTime(), InternalBackupServiceJobType.BackupValidation);
        logger.info("Got [{}] lost backup validation jobs.", result.size());
        if (result.isEmpty()) {
            return result;
        }
        logger.debug("Lost backups validation jobs found: {}", result);
        return result;
    }

    private void startBackupValidation(InternalBackupServiceJobVO job, long zoneId, String logId) {
        boolean result = false;
        try {
            ThreadContext.push(BACKUP_JOB + job.getId());
            ThreadContext.put(LOGCONTEXTID, logId);
            result = internalBackupService.validateBackup(job.getBackupId(), job.getHostId(), zoneId);
            if (result) {
                scheduleNextValidation(job);
            }
        } catch (Exception e) {
            logger.error("Caught exception [{}] while trying to validate backup [{}].", e.getMessage(), job.getBackupId(), e);
        } finally {
            processJobResult(job, result);
            ThreadContext.clearAll();
        }
    }

    private List<InternalBackupServiceJobVO> filterJobsOfDomainsAndAccountsWithDisabledValidationTask(List<InternalBackupServiceJobVO> jobsToFilter) {
        ArrayList<InternalBackupServiceJobVO> filteredJobs = new ArrayList<>();
        for (InternalBackupServiceJobVO job : jobsToFilter) {
            if (backupValidationTaskEnabled.valueIn(job.getAccountId())) {
                filteredJobs.add(job);
            }
        }
        return filteredJobs;
    }

    private void scheduleNextValidation(InternalBackupServiceJobVO job) {
        Calendar nextValidation = Calendar.getInstance();
        nextValidation.add(Calendar.HOUR, backupValidationInterval.valueIn(job.getAccountId()));
        internalBackupServiceJobDao.persist(new InternalBackupServiceJobVO(job.getBackupId(), job.getZoneId(), job.getInstanceId(), job.getAccountId(),
                InternalBackupServiceJobType.BackupValidation, nextValidation.getTime()));
    }

    @Override
    public String getConfigComponentName() {
        return BackupValidationServiceController.class.getSimpleName();
    }

    @Override
    protected int getMaxAttempts(InternalBackupServiceJobVO jobVo) {
        return backupValidationMaxJobRetries.valueIn(jobVo.getAccountId());
    }

    @Override
    protected int getRetryInterval(InternalBackupServiceJobVO jobVo) {
        return backupValidationRetryInterval.valueIn(jobVo.getAccountId());
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {backupValidationInterval, backupValidationTaskEnabled, backupValidationMaxConcurrentOperationsPerHost, backupValidationBootDefaultTimeout,
                backupValidationScriptDefaultTimeout, backupValidationScreenshotDefaultWait, backupValidationEndChainOnFail, backupValidationMaxConcurrentOperations,
                backupValidationMaxJobRetries, backupValidationRetryInterval};
    }
}
