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

package com.cloud.hypervisor.xenserver.resource;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.XenAPIObject;

public class CitrixResourceBaseTest {

    @Spy
    CitrixResourceBase _resource = new CitrixResourceBase() {

        @Override
        public ScaleVmAnswer execute(final ScaleVmCommand cmd) {
            return super.execute(cmd);
        }

        @Override
        public String callHostPlugin(final Connection conn, final String plugin, final String cmd, final String... params) {
            return "Success";
        }

        @Override
        public void scaleVM(final Connection conn, final VM vm, final VirtualMachineTO vmSpec, final Host host) throws Types.XenAPIException, XmlRpcException {
            _host.setSpeed(500);
            super.scaleVM(conn, vm, vmSpec, host);
        }

        @Override
        public boolean isDmcEnabled(final Connection conn, final Host host) throws Types.XenAPIException, XmlRpcException {
            return true;
        }
    };
    @Mock
    XsHost _host;
    @Mock
    Host host;
    @Mock
    ScaleVmCommand cmd;
    @Mock
    VirtualMachineTO vmSpec;
    @Mock
    Connection conn;
    @Mock
    VM vm;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);

        doReturn(vmSpec).when(cmd).getVirtualMachine();
        doReturn("i-2-3-VM").when(vmSpec).getName();

    }

    // Expecting XmlRpcException while trying to get the record of vm using connection
    @Test(expected = XmlRpcException.class)
    public void testScaleVMF1() throws Types.BadServerResponse, Types.XenAPIException, XmlRpcException {
        doReturn(conn).when(_resource).getConnection();
        final Set<VM> vms = mock(Set.class);

        final Iterator iter = mock(Iterator.class);
        doReturn(iter).when(vms).iterator();
        when(iter.hasNext()).thenReturn(true).thenReturn(false);
        doReturn(vm).when(iter).next();
        final VM.Record vmr = mock(VM.Record.class);
        when(vm.getRecord(conn)).thenThrow(new XmlRpcException("XmlRpcException"));
        when(vm.getRecord(conn)).thenReturn(vmr);
        vmr.powerState = Types.VmPowerState.RUNNING;
        vmr.residentOn = mock(Host.class);
        final XenAPIObject object = mock(XenAPIObject.class);
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
        final Map<String, String> args = mock(HashMap.class);
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
        doReturn(500).when(vmSpec).getMaxSpeed();
        doReturn(true).when(vmSpec).getLimitCpuUse();
        doReturn(null).when(_resource).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "cap", "value", "99", "vmname", "i-2-3-VM");
        final Map<String, String> args = mock(HashMap.class);
        when(host.callPlugin(conn, "vmops", "add_to_VCPUs_params_live", args)).thenReturn("Success");
        doReturn(null).when(_resource).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "weight", "value", "253", "vmname", "i-2-3-VM");

        _resource.scaleVM(conn, vm, vmSpec, host);

        verify(vmSpec, times(1)).getLimitCpuUse();
        verify(_resource, times(1)).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "weight", "value", "253", "vmname", "i-2-3-VM");
        verify(_resource, times(1)).callHostPlugin(conn, "vmops", "add_to_VCPUs_params_live", "key", "cap", "value", "99", "vmname", "i-2-3-VM");
    }


    @Test
    public void testSetNicDevIdIfCorrectVifIsNotNull() throws Exception {
        final IpAddressTO ip = mock(IpAddressTO.class);
        when(ip.isAdd()).thenReturn(false);
        final VIF correctVif = null;
        try {
            _resource.setNicDevIdIfCorrectVifIsNotNull(conn, ip, correctVif);
        } catch (final NullPointerException e) {
            fail("this test is meant to show that null pointer is not thrown");
        }
    }
}
