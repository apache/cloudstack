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
package org.apache.cloudstack.api.command.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.network.AddNetworkServiceProviderCmd;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.NetworkService;
import com.cloud.network.PhysicalNetworkServiceProvider;

public class AddNetworkServiceProviderCmdTest extends TestCase {

    private AddNetworkServiceProviderCmd addNetworkServiceProviderCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    @Before
    public void setUp() {
        addNetworkServiceProviderCmd = new AddNetworkServiceProviderCmd() {

            @Override
            public Long getPhysicalNetworkId() {
                return 2L;
            }

            @Override
            public String getProviderName() {
                return "ProviderName";
            }

            @Override
            public Long getDestinationPhysicalNetworkId() {
                return 2L;
            }

            @Override
            public List<String> getEnabledServices() {
                List<String> lOfEnabledServices = new ArrayList<String>();
                lOfEnabledServices.add("Enabled Services");
                return lOfEnabledServices;
            }

            @Override
            public Long getEntityId() {
                return 2L;
            }

        };

    }

    @Test
    public void testCreateProviderToPhysicalNetworkSuccess() {

        NetworkService networkService = Mockito.mock(NetworkService.class);
        addNetworkServiceProviderCmd._networkService = networkService;

        PhysicalNetworkServiceProvider physicalNetworkServiceProvider = Mockito.mock(PhysicalNetworkServiceProvider.class);
        Mockito.when(networkService.addProviderToPhysicalNetwork(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyList())).thenReturn(
            physicalNetworkServiceProvider);

        try {
            addNetworkServiceProviderCmd.create();
        } catch (ResourceAllocationException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testCreateProviderToPhysicalNetworkFailure() throws ResourceAllocationException {

        NetworkService networkService = Mockito.mock(NetworkService.class);
        addNetworkServiceProviderCmd._networkService = networkService;

        Mockito.when(networkService.addProviderToPhysicalNetwork(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyList())).thenReturn(null);

        try {
            addNetworkServiceProviderCmd.create();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add service provider entity to physical network", exception.getDescription());
        }

    }

}
