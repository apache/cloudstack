/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.StartupCommandProcessor;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostEnvironment;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.vmware.VmwareServerDiscoverer;
import com.cloud.hypervisor.xenserver.resource.XcpOssResource;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public class DirectAgentManagerSimpleImpl extends ManagerBase implements AgentManager {
    private final Map<Long, ServerResource> hostResourcesMap = new HashMap<Long, ServerResource>();
    @Inject
    HostDao hostDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    ClusterDetailsDao clusterDetailsDao;
    @Inject
    HostDao _hostDao;
    protected StateMachine2<Status, Event, Host> _statusStateMachine = Status.getStateMachine();

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void rescan() {
    }

    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer easySend(Long hostId, Command cmd) {
        try {
            return this.send(hostId, cmd);
        } catch (AgentUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationTimedoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    protected void loadResource(Long hostId) {
        HostVO host = hostDao.findById(hostId);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("guid", host.getGuid());
        params.put("ipaddress", host.getPrivateIpAddress());
        params.put("username", "root");
        params.put("password", "password");
        params.put("zone", String.valueOf(host.getDataCenterId()));
        params.put("pod", String.valueOf(host.getPodId()));

        ServerResource resource = null;
        if (host.getHypervisorType() == HypervisorType.XenServer) {
            resource = new XcpOssResource();
            try {
                resource.configure(host.getName(), params);

            } catch (ConfigurationException e) {
                logger.debug("Failed to load resource:" + e.toString());
            }
        } else if (host.getHypervisorType() == HypervisorType.KVM) {
            resource = new LibvirtComputingResource();
            try {
                params.put("public.network.device", "cloudbr0");
                params.put("private.network.device", "cloudbr0");
                resource.configure(host.getName(), params);
            } catch (ConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (host.getHypervisorType() == HypervisorType.VMware) {
            ClusterVO cluster = clusterDao.findById(host.getClusterId());
            String url = clusterDetailsDao.findDetail(cluster.getId(), "url").getValue();
            URI uri;
            try {
                uri = new URI(url);
                String userName = clusterDetailsDao.findDetail(cluster.getId(), "username").getValue();
                String password = clusterDetailsDao.findDetail(cluster.getId(), "password").getValue();
                VmwareServerDiscoverer discover = new VmwareServerDiscoverer();

                Map<? extends ServerResource, Map<String, String>> resources =
                        discover.find(host.getDataCenterId(), host.getPodId(), host.getClusterId(), uri, userName, password, null);
                for (Map.Entry<? extends ServerResource, Map<String, String>> entry : resources.entrySet()) {
                    resource = entry.getKey();
                }
                if (resource == null) {
                    throw new CloudRuntimeException("can't find resource");
                }
            } catch (DiscoveryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        hostResourcesMap.put(hostId, resource);
        HostEnvironment env = new HostEnvironment();
        SetupCommand cmd = new SetupCommand(env);
        cmd.setNeedSetup(true);

        resource.executeRequest(cmd);
    }

    @Override
    public synchronized Answer send(Long hostId, Command cmd) throws AgentUnavailableException, OperationTimedoutException {
        ServerResource resource = hostResourcesMap.get(hostId);
        if (resource == null) {
            loadResource(hostId);
            resource = hostResourcesMap.get(hostId);
        }

        if (resource == null) {
            return null;
        }

        Answer answer = resource.executeRequest(cmd);
        return answer;
    }

    @Override
    public Answer[] send(Long hostId, Commands cmds) throws AgentUnavailableException, OperationTimedoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Answer[] send(Long hostId, Commands cmds, int timeout) throws AgentUnavailableException, OperationTimedoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long send(Long hostId, Commands cmds, Listener listener) throws AgentUnavailableException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int registerForHostEvents(Listener listener, boolean connections, boolean commands, boolean priority) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int registerForInitialConnects(StartupCommandProcessor creator, boolean priority) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void unregisterForHostEvents(int id) {
        // TODO Auto-generated method stub

    }

    @Override
    public Answer sendTo(Long dcId, HypervisorType type, Command cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean agentStatusTransitTo(HostVO host, Event e, long msId) {
        try {
            return _statusStateMachine.transitTo(host, e, host.getId(), _hostDao);
        } catch (NoTransitionException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return true;

    }

    @Override
    public void disconnectWithoutInvestigation(long hostId, Event event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pullAgentToMaintenance(long hostId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void pullAgentOutMaintenance(long hostId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reconnect(long hostId) {
    }

    @Override
    public boolean isAgentAttached(long hostId) {
        return false;
    }

    @Override
    public boolean handleDirectConnectAgent(Host host, StartupCommand[] cmds, ServerResource resource, boolean forRebalance, boolean newHost) throws ConnectionException {
        return false;
    }

    @Override
    public void notifyMonitorsOfHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void notifyMonitorsOfRemovedHost(long hostId, long clusterId) {
    }

    @Override
    public void disconnectWithInvestigation(long hostId, Event event) {

    }

    @Override
    public void notifyMonitorsOfNewlyAddedHost(long hostId) {
    }
}
