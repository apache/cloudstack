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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmService;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vm.UpdateVmNicIpCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;

public class UpdateVmNicIpTest extends TestCase {

    private UpdateVmNicIpCmd updateVmNicIpCmd;
    private ResponseGenerator responseGenerator;

    @Override
    @Before
    public void setUp() {

    }

    @Test
    public void testSuccess() throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {

        UserVmService userVmService = Mockito.mock(UserVmService.class);
        updateVmNicIpCmd = Mockito.mock(UpdateVmNicIpCmd.class);
        UserVm userVm = Mockito.mock(UserVm.class);

        Mockito.when(userVmService.updateNicIpForVirtualMachine(Mockito.any(UpdateVmNicIpCmd.class))).thenReturn(userVm);

        updateVmNicIpCmd._userVmService = userVmService;
        responseGenerator = Mockito.mock(ResponseGenerator.class);

        List<UserVmResponse> list = new LinkedList<UserVmResponse>();
        UserVmResponse userVmResponse = Mockito.mock(UserVmResponse.class);
        list.add(userVmResponse);
        Mockito.when(responseGenerator.createUserVmResponse(ResponseView.Restricted, "virtualmachine", userVm)).thenReturn(list);

        updateVmNicIpCmd._responseGenerator = responseGenerator;
        updateVmNicIpCmd.execute();
    }

    @Test
    public void testFailure() throws ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        UserVmService userVmService = Mockito.mock(UserVmService.class);
        updateVmNicIpCmd = Mockito.mock(UpdateVmNicIpCmd.class);

        Mockito.when(userVmService.updateNicIpForVirtualMachine(Mockito.any(UpdateVmNicIpCmd.class))).thenReturn(null);

        updateVmNicIpCmd._userVmService = userVmService;

        updateVmNicIpCmd._responseGenerator = responseGenerator;
        try {
            updateVmNicIpCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to update ip address on vm NIC. Refer to server logs for details.", exception.getDescription());
        }
    }

}
