/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.baremetal;

import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupPxeServerCommand;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;

public class PxeServerResourceBase implements ServerResource {
	private static final Logger s_logger = Logger.getLogger(PxeServerResourceBase.class);
	String _name;
	String _guid;
	String _username;
	String _password;
	String _ip;
	String _zoneId;
	String _podId;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		_guid = (String)params.get("guid");
		_ip = (String)params.get("ip");
		_username = (String)params.get("username");
		_password = (String)params.get("password");
		_zoneId = (String)params.get("zone");
		_podId = (String)params.get("pod");

		if (_guid == null) {
			throw new ConfigurationException("No Guid specified");
		}
		
		if (_zoneId == null) {
			throw new ConfigurationException("No Zone specified");
		}
		
		if (_podId == null) {
			throw new ConfigurationException("No Pod specified");
		}
		
		if (_ip == null) {
			throw new ConfigurationException("No IP specified");
		}
		
		if (_username == null) {
			throw new ConfigurationException("No username specified");
		}
		
		if (_password == null) {
			throw new ConfigurationException("No password specified");
		}
		
		return true;
	}
	
	protected ReadyAnswer execute(ReadyCommand cmd) {
		s_logger.debug("Pxe resource " + _name + " is ready");
		return new ReadyAnswer(cmd);
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return _name;
	}

	@Override
	public Type getType() {
		return Type.PxeServer;
	}

	@Override
	public StartupCommand[] initialize() {
		StartupPxeServerCommand cmd = new StartupPxeServerCommand();
		cmd.setName(_name);
		cmd.setDataCenter(_zoneId);
		cmd.setPod(_podId);
		cmd.setPrivateIpAddress(_ip);
		cmd.setStorageIpAddress("");
		cmd.setVersion("");
		cmd.setGuid(_guid);
		return new StartupCommand[]{cmd};
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public IAgentControl getAgentControl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public Answer executeRequest(Command cmd) {
		if (cmd instanceof ReadyCommand) {
			return execute((ReadyCommand) cmd);
		} else {
			return Answer.createUnsupportedCommandAnswer(cmd);
		}
	}

}
