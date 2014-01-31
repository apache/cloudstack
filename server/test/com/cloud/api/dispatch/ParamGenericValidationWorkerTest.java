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
package com.cloud.api.dispatch;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

import org.apache.log4j.Logger;

public class ParamGenericValidationWorkerTest {

    protected String loggerOutput = null;

    protected void driveTest(final BaseCmd cmd, final Map<String, Object> params) {
        final ParamGenericValidationWorker genValidationWorker = new ParamGenericValidationWorker();

        // We create a mock logger to verify the result
        ParamGenericValidationWorker.s_logger = new Logger(""){
            @Override
            public void warn(final Object msg){
                loggerOutput = msg.toString();
            }
        };

        // Execute
        genValidationWorker.handle(cmd, params);
    }

    @Test
    public void testHandle() throws ResourceAllocationException {
        // Prepare
        final BaseCmd cmd = new FakeCmd();
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put(ApiConstants.COMMAND, "");
        params.put("addedParam", "");

        // Execute
        driveTest(cmd, params);

        // Assert
        assertEquals("There should be no errors since there are no unknown parameters for this command class", null, loggerOutput);
    }

    @Test
    public void testHandleWithUnknownParams() throws ResourceAllocationException {
        // Prepare
        final String unknownParamKey = "unknownParam";
        final BaseCmd cmd = new FakeCmd();
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put(ApiConstants.COMMAND, "");
        params.put("addedParam", "");
        params.put(unknownParamKey, "");

        // Execute
        driveTest(cmd, params);

        // Assert
        assertTrue("There should be error msg, since there is one unknown parameter", loggerOutput.contains(unknownParamKey));
    }

    @Test
    public void testHandleWithoutAuthorization() throws ResourceAllocationException {
        final short type = 2;
        driveAuthTest(type);

        // Assert
        assertTrue("There should be error msg, since there is one unauthorized parameter", loggerOutput.contains("paramWithRole"));
    }

    @Test
    public void testHandleWithAuthorization() throws ResourceAllocationException {
        final short type = 1;
        driveAuthTest(type);

        // Assert
        assertEquals("There should be no errors since parameters have authorization", null, loggerOutput);
    }

    protected void driveAuthTest(final short type) {
        // Prepare
        final BaseCmd cmd = new FakeCmdWithRoleAdmin();
        final Account account = mock(Account.class);
        ((FakeCmdWithRoleAdmin)cmd).account = account;
        when(account.getType()).thenReturn(type);
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put(ApiConstants.COMMAND, "");
        params.put("addedParam", "");
        params.put("paramWithRole", "");

        // Execute
        driveTest(cmd, params);
    }
}

class FakeCmd extends BaseCmd {

    @Parameter(name = "addedParam")
    private String addedParam;

    public Account account;

    @Override
    protected Account getCurrentContextAccount() {
        return account;
    }

    //
    // Dummy methods for mere correct compilation
    //
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
    }
    @Override
    public String getCommandName() {
        return null;
    }
    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}

class FakeCmdWithRoleAdmin extends FakeCmd {

    @Parameter(name = "paramWithRole", authorized = {RoleType.Admin})
    private String paramWithRole;
}
