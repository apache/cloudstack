package com.cloud.ucs.manager;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.ucs.database.UcsBladeVO;
import com.google.gson.annotations.SerializedName;
@EntityReference(value=UcsBladeVO.class)
public class UcsBladeResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ucs blade id")
    private String id;
    @SerializedName(ApiConstants.UCS_MANAGER_ID)
    @Param(description = "ucs manager id")
    private String ucsManagerId;
    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "cloudstack host id this blade associates to")
    private String hostId;
    @SerializedName(ApiConstants.UCS_BLADE_DN)
    @Param(description = "ucs blade dn")
    private String dn;
    @SerializedName(ApiConstants.UCS_PROFILE_DN)
    @Param(description = "associated ucs profile dn")
    private String associatedProfileDn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(String ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getAssociatedProfileDn() {
        return associatedProfileDn;
    }

    public void setAssociatedProfileDn(String associatedProfileDn) {
        this.associatedProfileDn = associatedProfileDn;
    }
    
}
