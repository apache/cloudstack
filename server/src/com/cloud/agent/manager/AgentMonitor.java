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
package com.cloud.agent.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.utils.db.ConnectionConcierge;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.time.InaccurateClock;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class AgentMonitor extends Thread implements AgentMonitorService {
    private static Logger s_logger = Logger.getLogger(AgentMonitor.class);
    private static Logger status_Logger = Logger.getLogger(Status.class);
    private long _pingTimeout = 120; // Default set to 120 seconds
    @Inject private HostDao _hostDao;
    private boolean _stop;
    @Inject private AgentManager _agentMgr;
    @Inject private VMInstanceDao _vmDao;
    @Inject private DataCenterDao _dcDao = null;
    @Inject private HostPodDao _podDao = null;
    @Inject private AlertManager _alertMgr;
    private long _msId;
    @Inject ClusterDao _clusterDao;
    @Inject ResourceManager _resourceMgr;
        
    // private ConnectionConcierge _concierge;
    private Map<Long, Long> _pingMap;

    public AgentMonitor() {
        _pingMap = new ConcurrentHashMap<Long, Long>(10007);
    }
    
    /**
     * Check if the agent is behind on ping
     *
     * @param agentId
     *            agent or host id.
     * @return null if the agent is not kept here. true if behind; false if not.
     */
    public Boolean isAgentBehindOnPing(long agentId) {
        Long pingTime = _pingMap.get(agentId);
        if (pingTime == null) {
            return null;
        }
        return pingTime < (InaccurateClock.getTimeInSeconds() - _pingTimeout);
    }

    public Long getAgentPingTime(long agentId) {
        return _pingMap.get(agentId);
    }

    public void pingBy(long agentId) {
        _pingMap.put(agentId, InaccurateClock.getTimeInSeconds());
    }

    // TODO : use host machine time is not safe in clustering environment
    @Override
    public void run() {
        s_logger.info("Agent Monitor is started.");

        while (!_stop) {
            try {
                // check every 60 seconds
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
                s_logger.info("Who woke me from my slumber?");
            }

            try {
                List<Long> behindAgents = findAgentsBehindOnPing();
                for (Long agentId : behindAgents) {
                	SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
                	sc.addAnd(sc.getEntity().getId(), Op.EQ, agentId);
                	HostVO h = sc.find();
                    if (h != null) {
                        ResourceState resourceState = h.getResourceState();
                        if (resourceState == ResourceState.Disabled || resourceState == ResourceState.Maintenance
                                || resourceState == ResourceState.ErrorInMaintenance) {
                            /*
                             * Host is in non-operation state, so no
                             * investigation and direct put agent to
                             * Disconnected
                             */
                            status_Logger.debug("Ping timeout but host " + agentId + " is in resource state of "
                                    + resourceState + ", so no investigation");
                            _agentMgr.disconnectWithoutInvestigation(agentId, Event.ShutdownRequested);
                        } else {
                            status_Logger.debug("Ping timeout for host " + agentId + ", do invstigation");
                            _agentMgr.disconnectWithInvestigation(agentId, Event.PingTimeout);
                        }
                	}
                }

                SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
                sc.addAnd(sc.getEntity().getResourceState(), Op.IN, ResourceState.PrepareForMaintenance, ResourceState.ErrorInMaintenance);
                List<HostVO> hosts = sc.list();

                for (HostVO host : hosts) {
                    long hostId = host.getId();
                    DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
                    HostPodVO podVO = _podDao.findById(host.getPodId());
                    String hostDesc = "name: " + host.getName() + " (id:" + hostId + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();

                    if (host.getType() != Host.Type.Storage) {
                        List<VMInstanceVO> vos = _vmDao.listByHostId(hostId);
                        List<VMInstanceVO> vosMigrating = _vmDao.listVmsMigratingFromHost(hostId);
                        if (vos.isEmpty() && vosMigrating.isEmpty()) {
                            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Migration Complete for host " + hostDesc, "Host [" + hostDesc + "] is ready for maintenance");
                            _resourceMgr.resourceStateTransitTo(host, ResourceState.Event.InternalEnterMaintenance, _msId);
                        }
                    }
                }
            } catch (Throwable th) {
                s_logger.error("Caught the following exception: ", th);
            }
        }

        s_logger.info("Agent Monitor is leaving the building!");
    }

    public void signalStop() {
        _stop = true;
        interrupt();
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override @DB
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        boolean processed = false;
        for (Command cmd : commands) {
            if (cmd instanceof PingCommand) {
                pingBy(agentId);
            }
        }
        return processed;
    }

    protected List<Long> findAgentsBehindOnPing() {
        List<Long> agentsBehind = new ArrayList<Long>();
        long cutoffTime = InaccurateClock.getTimeInSeconds() - _pingTimeout;
        for (Map.Entry<Long, Long> entry : _pingMap.entrySet()) {
            if (entry.getValue() < cutoffTime) {
                agentsBehind.add(entry.getKey());
            }
        }

        if (agentsBehind.size() > 0) {
            s_logger.info("Found the following agents behind on ping: " + agentsBehind);
        }

        return agentsBehind;
    }

    /**
     * @deprecated We're using the in-memory
     */
    @Deprecated
    protected List<HostVO> findHostsBehindOnPing() {
        long time = (System.currentTimeMillis() >> 10) - _pingTimeout;
        List<HostVO> hosts = _hostDao.findLostHosts(time);
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Found " + hosts.size() + " hosts behind on ping. pingTimeout : " + _pingTimeout +
                    ", mark time : " + time);
        }

        for (HostVO host : hosts) {
            if (host.getType().equals(Host.Type.ExternalFirewall) ||
                    host.getType().equals(Host.Type.ExternalLoadBalancer) ||
                    host.getType().equals(Host.Type.TrafficMonitor) ||
                    host.getType().equals(Host.Type.SecondaryStorage)) {
                continue;
            }

            if (host.getManagementServerId() == null || host.getManagementServerId() == _msId) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Asking agent mgr to investgate why host " + host.getId() +
                            " is behind on ping. last ping time: " + host.getLastPinged());
                }
                _agentMgr.disconnectWithInvestigation(host.getId(), Event.PingTimeout);
            }
        }

        return hosts;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) {
        if (host.getType().equals(Host.Type.TrafficMonitor) ||
                host.getType().equals(Host.Type.SecondaryStorage)) {
            return;
        }

        // NOTE: We don't use pingBy here because we're initiating.
        _pingMap.put(host.getId(), InaccurateClock.getTimeInSeconds());
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        _pingMap.remove(agentId);
        return true;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return true;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public void startMonitoring(long pingTimeout) {
        _pingTimeout = pingTimeout;
    	start();
    }
}

