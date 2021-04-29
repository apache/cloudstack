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
package com.cloud.ha;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.ha.HAManager;
import org.apache.cloudstack.kvm.ha.KvmHaAgentClient;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

public class KVMInvestigator extends AdapterBase implements Investigator {
    private final static Logger s_logger = Logger.getLogger(KVMInvestigator.class);
    @Inject
    private HostDao _hostDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private HAManager haManager;
    @Inject
    private VMInstanceDao vmInstanceDao;

    @Override
    public boolean isVmAlive(com.cloud.vm.VirtualMachine vm, Host host) throws UnknownVM {
        if (haManager.isHAEligible(host)) {
            return haManager.isVMAliveOnHost(host);
        }
        Status status = isAgentAlive(host);
        s_logger.debug("HA: HOST is ineligible legacy state " + status + " for host " + host.getId());
        if (status == null) {
            throw new UnknownVM();
        }
        if (status == Status.Up) {
            return true;
        } else {
            throw new UnknownVM();
        }
    }

    @Override
    public Status isAgentAlive(Host agent) {
        if (agent.getHypervisorType() != Hypervisor.HypervisorType.KVM && agent.getHypervisorType() != Hypervisor.HypervisorType.LXC) {
            return null;
        }

        if (haManager.isHAEligible(agent)) {
            return haManager.getHostStatus(agent);
        }

        Status agentStatus = Status.Disconnected;
        boolean hasNfs = isHostServedByNfsPool(agent);
        if (hasNfs) {
            agentStatus = checkAgentStatusViaNfs(agent);
            s_logger.debug(String.format("Agent investigation was requested on host %s. Agent status via NFS heartbeat is %s.", agent, agentStatus));
        } else {
            s_logger.debug(String.format("Agent investigation was requested on host %s, but host has no NFS storage. Skipping investigation via NFS.", agent));
        }

        agentStatus = checkAgentStatusViaKvmHaAgent(agent, agentStatus);

        return agentStatus;
    }

    /**
     * It checks the KVM node healthy via KVM HA Agent. If the agent is healthy it returns Status.Up, otherwise it relies keeps the provided Status as it is.
     */
    private Status checkAgentStatusViaKvmHaAgent(Host agent, Status agentStatus) {
        KvmHaAgentClient kvmHaAgentClient = new KvmHaAgentClient(agent);
        boolean isVmsCountOnKvmMatchingWithDatabase = kvmHaAgentClient.isKvmHaAgentHealthy(agent, vmInstanceDao);
        if(isVmsCountOnKvmMatchingWithDatabase) {
            agentStatus = Status.Up;
            s_logger.debug(String.format("Checking agent %s status; KVM HA Agent is Running as expected."));
        } else {
            s_logger.warn(String.format("Checking agent %s status. Failed to check host status via KVM HA Agent"));
        }
        return agentStatus;
    }

    private boolean isHostServedByNfsPool(Host agent) {
        boolean hasNfs = hasNfsPoolClusterWideForHost(agent);
        if (!hasNfs) {
            hasNfs = hasNfsPoolZoneWideForHost(agent);
        }
        return hasNfs;
    }

    private boolean hasNfsPoolZoneWideForHost(Host agent) {
        List<StoragePoolVO> zonePools = _storagePoolDao.findZoneWideStoragePoolsByHypervisor(agent.getDataCenterId(), agent.getHypervisorType());
        for (StoragePoolVO pool : zonePools) {
            if (pool.getPoolType() == StoragePoolType.NetworkFilesystem) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNfsPoolClusterWideForHost(Host agent) {
        List<StoragePoolVO> clusterPools = _storagePoolDao.listPoolsByCluster(agent.getClusterId());
        for (StoragePoolVO pool : clusterPools) {
            if (pool.getPoolType() == StoragePoolType.NetworkFilesystem) {
                return true;
            }
        }
        return false;
    }

    private Status checkAgentStatusViaNfs(Host agent) {
        Status hostStatus = null;
        Status neighbourStatus = null;
        CheckOnHostCommand cmd = new CheckOnHostCommand(agent);

        try {
            Answer answer = _agentMgr.easySend(agent.getId(), cmd);
            if (answer != null) {
                hostStatus = answer.getResult() ? Status.Down : Status.Up;
            }
        } catch (Exception e) {
            s_logger.debug("Failed to send command to host: " + agent.getId());
        }
        if (hostStatus == null) {
            hostStatus = Status.Disconnected;
        }

        List<HostVO> neighbors = _resourceMgr.listHostsInClusterByStatus(agent.getClusterId(), Status.Up);
        for (HostVO neighbor : neighbors) {
            if (neighbor.getId() == agent.getId()
                    || (neighbor.getHypervisorType() != Hypervisor.HypervisorType.KVM && neighbor.getHypervisorType() != Hypervisor.HypervisorType.LXC)) {
                continue;
            }
            s_logger.debug("Investigating host:" + agent.getId() + " via neighbouring host:" + neighbor.getId());
            try {
                Answer answer = _agentMgr.easySend(neighbor.getId(), cmd);
                if (answer != null) {
                    neighbourStatus = answer.getResult() ? Status.Down : Status.Up;
                    s_logger.debug("Neighbouring host:" + neighbor.getId() + " returned status:" + neighbourStatus + " for the investigated host:" + agent.getId());
                    if (neighbourStatus == Status.Up) {
                        break;
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Failed to send command to host: " + neighbor.getId());
            }
        }
        if (neighbourStatus == Status.Up && (hostStatus == Status.Disconnected || hostStatus == Status.Down)) {
            hostStatus = Status.Disconnected;
        }
        if (neighbourStatus == Status.Down && (hostStatus == Status.Disconnected || hostStatus == Status.Down)) {
            hostStatus = Status.Down;
        }
        s_logger.debug("HA: HOST is ineligible legacy state " + hostStatus + " for host " + agent.getId());
        return hostStatus;
    }
}
