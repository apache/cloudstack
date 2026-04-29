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
package com.cloud.agent.api;

import java.util.Map;

import com.cloud.agent.api.to.VirtualMachineTO;

public class PrepareExternalProvisioningAnswer extends Answer {

    Map<String, String> serverDetails;
    VirtualMachineTO virtualMachineTO;

    public PrepareExternalProvisioningAnswer() {
        super();
    }

    public PrepareExternalProvisioningAnswer(PrepareExternalProvisioningCommand cmd, Map<String, String> externalDetails, VirtualMachineTO virtualMachineTO, String details) {
        super(cmd, true, details);
        this.serverDetails = externalDetails;
        this.virtualMachineTO = virtualMachineTO;
    }

    public PrepareExternalProvisioningAnswer(PrepareExternalProvisioningCommand cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public Map<String, String> getServerDetails() {
        return serverDetails;
    }

    public VirtualMachineTO getVirtualMachineTO() {
        return virtualMachineTO;
    }
}
