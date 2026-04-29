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

package org.apache.cloudstack.outofbandmanagement.dao;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementVO;

import java.util.List;

public interface OutOfBandManagementDao extends GenericDao<OutOfBandManagementVO, Long>,
        StateDao<OutOfBandManagement.PowerState, OutOfBandManagement.PowerState.Event, OutOfBandManagement> {
    OutOfBandManagement findByHost(long hostId);
    OutOfBandManagementVO findByHostAddress(String address);
    List<OutOfBandManagementVO> findAllByManagementServer(long serverId);
    void expireServerOwnership(long serverId);
}
