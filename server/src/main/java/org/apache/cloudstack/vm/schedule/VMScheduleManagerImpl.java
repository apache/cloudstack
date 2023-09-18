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
package org.apache.cloudstack.vm.schedule;

import com.cloud.api.query.MutualExclusiveIdsManagerBase;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.AccountManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.user.vm.CreateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.DeleteVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMScheduleCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.schedule.dao.VMScheduleDao;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronExpression;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public class VMScheduleManagerImpl extends MutualExclusiveIdsManagerBase implements VMScheduleManager, PluggableService {

    @Inject
    private VMScheduleDao vmScheduleDao;
    @Inject
    private UserVmManager userVmManager;
    @Inject
    private VMScheduler vmScheduler;
    @Inject
    private AccountManager accountManager;

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateVMScheduleCmd.class);
        cmdList.add(ListVMScheduleCmd.class);
        cmdList.add(UpdateVMScheduleCmd.class);
        cmdList.add(DeleteVMScheduleCmd.class);
        return cmdList;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SCHEDULE_CREATE, eventDescription = "Creating VM Schedule", create = true)
    public VMScheduleResponse createSchedule(CreateVMScheduleCmd cmd) {
        VirtualMachine vm = userVmManager.getUserVm(cmd.getVmId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, vm);
        if (vm == null) {
            throw new InvalidParameterValueException(String.format("Invalid value for vmId: %s", cmd.getVmId()));
        }

        VMSchedule.Action action = null;
        if (cmd.getAction() != null) {
            try {
                action = VMSchedule.Action.valueOf(cmd.getAction().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new InvalidParameterValueException(String.format("Invalid value for action: %s", cmd.getAction()));
            }
        }

        Date cmdStartDate = cmd.getStartDate();
        Date cmdEndDate = cmd.getEndDate();
        String cmdTimeZone = cmd.getTimeZone();
        TimeZone timeZone = TimeZone.getTimeZone(cmdTimeZone);
        String timeZoneId = timeZone.getID();
        Date startDate = DateUtils.addMinutes(new Date(), 1);
        if (cmdStartDate != null) {
            startDate = Date.from(DateUtil.getZoneDateTime(cmdStartDate, timeZone.toZoneId()).toInstant());
        }
        Date endDate = null;
        if (cmdEndDate != null) {
            endDate = Date.from(DateUtil.getZoneDateTime(cmdEndDate, timeZone.toZoneId()).toInstant());
        }

        CronExpression cronExpression = DateUtil.parseSchedule(cmd.getSchedule());

        validateStartDateEndDate(startDate, endDate, timeZone);

        String description = null;
        if (StringUtils.isBlank(cmd.getDescription())) {
            description = String.format("%s - %s", action, DateUtil.getHumanReadableSchedule(cronExpression));
        } else description = cmd.getDescription();

        logger.warn(String.format("Using timezone [%s] for running the schedule for VM [%s], as an equivalent of [%s].", timeZoneId, vm.getUuid(), cmdTimeZone));

        String finalDescription = description;
        VMSchedule.Action finalAction = action;
        Date finalStartDate = startDate;
        Date finalEndDate = endDate;

        return Transaction.execute((TransactionCallback<VMScheduleResponse>) status -> {
            VMScheduleVO vmSchedule = vmScheduleDao.persist(new VMScheduleVO(cmd.getVmId(), finalDescription, cronExpression.toString(), timeZoneId, finalAction, finalStartDate, finalEndDate, cmd.getEnabled()));
            vmScheduler.scheduleNextJob(vmSchedule, new Date());
            CallContext.current().setEventResourceId(vm.getId());
            CallContext.current().setEventResourceType(ApiCommandResourceType.VirtualMachine);
            return createResponse(vmSchedule);
        });
    }

    @Override
    public VMScheduleResponse createResponse(VMSchedule vmSchedule) {
        VirtualMachine vm = userVmManager.getUserVm(vmSchedule.getVmId());
        VMScheduleResponse response = new VMScheduleResponse();

        response.setObjectName(VMSchedule.class.getSimpleName().toLowerCase());
        response.setId(vmSchedule.getUuid());
        response.setVmId(vm.getUuid());
        response.setDescription(vmSchedule.getDescription());
        response.setSchedule(vmSchedule.getSchedule());
        response.setTimeZone(vmSchedule.getTimeZone());
        response.setAction(vmSchedule.getAction());
        response.setEnabled(vmSchedule.getEnabled());
        response.setStartDate(vmSchedule.getStartDate());
        response.setEndDate(vmSchedule.getEndDate());
        response.setCreated(vmSchedule.getCreated());
        return response;
    }

    @Override
    public ListResponse<VMScheduleResponse> listSchedule(ListVMScheduleCmd cmd) {
        Long id = cmd.getId();
        Boolean enabled = cmd.getEnabled();
        Long vmId = cmd.getVmId();

        VirtualMachine vm = userVmManager.getUserVm(vmId);
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, vm);

        VMSchedule.Action action = null;
        if (cmd.getAction() != null) {
            try {
                action = VMSchedule.Action.valueOf(cmd.getAction());
            } catch (IllegalArgumentException exception) {
                throw new InvalidParameterValueException("Invalid value for action: " + cmd.getAction());
            }
        }

        Pair<List<VMScheduleVO>, Integer> result = vmScheduleDao.searchAndCount(id, vmId, action, enabled, cmd.getStartIndex(), cmd.getPageSizeVal());

        ListResponse<VMScheduleResponse> response = new ListResponse<>();
        List<VMScheduleResponse> responsesList = new ArrayList<>();
        for (VMSchedule vmSchedule : result.first()) {
            responsesList.add(createResponse(vmSchedule));
        }
        response.setResponses(responsesList, result.second());
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SCHEDULE_UPDATE, eventDescription = "Updating VM Schedule")
    public VMScheduleResponse updateSchedule(UpdateVMScheduleCmd cmd) {
        Long id = cmd.getId();
        VMScheduleVO vmSchedule = vmScheduleDao.findById(id);

        if (vmSchedule == null) {
            throw new CloudRuntimeException("VM schedule doesn't exist");
        }

        VirtualMachine vm = userVmManager.getUserVm(vmSchedule.getVmId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, vm);

        CronExpression cronExpression = Objects.requireNonNullElse(
                DateUtil.parseSchedule(cmd.getSchedule()),
                DateUtil.parseSchedule(vmSchedule.getSchedule())
        );

        String description = cmd.getDescription();
        if (description == null && vmSchedule.getDescription() == null) {
            description = String.format("%s - %s", vmSchedule.getAction(), DateUtil.getHumanReadableSchedule(cronExpression));
        }
        String cmdTimeZone = cmd.getTimeZone();
        Date cmdStartDate = cmd.getStartDate();
        Date cmdEndDate = cmd.getEndDate();
        Boolean enabled = cmd.getEnabled();

        TimeZone timeZone;
        String timeZoneId;
        if (cmdTimeZone != null) {
            timeZone = TimeZone.getTimeZone(cmdTimeZone);
            timeZoneId = timeZone.getID();
            if (!timeZoneId.equals(cmdTimeZone)) {
                logger.warn(String.format("Using timezone [%s] for running the schedule [%s] for VM %s, as an equivalent of [%s].",
                        timeZoneId, vmSchedule.getSchedule(), vmSchedule.getVmId(), cmdTimeZone));
            }
            vmSchedule.setTimeZone(timeZoneId);
        } else {
            timeZoneId = vmSchedule.getTimeZone();
            timeZone = TimeZone.getTimeZone(timeZoneId);
        }

        Date startDate = vmSchedule.getStartDate().before(DateUtils.addMinutes(new Date(), 1)) ? DateUtils.addMinutes(new Date(), 1) : vmSchedule.getStartDate();
        Date endDate = vmSchedule.getEndDate();
        if (cmdEndDate != null) {
            endDate = Date.from(DateUtil.getZoneDateTime(cmdEndDate, timeZone.toZoneId()).toInstant());
        }

        if (cmdStartDate != null) {
            startDate = Date.from(DateUtil.getZoneDateTime(cmdStartDate, timeZone.toZoneId()).toInstant());
        }

        validateStartDateEndDate(Objects.requireNonNullElse(startDate, DateUtils.addMinutes(new Date(), 1)), endDate, timeZone);

        if (enabled != null) {
            vmSchedule.setEnabled(enabled);
        }
        if (description != null) {
            vmSchedule.setDescription(description);
        }
        if (cmdEndDate != null) {
            vmSchedule.setEndDate(endDate);
        }
        if (cmdStartDate != null) {
            vmSchedule.setStartDate(startDate);
        }
        vmSchedule.setSchedule(cronExpression.toString());

        return Transaction.execute((TransactionCallback<VMScheduleResponse>) status -> {
            vmScheduleDao.update(cmd.getId(), vmSchedule);
            vmScheduler.updateScheduledJob(vmSchedule);
            CallContext.current().setEventResourceId(vm.getId());
            CallContext.current().setEventResourceType(ApiCommandResourceType.VirtualMachine);
            return createResponse(vmSchedule);
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
    @ActionEvent(eventType = EventTypes.EVENT_VM_SCHEDULE_DELETE, eventDescription = "Deleting VM Schedule for VM")
    public long removeScheduleByVmId(long vmId, boolean expunge) {
        SearchCriteria<VMScheduleVO> sc = vmScheduleDao.getSearchCriteriaForVMId(vmId);
        List<VMScheduleVO> vmSchedules = vmScheduleDao.search(sc, null);
        List<Long> ids = new ArrayList<>();
        for (final VMScheduleVO vmSchedule : vmSchedules) {
            ids.add(vmSchedule.getId());
        }
        vmScheduler.removeScheduledJobs(ids);
        if (expunge) {
            return vmScheduleDao.expunge(sc);
        }
        CallContext.current().setEventResourceId(vmId);
        CallContext.current().setEventResourceType(ApiCommandResourceType.VirtualMachine);
        return vmScheduleDao.remove(sc);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_SCHEDULE_DELETE, eventDescription = "Deleting VM Schedule")
    public Long removeSchedule(DeleteVMScheduleCmd cmd) {
        VirtualMachine vm = userVmManager.getUserVm(cmd.getVmId());
        accountManager.checkAccess(CallContext.current().getCallingAccount(), null, false, vm);

        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());

        if (ids.isEmpty()) {
            throw new InvalidParameterValueException("Either id or ids parameter must be specified");
        }
        return Transaction.execute((TransactionCallback<Long>) status -> {
            vmScheduler.removeScheduledJobs(ids);
            CallContext.current().setEventResourceId(vm.getId());
            CallContext.current().setEventResourceType(ApiCommandResourceType.VirtualMachine);
            return vmScheduleDao.removeSchedulesForVmIdAndIds(vm.getId(), ids);
        });
    }
}
