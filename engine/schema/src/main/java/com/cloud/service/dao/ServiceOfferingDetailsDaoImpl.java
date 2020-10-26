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
package com.cloud.service.dao;


import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.springframework.stereotype.Component;

import com.cloud.service.ServiceOfferingDetailsVO;

@Component
public class ServiceOfferingDetailsDaoImpl extends ResourceDetailsDaoBase<ServiceOfferingDetailsVO> implements ServiceOfferingDetailsDao {

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new ServiceOfferingDetailsVO(resourceId, key, value, display));
    }

    @Override
    public List<Long> findDomainIds(long resourceId) {
        final List<Long> domainIds = new ArrayList<>();
        for (final ServiceOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.DOMAIN_ID)) {
            final Long domainId = Long.valueOf(detail.getValue());
            if (domainId > 0) {
                domainIds.add(domainId);
            }
        }
        return domainIds;
    }

    @Override
    public List<Long> findZoneIds(long resourceId) {
        final List<Long> zoneIds = new ArrayList<>();
        for (final ServiceOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.ZONE_ID)) {
            final Long zoneId = Long.valueOf(detail.getValue());
            if (zoneId > 0) {
                zoneIds.add(zoneId);
            }
        }
        return zoneIds;
    }

    @Override
    public String getDetail(Long serviceOfferingId, String key) {
        String detailValue = null;
        ServiceOfferingDetailsVO serviceOfferingDetail = findDetail(serviceOfferingId, key);
        if (serviceOfferingDetail != null) {
            detailValue = serviceOfferingDetail.getValue();
        }
        return detailValue;
    }
}
