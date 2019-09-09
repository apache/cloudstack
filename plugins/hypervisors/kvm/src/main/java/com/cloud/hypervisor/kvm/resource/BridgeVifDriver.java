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
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Networks;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ConfigurationException;

public class BridgeVifDriver extends VifDriverBase {

    private static final Logger s_logger = Logger.getLogger(BridgeVifDriver.class);
    private int _timeout;

    private final Object _vnetBridgeMonitor = new Object();
    private String _modifyVlanPath;
    private String _modifyVxlanPath;
    private String _controlCidr = NetUtils.getLinkLocalCIDR();
    private Long libvirtVersion;

    @Override
    public void configure(Map<String, Object> params) throws ConfigurationException {

        super.configure(params);

        getPifs();

        // Set the domr scripts directory
        params.put("domr.scripts.dir", "scripts/network/domr/kvm");

        String networkScriptsDir = (String)params.get("network.scripts.dir");
        if (networkScriptsDir == null) {
            networkScriptsDir = "scripts/vm/network/vnet";
        }

        String controlCidr = (String)params.get("control.cidr");
        if (StringUtils.isNotBlank(controlCidr)) {
            _controlCidr = controlCidr;
        }

        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;

        _modifyVlanPath = Script.findScript(networkScriptsDir, "modifyvlan.sh");
        if (_modifyVlanPath == null) {
            throw new ConfigurationException("Unable to find modifyvlan.sh");
        }
        _modifyVxlanPath = Script.findScript(networkScriptsDir, "modifyvxlan.sh");
        if (_modifyVxlanPath == null) {
            throw new ConfigurationException("Unable to find modifyvxlan.sh");
        }

        libvirtVersion = (Long) params.get("libvirtVersion");
        if (libvirtVersion == null) {
            libvirtVersion = 0L;
        }
    }

    public void getPifs() {
        final File dir = new File("/sys/devices/virtual/net");
        final File[] netdevs = dir.listFiles();
        final List<String> bridges = new ArrayList<String>();
        for (File netdev : netdevs) {
            final File isbridge = new File(netdev.getAbsolutePath() + "/bridge");
            final String netdevName = netdev.getName();
            s_logger.debug("looking in file " + netdev.getAbsolutePath() + "/bridge");
            if (isbridge.exists()) {
                s_logger.debug("Found bridge " + netdevName);
                bridges.add(netdevName);
            }
        }

        String guestBridgeName = _libvirtComputingResource.getGuestBridgeName();
        String publicBridgeName = _libvirtComputingResource.getPublicBridgeName();

        for (final String bridge : bridges) {
            s_logger.debug("looking for pif for bridge " + bridge);
            final String pif = getPif(bridge);
            if (_libvirtComputingResource.isPublicBridge(bridge)) {
                _pifs.put("public", pif);
            }
            if (guestBridgeName != null && bridge.equals(guestBridgeName)) {
                _pifs.put("private", pif);
            }
            _pifs.put(bridge, pif);
        }

        // guest(private) creates bridges on a pif, if private bridge not found try pif direct
        // This addresses the unnecessary requirement of someone to create an unused bridge just for traffic label
        if (_pifs.get("private") == null) {
            s_logger.debug("guest(private) traffic label '" + guestBridgeName + "' not found as bridge, looking for physical interface");
            final File dev = new File("/sys/class/net/" + guestBridgeName);
            if (dev.exists()) {
                s_logger.debug("guest(private) traffic label '" + guestBridgeName + "' found as a physical device");
                _pifs.put("private", guestBridgeName);
            }
        }

        // public creates bridges on a pif, if private bridge not found try pif direct
        // This addresses the unnecessary requirement of someone to create an unused bridge just for traffic label
        if (_pifs.get("public") == null) {
            s_logger.debug("public traffic label '" + publicBridgeName+ "' not found as bridge, looking for physical interface");
            final File dev = new File("/sys/class/net/" + publicBridgeName);
            if (dev.exists()) {
                s_logger.debug("public traffic label '" + publicBridgeName + "' found as a physical device");
                _pifs.put("public", publicBridgeName);
            }
        }

        s_logger.debug("done looking for pifs, no more bridges");
    }

    private String getPif(final String bridge) {
        String pif = matchPifFileInDirectory(bridge);
        final File vlanfile = new File("/proc/net/vlan/" + pif);

        if (vlanfile.isFile()) {
            pif = Script.runSimpleBashScript("grep ^Device\\: /proc/net/vlan/" + pif + " | awk {'print $2'}");
        }

        return pif;
    }

    private String matchPifFileInDirectory(final String bridgeName) {
        final File brif = new File("/sys/devices/virtual/net/" + bridgeName + "/brif");

        if (!brif.isDirectory()) {
            final File pif = new File("/sys/class/net/" + bridgeName);
            if (pif.isDirectory()) {
                // if bridgeName already refers to a pif, return it as-is
                return bridgeName;
            }
            s_logger.debug("failing to get physical interface from bridge " + bridgeName + ", does " + brif.getAbsolutePath() + "exist?");
            return "";
        }

        final File[] interfaces = brif.listFiles();

        for (File anInterface : interfaces) {
            final String fname = anInterface.getName();
            s_logger.debug("matchPifFileInDirectory: file name '" + fname + "'");
            if (LibvirtComputingResource.isInterface(fname)) {
                return fname;
            }
        }

        s_logger.debug("failing to get physical interface from bridge " + bridgeName + ", did not find an eth*, bond*, team*, vlan*, em*, p*p*, ens*, eno*, enp*, or enx* in " + brif.getAbsolutePath());
        return "";
    }

    protected boolean isBroadcastTypeVlanOrVxlan(final NicTO nic) {
        return nic != null && (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan
                || nic.getBroadcastType() == Networks.BroadcastDomainType.Vxlan);
    }

    protected boolean isValidProtocolAndVnetId(final String vNetId, final String protocol) {
        return vNetId != null && protocol != null && !vNetId.equalsIgnoreCase("untagged");
    }

    @Override
    public LibvirtVMDef.InterfaceDef plug(NicTO nic, String guestOsType, String nicAdapter, Map<String, String> extraConfig) throws InternalErrorException, LibvirtException {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("nic=" + nic);
            if (nicAdapter != null && !nicAdapter.isEmpty()) {
                s_logger.debug("custom nic adapter=" + nicAdapter);
            }
        }

        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        String vNetId = null;
        String protocol = null;
        if (isBroadcastTypeVlanOrVxlan(nic)) {
            vNetId = Networks.BroadcastDomainType.getValue(nic.getBroadcastUri());
            protocol = Networks.BroadcastDomainType.getSchemeValue(nic.getBroadcastUri()).scheme();
        } else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Lswitch) {
            throw new InternalErrorException("Nicira NVP Logicalswitches are not supported by the BridgeVifDriver");
        }
        String trafficLabel = nic.getName();
        Integer networkRateKBps = 0;
        if (libvirtVersion > ((10 * 1000 + 10))) {
            networkRateKBps = (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1) ? nic.getNetworkRateMbps().intValue() * 128 : 0;
        }

        if (nic.getType() == Networks.TrafficType.Guest) {
            if (isBroadcastTypeVlanOrVxlan(nic) && isValidProtocolAndVnetId(vNetId, protocol)) {
                    if (trafficLabel != null && !trafficLabel.isEmpty()) {
                        s_logger.debug("creating a vNet dev and bridge for guest traffic per traffic label " + trafficLabel);
                        String brName = createVnetBr(vNetId, trafficLabel, protocol);
                        intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
                    } else {
                        String brName = createVnetBr(vNetId, "private", protocol);
                        intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
                    }
            } else {
                String brname = "";
                if (trafficLabel != null && !trafficLabel.isEmpty()) {
                    brname = trafficLabel;
                } else {
                    brname = _bridges.get("guest");
                }
                intf.defBridgeNet(brname, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
            }
        } else if (nic.getType() == Networks.TrafficType.Control) {
            /* Make sure the network is still there */
            createControlNetwork();
            intf.defBridgeNet(_bridges.get("linklocal"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), nic.getMtu());
        } else if (nic.getType() == Networks.TrafficType.Public) {
            if (isBroadcastTypeVlanOrVxlan(nic) && isValidProtocolAndVnetId(vNetId, protocol)) {
                if (trafficLabel != null && !trafficLabel.isEmpty()) {
                    s_logger.debug("creating a vNet dev and bridge for public traffic per traffic label " + trafficLabel);
                    String brName = createVnetBr(vNetId, trafficLabel, protocol);
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, null);
                } else {
                    String brName = createVnetBr(vNetId, "public", protocol);
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, null);
                }
            } else {
                intf.defBridgeNet(_bridges.get("public"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, null);
            }
        } else if (nic.getType() == Networks.TrafficType.Management) {
            intf.defBridgeNet(_bridges.get("private"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), nic.getMtu());
        } else if (nic.getType() == Networks.TrafficType.Storage) {
            String storageBrName = nic.getName() == null ? _bridges.get("private") : nic.getName();
            intf.defBridgeNet(storageBrName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), nic.getMtu());
        }
        if (nic.getPxeDisable()) {
            intf.setPxeDisable(true);
        }

        return intf;
    }

    @Override
    public void unplug(LibvirtVMDef.InterfaceDef iface) {
        deleteVnetBr(iface.getBrName());
    }

    @Override
    public void attach(LibvirtVMDef.InterfaceDef iface) {
        Script.runSimpleBashScript("brctl addif " + iface.getBrName() + " " + iface.getDevName());
    }

    @Override
    public void detach(LibvirtVMDef.InterfaceDef iface) {
        Script.runSimpleBashScript("test -d /sys/class/net/" + iface.getBrName() + "/brif/" + iface.getDevName() + " && brctl delif " + iface.getBrName() + " " + iface.getDevName());
    }

    private String generateVnetBrName(String pifName, String vnetId) {
        return "br" + pifName + "-" + vnetId;
    }

    private String generateVxnetBrName(String pifName, String vnetId) {
        return "brvx-" + vnetId;
    }

    private String createVnetBr(String vNetId, String pifKey, String protocol) throws InternalErrorException {
        String nic = _pifs.get(pifKey);
        if (nic == null) {
            // if not found in bridge map, maybe traffic label refers to pif already?
            File pif = new File("/sys/class/net/" + pifKey);
            if (pif.isDirectory()) {
                nic = pifKey;
            }
        }
        String brName = "";
        if (protocol.equals(Networks.BroadcastDomainType.Vxlan.scheme())) {
            brName = generateVxnetBrName(nic, vNetId);
        } else {
            brName = generateVnetBrName(nic, vNetId);
        }
        createVnet(vNetId, nic, brName, protocol);
        return brName;
    }

    private void createVnet(String vnetId, String pif, String brName, String protocol) throws InternalErrorException {
        synchronized (_vnetBridgeMonitor) {
            String script = _modifyVlanPath;
            if (protocol.equals(Networks.BroadcastDomainType.Vxlan.scheme())) {
                script = _modifyVxlanPath;
            }
            final Script command = new Script(script, _timeout, s_logger);
            command.add("-v", vnetId);
            command.add("-p", pif);
            command.add("-b", brName);
            command.add("-o", "add");

            final String result = command.execute();
            if (result != null) {
                throw new InternalErrorException("Failed to create vnet " + vnetId + ": " + result);
            }
        }
    }

    private void deleteVnetBr(String brName) {
        synchronized (_vnetBridgeMonitor) {
            String cmdout = Script.runSimpleBashScript("ls /sys/class/net/" + brName);
            if (cmdout == null)
                // Bridge does not exist
                return;
            cmdout = Script.runSimpleBashScript("ls /sys/class/net/" + brName + "/brif | tr '\n' ' '");
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
                if (brNameMatcher.group(1) != null || !brNameMatcher.group(1).isEmpty()) {
                    pName = brNameMatcher.group(1);
                } else {
                    pName = "undefined";
                }
                vNetId = brNameMatcher.group(2);
            }

            if (vNetId == null || vNetId.isEmpty()) {
                s_logger.debug("unable to get a vNet ID from name " + brName);
                return;
            }

            String scriptPath = null;
            if (cmdout != null && cmdout.contains("vxlan")) {
                scriptPath = _modifyVxlanPath;
            } else {
                scriptPath = _modifyVlanPath;
            }

            final Script command = new Script(scriptPath, _timeout, s_logger);
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

    private void deleteExistingLinkLocalRouteTable(String linkLocalBr) {
        Script command = new Script("/bin/bash", _timeout);
        command.add("-c");
        command.add("ip route | grep " + _controlCidr);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        boolean foundLinkLocalBr = false;
        if (result == null && parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                String[] tokens = line.split(" ");
                if (tokens != null && tokens.length < 2) {
                    continue;
                }
                final String device = tokens[2];
                if (!Strings.isNullOrEmpty(device) && !device.equalsIgnoreCase(linkLocalBr)) {
                    Script.runSimpleBashScript("ip route del " + _controlCidr + " dev " + tokens[2]);
                } else {
                    foundLinkLocalBr = true;
                }
            }
        }

        if (!foundLinkLocalBr) {
            Script.runSimpleBashScript("ip address add " + NetUtils.getLinkLocalAddressFromCIDR(_controlCidr) + " dev " + linkLocalBr);
            Script.runSimpleBashScript("ip route add " + _controlCidr + " dev " + linkLocalBr + " src " + NetUtils.getLinkLocalGateway(_controlCidr));
        }
    }

    private void createControlNetwork() {
        createControlNetwork(_bridges.get("linklocal"));
    }

    @Override
    public void createControlNetwork(String privBrName)  {
        deleteExistingLinkLocalRouteTable(privBrName);
        if (!isExistingBridge(privBrName)) {
            Script.runSimpleBashScript("ip link add name " + privBrName + " type bridge");
            Script.runSimpleBashScript("ip link set " + privBrName + " up");
            Script.runSimpleBashScript("ip address add " + NetUtils.getLinkLocalAddressFromCIDR(_controlCidr) + " dev " + privBrName);
        }
    }

    @Override
    public boolean isExistingBridge(String bridgeName) {
        File f = new File("/sys/devices/virtual/net/" + bridgeName + "/bridge");
        if (f.exists()) {
            return true;
        } else {
            return false;
        }
    }
}
