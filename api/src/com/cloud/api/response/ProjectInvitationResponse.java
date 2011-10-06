package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ProjectInvitationResponse extends BaseResponse implements ControlledEntityResponse{
    @SerializedName(ApiConstants.PROJECT_ID) @Param(description="the id of the project")
    private Long projectId;
    
    @SerializedName(ApiConstants.PROJECT) @Param(description="the name of the project")
    private String projectName;
    
    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id the project belongs to")
    private Long domainId;
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name where the project belongs to")
    private String domainName;
    
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account name of the project's owner")
    private String accountName;
    
    @SerializedName(ApiConstants.EMAIL) @Param(description="the email the invitation was sent to")
    private String email;
    
    @SerializedName(ApiConstants.STATE) @Param(description="the invitation state")
    private String invitationState;

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setDomainName(String domain) {
        this.domainName = domain;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setInvitationState(String invitationState) {
        this.invitationState = invitationState;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
