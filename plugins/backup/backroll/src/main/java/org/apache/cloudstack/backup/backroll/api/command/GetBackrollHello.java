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
package org.apache.cloudstack.backup.backroll.api.command;

/* import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.backup.backroll.api.response.ApiHelloResponse; */

/* @APICommand(name = "getBackrollHello", responseObject = ApiHelloResponse.class, description = "Get Hello",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false) */
public class GetBackrollHello {//extends BaseCmd {
    /* private static final Logger LOGGER = Logger.getLogger(GetBackrollHello.class.getName());
    private static final String NAME = "gethelloresponse";


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return NAME;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }

    @Override
    public void execute() {
        LOGGER.info("'getBackrollHello.execute' method invoked");

        ApiHelloResponse response = new ApiHelloResponse("Hello");

        response.setResponseName(getCommandName());
        response.setObjectName("msg");

        setResponseObject(response);
    } */
}
