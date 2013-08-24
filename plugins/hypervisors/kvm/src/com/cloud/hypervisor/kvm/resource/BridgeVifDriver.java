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

import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Networks;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import javax.naming.ConfigurationException;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

public class BridgeVifDriver extends VifDriverBase {

    private static final Logger s_logger = Logger
            .getLogger(BridgeVifDriver.class);
    private int _timeout;
    
    private static final Object _vnetBridgeMonitor = new Object();
    private String _modifyVlanPath;

    @Override
    public void configure(Map<String, Object> params) throws ConfigurationException {

        super.configure(params);

        // Set the domr scripts directory
        params.put("domr.scripts.dir", "scripts/network/domr/kvm");


        String networkScriptsDir = (String) params.get("network.scripts.dir");
        if (networkScriptsDir == null) {
            networkScriptsDir = "scripts/vm/network/vnet";
        }

        String value = (String) params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;

        _modifyVlanPath = Script.findScript(networkScriptsDir, "modifyvlan.sh");
        if (_modifyVlanPath == null) {
            throw new ConfigurationException("Unable to find modifyvlan.sh");
        }

        try {
            createControlNetwork();
        } catch (LibvirtException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    @Override
    public LibvirtVMDef.InterfaceDef plug(NicTO nic, String guestOsType)
            throws InternalErrorException, LibvirtException {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("nic=" + nic);
        }

        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        String vlanId = null;
        if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan) {
            URI broadcastUri = nic.getBroadcastUri();
            vlanId = broadcastUri.getHost();
        }
        else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Lswitch) {
            throw new InternalErrorException("Nicira NVP Logicalswitches are not supported by the BridgeVifDriver");
        }
        String trafficLabel = nic.getName();
        if (nic.getType() == Networks.TrafficType.Guest) {
            Integer networkRateKBps = (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1)? nic.getNetworkRateMbps().intValue() * 128: 0;
            if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan
                    && !vlanId.equalsIgnoreCase("untagged")) {
                if(trafficLabel != null && !trafficLabel.isEmpty()) {
                    s_logger.debug("creating a vlan dev and bridge for guest traffic per traffic label " + trafficLabel);
                    String brName = createVlanBr(vlanId, _pifs.get(trafficLabel));
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
                } else {
                    String brName = createVlanBr(vlanId, _pifs.get("private"));
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
                }
            } else {
                intf.defBridgeNet(_bridges.get("guest"), null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
            }
        } else if (nic.getType() == Networks.TrafficType.Control) {
            /* Make sure the network is still there */
            createControlNetwork();
            intf.defBridgeNet(_bridges.get("linklocal"), null, nic.getMac(), getGuestNicModel(guestOsType));
        } else if (nic.getType() == Networks.TrafficType.Public) {
            Integer networkRateKBps = (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1)? nic.getNetworkRateMbps().intValue() * 128: 0;
            if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan
                    && !vlanId.equalsIgnoreCase("untagged")) {
                if(trafficLabel != null && !trafficLabel.isEmpty()){
                    s_logger.debug("creating a vlan dev and bridge for public traffic per traffic label " + trafficLabel);
                    String brName = createVlanBr(vlanId, _pifs.get(trafficLabel));
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
                } else {
                    String brName = createVlanBr(vlanId, _pifs.get("public"));
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType), networkRateKBps);
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
    public void unplug(LibvirtVMDef.InterfaceDef iface) {
        deleteVnetBr(iface.getBrName());
    }

    private String setVnetBrName(String pifName, String vnetId) {
        String brName = "br" + pifName + "-"+ vnetId;
        String oldStyleBrName = "cloudVirBr" + vnetId;

        String cmdout = Script.runSimpleBashScript("brctl show | grep " + oldStyleBrName);
        if (cmdout != null && cmdout.contains(oldStyleBrName)) {
            s_logger.info("Using old style bridge name for vlan " + vnetId + " because existing bridge " + oldStyleBrName + " was found");
            brName = oldStyleBrName;
        }

        return brName;
    }

    private String createVlanBr(String vlanId, String nic)
            throws InternalErrorException {
        String brName = setVnetBrName(nic, vlanId);
        createVnet(vlanId, nic, brName);
        return brName;
    }

    private void createVnet(String vnetId, String pif, String brName)
            throws InternalErrorException {
        synchronized (_vnetBridgeMonitor) {
            final Script command = new Script(_modifyVlanPath, _timeout, s_logger);
            command.add("-v", vnetId);
            command.add("-p", pif);
            command.add("-b", brName);
            command.add("-o", "add");
            
            final String result = command.execute();
            if (result != null) {
                throw new InternalErrorException("Failed to create vnet " + vnetId
                        + ": " + result);
            }
        }
    }
    
    private void deleteVnetBr(String brName){
        synchronized (_vnetBridgeMonitor) {
            String cmdout = Script.runSimpleBashScript("ls /sys/class/net/" + brName + "/brif | grep vnet");
            if (cmdout != null && cmdout.contains("vnet")) {
                // Active VM remains on that bridge
                return;
            }
            
            Pattern oldStyleBrNameRegex = Pattern.compile("^cloudVirBr(\\d+)$");
            Pattern brNameRegex = Pattern.compile("^br(\\S+)-(\\d+)$");
            Matcher oldStyleBrNameMatcher = oldStyleBrNameRegex.matcher(brName);
            Matcher brNameMatcher = brNameRegex.matcher(brName);
            
            String pName = null;
            String vNetId = null;
            if (oldStyleBrNameMatcher.find()) {
                // Actually modifyvlan.sh doesn't require pif name when deleting its bridge so far.
                pName = "undefined";
                vNetId = oldStyleBrNameMatcher.group(1);
            } else if (brNameMatcher.find()) {
                if(brNameMatcher.group(1) != null || !brNameMatcher.group(1).isEmpty()) {
                    pName = brNameMatcher.group(1);
                } else {
                    pName = "undefined";
                }
                vNetId = brNameMatcher.group(2);
            }
            
            if (vNetId == null || vNetId.isEmpty()) {
                s_logger.debug("unable to get a vNet ID from name "+ brName);
                return;
            }
            
            final Script command = new Script(_modifyVlanPath, _timeout, s_logger);
            command.add("-o", "delete");
            command.add("-v", vNetId);
            command.add("-p", pName);
            command.add("-b", brName);
            
            final String result = command.execute();
            if (result != null) {
                s_logger.debug("Delete bridge " + brName + " failed: " + result);
            }
        }
    }

    private void createControlNetwork() throws LibvirtException {
        createControlNetwork(_bridges.get("linklocal"));
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
            Script.runSimpleBashScript("brctl addbr " + privBrName + "; ifconfig " + privBrName + " up; ifconfig " +
                    privBrName + " 169.254.0.1", _timeout);
        }

    }

    private boolean isBridgeExists(String bridgeName) {
        File f = new File("/sys/devices/virtual/net/" + bridgeName);
        if (f.exists()) {
            return true;
        } else {
            return false;
        }
    }
}
