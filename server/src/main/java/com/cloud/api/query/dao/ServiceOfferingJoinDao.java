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
import java.util.Map;

import org.apache.cloudstack.api.response.ServiceOfferingResponse;

import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.db.GenericDao;

public interface ServiceOfferingJoinDao extends GenericDao<ServiceOfferingJoinVO, Long> {

    List<ServiceOfferingJoinVO> findByDomainId(long domainId);

    ServiceOfferingResponse newServiceOfferingResponse(ServiceOfferingJoinVO offering);

    ServiceOfferingJoinVO newServiceOfferingView(ServiceOffering offering);

    Map<Long, List<String>> listDomainsOfServiceOfferingsUsedByDomainPath(String domainPath);

    List<ServiceOfferingJoinVO> searchByIds(Long... id);
}
