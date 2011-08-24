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

package com.cloud.hypervisor.hyperv.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;

/**
 * Implementation of dummy resource to be returned from discoverer
 **/

public class HypervDummyResourceBase extends ServerResourceBase implements 
		ServerResource {

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
	public Answer executeRequest(Command cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getDefaultScriptsDir() {
		// TODO Auto-generated method stub
		return null;
	}

}
