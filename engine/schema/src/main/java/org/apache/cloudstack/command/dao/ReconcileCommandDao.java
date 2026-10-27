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

package org.apache.cloudstack.command.dao;

import com.cloud.agent.api.Command;
import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.command.ReconcileCommandVO;

import java.util.List;

public interface ReconcileCommandDao extends GenericDao<ReconcileCommandVO, Long> {

    List<ReconcileCommandVO> listByManagementServerId(long managementServerId);

    List<ReconcileCommandVO> listByHostId(long hostId);

    List<ReconcileCommandVO> listByState(Command.State... states);

    void removeCommand(long commandId, String commandName, Command.State state);

    ReconcileCommandVO findCommand(long reqSequence, String commandName);

    void updateCommandsToInterruptedByManagementServerId(long managementServerId);

    void updateCommandsToInterruptedByHostId(long hostId);

    List<ReconcileCommandVO> listByResourceIdAndTypeAndStates(long resourceId, ApiCommandResourceType resourceType, Command.State... states);
}
