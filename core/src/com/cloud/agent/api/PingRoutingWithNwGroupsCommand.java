/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
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

import java.util.HashMap;
import java.util.Map;

import com.cloud.host.Host.Type;
import com.cloud.utils.Pair;
import com.cloud.vm.State;


public class PingRoutingWithNwGroupsCommand extends PingRoutingCommand {
	HashMap<String, Pair<Long, Long>> newGroupStates;

	protected PingRoutingWithNwGroupsCommand() {
		super();
	}

	public PingRoutingWithNwGroupsCommand(Type type, long id, Map<String, State> states, HashMap<String, Pair<Long, Long>> nwGrpStates) {
		super(type, id, states);
		newGroupStates = nwGrpStates;
	}

	public HashMap<String, Pair<Long, Long>> getNewGroupStates() {
		return newGroupStates;
	}

	public void setNewGroupStates(HashMap<String, Pair<Long, Long>> newGroupStates) {
		this.newGroupStates = newGroupStates;
	}

}
