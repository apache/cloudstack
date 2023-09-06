package org.apache.cloudstack.agent.api;

import com.cloud.network.dao.NetworkVO;

public class DeleteNsxSegmentCommand extends CreateNsxSegmentCommand {
    public DeleteNsxSegmentCommand(String accountName, NetworkVO network) {
        super(null, network.getDataCenterId(), accountName, network.getAccountId(), null, network);
    }
}
