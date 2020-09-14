package org.apache.cloudstack.storage.image.deployasis;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.vm.VirtualMachineProfile;

import java.util.Map;

public interface DeployAsIsHelper {

    void persistTemplateDeployAsIsDetails(long templateId, DownloadAnswer answer);
    Map<String, String> getVirtualMachineDeployAsIsProperties(VirtualMachineProfile vmId);

    String getAllocatedVirtualMachineTemplatePath(VirtualMachineProfile vm, String configuration, String destStoragePool);
    String getAllocatedVirtualMachineDestinationStoragePool(VirtualMachineProfile vm);
}
