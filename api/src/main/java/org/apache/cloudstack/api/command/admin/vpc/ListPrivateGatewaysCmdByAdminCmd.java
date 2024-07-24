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
package org.apache.cloudstack.api.command.admin.vpc;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.vpc.ListPrivateGatewaysCmd;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;

import com.cloud.network.vpc.VpcGateway;

@APICommand(name = "listPrivateGateways", description = "List private gateways", responseObject = PrivateGatewayResponse.class, entityType = {VpcGateway.class},
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListPrivateGatewaysCmdByAdminCmd extends ListPrivateGatewaysCmd implements AdminCmd {
    public static final Logger s_logger = Logger.getLogger(ListPrivateGatewaysCmdByAdminCmd.class.getName());

}
