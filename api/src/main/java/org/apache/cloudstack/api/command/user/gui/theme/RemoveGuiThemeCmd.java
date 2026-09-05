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
package org.apache.cloudstack.api.command.user.gui.theme;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GuiThemeResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.gui.theme.GuiTheme;
import org.apache.cloudstack.gui.theme.GuiThemeService;

import javax.inject.Inject;

@APICommand(name = "removeGuiTheme", description = "Removes an existing GUI theme.", responseObject = GuiThemeResponse.class, entityType = {GuiTheme.class},
        since = "4.21.0.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class RemoveGuiThemeCmd extends BaseCmd {

    @Inject
    GuiThemeService guiThemeService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuiThemeResponse.class, required = true,
            description = "The unique identifier of the GUI theme to be removed.")
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public void execute() {
        guiThemeService.removeGuiTheme(this);
        final SuccessResponse response = new SuccessResponse();
        response.setResponseName(getCommandName());
        response.setSuccess(true);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
