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

package com.cloud.hypervisor.ovm3.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class Network extends OvmObject {
    private static final Logger LOGGER = Logger.getLogger(Network.class);
    private static final String START = "start";
    private static final String BRIDGE = "Bridge";
    private static final String ADDRESS = "Address";
    private static final String PHYSICAL = "Physical";
    private Map<String, Interface> interfaceList = null;
    private Object postDiscovery = null;
    private List<String> netInterfaces = new ArrayList<String>();

    public Network(Connection c) {
        setClient(c);
    }

    public Map<String, Interface> getInterfaceList()
            throws Ovm3ResourceException {
        discoverNetwork();
        return interfaceList;
    }

    public static class Interface {
        private final Map<String, String> iFace = new HashMap<String, String>() {
            {
                put("Type", null);
                put(PHYSICAL, null);
                put("Name", null);
                put(ADDRESS, null);
                put("Broadcast", null);
                put("MAC", null);
                put("Vlan", null);
            }
            private static final long serialVersionUID = 6L;
        };

        public Interface() {
        }

        public void setIfType(String t) {
            iFace.put("Type", t);
        }

        public String getIfType() {
            return iFace.get("Type");
        }

        public void setInterface(Map<String, String> itf) {
            iFace.putAll(itf);
        }

        public String getName() {
            return iFace.get("Name");
        }

        public String getPhysical() {
            return iFace.get(PHYSICAL);
        }

        public String getAddress() {
            return iFace.get(ADDRESS);
        }

        public String getBroadcast() {
            return iFace.get("Broadcast");
        }

        public String getMac() {
            return iFace.get("MAC");
        }

        public String setName(String name) {
            return iFace.put("Name", name);
        }

        public String setPhysical(String ph) {
            return iFace.put(PHYSICAL, ph);
        }

        public String setMac(String mac) {
            return iFace.put("MAC", mac);
        }
    }

    private Network.Interface getNetIface(String key, String val)
            throws Ovm3ResourceException {
        Map<String, Network.Interface> ifaces = getInterfaceList();
        for (final Entry<String, Interface> iface : ifaces.entrySet()) {
            String match = "default";
            if (ADDRESS.equals(key)) {
                match = iface.getValue().getAddress();
            }
            if ("Name".equals(key)) {
                match = iface.getKey();
            }
            if (match != null && match.equals(val)) {
                return iface.getValue();
            }
        }
        LOGGER.debug("Unable to find " + key + " Interface by value: " + val);
        setSuccess(false);
        return null;
    }

    public Network.Interface getInterfaceByIp(String ip)
            throws Ovm3ResourceException {
        return getNetIface(ADDRESS, ip);
    }

    public Network.Interface getInterfaceByName(String name)
            throws Ovm3ResourceException {
        return getNetIface("Name", name);
    }

    /* check if it is a BRIDGE */
    public String getPhysicalByBridgeName(String name)
            throws Ovm3ResourceException {
        return getInterfaceByName(name).getPhysical();
    }

    public Network.Interface getBridgeByName(String name)
            throws Ovm3ResourceException {
        if (getNetIface("Name", name) != null
                && getNetIface("Name", name).getIfType().contentEquals(BRIDGE)) {
            return getNetIface("Name", name);
        }
        LOGGER.debug("Unable to find bridge by name: " + name);
        setSuccess(false);
        return null;
    }

    public Network.Interface getBridgeByIp(String ip)
            throws Ovm3ResourceException {
        if (getNetIface(ADDRESS, ip) != null
                && getNetIface(ADDRESS, ip).getIfType().contentEquals(BRIDGE)) {
            return getNetIface(ADDRESS, ip);
        }
        LOGGER.debug("Unable to find bridge by ip: " + ip);
        setSuccess(false);
        return null;
    }

    /*
     * configure_virtual_ip, <class
     * 'agent.api.network.linux_network.LinuxNetwork'> argument: self - default:
     * None argument: virtual_ip - default: None argument: base_ip - default:
     * None
     */
    public Boolean configureVip(String vip, String baseip)
            throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("configure_virtual_ip", vip, baseip);
    }

    public Boolean ovsIpConfig(String net, String optype, String ip,
            String netmask) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("ovs_ip_config", net, optype, ip, netmask);
    }

    /*
     * Restriction: - data string that starts with leading spaces will be
     * rejected ovs_if_meta('bond0',
     * 'ethernet:c0a80100{192.168.1.0}:MANAGEMENT,CLUSTER_HEARTBEAT,LIVE_MIGRATE,VIRTUAL_MACHINE,STORAGE')
     */

    public Boolean discoverNetwork() throws Ovm3ResourceException {
        postDiscovery = callWrapper("discover_network");
        if (postDiscovery == null) {
            return false;
        }
        interfaceList = new HashMap<String, Interface>();
        Document xmlDocument = prepParse((String) postDiscovery);
        String path = "//Discover_Network_Result/Network/Active";
        String bpath = path + "/Bridges/Device";

        netInterfaces = new ArrayList<String>();
        netInterfaces.addAll(xmlToList(bpath + "/@Name", xmlDocument));
        for (String b : netInterfaces) {
            Map<String, String> br = xmlToMap(bpath + "[@Name='" + b
                    + "']/Family", xmlDocument);
            /* vifs are here too */
            String phyInt = (String) this.xmlToMap(
                    bpath + "[@Name='" + b + "']/Interfaces", xmlDocument).get(
                    "PhyInterface");
            Interface iface = new Interface();
            iface.setInterface(br);
            iface.setName(b);
            iface.setIfType(BRIDGE);
            if (phyInt == null) {
                iface.setIfType("Local");
            }
            iface.setPhysical(phyInt);
            interfaceList.put(b, iface);
        }
        /* add "physical" interfaces */
        bpath = path + "/Network/Device";
        netInterfaces = new ArrayList<String>();
        netInterfaces.addAll(xmlToList(bpath + "/@Name", xmlDocument));
        for (String p : netInterfaces) {
            Map<String, String> nf = xmlToMap("//Device[@Name='" + p + "']",
                    xmlDocument);
            Interface iface = new Interface();
            iface.setPhysical(nf.get("Basename"));
            iface.setName(p);
            iface.setMac(nf.get("MAC"));
            iface.setIfType(PHYSICAL);
            interfaceList.put(p, iface);
        }
        /* add virtual interfaces ? */
        return true;
    }

    public Boolean startOvsLocalConfig(String br) throws Ovm3ResourceException {
        String s = (String) ovsLocalConfig(START, br);
        if (s.startsWith(START)) {
            return true;
        }
        return false;
    }

    public Boolean stopOvsLocalConfig(String br) throws Ovm3ResourceException {
        String s = (String) ovsLocalConfig("stop", br);
        if (s.startsWith("stop")) {
            return true;
        }
        return false;
    }

    private Object ovsLocalConfig(String action, String br)
            throws Ovm3ResourceException {
        return callWrapper("ovs_local_config", action, br);
    }

    public Boolean startOvsVlanConfig(String dev, int vlan)
            throws Ovm3ResourceException {
        return ovsVlanConfig("add", dev, vlan);
    }

    public Boolean stopOvsVlanConfig(String dev, int vlan)
            throws Ovm3ResourceException {
        return ovsVlanConfig("remove", dev, vlan);
    }

    private Boolean ovsVlanConfig(String action, String net, int vlan)
            throws Ovm3ResourceException {
        Object x = callWrapper("ovs_vlan_config", action, net, vlan);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean startOvsBrConfig(String br, String dev)
            throws Ovm3ResourceException {
        String s = (String) ovsBrConfig(START, br, dev);
        if (s.startsWith(START)) {
            return true;
        }
        return false;
    }

    public Boolean stopOvsBrConfig(String br, String dev)
            throws Ovm3ResourceException {
        String s = (String) ovsBrConfig("stop", br, dev);
        if (s.startsWith("stop")) {
            return true;
        }
        return false;
    }

    public Object ovsBrConfig(String action, String br, String net)
            throws Ovm3ResourceException {
        return callWrapper("ovs_br_config", action, br, net);
    }

    /* 1 is untagged, goes till 4095 */
    public Boolean stopOvsVlanBridge(String br, String net, int vlan)
            throws Ovm3ResourceException {
        String s = (String) ovsVlanBridge("stop", br, net, vlan);
        if (s.startsWith("stop")) {
            return true;
        }
        return false;
    }

    public Boolean startOvsVlanBridge(String br, String net, int vlan)
            throws Ovm3ResourceException {
        String s = (String) ovsVlanBridge(START, br, net, vlan);
        /* 3.2.1 uses start, 3.3.1 and up uses added... */
        if (s.startsWith(START) || s.startsWith("Added")) {
            return true;
        }
        return false;
    }

    private Object ovsVlanBridge(String action, String br, String net, int vlan)
            throws Ovm3ResourceException {
        return callWrapper("ovs_vlan_bridge", action, br, net, vlan);
    }

    /*
     * deconfigure_virtual_ip, <class
     * 'agent.api.network.linux_network.LinuxNetwork'> argument: self - default:
     * None argument: virtual_ip - default: None
     */
}
