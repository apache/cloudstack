package com.cloud.api.response;

public interface ControlledEntityResponse {

    public void setAccountName(String accountName);
    
    public void setProjectName(String projectName);

    public void setDomainId(Long domainId);
    
    public void setDomainName(String domainName);
}
