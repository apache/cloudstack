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

package org.apache.cloudstack.utils.net.ipaddress;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.validator.routines.InetAddressValidator;

public class IpAddressFactory {

    public static boolean isValidIpv4(final String ip) {
        final InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValidInet4Address(ip);
    }

    public static boolean isValidIpv6(final String ip) {
        throw new Error("Unimplemented for ipv6");
    }

    public static boolean isValidIp(final String ip) {
        final InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValid(ip);
    }

    public static InetAddress createFromHostName(String addr) {
        try {
            return InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            throw new Error("The fomat of address string is not valid " + addr);
        }
    }

}
