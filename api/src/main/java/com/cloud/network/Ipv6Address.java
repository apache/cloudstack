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
package com.cloud.network;

import java.util.Date;

import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;

/**
 *
 * - Allocated = null
 * - AccountId = null
 * - DomainId = null
 *
 * - State = Allocated
 * - AccountId = account owner.
 * - DomainId = domain of the account owner.
 * - Allocated = time it was allocated.
 */
public interface Ipv6Address extends Identity, InternalIdentity, Displayable {

    enum InternetProtocol {
        IPv4, IPv6, DualStack;

        public static InternetProtocol fromValue(String protocol) {
            if (StringUtils.isBlank(protocol)) {
                return null;
            } else if (protocol.equalsIgnoreCase("IPv4")) {
                return IPv4;
            } else if (protocol.equalsIgnoreCase("IPv6")) {
                return IPv6;
            } else if (protocol.equalsIgnoreCase("DualStack")) {
                return DualStack;
            }
            throw new InvalidParameterValueException("Unexpected Internet Protocol : " + protocol);
        }
    }

    enum IPv6Routing {
        Static, Dynamic;

        public static IPv6Routing fromValue(String mode) {
            if (StringUtils.isBlank(mode)) {
                return null;
            } else if (mode.equalsIgnoreCase("Static")) {
                return Static;
            } else if (mode.equalsIgnoreCase("Dynamic")) {
                return Dynamic;
            }
            throw new InvalidParameterValueException("Unexpected IPv6 routing mode : " + mode);
        }
    }

    long getDataCenterId();

    long getPhysicalNetworkId();

    String getIp6Gateway();

    String getIp6Cidr();

    String getRouterIpv6();

    String getRouterIpv6Gateway();

    Long getNetworkId();

    Long getDomainId();

    Long getAccountId();

    Date getTakenAt();

}
