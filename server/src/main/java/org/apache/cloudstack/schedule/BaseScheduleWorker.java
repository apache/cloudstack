/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.schedule;

import com.cloud.api.ApiGsonHelper;
import com.cloud.event.ActionEventUtils;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.GlobalLock;
import com.google.common.primitives.Longs;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDao;
import org.apache.cloudstack.schedule.dao.ResourceScheduledJobDao;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.scheduling.support.CronExpression;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Base class for per-resource-type schedule workers.
 * Each subclass owns a dedicated {@link Timer} and {@link GlobalLock} keyed by
 * its resource type, so VM scheduling and AutoScale scheduling (for example) run
 * independently and cannot block each other.
 */
public abstract class BaseScheduleWorker extends ManagerBase {

    public static final ConfigKey<Integer> ScheduledJobExpireInterval = new ConfigKey<>(
            ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "scheduler.jobs.expire.interval", "30",
            "Scheduled job expiry interval in days (applies to all resource-type schedulers)", true);

    @Inject
    protected ResourceScheduleDao resourceScheduleDao;
    @Inject
    protected ResourceScheduledJobDao resourceScheduledJobDao;
    @Inject
    protected AsyncJobManager asyncJobManager;

    protected AsyncJobDispatcher asyncJobDispatcher;
    private Timer schedulerTimer;
    protected Date currentTimestamp;

    public AsyncJobDispatcher getAsyncJobDispatcher() {
        return asyncJobDispatcher;
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        asyncJobDispatcher = dispatcher;
    }

    /** The API resource type this worker handles (e.g. {@code ApiCommandResourceType.VirtualMachine}). */
    public abstract ApiCommandResourceType getApiResourceType();

    /** Convenience method returning {@code getApiResourceType().name()} for use in DAO queries, locks, and logging. */
    protected final String getResourceTypeName() {
        return getApiResourceType().name();
    }

    /**
     * Execute the action described by {@code job} against the owning resource.
     *
     * @return the async-job id, or {@code null} if the job was skipped.
     */
    protected abstract Long processJob(ResourceScheduledJobVO job);

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public boolean start() {
        currentTimestamp = DateUtils.addMinutes(new Date(), 1);
        scheduleNextJobs(currentTimestamp);

        final TimerTask pollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    poll(new Date());
                } catch (final Throwable t) {
                    logger.warn("Uncaught throwable in {} scheduler", getResourceTypeName(), t);
                }
            }
        };

        schedulerTimer = new Timer(getResourceTypeName() + "SchedulerPollTask");
        schedulerTimer.scheduleAtFixedRate(pollTask, 5000L, 60 * 1000L);
        return true;
    }

    @Override
    public boolean stop() {
        if (schedulerTimer != null) {
            schedulerTimer.cancel();
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Poll loop (identical structure for every resource type)
    // -------------------------------------------------------------------------

    public void poll(Date timestamp) {
        currentTimestamp = DateUtils.round(timestamp, Calendar.MINUTE);
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        logger.debug("{} scheduler poll at {}", getResourceTypeName(), displayTime);

        GlobalLock scanLock = GlobalLock.getInternLock("resourceScheduler.poll." + getResourceTypeName());
        try {
            if (scanLock.lock(30)) {
                try {
                    scheduleNextJobs(currentTimestamp);
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        scanLock = GlobalLock.getInternLock("resourceScheduler.poll." + getResourceTypeName());
        try {
            if (scanLock.lock(30)) {
                try {
                    startJobs();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        try {
            cleanupScheduledJobs();
        } catch (Exception e) {
            logger.warn("Error cleaning up scheduled jobs for {}", getResourceTypeName(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Scheduling helpers
    // -------------------------------------------------------------------------

    private void scheduleNextJobs(Date timestamp) {
        for (ResourceScheduleVO schedule : resourceScheduleDao.listAllActiveSchedules(getApiResourceType())) {
            try {
                scheduleNextJob(schedule, timestamp);
            } catch (Exception e) {
                logger.warn("Error scheduling next job for schedule {}", schedule, e);
            }
        }
    }

    public Date scheduleNextJob(ResourceScheduleVO schedule, Date timestamp) {
        if (!schedule.getEnabled()) {
            logger.debug("Schedule {} is disabled. Skipping.", schedule);
            return null;
        }

        CronExpression cron = DateUtil.parseSchedule(schedule.getSchedule());
        Date startDate = schedule.getStartDate();
        Date endDate = schedule.getEndDate();

        if (!isResourceValid(schedule.getResourceId())) {
            logger.info("Resource id={} is no longer valid. Disabling schedule {}.", schedule.getResourceId(), schedule);
            schedule.setEnabled(false);
            resourceScheduleDao.persist(schedule);
            return null;
        }

        ZonedDateTime now = (timestamp != null)
                ? ZonedDateTime.ofInstant(timestamp.toInstant(), schedule.getTimeZoneId())
                : ZonedDateTime.now(schedule.getTimeZoneId());
        ZonedDateTime zonedStart = ZonedDateTime.ofInstant(startDate.toInstant(), schedule.getTimeZoneId());
        ZonedDateTime zonedEnd = (endDate != null)
                ? ZonedDateTime.ofInstant(endDate.toInstant(), schedule.getTimeZoneId())
                : null;

        if (zonedEnd != null && now.isAfter(zonedEnd)) {
            logger.info("End time has passed. Disabling schedule {}.", schedule);
            schedule.setEnabled(false);
            resourceScheduleDao.persist(schedule);
            return null;
        }

        ZonedDateTime ts = zonedStart.isAfter(now) ? cron.next(zonedStart) : cron.next(now);
        if (ts == null) {
            logger.info("No next schedule time found. Disabling schedule {}.", schedule);
            schedule.setEnabled(false);
            resourceScheduleDao.persist(schedule);
            return null;
        }

        Date scheduledDateTime = Date.from(ts.toInstant());
        ResourceScheduledJobVO existingJob = resourceScheduledJobDao.findByScheduleAndTimestamp(schedule.getId(), scheduledDateTime);
        if (existingJob != null) {
            logger.trace("Job already scheduled for {} at {}", schedule, scheduledDateTime);
            return scheduledDateTime;
        }

        ResourceScheduledJobVO job = new ResourceScheduledJobVO(
                getApiResourceType(), schedule.getResourceId(), schedule.getId(),
                schedule.getActionName(), scheduledDateTime);
        try {
            resourceScheduledJobDao.persist(job);
            long accountId = getEntityOwnerId(schedule.getResourceId());
            ActionEventUtils.onScheduledActionEvent(
                    User.UID_SYSTEM, accountId,
                    parseAction(schedule.getActionName()).getEventType(),
                    String.format("Scheduled action (%s) [resource: %d, schedule: %s] at %s",
                            schedule.getActionName(), schedule.getResourceId(), schedule, scheduledDateTime),
                    schedule.getResourceId(), getResourceTypeName(), true, 0);
        } catch (EntityExistsException e) {
            logger.debug("Job already scheduled (concurrent insert).");
        }
        return scheduledDateTime;
    }

    public void updateScheduledJob(ResourceScheduleVO schedule) {
        removeScheduledJobs(Longs.asList(schedule.getId()));
        scheduleNextJob(schedule, new Date());
    }

    public void removeScheduledJobs(List<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return;
        }
        int removed = resourceScheduledJobDao.expungeJobsForSchedules(scheduleIds, new Date());
        logger.debug("Removed {} scheduled jobs for schedules {}", removed, scheduleIds);
    }

    // -------------------------------------------------------------------------
    // Job execution
    // -------------------------------------------------------------------------

    private void startJobs() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        List<ResourceScheduledJobVO> jobs = resourceScheduledJobDao.listJobsToStart(getApiResourceType(), currentTimestamp);
        logger.debug("Got {} scheduled jobs for {} at {}", jobs.size(), getResourceTypeName(), displayTime);

        Map<Long, ResourceScheduledJobVO> toExecute = new HashMap<>();
        Map<Long, List<ResourceScheduledJobVO>> toSkip = new HashMap<>();

        for (ResourceScheduledJobVO job : jobs) {
            long resourceId = job.getResourceId();
            if (toExecute.get(resourceId) == null) {
                toExecute.put(resourceId, job);
            } else {
                toSkip.computeIfAbsent(resourceId, k -> new ArrayList<>()).add(job);
            }
        }

        executeJobs(toExecute);
        logSkippedJobs(toExecute, toSkip);
    }

    public void executeJobs(Map<Long, ResourceScheduledJobVO> jobsToExecute) {
        for (Map.Entry<Long, ResourceScheduledJobVO> entry : jobsToExecute.entrySet()) {
            ResourceScheduledJobVO job = entry.getValue();
            ResourceScheduledJobVO locked = null;
            try {
                locked = resourceScheduledJobDao.acquireInLockTable(job.getId());
                Long jobId = processJob(job);
                if (jobId != null) {
                    locked.setAsyncJobId(jobId);
                    resourceScheduledJobDao.update(job.getId(), locked);
                }
            } catch (Exception e) {
                logger.warn("Failed executing scheduled job {}", job, e);
            } finally {
                if (locked != null) {
                    resourceScheduledJobDao.releaseFromLockTable(job.getId());
                }
            }
        }
    }

    private void logSkippedJobs(Map<Long, ResourceScheduledJobVO> executed,
                                 Map<Long, List<ResourceScheduledJobVO>> skipped) {
        for (Map.Entry<Long, List<ResourceScheduledJobVO>> entry : skipped.entrySet()) {
            long resourceId = entry.getKey();
            ResourceScheduledJobVO running = executed.get(resourceId);
            for (ResourceScheduledJobVO s : entry.getValue()) {
                logger.info("Skipping job {} for resource {} — conflict with {}", s, resourceId, running);
            }
        }
    }

    private void cleanupScheduledJobs() {
        Date deleteBeforeDate = DateUtils.addDays(currentTimestamp,
                -1 * ScheduledJobExpireInterval.value());
        int removed = resourceScheduledJobDao.expungeJobsBefore(getApiResourceType(), deleteBeforeDate);
        logger.info("Cleaned up {} scheduled job entries for {}", removed, getResourceTypeName());
    }

    // -------------------------------------------------------------------------
    // Subclass helpers
    // -------------------------------------------------------------------------

    /** Returns true when the given resource ID is valid and eligible for scheduling. */
    public abstract boolean isResourceValid(long resourceId);

    /** Returns the account that owns the given resource (for ACL / event attribution). */
    public abstract long getEntityOwnerId(long resourceId);

    /** Parses an action string into the resource-type-specific typed action constant. Throws InvalidParameterValueException for unknown values. */
    public abstract ResourceSchedule.Action parseAction(String action);

    /** Validates action-specific detail parameters. Throws InvalidParameterValueException on failure. */
    public abstract void validateDetails(ResourceSchedule.Action action, Map<String, String> details);

    /**
     * Submits an async job for the given command class.
     *
     * @param cmdClass   the command to dispatch
     * @param accountId  account submitting the job
     * @param resourceId primary resource id (written to AsyncJobVO.instanceId)
     * @param eventId    the start-event id for correlation
     * @param extra      additional parameters (e.g. "forced" -> "true")
     * @return the async job id
     */
    public <T extends BaseCmd> long submitAsyncJob(
            Class<T> cmdClass, long accountId, long resourceId, long eventId,
            Map<String, String> extra) {
        Map<String, String> params = new HashMap<>(extra);
        params.put(ApiConstants.ID, String.valueOf(resourceId));
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", String.valueOf(accountId));
        params.put(ApiConstants.CTX_START_EVENT_ID, String.valueOf(eventId));

        T cmd;
        try {
            cmd = cmdClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + cmdClass.getName(), e);
        }
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, accountId,
                cmdClass.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params),
                resourceId,
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null,
                null);
        job.setDispatcher(asyncJobDispatcher.getName());
        return asyncJobManager.submitAsyncJob(job);
    }
}
