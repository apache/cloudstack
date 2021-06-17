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
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.OVFInformationTO;
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
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.Pair;
import com.cloud.utils.compression.CompressionUtil;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.cloud.agent.api.to.deployasis.OVFNetworkTO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State.Failed;

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
    @Inject
    private VMTemplateDao templateDao;
    @Inject
    private GuestOSDao guestOSDao;
    @Inject
    private GuestOSHypervisorDao guestOSHypervisorDao;
    @Inject
    private GuestOSCategoryDao guestOSCategoryDao;
    @Inject
    private TemplateDataStoreDao templateDataStoreDao;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        gson = builder.create();
    }

    public boolean persistTemplateDeployAsIsDetails(long templateId, DownloadAnswer answer, TemplateDataStoreVO tmpltStoreVO) {
        try {
            OVFInformationTO ovfInformationTO = answer.getOvfInformationTO();
            if (ovfInformationTO != null) {
                List<OVFPropertyTO> ovfProperties = ovfInformationTO.getProperties();
                List<OVFNetworkTO> networkRequirements = ovfInformationTO.getNetworks();
                OVFVirtualHardwareSectionTO ovfHardwareSection = ovfInformationTO.getHardwareSection();
                List<OVFEulaSectionTO> eulaSections = ovfInformationTO.getEulaSections();
                Pair<String, String> guestOsInfo = ovfInformationTO.getGuestOsInfo();

                if (CollectionUtils.isNotEmpty(ovfProperties)) {
                    persistTemplateDeployAsIsInformationTOList(templateId, ovfProperties);
                }
                if (CollectionUtils.isNotEmpty(networkRequirements)) {
                    persistTemplateDeployAsIsInformationTOList(templateId, networkRequirements);
                }
                if (CollectionUtils.isNotEmpty(eulaSections)) {
                    persistTemplateDeployAsIsInformationTOList(templateId, eulaSections);
                }
                String minimumHardwareVersion = null;
                if (ovfHardwareSection != null) {
                    if (CollectionUtils.isNotEmpty(ovfHardwareSection.getConfigurations())) {
                        persistTemplateDeployAsIsInformationTOList(templateId, ovfHardwareSection.getConfigurations());
                    }
                    if (CollectionUtils.isNotEmpty(ovfHardwareSection.getCommonHardwareItems())) {
                        persistTemplateDeployAsIsInformationTOList(templateId, ovfHardwareSection.getCommonHardwareItems());
                    }
                    minimumHardwareVersion = ovfHardwareSection.getMinimiumHardwareVersion();
                }
                if (guestOsInfo != null) {
                    String osType = guestOsInfo.first();
                    String osDescription = guestOsInfo.second();
                    LOGGER.info("Guest OS information retrieved from the template: " + osType + " - " + osDescription);
                    handleGuestOsFromOVFDescriptor(templateId, osType, osDescription, minimumHardwareVersion);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error persisting deploy-as-is details for template " + templateId, e);
            tmpltStoreVO.setErrorString(e.getMessage());
            tmpltStoreVO.setState(Failed);
            tmpltStoreVO.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
            templateDataStoreDao.update(tmpltStoreVO.getId(), tmpltStoreVO);
            return false;
        }
        LOGGER.info("Successfully persisted deploy-as-is details for template " + templateId);
        return true;
    }

    /**
     * Returns the mapped guest OS from the OVF file of the template to the CloudStack database OS ID
     */
    public Long retrieveTemplateGuestOsIdFromGuestOsInfo(long templateId, String guestOsType, String guestOsDescription,
                                                         String minimumHardwareVersion) {
        VMTemplateVO template = templateDao.findById(templateId);
        Hypervisor.HypervisorType hypervisor = template.getHypervisorType();
        if (hypervisor != Hypervisor.HypervisorType.VMware) {
            return null;
        }

        String minimumHypervisorVersion = getMinimumSupportedHypervisorVersionForHardwareVersion(minimumHardwareVersion);
        LOGGER.info("Minimum hardware version " + minimumHardwareVersion + " matched to hypervisor version " + minimumHypervisorVersion + ". " +
                "Checking guest OS supporting this version");

        List<GuestOSHypervisorVO> guestOsMappings = guestOSHypervisorDao.listByOsNameAndHypervisorMinimumVersion(guestOsType,
                hypervisor.toString(), minimumHypervisorVersion);

        if (CollectionUtils.isNotEmpty(guestOsMappings)) {
            Long guestOsId = null;
            if (guestOsMappings.size() == 1) {
                GuestOSHypervisorVO mapping = guestOsMappings.get(0);
                guestOsId = mapping.getGuestOsId();
            } else {
                if (!StringUtils.isEmpty(guestOsDescription)) {
                    for (GuestOSHypervisorVO guestOSHypervisorVO : guestOsMappings) {
                        GuestOSVO guestOSVO = guestOSDao.findById(guestOSHypervisorVO.getGuestOsId());
                        if (guestOsDescription.equalsIgnoreCase(guestOSVO.getDisplayName())) {
                            guestOsId = guestOSHypervisorVO.getGuestOsId();
                            break;
                        }
                    }
                }
                if (null == guestOsId) {
                    GuestOSHypervisorVO mapping = guestOsMappings.get(guestOsMappings.size()-1);
                    guestOsId = mapping.getGuestOsId();
                }
            }
            return guestOsId;
        } else {
            throw new CloudRuntimeException("Did not find a guest OS with type " + guestOsType);
        }
    }

    /**
     * Handle the guest OS read from the OVF and try to match it to an existing guest OS in DB.
     * If the guest OS cannot be mapped to an existing guest OS in DB, then create it and create support for hypervisor versions.
     * Roll back actions in case of unexpected errors
     */
    private void handleGuestOsFromOVFDescriptor(long templateId, String guestOsType, String guestOsDescription,
                                                String minimumHardwareVersion) {
        Long guestOsId = retrieveTemplateGuestOsIdFromGuestOsInfo(templateId, guestOsType, guestOsDescription, minimumHardwareVersion);
        if (guestOsId != null) {
            LOGGER.info("Updating deploy-as-is template guest OS to " + guestOsType);
            VMTemplateVO template = templateDao.findById(templateId);
            updateTemplateGuestOsId(template, guestOsId);
        }
    }

    /**
     * Updates the deploy-as-is template guest OS doing:
     * - Create a new guest OS with the guest OS description parsed from the OVF
     * - Create mappings for the new guest OS and supported hypervisor versions
     * - Update the template guest OS ID to the new guest OS ID
     */
    private void updateDeployAsIsTemplateToNewGuestOs(VMTemplateVO template, String guestOsType, String guestOsDescription,
                                                      Hypervisor.HypervisorType hypervisor, Collection<String> hypervisorVersions) {
        GuestOSVO newGuestOs = createGuestOsEntry(guestOsDescription);
        for (String hypervisorVersion : hypervisorVersions) {
            LOGGER.info(String.format("Adding a new guest OS mapping for hypervisor: %s version: %s and " +
                    "guest OS: %s", hypervisor.toString(), hypervisorVersion, guestOsType));
            createGuestOsHypervisorMapping(newGuestOs.getId(), guestOsType, hypervisor.toString(), hypervisorVersion);
        }
        updateTemplateGuestOsId(template, newGuestOs.getId());
    }

    private void updateTemplateGuestOsId(VMTemplateVO template, long guestOsId) {
        template.setGuestOSId(guestOsId);
        templateDao.update(template.getId(), template);
    }

    /**
     * Create a new entry on guest_os_hypervisor
     */
    private void createGuestOsHypervisorMapping(long guestOsId, String guestOsType, String hypervisorType, String hypervisorVersion) {
        GuestOSHypervisorVO mappingVO = new GuestOSHypervisorVO();
        mappingVO.setGuestOsId(guestOsId);
        mappingVO.setHypervisorType(hypervisorType);
        mappingVO.setHypervisorVersion(hypervisorVersion);
        mappingVO.setGuestOsName(guestOsType);
        guestOSHypervisorDao.persist(mappingVO);
    }

    /**
     * Create new guest OS entry with category 'Other'
     */
    private GuestOSVO createGuestOsEntry(String guestOsDescription) {
        GuestOSCategoryVO categoryVO = guestOSCategoryDao.findByCategoryName("Other");
        long categoryId = categoryVO != null ? categoryVO.getId() : 7L;
        GuestOSVO guestOSVO = new GuestOSVO();
        guestOSVO.setDisplayName(guestOsDescription);
        guestOSVO.setCategoryId(categoryId);
        return guestOSDao.persist(guestOSVO);
    }

    /**
     * Minimum VMware hosts supported version is 6.0
     */
    protected String getMinimumSupportedHypervisorVersionForHardwareVersion(String hardwareVersion) {
        // From https://kb.vmware.com/s/article/1003746 and https://kb.vmware.com/s/article/2007240
        String hypervisorVersion = "default";
        if (StringUtils.isBlank(hardwareVersion)) {
            return hypervisorVersion;
        }
        String hwVersion = hardwareVersion.replace("vmx-", "");
        try {
            int hwVersionNumber = Integer.parseInt(hwVersion);
            if (hwVersionNumber <= 11) {
                hypervisorVersion = "6.0";
            } else if (hwVersionNumber == 13) {
                hypervisorVersion = "6.5";
            } else if (hwVersionNumber >= 14) {
                hypervisorVersion = "6.7";
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Cannot parse hardware version " + hwVersion + " to integer. Using default hypervisor version", e);
        }
        return hypervisorVersion;
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
    public Map<Integer, String> getAllocatedVirtualMachineNicsAdapterMapping(VirtualMachineProfile vm, NicTO[] nics) {
        Map<Integer, String> map = new HashMap<>();
        List<OVFNetworkTO> networks = templateDeployAsIsDetailsDao.listNetworkRequirementsByTemplateId(vm.getTemplateId());
        if (ArrayUtils.isNotEmpty(nics)) {
            if (nics.length != networks.size()) {
                String msg = "Different number of networks provided vs networks defined in deploy-as-is template";
                LOGGER.error(msg);
                return map;
            }
            for (int i = 0; i < nics.length; i++) {
                // The nic Adapter is defined on the resource sub type
                map.put(nics[i].getDeviceId(), networks.get(i).getResourceSubType());
            }
        }
        return map;
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
