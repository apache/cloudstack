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
package com.cloud.network.router;

import java.util.Map;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.vm.VirtualMachineProfile.Param;

public class RouterDeploymentDefinition {

    protected Vpc vpc;
    protected Network guestNetwork;
    protected DeployDestination dest;
    protected Account owner;
    protected Map<Param, Object> params;
    protected boolean isRedundant;
    protected DeploymentPlan plan;

    public RouterDeploymentDefinition(final Vpc vpc, final DeployDestination dest,
            final Account owner, final Map<Param, Object> params,
            final boolean isRedundant) {

        this.vpc = vpc;
        this.dest = dest;
        this.owner = owner;
        this.params = params;
        this.isRedundant = isRedundant;
    }

    public RouterDeploymentDefinition(final Network guestNetwork, final DeployDestination dest,
            final Account owner, final Map<Param, Object> params, final boolean isRedundant) {

        this.guestNetwork = guestNetwork;
        this.dest = dest;
        this.owner = owner;
        this.params = params;
        this.isRedundant = isRedundant;
    }

    public Vpc getVpc() {
        return vpc;
    }
    public void setVpc(final Vpc vpc) {
        this.vpc = vpc;
    }
    public Network getGuestNetwork() {
        return guestNetwork;
    }
    public void setGuestNetwork(final Network guestNetwork) {
        this.guestNetwork = guestNetwork;
    }
    public DeployDestination getDest() {
        return dest;
    }
    public void setDest(final DeployDestination dest) {
        this.dest = dest;
    }
    public Account getOwner() {
        return owner;
    }
    public void setOwner(final Account owner) {
        this.owner = owner;
    }
    public Map<Param, Object> getParams() {
        return params;
    }
    public void setParams(final Map<Param, Object> params) {
        this.params = params;
    }
    public boolean isRedundant() {
        return isRedundant;
    }
    public void setRedundant(final boolean isRedundant) {
        this.isRedundant = isRedundant;
    }
    public DeploymentPlan getPlan() {
        return plan;
    }
    public void setPlan(final DeploymentPlan plan) {
        this.plan = plan;
    }

    public boolean isVpcRouter() {
        return vpc != null;
    }
    public Long getPodId() {
        return plan.getPodId();
    }
}