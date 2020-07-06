package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import net.juniper.contrail.api.ApiObjectBase;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

public class TungstenNetworkResponse extends BaseResponse {

    @SerializedName(ApiConstants.UUID)
    @Param(description = "UUID of the tungsten network")
    private String uuid;
    @SerializedName(ApiConstants.PARENT_UUID)
    @Param(description = "parent_uuid of the tungsten network")
    private String parentUuid;
    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the tungsten network")
    private String name;
    @SerializedName(ApiConstants.FQ_NAME)
    @Param(description = "fq_name of the tungsten network")
    private List<String> fqName;
    @SerializedName(ApiConstants.PARENT)
    @Param(description = "parent of the tungsten network")
    private ApiObjectBase parent;
    @SerializedName(ApiConstants.PARENT_TYPE)
    @Param(description = "parent_type of the tungsten network")
    private String parentType;
    @SerializedName(ApiConstants.TUNGSTEN_NETWORK_IPAM_UUID)
    @Param(description = "tungsten network ipam uuid")
    private String networkIpamUUID;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFqName() {
        return fqName;
    }

    public void setFqName(List<String> fqName) {
        this.fqName = fqName;
    }

    public ApiObjectBase getParent() {
        return parent;
    }

    public void setParent(ApiObjectBase parent) {
        this.parent = parent;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    public String getNetworkIpamUUID() {
        return networkIpamUUID;
    }

    public void setNetworkIpamUUID(String networkIpamUUID) {
        this.networkIpamUUID = networkIpamUUID;
    }
}
