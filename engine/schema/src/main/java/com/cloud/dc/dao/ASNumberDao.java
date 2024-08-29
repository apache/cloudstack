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
package com.cloud.dc.dao;

import com.cloud.dc.ASNumberVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface ASNumberDao extends GenericDao<ASNumberVO, Long> {

    Pair<List<ASNumberVO>, Integer> searchAndCountByZoneOrRangeOrAllocated(Long zoneId, Long asnRangeId, Integer asNumber, Long networkId, Long vpcId,
                                                                           Boolean allocated, Long accountId, Long domainId, String keyword, Account caller,
                                                                           Long startIndex, Long pageSizeVal);
    ASNumberVO findByAsNumber(Long asNumber);

    ASNumberVO findOneByAllocationStateAndZone(long zoneId, boolean allocated);

    List<ASNumberVO> listAllocatedByASRange(Long asRangeId);

    ASNumberVO findByZoneAndNetworkId(long zoneId, long networkId);
    ASNumberVO findByZoneAndVpcId(long zoneId, long vpcId);

    int removeASRangeNumbers(long rangeId);
}
