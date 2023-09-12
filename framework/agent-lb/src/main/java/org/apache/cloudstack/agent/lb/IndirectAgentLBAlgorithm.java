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

package org.apache.cloudstack.agent.lb;

import java.util.List;

public interface IndirectAgentLBAlgorithm {
    /**
     * Returns a sorted management server list to send to host after applying the algorithm
     * @param msList management server list
     * @param orderedHostList ordered host list
     * @param hostId host id
     * @return returns the list of management server addresses which will be sent to host id
     */
    List<String> sort(final List<String> msList, final List<Long> orderedHostList, final Long hostId);

    /**
     * Gets the unique name of the algorithm
     * @return returns the name of the Agent MSLB algorithm
     */
    String getName();

    /**
     * Compares and return if received mgmt server list is equal to the actual mgmt server lists
     * @param msList current mgmt server list
     * @param receivedMsList received mgmt server list
     * @return true if the lists are equal, false if not
     */
    boolean compare(final List<String> msList, final List<String> receivedMsList);
}
