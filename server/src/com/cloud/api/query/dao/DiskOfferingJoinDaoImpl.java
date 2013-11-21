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

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.response.DiskOfferingResponse;

import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {DiskOfferingJoinDao.class})
public class DiskOfferingJoinDaoImpl extends GenericDaoBase<DiskOfferingJoinVO, Long> implements DiskOfferingJoinDao {
    public static final Logger s_logger = Logger.getLogger(DiskOfferingJoinDaoImpl.class);

    private final SearchBuilder<DiskOfferingJoinVO> dofIdSearch;
    private final Attribute _typeAttr;

     protected DiskOfferingJoinDaoImpl() {

        dofIdSearch = createSearchBuilder();
        dofIdSearch.and("id", dofIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        dofIdSearch.done();

        _typeAttr = _allAttributes.get("type");

        _count = "select count(distinct id) from disk_offering_view WHERE ";
    }

    @Override
    public DiskOfferingResponse newDiskOfferingResponse(DiskOfferingJoinVO offering) {

        DiskOfferingResponse diskOfferingResponse = new DiskOfferingResponse();
        diskOfferingResponse.setId(offering.getUuid());
        diskOfferingResponse.setName(offering.getName());
        diskOfferingResponse.setDisplayText(offering.getDisplayText());
        diskOfferingResponse.setCreated(offering.getCreated());
        diskOfferingResponse.setDiskSize(offering.getDiskSize() / (1024 * 1024 * 1024));
        diskOfferingResponse.setMinIops(offering.getMinIops());
        diskOfferingResponse.setMaxIops(offering.getMaxIops());

        diskOfferingResponse.setDomain(offering.getDomainName());
        diskOfferingResponse.setDomainId(offering.getDomainUuid());
        diskOfferingResponse.setDisplayOffering(offering.isDisplayOffering());

        diskOfferingResponse.setTags(offering.getTags());
        diskOfferingResponse.setCustomized(offering.isCustomized());
        diskOfferingResponse.setCustomizedIops(offering.isCustomizedIops());
        diskOfferingResponse.setStorageType(offering.isUseLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared.toString());
        diskOfferingResponse.setBytesReadRate(offering.getBytesReadRate());
        diskOfferingResponse.setBytesWriteRate(offering.getBytesWriteRate());
        diskOfferingResponse.setIopsReadRate(offering.getIopsReadRate());
        diskOfferingResponse.setIopsWriteRate(offering.getIopsWriteRate());
        diskOfferingResponse.setCacheMode(offering.getCacheMode());
        diskOfferingResponse.setObjectName("diskoffering");

        return diskOfferingResponse;
    }

    @Override
    public DiskOfferingJoinVO newDiskOfferingView(DiskOffering offering) {
        SearchCriteria<DiskOfferingJoinVO> sc = dofIdSearch.create();
        sc.setParameters("id", offering.getId());
        List<DiskOfferingJoinVO> offerings = searchIncludingRemoved(sc, null, null, false);
        assert offerings != null && offerings.size() == 1 : "No disk offering found for offering id " + offering.getId();
        return offerings.get(0);
    }
}
