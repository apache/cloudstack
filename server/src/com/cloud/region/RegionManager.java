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

import java.util.Map;


public interface RegionManager {
	public boolean propogateAddAccount(String userName, String password, String firstName, String lastName, String email, String timezone, String accountName, short accountType, Long domainId, String networkDomain,
            Map<String, String> details, String accountUUID, String userUUID);
	public long getId();
	public void setId(long id);
	public void propogateAddUser(String userName, String password,
			String firstName, String lastName, String email, String timeZone,
			String accountName, String domainUUId, String userUUID);
	public void propogateAddDomain(String name, Long parentId, String networkDomain, String uuid);
}
