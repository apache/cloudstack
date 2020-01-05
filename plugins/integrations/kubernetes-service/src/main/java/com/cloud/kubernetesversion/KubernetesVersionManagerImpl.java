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

package com.cloud.kubernetesversion;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.kubernetesversion.AddKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetesversion.DeleteKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.kubernetesversion.ListKubernetesSupportedVersionsCmd;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.kubernetescluster.KubernetesClusterService;
import com.cloud.kubernetescluster.KubernetesClusterVO;
import com.cloud.kubernetescluster.dao.KubernetesClusterDao;
import com.cloud.kubernetesversion.dao.KubernetesSupportedVersionDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;

public class KubernetesVersionManagerImpl extends ManagerBase implements KubernetesVersionService {
    public static final Logger LOGGER = Logger.getLogger(KubernetesVersionManagerImpl.class.getName());

    @Inject
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDao;
    @Inject
    private KubernetesClusterDao kubernetesClusterDao;
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
    @Inject
    private ConfigurationDao globalConfigDao;

    private KubernetesSupportedVersionResponse createKubernetesSupportedVersionResponse(final KubernetesSupportedVersion kubernetesSupportedVersion) {
        KubernetesSupportedVersionResponse response = new KubernetesSupportedVersionResponse();
        response.setObjectName("kubernetessupportedversion");
        response.setId(kubernetesSupportedVersion.getUuid());
        response.setName(kubernetesSupportedVersion.getName());
        response.setKubernetesVersion(kubernetesSupportedVersion.getSemanticVersion());
        DataCenterVO zone = ApiDBUtils.findZoneById(kubernetesSupportedVersion.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        if (compareKubernetesVersion(kubernetesSupportedVersion.getSemanticVersion(),
                KubernetesClusterService.MIN_KUBERNETES_VERSION_HA_SUPPORT)>=0) {
            response.setSupportsHA(true);
        } else {
            response.setSupportsHA(false);
        }
        TemplateJoinVO template = templateJoinDao.findById(kubernetesSupportedVersion.getIsoId());
        response.setIsoId(template.getUuid());
        response.setIsoName(template.getName());
        response.setIsoState(template.getState().toString());
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

    public static int compareKubernetesVersion(String v1, String v2) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(v1) || Strings.isNullOrEmpty(v2)) {
            throw new IllegalArgumentException(String.format("Invalid version comparision with versions %s, %s", v1, v2));
        }
        if(!isSemanticVersion(v1)) {
            throw new IllegalArgumentException(String.format("Invalid version format, %s", v1));
        }
        if(!isSemanticVersion(v2)) {
            throw new IllegalArgumentException(String.format("Invalid version format, %s", v2));
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

    public static boolean canUpgradeKubernetesVersion(String currentVersion, String upgradeVersion) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(currentVersion) || Strings.isNullOrEmpty(upgradeVersion)) {
            throw new IllegalArgumentException(String.format("Invalid version comparision with versions %s, %s", currentVersion, upgradeVersion));
        }
        if(!isSemanticVersion(currentVersion)) {
            throw new IllegalArgumentException(String.format("Invalid version format, %s", currentVersion));
        }
        if(!isSemanticVersion(upgradeVersion)) {
            throw new IllegalArgumentException(String.format("Invalid version format, %s", upgradeVersion));
        }
        String[] thisParts = currentVersion.split("\\.");
        String[] thatParts = upgradeVersion.split("\\.");
        int majorVerDiff = Integer.parseInt(thatParts[0]) - Integer.parseInt(thisParts[0]);
        int minorVerDiff = Integer.parseInt(thatParts[1]) - Integer.parseInt(thisParts[1]);
        // You only can upgrade from one MINOR version to the next MINOR version, or between PATCH versions of the same MINOR.
        // That is, you cannot skip MINOR versions when you upgrade.
        // For example, you can upgrade from 1.y to 1.y+1, but not from 1.y to 1.y+2
        if (majorVerDiff != 0 || minorVerDiff != 1) {
            throw new IllegalArgumentException(String.format("Kubernetes clusters can be upgraded between next minor or patch version releases, current version: %s, upgrade version: %s", currentVersion, upgradeVersion));
        }
        return true;
    }

    private List <KubernetesSupportedVersionVO> filterKubernetesSupportedVersions(List <KubernetesSupportedVersionVO> versions, final String minimumSemanticVersion) {
        if (!Strings.isNullOrEmpty(minimumSemanticVersion)) {
            for (int i = versions.size() - 1; i >= 0; --i) {
                KubernetesSupportedVersionVO version = versions.get(i);
                try {
                    if (compareKubernetesVersion(minimumSemanticVersion, version.getSemanticVersion()) > 0) {
                        versions.remove(i);
                    }
                } catch (Exception e) {
                    LOGGER.warn(String.format("Unable to compare Kubernetes version for supported version ID: %s with %s", version.getUuid(), minimumSemanticVersion));
                    versions.remove(i);
                }
            }
        }
        return versions;
    }

    private VirtualMachineTemplate registerKubernetesVersionIso(final String versionName, final String isoUrl, final String isoChecksum)throws IllegalAccessException, NoSuchFieldException,
            IllegalArgumentException, ResourceAllocationException {
        String isoName = String.format("%s-Kubernetes-Binaries-ISO", versionName);
        RegisterIsoCmd registerIsoCmd = new RegisterIsoCmd();
        registerIsoCmd = ComponentContext.inject(registerIsoCmd);
        Field f = registerIsoCmd.getClass().getDeclaredField("isoName");
        f.setAccessible(true);
        f.set(registerIsoCmd, isoName);
        f = registerIsoCmd.getClass().getDeclaredField("displayText");
        f.setAccessible(true);
        f.set(registerIsoCmd, isoName);
        f = registerIsoCmd.getClass().getDeclaredField("bootable");
        f.setAccessible(true);
        f.set(registerIsoCmd, false);
        f = registerIsoCmd.getClass().getDeclaredField("publicIso");
        f.setAccessible(true);
        f.set(registerIsoCmd, true);
        f = registerIsoCmd.getClass().getDeclaredField("url");
        f.setAccessible(true);
        f.set(registerIsoCmd, isoUrl);
        if (Strings.isNullOrEmpty(isoChecksum)) {
            f = registerIsoCmd.getClass().getDeclaredField("checksum");
            f.setAccessible(true);
            f.set(registerIsoCmd, isoChecksum);
        }
        return templateService.registerIso(registerIsoCmd);
    }

    private void validateExistingTemplateForKubernetesVersionIso(VirtualMachineTemplate template, Long zoneId) {
        if (!template.getFormat().equals(Storage.ImageFormat.ISO)) {
            throw new InvalidParameterValueException(String.format("%s is not an ISO", template.getUuid()));
        }
        if (!template.isPublicTemplate()) {
            throw new InvalidParameterValueException(String.format("ISO ID: %s is not public", template.getUuid()));
        }
        if (!template.isCrossZones() && zoneId == null) {
            throw new InvalidParameterValueException(String.format("ISO ID: %s is not available across zones", template.getUuid()));
        }
        if (!template.isCrossZones() && zoneId != null) {
            List<VMTemplateZoneVO> templatesZoneVOs = templateZoneDao.listByZoneTemplate(zoneId, template.getId());
            if (templatesZoneVOs.isEmpty()) {
                DataCenterVO zone = dataCenterDao.findById(zoneId);
                throw new InvalidParameterValueException(String.format("ISO ID: %s is not available for zone ID: %s", template.getUuid(), zone.getUuid()));
            }
        }
    }

    private void deleteKubernetesVersionIso(long templateId) throws IllegalAccessException, NoSuchFieldException,
            IllegalArgumentException {
        DeleteIsoCmd deleteIsoCmd = new DeleteIsoCmd();
        deleteIsoCmd = ComponentContext.inject(deleteIsoCmd);
        Field f = deleteIsoCmd.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(deleteIsoCmd, templateId);
        templateService.deleteIso(deleteIsoCmd);
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

        List <KubernetesSupportedVersionVO> versions = new ArrayList<>();
        if (versionId != null) {
            KubernetesSupportedVersionVO version = kubernetesSupportedVersionDao.findById(versionId);
            if (version != null && (zoneId == null || version.getZoneId() == null || version.getZoneId().equals(zoneId))) {
                versions.add(version);
            }
        } else {
            if (zoneId == null) {
                versions = kubernetesSupportedVersionDao.listAll();
            } else {
                versions = kubernetesSupportedVersionDao.listAllInZone(zoneId);
            }
        }
        // Filter versions for minimum Kubernetes version
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
        final String kubernetesVersion = cmd.getKubernetesVersion();
        final Long zoneId = cmd.getZoneId();
        final Long isoId = cmd.getIsoId();
        final String isoUrl = cmd.getUrl();
        final String isoChecksum = cmd.getChecksum();
        if (compareKubernetesVersion(kubernetesVersion, MIN_KUBERNETES_VERSION) < 0) {
            throw new InvalidParameterValueException(String.format("New supported Kubernetes version cannot be added as %s is minimum version supported by Kubernetes Service", MIN_KUBERNETES_VERSION));
        }
        if (Strings.isNullOrEmpty(isoUrl) && (isoId == null || isoId <= 0)) {
            throw new InvalidParameterValueException(String.format("Either %s or %s parameter must be passed to add a new supported Kubernetes version", "isourl", ApiConstants.ISO_ID));
        }
        if (!Strings.isNullOrEmpty(isoUrl) && isoId != null && isoId > 0) {
            throw new InvalidParameterValueException(String.format("Both %s and %s parameters can not be passed simultaneously to add a new supported Kubernetes version", "isourl", ApiConstants.ISO_ID));
        }
        if (zoneId != null && dataCenterDao.findById(zoneId) == null) {
            throw new InvalidParameterValueException("Invalid zone specified");
        }
        if (Strings.isNullOrEmpty(name)) {
            name = String.format("v%s", kubernetesVersion);
            if (zoneId != null) {
                name = String.format("%s-%s", name, dataCenterDao.findById(zoneId).getName());
            }
        }

        VMTemplateVO template = null;
        if (isoId != null) {
            template = templateDao.findById(isoId);
        }
        if (template == null) { // register new ISO
            VirtualMachineTemplate vmTemplate = null;
            try {
                vmTemplate = registerKubernetesVersionIso(name, isoUrl, isoChecksum);
            } catch (IllegalAccessException | NoSuchFieldException | IllegalArgumentException | ResourceAllocationException ex) {
                LOGGER.error(String.format("Unable to register binaries ISO for supported kubernetes version, %s", name), ex);
                throw new CloudRuntimeException(String.format("Unable to register binaries ISO for supported kubernetes version, %s", name));
            }
            template = templateDao.findById(vmTemplate.getId());
        } else {
            validateExistingTemplateForKubernetesVersionIso(template, zoneId);
        }

        KubernetesSupportedVersionVO supportedVersionVO = new KubernetesSupportedVersionVO(name, kubernetesVersion, template.getId(), zoneId);
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
        final boolean isDeleteIso = cmd.isDeleteIso();
        KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(versionId);
        if (version == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes version id specified");
        }
        List<KubernetesClusterVO> clusters = kubernetesClusterDao.listAllByKubernetesVersion(versionId);
        if (clusters.size() > 0) {
            throw new CloudRuntimeException(String.format("Unable to delete Kubernetes version ID: %s. Existing clusters currently using the version.", version.getUuid()));
        }

        VMTemplateVO template = templateDao.findById(version.getIsoId());
        if (template == null) {
            LOGGER.warn(String.format("Unable to find ISO associated with supported Kubernetes version ID: %s", version.getUuid()));
        }
        if (isDeleteIso && template != null) { // Delete ISO
            try {
                deleteKubernetesVersionIso(template.getId());
            } catch (Exception ex) {
                LOGGER.error(String.format("Unable to delete binaries ISO ID: %s associated with supported kubernetes version ID: %s", template.getUuid(), version.getUuid()), ex);
                throw new CloudRuntimeException(String.format("Unable to delete binaries ISO ID: %s associated with supported kubernetes version ID: %s", template.getUuid(), version.getUuid()));
            }
        }
        return kubernetesSupportedVersionDao.remove(version.getId());
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
        return cmdList;
    }
}
