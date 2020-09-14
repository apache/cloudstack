/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.image.deployasis;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.deployasis.OVFConfigurationTO;
import com.cloud.agent.api.to.deployasis.OVFEulaSectionTO;
import com.cloud.agent.api.to.deployasis.OVFPropertyTO;
import com.cloud.agent.api.to.deployasis.OVFVirtualHardwareItemTO;
import com.cloud.agent.api.to.deployasis.OVFVirtualHardwareSectionTO;
import com.cloud.agent.api.to.deployasis.TemplateDeployAsIsInformationTO;
import com.cloud.deployasis.DeployAsIsConstants;
import com.cloud.deployasis.TemplateDeployAsIsDetailVO;
import com.cloud.deployasis.UserVmDeployAsIsDetailVO;
import com.cloud.deployasis.dao.TemplateDeployAsIsDetailsDao;
import com.cloud.deployasis.dao.UserVmDeployAsIsDetailsDao;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.compression.CompressionUtil;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.vm.VirtualMachineProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.cloud.agent.api.to.deployasis.OVFNetworkTO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeployAsIsHelperImpl implements DeployAsIsHelper {

    private static final Logger LOGGER = Logger.getLogger(DeployAsIsHelperImpl.class);
    private static Gson gson;

    @Inject
    private TemplateDeployAsIsDetailsDao templateDeployAsIsDetailsDao;
    @Inject
    private UserVmDeployAsIsDetailsDao userVmDeployAsIsDetailsDao;
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private VMTemplatePoolDao templateStoragePoolDao;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        gson = builder.create();
    }

    public void persistTemplateDeployAsIsDetails(long templateId, DownloadAnswer answer) {
        List<OVFPropertyTO> ovfProperties = answer.getOvfProperties();
        List<OVFNetworkTO> networkRequirements = answer.getNetworkRequirements();
        OVFVirtualHardwareSectionTO ovfHardwareSection = answer.getOvfHardwareSection();
        List<OVFEulaSectionTO> eulaSections = answer.getEulaSections();

        if (CollectionUtils.isNotEmpty(ovfProperties)) {
            persistTemplateDeployAsIsInformationTOList(templateId, ovfProperties);
        }
        if (CollectionUtils.isNotEmpty(networkRequirements)) {
            persistTemplateDeployAsIsInformationTOList(templateId, networkRequirements);
        }
        if (CollectionUtils.isNotEmpty(eulaSections)) {
            persistTemplateDeployAsIsInformationTOList(templateId, eulaSections);
        }
        if (ovfHardwareSection != null) {
            if (CollectionUtils.isNotEmpty(ovfHardwareSection.getConfigurations())) {
                persistTemplateDeployAsIsInformationTOList(templateId, ovfHardwareSection.getConfigurations());
            }
            if (CollectionUtils.isNotEmpty(ovfHardwareSection.getCommonHardwareItems())) {
                persistTemplateDeployAsIsInformationTOList(templateId, ovfHardwareSection.getCommonHardwareItems());
            }
        }
    }

    @Override
    public Map<String, String> getVirtualMachineDeployAsIsProperties(VirtualMachineProfile vm) {
        Map<String, String> map = new HashMap<>();
        List<UserVmDeployAsIsDetailVO> details = userVmDeployAsIsDetailsDao.listDetails(vm.getId());
        if (CollectionUtils.isNotEmpty(details)) {
            for (UserVmDeployAsIsDetailVO detail : details) {
                OVFPropertyTO property = templateDeployAsIsDetailsDao.findPropertyByTemplateAndKey(vm.getTemplateId(), detail.getName());
                String value = property.isPassword() ? DBEncryptionUtil.decrypt(detail.getValue()) : detail.getValue();
                map.put(detail.getName(), value);
            }
        }
        return map;
    }

    @Override
    public String getAllocatedVirtualMachineTemplatePath(VirtualMachineProfile vm, String configuration, String destStoragePool) {
        StoragePoolVO storagePoolVO = storagePoolDao.findByUuid(destStoragePool);
        VMTemplateStoragePoolVO tmplRef = templateStoragePoolDao.findByPoolTemplate(storagePoolVO.getId(),
                vm.getTemplate().getId(), configuration);
        if (tmplRef != null) {
            return tmplRef.getInstallPath();
        }
        return null;
    }

    @Override
    public String getAllocatedVirtualMachineDestinationStoragePool(VirtualMachineProfile vm) {
        if (vm != null) {
            if (CollectionUtils.isNotEmpty(vm.getDisks())) {
                for (DiskTO disk : vm.getDisks()) {
                    if (disk.getType() == Volume.Type.ISO) {
                        continue;
                    }
                    DataTO data = disk.getData();
                    if (data != null) {
                        DataStoreTO dataStore = data.getDataStore();
                        if (dataStore != null) {
                            return dataStore.getUuid();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void persistTemplateDeployAsIsInformationTOList(long templateId,
                                                            List<? extends TemplateDeployAsIsInformationTO> informationTOList) {
        for (TemplateDeployAsIsInformationTO informationTO : informationTOList) {
            String propKey = getKeyFromInformationTO(informationTO);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Saving property %s for template %d as detail", propKey, templateId));
            }
            String propValue = null;
            try {
                 propValue = getValueFromInformationTO(informationTO);
            } catch (RuntimeException re) {
                LOGGER.error("gson marshalling of property object fails: " + propKey,re);
            } catch (IOException e) {
                LOGGER.error("Could not decompress the license for template " + templateId, e);
            }
            saveTemplateDeployAsIsPropertyAttribute(templateId, propKey, propValue);
        }
    }

    private String getValueFromInformationTO(TemplateDeployAsIsInformationTO informationTO) throws IOException {
        if (informationTO instanceof OVFEulaSectionTO) {
            CompressionUtil compressionUtil = new CompressionUtil();
            byte[] compressedLicense = ((OVFEulaSectionTO) informationTO).getCompressedLicense();
            return compressionUtil.decompressByteArary(compressedLicense);
        }
        return gson.toJson(informationTO);
    }

    private String getKeyFromInformationTO(TemplateDeployAsIsInformationTO informationTO) {
        if (informationTO instanceof OVFPropertyTO) {
            return DeployAsIsConstants.PROPERTY_PREFIX + ((OVFPropertyTO) informationTO).getKey();
        } else if (informationTO instanceof OVFNetworkTO) {
            return DeployAsIsConstants.NETWORK_PREFIX + ((OVFNetworkTO) informationTO).getName();
        } else if (informationTO instanceof OVFConfigurationTO) {
            return DeployAsIsConstants.CONFIGURATION_PREFIX +
                    ((OVFConfigurationTO) informationTO).getIndex() + "-" + ((OVFConfigurationTO) informationTO).getId();
        } else if (informationTO instanceof OVFVirtualHardwareItemTO) {
            String key = ((OVFVirtualHardwareItemTO) informationTO).getResourceType().getName().trim().replaceAll("\\s","")
                    + "-" + ((OVFVirtualHardwareItemTO) informationTO).getInstanceId();
            return DeployAsIsConstants.HARDWARE_ITEM_PREFIX + key;
        } else if (informationTO instanceof OVFEulaSectionTO) {
            return DeployAsIsConstants.EULA_PREFIX + ((OVFEulaSectionTO) informationTO).getIndex() +
                    "-" + ((OVFEulaSectionTO) informationTO).getInfo();
        }
        return null;
    }

    private void saveTemplateDeployAsIsPropertyAttribute(long templateId, String key, String value) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Saving property %s for template %d as detail", key, templateId));
        }
        if (templateDeployAsIsDetailsDao.findDetail(templateId,key) != null) {
            LOGGER.debug(String.format("Detail '%s' existed for template %d, deleting.", key, templateId));
            templateDeployAsIsDetailsDao.removeDetail(templateId,key);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Template detail for template %d to save is '%s': '%s'", templateId, key, value));
        }
        TemplateDeployAsIsDetailVO detailVO = new TemplateDeployAsIsDetailVO(templateId, key, value);
        LOGGER.debug("Persisting template details " + detailVO.getName() + " from OVF properties for template " + templateId);
        templateDeployAsIsDetailsDao.persist(detailVO);
    }

}
