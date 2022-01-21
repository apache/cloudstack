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
package com.cloud.upgrade;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VMTemplateZoneDaoImpl;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.upgrade.dao.BasicTemplateDataStoreDaoImpl;
import com.cloud.user.Account;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDaoImpl;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ini4j.Ini;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SystemVmTemplateRegistration {
    private static final Logger LOGGER = Logger.getLogger(SystemVmTemplateRegistration.class);
    private static final String MOUNT_COMMAND = "sudo mount -t nfs %s %s";
    private static final String UMOUNT_COMMAND = "sudo umount %s";
    private static final String RELATIVE_TEMPLATE_PATH = "./engine/schema/dist/systemvm-templates/";
    private static final String ABSOLUTE_TEMPLATE_PATH = "/usr/share/cloudstack-management/templates/systemvm/";
    private static final String TEMPLATES_PATH = fetchTemplatesPath();
    private static final String METADATA_FILE_NAME = "metadata.ini";
    private static final String METADATA_FILE = TEMPLATES_PATH + METADATA_FILE_NAME;
    public static final String TEMPORARY_SECONDARY_STORE = "tmp";
    private static final String PARTIAL_TEMPLATE_FOLDER = String.format("/template/tmpl/%d/", Account.ACCOUNT_ID_SYSTEM);
    private static final String storageScriptsDir = "scripts/storage/secondary";
    private static final Integer OTHER_LINUX_ID = 99;
    private static final Integer LINUX_5_ID = 15;
    private static final Integer LINUX_7_ID = 183;
    private static final Integer SCRIPT_TIMEOUT = 1800000;
    private static final Integer LOCK_WAIT_TIMEOUT = 1200;


    public static String CS_MAJOR_VERSION = null;
    public static String CS_TINY_VERSION = null;

    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    VMTemplateDao vmTemplateDao;
    @Inject
    VMTemplateZoneDao vmTemplateZoneDao;
    @Inject
    TemplateDataStoreDao templateDataStoreDao;
    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    ImageStoreDao imageStoreDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    ConfigurationDao configurationDao;

    private String systemVmTemplateVersion;

    public SystemVmTemplateRegistration() {
        dataCenterDao = new DataCenterDaoImpl();
        vmTemplateDao = new VMTemplateDaoImpl();
        vmTemplateZoneDao = new VMTemplateZoneDaoImpl();
        templateDataStoreDao = new BasicTemplateDataStoreDaoImpl();
        vmInstanceDao = new VMInstanceDaoImpl();
        imageStoreDao = new ImageStoreDaoImpl();
        clusterDao = new ClusterDaoImpl();
        configurationDao = new ConfigurationDaoImpl();
    }

    /**
     * Convenience constructor method to use when there is no system VM template change for a new version.
     */
    public SystemVmTemplateRegistration(String systemVmTemplateVersion) {
        this();
        this.systemVmTemplateVersion = systemVmTemplateVersion;
    }

    public String getSystemVmTemplateVersion() {
        if (StringUtils.isEmpty(systemVmTemplateVersion)) {
            return String.format("%s.%s", CS_MAJOR_VERSION, CS_TINY_VERSION);
        }
        return systemVmTemplateVersion;
    }

    private static class SystemVMTemplateDetails {
        Long id;
        String uuid;
        String name;
        String uniqueName;
        Date created;
        String url;
        String checksum;
        ImageFormat format;
        Integer guestOsId;
        Hypervisor.HypervisorType hypervisorType;
        Long storeId;
        Long size;
        Long physicalSize;
        String installPath;
        boolean deployAsIs;
        Date updated;

        SystemVMTemplateDetails(String uuid, String name, Date created, String url, String checksum,
                                ImageFormat format, Integer guestOsId, Hypervisor.HypervisorType hypervisorType,
                                Long storeId) {
            this.uuid = uuid;
            this.name = name;
            this.created = created;
            this.url = url;
            this.checksum = checksum;
            this.format = format;
            this.guestOsId = guestOsId;
            this.hypervisorType = hypervisorType;
            this.storeId = storeId;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public Date getCreated() {
            return created;
        }

        public String getUrl() {
            return url;
        }

        public String getChecksum() {
            return checksum;
        }

        public ImageFormat getFormat() {
            return format;
        }

        public Integer getGuestOsId() {
            return guestOsId;
        }

        public Hypervisor.HypervisorType getHypervisorType() {
            return hypervisorType;
        }

        public Long getStoreId() {
            return storeId;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Long getPhysicalSize() {
            return physicalSize;
        }

        public void setPhysicalSize(Long physicalSize) {
            this.physicalSize = physicalSize;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getUniqueName() {
            return uniqueName;
        }

        public void setUniqueName(String uniqueName) {
            this.uniqueName = uniqueName;
        }

        public boolean isDeployAsIs() {
            return deployAsIs;
        }

        public void setDeployAsIs(boolean deployAsIs) {
            this.deployAsIs = deployAsIs;
        }

        public Date getUpdated() {
            return updated;
        }

        public void setUpdated(Date updated) {
            this.updated = updated;
        }
    }

    public static final List<Hypervisor.HypervisorType> hypervisorList = Arrays.asList(Hypervisor.HypervisorType.KVM,
            Hypervisor.HypervisorType.VMware,
            Hypervisor.HypervisorType.XenServer,
            Hypervisor.HypervisorType.Hyperv,
            Hypervisor.HypervisorType.LXC,
            Hypervisor.HypervisorType.Ovm3
    );

    public static final Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>();
    public static final Map<Hypervisor.HypervisorType, String> FileNames = new HashMap<Hypervisor.HypervisorType, String>();
    public static final Map<Hypervisor.HypervisorType, String> NewTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>();
    public static final Map<Hypervisor.HypervisorType, String> NewTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>();

    public static final Map<Hypervisor.HypervisorType, String> RouterTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
            put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
            put(Hypervisor.HypervisorType.XenServer, "router.template.xenserver");
            put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
            put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
            put(Hypervisor.HypervisorType.Ovm3, "router.template.ovm3");
        }
    };

    public static final Map<Hypervisor.HypervisorType, Integer> hypervisorGuestOsMap = new HashMap<Hypervisor.HypervisorType, Integer>() {
        {
            put(Hypervisor.HypervisorType.KVM, LINUX_5_ID);
            put(Hypervisor.HypervisorType.XenServer, OTHER_LINUX_ID);
            put(Hypervisor.HypervisorType.VMware, OTHER_LINUX_ID);
            put(Hypervisor.HypervisorType.Hyperv, LINUX_5_ID);
            put(Hypervisor.HypervisorType.LXC, LINUX_5_ID);
            put(Hypervisor.HypervisorType.Ovm3, LINUX_7_ID);
        }
    };

    public static final Map<Hypervisor.HypervisorType, ImageFormat> hypervisorImageFormat = new HashMap<Hypervisor.HypervisorType, ImageFormat>() {
        {
            put(Hypervisor.HypervisorType.KVM, ImageFormat.QCOW2);
            put(Hypervisor.HypervisorType.XenServer, ImageFormat.VHD);
            put(Hypervisor.HypervisorType.VMware, ImageFormat.OVA);
            put(Hypervisor.HypervisorType.Hyperv, ImageFormat.VHD);
            put(Hypervisor.HypervisorType.LXC, ImageFormat.QCOW2);
            put(Hypervisor.HypervisorType.Ovm3, ImageFormat.RAW);
        }
    };

    public static boolean validateIfSeeded(String url, String path) {
        String filePath = null;
        try {
            filePath = Files.createTempDirectory(TEMPORARY_SECONDARY_STORE).toString();
            if (filePath == null) {
                throw new CloudRuntimeException("Failed to create temporary directory to mount secondary store");
            }
            mountStore(url, filePath);
            int lastIdx = path.lastIndexOf(File.separator);
            String partialDirPath = path.substring(0, lastIdx);
            String templatePath = filePath + File.separator + partialDirPath;
            File templateProps = new File(templatePath + "/template.properties");
            if (templateProps.exists()) {
                LOGGER.info("SystemVM template already seeded, skipping registration");
                return true;
            }
            LOGGER.info("SystemVM template not seeded");
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to verify if the template is seeded", e);
            throw new CloudRuntimeException("Failed to verify if the template is seeded", e);
        } finally {
            unmountStore(filePath);
            try {
                Files.delete(Path.of(filePath));
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to delete temporary directory: %s", filePath));
            }
        }
    }

    public Long getRegisteredTemplateId(Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
        VMTemplateVO vmTemplate = vmTemplateDao.findLatestTemplateByName(hypervisorAndTemplateName.second());
        Long templateId = null;
        if (vmTemplate != null) {
            templateId = vmTemplate.getId();
        }
        return templateId;
    }

    private static String fetchTemplatesPath() {
        String filePath = RELATIVE_TEMPLATE_PATH + METADATA_FILE_NAME;
        LOGGER.debug(String.format("Looking for file [ %s ] in the classpath.", filePath));
        File metaFile = new File(filePath);
        String templatePath = null;
        if (metaFile.exists()) {
            templatePath = RELATIVE_TEMPLATE_PATH;
        }
        if (templatePath == null) {
            filePath = ABSOLUTE_TEMPLATE_PATH + METADATA_FILE_NAME;
            metaFile = new File(filePath);
            templatePath = ABSOLUTE_TEMPLATE_PATH;
            LOGGER.debug(String.format("Looking for file [ %s ] in the classpath.", filePath));
            if (!metaFile.exists()) {
                String errMsg = String.format("Unable to locate metadata file in your setup at %s", filePath.toString());
                LOGGER.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        }
        return templatePath;
    }

    private String getHypervisorName(String name) {
        if (name.equals("xenserver")) {
            return "xen";
        }
        if (name.equals("ovm3")) {
            return "ovm";
        }
        return name;

    }

    private Hypervisor.HypervisorType getHypervisorType(String hypervisor) {
        if (hypervisor.equalsIgnoreCase("xen")) {
            hypervisor = "xenserver";
        } else if (hypervisor.equalsIgnoreCase("ovm")) {
            hypervisor = "ovm3";
        }
        return Hypervisor.HypervisorType.getType(hypervisor);
    }

    private List<Long> getEligibleZoneIds() {
        List<Long> zoneIds = new ArrayList<>();
        List<ImageStoreVO> stores = imageStoreDao.findByProtocol("nfs");
        for (ImageStoreVO store : stores) {
            if (!zoneIds.contains(store.getDataCenterId())) {
                zoneIds.add(store.getDataCenterId());
            }
        }
        return zoneIds;
    }

    private Pair<String, Long> getNfsStoreInZone(Long zoneId) {
        String url = null;
        Long storeId = null;
        ImageStoreVO storeVO = imageStoreDao.findOneByZoneAndProtocol(zoneId, "nfs");
        if (storeVO == null) {
            String errMsg = String.format("Failed to fetch NFS store in zone = %s for SystemVM template registration", zoneId);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        url = storeVO.getUrl();
        storeId = storeVO.getId();
        return new Pair<>(url, storeId);
    }

    public static void mountStore(String storeUrl, String path) {
        try {
            if (storeUrl != null) {
                URI uri = new URI(UriUtils.encodeURIComponent(storeUrl));
                String host = uri.getHost();
                String mountPath = uri.getPath();
                String mount = String.format(MOUNT_COMMAND, host + ":" + mountPath, path);
                Script.runSimpleBashScript(mount);
            }
        } catch (Exception e) {
            String msg = "NFS Store URL is not in the correct format";
            LOGGER.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    private List<String> fetchAllHypervisors(Long zoneId) {
        List<String> hypervisorList = new ArrayList<>();
        List<Hypervisor.HypervisorType> hypervisorTypes = clusterDao.getAvailableHypervisorInZone(zoneId);
        hypervisorList = hypervisorTypes.stream().distinct().map(Enum::name).collect(Collectors.toList());
        return hypervisorList;
    }

    private VMTemplateVO createTemplateObjectInDB(SystemVMTemplateDetails details) {
        Long templateId = vmTemplateDao.getNextInSequence(Long.class, "id");
        VMTemplateVO template = new VMTemplateVO();
        template.setUuid(details.getUuid());
        template.setUniqueName(String.format("routing-%s" , String.valueOf(templateId)));
        template.setName(details.getName());
        template.setPublicTemplate(false);
        template.setFeatured(false);
        template.setTemplateType(Storage.TemplateType.SYSTEM);
        template.setRequiresHvm(true);
        template.setBits(64);
        template.setAccountId(Account.ACCOUNT_ID_SYSTEM);
        template.setUrl(details.getUrl());
        template.setChecksum(details.getChecksum());
        template.setEnablePassword(false);
        template.setDisplayText(details.getName());
        template.setFormat(details.getFormat());
        template.setGuestOSId(details.getGuestOsId());
        template.setCrossZones(true);
        template.setHypervisorType(details.getHypervisorType());
        template.setState(VirtualMachineTemplate.State.Inactive);
        template.setDeployAsIs(false);
        template = vmTemplateDao.persist(template);
        return template;
    }

    private VMTemplateZoneVO createOrUpdateTemplateZoneEntry(long zoneId, long templateId) {
        VMTemplateZoneVO templateZoneVO = vmTemplateZoneDao.findByZoneTemplate(zoneId, templateId);
        if (templateZoneVO == null) {
            templateZoneVO = new VMTemplateZoneVO(zoneId, templateId, new java.util.Date());
            templateZoneVO = vmTemplateZoneDao.persist(templateZoneVO);
        } else {
            templateZoneVO.setLastUpdated(new java.util.Date());
            if (vmTemplateZoneDao.update(templateZoneVO.getId(), templateZoneVO)) {
                templateZoneVO = null;
            }
        }
        return templateZoneVO;
    }

    private void createCrossZonesTemplateZoneRefEntries(VMTemplateVO template) {
        List<DataCenterVO> dcs = dataCenterDao.listAll();
        for (DataCenterVO dc : dcs) {
            VMTemplateZoneVO templateZoneVO = createOrUpdateTemplateZoneEntry(dc.getId(), template.getId());
            if (templateZoneVO == null) {
                throw new CloudRuntimeException(String.format("Failed to create template_zone_ref record for the systemVM template for hypervisor: %s and zone: %s", template.getHypervisorType().name(), dc));
            }
        }
    }

    private void createTemplateStoreRefEntry(SystemVMTemplateDetails details) {
        TemplateDataStoreVO templateDataStoreVO = new TemplateDataStoreVO(details.storeId, details.getId(), details.getCreated(), 0,
                VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED, null, null, null, details.getInstallPath(), details.getUrl());
        templateDataStoreVO.setDataStoreRole(DataStoreRole.Image);
        templateDataStoreVO = templateDataStoreDao.persist(templateDataStoreVO);
        if (templateDataStoreVO == null) {
            throw new CloudRuntimeException(String.format("Failed to create template_store_ref record for the systemVM template for hypervisor: %s", details.getHypervisorType().name()));
        }
    }

    public void updateTemplateDetails(SystemVMTemplateDetails details, boolean updateTemplateDetails) {
        VMTemplateVO template = vmTemplateDao.findById(details.getId());
        if (updateTemplateDetails) {
            template.setSize(details.getSize());
            template.setState(VirtualMachineTemplate.State.Active);
            vmTemplateDao.update(template.getId(), template);
        }
        TemplateDataStoreVO templateDataStoreVO = templateDataStoreDao.findByStoreTemplate(details.getStoreId(), template.getId());
        templateDataStoreVO.setSize(details.getSize());
        templateDataStoreVO.setPhysicalSize(details.getPhysicalSize());
        templateDataStoreVO.setDownloadPercent(100);
        templateDataStoreVO.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        templateDataStoreVO.setLastUpdated(details.getUpdated());
        templateDataStoreVO.setState(ObjectInDataStoreStateMachine.State.Ready);
        boolean updated = templateDataStoreDao.update(templateDataStoreVO.getId(), templateDataStoreVO);
        if (!updated) {
            throw new CloudRuntimeException("Failed to update template_store_ref entry for registered systemVM template");
        }
    }

    public void updateSystemVMEntries(Long templateId, Hypervisor.HypervisorType hypervisorType) {
        vmInstanceDao.updateSystemVmTemplateId(templateId, hypervisorType);
    }

    public void updateConfigurationParams(Map<String, String> configParams) {
        for (Map.Entry<String, String> config : configParams.entrySet()) {
            boolean updated = configurationDao.update(config.getKey(), config.getValue());
            if (!updated) {
                throw new CloudRuntimeException(String.format("Failed to update configuration parameter %s", config.getKey()));
            }
        }
    }

    private static void readTemplateProperties(String path, SystemVMTemplateDetails details) {
        File tmpFile = new File(path);
        Long size = null;
        Long physicalSize = 0L;
        try (FileReader fr = new FileReader(tmpFile); BufferedReader brf = new BufferedReader(fr);) {
            String line = null;
            while ((line = brf.readLine()) != null) {
                if (line.startsWith("size=")) {
                    physicalSize = Long.parseLong(line.split("=")[1]);
                } else if (line.startsWith("virtualsize=")) {
                    size = Long.parseLong(line.split("=")[1]);
                }
                if (size == null) {
                    size = physicalSize;
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to read from template.properties", ex);
        }
        details.setSize(size);
        details.setPhysicalSize(physicalSize);
    }

    private void updateTemplateTablesOnFailure(long templateId) {
        VMTemplateVO template = vmTemplateDao.createForUpdate(templateId);
        template.setState(VirtualMachineTemplate.State.Inactive);
        vmTemplateDao.update(template.getId(), template);
        vmTemplateDao.remove(templateId);
        TemplateDataStoreVO templateDataStoreVO = templateDataStoreDao.findByTemplate(template.getId(), DataStoreRole.Image);
        templateDataStoreDao.remove(templateDataStoreVO.getId());
    }

    public static void unmountStore(String filePath) {
        try {
            LOGGER.info("Unmounting store");
            String umountCmd = String.format(UMOUNT_COMMAND, filePath);
            Script.runSimpleBashScript(umountCmd);
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to cleanup mounted store at: %s", filePath), e);
            }
        } catch (Exception e) {
            String msg = String.format("Failed to unmount store mounted at %s", filePath);
            LOGGER.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    private void setupTemplate(String templateName, Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName,
                               String destTempFolder) throws CloudRuntimeException {
        String setupTmpltScript = Script.findScript(storageScriptsDir, "setup-sysvm-tmplt");
        if (setupTmpltScript == null) {
            throw new CloudRuntimeException("Unable to find the createtmplt.sh");
        }
        Script scr = new Script(setupTmpltScript, SCRIPT_TIMEOUT, LOGGER);
        scr.add("-u", templateName);
        scr.add("-f", TEMPLATES_PATH + FileNames.get(hypervisorAndTemplateName.first()));
        scr.add("-h", hypervisorAndTemplateName.first().name().toLowerCase(Locale.ROOT));
        scr.add("-d", destTempFolder);
        String result = scr.execute();
        if (result != null) {
            String errMsg = String.format("failed to create template: %s ", result);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

    }

    private Long performTemplateRegistrationOperations(Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName,
                                                       String url, String checksum, ImageFormat format, long guestOsId,
                                                       Long storeId, Long templateId, String filePath, boolean updateTmpltDetails) {
        Hypervisor.HypervisorType hypervisor = hypervisorAndTemplateName.first();
        String templateName = UUID.randomUUID().toString();
        Date created = new Date(DateUtil.currentGMTTime().getTime());
        SystemVMTemplateDetails details = new SystemVMTemplateDetails(templateName, hypervisorAndTemplateName.second(), created,
                url, checksum, format, (int) guestOsId, hypervisor, storeId);
        if (templateId == null) {
            VMTemplateVO template = createTemplateObjectInDB(details);
            if (template == null) {
                throw new CloudRuntimeException(String.format("Failed to register template for hypervisor: %s", hypervisor.name()));
            }
            templateId = template.getId();
            createCrossZonesTemplateZoneRefEntries(template);
        }
        details.setId(templateId);
        String destTempFolderName = String.valueOf(templateId);
        String destTempFolder = filePath + PARTIAL_TEMPLATE_FOLDER + destTempFolderName;
        details.setInstallPath(PARTIAL_TEMPLATE_FOLDER + destTempFolderName + File.separator + templateName + "." + hypervisorImageFormat.get(hypervisor).getFileExtension());
        createTemplateStoreRefEntry(details);
        setupTemplate(templateName, hypervisorAndTemplateName, destTempFolder);
        readTemplateProperties(destTempFolder + "/template.properties", details);
        details.setUpdated(new Date(DateUtil.currentGMTTime().getTime()));
        updateTemplateDetails(details, updateTmpltDetails);
        return templateId;
    }

    public void registerTemplate(Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName,
                                 Pair<String, Long> storeUrlAndId, VMTemplateVO templateVO, String filePath) {
        Long templateId = null;
        try {
            templateId = templateVO.getId();
            performTemplateRegistrationOperations(hypervisorAndTemplateName, templateVO.getUrl(), templateVO.getChecksum(),
                    templateVO.getFormat(), templateVO.getGuestOSId(), storeUrlAndId.second(), templateId, filePath, false);
        } catch (Exception e) {
            String errMsg = String.format("Failed to register template for hypervisor: %s", hypervisorAndTemplateName.first());
            LOGGER.error(errMsg, e);
            if (templateId != null) {
                updateTemplateTablesOnFailure(templateId);
                cleanupStore(templateId, filePath);
            }
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    public void registerTemplate(Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName, Pair<String, Long> storeUrlAndId, String filePath) {
        Long templateId = null;
        try {
            Hypervisor.HypervisorType hypervisor = hypervisorAndTemplateName.first();
            templateId = performTemplateRegistrationOperations(hypervisorAndTemplateName, NewTemplateUrl.get(hypervisor), NewTemplateChecksum.get(hypervisor),
                    hypervisorImageFormat.get(hypervisor), hypervisorGuestOsMap.get(hypervisor), storeUrlAndId.second(), null, filePath, true);
            Map<String, String> configParams = new HashMap<>();
            configParams.put(RouterTemplateConfigurationNames.get(hypervisorAndTemplateName.first()), hypervisorAndTemplateName.second());
            configParams.put("minreq.sysvmtemplate.version", getSystemVmTemplateVersion());
            updateConfigurationParams(configParams);
            updateSystemVMEntries(templateId, hypervisorAndTemplateName.first());
        } catch (Exception e) {
            String errMsg = String.format("Failed to register template for hypervisor: %s", hypervisorAndTemplateName.first());
            LOGGER.error(errMsg, e);
            if (templateId != null) {
                updateTemplateTablesOnFailure(templateId);
                cleanupStore(templateId, filePath);
            }
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    /**
     * This method parses the metadata file consisting of the systemVM templates information
     * @return the version of the systemvm template that is to be used. This is done to in order
     * to fallback on the latest available version of the systemVM template when there does not
     * exist a template corresponding to the current code version.
     */
    public static String parseMetadataFile() {
        try {
            Ini ini = new Ini();
            ini.load(new FileReader(METADATA_FILE));
            for (Hypervisor.HypervisorType hypervisorType : hypervisorList) {
                String hypervisor = hypervisorType.name().toLowerCase(Locale.ROOT);
                Ini.Section section = ini.get(hypervisor);
                NewTemplateNameList.put(hypervisorType, section.get("templatename"));
                FileNames.put(hypervisorType, section.get("filename"));
                NewTemplateChecksum.put(hypervisorType, section.get("checksum"));
                NewTemplateUrl.put(hypervisorType, section.get("downloadurl"));
            }
            Ini.Section section = ini.get("default");
            return section.get("version");
        } catch (Exception e) {
            String errMsg = String.format("Failed to parse systemVM template metadata file: %s", METADATA_FILE);
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    private static void cleanupStore(Long templateId, String filePath) {
        String destTempFolder = filePath + PARTIAL_TEMPLATE_FOLDER + String.valueOf(templateId);
        try {
            Files.deleteIfExists(Paths.get(destTempFolder));
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to cleanup mounted store at: %s", filePath), e);
        }
    }

    private void validateTemplates(Set<Hypervisor.HypervisorType> hypervisorsInUse) {
        Set<String> hypervisors = hypervisorsInUse.stream().map(Enum::name).
                map(name -> name.toLowerCase(Locale.ROOT)).map(this::getHypervisorName).collect(Collectors.toSet());
        List<String> templates = new ArrayList<>();
        for (Hypervisor.HypervisorType hypervisorType : hypervisorsInUse) {
            templates.add(FileNames.get(hypervisorType));
        }

        boolean templatesFound = true;
        for (String hypervisor : hypervisors) {
            String matchedTemplate = templates.stream().filter(x -> x.contains(hypervisor)).findAny().orElse(null);
            if (matchedTemplate == null) {
                templatesFound = false;
                break;
            }

            File tempFile = new File(TEMPLATES_PATH + matchedTemplate);
            String templateChecksum = DigestHelper.calculateChecksum(tempFile);
            if (!templateChecksum.equals(NewTemplateChecksum.get(getHypervisorType(hypervisor)))) {
                LOGGER.error(String.format("Checksum mismatch: %s != %s ", templateChecksum, NewTemplateChecksum.get(getHypervisorType(hypervisor))));
                templatesFound = false;
                break;
            }
        }

        if (!templatesFound) {
            String errMsg = "SystemVm template not found. Cannot upgrade system Vms";
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    public void registerTemplates(Set<Hypervisor.HypervisorType> hypervisorsInUse) {
        GlobalLock lock = GlobalLock.getInternLock("UpgradeDatabase-Lock");
        try {
            LOGGER.info("Grabbing lock to register templates.");
            if (!lock.lock(LOCK_WAIT_TIMEOUT)) {
                throw new CloudRuntimeException("Unable to acquire lock to register SystemVM template.");
            }
            try {
                validateTemplates(hypervisorsInUse);
                // Perform Registration if templates not already registered
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(final TransactionStatus status) {
                        List<Long> zoneIds = getEligibleZoneIds();
                        for (Long zoneId : zoneIds) {
                            String filePath = null;
                            try {
                                filePath = Files.createTempDirectory(TEMPORARY_SECONDARY_STORE).toString();
                                if (filePath == null) {
                                    throw new CloudRuntimeException("Failed to create temporary file path to mount the store");
                                }
                                Pair<String, Long> storeUrlAndId = getNfsStoreInZone(zoneId);
                                mountStore(storeUrlAndId.first(), filePath);
                                List<String> hypervisorList = fetchAllHypervisors(zoneId);
                                for (String hypervisor : hypervisorList) {
                                    Hypervisor.HypervisorType name = Hypervisor.HypervisorType.getType(hypervisor);
                                    String templateName = NewTemplateNameList.get(name);
                                    Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName = new Pair<Hypervisor.HypervisorType, String>(name, templateName);
                                    Long templateId = getRegisteredTemplateId(hypervisorAndTemplateName);
                                    if (templateId != null) {
                                        VMTemplateVO templateVO = vmTemplateDao.findById(templateId);
                                        TemplateDataStoreVO templateDataStoreVO = templateDataStoreDao.findByTemplate(templateId, DataStoreRole.Image);
                                        String installPath = templateDataStoreVO.getInstallPath();
                                        if (validateIfSeeded(storeUrlAndId.first(), installPath)) {
                                            continue;
                                        } else if (templateVO != null) {
                                            registerTemplate(hypervisorAndTemplateName, storeUrlAndId, templateVO, filePath);
                                            continue;
                                        }
                                    }
                                    registerTemplate(hypervisorAndTemplateName, storeUrlAndId, filePath);
                                }
                                unmountStore(filePath);
                            } catch (Exception e) {
                                unmountStore(filePath);
                                throw new CloudRuntimeException("Failed to register systemVM template. Upgrade Failed");
                            }
                        }
                    }
                });
            } catch (Exception e) {
                throw new CloudRuntimeException("Failed to register systemVM template. Upgrade Failed");
            }
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    private void updateRegisteredTemplateDetails(Long templateId, Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
        VMTemplateVO templateVO = vmTemplateDao.findById(templateId);
        templateVO.setTemplateType(Storage.TemplateType.SYSTEM);
        boolean updated = vmTemplateDao.update(templateVO.getId(), templateVO);
        if (!updated) {
            String errMsg = String.format("updateSystemVmTemplates:Exception while updating template with id %s to be marked as 'system'", templateId);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        updateSystemVMEntries(templateId, hypervisorAndTemplateName.getKey());

        // Change value of global configuration parameter router.template.* for the corresponding hypervisor and minreq.sysvmtemplate.version for the ACS version
        Map<String, String> configParams = new HashMap<>();
        configParams.put(RouterTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()), hypervisorAndTemplateName.getValue());
        configParams.put("minreq.sysvmtemplate.version", getSystemVmTemplateVersion());
        updateConfigurationParams(configParams);
    }

    private void updateTemplateUrlAndChecksum(VMTemplateVO templateVO, Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
        templateVO.setUrl(NewTemplateUrl.get(hypervisorAndTemplateName.getKey()));
        templateVO.setChecksum(NewTemplateChecksum.get(hypervisorAndTemplateName.getKey()));
        boolean updated = vmTemplateDao.update(templateVO.getId(), templateVO);
        if (!updated) {
            String errMsg = String.format("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type %s", hypervisorAndTemplateName.getKey().name());
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    public void updateSystemVmTemplates(final Connection conn) {
        LOGGER.debug("Updating System Vm template IDs");
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                Set<Hypervisor.HypervisorType> hypervisorsListInUse = new HashSet<Hypervisor.HypervisorType>();
                try {
                    hypervisorsListInUse = clusterDao.getDistictAvailableHypervisorsAcrossClusters();

                } catch (final Exception e) {
                    LOGGER.error("updateSystemVmTemplates: Exception caught while getting hypervisor types from clusters: " + e.getMessage());
                    throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting hypervisor types from clusters", e);
                }

                for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
                    LOGGER.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
                    Long templateId = getRegisteredTemplateId(new Pair<>(hypervisorAndTemplateName.getKey(), hypervisorAndTemplateName.getValue()));
                    try {
                        // change template type to SYSTEM
                        if (templateId != null) {
                            updateRegisteredTemplateDetails(templateId, hypervisorAndTemplateName);
                        } else {
                            if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())) {
                                try {
                                    registerTemplates(hypervisorsListInUse);
                                    break;
                                } catch (final Exception e) {
                                    throw new CloudRuntimeException(String.format("%s %s SystemVm template not found. Cannot upgrade system Vms", getSystemVmTemplateVersion(), hypervisorAndTemplateName.getKey()));
                                }
                            } else {
                                LOGGER.warn(String.format("%s %s SystemVm template not found. Cannot upgrade system Vms hypervisor is not used, so not failing upgrade",
                                        getSystemVmTemplateVersion(), hypervisorAndTemplateName.getKey()));
                                // Update the latest template URLs for corresponding hypervisor
                                VMTemplateVO templateVO = vmTemplateDao.findLatestTemplateByTypeAndHypervisor(hypervisorAndTemplateName.getKey(), Storage.TemplateType.SYSTEM);
                                if (templateVO != null) {
                                    updateTemplateUrlAndChecksum(templateVO, hypervisorAndTemplateName);
                                }
                            }
                        }
                    } catch (final Exception e) {
                        String errMsg = "updateSystemVmTemplates:Exception while getting ids of templates";
                        LOGGER.error(errMsg, e);
                        throw new CloudRuntimeException(errMsg, e);
                    }
                }
                LOGGER.debug("Updating System Vm Template IDs Complete");
            }
        });
    }
}
