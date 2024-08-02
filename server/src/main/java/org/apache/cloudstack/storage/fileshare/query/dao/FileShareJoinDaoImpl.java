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

package org.apache.cloudstack.storage.fileshare.query.dao;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.query.vo.FileShareJoinVO;

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

public class FileShareJoinDaoImpl extends GenericDaoBase<FileShareJoinVO, Long> implements FileShareJoinDao {

    @Inject
    NicDao nicDao;

    @Inject
    NetworkDao networkDao;

    @Inject
    private VmDiskStatisticsDao vmDiskStatsDao;

    private final SearchBuilder<FileShareJoinVO> fsSearch;
    private final SearchBuilder<FileShareJoinVO> fsIdInSearch;

    protected FileShareJoinDaoImpl() {
        fsSearch = createSearchBuilder();
        fsSearch.and("id", fsSearch.entity().getId(), SearchCriteria.Op.EQ);
        fsSearch.done();

        fsIdInSearch = createSearchBuilder();
        fsIdInSearch.and("idIN", fsIdInSearch.entity().getId(), SearchCriteria.Op.IN);
        fsIdInSearch.done();
    }

    @Override
    public FileShareJoinVO newFileShareView(FileShare fileShare) {
        SearchCriteria<FileShareJoinVO> sc = fsSearch.create();
        sc.setParameters("id", fileShare.getId());
        List<FileShareJoinVO> fileShares = searchIncludingRemoved(sc, null, null, false);
        assert fileShares != null && fileShares.size() == 1 : "No file shares found for id " + fileShare.getId();
        return fileShares.get(0);
    }

    @Override
    public FileShareResponse newFileShareResponse(ResponseObject.ResponseView view, FileShareJoinVO fileShare) {
        FileShareResponse response = new FileShareResponse();
        response.setId(fileShare.getUuid());
        response.setName(fileShare.getName());
        response.setDescription(fileShare.getDescription());
        response.setState(fileShare.getState().toString());
        response.setProvider(fileShare.getProvider());
        response.setFormat(fileShare.getFsType().toString());
        response.setPath(FileShare.getFileSharePathFromNameAndUuid(fileShare.getName(), fileShare.getUuid()));
        response.setObjectName(FileShare.class.getSimpleName().toLowerCase());
        response.setZoneId(fileShare.getZoneUuid());
        response.setZoneName(fileShare.getZoneName());

        response.setVirtualMachineId(fileShare.getInstanceUuid());
        if (fileShare.getIntanceState() != null) {
            response.setVirtualMachineState(fileShare.getIntanceState().toString());
        }
        response.setVolumeId(fileShare.getVolumeUuid());
        response.setVolumeName(fileShare.getVolumeName());

        response.setStoragePoolId(fileShare.getPoolUuid());
        response.setStoragePoolName(fileShare.getPoolName());

        final List<NicVO> nics = nicDao.listByVmId(fileShare.getInstanceId());
        if (nics.size() > 0) {
            for (NicVO nicVO : nics) {
                final NetworkVO network = networkDao.findById(nicVO.getNetworkId());
                response.setIpAddress(nicVO.getIPv4Address());
                NicResponse nicResponse = new NicResponse();
                nicResponse.setId(nicVO.getUuid());
                nicResponse.setNetworkid(network.getUuid());
                nicResponse.setIpaddress(nicVO.getIPv4Address());
                nicResponse.setNetworkName(network.getName());
                nicResponse.setObjectName("nic");
                response.addNic(nicResponse);
            }
        }

        response.setAccountName(fileShare.getAccountName());

        response.setDomainId(fileShare.getDomainUuid());
        response.setDomainName(fileShare.getDomainName());

        response.setProjectId(fileShare.getProjectUuid());
        response.setProjectName(fileShare.getProjectName());

        response.setDiskOfferingId(fileShare.getDiskOfferingUuid());
        response.setDiskOfferingName(fileShare.getDiskOfferingName());
        response.setDiskOfferingDisplayText(fileShare.getDiskOfferingDisplayText());
        response.setIsCustomDiskOffering(fileShare.isDiskOfferingCustom());
        if (fileShare.isDiskOfferingCustom() == true) {
            response.setSize(fileShare.getSize());
        } else {
            response.setSize(fileShare.getDiskOfferingSize());
        }
        response.setSizeGB(fileShare.getSize());

        response.setServiceOfferingId(fileShare.getServiceOfferingUuid());
        response.setServiceOfferingName(fileShare.getServiceOfferingName());

        if (fileShare.getProvisioningType() != null) {
            response.setProvisioningType(fileShare.getProvisioningType().toString());
        }

        VmDiskStatisticsVO diskStats = vmDiskStatsDao.findBy(fileShare.getAccountId(), fileShare.getZoneId(), fileShare.getInstanceId(), fileShare.getVolumeId());
        if (diskStats != null) {
            response.setDiskIORead(diskStats.getCurrentIORead());
            response.setDiskIOWrite(diskStats.getCurrentIOWrite());
            response.setDiskKbsRead((long) (diskStats.getCurrentBytesRead() / 1024.0));
            response.setDiskKbsWrite((long) (diskStats.getCurrentBytesWrite() / 1024.0));
        }

        VolumeStats vs = null;
        if (fileShare.getVolumeFormat() == Storage.ImageFormat.VHD || fileShare.getVolumeFormat() == Storage.ImageFormat.QCOW2 || fileShare.getVolumeFormat() == Storage.ImageFormat.RAW) {
            if (fileShare.getVolumePath() != null) {
                vs = ApiDBUtils.getVolumeStatistics(fileShare.getVolumePath());
            }
        } else if (fileShare.getVolumeFormat() == Storage.ImageFormat.OVA) {
            if (fileShare.getVolumeChainInfo() != null) {
                vs = ApiDBUtils.getVolumeStatistics(fileShare.getVolumeChainInfo());
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

    public List<FileShareResponse> createFileShareResponses(ResponseObject.ResponseView view, FileShareJoinVO... fileShares) {
        List<FileShareResponse> fileShareResponses = new ArrayList<>();

        for (FileShareJoinVO fileShare : fileShares) {
            fileShareResponses.add(newFileShareResponse(view, fileShare));
        }
        return fileShareResponses;
    }

    @Override
    public List<FileShareJoinVO> searchByIds(Long... fileShareIds) {
        SearchCriteria<FileShareJoinVO> sc = fsIdInSearch.create();
        sc.setParameters("idIN", fileShareIds);
        return search(sc, null, null, false);
    }
}
