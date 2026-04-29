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

package com.cloud.hypervisor.ovm3.resources.helpers;

import java.util.List;

import javax.naming.ConfigurationException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Network;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

public class Ovm3HypervisorNetwork {
    protected Logger logger = LogManager.getLogger(getClass());
    private Connection c;
    private Ovm3Configuration config;
    public Ovm3HypervisorNetwork(Connection conn, Ovm3Configuration ovm3config) {
        c = conn;
        config = ovm3config;
    }

    public void configureNetworking() throws ConfigurationException {
        /* TODO: setup meta tags for the management interface (probably
        * required with multiple interfaces)?
        */
        try {
           Network net = new Network(c);
           String controlIface = config.getAgentControlNetworkName();
           if (controlIface != null
                   && net.getInterfaceByName(controlIface) == null) {
               logger.debug("starting " + controlIface);
               net.startOvsLocalConfig(controlIface);
               /* ovs replies too "fast" so the bridge can be "busy" */
               int contCount = 0;
               while (net.getInterfaceByName(controlIface) == null) {
                   logger.debug("waiting for " + controlIface);
                   Thread.sleep(1 * 1000);
                   if (contCount > 9) {
                       throw new ConfigurationException("Unable to configure "
                               + controlIface + " on host "
                               + config.getAgentHostname());
                   }
                   contCount++;
               }
           } else {
               logger.debug("already have " + controlIface);
           }
           /*
            * The bridge is remembered upon reboot, but not the IP or the
            * config. Zeroconf also adds the route again by default.
            */
           net.ovsIpConfig(controlIface, "static",
                   NetUtils.getLinkLocalGateway(),
                   NetUtils.getLinkLocalNetMask());
           CloudstackPlugin cSp = new CloudstackPlugin(c);
           cSp.ovsControlInterface(controlIface,
                   NetUtils.getLinkLocalCIDR());
        } catch (InterruptedException e) {
            logger.error("interrupted?", e);
        } catch (Ovm3ResourceException e) {
            String msg = "Basic configuration failed on " + config.getAgentHostname();
            logger.error(msg, e);
            throw new ConfigurationException(msg + ", " + e.getMessage());
        }
    }

    /**/
    private boolean isNetworkSetupByName(String nameTag) {
        if (nameTag != null) {
            logger.debug("Looking for network setup by name " + nameTag);

            try {
                Network net = new Network(c);
                net.getInterfaceList();
                if (net.getBridgeByName(nameTag) != null) {
                    logger.debug("Found bridge with name: " + nameTag);
                    return true;
                }
            } catch (Ovm3ResourceException e) {
                logger.debug("Unxpected error looking for name: " + nameTag, e);
                return false;
            }
        }
        logger.debug("No bridge with name: " + nameTag);
        return false;
    }

    /* this might have to change in the future, works for now... */
    public CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        logger.debug("Checking if network name setup is done on "
                    + config.getAgentHostname());

        List<PhysicalNetworkSetupInfo> infoList = cmd
                .getPhysicalNetworkInfoList();
        /* here we assume all networks are set */
        for (PhysicalNetworkSetupInfo info : infoList) {
            if (info.getGuestNetworkName() == null) {
                info.setGuestNetworkName(config.getAgentGuestNetworkName());
            }
            if (info.getPublicNetworkName() == null) {
                info.setPublicNetworkName(config.getAgentPublicNetworkName());
            }
            if (info.getPrivateNetworkName() == null) {
                info.setPrivateNetworkName(config.getAgentPrivateNetworkName());
            }
            if (info.getStorageNetworkName() == null) {
                info.setStorageNetworkName(config.getAgentStorageNetworkName());
            }

            if (!isNetworkSetupByName(info.getGuestNetworkName())) {
                String msg = "Guest Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Guest Network is not configured on the backend by name "
                        + info.getGuestNetworkName();
                logger.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            if (!isNetworkSetupByName(info.getPrivateNetworkName())) {
                String msg = "Private Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Private Network is not configured on the backend by name "
                        + info.getPrivateNetworkName();
                logger.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            if (!isNetworkSetupByName(info.getPublicNetworkName())) {
                String msg = "Public Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Public Network is not configured on the backend by name "
                        + info.getPublicNetworkName();
                logger.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            /* Storage network is optional, will revert to private otherwise */
        }

        return new CheckNetworkAnswer(cmd, true,
                "Network Setup check by names is done");

    }

    public Answer execute(PingTestCommand cmd) {
        try {
            if (cmd.getComputingHostIp() != null) {
                CloudstackPlugin cSp = new CloudstackPlugin(c);
                if (!cSp.ping(cmd.getComputingHostIp())) {
                    return new Answer(cmd, false, "ping failed");
                }
            } else {
                return new Answer(cmd, false, "why asks me to ping a router???");
            }
            return new Answer(cmd, true, "success");
        } catch (Ovm3ResourceException e) {
            logger.debug("Ping " + cmd.getComputingHostIp() + " failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private String createVlanBridge(String networkName, Integer vlanId)
            throws Ovm3ResourceException {
        if (vlanId < 1 || vlanId > 4094) {
            String msg = "Incorrect vlan " + vlanId
                    + ", needs to be between 1 and 4094";
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        Network net = new Network(c);
        /* figure out if our bridged vlan exists, if not then create */
        String brName = networkName + "." + vlanId.toString();
        try {
            String physInterface = net.getPhysicalByBridgeName(networkName);
            if (net.getInterfaceByName(brName) == null) {
                net.startOvsVlanBridge(brName, physInterface, vlanId);
            } else {
                logger.debug("Interface " + brName + " already exists");
            }
        } catch (Ovm3ResourceException e) {
            String msg = "Unable to create vlan " + vlanId.toString()
                    + " bridge for " + networkName;
            logger.warn(msg + ": " + e);
            throw new CloudRuntimeException(msg + ":" + e.getMessage());
        }
        return brName;
    }
    /* getNetwork needs to be split in pure retrieval versus creation */
    public String getNetwork(NicTO nic) throws Ovm3ResourceException {
        String vlanId = null;
        String bridgeName = null;
        if (nic.getBroadcastType() == BroadcastDomainType.Vlan) {
            vlanId = BroadcastDomainType.getValue(nic.getBroadcastUri());
        }

        if (nic.getType() == TrafficType.Guest) {
            if (nic.getBroadcastType() == BroadcastDomainType.Vlan
                    && !"untagged".equalsIgnoreCase(vlanId)) {
                /* This is completely the wrong place for this, we should NEVER
                 * create a network when we're just trying to figure out if it's there
                 * The name of this is misleading and wrong.
                 */
                bridgeName = createVlanBridge(config.getAgentGuestNetworkName(),
                        Integer.valueOf(vlanId));
            } else {
                bridgeName = config.getAgentGuestNetworkName();
            }

            /* VLANs for other mgmt traffic ? */
        } else if (nic.getType() == TrafficType.Control) {
            bridgeName = config.getAgentControlNetworkName();
        } else if (nic.getType() == TrafficType.Public) {
            bridgeName = config.getAgentPublicNetworkName();
        } else if (nic.getType() == TrafficType.Management) {
            bridgeName = config.getAgentPrivateNetworkName();
        } else if (nic.getType() == TrafficType.Storage) {
            bridgeName = config.getAgentStorageNetworkName();
        } else {
            throw new CloudRuntimeException("Unknown network traffic type:"
                    + nic.getType());
        }
        return bridgeName;
    }
}
