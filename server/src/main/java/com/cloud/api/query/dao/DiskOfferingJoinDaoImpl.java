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

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.VsphereStoragePolicyVO;
import com.cloud.dc.dao.VsphereStoragePolicyDao;
import com.cloud.server.ResourceTag;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import javax.inject.Inject;

@Component
public class DiskOfferingJoinDaoImpl extends GenericDaoBase<DiskOfferingJoinVO, Long> implements DiskOfferingJoinDao {
    public static final Logger s_logger = Logger.getLogger(DiskOfferingJoinDaoImpl.class);

    @Inject
    VsphereStoragePolicyDao _vsphereStoragePolicyDao;

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
    public List<DiskOfferingJoinVO> findByDomainId(long domainId) {
        SearchBuilder<DiskOfferingJoinVO> sb = createSearchBuilder();
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.FIND_IN_SET);
        sb.done();

        SearchCriteria<DiskOfferingJoinVO> sc = sb.create();
        sc.setParameters("domainId", String.valueOf(domainId));
        return listBy(sc);
    }

    @Override
    public List<DiskOfferingJoinVO> findByZoneId(long zoneId) {
        SearchBuilder<DiskOfferingJoinVO> sb = createSearchBuilder();
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.FIND_IN_SET);
        sb.done();

        SearchCriteria<DiskOfferingJoinVO> sc = sb.create();
        sc.setParameters("zoneId", String.valueOf(zoneId));
        return listBy(sc);
    }

    @Override
    public DiskOfferingResponse newDiskOfferingResponse(DiskOfferingJoinVO offering) {

        DiskOfferingResponse diskOfferingResponse = new DiskOfferingResponse();
        diskOfferingResponse.setId(offering.getUuid());
        diskOfferingResponse.setName(offering.getName());
        diskOfferingResponse.setDisplayText(offering.getDisplayText());
        diskOfferingResponse.setProvisioningType(offering.getProvisioningType().toString());
        diskOfferingResponse.setCreated(offering.getCreated());
        diskOfferingResponse.setDiskSize(offering.getDiskSize() / (1024 * 1024 * 1024));
        diskOfferingResponse.setMinIops(offering.getMinIops());
        diskOfferingResponse.setMaxIops(offering.getMaxIops());

        diskOfferingResponse.setDisplayOffering(offering.isDisplayOffering());
        diskOfferingResponse.setDomainId(offering.getDomainUuid());
        diskOfferingResponse.setDomain(offering.getDomainPath());
        diskOfferingResponse.setZoneId(offering.getZoneUuid());
        diskOfferingResponse.setZone(offering.getZoneName());

        diskOfferingResponse.setTags(offering.getTags());
        diskOfferingResponse.setCustomized(offering.isCustomized());
        diskOfferingResponse.setCustomizedIops(offering.isCustomizedIops());
        diskOfferingResponse.setHypervisorSnapshotReserve(offering.getHypervisorSnapshotReserve());
        diskOfferingResponse.setStorageType(offering.isUseLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared.toString());
        diskOfferingResponse.setBytesReadRate(offering.getBytesReadRate());
        diskOfferingResponse.setBytesReadRateMax(offering.getBytesReadRateMax());
        diskOfferingResponse.setBytesReadRateMaxLength(offering.getBytesReadRateMaxLength());
        diskOfferingResponse.setBytesWriteRate(offering.getBytesWriteRate());
        diskOfferingResponse.setBytesWriteRateMax(offering.getBytesWriteRateMax());
        diskOfferingResponse.setBytesWriteRateMaxLength(offering.getBytesWriteRateMaxLength());
        diskOfferingResponse.setIopsReadRate(offering.getIopsReadRate());
        diskOfferingResponse.setIopsReadRateMax(offering.getIopsReadRateMax());
        diskOfferingResponse.setIopsReadRateMaxLength(offering.getIopsReadRateMaxLength());
        diskOfferingResponse.setIopsWriteRate(offering.getIopsWriteRate());
        diskOfferingResponse.setIopsWriteRateMax(offering.getIopsWriteRateMax());
        diskOfferingResponse.setIopsWriteRateMaxLength(offering.getIopsWriteRateMaxLength());
        diskOfferingResponse.setCacheMode(offering.getCacheMode());
        diskOfferingResponse.setObjectName("diskoffering");
        Map<String, String> offeringDetails = ApiDBUtils.getResourceDetails(offering.getId(), ResourceTag.ResourceObjectType.DiskOffering);
        if (offeringDetails != null && !offeringDetails.isEmpty()) {
            String vsphereStoragePolicyId = offeringDetails.get(ApiConstants.STORAGE_POLICY);
            if (vsphereStoragePolicyId != null) {
                VsphereStoragePolicyVO vsphereStoragePolicyVO = _vsphereStoragePolicyDao.findById(Long.parseLong(vsphereStoragePolicyId));
                if (vsphereStoragePolicyVO != null)
                    diskOfferingResponse.setVsphereStoragePolicy(vsphereStoragePolicyVO.getName());
            }
        }

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
