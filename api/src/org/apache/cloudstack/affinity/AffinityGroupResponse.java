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
package org.apache.cloudstack.affinity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.response.ControlledEntityResponse;
import org.apache.cloudstack.api.response.ControlledViewEntityResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.network.security.SecurityGroup;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = AffinityGroup.class)
public class AffinityGroupResponse extends BaseResponse implements ControlledViewEntityResponse {

    @SerializedName(ApiConstants.ID) @Param(description="the ID of the affinity group")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the affinity group")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="the description of the affinity group")
    private String description;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account owning the affinity group")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID of the affinity group")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the affinity group")
    private String domainName;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the type of the affinity group")
    private String type;

    @SerializedName("virtualmachineIds")
    @Param(description = "virtual machine Ids associated with this affinity group ")
    private List<String> vmIdList;

    public AffinityGroupResponse() {
    }

    @Override
    public String getObjectId() {
        return this.getId();
    }


    public String getId() {
        return id;
     }

    public void setId(String id) {
        this.id = id;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AffinityGroupResponse other = (AffinityGroupResponse) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public void setProjectId(String projectId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProjectName(String projectName) {
        // TODO Auto-generated method stub

    }

    public void setVMIdList(List<String> vmIdList) {
        this.vmIdList = vmIdList;
    }

    public void addVMId(String vmId) {
        if (this.vmIdList == null) {
            this.vmIdList = new ArrayList<String>();
        }

        this.vmIdList.add(vmId);
    }

}
