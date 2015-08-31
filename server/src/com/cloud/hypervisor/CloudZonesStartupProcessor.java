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
package com.cloud.hypervisor;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.ResourceDetail;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.StartupCommandProcessor;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.manager.authn.AgentAuthnException;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ZoneConfig;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;

/**
 * Creates a host record and supporting records such as pod and ip address
 *
 */
@Component
@Local(value = StartupCommandProcessor.class)
public class CloudZonesStartupProcessor extends AdapterBase implements StartupCommandProcessor {
    private static final Logger s_logger = Logger.getLogger(CloudZonesStartupProcessor.class);
    @Inject
    ClusterDao _clusterDao = null;
    @Inject
    ConfigurationDao _configDao = null;
    @Inject
    DataCenterDao _zoneDao = null;
    @Inject
    HostDao _hostDao = null;
    @Inject
    HostPodDao _podDao = null;
    @Inject
    DataCenterDetailsDao _zoneDetailsDao = null;

    @Inject
    AgentManager _agentManager = null;
    @Inject
    ConfigurationManager _configurationManager = null;

    long _nodeId = -1;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _agentManager.registerForInitialConnects(this, false);
        if (_nodeId == -1) {
            // FIXME: We really should not do this like this. It should be done
            // at config time and is stored as a config variable.
            _nodeId = MacAddress.getMacAddress().toLong();
        }
        return true;
    }

    @Override
    public boolean processInitialConnect(StartupCommand[] cmd) throws ConnectionException {
        StartupCommand startup = cmd[0];
        if (startup instanceof StartupRoutingCommand) {
            return processHostStartup((StartupRoutingCommand)startup);
        } else if (startup instanceof StartupStorageCommand) {
            return processStorageStartup((StartupStorageCommand)startup);
        }

        return false;
    }

    protected boolean processHostStartup(StartupRoutingCommand startup) throws ConnectionException {
        /*
        boolean found = false;
        Type type = Host.Type.Routing;
        final Map<String, String> hostDetails = startup.getHostDetails();
        HostVO server = _hostDao.findByGuid(startup.getGuid());
        if (server == null) {
            server = _hostDao.findByGuid(startup.getGuidWithoutResource());
        }
        if (server != null && server.getRemoved() == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found the host " + server.getId() + " by guid: "
                        + startup.getGuid());
            }
            found = true;

        } else {
            server = new HostVO(startup.getGuid());
        }
        server.setDetails(hostDetails);

        try {
            updateComputeHost(server, startup, type);
        } catch (AgentAuthnException e) {
            throw new ConnectionException(true, "Failed to authorize host, invalid configuration", e);
        }
        if (!found) {
            server.setHostAllocationState(Host.HostAllocationState.Enabled);
            server = _hostDao.persist(server);
        } else {
            if (!_hostDao.connect(server, _nodeId)) {
                throw new CloudRuntimeException(
                        "Agent cannot connect because the current state is "
                        + server.getStatus().toString());
            }
            s_logger.info("Old " + server.getType().toString()
                    + " host reconnected w/ id =" + server.getId());
        }
        */
        return true;

    }

    protected void updateComputeHost(final HostVO host, final StartupCommand startup, final Host.Type type) throws AgentAuthnException {

        String zoneToken = startup.getDataCenter();
        if (zoneToken == null) {
            s_logger.warn("No Zone Token passed in, cannot not find zone for the agent");
            throw new AgentAuthnException("No Zone Token passed in, cannot not find zone for agent");
        }

        DataCenterVO zone = _zoneDao.findByToken(zoneToken);
        if (zone == null) {
            zone = _zoneDao.findByName(zoneToken);
            if (zone == null) {
                try {
                    long zoneId = Long.parseLong(zoneToken);
                    zone = _zoneDao.findById(zoneId);
                    if (zone == null) {
                        throw new AgentAuthnException("Could not find zone for agent with token " + zoneToken);
                    }
                } catch (NumberFormatException nfe) {
                    throw new AgentAuthnException("Could not find zone for agent with token " + zoneToken);
                }
            }
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully loaded the DataCenter from the zone token passed in ");
        }

        long zoneId = zone.getId();
        ResourceDetail maxHostsInZone = _zoneDetailsDao.findDetail(zoneId, ZoneConfig.MaxHosts.key());
        if (maxHostsInZone != null) {
            long maxHosts = Long.parseLong(maxHostsInZone.getValue());
            long currentCountOfHosts = _hostDao.countRoutingHostsByDataCenter(zoneId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Number of hosts in Zone:" + currentCountOfHosts + ", max hosts limit: " + maxHosts);
            }
            if (currentCountOfHosts >= maxHosts) {
                throw new AgentAuthnException("Number of running Routing hosts in the Zone:" + zone.getName() + " is already at the max limit:" + maxHosts +
                    ", cannot start one more host");
            }
        }

        HostPodVO pod = null;

        if (startup.getPrivateIpAddress() == null) {
            s_logger.warn("No private IP address passed in for the agent, cannot not find pod for agent");
            throw new AgentAuthnException("No private IP address passed in for the agent, cannot not find pod for agent");
        }

        if (startup.getPrivateNetmask() == null) {
            s_logger.warn("No netmask passed in for the agent, cannot not find pod for agent");
            throw new AgentAuthnException("No netmask passed in for the agent, cannot not find pod for agent");
        }

        if (host.getPodId() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Pod is already created for this agent, looks like agent is reconnecting...");
            }
            pod = _podDao.findById(host.getPodId());
            if (!checkCIDR(type, pod, startup.getPrivateIpAddress(), startup.getPrivateNetmask())) {
                pod = null;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Subnet of Pod does not match the subnet of the agent, not using this Pod: " + host.getPodId());
                }
            } else {
                updatePodNetmaskIfNeeded(pod, startup.getPrivateNetmask());
            }
        }

        if (pod == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to detect the Pod to use from the agent's ip address and netmask passed in ");
            }

            //deduce pod
            boolean podFound = false;
            List<HostPodVO> podsInZone = _podDao.listByDataCenterId(zoneId);
            for (HostPodVO hostPod : podsInZone) {
                if (checkCIDR(type, hostPod, startup.getPrivateIpAddress(), startup.getPrivateNetmask())) {
                    pod = hostPod;

                    //found the default POD having the same subnet.
                    updatePodNetmaskIfNeeded(pod, startup.getPrivateNetmask());
                    podFound = true;
                    break;
                }
            }

            if (!podFound) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating a new Pod since no default Pod found that matches the agent's ip address and netmask passed in ");
                }

                if (startup.getGatewayIpAddress() == null) {
                    s_logger.warn("No Gateway IP address passed in for the agent, cannot create a new pod for the agent");
                    throw new AgentAuthnException("No Gateway IP address passed in for the agent, cannot create a new pod for the agent");
                }
                //auto-create a new pod, since pod matching the agent's ip is not found
                String podName = "POD-" + (podsInZone.size() + 1);
                try {
                    String gateway = startup.getGatewayIpAddress();
                    String cidr = NetUtils.getCidrFromGatewayAndNetmask(gateway, startup.getPrivateNetmask());
                    String[] cidrPair = cidr.split("\\/");
                    String cidrAddress = cidrPair[0];
                    long cidrSize = Long.parseLong(cidrPair[1]);
                    String startIp = NetUtils.getIpRangeStartIpFromCidr(cidrAddress, cidrSize);
                    String endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
                    pod = _configurationManager.createPod(-1, podName, zoneId, gateway, cidr, startIp, endIp, null, true);
                } catch (Exception e) {
                    // no longer tolerate exception during the cluster creation phase
                    throw new CloudRuntimeException("Unable to create new Pod " + podName + " in Zone: " + zoneId, e);
                }

            }
        }
        final StartupRoutingCommand scc = (StartupRoutingCommand)startup;

        ClusterVO cluster = null;
        if (host.getClusterId() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Cluster is already created for this agent, looks like agent is reconnecting...");
            }
            cluster = _clusterDao.findById(host.getClusterId());
        }
        if (cluster == null) {
            //auto-create cluster - assume one host per cluster
            String clusterName = "Cluster-" + startup.getPrivateIpAddress();
            ClusterVO existingCluster = _clusterDao.findBy(clusterName, pod.getId());
            if (existingCluster != null) {
                cluster = existingCluster;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating a new Cluster for this agent with name: " + clusterName + " in Pod: " + pod.getId() + ", in Zone:" + zoneId);
                }

                cluster = new ClusterVO(zoneId, pod.getId(), clusterName);
                cluster.setHypervisorType(scc.getHypervisorType().toString());
                try {
                    cluster = _clusterDao.persist(cluster);
                } catch (Exception e) {
                    // no longer tolerate exception during the cluster creation phase
                    throw new CloudRuntimeException("Unable to create cluster " + clusterName + " in pod " + pod.getId() + " and data center " + zoneId, e);
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Detected Zone: " + zoneId + ", Pod: " + pod.getId() + ", Cluster:" + cluster.getId());
        }
        host.setDataCenterId(zone.getId());
        host.setPodId(pod.getId());
        host.setClusterId(cluster.getId());
        host.setPrivateIpAddress(startup.getPrivateIpAddress());
        host.setPrivateNetmask(startup.getPrivateNetmask());
        host.setPrivateMacAddress(startup.getPrivateMacAddress());
        host.setPublicIpAddress(startup.getPublicIpAddress());
        host.setPublicMacAddress(startup.getPublicMacAddress());
        host.setPublicNetmask(startup.getPublicNetmask());
        host.setStorageIpAddress(startup.getStorageIpAddress());
        host.setStorageMacAddress(startup.getStorageMacAddress());
        host.setStorageNetmask(startup.getStorageNetmask());
        host.setVersion(startup.getVersion());
        host.setName(startup.getName());
        host.setType(type);
        host.setStorageUrl(startup.getIqn());
        host.setLastPinged(System.currentTimeMillis() >> 10);
        host.setCaps(scc.getCapabilities());
        host.setCpus(scc.getCpus());
        host.setTotalMemory(scc.getMemory());
        host.setSpeed(scc.getSpeed());
        HypervisorType hyType = scc.getHypervisorType();
        host.setHypervisorType(hyType);
        host.setHypervisorVersion(scc.getHypervisorVersion());

    }

    private boolean checkCIDR(Host.Type type, HostPodVO pod, String serverPrivateIP, String serverPrivateNetmask) {
        if (serverPrivateIP == null) {
            return true;
        }
        // Get the CIDR address and CIDR size
        String cidrAddress = pod.getCidrAddress();
        long cidrSize = pod.getCidrSize();

        // If the server's private IP address is not in the same subnet as the
        // pod's CIDR, return false
        String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSize);
        String serverSubnet = NetUtils.getSubNet(serverPrivateIP, serverPrivateNetmask);
        if (!cidrSubnet.equals(serverSubnet)) {
            return false;
        }
        return true;
    }

    private void updatePodNetmaskIfNeeded(HostPodVO pod, String agentNetmask) {
        // If the server's private netmask is less inclusive than the pod's CIDR
        // netmask, update cidrSize of the default POD
        //(reason: we are maintaining pods only for internal accounting.)
        long cidrSize = pod.getCidrSize();
        String cidrNetmask = NetUtils.getCidrSubNet("255.255.255.255", cidrSize);
        long cidrNetmaskNumeric = NetUtils.ip2Long(cidrNetmask);
        long serverNetmaskNumeric = NetUtils.ip2Long(agentNetmask);//
        if (serverNetmaskNumeric > cidrNetmaskNumeric) {
            //update pod's cidrsize
            int newCidrSize = new Long(NetUtils.getCidrSize(agentNetmask)).intValue();
            pod.setCidrSize(newCidrSize);
            _podDao.update(pod.getId(), pod);
        }
    }

    protected boolean processStorageStartup(StartupStorageCommand startup) throws ConnectionException {
        /*
        if (startup.getResourceType() != Storage.StorageResourceType.LOCAL_SECONDARY_STORAGE) {
            return false;
        }
        boolean found = false;
        Type type = Host.Type.LocalSecondaryStorage;
        final Map<String, String> hostDetails = startup.getHostDetails();
        HostVO server = _hostDao.findByGuid(startup.getGuid());
        if (server == null) {
            server = _hostDao.findByGuid(startup.getGuidWithoutResource());
        }
        if (server != null && server.getRemoved() == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found the host " + server.getId() + " by guid: "
                        + startup.getGuid());
            }
            found = true;

        } else {
            server = new HostVO(startup.getGuid());
        }
        server.setDetails(hostDetails);

        try {
            updateSecondaryHost(server, startup, type);
        } catch (AgentAuthnException e) {
            throw new ConnectionException(true, "Failed to authorize host, invalid configuration", e);
        }
        if (!found) {
            server.setHostAllocationState(Host.HostAllocationState.Enabled);
            server = _hostDao.persist(server);
        } else {
            if (!_hostDao.connect(server, _nodeId)) {
                throw new CloudRuntimeException(
                        "Agent cannot connect because the current state is "
                        + server.getStatus().toString());
            }
            s_logger.info("Old " + server.getType().toString()
                    + " host reconnected w/ id =" + server.getId());
        }
        */
        return true;

    }

    protected void updateSecondaryHost(final HostVO host, final StartupStorageCommand startup, final Host.Type type) throws AgentAuthnException {

        String zoneToken = startup.getDataCenter();
        if (zoneToken == null) {
            s_logger.warn("No Zone Token passed in, cannot not find zone for the agent");
            throw new AgentAuthnException("No Zone Token passed in, cannot not find zone for agent");
        }

        DataCenterVO zone = _zoneDao.findByToken(zoneToken);
        if (zone == null) {
            zone = _zoneDao.findByName(zoneToken);
            if (zone == null) {
                try {
                    long zoneId = Long.parseLong(zoneToken);
                    zone = _zoneDao.findById(zoneId);
                    if (zone == null) {
                        throw new AgentAuthnException("Could not find zone for agent with token " + zoneToken);
                    }
                } catch (NumberFormatException nfe) {
                    throw new AgentAuthnException("Could not find zone for agent with token " + zoneToken);
                }
            }
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully loaded the DataCenter from the zone token passed in ");
        }

        HostPodVO pod = findPod(startup, zone.getId(), Host.Type.Routing); //yes, routing
        Long podId = null;
        if (pod != null) {
            s_logger.debug("Found pod " + pod.getName() + " for the secondary storage host " + startup.getName());
            podId = pod.getId();
        }
        host.setDataCenterId(zone.getId());
        host.setPodId(podId);
        host.setClusterId(null);
        host.setPrivateIpAddress(startup.getPrivateIpAddress());
        host.setPrivateNetmask(startup.getPrivateNetmask());
        host.setPrivateMacAddress(startup.getPrivateMacAddress());
        host.setPublicIpAddress(startup.getPublicIpAddress());
        host.setPublicMacAddress(startup.getPublicMacAddress());
        host.setPublicNetmask(startup.getPublicNetmask());
        host.setStorageIpAddress(startup.getStorageIpAddress());
        host.setStorageMacAddress(startup.getStorageMacAddress());
        host.setStorageNetmask(startup.getStorageNetmask());
        host.setVersion(startup.getVersion());
        host.setName(startup.getName());
        host.setType(type);
        host.setStorageUrl(startup.getIqn());
        host.setLastPinged(System.currentTimeMillis() >> 10);
        host.setCaps(null);
        host.setCpus(null);
        host.setTotalMemory(0);
        host.setSpeed(null);
        host.setParent(startup.getParent());
        host.setTotalSize(startup.getTotalSize());
        host.setHypervisorType(HypervisorType.None);
        if (startup.getNfsShare() != null) {
            host.setStorageUrl(startup.getNfsShare());
        }

    }

    private HostPodVO findPod(StartupCommand startup, long zoneId, Host.Type type) {
        HostPodVO pod = null;
        List<HostPodVO> podsInZone = _podDao.listByDataCenterId(zoneId);
        for (HostPodVO hostPod : podsInZone) {
            if (checkCIDR(type, hostPod, startup.getPrivateIpAddress(), startup.getPrivateNetmask())) {
                pod = hostPod;

                //found the default POD having the same subnet.
                updatePodNetmaskIfNeeded(pod, startup.getPrivateNetmask());

                break;
            }
        }
        return pod;

    }

}
