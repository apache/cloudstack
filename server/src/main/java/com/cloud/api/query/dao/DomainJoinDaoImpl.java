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

import java.util.EnumSet;
import java.util.List;


import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
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
    public DomainResponse newDomainResponse(ResponseView view, EnumSet<DomainDetails> details, DomainJoinVO domain) {
        DomainResponse domainResponse = new DomainResponse();
        domainResponse.setDomainName(domain.getName());
        domainResponse.setId(domain.getUuid());
        domainResponse.setLevel(domain.getLevel());
        domainResponse.setNetworkDomain(domain.getNetworkDomain());
        if (domain.getParentUuid() != null) {
            domainResponse.setParentDomainId(domain.getParentUuid());
        }
        StringBuilder domainPath = new StringBuilder("ROOT");
        (domainPath.append(domain.getPath())).deleteCharAt(domainPath.length() - 1);
        domainResponse.setPath(domainPath.toString());
        if (domain.getParent() != null) {
            domainResponse.setParentDomainName(domain.getParentName());
        }
        if (domain.getChildCount() > 0) {
            domainResponse.setHasChild(true);
        }

        domainResponse.setState(domain.getState().toString());
        domainResponse.setCreated(domain.getCreated());
        domainResponse.setNetworkDomain(domain.getNetworkDomain());

        if (details.contains(DomainDetails.all) || details.contains(DomainDetails.resource)) {
            boolean fullView = (view == ResponseView.Full && domain.getId() == Domain.ROOT_DOMAIN);
            setResourceLimits(domain, fullView, domainResponse);

            //get resource limits for projects
            long projectLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getProjectLimit(), ResourceType.project, domain.getId());
            String projectLimitDisplay = (fullView || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit);
            long projectTotal = (domain.getProjectTotal() == null) ? 0 : domain.getProjectTotal();
            String projectAvail = (fullView || projectLimit == -1) ? "Unlimited" : String.valueOf(projectLimit - projectTotal);
            domainResponse.setProjectLimit(projectLimitDisplay);
            domainResponse.setProjectTotal(projectTotal);
            domainResponse.setProjectAvailable(projectAvail);
        }

        domainResponse.setObjectName("domain");

        return domainResponse;
    }

    @Override
    public void setResourceLimits(DomainJoinVO domain, boolean fullView, ResourceLimitAndCountResponse response) {
        // Get resource limits and counts
        long vmLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getVmLimit(), fullView, ResourceType.user_vm, domain.getId());
        String vmLimitDisplay = (fullView || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit);
        long vmTotal = (domain.getVmTotal() == null) ? 0 : domain.getVmTotal();
        String vmAvail = (fullView || vmLimit == -1) ? "Unlimited" : String.valueOf(vmLimit - vmTotal);
        response.setVmLimit(vmLimitDisplay);
        response.setVmTotal(vmTotal);
        response.setVmAvailable(vmAvail);

        long ipLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getIpLimit(), ResourceType.public_ip, domain.getId());
        String ipLimitDisplay = (fullView || ipLimit == -1) ? "Unlimited" : String.valueOf(ipLimit);
        long ipTotal = (domain.getIpTotal() == null) ? 0 : domain.getIpTotal();
        String ipAvail = ((fullView || ipLimit == -1)) ? "Unlimited" : String.valueOf(ipLimit - ipTotal);
        response.setIpLimit(ipLimitDisplay);
        response.setIpTotal(ipTotal);
        response.setIpAvailable(ipAvail);

        long volumeLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getVolumeLimit(), ResourceType.volume, domain.getId());
        String volumeLimitDisplay = (fullView || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit);
        long volumeTotal = (domain.getVolumeTotal() == null) ? 0 : domain.getVolumeTotal();
        String volumeAvail = (fullView || volumeLimit == -1) ? "Unlimited" : String.valueOf(volumeLimit - volumeTotal);
        response.setVolumeLimit(volumeLimitDisplay);
        response.setVolumeTotal(volumeTotal);
        response.setVolumeAvailable(volumeAvail);

        long snapshotLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getSnapshotLimit(), ResourceType.snapshot, domain.getId());
        String snapshotLimitDisplay = (fullView || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit);
        long snapshotTotal = (domain.getSnapshotTotal() == null) ? 0 : domain.getSnapshotTotal();
        String snapshotAvail = (fullView || snapshotLimit == -1) ? "Unlimited" : String.valueOf(snapshotLimit - snapshotTotal);
        response.setSnapshotLimit(snapshotLimitDisplay);
        response.setSnapshotTotal(snapshotTotal);
        response.setSnapshotAvailable(snapshotAvail);

        Long templateLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getTemplateLimit(), ResourceType.template, domain.getId());
        String templateLimitDisplay = (fullView || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit);
        Long templateTotal = (domain.getTemplateTotal() == null) ? 0 : domain.getTemplateTotal();
        String templateAvail = (fullView || templateLimit == -1) ? "Unlimited" : String.valueOf(templateLimit - templateTotal);
        response.setTemplateLimit(templateLimitDisplay);
        response.setTemplateTotal(templateTotal);
        response.setTemplateAvailable(templateAvail);

        //get resource limits for networks
        long networkLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getNetworkLimit(), ResourceType.network, domain.getId());
        String networkLimitDisplay = (fullView || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit);
        long networkTotal = (domain.getNetworkTotal() == null) ? 0 : domain.getNetworkTotal();
        String networkAvail = (fullView || networkLimit == -1) ? "Unlimited" : String.valueOf(networkLimit - networkTotal);
        response.setNetworkLimit(networkLimitDisplay);
        response.setNetworkTotal(networkTotal);
        response.setNetworkAvailable(networkAvail);

        //get resource limits for vpcs
        long vpcLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getVpcLimit(), ResourceType.vpc, domain.getId());
        String vpcLimitDisplay = (fullView || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit);
        long vpcTotal = (domain.getVpcTotal() == null) ? 0 : domain.getVpcTotal();
        String vpcAvail = (fullView || vpcLimit == -1) ? "Unlimited" : String.valueOf(vpcLimit - vpcTotal);
        response.setVpcLimit(vpcLimitDisplay);
        response.setVpcTotal(vpcTotal);
        response.setVpcAvailable(vpcAvail);

        //get resource limits for cpu cores
        long cpuLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getCpuLimit(), ResourceType.cpu, domain.getId());
        String cpuLimitDisplay = (fullView || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit);
        long cpuTotal = (domain.getCpuTotal() == null) ? 0 : domain.getCpuTotal();
        String cpuAvail = (fullView || cpuLimit == -1) ? "Unlimited" : String.valueOf(cpuLimit - cpuTotal);
        response.setCpuLimit(cpuLimitDisplay);
        response.setCpuTotal(cpuTotal);
        response.setCpuAvailable(cpuAvail);

        //get resource limits for memory
        long memoryLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getMemoryLimit(), ResourceType.memory, domain.getId());
        String memoryLimitDisplay = (fullView || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit);
        long memoryTotal = (domain.getMemoryTotal() == null) ? 0 : domain.getMemoryTotal();
        String memoryAvail = (fullView || memoryLimit == -1) ? "Unlimited" : String.valueOf(memoryLimit - memoryTotal);
        response.setMemoryLimit(memoryLimitDisplay);
        response.setMemoryTotal(memoryTotal);
        response.setMemoryAvailable(memoryAvail);

      //get resource limits for primary storage space and convert it from Bytes to GiB
        long primaryStorageLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getPrimaryStorageLimit(), ResourceType.primary_storage, domain.getId());
        String primaryStorageLimitDisplay = (fullView || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf(primaryStorageLimit / ResourceType.bytesToGiB);
        long primaryStorageTotal = (domain.getPrimaryStorageTotal() == null) ? 0 : (domain.getPrimaryStorageTotal() / ResourceType.bytesToGiB);
        String primaryStorageAvail = (fullView || primaryStorageLimit == -1) ? "Unlimited" : String.valueOf((primaryStorageLimit / ResourceType.bytesToGiB) - primaryStorageTotal);
        response.setPrimaryStorageLimit(primaryStorageLimitDisplay);
        response.setPrimaryStorageTotal(primaryStorageTotal);
        response.setPrimaryStorageAvailable(primaryStorageAvail);

        //get resource limits for secondary storage space and convert it from Bytes to GiB
        long secondaryStorageLimit = ApiDBUtils.findCorrectResourceLimitForDomain(domain.getSecondaryStorageLimit(), ResourceType.secondary_storage, domain.getId());
        String secondaryStorageLimitDisplay = (fullView || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf(secondaryStorageLimit / ResourceType.bytesToGiB);
        float secondaryStorageTotal = (domain.getSecondaryStorageTotal() == null) ? 0 : (domain.getSecondaryStorageTotal() / (ResourceType.bytesToGiB * 1f));
        String secondaryStorageAvail = (fullView || secondaryStorageLimit == -1) ? "Unlimited" : String.valueOf((secondaryStorageLimit / ResourceType.bytesToGiB) - secondaryStorageTotal);
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
