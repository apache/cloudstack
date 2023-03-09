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
package com.cloud.vm.schedule;


import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.ListUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.schedule.dao.VMScheduleDao;
import org.apache.cloudstack.api.command.user.vmschedule.CreateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.ListVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.UpdateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.DeleteVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.EnableVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.DisableVMScheduleCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class VMScheduleManagerImpl extends ManagerBase implements VMScheduleManager, Configurable, PluggableService {
    public static final Logger s_logger = Logger.getLogger(VMScheduleManagerImpl.class);

    @Inject
    private VMScheduleDao vmScheduleDao;

    @Inject
    private VirtualMachineManager vmManager;

    @Inject
    private BackgroundPollManager backgroundPollManager;

    @Inject
    private AsyncJobManager asyncJobManager;

    @Inject
    private VMInstanceDao vmInstanceDao;

    private static final ConfigKey<Integer> VMSchedulerInterval = new ConfigKey<>("Advanced", Integer.class,
            "gc.interval", "120",
            "The interval at which background tasks runs in milliseconds", false);

    @Override
    public boolean start() {

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return VMScheduleManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                VMSchedulerInterval
        };
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        backgroundPollManager.submitTask(new VMScheduleManagerImpl.VMScheduleBackgroundTask(this));
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateVMScheduleCmd.class);
        cmdList.add(ListVMScheduleCmd.class);
        cmdList.add(UpdateVMScheduleCmd.class);
        cmdList.add(DeleteVMScheduleCmd.class);
        cmdList.add(EnableVMScheduleCmd.class);
        cmdList.add(DisableVMScheduleCmd.class);
        cmdList.add(UpdateVMScheduleCmd.class);
        return cmdList;
    }

    @Override
    public VMSchedule findVMSchedule(Long id) {
        if (id == null || id < 1L) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("VMSchedule ID is invalid [%s]", id));
                return null;
            }
        }

         VMSchedule vmSchedule = vmScheduleDao.findById(id);
        if (vmSchedule == null) {
            if(s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("VmSchedule ID not found [id=%s]", id));
                return null;
            }
        }

        return vmSchedule;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_CREATE, eventDescription = "creating vm schedule", async = true)
    public VMSchedule createVMSchedule(Long vmId, String description, String action, String period, String tag, String timezone) {
        VMScheduleVO vmScheduleVo = new VMScheduleVO(vmId, description, action, period, tag, timezone);
        VMSchedule vmSchedule = vmScheduleDao.persist(vmScheduleVo);
        if (vmSchedule == null) {
            throw new CloudRuntimeException("Failed to create schedule for vm: " + vmId);
        }

        return vmSchedule;
    }

    @Override
    public List<VMSchedule> listVMSchedules(ListVMScheduleCmd cmd) {
        if(cmd.getId() != null) {
            VMSchedule vmSchedule = findVMSchedule(cmd.getId());
            List<VMSchedule> arr = new ArrayList<>();
            arr.add(vmSchedule);
            return arr;
        }

        List<? extends VMSchedule> vmSchedules= vmScheduleDao.listAll();
        return ListUtils.toListOfInterface(vmSchedules);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_DELETE, eventDescription = "deleting VM Schedule")
    public boolean deleteVMSchedule(Long vmScheduleId) {
        VMSchedule vmSchedule = vmScheduleDao.findById(vmScheduleId);
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + vmScheduleId);
        }

        return vmScheduleDao.remove(vmSchedule.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_ENABLE, eventDescription = "enable VM Schedule")
    public boolean enableVMSchedule(Long vmScheduleId) throws ResourceUnavailableException {
        VMScheduleVO vmSchedule = vmScheduleDao.findById(vmScheduleId);
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + vmScheduleId);
        }

        vmSchedule.setState(VMSchedule.State.Enabled);
        boolean updateResult = vmScheduleDao.update(vmSchedule.getId(), vmSchedule);

        return updateResult;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_DISABLE, eventDescription = "disable VM Schedule")
    public boolean disableVMSchedule(Long vmScheduleId) {
        VMScheduleVO vmSchedule = vmScheduleDao.findById(vmScheduleId);
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + vmScheduleId);
        }

        vmSchedule.setState(VMSchedule.State.Disabled);
        boolean updateResult = vmScheduleDao.update(vmSchedule.getId(), vmSchedule);

        return updateResult;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_UPDATE, eventDescription = "update VM Schedule")
    public VMSchedule updateVMSchedule(UpdateVMScheduleCmd cmd) {
        VMScheduleVO vmSchedule = vmScheduleDao.findById(cmd.getId());
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + cmd.getId());
        }
        String description = cmd.getDescription();
        String period = cmd.getPeriod();
        String action = cmd.getAction();
        String tag = cmd.getTag();
        String timezone = cmd.getTimezone();

        if (vmSchedule.getState() == VMSchedule.State.Disabled) {
            if (description != null)
                vmSchedule.setDescription(description);
            if (period != null)
                vmSchedule.setPeriod(period);
            if (action != null)
                vmSchedule.setAction(action);
            if (tag != null)
                vmSchedule.setTag(tag);
            if (timezone != null)
                vmSchedule.setTimezone(timezone);

            vmScheduleDao.update(vmSchedule.getId(), vmSchedule);
        } else {
            throw new InvalidParameterValueException("Enable the state of VM Schedule before updating it");
        }
        return vmSchedule;
    }

    public static final class VMScheduleBackgroundTask extends ManagedContextRunnable implements BackgroundPollTask {
        private  VMScheduleManagerImpl serviceImpl;

        public VMScheduleBackgroundTask(VMScheduleManagerImpl serviceImpl) {
            this.serviceImpl = serviceImpl;
        }

        private void scheduleActionOnVM(String action, VMSchedule.State state, String uuid, String period) {

            serviceImpl.vmManager.start(uuid, null);
        }

        private Boolean matchPeriodWithCurrentTimestamp(String period){
            Date currentTimestamp = new Date();
            String[] periodParts = period.split(" ");


            return true;
        }

        private void getVmSchedulesToBeExecuted() {

        }

        @Override
        protected void runInContext() {
            try {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM Scheduler GC task is running...");
                }
                // Date currentTimestamp = new Date();
                for (final VMScheduleVO vmSchedule :serviceImpl.vmScheduleDao.listAll()) {
                    VMInstanceVO vmInstance = serviceImpl.vmInstanceDao.findById(vmSchedule.getVmId());
                    scheduleActionOnVM(vmSchedule.getAction(), vmSchedule.getState(), vmInstance.getUuid(), vmSchedule.getPeriod());
                }

                /*
                Long accountId= CallContext.current().getCallingAccountId();
                List<VMInstanceVO> vmInstanceVOList = serviceImpl.vmInstanceDao.listByAccountId(accountId);
                for (final VMInstanceVO vmInstance : vmInstanceVOList) {
                    List<VMScheduleVO> vmScheduleVOList = serviceImpl.vmScheduleDao.findByVm(vmInstance.getId());
                    if (vmScheduleVOList != null) {
                        for (final VMScheduleVO vmSchedule : vmScheduleVOList) {
                            scheduleActionOnVM(vmSchedule.getAction(), vmSchedule.getState(), vmInstance.getUuid(), vmSchedule.getPeriod());
                        }
                    }
                }
                */
            } catch (final Throwable t) {
                s_logger.error("Error trying to run VM Scheduler GC task", t);
            }
        }

        @Override
        public Long getDelay() {
            // In Milliseconds
            return VMSchedulerInterval.value() * 1000L;
        }

    }

}
