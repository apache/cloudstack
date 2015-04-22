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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtConnectionWrapper;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtRequestWrapper;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;

@RunWith(PowerMockRunner.class)
public class LibvirtComputingResourceTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    String _hyperVisorType = "kvm";
    Random _random = new Random();

    /**
        This test tests if the Agent can handle a vmSpec coming
        from a <=4.1 management server.

        The overcommit feature has not been merged in there and thus
        only 'speed' is set.
     */
    @Test
    public void testCreateVMFromSpecLegacy() {
        final int id = _random.nextInt(65534);
        final String name = "test-instance-1";

        final int cpus = _random.nextInt(2) + 1;
        final int speed = 1024;
        final int minRam = 256 * 1024;
        final int maxRam = 512 * 1024;

        final String os = "Ubuntu";

        final String vncAddr = "";
        final String vncPassword = "mySuperSecretPassword";

        final LibvirtComputingResource lcr = new LibvirtComputingResource();
        final VirtualMachineTO to = new VirtualMachineTO(id, name, VirtualMachine.Type.User, cpus, speed, minRam, maxRam, BootloaderType.HVM, os, false, false, vncPassword);
        to.setVncAddr(vncAddr);
        to.setUuid("b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9");

        final LibvirtVMDef vm = lcr.createVMFromSpec(to);
        vm.setHvsType(_hyperVisorType);

        verifyVm(to, vm);
    }

    /**
        This test verifies that CPU topology is properly set for hex-core
     */
    @Test
    public void testCreateVMFromSpecWithTopology6() {
        final int id = _random.nextInt(65534);
        final String name = "test-instance-1";

        final int cpus = 12;
        final int minSpeed = 1024;
        final int maxSpeed = 2048;
        final int minRam = 256 * 1024;
        final int maxRam = 512 * 1024;

        final String os = "Ubuntu";

        final String vncAddr = "";
        final String vncPassword = "mySuperSecretPassword";

        final LibvirtComputingResource lcr = new LibvirtComputingResource();
        final VirtualMachineTO to = new VirtualMachineTO(id, name, VirtualMachine.Type.User, cpus, minSpeed, maxSpeed, minRam, maxRam, BootloaderType.HVM, os, false, false, vncPassword);
        to.setVncAddr(vncAddr);
        to.setUuid("b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9");

        final LibvirtVMDef vm = lcr.createVMFromSpec(to);
        vm.setHvsType(_hyperVisorType);

        verifyVm(to, vm);
    }

    /**
        This test verifies that CPU topology is properly set for quad-core
     */
    @Test
    public void testCreateVMFromSpecWithTopology4() {
        final int id = _random.nextInt(65534);
        final String name = "test-instance-1";

        final int cpus = 8;
        final int minSpeed = 1024;
        final int maxSpeed = 2048;
        final int minRam = 256 * 1024;
        final int maxRam = 512 * 1024;

        final String os = "Ubuntu";

        final String vncAddr = "";
        final String vncPassword = "mySuperSecretPassword";

        final LibvirtComputingResource lcr = new LibvirtComputingResource();
        final VirtualMachineTO to = new VirtualMachineTO(id, name, VirtualMachine.Type.User, cpus, minSpeed, maxSpeed, minRam, maxRam, BootloaderType.HVM, os, false, false, vncPassword);
        to.setVncAddr(vncAddr);
        to.setUuid("b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9");

        final LibvirtVMDef vm = lcr.createVMFromSpec(to);
        vm.setHvsType(_hyperVisorType);

        verifyVm(to, vm);
    }

    /**
        This test tests if the Agent can handle a vmSpec coming
        from a >4.1 management server.

        It tests if the Agent can handle a vmSpec with overcommit
        data like minSpeed and maxSpeed in there
     */
    @Test
    public void testCreateVMFromSpec() {
        final int id = _random.nextInt(65534);
        final String name = "test-instance-1";

        final int cpus = _random.nextInt(2) + 1;
        final int minSpeed = 1024;
        final int maxSpeed = 2048;
        final int minRam = 256 * 1024;
        final int maxRam = 512 * 1024;

        final String os = "Ubuntu";

        final String vncAddr = "";
        final String vncPassword = "mySuperSecretPassword";

        final LibvirtComputingResource lcr = new LibvirtComputingResource();
        final VirtualMachineTO to =
                new VirtualMachineTO(id, name, VirtualMachine.Type.User, cpus, minSpeed, maxSpeed, minRam, maxRam, BootloaderType.HVM, os, false, false, vncPassword);
        to.setVncAddr(vncAddr);
        to.setUuid("b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9");

        final LibvirtVMDef vm = lcr.createVMFromSpec(to);
        vm.setHvsType(_hyperVisorType);

        verifyVm(to, vm);
    }

    private void verifyVm(final VirtualMachineTO to, final LibvirtVMDef vm) {
        final Document domainDoc = parse(vm.toString());
        assertXpath(domainDoc, "/domain/@type", vm.getHvsType());
        assertXpath(domainDoc, "/domain/name/text()", to.getName());
        assertXpath(domainDoc, "/domain/uuid/text()", to.getUuid());
        assertXpath(domainDoc, "/domain/description/text()", to.getOs());
        assertXpath(domainDoc, "/domain/clock/@offset", "utc");
        assertNodeExists(domainDoc, "/domain/features/pae");
        assertNodeExists(domainDoc, "/domain/features/apic");
        assertNodeExists(domainDoc, "/domain/features/acpi");
        assertXpath(domainDoc, "/domain/devices/serial/@type", "pty");
        assertXpath(domainDoc, "/domain/devices/serial/target/@port", "0");
        assertXpath(domainDoc, "/domain/devices/graphics/@type", "vnc");
        assertXpath(domainDoc, "/domain/devices/graphics/@listen", to.getVncAddr());
        assertXpath(domainDoc, "/domain/devices/graphics/@autoport", "yes");
        assertXpath(domainDoc, "/domain/devices/graphics/@passwd", to.getVncPassword());

        assertXpath(domainDoc, "/domain/devices/console/@type", "pty");
        assertXpath(domainDoc, "/domain/devices/console/target/@port", "0");
        assertXpath(domainDoc, "/domain/devices/input/@type", "tablet");
        assertXpath(domainDoc, "/domain/devices/input/@bus", "usb");

        assertXpath(domainDoc, "/domain/memory/text()", String.valueOf( to.getMaxRam() / 1024 ));
        assertXpath(domainDoc, "/domain/currentMemory/text()", String.valueOf( to.getMinRam() / 1024 ));

        assertXpath(domainDoc, "/domain/devices/memballoon/@model", "virtio");
        assertXpath(domainDoc, "/domain/vcpu/text()", String.valueOf(to.getCpus()));

        assertXpath(domainDoc, "/domain/os/type/@machine", "pc");
        assertXpath(domainDoc, "/domain/os/type/text()", "hvm");

        assertNodeExists(domainDoc, "/domain/cpu");
        assertNodeExists(domainDoc, "/domain/os/boot[@dev='cdrom']");
        assertNodeExists(domainDoc, "/domain/os/boot[@dev='hd']");

        assertXpath(domainDoc, "/domain/on_reboot/text()", "restart");
        assertXpath(domainDoc, "/domain/on_poweroff/text()", "destroy");
        assertXpath(domainDoc, "/domain/on_crash/text()", "destroy");
    }

    static Document parse(final String input) {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(input.getBytes()));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IllegalArgumentException("Cloud not parse: "+input, e);
        }
    }

    static void assertNodeExists(final Document doc, final String xPathExpr) {
        try {
            Assert.assertNotNull(XPathFactory.newInstance().newXPath()
                    .evaluate(xPathExpr, doc, XPathConstants.NODE));
        } catch (final XPathExpressionException e) {
            Assert.fail(e.getMessage());
        }
    }

    static void assertXpath(final Document doc, final String xPathExpr,
            final String expected) {
        try {
            Assert.assertEquals(expected, XPathFactory.newInstance().newXPath()
                    .evaluate(xPathExpr, doc));
        } catch (final XPathExpressionException e) {
            Assert.fail("Could not evaluate xpath" + xPathExpr + ":"
                    + e.getMessage());
        }
    }

    @Test
    public void testGetNicStats() {
        //this test is only working on linux because of the loopback interface name
        //also the tested code seems to work only on linux
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        final Pair<Double, Double> stats = LibvirtComputingResource.getNicStats("lo");
        assertNotNull(stats);
    }

    @Test
    public void testUUID() {
        String uuid = "1";
        final LibvirtComputingResource lcr = new LibvirtComputingResource();
        uuid = lcr.getUuid(uuid);
        Assert.assertTrue(!uuid.equals("1"));

        final String oldUuid = UUID.randomUUID().toString();
        uuid = oldUuid;
        uuid = lcr.getUuid(uuid);
        Assert.assertTrue(uuid.equals(oldUuid));
    }

    private static final String VMNAME = "test";

    @Test
    public void testGetVmStat() throws LibvirtException {
        final Connect connect = Mockito.mock(Connect.class);
        final Domain domain = Mockito.mock(Domain.class);
        final DomainInfo domainInfo = new DomainInfo();
        Mockito.when(domain.getInfo()).thenReturn(domainInfo);
        Mockito.when(connect.domainLookupByName(VMNAME)).thenReturn(domain);
        final NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.cpus = 8;
        nodeInfo.memory = 8 * 1024 * 1024;
        nodeInfo.sockets = 2;
        nodeInfo.threads = 2;
        nodeInfo.model = "Foo processor";
        Mockito.when(connect.nodeInfo()).thenReturn(nodeInfo);
        // this is testing the interface stats, returns an increasing number of sent and received bytes
        Mockito.when(domain.interfaceStats(Matchers.anyString())).thenAnswer(new org.mockito.stubbing.Answer<DomainInterfaceStats>() {
            // increment with less than a KB, so this should be less than 1 KB
            final static int increment = 1000;
            int rxBytes = 1000;
            int txBytes = 1000;

            @Override
            public DomainInterfaceStats answer(final InvocationOnMock invocation) throws Throwable {
                final DomainInterfaceStats domainInterfaceStats = new DomainInterfaceStats();
                domainInterfaceStats.rx_bytes = rxBytes += increment;
                domainInterfaceStats.tx_bytes = txBytes += increment;
                return domainInterfaceStats;

            }

        });

        Mockito.when(domain.blockStats(Matchers.anyString())).thenAnswer(new org.mockito.stubbing.Answer<DomainBlockStats>() {
            // a little less than a KB
            final static int increment = 1000;

            int rdBytes = 0;
            int wrBytes = 1024;

            @Override
            public DomainBlockStats answer(final InvocationOnMock invocation) throws Throwable {
                final DomainBlockStats domainBlockStats = new DomainBlockStats();

                domainBlockStats.rd_bytes = rdBytes += increment;
                domainBlockStats.wr_bytes = wrBytes += increment;
                return domainBlockStats;
            }

        });

        final LibvirtComputingResource libvirtComputingResource = new LibvirtComputingResource() {
            @Override
            public List<InterfaceDef> getInterfaces(final Connect conn, final String vmName) {
                final InterfaceDef interfaceDef = new InterfaceDef();
                return Arrays.asList(interfaceDef);
            }

            @Override
            public List<DiskDef> getDisks(final Connect conn, final String vmName) {
                final DiskDef diskDef = new DiskDef();
                return Arrays.asList(diskDef);
            }

        };
        libvirtComputingResource.getVmStat(connect, VMNAME);
        final VmStatsEntry vmStat = libvirtComputingResource.getVmStat(connect, VMNAME);
        // network traffic as generated by the logic above, must be greater than zero
        Assert.assertTrue(vmStat.getNetworkReadKBs() > 0);
        Assert.assertTrue(vmStat.getNetworkWriteKBs() > 0);
        // IO traffic as generated by the logic above, must be greater than zero
        Assert.assertTrue(vmStat.getDiskReadKBs() > 0);
        Assert.assertTrue(vmStat.getDiskWriteKBs() > 0);
    }

    @Test
    public void getCpuSpeed() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        final NodeInfo nodeInfo = Mockito.mock(NodeInfo.class);
        LibvirtComputingResource.getCpuSpeed(nodeInfo);
    }

    /*
     * New Tests
     */

    @Test
    public void testStopCommandNoCheck() {
        // We cannot do much here due to the Native libraries and Static methods used by the LibvirtConnection we need
        // a better way to mock stuff!

        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final StopCommand stopCommand = new StopCommand(vmName, false, false);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(stopCommand, libvirtComputingResource);

        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testStopCommandCheck() {
        // We cannot do much here due to the Native libraries and Static methods used by the LibvirtConnection we need
        // a better way to mock stuff!

        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);
        final Domain domain = Mockito.mock(Domain.class);

        final String vmName = "Test";
        final StopCommand stopCommand = new StopCommand(vmName, false, true);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByName(vmName)).thenReturn(conn);
            when(conn.domainLookupByName(stopCommand.getVmName())).thenReturn(domain);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(stopCommand, libvirtComputingResource);

        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(2)).getConnectionByName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetVmStatsCommand() {
        // We cannot do much here due to the Native libraries and Static methods used by the LibvirtConnection we need
        // a better way to mock stuff!

        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final String uuid = "e8d6b4d0-bc6d-4613-b8bb-cb9e0600f3c6";
        final List<String> vms = new ArrayList<String>();
        vms.add(vmName);

        final GetVmStatsCommand stopCommand = new GetVmStatsCommand(vms, uuid, "hostname");

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(stopCommand, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetVmDiskStatsCommand() {
        // We cannot do much here due to the Native libraries and Static methods used by the LibvirtConnection we need
        // a better way to mock stuff!

        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final String uuid = "e8d6b4d0-bc6d-4613-b8bb-cb9e0600f3c6";
        final List<String> vms = new ArrayList<String>();
        vms.add(vmName);

        final GetVmDiskStatsCommand stopCommand = new GetVmDiskStatsCommand(vms, uuid, "hostname");

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnection()).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(stopCommand, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnection();
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }
}