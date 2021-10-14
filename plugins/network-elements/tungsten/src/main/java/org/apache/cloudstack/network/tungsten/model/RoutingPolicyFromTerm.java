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

import net.juniper.tungsten.api.types.PrefixMatchType;
import net.juniper.tungsten.api.types.TermMatchConditionType;

import java.util.ArrayList;
import java.util.List;

public class RoutingPolicyFromTerm {
    private final List<String> communities;
    private final boolean matchAll;
    private final List<String> protocolList;
    private final List<RoutingPolicyPrefix> prefixList;

    public RoutingPolicyFromTerm(List<String> communities, boolean matchAll, List<String> protocolList, List<RoutingPolicyPrefix> prefixList) {
        this.communities = communities;
        this.matchAll = matchAll;
        this.protocolList = protocolList;
        this.prefixList = prefixList;
    }

    public RoutingPolicyFromTerm(TermMatchConditionType term) {
        this.matchAll = term.getCommunityMatchAll();
        this.protocolList = term.getProtocol();
        this.communities = term.getCommunityList();
        List<RoutingPolicyPrefix> prefixList = new ArrayList<>();
        for(PrefixMatchType item : term.getPrefix()) {
            prefixList.add(new RoutingPolicyPrefix(item.getPrefix(), item.getPrefixType()));
        }
        this.prefixList = prefixList;
    }

    public List<String> getCommunities() {
        return communities;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public List<String> getProtocolList() {
        return protocolList;
    }

    public List<RoutingPolicyPrefix> getPrefixList() {
        return prefixList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoutingPolicyFromTerm that = (RoutingPolicyFromTerm) o;

        if (matchAll != that.matchAll) return false;
        if (communities != null ? !communities.equals(that.communities) : that.communities != null) return false;
        if (protocolList != null ? !protocolList.equals(that.protocolList) : that.protocolList != null) return false;
        return prefixList != null ? prefixList.equals(that.prefixList) : that.prefixList == null;

    }

    @Override
    public int hashCode() {
        int result = communities != null ? communities.hashCode() : 0;
        result = 31 * result + (matchAll ? 1 : 0);
        result = 31 * result + (protocolList != null ? protocolList.hashCode() : 0);
        result = 31 * result + (prefixList != null ? prefixList.hashCode() : 0);
        return result;
    }
}
