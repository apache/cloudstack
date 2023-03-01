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
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.schedule.dao.VMScheduleDao;
import org.apache.cloudstack.api.command.user.vmschedule.CreateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.DeleteVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.ListVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.UpdateVMScheduleCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class VMScheduleManagerImpl extends ManagerBase implements VMScheduleManager, Configurable, PluggableService {
    public static final Logger s_logger = Logger.getLogger(VMScheduleManagerImpl.class);

    @Inject
    private VMScheduleDao vmScheduleDao;

    @Override
    public String getConfigComponentName() {
        return null;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }

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
}
