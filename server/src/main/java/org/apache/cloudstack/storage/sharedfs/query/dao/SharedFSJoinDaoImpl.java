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

package org.apache.cloudstack.storage.sharedfs.query.dao;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.SharedFSResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.apache.cloudstack.storage.sharedfs.query.vo.SharedFSJoinVO;

import com.cloud.api.ApiDBUtils;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeStats;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.VmDiskStatisticsDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;

public class SharedFSJoinDaoImpl extends GenericDaoBase<SharedFSJoinVO, Long> implements SharedFSJoinDao {

    @Inject
    NicDao nicDao;

    @Inject
    NetworkDao networkDao;

    @Inject
    private VmDiskStatisticsDao vmDiskStatsDao;

    private final SearchBuilder<SharedFSJoinVO> fsSearch;
    private final SearchBuilder<SharedFSJoinVO> fsIdInSearch;

    protected SharedFSJoinDaoImpl() {
        fsSearch = createSearchBuilder();
        fsSearch.and("id", fsSearch.entity().getId(), SearchCriteria.Op.EQ);
        fsSearch.done();

        fsIdInSearch = createSearchBuilder();
        fsIdInSearch.and("idIN", fsIdInSearch.entity().getId(), SearchCriteria.Op.IN);
        fsIdInSearch.done();
    }

    @Override
    public SharedFSJoinVO newSharedFSView(SharedFS sharedFS) {
        SearchCriteria<SharedFSJoinVO> sc = fsSearch.create();
        sc.setParameters("id", sharedFS.getId());
        List<SharedFSJoinVO> sharedFSs = searchIncludingRemoved(sc, null, null, false);
        assert sharedFSs != null && sharedFSs.size() == 1 : "No shared filesystem found for id " + sharedFS.getId();
        return sharedFSs.get(0);
    }

    @Override
    public SharedFSResponse newSharedFSResponse(ResponseObject.ResponseView view, SharedFSJoinVO sharedFS) {
        SharedFSResponse response = new SharedFSResponse();
        response.setId(sharedFS.getUuid());
        response.setName(sharedFS.getName());
        response.setDescription(sharedFS.getDescription());
        response.setState(sharedFS.getState().toString());
        response.setProvider(sharedFS.getProvider());
        response.setFilesystem(sharedFS.getFsType().toString());
        response.setPath(SharedFS.getSharedFSPath());
        response.setObjectName(SharedFS.class.getSimpleName().toLowerCase());
        response.setZoneId(sharedFS.getZoneUuid());
        response.setZoneName(sharedFS.getZoneName());

        response.setVirtualMachineId(sharedFS.getInstanceUuid());
        if (sharedFS.getInstanceState() != null) {
            response.setVirtualMachineState(sharedFS.getInstanceState().toString());
        }
        response.setVolumeId(sharedFS.getVolumeUuid());
        response.setVolumeName(sharedFS.getVolumeName());

        response.setStoragePoolId(sharedFS.getPoolUuid());
        response.setStoragePoolName(sharedFS.getPoolName());

        final List<NicVO> nics = nicDao.listByVmId(sharedFS.getInstanceId());
        if (nics.size() > 0) {
            for (NicVO nicVO : nics) {
                final NetworkVO network = networkDao.findById(nicVO.getNetworkId());
                NicResponse nicResponse = new NicResponse();
                nicResponse.setId(nicVO.getUuid());
                nicResponse.setNetworkid(network.getUuid());
                nicResponse.setIpaddress(nicVO.getIPv4Address());
                nicResponse.setNetworkName(network.getName());
                nicResponse.setObjectName("nic");
                response.addNic(nicResponse);
            }
        }

        response.setAccountName(sharedFS.getAccountName());

        response.setDomainId(sharedFS.getDomainUuid());
        response.setDomainName(sharedFS.getDomainName());
        response.setDomainName(sharedFS.getDomainPath());

        response.setProjectId(sharedFS.getProjectUuid());
        response.setProjectName(sharedFS.getProjectName());

        response.setDiskOfferingId(sharedFS.getDiskOfferingUuid());
        response.setDiskOfferingName(sharedFS.getDiskOfferingName());
        response.setDiskOfferingDisplayText(sharedFS.getDiskOfferingDisplayText());
        response.setIsCustomDiskOffering(sharedFS.isDiskOfferingCustom());
        if (sharedFS.isDiskOfferingCustom() == true) {
            response.setSize(sharedFS.getSize());
        } else {
            response.setSize(sharedFS.getDiskOfferingSize());
        }
        response.setSizeGB(sharedFS.getSize());

        response.setServiceOfferingId(sharedFS.getServiceOfferingUuid());
        response.setServiceOfferingName(sharedFS.getServiceOfferingName());

        if (sharedFS.getProvisioningType() != null) {
            response.setProvisioningType(sharedFS.getProvisioningType().toString());
        }

        VmDiskStatisticsVO diskStats = vmDiskStatsDao.findBy(sharedFS.getAccountId(), sharedFS.getZoneId(), sharedFS.getInstanceId(), sharedFS.getVolumeId());
        if (diskStats != null) {
            response.setDiskIORead(diskStats.getCurrentIORead());
            response.setDiskIOWrite(diskStats.getCurrentIOWrite());
            response.setDiskKbsRead((long) (diskStats.getCurrentBytesRead() / 1024.0));
            response.setDiskKbsWrite((long) (diskStats.getCurrentBytesWrite() / 1024.0));
        }

        VolumeStats vs = null;
        if (sharedFS.getVolumeFormat() == Storage.ImageFormat.VHD || sharedFS.getVolumeFormat() == Storage.ImageFormat.QCOW2 || sharedFS.getVolumeFormat() == Storage.ImageFormat.RAW) {
            if (sharedFS.getVolumePath() != null) {
                vs = ApiDBUtils.getVolumeStatistics(sharedFS.getVolumePath());
            }
        } else if (sharedFS.getVolumeFormat() == Storage.ImageFormat.OVA) {
            if (sharedFS.getVolumeChainInfo() != null) {
                vs = ApiDBUtils.getVolumeStatistics(sharedFS.getVolumeChainInfo());
            }
        }
        if (vs != null) {
            response.setVirtualSize(vs.getVirtualSize());
            response.setPhysicalSize(vs.getPhysicalSize());
            double util = (double) vs.getPhysicalSize() / vs.getVirtualSize();
            DecimalFormat df = new DecimalFormat("0.0%");
            response.setUtilization(df.format(util));
        }

        return response;
    }

    public List<SharedFSResponse> createSharedFSResponses(ResponseObject.ResponseView view, SharedFSJoinVO... sharedFSs) {
        List<SharedFSResponse> sharedFSRespons = new ArrayList<>();

        for (SharedFSJoinVO sharedFS : sharedFSs) {
            sharedFSRespons.add(newSharedFSResponse(view, sharedFS));
        }
        return sharedFSRespons;
    }

    @Override
    public List<SharedFSJoinVO> searchByIds(Long... sharedFSIds) {
        SearchCriteria<SharedFSJoinVO> sc = fsIdInSearch.create();
        sc.setParameters("idIN", sharedFSIds);
        return search(sc, null, null, false);
    }
}
