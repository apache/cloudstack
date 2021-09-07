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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.kubernetes.version.AddKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.DeleteKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.UpdateKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.kubernetes.version.ListKubernetesSupportedVersionsCmd;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.kubernetes.cluster.KubernetesClusterService;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;

public class KubernetesVersionManagerImpl extends ManagerBase implements KubernetesVersionService {
    public static final Logger LOGGER = Logger.getLogger(KubernetesVersionManagerImpl.class.getName());

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
    private TemplateApiService templateService;

    private KubernetesSupportedVersionResponse createKubernetesSupportedVersionResponse(final KubernetesSupportedVersion kubernetesSupportedVersion) {
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
        if (compareSemanticVersions(kubernetesSupportedVersion.getSemanticVersion(),
                KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT)>=0) {
            response.setSupportsHA(true);
        } else {
            response.setSupportsHA(false);
        }
        TemplateJoinVO template = templateJoinDao.findById(kubernetesSupportedVersion.getIsoId());
        if (template != null) {
            response.setIsoId(template.getUuid());
            response.setIsoName(template.getName());
            response.setIsoState(template.getState().toString());
        }
        return response;
    }

    private ListResponse<KubernetesSupportedVersionResponse> createKubernetesSupportedVersionListResponse(List<KubernetesSupportedVersionVO> versions) {
        List<KubernetesSupportedVersionResponse> responseList = new ArrayList<>();
        for (KubernetesSupportedVersionVO version : versions) {
            responseList.add(createKubernetesSupportedVersionResponse(version));
        }
        ListResponse<KubernetesSupportedVersionResponse> response = new ListResponse<>();
        response.setResponses(responseList);
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
        if (!Strings.isNullOrEmpty(minimumSemanticVersion)) {
            for (int i = versions.size() - 1; i >= 0; --i) {
                KubernetesSupportedVersionVO version = versions.get(i);
                try {
                    if (compareSemanticVersions(minimumSemanticVersion, version.getSemanticVersion()) > 0) {
                        versions.remove(i);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn(String.format("Unable to compare Kubernetes version for supported version ID: %s with %s", version.getUuid(), minimumSemanticVersion));
                    versions.remove(i);
                }
            }
        }
        return versions;
    }

    private VirtualMachineTemplate registerKubernetesVersionIso(final Long zoneId, final String versionName, final String isoUrl, final String isoChecksum)throws IllegalAccessException, NoSuchFieldException,
            IllegalArgumentException, ResourceAllocationException {
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
        if (!Strings.isNullOrEmpty(isoChecksum)) {
            registerIsoCmd.setChecksum(isoChecksum);
        }
        registerIsoCmd.setAccountName(accountManager.getSystemAccount().getAccountName());
        registerIsoCmd.setDomainId(accountManager.getSystemAccount().getDomainId());
        return templateService.registerIso(registerIsoCmd);
    }

    private void deleteKubernetesVersionIso(long templateId) throws IllegalAccessException, NoSuchFieldException,
            IllegalArgumentException {
        DeleteIsoCmd deleteIsoCmd = new DeleteIsoCmd();
        deleteIsoCmd = ComponentContext.inject(deleteIsoCmd);
        deleteIsoCmd.setId(templateId);
        templateService.deleteIso(deleteIsoCmd);
    }

    public static int compareSemanticVersions(String v1, String v2) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(v1) || Strings.isNullOrEmpty(v2)) {
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
        if (versionDiff == 0) {
            throw new IllegalArgumentException(String.format("Kubernetes clusters can not be upgraded, current version: %s, upgrade version: %s", currentVersion, upgradeVersion));
        } else if (versionDiff < 0) {
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
        if (!Strings.isNullOrEmpty(minimumSemanticVersion) && minimumKubernetesVersionId != null) {
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
        List <KubernetesSupportedVersionVO> versions = kubernetesSupportedVersionDao.search(sc, searchFilter);
        versions = filterKubernetesSupportedVersions(versions, minimumSemanticVersion);

        return createKubernetesSupportedVersionListResponse(versions);
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_ADD, eventDescription = "Adding Kubernetes supported version")
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
        if (minimumCpu == null || minimumCpu < KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_CPU) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s parameter. Minimum %d vCPUs required.", ApiConstants.MIN_CPU_NUMBER, KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_CPU));
        }
        if (minimumRamSize == null || minimumRamSize < KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE) {
            throw new InvalidParameterValueException(String.format("Invalid value for %s parameter. Minimum %dMB memory required", ApiConstants.MIN_MEMORY, KubernetesClusterService.MIN_KUBERNETES_CLUSTER_NODE_RAM_SIZE));
        }
        if (compareSemanticVersions(semanticVersion, MIN_KUBERNETES_VERSION) < 0) {
            throw new InvalidParameterValueException(String.format("New supported Kubernetes version cannot be added as %s is minimum version supported by Kubernetes Service", MIN_KUBERNETES_VERSION));
        }
        if (zoneId != null && dataCenterDao.findById(zoneId) == null) {
            throw new InvalidParameterValueException("Invalid zone specified");
        }
        if (Strings.isNullOrEmpty(isoUrl)) {
            throw new InvalidParameterValueException(String.format("Invalid URL for ISO specified, %s", isoUrl));
        }
        if (Strings.isNullOrEmpty(name)) {
            name = String.format("v%s", semanticVersion);
            if (zoneId != null) {
                name = String.format("%s-%s", name, dataCenterDao.findById(zoneId).getName());
            }
        }

        VMTemplateVO template = null;
        try {
            VirtualMachineTemplate vmTemplate = registerKubernetesVersionIso(zoneId, name, isoUrl, isoChecksum);
            template = templateDao.findById(vmTemplate.getId());
        } catch (IllegalAccessException | NoSuchFieldException | IllegalArgumentException | ResourceAllocationException ex) {
            LOGGER.error(String.format("Unable to register binaries ISO for supported kubernetes version, %s, with url: %s", name, isoUrl), ex);
            throw new CloudRuntimeException(String.format("Unable to register binaries ISO for supported kubernetes version, %s, with url: %s", name, isoUrl));
        }

        KubernetesSupportedVersionVO supportedVersionVO = new KubernetesSupportedVersionVO(name, semanticVersion, template.getId(), zoneId, minimumCpu, minimumRamSize);
        supportedVersionVO = kubernetesSupportedVersionDao.persist(supportedVersionVO);

        return createKubernetesSupportedVersionResponse(supportedVersionVO);
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_DELETE, eventDescription = "Deleting Kubernetes supported version", async = true)
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
            LOGGER.warn(String.format("Unable to find ISO associated with supported Kubernetes version ID: %s", version.getUuid()));
        }
        if (template != null && template.getRemoved() == null) { // Delete ISO
            try {
                deleteKubernetesVersionIso(template.getId());
            } catch (IllegalAccessException | NoSuchFieldException | IllegalArgumentException ex) {
                LOGGER.error(String.format("Unable to delete binaries ISO ID: %s associated with supported kubernetes version ID: %s", template.getUuid(), version.getUuid()), ex);
                throw new CloudRuntimeException(String.format("Unable to delete binaries ISO ID: %s associated with supported kubernetes version ID: %s", template.getUuid(), version.getUuid()));
            }
        }
        return kubernetesSupportedVersionDao.remove(version.getId());
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_UPDATE, eventDescription = "Updating Kubernetes supported version")
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
        return  createKubernetesSupportedVersionResponse(version);
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        if (!KubernetesClusterService.KubernetesServiceEnabled.value()) {
            return cmdList;
        }
        cmdList.add(AddKubernetesSupportedVersionCmd.class);
        cmdList.add(ListKubernetesSupportedVersionsCmd.class);
        cmdList.add(DeleteKubernetesSupportedVersionCmd.class);
        cmdList.add(UpdateKubernetesSupportedVersionCmd.class);
        return cmdList;
    }
}
