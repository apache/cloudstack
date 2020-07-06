package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenVmInterfaceResponse extends BaseResponse{

    @SerializedName(ApiConstants.UUID)
    @Param(description = "UUID of the tungsten virtual machine interface")
    private String uuid;
    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the tungsten virtual machine interface")
    private String name;
    @SerializedName(ApiConstants.PARENT_UUID)
    @Param(description = "parent uuid of the tungsten virtual machine interface")
    private String parentUuid;
    @SerializedName(ApiConstants.TUNGSTEN_VIRTUAL_MACHINE_UUID)
    @Param(description = "virtual machine uuid of the tungsten virtual machine interface")
    private List<String> virtualMachinesUuid;
    @SerializedName(ApiConstants.TUNGSTEN_NETWORK_UUID)
    @Param(description = "virtual network of the tungsten virtual machine interface")
    private List<String> virtualNetworksUuid;

    public TungstenVmInterfaceResponse(){
        virtualMachinesUuid = new ArrayList<>();
        virtualNetworksUuid = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public List<String> getVirtualMachinesUuid() {
        return virtualMachinesUuid;
    }

    public void setVirtualMachinesUuid(List<String> virtualMachinesUuid) {
        this.virtualMachinesUuid = virtualMachinesUuid;
    }

    public List<String> getVirtualNetworksUuid() {
        return virtualNetworksUuid;
    }

    public void setVirtualNetworksUuid(List<String> virtualNetworksUuid) {
        this.virtualNetworksUuid = virtualNetworksUuid;
    }
}
