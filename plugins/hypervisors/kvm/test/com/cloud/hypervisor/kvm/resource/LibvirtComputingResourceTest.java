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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.commons.lang.SystemUtils;
import org.junit.Assume;
import org.junit.Test;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.NodeInfo;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;

public class LibvirtComputingResourceTest {

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
        int id = _random.nextInt(65534);
        String name = "test-instance-1";

        int cpus = _random.nextInt(7) + 1;
        int speed = 1024;
        int minRam = 256 * 1024;
        int maxRam = 512 * 1024;

        String os = "Ubuntu";
        boolean haEnabled = false;
        boolean limitCpuUse = false;

        String vncAddr = "";
        String vncPassword = "mySuperSecretPassword";

        LibvirtComputingResource lcr = new LibvirtComputingResource();
        VirtualMachineTO to = new VirtualMachineTO(id, name, VirtualMachine.Type.User, cpus, speed, minRam, maxRam, BootloaderType.HVM, os, false, false, vncPassword);
        to.setVncAddr(vncAddr);
        to.setUuid("b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9");

        LibvirtVMDef vm = lcr.createVMFromSpec(to);
        vm.setHvsType(_hyperVisorType);

        String vmStr = "<domain type='" + _hyperVisorType + "'>\n";
        vmStr += "<name>" + name + "</name>\n";
        vmStr += "<uuid>b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9</uuid>\n";
        vmStr += "<description>" + os + "</description>\n";
        vmStr += "<clock offset='utc'>\n";
        vmStr += "</clock>\n";
        vmStr += "<features>\n";
        vmStr += "<pae/>\n";
        vmStr += "<apic/>\n";
        vmStr += "<acpi/>\n";
        vmStr += "</features>\n";
        vmStr += "<devices>\n";
        vmStr += "<serial type='pty'>\n";
        vmStr += "<target port='0'/>\n";
        vmStr += "</serial>\n";
        vmStr += "<graphics type='vnc' autoport='yes' listen='" + vncAddr + "' passwd='" + vncPassword + "'/>\n";
        vmStr += "<console type='pty'>\n";
        vmStr += "<target port='0'/>\n";
        vmStr += "</console>\n";
        vmStr += "<input type='tablet' bus='usb'/>\n";
        vmStr += "</devices>\n";
        vmStr += "<memory>" + maxRam / 1024 + "</memory>\n";
        vmStr += "<currentMemory>" + minRam / 1024 + "</currentMemory>\n";
        vmStr += "<devices>\n";
        vmStr += "<memballoon model='virtio'/>\n";
        vmStr += "</devices>\n";
        vmStr += "<vcpu>" + cpus + "</vcpu>\n";
        vmStr += "<os>\n";
        vmStr += "<type  machine='pc'>hvm</type>\n";
        vmStr += "<boot dev='cdrom'/>\n";
        vmStr += "<boot dev='hd'/>\n";
        vmStr += "</os>\n";
        //vmStr += "<cputune>\n";
        //vmStr += "<shares>" + (cpus * speed) + "</shares>\n";
        //vmStr += "</cputune>\n";
        vmStr += "<on_reboot>restart</on_reboot>\n";
        vmStr += "<on_poweroff>destroy</on_poweroff>\n";
        vmStr += "<on_crash>destroy</on_crash>\n";
        vmStr += "</domain>\n";
        assertEquals(vmStr, vm.toString());
    }

    /**
        This test tests if the Agent can handle a vmSpec coming
        from a >4.1 management server.

        It tests if the Agent can handle a vmSpec with overcommit
        data like minSpeed and maxSpeed in there
    */
    @Test
    public void testCreateVMFromSpec() {
        int id = _random.nextInt(65534);
        String name = "test-instance-1";

        int cpus = _random.nextInt(7) + 1;
        int minSpeed = 1024;
        int maxSpeed = 2048;
        int minRam = 256 * 1024;
        int maxRam = 512 * 1024;

        String os = "Ubuntu";
        boolean haEnabled = false;
        boolean limitCpuUse = false;

        String vncAddr = "";
        String vncPassword = "mySuperSecretPassword";

        LibvirtComputingResource lcr = new LibvirtComputingResource();
        VirtualMachineTO to =
            new VirtualMachineTO(id, name, VirtualMachine.Type.User, cpus, minSpeed, maxSpeed, minRam, maxRam, BootloaderType.HVM, os, false, false, vncPassword);
        to.setVncAddr(vncAddr);
        to.setUuid("b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9");

        LibvirtVMDef vm = lcr.createVMFromSpec(to);
        vm.setHvsType(_hyperVisorType);

        String vmStr = "<domain type='" + _hyperVisorType + "'>\n";
        vmStr += "<name>" + name + "</name>\n";
        vmStr += "<uuid>b0f0a72d-7efb-3cad-a8ff-70ebf30b3af9</uuid>\n";
        vmStr += "<description>" + os + "</description>\n";
        vmStr += "<clock offset='utc'>\n";
        vmStr += "</clock>\n";
        vmStr += "<features>\n";
        vmStr += "<pae/>\n";
        vmStr += "<apic/>\n";
        vmStr += "<acpi/>\n";
        vmStr += "</features>\n";
        vmStr += "<devices>\n";
        vmStr += "<serial type='pty'>\n";
        vmStr += "<target port='0'/>\n";
        vmStr += "</serial>\n";
        vmStr += "<graphics type='vnc' autoport='yes' listen='" + vncAddr + "' passwd='" + vncPassword + "'/>\n";
        vmStr += "<console type='pty'>\n";
        vmStr += "<target port='0'/>\n";
        vmStr += "</console>\n";
        vmStr += "<input type='tablet' bus='usb'/>\n";
        vmStr += "</devices>\n";
        vmStr += "<memory>" + maxRam / 1024 + "</memory>\n";
        vmStr += "<currentMemory>" + minRam / 1024 + "</currentMemory>\n";
        vmStr += "<devices>\n";
        vmStr += "<memballoon model='virtio'/>\n";
        vmStr += "</devices>\n";
        vmStr += "<vcpu>" + cpus + "</vcpu>\n";
        vmStr += "<os>\n";
        vmStr += "<type  machine='pc'>hvm</type>\n";
        vmStr += "<boot dev='cdrom'/>\n";
        vmStr += "<boot dev='hd'/>\n";
        vmStr += "</os>\n";
        //vmStr += "<cputune>\n";
        //vmStr += "<shares>" + (cpus * minSpeed) + "</shares>\n";
        //vmStr += "</cputune>\n";
        vmStr += "<on_reboot>restart</on_reboot>\n";
        vmStr += "<on_poweroff>destroy</on_poweroff>\n";
        vmStr += "<on_crash>destroy</on_crash>\n";
        vmStr += "</domain>\n";

        assertEquals(vmStr, vm.toString());
    }

    @Test
    public void testGetNicStats() {
        //this test is only working on linux because of the loopback interface name
        //also the tested code seems to work only on linux
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Pair<Double, Double> stats = LibvirtComputingResource.getNicStats("lo");
        assertNotNull(stats);
    }

    @Test
    public void testUUID() {
        String uuid = "1";
        LibvirtComputingResource lcr = new LibvirtComputingResource();
        uuid = lcr.getUuid(uuid);
        Assert.assertTrue(!uuid.equals("1"));

        String oldUuid = UUID.randomUUID().toString();
        uuid = oldUuid;
        uuid = lcr.getUuid(uuid);
        Assert.assertTrue(uuid.equals(oldUuid));
    }

    private static final String VMNAME = "test";

    @Test
    public void testGetVmStat() throws LibvirtException {
        Connect connect = Mockito.mock(Connect.class);
        Domain domain = Mockito.mock(Domain.class);
        DomainInfo domainInfo = new DomainInfo();
        Mockito.when(domain.getInfo()).thenReturn(domainInfo);
        Mockito.when(connect.domainLookupByName(VMNAME)).thenReturn(domain);
        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.cpus = 8;
        nodeInfo.memory = 8 * 1024 * 1024;
        nodeInfo.sockets = 2;
        nodeInfo.threads = 2;
        nodeInfo.model = "Foo processor";
        Mockito.when(connect.nodeInfo()).thenReturn(nodeInfo);
        // this is testing the interface stats, returns an increasing number of sent and received bytes
        Mockito.when(domain.interfaceStats(Matchers.anyString())).thenAnswer(new Answer<DomainInterfaceStats>() {
            // increment with less than a KB, so this should be less than 1 KB
            final static int increment = 1000;
            int rx_bytes = 1000;
            int tx_bytes = 1000;

            @Override
            public DomainInterfaceStats answer(InvocationOnMock invocation) throws Throwable {
                DomainInterfaceStats domainInterfaceStats = new DomainInterfaceStats();
                domainInterfaceStats.rx_bytes = (this.rx_bytes += increment);
                domainInterfaceStats.tx_bytes = (this.tx_bytes += increment);
                return domainInterfaceStats;

            }

        });

        Mockito.when(domain.blockStats(Matchers.anyString())).thenAnswer(new Answer<DomainBlockStats>() {
            // a little less than a KB
            final static int increment = 1000;

            int rd_bytes = 0;
            int wr_bytes = 1024;

            @Override
            public DomainBlockStats answer(InvocationOnMock invocation) throws Throwable {
                DomainBlockStats domainBlockStats = new DomainBlockStats();

                domainBlockStats.rd_bytes = (rd_bytes += increment);
                domainBlockStats.wr_bytes = (wr_bytes += increment);
                return domainBlockStats;
            }

        });

        LibvirtComputingResource libvirtComputingResource = new LibvirtComputingResource() {
            @Override
            protected List<InterfaceDef> getInterfaces(Connect conn, String vmName) {
                InterfaceDef interfaceDef = new InterfaceDef();
                return Arrays.asList(interfaceDef);
            }

            @Override
            public List<DiskDef> getDisks(Connect conn, String vmName) {
                DiskDef diskDef = new DiskDef();
                return Arrays.asList(diskDef);
            }

        };
        libvirtComputingResource.getVmStat(connect, VMNAME);
        VmStatsEntry vmStat = libvirtComputingResource.getVmStat(connect, VMNAME);
        // network traffic as generated by the logic above, must be greater than zero
        Assert.assertTrue(vmStat.getNetworkReadKBs() > 0);
        Assert.assertTrue(vmStat.getNetworkWriteKBs() > 0);
        // IO traffic as generated by the logic above, must be greater than zero
        Assert.assertTrue(vmStat.getDiskReadKBs() > 0);
        Assert.assertTrue(vmStat.getDiskWriteKBs() > 0);
    }
}
