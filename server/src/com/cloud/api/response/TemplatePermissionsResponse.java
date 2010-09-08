package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class TemplatePermissionsResponse implements ResponseObject {
    @Param(name="id")
    private Long id;

    @Param(name="ispublic")
    private Boolean publicTemplate;

    @Param(name="domainid")
    private Long domainId;

    @Param(name="account")
    private List<String> accountNames;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getPublicTemplate() {
        return publicTemplate;
    }

    public void setPublicTemplate(Boolean publicTemplate) {
        this.publicTemplate = publicTemplate;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public List<String> getAccountNames() {
        return accountNames;
    }

    public void setAccountNames(List<String> accountNames) {
        this.accountNames = accountNames;
    }
}
