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

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.DpdkTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.storage.Storage;
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

        PrepareForMigrationAnswer prepareForMigrationAnswer = libvirtPrepareForMigrationCommandWrapperSpy.createPrepareForMigrationAnswer(prepareForMigrationCommandMock, dpdkInterfaceMapping, null,
                libvirtComputingResourceMock, virtualMachineTOMock);

        Assert.assertEquals(prepareForMigrationAnswer.getDpdkInterfaceMapping(), dpdkInterfaceMapping);
    }

    @Test
    public void createPrepareForMigrationAnswerTestNicBridgeMappingNotEmptyShouldSetParamOnAnswer() {
        Map<String, String> nicBridgeMapping = new HashMap<>();
        nicBridgeMapping.put("02:00:00:00:00:01", "cloudbr0");

        PrepareForMigrationAnswer prepareForMigrationAnswer = libvirtPrepareForMigrationCommandWrapperSpy.createPrepareForMigrationAnswer(prepareForMigrationCommandMock, null, nicBridgeMapping,
                libvirtComputingResourceMock, virtualMachineTOMock);

        Assert.assertEquals(nicBridgeMapping, prepareForMigrationAnswer.getNicBridgeMapping());
    }

    @Test
    public void createPrepareForMigrationAnswerTestVerifyThatCpuSharesIsSet() {
        int cpuShares = 1000;
        Mockito.doReturn(cpuShares).when(libvirtComputingResourceMock).calculateCpuShares(virtualMachineTOMock);
        PrepareForMigrationAnswer prepareForMigrationAnswer = libvirtPrepareForMigrationCommandWrapperSpy.createPrepareForMigrationAnswer(prepareForMigrationCommandMock, null, null,
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

    private DiskTO diskWithDestPoolTypeDetail(String destPoolType) {
        DiskTO disk = Mockito.mock(DiskTO.class);
        Map<String, String> details = new HashMap<>();
        if (destPoolType != null) {
            details.put(DiskTO.MIGRATION_DEST_POOL_TYPE, destPoolType);
        }
        Mockito.doReturn(details).when(disk).getDetails();
        return disk;
    }

    private VolumeObjectTO volumeOnPool(Storage.StoragePoolType poolType) {
        VolumeObjectTO volumeObjectTO = Mockito.mock(VolumeObjectTO.class);
        PrimaryDataStoreTO dataStore = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.doReturn(poolType).when(dataStore).getPoolType();
        Mockito.doReturn(dataStore).when(volumeObjectTO).getDataStore();
        return volumeObjectTO;
    }

    @Test
    public void refuseIfDestinationCannotHotplugEncryptedRbdAllowsANonRbdDestination() {
        Answer answer = libvirtPrepareForMigrationCommandWrapperSpy.refuseIfDestinationCannotHotplugEncryptedRbd(prepareForMigrationCommandMock,
                virtualMachineTOMock, diskWithDestPoolTypeDetail(Storage.StoragePoolType.NetworkFilesystem.toString()),
                volumeOnPool(Storage.StoragePoolType.RBD), libvirtComputingResourceMock);

        Assert.assertNull(answer);
        Mockito.verify(libvirtComputingResourceMock, Mockito.never()).getHypervisorLibvirtVersion();
    }

    @Test
    public void refuseIfDestinationCannotHotplugEncryptedRbdAllowsAHostWithNewEnoughLibvirt() {
        Mockito.doReturn(10001000L).when(libvirtComputingResourceMock).getHypervisorLibvirtVersion(); // libvirt 10.1.0

        Answer answer = libvirtPrepareForMigrationCommandWrapperSpy.refuseIfDestinationCannotHotplugEncryptedRbd(prepareForMigrationCommandMock,
                virtualMachineTOMock, diskWithDestPoolTypeDetail(Storage.StoragePoolType.RBD.toString()), volumeOnPool(Storage.StoragePoolType.RBD),
                libvirtComputingResourceMock);

        Assert.assertNull(answer);
    }

    @Test
    public void refuseIfDestinationCannotHotplugEncryptedRbdRefusesByTheDestPoolTypeDetailWhenStorageIsMigrating() {
        Mockito.doReturn("i-2-3-VM").when(virtualMachineTOMock).getName();
        Mockito.doReturn(10000000L).when(libvirtComputingResourceMock).getHypervisorLibvirtVersion(); // libvirt 10.0.0

        // the disk's own dataStore still reports the source (NFS) pool: the migration detail is what must be trusted
        Answer answer = libvirtPrepareForMigrationCommandWrapperSpy.refuseIfDestinationCannotHotplugEncryptedRbd(prepareForMigrationCommandMock,
                virtualMachineTOMock, diskWithDestPoolTypeDetail(Storage.StoragePoolType.RBD.toString()),
                volumeOnPool(Storage.StoragePoolType.NetworkFilesystem), libvirtComputingResourceMock);

        Assert.assertNotNull(answer);
        Assert.assertFalse(((PrepareForMigrationAnswer) answer).getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("Libvirt version 10.1.0 required"));
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("version 10.0.0 detected"));
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("i-2-3-VM"));
    }

    @Test
    public void refuseIfDestinationCannotHotplugEncryptedRbdFallsBackToTheDisksOwnPoolWhenStorageIsNotMigrating() {
        Mockito.doReturn("i-2-3-VM").when(virtualMachineTOMock).getName();
        Mockito.doReturn(10000000L).when(libvirtComputingResourceMock).getHypervisorLibvirtVersion(); // libvirt 10.0.0

        Answer answer = libvirtPrepareForMigrationCommandWrapperSpy.refuseIfDestinationCannotHotplugEncryptedRbd(prepareForMigrationCommandMock,
                virtualMachineTOMock, diskWithDestPoolTypeDetail(null), volumeOnPool(Storage.StoragePoolType.RBD), libvirtComputingResourceMock);

        Assert.assertNotNull(answer);
        Assert.assertFalse(((PrepareForMigrationAnswer) answer).getResult());
    }
}
