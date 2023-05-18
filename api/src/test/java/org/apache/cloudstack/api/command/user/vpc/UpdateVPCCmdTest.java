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

package org.apache.cloudstack.api.command.user.vpc;

import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcService;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.VpcResponse;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
public class UpdateVPCCmdTest extends TestCase {

    @Mock
    VpcService _vpcService;

    private ResponseGenerator responseGenerator;

    @InjectMocks
    UpdateVPCCmd cmd = new UpdateVPCCmd();

    public void testGetVpcName() {
        String vpcName = "updatedVpcName";
        ReflectionTestUtils.setField(cmd, "vpcName", vpcName);
        Assert.assertEquals(cmd.getVpcName(), vpcName);
    }

    public void testGetDisplayText() {
        String displayText = "Updated VPC Name";
        ReflectionTestUtils.setField(cmd, "displayText", displayText);
        Assert.assertEquals(cmd.getDisplayText(), displayText);
    }

    public void testGetId() {
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(cmd.getId(), id);
    }

    public void testIsDisplayVpc() {
        Boolean display = true;
        ReflectionTestUtils.setField(cmd, "display", display);
        Assert.assertEquals(cmd.isDisplayVpc(), display);
    }

    public void testGetPublicMtu() {
        Integer publicMtu = 1450;
        ReflectionTestUtils.setField(cmd, "publicMtu", publicMtu);
        Assert.assertEquals(cmd.getPublicMtu(), publicMtu);
    }

    public void testExecute() {
        ReflectionTestUtils.setField(cmd, "id", 1L);
        ReflectionTestUtils.setField(cmd, "vpcName", "updatedVpcName");
        ReflectionTestUtils.setField(cmd, "displayText", "Updated VPC Name");
        ReflectionTestUtils.setField(cmd, "displayText", "Updated VPC Name");
        ReflectionTestUtils.setField(cmd, "customId", null);
        ReflectionTestUtils.setField(cmd, "display", true);
        ReflectionTestUtils.setField(cmd, "publicMtu", 1450);
        Vpc vpc = Mockito.mock(Vpc.class);
        VpcResponse response = Mockito.mock(VpcResponse.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        cmd._responseGenerator = responseGenerator;
        Mockito.when(_vpcService.updateVpc(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(vpc);
        Mockito.when(responseGenerator.createVpcResponse(ResponseObject.ResponseView.Full, vpc)).thenReturn(response);
        Mockito.verify(_vpcService, Mockito.times(0)).updateVpc(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyInt());

    }
}
