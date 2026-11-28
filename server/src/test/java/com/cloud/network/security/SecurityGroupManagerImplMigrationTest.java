// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.network.security;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SecurityGroupManagerImplMigrationTest {

    @Mock
    AgentManager _agentMgr;

    @InjectMocks
    SecurityGroupManagerImpl manager = new SecurityGroupManagerImpl();

    @Test
    public void systemVmMigrationSendsNetworkRulesSystemVmCommand() throws Exception {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);
        Mockito.when(vm.getInstanceName()).thenReturn("s-1-VM");
        Mockito.when(vm.getHostId()).thenReturn(2L);

        manager.handleVmMigrated(vm);

        ArgumentCaptor<Commands> captor = ArgumentCaptor.forClass(Commands.class);
        Mockito.verify(_agentMgr).send(Mockito.eq(2L), captor.capture());
        Assert.assertEquals(1, captor.getValue().size());
        Assert.assertTrue(captor.getValue().toCommands()[0] instanceof NetworkRulesSystemVmCommand);
    }
}
