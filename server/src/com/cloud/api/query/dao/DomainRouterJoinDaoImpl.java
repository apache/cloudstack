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

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value={DomainRouterJoinDao.class})
public class DomainRouterJoinDaoImpl extends GenericDaoBase<DomainRouterJoinVO, Long> implements DomainRouterJoinDao {
    public static final Logger s_logger = Logger.getLogger(DomainRouterJoinDaoImpl.class);

    @Inject
    private ConfigurationDao  _configDao;

    private final SearchBuilder<DomainRouterJoinVO> vrSearch;

    private final SearchBuilder<DomainRouterJoinVO> vrIdSearch;

    protected DomainRouterJoinDaoImpl() {

        vrSearch = createSearchBuilder();
        vrSearch.and("idIN", vrSearch.entity().getId(), SearchCriteria.Op.IN);
        vrSearch.done();

        vrIdSearch = createSearchBuilder();
        vrIdSearch.and("id", vrIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        vrIdSearch.done();

        this._count = "select count(distinct id) from domain_router_view WHERE ";
    }


    @Override
    public DomainRouterResponse newDomainRouterResponse(DomainRouterJoinVO router, Account caller) {
        DomainRouterResponse routerResponse = new DomainRouterResponse();
        routerResponse.setId(router.getUuid());
        routerResponse.setZoneId(router.getDataCenterUuid());
        routerResponse.setName(router.getName());
        routerResponse.setTemplateId(router.getTemplateUuid());
        routerResponse.setCreated(router.getCreated());
        routerResponse.setState(router.getState());
        routerResponse.setIsRedundantRouter(router.isRedundantRouter());
        routerResponse.setRedundantState(router.getRedundantState().toString());

        if (caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN
                || caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            if (router.getHostId() != null) {
                routerResponse.setHostId(router.getHostUuid());
                routerResponse.setHostName(router.getHostName());
            }
            routerResponse.setPodId(router.getPodUuid());
            long nic_id = router.getNicId();
            if (nic_id > 0) {
                TrafficType ty = router.getTrafficType();
                if (ty != null) {
                    // legacy code, public/control/guest nic info is kept in
                    // nics response object
                    if (ty == TrafficType.Public) {
                        routerResponse.setPublicIp(router.getIpAddress());
                        routerResponse.setPublicMacAddress(router.getMacAddress());
                        routerResponse.setPublicNetmask(router.getNetmask());
                        routerResponse.setGateway(router.getGateway());
                        routerResponse.setPublicNetworkId(router.getNetworkUuid());
                    } else if (ty == TrafficType.Control) {
                        routerResponse.setLinkLocalIp(router.getIpAddress());
                        routerResponse.setLinkLocalMacAddress(router.getMacAddress());
                        routerResponse.setLinkLocalNetmask(router.getNetmask());
                        routerResponse.setLinkLocalNetworkId(router.getNetworkUuid());
                    } else if (ty == TrafficType.Guest) {
                        routerResponse.setGuestIpAddress(router.getIpAddress());
                        routerResponse.setGuestMacAddress(router.getMacAddress());
                        routerResponse.setGuestNetmask(router.getNetmask());
                        routerResponse.setGuestNetworkId(router.getNetworkUuid());
                        routerResponse.setNetworkDomain(router.getNetworkDomain());
                    }
                }

                NicResponse nicResponse = new NicResponse();
                nicResponse.setId(router.getNicUuid());
                nicResponse.setIpaddress(router.getIpAddress());
                nicResponse.setGateway(router.getGateway());
                nicResponse.setNetmask(router.getNetmask());
                nicResponse.setNetworkid(router.getNetworkUuid());
                nicResponse.setNetworkName(router.getNetworkName());
                nicResponse.setMacAddress(router.getMacAddress());
                nicResponse.setIp6Address(router.getIp6Address());
                nicResponse.setIp6Gateway(router.getIp6Gateway());
                nicResponse.setIp6Cidr(router.getIp6Cidr());
                if (router.getBroadcastUri() != null) {
                    nicResponse.setBroadcastUri(router.getBroadcastUri().toString());
                }
                if (router.getIsolationUri() != null) {
                    nicResponse.setIsolationUri(router.getIsolationUri().toString());
                }
                if (router.getTrafficType() != null) {
                    nicResponse.setTrafficType(router.getTrafficType().toString());
                }
                if (router.getGuestType() != null) {
                    nicResponse.setType(router.getGuestType().toString());
                }
                nicResponse.setIsDefault(router.isDefaultNic());
                nicResponse.setObjectName("nic");
                routerResponse.addNic(nicResponse);
            }
        }

        routerResponse.setServiceOfferingId(router.getServiceOfferingUuid());
        routerResponse.setServiceOfferingName(router.getServiceOfferingName());

        // populate owner.
        ApiResponseHelper.populateOwner(routerResponse, router);


        routerResponse.setDomainId(router.getDomainUuid());
        routerResponse.setDomainName(router.getDomainName());

        routerResponse.setZoneName(router.getDataCenterName());
        routerResponse.setDns1(router.getDns1());
        routerResponse.setDns2(router.getDns2());

        routerResponse.setIp6Dns1(router.getIp6Dns1());
        routerResponse.setIp6Dns2(router.getIp6Dns2());

        routerResponse.setVpcId(router.getVpcUuid());

        // set async job
        routerResponse.setJobId(router.getJobUuid());
        routerResponse.setJobStatus(router.getJobStatus());

        routerResponse.setObjectName("router");

        return routerResponse;
    }


    @Override
    public DomainRouterResponse setDomainRouterResponse(DomainRouterResponse vrData, DomainRouterJoinVO vr) {
        long nic_id = vr.getNicId();
        if (nic_id > 0) {
            TrafficType ty = vr.getTrafficType();
            if (ty != null) {
                // legacy code, public/control/guest nic info is kept in
                // nics response object
                if (ty == TrafficType.Public) {
                    vrData.setPublicIp(vr.getIpAddress());
                    vrData.setPublicMacAddress(vr.getMacAddress());
                    vrData.setPublicNetmask(vr.getNetmask());
                    vrData.setGateway(vr.getGateway());
                    vrData.setPublicNetworkId(vr.getNetworkUuid());
                } else if (ty == TrafficType.Control) {
                    vrData.setLinkLocalIp(vr.getIpAddress());
                    vrData.setLinkLocalMacAddress(vr.getMacAddress());
                    vrData.setLinkLocalNetmask(vr.getNetmask());
                    vrData.setLinkLocalNetworkId(vr.getNetworkUuid());
                } else if (ty == TrafficType.Guest) {
                    vrData.setGuestIpAddress(vr.getIpAddress());
                    vrData.setGuestMacAddress(vr.getMacAddress());
                    vrData.setGuestNetmask(vr.getNetmask());
                    vrData.setGuestNetworkId(vr.getNetworkUuid());
                    vrData.setNetworkDomain(vr.getNetworkDomain());
                }
            }
            NicResponse nicResponse = new NicResponse();
            nicResponse.setId(vr.getNicUuid());
            nicResponse.setIpaddress(vr.getIpAddress());
            nicResponse.setGateway(vr.getGateway());
            nicResponse.setNetmask(vr.getNetmask());
            nicResponse.setNetworkid(vr.getNetworkUuid());
            nicResponse.setMacAddress(vr.getMacAddress());
            nicResponse.setIp6Address(vr.getIp6Address());
            nicResponse.setIp6Gateway(vr.getIp6Gateway());
            nicResponse.setIp6Cidr(vr.getIp6Cidr());
            if (vr.getBroadcastUri() != null) {
                nicResponse.setBroadcastUri(vr.getBroadcastUri().toString());
            }
            if (vr.getIsolationUri() != null) {
                nicResponse.setIsolationUri(vr.getIsolationUri().toString());
            }
            if (vr.getTrafficType() != null) {
                nicResponse.setTrafficType(vr.getTrafficType().toString());
            }
            if (vr.getGuestType() != null) {
                nicResponse.setType(vr.getGuestType().toString());
            }
            nicResponse.setIsDefault(vr.isDefaultNic());
            nicResponse.setObjectName("nic");
            vrData.addNic(nicResponse);
        }
        return vrData;
    }




    @Override
    public List<DomainRouterJoinVO> searchByIds(Long... vrIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if ( batchCfg != null ){
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<DomainRouterJoinVO> uvList = new ArrayList<DomainRouterJoinVO>();
        // query details by batches
        int curr_index = 0;
        if ( vrIds.length > DETAILS_BATCH_SIZE ){
            while ( (curr_index + DETAILS_BATCH_SIZE ) <= vrIds.length ) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = vrIds[j];
                }
                SearchCriteria<DomainRouterJoinVO> sc = vrSearch.create();
                sc.setParameters("idIN", ids);
                List<DomainRouterJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < vrIds.length) {
            int batch_size = (vrIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = vrIds[j];
            }
            SearchCriteria<DomainRouterJoinVO> sc = vrSearch.create();
            sc.setParameters("idIN", ids);
            List<DomainRouterJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }


    @Override
    public List<DomainRouterJoinVO> newDomainRouterView(VirtualRouter vr) {

        SearchCriteria<DomainRouterJoinVO> sc = vrIdSearch.create();
        sc.setParameters("id", vr.getId());
        return searchIncludingRemoved(sc, null, null, false);
    }

}
