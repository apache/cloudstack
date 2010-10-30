/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.agent.resource;

import java.util.Map;

import javax.ejb.Local;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;

@Local(value={ServerResource.class})
public class DummyResource implements ServerResource {
    String _name;
    Host.Type _type;
    boolean _negative;
    IAgentControl _agentControl;

    @Override
    public void disconnected() {
    }
    
    @Override
    public Answer executeRequest(Command cmd) {
        System.out.println("Received Command: " + cmd.toString());
        Answer answer = new Answer(cmd, !_negative, "response");
        System.out.println("Replying with: " + answer.toString());
        return answer;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(_type, id);
    }

    @Override
    public Type getType() {
        return _type;
    }

    @Override
    public StartupCommand[] initialize() {
        return new StartupCommand[] {new StartupCommand()};
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        _name = name;
        
        String value = (String)params.get("type");
        _type = Host.Type.valueOf(value);
        
        value = (String)params.get("negative.reply");
        _negative = Boolean.parseBoolean(value);
        
        return true;
    }

    @Override
    public String getName() {
        return _name;
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
    public IAgentControl getAgentControl() {
    	return _agentControl;
    }
    
    @Override
    public void setAgentControl(IAgentControl agentControl) {
    	_agentControl = agentControl;
    }
}
