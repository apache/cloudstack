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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.admin.config.ListCfgsByCmd;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.config.Configuration;

import com.cloud.server.ManagementService;
import com.cloud.utils.Pair;

public class ListCfgCmdTest extends TestCase {

    private ListCfgsByCmd listCfgsByCmd;
    private ManagementService mgr;
    private ResponseGenerator responseGenerator;

    @Override
    @Before
    public void setUp() {
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        mgr = Mockito.mock(ManagementService.class);
        listCfgsByCmd = new ListCfgsByCmd();
    }

    @Test
    public void testCreateSuccess() {

        Configuration cfg = Mockito.mock(Configuration.class);
        listCfgsByCmd._mgr = mgr;
        listCfgsByCmd._responseGenerator = responseGenerator;

        List<Configuration> configList = new ArrayList<Configuration>();
        configList.add(cfg);

        Pair<List<? extends Configuration>, Integer> result = new Pair<List<? extends Configuration>, Integer>(configList, 1);

        try {
            Mockito.when(mgr.searchForConfigurations(listCfgsByCmd)).thenReturn(result);
        } catch (Exception e) {
            Assert.fail("Received exception when success expected " + e.getMessage());
        }
        ConfigurationResponse cfgResponse = new ConfigurationResponse();
        cfgResponse.setName("Test case");
        Mockito.when(responseGenerator.createConfigurationResponse(cfg)).thenReturn(cfgResponse);

        listCfgsByCmd.execute();
        Mockito.verify(responseGenerator).createConfigurationResponse(cfg);

        ListResponse<ConfigurationResponse> actualResponse = (ListResponse<ConfigurationResponse>)listCfgsByCmd.getResponseObject();
        Assert.assertEquals(cfgResponse, actualResponse.getResponses().get(0));
    }

}
