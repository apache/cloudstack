// Licensedname = "listTemplatePermissions",  to the Apache Software Foundation (ASF) under one
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
package org.apache.cloudstack.api.command.admin.template;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.template.ListTemplatePermissionsCmd;
import org.apache.cloudstack.api.response.TemplatePermissionsResponse;

@APICommand(name = "listTemplatePermissions", description = "List template visibility and all accounts that have permissions to view this template.", responseObject = TemplatePermissionsResponse.class, responseView = ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class ListTemplatePermissionsCmdByAdmin extends ListTemplatePermissionsCmd {

    @Override
    public void execute() {
        executeWithView(ResponseView.Full);
    }

}
