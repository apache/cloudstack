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
package com.cloud.bridge.util;

/**
 * Represents a network IP address or a range of addresses.
 * A range is useful when representing IP addresses defined in
 * CIDR format.   The range is a 32 bit IP inclusive.
 */
public class IpAddressRange {

    private long minAddress;
    private long maxAddress;

    public IpAddressRange() {

    }

    public long getMinAddress() {
        return minAddress;
    }

    public void setMinAddress(long param) {
        this.minAddress = param;
    }

    public long getMaxAddress() {
        return maxAddress;
    }

    public void setMaxAddress(long param) {
        this.maxAddress = param;
    }

    public String toString() {
        StringBuffer value = new StringBuffer();
        value.append("(ip range min: " + minAddress);
        value.append(", max: " + maxAddress + ")");
        return value.toString();
    }

    /**
     * Is the parameter (i.e., left) inside the range represented by this object?
     * @param left
     * @return boolean
     */
    public boolean contains(IpAddressRange left) {
        long leftMin = left.getMinAddress();

        if (leftMin < minAddress || leftMin > maxAddress)
            return false;
        else
            return true;
    }

    public static IpAddressRange parseRange(String ipAddress) throws Exception {
        IpAddressRange range = null;
        long maskBits = 0;
        long address = 0;

        if (null == ipAddress)
            return null;

        // -> is it a CIDR format?
        String[] halfs = ipAddress.split("/");
        if (2 == halfs.length) {
            range = new IpAddressRange();
            address = IpAddressRange.ipToInt(halfs[0]);

            maskBits = Integer.parseInt(halfs[1]) & 0xFF;
            if (maskBits >= 1 && maskBits <= 32) {
                range.setMinAddress(address & (~((1 << (32 - maskBits)) - 1) & 0xFFFFFFFF));
                range.setMaxAddress(range.getMinAddress() | (((1 << (32 - maskBits)) - 1) & 0xFFFFFFFF));
            }
        } else if (1 == halfs.length) {
            // -> should be just a simple IP address
            range = new IpAddressRange();
            address = IpAddressRange.ipToInt(ipAddress);
            range.setMaxAddress(address);
            range.setMinAddress(address);
        } else
            throw new Exception("Invalid Ip Address: " + ipAddress);

        return range;
    }

    /**
     * In order to do unsigned math here we must use long types so that high order bits
     * are not used as the sign of the number.
     *
     * @param ipAddress
     * @return
     */
    private static long ipToInt(String ipAddress) throws Exception {
        String[] parts = ipAddress.split("[.]");
        if (4 != parts.length)
            throw new Exception("Invalid Ip Address: " + ipAddress);

        long[] address = new long[4];
        address[0] = Long.parseLong(parts[0]);
        address[1] = Long.parseLong(parts[1]);
        address[2] = Long.parseLong(parts[2]);
        address[3] = Long.parseLong(parts[3]);

        if (address[0] < 0 || address[1] < 0 || address[2] < 0 || address[3] < 0)
            throw new Exception("Invalid Ip Address: " + ipAddress);

        if (address[0] > 255 || address[1] > 255 || address[2] > 255 || address[3] > 255)
            throw new Exception("Invalid Ip Address: " + ipAddress);

        long value = (address[0] << 24) | (address[1] << 16) | (address[2] << 8) | address[3];
        return value;
    }
}
