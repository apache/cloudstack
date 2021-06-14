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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class HypervisorCapabilitiesDaoImpl extends GenericDaoBase<HypervisorCapabilitiesVO, Long> implements HypervisorCapabilitiesDao {

    private static final Logger s_logger = Logger.getLogger(HypervisorCapabilitiesDaoImpl.class);

    protected final SearchBuilder<HypervisorCapabilitiesVO> HypervisorTypeSearch;
    protected final SearchBuilder<HypervisorCapabilitiesVO> HypervisorTypeAndVersionSearch;

    private static final String DEFAULT_VERSION = "default";

    protected HypervisorCapabilitiesDaoImpl() {
        HypervisorTypeSearch = createSearchBuilder();
        HypervisorTypeSearch.and("hypervisorType", HypervisorTypeSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        HypervisorTypeSearch.done();

        HypervisorTypeAndVersionSearch = createSearchBuilder();
        HypervisorTypeAndVersionSearch.and("hypervisorType", HypervisorTypeAndVersionSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        HypervisorTypeAndVersionSearch.and("hypervisorVersion", HypervisorTypeAndVersionSearch.entity().getHypervisorVersion(), SearchCriteria.Op.EQ);
        HypervisorTypeAndVersionSearch.done();
    }

    HypervisorCapabilitiesVO getCapabilities(HypervisorType hypervisorType, String hypervisorVersion) {
        HypervisorCapabilitiesVO result = findByHypervisorTypeAndVersion(hypervisorType, hypervisorVersion);
        if (result == null) { // if data is not available for a specific version then use 'default' as version
            result = findByHypervisorTypeAndVersion(hypervisorType, DEFAULT_VERSION);
        }
        return result;
    }

    @Override
    public List<HypervisorCapabilitiesVO> listAllByHypervisorType(HypervisorType hypervisorType) {
        SearchCriteria<HypervisorCapabilitiesVO> sc = HypervisorTypeSearch.create();
        sc.setParameters("hypervisorType", hypervisorType);
        return search(sc, null);
    }

    @Override
    public HypervisorCapabilitiesVO findByHypervisorTypeAndVersion(HypervisorType hypervisorType, String hypervisorVersion) {
        if (StringUtils.isBlank(hypervisorVersion)) {
            hypervisorVersion = DEFAULT_VERSION;
        }
        SearchCriteria<HypervisorCapabilitiesVO> sc = HypervisorTypeAndVersionSearch.create();
        sc.setParameters("hypervisorType", hypervisorType);
        sc.setParameters("hypervisorVersion", hypervisorVersion);
        return findOneBy(sc);
    }

    @Override
    public Long getMaxGuestsLimit(HypervisorType hypervisorType, String hypervisorVersion) {
        Long defaultLimit = new Long(50);
        HypervisorCapabilitiesVO result = getCapabilities(hypervisorType, hypervisorVersion);
        if (result == null) {
            return defaultLimit;
        }
        Long limit = result.getMaxGuestsLimit();
        if (limit == null) {
            return defaultLimit;
        }
        return limit;
    }

    @Override
    public Integer getMaxDataVolumesLimit(HypervisorType hypervisorType, String hypervisorVersion) {
        HypervisorCapabilitiesVO result = getCapabilities(hypervisorType, hypervisorVersion);
        return result.getMaxDataVolumesLimit();
    }

    @Override
    public Integer getMaxHostsPerCluster(HypervisorType hypervisorType, String hypervisorVersion) {
        HypervisorCapabilitiesVO result = getCapabilities(hypervisorType, hypervisorVersion);
        return result.getMaxHostsPerCluster();
    }

    @Override
    public Boolean isVmSnapshotEnabled(HypervisorType hypervisorType, String hypervisorVersion) {
        HypervisorCapabilitiesVO result = getCapabilities(hypervisorType, hypervisorVersion);
        return result.getVmSnapshotEnabled();
    }

    @Override
    public List<HypervisorType> getHypervisorsWithDefaultEntries() {
        SearchCriteria<HypervisorCapabilitiesVO> sc = HypervisorTypeAndVersionSearch.create();
        sc.setParameters("hypervisorVersion", DEFAULT_VERSION);
        List<HypervisorCapabilitiesVO> hypervisorCapabilitiesVOS = listBy(sc);
        List<HypervisorType> hvs = new ArrayList<>();
        for (HypervisorCapabilitiesVO capabilitiesVO : hypervisorCapabilitiesVOS) {
            hvs.add(capabilitiesVO.getHypervisorType());
        }
        return hvs;
    }

    @Override
    public Boolean isStorageMotionSupported(HypervisorType hypervisorType, String hypervisorVersion) {
        HypervisorCapabilitiesVO hostCapabilities = findByHypervisorTypeAndVersion(hypervisorType, hypervisorVersion);
        if (hostCapabilities == null && HypervisorType.KVM.equals(hypervisorType)) {
            List<HypervisorCapabilitiesVO> hypervisorCapabilitiesList = listAllByHypervisorType(HypervisorType.KVM);
            if (hypervisorCapabilitiesList != null) {
                for (HypervisorCapabilitiesVO hypervisorCapabilities : hypervisorCapabilitiesList) {
                    if (hypervisorCapabilities.isStorageMotionSupported()) {
                        hostCapabilities = hypervisorCapabilities;
                        break;
                    }
                }
            }
        }
        return hostCapabilities != null && hostCapabilities.isStorageMotionSupported();
    }
}
