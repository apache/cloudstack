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

package com.cloud.resource;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.net.MacAddress;
import com.cloud.vm.State;

public class DummyHostServerResource extends ServerResourceBase {

	private String _name;
	private String _zone;
	private String _pod;
	private String _guid;
	private String _url;
	private int _instanceId;
	private final int _prefix = 0x55;
	
	private static volatile int s_nextSequence = 1;
	
	@Override
	protected String getDefaultScriptsDir() {
		return "/dummy";
	}

	@Override
	public Answer executeRequest(Command cmd) {
		return new Answer(cmd, false, "Unsupported in dummy host server resource");
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
        HashMap<String, State> newStates = new HashMap<String, State>();
        return new PingRoutingCommand(com.cloud.host.Host.Type.Routing, id, newStates);
	}

	@Override
	public Type getType() {
        return com.cloud.host.Host.Type.Routing;
	}

	@Override
	public StartupCommand[] initialize() {
		
        StartupRoutingCommand cmd = new StartupRoutingCommand(1, 1000L, 1000000L,
        		256L, "hvm", null, null);
        cmd.setGuid(_guid);
        cmd.setDataCenter(_zone);
        cmd.setPod(_pod);
        cmd.setHypervisorType(Hypervisor.Type.None);
        cmd.setAgentTag("vmops-simulator");
        cmd.setName(_url);
        cmd.setPrivateIpAddress(this.getHostPrivateIp());
        cmd.setPrivateMacAddress(this.getHostMacAddress().toString());
        cmd.setPrivateNetmask("255.255.0.0");
        cmd.setIqn("iqn:"+_url);
        cmd.setStorageIpAddress(getHostStoragePrivateIp());
        cmd.setStorageMacAddress(getHostStorageMacAddress().toString());
        cmd.setStorageIpAddressDeux(getHostStoragePrivateIp2());
        cmd.setStorageMacAddressDeux(getHostStorageMacAddress2().toString());
        cmd.setPublicIpAddress(getHostStoragePrivateIp());
        cmd.setPublicMacAddress(getHostStorageMacAddress().toString());
        cmd.setPublicNetmask("255.255.0.0");
        cmd.setVersion("1.0");

        return new StartupCommand[] {cmd};
	}
	
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _guid = (String)params.get("guid");
        _zone = (String)params.get("zone");
        _pod = (String)params.get("pod");
        _url = (String)params.get("url");
        
        _instanceId = getNextSequenceId();
        return true;
    }
    
    public static int getNextSequenceId() {
    	return s_nextSequence++;
    }
    
	public MacAddress getHostMacAddress() {
		long address = 0;
		
		address = (_prefix & 0xff);
		address <<= 40;
		address |= _instanceId;
		return new MacAddress(address);
	}
	
	public String getHostPrivateIp() {
		int id = _instanceId;
		
		return "172.16." +
			String.valueOf((id >> 8) & 0xff) + "." +
			String.valueOf(id & 0xff);
	}
	
	public MacAddress getHostStorageMacAddress() {
		long address = 0;
		
		address = (_prefix & 0xff);
		address <<= 40;
		address |= (_instanceId | (1L << 31)) & 0xffffffff;
		return new MacAddress(address);
	}
	
	public MacAddress getHostStorageMacAddress2() {
		long address = 0;
		
		address = (_prefix & 0xff);
		address <<= 40;
		address |= (_instanceId | (3L << 30)) & 0xffffffff;
		return new MacAddress(address);
	}
	
	public String getHostStoragePrivateIp() {
		int id = _instanceId;
		id |= 1 << 15;
		
		return "172.16." +
			String.valueOf((id >> 8) & 0xff) + "." +
			String.valueOf(id & 0xff);
	}
	
	public String getHostStoragePrivateIp2() {
		int id = _instanceId;
		id |= 3 << 14;
		
		return "172.16." +
			String.valueOf((id >> 8) & 0xff) + "." +
			String.valueOf((id) & 0xff);
	}
}
