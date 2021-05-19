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
package org.apache.cloudstack.kvm.ha;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.resource.ResourceManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

/**
 * This class provides methods that help the KVM HA process on checking hosts status as well as deciding if a host should be fenced/recovered or not.
 */
public class KvmHaHelper {

    @Inject
    private ResourceManager resourceManager;
    @Inject
    private KvmHaAgentClient kvmHaAgentClient;
    @Inject
    private ClusterDao clusterDao;

    private final static Logger LOGGER = Logger.getLogger(KvmHaHelper.class);
    private final static double PROBLEMATIC_HOSTS_RATIO_ACCEPTED = 0.3;

    /**
     * It checks the KVM node status via KVM HA Agent.
     * If the agent is healthy it returns Status.Up, otherwise it keeps the provided Status as it is.
     */
    public Status checkAgentStatusViaKvmHaAgent(Host host, Status agentStatus) {
        boolean isVmsCountOnKvmMatchingWithDatabase = kvmHaAgentClient.isKvmHaAgentHealthy(host);
        if(isVmsCountOnKvmMatchingWithDatabase) {
            agentStatus = Status.Up;
            LOGGER.debug(String.format("Checking agent %s status; KVM HA Agent is Running as expected.", agentStatus));
        } else {
            LOGGER.warn(String.format("Checking agent %s status. Failed to check host status via KVM HA Agent", agentStatus));
        }
        return agentStatus;
    }

    /**
     * Returns false if the cluster has no problematic hosts or a small fraction of it.<br><br>
     * Returns true if the cluster is problematic. A cluster is problematic if many hosts are in Down or Disconnected states, in such case it should not recover/fence.<br>
     * Instead, Admins should be warned and check as it could be networking problems and also might not even have resources capacity on the few Healthy hosts at the cluster.
     */
    private boolean isClusteProblematic(Host host) {
        List<HostVO> hostsInCluster = resourceManager.listAllHostsInCluster(host.getClusterId());
        List<HostVO> problematicNeighbors = resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Down);
        problematicNeighbors.addAll(resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Disconnected));
        problematicNeighbors.addAll(resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Alert));
        problematicNeighbors.addAll(resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Error));
        int problematicHosts = problematicNeighbors.size();

        int problematicHostsRatioAccepted = (int)(hostsInCluster.size() * PROBLEMATIC_HOSTS_RATIO_ACCEPTED);
        if (problematicHosts >= problematicHostsRatioAccepted) {
            ClusterVO cluster = clusterDao.findById(host.getClusterId());
            LOGGER.warn(String.format("%s is problematic but HA will not fence/recover due to its cluster [id: %d, name: %s] containing %d problematic hosts (Down, Disconnected, "
                            + "Alert or Error states). Maximum problematic hosts accepted for this cluster is %d.",
                    host, cluster.getId(), cluster.getName(), problematicHosts, problematicHostsRatioAccepted));
            return true;
        }

        return false;
    }

    private boolean isHostAgentReachableByNeighbour(Host host) {
        List<HostVO> neighbors = resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Up);
        for (HostVO neighbor : neighbors) {
            boolean isVmActivtyOnNeighborHost = kvmHaAgentClient.isKvmHaAgentHealthy(neighbor);
            if(isVmActivtyOnNeighborHost) {
                boolean isReachable = kvmHaAgentClient.isHostReachableByNeighbour(neighbor, host);
                if (isReachable) {
                    String.format( "%s is reachable by neighbour %s. If CloudStack is failing to reach the respective host then it is probably a network issue between the host "
                            + "and CloudStack management server.", host, neighbor);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the host is healthy. The health-check is performed via HTTP GET request to a service that retrieves Running KVM instances via Libvirt. <br>
     * The health-check is executed on the KVM node and verifies the amount of VMs running and if the Libvirt service is running.
     */
    public boolean isVmActivtyOnHostViaKvmHaWebservice(Host host) {
        boolean isKvmHaAgentHealthy = kvmHaAgentClient.isKvmHaAgentHealthy(host);

        if (!isKvmHaAgentHealthy) {
            if (isClusteProblematic(host) || isHostAgentReachableByNeighbour(host)) {
                return true;
            }
        }

        return isKvmHaAgentHealthy;
    }

    /**
     * Checks if the KVM HA webservice is enabled. One can enable or disable it via global settings 'kvm.ha.webservice.enabled'.
     */
    public boolean isKvmHaWebserviceEnabled(Host host) {
        KvmHaAgentClient kvmHaAgentClient = new KvmHaAgentClient();
        if (!kvmHaAgentClient.isKvmHaWebserviceEnabled()) {
            LOGGER.debug(String.format("Skipping KVM HA web-service verification for %s due to 'kvm.ha.webservice.enabled' not enabled.", host));
            return false;
        }
        return true;
    }
}
