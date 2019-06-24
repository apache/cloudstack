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

package com.cloud.network.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.resource.ServerResource;

public class CreateLoadBalancerApplianceAnswer extends Answer {
    String deviceName;
    String providerName;
    ServerResource serverResource;
    String username;
    String password;
    String publicInterface;
    String privateInterface;

    public CreateLoadBalancerApplianceAnswer(Command cmd, boolean success, String details, String deviceName, String providerName, ServerResource serverResource,
            String publicInterface, String privateInterface, String username, String password) {
        this.deviceName = deviceName;
        this.providerName = providerName;
        this.serverResource = serverResource;
        this.result = success;
        this.details = details;
        this.username = username;
        this.password = password;
        this.publicInterface = publicInterface;
        this.privateInterface = privateInterface;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getProviderName() {
        return providerName;
    }

    public ServerResource getServerResource() {
        return serverResource;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPublicInterface() {
        return publicInterface;
    }

    public String getPrivateInterface() {
        return privateInterface;
    }
}
