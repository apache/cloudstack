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
package org.apache.cloudstack.api.command.admin.config;

import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.config.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.crypt.DBEncryptionUtil;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCfgCmdTest {

    private UpdateCfgCmd updateCfgCmd;

    private MockedStatic<DBEncryptionUtil> mockedStatic;

    @Before
    public void setUp() {
        updateCfgCmd = new UpdateCfgCmd();
        mockedStatic = Mockito.mockStatic(DBEncryptionUtil.class);
    }

    @After
    public void tearDown() {
        mockedStatic.close();
    }

    @Test
    public void setResponseValueSetsEncryptedValueWhenConfigurationIsEncrypted() {
        ConfigurationResponse response = new ConfigurationResponse();
        Configuration cfg = Mockito.mock(Configuration.class);
        Mockito.when(cfg.isEncrypted()).thenReturn(true);
        Mockito.when(cfg.getValue()).thenReturn("testValue");
        Mockito.when(DBEncryptionUtil.encrypt("testValue")).thenReturn("encryptedValue");
        updateCfgCmd.setResponseValue(response, cfg);
        Assert.assertEquals("encryptedValue", response.getValue());
    }

    @Test
    public void setResponseValueSetsPlainValueWhenConfigurationIsNotEncrypted() {
        ConfigurationResponse response = new ConfigurationResponse();
        Configuration cfg = Mockito.mock(Configuration.class);
        Mockito.when(cfg.isEncrypted()).thenReturn(false);
        Mockito.when(cfg.getValue()).thenReturn("testValue");
        updateCfgCmd.setResponseValue(response, cfg);
        Assert.assertEquals("testValue", response.getValue());
    }

    @Test
    public void setResponseValueHandlesNullConfigurationValueGracefully() {
        ConfigurationResponse response = new ConfigurationResponse();
        Configuration cfg = Mockito.mock(Configuration.class);
        Mockito.when(cfg.isEncrypted()).thenReturn(false);
        Mockito.when(cfg.getValue()).thenReturn(null);
        updateCfgCmd.setResponseValue(response, cfg);
        Assert.assertNull(response.getValue());
    }

}
