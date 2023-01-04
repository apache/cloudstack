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

package com.cloud.hypervisor.vmware.mo;

import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.GuestOsDescriptor;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineConfigOption;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HostMO.class)
public class HostMOTest {

    @Mock
    VmwareContext _context ;
    @Mock
    VmwareClient _client;
    @Mock
    ManagedObjectReference _mor;
    @Mock
    ManagedObjectReference _environmentBrowser;

    @Mock
    VirtualMachineConfigOption vmConfigOption;

    @Mock
    GuestOsDescriptor guestOsDescriptor;

    HostMO hostMO ;
    ClusterMO clusterMO ;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        hostMO = new HostMO(_context, _mor);
        clusterMO = new ClusterMO(_context, _mor);
        clusterMO._environmentBrowser = _environmentBrowser;
        when(_context.getVimClient()).thenReturn(_client);
        when(_client.getDynamicProperty(any(ManagedObjectReference.class), eq("parent"))).thenReturn(_mor);
        when(_mor.getType()).thenReturn("ClusterComputeResource");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetGuestOsDescriptors() throws Exception {
        VimPortType vimPortType = PowerMockito.mock(VimPortType.class);
        Mockito.when(_context.getService()).thenReturn(vimPortType);
        Mockito.when(vimPortType.queryConfigOption(_environmentBrowser, null, null)).thenReturn(vmConfigOption);
        PowerMockito.whenNew(ClusterMO.class).withArguments(_context, _mor).thenReturn(clusterMO);

        List<GuestOsDescriptor> guestOsDescriptors = new ArrayList<>();
        guestOsDescriptors.add(guestOsDescriptor);
        Mockito.when(clusterMO.getGuestOsDescriptors()).thenReturn(guestOsDescriptors);
        List<GuestOsDescriptor> result = hostMO.getGuestOsDescriptors();

        Assert.assertEquals(guestOsDescriptor, result.get(0));
    }

    @Test
    public void testGetGuestOsDescriptor() throws Exception {
        VimPortType vimPortType = PowerMockito.mock(VimPortType.class);
        Mockito.when(_context.getService()).thenReturn(vimPortType);
        Mockito.when(vimPortType.queryConfigOption(_environmentBrowser, null, null)).thenReturn(vmConfigOption);
        PowerMockito.whenNew(ClusterMO.class).withArguments(_context, _mor).thenReturn(clusterMO);

        Mockito.when(guestOsDescriptor.getId()).thenReturn("1");
        List<GuestOsDescriptor> guestOsDescriptors = new ArrayList<>();
        guestOsDescriptors.add(guestOsDescriptor);
        Mockito.when(vmConfigOption.getGuestOSDescriptor()).thenReturn(guestOsDescriptors);
        GuestOsDescriptor result = hostMO.getGuestOsDescriptor("1");

        Assert.assertEquals(guestOsDescriptor, result);
    }
}
