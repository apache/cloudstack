package com.cloud.api.commands;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

public class GetCiscoVSMByClusterIdCmd extends BaseCmd {

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getCommandName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getEntityOwnerId() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getClusterId() {
        // TODO Auto-generated method stub
        return 0;
    }

}
