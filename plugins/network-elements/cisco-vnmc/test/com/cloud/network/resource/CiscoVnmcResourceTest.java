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
package com.cloud.network.resource;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.network.cisco.CiscoVnmcConnectionImpl;
import com.cloud.utils.exception.ExecutionException;

public class CiscoVnmcResourceTest {
    CiscoVnmcConnectionImpl _connection = mock(CiscoVnmcConnectionImpl.class);
    CiscoVnmcResource _resource;
    Map<String,Object> _parameters;

    @Before
    public void setUp() throws ConfigurationException {
        _resource = new CiscoVnmcResource();

        _parameters = new HashMap<String, Object>();
        _parameters.put("name", "CiscoVnmc");
        _parameters.put("zoneId", "1");
        _parameters.put("physicalNetworkId", "100");
        _parameters.put("ip", "1.2.3.4");
        _parameters.put("username", "admin");
        _parameters.put("password", "pass");
        _parameters.put("guid", "e8e13097-0a08-4e82-b0af-1101589ec3b8");
        _parameters.put("numretries", "3");
        _parameters.put("timeout", "300");
    }

    @Test(expected=ConfigurationException.class)
    public void resourceConfigureFailure() throws ConfigurationException, ExecutionException {
        _resource.configure("CiscoVnmcResource", Collections.<String,Object>emptyMap());
    }

    @Test
    public void resourceConfigure() throws ConfigurationException {
        _resource.configure("CiscoVnmcResource", _parameters);
        assertTrue("CiscoVnmc".equals(_resource.getName()));
        assertTrue(_resource.getType() == Host.Type.ExternalFirewall);
    }

    @Test
    public void testInitialization() throws ConfigurationException {
        _resource.configure("CiscoVnmcResource", _parameters);
        StartupCommand[] sc = _resource.initialize();
        assertTrue(sc.length ==1);
        assertTrue("e8e13097-0a08-4e82-b0af-1101589ec3b8".equals(sc[0].getGuid()));
        assertTrue("CiscoVnmc".equals(sc[0].getName()));
        assertTrue("1".equals(sc[0].getDataCenter()));
    }

    @Test
    public void testPingCommandStatusOk() throws ConfigurationException, ExecutionException {
        _resource.configure("CiscoVnmcResource", _parameters);
        _resource.setConnection(_connection);
        when(_connection.login()).thenReturn(true);
        PingCommand ping = _resource.getCurrentStatus(1);
        assertTrue(ping != null);
        assertTrue(ping.getHostId() == 1);
        assertTrue(ping.getHostType() == Host.Type.ExternalFirewall);
    }

    @Test
    public void testPingCommandStatusFail() throws ConfigurationException, ExecutionException {
        _resource.configure("CiscoVnmcResource", _parameters);
        _resource.setConnection(_connection);
        when(_connection.login()).thenReturn(false);
        PingCommand ping = _resource.getCurrentStatus(1);
        assertTrue(ping == null);
    }

    @Test
    public void testSourceNat() throws ConfigurationException, ExecutionException, Exception {
        long vlanId = 123;
        IpAddressTO ip = new IpAddressTO(1, "1.2.3.4", true, false,
                false, null, "1.2.3.1", "255.255.255.0", null, null, false);
        SetSourceNatCommand cmd = new SetSourceNatCommand(ip, true);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "1.2.3.4/32");

        _resource.configure("CiscoVnmcResource", _parameters);
        _resource.setConnection(_connection);
        when(_connection.login()).thenReturn(true);
        when(_connection.createTenantVDCNatPolicySet((String)any())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatPolicy((String)any(), (String)any())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatPolicyRef((String)any(), (String)any())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatIpPool((String)any(), (String)any(), (String)any())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatRule((String)any(), (String)any(), (String)any(), (String)any())).thenReturn(true);
        when(_connection.associateNatPolicySet((String)any())).thenReturn(true);

        Answer answer = _resource.executeRequest(cmd);
        System.out.println(answer.getDetails());
        assertTrue(answer.getResult());
    }

}

