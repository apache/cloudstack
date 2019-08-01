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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.vpc.ListVPCsCmd;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.network.vpc.Vpc;


@APICommand(name = "listVPCs", description = "Lists VPCs", responseObject = VpcResponse.class, responseView = ResponseView.Full, entityType = {Vpc.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVPCsCmdByAdmin extends ListVPCsCmd implements AdminCmd {}
