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

import com.cloud.network.router.VirtualRouter;
import com.cloud.serializer.Param;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = {VirtualMachine.class, UserVm.class, VirtualRouter.class})
public class KubernetesUserVmResponse extends UserVmResponse {
    @SerializedName(ApiConstants.IS_EXTERNAL_NODE)
    @Param(description = "If the VM is an externally added node")
    private boolean isExternalNode;

    @SerializedName(ApiConstants.IS_ETCD_NODE)
    @Param(description = "If the VM is an etcd node")
    private boolean isEtcdNode;

    @SerializedName(ApiConstants.KUBERNETES_NODE_VERSION)
    @Param(description = "Kubernetes version of the node")
    private String nodeVersion;


    public void setExternalNode(boolean externalNode) {
        isExternalNode = externalNode;
    }

    public void setEtcdNode(boolean etcdNode) {
        isEtcdNode = etcdNode;
    }

    public void setNodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion;}
}
