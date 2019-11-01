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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.kubernetesversion.AddKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetesversion.DeleteKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.kubernetesversion.ListKubernetesSupportedVersionsCmd;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.exception.InvalidParameterValueException;
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
        DataCenterVO zone = ApiDBUtils.findZoneById(kubernetesSupportedVersion.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        VMTemplateVO template = ApiDBUtils.findTemplateById(kubernetesSupportedVersion.getIsoId());
        response.setIsoId(template.getUuid());
        response.setIsoName(template.getName());
        response.setIsoState(template.getState().toString());
        return response;
    }

    @Override
    public ListResponse<KubernetesSupportedVersionResponse> listKubernetesSupportedVersions(final ListKubernetesSupportedVersionsCmd cmd) {
        final Long versionId = cmd.getId();
        List<KubernetesSupportedVersionResponse> responseList = new ArrayList<>();
        if (versionId != null) {
            KubernetesSupportedVersionVO version = kubernetesSupportedVersionDao.findById(versionId);
            if (version != null) {
                responseList.add(createKubernetesSupportedVersionResponse(version));
            }
        } else {
            List <KubernetesSupportedVersionVO> versions = kubernetesSupportedVersionDao.listAll();
            for (KubernetesSupportedVersionVO version : versions) {
                responseList.add(createKubernetesSupportedVersionResponse(version));
            }
        }
        ListResponse<KubernetesSupportedVersionResponse> response = new ListResponse<>();
        response.setResponses(responseList);
        return response;
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_ADD, eventDescription = "Adding Kubernetes supported version")
    public KubernetesSupportedVersionResponse addKubernetesSupportedVersion(final AddKubernetesSupportedVersionCmd cmd) {
        final String name = cmd.getName();
        final Long zoneId = cmd.getZoneId();
        final Long isoId = cmd.getIsoId();
        final String isoUrl = cmd.getUrl();
        final String isoChecksum = cmd.getChecksum();
        if (Strings.isNullOrEmpty(name)) {
            throw new InvalidParameterValueException("Name cannot be empty to add a new supported Kubernetes version");
        }
        if (Strings.isNullOrEmpty(isoUrl) && (isoId == null || isoId <= 0)) {
            throw new InvalidParameterValueException(String.format("Either %s or %s paramter must be passed to add a new supported Kubernetes version", "isourl", ApiConstants.ISO_ID));
        }

        if (!Strings.isNullOrEmpty(isoUrl) && isoId != null && isoId > 0) {
            throw new InvalidParameterValueException(String.format("Both %s and %s parameters can not be passed simultaneously to add a new supported Kubernetes version", "isourl", ApiConstants.ISO_ID));
        }

        VMTemplateVO template = null;
        if (isoId != null) {
            template = templateDao.findById(isoId);
        }
        if (template == null) { // register new ISO
            VirtualMachineTemplate vmTemplate = null;
            try {
                String isoName = String.format("%s-Kubernetes-Binaries-ISO", name);
                RegisterIsoCmd registerIsoCmd = new RegisterIsoCmd();
                registerIsoCmd = ComponentContext.inject(registerIsoCmd);
                Field f = registerIsoCmd.getClass().getDeclaredField("name");
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
                vmTemplate = templateService.registerIso(registerIsoCmd);
            } catch (Exception ex) {
                LOGGER.error(String.format("Unable to register binaries ISO for supported kubernetes version, %s", name), ex);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to register binaries ISO for supported kubernetes version, %s", name));
            }
            template = templateDao.findById(vmTemplate.getId());
        } else {
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
        KubernetesSupportedVersionVO supportedVersionVO = new KubernetesSupportedVersionVO(name, template.getId(), zoneId);
        supportedVersionVO = kubernetesSupportedVersionDao.persist(supportedVersionVO);
        return createKubernetesSupportedVersionResponse(supportedVersionVO);
    }

    @Override
    @ActionEvent(eventType = KubernetesVersionEventTypes.EVENT_KUBERNETES_VERSION_DELETE, eventDescription = "Deleting Kubernetes supported version", async = true)
    public boolean deleteKubernetesSupportedVersion(final DeleteKubernetesSupportedVersionCmd cmd) {
        final Long versionId = cmd.getId();
        final Boolean isDeleteIso = cmd.isDeleteIso();
        KubernetesSupportedVersion version = kubernetesSupportedVersionDao.findById(versionId);
        if (version == null) {
            throw new InvalidParameterValueException("Invalid Kubernetes version id specified");
        }
        List<KubernetesClusterVO> clusters = kubernetesClusterDao.listAllByKubernetesVersion(versionId);
        if (clusters.size() > 0) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to delete Kubernetes version ID: %s. Exisiting clusters currently using the version.", version.getUuid()));
        }

        VMTemplateVO template = templateDao.findById(version.getId());
        if (template == null) {
            LOGGER.warn(String.format("Unable to find ISO associated with supported Kubernetes version ID: %s", version.getUuid()));
        }
        if (isDeleteIso && template != null) { // Delete ISO
            try {
                DeleteIsoCmd deleteIsoCmd = new DeleteIsoCmd();
                deleteIsoCmd = ComponentContext.inject(deleteIsoCmd);
                Field f = deleteIsoCmd.getClass().getDeclaredField("id");
                f.setAccessible(true);
                f.set(deleteIsoCmd, template.getId());
                templateService.deleteIso(deleteIsoCmd);
            } catch (Exception ex) {
                LOGGER.error(String.format("Unable to delete binaries ISO ID: %s associated with supported kubernetes version ID: %s", template.getUuid(), version.getUuid()), ex);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Unable to delete binaries ISO ID: %s associated with supported kubernetes version ID: %s", template.getUuid(), version.getUuid()));
            }
        }
        return kubernetesSupportedVersionDao.remove(version.getId());
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddKubernetesSupportedVersionCmd.class);
        cmdList.add(ListKubernetesSupportedVersionsCmd.class);
        cmdList.add(DeleteKubernetesSupportedVersionCmd.class);
        return cmdList;
    }
}
