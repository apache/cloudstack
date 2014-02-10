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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.ApiConstants.HostDetails;
import org.apache.cloudstack.api.response.HostForMigrationResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.storage.StorageStats;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {HostJoinDao.class})
public class HostJoinDaoImpl extends GenericDaoBase<HostJoinVO, Long> implements HostJoinDao {
    public static final Logger s_logger = Logger.getLogger(HostJoinDaoImpl.class);

    @Inject
    private ConfigurationDao _configDao;

    private final SearchBuilder<HostJoinVO> hostSearch;

    private final SearchBuilder<HostJoinVO> hostIdSearch;

    protected HostJoinDaoImpl() {

        hostSearch = createSearchBuilder();
        hostSearch.and("idIN", hostSearch.entity().getId(), SearchCriteria.Op.IN);
        hostSearch.done();

        hostIdSearch = createSearchBuilder();
        hostIdSearch.and("id", hostIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        hostIdSearch.done();

        this._count = "select count(distinct id) from host_view WHERE ";
    }

    @Override
    public HostResponse newHostResponse(HostJoinVO host, EnumSet<HostDetails> details) {
        HostResponse hostResponse = new HostResponse();
        hostResponse.setId(host.getUuid());
        hostResponse.setCapabilities(host.getCapabilities());
        hostResponse.setClusterId(host.getClusterUuid());
        hostResponse.setCpuSockets(host.getCpuSockets());
        hostResponse.setCpuNumber(host.getCpus());
        hostResponse.setZoneId(host.getZoneUuid());
        hostResponse.setDisconnectedOn(host.getDisconnectedOn());
        hostResponse.setHypervisor(host.getHypervisorType());
        hostResponse.setHostType(host.getType());
        hostResponse.setLastPinged(new Date(host.getLastPinged()));
        hostResponse.setManagementServerId(host.getManagementServerId());
        hostResponse.setName(host.getName());
        hostResponse.setPodId(host.getPodUuid());
        hostResponse.setRemoved(host.getRemoved());
        hostResponse.setCpuSpeed(host.getSpeed());
        hostResponse.setState(host.getStatus());
        hostResponse.setIpAddress(host.getPrivateIpAddress());
        hostResponse.setVersion(host.getVersion());
        hostResponse.setCreated(host.getCreated());

        if (details.contains(HostDetails.all) || details.contains(HostDetails.capacity) || details.contains(HostDetails.stats) || details.contains(HostDetails.events)) {

            hostResponse.setOsCategoryId(host.getOsCategoryUuid());
            hostResponse.setOsCategoryName(host.getOsCategoryName());
            hostResponse.setZoneName(host.getZoneName());
            hostResponse.setPodName(host.getPodName());
            if (host.getClusterId() > 0) {
                hostResponse.setClusterName(host.getClusterName());
                hostResponse.setClusterType(host.getClusterType().toString());
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        if (host.getType() == Host.Type.Routing) {
            if (details.contains(HostDetails.all) || details.contains(HostDetails.capacity)) {
                // set allocated capacities
                Long mem = host.getMemReservedCapacity() + host.getMemUsedCapacity();
                Long cpu = host.getCpuReservedCapacity() + host.getCpuUsedCapacity();

                hostResponse.setMemoryAllocated(mem);
                hostResponse.setMemoryTotal(host.getTotalMemory());

                String hostTags = host.getTag();
                hostResponse.setHostTags(host.getTag());

                String haTag = ApiDBUtils.getHaTag();
                if (haTag != null && !haTag.isEmpty() && hostTags != null && !hostTags.isEmpty()) {
                    if (haTag.equalsIgnoreCase(hostTags)) {
                        hostResponse.setHaHost(true);
                    } else {
                        hostResponse.setHaHost(false);
                    }
                } else {
                    hostResponse.setHaHost(false);
                }

                hostResponse.setHypervisorVersion(host.getHypervisorVersion());

                String cpuAlloc = decimalFormat.format(((float)cpu / (float)(host.getCpus() * host.getSpeed())) * 100f) + "%";
                hostResponse.setCpuAllocated(cpuAlloc);
                String cpuWithOverprovisioning = new Float(host.getCpus() * host.getSpeed() * ApiDBUtils.getCpuOverprovisioningFactor()).toString();
                hostResponse.setCpuWithOverprovisioning(cpuWithOverprovisioning);
            }

            if (details.contains(HostDetails.all) || details.contains(HostDetails.stats)) {
                // set CPU/RAM/Network stats
                String cpuUsed = null;
                HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
                if (hostStats != null) {
                    float cpuUtil = (float)hostStats.getCpuUtilization();
                    cpuUsed = decimalFormat.format(cpuUtil) + "%";
                    hostResponse.setCpuUsed(cpuUsed);
                    hostResponse.setMemoryUsed((new Double(hostStats.getUsedMemory())).longValue());
                    hostResponse.setNetworkKbsRead((new Double(hostStats.getNetworkReadKBs())).longValue());
                    hostResponse.setNetworkKbsWrite((new Double(hostStats.getNetworkWriteKBs())).longValue());

                }
            }

        } else if (host.getType() == Host.Type.SecondaryStorage) {
            StorageStats secStorageStats = ApiDBUtils.getSecondaryStorageStatistics(host.getId());
            if (secStorageStats != null) {
                hostResponse.setDiskSizeTotal(secStorageStats.getCapacityBytes());
                hostResponse.setDiskSizeAllocated(secStorageStats.getByteUsed());
            }
        }

        hostResponse.setLocalStorageActive(ApiDBUtils.isLocalStorageActiveOnHost(host.getId()));

        if (details.contains(HostDetails.all) || details.contains(HostDetails.events)) {
            Set<com.cloud.host.Status.Event> possibleEvents = host.getStatus().getPossibleEvents();
            if ((possibleEvents != null) && !possibleEvents.isEmpty()) {
                String events = "";
                Iterator<com.cloud.host.Status.Event> iter = possibleEvents.iterator();
                while (iter.hasNext()) {
                    com.cloud.host.Status.Event event = iter.next();
                    events += event.toString();
                    if (iter.hasNext()) {
                        events += "; ";
                    }
                }
                hostResponse.setEvents(events);
            }
        }

        hostResponse.setResourceState(host.getResourceState().toString());

        // set async job
        if (host.getJobId() != null) {
            hostResponse.setJobId(host.getJobUuid());
            hostResponse.setJobStatus(host.getJobStatus());
        }

        hostResponse.setObjectName("host");

        return hostResponse;
    }

    @Override
    public HostResponse setHostResponse(HostResponse response, HostJoinVO host) {
        String tag = host.getTag();
        if (tag != null) {
            if (response.getHostTags() != null && response.getHostTags().length() > 0) {
                response.setHostTags(response.getHostTags() + "," + tag);
            } else {
                response.setHostTags(tag);
            }
        }
        return response;
    }

    @Override
    public HostForMigrationResponse newHostForMigrationResponse(HostJoinVO host, EnumSet<HostDetails> details) {
        HostForMigrationResponse hostResponse = new HostForMigrationResponse();
        hostResponse.setId(host.getUuid());
        hostResponse.setCapabilities(host.getCapabilities());
        hostResponse.setClusterId(host.getClusterUuid());
        hostResponse.setCpuNumber(host.getCpus());
        hostResponse.setZoneId(host.getZoneUuid());
        hostResponse.setDisconnectedOn(host.getDisconnectedOn());
        hostResponse.setHypervisor(host.getHypervisorType());
        hostResponse.setHostType(host.getType());
        hostResponse.setLastPinged(new Date(host.getLastPinged()));
        hostResponse.setManagementServerId(host.getManagementServerId());
        hostResponse.setName(host.getName());
        hostResponse.setPodId(host.getPodUuid());
        hostResponse.setRemoved(host.getRemoved());
        hostResponse.setCpuSpeed(host.getSpeed());
        hostResponse.setState(host.getStatus());
        hostResponse.setIpAddress(host.getPrivateIpAddress());
        hostResponse.setVersion(host.getVersion());
        hostResponse.setCreated(host.getCreated());

        if (details.contains(HostDetails.all) || details.contains(HostDetails.capacity) || details.contains(HostDetails.stats) || details.contains(HostDetails.events)) {

            hostResponse.setOsCategoryId(host.getOsCategoryUuid());
            hostResponse.setOsCategoryName(host.getOsCategoryName());
            hostResponse.setZoneName(host.getZoneName());
            hostResponse.setPodName(host.getPodName());
            if (host.getClusterId() > 0) {
                hostResponse.setClusterName(host.getClusterName());
                hostResponse.setClusterType(host.getClusterType().toString());
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        if (host.getType() == Host.Type.Routing) {
            if (details.contains(HostDetails.all) || details.contains(HostDetails.capacity)) {
                // set allocated capacities
                Long mem = host.getMemReservedCapacity() + host.getMemUsedCapacity();
                Long cpu = host.getCpuReservedCapacity() + host.getCpuReservedCapacity();

                hostResponse.setMemoryAllocated(mem);
                hostResponse.setMemoryTotal(host.getTotalMemory());

                String hostTags = host.getTag();
                hostResponse.setHostTags(host.getTag());

                String haTag = ApiDBUtils.getHaTag();
                if (haTag != null && !haTag.isEmpty() && hostTags != null && !hostTags.isEmpty()) {
                    if (haTag.equalsIgnoreCase(hostTags)) {
                        hostResponse.setHaHost(true);
                    } else {
                        hostResponse.setHaHost(false);
                    }
                } else {
                    hostResponse.setHaHost(false);
                }

                hostResponse.setHypervisorVersion(host.getHypervisorVersion());

                String cpuAlloc = decimalFormat.format(((float)cpu / (float)(host.getCpus() * host.getSpeed())) * 100f) + "%";
                hostResponse.setCpuAllocated(cpuAlloc);
                String cpuWithOverprovisioning = new Float(host.getCpus() * host.getSpeed() * ApiDBUtils.getCpuOverprovisioningFactor()).toString();
                hostResponse.setCpuWithOverprovisioning(cpuWithOverprovisioning);
            }

            if (details.contains(HostDetails.all) || details.contains(HostDetails.stats)) {
                // set CPU/RAM/Network stats
                String cpuUsed = null;
                HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
                if (hostStats != null) {
                    float cpuUtil = (float)hostStats.getCpuUtilization();
                    cpuUsed = decimalFormat.format(cpuUtil) + "%";
                    hostResponse.setCpuUsed(cpuUsed);
                    hostResponse.setMemoryUsed((new Double(hostStats.getUsedMemory())).longValue());
                    hostResponse.setNetworkKbsRead((new Double(hostStats.getNetworkReadKBs())).longValue());
                    hostResponse.setNetworkKbsWrite((new Double(hostStats.getNetworkWriteKBs())).longValue());

                }
            }

        } else if (host.getType() == Host.Type.SecondaryStorage) {
            StorageStats secStorageStats = ApiDBUtils.getSecondaryStorageStatistics(host.getId());
            if (secStorageStats != null) {
                hostResponse.setDiskSizeTotal(secStorageStats.getCapacityBytes());
                hostResponse.setDiskSizeAllocated(secStorageStats.getByteUsed());
            }
        }

        hostResponse.setLocalStorageActive(ApiDBUtils.isLocalStorageActiveOnHost(host.getId()));

        if (details.contains(HostDetails.all) || details.contains(HostDetails.events)) {
            Set<com.cloud.host.Status.Event> possibleEvents = host.getStatus().getPossibleEvents();
            if ((possibleEvents != null) && !possibleEvents.isEmpty()) {
                String events = "";
                Iterator<com.cloud.host.Status.Event> iter = possibleEvents.iterator();
                while (iter.hasNext()) {
                    com.cloud.host.Status.Event event = iter.next();
                    events += event.toString();
                    if (iter.hasNext()) {
                        events += "; ";
                    }
                }
                hostResponse.setEvents(events);
            }
        }

        hostResponse.setResourceState(host.getResourceState().toString());

        // set async job
        hostResponse.setJobId(host.getJobUuid());
        hostResponse.setJobStatus(host.getJobStatus());

        hostResponse.setObjectName("host");

        return hostResponse;
    }

    @Override
    public HostForMigrationResponse setHostForMigrationResponse(HostForMigrationResponse response, HostJoinVO host) {
        String tag = host.getTag();
        if (tag != null) {
            if (response.getHostTags() != null && response.getHostTags().length() > 0) {
                response.setHostTags(response.getHostTags() + "," + tag);
            } else {
                response.setHostTags(tag);
            }
        }
        return response;
    }

    @Override
    public List<HostJoinVO> newHostView(Host host) {
        SearchCriteria<HostJoinVO> sc = hostIdSearch.create();
        sc.setParameters("id", host.getId());
        return searchIncludingRemoved(sc, null, null, false);

    }

    @Override
    public List<HostJoinVO> searchByIds(Long... hostIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<HostJoinVO> uvList = new ArrayList<HostJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (hostIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= hostIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = hostIds[j];
                }
                SearchCriteria<HostJoinVO> sc = hostSearch.create();
                sc.setParameters("idIN", ids);
                List<HostJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < hostIds.length) {
            int batch_size = (hostIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = hostIds[j];
            }
            SearchCriteria<HostJoinVO> sc = hostSearch.create();
            sc.setParameters("idIN", ids);
            List<HostJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

}
