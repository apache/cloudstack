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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.host.AddSecondaryStorageCmd;
import org.apache.cloudstack.api.response.HostResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.cloud.host.Host;
import com.cloud.resource.ResourceService;

import edu.emory.mathcs.backport.java.util.Arrays;

public class AddSecondaryStorageCmdTest extends TestCase {

    private AddSecondaryStorageCmd addSecondaryStorageCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        addSecondaryStorageCmd = new AddSecondaryStorageCmd() {
        };

    }

    @Test
    public void testExecuteForResult() throws Exception {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        addSecondaryStorageCmd._resourceService = resourceService;

        Host host = Mockito.mock(Host.class);
        Host[] mockHosts = new Host[] { host };

        Mockito.when(resourceService.discoverHosts(addSecondaryStorageCmd))
                .thenReturn(Arrays.asList(mockHosts));

        ResponseGenerator responseGenerator = Mockito
                .mock(ResponseGenerator.class);
        addSecondaryStorageCmd._responseGenerator = responseGenerator;

        HostResponse responseHost = new HostResponse();
        responseHost.setName("Test");

        Mockito.when(responseGenerator.createHostResponse(host)).thenReturn(
                responseHost);

        addSecondaryStorageCmd.execute();

        Mockito.verify(responseGenerator).createHostResponse(host);

        HostResponse actualResponse = (HostResponse) addSecondaryStorageCmd
                .getResponseObject();

        Assert.assertEquals(responseHost, actualResponse);
        Assert.assertEquals("addsecondarystorageresponse",
                actualResponse.getResponseName());

    }

    @Test
    public void testExecuteForEmptyResult() throws Exception {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        addSecondaryStorageCmd._resourceService = resourceService;

        Host[] mockHosts = new Host[] {};

        Mockito.when(resourceService.discoverHosts(addSecondaryStorageCmd))
                .thenReturn(Arrays.asList(mockHosts));

        try {
            addSecondaryStorageCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add secondary storage",
                    exception.getDescription());
        }

    }

    @Test
    public void testExecuteForNullResult() throws Exception {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        addSecondaryStorageCmd._resourceService = resourceService;

        Mockito.when(resourceService.discoverHosts(addSecondaryStorageCmd))
                .thenReturn(null);

        try {
            addSecondaryStorageCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add secondary storage",
                    exception.getDescription());
        }

    }

}
