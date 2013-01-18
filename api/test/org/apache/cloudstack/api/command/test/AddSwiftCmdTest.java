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
import org.apache.cloudstack.api.command.admin.swift.AddSwiftCmd;
import org.apache.cloudstack.api.response.SwiftResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.cloud.exception.DiscoveryException;
import com.cloud.resource.ResourceService;
import com.cloud.storage.Swift;

public class AddSwiftCmdTest extends TestCase {

    private AddSwiftCmd addSwiftCmd;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        addSwiftCmd = new AddSwiftCmd();
    }

    @Test
    public void testExecuteSuccess() {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        addSwiftCmd._resourceService = resourceService;

        Swift swift = Mockito.mock(Swift.class);

        try {
            Mockito.when(resourceService.discoverSwift(addSwiftCmd))
                    .thenReturn(swift);
        } catch (DiscoveryException e) {
            e.printStackTrace();
        }

        ResponseGenerator responseGenerator = Mockito
                .mock(ResponseGenerator.class);
        addSwiftCmd._responseGenerator = responseGenerator;

        SwiftResponse swiftResponse = Mockito.mock(SwiftResponse.class);

        Mockito.when(responseGenerator.createSwiftResponse(swift)).thenReturn(
                swiftResponse);

        addSwiftCmd.execute();

    }

    @Test
    public void testExecuteFailure() {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        addSwiftCmd._resourceService = resourceService;
        try {
            Mockito.when(resourceService.discoverSwift(addSwiftCmd))
                    .thenReturn(null);
        } catch (DiscoveryException e) {
            e.printStackTrace();
        }

        try {
            addSwiftCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add Swift",
                    exception.getDescription());
        }

    }

}
