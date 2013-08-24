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
package com.cloud.hypervisor.kvm.resource;

import java.net.URI;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.network.Networks;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class OvsVifDriver extends VifDriverBase {
    private static final Logger s_logger = Logger.getLogger(OvsVifDriver.class);
    private int _timeout;
    
    @Override
    public void configure(Map<String, Object> params) throws ConfigurationException {
        super.configure(params);

        String networkScriptsDir = (String) params.get("network.scripts.dir");
        if (networkScriptsDir == null) {
            networkScriptsDir = "scripts/vm/network/vnet";
        }

        String value = (String) params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;

        createControlNetwork(_bridges.get("linklocal"));
    }

    @Override
    public InterfaceDef plug(NicTO nic, String guestOsType)
            throws InternalErrorException, LibvirtException {
        s_logger.debug("plugging nic=" + nic);

        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        intf.setVirtualPortType("openvswitch");
        
        String vlanId = null;
        String logicalSwitchUuid = null;
        if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan) {
            URI broadcastUri = nic.getBroadcastUri();
            vlanId = broadcastUri.getHost();
        }
        else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Lswitch) {
            logicalSwitchUuid = nic.getBroadcastUri().getSchemeSpecificPart();
        } else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Pvlan) {
            vlanId = NetUtils.getPrimaryPvlanFromUri(nic.getBroadcastUri());
        }
        String trafficLabel = nic.getName();
        if (nic.getType() == Networks.TrafficType.Guest) {
            Integer networkRateKBps = (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1)? nic.getNetworkRateMbps().intValue() * 128: 0;
            if ((nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan || nic.getBroadcastType() == Networks.BroadcastDomainType.Pvlan)
                    && !vlanId.equalsIgnoreCase("untagged")) {
                if(trafficLabel != null && !trafficLabel.isEmpty()) {
                    s_logger.debug("creating a vlan dev and bridge for guest traffic per traffic label " + trafficLabel);
                    intf.defBridgeNet(_pifs.get(trafficLabel), null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
                    intf.setVlanTag(Integer.parseInt(vlanId));
                } else {
                    intf.defBridgeNet(_pifs.get("private"), null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
                    intf.setVlanTag(Integer.parseInt(vlanId));
                }
            } else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Lswitch) {
                s_logger.debug("nic " + nic + " needs to be connected to LogicalSwitch " + logicalSwitchUuid);
                intf.setVirtualPortInterfaceId(nic.getUuid());
                String brName = (trafficLabel != null && !trafficLabel.isEmpty()) ? _pifs.get(trafficLabel) : _pifs.get("private");
                intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
            }
            else {
                intf.defBridgeNet(_bridges.get("guest"), null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
            }
        } else if (nic.getType() == Networks.TrafficType.Control) {
            /* Make sure the network is still there */
            createControlNetwork(_bridges.get("linklocal"));
            intf.defBridgeNet(_bridges.get("linklocal"), null, nic.getMac(), getGuestNicModel(guestOsType));
        } else if (nic.getType() == Networks.TrafficType.Public) {
            Integer networkRateKBps = (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1)? nic.getNetworkRateMbps().intValue() * 128: 0;
            if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan
                    && !vlanId.equalsIgnoreCase("untagged")) {
                if(trafficLabel != null && !trafficLabel.isEmpty()){
                    s_logger.debug("creating a vlan dev and bridge for public traffic per traffic label " + trafficLabel);
                    intf.defBridgeNet(_pifs.get(trafficLabel), null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
                    intf.setVlanTag(Integer.parseInt(vlanId));
                } else {
                    intf.defBridgeNet(_pifs.get("public"), null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
                    intf.setVlanTag(Integer.parseInt(vlanId));
                }
            } else {
                intf.defBridgeNet(_bridges.get("public"), null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
            }
        } else if (nic.getType() == Networks.TrafficType.Management) {
            intf.defBridgeNet(_bridges.get("private"), null, nic.getMac(), getGuestNicModel(guestOsType));
        } else if (nic.getType() == Networks.TrafficType.Storage) {
            String storageBrName = nic.getName() == null ? _bridges.get("private")
                    : nic.getName();
            intf.defBridgeNet(storageBrName, null, nic.getMac(), getGuestNicModel(guestOsType));
        }
        return intf;
    }

    @Override
    public void unplug(InterfaceDef iface) {
        // Libvirt apparently takes care of this, see BridgeVifDriver unplug
    }

    private String setVnetBrName(String pifName, String vnetId) {
        String brName = "br" + pifName + "-"+ vnetId;
        String oldStyleBrName = "cloudVirBr" + vnetId;

        if (isBridgeExists(oldStyleBrName)) {
            s_logger.info("Using old style bridge name for vlan " + vnetId + " because existing bridge " + oldStyleBrName + " was found");
            brName = oldStyleBrName;
        }

        return brName;
    }
    
    private void deleteExitingLinkLocalRouteTable(String linkLocalBr) {
        Script command = new Script("/bin/bash", _timeout);
        command.add("-c");
        command.add("ip route | grep " + NetUtils.getLinkLocalCIDR());
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        boolean foundLinkLocalBr = false;
        if (result == null && parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                String[] tokens = line.split(" ");
                if (!tokens[2].equalsIgnoreCase(linkLocalBr)) {
                    Script.runSimpleBashScript("ip route del " + NetUtils.getLinkLocalCIDR());
                } else {
                    foundLinkLocalBr = true;
                }
            }
        }
        if (!foundLinkLocalBr) {
            Script.runSimpleBashScript("ifconfig " + linkLocalBr + " 169.254.0.1;" + "ip route add " +
                    NetUtils.getLinkLocalCIDR() + " dev " + linkLocalBr + " src " + NetUtils.getLinkLocalGateway());
        }
    }

    private void createControlNetwork(String privBrName) {
        deleteExitingLinkLocalRouteTable(privBrName);
        if (!isBridgeExists(privBrName)) {
            Script.runSimpleBashScript("ovs-vsctl add-br " + privBrName + "; ifconfig " + privBrName + " up; ifconfig " +
                    privBrName + " 169.254.0.1", _timeout);
        }
    }

    private boolean isBridgeExists(String bridgeName) {
        Script command = new Script("/bin/sh", _timeout);
        command.add("-c");
        command.add("ovs-vsctl br-exists " + bridgeName);
        String result = command.execute(null);
        if ("Ok".equals(result)) {
            return true;
        } else {
            return false;
        }
    }
}
