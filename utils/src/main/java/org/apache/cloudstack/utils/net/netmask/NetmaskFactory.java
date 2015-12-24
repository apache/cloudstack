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

package org.apache.cloudstack.utils.net.netmask;

import java.util.StringTokenizer;

import com.cloud.utils.net.NetUtils;

public class NetmaskFactory {

    public static boolean isValidNetmask(final String netmask) {
        if (!NetUtils.isValidIp(netmask)) {
            return false;
        }
        final long ip = NetUtils.ip2Long(netmask);
        int count = 0;
        boolean finished = false;
        for (int i = 31; i >= 0; i--) {
            if ((ip >> i & 0x1) == 0) {
                finished = true;
            } else {
                if (finished) {
                    return false;
                }
                count += 1;
            }
        }
        if (count == 0) {
            return false;
        }
        return true;
    }

    public static int getNetMask(String netMask) {
        StringTokenizer nm = new StringTokenizer(netMask, ".");
        int i = 0;
        int[] netmask = new int[4];
        while (nm.hasMoreTokens()) {
            netmask[i] = Integer.parseInt(nm.nextToken());
            i++;
        }
        int mask1 = 0;
        for (i = 0; i < 4; i++) {
            mask1 += Integer.bitCount(netmask[i]);
        }
        return mask1;
    }

}
