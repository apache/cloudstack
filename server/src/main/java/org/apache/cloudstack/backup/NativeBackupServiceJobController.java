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
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.NativeBackupServiceJobDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.logging.log4j.ThreadContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Abstract class that implements most of the native backup services logic. Classes that implement this one should only implement service specific logic.
 * */
public abstract class NativeBackupServiceJobController extends ManagerBase {

    protected static final String LOGCONTEXTID = "logcontextid";

    @Inject
    protected AsyncJobManager asyncJobManager;

    @Inject
    protected ClusterDao clusterDao;

    @Inject
    protected DataCenterDao dataCenterDao;

    @Inject
    protected HostDao hostDao;

    @Inject
    protected BackupDao backupDao;

    @Inject
    private VMInstanceDao instanceDao;

    @Inject
    protected NativeBackupServiceJobDao nativeBackupServiceJobDao;

    protected String controllerType = "abstract";

    protected static final String BACKUP_JOB = "backup-service-job-";

    protected static final double RESCHEDULE_TO_TIMEOUT_RATIO = 2.5;

    protected void run() {
        ThreadContext.put(LOGCONTEXTID, UuidUtils.first(UUID.randomUUID().toString()));
        logger.debug("Searching for {} jobs to dispatch.", controllerType);

        if (!asyncJobManager.isAsyncJobsEnabled()) {
            logger.debug("A management shutdown has been triggered. Not running {} task.", controllerType);
            return;
        }

        Transaction.execute(TransactionLegacy.CLOUD_DB, new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                searchAndDispatchJobs();
            }
        });
    }

    /**
     * Reschedule jobs that seem to be stuck. This should run even if the native backup service task is disabled, so that stuck jobs get cleaned if necessary.
     * */
    protected void rescheduleLostJobs() {
        for (DataCenterVO dataCenterVO : dataCenterDao.listAllZones()) {
            logger.debug("Searching lost {} jobs to reschedule in zone [{}].", controllerType, dataCenterVO.getUuid());
            for (ClusterVO clusterVO : clusterDao.listByDcHyType(dataCenterVO.getId(), Hypervisor.HypervisorType.KVM.toString())) {
                List<HostVO> hostVOS = hostDao.findRoutingByClusterId(clusterVO.getId());
                if (hostVOS.isEmpty()) {
                    logger.debug("No hosts found in cluster [{}]. Cannot reschedule jobs for it.", clusterVO.getUuid());
                    continue;
                }
                Calendar date = Calendar.getInstance();
                List<NativeBackupServiceJobVO> lostJobs = getLostJobs(clusterVO, date, hostVOS);
                if (lostJobs.isEmpty()) {
                    logger.debug("Found no {} jobs to reschedule for cluster [{}].", controllerType, clusterVO.getUuid());
                    continue;
                }
                logger.debug("Found [{}] {} jobs to reschedule for cluster [{}]. Processing them as failures and rescheduling them.", lostJobs.size(), controllerType,
                        clusterVO.getUuid());
                lostJobs.forEach(job -> processJobResult(job, false));
            }
        }
    }

    protected void processJobResult(NativeBackupServiceJobVO job, boolean result) {
        job.setAttempts(job.getAttempts() + 1);
        if (result) {
            logger.debug("{} job [{}] finished with success. Removing it from queue.", controllerType, job);
            job.setRemoved(DateUtil.now());
            nativeBackupServiceJobDao.update(job);
            return;
        }

        BackupVO backupVO = backupDao.findByIdIncludingRemoved(job.getBackupId());
        if (backupVO.getRemoved() != null) {
            logger.debug("Backup [{}] is marked as removed. Will not reschedule the {} job for it.", backupVO, controllerType);
            job.setRemoved(DateUtil.now());
            nativeBackupServiceJobDao.update(job);
            return;
        }

        int maxAttempts = getMaxAttempts(job);
        if (job.getAttempts() >= maxAttempts) {
            logger.debug("{} job [{}] reached the maximum amount of attempts [{}]. Removing it from queue.", controllerType, job, maxAttempts);
            job.setRemoved(DateUtil.now());
            nativeBackupServiceJobDao.update(job);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MINUTE, getRetryInterval(job));
        job.setScheduledStartTime(calendar.getTime());
        job.setStartTime(null);
        job.setHostId(null);
        logger.debug("{} job [{}] failed. Scheduling it to retry at [{}].", controllerType, job, job.getScheduledStartTime());
        nativeBackupServiceJobDao.update(job);
    }

    /**
     * Goes through all the executing jobs for the zone and returns a map from up and enabled KVM hosts to the number of executing jobs.
     */
    protected Pair<HashMap<HostVO, Long>, Integer> getHostToNumberOfExecutingJobsAndTotalExecutingJobs(DataCenterVO zone, NativeBackupServiceJobType... jobTypes) {
        List<HostVO> allKvmHostsForZone = hostDao.listAllRoutingHostsByZoneAndHypervisorType(zone.getId(), Hypervisor.HypervisorType.KVM);
        HashMap<HostVO, Long> hostToNumberOfExecutingJobs = new HashMap<>();

        for (HostVO host : allKvmHostsForZone) {
            if (host.getStatus() == Status.Up && host.getResourceState() == ResourceState.Enabled) {
                hostToNumberOfExecutingJobs.put(host, 0L);
            }
        }

        List<NativeBackupServiceJobVO> executingJobs = nativeBackupServiceJobDao.listExecutingJobsByZoneIdAndJobType(zone.getId(), jobTypes);
        for (NativeBackupServiceJobVO executingJob : executingJobs) {
            HostVO host = allKvmHostsForZone.stream().filter(hostVO -> hostVO.getId() == executingJob.getHostId()).findFirst().orElse(null);
            if (host == null) {
                logger.error("{} job [{}] is running in an unknown host. This job will be rescheduled in the future.", controllerType, executingJob);
                continue;
            } else if (host.getStatus() != Status.Up || host.getResourceState() != ResourceState.Enabled) {
                logger.warn("{} job [{}] is running in host [{}], which is not up or not enabled. If possible, wait for the job to finish before restarting the Agent.",
                        controllerType, executingJob, host);
                continue;
            }

            hostToNumberOfExecutingJobs.computeIfPresent(host, (hostVO, numberOfJobs) -> numberOfJobs + 1);
        }

        return new Pair<>(hostToNumberOfExecutingJobs, executingJobs.size());
    }

    protected List<NativeBackupServiceJobVO> thinJobsToStartList(DataCenterVO zone, List<NativeBackupServiceJobVO> jobsToStart, Integer totalExecutingJobs,
            ConfigKey<Integer> jobsPerZoneConfiguration) {
        Integer maxConcurrentJobsInTheZone = jobsPerZoneConfiguration.valueIn(zone.getId());
        if (maxConcurrentJobsInTheZone < 0) {
            return jobsToStart;
        }
        logger.debug("Since [{}] is set to [{}]. We will only execute up to [{}] at the same time. We already have [{}] executing jobs.",
                jobsPerZoneConfiguration.toString(), maxConcurrentJobsInTheZone, maxConcurrentJobsInTheZone, totalExecutingJobs);
        if (maxConcurrentJobsInTheZone >= totalExecutingJobs) {
            logger.debug("We are already executing the maximum amount of jobs in this zone, we will not execute any new jobs.");
            return List.of();
        }
        return jobsToStart.subList(0, maxConcurrentJobsInTheZone);
    }

    protected List<Pair<HostVO, Long>> filterHostsWithTooManyJobs(HashMap<HostVO, Long> hostToNumberOfExecutingJobs, ConfigKey<Integer> jobsPerHostConfiguration) {
        List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList = new ArrayList<>();
        for (HostVO host : hostToNumberOfExecutingJobs.keySet()) {
            Long numberOfJobs = hostToNumberOfExecutingJobs.get(host);
            hostDao.loadDetails(host);
            Integer maxConcurrentCompressionsPerHost = getMaxConcurrentCompressionsPerHost(jobsPerHostConfiguration, host);
            if (maxConcurrentCompressionsPerHost > 0 && numberOfJobs >= maxConcurrentCompressionsPerHost) {
                logger.debug("Host [{}] is already executing the maximum number of concurrent compression jobs set in [{}]. Current number of jobs being executed is " +
                        "[{}], the value for the configuration is [{}].", host, jobsPerHostConfiguration.toString(), numberOfJobs, maxConcurrentCompressionsPerHost);
                continue;
            }
            hostAndNumberOfJobsPairList.add(new Pair<>(host, numberOfJobs));
        }
        return hostAndNumberOfJobsPairList;
    }

    private Integer getMaxConcurrentCompressionsPerHost(ConfigKey<Integer> jobsPerHostConfiguration, HostVO host) {
        if (host.getDetail(jobsPerHostConfiguration.key()) != null) {
            return Integer.valueOf(host.getDetail(jobsPerHostConfiguration.key()));
        } else {
            return jobsPerHostConfiguration.valueIn(host.getClusterId());
        }

    }

    /**
     * Submit StartCompression jobs, this should be called after submitFinalizeJobsForExecution.
     * */
    protected void submitQueuedJobsForExecution(List<NativeBackupServiceJobVO> jobsToExecute, List<Pair<HostVO, Long>> hostAndNumberOfJobsPairList,
            Set<Long> busyInstances, ConfigKey<Integer> maxJobPerHostConfig, long zoneId) {
        for (NativeBackupServiceJobVO job : jobsToExecute) {
            if (hostAndNumberOfJobsPairList.isEmpty()) {
                logger.debug("There are no more available hosts to send [{}] jobs. Will try to submit them later.", job.getType());
                return;
            }

            if (busyInstances.contains(job.getInstanceId())) {
                VirtualMachine vm = instanceDao.findById(job.getInstanceId());
                logger.debug("Instance [{}] has another backup service job running, will not schedule a {} job for it now.", vm.getUuid(), controllerType);
                continue;
            }

            String logId = UuidUtils.first(UUID.randomUUID().toString());
            logger.debug("Dispatching backup {} job [{}{}] with logid:{} for backup [{}].", controllerType, BACKUP_JOB, job.getId(), logId, job.getBackupId());

            Pair<HostVO, Long> hostAndNumberOfJobs;
            hostAndNumberOfJobs = hostAndNumberOfJobsPairList.remove(0);
            hostAndNumberOfJobs.second(hostAndNumberOfJobs.second()+1);
            job.setHostId(hostAndNumberOfJobs.first().getId());
            job.setStartTime(DateUtil.now());
            nativeBackupServiceJobDao.update(job);

            submitQueuedJob(job, zoneId, logId);

            Integer maxJobsPerHost = getMaxConcurrentCompressionsPerHost(maxJobPerHostConfig, hostAndNumberOfJobs.first());
            if (hostAndNumberOfJobs.second() < maxJobsPerHost || maxJobsPerHost < 0) {
                hostAndNumberOfJobsPairList.add(hostAndNumberOfJobs);
                hostAndNumberOfJobsPairList.sort(Comparator.comparing(Pair::second));
            }
            busyInstances.add(job.getInstanceId());
        }
    }

    protected void submitQueuedJob(NativeBackupServiceJobVO job, long zoneId, String logId) {
    }

    /**
     * Implementing classes should override this if they want jobs to be tried more than once.
     * */
    protected int getMaxAttempts(NativeBackupServiceJobVO jobVo) {
        return 1;
    }

    /**
     * Implementing classes should override this if the jobs execute more than once. Otherwise, you can leave it as is.
     * */
    protected int getRetryInterval(NativeBackupServiceJobVO jobVo) {
        return -1;
    }

    /**
     * Implementing classes should override this so their lost jobs are caught and rescheduled, otherwise lost jobs will be left as is.
     * */
    protected List<NativeBackupServiceJobVO> getLostJobs(ClusterVO clusterVO, Calendar date, List<HostVO> hostVOS) {
        return List.of();
    }

    protected void searchAndDispatchJobs() {
    }
}
