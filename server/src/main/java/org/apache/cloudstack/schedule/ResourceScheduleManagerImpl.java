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
package org.apache.cloudstack.schedule;

import com.cloud.api.query.MutualExclusiveIdsManagerBase;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.AccountManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.command.user.schedule.CreateResourceScheduleCmd;
import org.apache.cloudstack.api.command.user.schedule.DeleteResourceScheduleCmd;
import org.apache.cloudstack.api.command.user.schedule.ListResourceScheduleCmd;
import org.apache.cloudstack.api.command.user.schedule.UpdateResourceScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.CreateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.DeleteVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMScheduleCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDao;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDetailsDao;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronExpression;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class ResourceScheduleManagerImpl extends MutualExclusiveIdsManagerBase implements ResourceScheduleManager, PluggableService, Configurable {

    @Inject
    private ResourceScheduleDao resourceScheduleDao;

    @Inject
    private ResourceScheduleDetailsDao resourceScheduleDetailsDao;

    @Inject
    private AccountManager accountManager;

    @Inject
    private EntityManager entityManager;

    @Inject
    private List<BaseScheduleWorker> workerList;

    private Map<ApiCommandResourceType, BaseScheduleWorker> workerMap;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        workerMap = new HashMap<>();
        if (workerList != null) {
            for (BaseScheduleWorker worker : workerList) {
                workerMap.put(worker.getApiResourceType(), worker);
            }
        }
        return super.configure(name, params);
    }

    private BaseScheduleWorker getWorker(ApiCommandResourceType resourceType) {
        BaseScheduleWorker worker = workerMap.get(resourceType);
        if (worker == null) {
            throw new InvalidParameterValueException("Scheduling is not supported for resource type: " + resourceType);
        }
        return worker;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateVMScheduleCmd.class);
        cmdList.add(ListVMScheduleCmd.class);
        cmdList.add(UpdateVMScheduleCmd.class);
        cmdList.add(DeleteVMScheduleCmd.class);
        cmdList.add(CreateResourceScheduleCmd.class);
        cmdList.add(ListResourceScheduleCmd.class);
        cmdList.add(UpdateResourceScheduleCmd.class);
        cmdList.add(DeleteResourceScheduleCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return ResourceScheduleManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                BaseScheduleWorker.ScheduledJobExpireInterval
        };
    }

    // Helper to resolve UUID string to internal ID
    private long resolveResourceId(String resourceIdStr, Class<?> entityClass) {
        if (entityClass == null) {
            throw new CloudRuntimeException("Entity class is required to resolve resource ID");
        }
        Object obj = entityManager.findByUuid(entityClass, resourceIdStr);
        if (obj == null) {
            try {
                long id = Long.parseLong(resourceIdStr);
                obj = entityManager.findById(entityClass, id);
                if (obj == null) {
                    throw new InvalidParameterValueException("Unable to find resource by id " + resourceIdStr);
                }
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("Unable to find resource by id " + resourceIdStr);
            }
        }
        return ((InternalIdentity) obj).getId();
    }

    private String getResourceUuid(long internalId, Class<?> entityClass) {
        if (entityClass != null) {
            Object obj = entityManager.findById(entityClass, internalId);
            if (obj instanceof Identity) {
                return ((Identity) obj).getUuid();
            }
        }
        return String.valueOf(internalId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SCHEDULE_CREATE, eventDescription = "Creating Resource Schedule", create = true)
    public ResourceScheduleResponse createSchedule(ApiCommandResourceType resourceType, String resourceUuid, String description,
                                                   String schedule, String timeZoneStr, String action,
                                                   Date cmdStartDate, Date cmdEndDate, boolean enabled,
                                                   Map<String, String> details) {
        BaseScheduleWorker worker = getWorker(resourceType);

        long internalResourceId = resolveResourceId(resourceUuid, worker.getApiResourceType().getAssociatedClass());

        if (!worker.isResourceValid(internalResourceId)) {
            throw new InvalidParameterValueException("Invalid or non-existent resource: " + resourceUuid);
        }

        long ownerId = worker.getEntityOwnerId(internalResourceId);
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, accountManager.getAccount(ownerId));

        ResourceSchedule.Action parsedAction = worker.parseAction(action);

        worker.validateDetails(parsedAction, details);

        TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
        String timeZoneId = timeZone.getID();
        Date startDate = DateUtils.addMinutes(new Date(), 1);
        if (cmdStartDate != null) {
            startDate = Date.from(DateUtil.getZoneDateTime(cmdStartDate, timeZone.toZoneId()).toInstant());
        }
        Date endDate = null;
        if (cmdEndDate != null) {
            endDate = Date.from(DateUtil.getZoneDateTime(cmdEndDate, timeZone.toZoneId()).toInstant());
        }

        CronExpression cronExpression = DateUtil.parseSchedule(schedule);
        validateStartDateEndDate(startDate, endDate, timeZone);

        if (StringUtils.isBlank(description)) {
            description = worker.getDescription(parsedAction, cronExpression, details);
        }

        logger.warn("Using timezone [{}] for running the schedule for resource [{}], as an equivalent of [{}].",
                timeZoneId, resourceUuid, timeZoneStr);

        String finalDescription = description;
        String finalAction = parsedAction.name();
        Date finalStartDate = startDate;
        Date finalEndDate = endDate;

        return Transaction.execute((TransactionCallback<ResourceScheduleResponse>) status -> {
            ResourceScheduleVO scheduleVO = resourceScheduleDao.persist(new ResourceScheduleVO(
                    resourceType, internalResourceId,
                    finalDescription, cronExpression.toString(), timeZoneId,
                    finalAction, finalStartDate, finalEndDate, enabled));

            if (MapUtils.isNotEmpty(details)) {
                List<ResourceScheduleDetailVO> detailVOs = new ArrayList<>();
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailVOs.add(new ResourceScheduleDetailVO(scheduleVO.getId(), entry.getKey(), entry.getValue(), true));
                }
                resourceScheduleDetailsDao.saveDetails(detailVOs);
            }

            worker.scheduleNextJob(scheduleVO, new Date());

            CallContext.current().setEventResourceId(internalResourceId);
            CallContext.current().setEventResourceType(worker.getApiResourceType());
            CallContext.current().setEventDetails(String.format("Created resource schedule %s", scheduleVO));
            return createResponse(scheduleVO, details);
        });
    }

    ResourceScheduleResponse createResponse(ResourceSchedule schedule, Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            details = resourceScheduleDetailsDao.listDetailsKeyPairs(schedule.getId(), true);
        }

        BaseScheduleWorker worker = getWorker(schedule.getResourceType());

        ResourceScheduleResponse response = new ResourceScheduleResponse();
        response.setObjectName("resourceschedule");
        response.setId(schedule.getUuid());
        response.setResourceType(schedule.getResourceType());

        String uuid = getResourceUuid(schedule.getResourceId(), worker.getApiResourceType().getAssociatedClass());
        response.setResourceId(uuid);

        response.setDescription(schedule.getDescription());
        response.setSchedule(schedule.getSchedule());
        response.setTimeZone(schedule.getTimeZone());
        response.setAction(worker.parseAction(schedule.getActionName()));
        response.setEnabled(schedule.getEnabled());
        response.setStartDate(schedule.getStartDate());
        response.setEndDate(schedule.getEndDate());
        response.setDetails(details);
        response.setCreated(schedule.getCreated());
        return response;
    }

    @Override
    public ListResponse<ResourceScheduleResponse> listSchedule(Long id, List<Long> ids, ApiCommandResourceType resourceType,
                                                               String resourceUuid, String action, Boolean enabled,
                                                               Long startIndex, Long pageSize) {
        Long internalResourceId = null;
        BaseScheduleWorker worker = getWorker(resourceType);
        if (StringUtils.isBlank(resourceUuid)) {
            throw new InvalidParameterValueException("Resource ID must be specified");
        } else {
            internalResourceId = resolveResourceId(resourceUuid, worker.getApiResourceType().getAssociatedClass());
            long ownerId = worker.getEntityOwnerId(internalResourceId);
            accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, accountManager.getAccount(ownerId));
        }

        List<Long> scheduleIds = getIdsListFromCmd(id, ids);
        if (action != null) {
            action = worker.parseAction(action).name();
        }

        Pair<List<ResourceScheduleVO>, Integer> result = resourceScheduleDao.searchAndCount(
                scheduleIds, resourceType, internalResourceId, action, enabled, startIndex, pageSize);

        ListResponse<ResourceScheduleResponse> response = new ListResponse<>();
        List<ResourceScheduleResponse> responsesList = new ArrayList<>();
        for (ResourceScheduleVO schedule : result.first()) {
            responsesList.add(createResponse(schedule, null));
        }
        response.setResponses(responsesList, result.second());
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SCHEDULE_UPDATE, eventDescription = "Updating Resource Schedule")
    public ResourceScheduleResponse updateSchedule(Long id, String description, String schedule,
                                                   String timeZoneStr, Date cmdStartDate, Date cmdEndDate,
                                                   Boolean enabled, Map<String, String> details) {
        ResourceScheduleVO scheduleVO = resourceScheduleDao.findById(id);

        if (scheduleVO == null) {
            throw new CloudRuntimeException("Resource schedule doesn't exist");
        }

        BaseScheduleWorker worker = getWorker(scheduleVO.getResourceType());
        long ownerId = worker.getEntityOwnerId(scheduleVO.getResourceId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, accountManager.getAccount(ownerId));

        ResourceSchedule.Action parsedAction = worker.parseAction(scheduleVO.getActionName());
        if (MapUtils.isNotEmpty(details)) {
            worker.validateDetails(parsedAction, details);
        }

        CronExpression cronExpression = Objects.requireNonNullElse(
                DateUtil.parseSchedule(schedule),
                DateUtil.parseSchedule(scheduleVO.getSchedule())
        );

        if (description == null && scheduleVO.getDescription() == null) {
            Map<String, String> effectiveDetails = MapUtils.isNotEmpty(details)
                    ? details
                    : resourceScheduleDetailsDao.listDetailsKeyPairs(id, true);
            description = worker.getDescription(parsedAction, cronExpression, effectiveDetails);
        }

        final String originalTimeZone = scheduleVO.getTimeZone();
        final Date originalStartDate = scheduleVO.getStartDate();
        final Date originalEndDate = scheduleVO.getEndDate();

        TimeZone timeZone;
        String timeZoneId;
        if (timeZoneStr != null) {
            timeZone = TimeZone.getTimeZone(timeZoneStr);
            timeZoneId = timeZone.getID();
            if (!timeZoneId.equals(timeZoneStr)) {
                logger.warn("Using timezone [{}] for running the schedule [{}] for resource {}, as an equivalent of [{}].",
                        timeZoneId, scheduleVO.getSchedule(), scheduleVO.getResourceId(), timeZoneStr);
            }
            scheduleVO.setTimeZone(timeZoneId);
        } else {
            timeZoneId = scheduleVO.getTimeZone();
            timeZone = TimeZone.getTimeZone(timeZoneId);
        }

        Date startDate = scheduleVO.getStartDate().before(DateUtils.addMinutes(new Date(), 1)) ? DateUtils.addMinutes(new Date(), 1) : scheduleVO.getStartDate();
        Date endDate = scheduleVO.getEndDate();
        if (cmdEndDate != null) {
            endDate = Date.from(DateUtil.getZoneDateTime(cmdEndDate, timeZone.toZoneId()).toInstant());
        }

        if (cmdStartDate != null) {
            startDate = Date.from(DateUtil.getZoneDateTime(cmdStartDate, timeZone.toZoneId()).toInstant());
        }

        if (ObjectUtils.anyNotNull(cmdStartDate, cmdEndDate, timeZoneStr) &&
                (!Objects.equals(originalTimeZone, timeZoneId) ||
                        !Objects.equals(originalStartDate, startDate) ||
                        !Objects.equals(originalEndDate, endDate))) {
            validateStartDateEndDate(Objects.requireNonNullElse(startDate, DateUtils.addMinutes(new Date(), 1)),
                    endDate, timeZone);
        }

        if (enabled != null) {
            scheduleVO.setEnabled(enabled);
        }
        if (description != null) {
            scheduleVO.setDescription(description);
        }
        if (cmdEndDate != null) {
            scheduleVO.setEndDate(endDate);
        }
        if (cmdStartDate != null) {
            scheduleVO.setStartDate(startDate);
        }
        scheduleVO.setSchedule(cronExpression.toString());

        return Transaction.execute((TransactionCallback<ResourceScheduleResponse>) status -> {
            resourceScheduleDao.update(id, scheduleVO);

            if (MapUtils.isNotEmpty(details)) {
                List<ResourceScheduleDetailVO> detailVOs = new ArrayList<>();
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailVOs.add(new ResourceScheduleDetailVO(id, entry.getKey(), entry.getValue(), true));
                }
                resourceScheduleDetailsDao.saveDetails(detailVOs);
            }

            worker.updateScheduledJob(scheduleVO);

            CallContext.current().setEventResourceId(scheduleVO.getResourceId());
            CallContext.current().setEventResourceType(worker.getApiResourceType());

            CallContext.current().setEventDetails(String.format("Updated resource schedule %s", scheduleVO));

            // Re-load details if they weren't fully replaced
            Map<String, String> currentDetails = resourceScheduleDetailsDao.listDetailsKeyPairs(id, true);
            return createResponse(scheduleVO, currentDetails);
        });
    }

    void validateStartDateEndDate(Date startDate, Date endDate, TimeZone tz) {
        ZonedDateTime now = ZonedDateTime.now(tz.toZoneId());
        ZonedDateTime zonedStartDate = ZonedDateTime.ofInstant(startDate.toInstant(), tz.toZoneId());

        if (zonedStartDate.isBefore(now)) {
            throw new InvalidParameterValueException(String.format("Invalid value for start date. Start date [%s] can't be before current time [%s].", zonedStartDate, now));
        }

        if (endDate != null) {
            ZonedDateTime zonedEndDate = ZonedDateTime.ofInstant(endDate.toInstant(), tz.toZoneId());
            if (zonedEndDate.isBefore(now)) {
                throw new InvalidParameterValueException(String.format("Invalid value for end date. End date [%s] can't be before current time [%s].", zonedEndDate, now));
            }
            if (zonedEndDate.isBefore(zonedStartDate)) {
                throw new InvalidParameterValueException(String.format("Invalid value for end date. End date [%s] can't be before start date [%s].", zonedEndDate, zonedStartDate));
            }
        }
    }

    @Override
    public void removeSchedulesForResource(ApiCommandResourceType resourceType, long resourceId) {
        List<ResourceScheduleVO> schedules = resourceScheduleDao.search(
                resourceScheduleDao.getSearchCriteriaForResource(resourceType, resourceId), null);
        List<Long> ids = new ArrayList<>();
        for (ResourceScheduleVO schedule : schedules) {
            ids.add(schedule.getId());
        }

        getWorker(resourceType).removeScheduledJobs(ids);
        resourceScheduleDao.removeAllSchedulesForResource(resourceType, resourceId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SCHEDULE_DELETE, eventDescription = "Deleting Resource Schedule")
    public Long removeSchedule(ApiCommandResourceType resourceType, String resourceUuid, Long id, List<Long> idsList) {
        BaseScheduleWorker worker = getWorker(resourceType);
        long internalResourceId = resolveResourceId(resourceUuid, worker.getApiResourceType().getAssociatedClass());

        long ownerId = worker.getEntityOwnerId(internalResourceId);
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, accountManager.getAccount(ownerId));

        List<Long> ids = getIdsListFromCmd(id, idsList);
        Pair<List<ResourceScheduleVO>, Integer> result = resourceScheduleDao.searchAndCount(ids, resourceType, internalResourceId, null, null, null, null);
        List<ResourceScheduleVO> schedulesToRemove = result.first();
        List<Long> scheduleIdsToRemove = schedulesToRemove.stream().map(ResourceScheduleVO::getId).collect(Collectors.toList());
        return Transaction.execute((TransactionCallback<Long>) status -> {
            worker.removeScheduledJobs(scheduleIdsToRemove);

            CallContext.current().setEventResourceId(internalResourceId);
            CallContext.current().setEventResourceType(worker.getApiResourceType());
            return resourceScheduleDao.removeSchedulesForResourceAndIds(resourceType, internalResourceId, scheduleIdsToRemove);
        });
    }
}
