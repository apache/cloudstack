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
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import org.apache.cloudstack.api.command.user.vm.CreateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.DeleteVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMScheduleCmd;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.cloudstack.vm.schedule.dao.VMScheduleDao;
import org.apache.log4j.Logger;

import org.springframework.scheduling.support.CronExpression;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class VMScheduleManagerImpl extends MutualExclusiveIdsManagerBase implements VMScheduleManager, PluggableService {

    private static Logger LOGGER = Logger.getLogger(VMScheduleManagerImpl.class);

    @Inject
    private VMScheduleDao vmScheduleDao;

    @Inject
    private VirtualMachineManager virtualMachineManager;

    @Inject
    private VMScheduler vmScheduler;

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
    public VMSchedule createSchedule(CreateVMScheduleCmd cmd) {
        // TODO: Check if user is permitted to create the schedule
        CronExpression cronExpression;
        try {
            cronExpression = CronExpression.parse(cmd.getSchedule());
        } catch (IllegalArgumentException exception) {
            throw new InvalidParameterValueException("Invalid cron format: " + exception.getMessage());
        }

        VMSchedule.Action action = null;
        if (cmd.getAction() != null) {
            try {
                action = VMSchedule.Action.valueOf(cmd.getAction().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new InvalidParameterValueException("Invalid value for action: " + cmd.getAction());
            }
        }

        // TODO: Check for timezone related issues
        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();
        Date now = new Date();
        if (startDate == null) {
            startDate = now;
        } else if (startDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException("Invalid value for start date. start can't be less than current datetime");
        }

        if (endDate != null && startDate.compareTo(endDate) > 0) {
            throw new InvalidParameterValueException("Invalid value for end date. End date can't be less than start date. ");
        }

        // TODO: Revisit validation here.
        String cmdTimeZone = cmd.getTimeZone();
        TimeZone timeZone = TimeZone.getTimeZone(cmdTimeZone);
        String timeZoneId = timeZone.getID();

        LOGGER.warn(String.format("Using timezone [%s] for running the schedule [%s] for VM [%s], as an equivalent of [%s].", cmd.getVmId(), cmd.getName(), cmd.getVmId(),
                cmdTimeZone));


        // TODO: Wrap in a db transaction
        VMScheduleVO vmSchedule = vmScheduleDao.persist(new VMScheduleVO(cmd.getVmId(), cmd.getName(), cmd.getDescription(), cronExpression.toString(), timeZoneId, action, startDate, endDate, false));
        vmScheduler.scheduleNextJob(vmSchedule);

        return vmSchedule;
    }

    @Override
    public VMScheduleResponse createResponse(VMSchedule vmSchedule) {
        VirtualMachine vm = virtualMachineManager.findById(vmSchedule.getVmId());
        VMScheduleResponse response = new VMScheduleResponse();

        response.setObjectName(VMSchedule.class.getSimpleName().toLowerCase());
        response.setId(vmSchedule.getUuid());
        response.setVmId(vm.getUuid());
        response.setName(vmSchedule.getName());
        response.setDescription(vmSchedule.getDescription());
        response.setSchedule(vmSchedule.getSchedule());
        response.setTimeZone(vmSchedule.getTimeZone());
        response.setAction(vmSchedule.getAction());
        response.setEnabled(vmSchedule.getEnabled());
        response.setStartDate(vmSchedule.getStartDate());
        response.setEndDate(vmSchedule.getEndDate());
        return response;
    }

    @Override
    public Pair<List<? extends VMSchedule>, Integer> listSchedule(ListVMScheduleCmd cmd) {
        Long id = cmd.getId();
        Boolean enabled = cmd.getEnabled();
        Long vmId = cmd.getVmId();
        VMSchedule.Action action = null;
        if (cmd.getAction() != null) {
            try {
                action = VMSchedule.Action.valueOf(cmd.getAction());
            } catch (IllegalArgumentException exception) {
                throw new InvalidParameterValueException("Invalid value for action: " + cmd.getAction());
            }
        }

        Filter searchFilter = new Filter(VMScheduleVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<VMScheduleVO> sb = vmScheduleDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vm_id", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        sb.and("action", sb.entity().getAction(), SearchCriteria.Op.EQ);
        sb.and("enabled", sb.entity().getEnabled(), SearchCriteria.Op.EQ);

        SearchCriteria<VMScheduleVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }
        if (enabled != null) {
            sc.setParameters("enabled", enabled);
        }
        if (action != null) {
            sc.setParameters("action", action);
        }
        sc.setParameters("vm_id", vmId);
        Pair<List<VMScheduleVO>, Integer> result = vmScheduleDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends VMSchedule>, Integer>(result.first(), result.second());
    }

    @Override
    public VMSchedule updateSchedule(UpdateVMScheduleCmd cmd) {
        // TODO: Check if user is permitted to edit the schedule
        Long id = cmd.getId();
        VMScheduleVO vmSchedule = vmScheduleDao.findById(id);

        if (vmSchedule == null) {
            throw new CloudRuntimeException("VM schedule doesn't exist");
        }

        String name = cmd.getName();
        String description = cmd.getDescription();
        String schedule = cmd.getSchedule();

        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();

        if (endDate != null && ((startDate != null && startDate.compareTo(endDate) > 0) || vmSchedule.getStartDate().compareTo(endDate) > 0)) {
            throw new InvalidParameterValueException("Invalid value for end date. End date can't be less than start date.");
        }
        Boolean enabled = cmd.getEnabled();
        VMSchedule.Action action = null;
        if (cmd.getAction()!= null) {
            try {
                action = VMSchedule.Action.valueOf(cmd.getAction().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new InvalidParameterValueException("Invalid value for action: " + cmd.getAction());
            }
        }

        if (name != null) {
            vmSchedule.setName(name);
        }
        if (description != null) {
            vmSchedule.setDescription(description);
        }
        if (schedule != null) {
            vmSchedule.setSchedule(schedule);
        }
        if (cmd.getTimeZone() != null) {
            // TODO: Revisit validation here.
            String cmdTimeZone = cmd.getTimeZone();
            TimeZone timeZone = TimeZone.getTimeZone(cmdTimeZone);
            String timeZoneId = timeZone.getID();
            if (!timeZoneId.equals(cmdTimeZone)) {
                LOGGER.warn(String.format("Using timezone [%s] for running the schedule [%s] for VM %s, as an equivalent of [%s].", timeZoneId, vmSchedule.getSchedule(), vmSchedule.getVmId(),
                        cmdTimeZone));
            }
            vmSchedule.setTimeZone(timeZoneId);
        }
        if (startDate != null) {
            vmSchedule.setStartDate(startDate);
        }
        if (endDate != null) {
            vmSchedule.setEndDate(endDate);
        }
        if (enabled != null) {
            vmSchedule.setEnabled(enabled);
        }
        if (action != null) {
            vmSchedule.setAction(action);
        }

        // TODO: Wrap this in a transaction
        vmScheduleDao.update(cmd.getId(), vmSchedule);
        vmScheduler.updateScheduledJob(vmSchedule);

        return vmSchedule;
    }

    @Override
    public long removeSchedule(DeleteVMScheduleCmd cmd) {
        // TODO: Check if the user has access to delete all schedules in the list
        List<Long> ids = getIdsListFromCmd(cmd.getId(), cmd.getIds());
        SearchBuilder<VMScheduleVO> sb = vmScheduleDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.IN);

        SearchCriteria<VMScheduleVO> sc = sb.create();
        sc.setParameters("id", ids.toArray());
        // TODO: Wrap this in a transaction
        vmScheduler.removeScheduledJobs(ids);
        int rowsRemoved = vmScheduleDao.remove(sc);
        return rowsRemoved;
    }
}
