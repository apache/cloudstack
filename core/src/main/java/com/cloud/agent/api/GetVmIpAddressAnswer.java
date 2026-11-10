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

/**
 * Answer to {@link GetVmIpAddressCommand}. The IPv4 address is carried in {@link #getDetails()} for
 * compatibility with agents answering with a plain {@link Answer}; the (global) IPv6 address, if any, is optional.
 */
public class GetVmIpAddressAnswer extends Answer {

    private String ip6Address;

    public GetVmIpAddressAnswer() {
    }

    public GetVmIpAddressAnswer(GetVmIpAddressCommand command, boolean result, String ip4Address, String ip6Address) {
        super(command, result, ip4Address);
        this.ip6Address = ip6Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }
}
