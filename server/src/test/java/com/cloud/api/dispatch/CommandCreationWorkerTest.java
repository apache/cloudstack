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

import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CommandCreationWorkerTest {

    @Test
    public void testHandle() throws ResourceAllocationException {
        // Prepare
        final BaseAsyncCreateCmd asyncCreateCmd = mock(BaseAsyncCreateCmd.class);
        final Map<String, String> params = new HashMap<String, String>();
        Account account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        // Execute
        final CommandCreationWorker creationWorker = new CommandCreationWorker();

        creationWorker.handle(new DispatchTask(asyncCreateCmd, params));

        // Assert
        verify(asyncCreateCmd, times(1)).create();
    }
}
