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
package org.apache.cloudstack.api.response;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = {KubernetesClusterNodeGroup.class})
public class KubernetesClusterNodeGroupResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the node group")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the node group")
    private String name;

    @SerializedName(ApiConstants.IS_DEFAULT)
    @Param(description = "Whether the node group is the default node group of the Kubernetes cluster, which holds the worker nodes " +
            "of the cluster size and can not be deleted")
    private Boolean isDefault;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the node group")
    private String state;

    @SerializedName(ApiConstants.CLUSTER_ID)
    @Param(description = "The ID of the Kubernetes cluster of the node group")
    private String clusterId;

    @SerializedName(ApiConstants.CLUSTER_NAME)
    @Param(description = "The name of the Kubernetes cluster of the node group")
    private String clusterName;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "The ID of the service offering of the node group nodes")
    private String serviceOfferingId;

    @SerializedName(ApiConstants.SERVICE_OFFERING_NAME)
    @Param(description = "The name of the service offering of the node group nodes")
    private String serviceOfferingName;

    @SerializedName(ApiConstants.CPU_NUMBER)
    @Param(description = "The number of CPU cores of each node of the node group")
    private Integer cpuNumber;

    @SerializedName(ApiConstants.MEMORY)
    @Param(description = "The memory (in MiB) of each node of the node group")
    private Integer memory;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "The ID of the template of the node group nodes")
    private String templateId;

    @SerializedName(ApiConstants.TEMPLATE_NAME)
    @Param(description = "The name of the template of the node group nodes")
    private String templateName;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "The number of nodes in the node group")
    private Long size;

    @SerializedName(ApiConstants.NODE_LABELS)
    @Param(description = "The Kubernetes labels of the node group nodes")
    private Map<String, String> labels;

    @SerializedName(ApiConstants.NODE_TAINTS)
    @Param(description = "The Kubernetes taints of the node group nodes, in the key[=value]:effect format")
    private List<String> taints;

    @SerializedName(ApiConstants.AUTOSCALING_ENABLED)
    @Param(description = "Whether the size of the node group is managed by the cluster autoscaler")
    private Boolean autoscalingEnabled;

    @SerializedName(ApiConstants.MIN_SIZE)
    @Param(description = "The minimum size of the node group when autoscaling is enabled")
    private Long minSize;

    @SerializedName(ApiConstants.MAX_SIZE)
    @Param(description = "The maximum size of the node group when autoscaling is enabled")
    private Long maxSize;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_IDS)
    @Param(description = "The IDs of the instances of the node group nodes")
    private List<String> virtualMachineIds;

    @SerializedName("nodes")
    @Param(description = "The nodes of the node group, with their own Kubernetes labels and taints",
            responseObject = KubernetesClusterNodeResponse.class)
    private List<KubernetesClusterNodeResponse> nodes;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The date when the node group was created")
    private Date created;

    public KubernetesClusterNodeGroupResponse() {
        setObjectName("kubernetesclusternodegroup");
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

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setServiceOfferingName(String serviceOfferingName) {
        this.serviceOfferingName = serviceOfferingName;
    }

    public void setCpuNumber(Integer cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<String> getTaints() {
        return taints;
    }

    public void setTaints(List<String> taints) {
        this.taints = taints;
    }

    public Boolean getAutoscalingEnabled() {
        return autoscalingEnabled;
    }

    public void setAutoscalingEnabled(Boolean autoscalingEnabled) {
        this.autoscalingEnabled = autoscalingEnabled;
    }

    public Long getMinSize() {
        return minSize;
    }

    public void setMinSize(Long minSize) {
        this.minSize = minSize;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public List<String> getVirtualMachineIds() {
        return virtualMachineIds;
    }

    public void setVirtualMachineIds(List<String> virtualMachineIds) {
        this.virtualMachineIds = virtualMachineIds;
    }

    public List<KubernetesClusterNodeResponse> getNodes() {
        return nodes;
    }

    public void setNodes(List<KubernetesClusterNodeResponse> nodes) {
        this.nodes = nodes;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
