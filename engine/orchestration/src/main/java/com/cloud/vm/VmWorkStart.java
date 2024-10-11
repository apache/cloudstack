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

package com.cloud.vm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;

import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.utils.Journal;

public class VmWorkStart extends VmWork {
    private static final long serialVersionUID = 9038937399817468894L;


    long dcId;
    Long podId;
    Long clusterId;
    Long hostId;
    Long poolId;
    ExcludeList avoids;
    Long physicalNetworkId;

    String reservationId;
    String journalName;
    String planner;

    // use serialization friendly map
    private Map<String, String> rawParams;

    public VmWorkStart(long userId, long accountId, long vmId, String handlerName) {
        super(userId, accountId, vmId, handlerName);
    }

    public VmWorkStart(VmWork vmWork){
        super(vmWork);
    }

    public DeploymentPlan getPlan() {

        if (podId != null || clusterId != null || hostId != null || poolId != null || physicalNetworkId != null || avoids !=null) {
            // this is ugly, to work with legacy code, we need to re-construct the DeploymentPlan hard-codely
            // this has to be refactored together with migrating legacy code into the new way
            ReservationContext context = null;
            if (reservationId != null) {
                Journal journal = new Journal.LogJournal("VmWorkStart", logger);
                context = new ReservationContextImpl(reservationId, journal,
                        CallContext.current().getCallingUser(),
                        CallContext.current().getCallingAccount());
            }

            DeploymentPlan plan = new DataCenterDeployment(
                    dcId, podId, clusterId, hostId, poolId, physicalNetworkId,
                    context);
            plan.setAvoids(avoids);
            return plan;
        }

        return null;
    }

    public void setPlan(DeploymentPlan plan) {
        if (plan != null) {
            dcId = plan.getDataCenterId();
            podId = plan.getPodId();
            clusterId = plan.getClusterId();
            hostId = plan.getHostId();
            poolId = plan.getPoolId();
            physicalNetworkId = plan.getPhysicalNetworkId();
            avoids = plan.getAvoids();

            if (plan.getReservationContext() != null)
                reservationId = plan.getReservationContext().getReservationId();
        }
    }

    public void setDeploymentPlanner(String planner) {
        this.planner = planner;
    }

    public String getDeploymentPlanner() {
        return this.planner;
    }

    public Map<String, String> getRawParams() {
        return rawParams;
    }

    public void setRawParams(Map<String, String> params) {
        rawParams = params;
    }

    public Map<VirtualMachineProfile.Param, Object> getParams() {
        Map<VirtualMachineProfile.Param, Object> map = new HashMap<VirtualMachineProfile.Param, Object>();

        if (rawParams != null) {
            for (Map.Entry<String, String> entry : rawParams.entrySet()) {
                VirtualMachineProfile.Param key = new VirtualMachineProfile.Param(entry.getKey());
                Object val = JobSerializerHelper.fromObjectSerializedString(entry.getValue());
                map.put(key, val);
            }
        }

        return map;
    }

    public void setParams(Map<VirtualMachineProfile.Param, Object> params) {
        if (params != null) {
            rawParams = new HashMap<String, String>();
            for (Map.Entry<VirtualMachineProfile.Param, Object> entry : params.entrySet()) {
                rawParams.put(entry.getKey().getName(), JobSerializerHelper.toObjectSerializedString(
                        entry.getValue() instanceof Serializable ? (Serializable)entry.getValue() : entry.getValue().toString()));
            }
        }
    }

    @Override
    public String toString() {
        List<String> params = new ArrayList<>();
        params.add(VirtualMachineProfile.Param.VmPassword.getName());
        return super.toStringAfterRemoveParams("rawParams", params);
    }
}
