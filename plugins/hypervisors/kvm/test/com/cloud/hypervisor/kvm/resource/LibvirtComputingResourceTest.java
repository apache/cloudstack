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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
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
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtConnectionWrapper;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtRequestWrapper;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;

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
        final LibvirtComputingResource libvirtComputingResource = new LibvirtComputingResource();
        final Pair<Double, Double> stats = libvirtComputingResource.getNicStats("lo");
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
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final StopCommand command = new StopCommand(vmName, false, false);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testStopCommandCheck() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);
        final Domain domain = Mockito.mock(Domain.class);

        final String vmName = "Test";
        final StopCommand command = new StopCommand(vmName, false, true);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
            when(conn.domainLookupByName(command.getVmName())).thenReturn(domain);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);

        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(2)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetVmStatsCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final String uuid = "e8d6b4d0-bc6d-4613-b8bb-cb9e0600f3c6";
        final List<String> vms = new ArrayList<String>();
        vms.add(vmName);

        final GetVmStatsCommand command = new GetVmStatsCommand(vms, uuid, "hostname");

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetVmDiskStatsCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final String uuid = "e8d6b4d0-bc6d-4613-b8bb-cb9e0600f3c6";
        final List<String> vms = new ArrayList<String>();
        vms.add(vmName);

        final GetVmDiskStatsCommand command = new GetVmDiskStatsCommand(vms, uuid, "hostname");

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnection()).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnection();
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testRebootCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final RebootCommand command = new RebootCommand(vmName);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testRebootRouterCommand() {
        final VirtualRoutingResource routingResource = Mockito.mock(VirtualRoutingResource.class);
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final RebootRouterCommand command = new RebootRouterCommand(vmName, "192.168.0.10");

        when(libvirtComputingResource.getVirtRouterResource()).thenReturn(routingResource);
        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getVirtRouterResource();

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test(expected = NumberFormatException.class)
    public void testGetHostStatsCommand() {
        // A bit difficult to test due to the logger being passed and the parser itself relying on the connection.
        // Have to spend some more time afterwards in order to refactor the wrapper itself.

        final String uuid = "e8d6b4d0-bc6d-4613-b8bb-cb9e0600f3c6";
        final GetHostStatsCommand command = new GetHostStatsCommand(uuid, "summer", 1l);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());
    }

    @Test
    public void testCheckHealthCommand() {
        final CheckHealthCommand command = new CheckHealthCommand();

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());
    }

    @Test
    public void testPrepareForMigrationCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final VirtualMachineTO vm = Mockito.mock(VirtualMachineTO.class);
        final KVMStoragePoolManager storagePoolManager = Mockito.mock(KVMStoragePoolManager.class);
        final NicTO nicTO = Mockito.mock(NicTO.class);
        final DiskTO diskTO = Mockito.mock(DiskTO.class);
        final VifDriver vifDriver = Mockito.mock(VifDriver.class);

        final PrepareForMigrationCommand command = new PrepareForMigrationCommand(vm);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vm.getName())).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(vm.getNics()).thenReturn(new NicTO[]{nicTO});
        when(vm.getDisks()).thenReturn(new DiskTO[]{diskTO});

        when(nicTO.getType()).thenReturn(TrafficType.Guest);
        when(diskTO.getType()).thenReturn(Volume.Type.ISO);

        when(libvirtComputingResource.getVifDriver(nicTO.getType())).thenReturn(vifDriver);
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vm.getName());
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(vm, times(1)).getNics();
        verify(vm, times(1)).getDisks();
        verify(diskTO, times(1)).getType();
    }

    @Test(expected = UnsatisfiedLinkError.class)
    public void testMigrateCommand() {
        // The Connect constructor used inside the LibvirtMigrateCommandWrapper has a call to native methods, which
        // makes difficult to test it now.
        // Will keep it expecting the UnsatisfiedLinkError and fix later.

        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final String destIp = "10.1.1.100";
        final boolean isWindows = false;
        final VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);
        final boolean executeInSequence = false;

        final MigrateCommand command = new MigrateCommand(vmName, destIp, isWindows, vmTO, executeInSequence );

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final InterfaceDef interfaceDef = Mockito.mock(InterfaceDef.class);
        final List<InterfaceDef> ifaces = new ArrayList<InterfaceDef>();
        ifaces.add(interfaceDef);

        when(libvirtComputingResource.getInterfaces(conn, vmName)).thenReturn(ifaces);

        final DiskDef diskDef = Mockito.mock(DiskDef.class);
        final List<DiskDef> disks = new ArrayList<DiskDef>();
        disks.add(diskDef);

        when(libvirtComputingResource.getDisks(conn, vmName)).thenReturn(disks);
        final Domain dm = Mockito.mock(Domain.class);
        try {
            when(conn.domainLookupByName(vmName)).thenReturn(dm);

            when(libvirtComputingResource.getPrivateIp()).thenReturn("192.168.1.10");
            when(dm.getXMLDesc(0)).thenReturn("host_domain");
            when(dm.isPersistent()).thenReturn(1);
            doNothing().when(dm).undefine();

        } catch (final LibvirtException e) {
            fail(e.getMessage());
        } catch (final Exception e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        verify(libvirtComputingResource, times(1)).getInterfaces(conn, vmName);
        verify(libvirtComputingResource, times(1)).getDisks(conn, vmName);
        try {
            verify(conn, times(1)).domainLookupByName(vmName);
            verify(dm, times(1)).getXMLDesc(0);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testPingTestHostIpCommand() {
        final PingTestCommand command = new PingTestCommand("172.1.10.10");

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());
    }

    @Test
    public void testPingTestPvtIpCommand() {
        final PingTestCommand command = new PingTestCommand("169.17.1.10", "192.168.10.10");

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());
    }

    @Test
    public void testPingOnlyOneIpCommand() {
        final PingTestCommand command = new PingTestCommand("169.17.1.10", null);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());
    }

    @Test
    public void testCheckVirtualMachineCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final CheckVirtualMachineCommand command = new CheckVirtualMachineCommand(vmName);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(libvirtComputingResource.getVmState(conn, command.getVmName())).thenReturn(PowerState.PowerOn);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExceptionCheckVirtualMachineCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final CheckVirtualMachineCommand command = new CheckVirtualMachineCommand(vmName);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenThrow(LibvirtException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(libvirtComputingResource.getVmState(conn, command.getVmName())).thenReturn(PowerState.PowerOn);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testReadyCommand() {
        final ReadyCommand command = new ReadyCommand(1l);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());
    }

    @Test
    public void testAttachIsoCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final AttachIsoCommand command = new AttachIsoCommand(vmName, "/path", true);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAttachIsoCommandLibvirtException() {
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final AttachIsoCommand command = new AttachIsoCommand(vmName, "/path", true);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenThrow(LibvirtException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAttachIsoCommandURISyntaxException() {
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final AttachIsoCommand command = new AttachIsoCommand(vmName, "/path", true);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenThrow(URISyntaxException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAttachIsoCommandInternalErrorException() {
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final AttachIsoCommand command = new AttachIsoCommand(vmName, "/path", true);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenThrow(InternalErrorException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(vmName);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }
}