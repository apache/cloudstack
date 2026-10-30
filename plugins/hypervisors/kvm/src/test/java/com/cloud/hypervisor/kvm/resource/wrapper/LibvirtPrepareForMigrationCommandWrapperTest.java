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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.DpdkTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtPrepareForMigrationCommandWrapperTest {

    @Mock
    LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    PrepareForMigrationCommand prepareForMigrationCommandMock;

    @Mock
    VirtualMachineTO virtualMachineTOMock;

    @Spy
    LibvirtPrepareForMigrationCommandWrapper libvirtPrepareForMigrationCommandWrapperSpy = new LibvirtPrepareForMigrationCommandWrapper();

    @Test
    public void createPrepareForMigrationAnswerTestDpdkInterfaceNotEmptyShouldSetParamOnAnswer() {
        Map<String, DpdkTO> dpdkInterfaceMapping = new HashMap<>();
        dpdkInterfaceMapping.put("Interface", new DpdkTO());

        PrepareForMigrationAnswer prepareForMigrationAnswer = libvirtPrepareForMigrationCommandWrapperSpy.createPrepareForMigrationAnswer(prepareForMigrationCommandMock, dpdkInterfaceMapping, libvirtComputingResourceMock,
                virtualMachineTOMock);

        Assert.assertEquals(prepareForMigrationAnswer.getDpdkInterfaceMapping(), dpdkInterfaceMapping);
    }

    @Test
    public void createPrepareForMigrationAnswerTestVerifyThatCpuSharesIsSet() {
        int cpuShares = 1000;
        Mockito.doReturn(cpuShares).when(libvirtComputingResourceMock).calculateCpuShares(virtualMachineTOMock);
        PrepareForMigrationAnswer prepareForMigrationAnswer = libvirtPrepareForMigrationCommandWrapperSpy.createPrepareForMigrationAnswer(prepareForMigrationCommandMock,null,
                libvirtComputingResourceMock, virtualMachineTOMock);

        Assert.assertEquals(cpuShares, prepareForMigrationAnswer.getNewVmCpuShares().intValue());
    }

    private String getTempFilepath() {
        return String.format("%s/%s.txt", System.getProperty("java.io.tmpdir"), UUID.randomUUID());
    }

    private void runTestRemoveDpdkPortForCommandInjection(String portWithCommand) {
        try {
            libvirtPrepareForMigrationCommandWrapperSpy.removeDpdkPort(portWithCommand);
            Assert.fail(String.format("Command injection working for portWithCommand: %s", portWithCommand));
        } catch (CloudRuntimeException ignored) {}
    }

    @Test
    public void testRemoveDpdkPortForCommandInjection() {
        List<String> commandVariants = List.of(
                "';touch %s'",
                ";touch %s",
                "&& touch %s",
                "|| touch %s",
                UUID.randomUUID().toString());
        for (String cmd : commandVariants) {
            String portWithCommand = String.format(cmd, getTempFilepath());
            runTestRemoveDpdkPortForCommandInjection(portWithCommand);
        }
    }

    private LibvirtVMDef.InterfaceDef mockPreparedVmWithOnePluggedNic(KVMStoragePoolManager storagePoolMgr) throws Exception {
        NicTO nic = new NicTO();
        Mockito.when(prepareForMigrationCommandMock.getVirtualMachine()).thenReturn(virtualMachineTOMock);
        Mockito.when(virtualMachineTOMock.getName()).thenReturn("i-2-10-VM");
        Mockito.when(virtualMachineTOMock.getNics()).thenReturn(new NicTO[] {nic});
        Mockito.when(virtualMachineTOMock.getDisks()).thenReturn(new DiskTO[0]);
        Mockito.when(libvirtComputingResourceMock.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        Mockito.when(libvirtComputingResourceMock.getLibvirtUtilitiesHelper()).thenReturn(Mockito.mock(LibvirtUtilitiesHelper.class));
        VifDriver vifDriver = Mockito.mock(VifDriver.class);
        LibvirtVMDef.InterfaceDef interfaceDef = Mockito.mock(LibvirtVMDef.InterfaceDef.class);
        Mockito.when(libvirtComputingResourceMock.getVifDriver(Mockito.any(), Mockito.any())).thenReturn(vifDriver);
        Mockito.when(vifDriver.plug(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(interfaceDef);
        return interfaceDef;
    }

    /**
     * Regression: a prepare that fails after the NICs were plugged must unplug them again -
     * on a Direct Routed network the plug installed host routes and neighbour entries that
     * would otherwise linger on a host the Instance never reaches.
     */
    @Test
    public void executeUnplugsPluggedNicsWhenDiskConnectFails() throws Exception {
        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        LibvirtVMDef.InterfaceDef interfaceDef = mockPreparedVmWithOnePluggedNic(storagePoolMgr);
        Mockito.when(storagePoolMgr.connectPhysicalDisksViaVmSpec(virtualMachineTOMock, true)).thenReturn(false);

        Answer answer = libvirtPrepareForMigrationCommandWrapperSpy.execute(prepareForMigrationCommandMock, libvirtComputingResourceMock);

        Assert.assertFalse(answer.getResult());
        Mockito.verify(libvirtComputingResourceMock).cleanupVMNetworks(Mockito.any(), Mockito.argThat(list -> list.contains(interfaceDef)));
    }

    @Test
    public void executeUnplugsPluggedNicsWhenPrepareThrows() throws Exception {
        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        LibvirtVMDef.InterfaceDef interfaceDef = mockPreparedVmWithOnePluggedNic(storagePoolMgr);
        Mockito.when(virtualMachineTOMock.getDisks()).thenThrow(new CloudRuntimeException("boom"));

        Answer answer = libvirtPrepareForMigrationCommandWrapperSpy.execute(prepareForMigrationCommandMock, libvirtComputingResourceMock);

        Assert.assertFalse(answer.getResult());
        Mockito.verify(libvirtComputingResourceMock).cleanupVMNetworks(Mockito.any(), Mockito.argThat(list -> list.contains(interfaceDef)));
    }
}
