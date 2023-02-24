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
package org.apache.cloudstack.api.command.user.vmschedule;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.response.VMScheduleResponse;
import org.apache.log4j.Logger;

@APICommand(name = UpdateVMScheduleCmd.APINAME,
        description = "Lists Schedules for a VM",
        responseObject = VMScheduleResponse.class,
        since = "4.18.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateVMScheduleCmd extends BaseAsyncCmd {
    public static final String APINAME = "updateVMSchedule";
    public static final Logger s_logger = Logger.getLogger(UpdateVMScheduleCmd.class.getName());

    @Override
    public void execute() {

    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }


    @Override
    public String getEventType() {
        return null;
    }


    @Override
    public String getEventDescription() {
        return null;
    }
}
