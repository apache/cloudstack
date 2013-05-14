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
package com.cloud.network.dao;

import com.cloud.network.cisco.NetworkAsa1000vMapVO;
import com.cloud.utils.db.GenericDao;

public interface NetworkAsa1000vMapDao extends GenericDao<NetworkAsa1000vMapVO, Long>{

    NetworkAsa1000vMapVO findByNetworkId(long networkId);

    NetworkAsa1000vMapVO findByAsa1000vId(long asa1000vId);

}
