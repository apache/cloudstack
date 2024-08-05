//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.gui.theme.GuiThemeJoin;

import java.util.Date;

@EntityReference(value = {GuiThemeJoin.class})
public class GuiThemeResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the custom GUI theme.")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the GUI theme.")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the GUI theme.")
    private String description;

    @SerializedName(ApiConstants.CSS)
    @Param(description = "The CSS to be retrieved and imported into the GUI when matching the theme access configurations.")
    private String css;

    @SerializedName(ApiConstants.JSON_CONFIGURATION)
    @Param(description = "The JSON with the configurations to be retrieved and imported into the GUI when matching the theme access configurations.")
    private String jsonConfiguration;

    @SerializedName(ApiConstants.COMMON_NAMES)
    @Param(description = "A set of Common Names (CN) (fixed or wildcard) separated by comma that can retrieve the theme; e.g.: *acme.com,acme2.com")
    private String commonNames;

    @SerializedName(ApiConstants.DOMAIN_IDS)
    @Param(description = "A set of domain UUIDs (also known as ID for the end-user) separated by comma that can retrieve the theme.")
    private String domainIds;

    @SerializedName(ApiConstants.RECURSIVE_DOMAINS)
    @Param(description = "Whether to consider the subdomains of the informed domain IDs.")
    private Boolean recursiveDomains;

    @SerializedName(ApiConstants.ACCOUNT_IDS)
    @Param(description = "A set of account UUIDs (also known as ID for the end-user) separated by comma that can retrieve the theme.")
    private String accountIds;

    @SerializedName(ApiConstants.IS_PUBLIC)
    @Param(description = "Defines whether a theme can be retrieved by anyone when only the `commonNames` is informed. If the `domainIds` or `accountIds` is informed, it is " +
            "considered as `false`.")
    private Boolean isPublic;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "When the GUI theme was created.")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "When the GUI theme was removed.")
    private Date removed;

    public GuiThemeResponse() {
        super("guiThemes");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCss() {
        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }

    public String getJsonConfiguration() {
        return jsonConfiguration;
    }

    public void setJsonConfiguration(String jsonConfiguration) {
        this.jsonConfiguration = jsonConfiguration;
    }

    public String getCommonNames() {
        return commonNames;
    }

    public void setCommonNames(String commonNames) {
        this.commonNames = commonNames;
    }

    public String getDomainIds() {
        return domainIds;
    }

    public void setDomainIds(String domainIds) {
        this.domainIds = domainIds;
    }

    public String getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(String accountIds) {
        this.accountIds = accountIds;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public Boolean getRecursiveDomains() {
        return recursiveDomains;
    }

    public void setRecursiveDomains(Boolean recursiveDomains) {
        this.recursiveDomains = recursiveDomains;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
