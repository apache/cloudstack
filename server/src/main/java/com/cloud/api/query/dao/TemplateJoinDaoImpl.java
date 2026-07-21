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

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.ChildTemplateResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.VnfNicResponse;
import org.apache.cloudstack.api.response.VnfTemplateResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateState;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.query.vo.ResourceTagJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.deployasis.DeployAsIsConstants;
import com.cloud.deployasis.TemplateDeployAsIsDetailVO;
import com.cloud.deployasis.dao.TemplateDeployAsIsDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VnfTemplateDetailVO;
import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VnfTemplateDetailsDao;
import com.cloud.storage.dao.VnfTemplateNicDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;


@Component
public class TemplateJoinDaoImpl extends GenericDaoBaseWithTagInformation<TemplateJoinVO, TemplateResponse> implements TemplateJoinDao {


    @Inject
    private ConfigurationDao  _configDao;
    @Inject
    private AccountService _accountService;
    @Inject
    private VMTemplateDao _vmTemplateDao;
    @Inject
    private TemplateDataStoreDao _templateStoreDao;
    @Inject
    private ImageStoreDao dataStoreDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private VMTemplatePoolDao templatePoolDao;
    @Inject
    private VMTemplateDetailsDao _templateDetailsDao;
    @Inject
    private TemplateDeployAsIsDetailsDao templateDeployAsIsDetailsDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private UserDataDao userDataDao;
    @Inject
    VnfTemplateDetailsDao vnfTemplateDetailsDao;
    @Inject
    VnfTemplateNicDao vnfTemplateNicDao;

    private final SearchBuilder<TemplateJoinVO> tmpltIdPairSearch;

    private final SearchBuilder<TemplateJoinVO> tmpltIdPairCrossZoneSearch;

    private final SearchBuilder<TemplateJoinVO> tmpltIdSearch;

    private final SearchBuilder<TemplateJoinVO> tmpltIdsSearch;

    private final SearchBuilder<TemplateJoinVO> tmpltZoneSearch;

    private final SearchBuilder<TemplateJoinVO> activeTmpltSearch;

    private final SearchBuilder<TemplateJoinVO> publicTmpltSearch;

    protected TemplateJoinDaoImpl() {

        tmpltIdPairSearch = createSearchBuilder();
        tmpltIdPairSearch.and("template_dc_pair_templateid", tmpltIdPairSearch.entity().getId(), SearchCriteria.Op.EQ);
        tmpltIdPairSearch.and("template_dc_pair_dcid", tmpltIdPairSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        tmpltIdPairSearch.done();

        tmpltIdPairCrossZoneSearch = createSearchBuilder();
        tmpltIdPairCrossZoneSearch.and("template_dc_pair_templateid", tmpltIdPairCrossZoneSearch.entity().getId(), SearchCriteria.Op.EQ);
        tmpltIdPairCrossZoneSearch.and("template_dc_pair_dcid", tmpltIdPairCrossZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.NULL);
        tmpltIdPairCrossZoneSearch.done();

        tmpltIdSearch = createSearchBuilder();
        tmpltIdSearch.and("id", tmpltIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        tmpltIdSearch.done();

        tmpltIdsSearch = createSearchBuilder();
        tmpltIdsSearch.and("idsIN", tmpltIdsSearch.entity().getId(), SearchCriteria.Op.IN);
        tmpltIdsSearch.groupBy(tmpltIdsSearch.entity().getId());
        tmpltIdsSearch.done();

        tmpltZoneSearch = createSearchBuilder();
        tmpltZoneSearch.and("id", tmpltZoneSearch.entity().getId(), SearchCriteria.Op.EQ);
        tmpltZoneSearch.and("dataCenterId", tmpltZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        tmpltZoneSearch.and("state", tmpltZoneSearch.entity().getState(), SearchCriteria.Op.EQ);
        tmpltZoneSearch.done();

        activeTmpltSearch = createSearchBuilder();
        activeTmpltSearch.and("store_id", activeTmpltSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        activeTmpltSearch.and("type", activeTmpltSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
        activeTmpltSearch.and("templateState", activeTmpltSearch.entity().getTemplateState(), SearchCriteria.Op.EQ);
        activeTmpltSearch.done();

        publicTmpltSearch = createSearchBuilder();
        publicTmpltSearch.and("public", publicTmpltSearch.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
        publicTmpltSearch.done();

        // select distinct pair (template_id, zone_id)
        _count = "select count(distinct temp_zone_pair) from template_view WHERE ";
    }

    private enum TemplateStatus {
        SUCCESSFULLY_INSTALLED("Successfully Installed"),
        INSTALLING_TEMPLATE("Installing Template"),
        INSTALLING_ISO("Installing ISO"),
        BYPASSED_SECONDARY_STORAGE("Bypassed Secondary Storage"),
        PROCESSING("Processing"),
        DOWNLOADING("%d%% Downloaded"),
        DOWNLOAD_COMPLETE("Download Complete");

        private final String status;
        TemplateStatus(String status) {
            this.status = status;
        }
        public String getStatus() {
            return status;
        }
        // For statuses that have dynamic details (e.g. "75% Downloaded").
        public String format(int percent) {
            return String.format(status, percent);
        }
    }

    private String getTemplateStatus(TemplateJoinVO template) {
        if (template == null) {
            return  null;
        }
        boolean isIso = Storage.ImageFormat.ISO == template.getFormat();
        TemplateStatus templateStatus;
        if (template.getDownloadState() == Status.DOWNLOADED) {
            templateStatus =  isIso ? TemplateStatus.SUCCESSFULLY_INSTALLED : TemplateStatus.DOWNLOAD_COMPLETE;
        } else if (template.getDownloadState() == Status.DOWNLOAD_IN_PROGRESS) {
            if (template.getDownloadPercent() == 100) {
                templateStatus = isIso ? TemplateStatus.INSTALLING_ISO : TemplateStatus.INSTALLING_TEMPLATE;
            } else {
                return TemplateStatus.DOWNLOADING.format(template.getDownloadPercent());
            }
        } else if (template.getDownloadState() == Status.BYPASSED) {
            templateStatus = TemplateStatus.BYPASSED_SECONDARY_STORAGE;
        } else if (StringUtils.isNotBlank(template.getErrorString())) {
            return template.getErrorString().trim();
        } else {
            templateStatus = TemplateStatus.PROCESSING;
        }
        return templateStatus.getStatus();
    }

    @Override
    public TemplateResponse newTemplateResponse(EnumSet<ApiConstants.DomainDetails> detailsView, ResponseView view, TemplateJoinVO template) {
        List<ImageStoreVO> storesInZone = dataStoreDao.listStoresByZoneId(template.getDataCenterId());
        Long[] storeIds = storesInZone.stream().map(ImageStoreVO::getId).toArray(Long[]::new);
        List<TemplateDataStoreVO> templatesInStore = _templateStoreDao.listByTemplateNotBypassed(template.getId(), storeIds);

        List<Long> dataStoreIdList = templatesInStore.stream().map(TemplateDataStoreVO::getDataStoreId).collect(Collectors.toList());
        Map<Long, ImageStoreVO> imageStoreMap = dataStoreDao.listByIds(dataStoreIdList).stream().collect(Collectors.toMap(ImageStoreVO::getId, imageStore -> imageStore));

        List<Map<String, String>> downloadProgressDetails = new ArrayList<>();
        HashMap<String, String> downloadDetailInImageStores = null;
        for (TemplateDataStoreVO templateInStore : templatesInStore) {
            downloadDetailInImageStores = new HashMap<>();
            ImageStoreVO imageStore = imageStoreMap.get(templateInStore.getDataStoreId());
            if (imageStore != null) {
                downloadDetailInImageStores.put("datastore", imageStore.getName());
                if (view.equals(ResponseView.Full)) {
                    downloadDetailInImageStores.put("datastoreId", imageStore.getUuid());
                    downloadDetailInImageStores.put("datastoreRole", imageStore.getRole().name());
                }
                downloadDetailInImageStores.put("downloadPercent", Integer.toString(templateInStore.getDownloadPercent()));
                downloadDetailInImageStores.put("downloadState", (templateInStore.getDownloadState() != null ? templateInStore.getDownloadState().toString() : ""));
                downloadProgressDetails.add(downloadDetailInImageStores);
            }
        }

        List<StoragePoolVO> poolsInZone = primaryDataStoreDao.listByDataCenterId(template.getDataCenterId());
        List<Long> poolIds = poolsInZone.stream().map(StoragePoolVO::getId).collect(Collectors.toList());
        List<VMTemplateStoragePoolVO> templatesInPool = templatePoolDao.listByTemplateId(template.getId(), poolIds);

        dataStoreIdList = templatesInStore.stream().map(TemplateDataStoreVO::getDataStoreId).collect(Collectors.toList());
        Map<Long, StoragePoolVO> storagePoolMap = primaryDataStoreDao.listByIds(dataStoreIdList).stream().collect(Collectors.toMap(StoragePoolVO::getId, store -> store));

        for (VMTemplateStoragePoolVO templateInPool : templatesInPool) {
            downloadDetailInImageStores = new HashMap<>();
            StoragePoolVO storagePool = storagePoolMap.get(templateInPool.getDataStoreId());
            if (storagePool != null) {
                downloadDetailInImageStores.put("datastore", storagePool.getName());
                if (view.equals(ResponseView.Full)) {
                    downloadDetailInImageStores.put("datastoreId", storagePool.getUuid());
                    downloadDetailInImageStores.put("datastoreRole", DataStoreRole.Primary.name());
                }
                downloadDetailInImageStores.put("downloadPercent", Integer.toString(templateInPool.getDownloadPercent()));
                downloadDetailInImageStores.put("downloadState", (templateInPool.getDownloadState() != null ? templateInPool.getDownloadState().toString() : ""));
                downloadProgressDetails.add(downloadDetailInImageStores);
            }
        }

        TemplateResponse templateResponse = initTemplateResponse(template);
        templateResponse.setDownloadProgress(downloadProgressDetails);
        templateResponse.setId(template.getUuid());
        templateResponse.setName(template.getName());
        templateResponse.setDisplayText(template.getDisplayText());
        templateResponse.setPublic(template.isPublicTemplate());
        templateResponse.setCreated(template.getCreatedOnStore());
        if (template.getFormat() == Storage.ImageFormat.BAREMETAL || template.getFormat() == Storage.ImageFormat.EXTERNAL) {
            // for baremetal template, we didn't download, but is ready to use.
            templateResponse.setReady(true);
        } else {
            templateResponse.setReady(template.getState() == ObjectInDataStoreStateMachine.State.Ready);
        }
        templateResponse.setFeatured(template.isFeatured());
        templateResponse.setExtractable(template.isExtractable() && !(template.getTemplateType() == TemplateType.SYSTEM));
        templateResponse.setPasswordEnabled(template.isEnablePassword());
        templateResponse.setDynamicallyScalable(template.isDynamicallyScalable());
        templateResponse.setSshKeyEnabled(template.isEnableSshKey());
        templateResponse.setCrossZones(template.isCrossZones());
        if (template.getTemplateType() != null) {
            templateResponse.setTemplateType(template.getTemplateType().toString());
        }

        templateResponse.setHypervisor(template.getHypervisorType().getHypervisorDisplayName());
        templateResponse.setFormat(template.getFormat());

        templateResponse.setOsTypeId(template.getGuestOSUuid());
        templateResponse.setOsTypeName(template.getGuestOSName());
        templateResponse.setOsTypeCategoryId(template.getGuestOSCategoryId());

        // populate owner.
        ApiResponseHelper.populateOwner(templateResponse, template);

        // If the user is an 'Admin' or 'the owner of template' or template belongs to a project, add the template download status
        if (view == ResponseView.Full ||
                template.getAccountId() == CallContext.current().getCallingAccount().getId() ||
                template.getAccountType() == Account.Type.PROJECT) {
            String templateStatus = getTemplateStatus(template);
            if (templateStatus != null) {
                templateResponse.setStatus(templateStatus);
            }
            templateResponse.setUrl(template.getUrl());
        }

        if (template.getDataCenterId() > 0) {
            templateResponse.setZoneId(template.getDataCenterUuid());
            templateResponse.setZoneName(template.getDataCenterName());
        }

        Long templateSize = template.getSize();
        if (templateSize > 0) {
            templateResponse.setSize(templateSize);
        }

        Long templatePhysicalSize = template.getPhysicalSize();
        if (templatePhysicalSize > 0) {
            templateResponse.setPhysicalSize(templatePhysicalSize);
        }

        templateResponse.setChecksum(DigestHelper.getHashValueFromChecksumValue(template.getChecksum()));
        if (template.getSourceTemplateId() != null) {
            templateResponse.setSourceTemplateId(template.getSourceTemplateUuid());
        }
        templateResponse.setTemplateTag(template.getTemplateTag());

        if (template.getParentTemplateId() != null) {
            templateResponse.setParentTemplateId(template.getParentTemplateUuid());
        }

        // set details map
        if (detailsView.contains(ApiConstants.DomainDetails.all)) {
            Map<String, String> details = _templateDetailsDao.listDetailsKeyPairs(template.getId());
            templateResponse.setDetails(details);

            setDeployAsIsDetails(template, templateResponse);
            templateResponse.setForCks(template.isForCks());
        }

        // update tag information
        long tag_id = template.getTagId();
        if (tag_id > 0) {
            addTagInformation(template, templateResponse);
        }

        templateResponse.setHasAnnotation(annotationDao.hasAnnotations(template.getUuid(), AnnotationService.EntityType.TEMPLATE.name(),
                _accountService.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        templateResponse.setDirectDownload(template.isDirectDownload());
        templateResponse.setDeployAsIs(template.isDeployAsIs());
        templateResponse.setRequiresHvm(template.isRequiresHvm());
        if (template.getArch() != null) {
            templateResponse.setArch(template.getArch().getType());
        }
        if (template.getExtensionId() != null) {
            templateResponse.setExtensionId(template.getExtensionUuid());
            templateResponse.setExtensionName(template.getExtensionName());
        }

        //set template children disks
        Set<ChildTemplateResponse> childTemplatesSet = new HashSet<ChildTemplateResponse>();
        if (template.getHypervisorType() == HypervisorType.VMware) {
            List<VMTemplateVO> childTemplates = _vmTemplateDao.listByParentTemplatetId(template.getId());
            for (VMTemplateVO tmpl : childTemplates) {
                if (tmpl.getTemplateType() != TemplateType.ISODISK) {
                    ChildTemplateResponse childTempl = new ChildTemplateResponse();
                    childTempl.setId(tmpl.getUuid());
                    childTempl.setName(tmpl.getName());
                    childTempl.setSize(Math.round(tmpl.getSize() / (1024 * 1024 * 1024)));
                    childTemplatesSet.add(childTempl);
                }
            }
            templateResponse.setChildTemplates(childTemplatesSet);
        }

        if (template.getUserDataId() != null) {
            templateResponse.setUserDataId(template.getUserDataUUid());
            templateResponse.setUserDataName(template.getUserDataName());
            templateResponse.setUserDataParams(template.getUserDataParams());
            templateResponse.setUserDataPolicy(template.getUserDataPolicy());
        }

        templateResponse.setObjectName("template");
        return templateResponse;
    }

    private TemplateResponse initTemplateResponse(TemplateJoinVO template) {
        TemplateResponse templateResponse = new TemplateResponse();
        if (Storage.TemplateType.VNF.equals(template.getTemplateType())) {
            VnfTemplateResponse vnfTemplateResponse = new VnfTemplateResponse();
            List<VnfTemplateNicVO> nics = vnfTemplateNicDao.listByTemplateId(template.getId());
            for (VnfTemplateNicVO nic : nics) {
                vnfTemplateResponse.addVnfNic(new VnfNicResponse(nic.getDeviceId(), nic.getDeviceName(), nic.isRequired(), nic.isManagement(), nic.getDescription()));
            }
            List<VnfTemplateDetailVO> details = vnfTemplateDetailsDao.listDetails(template.getId());
            Collections.sort(details, (v1, v2) -> v1.getName().compareToIgnoreCase(v2.getName()));
            for (VnfTemplateDetailVO detail : details) {
                vnfTemplateResponse.addVnfDetail(detail.getName(), detail.getValue());
            }
            templateResponse = vnfTemplateResponse;
        }
        return templateResponse;
    }

    private void setDeployAsIsDetails(TemplateJoinVO template, TemplateResponse templateResponse) {
        if (template.isDeployAsIs()) {
            List<TemplateDeployAsIsDetailVO> deployAsIsDetails = templateDeployAsIsDetailsDao.listDetails(template.getId());
            for (TemplateDeployAsIsDetailVO deployAsIsDetailVO : deployAsIsDetails) {
                if (deployAsIsDetailVO.getName().startsWith(DeployAsIsConstants.HARDWARE_ITEM_PREFIX)) {
                    //Do not list hardware items
                    continue;
                }
                templateResponse.addDeployAsIsDetail(deployAsIsDetailVO.getName(), deployAsIsDetailVO.getValue());
            }
        }
    }

    //TODO: This is to keep compatibility with 4.1 API, where updateTemplateCmd and updateIsoCmd will return a simpler TemplateResponse
    // compared to listTemplates and listIsos.
    @Override
    public TemplateResponse newUpdateResponse(TemplateJoinVO result) {
        TemplateResponse response = initTemplateResponse(result);
        response.setId(result.getUuid());
        response.setName(result.getName());
        response.setDisplayText(result.getDisplayText());
        response.setPublic(result.isPublicTemplate());
        response.setCreated(result.getCreated());
        response.setFormat(result.getFormat());
        response.setOsTypeId(result.getGuestOSUuid());
        response.setOsTypeName(result.getGuestOSName());
        response.setOsTypeCategoryId(result.getGuestOSCategoryId());
        response.setBootable(result.isBootable());
        response.setHypervisor(result.getHypervisorType().getHypervisorDisplayName());
        response.setDynamicallyScalable(result.isDynamicallyScalable());

        // populate owner.
        ApiResponseHelper.populateOwner(response, result);

        // set details map
        if (result.getDetailName() != null) {
            Map<String, String> details = new HashMap<>();
            details.put(result.getDetailName(), result.getDetailValue());
            response.setDetails(details);
        }

        if (result.getUserDataId() != null) {
            response.setUserDataId(result.getUserDataUUid());
            response.setUserDataName(result.getUserDataName());
            response.setUserDataParams(result.getUserDataParams());
            response.setUserDataPolicy(result.getUserDataPolicy());
        }

        // update tag information
        long tag_id = result.getTagId();
        if (tag_id > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if (vtag != null) {
                response.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }

        response.setObjectName("iso");
        return response;
    }

    @Override
    public TemplateResponse setTemplateResponse(EnumSet<ApiConstants.DomainDetails> detailsView, ResponseView view, TemplateResponse templateResponse, TemplateJoinVO template) {
        if (detailsView.contains(ApiConstants.DomainDetails.all)) {
            // update details map
            String key = template.getDetailName();
            if (key != null) {
                templateResponse.addDetail(key, template.getDetailValue());
            }
        }

        // update tag information
        long tag_id = template.getTagId();
        if (tag_id > 0) {
            addTagInformation(template, templateResponse);
        }

        if (templateResponse.hasAnnotation() == null) {
            templateResponse.setHasAnnotation(annotationDao.hasAnnotations(template.getUuid(), AnnotationService.EntityType.TEMPLATE.name(),
                    _accountService.isRootAdmin(CallContext.current().getCallingAccount().getId())));
        }

        return templateResponse;
    }

    @Override
    public TemplateResponse newIsoResponse(TemplateJoinVO iso, ResponseView view) {

        TemplateResponse isoResponse = new TemplateResponse();
        isoResponse.setId(iso.getUuid());
        isoResponse.setName(iso.getName());
        isoResponse.setDisplayText(iso.getDisplayText());
        isoResponse.setPublic(iso.isPublicTemplate());
        isoResponse.setExtractable(iso.isExtractable() && !(iso.getTemplateType() == TemplateType.PERHOST));
        isoResponse.setCreated(iso.getCreatedOnStore());
        isoResponse.setDynamicallyScalable(iso.isDynamicallyScalable());
        isoResponse.setFormat(iso.getFormat());
        if (iso.getTemplateType() == TemplateType.PERHOST) {
            // for TemplateManager.XS_TOOLS_ISO and TemplateManager.VMWARE_TOOLS_ISO, we didn't download, but is ready to use.
            isoResponse.setReady(true);
        } else {
            isoResponse.setReady(iso.getState() == ObjectInDataStoreStateMachine.State.Ready);
        }
        isoResponse.setBootable(iso.isBootable());
        isoResponse.setFeatured(iso.isFeatured());
        isoResponse.setCrossZones(iso.isCrossZones());
        isoResponse.setPublic(iso.isPublicTemplate());
        isoResponse.setChecksum(DigestHelper.getHashValueFromChecksumValue(iso.getChecksum()));

        isoResponse.setOsTypeId(iso.getGuestOSUuid());
        isoResponse.setOsTypeName(iso.getGuestOSName());
        isoResponse.setOsTypeCategoryId(iso.getGuestOSCategoryId());
        isoResponse.setBits(iso.getBits());
        isoResponse.setPasswordEnabled(iso.isEnablePassword());

        // populate owner.
        ApiResponseHelper.populateOwner(isoResponse, iso);

        Account caller = CallContext.current().getCallingAccount();
        boolean isAdmin = false;
        if ((caller == null) || _accountService.isAdmin(caller.getId())) {
            isAdmin = true;
        }

        // If the user is an admin, add the template download status
        if (isAdmin || caller.getId() == iso.getAccountId()) {
            // add download status
            String templateStatus = getTemplateStatus(iso);
            if (templateStatus != null) {
                isoResponse.setStatus(templateStatus);
            }
            isoResponse.setUrl(iso.getUrl());
            List<TemplateDataStoreVO> isosInStore = _templateStoreDao.listByTemplateNotBypassed(iso.getId());
            List<Map<String, String>> downloadProgressDetails = new ArrayList<>();
            HashMap<String, String> downloadDetailInImageStores = null;
            for (TemplateDataStoreVO isoInStore : isosInStore) {
                downloadDetailInImageStores = new HashMap<>();
                ImageStoreVO imageStore = dataStoreDao.findById(isoInStore.getDataStoreId());
                if (imageStore != null) {
                    downloadDetailInImageStores.put("datastore", imageStore.getName());
                    if (view.equals(ResponseView.Full)) {
                        downloadDetailInImageStores.put("datastoreId", imageStore.getUuid());
                        downloadDetailInImageStores.put("datastoreRole", imageStore.getRole().name());
                    }
                    downloadDetailInImageStores.put("downloadPercent", Integer.toString(isoInStore.getDownloadPercent()));
                    downloadDetailInImageStores.put("downloadState", (isoInStore.getDownloadState() != null ? isoInStore.getDownloadState().toString() : ""));
                    downloadProgressDetails.add(downloadDetailInImageStores);
                }
            }

            List<StoragePoolVO> poolsInZone = primaryDataStoreDao.listByDataCenterId(iso.getDataCenterId());
            List<Long> poolIds = poolsInZone.stream().map(StoragePoolVO::getId).collect(Collectors.toList());
            List<VMTemplateStoragePoolVO> isosInPool = templatePoolDao.listByTemplateId(iso.getId(), poolIds);

            for (VMTemplateStoragePoolVO isoInPool : isosInPool) {
                downloadDetailInImageStores = new HashMap<>();
                StoragePoolVO storagePool = primaryDataStoreDao.findById(isoInPool.getDataStoreId());
                if (storagePool != null) {
                    downloadDetailInImageStores.put("datastore", storagePool.getName());
                    if (view.equals(ResponseView.Full)) {
                        downloadDetailInImageStores.put("datastoreId", storagePool.getUuid());
                        downloadDetailInImageStores.put("datastoreRole", DataStoreRole.Primary.name());
                    }
                    downloadDetailInImageStores.put("downloadPercent", Integer.toString(isoInPool.getDownloadPercent()));
                    downloadDetailInImageStores.put("downloadState", (isoInPool.getDownloadState() != null ? isoInPool.getDownloadState().toString() : ""));
                    downloadProgressDetails.add(downloadDetailInImageStores);
                }
            }
            isoResponse.setDownloadProgress(downloadProgressDetails);
        }

        if (iso.getDataCenterId() > 0) {
            isoResponse.setZoneId(iso.getDataCenterUuid());
            isoResponse.setZoneName(iso.getDataCenterName());
        }

        long isoSize = iso.getSize();
        if (isoSize > 0) {
            isoResponse.setSize(isoSize);
        }
        long isoPhysicalSize = iso.getPhysicalSize();
        if (isoPhysicalSize > 0) {
            isoResponse.setPhysicalSize(isoPhysicalSize);
        }

        if (iso.getUserDataId() != null) {
            isoResponse.setUserDataId(iso.getUserDataUUid());
            isoResponse.setUserDataName(iso.getUserDataName());
            isoResponse.setUserDataParams(iso.getUserDataParams());
            isoResponse.setUserDataPolicy(iso.getUserDataPolicy());
        }

        // update tag information
        long tag_id = iso.getTagId();
        if (tag_id > 0) {
            ResourceTagJoinVO vtag = ApiDBUtils.findResourceTagViewById(tag_id);
            if (vtag != null) {
                isoResponse.addTag(ApiDBUtils.newResourceTagResponse(vtag, false));
            }
        }
        isoResponse.setHasAnnotation(annotationDao.hasAnnotations(iso.getUuid(), AnnotationService.EntityType.ISO.name(),
                _accountService.isRootAdmin(CallContext.current().getCallingAccount().getId())));

        isoResponse.setDirectDownload(iso.isDirectDownload());
        if (iso.getArch() != null) {
            isoResponse.setArch(iso.getArch().getType());
        }

        isoResponse.setObjectName("iso");
        return isoResponse;

    }

    @Override
    public List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate template) {
        SearchCriteria<TemplateJoinVO> sc = tmpltIdSearch.create();
        sc.setParameters("id", template.getId());
        return searchIncludingRemoved(sc, null, null, false);
    }

    @Override
    public List<TemplateJoinVO> newTemplateView(VirtualMachineTemplate template, long zoneId, boolean readyOnly) {
        SearchCriteria<TemplateJoinVO> sc = tmpltZoneSearch.create();
        sc.setParameters("id", template.getId());
        sc.setParameters("dataCenterId", zoneId);
        if (readyOnly) {
            sc.setParameters("state", TemplateState.Ready);
        }
        return searchIncludingRemoved(sc, null, null, false);
    }

    @Override
    public List<TemplateJoinVO> searchByTemplateZonePair(Boolean showRemoved, String... idPairs) {
        Filter searchFilter = new Filter(TemplateJoinVO.class, "sortKey", QueryService.SortKeyAscending.value(), null, null);
        searchFilter.addOrderBy(TemplateJoinVO.class, "tempZonePair", QueryService.SortKeyAscending.value());

        List<TemplateJoinVO> uvList = new ArrayList<TemplateJoinVO>();
        if (idPairs == null || idPairs.length == 0) {
            return uvList;
        }

        for (String idPair : idPairs) {
            SearchCriteria<TemplateJoinVO> sc = buildTemplateZonePairSearchCriteria(idPair);
            List<TemplateJoinVO> rows = searchIncludingRemoved(sc, searchFilter, null, false);
            if (rows != null) {
                uvList.addAll(rows);
            }
        }
        return uvList;
    }

    private SearchCriteria<TemplateJoinVO> buildTemplateZonePairSearchCriteria(String idPair) {
        if (idPair == null || idPair.isEmpty()) {
            throw new IllegalArgumentException("template zone pair id is null or empty");
        }
        // eg "3124_3" → templateId=3124, dcId=3
        String[] parts = idPair.split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException("unexpected template zone pair id format: " + idPair);
        }
        long templateId = Long.parseLong(parts[0]);
        long dcId = Long.parseLong(parts[1]);

        SearchCriteria<TemplateJoinVO> sc;
        if (dcId == 0) {
            sc = tmpltIdPairCrossZoneSearch.create();
            sc.setParameters("template_dc_pair_templateid", templateId);
        } else {
            sc = tmpltIdPairSearch.create();
            sc.setParameters("template_dc_pair_templateid", templateId);
            sc.setParameters("template_dc_pair_dcid", dcId);
        }
        return sc;
    }

    @Override
    public List<TemplateJoinVO> listActiveTemplates(long storeId) {
        SearchCriteria<TemplateJoinVO> sc = activeTmpltSearch.create();
        sc.setParameters("store_id", storeId);
        sc.setParameters("type", TemplateType.USER);
        sc.setParameters("templateState", VirtualMachineTemplate.State.Active);
        return searchIncludingRemoved(sc, null, null, false);
    }

    @Override
    public List<TemplateJoinVO> listPublicTemplates() {
        SearchCriteria<TemplateJoinVO> sc = publicTmpltSearch.create();
        sc.setParameters("public", Boolean.TRUE);
        return listBy(sc);
    }

    @Override
    public Pair<List<TemplateJoinVO>, Integer> searchIncludingRemovedAndCount(final SearchCriteria<TemplateJoinVO> sc, final Filter filter) {
        List<TemplateJoinVO> objects = searchIncludingRemoved(sc, filter, null, false);
        Integer count = getCountIncludingRemoved(sc);
        return new Pair<List<TemplateJoinVO>, Integer>(objects, count);
    }

    // ============================================================================
    // The standard Phase 1 path runs `SELECT DISTINCT temp_zone_pair FROM
    // template_view WHERE ...` against a 13-table view.
    //
    // This bypass path issues hand-tuned SQL against only the 6 tables needed
    // to compute the (template_id, data_center_id) pair: vm_template, account,
    // template_store_ref, image_store, template_zone_ref, data_center. The OR
    // join is replaced with COALESCE.
    //
    // Hard filters (tags, sharedAccountIds, domainPath, featured/community
    // domain hierarchy) are not implemented here — TemplateListFilter#canBypass()
    // returns false in those cases and the dispatcher falls back to the
    // SearchBuilder path.

    private static final Field TEMPLATE_JOIN_ID_FIELD;
    private static final Field TEMPLATE_JOIN_PAIR_FIELD;

    static {
        try {
            TEMPLATE_JOIN_ID_FIELD = findFieldUpHierarchy(TemplateJoinVO.class, "id");
            TEMPLATE_JOIN_PAIR_FIELD = findFieldUpHierarchy(TemplateJoinVO.class, "tempZonePair");
            TEMPLATE_JOIN_ID_FIELD.setAccessible(true);
            TEMPLATE_JOIN_PAIR_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Field findFieldUpHierarchy(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " on " + clazz);
    }

    @Override
    public Pair<List<TemplateJoinVO>, Integer> findDistinctTempZonePairs(TemplateListFilter filter) {
        if (!filter.canBypass()) {
            throw new IllegalArgumentException(
                    "findDistinctTempZonePairs called with unsupported filter (tags / sharedAccountIds / domainPath / domainIds populated). Caller should fall back to searchAndDistinctCount.");
        }

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        appendCommonWhere(where, params, filter);

        String fromClause = buildFromClause(filter);

        String selectExpr = filter.showUnique
                ? "vt.id"
                : "CONCAT(vt.id, '_', IFNULL(dc.id, 0))";

        String dataSql = "SELECT DISTINCT " + selectExpr + " AS distinct_key, vt.sort_key"
                + " FROM " + fromClause
                + where
                + " ORDER BY vt.sort_key " + (filter.sortAscending ? "ASC" : "DESC")
                + ", distinct_key " + (filter.sortAscending ? "ASC" : "DESC")
                + buildLimitClause(filter);

        String countSql = "SELECT COUNT(DISTINCT " + selectExpr + ")"
                + " FROM " + fromClause
                + where;

        List<TemplateJoinVO> rows = executeDistinctQuery(dataSql, params, filter.showUnique);
        int count = executeCountQuery(countSql, params);
        return new Pair<>(rows, count);
    }

    /**
     * Build the FROM clause. The 6-table base is always present; conditional
     * joins are added when the filter actually needs them. Today only
     * `domain` is conditional (used for {@code domainPathLike} predicates);
     * `launch_permission` and `resource_tags` are added in later iterations.
     */
    private String buildFromClause(TemplateListFilter filter) {
        StringBuilder from = new StringBuilder()
                .append("cloud.vm_template vt")
                .append(" JOIN cloud.account a ON a.id = vt.account_id")
                .append(" LEFT JOIN cloud.template_store_ref tsr")
                .append("   ON tsr.template_id = vt.id AND tsr.store_role = 'Image' AND tsr.destroyed = 0")
                .append(" LEFT JOIN cloud.image_store img")
                .append("   ON img.id = tsr.store_id AND img.removed IS NULL")
                .append(" LEFT JOIN cloud.template_zone_ref tzr")
                .append("   ON tzr.template_id = vt.id AND tsr.store_id IS NULL AND tzr.removed IS NULL")
                .append(" LEFT JOIN cloud.data_center dc")
                .append("   ON dc.id = COALESCE(img.data_center_id, tzr.zone_id)");

        if (filter.domainPathLike != null) {
            from.append(" JOIN cloud.domain d ON d.id = a.domain_id");
        }
        return from.toString();
    }

    private void appendCommonWhere(StringBuilder where, List<Object> params, TemplateListFilter filter) {
        if (filter.templateId != null) {
            where.append(" AND vt.id = ?");
            params.add(filter.templateId);
        }

        if (!filter.ids.isEmpty()) {
            where.append(" AND vt.id IN ").append(inClausePlaceholders(filter.ids.size()));
            params.addAll(filter.ids);
        }

        if (filter.keyword != null) {
            where.append(" AND vt.name LIKE ?");
            params.add("%" + filter.keyword + "%");
        } else if (filter.name != null) {
            where.append(" AND vt.name = ?");
            params.add(filter.name);
        }

        if (filter.format != null) {
            // searchForTemplatesInternal: format = 'ISO' for isos, format != 'ISO' otherwise.
            String condition = filter.isIso ? "=" : "!=";
            where.append(" AND vt.format " + condition + " 'ISO'");
        }

        if (filter.hypervisorType != null) {
            where.append(" AND vt.hypervisor_type = ?");
            params.add(filter.hypervisorType.toString());
        }

        if (!filter.availableHypervisors.isEmpty()) {
            where.append(" AND vt.hypervisor_type IN ").append(inClausePlaceholders(filter.availableHypervisors.size()));
            for (HypervisorType h : filter.availableHypervisors) {
                params.add(h.toString());
            }
        }

        if (filter.publicTemplate != null && !filter.publicOrAccountIdComposite) {
            where.append(" AND vt.public = ?");
            params.add(filter.publicTemplate ? 1 : 0);
        }

        if (filter.featured != null) {
            where.append(" AND vt.featured = ?");
            params.add(filter.featured ? 1 : 0);
        }

        if (filter.bootable != null) {
            where.append(" AND vt.bootable = ?");
            params.add(filter.bootable ? 1 : 0);
        }

        if (filter.parentTemplateId != null) {
            where.append(" AND vt.parent_template_id = ?");
            params.add(filter.parentTemplateId);
        }

        if (filter.excludeSystemTemplates) {
            where.append(" AND vt.type != 'SYSTEM'");
        }

        if (filter.accountTypeNeq != null) {
            where.append(" AND a.type != ?");
            params.add(filter.accountTypeNeq.ordinal());
        }
        if (filter.accountTypeEq != null) {
            where.append(" AND a.type = ?");
            params.add(filter.accountTypeEq.ordinal());
        }

        // ACL — accountIds: either pure IN, or composite OR with public=true.
        if (filter.publicOrAccountIdComposite) {
            where.append(" AND (vt.public = 1");
            if (!filter.accountIds.isEmpty()) {
                where.append(" OR vt.account_id IN ").append(inClausePlaceholders(filter.accountIds.size()));
                params.addAll(filter.accountIds);
            }
            where.append(")");
        } else if (filter.publicOrDomainPathComposite) {
            // all + non-admin + SkipProjectResources: (public OR domain.path LIKE)
            where.append(" AND (vt.public = 1");
            if (filter.domainPathLike != null) {
                where.append(" OR d.path LIKE ?");
                params.add(filter.domainPathLike);
            }
            where.append(")");
        } else if (filter.accountIdRequiredForFilter && !filter.accountIds.isEmpty()) {
            where.append(" AND vt.account_id IN ").append(inClausePlaceholders(filter.accountIds.size()));
            params.addAll(filter.accountIds);
        }

        // Standalone domain_path predicate — used when domainPathLike is set
        // outside any composite (self/selfexecutable + DOMAIN_ADMIN scoping).
        if (filter.domainPathLike != null
                && !filter.publicOrDomainPathComposite) {
            where.append(" AND d.path LIKE ?");
            params.add(filter.domainPathLike);
        }

        if (filter.zoneId != null) {
            // mirrors templateChecks(): zone match OR REGION-scoped store OR (ISO + PERHOST)
            where.append(" AND (dc.id = ? OR img.scope = 'REGION' OR (vt.format = 'ISO' AND vt.type = 'PERHOST'))");
            params.add(filter.zoneId);
        }

        if (filter.onlyReady) {
            // mirrors templateChecks(): tsr Ready OR BAREMETAL format OR (ISO + PERHOST)
            where.append(" AND (tsr.state = 'Ready' OR vt.format = 'BAREMETAL' OR (vt.format = 'ISO' AND vt.type = 'PERHOST'))");
        }

        if (!filter.showRemoved) {
            where.append(" AND vt.removed IS NULL");
            if (!filter.templateStates.isEmpty()) {
                where.append(" AND vt.state IN ").append(inClausePlaceholders(filter.templateStates.size()));
                for (VirtualMachineTemplate.State s : filter.templateStates) {
                    params.add(s.toString());
                }
            }
        }
    }

    private String buildLimitClause(TemplateListFilter filter) {
        if (filter.pageSize == null) {
            return "";
        }
        long start = filter.startIndex == null ? 0L : filter.startIndex;
        return " LIMIT " + start + ", " + filter.pageSize;
    }

    private static String inClausePlaceholders(int n) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        sb.append(')');
        return sb.toString();
    }

    private List<TemplateJoinVO> executeDistinctQuery(String sql, List<Object> params, boolean showUnique) {
        List<TemplateJoinVO> out = new ArrayList<>();
        try (TransactionLegacy txn = TransactionLegacy.open("TemplateJoinDao");
             PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TemplateJoinVO vo = new TemplateJoinVO();
                    if (showUnique) {
                        TEMPLATE_JOIN_ID_FIELD.setLong(vo, rs.getLong(1));
                    } else {
                        TEMPLATE_JOIN_PAIR_FIELD.set(vo, rs.getString(1));
                    }
                    out.add(vo);
                }
            }
        } catch (SQLException | IllegalAccessException e) {
            throw new CloudRuntimeException("findDistinctTempZonePairs data query failed: " + sql, e);
        }
        return out;
    }

    private int executeCountQuery(String sql, List<Object> params) {
        try (TransactionLegacy txn = TransactionLegacy.open("TemplateJoinDao");
             PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("findDistinctTempZonePairs count query failed: " + sql, e);
        }
    }

    private static void bindParams(PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    @Override
    public List<TemplateJoinVO> findByDistinctIds(Long... ids) {
        if (ids == null || ids.length == 0) {
            return new ArrayList<TemplateJoinVO>();
        }

        Filter searchFilter = new Filter(TemplateJoinVO.class, "sortKey", QueryService.SortKeyAscending.value(), null, null);
        searchFilter.addOrderBy(TemplateJoinVO.class, "tempZonePair", true);

        SearchCriteria<TemplateJoinVO> sc = tmpltIdsSearch.create();
        sc.setParameters("idsIN", ids);
        return searchIncludingRemoved(sc, searchFilter, null, false);
    }
}
