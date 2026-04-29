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
package com.cloud.offerings.dao;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDao;

import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offerings.NetworkOfferingDetailsVO;

public interface NetworkOfferingDetailsDao extends ResourceDetailsDao<NetworkOfferingDetailsVO> {
    Map<NetworkOffering.Detail, String> getNtwkOffDetails(long offeringId);
    String getDetail(long offeringId, Detail detailName);
    List<Long> findDomainIds(final long resourceId);
    List<Long> findZoneIds(final long resourceId);
}
