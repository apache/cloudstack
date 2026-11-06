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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class KubernetesClusterNodeResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the instance of the node")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the instance of the node")
    private String name;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "The host name of the instance, which is the name of the node in the Kubernetes cluster")
    private String hostName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the instance of the node")
    private String state;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "The IP address of the instance of the node in the network of the Kubernetes cluster")
    private String ipAddress;

    @SerializedName(ApiConstants.NODE_GROUP_ID)
    @Param(description = "The ID of the node group of the node")
    private String nodeGroupId;

    @SerializedName(ApiConstants.NODE_GROUP_NAME)
    @Param(description = "The name of the node group of the node")
    private String nodeGroupName;

    @SerializedName(ApiConstants.NODE_LABELS)
    @Param(description = "The Kubernetes labels of the node, in addition to the labels of its node group")
    private Map<String, String> labels;

    @SerializedName(ApiConstants.NODE_TAINTS)
    @Param(description = "The Kubernetes taints of the node in the key[=value]:effect format, in addition to the taints of its node group")
    private List<String> taints;

    public KubernetesClusterNodeResponse() {
        setObjectName("kubernetesclusternode");
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

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setNodeGroupId(String nodeGroupId) {
        this.nodeGroupId = nodeGroupId;
    }

    public void setNodeGroupName(String nodeGroupName) {
        this.nodeGroupName = nodeGroupName;
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
}
