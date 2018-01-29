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
package org.apache.cloudstack.agent.mslb;

import java.util.List;

public interface AgentMSLB {

    /**
     * Return list of management server addresses after applying configured MSLB algorithm
     * @param hostId host id (if present)
     * @param dcId zone id
     * @return management servers string list
     */
    List<String> getManagementServerList(Long hostId, Long dcId);

    /**
     * Return true if received management server list is up to date for hostId on dcId
     * @param hostId host id
     * @param dcId zone id
     * @param receivedMgmtHosts received management server list
     * @return true if mgmtHosts is up to date, false if not
     */
    boolean isManagementServerListUpToDate(Long hostId, Long dcId, List<String> receivedMgmtHosts);
}