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

import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public interface PhysicalNetwork extends Identity, InternalIdentity {

    public enum State {
        Disabled, Enabled;
    }

    public class IsolationMethod {
        protected static final String UNKNOWN_PROVIDER = "Unknown";
        private static Set<IsolationMethod> registeredIsolationMethods = new HashSet<>();

        /**
         * gets a IsolationMethod object that defines this prefix and if any it returns the first one found that has a known provider. If none has a known provider
         * it will return the one with the unknown provider. if none is found it return null.
         *
         * @param prfx
         * @return
         */
        public static IsolationMethod getIsolationMethod(String prfx) {
            IsolationMethod rc = null;
            for (IsolationMethod method: registeredIsolationMethods) {
                if (method.provider.equals(prfx)) {
                    rc = method;
                    if(! rc.getProvider().equals(UNKNOWN_PROVIDER)) {
                        break;
                    }
                }
            }
            return rc;
        }

        public String getMethodPrefix() {
            return methodPrefix;
        }

        public String getProvider() {
            return provider;
        }

        String methodPrefix;
        String provider;

        // VLAN, L3, GRE, STT, BCF_SEGMENT, MIDO, SSP, VXLAN, ODL, L3VPN, VSP, VCS;

        public IsolationMethod(String prfx) {
            this(prfx, UNKNOWN_PROVIDER);
        }

        public IsolationMethod(String prfx, String prvdr) {
            methodPrefix = prfx;
            provider = StringUtils.isNotBlank(prvdr)? prvdr : UNKNOWN_PROVIDER;
            registeredIsolationMethods.add(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            IsolationMethod that = (IsolationMethod)o;
            return Objects.equals(methodPrefix, that.methodPrefix) && Objects.equals(provider, that.provider);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodPrefix, provider);
        }

        @Override
        public String toString() {
            return methodPrefix.toString();
        }

        public static boolean remove(String prfx, String prvdr) {
            if(prvdr == null || prvdr.isEmpty()) {
                prvdr = UNKNOWN_PROVIDER;
            }
            return remove(new IsolationMethod(prfx, prvdr));
        }
        public static boolean remove(IsolationMethod method) {
            return registeredIsolationMethods.remove(method);
        }
    }

    public enum BroadcastDomainRange {
        POD, ZONE;
    }

    BroadcastDomainRange getBroadcastDomainRange();

    // TrafficType getTrafficType();

    long getDataCenterId();

    State getState();

    List<String> getTags();

    List<String> getIsolationMethods();

    Long getDomainId();

    List<Pair<Integer, Integer>> getVnet();

    String getVnetString();

    String getSpeed();

    String getName();

}
