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
package org.apache.cloudstack.engine.datacenter.entity.api;


import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.ClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostVO;

import com.cloud.utils.fsm.NoTransitionException;



public interface DataCenterResourceManager {
	
	DataCenterVO loadDataCenter(String dataCenterId);
	
	void saveDataCenter(DataCenterVO dc);
	
	void savePod(HostPodVO dc);
	
	void saveCluster(ClusterVO cluster);
	
	boolean changeState(DataCenterResourceEntity entity, Event event) throws NoTransitionException;
	
	HostPodVO loadPod(String uuid);
	
	ClusterVO loadCluster(String uuid);

	HostVO loadHost(String uuid);

	void saveHost(HostVO hostVO);

}
