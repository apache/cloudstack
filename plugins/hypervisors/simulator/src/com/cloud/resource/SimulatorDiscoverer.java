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
package com.cloud.resource;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.manager.MockAgentManager;
import com.cloud.agent.manager.MockStorageManager;
import com.cloud.dc.ClusterVO;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;

@Local(value = Discoverer.class)
public class SimulatorDiscoverer extends DiscovererBase implements Discoverer, Listener, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(SimulatorDiscoverer.class);

    @Inject
    HostDao _hostDao;
    @Inject
    VMTemplateDao _vmTemplateDao;
    @Inject
    VMTemplateZoneDao _vmTemplateZoneDao;
    @Inject
    AgentManager _agentMgr = null;
    @Inject
    MockAgentManager _mockAgentMgr = null;
    @Inject
    MockStorageManager _mockStorageMgr = null;

    /**
     * Finds ServerResources of an in-process simulator
     *
     * @see com.cloud.resource.Discoverer#find(long, java.lang.Long,
     *      java.lang.Long, java.net.URI, java.lang.String, java.lang.String)
     */
    @Override
    public Map<? extends ServerResource, Map<String, String>>
        find(long dcId, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) throws DiscoveryException {
        Map<AgentResourceBase, Map<String, String>> resources;

        try {
            //http://sim/count=$count, it will add $count number of hosts into the cluster
            String scheme = uri.getScheme();
            String host = uri.getAuthority();
            String commands = URLDecoder.decode(uri.getPath());

            long cpuSpeed = MockAgentManager.DEFAULT_HOST_SPEED_MHZ;
            long cpuCores = MockAgentManager.DEFAULT_HOST_CPU_CORES;
            long memory = MockAgentManager.DEFAULT_HOST_MEM_SIZE;
            long localstorageSize = MockStorageManager.DEFAULT_HOST_STORAGE_SIZE;
            if (scheme.equals("http")) {
                if (host == null || !host.startsWith("sim")) {
                    String msg = "uri is not of simulator type so we're not taking care of the discovery for this: " + uri;
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(msg);
                    }
                    return null;
                }
                if (commands != null) {
                    int index = commands.lastIndexOf("/");
                    if (index != -1) {
                        commands = commands.substring(index + 1);

                        String[] cmds = commands.split("&");
                        for (String cmd : cmds) {
                            String[] parameter = cmd.split("=");
                            if (parameter[0].equalsIgnoreCase("cpuspeed") && parameter[1] != null) {
                                cpuSpeed = Long.parseLong(parameter[1]);
                            } else if (parameter[0].equalsIgnoreCase("cpucore") && parameter[1] != null) {
                                cpuCores = Long.parseLong(parameter[1]);
                            } else if (parameter[0].equalsIgnoreCase("memory") && parameter[1] != null) {
                                memory = Long.parseLong(parameter[1]);
                            } else if (parameter[0].equalsIgnoreCase("localstorage") && parameter[1] != null) {
                                localstorageSize = Long.parseLong(parameter[1]);
                            }
                        }
                    }
                }
            } else {
                String msg = "uriString is not http so we're not taking care of the discovery for this: " + uri;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
                }
                return null;
            }

            String cluster = null;
            if (clusterId == null) {
                String msg = "must specify cluster Id when adding host";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
                }
                throw new RuntimeException(msg);
            } else {
                ClusterVO clu = _clusterDao.findById(clusterId);
                if (clu == null || (clu.getHypervisorType() != HypervisorType.Simulator)) {
                    if (s_logger.isInfoEnabled())
                        s_logger.info("invalid cluster id or cluster is not for Simulator hypervisors");
                    return null;
                }
                cluster = Long.toString(clusterId);
                if (clu.getGuid() == null) {
                    clu.setGuid(UUID.randomUUID().toString());
                }
                _clusterDao.update(clusterId, clu);
            }

            String pod;
            if (podId == null) {
                String msg = "must specify pod Id when adding host";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);
                }
                throw new RuntimeException(msg);
            } else {
                pod = Long.toString(podId);
            }

            Map<String, String> details = new HashMap<String, String>();
            Map<String, Object> params = new HashMap<String, Object>();
            details.put("username", username);
            params.put("username", username);
            details.put("password", password);
            params.put("password", password);
            params.put("zone", Long.toString(dcId));
            params.put("pod", pod);
            params.put("cluster", cluster);
            params.put("cpuspeed", Long.toString(cpuSpeed));
            params.put("cpucore", Long.toString(cpuCores));
            params.put("memory", Long.toString(memory));
            params.put("localstorage", Long.toString(localstorageSize));

            resources = createAgentResources(params);
            return resources;
        } catch (Exception ex) {
            s_logger.error("Exception when discovering simulator hosts: " + ex.getMessage());
        }
        return null;
    }

    private Map<AgentResourceBase, Map<String, String>> createAgentResources(Map<String, Object> params) {
        try {
            s_logger.info("Creating Simulator Resources");
            return _mockAgentMgr.createServerResources(params);
        } catch (Exception ex) {
            s_logger.warn("Caught exception at agent resource creation: " + ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {

        for (HostVO h : hosts) {
            associateTemplatesToZone(h.getId(), h.getDataCenterId());
        }
    }

    private void associateTemplatesToZone(long hostId, long dcId) {
        VMTemplateZoneVO tmpltZone;

        List<VMTemplateVO> allTemplates = _vmTemplateDao.listAll();
        for (VMTemplateVO vt : allTemplates) {
            if (vt.isCrossZones()) {
                tmpltZone = _vmTemplateZoneDao.findByZoneTemplate(dcId, vt.getId());
                if (tmpltZone == null) {
                    VMTemplateZoneVO vmTemplateZone = new VMTemplateZoneVO(dcId, vt.getId(), new Date());
                    _vmTemplateZoneDao.persist(vmTemplateZone);
                }
            }
        }
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.Simulator;
    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        return hypervisor.equalsIgnoreCase(HypervisorType.Simulator.toString());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _agentMgr.registerForHostEvents(this, true, false, false);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {

        /*if(forRebalance)
        return;
        if ( Host.Type.SecondaryStorage == host.getType() ) {
            List<VMTemplateVO> tmplts = _vmTemplateDao.listAll();
            for( VMTemplateVO tmplt : tmplts ) {
                VMTemplateHostVO vmTemplateHost = _vmTemplateHostDao.findByHostTemplate(host.getId(), tmplt.getId());
                if (vmTemplateHost == null) {
                    vmTemplateHost = new VMTemplateHostVO(host.getId(), tmplt.getId(), new Date(), 100,
                            com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED, null, null, null, null, tmplt.getUrl());
                    _vmTemplateHostDao.persist(vmTemplateHost);
                } else {
                    vmTemplateHost.setDownloadState(com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
                    vmTemplateHost.setDownloadPercent(100);
                    _vmTemplateHostDao.update(vmTemplateHost.getId(), vmTemplateHost);
                }
            }
        }*/

    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = ((StartupRoutingCommand)firstCmd);
        if (ssCmd.getHypervisorType() != HypervisorType.Simulator) {
            return null;
        }

        return _resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.Simulator, details, hostTags);
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        return null;
    }

    @Override
    public boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return super.stop();
    }

}
