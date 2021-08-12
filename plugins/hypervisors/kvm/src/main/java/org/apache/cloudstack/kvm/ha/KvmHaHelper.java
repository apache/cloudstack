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
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides methods that help the KVM HA process on checking hosts status as well as deciding if a host should be fenced/recovered or not.
 */
public class KvmHaHelper {

    @Inject
    protected ResourceManager resourceManager;
    @Inject
    protected KvmHaAgentClient kvmHaAgentClient;
    @Inject
    protected ClusterDao clusterDao;

    private static final Logger LOGGER = Logger.getLogger(KvmHaHelper.class);
    private static final int CAUTIOUS_MARGIN_OF_VMS_ON_HOST = 1;

    private static final Set<Status> PROBLEMATIC_HOST_STATUS = new HashSet<>(Arrays.asList(Status.Alert, Status.Disconnected, Status.Down, Status.Error));

    /**
     * It checks the KVM node status via KVM HA Agent.
     * If the agent is healthy it returns Status.Up, otherwise it keeps the provided Status as it is.
     */
    public Status checkAgentStatusViaKvmHaAgent(Host host, Status agentStatus) {
        boolean isVmsCountOnKvmMatchingWithDatabase = isKvmHaAgentHealthy(host);
        if (isVmsCountOnKvmMatchingWithDatabase) {
            agentStatus = Status.Up;
            LOGGER.debug(String.format("Checking agent %s status; KVM HA Agent is Running as expected.", agentStatus));
        } else {
            LOGGER.warn(String.format("Checking agent %s status. Failed to check host status via KVM HA Agent", agentStatus));
        }
        return agentStatus;
    }

    /**
     * Given a List of Hosts, it lists Hosts that are in the following states:
     * <ul>
     *  <li> Status.Alert;
     *  <li> Status.Disconnected;
     *  <li> Status.Down;
     *  <li> Status.Error.
     * </ul>
     */
    @NotNull
    protected List<HostVO> listProblematicHosts(List<HostVO> hostsInCluster) {
        return hostsInCluster.stream().filter(neighbour -> PROBLEMATIC_HOST_STATUS.contains(neighbour.getStatus())).collect(Collectors.toList());
    }

    /**
     * Returns false if the cluster has no problematic hosts or a small fraction of it.<br><br>
     * Returns true if the cluster is problematic. A cluster is problematic if many hosts are in Down or Disconnected states, in such case it should not recover/fence.<br>
     * Instead, Admins should be warned and check as it could be networking problems and also might not even have resources capacity on the few Healthy hosts at the cluster.
     * <br><br>
     * Admins can change the accepted ration of problematic hosts via global settings by updating configuration: "kvm.ha.accepted.problematic.hosts.ratio".
     */
    protected boolean isClusteProblematic(Host host) {
        List<HostVO> hostsInCluster = resourceManager.listAllHostsInCluster(host.getClusterId());
        List<HostVO> problematicNeighbors = listProblematicHosts(hostsInCluster);
        int problematicHosts = problematicNeighbors.size();
        double acceptedProblematicHostsRatio = KVMHAConfig.KvmHaAcceptedProblematicHostsRatio.valueIn(host.getClusterId());
        int problematicHostsRatioAccepted = (int) (hostsInCluster.size() * acceptedProblematicHostsRatio);

        if (problematicHosts > problematicHostsRatioAccepted) {
            ClusterVO cluster = clusterDao.findById(host.getClusterId());
            LOGGER.warn(String.format("%s is problematic but HA will not fence/recover due to its cluster [id: %d, name: %s] containing %d problematic hosts (Down, Disconnected, "
                            + "Alert or Error states). Maximum problematic hosts accepted for this cluster is %d.",
                    host, cluster.getId(), cluster.getName(), problematicHosts, problematicHostsRatioAccepted));
            return true;
        }
        return false;
    }

    /**
     * Returns true if the given Host KVM-HA-Helper is reachable by another host in the same cluster.
     */
    protected boolean isHostAgentReachableByNeighbour(Host host) {
        List<HostVO> neighbors = resourceManager.listHostsInClusterByStatus(host.getClusterId(), Status.Up);
        for (HostVO neighbor : neighbors) {
            boolean isVmActivtyOnNeighborHost = isKvmHaAgentHealthy(neighbor);
            if (isVmActivtyOnNeighborHost) {
                boolean isReachable = kvmHaAgentClient.isHostReachableByNeighbour(neighbor, host);
                if (isReachable) {
                    String.format("%s is reachable by neighbour %s. If CloudStack is failing to reach the respective host then it is probably a network issue between the host "
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
    public boolean isKvmHealthyCheckViaLibvirt(Host host) {
        boolean isKvmHaAgentHealthy = isKvmHaAgentHealthy(host);
        if (!isKvmHaAgentHealthy && (isClusteProblematic(host) || isHostAgentReachableByNeighbour(host))) {
            return true;
        }
        return isKvmHaAgentHealthy;
    }

    /**
     * Checks if the KVM HA webservice is enabled. One can enable or disable it via global settings 'kvm.ha.webservice.enabled'.
     */
    public boolean isKvmHaWebserviceEnabled(Host host) {
        boolean isKvmHaWebserviceEnabled = KVMHAConfig.IsKvmHaWebserviceEnabled.value();
        if (!isKvmHaWebserviceEnabled) {
            LOGGER.debug(String.format("Skipping KVM HA web-service verification for %s due to 'kvm.ha.webservice.enabled' not enabled.", host));
            return false;
        }
        return true;
    }

    /**
     *  Returns true in case of the expected number of VMs matches with the VMs running on the KVM host according to Libvirt. <br><br>
     *
     *  IF: <br>
     *  (i) KVM HA agent finds 0 running but CloudStack considers that the host has 2 or more VMs running: returns false as could not find VMs running but it expected at least
     *    2 VMs running, fencing/recovering host would avoid downtime to VMs in this case.<br>
     *  (ii) KVM HA agent finds 0 VM running but CloudStack considers that the host has 1 VM running: return true and log WARN messages and avoids triggering HA recovery/fencing
     *    when it could be a inconsistency when migrating a VM.<br>
     *  (iii) amount of listed VMs is different than expected: return true and print WARN messages so Admins can monitor and react accordingly
     */
    public boolean isKvmHaAgentHealthy(Host host) {
        int numberOfVmsOnHostAccordingToDb = kvmHaAgentClient.listVmsOnHost(host).size();
        int numberOfVmsOnAgent = kvmHaAgentClient.countRunningVmsOnAgent(host);

        if (numberOfVmsOnAgent < 0) {
            LOGGER.error(String.format("KVM HA Agent health check failed, either the KVM Agent %s is unreachable or Libvirt validation failed.", host));
            if (isHostAgentReachableByNeighbour(host)) {
                return true;
            }
            logIfFencingOrRecoveringMightBeTriggered(host);
            return false;
        }

        if (numberOfVmsOnHostAccordingToDb == numberOfVmsOnAgent) {
            return true;
        }

        if (numberOfVmsOnAgent == 0 && numberOfVmsOnHostAccordingToDb > CAUTIOUS_MARGIN_OF_VMS_ON_HOST) {
            LOGGER.warn(String.format("KVM HA Agent %s could not find VMs; it was expected to list %d VMs.", host, numberOfVmsOnHostAccordingToDb));
            logIfFencingOrRecoveringMightBeTriggered(host);
            return false;
        }

        LOGGER.warn(String.format("KVM HA Agent %s listed %d VMs; however, it was expected %d VMs.", host, numberOfVmsOnAgent, numberOfVmsOnHostAccordingToDb));
        return true;
    }

    private void logIfFencingOrRecoveringMightBeTriggered(Host agent) {
        LOGGER.warn(String.format("Host %s is not considered healthy and HA fencing/recovering process might be triggered.", agent.getName()));
    }
}
