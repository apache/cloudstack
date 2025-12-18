//
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
//

package com.cloud.network.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.BcfAnswer;
import com.cloud.agent.api.CreateBcfRouterCommand;
import com.cloud.agent.api.CreateBcfSegmentCommand;
import com.cloud.agent.api.CreateBcfAttachmentCommand;
import com.cloud.agent.api.CreateBcfStaticNatCommand;
import com.cloud.agent.api.DeleteBcfSegmentCommand;
import com.cloud.agent.api.DeleteBcfAttachmentCommand;
import com.cloud.agent.api.DeleteBcfStaticNatCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.SyncBcfTopologyCommand;
import com.cloud.agent.api.UpdateBcfAttachmentCommand;
import com.cloud.agent.api.UpdateBcfRouterCommand;
import com.cloud.host.Host;
import com.cloud.network.bigswitch.BigSwitchBcfApi;
import com.cloud.network.bigswitch.BigSwitchBcfApiException;
import com.cloud.network.bigswitch.Capabilities;
import com.cloud.network.bigswitch.ControlClusterStatus;
import com.cloud.network.bigswitch.FloatingIpData;
import com.cloud.network.bigswitch.NetworkData;
import com.cloud.network.bigswitch.AttachmentData;
import com.cloud.network.bigswitch.RouterData;
import com.cloud.network.bigswitch.TopologyData;

public class BigSwitchBcfResourceTest {
    BigSwitchBcfApi _bigswitchBcfApi = mock(BigSwitchBcfApi.class);
    BigSwitchBcfResource _resource;
    Map<String, Object> _parameters;

    String bcfAddress = "127.0.0.1";
    String bcfUserName = "myname";
    String bcfPassword = "mypassword";

    @Before
    public void setUp() throws ConfigurationException {
        _resource = new BigSwitchBcfResource() {
            @Override
            protected BigSwitchBcfApi createBigSwitchBcfApi() {
                return _bigswitchBcfApi;
            }
        };

        _parameters = new HashMap<String, Object>();
        _parameters.put("name", "bigswitchbcftestdevice");
        _parameters.put("hostname", bcfAddress);
        _parameters.put("guid", "aaaaa-bbbbb-ccccc");
        _parameters.put("zoneId", "blublub");
        _parameters.put("username", bcfUserName);
        _parameters.put("password", bcfPassword);
    }

    @Test(expected = ConfigurationException.class)
    public void resourceConfigureFailure() throws ConfigurationException {
        _resource.configure("BigSwitchBcfResource", Collections.<String, Object> emptyMap());
    }

    @Test
    public void resourceConfigure() throws ConfigurationException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        verify(_bigswitchBcfApi).setControllerAddress(bcfAddress);
        verify(_bigswitchBcfApi).setControllerUsername(bcfUserName);
        verify(_bigswitchBcfApi).setControllerPassword(bcfPassword);

        assertTrue("bigswitchbcftestdevice".equals(_resource.getName()));

        /* Pretty lame test, but here to assure this plugin fails
         * if the type name ever changes from L2Networking
         */
        assertTrue(_resource.getType() == Host.Type.L2Networking);
    }

    @Test
    public void testInitialization() throws ConfigurationException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        StartupCommand[] sc = _resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
        assertTrue("bigswitchbcftestdevice".equals(sc[0].getName()));
        assertTrue("blublub".equals(sc[0].getDataCenter()));
    }

    @Test
    public void testPingCommandStatusOk() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        ControlClusterStatus ccs = mock(ControlClusterStatus.class);
        when(ccs.getStatus()).thenReturn(true);
        when(_bigswitchBcfApi.getControlClusterStatus()).thenReturn(ccs);

        Capabilities cap = mock(Capabilities.class);
        when(_bigswitchBcfApi.getCapabilities()).thenReturn(cap);

        PingCommand ping = _resource.getCurrentStatus(42);
        assertTrue(ping != null);
        assertTrue(ping.getHostId() == 42);
        assertTrue(ping.getHostType() == Host.Type.L2Networking);
    }

    @Test
    public void testPingCommandStatusFail() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        ControlClusterStatus ccs = mock(ControlClusterStatus.class);
        when(ccs.getStatus()).thenReturn(false);
        when(_bigswitchBcfApi.getControlClusterStatus()).thenReturn(ccs);

        PingCommand ping = _resource.getCurrentStatus(42);
        assertTrue(ping == null);
    }

    @Test
    public void testPingCommandStatusApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        ControlClusterStatus ccs = mock(ControlClusterStatus.class);
        when(ccs.getStatus()).thenReturn(false);
        when(_bigswitchBcfApi.getControlClusterStatus()).thenThrow(new BigSwitchBcfApiException());

        PingCommand ping = _resource.getCurrentStatus(42);
        assertTrue(ping == null);
    }

    @Test
    public void testCreateNetworkRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        NetworkData networkdata = mock(NetworkData.class);
        NetworkData.Network network = mock(NetworkData.Network.class);
        when(networkdata.getNetwork()).thenReturn(network);
        when(network.getId()).thenReturn("cccc");
        when(_bigswitchBcfApi.createNetwork((NetworkData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        CreateBcfSegmentCommand cmd = new CreateBcfSegmentCommand("tenantid", "tenantname",
                (String)_parameters.get("guid"), "networkName", 0);
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testCreateNetworkApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        NetworkData networkdata = mock(NetworkData.class);
        NetworkData.Network network = mock(NetworkData.Network.class);
        when(networkdata.getNetwork()).thenReturn(network);
        when(network.getId()).thenReturn("cccc");
        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).createNetwork((NetworkData)any());

        CreateBcfSegmentCommand cmd = new CreateBcfSegmentCommand("tenantid", "tenantname",
                (String)_parameters.get("guid"), "networkName", 0);
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).createNetwork((NetworkData)any());
    }

    @Test
    public void testDeleteNetworkRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);
        when(_bigswitchBcfApi.deleteNetwork((String)any(), (String)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        DeleteBcfSegmentCommand cmd = new DeleteBcfSegmentCommand("tenantid", "networkid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testDeleteNetworkApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).deleteNetwork((String)any(), (String)any());

        DeleteBcfSegmentCommand cmd = new DeleteBcfSegmentCommand("tenantid", "networkid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).deleteNetwork((String)any(), (String)any());
    }

    @Test
    public void testCreateAttachmentRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        AttachmentData attachmentData = mock(AttachmentData.class);
        AttachmentData.Attachment attachment = mock(AttachmentData.Attachment.class);
        when(attachmentData.getAttachment()).thenReturn(attachment);
        when(attachment.getId()).thenReturn("eeee");
        when(_bigswitchBcfApi.createAttachment((String)any(), (String)any(), (AttachmentData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        CreateBcfAttachmentCommand cmd = new CreateBcfAttachmentCommand("tenantid", "tenantname",
                "networkid", "portid", "nicId", 100, "1.2.3.4", "aa:bb:cc:dd:ee:ff");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testCreateAttachmentApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        AttachmentData attachmentData = mock(AttachmentData.class);
        AttachmentData.Attachment attachment = mock(AttachmentData.Attachment.class);
        when(attachmentData.getAttachment()).thenReturn(attachment);
        when(attachment.getId()).thenReturn("eeee");
        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).createAttachment((String)any(), (String)any(), (AttachmentData)any());

        CreateBcfAttachmentCommand cmd = new CreateBcfAttachmentCommand("tenantid", "tenantname",
                "networkid", "portid", "nicId", 100, "1.2.3.4", "aa:bb:cc:dd:ee:ff");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).createAttachment((String)any(), (String)any(), (AttachmentData)any());
    }

    @Test
    public void testDeleteAttachmentRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        when(_bigswitchBcfApi.deleteAttachment((String)any(), (String)any(), (String)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(new DeleteBcfAttachmentCommand("networkId", "portid", "tenantid"));
        assertTrue(ans.getResult());
    }

    @Test
    public void testDeleteAttachmentException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).deleteAttachment((String)any(), (String)any(), (String)any());
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(new DeleteBcfAttachmentCommand("networkId", "portid", "tenantid"));
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).deleteAttachment((String)any(), (String)any(), (String)any());
    }

    @Test
    public void testUpdateAttachmentRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        when(_bigswitchBcfApi.modifyAttachment((String)any(), (String)any(), (AttachmentData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(new UpdateBcfAttachmentCommand("networkId", "portId", "tenantId", "portname"));
        assertTrue(ans.getResult());
    }

    @Test
    public void testUpdateAttachmentException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).modifyAttachment((String)any(), (String)any(), (AttachmentData)any());
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(new UpdateBcfAttachmentCommand("networkId", "portId", "tenantId", "portname"));
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).modifyAttachment((String)any(), (String)any(), (AttachmentData)any());
    }

    @Test
    public void testCreateStaticNatRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        when(_bigswitchBcfApi.createFloatingIp((String)any(), (FloatingIpData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        CreateBcfStaticNatCommand cmd = new CreateBcfStaticNatCommand("tenantid",
                "networkid", "192.168.0.10", "10.4.4.100", "90:b1:1c:49:d8:56");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testCreateStaticNatApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).createFloatingIp((String)any(), (FloatingIpData)any());

        CreateBcfStaticNatCommand cmd = new CreateBcfStaticNatCommand("tenantid",
                "networkid", "192.168.0.10", "10.4.4.100", "90:b1:1c:49:d8:56");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).createFloatingIp((String)any(), (FloatingIpData)any());
    }

    @Test
    public void testDeleteStaticNatRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        when(_bigswitchBcfApi.deleteFloatingIp((String)any(), (String)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        DeleteBcfStaticNatCommand cmd = new DeleteBcfStaticNatCommand("tenantid",
                "10.4.4.100");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testDeleteStaticNatApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).deleteFloatingIp((String)any(), (String)any());

        DeleteBcfStaticNatCommand cmd = new DeleteBcfStaticNatCommand("tenantid",
                "10.4.4.100");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).deleteFloatingIp((String)any(), (String)any());
    }

    @Test
    public void testCreateRouterRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        when(_bigswitchBcfApi.createRouter((String)any(), (RouterData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        CreateBcfRouterCommand cmd = new CreateBcfRouterCommand("tenantid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testCreateRouterApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).createRouter((String)any(), (RouterData)any());

        CreateBcfRouterCommand cmd = new CreateBcfRouterCommand("tenantid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).createRouter((String)any(), (RouterData)any());
    }

    @Test
    public void testCreateSourceNatRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        when(_bigswitchBcfApi.modifyRouter((String)any(), (RouterData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        UpdateBcfRouterCommand cmd = new UpdateBcfRouterCommand("tenantid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testCreateSourceNatApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).modifyRouter((String)any(), (RouterData)any());

        UpdateBcfRouterCommand cmd = new UpdateBcfRouterCommand("tenantid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).modifyRouter((String)any(), (RouterData)any());
    }

    @Test
    public void testDeleteSourceNatRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        when(_bigswitchBcfApi.modifyRouter((String)any(), (RouterData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());

        UpdateBcfRouterCommand cmd = new UpdateBcfRouterCommand("tenantid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertTrue(ans.getResult());
    }

    @Test
    public void testDeleteSourceNatApiException() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);

        doThrow(new BigSwitchBcfApiException()).when(_bigswitchBcfApi).modifyRouter((String)any(), (RouterData)any());

        UpdateBcfRouterCommand cmd = new UpdateBcfRouterCommand("tenantid");
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(cmd);
        assertFalse(ans.getResult());
        verify(_bigswitchBcfApi, times(3)).modifyRouter((String)any(), (RouterData)any());
    }

    @Test
    public void testSyncTopologyRetryOnce() throws ConfigurationException, BigSwitchBcfApiException {
        _resource.configure("BigSwitchBcfResource", _parameters);
        _resource.setTopology(new TopologyData());

        when(_bigswitchBcfApi.syncTopology((TopologyData)any())).thenThrow(new BigSwitchBcfApiException())
        .thenReturn(UUID.randomUUID().toString());
        BcfAnswer ans = (BcfAnswer)_resource.executeRequest(new SyncBcfTopologyCommand(true, false));
        assertTrue(ans.getResult());
    }

}
