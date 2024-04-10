package org.apache.cloudstack.api.response;

import com.cloud.network.router.VirtualRouter;
import com.cloud.serializer.Param;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = {VirtualMachine.class, UserVm.class, VirtualRouter.class})
public class KubernetesUserVmResponse extends UserVmResponse {
    @SerializedName(ApiConstants.IS_EXTERNAL_NODE)
    @Param(description = "If the VM is an externally added node")
    private boolean isExternalNode;

    public boolean isExternalNode() {
        return isExternalNode;
    }

    public void setExternalNode(boolean externalNode) {
        isExternalNode = externalNode;
    }
}
