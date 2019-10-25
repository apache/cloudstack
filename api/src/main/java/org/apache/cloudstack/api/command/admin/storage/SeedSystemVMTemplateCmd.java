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

package org.apache.cloudstack.api.command.admin.storage;

import java.util.HashSet;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.SeedSystemVMTemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.user.Account;

@APICommand(name = "seedSystemVMTemplate", description = "Copies a system vm template into secondary storage",
        responseObject = ZoneResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.13",
        authorized = {RoleType.Admin})
public class SeedSystemVMTemplateCmd extends BaseCmd {

    private static final String s_name = "seedsystemvmtemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of the zone")
    private Long id;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, description = "The template download url")
    private String url;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true, description = "The Hypervisor to register this template for")
    private String hypervisor;

    @Parameter(name = ApiConstants.FILE_UUID, type = CommandType.STRING, description = "File uuid.")
    private String fileUUID;

    @Parameter(name = ApiConstants.LOCAL_FILE, type = CommandType.BOOLEAN, required = true, description = "Local file or url")
    private Boolean localFile;

    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.STRING, required = true, description = "The id of a specific template")
    private String templateId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getFileUUID() {
        return fileUUID;
    }

    public void setFileUUID(String fileUUID) {
        this.fileUUID = fileUUID;
    }

    public Boolean getLocalFile() {
        return localFile;
    }

    public void setLocalFile(Boolean localFile) {
        this.localFile = localFile;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        // we need to get secondary storage for this zone.
        HashSet<String> imageStores = _queryService.searchForImageStores(this);
        _templateService.seedSystemVMTemplate(imageStores, this);
        SeedSystemVMTemplateResponse response = new SeedSystemVMTemplateResponse();
        response.setResult("Done");
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }
}
