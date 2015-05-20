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

public class GetVmIpAddressCommand extends Command {

    String vmName;
    String vmNetworkCidr;
    boolean windows = false;

    public GetVmIpAddressCommand(String vmName, String vmNetworkCidr, boolean windows) {
        this.vmName = vmName;
        this.windows = windows;
        this.vmNetworkCidr = vmNetworkCidr;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getVmName(){
        return vmName;
    }

    public boolean isWindows(){
        return windows;
    }

    public String getVmNetworkCidr() {
        return vmNetworkCidr;
    }
}
