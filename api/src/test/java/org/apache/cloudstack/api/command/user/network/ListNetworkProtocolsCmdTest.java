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

package org.apache.cloudstack.api.command.user.network;

import com.cloud.utils.net.NetworkProtocols;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkProtocolResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ListNetworkProtocolsCmdTest {

    @Test
    public void testListNetworkProtocolNumbers() {
        ListNetworkProtocolsCmd cmd = new ListNetworkProtocolsCmd();
        String option = NetworkProtocols.Option.ProtocolNumber.toString();
        ReflectionTestUtils.setField(cmd, "option", option);
        Assert.assertEquals(cmd.getOption(), option);

        try {
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Object response = cmd.getResponseObject();
        Assert.assertTrue(response instanceof ListResponse);
        ListResponse listResponse = (ListResponse) response;
        Assert.assertEquals(BaseCmd.getResponseNameByClass(cmd.getClass()), listResponse.getResponseName());
        Assert.assertNotNull(listResponse.getResponses());
        Assert.assertNotEquals(0, listResponse.getResponses().size());
        Object firstResponse = listResponse.getResponses().get(0);
        Assert.assertTrue(firstResponse instanceof NetworkProtocolResponse);
        Assert.assertEquals("networkprotocol", ((NetworkProtocolResponse) firstResponse).getObjectName());
        Assert.assertEquals(Integer.valueOf(0), ((NetworkProtocolResponse) firstResponse).getIndex());
        Assert.assertEquals("HOPOPT", ((NetworkProtocolResponse) firstResponse).getName());
    }

    @Test
    public void testListIcmpTypes() {
        ListNetworkProtocolsCmd cmd = new ListNetworkProtocolsCmd();
        String option = NetworkProtocols.Option.IcmpType.toString();
        ReflectionTestUtils.setField(cmd, "option", option);
        Assert.assertEquals(cmd.getOption(), option);

        try {
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Object response = cmd.getResponseObject();
        Assert.assertTrue(response instanceof ListResponse);
        ListResponse listResponse = (ListResponse) response;
        Assert.assertEquals(BaseCmd.getResponseNameByClass(cmd.getClass()), listResponse.getResponseName());
        Assert.assertNotNull(listResponse.getResponses());
        Assert.assertNotEquals(0, listResponse.getResponses().size());
        Object firstResponse = listResponse.getResponses().get(0);
        Assert.assertTrue(firstResponse instanceof NetworkProtocolResponse);
        Assert.assertEquals("networkprotocol", ((NetworkProtocolResponse) firstResponse).getObjectName());
        Assert.assertEquals(Integer.valueOf(0), ((NetworkProtocolResponse) firstResponse).getIndex());
        Assert.assertNotNull(((NetworkProtocolResponse) firstResponse).getDetails());
        System.out.println(((NetworkProtocolResponse) firstResponse).getDetails());
        Assert.assertEquals("Echo reply", ((NetworkProtocolResponse) firstResponse).getDetails().get("0"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testListInvalidOption() {
        ListNetworkProtocolsCmd cmd = new ListNetworkProtocolsCmd();
        String option = "invalid-option";
        ReflectionTestUtils.setField(cmd, "option", option);
        Assert.assertEquals(cmd.getOption(), option);

        cmd.execute();
    }
}
