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

import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Component
@Local(value={VolumeJoinDao.class})
public class VolumeJoinDaoImpl extends GenericDaoBase<VolumeJoinVO, Long> implements VolumeJoinDao {
    public static final Logger s_logger = Logger.getLogger(VolumeJoinDaoImpl.class);

    @Inject
    private ConfigurationDao  _configDao;

    private final SearchBuilder<VolumeJoinVO> volSearch;

    private final SearchBuilder<VolumeJoinVO> volIdSearch;

    protected VolumeJoinDaoImpl() {

        volSearch = createSearchBuilder();
        volSearch.and("idIN", volSearch.entity().getId(), SearchCriteria.Op.IN);
        volSearch.done();

        volIdSearch = createSearchBuilder();
        volIdSearch.and("id", volIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        volIdSearch.done();

        this._count = "select count(distinct id) from volume_view WHERE ";
    }




    @Override
    public VolumeResponse newVolumeResponse(VolumeJoinVO volume) {
        Account caller = CallContext.current().getCallingAccount();

        VolumeResponse volResponse = new VolumeResponse();
        volResponse.setId(volume.getUuid());

        if (volume.getName() != null) {
            volResponse.setName(volume.getName());
        } else {
            volResponse.setName("");
        }

        volResponse.setZoneId(volume.getDataCenterUuid());
        volResponse.setZoneName(volume.getDataCenterName());

        volResponse.setVolumeType(volume.getVolumeType().toString());
        volResponse.setDeviceId(volume.getDeviceId());

        long instanceId = volume.getVmId();
        if (instanceId > 0 && volume.getState() != Volume.State.Destroy) {
            volResponse.setVirtualMachineId(volume.getVmUuid());
            volResponse.setVirtualMachineName(volume.getVmName());
            volResponse.setVirtualMachineState(volume.getVmState().toString());
            volResponse.setVirtualMachineDisplayName(volume.getVmDisplayName());
        }

        // Show the virtual size of the volume
        volResponse.setSize(volume.getSize());

        volResponse.setMinIops(volume.getMinIops());
        volResponse.setMaxIops(volume.getMaxIops());

        volResponse.setCreated(volume.getCreated());
        volResponse.setState(volume.getState().toString());
        if (volume.getState() == Volume.State.UploadOp) {
            // com.cloud.storage.VolumeHostVO volumeHostRef =
            // ApiDBUtils.findVolumeHostRef(volume.getId(),
            // volume.getDataCenterId());
            volResponse.setSize(volume.getVolumeHostSize());
            volResponse.setCreated(volume.getVolumeHostCreated());

            if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)
                volResponse.setHypervisor(ApiDBUtils.getHypervisorTypeFromFormat(volume.getFormat()).toString());
            if (volume.getDownloadState() != Status.DOWNLOADED) {
                String volumeStatus = "Processing";
                if (volume.getDownloadState() == VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS) {
                    if (volume.getDownloadPercent() == 100) {
                        volumeStatus = "Checking Volume";
                    } else {
                        volumeStatus = volume.getDownloadPercent() + "% Uploaded";
                    }
                    volResponse.setState("Uploading");
                } else {
                    volumeStatus = volume.getErrorString();
                    if (volume.getDownloadState() == VMTemplateHostVO.Status.NOT_DOWNLOADED) {
                        volResponse.setState("UploadNotStarted");
                    } else {
                        volResponse.setState("UploadError");
                    }
                }
                volResponse.setStatus(volumeStatus);
            } else if (volume.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
                volResponse.setStatus("Upload Complete");
                volResponse.setState("Uploaded");
            } else {
                volResponse.setStatus("Successfully Installed");
            }
        }

        // populate owner.
        ApiResponseHelper.populateOwner(volResponse, volume);

        // DiskOfferingVO diskOffering =
        // ApiDBUtils.findDiskOfferingById(volume.getDiskOfferingId());
        if (volume.getDiskOfferingId() > 0) {
            if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
                volResponse.setServiceOfferingId(volume.getDiskOfferingUuid());
            } else {
                volResponse.setDiskOfferingId(volume.getDiskOfferingUuid());
            }

            if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
                volResponse.setServiceOfferingName(volume.getDiskOfferingName());
                volResponse.setServiceOfferingDisplayText(volume.getDiskOfferingDisplayText());
            } else {
                volResponse.setDiskOfferingName(volume.getDiskOfferingName());
                volResponse.setDiskOfferingDisplayText(volume.getDiskOfferingDisplayText());
            }
            volResponse.setStorageType(volume.isUseLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared
                    .toString());
            volResponse.setBytesReadRate(volume.getBytesReadRate());
            volResponse.setBytesWriteRate(volume.getBytesReadRate());
            volResponse.setIopsReadRate(volume.getIopsWriteRate());
            volResponse.setIopsWriteRate(volume.getIopsWriteRate());

        }
        Long poolId = volume.getPoolId();
        String poolName = (poolId == null) ? "none" : volume.getPoolName();
        volResponse.setStoragePoolName(poolName);

        // return hypervisor for ROOT and Resource domain only
        if ((caller.getType() == Account.ACCOUNT_TYPE_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)
                && volume.getState() != Volume.State.UploadOp && volume.getHypervisorType() != null) {
            volResponse.setHypervisor(volume.getHypervisorType().toString());
        }

        volResponse.setAttached(volume.getAttached());
        volResponse.setDestroyed(volume.getState() == Volume.State.Destroy);
        boolean isExtractable = true;
        if (volume.getVolumeType() != Volume.Type.DATADISK) { // Datadisk dont
            // have any
            // template
            // dependence.
            if (volume.getTemplateId() > 0) { // For ISO based volumes template
                // = null and we allow extraction
                // of all ISO based volumes
                isExtractable = volume.isExtractable() && volume.getTemplateType() != Storage.TemplateType.SYSTEM;
            }
        }

        // update tag information
        long tag_id = volume.getTagId();
        if (tag_id > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if (vtag != null) {
                volResponse.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }

        volResponse.setExtractable(isExtractable);
        volResponse.setDisplayVm(volume.isDisplayVolume());

        // set async job
        if (volume.getJobId() != null) {
            volResponse.setJobId(volume.getJobUuid());
            volResponse.setJobStatus(volume.getJobStatus());
        }

        volResponse.setObjectName("volume");
        return volResponse;
    }



    @Override
    public VolumeResponse setVolumeResponse(VolumeResponse volData, VolumeJoinVO vol) {
        long tag_id = vol.getTagId();
        if (tag_id > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if ( vtag != null ){
                volData.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }
        return volData;
    }




    @Override
    public List<VolumeJoinVO> newVolumeView(Volume vol) {
        SearchCriteria<VolumeJoinVO> sc = volIdSearch.create();
        sc.setParameters("id", vol.getId());
        return searchIncludingRemoved(sc, null, null, false);
    }




    @Override
    public List<VolumeJoinVO> searchByIds(Long... volIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if ( batchCfg != null ){
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<VolumeJoinVO> uvList = new ArrayList<VolumeJoinVO>();
        // query details by batches
        int curr_index = 0;
        if ( volIds.length > DETAILS_BATCH_SIZE ){
            while ( (curr_index + DETAILS_BATCH_SIZE ) <= volIds.length ) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = volIds[j];
                }
                SearchCriteria<VolumeJoinVO> sc = volSearch.create();
                sc.setParameters("idIN", ids);
                List<VolumeJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
                if (vms != null) {
                    uvList.addAll(vms);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < volIds.length) {
            int batch_size = (volIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = volIds[j];
            }
            SearchCriteria<VolumeJoinVO> sc = volSearch.create();
            sc.setParameters("idIN", ids);
            List<VolumeJoinVO> vms = searchIncludingRemoved(sc, null, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }



}
