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

import com.cloud.dc.VsphereStoragePolicyVO;
import com.cloud.dc.dao.VsphereStoragePolicyDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import javax.inject.Inject;

@Component
public class ServiceOfferingJoinDaoImpl extends GenericDaoBase<ServiceOfferingJoinVO, Long> implements ServiceOfferingJoinDao {
    public static final Logger s_logger = Logger.getLogger(ServiceOfferingJoinDaoImpl.class);

    @Inject
    VsphereStoragePolicyDao _vsphereStoragePolicyDao;

    private SearchBuilder<ServiceOfferingJoinVO> sofIdSearch;

    protected ServiceOfferingJoinDaoImpl() {

        sofIdSearch = createSearchBuilder();
        sofIdSearch.and("id", sofIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        sofIdSearch.done();

        this._count = "select count(distinct service_offering_view.id) from service_offering_view WHERE ";
    }

    @Override
    public List<ServiceOfferingJoinVO> findByDomainId(long domainId) {
        SearchBuilder<ServiceOfferingJoinVO> sb = createSearchBuilder();
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.FIND_IN_SET);
        sb.done();

        SearchCriteria<ServiceOfferingJoinVO> sc = sb.create();
        sc.setParameters("domainId", String.valueOf(domainId));
        return listBy(sc);
    }

    @Override
    public ServiceOfferingResponse newServiceOfferingResponse(ServiceOfferingJoinVO offering) {

        ServiceOfferingResponse offeringResponse = new ServiceOfferingResponse();
        offeringResponse.setId(offering.getUuid());
        offeringResponse.setName(offering.getName());
        offeringResponse.setIsSystemOffering(offering.isSystemUse());
        offeringResponse.setDefaultUse(offering.isDefaultUse());
        offeringResponse.setSystemVmType(offering.getSystemVmType());
        offeringResponse.setDisplayText(offering.getDisplayText());
        offeringResponse.setProvisioningType(offering.getProvisioningType().toString());
        offeringResponse.setCpuNumber(offering.getCpu());
        offeringResponse.setCpuSpeed(offering.getSpeed());
        offeringResponse.setMemory(offering.getRamSize());
        offeringResponse.setCreated(offering.getCreated());
        offeringResponse.setStorageType(offering.isUseLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared.toString());
        offeringResponse.setOfferHa(offering.isOfferHA());
        offeringResponse.setLimitCpuUse(offering.isLimitCpuUse());
        offeringResponse.setVolatileVm(offering.getVolatileVm());
        offeringResponse.setTags(offering.getTags());
        offeringResponse.setDomain(offering.getDomainName());
        offeringResponse.setDomainId(offering.getDomainUuid());
        offeringResponse.setZone(offering.getZoneName());
        offeringResponse.setZoneId(offering.getZoneUuid());
        offeringResponse.setNetworkRate(offering.getRateMbps());
        offeringResponse.setHostTag(offering.getHostTag());
        offeringResponse.setDeploymentPlanner(offering.getDeploymentPlanner());
        offeringResponse.setCustomizedIops(offering.isCustomizedIops());
        offeringResponse.setMinIops(offering.getMinIops());
        offeringResponse.setMaxIops(offering.getMaxIops());
        offeringResponse.setHypervisorSnapshotReserve(offering.getHypervisorSnapshotReserve());
        offeringResponse.setBytesReadRate(offering.getBytesReadRate());
        offeringResponse.setBytesReadRateMax(offering.getBytesReadRateMax());
        offeringResponse.setBytesReadRateMaxLength(offering.getBytesReadRateMaxLength());
        offeringResponse.setBytesWriteRate(offering.getBytesWriteRate());
        offeringResponse.setBytesWriteRateMax(offering.getBytesWriteRateMax());
        offeringResponse.setBytesWriteRateMaxLength(offering.getBytesWriteRateMaxLength());
        offeringResponse.setIopsReadRate(offering.getIopsReadRate());
        offeringResponse.setIopsReadRateMax(offering.getIopsReadRateMax());
        offeringResponse.setIopsReadRateMaxLength(offering.getIopsReadRateMaxLength());
        offeringResponse.setIopsWriteRate(offering.getIopsWriteRate());
        offeringResponse.setIopsWriteRateMax(offering.getIopsWriteRateMax());
        offeringResponse.setIopsWriteRateMaxLength(offering.getIopsWriteRateMaxLength());
        Map<String, String> offeringDetails = ApiDBUtils.getResourceDetails(offering.getId(), ResourceObjectType.ServiceOffering);
        offeringResponse.setDetails(offeringDetails);
        offeringResponse.setObjectName("serviceoffering");
        offeringResponse.setIscutomized(offering.isDynamic());
        offeringResponse.setCacheMode(offering.getCacheMode());
        offeringResponse.setDynamicScalingEnabled(offering.isDynamicScalingEnabled());

        if (offeringDetails != null && !offeringDetails.isEmpty()) {
            String vsphereStoragePolicyId = offeringDetails.get(ApiConstants.STORAGE_POLICY);
            if (vsphereStoragePolicyId != null) {
                VsphereStoragePolicyVO vsphereStoragePolicyVO = _vsphereStoragePolicyDao.findById(Long.parseLong(vsphereStoragePolicyId));
                if (vsphereStoragePolicyVO != null)
                    offeringResponse.setVsphereStoragePolicy(vsphereStoragePolicyVO.getName());
            }
        }

        offeringResponse.setRootDiskSize(offering.getRootDiskSize());

        return offeringResponse;
    }

    @Override
    public ServiceOfferingJoinVO newServiceOfferingView(ServiceOffering offering) {
        SearchCriteria<ServiceOfferingJoinVO> sc = sofIdSearch.create();
        sc.setParameters("id", offering.getId());
        List<ServiceOfferingJoinVO> offerings = searchIncludingRemoved(sc, null, null, false);
        assert offerings != null && offerings.size() == 1 : "No service offering found for offering id " + offering.getId();
        return offerings.get(0);
    }

}
