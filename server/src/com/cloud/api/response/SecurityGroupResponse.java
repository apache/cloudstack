package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class SecurityGroupResponse implements ResponseObject {
    @Param(name="id")
    private Long id;
    
    @Param(name="name")
    private String name;
    
    @Param(name="description")
    private String description;
    
    @Param(name="account")
    private String accountName;
    
    @Param(name="domainid")
    private Long domainId;
    
    @Param(name="domain")
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
}
