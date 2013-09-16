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

package com.cloud.hypervisor.xen.resource;

import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import com.cloud.hypervisor.xen.resource.CitrixResourceBase.XsHost;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.ScaleVmAnswer;
import com.xensource.xenapi.*;
import org.apache.xmlrpc.XmlRpcException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;




public class CitrixResourceBaseTest {

    @Spy CitrixResourceBase _resource = new CitrixResourceBase() {

        @Override
        public ScaleVmAnswer execute(ScaleVmCommand cmd) {
            return super.execute(cmd);
        }
        public String callHostPlugin(Connection conn, String plugin, String cmd, String... params) {
            return "Success";
        }
        @Override
        protected void scaleVM(Connection conn, VM vm, VirtualMachineTO vmSpec, Host host) throws Types.XenAPIException, XmlRpcException {
            _host.speed = 500;
            super.scaleVM(conn, vm, vmSpec, host);
        }

        @Override
        protected boolean isDmcEnabled(Connection conn, Host host) throws Types.XenAPIException, XmlRpcException {
            return true;
        }
    };
    @Mock XsHost _host;
    @Mock Host host;
    @Mock ScaleVmCommand cmd;
    @Mock VirtualMachineTO vmSpec;
    @Mock Connection conn;
    @Mock VM vm;

    @Before
    public void setup(){

        MockitoAnnotations.initMocks(this);

        doReturn(vmSpec).when(cmd).getVirtualMachine();
        doReturn("i-2-3-VM").when(vmSpec).getName();

    }


    // Expecting XmlRpcException while trying to get the record of vm using connection
    @Test(expected = XmlRpcException.class)
    public void testScaleVMF1()  throws
            Types.BadServerResponse,
            Types.XenAPIException,
            XmlRpcException {
        doReturn(conn).when(_resource).getConnection();
        Set<VM> vms = (Set<VM> )mock(Set.class);

        Iterator iter =  mock(Iterator.class);
        doReturn(iter).when(vms).iterator();
        when(iter.hasNext()).thenReturn(true).thenReturn(false);
        doReturn(vm).when(iter).next();
        VM.Record vmr = mock(VM.Record.class);
        when(vm.getRecord(conn)).thenThrow(new XmlRpcException("XmlRpcException"));
        when(vm.getRecord(conn)).thenReturn(vmr);
        vmr.powerState = Types.VmPowerState.RUNNING;
        vmr.residentOn = mock(Host.class);
        XenAPIObject object = mock(XenAPIObject.class);
        doReturn(new String("OpaqueRef:NULL")).when(object).toWireString();
        doNothing().when(_resource).scaleVM(conn, vm, vmSpec, host);

        _resource.execute(cmd);
        verify(iter, times(2)).hasNext();
        verify(iter, times(2)).next();

    }

    // Test to scale vm "i-2-3-VM" cpu-cap disabled
    @Test
    public void testScaleVMF2() throws Types.XenAPIException, XmlRpcException {

        when(vm.getMemoryStaticMax(conn)).thenReturn(1073741824L);
        when(vm.getMemoryStaticMin(conn)).thenReturn(268435456L);
        doReturn(536870912L).when(vmSpec).getMinRam();
        doReturn(536870912L).when(vmSpec).getMaxRam();
        doNothing().when(vm).setMemoryDynamicRange(conn, 536870912L, 536870912L);
        doReturn(1).when(vmSpec).getCpus();
        doNothing().when(vm).setVCPUsNumberLive(conn, 1L);
        doReturn(500).when(vmSpec).getMinSpeed();
        doReturn(false).when(vmSpec).getLimitCpuUse();
        Map<String, String> args = (Map<String, String>)mock(HashMap.class);
        when(host.callPlugin(conn, "vmops", "add_to_VCPUs_params_live", args)).thenReturn("Success");
        doReturn(null).when(_resource).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "weight", "value", "253", "vmname", "i-2-3-VM");

        _resource.scaleVM(conn, vm, vmSpec, host);

        verify(vmSpec, times(1)).getLimitCpuUse();
        verify(_resource, times(1)).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "weight", "value", "253", "vmname", "i-2-3-VM");
    }

    // Test to scale vm "i-2-3-VM" cpu-cap enabled
    @Test
    public void testScaleVMF3() throws Types.XenAPIException, XmlRpcException {

        when(vm.getMemoryStaticMax(conn)).thenReturn(1073741824L);
        when(vm.getMemoryStaticMin(conn)).thenReturn(268435456L);
        doReturn(536870912L).when(vmSpec).getMinRam();
        doReturn(536870912L).when(vmSpec).getMaxRam();
        doNothing().when(vm).setMemoryDynamicRange(conn, 536870912L, 536870912L);
        doReturn(1).when(vmSpec).getCpus();
        doNothing().when(vm).setVCPUsNumberLive(conn, 1L);
        doReturn(500).when(vmSpec).getMinSpeed();
        doReturn(true).when(vmSpec).getLimitCpuUse();
        doReturn(null).when(_resource).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "cap", "value", "99", "vmname", "i-2-3-VM");
        Map<String, String> args = (Map<String, String>)mock(HashMap.class);
        when(host.callPlugin(conn, "vmops", "add_to_VCPUs_params_live", args)).thenReturn("Success");
        doReturn(null).when(_resource).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "weight", "value", "253", "vmname", "i-2-3-VM");

        _resource.scaleVM(conn, vm, vmSpec, host);

        verify(vmSpec, times(1)).getLimitCpuUse();
        verify(_resource, times(1)).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "weight", "value", "253", "vmname", "i-2-3-VM");
        verify(_resource, times(1)).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "cap", "value", "99", "vmname", "i-2-3-VM");
    }
}