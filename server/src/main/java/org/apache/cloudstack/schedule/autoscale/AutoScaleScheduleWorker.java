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
package org.apache.cloudstack.schedule.autoscale;

import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmGroupVO;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.user.User;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmGroupCmd;
import org.apache.cloudstack.schedule.BaseScheduleWorker;
import org.apache.cloudstack.schedule.ResourceSchedule;
import org.apache.cloudstack.schedule.ResourceScheduledJobVO;
import org.apache.cloudstack.schedule.dao.ResourceScheduleDetailsDao;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.cloudstack.api.ApiConstants.MAX_MEMBERS;
import static org.apache.cloudstack.api.ApiConstants.MIN_MEMBERS;

public class AutoScaleScheduleWorker extends BaseScheduleWorker {

    @Inject
    private AutoScaleManager autoScaleManager;

    @Inject
    private AutoScaleVmGroupDao autoScaleVmGroupDao;

    @Inject
    private ResourceScheduleDetailsDao resourceScheduleDetailsDao;

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.AutoScaleVmGroup;
    }

    @Override
    public boolean isResourceValid(long resourceId) {
        AutoScaleVmGroupVO group = autoScaleVmGroupDao.findById(resourceId);
        return group != null && !AutoScaleVmGroup.State.REVOKE.equals(group.getState());
    }

    @Override
    public long getEntityOwnerId(long resourceId) {
        AutoScaleVmGroupVO group = autoScaleVmGroupDao.findById(resourceId);
        return group != null ? group.getAccountId() : User.UID_SYSTEM;
    }

    @Override
    public AutoScaleScheduleAction parseAction(String actionName) {
        AutoScaleScheduleAction action = EnumUtils.getEnumIgnoreCase(AutoScaleScheduleAction.class, actionName);
        if (action == null) {
            throw new InvalidParameterValueException(String.format(
                    "Invalid action for AutoScaleVmGroup schedule: %s. Supported actions: %s",
                    actionName, Arrays.toString(AutoScaleScheduleAction.values())));
        }
        return action;
    }

    @Override
    public void validateDetails(ResourceSchedule.Action action, Map<String, String> details) {
        if (!(action instanceof AutoScaleScheduleAction)) {
            throw new InvalidParameterValueException("Invalid action type for AutoScaleVmGroup schedule");
        }
        if (MapUtils.isEmpty(details)) {
            throw new InvalidParameterValueException("Details are required for AutoScaleVmGroup schedule");
        }
        if (!details.keySet().stream().allMatch(key -> MIN_MEMBERS.equalsIgnoreCase(key) || MAX_MEMBERS.equalsIgnoreCase(key))) {
            throw new InvalidParameterValueException("Only minmembers and maxmembers are supported for AutoScaleVmGroup schedule details");
        }

        String minMembersRaw = details.get(MIN_MEMBERS);
        String maxMembersRaw = details.get(MAX_MEMBERS);
        if (StringUtils.isBlank(minMembersRaw) || StringUtils.isBlank(maxMembersRaw)) {
            throw new InvalidParameterValueException("Both minmembers and maxmembers are required for AutoScaleVmGroup schedule");
        }

        int minMembers;
        int maxMembers;
        try {
            minMembers = Integer.parseInt(minMembersRaw);
            maxMembers = Integer.parseInt(maxMembersRaw);
        } catch (NumberFormatException e) {
            throw new InvalidParameterValueException("minmembers and maxmembers must be valid integers");
        }

        autoScaleManager.validateMinMaxMembers(minMembers, maxMembers);
    }

    @Override
    protected Long processJob(ResourceScheduledJobVO job) {
        AutoScaleVmGroupVO group = autoScaleVmGroupDao.findById(job.getResourceId());
        if (group == null || AutoScaleVmGroup.State.REVOKE.equals(group.getState())) {
            logger.warn("AutoScaleVmGroup id={} not found/invalid; skipping scheduled job {}", job.getResourceId(), job);
            return null;
        }

        AutoScaleScheduleAction action = parseAction(job.getActionName());
        Map<String, String> details = resourceScheduleDetailsDao.listDetailsKeyPairs(job.getScheduleId(), true);
        validateDetails(action, details);

        long eventId = ActionEventUtils.onCompletedActionEvent(
                User.UID_SYSTEM, group.getAccountId(), null,
                action.getEventType(), true,
                String.format("Executing action (%s) for AutoScaleVmGroup: %s", action, group.getUuid()),
                group.getId(), ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);

        Map<String, String> params = new HashMap<>();
        params.put(MIN_MEMBERS, details.get(MIN_MEMBERS));
        params.put(MAX_MEMBERS, details.get(MAX_MEMBERS));
        return submitAsyncJob(UpdateAutoScaleVmGroupCmd.class, group.getAccountId(), group.getId(), eventId, params);
    }
}
