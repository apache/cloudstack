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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineVideoCard;
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
    @Mock
    VirtualMachineMO vmMo3dgpu;
    @Mock
    VirtualMachineTO vmSpec3dgpu;

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

        _resource.execute(cmd);
        verify(_resource).execute(cmd);
    }

    @Test
    public void testStartVm3dgpuEnabled() throws Exception{
        Map<String, String> specDetails = new HashMap<String, String>();
        specDetails.put("svga.vramSize", "131072");
        when(vmSpec3dgpu.getDetails()).thenReturn(specDetails);

        VirtualMachineVideoCard videoCard = mock(VirtualMachineVideoCard.class);
        when(videoCard.getVideoRamSizeInKB()).thenReturn(65536l);
        when(vmMo3dgpu.getAllDeviceList()).thenReturn(Arrays.asList((VirtualDevice) videoCard));

        when(vmMo3dgpu.configureVm(any(VirtualMachineConfigSpec.class))).thenReturn(true);

        _resource.postVideoCardMemoryConfigBeforeStart(vmMo3dgpu, vmSpec3dgpu);
        verify(vmMo3dgpu).configureVm(any(VirtualMachineConfigSpec.class));
    }

}
