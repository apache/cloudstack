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
package org.apache.cloudstack.affinity;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.AffinityConflictException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachineProfile;

public class AffinityProcessorBase extends AdapterBase implements AffinityGroupProcessor {

    protected String _type;

    @Override
    public void process(VirtualMachineProfile vm, DeploymentPlan plan, ExcludeList avoid) throws AffinityConflictException {

    }

    @Override
    public String getType() {
        return _type;
    }

    public void setType(String type) {
        _type = type;
    }

    @Override
    public boolean check(VirtualMachineProfile vm, DeployDestination plannedDestination) throws AffinityConflictException {
        return true;
    }

    @Override
    public boolean isAdminControlledGroup() {
        return false;
    }

    @Override
    public boolean canBeSharedDomainWide() {
        return false;
    }

    @Override
    public void handleDeleteGroup(AffinityGroup group) {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public boolean subDomainAccess() {
        return false;
    }
}
