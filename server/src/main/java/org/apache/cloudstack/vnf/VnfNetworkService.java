package org.apache.cloudstack.vnf;

import org.apache.cloudstack.api.command.user.vnf.*;
import org.apache.cloudstack.api.response.CreateNetworkResponse;

public interface VnfNetworkService {
    CreateNetworkResponse createVnfNetwork(CreateVnfNetworkCmd cmd);
    void uploadDictionary(long networkId, String name, String yaml, long ownerId);
    void attachVnfVm(long networkId, long vmId, long ownerId);
    org.apache.cloudstack.api.response.SuccessResponse getStatus(long networkId);
}
