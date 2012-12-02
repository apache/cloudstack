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
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.agent.api.CreateLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalRouterAnswer;
import com.cloud.agent.api.DeleteLogicalRouterCommand;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortAnswer;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.host.Host;
import com.cloud.network.nicira.Attachment;
import com.cloud.network.nicira.ControlClusterStatus;
import com.cloud.network.nicira.LogicalRouterConfig;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NatRule;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.NiciraNvpList;

public class NiciraNvpResourceTest {
	NiciraNvpApi _nvpApi = mock(NiciraNvpApi.class);
	NiciraNvpResource _resource;
	Map<String,Object> _parameters;
	
	@Before
	public void setUp() throws ConfigurationException {
		_resource = new NiciraNvpResource() {
			protected NiciraNvpApi createNiciraNvpApi() {
				return _nvpApi;
			}
		};

		_parameters = new HashMap<String,Object>();
		_parameters.put("name","nvptestdevice");
		_parameters.put("ip","127.0.0.1");
		_parameters.put("adminuser","adminuser");
		_parameters.put("guid", "aaaaa-bbbbb-ccccc");
		_parameters.put("zoneId", "blublub");
		_parameters.put("adminpass","adminpass");
	}
	
	@Test (expected=ConfigurationException.class)
	public void resourceConfigureFailure() throws ConfigurationException {
		_resource.configure("NiciraNvpResource", Collections.<String,Object>emptyMap());
	}
	
	@Test 
	public void resourceConfigure() throws ConfigurationException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		verify(_nvpApi).setAdminCredentials("adminuser", "adminpass");
		verify(_nvpApi).setControllerAddress("127.0.0.1");
		
		assertTrue("nvptestdevice".equals(_resource.getName()));
		
		/* Pretty lame test, but here to assure this plugin fails 
		 * if the type name ever changes from L2Networking
		 */ 
		assertTrue(_resource.getType() == Host.Type.L2Networking);
	}
	
	@Test
	public void testInitialization() throws ConfigurationException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		StartupCommand[] sc = _resource.initialize();
		assertTrue(sc.length ==1);
		assertTrue("aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
		assertTrue("nvptestdevice".equals(sc[0].getName()));
		assertTrue("blublub".equals(sc[0].getDataCenter()));
	}
	
	@Test
	public void testPingCommandStatusOk() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		ControlClusterStatus ccs = mock(ControlClusterStatus.class);
		when(ccs.getClusterStatus()).thenReturn("stable");
		when(_nvpApi.getControlClusterStatus()).thenReturn(ccs);
		
		PingCommand ping = _resource.getCurrentStatus(42);
		assertTrue(ping != null);
		assertTrue(ping.getHostId() == 42);
		assertTrue(ping.getHostType() == Host.Type.L2Networking);		
	}

	@Test
	public void testPingCommandStatusFail() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		ControlClusterStatus ccs = mock(ControlClusterStatus.class);
		when(ccs.getClusterStatus()).thenReturn("unstable");
		when(_nvpApi.getControlClusterStatus()).thenReturn(ccs);
		
		PingCommand ping = _resource.getCurrentStatus(42);
		assertTrue(ping == null);
	}

	@Test
	public void testPingCommandStatusApiException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		ControlClusterStatus ccs = mock(ControlClusterStatus.class);
		when(ccs.getClusterStatus()).thenReturn("unstable");
		when(_nvpApi.getControlClusterStatus()).thenThrow(new NiciraNvpApiException());
		
		PingCommand ping = _resource.getCurrentStatus(42);
		assertTrue(ping == null);
	}
	
	@Test
	public void testRetries() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalSwitch ls = mock(LogicalSwitch.class);
		when(ls.getUuid()).thenReturn("cccc").thenReturn("cccc");
		when(_nvpApi.createLogicalSwitch((LogicalSwitch) any())).thenThrow(new NiciraNvpApiException()).thenThrow(new NiciraNvpApiException()).thenReturn(ls);
		
		CreateLogicalSwitchCommand clsc = new CreateLogicalSwitchCommand((String)_parameters.get("guid"), "stt", "loigicalswitch","owner");
		CreateLogicalSwitchAnswer clsa = (CreateLogicalSwitchAnswer) _resource.executeRequest(clsc);
		assertTrue(clsa.getResult());
	}
	
	@Test
	public void testCreateLogicalSwitch() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalSwitch ls = mock(LogicalSwitch.class);
		when(ls.getUuid()).thenReturn("cccc").thenReturn("cccc");
		when(_nvpApi.createLogicalSwitch((LogicalSwitch) any())).thenReturn(ls);
		
		CreateLogicalSwitchCommand clsc = new CreateLogicalSwitchCommand((String)_parameters.get("guid"), "stt", "loigicalswitch","owner");
		CreateLogicalSwitchAnswer clsa = (CreateLogicalSwitchAnswer) _resource.executeRequest(clsc);
		assertTrue(clsa.getResult());
		assertTrue("cccc".equals(clsa.getLogicalSwitchUuid()));
	}

	@Test
	public void testCreateLogicalSwitchApiException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalSwitch ls = mock(LogicalSwitch.class);
		when(ls.getUuid()).thenReturn("cccc").thenReturn("cccc");
		when(_nvpApi.createLogicalSwitch((LogicalSwitch) any())).thenThrow(new NiciraNvpApiException());
		
		CreateLogicalSwitchCommand clsc = new CreateLogicalSwitchCommand((String)_parameters.get("guid"), "stt", "loigicalswitch","owner");
		CreateLogicalSwitchAnswer clsa = (CreateLogicalSwitchAnswer) _resource.executeRequest(clsc);
		assertFalse(clsa.getResult());
	}

	@Test
	public void testDeleteLogicalSwitch() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		DeleteLogicalSwitchCommand dlsc = new DeleteLogicalSwitchCommand("cccc");
		DeleteLogicalSwitchAnswer dlsa = (DeleteLogicalSwitchAnswer) _resource.executeRequest(dlsc);
		assertTrue(dlsa.getResult());
	}

	@Test
	public void testDeleteLogicalSwitchApiException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		doThrow(new NiciraNvpApiException()).when(_nvpApi).deleteLogicalSwitch((String)any());
		
		DeleteLogicalSwitchCommand dlsc = new DeleteLogicalSwitchCommand("cccc");
		DeleteLogicalSwitchAnswer dlsa = (DeleteLogicalSwitchAnswer) _resource.executeRequest(dlsc);
		assertFalse(dlsa.getResult());
	}

	@Test
	public void testCreateLogicalSwitchPort() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
		when(lsp.getUuid()).thenReturn("eeee");
		when(_nvpApi.createLogicalSwitchPort(eq("cccc"), (LogicalSwitchPort) any())).thenReturn(lsp);
		
		CreateLogicalSwitchPortCommand clspc = new CreateLogicalSwitchPortCommand("cccc", "dddd", "owner", "nicname");
		CreateLogicalSwitchPortAnswer clspa = (CreateLogicalSwitchPortAnswer) _resource.executeRequest(clspc);
		assertTrue(clspa.getResult());
		assertTrue("eeee".equals(clspa.getLogicalSwitchPortUuid()));
		
	}

	@Test
	public void testCreateLogicalSwitchPortApiExceptionInCreate() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
		when(lsp.getUuid()).thenReturn("eeee");
		when(_nvpApi.createLogicalSwitchPort(eq("cccc"), (LogicalSwitchPort) any())).thenThrow(new NiciraNvpApiException());
		
		CreateLogicalSwitchPortCommand clspc = new CreateLogicalSwitchPortCommand("cccc", "dddd", "owner", "nicname");
		CreateLogicalSwitchPortAnswer clspa = (CreateLogicalSwitchPortAnswer) _resource.executeRequest(clspc);
		assertFalse(clspa.getResult());		
	}

	@Test
	public void testCreateLogicalSwitchPortApiExceptionInModify() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
		when(lsp.getUuid()).thenReturn("eeee");
		when(_nvpApi.createLogicalSwitchPort(eq("cccc"), (LogicalSwitchPort) any())).thenReturn(lsp);
		doThrow(new NiciraNvpApiException()).when(_nvpApi).modifyLogicalSwitchPortAttachment((String)any(), (String)any(), (Attachment)any());
		
		
		CreateLogicalSwitchPortCommand clspc = new CreateLogicalSwitchPortCommand("cccc", "dddd", "owner", "nicname");
		CreateLogicalSwitchPortAnswer clspa = (CreateLogicalSwitchPortAnswer) _resource.executeRequest(clspc);
		assertFalse(clspa.getResult());		
		verify(_nvpApi, atLeastOnce()).deleteLogicalSwitchPort((String) any(),  (String) any());
	}

	@Test
	public void testDeleteLogicalSwitchPortException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		doThrow(new NiciraNvpApiException()).when(_nvpApi).deleteLogicalSwitchPort((String) any(), (String) any());
		DeleteLogicalSwitchPortAnswer dlspa = (DeleteLogicalSwitchPortAnswer) _resource.executeRequest(new DeleteLogicalSwitchPortCommand("aaaa","bbbb"));
		assertFalse(dlspa.getResult());
	}
	
	@Test
	public void testUpdateLogicalSwitchPortException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		doThrow(new NiciraNvpApiException()).when(_nvpApi).modifyLogicalSwitchPortAttachment((String) any(), (String) any(), (Attachment) any());
		UpdateLogicalSwitchPortAnswer dlspa = (UpdateLogicalSwitchPortAnswer) _resource.executeRequest(
				new UpdateLogicalSwitchPortCommand("aaaa","bbbb","cccc","owner","nicname"));
		assertFalse(dlspa.getResult());
	}
	
	@Test
	public void testFindLogicalSwitchPort() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		@SuppressWarnings("unchecked")
		NiciraNvpList<LogicalSwitchPort> lspl = (NiciraNvpList<LogicalSwitchPort>)mock(NiciraNvpList.class);
		when(lspl.getResultCount()).thenReturn(1); 
		when(_nvpApi.findLogicalSwitchPortsByUuid("aaaa", "bbbb")).thenReturn(lspl);
		
		FindLogicalSwitchPortAnswer flspa = (FindLogicalSwitchPortAnswer) _resource.executeRequest(new FindLogicalSwitchPortCommand("aaaa", "bbbb"));
		assertTrue(flspa.getResult());
	}

	@Test
	public void testFindLogicalSwitchPortNotFound() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		@SuppressWarnings("unchecked")
		NiciraNvpList<LogicalSwitchPort> lspl = (NiciraNvpList<LogicalSwitchPort>)mock(NiciraNvpList.class);
		when(lspl.getResultCount()).thenReturn(0); 
		when(_nvpApi.findLogicalSwitchPortsByUuid("aaaa", "bbbb")).thenReturn(lspl);
		
		FindLogicalSwitchPortAnswer flspa = (FindLogicalSwitchPortAnswer) _resource.executeRequest(new FindLogicalSwitchPortCommand("aaaa", "bbbb"));
		assertFalse(flspa.getResult());
	}

	@Test
	public void testFindLogicalSwitchPortApiException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		when(_nvpApi.findLogicalSwitchPortsByUuid("aaaa", "bbbb")).thenThrow(new NiciraNvpApiException());
		
		FindLogicalSwitchPortAnswer flspa = (FindLogicalSwitchPortAnswer) _resource.executeRequest(new FindLogicalSwitchPortCommand("aaaa", "bbbb"));
		assertFalse(flspa.getResult());
	}
	
	@Test
	public void testCreateLogicalRouter() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalRouterConfig lrc = mock(LogicalRouterConfig.class);
		LogicalRouterPort lrp = mock(LogicalRouterPort.class);
		LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
		when(lrc.getUuid()).thenReturn("ccccc");
		when(lrp.getUuid()).thenReturn("ddddd").thenReturn("eeeee");
		when(lsp.getUuid()).thenReturn("fffff");
		when(_nvpApi.createLogicalRouter((LogicalRouterConfig)any())).thenReturn(lrc);
		when(_nvpApi.createLogicalRouterPort(eq("ccccc"), (LogicalRouterPort)any())).thenReturn(lrp);
		when(_nvpApi.createLogicalSwitchPort(eq("bbbbb"), (LogicalSwitchPort)any())).thenReturn(lsp);
		CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
		CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) _resource.executeRequest(clrc);
		
		assertTrue(clra.getResult());
		assertTrue("ccccc".equals(clra.getLogicalRouterUuid()));
		verify(_nvpApi, atLeast(1)).createLogicalRouterNatRule((String) any(), (NatRule) any());
	}

	@Test
	public void testCreateLogicalRouterApiException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		when(_nvpApi.createLogicalRouter((LogicalRouterConfig)any())).thenThrow(new NiciraNvpApiException());
		CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
		CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) _resource.executeRequest(clrc);
		
		assertFalse(clra.getResult());
	}

	@Test
	public void testCreateLogicalRouterApiExceptionRollbackRouter() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalRouterConfig lrc = mock(LogicalRouterConfig.class);
		when(lrc.getUuid()).thenReturn("ccccc");
		when(_nvpApi.createLogicalRouter((LogicalRouterConfig)any())).thenReturn(lrc);
		when(_nvpApi.createLogicalRouterPort(eq("ccccc"), (LogicalRouterPort)any())).thenThrow(new NiciraNvpApiException());
		CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
		CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) _resource.executeRequest(clrc);
		
		assertFalse(clra.getResult());
		verify(_nvpApi, atLeast(1)).deleteLogicalRouter(eq("ccccc"));
	}

	@Test
	public void testCreateLogicalRouterApiExceptionRollbackRouterAndSwitchPort() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		LogicalRouterConfig lrc = mock(LogicalRouterConfig.class);
		LogicalRouterPort lrp = mock(LogicalRouterPort.class);
		LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
		when(lrc.getUuid()).thenReturn("ccccc");
		when(lrp.getUuid()).thenReturn("ddddd").thenReturn("eeeee");
		when(lsp.getUuid()).thenReturn("fffff");
		when(_nvpApi.createLogicalRouter((LogicalRouterConfig)any())).thenReturn(lrc);
		when(_nvpApi.createLogicalRouterPort(eq("ccccc"), (LogicalRouterPort)any())).thenReturn(lrp);
		when(_nvpApi.createLogicalSwitchPort(eq("bbbbb"), (LogicalSwitchPort)any())).thenReturn(lsp);
		when(_nvpApi.createLogicalRouterNatRule((String) any(), (NatRule)any())).thenThrow(new NiciraNvpApiException());
		CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
		CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) _resource.executeRequest(clrc);
		
		assertFalse(clra.getResult());
		verify(_nvpApi, atLeast(1)).deleteLogicalRouter(eq("ccccc"));
		verify(_nvpApi, atLeast(1)).deleteLogicalSwitchPort(eq("bbbbb"), eq("fffff"));
	}
	
	@Test
	public void testDeleteLogicalRouterApiException() throws ConfigurationException,NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		doThrow(new NiciraNvpApiException()).when(_nvpApi).deleteLogicalRouter(eq("aaaaa"));
		DeleteLogicalRouterAnswer dlspa = (DeleteLogicalRouterAnswer) _resource.executeRequest(new DeleteLogicalRouterCommand("aaaaa"));
		assertFalse(dlspa.getResult());		
	}
	
	@Test
	public void testConfigurePublicIpsOnLogicalRouterApiException() throws ConfigurationException, NiciraNvpApiException {
		_resource.configure("NiciraNvpResource", _parameters);
		
		ConfigurePublicIpsOnLogicalRouterCommand cmd = mock(ConfigurePublicIpsOnLogicalRouterCommand.class);
		@SuppressWarnings("unchecked")
		NiciraNvpList<LogicalRouterPort> list = mock(NiciraNvpList.class);
		
		when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");
		when(cmd.getL3GatewayServiceUuid()).thenReturn("bbbbb");
		doThrow(new NiciraNvpApiException()).when(_nvpApi).modifyLogicalRouterPort((String) any(), (LogicalRouterPort) any());
		when(_nvpApi.findLogicalRouterPortByGatewayServiceUuid("aaaaa","bbbbb")).thenReturn(list);
		
		ConfigurePublicIpsOnLogicalRouterAnswer answer = 
				(ConfigurePublicIpsOnLogicalRouterAnswer) _resource.executeRequest(cmd);
		assertFalse(answer.getResult());
		
	}
}

