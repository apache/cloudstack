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

import javax.inject.Inject;

import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dedicated.DedicatedResourceResponse;

import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.AffinityGroupJoinVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.DedicatedResources;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.user.AccountManager;

public class AffinityGroupJoinDaoImpl extends GenericDaoBase<AffinityGroupJoinVO, Long> implements AffinityGroupJoinDao {

    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private DedicatedResourceDao dedicatedResourceDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private HostPodDao podDao;
    @Inject
    private ClusterDao clusterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private AccountManager accountManager;

    private final SearchBuilder<AffinityGroupJoinVO> agSearch;

    private final SearchBuilder<AffinityGroupJoinVO> agIdSearch;

    protected AffinityGroupJoinDaoImpl() {

        agSearch = createSearchBuilder();
        agSearch.and("idIN", agSearch.entity().getId(), SearchCriteria.Op.IN);
        agSearch.done();

        agIdSearch = createSearchBuilder();
        agIdSearch.and("id", agIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        agIdSearch.done();

        this._count = "select count(distinct id) from affinity_group_view WHERE ";
    }

    @Override
    public AffinityGroupResponse newAffinityGroupResponse(AffinityGroupJoinVO vag) {
        AffinityGroupResponse agResponse = new AffinityGroupResponse();
        agResponse.setId(vag.getUuid());
        agResponse.setName(vag.getName());
        agResponse.setDescription(vag.getDescription());
        agResponse.setType(vag.getType());

        ApiResponseHelper.populateOwner(agResponse, vag);

        Long callerId = CallContext.current().getCallingAccountId();
        boolean isCallerRootAdmin = accountManager.isRootAdmin(callerId);
        boolean containsDedicatedResources = vag.getType().equals("ExplicitDedication");
        if (isCallerRootAdmin && containsDedicatedResources) {
            List<DedicatedResourceVO> dedicatedResources = dedicatedResourceDao.listByAffinityGroupId(vag.getId());
            this.populateDedicatedResourcesField(dedicatedResources, agResponse);
        }

        // update vm information
        long instanceId = vag.getVmId();
        if (instanceId > 0) {
            List<String> vmIdList = new ArrayList<String>();
            vmIdList.add(vag.getVmUuid());
            agResponse.setVMIdList(vmIdList);
        }

        agResponse.setObjectName("affinitygroup");
        return agResponse;
    }

    private void populateDedicatedResourcesField(List<DedicatedResourceVO> dedicatedResources, AffinityGroupResponse agResponse) {
        if (dedicatedResources.isEmpty()) {
            return;
        }

        for (DedicatedResourceVO resource : dedicatedResources) {
            DedicatedResourceResponse dedicatedResourceResponse = null;

            if (resource.getDataCenterId() != null) {
                DataCenter dataCenter = dataCenterDao.findById(resource.getDataCenterId());
                dedicatedResourceResponse = new DedicatedResourceResponse(dataCenter.getUuid(), dataCenter.getName(), DedicatedResources.Type.Zone);
            } else if (resource.getPodId() != null) {
                HostPodVO pod = podDao.findById(resource.getPodId());
                dedicatedResourceResponse = new DedicatedResourceResponse(pod.getUuid(), pod.getName(), DedicatedResources.Type.Pod);
            } else if (resource.getClusterId() != null) {
                Cluster cluster = clusterDao.findById(resource.getClusterId());
                dedicatedResourceResponse = new DedicatedResourceResponse(cluster.getUuid(), cluster.getName(), DedicatedResources.Type.Cluster);
            } else if (resource.getHostId() != null) {
                Host host = hostDao.findById(resource.getHostId());
                dedicatedResourceResponse = new DedicatedResourceResponse(host.getUuid(), host.getName(), DedicatedResources.Type.Host);
            }

            agResponse.addDedicatedResource(dedicatedResourceResponse);
        }
    }

    @Override
    public AffinityGroupResponse setAffinityGroupResponse(AffinityGroupResponse vagData, AffinityGroupJoinVO vag) {
        // update vm information
        long instanceId = vag.getVmId();
        if (instanceId > 0) {
            vagData.addVMId(vag.getVmUuid());
        }
        return vagData;
    }

    @Override
    public List<AffinityGroupJoinVO> newAffinityGroupView(AffinityGroup ag) {

        SearchCriteria<AffinityGroupJoinVO> sc = agIdSearch.create();
        sc.setParameters("id", ag.getId());
        return searchIncludingRemoved(sc, null, null, false);
    }

    @Override
    public List<AffinityGroupJoinVO> searchByIds(Long... agIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<AffinityGroupJoinVO> uvList = new ArrayList<AffinityGroupJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (agIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= agIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = agIds[j];
                }
                SearchCriteria<AffinityGroupJoinVO> sc = agSearch.create();
                sc.setParameters("idIN", ids);
                List<AffinityGroupJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < agIds.length) {
            int batch_size = (agIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = agIds[j];
            }
            SearchCriteria<AffinityGroupJoinVO> sc = agSearch.create();
            sc.setParameters("idIN", ids);
            List<AffinityGroupJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }
}
