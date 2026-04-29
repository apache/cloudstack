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
import org.apache.cloudstack.api.command.admin.cluster.AddClusterCmd;

import com.cloud.exception.DiscoveryException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceService;

public class AddClusterCmdTest extends TestCase {

    private AddClusterCmd addClusterCmd;
    private ResourceService resourceService;
    private ResponseGenerator responseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    @Before
    public void setUp() {
        /*
         * resourceService = Mockito.mock(ResourceService.class);
         * responseGenerator = Mockito.mock(ResponseGenerator.class);
         */addClusterCmd = new AddClusterCmd() {
        };
    }

    @Test
    public void testExecuteForNullResult() {

        ResourceService resourceService = Mockito.mock(ResourceService.class);

        try {
            Mockito.when(resourceService.discoverCluster(addClusterCmd)).thenReturn(null);
        } catch (ResourceInUseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DiscoveryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        addClusterCmd._resourceService = resourceService;

        try {
            addClusterCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add cluster", exception.getDescription());
        }

    }

    @Test
    public void testExecuteForEmptyResult() {

        ResourceService resourceService = Mockito.mock(ResourceService.class);
        addClusterCmd._resourceService = resourceService;

        try {
            addClusterCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add cluster", exception.getDescription());
        }

    }

    @Test
    public void testExecuteForResult() throws Exception {

        resourceService = Mockito.mock(ResourceService.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);

        addClusterCmd._resourceService = resourceService;
        addClusterCmd._responseGenerator = responseGenerator;

        Cluster cluster = Mockito.mock(Cluster.class);
        Cluster[] clusterArray = new Cluster[] {cluster};

        Mockito.doReturn(Arrays.asList(clusterArray)).when(resourceService).discoverCluster(addClusterCmd);

        addClusterCmd.execute();

    }

}
