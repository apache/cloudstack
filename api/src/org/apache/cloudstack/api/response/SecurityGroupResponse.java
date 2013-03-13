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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.security.SecurityGroup;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = SecurityGroup.class)
public class SecurityGroupResponse extends BaseResponse implements ControlledViewEntityResponse{

    @SerializedName(ApiConstants.ID) @Param(description="the ID of the security group")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the security group")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION) @Param(description="the description of the security group")
    private String description;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account owning the security group")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the group")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the group")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID of the security group")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the security group")
    private String domainName;

    @SerializedName("ingressrule")  @Param(description="the list of ingress rules associated with the security group", responseObject = SecurityGroupRuleResponse.class)
    private Set<SecurityGroupRuleResponse> ingressRules;

    @SerializedName("egressrule")  @Param(description="the list of egress rules associated with the security group", responseObject = SecurityGroupRuleResponse.class)
    private Set<SecurityGroupRuleResponse> egressRules;

    @SerializedName(ApiConstants.TAGS)  @Param(description="the list of resource tags associated with the rule", responseObject = ResourceTagResponse.class)
    private Set<ResourceTagResponse> tags;

    public SecurityGroupResponse(){
        this.ingressRules = new LinkedHashSet<SecurityGroupRuleResponse>();
        this.egressRules = new LinkedHashSet<SecurityGroupRuleResponse>();
        this.tags = new LinkedHashSet<ResourceTagResponse>();
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

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setSecurityGroupIngressRules(Set<SecurityGroupRuleResponse> securityGroupRules) {
        this.ingressRules = securityGroupRules;
    }

    public void addSecurityGroupIngressRule(SecurityGroupRuleResponse rule){
        this.ingressRules.add(rule);
    }

    public void setSecurityGroupEgressRules(Set<SecurityGroupRuleResponse> securityGroupRules) {
        this.egressRules = securityGroupRules;
    }

    public void addSecurityGroupEgressRule(SecurityGroupRuleResponse rule){
        this.egressRules.add(rule);
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
        SecurityGroupResponse other = (SecurityGroupResponse) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void addTag(ResourceTagResponse tag){
        this.tags.add(tag);
    }
}
