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
package com.cloud.hypervisor.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value=HypervisorCapabilitiesDao.class)
public class HypervisorCapabilitiesDaoImpl extends GenericDaoBase<HypervisorCapabilitiesVO, Long> implements HypervisorCapabilitiesDao {
    
    private static final Logger s_logger = Logger.getLogger(HypervisorCapabilitiesDaoImpl.class);

    protected final SearchBuilder<HypervisorCapabilitiesVO> HypervisorTypeSearch;
    protected final SearchBuilder<HypervisorCapabilitiesVO> HypervisorTypeAndVersionSearch;
    protected final GenericSearchBuilder<HypervisorCapabilitiesVO, Long> MaxGuestLimitByHypervisorSearch;
    protected final GenericSearchBuilder<HypervisorCapabilitiesVO, Integer> MaxDataVolumesLimitByHypervisorSearch;

    private static final String DEFAULT_VERSION = "default";
    
    protected HypervisorCapabilitiesDaoImpl() {
        HypervisorTypeSearch = createSearchBuilder();
        HypervisorTypeSearch.and("hypervisorType", HypervisorTypeSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        HypervisorTypeSearch.done();
        
        HypervisorTypeAndVersionSearch = createSearchBuilder();
        HypervisorTypeAndVersionSearch.and("hypervisorType", HypervisorTypeAndVersionSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        HypervisorTypeAndVersionSearch.and("hypervisorVersion", HypervisorTypeAndVersionSearch.entity().getHypervisorVersion(), SearchCriteria.Op.EQ);
        HypervisorTypeAndVersionSearch.done();
        
        MaxGuestLimitByHypervisorSearch = createSearchBuilder(Long.class);
        MaxGuestLimitByHypervisorSearch.selectField(MaxGuestLimitByHypervisorSearch.entity().getMaxGuestsLimit());
        MaxGuestLimitByHypervisorSearch.and("hypervisorType", MaxGuestLimitByHypervisorSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        MaxGuestLimitByHypervisorSearch.and("hypervisorVersion", MaxGuestLimitByHypervisorSearch.entity().getHypervisorVersion(), SearchCriteria.Op.EQ);
        MaxGuestLimitByHypervisorSearch.done();

        MaxDataVolumesLimitByHypervisorSearch = createSearchBuilder(Integer.class);
        MaxDataVolumesLimitByHypervisorSearch.selectField(MaxDataVolumesLimitByHypervisorSearch.entity().getMaxDataVolumesLimit());
        MaxDataVolumesLimitByHypervisorSearch.and("hypervisorType", MaxDataVolumesLimitByHypervisorSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        MaxDataVolumesLimitByHypervisorSearch.and("hypervisorVersion", MaxDataVolumesLimitByHypervisorSearch.entity().getHypervisorVersion(), SearchCriteria.Op.EQ);
        MaxDataVolumesLimitByHypervisorSearch.done();
    }

    @Override
    public List<HypervisorCapabilitiesVO> listAllByHypervisorType(HypervisorType hypervisorType){
        SearchCriteria<HypervisorCapabilitiesVO> sc = HypervisorTypeSearch.create();
        sc.setParameters("hypervisorType", hypervisorType);
        return search(sc, null);
    }
    
    @Override
    public HypervisorCapabilitiesVO findByHypervisorTypeAndVersion(HypervisorType hypervisorType, String hypervisorVersion){
        SearchCriteria<HypervisorCapabilitiesVO> sc = HypervisorTypeAndVersionSearch.create();
        sc.setParameters("hypervisorType", hypervisorType);
        sc.setParameters("hypervisorVersion", hypervisorVersion);
        return findOneBy(sc);
    }
    
    @Override
    public Long getMaxGuestsLimit(HypervisorType hypervisorType, String hypervisorVersion){
        Long defaultLimit = new Long(50);
        Long result = null;
        boolean useDefault = false;
        if(hypervisorVersion != null){
            SearchCriteria<Long> sc = MaxGuestLimitByHypervisorSearch.create();
            sc.setParameters("hypervisorType", hypervisorType);
            sc.setParameters("hypervisorVersion", hypervisorVersion);
            List<Long> limitList = customSearch(sc, null);
            if(!limitList.isEmpty()){
                result = limitList.get(0);
            }else{
                useDefault = true;
            }
        }else{
            useDefault = true;
        }
        if(useDefault){
            SearchCriteria<Long> sc = MaxGuestLimitByHypervisorSearch.create();
            sc.setParameters("hypervisorType", hypervisorType);
            sc.setParameters("hypervisorVersion", DEFAULT_VERSION);
            List<Long> limitList = customSearch(sc, null);
            if(!limitList.isEmpty()){
                result = limitList.get(0);
            }
        }
        if(result == null){
            return defaultLimit;
        }
        return result;
    }

    @Override
    public Integer getMaxDataVolumesLimit(HypervisorType hypervisorType, String hypervisorVersion) {
        Integer result = null;
        boolean useDefault = false;
        if (hypervisorVersion != null) {
            SearchCriteria<Integer> sc = MaxDataVolumesLimitByHypervisorSearch.create();
            sc.setParameters("hypervisorType", hypervisorType);
            sc.setParameters("hypervisorVersion", hypervisorVersion);
            List<Integer> limitList = customSearch(sc, null);
            if (!limitList.isEmpty()) {
                result = limitList.get(0);
            } else {
                useDefault = true;
            }
        } else {
            useDefault = true;
        }
        // If data is not available for a specific hypervisor version then use 'default' as the version
        if (useDefault) {
            SearchCriteria<Integer> sc = MaxDataVolumesLimitByHypervisorSearch.create();
            sc.setParameters("hypervisorType", hypervisorType);
            sc.setParameters("hypervisorVersion", DEFAULT_VERSION);
            List<Integer> limitList = customSearch(sc, null);
            if (!limitList.isEmpty()) {
                result = limitList.get(0);
            }
        }
        return result;
    }
}