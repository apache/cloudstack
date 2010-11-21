package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class InstanceGroupResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the instance group")
    private Long id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the instance group")
    private String name;

    @SerializedName(ApiConstants.CREATED) @Param(description="time and date the instance group was created")
    private Date created;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account owning the instance group")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID of the instance group")
    private Long domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name of the instance group")
    private String domainName;

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

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
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
}
