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
package com.cloud.api.query.dao;

import java.util.List;

import com.cloud.network.Networks;
import org.apache.cloudstack.api.response.DomainRouterResponse;

import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDao;

public interface DomainRouterJoinDao extends GenericDao<DomainRouterJoinVO, Long> {

    DomainRouterResponse newDomainRouterResponse(DomainRouterJoinVO uvo, Account caller);

    DomainRouterResponse setDomainRouterResponse(DomainRouterResponse userVmData, DomainRouterJoinVO uvo);

    List<DomainRouterJoinVO> newDomainRouterView(VirtualRouter vr);

    List<DomainRouterJoinVO> searchByIds(Long... ids);

    List<DomainRouterJoinVO> getRouterByIdAndTrafficType(Long id, Networks.TrafficType... trafficType);
}
