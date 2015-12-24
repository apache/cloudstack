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

package com.cloud.utils.net.cidr;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.cloud.utils.net.NetUtils;

public class CIDRFactory {

    public static CIDR getCIDR(InetAddress baseAddress, int cidrMask) throws BadCIDRException {
        if (cidrMask < 0) {
            throw new BadCIDRException("Invalid mask length used: " + cidrMask);
        }
        if (baseAddress instanceof Inet4Address) {
            if (cidrMask > 32) {
                throw new BadCIDRException("Invalid mask length used: " + cidrMask);
            }
            return new CIDR4((Inet4Address)baseAddress, cidrMask);
        }
        // IPv6.
        if (cidrMask > 128) {
            throw new BadCIDRException("Invalid mask length used: " + cidrMask);
        }
        return new CIDR6((Inet6Address)baseAddress, cidrMask);
    }

    public static CIDR getCIDR(String cidr) throws BadCIDRException {
        int p = cidr.indexOf('/');
        if (p < 0) {
            throw new BadCIDRException("Invalid CIDR notation used: " + cidr);
        }
        String addrString = cidr.substring(0, p);
        String maskString = cidr.substring(p + 1);
        InetAddress addr = addressStringToInet(addrString);
        int mask;
        if (maskString.indexOf('.') < 0) {
            mask = Integer.decode(maskString);
        } else {
            mask = NetUtils.getNetMask(maskString);
            if (addr instanceof Inet6Address) {
                mask += 96;
            }
        }
        if (mask < 0) {
            throw new BadCIDRException("Invalid mask length used: " + maskString);
        }
        return getCIDR(addr, mask);
    }

    public static InetAddress addressStringToInet(String addr) throws BadCIDRException {
        try {
            return InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            throw new BadCIDRException("The fomat of address string is not valid " + addr);
        }
    }

}
