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

package com.cloud.utils.net;

import java.io.Serializable;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.SerialVersionUID;

/**
 * Simple Ip implementation class that works with both ip4 and ip6.
 *
 */
public class Ip implements Serializable, Comparable<Ip> {

    private static final long serialVersionUID = SerialVersionUID.Ip;

    long ip;

    public Ip(long ip) {
        this.ip = ip;
    }

    public Ip(String ip) {
        this.ip = NetUtils.ip2Long(ip);
    }

    protected Ip() {
    }

    public String addr() {
        return toString();
    }

    public long longValue() {
        return ip;
    }

    @Override
    public String toString() {
        return NetUtils.long2Ip(ip);
    }

    public boolean isIp4() {
        return ip <= 2L * Integer.MAX_VALUE + 1;
    }

    public boolean isIp6() {
        return ip > Integer.MAX_VALUE;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Ip) {
            return ip == ((Ip)obj).ip;
        }
        return false;
    }

    public boolean isSameAddressAs(Object obj) {
        if (this.equals(obj)) {
            return true;
        } else if (obj instanceof String) {
            return ip == NetUtils.ip2Long((String)obj);
        } else if (obj instanceof Long) {
            return ip == (Long)obj;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Ip that) {
        return (int)(this.ip - that.ip);
    }
}
