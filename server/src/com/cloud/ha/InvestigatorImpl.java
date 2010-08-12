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
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;

@Local(value={Investigator.class})
public class InvestigatorImpl implements Investigator {
    private static final Logger s_logger = Logger.getLogger(InvestigatorImpl.class);

    private String _name = null;
    private HostDao _hostDao = null;
    private DomainRouterDao _routerDao = null;;
    private UserVmDao _userVmDao = null;
    private AgentManager _agentMgr = null;

    @Override
    public Boolean isVmAlive(VMInstanceVO vm, HostVO host) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("testing if vm (" + vm.getId() + ") is alive");
        }
        if (vm.getType() == VirtualMachine.Type.User) {
            // to verify that the VM is alive, we ask the domR (router) to ping the VM (private IP)
            UserVmVO userVm = _userVmDao.findById(vm.getId());
            Long routerId = userVm.getDomainRouterId();
            if (routerId == null) {
            	/*TODO: checking vm status for external dhcp mode*/
            	s_logger.debug("It's external dhcp mode, how to checking the vm is alive?");
            	return true;
            }
            else
            	return testUserVM(vm, routerId);
        } else if ((vm.getType() == VirtualMachine.Type.DomainRouter) || (vm.getType() == VirtualMachine.Type.ConsoleProxy)) {
            // get the data center IP address, find a host on the pod, use that host to ping the data center IP address
            HostVO vmHost = _hostDao.findById(vm.getHostId());
            List<HostVO> otherHosts = findHostByPod(vm.getPodId(), vm.getHostId());
            for (HostVO otherHost : otherHosts) {

                Status vmState = testIpAddress(otherHost.getId(), vm.getPrivateIpAddress());
                if (vmState == null) {
                    // can't get information from that host, try the next one
                    continue;
                }
                if (vmState == Status.Up) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("successfully pinged vm's private IP (" + vm.getPrivateIpAddress() + "), returning that the VM is up");
                    }
                    return Boolean.TRUE;
                } else if (vmState == Status.Down) {
                    // We can't ping the VM directly...if we can ping the host, then report the VM down.
                    // If we can't ping the host, then we don't have enough information.
                    Status vmHostState = testIpAddress(otherHost.getId(), vmHost.getPrivateIpAddress());
                    if ((vmHostState != null) && (vmHostState == Status.Up)) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("successfully pinged vm's host IP (" + vmHost.getPrivateIpAddress() + "), but could not ping VM, returning that the VM is down");
                        }
                        return Boolean.FALSE;
                    }
                }
            }
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("unable to determine state of vm (" + vm.getId() + "), returning null");
        }
        return null;
    }

    @Override
    public Status isAgentAlive(HostVO agent) {
        Long hostId = agent.getId();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("checking if agent (" + hostId + ") is alive");
        }
        
        if (agent.getPodId() == null) {
            return null;
        }
        
        List<HostVO> otherHosts = findHostByPod(agent.getPodId(), agent.getId());
        
        for (HostVO otherHost : otherHosts) {

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("sending ping from (" + otherHost.getId() + ") to agent's host ip address (" + agent.getPrivateIpAddress() + ")");
            }
            Status hostState = testIpAddress(otherHost.getId(), agent.getPrivateIpAddress());
            if (hostState == null) {
                continue;
            }
            if (hostState == Status.Up) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("ping from (" + otherHost.getId() + ") to agent's host ip address (" + agent.getPrivateIpAddress() + ") successful, returning that agent is disconnected");
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
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _hostDao = locator.getDao(HostDao.class);
        _routerDao = locator.getDao(DomainRouterDao.class);
        _userVmDao = locator.getDao(UserVmDao.class);
        _agentMgr = locator.getManager(AgentManager.class);

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
    // Host.status is up and Host.type is routing
    private List<HostVO> findHostByPod(long podId, Long excludeHostId) {
        SearchCriteria sc = _hostDao.createSearchCriteria();
        sc.addAnd("podId", SearchCriteria.Op.EQ, podId);
        sc.addAnd("status", SearchCriteria.Op.EQ, Status.Up);
        sc.addAnd("type", SearchCriteria.Op.EQ, Host.Type.Routing);
        if (excludeHostId != null) {
            sc.addAnd("id", SearchCriteria.Op.NEQ, excludeHostId);
        }
        return _hostDao.search(sc, null);
    }

    private Boolean testUserVM(VMInstanceVO vm, Long routerId) {
        DomainRouterVO router = _routerDao.findById(routerId);
        String privateIp = vm.getPrivateIpAddress();
        String routerPrivateIp = router.getPrivateIpAddress();

        List<HostVO> otherHosts = findHostByPod(router.getPodId(), null);
        for (HostVO otherHost : otherHosts) {

            try {
                Answer pingTestAnswer = _agentMgr.send(otherHost.getId(), new PingTestCommand(routerPrivateIp, privateIp), 30 * 1000);
                if (pingTestAnswer.getResult()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("user vm " + vm.getName() + " has been successfully pinged, returning that it is alive");
                    }
                    return Boolean.TRUE;
                } 
            } catch (AgentUnavailableException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Couldn't reach " + e.getAgentId());
                }
                continue;
            } catch (OperationTimedoutException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Operation timed out: " + e.getMessage());
                }
                continue;
            }
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("user vm " + vm.getName() + " could not be pinged, returning that it is unknown");
        }
        return null;
        
    }

    private Status testIpAddress(Long hostId, String testHostIp) {
        try {
            Answer pingTestAnswer = _agentMgr.send(hostId, new PingTestCommand(testHostIp), 30 * 1000);
            if(pingTestAnswer == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("host (" + testHostIp + ") returns null answer");
                }
            	return null;
            }
            
            if (pingTestAnswer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("host (" + testHostIp + ") has been successfully pinged, returning that host is up");
                }
                // computing host is available, but could not reach agent, return false
                return Status.Up;
            } else {
                if (pingTestAnswer.getDetails().startsWith("Unable to ping default route")) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("host (" + hostId + ") cannot ping default route, returning 'I don't know'");
                    }
                    return null;
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("host (" + testHostIp + ") cannot be pinged, returning that host is down");
                }
                return Status.Down;
            }
        } catch (AgentUnavailableException e) {
            return null;
        } catch (OperationTimedoutException e) {
            return null;
        }
    }
}
