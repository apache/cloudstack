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
package com.cloud.vm;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.ItWorkVO.Step;
import com.cloud.vm.VirtualMachine.State;

public interface ItWorkDao extends GenericDao<ItWorkVO, String> {
    /**
     * find a work item based on the instanceId and the state.
     *
     * @param instanceId vm instance id
     * @param state state
     * @return ItWorkVO if found; null if not.
     */
    ItWorkVO findByOutstandingWork(long instanceId, State state);

    /**
     * cleanup rows that are either Done or Cancelled and been that way
     * for at least wait time.
     */
    void cleanup(long wait);

    boolean updateStep(ItWorkVO work, Step step);

    List<ItWorkVO> listWorkInProgressFor(long nodeId);

}
