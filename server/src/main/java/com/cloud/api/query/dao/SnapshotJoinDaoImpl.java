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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.query.QueryService;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.SnapshotJoinVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.Snapshot;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VMInstanceVO;

public class SnapshotJoinDaoImpl extends GenericDaoBaseWithTagInformation<SnapshotJoinVO, SnapshotResponse> implements SnapshotJoinDao {

    @Inject
    private AccountService accountService;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private ConfigurationDao configDao;
    @Inject
    SnapshotDataFactory snapshotDataFactory;

    private final SearchBuilder<SnapshotJoinVO> snapshotStorePairSearch;

    private final SearchBuilder<SnapshotJoinVO> snapshotIdsSearch;

    SnapshotJoinDaoImpl() {
        snapshotStorePairSearch = createSearchBuilder();
        snapshotStorePairSearch.and("snapshotStoreState", snapshotStorePairSearch.entity().getStoreState(), SearchCriteria.Op.IN);
        snapshotStorePairSearch.and("snapshotStoreIdIN", snapshotStorePairSearch.entity().getSnapshotStorePair(), SearchCriteria.Op.IN);
        snapshotStorePairSearch.done();

        snapshotIdsSearch = createSearchBuilder();
        snapshotIdsSearch.and("zoneId", snapshotIdsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        snapshotIdsSearch.and("idsIN", snapshotIdsSearch.entity().getId(), SearchCriteria.Op.IN);
        snapshotIdsSearch.groupBy(snapshotIdsSearch.entity().getId());
        snapshotIdsSearch.done();
    }

    private void setSnapshotInfoDetailsInResponse(SnapshotJoinVO snapshot, SnapshotResponse snapshotResponse, boolean isShowUnique) {
        if (!isShowUnique) {
            return;
        }
        if (snapshot.getDataCenterId() == null) {
            return;
        }
        SnapshotInfo snapshotInfo = null;
        snapshotInfo = snapshotDataFactory.getSnapshotWithRoleAndZone(snapshot.getId(), snapshot.getStoreRole(), snapshot.getDataCenterId());
        if (snapshotInfo == null) {
            logger.debug("Unable to find info for image store snapshot with uuid " + snapshot.getUuid());
            snapshotResponse.setRevertable(false);
        } else {
            snapshotResponse.setRevertable(snapshotInfo.isRevertable());
            snapshotResponse.setPhysicalSize(snapshotInfo.getPhysicalSize());
        }
    }

    private String getSnapshotStatus(SnapshotJoinVO snapshot) {
        String status = snapshot.getStatus().toString();
        if (snapshot.getDownloadState() == null) {
            return status;
        }
        if (snapshot.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
            status = "Processing";
            if (snapshot.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                status = snapshot.getDownloadPercent() + "% Downloaded";
            } else if (snapshot.getErrorString() == null) {
                status = snapshot.getStoreState().toString();
            } else {
                status = snapshot.getErrorString();
            }
        }
        return status;
    }

    @Override
    public SnapshotResponse newSnapshotResponse(ResponseObject.ResponseView view, boolean isShowUnique, SnapshotJoinVO snapshot) {
        final Account caller = CallContext.current().getCallingAccount();
        SnapshotResponse snapshotResponse = new SnapshotResponse();
        snapshotResponse.setId(snapshot.getUuid());
        // populate owner.
        ApiResponseHelper.populateOwner(snapshotResponse, snapshot);
        if (snapshot.getVolumeId() != null) {
            snapshotResponse.setVolumeId(snapshot.getVolumeUuid());
            snapshotResponse.setVolumeName(snapshot.getVolumeName());
            snapshotResponse.setVolumeType(snapshot.getVolumeType().name());
            snapshotResponse.setVolumeState(snapshot.getVolumeState().name());
            snapshotResponse.setVirtualSize(snapshot.getVolumeSize());
            VolumeVO volume = ApiDBUtils.findVolumeById(snapshot.getVolumeId());
            if (volume != null && volume.getVolumeType() == Type.ROOT && volume.getInstanceId() != null) {
                VMInstanceVO vm = ApiDBUtils.findVMInstanceById(volume.getInstanceId());
                if (vm != null) {
                    GuestOS guestOS = ApiDBUtils.findGuestOSById(vm.getGuestOSId());
                    if (guestOS != null) {
                        snapshotResponse.setOsTypeId(guestOS.getUuid());
                    }
                }
            }
        }
        snapshotResponse.setZoneId(snapshot.getDataCenterUuid());
        snapshotResponse.setZoneName(snapshot.getDataCenterName());
        snapshotResponse.setCreated(snapshot.getCreated());
        snapshotResponse.setName(snapshot.getName());
        String intervalType = null;
        if (snapshot.getSnapshotType() >= 0 && snapshot.getSnapshotType() < Snapshot.Type.values().length) {
            intervalType = Snapshot.Type.values()[snapshot.getSnapshotType()].name();
        }
        snapshotResponse.setIntervalType(intervalType);
        snapshotResponse.setState(snapshot.getStatus());
        snapshotResponse.setLocationType(snapshot.getLocationType() != null ? snapshot.getLocationType().name() : null);
        if (!isShowUnique) {
            snapshotResponse.setDatastoreState(snapshot.getStoreState() != null ? snapshot.getStoreState().name() : null);
            if (view.equals(ResponseObject.ResponseView.Full)) {
                snapshotResponse.setDatastoreId(snapshot.getStoreUuid());
                snapshotResponse.setDatastoreName(snapshot.getStoreName());
                snapshotResponse.setDatastoreType(snapshot.getStoreRole() != null ? snapshot.getStoreRole().name() : null);
            }
            // If the user is an 'Admin' or 'the owner of template' or template belongs to a project, add the template download status
            if (view == ResponseObject.ResponseView.Full ||
                    snapshot.getAccountId() == caller.getId() ||
                    snapshot.getAccountType() == Account.Type.PROJECT) {
                String status = getSnapshotStatus(snapshot);
                if (status != null) {
                    snapshotResponse.setStatus(status);
                }
            }
            Map<String, String> downloadDetails = new HashMap<>();
            downloadDetails.put("downloadPercent", Integer.toString(snapshot.getDownloadPercent()));
            downloadDetails.put("downloadState", (snapshot.getDownloadState() != null ? snapshot.getDownloadState().toString() : ""));
            snapshotResponse.setDownloadDetails(downloadDetails);
        }
        setSnapshotInfoDetailsInResponse(snapshot, snapshotResponse, isShowUnique);
        setSnapshotResponse(snapshotResponse, snapshot);

        snapshotResponse.setObjectName("snapshot");
        return snapshotResponse;
    }

    @Override
    public SnapshotResponse setSnapshotResponse(SnapshotResponse snapshotResponse, SnapshotJoinVO snapshot) {
        // update tag information
        long tag_id = snapshot.getTagId();
        if (tag_id > 0) {
            addTagInformation(snapshot, snapshotResponse);
        }

        if (snapshotResponse.hasAnnotation() == null) {
            snapshotResponse.setHasAnnotation(annotationDao.hasAnnotations(snapshot.getUuid(), AnnotationService.EntityType.SNAPSHOT.name(),
                    accountService.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        }
        return snapshotResponse;
    }

    @Override
    public Pair<List<SnapshotJoinVO>, Integer> searchIncludingRemovedAndCount(final SearchCriteria<SnapshotJoinVO> sc, final Filter filter) {
        List<SnapshotJoinVO> objects = searchIncludingRemoved(sc, filter, null, false);
        Integer count = getDistinctCount(sc);
        return new Pair<>(objects, count);
    }

    @Override
    public List<SnapshotJoinVO> searchBySnapshotStorePair(String... pairs) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        Filter searchFilter = new Filter(SnapshotJoinVO.class, "snapshotStorePair", QueryService.SortKeyAscending.value(), null, null);
        List<SnapshotJoinVO> uvList = new ArrayList<>();
        // query details by batches
        int curr_index = 0;
        if (pairs.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= pairs.length) {
                String[] labels = new String[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    labels[k] = pairs[j];
                }
                SearchCriteria<SnapshotJoinVO> sc = snapshotStorePairSearch.create();
                sc.setParameters("snapshotStoreIdIN", labels);
                List<SnapshotJoinVO> snaps = searchIncludingRemoved(sc, searchFilter, null, false);
                if (snaps != null) {
                    uvList.addAll(snaps);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < pairs.length) {
            int batch_size = (pairs.length - curr_index);
            String[] labels = new String[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                labels[k] = pairs[j];
            }
            SearchCriteria<SnapshotJoinVO> sc = snapshotStorePairSearch.create();
            sc.setParameters("snapshotStoreIdIN", labels);
            List<SnapshotJoinVO> vms = searchIncludingRemoved(sc, searchFilter, null, false);
            if (vms != null) {
                uvList.addAll(vms);
            }
        }
        return uvList;
    }

    @Override
    public List<SnapshotJoinVO> findByDistinctIds(Long zoneId, Long... ids) {
        if (ids == null || ids.length == 0) {
            return new ArrayList<>();
        }

        Filter searchFilter = new Filter(SnapshotJoinVO.class, "snapshotStorePair", QueryService.SortKeyAscending.value(), null, null);

        SearchCriteria<SnapshotJoinVO> sc = snapshotIdsSearch.create();
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        sc.setParameters("idsIN", ids);
        return searchIncludingRemoved(sc, searchFilter, null, false);
    }
}
