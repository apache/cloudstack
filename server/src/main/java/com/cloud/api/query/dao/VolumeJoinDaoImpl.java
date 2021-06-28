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

import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.Volume;
import com.cloud.user.AccountManager;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.VmDiskStatisticsDao;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class VolumeJoinDaoImpl extends GenericDaoBaseWithTagInformation<VolumeJoinVO, VolumeResponse> implements VolumeJoinDao {
    public static final Logger s_logger = Logger.getLogger(VolumeJoinDaoImpl.class);

    @Inject
    private ConfigurationDao  _configDao;
    @Inject
    public AccountManager _accountMgr;
    @Inject
    private VmDiskStatisticsDao vmDiskStatsDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;

    private final SearchBuilder<VolumeJoinVO> volSearch;

    private final SearchBuilder<VolumeJoinVO> volIdSearch;

    protected VolumeJoinDaoImpl() {

        volSearch = createSearchBuilder();
        volSearch.and("idIN", volSearch.entity().getId(), SearchCriteria.Op.IN);
        volSearch.done();

        volIdSearch = createSearchBuilder();
        volIdSearch.and("id", volIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        volIdSearch.done();

        _count = "select count(distinct id) from volume_view WHERE ";
    }

    @Override
    public VolumeResponse newVolumeResponse(ResponseView view, VolumeJoinVO volume) {
        VolumeResponse volResponse = new VolumeResponse();
        volResponse.setId(volume.getUuid());

        if (volume.getName() != null) {
            volResponse.setName(volume.getName());
        } else {
            volResponse.setName("");
        }

        volResponse.setZoneId(volume.getDataCenterUuid());
        volResponse.setZoneName(volume.getDataCenterName());
        if (view == ResponseView.Full) {
            volResponse.setClusterId(volume.getClusterUuid());
            volResponse.setClusterName(volume.getClusterName());
            volResponse.setPodId(volume.getPodUuid());
            volResponse.setPodName(volume.getPodName());
        }

        if (volume.getVolumeType() != null) {
            volResponse.setVolumeType(volume.getVolumeType().toString());
        }
        volResponse.setDeviceId(volume.getDeviceId());

        long instanceId = volume.getVmId();
        if (instanceId > 0 && volume.getState() != Volume.State.Destroy) {
            volResponse.setVirtualMachineId(volume.getVmUuid());
            volResponse.setVirtualMachineName(volume.getVmName());
            if (volume.getVmState() != null) {
                volResponse.setVirtualMachineState(volume.getVmState().toString());
            }
            if (volume.getVmDisplayName() != null) {
                volResponse.setVirtualMachineDisplayName(volume.getVmDisplayName());
            } else {
                volResponse.setVirtualMachineDisplayName(volume.getVmName());
            }

            VmDiskStatisticsVO diskStats = vmDiskStatsDao.findBy(volume.getAccountId(), volume.getDataCenterId(), instanceId, volume.getId());
            if (diskStats != null) {
                volResponse.setDiskIORead(diskStats.getCurrentIORead());
                volResponse.setDiskIOWrite(diskStats.getCurrentIOWrite());
                volResponse.setDiskKbsRead((long) (diskStats.getCurrentBytesRead() / 1024.0));
                volResponse.setDiskKbsWrite((long) (diskStats.getCurrentBytesWrite() / 1024.0));
            }
        }

        if (volume.getProvisioningType() != null) {
            volResponse.setProvisioningType(volume.getProvisioningType().toString());
        }

        // Show the virtual size of the volume
        volResponse.setSize(volume.getSize());

        volResponse.setMinIops(volume.getMinIops());
        volResponse.setMaxIops(volume.getMaxIops());

        volResponse.setCreated(volume.getCreated());
        if (volume.getState() != null) {
            volResponse.setState(volume.getState().toString());
        }
        if (volume.getState() == Volume.State.UploadOp) {
            volResponse.setSize(volume.getVolumeStoreSize());
            volResponse.setCreated(volume.getCreatedOnStore());

            if (view == ResponseView.Full)
                volResponse.setHypervisor(ApiDBUtils.getHypervisorTypeFromFormat(volume.getDataCenterId(), volume.getFormat()).toString());
            if (volume.getDownloadState() != Status.DOWNLOADED) {
                String volumeStatus = "Processing";
                if (volume.getDownloadState() == Status.DOWNLOAD_IN_PROGRESS) {
                    if (volume.getDownloadPercent() == 100) {
                        volumeStatus = "Checking Volume";
                    } else {
                        volumeStatus = volume.getDownloadPercent() + "% Uploaded";
                    }
                    volResponse.setState("Uploading");
                } else {
                    volumeStatus = volume.getErrorString();
                    if (volume.getDownloadState() == Status.NOT_DOWNLOADED) {
                        volResponse.setState("UploadNotStarted");
                    } else {
                        volResponse.setState("UploadError");
                    }
                }
                volResponse.setStatus(volumeStatus);
            } else if (volume.getDownloadState() == Status.DOWNLOADED) {
                volResponse.setStatus("Upload Complete");
                volResponse.setState("Uploaded");
            } else {
                volResponse.setStatus("Successfully Installed");
            }
        }

        if (view == ResponseView.Full) {
            volResponse.setPath(volume.getPath());
        }

        // populate owner.
        ApiResponseHelper.populateOwner(volResponse, volume);

        if (volume.getDiskOfferingId() > 0) {
            if (ApiDBUtils.findServiceOfferingByUuid(volume.getDiskOfferingUuid()) != null) {
                volResponse.setServiceOfferingId(volume.getDiskOfferingUuid());
                volResponse.setServiceOfferingName(volume.getDiskOfferingName());
                volResponse.setServiceOfferingDisplayText(volume.getDiskOfferingDisplayText());
            } else {
                volResponse.setDiskOfferingId(volume.getDiskOfferingUuid());
                volResponse.setDiskOfferingName(volume.getDiskOfferingName());
                volResponse.setDiskOfferingDisplayText(volume.getDiskOfferingDisplayText());
            }

            if (view == ResponseView.Full) {
                volResponse.setStorageType(volume.isUseLocalStorage() ? ServiceOffering.StorageType.local.toString() : ServiceOffering.StorageType.shared.toString());
            }
            volResponse.setBytesReadRate(volume.getBytesReadRate());
            volResponse.setBytesWriteRate(volume.getBytesReadRate());
            volResponse.setIopsReadRate(volume.getIopsWriteRate());
            volResponse.setIopsWriteRate(volume.getIopsWriteRate());

        }

        // return hypervisor and storage pool info for ROOT and Resource domain only
        if (view == ResponseView.Full) {
            if (volume.getState() != Volume.State.UploadOp) {
                if (volume.getHypervisorType() != null) {
                    volResponse.setHypervisor(volume.getHypervisorType().toString());
                } else {
                    volResponse.setHypervisor(ApiDBUtils.getHypervisorTypeFromFormat(volume.getDataCenterId(), volume.getFormat()).toString());
                }
            }
            Long poolId = volume.getPoolId();
            String poolName = (poolId == null) ? "none" : volume.getPoolName();
            volResponse.setStoragePoolName(poolName);
            volResponse.setStoragePoolId(volume.getPoolUuid());
            if (poolId != null) {
                StoragePoolVO poolVO = primaryDataStoreDao.findById(poolId);
                if (poolVO != null && poolVO.getParent() != 0L) {
                    StoragePoolVO datastoreClusterVO = primaryDataStoreDao.findById(poolVO.getParent());
                    volResponse.setStoragePoolName(datastoreClusterVO.getName());
                    volResponse.setStoragePoolId(datastoreClusterVO.getUuid());
                }
            }
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
            addTagInformation(volume, volResponse);
        }

        volResponse.setExtractable(isExtractable);
        volResponse.setDisplayVolume(volume.isDisplayVolume());
        volResponse.setChainInfo(volume.getChainInfo());

        volResponse.setTemplateId(volume.getTemplateUuid());
        volResponse.setTemplateName(volume.getTemplateName());
        volResponse.setTemplateDisplayText(volume.getTemplateDisplayText());

        volResponse.setIsoId(volume.getIsoUuid());
        volResponse.setIsoName(volume.getIsoName());
        volResponse.setIsoDisplayText(volume.getIsoDisplayText());

        // set async job
        if (volume.getJobId() != null) {
            volResponse.setJobId(volume.getJobUuid());
            volResponse.setJobStatus(volume.getJobStatus());
        }

        volResponse.setObjectName("volume");
        return volResponse;
    }

    @Override
    public VolumeResponse setVolumeResponse(ResponseView view, VolumeResponse volData, VolumeJoinVO vol) {
        long tag_id = vol.getTagId();
        if (tag_id > 0) {
            addTagInformation(vol, volData);
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
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<VolumeJoinVO> uvList = new ArrayList<VolumeJoinVO>();
        // query details by batches
        int curr_index = 0;
        if (volIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= volIds.length) {
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
