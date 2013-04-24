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
package com.cloud.deploy;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.affinity.AffinityGroupProcessor;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanner.PlannerResourceUsage;
import com.cloud.deploy.dao.PlannerHostReservationDao;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;

@Local(value = { DeploymentPlanningManager.class })
public class DeploymentPlanningManagerImpl extends ManagerBase implements DeploymentPlanningManager, Manager, Listener {

    private static final Logger s_logger = Logger.getLogger(DeploymentPlanningManagerImpl.class);
    @Inject
    AgentManager _agentMgr;
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    PlannerHostReservationDao _plannerHostReserveDao;

    protected List<DeploymentPlanner> _planners;
    public List<DeploymentPlanner> getPlanners() {
        return _planners;
    }
    public void setPlanners(List<DeploymentPlanner> _planners) {
        this._planners = _planners;
    }

    protected List<AffinityGroupProcessor> _affinityProcessors;
    public List<AffinityGroupProcessor> getAffinityGroupProcessors() {
        return _affinityProcessors;
    }
    public void setAffinityGroupProcessors(List<AffinityGroupProcessor> affinityProcessors) {
        this._affinityProcessors = affinityProcessors;
    }

    @Override
    public DeployDestination planDeployment(VirtualMachineProfile<? extends VirtualMachine> vmProfile,
            DeploymentPlan plan, ExcludeList avoids) throws InsufficientServerCapacityException,
            AffinityConflictException {

        // call affinitygroup chain
        VirtualMachine vm = vmProfile.getVirtualMachine();
        long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            for (AffinityGroupProcessor processor : _affinityProcessors) {
                processor.process(vmProfile, plan, avoids);
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deploy avoids pods: " + avoids.getPodsToAvoid() + ", clusters: "
                    + avoids.getClustersToAvoid() + ", hosts: " + avoids.getHostsToAvoid());
        }

        // call planners
        DeployDestination dest = null;
        List<Long> clusterIds = null;

        ServiceOffering offering = vmProfile.getServiceOffering();
        if (offering != null && offering.getDeploymentPlanner() != null) {
            DeploymentPlanner planner = ComponentContext.getComponent(offering.getDeploymentPlanner());
            if (planner != null && planner.canHandle(vmProfile, plan, avoids)) {
                while (true) {
                    if (planner instanceof DeploymentClusterPlanner) {
                        clusterIds = ((DeploymentClusterPlanner) planner).orderClusters(vmProfile, plan, avoids);
                    } else {
                        dest = planner.plan(vmProfile, plan, avoids);
                    }

                    if (dest != null) {
                        long hostId = dest.getHost().getId();
                        avoids.addHost(dest.getHost().getId());

                        if (checkIfHostCanBeUsed(hostId, DeploymentPlanner.PlannerResourceUsage.Shared)) {
                            // found destination
                            return dest;
                        } else {
                            // find another host - seems some concurrent deployment picked it up for dedicated access
                            continue;
                        }
                    } else if (clusterIds != null && !clusterIds.isEmpty()) {
                        // planner refactoring. call allocators to list hosts

                    } else {
                        return null;
                    }
                }
            }
        }


        return dest;
    }

    @DB
    private boolean checkIfHostCanBeUsed(long hostId, PlannerResourceUsage resourceTypeRequired) {
        // TODO Auto-generated method stub
        // check if this host has been picked up by some other planner
        // exclusively
        // if planner can work with shared host, check if this host has
        // been marked as 'shared'
        // else if planner needs dedicated host,

        PlannerHostReservationVO reservationEntry = _plannerHostReserveDao.findByHostId(hostId);
        if (reservationEntry != null) {
            long id = reservationEntry.getId();
            PlannerResourceUsage hostResourceType = reservationEntry.getResourceUsage();

            if (hostResourceType != null) {
                if (hostResourceType == resourceTypeRequired) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // reserve the host for required resourceType
                // let us lock the reservation entry before updating.
                final Transaction txn = Transaction.currentTxn();

                try {
                    txn.start();

                    final PlannerHostReservationVO lockedEntry = _plannerHostReserveDao.lockRow(id, true);
                    if (lockedEntry == null) {
                        s_logger.error("Unable to lock the host entry for reservation, host: " + hostId);
                        return false;
                    }
                    // check before updating
                    if (lockedEntry.getResourceUsage() == null) {
                        lockedEntry.setResourceUsage(resourceTypeRequired);
                        _plannerHostReserveDao.persist(lockedEntry);
                        return true;
                    } else {
                        // someone updated it earlier. check if we can still use it
                        if (lockedEntry.getResourceUsage() == resourceTypeRequired) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                } finally {
                    txn.commit();
                }
            }

        }

        return false;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }

        PlannerHostReservationVO reservationEntry = _plannerHostReserveDao.findByHostId(host.getId());
        if (reservationEntry == null) {
            // record the host in this table
            PlannerHostReservationVO newHost = new PlannerHostReservationVO(host.getId(), host.getDataCenterId(),
                    host.getPodId(), host.getClusterId());
            _plannerHostReserveDao.persist(newHost);
        }

    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRecurring() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _agentMgr.registerForHostEvents(this, true, false, true);
        return super.configure(name, params);
    }

}
