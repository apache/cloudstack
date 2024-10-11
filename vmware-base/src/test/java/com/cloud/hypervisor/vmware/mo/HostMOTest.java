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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
    GuestOsDescriptor guestOsDescriptor;

    HostMO hostMO ;
    ClusterMO clusterMO ;

    AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        hostMO = new HostMO(_context, _mor);
        clusterMO = new ClusterMO(_context, _mor);
        clusterMO._environmentBrowser = _environmentBrowser;
        when(_context.getVimClient()).thenReturn(_client);
        when(_client.getDynamicProperty(any(ManagedObjectReference.class), eq("parent"))).thenReturn(_mor);
        when(_mor.getType()).thenReturn("ClusterComputeResource");
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetGuestOsDescriptors() throws Exception {
        List<GuestOsDescriptor> guestOsDescriptors = new ArrayList<>();
        guestOsDescriptors.add(guestOsDescriptor);
        try (MockedConstruction<ClusterMO> ignored = Mockito.mockConstruction(ClusterMO.class,
                (mock, context) -> when(mock.getGuestOsDescriptors()).thenReturn(guestOsDescriptors))) {
            List<GuestOsDescriptor> result = hostMO.getGuestOsDescriptors();
            Assert.assertEquals(guestOsDescriptor, result.get(0));
        }
    }

    @Test
    public void testGetGuestOsDescriptor() throws Exception {
        try (MockedConstruction<ClusterMO> ignored = Mockito.mockConstruction(ClusterMO.class,
                (mock, context) -> when(mock.getGuestOsDescriptor(any(String.class))).thenReturn(guestOsDescriptor))) {
            GuestOsDescriptor result = hostMO.getGuestOsDescriptor("1");
            Assert.assertEquals(guestOsDescriptor, result);
        }
    }
}
