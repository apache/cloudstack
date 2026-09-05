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
package org.apache.cloudstack.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import org.apache.cloudstack.agent.api.AddOrUpdateNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisACLCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisACLCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisLoadBalancerRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisStaticRouteCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVnetCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.agent.api.ReleaseNatIpCommand;
import org.apache.cloudstack.agent.api.SetupNetrisPublicRangeCommand;
import org.apache.cloudstack.service.NetrisApiClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.List;

public class NetrisResourceTest {

    @Mock
    private NetrisApiClient netrisApiClient;

    @Spy
    @InjectMocks
    private NetrisResource netrisResource = new NetrisResource();

    private AutoCloseable closeable;

    @Mock
    private CreateNetrisVpcCommand createNetrisVpcCommand;
    @Mock
    private CreateNetrisVnetCommand createNetrisVnetCommand;
    @Mock
    private DeleteNetrisVnetCommand deleteNetrisVnetCommand;
    @Mock
    private DeleteNetrisVpcCommand deleteNetrisVpcCommand;
    @Mock
    private SetupNetrisPublicRangeCommand setupNetrisPublicRangeCommand;
    @Mock
    private DeleteNetrisNatRuleCommand deleteNetrisNatRuleCommand;
    @Mock
    private CreateOrUpdateNetrisACLCommand createNetrisACLCommand;
    @Mock
    private DeleteNetrisACLCommand deleteNetrisACLCommand;
    @Mock
    private AddOrUpdateNetrisStaticRouteCommand addOrUpdateNetrisStaticRouteCommand;
    @Mock
    private DeleteNetrisStaticRouteCommand deleteNetrisStaticRouteCommand;
    @Mock
    private ReleaseNatIpCommand releaseNatIpCommand;
    @Mock
    private CreateOrUpdateNetrisLoadBalancerRuleCommand createOrUpdateNetrisLoadBalancerRuleCommand;
    @Mock
    private DeleteNetrisLoadBalancerRuleCommand deleteNetrisLoadBalancerRuleCommand;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testExecuteRequest() {
        List<Command> commands = Arrays.asList(createNetrisVpcCommand, createNetrisVnetCommand, deleteNetrisVnetCommand,
                deleteNetrisVpcCommand, setupNetrisPublicRangeCommand, deleteNetrisNatRuleCommand, createNetrisACLCommand,
                deleteNetrisACLCommand, addOrUpdateNetrisStaticRouteCommand, deleteNetrisStaticRouteCommand,
                releaseNatIpCommand, createOrUpdateNetrisLoadBalancerRuleCommand, deleteNetrisLoadBalancerRuleCommand);

        for (boolean res : new boolean[]{true, false}) {
            setMocksToValue(res);
            for (Command command : commands) {
                Answer answer = netrisResource.executeRequest(command);
                Assert.assertEquals(res, answer.getResult());
            }
        }

        Mockito.verify(netrisApiClient, Mockito.times(2)).createVpc(createNetrisVpcCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).createVnet(createNetrisVnetCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).deleteVnet(deleteNetrisVnetCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).deleteVpc(deleteNetrisVpcCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).setupZoneLevelPublicRange(setupNetrisPublicRangeCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).deleteNatRule(deleteNetrisNatRuleCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).addOrUpdateAclRule(Mockito.eq(createNetrisACLCommand), Mockito.anyBoolean());
        Mockito.verify(netrisApiClient, Mockito.times(2)).deleteAclRule(Mockito.eq(deleteNetrisACLCommand), Mockito.anyBoolean());
        Mockito.verify(netrisApiClient, Mockito.times(2)).addOrUpdateStaticRoute(addOrUpdateNetrisStaticRouteCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).deleteStaticRoute(deleteNetrisStaticRouteCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).releaseNatIp(releaseNatIpCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).createOrUpdateLbRule(createOrUpdateNetrisLoadBalancerRuleCommand);
        Mockito.verify(netrisApiClient, Mockito.times(2)).deleteLbRule(deleteNetrisLoadBalancerRuleCommand);
    }

    private void setMocksToValue(boolean value) {
        Mockito.when(netrisApiClient.createVpc(createNetrisVpcCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.createVnet(createNetrisVnetCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.deleteVnet(deleteNetrisVnetCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.deleteVpc(deleteNetrisVpcCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.setupZoneLevelPublicRange(setupNetrisPublicRangeCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.deleteNatRule(deleteNetrisNatRuleCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.addOrUpdateAclRule(Mockito.eq(createNetrisACLCommand), Mockito.anyBoolean())).thenReturn(value);
        Mockito.when(netrisApiClient.deleteAclRule(Mockito.eq(deleteNetrisACLCommand), Mockito.anyBoolean())).thenReturn(value);
        Mockito.when(netrisApiClient.addOrUpdateStaticRoute(addOrUpdateNetrisStaticRouteCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.deleteStaticRoute(deleteNetrisStaticRouteCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.releaseNatIp(releaseNatIpCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.createOrUpdateLbRule(createOrUpdateNetrisLoadBalancerRuleCommand)).thenReturn(value);
        Mockito.when(netrisApiClient.deleteLbRule(deleteNetrisLoadBalancerRuleCommand)).thenReturn(value);
    }
}
