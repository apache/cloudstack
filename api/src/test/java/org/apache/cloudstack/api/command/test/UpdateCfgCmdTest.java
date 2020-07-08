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

import org.apache.cloudstack.acl.RoleService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.config.UpdateCfgCmd;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.config.Configuration;

import com.cloud.configuration.ConfigurationService;
import com.cloud.exception.InvalidParameterValueException;

public class UpdateCfgCmdTest extends TestCase {

    private UpdateCfgCmd updateCfgCmd;
    private ConfigurationService configService;
    private ResponseGenerator responseGenerator;

    @Override
    @Before
    public void setUp() {
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        configService = Mockito.mock(ConfigurationService.class);
        updateCfgCmd = new UpdateCfgCmd();
    }

    @Test
    public void testExecuteForEmptyCfgName() {
        updateCfgCmd._configService = configService;

        try {
            updateCfgCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Empty configuration name provided", exception.getDescription());
        }
    }

    @Test
    public void testExecuteForRestrictedCfg() {
        updateCfgCmd._configService = configService;
        updateCfgCmd.setCfgName(RoleService.EnableDynamicApiChecker.key());

        try {
            updateCfgCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Restricted configuration update not allowed", exception.getDescription());
        }
    }

    @Test
    public void testExecuteForEmptyResult() {
        updateCfgCmd._configService = configService;
        updateCfgCmd.setCfgName("some.cfg");

        try {
            updateCfgCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to update config", exception.getDescription());
        }
    }

    @Test
    public void testExecuteForNullResult() {

        updateCfgCmd._configService = configService;
        updateCfgCmd.setCfgName("some.cfg");

        try {
            Mockito.when(configService.updateConfiguration(updateCfgCmd)).thenReturn(null);
        } catch (InvalidParameterValueException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            updateCfgCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to update config", exception.getDescription());
        }

    }

    @Test
    public void testCreateSuccess() {

        Configuration cfg = Mockito.mock(Configuration.class);
        updateCfgCmd._configService = configService;
        updateCfgCmd._responseGenerator = responseGenerator;
        updateCfgCmd.setCfgName("some.cfg");

        try {
            Mockito.when(configService.updateConfiguration(updateCfgCmd)).thenReturn(cfg);
        } catch (Exception e) {
            Assert.fail("Received exception when success expected " + e.getMessage());
        }

        ConfigurationResponse response = new ConfigurationResponse();
        response.setName("Test case");
        Mockito.when(responseGenerator.createConfigurationResponse(cfg)).thenReturn(response);

        updateCfgCmd.execute();
        Mockito.verify(responseGenerator).createConfigurationResponse(cfg);
        ConfigurationResponse actualResponse = (ConfigurationResponse)updateCfgCmd.getResponseObject();
        Assert.assertEquals(response, actualResponse);
        Assert.assertEquals("updateconfigurationresponse", response.getResponseName());
    }

}
