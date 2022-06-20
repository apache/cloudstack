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
package com.cloud.agent.api.routing;

import com.cloud.agent.api.to.IpAddressTO;

import java.util.Arrays;

public class UpdateNetworkCommand extends NetworkElementCommand{
    IpAddressTO[] ipAddresses;

    public UpdateNetworkCommand(IpAddressTO[] ips) {
        this.ipAddresses = ips;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public IpAddressTO[] getIpAddresses() {
        return ipAddresses;
    }

    @Override
    public int getAnswersCount() {
        return ipAddresses.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateNetworkCommand command = (UpdateNetworkCommand) o;
        return Arrays.equals(ipAddresses, command.ipAddresses);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(ipAddresses);
        return result;
    }
}
