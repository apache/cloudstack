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
package org.apache.cloudstack.api.command.admin.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.vm.UserVmService;

public class AssignVMCmdTest {

    private AssignVMCmd assignVMCmd;
    private UserVmService userVmService;
    private ConfigDepotImpl configDepot;

    @Before
    public void setUp() {
        assignVMCmd = new AssignVMCmd() {
            @Override
            public Long getVmId() {
                return 1L;
            }
        };

        userVmService = mock(UserVmService.class);
        assignVMCmd._userVmService = userVmService;

        configDepot = mock(ConfigDepotImpl.class);
        ConfigKey.init(configDepot);
    }

    @After
    public void tearDown() {
        ConfigKey.init(null);
    }

    private void setDetailedFailureMessageFlag(boolean enabled) {
        when(configDepot.getConfigStringValue(UserVmService.AllowExposingVmAssignFailureDetails.key(),
                ConfigKey.Scope.Global, null)).thenReturn(Boolean.toString(enabled));
    }

    @Test
    public void testExecuteReturnsGenericMessageWhenDetailsDisabled() throws Exception {
        setDetailedFailureMessageFlag(false);
        when(userVmService.moveVmToUser(assignVMCmd)).thenThrow(new InvalidParameterValueException("account over resource limit"));

        try {
            assignVMCmd.execute();
            fail("Expected a ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.PARAM_ERROR, e.getErrorCode());
            assertEquals("Failed to move Instance [1].", e.getDescription());
        }
    }

    @Test
    public void testExecuteReturnsDetailedMessageWhenDetailsEnabledAndParamInvalid() throws Exception {
        setDetailedFailureMessageFlag(true);
        when(userVmService.moveVmToUser(assignVMCmd)).thenThrow(new InvalidParameterValueException("account over resource limit"));

        try {
            assignVMCmd.execute();
            fail("Expected a ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.PARAM_ERROR, e.getErrorCode());
            assertEquals("Failed to move Instance [1]: account over resource limit", e.getDescription());
        }
    }

    @Test
    public void testExecuteReturnsGenericMessageForNonParamExceptionEvenWhenDetailsEnabled() throws Exception {
        setDetailedFailureMessageFlag(true);
        when(userVmService.moveVmToUser(assignVMCmd)).thenThrow(new ResourceUnavailableException("host unreachable", AssignVMCmd.class, 1L));

        try {
            assignVMCmd.execute();
            fail("Expected a ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Failed to move Instance [1].", e.getDescription());
        }
    }
}
