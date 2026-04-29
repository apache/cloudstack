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

import org.junit.Test;

public class NetworkTest {
    ConnectionTest con = new ConnectionTest();
    Network net = new Network(con);
    XmlTestResultTest results = new XmlTestResultTest();

    private String IP = "192.168.1.64";
    private String BR = "192.168.1.255";
    private String MAC = "52:54:00:24:47:70";
    private String INT = "xenbr0";
    private String PHY = "bond0";
    private String VLANBR = "bond0";
    public String getBroadcast() {
        return BR;
    }

    public String getMac() {
        return MAC;
    }

    public String getInterface() {
        return INT;
    }

    public String getPhysical() {
        return PHY;
    }

    public String getVlanBridge() {
        return VLANBR;
    }

    public String getVlanInterface() {
        return VLANINT;
    }

    public Integer getVlan() {
        return VLAN;
    }

    public String getControl() {
        return CONTROL;
    }

    private String VLANINT = "xenbr0";
    private Integer VLAN = 200;
    private String CONTROL = "control0";
    private String CONTROLMAC = "B2:D1:75:69:8C:58";
    private String EMPTY = results.escapeOrNot("<?xml version=\"1.0\" ?>"
            + "<Discover_Network_Result>" + "</Discover_Network_Result>");
    private String DISCOVERNETWORK = results
            .escapeOrNot("<?xml version=\"1.0\" ?>"
                    + "<Discover_Network_Result>" + "  <Network>"
                    + "    <Active>" + "      <Network>"
                    + "        <Device Name=\""
                    + PHY
                    + "\">"
                    + "          <MAC>52:54:00:24:47:70</MAC>"
                    + "          <Flags>(0x1043) IFF_UP IFF_BROADCAST IFF_RUNNING IFF_MULTICAST</Flags>"
                    + "          <MII>"
                    + "            <Autonegotiate>"
                    + "              <State>Incomplete</State>"
                    + "              <Speed>100baseT-FD</Speed>"
                    + "            </Autonegotiate>"
                    + "            <Link>ok</Link>"
                    + "            <Product>"
                    + "              <Vendor>00:00:00</Vendor>"
                    + "              <Model>0</Model>"
                    + "              <Revision>0</Revision>"
                    + "            </Product>"
                    + "            <Status>autonegotiation complete </Status>"
                    + "            <Capabilities>100baseT-FD 100baseT-HD 10baseT-FD 10baseT-HD</Capabilities>"
                    + "            <Advertising>100baseT-FD 100baseT-HD 10baseT-FD 10baseT-HD</Advertising>"
                    + "            <LinkPartner>100baseT-FD 100baseT-HD 10baseT-FD 10baseT-HD</LinkPartner>"
                    + "          </MII>"
                    + "          <WOL>"
                    + "            <WakeOnLan>disabled</WakeOnLan>"
                    + "          </WOL>"
                    + "          <SysFS>"
                    + "            <uevent>INTERFACE="
                    + PHY
                    + "IFINDEX=2</uevent>"
                    + "            <addr_assign_type>0</addr_assign_type>"
                    + "            <addr_len>6</addr_len>"
                    + "            <dev_id>0x0</dev_id>"
                    + "            <ifalias/>"
                    + "            <iflink>2</iflink>"
                    + "            <ifindex>2</ifindex>"
                    + "            <features>0x200041a0</features>"
                    + "            <type>1</type>"
                    + "            <link_mode>0</link_mode>"
                    + "            <carrier>1</carrier>"
                    + "            <speed>100</speed>"
                    + "            <duplex>full</duplex>"
                    + "            <dormant>0</dormant>"
                    + "            <operstate>up</operstate>"
                    + "            <mtu>1500</mtu>"
                    + "            <flags>0x1103</flags>"
                    + "            <tx_queue_len>1000</tx_queue_len>"
                    + "            <netdev_group>0</netdev_group>"
                    + "            <SysFSDev>"
                    + "              <vendor>0x10ec</vendor>"
                    + "              <device>0x8139</device>"
                    + "              <subsystem_vendor>0x1af4</subsystem_vendor>"
                    + "              <subsystem_device>0x1100</subsystem_device>"
                    + "              <class>0x020000</class>"
                    + "            </SysFSDev>"
                    + "          </SysFS>"
                    + "          <BootProto>none</BootProto>"
                    + "          <MetaData>ethernet:c0a80100{192.168.1.0}:MANAGEMENT,CLUSTER_HEARTBEAT,LIVE_MIGRATE,VIRTUAL_MACHINE,STORAGE</MetaData>"
                    + "        </Device>"
                    + "        <Device Name=\"eth1\">"
                    + "          <MAC>52:54:00:26:7F:A0</MAC>"
                    + "          <Flags>(0x1843) IFF_UP IFF_BROADCAST IFF_RUNNING IFF_SLAVE IFF_MULTICAST</Flags>"
                    + "          <MII>"
                    + "            <Autonegotiate>"
                    + "              <State>Incomplete</State>"
                    + "              <Speed>100baseT-FD</Speed>"
                    + "            </Autonegotiate>"
                    + "            <Link>ok</Link>"
                    + "            <Product>"
                    + "              <Vendor>00:00:00</Vendor>"
                    + "              <Model>0</Model>"
                    + "              <Revision>0</Revision>"
                    + "            </Product>"
                    + "            <Status>autonegotiation complete </Status>"
                    + "            <Capabilities>100baseT-FD 100baseT-HD 10baseT-FD 10baseT-HD</Capabilities>"
                    + "            <Advertising>100baseT-FD 100baseT-HD 10baseT-FD 10baseT-HD</Advertising>"
                    + "            <LinkPartner>100baseT-FD 100baseT-HD 10baseT-FD 10baseT-HD</LinkPartner>"
                    + "          </MII>"
                    + "          <WOL>"
                    + "            <WakeOnLan>disabled</WakeOnLan>"
                    + "          </WOL>"
                    + "          <SysFS>"
                    + "            <uevent>INTERFACE=eth1"
                    + "IFINDEX=3</uevent>"
                    + "            <addr_assign_type>0</addr_assign_type>"
                    + "            <addr_len>6</addr_len>"
                    + "            <dev_id>0x0</dev_id>"
                    + "            <ifalias/>"
                    + "            <iflink>3</iflink>"
                    + "            <ifindex>3</ifindex>"
                    + "            <features>0x200041a0</features>"
                    + "            <type>1</type>"
                    + "            <link_mode>0</link_mode>"
                    + "            <carrier>1</carrier>"
                    + "            <speed>100</speed>"
                    + "            <duplex>full</duplex>"
                    + "            <dormant>0</dormant>"
                    + "            <operstate>up</operstate>"
                    + "            <mtu>1500</mtu>"
                    + "            <flags>0x1903</flags>"
                    + "            <tx_queue_len>1000</tx_queue_len>"
                    + "            <netdev_group>0</netdev_group>"
                    + "            <SysFSDev>"
                    + "              <vendor>0x10ec</vendor>"
                    + "              <device>0x8139</device>"
                    + "              <subsystem_vendor>0x1af4</subsystem_vendor>"
                    + "              <subsystem_device>0x1100</subsystem_device>"
                    + "              <class>0x020000</class>"
                    + "            </SysFSDev>"
                    + "          </SysFS>"
                    + "          <BootProto>none</BootProto>"
                    + "        </Device>"
                    + "      </Network>"
                    + "      <Bonding>"
                    + "        <Device Name=\"bond1\">"
                    + "          <Bonding_Mode>active-backup</Bonding_Mode>"
                    + "          <Primary_Slave>eth1 (primary_reselect always)</Primary_Slave>"
                    + "          <Currently_Active_Slave>eth1</Currently_Active_Slave>"
                    + "          <MII_Status>up</MII_Status>"
                    + "          <MII_Polling_Interval>250</MII_Polling_Interval>"
                    + "          <Up_Delay>500</Up_Delay>"
                    + "          <Down_Delay>500</Down_Delay>"
                    + "          <Slave_Interface Name=\"eth1\">"
                    + "            <MII_Status>up</MII_Status>"
                    + "            <Speed>100 Mbps</Speed>"
                    + "            <Duplex>full</Duplex>"
                    + "            <Link_Failure_Count>0</Link_Failure_Count>"
                    + "            <Permanent_HW_addr>52:54:00:26:7f:a0</Permanent_HW_addr>"
                    + "          </Slave_Interface>"
                    + "          <Family Type=\"AF_INET\">"
                    + "            <MAC>52:54:00:26:7F:A0</MAC>"
                    + "            <mtu>1500</mtu>"
                    + "          </Family>"
                    + "          <BootProto>none</BootProto>"
                    + "        </Device>"
                    + "      </Bonding>"
                    + "      <Bridges>"
                    + "        <Device Name=\""
                    + INT
                    + "\">"
                    + "          <Family Type=\"AF_INET\">"
                    + "            <MAC>"
                    + MAC
                    + "</MAC>"
                    + "            <Address>"
                    + IP
                    + "</Address>"
                    + "            <Netmask>255.255.255.0</Netmask>"
                    + "            <Broadcast>"
                    + BR
                    + "</Broadcast>"
                    + "          </Family>"
                    + "          <Interfaces>"
                    + "            <PhyInterface>"
                    + PHY
                    + "</PhyInterface>"
                    + "          </Interfaces>"
                    + "          <BootProto>static</BootProto>"
                    + "        </Device>"
                    + "        <Device Name=\"xenbr1\">"
                    + "          <Family Type=\"AF_INET\">"
                    + "            <MAC>52:54:00:26:7F:A0</MAC>"
                    + "          </Family>"
                    + "          <Interfaces>"
                    + "            <PhyInterface>bond1</PhyInterface>"
                    + "          </Interfaces>"
                    + "          <BootProto>none</BootProto>"
                    + "        </Device>"
                    + "        <Device Name=\""
                    + CONTROL
                    + "\">"
                    + "          \"<Family Type=\"AF_INET\">"
                    + "            <MAC>"
                    + CONTROLMAC
                    + "</MAC>"
                    + "          </Family>"
                    + "          <Interfaces>"
                    + "          </Interfaces>"
                    + "          <BootProto>none</BootProto>"
                    + "        </Device>"
                    + "        <Device Name=\"xenbr0.200\">"
                    + "          <Family Type=\"AF_INET\">"
                    + "            <MAC>52:54:00:26:7F:A0</MAC>"
                    + "          </Family>"
                    + "          <Interfaces>"
                    + "            <PhyInterface>bond1.200</PhyInterface>"
                    + "          </Interfaces>"
                    + "         <BootProto>none</BootProto>"
                    + "        </Device>"
                    + "      </Bridges>"
                    + "      <Infiniband>"
                    + "      </Infiniband>"
                    + "    </Active>"
                    + "  </Network>"
                    + "</Discover_Network_Result>");

    public String getDiscoverNetwork() {
        return DISCOVERNETWORK;
    }

    @Test
    public void testDiscoverNetwork() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(net.discoverNetwork(), false);
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERNETWORK));
        results.basicBooleanTest(net.discoverNetwork());
        results.basicStringTest(net.getBridgeByIp(IP).getName(), INT);
        results.basicStringTest(net.getBridgeByName(INT).getAddress(), IP);
        results.basicStringTest(net.getInterfaceByName(CONTROL).getMac(), CONTROLMAC);
        net.getBridgeByIp("");
        results.basicBooleanTest(net.getSuccess(), false);
        net.getBridgeByName("");
        results.basicBooleanTest(net.getSuccess(), false);
        // results.basicStringTest(net.getBridgeByIp("").getName(), INT);
        results.basicStringTest(net.getInterfaceByIp(IP).getName(), INT);
        results.basicStringTest(net.getInterfaceByName(INT).getAddress(), IP);
        results.basicStringTest(net.getInterfaceByName(INT).getBroadcast(), BR);
        results.basicStringTest(net.getInterfaceByName(INT).getMac(), MAC);
        results.basicStringTest(net.getPhysicalByBridgeName(INT), PHY);

    }

    @Test
    public void testInterfacesNotFound() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(net.discoverNetwork(), false);
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERNETWORK));
        results.basicBooleanTest(net.discoverNetwork());
        if (net.getInterfaceByName(CONTROL+ "xx") == null) {
            System.out.println("yay!");
        };
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testLocalBreak() throws Ovm3ResourceException {
        con.setResult(results
                .errorResponseWrap(
                        1,
                        "exceptions.RuntimeError:Command: ['/etc/xen/scripts/linuxbridge/ovs-local-bridge', 'start', 'bridge=control0'] failed (1): stderr: Start local network: Bridge control0 Is Busy"));
        results.basicBooleanTest(net.startOvsLocalConfig(CONTROL), false);
    }

    @Test
    public void testLocal() throws Ovm3ResourceException {
        String resp = "local bridge " + CONTROL;
        con.setResult(results.simpleResponseWrap("start " + resp));
        results.basicBooleanTest(net.startOvsLocalConfig(CONTROL));
        con.setResult(results.simpleResponseWrap("stop " + resp));
        results.basicBooleanTest(net.stopOvsLocalConfig(CONTROL));
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testVlanBridgeBreak() throws Ovm3ResourceException {
        con.setResult(results
                .errorResponseWrap(
                        1,
                        "exceptions.RuntimeError:Command: ['/etc/xen/scripts/linuxbridge/ovs-local-bridge', 'start', 'bridge=control0'] failed (1): stderr: Start local network: Bridge control0 Is Busy"));
        results.basicBooleanTest(net.startOvsVlanBridge(
                VLANINT + "." + VLAN.toString(), VLANBR, VLAN), false);
    }

    @Test
    public void testVlanBridge() throws Ovm3ResourceException {
        String resp = "bridge=" + VLANINT + "." + VLAN.toString() + " netdev="
                + VLANBR + " vlan " + VLAN.toString();
        con.setResult(results.simpleResponseWrap("start " + resp));
        results.basicBooleanTest(net.startOvsVlanBridge(
                VLANINT + "." + VLAN.toString(), VLANBR, VLAN));
        con.setResult(results.simpleResponseWrap("stop " + resp));
        results.basicBooleanTest(net.stopOvsVlanBridge(
                VLANINT + "." + VLAN.toString(), VLANBR, VLAN));
    }
}
