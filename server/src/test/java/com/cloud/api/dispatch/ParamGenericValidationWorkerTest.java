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

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParamGenericValidationWorkerTest {

    protected static final String FAKE_CMD_NAME = "fakecmdname";

    protected static final String FAKE_CMD_ROLE_NAME = "fakecmdrolename";

    protected String loggerOutput;

    protected void driveTest(final BaseCmd cmd, final Map<String, String> params) {
        final ParamGenericValidationWorker genValidationWorker = new ParamGenericValidationWorker();

        // We create a mock logger to verify the result
        ParamGenericValidationWorker.s_logger = new Logger("") {
            @Override
            public void warn(final Object msg) {
                loggerOutput = msg.toString();
            }
        };

        // Execute
        genValidationWorker.handle(new DispatchTask(cmd, params));
    }

    @Test
    public void testHandle() throws ResourceAllocationException {
        // Prepare
        final BaseCmd cmd = new FakeCmd();
        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.COMMAND, "");
        params.put(ApiConstants.ACCOUNT_ID, "");
        params.put(ApiConstants.CTX_START_EVENT_ID, "");
        params.put(ApiConstants.COMMAND, "");
        params.put(ApiConstants.CMD_EVENT_TYPE, "");
        params.put(ApiConstants.USERNAME, "");
        params.put(ApiConstants.USER_ID, "");
        params.put(ApiConstants.PASSWORD, "");
        params.put(ApiConstants.DOMAIN, "");
        params.put(ApiConstants.DOMAIN_ID, "");
        params.put(ApiConstants.DOMAIN__ID, "");
        params.put(ApiConstants.SESSIONKEY, "");
        params.put(ApiConstants.RESPONSE, "");
        params.put(ApiConstants.PAGE, "");
        params.put(ApiConstants.USER_API_KEY, "");
        params.put(ApiConstants.API_KEY, "");
        params.put(ApiConstants.PAGE_SIZE, "");
        params.put(ApiConstants.HTTPMETHOD, "");
        params.put(ApiConstants.SIGNATURE, "");
        params.put(ApiConstants.CTX_ACCOUNT_ID, "");
        params.put(ApiConstants.CTX_START_EVENT_ID, "");
        // Make sure it's case insensitive
        params.put(ApiConstants.CTX_START_EVENT_ID, "");
        params.put(ApiConstants.CTX_START_EVENT_ID.toLowerCase(), "");
        params.put(ApiConstants.CTX_USER_ID.toUpperCase(), "");
        params.put(ApiConstants.CTX_USER_ID, "");
        params.put(ApiConstants.UUID, "");
        params.put(ApiConstants.ID, "");
        params.put("_", "");
        params.put("addedParam", "");

        Account account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        // Execute
        try {
            driveTest(cmd, params);
        } finally {
            CallContext.unregister();
        }

        // Assert
        assertEquals("There should be no errors since there are no unknown parameters for this command class", null, loggerOutput);
    }

    @Test
    public void testHandleWithUnknownParams() throws ResourceAllocationException {
        // Prepare
        final String unknownParamKey = "unknownParam";
        final BaseCmd cmd = new FakeCmd();
        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.COMMAND, "");
        params.put("addedParam", "");
        params.put(unknownParamKey, "");

        Account account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        // Execute
        try {
            driveTest(cmd, params);
        } finally {
            CallContext.unregister();
        }

        // Assert
        assertTrue("There should be error msg, since there is one unknown parameter", loggerOutput.contains(unknownParamKey));
        assertTrue("There should be error msg containing the correct command name", loggerOutput.contains(FAKE_CMD_NAME));
    }

    @Test
    public void testHandleWithoutAuthorization() throws ResourceAllocationException {
        final Account.Type type = Account.Type.NORMAL;

        driveAuthTest(type);

        // Assert
        assertTrue("There should be error msg, since there is one unauthorized parameter", loggerOutput.contains("paramWithRole"));
        assertTrue("There should be error msg containing the correct command name", loggerOutput.contains(FAKE_CMD_ROLE_NAME));
    }

    @Test
    public void testHandleWithAuthorization() throws ResourceAllocationException {
        final Account.Type type = Account.Type.ADMIN;

        driveAuthTest(type);
        // Assert
        assertEquals("There should be no errors since parameters have authorization", null, loggerOutput);
    }

    protected void driveAuthTest(final Account.Type type) {

        // Prepare
        final BaseCmd cmd = new FakeCmdWithRoleAdmin();
        final Account account = mock(Account.class);
        ((FakeCmdWithRoleAdmin)cmd).account = account;
        when(account.getType()).thenReturn(type);
        User user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.COMMAND, "");
        params.put("addedParam", "");
        params.put("paramWithRole", "");

        // Execute
        try {
            driveTest(cmd, params);
        } finally {
            CallContext.unregister();
        }
    }
}


@APICommand(name=ParamGenericValidationWorkerTest.FAKE_CMD_NAME, responseObject=BaseResponse.class)
class FakeCmd extends BaseCmd {

    @Parameter(name = "addedParam")
    private String addedParam;

    public Account account;

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

@APICommand(name=ParamGenericValidationWorkerTest.FAKE_CMD_ROLE_NAME, responseObject=BaseResponse.class)
class FakeCmdWithRoleAdmin extends FakeCmd {

    @Parameter(name = "paramWithRole", authorized = {RoleType.Admin})
    private String paramWithRole;
}
