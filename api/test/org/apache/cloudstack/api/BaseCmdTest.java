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
package org.apache.cloudstack.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

public class BaseCmdTest {

    private static final String NON_EXPECTED_COMMAND_NAME = "Non expected command name";
    protected static final String CMD1_NAME = "Cmd1Name";
    protected static final String CMD2_NAME = "Cmd2Name";
    protected static final String CMD1_RESPONSE = "cmd1response";
    protected static final String CMD2_RESPONSE = "cmd2response";

    @Test
    public void testGetActualCommandName(){
        BaseCmd cmd1 = new Cmd1();
        BaseCmd cmd2 = new Cmd2();

        assertEquals(NON_EXPECTED_COMMAND_NAME, CMD1_NAME, cmd1.getActualCommandName());
        assertEquals(NON_EXPECTED_COMMAND_NAME, CMD2_NAME, cmd2.getActualCommandName());
    }
}

@APICommand(name=BaseCmdTest.CMD1_NAME, responseObject=BaseResponse.class)
class Cmd1 extends BaseCmd {
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
    }
    @Override
    public String getCommandName() {
        return BaseCmdTest.CMD1_RESPONSE;
    }
    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}

@APICommand(name=BaseCmdTest.CMD2_NAME, responseObject=BaseResponse.class)
class Cmd2 extends Cmd1 {
    @Override
    public String getCommandName() {
        return BaseCmdTest.CMD2_RESPONSE;
    }
}
