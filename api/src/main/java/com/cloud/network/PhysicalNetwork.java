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

import com.cloud.exception.CloudException;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.StringUtils;

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
        static Set<IsolationMethod> registeredIsolationMethods = new HashSet<>();

        String methodPrefix;
        String provider;

        public IsolationMethod(String prfx) {
            this(prfx, UNKNOWN_PROVIDER);
        }

        public IsolationMethod(String prfx, String prvdr) {
            methodPrefix = prfx;
            provider = StringUtils.isNotBlank(prvdr)? prvdr : UNKNOWN_PROVIDER;
            registeredIsolationMethods.add(this);
        }

        /**
         * gets a IsolationMethod object that defines this prefix and if any it returns the first one found that has a known provider. If none has a known provider
         * it will return the one with the unknown provider. if none is found it return null.
         *
         * @param prfx
         * @return
         */
        public static IsolationMethod getIsolationMethod(String prfx) throws IsolationMethodNotRegistered {
            IsolationMethod rc = null;
            for (IsolationMethod method: registeredIsolationMethods) {
                if (method.methodPrefix.equals(prfx)) {
                    rc = method;
                    if(! rc.getProvider().equals(UNKNOWN_PROVIDER)) {
                        break;
                    }
                }
            }
            if (rc == null) {
                throw new IsolationMethodNotRegistered("No registration of prefix '" + prfx + "' found.");
            }
            return rc;
        }

        public static IsolationMethod getIsolationMethod(String prfx, String provider) throws IsolationMethodNotRegistered {
            for (IsolationMethod method: registeredIsolationMethods) {
                if (method.methodPrefix.equals(prfx) && method.provider.equals(provider)) {
                    return method;
                }
            }
            throw new IsolationMethodNotRegistered("No registration of prefix '" + prfx + "' for provider '" + provider + "' found.");
        }

        static class IsolationMethodNotRegistered extends CloudException {
            IsolationMethodNotRegistered (String message) {
                super(message);
            }
        }

        public String getMethodPrefix() {
            return methodPrefix;
        }

        public String getProvider() {
            return provider;
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
            return methodPrefix;
        }

        public static boolean remove(String prfx, String prvdr) {
            prvdr = StringUtils.isNotBlank(prvdr)? prvdr : UNKNOWN_PROVIDER;

            try {
                return remove(getIsolationMethod(prfx, prvdr));
            } catch (IsolationMethodNotRegistered isolationMethodNotRegistered) {
                return false;
            }
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
