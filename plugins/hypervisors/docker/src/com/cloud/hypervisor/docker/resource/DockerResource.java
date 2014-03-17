package com.cloud.hypervisor.docker.resource;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;

@Local(value = {ServerResource.class})
public class DockerResource extends ServerResourceBase implements ServerResource {
	private static final Logger s_logger = Logger.getLogger(DockerResource.class);

	@Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		return true;
	}
	
	@Override
	public Answer executeRequest(Command cmd) {
		try {
			if (cmd instanceof StopCommand) {
                return execute((StopCommand)cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand)cmd);
            }  else if (cmd instanceof StartCommand) {
                return execute((StartCommand)cmd);
            }  else {
                s_logger.warn("Unsupported command ");
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
	}
	
	protected Answer execute(StopCommand cmd) {
		
	}
	
	private Answer execute(RebootCommand cmd) {
		
	}
	
	protected StartAnswer execute(StartCommand cmd) {
		
	}
	
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConfigParams(Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getConfigParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRunLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRunLevel(int level) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StartupCommand[] initialize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getDefaultScriptsDir() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
