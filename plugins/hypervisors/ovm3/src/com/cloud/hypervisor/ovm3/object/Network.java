/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
// import java.util.Map;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
// import java.util.HashMap;

/*
 * should become an interface implementation
 */
public class Network extends OvmObject {

    public Network(Connection c) {
        client = c;
    }

    private Map<String, Interface> interfaceList = null;

    public Map<String, Interface> getInterfaceList()
            throws ParserConfigurationException, IOException, Exception {
        try {
            this.discoverNetwork();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return interfaceList;
    }

    public void setBridgeList(Map<String, Interface> list) {
        interfaceList = list;
    }

    public class Interface {
        private Map<String, String> _interface = new HashMap<String, String>() {
            {
                put("Type", null);
                put("Physical", null);
                put("Name", null);
                put("Address", null);
                put("Broadcast", null);
                put("MAC", null);
                put("Vlan", null);
            }
        };

        public void setIfType(String t) {
            this._interface.put("Type", t);
        }
        public String getIfType() {
            return this._interface.get("Type");
        }
        public void setInterface(Map<String, String> itf) {
            this._interface.putAll(itf);
        }

        public String getName() {
            return _interface.get("Name");
        }

        public String getPhysical() {
            return _interface.get("Physical");
        }

        public String getAddress() {
            return _interface.get("Address");
        }

        public String getBroadcast() {
            return _interface.get("Broadcast");
        }

        public String getMac() {
            return _interface.get("MAC");
        }

        public String setName(String name) {
            return _interface.put("Name", name);
        }

        public String setPhysical(String ph) {
            return _interface.put("Physical", ph);
        }
        public String setAddress(String addr) {
            return _interface.put("Address", addr);
        }

        public String setBroadcast(String bcast) {
            return _interface.put("Broadcast", bcast);
        }

        public String setMac(String mac) {
            return _interface.put("MAC", mac);
        }
    }

    private Network.Interface _getInterface(String key, String val)
            throws ParserConfigurationException, IOException, Exception {
        HashMap<String, Network.Interface> ifaces = new HashMap<String, Network.Interface>();
        ifaces = (HashMap<String, Interface>) this.getInterfaceList();
        for (final Entry<String, Interface> iface : ifaces.entrySet()) {
            String match = "default";
            if (key.equals("Address"))
                match = iface.getValue().getAddress();
            if (key.equals("Name"))
                match = iface.getKey();
            // .getName();
            if (match != null && match.equals(val))
                return iface.getValue();
        }
        return null;
    }

    public Network.Interface getInterfaceByIp(String ip)
            throws ParserConfigurationException, IOException, Exception {
        return _getInterface("Address", ip);
    }

    public Network.Interface getInterfaceByName(String name)
            throws ParserConfigurationException, IOException, Exception {
        return _getInterface("Name", name);
    }
    /* check if it is a BRIDGE */
    public String getPhysicalByBridgeName(String name)
            throws ParserConfigurationException, IOException, Exception {
        return getInterfaceByName(name).getPhysical();
    }

    public Network.Interface getBridgeByName(String name)
            throws ParserConfigurationException, IOException, Exception {
        if (_getInterface("Name", name).getIfType().contentEquals("Bridge")) {
              return _getInterface("Name", name);
        }
        return null;
    }
    public Network.Interface getBridgeByIp(String ip)
            throws ParserConfigurationException, IOException, Exception {
        if (_getInterface("Address", ip).getIfType().contentEquals("Bridge")) {
              return _getInterface("Address", ip);
        }
        return null;
    }
    private Object postDiscovery = null;

    private List<String> netInterfaces = new ArrayList<String>();

    /*
     * ovs_bond_mode, <class 'agent.api.network.linux_network.LinuxNetwork'>
        Change Bond mode.

        @param bond   One of the logical channel bonds (bond0, bond1 ...etc)
        @mode         Current supported bonding modes (1 = active-backup, 4 = 802.3ad
                      6 = balance-alb).
        @return       If successful, returns bond's names and its new mode
                      Raises an exception on failure
        Restriction:
        -bond     must be one of the logical channel bonds (bond0, bond1 ...etc)

     */
    public Boolean ovsBondMode(String bond, String mode) throws XmlRpcException {
        Object x = callWrapper("ovs_bond_mode", bond, mode);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_change_mtu, <class 'agent.api.network.linux_network.LinuxNetwork'>
        Changes MTU on a physical,vlan,bond,and bridge interface.
        Changing a bond MTU will also change the MTU of its slaves.
        Changing the MTU of an interface that is part of a bridge,
        will cause the bridge MTU and all of the interfaces change.
        When a Guest VIF attach to a bridge,the VIF MTU will be set
        to the bridge MTU


        @param   interface    Physical,bond,vlan, and a bridge
        @param   MTU          Values are 1500 to 64000

        @return  If successful, returns the interface, and the new MTU
                 Raises an exception on failure

        Restriction:
        -Can not change the MTU of a bridge without interfaces.
        -VLAN MTU must less or equal to the MTU of the underlying
         physical interface.
     */
    public Boolean ovsChangeMtu(String net, int mtu) throws XmlRpcException {
        Object x = callWrapper("ovs_change_mtu", net, mtu);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_async_bridge, <class 'agent.api.network.linux_network.LinuxNetwork'>
     * argument: self - default: None argument: action - default: None argument:
     * br_name - default: None argument: net_dev - default: None
     */
    public Boolean ovsAsyncBridge(String action, String bridge, String netdev)
            throws XmlRpcException {
        Object x = callWrapper("ovs_async_bridge", action, bridge, netdev);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_bond_op, <class 'agent.api.network.linux_network.LinuxNetwork'>
     * argument: self - default: None argument: action - default: None argument:
     * bond - default: None argument: backup - default: None
     */
    public Boolean ovsBondOp(String action, String bond, String backup)
            throws XmlRpcException {
        Object x = callWrapper("ovs_bond_op", action, bond, backup);
        if (x == null)
            return true;

        return false;
    }

    /*
     * configure_virtual_ip, <class
     * 'agent.api.network.linux_network.LinuxNetwork'> argument: self - default:
     * None argument: virtual_ip - default: None argument: base_ip - default:
     * None
     */
    public Boolean configureVip(String vip, String baseip)
            throws XmlRpcException {
        Object x = callWrapper("configure_virtual_ip", vip, baseip);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_ip_config, <class 'agent.api.network.linux_network.LinuxNetwork'>
        Assigns/flushes IP, netmask address to a physical,VLAN, and bond interfaces.

        @param  interface    The interface on which to assign
        @param  optype (static|dynamic|flush)
            static:  Assigns the given IP, and netmask to the interface, and
                         saves the config file to /etc/sysconfig/network-scripts.
            dynamic: Flushes current address, and creats and save the config
                         file to /etc/sysconfig/network-scripts, (BOOTPROTO=dhcp)
            flush:   flushes the interface address,routes, removes the current
                         config file from /etc/sysconfig/network-scripts.
                         Creats a new one with BOOTPROTO=static

        @args   Required for the static option, otherwise it is ignored
            IP address:  IPv4 address in decimal notation (101.230.112)
            netmask:     Standard netmask in a decimal notation,NOT CIDR.
                             example(255.255.255.0)

        @return  If successful, returns the interface, and addresses added/flushed
                 Raises an exception on failure

        Restriction:
        -Interface must be physical, VLAN, or a Bond
        -Interface must not be a bridge port, or slave to a bond
        -Addresses must be valid in a decimal notation

     */
    public Boolean ovsIpConfig(String net, String optype, String ip,
            String netmask)
            throws XmlRpcException {
        Object x = callWrapper("ovs_ip_config", net, optype, ip, netmask);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_if_meta, <class 'agent.api.network.linux_network.LinuxNetwork'>
        This function creates meta data file meta-interface, and write the string
        (METADATA=data) to it.  This string is used by the manager to identify
        networks that interfaces belong to. Dom0 does not make used of this string,
        it just saves it and returns it during running, saved network discovery.

         - If an interface already has a meta data string, then it gets
           replace by the new one
         - An empty meta data string, indicates to remove the existing string
           (remove the meta-interface) file

        @param  interface    physical,VLAN, bond ...etc interfaces
        @param  data         meta data to save

        @return  If successful, returns the interface, and meta data
                 Raises an exception on failure

        Restriction:
        - data string that starts with leading spaces will be rejected
     * ovs_if_meta('bond0',
     * 'ethernet:c0a80100{192.168.1.0}:MANAGEMENT,CLUSTER_HEARTBEAT,LIVE_MIGRATE,VIRTUAL_MACHINE,STORAGE')
     */
    public Boolean ovsIfMeta(String net, String data) throws XmlRpcException {
        Object x = callWrapper("ovs_if_meta", net, data);
        if (x == null)
            return true;

        return false;
    }


    /*
     * ovs_bond_config, <class 'agent.api.network.linux_network.LinuxNetwork'>
     * argument: self - default: None argument: action - default: None argument:
     * bond - default: None
     */
    public Boolean ovsBondConfig(String action, String bond)
            throws XmlRpcException {
        Object x = callWrapper("ovs_bond_config", action, bond);
        if (x == null)
            return true;

        return false;
    }

    /*
     * discover_network, <class 'agent.api.network.linux_network.LinuxNetwork'>
        Discover information about the current network configuration.
        This includes the state of physical NICs, bonds, and bridges.  Also return
        information stored for this server that is needed to configure
        the network when the OVM Manager is not available.

        Discovery of the current network configuration is handled by invoking
        a python extension that calls legacy C code from the VI agent.

        @param None

        @return    Returns the discovery data as an XML document.
                   Raises an exception on failure.
     * <?xml version="1.0" ?> <Discover_Network_Result>
     * <Network><Active><Network> <Device Name="eth0">
     * <MAC>52:54:00:24:47:70</MAC> <Flags>(0x1843) IFF_UP IFF_BROADCAST
     * IFF_RUNNING IFF_SLAVE IFF_MULTICAST</Flags> <ETHTOOL> <Autonegotiate>
     * <State>Complete</State> <Speed>1000baseT-FD</Speed> </Autonegotiate>
     * <Link>ok</Link> <Capabilities>10baseT-HD 10baseT-FD 100baseT-HD
     * 100baseT-FD 1000baseT-FD</Capabilities> <Advertising>10baseT-HD
     * 10baseT-FD 100baseT-HD 100baseT-FD 1000baseT-FD</Advertising> </ETHTOOL>
     * <WOL> <WakeOnLan>disabled</WakeOnLan> </WOL> <SysFS>
     * <uevent>INTERFACE=eth0 IFINDEX=2</uevent>
     * <addr_assign_type>0</addr_assign_type> <addr_len>6</addr_len>
     * <dev_id>0x0</dev_id> <ifalias/> <iflink>2</iflink> <ifindex>2</ifindex>
     * <features>0x40014ba9</features> <type>1</type> <link_mode>0</link_mode>
     * <carrier>1</carrier> <speed>1000</speed> <duplex>full</duplex>
     * <dormant>0</dormant> <operstate>up</operstate> <mtu>1500</mtu>
     * <flags>0x1903</flags> <tx_queue_len>1000</tx_queue_len>
     * <netdev_group>0</netdev_group> <SysFSDev> <vendor>0x8086</vendor>
     * <device>0x100e</device> <subsystem_vendor>0x1af4</subsystem_vendor>
     * <subsystem_device>0x1100</subsystem_device> <class>0x020000</class>
     * </SysFSDev> </SysFS> <BootProto>none</BootProto> </Device> ....
     * </Network> <Bonding> <Device Name="bond0">
     * <Bonding_Mode>active-backup</Bonding_Mode> <Primary_Slave>eth0
     * (primary_reselect always)</Primary_Slave>
     * <Currently_Active_Slave>eth0</Currently_Active_Slave>
     * <MII_Status>up</MII_Status>
     * <MII_Polling_Interval>250</MII_Polling_Interval> <Up_Delay>500</Up_Delay>
     * <Down_Delay>500</Down_Delay> <Slave_Interface Name="eth0">
     * <MII_Status>up</MII_Status> <Speed>1000 Mbps</Speed>
     * <Duplex>full</Duplex> <Link_Failure_Count>0</Link_Failure_Count>
     * <Permanent_HW_addr>52:54:00:24:47:70</Permanent_HW_addr>
     * </Slave_Interface> <Family Type="AF_INET"> <MAC>52:54:00:24:47:70</MAC>
     * <mtu>1500</mtu> </Family> <BootProto>none</BootProto>
     * <MetaData>ethernet:c0a80100
     * {192.168.1.0}:MANAGEMENT,CLUSTER_HEARTBEAT,LIVE_MIGRATE
     * ,VIRTUAL_MACHINE,STORAGE</MetaData> </Device> </Bonding> <Bridges>
     * <Device Name="c0a80100"> <Family Type="AF_INET">
     * <MAC>52:54:00:24:47:70</MAC> <Address>192.168.1.64</Address>
     * <Netmask>255.255.255.0</Netmask> <Broadcast>192.168.1.255</Broadcast>
     * </Family> <Interfaces> <PhyInterface>bond0</PhyInterface> </Interfaces>
     * <BootProto>static</BootProto></Device> </Bridges> <Infiniband>
     * </Infiniband> </Active></Network></Discover_Network_Result>
     */
    /* put more in when required, for now ok */
    public Boolean discoverNetwork() throws ParserConfigurationException,
            IOException, Exception {
        // if (postDiscovery == null) {
            postDiscovery = callWrapper("discover_network");
            this.interfaceList = new HashMap<String, Interface>();
        // }
        // System.out.println(postDiscovery);
        Document xmlDocument = prepParse((String) postDiscovery);
        String path = "//Discover_Network_Result/Network/Active";
        String bpath = path + "/Bridges/Device";

        netInterfaces = new ArrayList<String>();
        netInterfaces.addAll(xmlToList(bpath + "/@Name",
                xmlDocument));
        for (String b : netInterfaces) {
            Map<String, String> br = xmlToMap(bpath + "[@Name='" + b
                    + "']/Family", xmlDocument);
            /* vifs are here too */
            String phyInt = (String) this.xmlToMap(bpath + "[@Name='" + b
                    + "']/Interfaces", xmlDocument).get("PhyInterface");
            Interface iface = new Interface();
            iface.setInterface(br);
            iface.setName(b);
            iface.setIfType("Bridge");
            if (phyInt == null) {
                iface.setIfType("Local");
            }
            iface.setPhysical(phyInt);
            interfaceList.put(b, iface);
        }
        /* add "physical" interfaces */
        bpath =  path + "/Network/Device";
        netInterfaces = new ArrayList<String>();
        netInterfaces.addAll(xmlToList(bpath + "/@Name", xmlDocument));
        for (String p : netInterfaces) {
            Map<String, String> nf = xmlToMap("//Device[@Name='" + p
                    + "']", xmlDocument);
            Interface iface = new Interface();
            iface.setPhysical(nf.get("Basename"));
            iface.setName(p);
            iface.setMac(nf.get("MAC"));
            iface.setIfType("Physical");
            interfaceList.put(p, iface);
        }
        /* add virtual interfaces ? */
        if (postDiscovery == null)
            return false;

        return true;
    }

    /*
     * ovs_local_config, <class 'agent.api.network.linux_network.LinuxNetwork'>
        Configure a Local Bridge ((NIC-less bridge)

        @param action (start | stop)
             start: Creates local bridge without a physical interface, and saves bridge config
                    file in /etc/sysconfig/network-scripts
             stop:  Deletes local bridge, removes bridge config file from
                    /etc/sysconfig/network-scripts

        @param br_name       The bridge name to add

        @return       If successful, returns the name of the bridge
                      Raises an exception on failure

     */
    public Boolean startOvsLocalConfig(String br)
            throws XmlRpcException {
        return ovsLocalConfig("start", br);
    }

    public Boolean stopOvsLocalConfig(String br)
            throws XmlRpcException {
        return ovsLocalConfig("stop", br);
    }

    public Boolean ovsLocalConfig(String action, String br)
            throws XmlRpcException {
        Object x = callWrapper("ovs_local_config", action, br);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_vlan_config, <class 'agent.api.network.linux_network.LinuxNetwork'>
        Creates a VLAN interface on a physical, or a bond interface.

        @param  action (add|remove)
              add:    Creates a VLAN on an interface,saves the VLAN config file in
                      /etc/sysconfig/network-scripts
              remove: Removes a VLAN from an interfacei,removes its config file from
                      /etc/sysconfig/network-scripts

        @param   interface    The interface on which to create a VLAN
        @param   vlanid       VLAN ID (2-4095)

        @return  If successful, returns the interface, and VLAN created
                 Raises an exception on failure

        Restriction:
        -Interface  must be physical, or bond
        -Interface must not be member of a bridge, or slave to a bond
        -VLAN ID must not exist on the same interface

     */
    public Boolean startOvsVlanConfig(String dev, int vlan)
            throws XmlRpcException {
        return ovsVlanConfig("add", dev, vlan);
    }

    public Boolean stopOvsVlanConfig(String dev, int vlan)
            throws XmlRpcException {
        return ovsVlanConfig("del", dev, vlan);
    }
    public Boolean ovsVlanConfig(String action, String net, int vlan)
            throws XmlRpcException {
        Object x = callWrapper("ovs_vlan_config", action, net, vlan);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_br_config, <class 'agent.api.network.linux_network.LinuxNetwork'>
        Configure a Standard Bridge.

        @param action (start | stop)
              start: Creates the bridge, Copies the IP and MAC addresses from netdev to bridge,
                     enslaves net_dev to bridge, and saves bridge config files in
                     /etc/sysconfig/network-scripts
              stop:  Removes net_dev from the bridge,transfers addresses, routes from bridge to
                     net_dev, deletes the bridge, and revomes bridge config files from
                     /etc/sysconfig/network-scripts

        @param br_name       The bridge name to add
        @param net_dev       The physical interface to add to the bridge

        @return    If successful, returns the names of the bridge and it's physical interface.
                   Raises an exception on failure
        Restriction:
        -net_dev  must be physical, or bond
        -net_dev  must not be member of a bridge, or slave to a bond
     */
    public Boolean startOvsBrConfig(String br, String dev)
            throws XmlRpcException {
        return ovsBrConfig("start", br, dev);
    }

    public Boolean stopOvsBrConfig(String br, String dev)
            throws XmlRpcException {
        return ovsBrConfig("stop", br, dev);
    }

    public Boolean ovsBrConfig(String action, String br, String net)
            throws XmlRpcException {
        Object x = callWrapper("ovs_br_config", action, br, net);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_vlan_bridge, <class 'agent.api.network.linux_network.LinuxNetwork'>
         @param action (start | stop)
              start: Creates the bridge, creats VLAN on net_dev,enslaves
                     the VLAN to the bridge, and saves VLAN bridge config
                     files in /etc/sysconfig/network-scripts
              stop:  Removes the VLAN from the bridge, removes the VLAN,
                     deletes the bridge, and removes VLAN bridge config files
                     from /etc/sysconfig/network-scripts

        @param br_name       The bridge name to add
        @param net_dev       The physical interface on which to create a VLAN.
        @param vlan_id       VLAN ID (1-4095). VLAN ID of 1 is the untagged VLAN.

        @return       If successful, returns the names of the bridge and it's VLAN interface
                      Raises an exception on failure
     */
    public Boolean stopOvsVlanBridge(String br, String net, int vlan) throws XmlRpcException {
        return ovsVlanBridge("stop", br, net, vlan);
    }

    public Boolean startOvsVlanBridge(String br, String net, int vlan) throws XmlRpcException {
        return ovsVlanBridge("start", br, net, vlan);
    }

    public Boolean ovsVlanBridge(String action, String br, String net, int vlan)
            throws XmlRpcException {
        Object x = callWrapper("ovs_vlan_bridge", action, br, net, vlan);
        if (x == null)
            return true;

        return false;
    }

    /*
     * deconfigure_virtual_ip, <class
     * 'agent.api.network.linux_network.LinuxNetwork'> argument: self - default:
     * None argument: virtual_ip - default: None
     */
    public Boolean deconfigureVip(String vip) throws XmlRpcException {
        Object x = callWrapper("deconfigure_virtual_ip", vip);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_async_bond, <class 'agent.api.network.linux_network.LinuxNetwork'>
     * argument: self - default: None argument: action - default: None argument:
     * bond - default: None
     */
    public Boolean ovsAsyncBond(String action, String bond)
            throws XmlRpcException {
        Object x = callWrapper("ovs_async_bond", action, bond);
        if (x == null)
            return true;

        return false;
    }
}
