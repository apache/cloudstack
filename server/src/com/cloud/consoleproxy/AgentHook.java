/**
 * 
 */
package com.cloud.consoleproxy;

import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.ConsoleAccessAuthenticationCommand;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.HostVO;
import com.cloud.host.Status;

public interface AgentHook {
    void onLoadReport(ConsoleProxyLoadReportCommand cmd);
    AgentControlAnswer onConsoleAccessAuthentication(ConsoleAccessAuthenticationCommand cmd);
    void onAgentConnect(HostVO host, StartupCommand cmd);
    
    public void onAgentDisconnect(long agentId, Status state);
}
