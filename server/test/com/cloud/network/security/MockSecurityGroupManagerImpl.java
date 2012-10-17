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
package com.cloud.network.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.api.commands.AuthorizeSecurityGroupEgressCmd;
import com.cloud.api.commands.AuthorizeSecurityGroupIngressCmd;
import com.cloud.api.commands.CreateSecurityGroupCmd;
import com.cloud.api.commands.DeleteSecurityGroupCmd;
import com.cloud.api.commands.ListSecurityGroupsCmd;
import com.cloud.api.commands.RevokeSecurityGroupEgressCmd;
import com.cloud.api.commands.RevokeSecurityGroupIngressCmd;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;

@Local(value = { SecurityGroupManager.class, SecurityGroupService.class })
public class MockSecurityGroupManagerImpl implements SecurityGroupManager, SecurityGroupService, Manager, StateListener<State, VirtualMachine.Event, VirtualMachine> {

	@Override
	public boolean preStateTransitionEvent(State oldState, Event event,
			State newState, VirtualMachine vo, boolean status, Object opaque) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean postStateTransitionEvent(State oldState, Event event,
			State newState, VirtualMachine vo, boolean status, Object opaque) {
		// TODO Auto-generated method stub
		return false;
	}

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
	public SecurityGroup createSecurityGroup(CreateSecurityGroupCmd command)
			throws PermissionDeniedException, InvalidParameterValueException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean revokeSecurityGroupIngress(RevokeSecurityGroupIngressCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean revokeSecurityGroupEgress(RevokeSecurityGroupEgressCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteSecurityGroup(DeleteSecurityGroupCmd cmd)
			throws ResourceInUseException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<? extends SecurityGroupRules> searchForSecurityGroupRules(
			ListSecurityGroupsCmd cmd) throws PermissionDeniedException,
			InvalidParameterValueException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends SecurityRule> authorizeSecurityGroupIngress(
			AuthorizeSecurityGroupIngressCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends SecurityRule> authorizeSecurityGroupEgress(
			AuthorizeSecurityGroupEgressCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SecurityGroupVO createSecurityGroup(String name, String description,
			Long domainId, Long accountId, String accountName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SecurityGroupVO createDefaultSecurityGroup(Long accountId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addInstanceToGroups(Long userVmId, List<Long> groups) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeInstanceFromGroups(long userVmId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fullSync(long agentId,
			HashMap<String, Pair<Long, Long>> newGroupStates) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSecurityGroupsNamesForVm(long vmId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SecurityGroupVO> getSecurityGroupsForVm(long vmId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isVmSecurityGroupEnabled(Long vmId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SecurityGroup getDefaultSecurityGroup(long accountId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SecurityGroup getSecurityGroup(String name, long accountId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isVmMappedToDefaultSecurityGroup(long vmId) {
		// TODO Auto-generated method stub
		return false;
	}
   }
