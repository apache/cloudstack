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


import com.cloud.server.ManagementService;
import com.cloud.utils.Pair;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.admin.config.ListCfgsByCmd;
import org.apache.cloudstack.api.command.user.config.ListCapabilitiesCmd;
import org.apache.cloudstack.api.response.CapabilitiesResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.config.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCapabilitiesCmdTest extends TestCase {

    private ListCapabilitiesCmd listCapabilitiesCmd;
    private ManagementService mgr;
    private ResponseGenerator responseGenerator;

    @Override
    @Before
    public void setUp() {
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        mgr = Mockito.mock(ManagementService.class);
        listCapabilitiesCmd = new ListCapabilitiesCmd();
    }

    @Test
    public void testCreateSuccess() {

        listCapabilitiesCmd._mgr = mgr;
        listCapabilitiesCmd._responseGenerator = responseGenerator;

        Map<String, Object> result = new HashMap<String, Object>();

        try {
            Mockito.when(mgr.listCapabilities(listCapabilitiesCmd)).thenReturn(result);
        } catch (Exception e) {
            Assert.fail("Received exception when success expected " + e.getMessage());
        }

        CapabilitiesResponse capResponse = new CapabilitiesResponse();
        Mockito.when(responseGenerator.createCapabilitiesResponse(result)).thenReturn(capResponse);

        listCapabilitiesCmd.execute();
        Mockito.verify(responseGenerator).createCapabilitiesResponse(result);

        CapabilitiesResponse actualResponse = (CapabilitiesResponse) listCapabilitiesCmd.getResponseObject();

        Assert.assertEquals(capResponse, actualResponse);
    }

}