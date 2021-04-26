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
package org.apache.cloudstack.network.tungsten.model;

import com.cloud.network.dao.IPAddressVO;
import net.juniper.tungsten.api.types.FloatingIp;

import java.util.Objects;

public class TungstenFloatingIP implements TungstenModel {
    private final String ip;

    public TungstenFloatingIP(final String ip) {
        this.ip = ip;
    }

    public TungstenFloatingIP(final IPAddressVO ipAddress) {
        this.ip = ipAddress.getAddress().addr();
    }

    public TungstenFloatingIP(final FloatingIp floatingIp) {
        this.ip = floatingIp.getAddress();
    }

    public String getIp() {
        return ip;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;

        if (o == null) {
            return false;
        }

        if (o instanceof IPAddressVO) {
            final IPAddressVO ipAddress = (IPAddressVO) o;
            return ip.equals(ipAddress.getAddress().addr());
        }

        if (o instanceof FloatingIp) {
            final FloatingIp floatingIp = (FloatingIp) o;
            return ip.equals(floatingIp.getAddress());
        }

        if (o instanceof TungstenFloatingIP) {
            final TungstenFloatingIP that = (TungstenFloatingIP) o;
            return ip.equals(that.ip);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }
}
