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
import org.apache.cloudstack.api.command.admin.region.AddRegionCmd;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.region.RegionService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class RegionCmdTest extends TestCase {

    private AddRegionCmd addRegionCmd;
    private ResponseGenerator responseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {

    	addRegionCmd = new AddRegionCmd() {

            @Override
            public Integer getId() {
                return 2;
            }

            @Override
            public String getRegionName() {
                return "APAC";
            }

        };
    }

    @Test
    public void testCreateSuccess() {

        RegionService regionService = Mockito.mock(RegionService.class);

        Region region = Mockito.mock(Region.class);
        Mockito.when(
        		regionService.addRegion(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
        		.thenReturn(region);

        addRegionCmd._regionService = regionService;
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        
        RegionResponse regionResponse = Mockito.mock(RegionResponse.class);

        Mockito.when(responseGenerator.createRegionResponse(region)).thenReturn(
        		regionResponse);
        
        addRegionCmd._responseGenerator = responseGenerator;                
        addRegionCmd.execute();

    }

    @Test
    public void testCreateFailure() {

        RegionService regionService = Mockito.mock(RegionService.class);

        Region region = Mockito.mock(Region.class);
        Mockito.when(
        		regionService.addRegion(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
        		.thenReturn(null);

        addRegionCmd._regionService = regionService;

        try {
        	addRegionCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to add Region",
                    exception.getDescription());
        }

    }    

}
