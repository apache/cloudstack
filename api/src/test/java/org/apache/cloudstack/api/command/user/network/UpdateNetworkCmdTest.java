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

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.db.EntityManager;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
public class UpdateNetworkCmdTest extends TestCase {

    @Mock
    NetworkService networkService;
    @Mock
    public EntityManager _entityMgr;
    private ResponseGenerator responseGenerator;
    @InjectMocks
    UpdateNetworkCmd cmd = new UpdateNetworkCmd();

    public void testGetId() {
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(cmd.getId(), id);
    }

    public void testGetNetworkName() {
        String name = "testNetwork";
        ReflectionTestUtils.setField(cmd, "name", name);
        Assert.assertEquals(cmd.getNetworkName(), name);
    }

    public void testGetDisplayText() {
        String displayText = "test network";
        ReflectionTestUtils.setField(cmd, "displayText", displayText);
        Assert.assertEquals(cmd.getDisplayText(), displayText);
    }

    public void testGetNetworkDomain() {
        String netDomain = "cs1cloud.internal";
        ReflectionTestUtils.setField(cmd, "networkDomain", netDomain);
        Assert.assertEquals(cmd.getNetworkDomain(), netDomain);
    }

    public void testGetNetworkOfferingId() {
        Long networkOfferingId = 1L;
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        Assert.assertEquals(cmd.getNetworkOfferingId(), networkOfferingId);
    }

    public void testGetChangeCidr() {
        Boolean changeCidr = true;
        ReflectionTestUtils.setField(cmd, "changeCidr", changeCidr);
        Assert.assertTrue(cmd.getChangeCidr());
    }

    public void testGetGuestVmCidr() {
        String guestVmCidr = "10.10.0.0/24";
        ReflectionTestUtils.setField(cmd, "guestVmCidr", guestVmCidr);
        Assert.assertEquals(cmd.getGuestVmCidr(), guestVmCidr);
    }

    public void testGetDisplayNetwork() {
        Boolean displayNetwork = true;
        ReflectionTestUtils.setField(cmd, "displayNetwork", displayNetwork);
        Assert.assertTrue(cmd.getDisplayNetwork());
    }

    public void testGetUpdateInSequenceIfNull() {
        Boolean updateInSequence = null;
        ReflectionTestUtils.setField(cmd, "updateInSequence", updateInSequence);
        Assert.assertFalse(cmd.getUpdateInSequence());
    }

    public void testGetUpdateInSequenceIfValidValuePassed() {
        Boolean updateInSequence = true;
        ReflectionTestUtils.setField(cmd, "updateInSequence", updateInSequence);
        Assert.assertTrue(cmd.getUpdateInSequence());
    }

    public void testGetForcedIfNull() {
        Boolean forced = null;
        ReflectionTestUtils.setField(cmd, "forced", forced);
        Assert.assertFalse(cmd.getUpdateInSequence());
    }

    public void testGetForcedIfValidValuePassed() {
        Boolean forced = true;
        ReflectionTestUtils.setField(cmd, "forced", forced);
        Assert.assertTrue(cmd.getForced());
    }

    public void testGetPublicMtu() {
        Integer publicMtu = 1450;
        ReflectionTestUtils.setField(cmd, "publicMtu", publicMtu);
        Assert.assertEquals(cmd.getPublicMtu(), publicMtu);
    }

    public void testGetPublicMtuIfNull() {
        Integer publicMtu = null;
        ReflectionTestUtils.setField(cmd, "publicMtu", publicMtu);
        Assert.assertNull(cmd.getPublicMtu());
    }

    public void testGetPrivateMtu() {
        Integer privateMtu = 1450;
        ReflectionTestUtils.setField(cmd, "privateMtu", privateMtu);
        Assert.assertEquals(cmd.getPrivateMtu(), privateMtu);
    }

    public void testGetPrivateMtuIfNull() {
        Integer privateMtu = null;
        ReflectionTestUtils.setField(cmd, "privateMtu", privateMtu);
        Assert.assertNull(cmd.getPrivateMtu());
    }

    public void testEventDescription() {
        long networkOfferingId = 1L;
        Network network = Mockito.mock(Network.class);
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        ReflectionTestUtils.setField(cmd, "networkOfferingId", networkOfferingId);
        ReflectionTestUtils.setField(cmd, "id", 1L);
        Mockito.when(networkService.getNetwork(Mockito.any(Long.class))).thenReturn(network);
        Mockito.when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        Mockito.when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(offering);
        String msg = cmd.getEventDescription();
        Assert.assertTrue(msg.contains("Updating network"));
    }

    public void testExecute() throws InsufficientCapacityException {
        long networkId = 1L;
        Integer publicmtu = 1200;
        ReflectionTestUtils.setField(cmd, "id", networkId);
        ReflectionTestUtils.setField(cmd, "publicMtu", publicmtu);
        Network network = Mockito.mock(Network.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        NetworkResponse response = Mockito.mock(NetworkResponse.class);
        response.setPublicMtu(publicmtu);
        Mockito.when(networkService.getNetwork(networkId)).thenReturn(network);
        Mockito.when(networkService.updateGuestNetwork(cmd)).thenReturn(network);
        cmd._responseGenerator = responseGenerator;
        Mockito.when(responseGenerator.createNetworkResponse(ResponseObject.ResponseView.Restricted, network)).thenReturn(response);
        cmd.execute();
        Mockito.verify(responseGenerator).createNetworkResponse(Mockito.any(ResponseObject.ResponseView.class), Mockito.any(Network.class));
        NetworkResponse actualResponse = (NetworkResponse) cmd.getResponseObject();

        Assert.assertEquals(response, actualResponse);
    }
}
