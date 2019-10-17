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
import java.util.HashSet;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.SeedOfficialSystemVMTemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.org.Cluster;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.http.downloader.HttpDirectTemplateDownloader;
import com.cloud.utils.script.Script;

@APICommand(name = "seedOfficialSystemVMTemplate", description = "Copies an official system vm template into secondary storage",
        responseObject = ZoneResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.13",
        authorized = {RoleType.Admin})
public class SeedOfficialSystemVMTemplateCmd extends BaseCmd {

    public Long getId() {
        return id;
    }

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the ID of the zone")
    private Long id;

    public String getUrl() {
        return url;
    }

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, entityType = ZoneResponse.class, description = "The template url")
    private String url;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, entityType = ZoneResponse.class, description = "The Hypervisor to register this template for")
    private String hypervisor;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    private Long templateId;

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    private String installPath;

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    private String fileExtension;


    @Override
    public void execute(){
        // Get all the clusters for this zone
        Pair<List<? extends Cluster>, Integer> clusters = _mgr.searchForClusters(this);
        HashSet<String> hypervisors = new HashSet<>();
        // we need to get the hypervisors from the clusters
        for (Cluster cluster: clusters.first()){
            hypervisors.add(cluster.getHypervisorType().toString());
        }

        // we need to get secondary storage for this zone.
        HashSet<String> imageStores = _queryService.searchForImageStores(this);

        String script = Script.findScript("scripts/storage/secondary/","cloud-install-sys-tmplt");

        // mount locally
        String mountPoint = "/tmp/nfsmount";
        Script.runSimpleBashScript("mkdir " + mountPoint);

        for (String imageStore: imageStores){
            try {
                URI uri = new URI(imageStore);
                Script.runSimpleBashScript("sudo mount -t nfs " + uri.getHost() + ":" + uri.getPath() + " " + mountPoint);
            } catch (URISyntaxException e) {
                throw new CloudRuntimeException("Malformed URI " + imageStore);
            }

            // now download the template to the image store for every hypervisor if not specified
            if (this.hypervisor != null && this.url != null){

                if (this.hypervisor.equalsIgnoreCase("kvm") || this.hypervisor.equalsIgnoreCase("lxc")) {
                    this.fileExtension = "qcow2";
                } else if (this.hypervisor.equalsIgnoreCase("xenserver") || this.hypervisor.equalsIgnoreCase("hyperv")) {
                    this.fileExtension = "vhd";
                } else if (this.hypervisor.equalsIgnoreCase("vmware")) {
                    this.fileExtension = "ova";
                } else if (this.hypervisor.equalsIgnoreCase("ovm3")) {
                    this.fileExtension = "raw";
                }

                this.templateId = _queryService.getSystemVMTemplateId(this);

                HttpDirectTemplateDownloader httpDirectTemplateDownloader = new HttpDirectTemplateDownloader(url, templateId, mountPoint, null,null);
                httpDirectTemplateDownloader.downloadTemplate();

                String command = script + " -h " + this.hypervisor.toLowerCase() + " -m " + mountPoint + " -f " + httpDirectTemplateDownloader.getDownloadedFilePath();
                Script.runSimpleBashScript(command);
                _templateService.updateTemplate(this);
            } else {
                for (String hypervisor: hypervisors){
                    String url = _templateService.getSystemVMTemplateDefaultURL(hypervisor);
                    String command = script + " -h " + hypervisor.toLowerCase() + " -m " + mountPoint + " -u " + url;
                    Script.runSimpleBashScript(command);
                }
            }
        }

        Script.runSimpleBashScript("sudo umount " + mountPoint);
        SeedOfficialSystemVMTemplateResponse response = new SeedOfficialSystemVMTemplateResponse();
        response.setResult("Done");
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return "seedofficialsystemvmtemplateresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
