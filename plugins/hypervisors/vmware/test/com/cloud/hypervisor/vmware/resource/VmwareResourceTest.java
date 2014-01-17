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
package com.cloud.hypervisor.vmware.resource;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.vmware.vim25.VirtualMachineConfigSpec;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareContext;

public class VmwareResourceTest {

    @Spy
    VmwareResource _resource = new VmwareResource() {

        @Override
        public ScaleVmAnswer execute(ScaleVmCommand cmd) {
            return super.execute(cmd);
        }

        @Override
        public VmwareHypervisorHost getHyperHost(VmwareContext context, Command cmd) {
            return hyperHost;
        }
    };

    @Mock
    VmwareContext context;
    @Mock
    ScaleVmCommand cmd;
    @Mock
    VirtualMachineTO vmSpec;
    @Mock
    VmwareHypervisorHost hyperHost;
    @Mock
    VirtualMachineMO vmMo;
    @Mock
    VirtualMachineConfigSpec vmConfigSpec;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(context).when(_resource).getServiceContext(null);
        when(cmd.getVirtualMachine()).thenReturn(vmSpec);
    }

    //Test successful scaling up the vm
    @Test
    public void testScaleVMF1() throws Exception {
        when(_resource.getHyperHost(context, null)).thenReturn(hyperHost);
        doReturn("i-2-3-VM").when(cmd).getVmName();
        when(hyperHost.findVmOnHyperHost("i-2-3-VM")).thenReturn(vmMo);
        doReturn(536870912L).when(vmSpec).getMinRam();
        doReturn(1).when(vmSpec).getCpus();
        doReturn(1000).when(vmSpec).getMinSpeed();
        doReturn(1000).when(vmSpec).getMaxSpeed();
        doReturn(536870912L).when(vmSpec).getMaxRam();
        doReturn(false).when(vmSpec).getLimitCpuUse();
        when(vmMo.configureVm(vmConfigSpec)).thenReturn(true);

        ScaleVmAnswer answer = _resource.execute(cmd);
        verify(_resource).execute(cmd);
    }

}
