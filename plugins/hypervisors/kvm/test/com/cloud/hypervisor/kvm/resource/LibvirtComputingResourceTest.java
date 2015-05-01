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
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.OvsDestroyBridgeCommand;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.agent.api.OvsSetupBridgeCommand;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand.Host;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand.Tier;
import com.cloud.agent.api.OvsVpcPhysicalTopologyConfigCommand.Vm;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand;
import com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand.Acl;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtConnectionWrapper;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtRequestWrapper;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
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

    @SuppressWarnings("unchecked")
    @Test
    public void testGetVmDiskStatsCommandException() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final String uuid = "e8d6b4d0-bc6d-4613-b8bb-cb9e0600f3c6";
        final List<String> vms = new ArrayList<String>();
        vms.add(vmName);

        final GetVmDiskStatsCommand command = new GetVmDiskStatsCommand(vms, uuid, "hostname");

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnection()).thenThrow(LibvirtException.class);
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

    @Test
    public void testRebootRouterCommandConnect() {
        final VirtualRoutingResource routingResource = Mockito.mock(VirtualRoutingResource.class);
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final String vmName = "Test";
        final RebootRouterCommand command = new RebootRouterCommand(vmName, "192.168.0.10");

        when(libvirtComputingResource.getVirtRouterResource()).thenReturn(routingResource);
        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        when(routingResource.connect(command.getPrivateIpAddress())).thenReturn(true);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

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

    @Test
    public void testPrepareForMigrationCommandMigration() {
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
        when(storagePoolManager.connectPhysicalDisksViaVmSpec(vm)).thenReturn(true);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

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

    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareForMigrationCommandLibvirtException() {
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final VirtualMachineTO vm = Mockito.mock(VirtualMachineTO.class);
        final KVMStoragePoolManager storagePoolManager = Mockito.mock(KVMStoragePoolManager.class);
        final NicTO nicTO = Mockito.mock(NicTO.class);
        final VifDriver vifDriver = Mockito.mock(VifDriver.class);

        final PrepareForMigrationCommand command = new PrepareForMigrationCommand(vm);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vm.getName())).thenThrow(LibvirtException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(vm.getNics()).thenReturn(new NicTO[]{nicTO});
        when(nicTO.getType()).thenReturn(TrafficType.Guest);

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
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareForMigrationCommandURISyntaxException() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final VirtualMachineTO vm = Mockito.mock(VirtualMachineTO.class);
        final KVMStoragePoolManager storagePoolManager = Mockito.mock(KVMStoragePoolManager.class);
        final NicTO nicTO = Mockito.mock(NicTO.class);
        final DiskTO volume = Mockito.mock(DiskTO.class);
        final VifDriver vifDriver = Mockito.mock(VifDriver.class);

        final PrepareForMigrationCommand command = new PrepareForMigrationCommand(vm);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vm.getName())).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(vm.getNics()).thenReturn(new NicTO[]{nicTO});
        when(vm.getDisks()).thenReturn(new DiskTO[]{volume});

        when(nicTO.getType()).thenReturn(TrafficType.Guest);
        when(volume.getType()).thenReturn(Volume.Type.ISO);

        when(libvirtComputingResource.getVifDriver(nicTO.getType())).thenReturn(vifDriver);
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        try {
            when(libvirtComputingResource.getVolumePath(conn, volume)).thenThrow(URISyntaxException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        } catch (final URISyntaxException e) {
            fail(e.getMessage());
        }

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
        verify(volume, times(1)).getType();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPrepareForMigrationCommandInternalErrorException() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final VirtualMachineTO vm = Mockito.mock(VirtualMachineTO.class);
        final KVMStoragePoolManager storagePoolManager = Mockito.mock(KVMStoragePoolManager.class);
        final NicTO nicTO = Mockito.mock(NicTO.class);
        final DiskTO volume = Mockito.mock(DiskTO.class);

        final PrepareForMigrationCommand command = new PrepareForMigrationCommand(vm);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vm.getName())).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(vm.getNics()).thenReturn(new NicTO[]{nicTO});
        when(nicTO.getType()).thenReturn(TrafficType.Guest);

        when(libvirtComputingResource.getVifDriver(nicTO.getType())).thenThrow(InternalErrorException.class);
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        try {
            when(libvirtComputingResource.getVolumePath(conn, volume)).thenReturn("/path");
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        } catch (final URISyntaxException e) {
            fail(e.getMessage());
        }

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

    @Test
    public void testAttachVolumeCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final boolean attach = true;
        final boolean managed = true;
        final String vmName = "Test";
        final StoragePoolType poolType = StoragePoolType.ISO;
        final String volumePath = "/path";
        final String volumeName = "volume";
        final Long volumeSize = 200l;
        final Long deviceId = 1l;
        final String chainInfo = "none";
        final AttachVolumeCommand command = new AttachVolumeCommand(attach, managed, vmName, poolType, volumePath, volumeName, volumeSize, deviceId, chainInfo);

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk disk = Mockito.mock(KVMPhysicalDisk.class);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(command.getPooltype(), command.getPoolUuid())).thenReturn(primary);
        when(primary.getPhysicalDisk(command.getVolumePath())).thenReturn(disk);

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
    public void testAttachVolumeCommandLibvirtException() {
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final boolean attach = true;
        final boolean managed = true;
        final String vmName = "Test";
        final StoragePoolType poolType = StoragePoolType.ISO;
        final String volumePath = "/path";
        final String volumeName = "volume";
        final Long volumeSize = 200l;
        final Long deviceId = 1l;
        final String chainInfo = "none";
        final AttachVolumeCommand command = new AttachVolumeCommand(attach, managed, vmName, poolType, volumePath, volumeName, volumeSize, deviceId, chainInfo);

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk disk = Mockito.mock(KVMPhysicalDisk.class);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenThrow(LibvirtException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(command.getPooltype(), command.getPoolUuid())).thenReturn(primary);
        when(primary.getPhysicalDisk(command.getVolumePath())).thenReturn(disk);

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
    public void testAttachVolumeCommandInternalErrorException() {
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final boolean attach = true;
        final boolean managed = true;
        final String vmName = "Test";
        final StoragePoolType poolType = StoragePoolType.ISO;
        final String volumePath = "/path";
        final String volumeName = "volume";
        final Long volumeSize = 200l;
        final Long deviceId = 1l;
        final String chainInfo = "none";
        final AttachVolumeCommand command = new AttachVolumeCommand(attach, managed, vmName, poolType, volumePath, volumeName, volumeSize, deviceId, chainInfo);

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk disk = Mockito.mock(KVMPhysicalDisk.class);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(vmName)).thenThrow(InternalErrorException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(command.getPooltype(), command.getPoolUuid())).thenReturn(primary);
        when(primary.getPhysicalDisk(command.getVolumePath())).thenReturn(disk);

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
    public void testWatchConsoleProxyLoadCommand() {
        final int interval = 0;
        final long proxyVmId = 0l;
        final String proxyVmName = "host";
        final String proxyManagementIp = "169.172.15.16";
        final int proxyCmdPort = 0;

        final WatchConsoleProxyLoadCommand command = new WatchConsoleProxyLoadCommand(interval, proxyVmId, proxyVmName, proxyManagementIp, proxyCmdPort);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());
    }

    @Test
    public void testCheckConsoleProxyLoadCommand() {
        final long proxyVmId = 0l;
        final String proxyVmName = "host";
        final String proxyManagementIp = "169.172.15.16";
        final int proxyCmdPort = 0;

        final CheckConsoleProxyLoadCommand command = new CheckConsoleProxyLoadCommand(proxyVmId, proxyVmName, proxyManagementIp, proxyCmdPort);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());
    }

    @Test
    public void testGetVncPortCommand() {
        final Connect conn = Mockito.mock(Connect.class);
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final GetVncPortCommand command = new GetVncPortCommand(1l, "host");

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(command.getName())).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(command.getName());
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetVncPortCommandLibvirtException() {
        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        final GetVncPortCommand command = new GetVncPortCommand(1l, "host");

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(command.getName())).thenThrow(LibvirtException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(command.getName());
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testModifySshKeysCommand() {
        final ModifySshKeysCommand command = new ModifySshKeysCommand("", "");

        when(libvirtComputingResource.getTimeout()).thenReturn(0);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getTimeout();
    }

    @Test
    public void testMaintainCommand() {
        final MaintainCommand command = new MaintainCommand();

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateCommandNoTemplate() {
        final DiskProfile diskCharacteristics = Mockito.mock(DiskProfile.class);
        final StorageFilerTO pool = Mockito.mock(StorageFilerTO.class);
        final boolean executeInSequence = false;

        final CreateCommand command = new CreateCommand(diskCharacteristics, pool, executeInSequence );

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk vol = Mockito.mock(KVMPhysicalDisk.class);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(pool.getType(), pool.getUuid())).thenReturn(primary);

        when(primary.createPhysicalDisk(diskCharacteristics.getPath(), diskCharacteristics.getProvisioningType(), diskCharacteristics.getSize())).thenReturn(vol);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(poolManager, times(1)).getStoragePool(pool.getType(), pool.getUuid());
    }

    @Test
    public void testCreateCommand() {
        final DiskProfile diskCharacteristics = Mockito.mock(DiskProfile.class);
        final StorageFilerTO pool = Mockito.mock(StorageFilerTO.class);
        final String templateUrl = "http://template";
        final boolean executeInSequence = false;

        final CreateCommand command = new CreateCommand(diskCharacteristics, templateUrl, pool, executeInSequence );

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk vol = Mockito.mock(KVMPhysicalDisk.class);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(pool.getType(), pool.getUuid())).thenReturn(primary);

        when(primary.getType()).thenReturn(StoragePoolType.CLVM);
        when(libvirtComputingResource.templateToPrimaryDownload(command.getTemplateUrl(), primary, diskCharacteristics.getPath())).thenReturn(vol);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(poolManager, times(1)).getStoragePool(pool.getType(), pool.getUuid());
    }

    @Test
    public void testCreateCommandCLVM() {
        final DiskProfile diskCharacteristics = Mockito.mock(DiskProfile.class);
        final StorageFilerTO pool = Mockito.mock(StorageFilerTO.class);
        final String templateUrl = "http://template";
        final boolean executeInSequence = false;

        final CreateCommand command = new CreateCommand(diskCharacteristics, templateUrl, pool, executeInSequence );

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk vol = Mockito.mock(KVMPhysicalDisk.class);
        final KVMPhysicalDisk baseVol = Mockito.mock(KVMPhysicalDisk.class);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(pool.getType(), pool.getUuid())).thenReturn(primary);

        when(primary.getPhysicalDisk(command.getTemplateUrl())).thenReturn(baseVol);
        when(poolManager.createDiskFromTemplate(baseVol,
                diskCharacteristics.getPath(), diskCharacteristics.getProvisioningType(), primary, 0)).thenReturn(vol);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(poolManager, times(1)).getStoragePool(pool.getType(), pool.getUuid());
    }

    @Test
    public void testDestroyCommand() {
        final StoragePool pool = Mockito.mock(StoragePool.class);
        final Volume volume = Mockito.mock(Volume.class);
        final String vmName = "Test";

        final DestroyCommand command = new DestroyCommand(pool, volume, vmName);

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);

        final VolumeTO vol = command.getVolume();

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(vol.getPoolType(), vol.getPoolUuid())).thenReturn(primary);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(poolManager, times(1)).getStoragePool(vol.getPoolType(), vol.getPoolUuid());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDestroyCommandError() {
        final StoragePool pool = Mockito.mock(StoragePool.class);
        final Volume volume = Mockito.mock(Volume.class);
        final String vmName = "Test";

        final DestroyCommand command = new DestroyCommand(pool, volume, vmName);

        final KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primary = Mockito.mock(KVMStoragePool.class);

        final VolumeTO vol = command.getVolume();

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(poolManager);
        when(poolManager.getStoragePool(vol.getPoolType(), vol.getPoolUuid())).thenReturn(primary);

        when(primary.deletePhysicalDisk(vol.getPath(), null)).thenThrow(CloudRuntimeException.class);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(poolManager, times(1)).getStoragePool(vol.getPoolType(), vol.getPoolUuid());
    }

    @Test(expected = NullPointerException.class)
    public void testPrimaryStorageDownloadCommandNOTemplateDisk() {
        final StoragePool pool = Mockito.mock(StoragePool.class);

        final List<KVMPhysicalDisk> disks = new ArrayList<KVMPhysicalDisk>();

        final String name = "Test";
        final String url = "http://template/";
        final ImageFormat format = ImageFormat.QCOW2;
        final long accountId = 1l;
        final int wait = 0;
        final PrimaryStorageDownloadCommand command = new PrimaryStorageDownloadCommand(name, url, format, accountId, pool, wait);

        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMStoragePool secondaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk tmplVol = Mockito.mock(KVMPhysicalDisk.class);
        final KVMPhysicalDisk primaryVol = Mockito.mock(KVMPhysicalDisk.class);

        final KVMPhysicalDisk disk = new KVMPhysicalDisk("/path", "disk.qcow2", primaryPool);
        disks.add(disk);

        final int index = url.lastIndexOf("/");
        final String mountpoint = url.substring(0, index);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePoolByURI(mountpoint)).thenReturn(secondaryPool);
        when(secondaryPool.listPhysicalDisks()).thenReturn(disks);
        when(storagePoolMgr.getStoragePool(command.getPool().getType(), command.getPoolUuid())).thenReturn(primaryPool);
        when(storagePoolMgr.copyPhysicalDisk(tmplVol, UUID.randomUUID().toString(), primaryPool, 0)).thenReturn(primaryVol);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
    }

    @Test
    public void testPrimaryStorageDownloadCommandNOTemplateNODisk() {
        final StoragePool pool = Mockito.mock(StoragePool.class);

        final List<KVMPhysicalDisk> disks = new ArrayList<KVMPhysicalDisk>();

        final String name = "Test";
        final String url = "http://template/";
        final ImageFormat format = ImageFormat.QCOW2;
        final long accountId = 1l;
        final int wait = 0;
        final PrimaryStorageDownloadCommand command = new PrimaryStorageDownloadCommand(name, url, format, accountId, pool, wait);

        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMStoragePool secondaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk tmplVol = Mockito.mock(KVMPhysicalDisk.class);
        final KVMPhysicalDisk primaryVol = Mockito.mock(KVMPhysicalDisk.class);

        final int index = url.lastIndexOf("/");
        final String mountpoint = url.substring(0, index);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePoolByURI(mountpoint)).thenReturn(secondaryPool);
        when(secondaryPool.listPhysicalDisks()).thenReturn(disks);
        when(storagePoolMgr.getStoragePool(command.getPool().getType(), command.getPoolUuid())).thenReturn(primaryPool);
        when(storagePoolMgr.copyPhysicalDisk(tmplVol, UUID.randomUUID().toString(), primaryPool, 0)).thenReturn(primaryVol);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
    }

    @Test
    public void testPrimaryStorageDownloadCommandNOTemplateNOQcow2() {
        final StoragePool pool = Mockito.mock(StoragePool.class);

        final List<KVMPhysicalDisk> disks = new ArrayList<KVMPhysicalDisk>();
        final List<KVMPhysicalDisk> spiedDisks = Mockito.spy(disks);

        final String name = "Test";
        final String url = "http://template/";
        final ImageFormat format = ImageFormat.QCOW2;
        final long accountId = 1l;
        final int wait = 0;
        final PrimaryStorageDownloadCommand command = new PrimaryStorageDownloadCommand(name, url, format, accountId, pool, wait);

        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMStoragePool secondaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk tmplVol = Mockito.mock(KVMPhysicalDisk.class);
        final KVMPhysicalDisk primaryVol = Mockito.mock(KVMPhysicalDisk.class);

        final int index = url.lastIndexOf("/");
        final String mountpoint = url.substring(0, index);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePoolByURI(mountpoint)).thenReturn(secondaryPool);
        when(secondaryPool.listPhysicalDisks()).thenReturn(spiedDisks);
        when(spiedDisks.isEmpty()).thenReturn(false);

        when(storagePoolMgr.getStoragePool(command.getPool().getType(), command.getPoolUuid())).thenReturn(primaryPool);
        when(storagePoolMgr.copyPhysicalDisk(tmplVol, UUID.randomUUID().toString(), primaryPool, 0)).thenReturn(primaryVol);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
    }

    @Test(expected = NullPointerException.class)
    public void testPrimaryStorageDownloadCommandTemplateNoDisk() {
        final StoragePool pool = Mockito.mock(StoragePool.class);

        final String name = "Test";
        final String url = "http://template/template.qcow2";
        final ImageFormat format = ImageFormat.VHD;
        final long accountId = 1l;
        final int wait = 0;
        final PrimaryStorageDownloadCommand command = new PrimaryStorageDownloadCommand(name, url, format, accountId, pool, wait);

        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool primaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMStoragePool secondaryPool = Mockito.mock(KVMStoragePool.class);
        final KVMPhysicalDisk tmplVol = Mockito.mock(KVMPhysicalDisk.class);
        final KVMPhysicalDisk primaryVol = Mockito.mock(KVMPhysicalDisk.class);

        final int index = url.lastIndexOf("/");
        final String mountpoint = url.substring(0, index);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePoolByURI(mountpoint)).thenReturn(secondaryPool);
        when(secondaryPool.getPhysicalDisk("template.qcow2")).thenReturn(tmplVol);
        when(storagePoolMgr.getStoragePool(command.getPool().getType(), command.getPoolUuid())).thenReturn(primaryPool);
        when(storagePoolMgr.copyPhysicalDisk(tmplVol, UUID.randomUUID().toString(), primaryPool, 0)).thenReturn(primaryVol);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(storagePoolMgr, times(1)).getStoragePool(command.getPool().getType(), command.getPoolUuid());
    }

    @Test
    public void testGetStorageStatsCommand() {
        final DataStoreTO store = Mockito.mock(DataStoreTO.class);
        final GetStorageStatsCommand command = new GetStorageStatsCommand(store );

        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool secondaryPool = Mockito.mock(KVMStoragePool.class);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePool(command.getPooltype(), command.getStorageId(), true)).thenReturn(secondaryPool);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(storagePoolMgr, times(1)).getStoragePool(command.getPooltype(), command.getStorageId(), true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetStorageStatsCommandException() {
        final DataStoreTO store = Mockito.mock(DataStoreTO.class);
        final GetStorageStatsCommand command = new GetStorageStatsCommand(store );

        when(libvirtComputingResource.getStoragePoolMgr()).thenThrow(CloudRuntimeException.class);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
    }

    @Test
    public void testUpgradeSnapshotCommand() {
        final StoragePool pool = null;
        final String secondaryStoragePoolURL = null;
        final Long dcId = null;
        final Long accountId = null;
        final Long volumeId = null;
        final Long templateId = null;
        final Long tmpltAccountId = null;
        final String volumePath = null;
        final String snapshotUuid = null;
        final String snapshotName = null;
        final String version = null;

        final UpgradeSnapshotCommand command = new UpgradeSnapshotCommand(pool, secondaryStoragePoolURL, dcId, accountId, volumeId, templateId, tmpltAccountId, volumePath, snapshotUuid, snapshotName, version);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteStoragePoolCommand() {
        final StoragePool storagePool = Mockito.mock(StoragePool.class);
        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);

        final DeleteStoragePoolCommand command = new DeleteStoragePoolCommand(storagePool);

        final StorageFilerTO pool = command.getPool();
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.deleteStoragePool(pool.getType(), pool.getUuid())).thenReturn(true);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(storagePoolMgr, times(1)).deleteStoragePool(pool.getType(), pool.getUuid());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteStoragePoolCommandException() {
        final StoragePool storagePool = Mockito.mock(StoragePool.class);
        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);

        final DeleteStoragePoolCommand command = new DeleteStoragePoolCommand(storagePool);

        final StorageFilerTO pool = command.getPool();
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.deleteStoragePool(pool.getType(), pool.getUuid())).thenThrow(CloudRuntimeException.class);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(storagePoolMgr, times(1)).deleteStoragePool(pool.getType(), pool.getUuid());
    }

    @Test
    public void testOvsSetupBridgeCommand() {
        final String name = "Test";
        final Long hostId = 1l;
        final Long networkId = 1l;

        final OvsSetupBridgeCommand command = new OvsSetupBridgeCommand(name, hostId, networkId);

        when(libvirtComputingResource.findOrCreateTunnelNetwork(command.getBridgeName())).thenReturn(true);
        when(libvirtComputingResource.configureTunnelNetwork(command.getNetworkId(), command.getHostId(),
                command.getBridgeName())).thenReturn(true);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).findOrCreateTunnelNetwork(command.getBridgeName());
        verify(libvirtComputingResource, times(1)).configureTunnelNetwork(command.getNetworkId(), command.getHostId(),
                command.getBridgeName());
    }

    @Test
    public void testOvsSetupBridgeCommandFailure() {
        final String name = "Test";
        final Long hostId = 1l;
        final Long networkId = 1l;

        final OvsSetupBridgeCommand command = new OvsSetupBridgeCommand(name, hostId, networkId);

        when(libvirtComputingResource.findOrCreateTunnelNetwork(command.getBridgeName())).thenReturn(true);
        when(libvirtComputingResource.configureTunnelNetwork(command.getNetworkId(), command.getHostId(),
                command.getBridgeName())).thenReturn(false);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).findOrCreateTunnelNetwork(command.getBridgeName());
        verify(libvirtComputingResource, times(1)).configureTunnelNetwork(command.getNetworkId(), command.getHostId(),
                command.getBridgeName());
    }

    @Test
    public void testOvsDestroyBridgeCommand() {
        final String name = "Test";
        final Long hostId = 1l;
        final Long networkId = 1l;

        final OvsDestroyBridgeCommand command = new OvsDestroyBridgeCommand(networkId, name, hostId);

        when(libvirtComputingResource.destroyTunnelNetwork(command.getBridgeName())).thenReturn(true);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).destroyTunnelNetwork(command.getBridgeName());
    }

    @Test
    public void testOvsDestroyBridgeCommandFailure() {
        final String name = "Test";
        final Long hostId = 1l;
        final Long networkId = 1l;

        final OvsDestroyBridgeCommand command = new OvsDestroyBridgeCommand(networkId, name, hostId);

        when(libvirtComputingResource.destroyTunnelNetwork(command.getBridgeName())).thenReturn(false);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).destroyTunnelNetwork(command.getBridgeName());
    }

    @Test
    public void testOvsFetchInterfaceCommand() {
        final String label = "eth0";

        final OvsFetchInterfaceCommand command = new OvsFetchInterfaceCommand(label);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());
    }

    @Test
    public void testOvsVpcPhysicalTopologyConfigCommand() {
        final Host[] hosts = null;
        final Tier[] tiers = null;
        final Vm[] vms = null;
        final String cidr = null;

        final OvsVpcPhysicalTopologyConfigCommand command = new OvsVpcPhysicalTopologyConfigCommand(hosts, tiers, vms, cidr);

        when(libvirtComputingResource.getOvsTunnelPath()).thenReturn("/path");
        when(libvirtComputingResource.getTimeout()).thenReturn(0);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getOvsTunnelPath();
        verify(libvirtComputingResource, times(1)).getTimeout();
    }

    @Test
    public void testOvsVpcRoutingPolicyConfigCommand() {
        final String id = null;
        final String cidr = null;
        final Acl[] acls = null;
        final com.cloud.agent.api.OvsVpcRoutingPolicyConfigCommand.Tier[] tiers = null;

        final OvsVpcRoutingPolicyConfigCommand command = new OvsVpcRoutingPolicyConfigCommand(id, cidr, acls, tiers);

        when(libvirtComputingResource.getOvsTunnelPath()).thenReturn("/path");
        when(libvirtComputingResource.getTimeout()).thenReturn(0);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getOvsTunnelPath();
        verify(libvirtComputingResource, times(1)).getTimeout();
    }

    @Test
    public void testCreateStoragePoolCommand() {
        final StoragePool pool = Mockito.mock(StoragePool.class);
        final CreateStoragePoolCommand command = new CreateStoragePoolCommand(true, pool);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());
    }

    @Test
    public void testModifyStoragePoolCommand() {
        final StoragePool pool = Mockito.mock(StoragePool.class);;
        final ModifyStoragePoolCommand command = new ModifyStoragePoolCommand(true, pool);

        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        final KVMStoragePool kvmStoragePool = Mockito.mock(KVMStoragePool.class);


        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.createStoragePool(command.getPool().getUuid(), command.getPool().getHost(), command.getPool().getPort(), command.getPool().getPath(), command.getPool()
                .getUserInfo(), command.getPool().getType())).thenReturn(kvmStoragePool);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(storagePoolMgr, times(1)).createStoragePool(command.getPool().getUuid(), command.getPool().getHost(), command.getPool().getPort(), command.getPool().getPath(), command.getPool()
                .getUserInfo(), command.getPool().getType());
    }

    @Test
    public void testModifyStoragePoolCommandFailure() {
        final StoragePool pool = Mockito.mock(StoragePool.class);;
        final ModifyStoragePoolCommand command = new ModifyStoragePoolCommand(true, pool);

        final KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);

        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.createStoragePool(command.getPool().getUuid(), command.getPool().getHost(), command.getPool().getPort(), command.getPool().getPath(), command.getPool()
                .getUserInfo(), command.getPool().getType())).thenReturn(null);


        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        verify(libvirtComputingResource, times(1)).getStoragePoolMgr();
        verify(storagePoolMgr, times(1)).createStoragePool(command.getPool().getUuid(), command.getPool().getHost(), command.getPool().getPort(), command.getPool().getPath(), command.getPool()
                .getUserInfo(), command.getPool().getType());
    }

    @Test
    public void testCleanupNetworkRulesCmd() {
        final CleanupNetworkRulesCmd command = new CleanupNetworkRulesCmd(1);

        when(libvirtComputingResource.cleanupRules()).thenReturn(true);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        verify(libvirtComputingResource, times(1)).cleanupRules();
    }

    @Test
    public void testNetworkRulesVmSecondaryIpCommand() {
        final String vmName = "Test";
        final String vmMac = "00:00:00:00";
        final String secondaryIp = "172.168.25.25";
        final boolean action = true;

        final NetworkRulesVmSecondaryIpCommand command = new NetworkRulesVmSecondaryIpCommand(vmName, vmMac, secondaryIp, action );

        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);
        final Connect conn = Mockito.mock(Connect.class);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(command.getVmName())).thenReturn(conn);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
        when(libvirtComputingResource.configureNetworkRulesVMSecondaryIP(conn, command.getVmName(), command.getVmSecIp(), command.getAction())).thenReturn(true);

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertTrue(answer.getResult());

        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(command.getVmName());
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
        verify(libvirtComputingResource, times(1)).configureNetworkRulesVMSecondaryIP(conn, command.getVmName(), command.getVmSecIp(), command.getAction());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNetworkRulesVmSecondaryIpCommandFailure() {
        final String vmName = "Test";
        final String vmMac = "00:00:00:00";
        final String secondaryIp = "172.168.25.25";
        final boolean action = true;

        final NetworkRulesVmSecondaryIpCommand command = new NetworkRulesVmSecondaryIpCommand(vmName, vmMac, secondaryIp, action );

        final LibvirtConnectionWrapper libvirtConnectionWrapper = Mockito.mock(LibvirtConnectionWrapper.class);

        when(libvirtComputingResource.getLibvirtConnectionWrapper()).thenReturn(libvirtConnectionWrapper);
        try {
            when(libvirtConnectionWrapper.getConnectionByVmName(command.getVmName())).thenThrow(LibvirtException.class);
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResource);
        assertFalse(answer.getResult());

        try {
            verify(libvirtConnectionWrapper, times(1)).getConnectionByVmName(command.getVmName());
        } catch (final LibvirtException e) {
            fail(e.getMessage());
        }
        verify(libvirtComputingResource, times(1)).getLibvirtConnectionWrapper();
    }
}