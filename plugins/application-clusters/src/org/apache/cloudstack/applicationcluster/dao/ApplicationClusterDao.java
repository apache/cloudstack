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
package org.apache.cloudstack.applicationcluster.dao;

import org.apache.cloudstack.applicationcluster.ApplicationCluster;
import org.apache.cloudstack.applicationcluster.ApplicationClusterVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

import java.util.List;

public interface ApplicationClusterDao extends GenericDao<ApplicationClusterVO, Long>,
        StateDao<ApplicationCluster.State, ApplicationCluster.Event, ApplicationCluster> {

    List<ApplicationClusterVO> listByAccount(long accountId);
    List<ApplicationClusterVO> findClustersToGarbageCollect();
    List<ApplicationClusterVO> findClustersInState(ApplicationCluster.State state);
    List<ApplicationClusterVO> listByNetworkId(long networkId);
}
