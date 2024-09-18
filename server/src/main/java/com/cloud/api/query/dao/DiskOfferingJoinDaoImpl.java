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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.DiskOfferingJoinVO;
import com.cloud.dc.VsphereStoragePolicyVO;
import com.cloud.dc.dao.VsphereStoragePolicyDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.ResourceTag;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import static org.apache.cloudstack.query.QueryService.SortKeyAscending;

@Component
public class DiskOfferingJoinDaoImpl extends GenericDaoBase<DiskOfferingJoinVO, Long> implements DiskOfferingJoinDao {

    @Inject
    VsphereStoragePolicyDao _vsphereStoragePolicyDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private ConfigurationDao configDao;
    @Inject
    private AccountManager accountManager;

    private final SearchBuilder<DiskOfferingJoinVO> dofIdSearch;
    private SearchBuilder<DiskOfferingJoinVO> diskOfferingSearch;

    protected DiskOfferingJoinDaoImpl() {

        dofIdSearch = createSearchBuilder();
        dofIdSearch.and("id", dofIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        dofIdSearch.done();

        diskOfferingSearch = createSearchBuilder();
        diskOfferingSearch.and("idIN", diskOfferingSearch.entity().getId(), SearchCriteria.Op.IN);
        diskOfferingSearch.done();

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
        diskOfferingResponse.setState(offering.getState().toString());
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
        diskOfferingResponse.setEncrypt(offering.getEncrypt());

        diskOfferingResponse.setHasAnnotation(annotationDao.hasAnnotations(offering.getUuid(), AnnotationService.EntityType.DISK_OFFERING.name(),
                accountManager.isRootAdmin(CallContext.current().getCallingAccount().getId())));

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
        diskOfferingResponse.setDetails(offeringDetails);
        if (offeringDetails != null && !offeringDetails.isEmpty()) {
            String vsphereStoragePolicyId = offeringDetails.get(ApiConstants.STORAGE_POLICY);
            if (vsphereStoragePolicyId != null) {
                VsphereStoragePolicyVO vsphereStoragePolicyVO = _vsphereStoragePolicyDao.findById(Long.parseLong(vsphereStoragePolicyId));
                if (vsphereStoragePolicyVO != null)
                    diskOfferingResponse.setVsphereStoragePolicy(vsphereStoragePolicyVO.getName());
            }
        }
        diskOfferingResponse.setDiskSizeStrictness(offering.getDiskSizeStrictness());

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

    @Override
    public List<DiskOfferingJoinVO> searchByIds(Long... offeringIds) {
        Filter searchFilter = new Filter(DiskOfferingJoinVO.class, "sortKey", SortKeyAscending.value());
        searchFilter.addOrderBy(DiskOfferingJoinVO.class, "id", true);
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }

        List<DiskOfferingJoinVO> uvList = new ArrayList<>();
        // query details by batches
        int curr_index = 0;
        if (offeringIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= offeringIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = offeringIds[j];
                }
                SearchCriteria<DiskOfferingJoinVO> sc = diskOfferingSearch.create();
                sc.setParameters("idIN", ids);
                List<DiskOfferingJoinVO> accounts = searchIncludingRemoved(sc, searchFilter, null, false);
                if (accounts != null) {
                    uvList.addAll(accounts);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < offeringIds.length) {
            int batch_size = (offeringIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = offeringIds[j];
            }
            SearchCriteria<DiskOfferingJoinVO> sc = diskOfferingSearch.create();
            sc.setParameters("idIN", ids);
            List<DiskOfferingJoinVO> accounts = searchIncludingRemoved(sc, searchFilter, null, false);
            if (accounts != null) {
                uvList.addAll(accounts);
            }
        }
        return uvList;
    }
}
