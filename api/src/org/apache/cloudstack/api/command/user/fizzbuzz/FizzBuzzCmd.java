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

package org.apache.cloudstack.api.command.user.fizzbuzz;

import com.cloud.fizzbuzz.FizzBuzzService;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = FizzBuzzCmd.APINAME,
        description = "the classic fizzBuzz test",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.11",
        authorized = {RoleType.User})
public class FizzBuzzCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(FizzBuzzCmd.class.getName());

    public static final String APINAME = "fizzBuzz";

    @Parameter(name = "number",
            type = CommandType.INTEGER,
            required = false,
            description = "A number passed by user to do FizzBuzz test on.")
    private Integer number;

    @Inject
    private FizzBuzzService _fizzBuzzService;

    // MARK - Implementation

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        final String responseText = _fizzBuzzService.getDisplayText(this.number);
        s_logger.debug("FizzBuzz: " + number + " response " + responseText);
        final SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText(responseText);
        this.setResponseObject(response);
    }

}

