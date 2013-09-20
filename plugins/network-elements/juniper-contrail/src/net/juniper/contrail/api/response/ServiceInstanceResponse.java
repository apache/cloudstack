package net.juniper.contrail.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.response.ControlledEntityResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ServiceInstanceResponse extends BaseResponse implements
        ControlledEntityResponse {
    
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the virtual machine")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the virtual machine")
    private String name;

    @SerializedName("displayname") @Param(description="user generated name. The name of the virtual machine is returned if no displayname exists.")
    private String displayName;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account associated with the virtual machine")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the project id of the vm")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT) @Param(description="the project name of the vm")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain in which the virtual machine exists")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the name of the domain in which the virtual machine exists")
    private String domainName;

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

}
