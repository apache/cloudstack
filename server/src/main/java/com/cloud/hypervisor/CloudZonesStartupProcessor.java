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

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.StartupCommandProcessor;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.manager.authn.AgentAuthnException;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;

/**
 * Creates a host record and supporting records such as pod and ip address
 *
 */
@Component
public class CloudZonesStartupProcessor extends AdapterBase implements StartupCommandProcessor {
    @Inject
    private DataCenterDao _zoneDao = null;
    @Inject
    private HostPodDao _podDao = null;
    @Inject
    private AgentManager _agentManager = null;

    private long _nodeId = -1;

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
    public boolean processInitialConnect(StartupCommand[] cmd) {
        StartupCommand startup = cmd[0];
        return startup instanceof StartupRoutingCommand || startup instanceof StartupStorageCommand;
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

    protected void updateSecondaryHost(final HostVO host, final StartupStorageCommand startup, final Host.Type type) throws AgentAuthnException {

        String zoneToken = startup.getDataCenter();
        if (zoneToken == null) {
            logger.warn("No Zone Token passed in, cannot not find zone for the agent");
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
        if (logger.isDebugEnabled()) {
            logger.debug("Successfully loaded the DataCenter from the zone token passed in ");
        }

        HostPodVO pod = findPod(startup, zone.getId(), Host.Type.Routing); //yes, routing
        Long podId = null;
        if (pod != null) {
            logger.debug("Found pod " + pod.getName() + " for the secondary storage host " + startup.getName());
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
