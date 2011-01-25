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

package com.cloud.agent.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.annotations.Expose;

public class SecStorageFirewallCfgCommand extends Command {
	private static final Logger s_logger = Logger.getLogger(SecStorageFirewallCfgCommand.class);


	public static class PortConfig {
		@Expose boolean add;
		@Expose String sourceIp;
		@Expose String port;
		@Expose String intf;
		public PortConfig(String sourceIp, String port, boolean add, String intf) {
			this.add = add;
			this.sourceIp = sourceIp;
			this.port = port;
			this.intf = intf;
		}
		public PortConfig() {
			
		}
		public boolean isAdd() {
			return add;
		}
		public String getSourceIp() {
			return sourceIp;
		}
		public String getPort() {
			return port;
		}
		public String getIntf() {
			return intf;
		}
	}
	
	@Expose
	private List<PortConfig> portConfigs = new ArrayList<PortConfig>();
	
	public SecStorageFirewallCfgCommand() {
		
	}
    
    
    public void addPortConfig(String sourceIp, String port, boolean add, String intf) {
    	PortConfig pc = new PortConfig(sourceIp, port, add, intf);
    	this.portConfigs.add(pc);
    	
    }

	@Override
    public boolean executeInSequence() {
        return false;
    }


	public List<PortConfig> getPortConfigs() {
		return portConfigs;
	}
}
