package com.cloud.cluster;

import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Status.Event;

public interface ClusteredAgentRebalanceService {
    public static final int DEFAULT_TRANSFER_CHECK_INTERVAL = 10000;

    void startRebalanceAgents();

    boolean executeRebalanceRequest(long agentId, Event event) throws AgentUnavailableException, OperationTimedoutException;

}
