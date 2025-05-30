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
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceState;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.backup.dao.BackupCompressionJobDao;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.logging.log4j.ThreadContext;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupCompressionService extends ManagerBase implements Configurable {

    private static final String COMPRESSION_JOB = "compression-job-";
    private static final String LOGCONTEXTID = "logcontextid";

    private static final String LOCK = "compression_lock";
    private static final double RESCHEDULE_TO_TIMEOUT_RATIO = 2.5;
    protected ConfigKey<Integer> backupCompressionMaxConcurrentCompressionsPerHost = new ConfigKey<>("Advanced", Integer.class,
            "backup.compression.max.concurrent.compressions.per.host", "5", "Determines the maximum number of concurrent backup compressions per host. Values lower than 0 remove" +
            " the limit, meaning that as many compressions as possible will be done at the same time.", true, ConfigKey.Scope.Cluster);

    protected ConfigKey<Integer> backupCompressionMaxJobRetries = new ConfigKey<>("Advanced", Integer.class,
            "backup.compression.max.job.retries", "2", "Determines the maximum number of retries for backup compression jobs. This includes both start compression jobs and " +
            "finalize compression jobs.", true, ConfigKey.Scope.Cluster);

    protected ConfigKey<Integer> backupCompressionRetryInterval = new ConfigKey<>("Advanced", Integer.class,
            "backup.compression.retry.interval", "60", "Determines the minimum amount of time (in minutes) to retry a backup compression job. This includes both start " +
            "compression jobs and finalize compression jobs.", true, ConfigKey.Scope.Cluster);

    protected ConfigKey<Boolean> backupCompressionTaskEnabled = new ConfigKey<>("Advanced", Boolean.class, "backup.compression.task.enabled", "true", "Whether the backup " +
            "compression task should be running or not. Please set this to false and wait for any compression jobs to finish before restarting the Management Server.", true,
            ConfigKey.Scope.Global);

    @Inject
    private BackupCompressionJobDao backupCompressionJobDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private BackupDao backupDao;

    @Inject
    private DataCenterDao dataCenterDao;

    @Inject
    private NativeBackupService nativeBackupService;

    @Inject
    private ClusterDao clusterDao;

    @Inject
    private AsyncJobManager asyncJobManager;

    @Inject
    private VMInstanceDao instanceDao;

    private ExecutorService executor;

    private ScheduledExecutorService scheduledExecutor;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        executor = Executors.newCachedThreadPool(new NamedThreadFactory("BackupCompressionTask"));
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("BackupCompressionScheduler"));
        scheduledExecutor.scheduleAtFixedRate(this::searchAndDispatchJobs, 60, 60, TimeUnit.SECONDS);
        return true;
    }

    /**
     * For each zone, get the jobs that should be started and distribute them through the hosts.
     * Will lock the backup compression table so that only one management server executes this task at a time.
     * Catches all exceptions and only sends them to the log. If we throw an exception, the task will stop running until the server is restarted.
     * */
    private void searchAndDispatchJobs() {
        ThreadContext.put(LOGCONTEXTID, UuidUtils.first(UUID.randomUUID().toString()));
        logger.debug("Searching for backup compression jobs to dispatch.");

        if (!asyncJobManager.isAsyncJobsEnabled()) {
            logger.debug("A management shutdown has been triggered. Not running backup compression task.");
            return;
        }

        Transaction.execute(TransactionLegacy.CLOUD_DB, new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    List<DataCenterVO> zones = dataCenterDao.listEnabledZones();
                    if (!backupCompressionJobDao.lockInLockTable(LOCK, 300)) {
                        logger.warn("Unable to get lock for compression jobs.");
                        return;
                    }

                    rescheduleLostJobs();

                    if (!backupCompressionTaskEnabled.value()) {
                        logger.debug("Backup compression task is disabled. Not running.");
                        return;
                    }

                    for (DataCenterVO zone : zones) {
                        if (!BackupManager.BackupFrameworkEnabled.valueIn(zone.getId())) {
                            logger.debug("Backup framework is not enabled for zone [{}], will not run the backup compression task for this zone.", zone.getUuid());
                            continue;
                        }
                        List<BackupCompressionJobVO> jobsToStart = backupCompressionJobDao.listWaitingJobsAndScheduledToBeforeNow(zone.getId());
                        if (jobsToStart.isEmpty()) {
                            continue;
                        }
                        logger.debug("Found [{}] compression jobs to submit.");
                        HashMap<HostVO, Long> hostToNumberOfExecutingJobs = getHostToNumberOfExecutingJobs(zone);
                        List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = filterHostsWithTooManyCompressionJobs(hostToNumberOfExecutingJobs);
                        HashSet<Long> busyInstances = submitFinalizeJobsForExecution(jobsToStart, hostAndNumberOfJobsPairList, zone.getId());
                        submitStartJobsForExecution(jobsToStart, hostAndNumberOfJobsPairList, busyInstances, zone.getId());
                    }

                    ThreadContext.pop();
                } catch (Exception e) {
                    logger.error("Caught exception [{}] while trying to search and dispatch backup compression jobs.", e.getMessage(), e);
                } finally {
                    backupCompressionJobDao.unlockFromLockTable(LOCK);
                }
            }
        });
    }

    /**
     * Goes through all the executing jobs for the zone and returns a map from up and enabled KVM hosts to the number of executing jobs.
     * */
    private HashMap<HostVO, Long> getHostToNumberOfExecutingJobs(DataCenterVO zone) {
        List<HostVO> allKvmHostsForZone = hostDao.listAllRoutingHostsByZoneAndHypervisorType(zone.getId(), Hypervisor.HypervisorType.KVM);
        HashMap<HostVO, Long> hostToNumberOfExecutingJobs = new HashMap<>();

        for (HostVO host : allKvmHostsForZone) {
            if (host.getStatus() == Status.Up && host.getResourceState() == ResourceState.Enabled) {
                hostToNumberOfExecutingJobs.put(host, 0L);
            }
        }

        List<BackupCompressionJobVO> executingStartJobs = backupCompressionJobDao.listExecutingJobsByZoneIdAndJobType(zone.getId(), BackupCompressionJobType.StartCompression);
        for (BackupCompressionJobVO executingJob : executingStartJobs) {
            HostVO host = allKvmHostsForZone.stream().filter(hostVO -> hostVO.getId() == executingJob.getHostId()).findFirst().orElse(null);
            if (host == null) {
                logger.error("Compression job [{}] is running in an unknown host. This job will be rescheduled in the future.", executingJob);
                continue;
            } else if (host.getStatus() != Status.Up || host.getResourceState() != ResourceState.Enabled) {
                logger.warn("Compression job [{}] is running in host [{}], which is not up or not enabled. If possible, wait for the job to finish before restarting the Agent.",
                        executingJob, host);
                continue;
            }

            hostToNumberOfExecutingJobs.computeIfPresent(host, (hostVO, numberOfJobs) -> numberOfJobs + 1);
        }

        return hostToNumberOfExecutingJobs;
    }

    private List<Pair<HostVO, Long>> filterHostsWithTooManyCompressionJobs(HashMap<HostVO, Long> hostToNumberOfExecutingJobs) {
        List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = new ArrayList<>();
        for (HostVO host : hostToNumberOfExecutingJobs.keySet()) {
            Long numberOfJobs = hostToNumberOfExecutingJobs.get(host);
            Integer maxConcurrentCompressionsPerHost = backupCompressionMaxConcurrentCompressionsPerHost.valueIn(host.getClusterId());
            if (maxConcurrentCompressionsPerHost > 0 && numberOfJobs >= maxConcurrentCompressionsPerHost) {
                logger.debug("Host [{}] is already executing the maximum number of concurrent compression jobs set in [{}]. Current number of jobs being executed is " +
                        "[{}], the value for the configuration is [{]].", host, backupCompressionMaxConcurrentCompressionsPerHost.toString(), numberOfJobs, maxConcurrentCompressionsPerHost);
                continue;
            }
            hostAndNumberOfJobsPairList.add(new Pair<>(host, numberOfJobs));
        }
        return hostAndNumberOfJobsPairList;
    }

    /**
     * Reschedule jobs that seem to be stuck. This should run even if the backup compression task is disabled, so that stuck jobs get cleaned if necessary.
     * */
    private void rescheduleLostJobs() {
        for (DataCenterVO dataCenterVO : dataCenterDao.listAllZones()) {
            logger.debug("Searching lost compression jobs to reschedule in zone [{}].", dataCenterVO.getUuid());
            for (ClusterVO clusterVO : clusterDao.listByDcHyType(dataCenterVO.getId(), Hypervisor.HypervisorType.KVM.toString())) {
                List<HostVO> hostVOS = hostDao.findRoutingByClusterId(clusterVO.getId());
                if (hostVOS.isEmpty()) {
                    logger.debug("No hosts found in cluster [{}]. Cannot reschedule jobs for it.", clusterVO.getUuid());
                    continue;
                }
                Calendar date = Calendar.getInstance();
                date.add(Calendar.SECOND, (int)Math.round(NativeBackupProvider.backupCompressionTimeout.valueIn(clusterVO.getId()) * -RESCHEDULE_TO_TIMEOUT_RATIO));
                List<BackupCompressionJobVO> lostJobs = backupCompressionJobDao.listExecutingJobsByHostsAndStartTimeBefore(hostVOS.stream().map(HostVO::getId).toArray(),
                        date.getTime());
                if (lostJobs.isEmpty()) {
                    logger.debug("Found no compression jobs to reschedule for cluster [{}].", clusterVO.getUuid());
                    continue;
                }
                logger.debug("Found [{}] compression jobs to reschedule for cluster [{}]. Processing them as failures and rescheduling them.", lostJobs.size(), clusterVO.getUuid());
                lostJobs.forEach(job -> processJobResult(job, false));
            }
        }
    }

    /**
     * Submit StartCompression jobs, this should be called after submitFinalizeJobsForExecution.
     * */
    private void submitStartJobsForExecution(List<BackupCompressionJobVO> jobsToExecute, List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList,
            HashSet<Long> instancesWithFinalizingJobs, long zoneId) {
        for (BackupCompressionJobVO job : jobsToExecute) {
            if (hostAndNumberOfJobsPairList.isEmpty()) {
                logger.debug("There are no more available hosts to send [{}] jobs. Will try to submit them later.", BackupCompressionJobType.StartCompression);
                return;
            }

            if (instancesWithFinalizingJobs.contains(job.getInstanceId())) {
                VirtualMachine vm = instanceDao.findById(job.getInstanceId());
                logger.debug("Instance [{}] has a finalize compression job running, will not schedule a compression job for it now.", vm.getUuid());
                continue;
            }

            String logId = UuidUtils.first(UUID.randomUUID().toString());
            logger.debug("Dispatching backup compression job [{}{}] with logid:{} for backup [{}].", COMPRESSION_JOB, job.getId(), logId, job.getBackupId());

            Pair<HostVO, Long> hostAndNumberOfJobs;
            hostAndNumberOfJobs = hostAndNumberOfJobsPairList.remove(0);
            hostAndNumberOfJobs.second(hostAndNumberOfJobs.second()+1);
            job.setHostId(hostAndNumberOfJobs.first().getId());
            job.setStartTime(DateUtil.now());
            backupCompressionJobDao.update(job);

            executor.submit(() -> startBackupCompression(job, zoneId, logId));

            Integer maxJobsPerHost = backupCompressionMaxConcurrentCompressionsPerHost.valueIn(hostAndNumberOfJobs.first().getClusterId());
            if (hostAndNumberOfJobs.second() < maxJobsPerHost || maxJobsPerHost < 0) {
                hostAndNumberOfJobsPairList.add(hostAndNumberOfJobs);
                hostAndNumberOfJobsPairList.sort(Comparator.comparing(Pair::second));
            }
        }
    }

    /**
     * Submit FinalizeCompression jobs, this should be called before submitStartJobsForExecution.
     * */
    private HashSet<Long> submitFinalizeJobsForExecution(List<BackupCompressionJobVO> jobsToExecute, List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList, long zoneId) {
        List<BackupCompressionJobVO> submittedJobs = new ArrayList<>();
        HashSet<Long> setOfInstancesWithExecutingCompressionJobs = new HashSet<>();
        for (BackupCompressionJobVO job : jobsToExecute) {
            if (job.getType() != BackupCompressionJobType.FinalizeCompression) {
                continue;
            }
            submittedJobs.add(job);
            String logId = UuidUtils.first(UUID.randomUUID().toString());
            logger.debug("Dispatching backup compression job [{}{}] with logid:{} for backup [{}].", COMPRESSION_JOB, job.getId(), logId, job.getBackupId());

            Pair<HostVO, Long> hostAndNumberOfJobs = hostAndNumberOfJobsPairList.get((int) (Math.random()*hostAndNumberOfJobsPairList.size()));
            job.setHostId(hostAndNumberOfJobs.first().getId());
            job.setStartTime(DateUtil.now());
            backupCompressionJobDao.update(job);

            setOfInstancesWithExecutingCompressionJobs.add(job.getInstanceId());
            executor.submit(() -> finalizeBackupCompression(job, zoneId, logId));
        }
        jobsToExecute.removeAll(submittedJobs);
        return setOfInstancesWithExecutingCompressionJobs;
    }

    private void startBackupCompression(BackupCompressionJobVO job, long zoneId, String logId) {
        boolean result = false;
        try {
            ThreadContext.push(COMPRESSION_JOB + job.getId());
            ThreadContext.put(LOGCONTEXTID, logId);
            result = nativeBackupService.startBackupCompression(job.getBackupId(), job.getHostId(), zoneId);
        } catch (Exception e) {
            logger.error("Caught exception [{}] while trying to compress backup [{}].", e.getMessage(), job.getBackupId(), e);
        } finally {
            processJobResult(job, result);
            ThreadContext.clearAll();
        }
    }

    private void finalizeBackupCompression(BackupCompressionJobVO job, long zoneId, String logId) {
        boolean result = false;
        try {
            ThreadContext.push(COMPRESSION_JOB + job.getId());
            ThreadContext.put(LOGCONTEXTID, logId);
            result = nativeBackupService.finalizeBackupCompression(job.getBackupId(), job.getHostId(), zoneId);
        } catch (Exception e) {
            logger.error("Caught exception [{}] while trying to finalize backup compression [{}].", e.getMessage(), job.getBackupId(), e);
        } finally {
            processJobResult(job, result);
            ThreadContext.clearAll();
        }
    }

    private void processJobResult(BackupCompressionJobVO job, boolean result) {
        job.setAttempts(job.getAttempts() + 1);
        if (result) {
            logger.debug("Compression job [{}] finished with success. Removing it from queue.", job);
            job.setRemoved(DateUtil.now());
            backupCompressionJobDao.update(job);
            return;
        }

        BackupVO backupVO = backupDao.findByIdIncludingRemoved(job.getBackupId());
        if (backupVO.getRemoved() != null) {
            logger.debug("Backup [{}] is marked as removed. Will not reschedule the compression job for it.", backupVO);
            job.setRemoved(DateUtil.now());
            backupCompressionJobDao.update(job);
            return;
        }

        HostVO hostVO = hostDao.findById(job.getHostId());
        int maxAttempts = backupCompressionMaxJobRetries.valueIn(hostVO.getClusterId());
        if (job.getAttempts() >= maxAttempts) {
            logger.debug("Compression job [{}] reached the maximum amount of attempts [{}]. Removing it from queue.", job, maxAttempts);
            job.setRemoved(DateUtil.now());
            backupCompressionJobDao.update(job);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, backupCompressionRetryInterval.valueIn(hostVO.getClusterId()));
        job.setScheduledStartTime(calendar.getTime());
        job.setStartTime(null);
        job.setHostId(null);
        logger.debug("Compression job [{}] failed. Scheduling it to retry at [{}].", job, job.getScheduledStartTime());
        backupCompressionJobDao.update(job);
    }

    @Override
    public String getConfigComponentName() {
        return BackupCompressionService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {backupCompressionMaxConcurrentCompressionsPerHost, backupCompressionMaxJobRetries, backupCompressionRetryInterval, backupCompressionTaskEnabled};
    }
}