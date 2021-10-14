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

public class RoutingPolicyPrefix {

    private final String prefix;
    private final String prefixType;

    public RoutingPolicyPrefix(String prefix, String prefixType) {
        this.prefix = prefix;
        this.prefixType = prefixType;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPrefixType() {
        return prefixType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoutingPolicyPrefix that = (RoutingPolicyPrefix) o;

        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) return false;
        return prefixType != null ? prefixType.equals(that.prefixType) : that.prefixType == null;

    }

    @Override
    public int hashCode() {
        int result = prefix != null ? prefix.hashCode() : 0;
        result = 31 * result + (prefixType != null ? prefixType.hashCode() : 0);
        return result;
    }
}
