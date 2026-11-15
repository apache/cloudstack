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

package com.cloud.kubernetes.version;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.kubernetes.version.AddKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.DeleteKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.GetUploadParamsForKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.UpdateKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.GetUploadParamsForIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.kubernetes.version.ListKubernetesSupportedVersionsCmd;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.CheckFileInIsoAnswer;
import org.apache.cloudstack.storage.command.CheckFileInIsoCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.cpu.CPU;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;

public class KubernetesVersionManagerImpl extends ManagerBase implements KubernetesVersionService {

    @Inject
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Inject
    private KubernetesClusterDao kubernetesClusterDao;
    @Inject
    private AccountManager accountManager;
    @Inject
    private VMTemplateDao templateDao;
    @Inject
    private TemplateJoinDao templateJoinDao;
    @Inject
    private VMTemplateZoneDao templateZoneDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ImageStoreDao imageStoreDao;
    @Inject
    private TemplateApiService templateService;
    @Inject
    private VMTemplateDetailsDao templateDetailsDao;
    @Inject
    private TemplateDataStoreDao templateDataStoreDao;
    @Inject
    private DataStoreManager dataStoreManager;
    @Inject
    private EndPointSelector endPointSelector;

    protected static final int ETCD_IN_ISO_CHECK_INTERVAL_SECONDS = 300;
    protected static final int MAX_FAILED_BACKGROUND_ETCD_IN_ISO_CHECKS = 3;

    private ScheduledExecutorService etcdInIsoChecker;
    // ISO ID -> number of background checks that could not be run for it
    private final Map<Long, Integer> failedEtcdInIsoChecks = new ConcurrentHashMap<>();

    public static final String MINIMUN_AUTOSCALER_SUPPORTED_VERSION = "1.15.0";

    protected void updateTemplateDetailsInKubernetesSupportedVersionResponse(
            final KubernetesSupportedVersion kubernetesSupportedVersion, KubernetesSupportedVersionResponse response, boolean isRootAdmin) {
        TemplateJoinVO template = templateJoinDao.findById(kubernetesSupportedVersion.getIsoId());
        if (template == null) {
            return;
        }
        response.setIsoId(template.getUuid());
        response.setIsoName(template.getName());
        if (template.getState() != null) {
            response.setIsoState(template.getState().toString());
        }
        if (isRootAdmin) {
            response.setIsoUrl(template.getUrl());
        }
        response.setIsoArch(template.getArch().getType());
        response.setDirectDownload(template.isDirectDownload());
    }

    private KubernetesSupportedVersionResponse createKubernetesSupportedVersionResponse(final KubernetesSupportedVersion kubernetesSupportedVersion, boolean isRootAdmin) {
        KubernetesSupportedVersionResponse response = new KubernetesSupportedVersionResponse();
        response.setObjectName("kubernetessupportedversion");
        response.setId(kubernetesSupportedVersion.getUuid());
        response.setName(kubernetesSupportedVersion.getName());
        response.setSemanticVersion(kubernetesSupportedVersion.getSemanticVersion());
        if (kubernetesSupportedVersion.getState() != null) {
            response.setState(kubernetesSupportedVersion.getState().toString());
        }
        response.setMinimumCpu(kubernetesSupportedVersion.getMinimumCpu());
        response.setMinimumRamSize(kubernetesSupportedVersion.getMinimumRamSize());
        DataCenterVO zone = dataCenterDao.findById(kubernetesSupportedVersion.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        response.setSupportsHA(compareSemanticVersions(kubernetesSupportedVersion.getSemanticVersion(),
            KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT)>=0);
        response.setSupportsAutoscaling(versionSupportsAutoscaling(kubernetesSupportedVersion));
        response.setSupportsEtcd(getStoredEtcdIncludedInIso(kubernetesSupportedVersion.getIsoId()));
        updateTemplateDetailsInKubernetesSupportedVersionResponse(kubernetesSupportedVersion, response, isRootAdmin);
        response.setCreated(kubernetesSupportedVersion.getCreated());
        return response;
    }

    private ListResponse<KubernetesSupportedVersionResponse> createKubernetesSupportedVersionListResponse(
            List<KubernetesSupportedVersionVO> versions, Integer count) {
        List<KubernetesSupportedVersionResponse> responseList = new ArrayList<>();
        Account caller = CallContext.current().getCallingAccount();
        boolean isRootAdmin = accountManager.isRootAdmin(caller.getId());

        for (KubernetesSupportedVersionVO version : versions) {
            responseList.add(createKubernetesSupportedVersionResponse(version, isRootAdmin));
        }
        ListResponse<KubernetesSupportedVersionResponse> response = new ListResponse<>();
        response.setResponses(responseList, count);
        return response;
    }

    private static boolean isSemanticVersion(final String version) {
        if(!version.matches("[0-9]+(\\.[0-9]+)*")) {
            return false;
        }
        String[] parts = version.split("\\.");
        if (parts.length < 3) {
            return false;
        }
        return true;
    }

    private List <KubernetesSupportedVersionVO> filterKubernetesSupportedVersions(List <KubernetesSupportedVersionVO> versions, final String minimumSemanticVersion) {
        if (StringUtils.isNotEmpty(minimumSemanticVersion)) {
            for (int i = versions.size() - 1; i >= 0; --i) {
                KubernetesSupportedVersionVO version = versions.get(i);
                try {
                    if (compareSemanticVersions(minimumSemanticVersion, version.getSemanticVersion()) > 0) {
                        versions.remove(i);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Unable to compare Kubernetes version for supported version {} with {}", version, minimumSemanticVersion);
                    versions.remove(i);
                }
            }
        }
        return versions;
    }

    private GetUploadParamsResponse registerKubernetesVersionIsoForUpload(final Long zoneId, final String versionName, final String isoChecksum) {
        CallContext.register(CallContext.current(), ApiCommandResourceType.Iso);
        String isoName = String.format("%s-Kubernetes-Binaries-ISO", versionName);
        GetUploadParamsForIsoCmd uploadIso = new GetUploadParamsForIsoCmd();
        uploadIso = ComponentContext.inject(uploadIso);
        uploadIso.setName(isoName);
        uploadIso.setPublicIso(true);
        if (zoneId != null) {
            uploadIso.setZoneId(zoneId);
        }
        uploadIso.setDisplayText(isoName);
        uploadIso.setBootable(false);
        if (StringUtils.isNotEmpty(isoChecksum)) {
            uploadIso.setChecksum(isoChecksum);
        }
        uploadIso.setAccountName(accountManager.getSystemAccount().getAccountName());
        uploadIso.setDomainId(accountManager.getSystemAccount().getDomainId());
        try {
            return templateService.registerIsoForPostUpload(uploadIso);
        } catch (MalformedURLException | ResourceAllocationException e) {
            logger.error(String.format("Unable to register binaries ISO for supported kubernetes version, %s", versionName), e);
            throw new CloudRuntimeException(String.format("Unable to register binaries ISO for supported kubernetes version, %s", versionName), e);
        } finally {
            CallContext.unregister();
        }
    }

    private VirtualMachineTemplate registerKubernetesVersionIso(final Long zoneId, final String versionName, final String isoUrl, final String isoChecksum, final boolean directDownload, CPU.CPUArch arch) throws IllegalAccessException, NoSuchFieldException,
            IllegalArgumentException, ResourceAllocationException {
        CallContext.register(CallContext.current(), ApiCommandResourceType.Iso);
        String isoName = String.format("%s-Kubernetes-Binaries-ISO", versionName);
        RegisterIsoCmd registerIsoCmd = new RegisterIsoCmd();
        registerIsoCmd = ComponentContext.inject(registerIsoCmd);
        registerIsoCmd.setIsoName(isoName);
        registerIsoCmd.setPublic(true);
        if (zoneId != null) {
            registerIsoCmd.setZoneId(zoneId);
        }
        registerIsoCmd.setDisplayText(isoName);
        registerIsoCmd.setBootable(false);
        registerIsoCmd.setUrl(isoUrl);
        if (StringUtils.isNotEmpty(isoChecksum)) {
            registerIsoCmd.setChecksum(isoChecksum);
        }
        registerIsoCmd.setDirectDownload(directDownload);
        registerIsoCmd.setArch(arch.getType());
        registerIsoCmd.setAccountName(accountManager.getSystemAccount().getAccountName());
        registerIsoCmd.setDomainId(accountManager.getSystemAccount().getDomainId());
        try {
            return templateService.registerIso(registerIsoCmd);
        } finally {
            CallContext.unregister();
        }
    }

    private void deleteKubernetesVersionIso(long templateId) throws IllegalAccessException, NoSuchFieldException,
            IllegalArgumentException {
        CallContext isoContext = CallContext.register(CallContext.current(), ApiCommandResourceType.Iso);
        isoContext.setEventResourceId(templateId);
        DeleteIsoCmd deleteIsoCmd = new DeleteIsoCmd();
        deleteIsoCmd = ComponentContext.inject(deleteIsoCmd);
        deleteIsoCmd.setId(templateId);
        try {
            templateService.deleteIso(deleteIsoCmd);
        } finally {
            CallContext.unregister();
        }
    }

    public static int compareSemanticVersions(String v1, String v2) throws IllegalArgumentException {
        if (StringUtils.isAnyEmpty(v1, v2)) {
            throw new IllegalArgumentException(String.format("Invalid version comparision with versions %s, %s", v1, v2));
        }
        if(!isSemanticVersion(v1)) {
            throw new IllegalArgumentException(String.format("Invalid version format, %s. Semantic version should be specified in MAJOR.MINOR.PATCH format", v1));
        }
        if(!isSemanticVersion(v2)) {
            throw new IllegalArgumentException(String.format("Invalid version format, %s. Semantic version should be specified in MAJOR.MINOR.PATCH format", v2));
        }
        String[] thisParts = v1.split("\\.");
        String[] thatParts = v2.split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    public static boolean versionSupportsAutoscaling(KubernetesSupportedVersion clusterVersion) {
        return clusterVersion.getSemanticVersion().compareTo(MINIMUN_AUTOSCALER_SUPPORTED_VERSION) >= 0;
    }

    /**
     * Returns a boolean value whether Kubernetes cluster upgrade can be carried from a given currentVersion to upgradeVersion
     * Kubernetes clusters can only be upgraded from one MINOR version to the next MINOR version, or between PATCH versions of the same MINOR.
     * That is, MINOR versions cannot be skipped during upgrade.
     * For example, you can upgrade from 1.y to 1.y+1, but not from 1.y to 1.y+2
     * @param currentVersion
     * @param upgradeVersion
     * @return
     * @throws IllegalArgumentException
     */
    public static boolean canUpgradeKubernetesVersion(final String currentVersion, final String upgradeVersion) throws IllegalArgumentException {
        int versionDiff = compareSemanticVersions(upgradeVersion, currentVersion);
        if (versionDiff < 0) {
            throw new IllegalArgumentException(String.format("Kubernetes clusters can not be downgraded, current version: %s, upgrade version: %s", currentVersion, upgradeVersion));
        }
        String[] thisParts = currentVersion.split("\\.");
        String[] thatParts = upgradeVersion.split("\\.");
        int majorVerDiff = Integer.parseInt(thatParts[0]) - Integer.parseInt(thisParts[0]);
        int minorVerDiff = Integer.parseInt(thatParts[1]) - Integer.parseInt(thisParts[1]);

        if (majorVerDiff != 0 || minorVerDiff > 1) {
            throw new IllegalArgumentException(String.format("Kubernetes clusters can be upgraded between next minor or patch version releases, current version: %s, upgrade version: %s", currentVersion, upgradeVersion));
        }
        return true;
    }

    @Override
    public ListResponse<KubernetesSupportedVersionResponse> listKubernetesSupportedVersions(final ListKubernetesSupportedVersionsCmd cmd) {
        if (!KubernetesClusterService.KubernetesServiceEnabled.value()) {
            throw new CloudRuntimeException("Kubernetes Service plugin is disabled");
        }
        final Long versionId = cmd.getId();
        final Long zoneId = cmd.getZoneId();
        String minimumSemanticVersion = cmd.getMinimumSemanticVersion();
        final Long minimumKubernetesVersionId = cmd.getMinimumKubernetesVersionId();
        final String arch = cmd.getArch();
        if (StringUtils.isNotEmpty(minimumSemanticVersion) && minimumKubernetesVersionId != null) {
            throw new CloudRuntimeException(String.format("Both parameters %s and %s can not be passed together", ApiConstants.MIN_SEMANTIC_VERSION, ApiConstants.MIN_KUBERNETES_VERSION_ID));
        }
        if (minimumKubernetesVersionId != null) {
            KubernetesSupportedVersionVO minVersion = kubernetesSupportedVersionDao.findById(minimumKubernetesVersionId);
            if (minVersion == null) {
                throw new InvalidParameterValueException(String.format("Invalid %s passed", ApiConstants.MIN_KUBERNETES_VERSION_ID));
            }
            minimumSemanticVersion = minVersion.getSemanticVersion();
        }
        Filter searchFilter = new Filter(KubernetesSupportedVersionVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<KubernetesSupportedVersionVO> sb = kubernetesSupportedVersionDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        if (StringUtils.isNotBlank(arch)) {
            SearchBuilder<VMTemplateVO> isoSearch = templateDao.createSearchBuilder();
            isoSearch.and("arch", isoSearch.entity().getArch(), SearchCriteria.Op.EQ);
            sb.join("isoSearch", isoSearch, isoSearch.entity().getId(), sb.entity().getIsoId(), JoinBuilder.JoinType.INNER);
            isoSearch.done();
        }
        sb.done();
        SearchCriteria<KubernetesSupportedVersionVO> sc = sb.create();
        String keyword = cmd.getKeyword();
        if (versionId != null) {
            sc.setParameters("id", versionId);
        }
        if (zoneId != null) {
            SearchCriteria<KubernetesSupportedVersionVO> scc = kubernetesSupportedVersionDao.createSearchCriteria();
            scc.addOr("zoneId", SearchCriteria.Op.EQ, zoneId);
            scc.addOr("zoneId", SearchCriteria.Op.NULL);
            sc.addAnd("zoneId", SearchCriteria.Op.SC, scc);
        }
        if(keyword != null){
            sc.setParameters("keyword", "%" + keyword + "%");
        }
        if (StringUtils.isNotBlank(arch)) {
            sc.setJoinParameters("isoSearch", "arch", arch);
        }
        Pair<List<KubernetesSupportedVersionVO>, Integer> versionsAndCount =
                kubernetesSupportedVersionDao.searchAndCount(sc, searchFilter);
        List<KubernetesSupportedVersionVO> versions =
                filterKubernetesSupportedVersions(versionsAndCount.first(), minimumSemanticVersion);

        return createKubernetesSupportedVersionListResponse(versions, versionsAndCount.second());
    }

    private void validateImageStoreForZone(Long zoneId, boolean directDownload) {
        if (directDownload) {
            return;
        }
        if (zoneId != null) {
            List<ImageStoreVO> imageStores = imageStoreDao.listStoresByZoneId(zoneId);
            if (CollectionUtils.isEmpty(imageStores)) {
                DataCenterVO zone = dataCenterDao.findById(zoneId);
                String zoneName = zone != null ? zone.getName() : String.valueOf(zoneId);
                throw new InvalidParameterValueException(String.format("Unable to register Kubernetes version ISO. No image store available in zone: %s", zoneName));
            }
        } else {
            List<DataCenterVO> zones = dataCenterDao.listAllZones();
            List<String> zonesWithoutStorage = new ArrayList<>();
            for (DataCenterVO zone : zones) {
                List<ImageStoreVO> imageStores = imageStoreDao.listStoresByZoneId(zone.getId());
                if (CollectionUtils.isEmpty(imageStores)) {
                    zonesWithoutStorage.add(zone.getName());
                }
            }
            if (!zonesWithoutStorage.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Unable to register Kubernetes version ISO for all zones. The following zones have no image store: %s", String.join(", ", zonesWithoutStorage)));
            }
        }
    }

    private void validateKubernetesSupportedVersion(Long zoneId, String semanticVersion, Integer minimumCpu,
                                                    Integer minimumRamSize, boolean isDirectDownload) {
        if (minimumCpu == null || minimumCpu < KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_CPU) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s parameter. Minimum %d vCPUs required.", ApiConstants.MIN_CPU_NUMBER, KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_CPU));
        }
        if (minimumRamSize == null || minimumRamSize < KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s parameter. Minimum %dMB memory required", ApiConstants.MIN_MEMORY, KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE));
        }
        if (compareSemanticVersions(semanticVersion, MIN_KUBERNETES_VERSION) < 0) {
            throw new InvalidParameterValueException(String.format("New supported Kubernetes version cannot be added as %s is minimum version supported by Kubernetes Service", MIN_KUBERNETES_VERSION));
        }
        if (zoneId != null) {
            DataCenter zone = dataCenterDao.findById(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Invalid zone specified");
            }
            if (DataCenter.Type.Edge.equals(zone.getType()) && !isDirectDownload) {
                throw new InvalidParameterValueException(String.format("Zone: %s supports only direct download Kubernetes versions", zone.getName()));
            }
        }
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_ADD,
            eventDescription = "Adding Kubernetes supported version")
    public KubernetesSupportedVersionResponse addKubernetesSupportedVersion(final AddKubernetesSupportedVersionCmd cmd) {
        if (!KubernetesClusterService.KubernetesServiceEnabled.value()) {
            throw new CloudRuntimeException("Kubernetes Service plugin is disabled");
        }
        String name = cmd.getName();
        final String semanticVersion = cmd.getSemanticVersion();
        final Long zoneId = cmd.getZoneId();
        final String isoUrl = cmd.getUrl();
        final String isoChecksum = cmd.getChecksum();
        final Integer minimumCpu = cmd.getMinimumCpu();
        final Integer minimumRamSize = cmd.getMinimumRamSize();
        final boolean isDirectDownload = cmd.isDirectDownload();
        CPU.CPUArch arch = cmd.getArch();

        validateKubernetesSupportedVersion(zoneId, semanticVersion, minimumCpu, minimumRamSize, isDirectDownload);

        if (StringUtils.isEmpty(isoUrl)) {
            throw new InvalidParameterValueException(String.format("Invalid URL for ISO specified, %s", isoUrl));
        }
        if (StringUtils.isEmpty(name)) {
            name = String.format("v%s", semanticVersion);
            if (zoneId != null) {
                name = String.format("%s-%s", name, dataCenterDao.findById(zoneId).getName());
            }
        }

        validateImageStoreForZone(zoneId, isDirectDownload);

        VMTemplateVO template = null;
        try {
            VirtualMachineTemplate vmTemplate = registerKubernetesVersionIso(zoneId, name, isoUrl, isoChecksum, isDirectDownload, arch);
            template = templateDao.findById(vmTemplate.getId());
        } catch (IllegalAccessException | NoSuchFieldException | IllegalArgumentException | ResourceAllocationException ex) {
            logger.error(String.format("Unable to register binaries ISO for supported kubernetes version, %s, with url: %s", name, isoUrl), ex);
            throw new CloudRuntimeException(String.format("Unable to register binaries ISO for supported kubernetes version, %s, with url: %s", name, isoUrl));
        }

        KubernetesSupportedVersionVO supportedVersionVO = new KubernetesSupportedVersionVO(name, semanticVersion, template.getId(), zoneId, minimumCpu, minimumRamSize);
        supportedVersionVO = kubernetesSupportedVersionDao.persist(supportedVersionVO);
        CallContext.current().putContextParameter(KubernetesSupportedVersion.class, supportedVersionVO.getUuid());

        return createKubernetesSupportedVersionResponse(supportedVersionVO, true);
    }

    @Override
    public GetUploadParamsResponse registerKubernetesSupportedVersionForPostUpload(GetUploadParamsForKubernetesSupportedVersionCmd cmd) {
        if (!KubernetesClusterService.KubernetesServiceEnabled.value()) {
            throw new CloudRuntimeException("Kubernetes Service plugin is disabled");
        }
        String name = cmd.getName();
        final String semanticVersion = cmd.getSemanticVersion();
        final Long zoneId = cmd.getZoneId();
        final String isoChecksum = cmd.getChecksum();
        final Integer minimumCpu = cmd.getMinimumCpu();
        final Integer minimumRamSize = cmd.getMinimumRamSize();

        validateKubernetesSupportedVersion(zoneId, semanticVersion, minimumCpu, minimumRamSize, false);

        GetUploadParamsResponse response = registerKubernetesVersionIsoForUpload(zoneId, name, isoChecksum);

        VMTemplateVO template = templateDao.findByUuid(response.getId().toString());
        KubernetesSupportedVersionVO supportedVersionVO = new KubernetesSupportedVersionVO(name, semanticVersion, template.getId(), zoneId, minimumCpu, minimumRamSize);
        supportedVersionVO = kubernetesSupportedVersionDao.persist(supportedVersionVO);
        CallContext.current().putContextParameter(KubernetesSupportedVersion.class, supportedVersionVO.getUuid());

        return response;
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_DELETE,
            eventDescription = "deleting Kubernetes supported version", async = true)
    public boolean deleteKubernetesSupportedVersion(final DeleteKubernetesSupportedVersionCmd cmd) {
        if (!KubernetesClusterService.KubernetesServiceEnabled.value()) {
            throw new CloudRuntimeException("Kubernetes Service plugin is disabled");
        }
        final Long versionId = cmd.getId();
        KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(versionId);
        if (version == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes version id specified");
        }
        List<KubernetesClusterVO> clusters = kubernetesClusterDao.listAllByKubernetesVersion(versionId);
        if (clusters.size() > 0) {
            throw new CloudRuntimeException(String.format("Unable to delete Kubernetes version ID: %s. Existing clusters currently using the version.", version.getUuid()));
        }

        VMTemplateVO template = templateDao.findByIdIncludingRemoved(version.getIsoId());
        if (template == null) {
            logger.warn("Unable to find ISO associated with supported Kubernetes version {}", version);
        }
        if (template != null && template.getRemoved() == null) { // Delete ISO
            try {
                deleteKubernetesVersionIso(template.getId());
            } catch (IllegalAccessException | NoSuchFieldException | IllegalArgumentException ex) {
                logger.error("Unable to delete binaries ISO: {} associated with supported kubernetes version: {}", template, version, ex);
                throw new CloudRuntimeException(String.format("Unable to delete binaries ISO ID: %s associated with supported kubernetes version ID: %s", template.getUuid(), version.getUuid()));
            }
        }
        return kubernetesSupportedVersionDao.remove(version.getId());
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_UPDATE,
            eventDescription = "Updating Kubernetes supported version")
    public KubernetesSupportedVersionResponse updateKubernetesSupportedVersion(final UpdateKubernetesSupportedVersionCmd cmd) {
        if (!KubernetesClusterService.KubernetesServiceEnabled.value()) {
            throw new CloudRuntimeException("Kubernetes Service plugin is disabled");
        }
        final Long versionId = cmd.getId();
        KubernetesSupportedVersion.State state = null;
        KubernetesSupportedVersionVO version = kubernetesSupportedVersionDao.findById(versionId);
        if (version == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes version id specified");
        }
        try {
            state = KubernetesSupportedVersion.State.valueOf(cmd.getState());
        } catch (IllegalArgumentException iae) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s parameter", ApiConstants.STATE));
        }
        if (!state.equals(version.getState())) {
            version = kubernetesSupportedVersionDao.createForUpdate(version.getId());
            version.setState(state);
            if (!kubernetesSupportedVersionDao.update(version.getId(), version)) {
                throw new CloudRuntimeException(String.format("Failed to update Kubernetes supported version ID: %s", version.getUuid()));
            }
            version = kubernetesSupportedVersionDao.findById(versionId);
        }
        return  createKubernetesSupportedVersionResponse(version, true);
    }

    /**
     * @return what an earlier check found for the ISO, or null if the ISO has not been checked yet
     */
    protected Boolean getStoredEtcdIncludedInIso(long isoId) {
        VMTemplateDetailVO detail = templateDetailsDao.findDetail(isoId, ETCD_INCLUDED_ISO_DETAIL);
        if (detail == null || StringUtils.isBlank(detail.getValue())) {
            return null;
        }
        return Boolean.parseBoolean(detail.getValue());
    }

    /**
     * Asks the secondary storage VM whether the ISO has the etcd binaries and stores the answer as a detail of
     * the ISO, so the ISO is read only once.
     *
     * @param zoneId the zone whose copy of the ISO is read, or null to read any copy that is ready
     * @return null if the check could not be run, in which case nothing is stored
     */
    protected Boolean checkAndStoreEtcdIncludedInIso(long isoId, Long zoneId) {
        VMTemplateVO iso = templateDao.findById(isoId);
        if (iso == null || iso.isDirectDownload()) {
            // A direct download ISO is not kept on an image store, so there is nothing the secondary storage VM can read
            return null;
        }
        TemplateDataStoreVO isoOnStore = zoneId == null ? templateDataStoreDao.findReadyByTemplate(isoId, DataStoreRole.Image) :
                templateDataStoreDao.findByTemplateZoneReady(isoId, zoneId);
        if (isoOnStore == null || StringUtils.isBlank(isoOnStore.getInstallPath())) {
            logger.debug("Kubernetes ISO {} is not ready on an image store, unable to check it for the etcd binaries", iso);
            return null;
        }
        DataStore store = dataStoreManager.getDataStore(isoOnStore.getDataStoreId(), DataStoreRole.Image);
        EndPoint endPoint = store == null ? null : endPointSelector.select(store);
        if (endPoint == null) {
            logger.debug("No secondary storage VM available to check Kubernetes ISO {} for the etcd binaries", iso);
            return null;
        }
        CheckFileInIsoCommand cmd = new CheckFileInIsoCommand(store.getTO(), isoOnStore.getInstallPath(), ETCD_ARCHIVE_PATH_IN_ISO);
        Answer answer = endPoint.sendMessage(cmd);
        if (!(answer instanceof CheckFileInIsoAnswer) || !answer.getResult()) {
            logger.warn("Unable to check Kubernetes ISO {} for the etcd binaries: {}", iso, answer == null ? "no answer" : answer.getDetails());
            return null;
        }
        boolean etcdIncluded = ((CheckFileInIsoAnswer)answer).isFilePresent();
        templateDetailsDao.addDetail(isoId, ETCD_INCLUDED_ISO_DETAIL, String.valueOf(etcdIncluded), true);
        logger.info("Kubernetes ISO {} {} the etcd binaries needed for dedicated etcd nodes", iso, etcdIncluded ? "includes" : "does not include");
        return etcdIncluded;
    }

    @Override
    public Boolean isEtcdIncludedInIso(KubernetesSupportedVersion kubernetesSupportedVersion, long zoneId) {
        long isoId = kubernetesSupportedVersion.getIsoId();
        Boolean etcdIncluded = getStoredEtcdIncludedInIso(isoId);
        if (etcdIncluded != null) {
            return etcdIncluded;
        }
        try {
            return checkAndStoreEtcdIncludedInIso(isoId, zoneId);
        } catch (CloudRuntimeException e) {
            logger.warn("Unable to check the ISO of Kubernetes version {} for the etcd binaries", kubernetesSupportedVersion, e);
            return null;
        }
    }

    /**
     * Checks the Kubernetes ISOs that are ready but have not been checked for the etcd binaries yet. This covers
     * both newly registered ISOs and the ones registered before this check existed.
     */
    protected void checkEtcdIncludedInKubernetesIsos() {
        Set<Long> isoIds = new HashSet<>();
        for (KubernetesSupportedVersionVO version : kubernetesSupportedVersionDao.listAll()) {
            long isoId = version.getIsoId();
            if (!isoIds.add(isoId)) {
                continue;
            }
            if (getStoredEtcdIncludedInIso(isoId) != null) {
                failedEtcdInIsoChecks.remove(isoId);
                continue;
            }
            if (failedEtcdInIsoChecks.getOrDefault(isoId, 0) >= MAX_FAILED_BACKGROUND_ETCD_IN_ISO_CHECKS) {
                // Stop trying in the background, the ISO is still checked when a cluster with etcd nodes is requested
                continue;
            }
            VMTemplateVO iso = templateDao.findById(isoId);
            if (iso == null || iso.isDirectDownload() || templateDataStoreDao.findReadyByTemplate(isoId, DataStoreRole.Image) == null) {
                continue;
            }
            try {
                if (checkAndStoreEtcdIncludedInIso(isoId, null) == null) {
                    failedEtcdInIsoChecks.merge(isoId, 1, Integer::sum);
                }
            } catch (CloudRuntimeException e) {
                logger.warn("Unable to check the ISO of Kubernetes version {} for the etcd binaries", version, e);
                failedEtcdInIsoChecks.merge(isoId, 1, Integer::sum);
            }
        }
        failedEtcdInIsoChecks.keySet().retainAll(isoIds);
    }

    public class EtcdInKubernetesIsoChecker extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock lock = GlobalLock.getInternLock("KubernetesVersion.EtcdInIsoCheck.Lock");
            try {
                if (lock.lock(3)) {
                    try {
                        checkEtcdIncludedInKubernetesIsos();
                    } catch (Exception e) {
                        logger.warn("Failed to check the Kubernetes ISOs for the etcd binaries", e);
                    } finally {
                        lock.unlock();
                    }
                }
            } finally {
                lock.releaseRef();
            }
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        etcdInIsoChecker = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Kubernetes-ISO-Etcd-Checker"));
        return true;
    }

    @Override
    public boolean start() {
        if (KubernetesClusterService.KubernetesServiceEnabled.value()) {
            etcdInIsoChecker.scheduleWithFixedDelay(new EtcdInKubernetesIsoChecker(), ETCD_IN_ISO_CHECK_INTERVAL_SECONDS,
                    ETCD_IN_ISO_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (etcdInIsoChecker != null) {
            etcdInIsoChecker.shutdownNow();
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        if (!KubernetesClusterService.KubernetesServiceEnabled.value()) {
            return cmdList;
        }
        cmdList.add(AddKubernetesSupportedVersionCmd.class);
        cmdList.add(GetUploadParamsForKubernetesSupportedVersionCmd.class);
        cmdList.add(ListKubernetesSupportedVersionsCmd.class);
        cmdList.add(DeleteKubernetesSupportedVersionCmd.class);
        cmdList.add(UpdateKubernetesSupportedVersionCmd.class);
        return cmdList;
    }
}
