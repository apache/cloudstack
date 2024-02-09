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

package org.apache.cloudstack.ha.provider.host;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.fsm.NoTransitionException;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.ha.HAResource;
import org.apache.cloudstack.ha.provider.HAProvider;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import javax.inject.Inject;

public abstract class HAAbstractHostProvider extends AdapterBase implements HAProvider<Host> {


    @Inject
    private AlertManager alertManager;
    @Inject
    protected AgentManager agentManager;
    @Inject
    protected ResourceManager resourceManager;
    @Inject
    protected HighAvailabilityManager oldHighAvailabilityManager;


    @Override
    public HAResource.ResourceType resourceType() {
        return HAResource.ResourceType.Host;
    }

    public HAResource.ResourceSubType resourceSubType() {
        return HAResource.ResourceSubType.Unknown;
    }

    @Override
    public boolean isDisabled(final Host host) {
        return host.isDisabled();
    }

    @Override
    public boolean isInMaintenanceMode(final Host host) {
        return host.isInMaintenanceStates();
    }

    @Override
    public void fenceSubResources(final Host r) {
        if (r.getState() != Status.Down) {
            try {
                logger.debug("Trying to disconnect the host without investigation and scheduling HA for the VMs on host id=" + r.getId());
                agentManager.disconnectWithoutInvestigation(r.getId(), Event.HostDown);
                oldHighAvailabilityManager.scheduleRestartForVmsOnHost((HostVO)r, true);
            } catch (Exception e) {
                logger.error("Failed to disconnect host and schedule HA restart of VMs after fencing the host: ", e);
            }
        }
    }

    @Override
    public void enableMaintenance(final Host r) {
        try {
            resourceManager.resourceStateTransitTo(r, ResourceState.Event.InternalEnterMaintenance, ManagementServerNode.getManagementServerId());
        } catch (NoTransitionException e) {
            logger.error("Failed to put host in maintenance mode after host-ha fencing and scheduling VM-HA: ", e);
        }
    }

    @Override
    public void sendAlert(final Host host, final HAConfig.HAState nextState) {
        String subject = "HA operation performed for host";
        String body = subject;
        if (HAConfig.HAState.Fencing.equals(nextState)) {
            subject = String.format("HA Fencing of host id=%d, in dc id=%d performed", host.getId(), host.getDataCenterId());
            body = String.format("HA Fencing has been performed for host id=%d, uuid=%s in datacenter id=%d", host.getId(), host.getUuid(), host.getDataCenterId());
        } else if (HAConfig.HAState.Recovering.equals(nextState)) {
            subject = String.format("HA Recovery of host id=%d, in dc id=%d performed", host.getId(), host.getDataCenterId());
            body = String.format("HA Recovery has been performed for host id=%d, uuid=%s in datacenter id=%d", host.getId(), host.getUuid(), host.getDataCenterId());
        }
        alertManager.sendAlert(AlertService.AlertType.ALERT_TYPE_HA_ACTION, host.getDataCenterId(), host.getPodId(), subject, body);
    }

}
