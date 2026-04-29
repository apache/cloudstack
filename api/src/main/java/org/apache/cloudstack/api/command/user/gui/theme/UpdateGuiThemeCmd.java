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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuiThemeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.gui.theme.GuiTheme;
import org.apache.cloudstack.gui.theme.GuiThemeJoin;
import org.apache.cloudstack.gui.theme.GuiThemeService;

import javax.inject.Inject;


@APICommand(name = "updateGuiTheme", description = "Updates an existing GUI theme.", responseObject = GuiThemeResponse.class, entityType = {GuiTheme.class},
        since = "4.21.0.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class UpdateGuiThemeCmd extends BaseCmd {

    @Inject
    GuiThemeService guiThemeService;

    @Parameter(name = ApiConstants.ID, required = true, type = CommandType.UUID, entityType = GuiThemeResponse.class, description = "The ID of the theme to be updated.")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, length = 2048, description = "A name to identify the theme.")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, length = 4096, description = "A description for the theme.")
    private String description;

    @Parameter(name = ApiConstants.CSS, type = CommandType.STRING, length = 65535, description = "The CSS to be retrieved and imported into the GUI " +
            "when matching the theme access configurations.")
    private String css;

    @Parameter(name = ApiConstants.JSON_CONFIGURATION, type = CommandType.STRING, length = 65535, description = "The JSON with the configurations to be " +
            "retrieved and imported into the GUI when matching the theme access configurations.")
    private String jsonConfiguration;

    @Parameter(name = ApiConstants.COMMON_NAMES, type = CommandType.STRING, length = 65535, description = "A set of Common Names (CN) (fixed or " +
            "wildcard) separated by comma that can retrieve the theme; e.g.: *acme.com,acme2.com")
    private String commonNames;

    @Parameter(name = ApiConstants.DOMAIN_IDS, type = CommandType.STRING, length = 65535, description = "A set of domain UUIDs (also known as ID for " +
            "the end-user) separated by comma that can retrieve the theme.")
    private String domainIds;

    @Parameter(name = ApiConstants.RECURSIVE_DOMAINS, type = CommandType.BOOLEAN, description = "Defines whether the subdomains of the informed domains are considered. Default " +
            "value is false.")
    private Boolean recursiveDomains = false;

    @Parameter(name = ApiConstants.ACCOUNT_IDS, type = CommandType.STRING, length = 65535, description = "A set of account UUIDs (also known as ID for" +
            " the end-user) separated by comma that can retrieve the theme.")
    private String accountIds;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "Defines whether a theme can be retrieved by anyone when only " +
            "the `commonNames` is informed. If the `domainIds` or `accountIds` is informed, it is considered as `false`.")
    private Boolean isPublic = true;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCss() {
        return css;
    }

    public String getJsonConfiguration() {
        return jsonConfiguration;
    }

    public String getCommonNames() {
        return commonNames;
    }

    public String getDomainIds() {
        return domainIds;
    }

    public String getAccountIds() {
        return accountIds;
    }

    public Boolean getRecursiveDomains() {
        return recursiveDomains;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    @Override
    public void execute() {
        GuiThemeJoin guiThemeJoin = guiThemeService.updateGuiTheme(this);

        if (guiThemeJoin == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update the GUI theme.");
        }

        GuiThemeResponse response = _responseGenerator.createGuiThemeResponse(guiThemeJoin);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
