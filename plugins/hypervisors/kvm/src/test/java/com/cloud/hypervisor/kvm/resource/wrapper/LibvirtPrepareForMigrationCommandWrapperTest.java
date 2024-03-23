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

import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.to.DpdkTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

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
}
