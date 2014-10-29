//
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
//

package com.cloud.agent.api.routing;

public class UserDataCommand extends NetworkElementCommand {

    String userData;
    String vmIpAddress;
    String routerPrivateIpAddress;
    String vmName;
    boolean executeInSequence = false;

    protected UserDataCommand() {

    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

    public UserDataCommand(String userData, String vmIpAddress, String routerPrivateIpAddress, String vmName, boolean executeInSequence) {
        this.userData = userData;
        this.vmIpAddress = vmIpAddress;
        this.routerPrivateIpAddress = routerPrivateIpAddress;
        this.vmName = vmName;
        this.executeInSequence = executeInSequence;
    }

    public String getRouterPrivateIpAddress() {
        return routerPrivateIpAddress;
    }

    public String getVmIpAddress() {
        return vmIpAddress;
    }

    public String getVmName() {
        return vmName;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

}
