//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockJobInfo;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.libvirt.TypedParameter;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtMigrateVolumeCommandWrapperTest {

    @Spy
    @InjectMocks
    private LibvirtMigrateVolumeCommandWrapper libvirtMigrateVolumeCommandWrapper;

    @Mock
    MigrateVolumeCommand command;

    @Mock
    LibvirtComputingResource libvirtComputingResource;

    @Mock
    LibvirtUtilitiesHelper libvirtUtilitiesHelper;

    private String domxml = "<domain type='kvm' id='1'>\n" +
            "  <name>i-2-27-VM</name>\n" +
            "  <uuid>2d37fe1a-621a-4903-9ab5-5c9544c733f8</uuid>\n" +
            "  <description>Ubuntu 18.04 LTS</description>\n" +
            "  <memory unit='KiB'>524288</memory>\n" +
            "  <currentMemory unit='KiB'>524288</currentMemory>\n" +
            "  <vcpu placement='static'>1</vcpu>\n" +
            "  <cputune>\n" +
            "    <shares>256</shares>\n" +
            "  </cputune>\n" +
            "  <resource>\n" +
            "    <partition>/machine</partition>\n" +
            "  </resource>\n" +
            "  <sysinfo type='smbios'>\n" +
            "    <system>\n" +
            "      <entry name='manufacturer'>Apache Software Foundation</entry>\n" +
            "      <entry name='product'>CloudStack KVM Hypervisor</entry>\n" +
            "      <entry name='uuid'>2d37fe1a-621a-4903-9ab5-5c9544c733f8</entry>\n" +
            "    </system>\n" +
            "  </sysinfo>\n" +
            "  <os>\n" +
            "    <type arch='x86_64' machine='pc-i440fx-rhel7.6.0'>hvm</type>\n" +
            "    <boot dev='cdrom'/>\n" +
            "    <boot dev='hd'/>\n" +
            "    <smbios mode='sysinfo'/>\n" +
            "  </os>\n" +
            "  <features>\n" +
            "    <acpi/>\n" +
            "    <apic/>\n" +
            "    <pae/>\n" +
            "  </features>\n" +
            "  <cpu mode='custom' match='exact' check='full'>\n" +
            "    <model fallback='forbid'>qemu64</model>\n" +
            "    <feature policy='require' name='x2apic'/>\n" +
            "    <feature policy='require' name='hypervisor'/>\n" +
            "    <feature policy='require' name='lahf_lm'/>\n" +
            "    <feature policy='disable' name='svm'/>\n" +
            "  </cpu>\n" +
            "  <clock offset='utc'>\n" +
            "    <timer name='kvmclock'/>\n" +
            "  </clock>\n" +
            "  <on_poweroff>destroy</on_poweroff>\n" +
            "  <on_reboot>restart</on_reboot>\n" +
            "  <on_crash>destroy</on_crash>\n" +
            "  <devices>\n" +
            "    <emulator>/usr/libexec/qemu-kvm</emulator>\n" +
            "    <disk type='block' device='disk'>\n" +
            "      <driver name='qemu' type='raw' cache='none'/>\n" +
            "      <source dev='/dev/disk/by-id/emc-vol-610204d03e3ad60f-bec108c400000018' index='4'/>\n" +
            "      <backingStore/>\n" +
            "      <target dev='vda' bus='virtio'/>\n" +
            "      <serial>38a54bf719f24af6b070</serial>\n" +
            "      <alias name='virtio-disk0'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x05' function='0x0'/>\n" +
            "    </disk>\n" +
            "    <disk type='block' device='disk'>\n" +
            "      <driver name='qemu' type='raw' cache='none'/>\n" +
            "      <source dev='/dev/disk/by-id/emc-vol-7332760565f6340f-01b381820000001c' index='2'/>\n" +
            "      <backingStore/>\n" +
            "      <target dev='vdb' bus='virtio'/>\n" +
            "      <serial>0ceeb7c643b447aba5ce</serial>\n" +
            "      <alias name='virtio-disk1'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x06' function='0x0'/>\n" +
            "    </disk>\n" +
            "    <disk type='file' device='cdrom'>\n" +
            "      <driver name='qemu'/>\n" +
            "      <target dev='hdc' bus='ide'/>\n" +
            "      <readonly/>\n" +
            "      <alias name='ide0-1-0'/>\n" +
            "      <address type='drive' controller='0' bus='1' target='0' unit='0'/>\n" +
            "    </disk>\n" +
            "    <controller type='usb' index='0' model='piix3-uhci'>\n" +
            "      <alias name='usb'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x2'/>\n" +
            "    </controller>\n" +
            "    <controller type='pci' index='0' model='pci-root'>\n" +
            "      <alias name='pci.0'/>\n" +
            "    </controller>\n" +
            "    <controller type='ide' index='0'>\n" +
            "      <alias name='ide'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x01' function='0x1'/>\n" +
            "    </controller>\n" +
            "    <controller type='virtio-serial' index='0'>\n" +
            "      <alias name='virtio-serial0'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x04' function='0x0'/>\n" +
            "    </controller>\n" +
            "    <interface type='bridge'>\n" +
            "      <mac address='02:00:23:fd:00:17'/>\n" +
            "      <source bridge='breth1-1640'/>\n" +
            "      <bandwidth>\n" +
            "        <inbound average='25600' peak='25600'/>\n" +
            "        <outbound average='25600' peak='25600'/>\n" +
            "      </bandwidth>\n" +
            "      <target dev='vnet0'/>\n" +
            "      <model type='virtio'/>\n" +
            "      <link state='up'/>\n" +
            "      <alias name='net0'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x03' function='0x0'/>\n" +
            "    </interface>\n" +
            "    <serial type='pty'>\n" +
            "      <source path='/dev/pts/1'/>\n" +
            "      <target type='isa-serial' port='0'>\n" +
            "        <model name='isa-serial'/>\n" +
            "      </target>\n" +
            "      <alias name='serial0'/>\n" +
            "    </serial>\n" +
            "    <console type='pty' tty='/dev/pts/1'>\n" +
            "      <source path='/dev/pts/1'/>\n" +
            "      <target type='serial' port='0'/>\n" +
            "      <alias name='serial0'/>\n" +
            "    </console>\n" +
            "    <channel type='unix'>\n" +
            "      <source mode='bind' path='/var/lib/libvirt/qemu/i-2-27-VM.org.qemu.guest_agent.0'/>\n" +
            "      <target type='virtio' name='org.qemu.guest_agent.0' state='connected'/>\n" +
            "      <alias name='channel0'/>\n" +
            "      <address type='virtio-serial' controller='0' bus='0' port='1'/>\n" +
            "    </channel>\n" +
            "    <input type='tablet' bus='usb'>\n" +
            "      <alias name='input0'/>\n" +
            "      <address type='usb' bus='0' port='1'/>\n" +
            "    </input>\n" +
            "    <input type='mouse' bus='ps2'>\n" +
            "      <alias name='input1'/>\n" +
            "    </input>\n" +
            "    <input type='keyboard' bus='ps2'>\n" +
            "      <alias name='input2'/>\n" +
            "    </input>\n" +
            "    <graphics type='vnc' port='5900' autoport='yes' listen='10.0.32.170'>\n" +
            "      <listen type='address' address='10.0.32.170'/>\n" +
            "    </graphics>\n" +
            "    <audio id='1' type='none'/>\n" +
            "    <video>\n" +
            "      <model type='cirrus' vram='16384' heads='1' primary='yes'/>\n" +
            "      <alias name='video0'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x02' function='0x0'/>\n" +
            "    </video>\n" +
            "    <watchdog model='i6300esb' action='none'>\n" +
            "      <alias name='watchdog0'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x08' function='0x0'/>\n" +
            "    </watchdog>\n" +
            "    <memballoon model='virtio'>\n" +
            "      <alias name='balloon0'/>\n" +
            "      <address type='pci' domain='0x0000' bus='0x00' slot='0x07' function='0x0'/>\n" +
            "    </memballoon>\n" +
            "  </devices>\n" +
            "  <seclabel type='dynamic' model='dac' relabel='yes'>\n" +
            "    <label>+0:+0</label>\n" +
            "    <imagelabel>+0:+0</imagelabel>\n" +
            "  </seclabel>\n" +
            "</domain>\n";

    @Test
    public void testPowerFlexMigrateVolumeMethod() {
        VolumeObjectTO srcVolumeObjectTO = Mockito.mock(VolumeObjectTO.class);
        Mockito.doReturn(srcVolumeObjectTO).when(command).getSrcData();

        PrimaryDataStoreTO srcPrimaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.doReturn(srcPrimaryDataStore).when(srcVolumeObjectTO).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.PowerFlex).when(srcPrimaryDataStore).getPoolType();

        MigrateVolumeAnswer powerFlexAnswer = Mockito.mock(MigrateVolumeAnswer.class);
        MigrateVolumeAnswer regularVolumeAnswer = Mockito.mock(MigrateVolumeAnswer.class);

        Mockito.doReturn(true).when(powerFlexAnswer).getResult();
        Mockito.doReturn(powerFlexAnswer).when(libvirtMigrateVolumeCommandWrapper).migratePowerFlexVolume(command, libvirtComputingResource);

        Answer answer = libvirtMigrateVolumeCommandWrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
    }

    @Test
    public void testRegularMigrateVolumeMethod() {
        VolumeObjectTO srcVolumeObjectTO = Mockito.mock(VolumeObjectTO.class);
        Mockito.doReturn(srcVolumeObjectTO).when(command).getSrcData();

        PrimaryDataStoreTO srcPrimaryDataStore = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.doReturn(srcPrimaryDataStore).when(srcVolumeObjectTO).getDataStore();
        Mockito.doReturn(Storage.StoragePoolType.NetworkFilesystem).when(srcPrimaryDataStore).getPoolType();

        MigrateVolumeAnswer powerFlexAnswer = Mockito.mock(MigrateVolumeAnswer.class);
        MigrateVolumeAnswer regularVolumeAnswer = Mockito.mock(MigrateVolumeAnswer.class);

        Mockito.doReturn(false).when(regularVolumeAnswer).getResult();
        Mockito.doReturn(regularVolumeAnswer).when(libvirtMigrateVolumeCommandWrapper).migrateRegularVolume(command, libvirtComputingResource);

        Answer answer = libvirtMigrateVolumeCommandWrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
    }

    @Test
    public void testMigratePowerFlexVolume() throws LibvirtException, ParserConfigurationException, IOException, TransformerException, SAXException {
        VolumeObjectTO srcVolumeObjectTO = Mockito.mock(VolumeObjectTO.class);
        Mockito.doReturn(srcVolumeObjectTO).when(command).getSrcData();
        String srcPath = "bec108c400000018:vol-60-7acb-9e22";
        Mockito.doReturn(srcPath).when(srcVolumeObjectTO).getPath();
        String vmName = "i-2-27-VM";
        Mockito.doReturn(vmName).when(srcVolumeObjectTO).getVmName();

        VolumeObjectTO destVolumeObjectTO = Mockito.mock(VolumeObjectTO.class);
        Mockito.doReturn(destVolumeObjectTO).when(command).getDestData();
        String destPath = "01b381820000001c:vol-60-ec76-b7dc";
        Mockito.doReturn(destPath).when(destVolumeObjectTO).getPath();
        Map<String, String> destDetails = new HashMap<>();
        destDetails.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, "610204d03e3ad60f");

        Mockito.doReturn(libvirtUtilitiesHelper).when(libvirtComputingResource).getLibvirtUtilitiesHelper();
        Connect conn = Mockito.mock(Connect.class);
        Domain dm = Mockito.mock(Domain.class);
        Mockito.doReturn(conn).when(libvirtUtilitiesHelper).getConnection();
        Mockito.doReturn(dm).when(libvirtComputingResource).getDomain(conn, vmName);

        DomainInfo domainInfo = Mockito.mock(DomainInfo.class);
        domainInfo.state = DomainInfo.DomainState.VIR_DOMAIN_RUNNING;
        Mockito.doReturn(domainInfo).when(dm).getInfo();

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        Mockito.doReturn(storagePoolMgr).when(libvirtComputingResource).getStoragePoolMgr();
        PrimaryDataStoreTO spool = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.doReturn(spool).when(destVolumeObjectTO).getDataStore();
        KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        Mockito.doReturn(pool).when(storagePoolMgr).getStoragePool(Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(pool).connectPhysicalDisk(Mockito.any(), Mockito.any());

        Mockito.doReturn(null).when(destVolumeObjectTO).getPassphrase();

        Mockito.doReturn(domxml).when(dm).getXMLDesc(0);

        Mockito.doNothing().when(dm).blockCopy(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(TypedParameter[].class), ArgumentMatchers.anyInt());
        MigrateVolumeAnswer answer = new MigrateVolumeAnswer(command, true, null, destPath);
        Mockito.doReturn(answer).when(libvirtMigrateVolumeCommandWrapper).checkBlockJobStatus(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Answer migrateVolumeAnswer = libvirtMigrateVolumeCommandWrapper.migratePowerFlexVolume(command, libvirtComputingResource);

        Assert.assertTrue(migrateVolumeAnswer.getResult());
    }

    @Test
    public void testMigratePowerFlexVolumeFailure() throws LibvirtException, ParserConfigurationException, IOException, TransformerException, SAXException {
        VolumeObjectTO srcVolumeObjectTO = Mockito.mock(VolumeObjectTO.class);
        Mockito.doReturn(srcVolumeObjectTO).when(command).getSrcData();
        String srcPath = "bec108c400000018:vol-60-7acb-9e22";
        Mockito.doReturn(srcPath).when(srcVolumeObjectTO).getPath();
        String vmName = "i-2-27-VM";
        Mockito.doReturn(vmName).when(srcVolumeObjectTO).getVmName();

        VolumeObjectTO destVolumeObjectTO = Mockito.mock(VolumeObjectTO.class);
        Mockito.doReturn(destVolumeObjectTO).when(command).getDestData();
        String destPath = "01b381820000001c:vol-60-ec76-b7dc";
        Mockito.doReturn(destPath).when(destVolumeObjectTO).getPath();
        Map<String, String> destDetails = new HashMap<>();
        destDetails.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, "610204d03e3ad60f");

        Mockito.doReturn(libvirtUtilitiesHelper).when(libvirtComputingResource).getLibvirtUtilitiesHelper();
        Connect conn = Mockito.mock(Connect.class);
        Domain dm = Mockito.mock(Domain.class);
        Mockito.doReturn(conn).when(libvirtUtilitiesHelper).getConnection();
        Mockito.doReturn(dm).when(libvirtComputingResource).getDomain(conn, vmName);

        DomainInfo domainInfo = Mockito.mock(DomainInfo.class);
        domainInfo.state = DomainInfo.DomainState.VIR_DOMAIN_RUNNING;
        Mockito.doReturn(domainInfo).when(dm).getInfo();

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        Mockito.doReturn(storagePoolMgr).when(libvirtComputingResource).getStoragePoolMgr();
        PrimaryDataStoreTO spool = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.doReturn(spool).when(destVolumeObjectTO).getDataStore();
        KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        Mockito.doReturn(pool).when(storagePoolMgr).getStoragePool(Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(pool).connectPhysicalDisk(Mockito.any(), Mockito.any());

        Mockito.doReturn(null).when(destVolumeObjectTO).getPassphrase();
        Mockito.doReturn(domxml).when(dm).getXMLDesc(0);
        Mockito.doThrow(LibvirtException.class).when(dm).blockCopy(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(TypedParameter[].class), ArgumentMatchers.anyInt());

        Answer migrateVolumeAnswer = libvirtMigrateVolumeCommandWrapper.migratePowerFlexVolume(command, libvirtComputingResource);

        Assert.assertFalse(migrateVolumeAnswer.getResult());
    }

    @Test
    public void testCheckBlockJobStatus() throws LibvirtException {
        Connect conn = Mockito.mock(Connect.class);
        Domain dm = Mockito.mock(Domain.class);
        String destDiskLabel = "vda";
        String srcPath = "bec108c400000018:vol-60-7acb-9e22";
        String destPath = "01b381820000001c:vol-60-ec76-b7dc";
        Mockito.doReturn(60).when(command).getWait();
        DomainBlockJobInfo blockJobInfo = Mockito.mock(DomainBlockJobInfo.class);
        Mockito.doReturn(blockJobInfo).when(dm).getBlockJobInfo(destDiskLabel, 0);
        blockJobInfo.cur = 100;
        blockJobInfo.end = 100;

        MigrateVolumeAnswer answer = libvirtMigrateVolumeCommandWrapper.checkBlockJobStatus(command, dm, destDiskLabel, srcPath, destPath, libvirtComputingResource, conn, null);

        Assert.assertTrue(answer.getResult());
    }

    @Test
    public void testCheckBlockJobStatusFailure() throws LibvirtException {
        Connect conn = Mockito.mock(Connect.class);
        Domain dm = Mockito.mock(Domain.class);
        String destDiskLabel = "vda";
        String srcPath = "bec108c400000018:vol-60-7acb-9e22";
        String destPath = "01b381820000001c:vol-60-ec76-b7dc";
        Mockito.doReturn(1).when(command).getWait();
        DomainBlockJobInfo blockJobInfo = Mockito.mock(DomainBlockJobInfo.class);
        Mockito.doReturn(blockJobInfo).when(dm).getBlockJobInfo(destDiskLabel, 0);
        blockJobInfo.cur = 10;
        blockJobInfo.end = 100;

        MigrateVolumeAnswer answer = libvirtMigrateVolumeCommandWrapper.checkBlockJobStatus(command, dm, destDiskLabel, srcPath, destPath, libvirtComputingResource, conn, null);

        Assert.assertFalse(answer.getResult());
    }

}
