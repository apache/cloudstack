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
package org.apache.cloudstack.network.lb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface LoadBalancerConfig extends Identity, InternalIdentity {

    public enum SSLConfiguration {
        NONE("none"), OLD("old"), INTERMEDIATE("intermediate");

        String _config;

        SSLConfiguration(String config) {
            _config = config;
        }

        @Override
        public String toString() {
            return _config;
        }

        public static boolean validate(String value) {
            for (SSLConfiguration config : SSLConfiguration.values()) {
                if (config.toString().equals(value)) {
                    return true;
                }
            }
            return false;
        }

        public static List<String> getValues() {
            ArrayList values = new ArrayList<String>();
            for (SSLConfiguration config : SSLConfiguration.values()) {
                values.add(config.toString());
            }
            return values;
        }
    }

    public enum Scope {
        Network, Vpc, LoadBalancerRule;
    }

    Scope getScope();

    Long getNetworkId();

    Long getVpcId();

    Long getLoadBalancerId();

    String getName();

    String getValue();

    String getDefaultValue();

    String getDisplayText();

    Date getCreated();

    Date getRemoved();
}
