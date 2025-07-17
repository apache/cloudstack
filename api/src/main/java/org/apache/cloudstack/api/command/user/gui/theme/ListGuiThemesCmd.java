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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GuiThemeResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.gui.theme.GuiTheme;
import org.apache.cloudstack.gui.theme.GuiThemeService;

import javax.inject.Inject;

@APICommand(name = "listGuiThemes", description = "Lists GUI themes.", responseObject = GuiThemeResponse.class, entityType = {GuiTheme.class},
        since = "4.21.0.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin, RoleType.User, RoleType.DomainAdmin,
        RoleType.ResourceAdmin})
public class ListGuiThemesCmd extends BaseListCmd {

    @Inject
    GuiThemeService guiThemeService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "The theme ID.", entityType = GuiThemeResponse.class)
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the theme.")
    private String name;

    @Parameter(name = ApiConstants.COMMON_NAME, type = CommandType.STRING, description = "The internet Common Name (CN) to be filtered.")
    private String commonName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "The ID of the domain to be filtered.")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "The ID of the account to be filtered.")
    private Long accountId;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, description = "Whether to list all themes.")
    private boolean listAll = false;

    @Parameter(name = ApiConstants.SHOW_REMOVED, type = CommandType.BOOLEAN, description = "Whether to list removed themes.")
    private boolean showRemoved = false;

    @Parameter(name = ApiConstants.SHOW_PUBLIC, type = CommandType.BOOLEAN, description = "Whether to list public themes.")
    private Boolean showPublic;

    @Parameter(name = ApiConstants.LIST_ONLY_DEFAULT_THEME, type = CommandType.BOOLEAN, description = "Whether to only list the default theme.")
    private boolean listOnlyDefaultTheme = false;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCommonName() {
        return commonName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public boolean getListAll() {
        return listAll;
    }

    public boolean getShowRemoved() {
        return showRemoved;
    }

    public Boolean getShowPublic() {
        return showPublic;
    }

    public boolean getListOnlyDefaultTheme() {
        return listOnlyDefaultTheme;
    }

    @Override
    public void execute() {
        ListResponse<GuiThemeResponse> response = guiThemeService.listGuiThemes(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
