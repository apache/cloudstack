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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.user.vm.AddIpToVmNicCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveIpFromVmNicCmd;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkService;
import com.cloud.vm.NicSecondaryIp;

public class AddIpToVmNicTest extends TestCase {

    private AddIpToVmNicCmd addIpToVmNicCmd;
    private RemoveIpFromVmNicCmd removeIpFromVmNicCmd;
    private ResponseGenerator responseGenerator;
    private SuccessResponse successResponseGenerator;

    @Override
    @Before
    public void setUp() {

    }

    @Test
    public void testCreateSuccess() throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {

        NetworkService networkService = Mockito.mock(NetworkService.class);
        AddIpToVmNicCmd ipTonicCmd = Mockito.mock(AddIpToVmNicCmd.class);
        NicSecondaryIp secIp = Mockito.mock(NicSecondaryIp.class);

        Mockito.when(
            networkService.allocateSecondaryGuestIP(Matchers.anyLong(), Matchers.any()))
            .thenReturn(secIp);

        ipTonicCmd._networkService = networkService;
        responseGenerator = Mockito.mock(ResponseGenerator.class);

        NicSecondaryIpResponse ipres = Mockito.mock(NicSecondaryIpResponse.class);
        Mockito.when(responseGenerator.createSecondaryIPToNicResponse(secIp)).thenReturn(ipres);

        ipTonicCmd._responseGenerator = responseGenerator;
        ipTonicCmd.execute();
    }

    @Test
    public void testCreateFailure() throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {

        NetworkService networkService = Mockito.mock(NetworkService.class);
        AddIpToVmNicCmd ipTonicCmd = Mockito.mock(AddIpToVmNicCmd.class);

        Mockito.when(
            networkService.allocateSecondaryGuestIP(Matchers.anyLong(), Matchers.any()))
            .thenReturn(null);

        ipTonicCmd._networkService = networkService;

        try {
            ipTonicCmd.execute();
        } catch (InsufficientAddressCapacityException e) {
            throw new InvalidParameterValueException("Allocating guest ip for nic failed");
        }
    }

    @Test
    public void testRemoveIpFromVmNicSuccess() throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException,
        InsufficientCapacityException {

        NetworkService networkService = Mockito.mock(NetworkService.class);
        RemoveIpFromVmNicCmd removeIpFromNic = Mockito.mock(RemoveIpFromVmNicCmd.class);

        Mockito.when(networkService.releaseSecondaryIpFromNic(Matchers.anyInt())).thenReturn(true);

        removeIpFromNic._networkService = networkService;
        removeIpFromNic.execute();
    }

    @Test
    public void testRemoveIpFromVmNicFailure() throws InsufficientAddressCapacityException {
        NetworkService networkService = Mockito.mock(NetworkService.class);
        RemoveIpFromVmNicCmd removeIpFromNic = Mockito.mock(RemoveIpFromVmNicCmd.class);

        Mockito.when(networkService.releaseSecondaryIpFromNic(Matchers.anyInt())).thenReturn(false);

        removeIpFromNic._networkService = networkService;
        successResponseGenerator = Mockito.mock(SuccessResponse.class);

        try {
            removeIpFromNic.execute();
        } catch (InvalidParameterValueException exception) {
            Assert.assertEquals("Failed to remove secondary  ip address for the nic", exception.getLocalizedMessage());
        }
    }
}
