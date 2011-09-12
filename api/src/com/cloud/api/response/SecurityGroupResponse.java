/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SecurityGroupResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the security group")
    private Long id;

    @SerializedName("name") @Param(description="the name of the security group")
    private String name;

    @SerializedName("description") @Param(description="the description of the security group")
    private String description;

    @SerializedName("account") @Param(description="the account owning the security group")
    private String accountName;

    @SerializedName("domainid") @Param(description="the domain ID of the security group")
    private Long domainId;

    @SerializedName("domain") @Param(description="the domain name of the security group")
    private String domainName;
    
    @SerializedName(ApiConstants.JOB_ID) @Param(description="shows the current pending asynchronous job ID. This tag is not returned if no current pending jobs are acting on the volume")
    private Long jobId;

    @SerializedName("jobstatus") @Param(description="shows the current pending asynchronous job status")
    private Integer jobStatus;

    @SerializedName("ingressrule")  @Param(description="the list of ingress rules associated with the security group", responseObject = IngressRuleResponse.class)
    private List<IngressRuleResponse> ingressRules;

    @SerializedName("egressrule")  @Param(description="the list of ingress rules associated with the security group", responseObject = EgressRuleResponse.class)
    private List<EgressRuleResponse> egressRules;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public List<IngressRuleResponse> getIngressRules() {
        return ingressRules;
    }
    
    public List<EgressRuleResponse> getEgressRules() {
        return egressRules;
    }

    public void setIngressRules(List<IngressRuleResponse> ingressRules) {
        this.ingressRules = ingressRules;
    }
    
    public void setEgressRules(List<EgressRuleResponse> egressRules) {
        this.egressRules = egressRules;
    }

    @Override
    public Long getObjectId() {
        return getId();
    }
    
    @Override
    public Long getJobId() {
        return jobId;
    }

    @Override
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
    
    @Override
    public Integer getJobStatus() {
        return jobStatus;
    }

    @Override
    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
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
}
