// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
// 
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.vpn;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.api.commands.ListRemoteAccessVpnsCmd;
import com.cloud.api.commands.ListVpnUsersCmd;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.utils.component.Manager;

@Local(value = RemoteAccessVpnService.class)
public class MockRemoteAccessVpnManagerImpl implements RemoteAccessVpnService, Manager {

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteAccessVpn createRemoteAccessVpn(long vpnServerAddressId,
			String ipRange, boolean openFirewall, long networkId)
			throws NetworkRuleConflictException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroyRemoteAccessVpn(long vpnServerAddressId)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RemoteAccessVpn startRemoteAccessVpn(long vpnServerAddressId,
			boolean openFirewall) throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VpnUser addVpnUser(long vpnOwnerId, String userName, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeVpnUser(long vpnOwnerId, String userName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<? extends VpnUser> listVpnUsers(long vpnOwnerId, String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean applyVpnUsers(long vpnOwnerId, String userName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<? extends RemoteAccessVpn> searchForRemoteAccessVpns(
			ListRemoteAccessVpnsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends VpnUser> searchForVpnUsers(ListVpnUsersCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends RemoteAccessVpn> listRemoteAccessVpns(long networkId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteAccessVpn getRemoteAccessVpn(long vpnId) {
		// TODO Auto-generated method stub
		return null;
	}
}
