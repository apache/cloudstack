package com.cloud.hypervisor.ovm3.resources.helpers;

import java.util.List;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Network;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.network.PhysicalNetworkSetupInfo;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

public class Ovm3HypervisorNetwork {
    private final Logger LOGGER = Logger
            .getLogger(Ovm3HypervisorNetwork.class);
    private Connection c;
    private Ovm3Configuration config;
    public Ovm3HypervisorNetwork(Connection conn, Ovm3Configuration ovm3config) {
        c = conn;
        config = ovm3config;
    }

    /* Configure the control network for system VMs */
    public void configureNetworking() throws ConfigurationException {
        /* TODO: setup meta tags for the management interface (probably
        * required with multiple interfaces)?
        */
        try {
           Network net = new Network(c);
           config.setAgentInterfaces(net.getInterfaceList());
           String controlIface = config.getAgentControlNetworkName();
           if (controlIface != null
                   && !config.getAgentInterfaces().containsKey(controlIface)) {
               LOGGER.debug("starting " + controlIface);
               net.startOvsLocalConfig(controlIface);
               /* ovs replies too "fast" so the bridge can be "busy" */
               int contCount = 0;
               while (!config.getAgentInterfaces().containsKey(controlIface)) {
                   LOGGER.debug("waiting for " + controlIface);
                   config.setAgentInterfaces(net.getInterfaceList());
                   Thread.sleep(1 * 1000);
                   if (contCount > 9) {
                       throw new ConfigurationException("Unable to configure "
                               + controlIface + " on host "
                               + config.getAgentHostname());
                   }
                   contCount++;
               }
           } else {
               LOGGER.debug("already have " + controlIface);
           }
           /*
            * The bridge is remembered upon reboot, but not the IP or the
            * config. Zeroconf also adds the route again by default.
            */
           net.ovsIpConfig(controlIface, "static",
                   NetUtils.getLinkLocalGateway(),
                   NetUtils.getLinkLocalNetMask());
           CloudStackPlugin cSp = new CloudStackPlugin(c);
           cSp.ovsControlInterface(controlIface,
                   NetUtils.getLinkLocalCIDR());
        } catch (InterruptedException e) {
            LOGGER.error("interrupted?", e);
        } catch (Ovm3ResourceException e) {
            String msg = "Basic configuration failed on " + config.getAgentHostname();
            LOGGER.error(msg, e);
            throw new ConfigurationException(msg + ", " + e.getMessage());
        }
    }

    /**/
    private boolean isNetworkSetupByName(String nameTag) {
        if (nameTag != null) {
            LOGGER.debug("Looking for network setup by name " + nameTag);

            try {
                Network net = new Network(c);
                net.getInterfaceList();
                if (net.getBridgeByName(nameTag) != null) {
                    LOGGER.debug("Found bridge with name: " + nameTag);
                    return true;
                }
            } catch (Ovm3ResourceException e) {
                LOGGER.debug("Unxpected error looking for name: " + nameTag, e);
                return false;
            }
        }
        LOGGER.debug("No bridge with name: " + nameTag);
        return false;
    }

    /* TODO: check getPhysicalNetworkInfoList */
    /*
     * need to refactor this bit, networksetupbyname makes no sense, and neither
     * does the physical bit
     */
    public CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        LOGGER.debug("Checking if network name setup is done on "
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
                String msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Guest Network is not configured on the backend by name "
                        + info.getGuestNetworkName();
                LOGGER.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            if (!isNetworkSetupByName(info.getPrivateNetworkName())) {
                String msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Private Network is not configured on the backend by name "
                        + info.getPrivateNetworkName();
                LOGGER.error(msg);
                return new CheckNetworkAnswer(cmd, false, msg);
            }
            if (!isNetworkSetupByName(info.getPublicNetworkName())) {
                String msg = "For Physical Network id:"
                        + info.getPhysicalNetworkId()
                        + ", Public Network is not configured on the backend by name "
                        + info.getPublicNetworkName();
                LOGGER.error(msg);
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
                CloudStackPlugin cSp = new CloudStackPlugin(c);
                if (!cSp.ping(cmd.getComputingHostIp())) {
                    return new Answer(cmd, false, "ping failed");
                }
            } else {
                return new Answer(cmd, false, "why asks me to ping router???");
            }
            return new Answer(cmd, true, "success");
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("Ping " + cmd.getComputingHostIp() + " failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    private String createVlanBridge(String networkName, Integer vlanId)
            throws Ovm3ResourceException {
        if (vlanId < 1 || vlanId > 4094) {
            String msg = "Incorrect vlan " + vlanId
                    + ", needs to be between 1 and 4094";
            LOGGER.info(msg);
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
                LOGGER.debug("Interface " + brName + " already exists");
            }
        } catch (Ovm3ResourceException e) {
            String msg = "Unable to create vlan " + vlanId.toString()
                    + " bridge for " + networkName;
            LOGGER.info(msg);
            throw new CloudRuntimeException(msg + ":" + e.getMessage());
        }
        return brName;
    }

    public String getNetwork(NicTO nic) throws Ovm3ResourceException {
        String vlanId = null;
        String bridgeName = null;
        if (nic.getBroadcastType() == BroadcastDomainType.Vlan) {
            vlanId = BroadcastDomainType.getValue(nic.getBroadcastUri());
        }

        if (nic.getType() == TrafficType.Guest) {
            if (nic.getBroadcastType() == BroadcastDomainType.Vlan
                    && !"untagged".equalsIgnoreCase(vlanId)) {
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
            /* TODO: Add storage network */
            bridgeName = config.getAgentStorageNetworkName();
        } else {
            throw new CloudRuntimeException("Unknown network traffic type:"
                    + nic.getType());
        }
        return bridgeName;
    }
}
