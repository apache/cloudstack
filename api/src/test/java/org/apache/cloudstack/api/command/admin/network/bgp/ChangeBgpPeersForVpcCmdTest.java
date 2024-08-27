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

package org.apache.cloudstack.api.command.admin.network.bgp;

import com.cloud.event.EventTypes;

import com.cloud.network.vpc.Vpc;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ChangeBgpPeersForVpcCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    ResponseGenerator _responseGenerator = Mockito.spy(ResponseGenerator.class);

    @Test
    public void testChangeBgpPeersForVpcCmd() {
        Long VpcId = 10L;
        List<Long> bgpPeerIds = Arrays.asList(20L, 21L);

        ChangeBgpPeersForVpcCmd cmd = new ChangeBgpPeersForVpcCmd();
        ReflectionTestUtils.setField(cmd, "vpcId", VpcId);
        ReflectionTestUtils.setField(cmd, "bgpPeerIds", bgpPeerIds);
        ReflectionTestUtils.setField(cmd,"routedIpv4Manager", routedIpv4Manager);
        ReflectionTestUtils.setField(cmd,"_responseGenerator", _responseGenerator);

        Assert.assertEquals(VpcId, cmd.getVpcId());
        Assert.assertEquals(bgpPeerIds, cmd.getBgpPeerIds());
        Assert.assertEquals(1L, cmd.getEntityOwnerId());
        Assert.assertEquals(EventTypes.EVENT_VPC_BGP_PEER_UPDATE, cmd.getEventType());
        Assert.assertEquals(String.format("Changing Bgp Peers for VPC %s", VpcId), cmd.getEventDescription());

        Vpc Vpc = Mockito.mock(Vpc.class);
        Mockito.when(routedIpv4Manager.changeBgpPeersForVpc(cmd)).thenReturn(Vpc);

        VpcResponse response = Mockito.mock(VpcResponse.class);
        Mockito.when(_responseGenerator.createVpcResponse(ResponseObject.ResponseView.Full, Vpc)).thenReturn(response);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(response, cmd.getResponseObject());
    }
}
