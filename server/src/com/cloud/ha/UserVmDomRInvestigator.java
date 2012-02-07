/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.ha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.NetworkManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.utils.component.Inject;
import com.cloud.vm.Nic;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

@Local(value={Investigator.class})
public class UserVmDomRInvestigator extends AbstractInvestigatorImpl {
    private static final Logger s_logger = Logger.getLogger(UserVmDomRInvestigator.class);

    private String _name = null;
    @Inject private UserVmDao _userVmDao = null;
    @Inject private AgentManager _agentMgr = null;
    @Inject private NetworkManager _networkMgr = null;
    @Inject private VirtualNetworkApplianceManager _vnaMgr = null;

    @Override
    public Boolean isVmAlive(VMInstanceVO vm, HostVO host) {
        if (vm.getType() != VirtualMachine.Type.User) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Not a User Vm, unable to determine state of " + vm + " returning null");
            }
            return null;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("testing if " + vm + " is alive");
        }
        // to verify that the VM is alive, we ask the domR (router) to ping the VM (private IP)
        UserVmVO userVm = _userVmDao.findById(vm.getId());

        List<? extends Nic> nics = _networkMgr.getNicsForTraffic(userVm.getId(), TrafficType.Guest);

        for (Nic nic : nics) {
            if (nic.getIp4Address() == null) {
                continue;
            }

            List<VirtualRouter> routers = _vnaMgr.getRoutersForNetwork(nic.getNetworkId());
            if (routers == null || routers.isEmpty()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find a router in network " + nic.getNetworkId() + " to ping " + vm);
                }
                continue;
            }

            Boolean result = null;
            for (VirtualRouter router : routers) {
                result = testUserVM(vm, nic, router);
                if (result != null) {
                    break;
                }
            }

            if (result == null) {
                continue;
            }

            return result;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Returning null since we're unable to determine state of " + vm);
        }
        return null;
    }

    @Override
    public Status isAgentAlive(HostVO agent) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("checking if agent (" + agent.getId() + ") is alive");
        }

        if (agent.getPodId() == null) {
            return null;
        }

        List<Long> otherHosts = findHostByPod(agent.getPodId(), agent.getId());

        for (Long hostId : otherHosts) {

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("sending ping from (" + hostId + ") to agent's host ip address (" + agent.getPrivateIpAddress() + ")");
            }
            Status hostState = testIpAddress(hostId, agent.getPrivateIpAddress());
            if (hostState == null) {
                continue;
            }
            if (hostState == Status.Up) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("ping from (" + hostId + ") to agent's host ip address (" + agent.getPrivateIpAddress() + ") successful, returning that agent is disconnected");
                }
                return Status.Disconnected; // the computing host ip is ping-able, but the computing agent is down, report that the agent is disconnected
            } else if (hostState == Status.Down) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("returning host state: " + hostState);
                }
                return hostState;
            }
        }

        // could not reach agent, could not reach agent's host, unclear what the problem is but it'll require more investigation...
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("could not reach agent, could not reach agent's host, returning that we don't have enough information");
        }
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private Boolean testUserVM(VMInstanceVO vm, Nic nic, VirtualRouter router) {
        String privateIp = nic.getIp4Address();
        String routerPrivateIp = router.getPrivateIpAddress();

        List<Long> otherHosts = new ArrayList<Long>();
        if(vm.getHypervisorType() == HypervisorType.XenServer
        		|| vm.getHypervisorType() == HypervisorType.KVM){
        	otherHosts.add(router.getHostId());
        }else{
        	otherHosts = findHostByPod(router.getPodIdToDeployIn(), null);
        }
        for (Long hostId : otherHosts) {
            try {
                Answer pingTestAnswer = _agentMgr.easySend(hostId, new PingTestCommand(routerPrivateIp, privateIp));
                if (pingTestAnswer!=null && pingTestAnswer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("user vm " + vm.getHostName() + " has been successfully pinged, returning that it is alive");
                    }
                    return Boolean.TRUE;
                }
            } catch (Exception e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Couldn't reach due to" + e.toString());
                }
                continue;
            }
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(vm + " could not be pinged, returning that it is unknown");
        }
        return null;

    }
}
