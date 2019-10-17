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
package com.cloud.dc;

import java.util.Map;

import com.cloud.utils.db.GenericDao;

public interface ClusterDetailsDao extends GenericDao<ClusterDetailsVO, Long> {
    Map<String, String> findDetails(long clusterId);

    void persist(long clusterId, Map<String, String> details);

    void persist(long clusterId, String name, String value);

    ClusterDetailsVO findDetail(long clusterId, String name);

    void deleteDetails(long clusterId);

    String getVmwareDcName(Long clusterId);
}
