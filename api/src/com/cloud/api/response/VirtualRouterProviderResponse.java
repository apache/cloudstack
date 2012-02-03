package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VirtualRouterProviderResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the router")
    private IdentityProxy id = new IdentityProxy("virtual_router_providers");
 
    @SerializedName(ApiConstants.NSP_ID) @Param(description="the physical network service provider id of the provider")
    private IdentityProxy nspId = new IdentityProxy("physical_network_service_providers");
 
    @SerializedName(ApiConstants.ENABLED) @Param(description="Enabled/Disabled the service provider")
    private Boolean enabled;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the provider")
    private String accountName;
    
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the ipaddress")
    private Long projectId;
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the address")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID associated with the provider")
    private Long domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain associated with the provider")
    private String domainName;
    
    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    @Override
    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setNspId(Long nspId) {
        this.nspId.setValue(nspId);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
