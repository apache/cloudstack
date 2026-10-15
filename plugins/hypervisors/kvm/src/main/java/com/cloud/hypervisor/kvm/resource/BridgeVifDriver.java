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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.ConfigurationException;

import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.to.NetworkTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.exception.InternalErrorException;
import com.cloud.network.Networks;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.script.Script;

public class BridgeVifDriver extends VifDriverBase {

    private int _timeout;

    private final Object _vnetBridgeMonitor = new Object();
    private String _modifyVlanPath;
    private String _modifyVxlanPath;
    private String _macIpScriptPath;
    private String _controlCidr = NetUtils.getLinkLocalCIDR();
    private Long libvirtVersion;

    private static boolean isVxlanOrNetris(String protocol) {
        return protocol.equals(Networks.BroadcastDomainType.Vxlan.scheme()) || protocol.equals(Networks.BroadcastDomainType.Netris.scheme());
    }

    @Override
    public void configure(Map<String, Object> params) throws ConfigurationException {

        super.configure(params);

        getPifs();

        // Set the domr scripts directory
        params.put("domr.scripts.dir", "scripts/network/domr/kvm");

        String networkScriptsDir = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.NETWORK_SCRIPTS_DIR);

        _controlCidr = getControlCidr(_controlCidr);

        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;

        _modifyVlanPath = Script.findScript(networkScriptsDir, "modifyvlan.sh");
        if (_modifyVlanPath == null) {
            throw new ConfigurationException("Unable to find modifyvlan.sh");
        }
        String vxlanMode = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.NETWORK_VXLAN_MODE);
        String vxlanScript = "evpn".equalsIgnoreCase(vxlanMode) ? "modifyvxlan-evpn.sh" : "modifyvxlan.sh";
        _modifyVxlanPath = Script.findScript(networkScriptsDir, vxlanScript);
        if (_modifyVxlanPath == null) {
            throw new ConfigurationException("Unable to find " + vxlanScript);
        }

        if (Boolean.TRUE.equals(AgentPropertiesFileHandler.getPropertyValue(AgentProperties.VM_NETWORK_MACIP_STATIC))) {
            _macIpScriptPath = Script.findScript(networkScriptsDir, "modifymacip.sh");
            if (_macIpScriptPath == null) {
                throw new ConfigurationException("Unable to find modifymacip.sh");
            }
            logger.info("VM network MAC/IP static script configured: {}", _macIpScriptPath);
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
            logger.debug("looking in file " + netdev.getAbsolutePath() + "/bridge");
            if (isbridge.exists()) {
                logger.debug("Found bridge " + netdevName);
                bridges.add(netdevName);
            }
        }

        String guestBridgeName = _libvirtComputingResource.getGuestBridgeName();
        String publicBridgeName = _libvirtComputingResource.getPublicBridgeName();

        for (final String bridge : bridges) {
            logger.debug("looking for pif for bridge " + bridge);
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
            logger.debug("guest(private) traffic label '" + guestBridgeName + "' not found as bridge, looking for physical interface");
            final File dev = new File("/sys/class/net/" + guestBridgeName);
            if (dev.exists()) {
                logger.debug("guest(private) traffic label '" + guestBridgeName + "' found as a physical device");
                _pifs.put("private", guestBridgeName);
            }
        }

        // public creates bridges on a pif, if private bridge not found try pif direct
        // This addresses the unnecessary requirement of someone to create an unused bridge just for traffic label
        if (_pifs.get("public") == null) {
            logger.debug("public traffic label '" + publicBridgeName+ "' not found as bridge, looking for physical interface");
            final File dev = new File("/sys/class/net/" + publicBridgeName);
            if (dev.exists()) {
                logger.debug("public traffic label '" + publicBridgeName + "' found as a physical device");
                _pifs.put("public", publicBridgeName);
            }
        }

        logger.debug("done looking for pifs, no more bridges");
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
            logger.debug("failing to get physical interface from bridge " + bridgeName + ", does " + brif.getAbsolutePath() + "exist?");
            return "";
        }

        final File[] interfaces = brif.listFiles();

        for (File anInterface : interfaces) {
            final String fname = anInterface.getName();
            logger.debug("matchPifFileInDirectory: file name '" + fname + "'");
            if (LibvirtComputingResource.isInterface(fname)) {
                return fname;
            }
        }

        logger.debug("failing to get physical interface from bridge " + bridgeName + ", did not find an eth*, bond*, team*, vlan*, em*, p*p*, ens*, eno*, enp*, or enx* in " + brif.getAbsolutePath());
        return "";
    }

    protected boolean isBroadcastTypeVlanOrVxlan(final NicTO nic) {
        return nic != null && (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan
                || nic.getBroadcastType() == Networks.BroadcastDomainType.Vxlan || nic.getBroadcastType() == Networks.BroadcastDomainType.Netris);
    }

    protected boolean isValidProtocolAndVnetId(final String vNetId, final String protocol) {
        return vNetId != null && protocol != null && !vNetId.equalsIgnoreCase("untagged");
    }

    protected boolean usesSharedVlanAwareBridge(final NicTO nic) {
        return nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan && _libvirtComputingResource.hostSupportsVlanFiltering();
    }

    protected void plugTrunkVlanNic(LibvirtVMDef.InterfaceDef intf, NicTO nic, String trafficLabel, String guestOsType, String nicAdapter,
            Integer networkRateKBps) throws InternalErrorException {
        if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vlan) {
            throw new InternalErrorException("Multi-VLAN trunk nics are only supported on VLAN-isolated guest networks");
        }
        if (!_libvirtComputingResource.hostSupportsVlanFiltering()) {
            throw new InternalErrorException("vlan_filtering is not enabled on this host's guest bridge; "
                    + "this host cannot accept a multi-VLAN trunk nic");
        }

        String brName = trafficLabel != null && !trafficLabel.isEmpty() ? trafficLabel : _bridges.get("guest");

        Integer primaryVlanTag = parseVlanTag(nic.getBroadcastUri(), "primary network of nic " + nic.getMac());
        List<Integer> vlanTags = collectTrunkVlanTags(nic);
        warnIfUplinkMissingVlanMembership(brName, vlanTags);

        logger.debug("plugging trunk nic " + nic.getMac() + " onto guest bridge " + brName + " with vlan tags " + vlanTags
                + ", native vlan " + primaryVlanTag);
        intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps);

        if (_libvirtComputingResource.hostSupportsVlanTrunkXml()) {
            intf.setTrunkVlanTags(vlanTags, primaryVlanTag);
        }
        // else: older libvirt can't express trunk membership; ensureVlanTrunkMembership() applies it manually once the tap exists
    }

    private List<Integer> collectTrunkVlanTags(NicTO nic) throws InternalErrorException {
        Set<Integer> vlanTags = new LinkedHashSet<>();
        vlanTags.add(parseVlanTag(nic.getBroadcastUri(), "primary network of nic " + nic.getMac()));
        if (nic.getAssociatedNetworks() != null) {
            for (NetworkTO associatedNetwork : nic.getAssociatedNetworks()) {
                if (associatedNetwork.getBroadcastType() != Networks.BroadcastDomainType.Vlan) {
                    throw new InternalErrorException("Multi-VLAN trunk nics only support VLAN-isolated associated networks");
                }
                vlanTags.add(parseVlanTag(associatedNetwork.getBroadcastUri(), "associated network " + associatedNetwork.getUuid()));
            }
        }
        return new ArrayList<>(vlanTags);
    }

    private Integer parseVlanTag(URI broadcastUri, String description) throws InternalErrorException {
        String vlanValue = broadcastUri == null ? null : Networks.BroadcastDomainType.getValue(broadcastUri);
        if (StringUtils.isBlank(vlanValue)) {
            throw new InternalErrorException("Cannot determine VLAN for " + description
                    + ": no VLAN has been assigned yet (is the network implemented?). Refusing to plug this multi-VLAN trunk nic.");
        }
        try {
            return Integer.valueOf(vlanValue);
        } catch (NumberFormatException e) {
            throw new InternalErrorException("Invalid VLAN value '" + vlanValue + "' for " + description);
        }
    }

    /**
     * The uplink's own tagged VLAN membership is the operator's responsibility, not CloudStack's - the uplink
     * is never modified here. This only gives the operator a diagnostic signal: a VLAN missing from the uplink
     * still lets same-host traffic on that VLAN work (pure bridge-local forwarding never touches the uplink),
     * while cross-host traffic on it silently fails - a confusing signature this warning is meant to shortcut.
     * Never blocks the plug and never throws; a failure to even read the uplink's membership is itself just logged.
     */
    private void warnIfUplinkMissingVlanMembership(String brName, List<Integer> vlanTags) {
        String uplinkPif = _pifs.get(brName);
        if (StringUtils.isBlank(uplinkPif)) {
            logger.warn("Cannot determine the uplink interface for guest bridge {} to check VLAN membership for tags {}; "
                    + "cross-host traffic for this nic will fail unless the uplink has already been configured for these VLANs", brName, vlanTags);
            return;
        }
        try {
            Map<Integer, Boolean> currentMembership = readCurrentVlanMembership(uplinkPif);
            List<Integer> missingTags = new ArrayList<>();
            for (Integer vlanTag : vlanTags) {
                if (!currentMembership.containsKey(vlanTag)) {
                    missingTags.add(vlanTag);
                }
            }
            if (!missingTags.isEmpty()) {
                logger.warn("Uplink {} is not a tagged member of VLAN(s) {}; same-host traffic on these VLANs will work, but cross-host "
                        + "traffic will not, until an operator adds them (e.g. 'bridge vlan add dev {} vid <vlan>')", uplinkPif, missingTags, uplinkPif);
            }
        } catch (InternalErrorException e) {
            logger.warn("Unable to check VLAN membership on uplink {}: {}", uplinkPif, e.getMessage());
        }
    }

    @Override
    public void ensureVlanTrunkMembership(LibvirtVMDef.InterfaceDef iface, NicTO nic) throws InternalErrorException {
        boolean usesSharedBridge = nic.isTrunkVlan() || usesSharedVlanAwareBridge(nic);
        if (!usesSharedBridge || _libvirtComputingResource.hostSupportsVlanTrunkXml()) {
            return;
        }
        String tapName = iface.getDevName();
        if (StringUtils.isBlank(tapName)) {
            throw new InternalErrorException("Cannot apply manual VLAN trunk membership: tap device name unknown for nic " + nic.getMac());
        }
        Integer primaryVlanTag = parseVlanTag(nic.getBroadcastUri(), "primary network of nic " + nic.getMac());
        for (Integer vlanTag : collectTrunkVlanTags(nic)) {
            runBridgeVlanCommand("add", tapName, String.valueOf(vlanTag), vlanTag.equals(primaryVlanTag));
        }
    }

    protected void runBridgeVlanCommand(String operation, String dev, String vid, boolean pvidUntagged) throws InternalErrorException {
        final Script command = new Script("bridge", _timeout, logger);
        command.add("vlan");
        command.add(operation);
        command.add("dev", dev);
        command.add("vid", vid);
        if (pvidUntagged) {
            command.add("pvid");
            command.add("untagged");
        }
        final String result = command.execute();
        if (result != null) {
            throw new InternalErrorException("Failed to " + operation + " VLAN " + vid + " membership on " + dev + ": " + result);
        }
    }

    @Override
    public void updateVlanTrunkMembership(Domain vm, LibvirtVMDef.InterfaceDef iface, NicTO nic) throws InternalErrorException, LibvirtException {
        boolean usesSharedBridge = nic.isTrunkVlan() || usesSharedVlanAwareBridge(nic);
        if (!usesSharedBridge) {
            throw new InternalErrorException("Nic " + nic.getMac() + " does not use the shared VLAN-aware bridge; cannot update its VLAN membership live");
        }
        if (!_libvirtComputingResource.hostSupportsVlanFiltering()) {
            throw new InternalErrorException("vlan_filtering is not enabled on this host's guest bridge; cannot update multi-VLAN trunk membership live");
        }

        Integer primaryVlanTag = parseVlanTag(nic.getBroadcastUri(), "primary network of nic " + nic.getMac());
        List<Integer> vlanTags = collectTrunkVlanTags(nic);

        if (_libvirtComputingResource.hostSupportsVlanTrunkXml()) {
            iface.setTrunkVlanTags(vlanTags, primaryVlanTag);
            // CloudStack's KVM domains are transient (no persistent libvirt config to update) - matches the
            // existing LIVE-only precedent in LibvirtReplugNicCommandWrapper/LibvirtUpdateVmNicCommandWrapper.
            // Persistence across a restart is already guaranteed by nic_network_map/nics.network_id, not libvirt.
            vm.updateDeviceFlags(iface.toString(), Domain.DeviceModifyFlags.LIVE);
        } else {
            applyVlanTrunkMembershipDiff(iface.getDevName(), vlanTags, primaryVlanTag);
        }
    }

    private void applyVlanTrunkMembershipDiff(String tapName, List<Integer> desiredVlanTags, Integer primaryVlanTag) throws InternalErrorException {
        if (StringUtils.isBlank(tapName)) {
            throw new InternalErrorException("Cannot update VLAN trunk membership: tap device name unknown");
        }
        Map<Integer, Boolean> currentMembership = readCurrentVlanMembership(tapName);
        Set<Integer> desiredSet = new LinkedHashSet<>(desiredVlanTags);

        for (Integer currentVlan : currentMembership.keySet()) {
            if (!desiredSet.contains(currentVlan)) {
                runBridgeVlanCommand("del", tapName, String.valueOf(currentVlan), false);
            }
        }
        for (Integer desiredVlan : desiredSet) {
            boolean shouldBePvid = desiredVlan.equals(primaryVlanTag);
            Boolean currentlyPvid = currentMembership.get(desiredVlan);
            if (currentlyPvid == null || currentlyPvid != shouldBePvid) {
                runBridgeVlanCommand("add", tapName, String.valueOf(desiredVlan), shouldBePvid);
            }
        }
    }

    protected Map<Integer, Boolean> readCurrentVlanMembership(String tapName) throws InternalErrorException {
        final Script command = new Script("bridge", _timeout, logger);
        command.add("-j");
        command.add("vlan");
        command.add("show");
        command.add("dev", tapName);
        final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        final String errors = command.execute(parser);
        if (errors != null) {
            throw new InternalErrorException("Failed to read current VLAN membership for " + tapName + ": " + errors);
        }

        final Map<Integer, Boolean> membership = new LinkedHashMap<>();
        final String json = parser.getLines();
        if (StringUtils.isBlank(json)) {
            return membership;
        }
        for (JsonElement deviceEl : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject device = deviceEl.getAsJsonObject();
            if (!device.has("vlans")) {
                continue;
            }
            for (JsonElement vlanEl : device.getAsJsonArray("vlans")) {
                JsonObject vlanObj = vlanEl.getAsJsonObject();
                int vlan = vlanObj.get("vlan").getAsInt();
                boolean pvid = false;
                if (vlanObj.has("flags")) {
                    for (JsonElement flagEl : vlanObj.getAsJsonArray("flags")) {
                        if ("PVID".equalsIgnoreCase(flagEl.getAsString())) {
                            pvid = true;
                        }
                    }
                }
                membership.put(vlan, pvid);
            }
        }
        return membership;
    }

    protected String createStorageVnetBridgeIfNeeded(NicTO nic, String trafficLabel,
                 String storageBrName) throws InternalErrorException {
        if (nic.getBroadcastUri() == null) {
            return storageBrName;
        }

        boolean isStorageBroadcast = Networks.BroadcastDomainType.Storage.equals(nic.getBroadcastType()) ||
                Networks.BroadcastDomainType.Storage.equals(Networks.BroadcastDomainType.getSchemeValue(nic.getBroadcastUri()));
        if (!isStorageBroadcast) {
            return storageBrName;
        }

        String vNetId = Networks.BroadcastDomainType.getValue(nic.getBroadcastUri());
        String protocol = Networks.BroadcastDomainType.Vlan.scheme();
        if (!isValidProtocolAndVnetId(vNetId, protocol))  {
            return storageBrName;
        }
        logger.debug(String.format("creating a vNet dev and bridge for %s traffic per traffic label %s",
                Networks.TrafficType.Storage.name(), trafficLabel));
        return createVnetBr(vNetId, storageBrName, protocol);
    }

    @Override
    public LibvirtVMDef.InterfaceDef plug(NicTO nic, String guestOsType, String nicAdapter, Map<String, String> extraConfig) throws InternalErrorException, LibvirtException {

        if (logger.isDebugEnabled()) {
            logger.debug("nic=" + nic);
            if (nicAdapter != null && !nicAdapter.isEmpty()) {
                logger.debug("custom nic adapter=" + nicAdapter);
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
            networkRateKBps = getNetworkRateKbps(nic);
        }

        if (nic.getType() == Networks.TrafficType.Guest) {
            if (nic.isTrunkVlan()) {
                plugTrunkVlanNic(intf, nic, trafficLabel, guestOsType, nicAdapter, networkRateKBps);
            } else if (isBroadcastTypeVlanOrVxlan(nic) && isValidProtocolAndVnetId(vNetId, protocol)) {
                    if (usesSharedVlanAwareBridge(nic)) {
                        // host is VLAN-filtering-ready: single-VLAN nics share the same VLAN-aware bridge
                        // trunk nics use, rather than getting their own dedicated per-VLAN bridge, so the
                        // two mechanisms never end up on disconnected bridges for the same VLAN on one host
                        plugTrunkVlanNic(intf, nic, trafficLabel, guestOsType, nicAdapter, networkRateKBps);
                    } else if (trafficLabel != null && !trafficLabel.isEmpty()) {
                        logger.debug("creating a vNet dev and bridge for guest traffic per traffic label " + trafficLabel);
                        String brName = createVnetBr(vNetId, trafficLabel, protocol);
                        intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps);
                    } else {
                        String brName = createVnetBr(vNetId, _bridges.get("guest"), protocol);
                        intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps);
                    }
            } else {
                String brname = "";
                if (trafficLabel != null && !trafficLabel.isEmpty()) {
                    brname = trafficLabel;
                } else {
                    brname = _bridges.get("guest");
                }
                intf.defBridgeNet(brname, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps);
            }
        } else if (nic.getType() == Networks.TrafficType.Control) {
            /* Make sure the network is still there */
            createControlNetwork();
            intf.defBridgeNet(_bridges.get("linklocal"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter));
        } else if (nic.getType() == Networks.TrafficType.Public) {
            if (isBroadcastTypeVlanOrVxlan(nic) && isValidProtocolAndVnetId(vNetId, protocol)) {
                if (trafficLabel != null && !trafficLabel.isEmpty()) {
                    logger.debug("creating a vNet dev and bridge for public traffic per traffic label " + trafficLabel);
                    String brName = createVnetBr(vNetId, trafficLabel, protocol);
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps);
                } else {
                    String brName = createVnetBr(vNetId, "public", protocol);
                    intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps);
                }
            } else {
                intf.defBridgeNet(_bridges.get("public"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps);
            }
        } else if (nic.getType() == Networks.TrafficType.Management) {
            intf.defBridgeNet(_bridges.get("private"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter));
        } else if (nic.getType() == Networks.TrafficType.Storage) {
            String storageBrName = nic.getName() == null ? _bridges.get("private") : nic.getName();
            storageBrName = createStorageVnetBridgeIfNeeded(nic, trafficLabel, storageBrName);
            intf.defBridgeNet(storageBrName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter));
        }
        if (nic.getPxeDisable()) {
            intf.setPxeDisable(true);
        }
        intf.setLinkStateUp(nic.isEnabled());

        executeMacIpScript(intf.getBrName(), nic.getMac(), nic.getIp(), nic.getIp6Address(), nic.getNicSecIps());

        return intf;
    }

    @Override
    public void unplug(LibvirtVMDef.InterfaceDef iface, boolean deleteBr) {
        executeMacIpScript(iface.getBrName(), iface.getMacAddress());
        deleteVnetBr(iface.getBrName(), deleteBr);
    }

    @Override
    public void attach(LibvirtVMDef.InterfaceDef iface) {
        Script.runSimpleBashScript("ip link set " + iface.getDevName() + " master " +  iface.getBrName());
    }

    @Override
    public void detach(LibvirtVMDef.InterfaceDef iface) {
        Script.runSimpleBashScript("test -d /sys/class/net/" + iface.getBrName() + "/brif/" + iface.getDevName() + " && ip link set " + iface.getDevName() + " nomaster");
    }

    private String generateVnetBrName(String pifName, String vnetId) {
        return "br" + pifName + "-" + vnetId;
    }

    private String generateVxnetBrName(String pifName, String vnetId) {
        return "brvx-" + vnetId;
    }

    protected String createVnetBr(String vNetId, String pifKey, String protocol) throws InternalErrorException {
        String nic = _pifs.get(pifKey);
        if (nic == null || isVxlanOrNetris(protocol)) {
            // if not found in bridge map, maybe traffic label refers to pif already?
            File pif = new File("/sys/class/net/" + pifKey);
            if (pif.isDirectory()) {
                nic = pifKey;
            }
        }
        String brName = "";
        if (isVxlanOrNetris(protocol)) {
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
            if (isVxlanOrNetris(protocol)) {
                script = _modifyVxlanPath;
            }
            final Script command = new Script(script, _timeout, logger);
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

    private void deleteVnetBr(String brName, boolean deleteBr) {
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
                logger.debug("unable to get a vNet ID from name " + brName);
                return;
            }

            String scriptPath = null;
            if (cmdout != null && cmdout.contains("vxlan")) {
                scriptPath = _modifyVxlanPath;
            } else {
                scriptPath = _modifyVlanPath;
            }

            final Script command = new Script(scriptPath, _timeout, logger);
            command.add("-o", "delete");
            command.add("-v", vNetId);
            command.add("-p", pName);
            command.add("-b", brName);
            if (cmdout != null && !cmdout.contains("vxlan")) {
                command.add("-d", String.valueOf(deleteBr));
            }

            final String result = command.execute();
            if (result != null) {
                logger.debug("Delete bridge " + brName + " failed: " + result);
            }
        }
    }

    private void executeMacIpScript(String brName, String mac) {
        if (_macIpScriptPath == null || mac == null || brName == null) {
            return;
        }
        try {
            final Script command = new Script(_macIpScriptPath, _timeout, logger);
            command.add("-o", "delete");
            command.add("-b", brName);
            command.add("-m", mac);
            final String result = command.execute();
            if (result != null) {
                logger.warn("MAC/IP script returned error for delete on {}: {}", mac, result);
            }
        } catch (Exception e) {
            // Managing host neighbour/route entries is best-effort and must never break VM lifecycle operations
            logger.warn("Failed to run MAC/IP script for delete on {} ({})", mac, brName, e);
        }
    }

    private void executeMacIpScript(String brName, String mac, String ipv4, String ipv6, List<String> secondaryIps) {
        if (_macIpScriptPath == null || mac == null || brName == null) {
            return;
        }
        try {
            final Script command = new Script(_macIpScriptPath, _timeout, logger);
            command.add("-o", "add");
            command.add("-b", brName);
            command.add("-m", mac);
            if (ipv4 != null && !ipv4.isEmpty()) {
                command.add("-4", ipv4);
            }
            command.add("-6", NetUtils.ipv6LinkLocal(mac).toString());
            if (ipv6 != null && !ipv6.isEmpty()) {
                command.add("-6", ipv6);
            }
            if (secondaryIps != null) {
                for (String secIp : secondaryIps) {
                    if (NetUtils.isValidIp6(secIp)) {
                        command.add("-6", secIp);
                    } else {
                        command.add("-4", secIp);
                    }
                }
            }
            final String result = command.execute();
            if (result != null) {
                logger.warn("MAC/IP script returned error for add on {}: {}", mac, result);
            }
        } catch (Exception e) {
            // Managing host neighbour/route entries is best-effort and must never break VM lifecycle operations
            logger.warn("Failed to run MAC/IP script for add on {} ({})", mac, brName, e);
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
                if (StringUtils.isNotEmpty(device) && !device.equalsIgnoreCase(linkLocalBr)) {
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

    @Override
    public void deleteBr(NicTO nic) {
        String vlanId = Networks.BroadcastDomainType.getValue(nic.getBroadcastUri());
        String trafficLabel = nic.getName();
        String pifName = _pifs.get(trafficLabel);
        if (pifName == null) {
            // if not found in bridge map, maybe traffic label refers to pif already?
            File pif = new File("/sys/class/net/" + trafficLabel);
            if (pif.isDirectory()) {
                pifName = trafficLabel;
            }
        }
        if (vlanId != null && pifName != null) {
            String brName = generateVnetBrName(pifName, vlanId);
            deleteVnetBr(brName, true);
        }
    }
}
