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

public interface IndirectAgentLB {

    /**
     * Return list of management server addresses after applying configured lb algorithm
     * for a host in a zone.
     * @param hostId host id (if present)
     * @param dcId zone id
     * @param orderedHostIdList (optional) list of ordered host id list
     * @return management servers string list
     */
    List<String> getManagementServerList(Long hostId, Long dcId, List<Long> orderedHostIdList);

    /**
     * Compares received management server list against expected list for a host in a zone.
     * @param hostId host id
     * @param dcId zone id
     * @param receivedMSHosts received management server list
     * @return true if mgmtHosts is up to date, false if not
     */
    boolean compareManagementServerList(Long hostId, Long dcId, List<String> receivedMSHosts, String lbAlgorithm);

    /**
     * Returns the configure LB algorithm
     * @return returns algorithm name
     */
    String getLBAlgorithmName();

    /**
     * Returns the configured LB preferred host check interval (if applicable at cluster scope)
     * @return returns interval in seconds
     */
    Long getLBPreferredHostCheckInterval(Long clusterId);

    void propagateMSListToAgents();

}
