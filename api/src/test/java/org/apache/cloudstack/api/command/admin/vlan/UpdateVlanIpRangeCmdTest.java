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
package org.apache.cloudstack.api.command.admin.vlan;

import junit.framework.TestCase;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.configuration.ConfigurationService;
import com.cloud.dc.Vlan;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

public class UpdateVlanIpRangeCmdTest extends TestCase {

    private UpdateVlanIpRangeCmd updateVlanIpRangeCmd;
    private ResponseGenerator responseGenerator;

    @Test
    public void testUpdateSuccess() throws Exception {

        ConfigurationService configService = Mockito.mock(ConfigurationService.class);
        Vlan result = Mockito.mock(Vlan.class);

        responseGenerator = Mockito.mock(ResponseGenerator.class);
        updateVlanIpRangeCmd = new UpdateVlanIpRangeCmd();

        Mockito.when(configService.updateVlanAndPublicIpRange(updateVlanIpRangeCmd)).thenReturn(result);
        updateVlanIpRangeCmd._configService = configService;

        VlanIpRangeResponse ipRes = Mockito.mock(VlanIpRangeResponse.class);
        Mockito.when(responseGenerator.createVlanIpRangeResponse(result)).thenReturn(ipRes);

        updateVlanIpRangeCmd._responseGenerator = responseGenerator;
        try {
            updateVlanIpRangeCmd.execute();
        } catch (ServerApiException ex) {
            assertEquals("Failed to Update vlan ip range", ex.getMessage());
        }
    }

    @Test
    public void testUpdateFailure() throws ResourceAllocationException, ResourceUnavailableException {

        ConfigurationService configService = Mockito.mock(ConfigurationService.class);

        responseGenerator = Mockito.mock(ResponseGenerator.class);
        updateVlanIpRangeCmd = new UpdateVlanIpRangeCmd();
        updateVlanIpRangeCmd._configService = configService;

        Mockito.when(configService.updateVlanAndPublicIpRange(updateVlanIpRangeCmd)).thenReturn(null);

        try {
            updateVlanIpRangeCmd.execute();
        } catch (ServerApiException ex) {
            assertEquals("Failed to Update vlan ip range", ex.getMessage());
        }

    }
}
