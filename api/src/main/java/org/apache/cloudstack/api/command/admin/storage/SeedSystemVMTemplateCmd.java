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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SeedSystemVMTemplateResponse;

import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;


@APICommand(name = "seedSystemVMTemplate", description = "Copies a system vm template into secondary storage",
        responseObject = SeedSystemVMTemplateResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.13",
authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class SeedSystemVMTemplateCmd extends BaseCmd {

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true, description = "The target hypervisor for the template.")
    private String hypervisor;

    @Parameter(name = ApiConstants.IMAGE_STORE_UUID, type = CommandType.STRING, required = true, description = "Destination image store id.")
    private String imageStoreId;

    @Parameter(name = ApiConstants.FILE_UUID, type = CommandType.STRING, required = true, description = "File uuid.")
    private String fileUUID;

    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.LONG, required = false, description = "template id")
    private Long templateId;

    @Override
    public void execute(){
        ListResponse<ImageStoreResponse> imageStoreResponses = _queryService.searchForImageStores(new ListImageStoresCmd());

        ImageStoreResponse imageStoreResponse = new ImageStoreResponse();

        if (imageStoreResponses.getCount() > 1){
            // get image store with correct id;
            for (ImageStoreResponse response: imageStoreResponses.getResponses()){
                if (response.getId().equalsIgnoreCase(imageStoreId)){
                    imageStoreResponse = response;
                }
            }
        } else if (imageStoreResponses.getCount() == 1) {
            imageStoreResponse = imageStoreResponses.getResponses().get(0);
        }

        hypervisor = hypervisor.toLowerCase();

        String mountPoint = "/tmp/nfsmount";
        String uploadPath = "/tmp/upload";

        try {
            URI uri = new URI(imageStoreResponse.getUrl());

            Script.runSimpleBashScript("mkdir " + mountPoint);
            Script.runSimpleBashScript("sudo mount -t nfs " + uri.getHost() + ":" + uri.getPath() + " " + mountPoint);

        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Malformed URI " + imageStoreResponse.getUrl());
        }

        String script = Script.findScript("scripts/storage/secondary/","cloud-install-sys-tmplt");

        String command = script + " -h " + hypervisor + " -F -m " + mountPoint + " -f " + uploadPath + "/" + fileUUID;

        if (templateId != null){
            command += " -t " + templateId;
        }

        Script.runSimpleBashScriptForExitValue(command);

        Script.runSimpleBashScript("sudo umount " + mountPoint);
        SeedSystemVMTemplateResponse response = new SeedSystemVMTemplateResponse();
        response.setResponseName(getCommandName());

        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return "seedsystemvmtemplateresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
}
