/*
 * Copyright 2016 ShapeBlue Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.applicationcluster.ApplicationCluster;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApplicationClusterApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.List;

@SuppressWarnings("unused")
@EntityReference(value = {ApplicationCluster.class})
public class ApplicationClusterResponse extends BaseResponse implements ControlledEntityResponse {

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

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getKeypair() {
        return keypair;
    }

    public void setKeypair(String keypair) {
        this.keypair = keypair;
    }

    public String getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(String clusterSize) {
        this.clusterSize = clusterSize;
    }

    public String getCores() {
        return cores;
    }

    public void setCores(String cores) {
        this.cores = cores;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getState() { return  state;}

    public void setState(String state) {this.state = state;}

    public String getEndpoint() { return  endpoint;}

    public void setEndpoint(String endpoint) {this.endpoint = endpoint;}

    public String getConsoleEndpoint() { return  consoleendpoint;}

    public void setConsoleEndpoint(String consoleendpoint) {this.consoleendpoint = consoleendpoint;}

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public void setVirtualMachineIds(List<String> virtualMachineIds) { this.virtualMachineIds = virtualMachineIds;};

    public void setUsername(String userName) { this.username = userName;}

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) { this.password = password;}

    public String getUsername() {
        return username;
    }

    public List<String> getVirtualMachineIds() {
        return virtualMachineIds;
    }

    public String getConsoleendpoint() {
        return consoleendpoint;
    }

    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the container cluster")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the container cluster")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the container cluster")
    private String description;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "zone name")
    private String zoneName;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "Service Offering id")
    private String serviceOfferingId;

    @SerializedName("serviceofferingname")
    @Param(description = "the name of the service offering of the virtual machine")
    private String serviceOfferingName;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "template id")
    private String templateId;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "network id details")
    private String networkId;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_NAME)
    @Param(description = "the name of the Network associated with the IP address")
    private String associatedNetworkName;

    public String getAssociatedNetworkName() {
        return associatedNetworkName;
    }

    public void setAssociatedNetworkName(String associatedNetworkName) {
        this.associatedNetworkName = associatedNetworkName;
    }

    @SerializedName(ApiConstants.SSH_KEYPAIR)
    @Param(description = "keypair details")
    private String keypair;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "cluster size")
    private String clusterSize;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of the cluster")
    private String state;

    @SerializedName(ApiConstants.CPU_NUMBER)
    @Param(description = "cluster cpu cores")
    private String cores;

    @SerializedName(ApiConstants.MEMORY)
    @Param(description = "cluster size")
    private String memory;

    @SerializedName(ApiConstants.END_POINT)
    @Param(description = "URL end point for the cluster")
    private String endpoint;

    @SerializedName(ApplicationClusterApiConstants.CONSOLE_END_POINT)
    @Param(description = "URL end point for the cluster UI")
    private String consoleendpoint;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_IDS)
    @Param(description = "the list of virtualmachine ids associated with this container cluster")
    private List<String> virtualMachineIds;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "Username with which container cluster is setup")
    private String username;

    @SerializedName(ApiConstants.PASSWORD)
    @Param(description = "Password with which container cluster is setup")
    private String password;

    public ApplicationClusterResponse() {

    }

    @Override
    public void setAccountName(String accountName) {

    }

    @Override
    public void setProjectId(String projectId) {

    }

    @Override
    public void setProjectName(String projectName) {

    }

    @Override
    public void setDomainId(String domainId) {

    }

    @Override
    public void setDomainName(String domainName) {

    }
}
