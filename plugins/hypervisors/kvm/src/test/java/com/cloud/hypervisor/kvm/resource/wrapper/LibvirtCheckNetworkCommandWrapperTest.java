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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.network.PhysicalNetworkSetupInfo;

public class LibvirtCheckNetworkCommandWrapperTest {

    private final LibvirtCheckNetworkCommandWrapper wrapper = new LibvirtCheckNetworkCommandWrapper();

    @Test
    public void executeReportsTheMigrationIpResolvedFromTheLabel() {
        LibvirtComputingResource resource = Mockito.mock(LibvirtComputingResource.class);
        Mockito.when(resource.checkNetwork(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(resource.resolveMigrationNetworkIp("cloudbr5")).thenReturn("10.9.9.9");

        PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();
        info.setMigrationNetworkName("cloudbr5");
        CheckNetworkCommand command = new CheckNetworkCommand(Collections.singletonList(info));

        CheckNetworkAnswer answer = (CheckNetworkAnswer) wrapper.execute(command, resource);

        Assert.assertTrue(answer.getResult());
        Assert.assertEquals("10.9.9.9", answer.getMigrationIp());
    }

    @Test
    public void executeReportsNoMigrationIpWhenTheLabelDoesNotResolve() {
        LibvirtComputingResource resource = Mockito.mock(LibvirtComputingResource.class);
        Mockito.when(resource.checkNetwork(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(resource.resolveMigrationNetworkIp("cloudbr5")).thenReturn(null);

        PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();
        info.setMigrationNetworkName("cloudbr5");
        CheckNetworkCommand command = new CheckNetworkCommand(Collections.singletonList(info));

        CheckNetworkAnswer answer = (CheckNetworkAnswer) wrapper.execute(command, resource);

        Assert.assertTrue(answer.getResult());
        Assert.assertNull(answer.getMigrationIp());
        Mockito.verify(resource).resolveMigrationNetworkIp("cloudbr5");
    }
}
