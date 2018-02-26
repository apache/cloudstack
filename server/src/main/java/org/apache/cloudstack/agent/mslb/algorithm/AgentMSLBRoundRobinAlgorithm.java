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
package org.apache.cloudstack.agent.mslb.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.agent.mslb.AgentMSLBAlgorithm;

public class AgentMSLBRoundRobinAlgorithm implements AgentMSLBAlgorithm {

    private static final long LAST_ID_FOR_NOT_EXISTING_HOSTS = 0l;

    /**
     * Get RR index for a hostId depending on its order in the hosts list
     * @param msList
     * @param orderedHostList
     * @param hostId
     * @return
     */
    private int findRoundRobinIndexForHost(final List<String> msList, final List<Long> orderedHostList, final Long hostId) {
        return orderedHostList.indexOf(hostId) % msList.size();
    }

    /**
     * When discovering new hosts there is no host id on DB, in order to apply RR algorithm an entry is added at the end of the hosts list
     * @param hostId host id
     * @param orderedHostList hosts list
     * @return added id on the list if hostId is null, hostId if not
     */
    private Long getHostForRoundRobinAlgorithm(Long hostId, List<Long> orderedHostList) {
        if (hostId == null) {
            orderedHostList.add(LAST_ID_FOR_NOT_EXISTING_HOSTS);
            return LAST_ID_FOR_NOT_EXISTING_HOSTS;
        }
        return hostId;
    }

    /**
     * If an entry was added at the end of the list it is removed (when hostId is null)
     * @param hostId host id
     * @param orderedHostList hosts list
     */
    private void revertChangesAfterApplyingRoundRobinAlgorithm(Long hostId, List<Long> orderedHostList) {
        if (hostId == null) {
            orderedHostList.remove(orderedHostList.size()-1);
        }
    }

    @Override
    public List<String> getMSList(final List<String> msList, final List<Long> orderedHostList, final Long hostId) {
        if (msList.size() < 2) {
            return msList;
        }
        Long host = getHostForRoundRobinAlgorithm(hostId, orderedHostList);

        final int currentRRIndex = findRoundRobinIndexForHost(msList, orderedHostList, host);
        final List<String> roundRobin = new ArrayList<>(msList.subList(currentRRIndex, msList.size()));
        roundRobin.addAll(msList.subList(0, currentRRIndex));

        revertChangesAfterApplyingRoundRobinAlgorithm(hostId, orderedHostList);
        return roundRobin;
    }

    @Override
    public String getName() {
        return "roundrobin";
    }

    @Override
    public boolean isMSListEqual(List<String> msList, List<String> receivedMsList) {
        if (msList.size() != receivedMsList.size()) return false;
        for (int i = 0; i < msList.size(); i++) {
            if (!msList.get(i).equals(receivedMsList.get(i))) {
                return false;
            }
        }
        return true;
    }
}