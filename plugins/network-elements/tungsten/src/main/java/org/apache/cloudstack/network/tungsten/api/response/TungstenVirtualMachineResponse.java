package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenVirtualMachineResponse extends BaseResponse {

    @SerializedName(ApiConstants.UUID)
    @Param(description = "UUID of the tungsten virtual machine")
    private String uuid;
    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the tungsten virtual machine")
    private String name;
    @SerializedName(ApiConstants.TUNGSTEN_VM_INTERFACE_UUID)
    @Param(description = "virtual machine interface uuid of the tungsten virtual machine")
    private List<String> vmInterfacesUuid;

    public TungstenVirtualMachineResponse(){
        vmInterfacesUuid = new ArrayList<>();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getVmInterfacesUuid() {
        return vmInterfacesUuid;
    }

    public void setVmInterfacesUuid(List<String> vmInterfacesUuid) {
        this.vmInterfacesUuid = vmInterfacesUuid;
    }
}
