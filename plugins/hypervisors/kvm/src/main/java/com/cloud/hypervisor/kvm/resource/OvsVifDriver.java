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
import com.cloud.hypervisor.kvm.dpdk.DpdkDriver;
import com.cloud.hypervisor.kvm.dpdk.DpdkDriverImpl;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelper;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.network.Networks;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

public class OvsVifDriver extends VifDriverBase {
    private static final Logger s_logger = Logger.getLogger(OvsVifDriver.class);
    private int _timeout;
    private String _controlCidr = NetUtils.getLinkLocalCIDR();
    private DpdkDriver dpdkDriver;

    @Override
    public void configure(Map<String, Object> params) throws ConfigurationException {
        super.configure(params);

        getPifs();

        String networkScriptsDir = (String)params.get("network.scripts.dir");
        if (networkScriptsDir == null) {
            networkScriptsDir = "scripts/vm/network/vnet";
        }

        String dpdk = (String) params.get("openvswitch.dpdk.enabled");
        if (StringUtils.isNotBlank(dpdk) && Boolean.parseBoolean(dpdk)) {
            dpdkDriver = new DpdkDriverImpl();
        }

        String controlCidr = (String)params.get("control.cidr");
        if (com.cloud.utils.StringUtils.isNotBlank(controlCidr)) {
            _controlCidr = controlCidr;
        }

        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;
    }

    public void getPifs() {
        final String cmdout = Script.runSimpleBashScript("ovs-vsctl list-br | sed '{:q;N;s/\\n/%/g;t q}'");
        s_logger.debug("cmdout was " + cmdout);
        final List<String> bridges = Arrays.asList(cmdout.split("%"));
        for (final String bridge : bridges) {
            s_logger.debug("looking for pif for bridge " + bridge);
            // String pif = getOvsPif(bridge);
            // Not really interested in the pif name at this point for ovs
            // bridges
            final String pif = bridge;
            if (_libvirtComputingResource.isPublicBridge(bridge)) {
                _pifs.put("public", pif);
            }
            if (_libvirtComputingResource.isGuestBridge(bridge)) {
                _pifs.put("private", pif);
            }
            _pifs.put(bridge, pif);
        }
        s_logger.debug("done looking for pifs, no more bridges");
    }

    /**
     * Plug interface with DPDK support:
     *      - Create a new port with DPDK support for the interface
     *      - Set the 'intf' path to the new port
     */
    protected void plugDPDKInterface(InterfaceDef intf, String trafficLabel, Map<String, String> extraConfig,
                                     String vlanId, String guestOsType, NicTO nic, String nicAdapter) {
        s_logger.debug("DPDK support enabled: configuring per traffic label " + trafficLabel);
        String dpdkOvsPath = _libvirtComputingResource.dpdkOvsPath;
        if (StringUtils.isBlank(dpdkOvsPath)) {
            throw new CloudRuntimeException("DPDK is enabled on the host but no OVS path has been provided");
        }
        String port = dpdkDriver.getNextDpdkPort();
        DpdkHelper.VHostUserMode dpdKvHostUserMode = dpdkDriver.getDpdkvHostUserMode(extraConfig);
        dpdkDriver.addDpdkPort(_pifs.get(trafficLabel), port, vlanId, dpdKvHostUserMode, dpdkOvsPath);
        String interfaceMode = dpdkDriver.getGuestInterfacesModeFromDpdkVhostUserMode(dpdKvHostUserMode);
        intf.defDpdkNet(dpdkOvsPath, port, nic.getMac(),
                getGuestNicModel(guestOsType, nicAdapter), 0,
                dpdkDriver.getExtraDpdkProperties(extraConfig),
                interfaceMode, nic.getMtu());
    }

    @Override
    public InterfaceDef plug(NicTO nic, String guestOsType, String nicAdapter, Map<String, String> extraConfig) throws InternalErrorException, LibvirtException {
        s_logger.debug("plugging nic=" + nic);

        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();
        if (!_libvirtComputingResource.dpdkSupport || !nic.isDpdkEnabled()) {
            // Let libvirt handle OVS ports creation when DPDK property is disabled or when it is enabled but disabled for the nic
            // For DPDK support, libvirt does not handle ports creation, invoke 'addDpdkPort' method
            intf.setVirtualPortType("openvswitch");
        }

        String vlanId = null;
        String logicalSwitchUuid = null;
        if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan) {
            vlanId = Networks.BroadcastDomainType.getValue(nic.getBroadcastUri());
        } else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Lswitch) {
            logicalSwitchUuid = Networks.BroadcastDomainType.getValue(nic.getBroadcastUri());
        } else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Pvlan) {
            // TODO consider moving some of this functionality from NetUtils to Networks....
            vlanId = NetUtils.getPrimaryPvlanFromUri(nic.getBroadcastUri());
        }
        String trafficLabel = nic.getName();
        if (nic.getType() == Networks.TrafficType.Guest) {
            Integer networkRateKBps = (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1) ? nic.getNetworkRateMbps().intValue() * 128 : 0;
            if ((nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan || nic.getBroadcastType() == Networks.BroadcastDomainType.Pvlan) &&
                    !vlanId.equalsIgnoreCase("untagged")) {
                if (trafficLabel != null && !trafficLabel.isEmpty()) {
                    if (_libvirtComputingResource.dpdkSupport && nic.isDpdkEnabled()) {
                        plugDPDKInterface(intf, trafficLabel, extraConfig, vlanId, guestOsType, nic, nicAdapter);
                    } else {
                        s_logger.debug("creating a vlan dev and bridge for guest traffic per traffic label " + trafficLabel);
                        intf.defBridgeNet(_pifs.get(trafficLabel), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
                        intf.setVlanTag(Integer.parseInt(vlanId));
                    }
                } else {
                    intf.defBridgeNet(_pifs.get("private"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
                    intf.setVlanTag(Integer.parseInt(vlanId));
                }
            } else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Lswitch || nic.getBroadcastType() == Networks.BroadcastDomainType.OpenDaylight) {
                s_logger.debug("nic " + nic + " needs to be connected to LogicalSwitch " + logicalSwitchUuid);
                intf.setVirtualPortInterfaceId(nic.getUuid());
                String brName = (trafficLabel != null && !trafficLabel.isEmpty()) ? _pifs.get(trafficLabel) : _pifs.get("private");
                intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
            } else if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vswitch) {
                String vnetId = Networks.BroadcastDomainType.getValue(nic.getBroadcastUri());
                String brName = "OVSTunnel" + vnetId;
                s_logger.debug("nic " + nic + " needs to be connected to LogicalSwitch " + brName);
                intf.defBridgeNet(brName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
            } else {
                intf.defBridgeNet(_bridges.get("guest"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, nic.getMtu());
            }
        } else if (nic.getType() == Networks.TrafficType.Control) {
            /* Make sure the network is still there */
            createControlNetwork(_bridges.get("linklocal"));
            intf.defBridgeNet(_bridges.get("linklocal"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), 0, null);
        } else if (nic.getType() == Networks.TrafficType.Public) {
            Integer networkRateKBps = (nic.getNetworkRateMbps() != null && nic.getNetworkRateMbps().intValue() != -1) ? nic.getNetworkRateMbps().intValue() * 128 : 0;
            if (nic.getBroadcastType() == Networks.BroadcastDomainType.Vlan && !vlanId.equalsIgnoreCase("untagged")) {
                if (trafficLabel != null && !trafficLabel.isEmpty()) {
                    s_logger.debug("creating a vlan dev and bridge for public traffic per traffic label " + trafficLabel);
                    intf.defBridgeNet(_pifs.get(trafficLabel), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, null);
                    intf.setVlanTag(Integer.parseInt(vlanId));
                } else {
                    intf.defBridgeNet(_pifs.get("public"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, null);
                    intf.setVlanTag(Integer.parseInt(vlanId));
                }
            } else {
                intf.defBridgeNet(_bridges.get("public"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), networkRateKBps, null);
            }
        } else if (nic.getType() == Networks.TrafficType.Management) {
            intf.defBridgeNet(_bridges.get("private"), null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), 0, null);
        } else if (nic.getType() == Networks.TrafficType.Storage) {
            String storageBrName = nic.getName() == null ? _bridges.get("private") : nic.getName();
            intf.defBridgeNet(storageBrName, null, nic.getMac(), getGuestNicModel(guestOsType, nicAdapter), 0, null);
        }
        return intf;
    }

    @Override
    public void unplug(InterfaceDef iface) {
        // Libvirt apparently takes care of this, see BridgeVifDriver unplug
        if (_libvirtComputingResource.dpdkSupport && StringUtils.isNotBlank(iface.getDpdkSourcePort())) {
            // If DPDK is enabled, we'll need to cleanup the port as libvirt won't
            String dpdkPort = iface.getDpdkSourcePort();
            String cmd = String.format("ovs-vsctl del-port %s", dpdkPort);
            s_logger.debug("Removing DPDK port: " + dpdkPort);
            Script.runSimpleBashScript(cmd);
        }
    }


    @Override
    public void attach(LibvirtVMDef.InterfaceDef iface) {
        Script.runSimpleBashScript("ovs-vsctl add-port " + iface.getBrName() + " " + iface.getDevName());
    }

    @Override
    public void detach(LibvirtVMDef.InterfaceDef iface) {
        Script.runSimpleBashScript("ovs-vsctl port-to-br " + iface.getDevName() + " && ovs-vsctl del-port " + iface.getBrName() + " " + iface.getDevName());
    }

    private void deleteExitingLinkLocalRouteTable(String linkLocalBr) {
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
                if (!tokens[2].equalsIgnoreCase(linkLocalBr)) {
                    Script.runSimpleBashScript("ip route del " + _controlCidr);
                } else {
                    foundLinkLocalBr = true;
                }
            }
        }
        if (!foundLinkLocalBr) {
            Script.runSimpleBashScript("ip address add " + NetUtils.getLinkLocalAddressFromCIDR(_controlCidr) + " dev " + linkLocalBr + ";" + "ip route add " + _controlCidr + " dev " + linkLocalBr + " src " +
                    NetUtils.getLinkLocalGateway(_controlCidr));
        }
    }

    @Override
    public void createControlNetwork(String privBrName) {
        deleteExitingLinkLocalRouteTable(privBrName);
        if (!isExistingBridge(privBrName)) {
            Script.runSimpleBashScript("ovs-vsctl add-br " + privBrName + "; ip link set " + privBrName + " up; ip address add " + NetUtils.getLinkLocalAddressFromCIDR(_controlCidr) + " dev " + privBrName, _timeout);
        }
    }

    @Override
    public boolean isExistingBridge(String bridgeName) {
        Script command = new Script("/bin/sh", _timeout);
        command.add("-c");
        command.add("ovs-vsctl br-exists " + bridgeName);
        String result = command.execute(null);
        if ("0".equals(result)) {
            return true;
        } else {
            return false;
        }
    }
}
