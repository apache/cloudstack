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
package org.apache.cloudstack.agent.lb.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.agent.lb.IndirectAgentLBAlgorithm;

public class IndirectAgentLBRoundRobinAlgorithm implements IndirectAgentLBAlgorithm {

    private int findRRPivotIndex(final List<String> msList, final List<Long> orderedHostList, final Long hostId) {
        return orderedHostList.indexOf(hostId) % msList.size();
    }

    @Override
    public List<String> sort(final List<String> msList, final List<Long> orderedHostList, final Long hostId) {
        if (msList.size() < 2) {
            return msList;
        }

        final List<Long> hostList = new ArrayList<>(orderedHostList);
        Long searchId = hostId;
        if (hostId == null) {
            searchId = -1L;
            hostList.add(searchId);
        }

        final int pivotIndex = findRRPivotIndex(msList, hostList, searchId);
        final List<String> roundRobin = new ArrayList<>(msList.subList(pivotIndex, msList.size()));
        roundRobin.addAll(msList.subList(0, pivotIndex));

        return roundRobin;
    }

    @Override
    public String getName() {
        return "roundrobin";
    }

    @Override
    public boolean compare(final List<String> msList, final List<String> receivedMsList) {
        return msList != null && receivedMsList != null && msList.equals(receivedMsList);
    }
}
