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
package com.cloud.region;

import java.util.List;

import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.command.user.region.ListRegionsCmd;

import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;


public interface RegionService {
	public Region addRegion(int id, String name, String endPoint, String apiKey, String secretKey);
	public Region updateRegion(int id, String name, String endPoint, String apiKey, String secretKey);
	public boolean removeRegion(int id);
	public List<? extends Region> listRegions(ListRegionsCmd cmd);
	boolean deleteUserAccount(long accountId);
	Account updateAccount(UpdateAccountCmd cmd);
	public Account disableAccount(String accountName, Long domainId, Long id, Boolean lockRequested) throws ConcurrentOperationException, ResourceUnavailableException;
	public Account enableAccount(String accountName, Long domainId, Long id);
	public boolean deleteUser(DeleteUserCmd deleteUserCmd);
	public boolean deleteDomain(Long id, Boolean cleanup);
	public UserAccount updateUser(UpdateUserCmd updateUserCmd);
	public Domain updateDomain(UpdateDomainCmd updateDomainCmd);
	public UserAccount disableUser(Long id);
	public UserAccount enableUser(Long id);
}
