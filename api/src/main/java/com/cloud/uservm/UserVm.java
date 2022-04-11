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
package com.cloud.uservm;

import org.apache.cloudstack.acl.ControlledEntity;

import com.cloud.vm.VirtualMachine;

/**
 * This represents one running virtual machine instance.
 */
public interface UserVm extends VirtualMachine, ControlledEntity {

    Long getIsoId();

    String getDisplayName();

    String getUserData();

    String getPassword();

    void setUserData(String userData);

    void setUserDataId(Long userDataId);

    Long getUserDataId();

    void setUserDataDetails(String userDataDetails);

    String getUserDataDetails();

    String getDetail(String name);

    void setAccountId(long accountId);

    public boolean isDisplayVm();
}
