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
package com.cloud.network.as;

import java.util.List;

import org.apache.cloudstack.api.command.admin.autoscale.CreateCounterCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateConditionCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScalePoliciesCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScaleVmGroupsCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScaleVmProfilesCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListConditionsCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListCountersCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateConditionCmd;

import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;

public interface AutoScaleService {

    public AutoScalePolicy createAutoScalePolicy(CreateAutoScalePolicyCmd createAutoScalePolicyCmd);

    public boolean deleteAutoScalePolicy(long autoScalePolicyId);

    List<? extends AutoScalePolicy> listAutoScalePolicies(ListAutoScalePoliciesCmd cmd);

    AutoScalePolicy updateAutoScalePolicy(UpdateAutoScalePolicyCmd cmd);

    AutoScaleVmProfile createAutoScaleVmProfile(CreateAutoScaleVmProfileCmd cmd);

    boolean deleteAutoScaleVmProfile(long profileId);

    List<? extends AutoScaleVmProfile> listAutoScaleVmProfiles(ListAutoScaleVmProfilesCmd listAutoScaleVmProfilesCmd);

    AutoScaleVmProfile updateAutoScaleVmProfile(UpdateAutoScaleVmProfileCmd cmd);

    AutoScaleVmGroup createAutoScaleVmGroup(CreateAutoScaleVmGroupCmd cmd);

    boolean configureAutoScaleVmGroup(CreateAutoScaleVmGroupCmd cmd) throws ResourceUnavailableException;

    boolean deleteAutoScaleVmGroup(long vmGroupId, Boolean cleanup);

    AutoScaleVmGroup updateAutoScaleVmGroup(UpdateAutoScaleVmGroupCmd cmd);

    AutoScaleVmGroup enableAutoScaleVmGroup(Long id);

    AutoScaleVmGroup disableAutoScaleVmGroup(Long id);

    List<? extends AutoScaleVmGroup> listAutoScaleVmGroups(ListAutoScaleVmGroupsCmd listAutoScaleVmGroupsCmd);

    Counter createCounter(CreateCounterCmd cmd);

    boolean deleteCounter(long counterId) throws ResourceInUseException;

    List<? extends Counter> listCounters(ListCountersCmd cmd);

    Condition createCondition(CreateConditionCmd cmd);

    List<? extends Condition> listConditions(ListConditionsCmd cmd);

    boolean deleteCondition(long conditionId) throws ResourceInUseException;

    Condition updateCondition(UpdateConditionCmd cmd) throws ResourceInUseException;
}
