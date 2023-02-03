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

import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.host.AddHostCmd;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.resource.ResourceService;

public class AddHostCmdTest extends TestCase {

    private AddHostCmd addHostCmd;
    private ResourceService resourceService;
    private ResponseGenerator responseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    @Before
    public void setUp() {
        resourceService = Mockito.mock(ResourceService.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        addHostCmd = new AddHostCmd();
    }

    @Test
    public void testExecuteForEmptyResult() {
        addHostCmd._resourceService = resourceService;

        try {
            addHostCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add host", exception.getDescription());
        }

    }

    @Test
    public void testExecuteForNullResult() {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        addHostCmd._resourceService = resourceService;

        try {
            Mockito.when(resourceService.discoverHosts(addHostCmd)).thenReturn(null);
        } catch (InvalidParameterValueException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DiscoveryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            addHostCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add host", exception.getDescription());
        }

    }

    /*
     * @Test public void testExecuteForResult() throws Exception {
     *
     * addHostCmd._resourceService = resourceService;
     * addHostCmd._responseGenerator = responseGenerator; MockHost mockInstance
     * = new MockHost(); MockHost[] mockArray = new MockHost[]{mockInstance};
     * HostResponse responseHost = new HostResponse();
     * responseHost.setName("Test");
     * Mockito.when(resourceService.discoverHosts(addHostCmd
     * )).thenReturn(Arrays.asList(mockArray));
     * Mockito.when(responseGenerator.createHostResponse
     * (mockInstance)).thenReturn(responseHost); addHostCmd.execute();
     * Mockito.verify(responseGenerator).createHostResponse(mockInstance);
     * ListResponse<HostResponse> actualResponse =
     * ((ListResponse<HostResponse>)addHostCmd.getResponseObject());
     * Assert.assertEquals(responseHost, actualResponse.getResponses().get(0));
     * Assert.assertEquals("addhostresponse", actualResponse.getResponseName());
     * }
     */
    @Test
    public void testExecuteForResult() throws Exception {

        addHostCmd._resourceService = resourceService;
        addHostCmd._responseGenerator = responseGenerator;
        Host host = Mockito.mock(Host.class);
        Host[] mockArray = new Host[] {host};

        HostResponse responseHost = new HostResponse();
        responseHost.setName("Test");
        Mockito.doReturn(Arrays.asList(mockArray)).when(resourceService).discoverHosts(addHostCmd);
        Mockito.when(responseGenerator.createHostResponse(host)).thenReturn(responseHost);
        addHostCmd.execute();
        Mockito.verify(responseGenerator).createHostResponse(host);
        @SuppressWarnings("unchecked")
        ListResponse<HostResponse> actualResponse = ((ListResponse<HostResponse>)addHostCmd.getResponseObject());
        Assert.assertEquals(responseHost, actualResponse.getResponses().get(0));
        Assert.assertEquals("addhostresponse", actualResponse.getResponseName());

    }

    @Test
    public void testExecuteForDiscoveryException() {

        addHostCmd._resourceService = resourceService;

        try {
            Mockito.when(resourceService.discoverHosts(addHostCmd)).thenThrow(DiscoveryException.class);
        } catch (InvalidParameterValueException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (DiscoveryException e) {
            e.printStackTrace();
        }

        try {
            addHostCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertNull(exception.getDescription());
        }

    }

}
