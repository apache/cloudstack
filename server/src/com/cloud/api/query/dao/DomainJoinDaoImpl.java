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

import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ResourceLimitAndCountResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.DomainJoinVO;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.domain.Domain;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={DomainJoinDao.class})
public class DomainJoinDaoImpl extends GenericDaoBase<DomainJoinVO, Long> implements DomainJoinDao {
    public static final Logger s_logger = Logger.getLogger(DomainJoinDaoImpl.class);

    private SearchBuilder<DomainJoinVO> domainIdSearch;

    protected DomainJoinDaoImpl() {

        domainIdSearch = createSearchBuilder();
        domainIdSearch.and("id", domainIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        domainIdSearch.done();

        this._count = "select count(distinct id) from domain_view WHERE ";
    }

    @Override
    public DomainResponse newDomainResponse(DomainJoinVO domain) {
        DomainResponse domainResponse = new DomainResponse();
        domainResponse.setDomainName(domain.getName());
        domainResponse.setId(domain.getUuid());
        domainResponse.setLevel(domain.getLevel());
        domainResponse.setNetworkDomain(domain.getNetworkDomain());
        Domain parentDomain = ApiDBUtils.findDomainById(domain.getParent());
        if (parentDomain != null) {
            domainResponse.setParentDomainId(parentDomain.getUuid());
        }
        StringBuilder domainPath = new StringBuilder("ROOT");
        (domainPath.append(domain.getPath())).deleteCharAt(domainPath.length() - 1);
        domainResponse.setPath(domainPath.toString());
        if (domain.getParent() != null) {
            domainResponse.setParentDomainName(ApiDBUtils.findDomainById(domain.getParent()).getName());
        }
        if (domain.getChildCount() > 0) {
            domainResponse.setHasChild(true);
        }

        domainResponse.setState(domain.getState().toString());
        domainResponse.setNetworkDomain(domain.getNetworkDomain());

        boolean isRootDomain = (domain.getId() == Domain.ROOT_DOMAIN);
        setResourceLimits(domain, isRootDomain, domainResponse);

        //get resource limits for projects
        long projectLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getProjectLimit(), isRootDomain, ResourceType.project, domain.getId());
        String projectLimitDisplay = (isRootDomain || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit);
        long projectTotal = (domain.getProjectTotal() == null) ? 0 : domain.getProjectTotal();
        String projectAvail = (isRootDomain || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit - projectTotal);
        domainResponse.setProjectLimit(projectLimitDisplay);
        domainResponse.setProjectTotal(projectTotal);
        domainResponse.setProjectAvailable(projectAvail);

        domainResponse.setObjectName("domain");

        return domainResponse;
    }

    @Override
    public void setResourceLimits(DomainJoinVO domain, boolean isRootDomain, ResourceLimitAndCountResponse response) {
        // Get resource limits and counts
        long vmLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getVmLimit(), isRootDomain, ResourceType.user_vm, domain.getId());
        String vmLimitDisplay = (isRootDomain || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
        long vmTotal = (domain.getVmTotal() == null) ? 0 : domain.getVmTotal();
        String vmAvail = (isRootDomain || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
        response.setVmLimit(vmLimitDisplay);
        response.setVmTotal(vmTotal);
        response.setVmAvailable(vmAvail);

        long ipLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getIpLimit(), isRootDomain, ResourceType.public_ip, domain.getId());
        String ipLimitDisplay = (isRootDomain || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
        long ipTotal = (domain.getIpTotal() == null) ? 0 : domain.getIpTotal();
        String ipAvail = ((isRootDomain || ipLimit == -1)) ? "Unlimited" : String.valueOf(ipLimit - ipTotal);
        response.setIpLimit(ipLimitDisplay);
        response.setIpTotal(ipTotal);
        response.setIpAvailable(ipAvail);

        long volumeLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getVolumeLimit(), isRootDomain, ResourceType.volume, domain.getId());
        String volumeLimitDisplay = (isRootDomain || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
        long volumeTotal = (domain.getVolumeTotal() == 0) ? 0 : domain.getVolumeTotal();
        String volumeAvail = (isRootDomain || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
        response.setVolumeLimit(volumeLimitDisplay);
        response.setVolumeTotal(volumeTotal);
        response.setVolumeAvailable(volumeAvail);

        long snapshotLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getSnapshotLimit(), isRootDomain, ResourceType.snapshot, domain.getId());
        String snapshotLimitDisplay = (isRootDomain || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
        long snapshotTotal = (domain.getSnapshotTotal() == null) ? 0 : domain.getSnapshotTotal();
        String snapshotAvail = (isRootDomain || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
        response.setSnapshotLimit(snapshotLimitDisplay);
        response.setSnapshotTotal(snapshotTotal);
        response.setSnapshotAvailable(snapshotAvail);

        Long templateLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getTemplateLimit(), isRootDomain, ResourceType.template, domain.getId());
        String templateLimitDisplay = (isRootDomain || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
        Long templateTotal = (domain.getTemplateTotal() == null) ? 0 : domain.getTemplateTotal();
        String templateAvail = (isRootDomain || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
        response.setTemplateLimit(templateLimitDisplay);
        response.setTemplateTotal(templateTotal);
        response.setTemplateAvailable(templateAvail);

        //get resource limits for networks
        long networkLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getNetworkLimit(), isRootDomain, ResourceType.network, domain.getId());
        String networkLimitDisplay = (isRootDomain || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit);
        long networkTotal = (domain.getNetworkTotal() == null) ? 0 : domain.getNetworkTotal();
        String networkAvail = (isRootDomain || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit - networkTotal);
        response.setNetworkLimit(networkLimitDisplay);
        response.setNetworkTotal(networkTotal);
        response.setNetworkAvailable(networkAvail);

        //get resource limits for vpcs
        long vpcLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getVpcLimit(), isRootDomain, ResourceType.vpc, domain.getId());
        String vpcLimitDisplay = (isRootDomain || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit);
        long vpcTotal = (domain.getVpcTotal() == null) ? 0 : domain.getVpcTotal();
        String vpcAvail = (isRootDomain || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit - vpcTotal);
        response.setVpcLimit(vpcLimitDisplay);
        response.setVpcTotal(vpcTotal);
        response.setVpcAvailable(vpcAvail);

        //get resource limits for cpu cores
        long cpuLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getCpuLimit(), isRootDomain, ResourceType.cpu, domain.getId());
        String cpuLimitDisplay = (isRootDomain || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit);
        long cpuTotal = (domain.getCpuTotal() == null) ? 0 : domain.getCpuTotal();
        String cpuAvail = (isRootDomain || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit - cpuTotal);
        response.setCpuLimit(cpuLimitDisplay);
        response.setCpuTotal(cpuTotal);
        response.setCpuAvailable(cpuAvail);

        //get resource limits for memory
        long memoryLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getMemoryLimit(), isRootDomain, ResourceType.memory, domain.getId());
        String memoryLimitDisplay = (isRootDomain || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit);
        long memoryTotal = (domain.getMemoryTotal() == null) ? 0 : domain.getMemoryTotal();
        String memoryAvail = (isRootDomain || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit - memoryTotal);
        response.setMemoryLimit(memoryLimitDisplay);
        response.setMemoryTotal(memoryTotal);
        response.setMemoryAvailable(memoryAvail);

      //get resource limits for primary storage space and convert it from Bytes to GiB
        long primaryStorageLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getPrimaryStorageLimit(), isRootDomain, ResourceType.primary_storage, domain.getId());
        String primaryStorageLimitDisplay = (isRootDomain || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf(primaryStorageLimit / ResourceType.bytesToGiB);
        long primaryStorageTotal = (domain.getPrimaryStorageTotal() == null) ? 0 : (domain.getPrimaryStorageTotal() / ResourceType.bytesToGiB);
        String primaryStorageAvail = (isRootDomain || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf((primaryStorageLimit / ResourceType.bytesToGiB) - primaryStorageTotal);
        response.setPrimaryStorageLimit(primaryStorageLimitDisplay);
        response.setPrimaryStorageTotal(primaryStorageTotal);
        response.setPrimaryStorageAvailable(primaryStorageAvail);

        //get resource limits for secondary storage space and convert it from Bytes to GiB
        long secondaryStorageLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getSecondaryStorageLimit(), isRootDomain, ResourceType.secondary_storage, domain.getId());
        String secondaryStorageLimitDisplay = (isRootDomain || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf(secondaryStorageLimit / ResourceType.bytesToGiB);
        long secondaryStorageTotal = (domain.getSecondaryStorageTotal() == null) ? 0 : (domain.getSecondaryStorageTotal() / ResourceType.bytesToGiB);
        String secondaryStorageAvail = (isRootDomain || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf((secondaryStorageLimit / ResourceType.bytesToGiB) - secondaryStorageTotal);
        response.setSecondaryStorageLimit(secondaryStorageLimitDisplay);
        response.setSecondaryStorageTotal(secondaryStorageTotal);
        response.setSecondaryStorageAvailable(secondaryStorageAvail);
    }

    @Override
    public DomainJoinVO newDomainView(Domain domain) {
        SearchCriteria<DomainJoinVO> sc = domainIdSearch.create();
        sc.setParameters("id", domain.getId());
        List<DomainJoinVO> domains = searchIncludingRemoved(sc, null, null, false);
        assert domains != null && domains.size() == 1 : "No domain found for domain id " + domain.getId();
        return domains.get(0);
    }
}
