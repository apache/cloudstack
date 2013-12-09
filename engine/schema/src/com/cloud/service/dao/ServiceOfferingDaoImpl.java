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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;

import com.cloud.event.UsageEventVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDetailsDao;

@Component
@Local(value = {ServiceOfferingDao.class})
@DB()
public class ServiceOfferingDaoImpl extends GenericDaoBase<ServiceOfferingVO, Long> implements ServiceOfferingDao {
    protected static final Logger s_logger = Logger.getLogger(ServiceOfferingDaoImpl.class);

    @Inject
    protected ServiceOfferingDetailsDao detailsDao;
    @Inject
    protected UserVmDetailsDao userVmDetailsDao;

    protected final SearchBuilder<ServiceOfferingVO> UniqueNameSearch;
    protected final SearchBuilder<ServiceOfferingVO> ServiceOfferingsByDomainIdSearch;
    protected final SearchBuilder<ServiceOfferingVO> SystemServiceOffering;
    protected final SearchBuilder<ServiceOfferingVO> ServiceOfferingsByKeywordSearch;
    protected final SearchBuilder<ServiceOfferingVO> PublicServiceOfferingSearch;

    public ServiceOfferingDaoImpl() {
        super();

        UniqueNameSearch = createSearchBuilder();
        UniqueNameSearch.and("name", UniqueNameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
        UniqueNameSearch.and("system", UniqueNameSearch.entity().getSystemUse(), SearchCriteria.Op.EQ);
        UniqueNameSearch.done();

        ServiceOfferingsByDomainIdSearch = createSearchBuilder();
        ServiceOfferingsByDomainIdSearch.and("domainId", ServiceOfferingsByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ServiceOfferingsByDomainIdSearch.done();

        SystemServiceOffering = createSearchBuilder();
        SystemServiceOffering.and("domainId", SystemServiceOffering.entity().getDomainId(), SearchCriteria.Op.EQ);
        SystemServiceOffering.and("system", SystemServiceOffering.entity().getSystemUse(), SearchCriteria.Op.EQ);
        SystemServiceOffering.and("vm_type", SystemServiceOffering.entity().getSpeed(), SearchCriteria.Op.EQ);
        SystemServiceOffering.and("removed", SystemServiceOffering.entity().getRemoved(), SearchCriteria.Op.NULL);
        SystemServiceOffering.done();

        PublicServiceOfferingSearch = createSearchBuilder();
        PublicServiceOfferingSearch.and("domainId", PublicServiceOfferingSearch.entity().getDomainId(), SearchCriteria.Op.NULL);
        PublicServiceOfferingSearch.and("system", PublicServiceOfferingSearch.entity().getSystemUse(), SearchCriteria.Op.EQ);
        PublicServiceOfferingSearch.and("removed", PublicServiceOfferingSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        PublicServiceOfferingSearch.done();

        ServiceOfferingsByKeywordSearch = createSearchBuilder();
        ServiceOfferingsByKeywordSearch.or("name", ServiceOfferingsByKeywordSearch.entity().getName(), SearchCriteria.Op.EQ);
        ServiceOfferingsByKeywordSearch.or("displayText", ServiceOfferingsByKeywordSearch.entity().getDisplayText(), SearchCriteria.Op.EQ);
        ServiceOfferingsByKeywordSearch.done();
    }

    @Override
    public ServiceOfferingVO findByName(String name) {
        SearchCriteria<ServiceOfferingVO> sc = UniqueNameSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("system", true);
        List<ServiceOfferingVO> vos = search(sc, null, null, false);
        if (vos.size() == 0) {
            return null;
        }

        return vos.get(0);
    }

    @Override
    @DB
    public ServiceOfferingVO persistSystemServiceOffering(ServiceOfferingVO offering) {
        assert offering.getUniqueName() != null : "how are you going to find this later if you don't set it?";
        ServiceOfferingVO vo = findByName(offering.getUniqueName());
        if (vo != null) {
            // check invalid CPU speed in system service offering, set it to default value of 500 Mhz if 0 CPU speed is found
            if (vo.getSpeed() <= 0) {
                vo.setSpeed(500);
                update(vo.getId(), vo);
            }
            return vo;
        }
        try {
            return persist(offering);
        } catch (EntityExistsException e) {
            // Assume it's conflict on unique name
            return findByName(offering.getUniqueName());
        }
    }

    @Override
    public List<ServiceOfferingVO> findServiceOfferingByDomainId(Long domainId) {
        SearchCriteria<ServiceOfferingVO> sc = ServiceOfferingsByDomainIdSearch.create();
        sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<ServiceOfferingVO> findSystemOffering(Long domainId, Boolean isSystem, String vm_type) {
        SearchCriteria<ServiceOfferingVO> sc = SystemServiceOffering.create();
        sc.setParameters("domainId", domainId);
        sc.setParameters("system", isSystem);
        sc.setParameters("vm_type", vm_type);
        return listBy(sc);
    }

    @Override
    public List<ServiceOfferingVO> findPublicServiceOfferings() {
        SearchCriteria<ServiceOfferingVO> sc = PublicServiceOfferingSearch.create();
        sc.setParameters("system", false);
        return listBy(sc);
    }

    @Override
    @DB
    public ServiceOfferingVO persistDeafultServiceOffering(ServiceOfferingVO offering) {
        assert offering.getUniqueName() != null : "unique name should be set for the service offering";
        ServiceOfferingVO vo = findByName(offering.getUniqueName());
        if (vo != null) {
            return vo;
        }
        try {
            return persist(offering);
        } catch (EntityExistsException e) {
            // Assume it's conflict on unique name
            return findByName(offering.getUniqueName());
        }
    }

    @Override
    public boolean remove(Long id) {
        ServiceOfferingVO offering = createForUpdate();
        offering.setRemoved(new Date());

        return update(id, offering);
    }

    @Override
    public void loadDetails(ServiceOfferingVO serviceOffering) {
        Map<String, String> details = detailsDao.listDetailsKeyPairs(serviceOffering.getId());
        serviceOffering.setDetails(details);
    }

    @Override
    public void saveDetails(ServiceOfferingVO serviceOffering) {
        Map<String, String> details = serviceOffering.getDetails();
        if (details == null) {
            return;
        }

        List<ServiceOfferingDetailsVO> resourceDetails = new ArrayList<ServiceOfferingDetailsVO>();
        for (String key : details.keySet()) {
            resourceDetails.add(new ServiceOfferingDetailsVO(serviceOffering.getId(), key, details.get(key)));
        }

        detailsDao.saveDetails(resourceDetails);
    }

    @Override
    public ServiceOfferingVO findById(Long vmId, long serviceOfferingId) {
        ServiceOfferingVO offering = super.findById(serviceOfferingId);
        if (offering.isDynamic()) {
            offering.setDynamicFlag(true);
            if (vmId == null) {
                throw new CloudRuntimeException("missing argument vmId");
            }
            Map<String, String> dynamicOffering = userVmDetailsDao.listDetailsKeyPairs(vmId);
            return getcomputeOffering(offering, dynamicOffering);
        }
        return offering;
    }

    @Override
    public ServiceOfferingVO findByIdIncludingRemoved(Long vmId, long serviceOfferingId) {
        ServiceOfferingVO offering = super.findByIdIncludingRemoved(serviceOfferingId);
        if (offering.isDynamic()) {
            offering.setDynamicFlag(true);
            if (vmId == null) {
                throw new CloudRuntimeException("missing argument vmId");
            }
            Map<String, String> dynamicOffering = userVmDetailsDao.listDetailsKeyPairs(vmId);
            return  getcomputeOffering(offering, dynamicOffering);
        }
        return offering;
    }

    @Override
    public boolean isDynamic(long serviceOfferingId) {
        ServiceOfferingVO offering = super.findById(serviceOfferingId);
        return offering.getCpu() == null || offering.getSpeed() == null || offering.getRamSize() == null;
    }

    @Override
    public ServiceOfferingVO getcomputeOffering(ServiceOfferingVO serviceOffering, Map<String, String> customParameters) {
        ServiceOfferingVO dummyoffering = new ServiceOfferingVO(serviceOffering);
        dummyoffering.setDynamicFlag(true);
        if (customParameters.containsKey(UsageEventVO.DynamicParameters.cpuNumber.name())) {
            dummyoffering.setCpu(Integer.parseInt(customParameters.get(UsageEventVO.DynamicParameters.cpuNumber.name())));
        }
        if (customParameters.containsKey(UsageEventVO.DynamicParameters.cpuSpeed.name())) {
            dummyoffering.setSpeed(Integer.parseInt(customParameters.get(UsageEventVO.DynamicParameters.cpuSpeed.name())));
        }
        if (customParameters.containsKey(UsageEventVO.DynamicParameters.memory.name())) {
            dummyoffering.setRamSize(Integer.parseInt(customParameters.get(UsageEventVO.DynamicParameters.memory.name())));
        }

        return dummyoffering;
    }
}
