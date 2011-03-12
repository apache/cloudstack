package com.cloud.agent.manager.authn.impl;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.StartupCommandProcessor;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.authn.AgentAuthnException;
import com.cloud.agent.manager.authn.AgentAuthorizer;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.ConnectionException;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.Inject;

@Local(value={AgentAuthorizer.class, StartupCommandProcessor.class})
public class BasicAgentAuthManager implements AgentAuthorizer, StartupCommandProcessor {
    private static final Logger s_logger = Logger.getLogger(BasicAgentAuthManager.class);
    @Inject HostDao _hostDao = null;
    @Inject ConfigurationDao _configDao = null;
    @Inject AgentManager _agentManager = null;
    
    @Override
    public boolean processInitialConnect(StartupCommand[] cmd)
            throws ConnectionException {
        try {
            authorizeAgent(cmd);
        } catch (AgentAuthnException e) {
            throw new ConnectionException(true, "Failed to authenticate/authorize", e);
        }
        s_logger.debug("Authorized agent with guid " + cmd[0].getGuid());
        return false;//so that the next host creator can process it
    }

    @Override
    public boolean authorizeAgent(StartupCommand[] cmd) throws AgentAuthnException{
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        _agentManager.registerForInitialConnects(this, true);
        return true;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
