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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDaoImpl;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDaoImpl;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

import com.cloud.cpu.CPU;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSDaoImpl;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VMTemplateZoneDaoImpl;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.upgrade.dao.BasicTemplateDataStoreDaoImpl;
import com.cloud.user.Account;
import com.cloud.utils.DateUtil;
import com.cloud.utils.HttpUtils;
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

public class SystemVmTemplateRegistration {
    protected static Logger LOGGER = LogManager.getLogger(SystemVmTemplateRegistration.class);
    private static final String MOUNT_COMMAND_BASE = "sudo mount -t nfs";
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
    private static Integer LINUX_12_ID = 363;
    private static final Integer SCRIPT_TIMEOUT = 1800000;
    private static final Integer LOCK_WAIT_TIMEOUT = 1200;
    protected static final List<CPU.CPUArch> DOWNLOADABLE_TEMPLATE_ARCH_TYPES = Arrays.asList(
            CPU.CPUArch.arm64
    );

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
    ImageStoreDetailsDao imageStoreDetailsDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    ConfigurationDao configurationDao;
    @Inject
    private GuestOSDao guestOSDao;

    private String systemVmTemplateVersion;

    private final File tempDownloadDir;

    public SystemVmTemplateRegistration() {
        dataCenterDao = new DataCenterDaoImpl();
        vmTemplateDao = new VMTemplateDaoImpl();
        vmTemplateZoneDao = new VMTemplateZoneDaoImpl();
        templateDataStoreDao = new BasicTemplateDataStoreDaoImpl();
        vmInstanceDao = new VMInstanceDaoImpl();
        imageStoreDao = new ImageStoreDaoImpl();
        imageStoreDetailsDao = new ImageStoreDetailsDaoImpl();
        clusterDao = new ClusterDaoImpl();
        configurationDao = new ConfigurationDaoImpl();
        guestOSDao = new GuestOSDaoImpl();
        tempDownloadDir = new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Convenience constructor method to use when there is no system VM template change for a new version.
     */
    public SystemVmTemplateRegistration(String systemVmTemplateVersion) {
        this();
        this.systemVmTemplateVersion = systemVmTemplateVersion;
    }

    public static String getMountCommand(String nfsVersion, String device, String dir) {
        String cmd = MOUNT_COMMAND_BASE;
        if (StringUtils.isNotBlank(nfsVersion)) {
            cmd = String.format("%s -o vers=%s", cmd, nfsVersion);
        }
        return String.format("%s %s %s", cmd, device, dir);
    }

    public String getSystemVmTemplateVersion() {
        if (StringUtils.isEmpty(systemVmTemplateVersion)) {
            return String.format("%s.%s", CS_MAJOR_VERSION, CS_TINY_VERSION);
        }
        return systemVmTemplateVersion;
    }

    public File getTempDownloadDir() {
        return tempDownloadDir;
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
        CPU.CPUArch arch;
        Long storeId;
        Long size;
        Long physicalSize;
        String installPath;
        boolean deployAsIs;
        Date updated;

        SystemVMTemplateDetails(String uuid, String name, Date created, String url, String checksum,
                                ImageFormat format, Integer guestOsId, Hypervisor.HypervisorType hypervisorType,
                                CPU.CPUArch arch, Long storeId) {
            this.uuid = uuid;
            this.name = name;
            this.created = created;
            this.url = url;
            this.checksum = checksum;
            this.format = format;
            this.guestOsId = guestOsId;
            this.hypervisorType = hypervisorType;
            this.arch = arch;
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

        public CPU.CPUArch getArch() {
            return arch;
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

    public static final List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorList = Arrays.asList(
            new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64),
            new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.arm64),
            new Pair<>(Hypervisor.HypervisorType.VMware, null),
            new Pair<>(Hypervisor.HypervisorType.XenServer, null),
            new Pair<>(Hypervisor.HypervisorType.Hyperv, null),
            new Pair<>(Hypervisor.HypervisorType.LXC, null),
            new Pair<>(Hypervisor.HypervisorType.Ovm3, null)
    );

    public static final Map<String, MetadataTemplateDetails> NewTemplateMap = new HashMap<>();

    public static final Map<Hypervisor.HypervisorType, String> RouterTemplateConfigurationNames = new HashMap<>() {
        {
            put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
            put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
            put(Hypervisor.HypervisorType.XenServer, "router.template.xenserver");
            put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
            put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
            put(Hypervisor.HypervisorType.Ovm3, "router.template.ovm3");
        }
    };

    public static Map<Hypervisor.HypervisorType, Integer> hypervisorGuestOsMap = new HashMap<>() {
        {
            put(Hypervisor.HypervisorType.KVM, LINUX_12_ID);
            put(Hypervisor.HypervisorType.XenServer, OTHER_LINUX_ID);
            put(Hypervisor.HypervisorType.VMware, OTHER_LINUX_ID);
            put(Hypervisor.HypervisorType.Hyperv, LINUX_12_ID);
            put(Hypervisor.HypervisorType.LXC, LINUX_12_ID);
            put(Hypervisor.HypervisorType.Ovm3, LINUX_12_ID);
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

    public boolean validateIfSeeded(TemplateDataStoreVO templDataStoreVO, String url, String path, String nfsVersion) {
        String filePath = null;
        try {
            filePath = Files.createTempDirectory(TEMPORARY_SECONDARY_STORE).toString();
            if (filePath == null) {
                throw new CloudRuntimeException("Failed to create temporary directory to mount secondary store");
            }
            mountStore(url, filePath, nfsVersion);
            int lastIdx = path.lastIndexOf(File.separator);
            String partialDirPath = path.substring(0, lastIdx);
            String templatePath = filePath + File.separator + partialDirPath;
            File templateProps = new File(templatePath + "/template.properties");
            if (templateProps.exists()) {
                Pair<Long, Long> templateSizes = readTemplatePropertiesSizes(templatePath + "/template.properties");
                updateSeededTemplateDetails(templDataStoreVO.getTemplateId(), templDataStoreVO.getDataStoreId(),
                        templateSizes.first(), templateSizes.second());
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

    private static String getHypervisorArchLog(Hypervisor.HypervisorType hypervisorType, CPU.CPUArch arch) {
        StringBuilder sb = new StringBuilder("hypervisor: ").append(hypervisorType.name());
        if (Hypervisor.HypervisorType.KVM.equals(hypervisorType)) {
            sb.append(", arch: ").append(arch == null ? CPU.CPUArch.amd64.getType() : arch.getType());
        }
        return sb.toString();
    }

    protected static String getHypervisorArchKey(Hypervisor.HypervisorType hypervisorType, CPU.CPUArch arch) {
        if (Hypervisor.HypervisorType.KVM.equals(hypervisorType)) {
            return String.format("%s-%s", hypervisorType.name().toLowerCase(),
                    arch == null ? CPU.CPUArch.amd64.getType() : arch.getType());
        }
        return hypervisorType.name().toLowerCase();
    }

    protected static MetadataTemplateDetails getMetadataTemplateDetails(Hypervisor.HypervisorType hypervisorType,
              CPU.CPUArch arch) {
        return NewTemplateMap.get(getHypervisorArchKey(hypervisorType, arch));
    }

    public VMTemplateVO getRegisteredTemplate(String templateName, CPU.CPUArch arch) {
        return vmTemplateDao.findLatestTemplateByName(templateName, arch);
    }

    private static boolean isRunningInTest() {
        return "true".equalsIgnoreCase(System.getProperty("test.mode"));
    }

    /**
     * Attempts to determine the templates directory path by locating the metadata file.
     * <p>
     * This method checks if the application is running in a test environment by invoking
     * {@code isRunningInTest()}. If so, it immediately returns the {@code RELATIVE_TEMPLATE_PATH}.
     * </p>
     * <p>
     * Otherwise, it creates a list of candidate paths (typically including both relative and absolute
     * template paths) and iterates through them. For each candidate, it constructs the metadata file
     * path by appending {@code METADATA_FILE_NAME} to {@code RELATIVE_TEMPLATE_PATH} (note: the candidate
     * path is not used in the file path construction in this implementation) and checks if that file exists.
     * If the metadata file exists, the candidate path is returned.
     * </p>
     * <p>
     * If none of the candidate paths contain the metadata file, the method logs an error and throws a
     * {@link CloudRuntimeException}.
     * </p>
     *
     * @return the path to the templates directory if the metadata file is found, or {@code RELATIVE_TEMPLATE_PATH}
     *         when running in a test environment.
     * @throws CloudRuntimeException if the metadata file cannot be located in any of the candidate paths.
     */
    private static String fetchTemplatesPath() {
        if (isRunningInTest()) {
            return RELATIVE_TEMPLATE_PATH;
        }
        List<String> paths = Arrays.asList(RELATIVE_TEMPLATE_PATH, ABSOLUTE_TEMPLATE_PATH);
        for (String path : paths) {
            String filePath = path + METADATA_FILE_NAME;
            LOGGER.debug("Looking for file [ {} ] in the classpath.", filePath);
            File metaFile = new File(filePath);
            if (metaFile.exists()) {
                return path;
            }
        }
        String errMsg = String.format("Unable to locate metadata file in your setup at %s", StringUtils.join(paths));
        LOGGER.error(errMsg);
        throw new CloudRuntimeException(errMsg);
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

    protected Pair<String, Long> getNfsStoreInZone(Long zoneId) {
        ImageStoreVO storeVO = imageStoreDao.findOneByZoneAndProtocol(zoneId, "nfs");
        if (storeVO == null) {
            String errMsg = String.format("Failed to fetch NFS store in zone = %s for SystemVM template registration",
                    zoneId);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        String url = storeVO.getUrl();
        Long storeId = storeVO.getId();
        return new Pair<>(url, storeId);
    }

    public static void mountStore(String storeUrl, String path, String nfsVersion) {
        try {
            if (storeUrl == null) {
                return;
            }
            URI uri = new URI(UriUtils.encodeURIComponent(storeUrl));
            String host = uri.getHost();
            String mountPath = uri.getPath();
            Script.runSimpleBashScript(getMountCommand(nfsVersion, host + ":" + mountPath, path));
        } catch (Exception e) {
            String msg = "NFS Store URL is not in the correct format";
            LOGGER.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
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
        template.setArch(details.getArch());
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
            if (!vmTemplateZoneDao.update(templateZoneVO.getId(), templateZoneVO)) {
                templateZoneVO = null;
            }
        }
        return templateZoneVO;
    }

    private void createCrossZonesTemplateZoneRefEntries(Long templateId) {
        List<DataCenterVO> dcs = dataCenterDao.listAll();
        for (DataCenterVO dc : dcs) {
            VMTemplateZoneVO templateZoneVO = createOrUpdateTemplateZoneEntry(dc.getId(), templateId);
            if (templateZoneVO == null) {
                throw new CloudRuntimeException(String.format("Failed to create template_zone_ref record for the systemVM template (id: %s) and zone: %s", templateId, dc));
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

    public void updateTemplateDetails(SystemVMTemplateDetails details) {
        VMTemplateVO template = vmTemplateDao.findById(details.getId());
        template.setSize(details.getSize());
        template.setState(VirtualMachineTemplate.State.Active);
        vmTemplateDao.update(template.getId(), template);

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

    public void updateSeededTemplateDetails(long templateId, long storeId, long size, long physicalSize) {
        VMTemplateVO template = vmTemplateDao.findById(templateId);
        template.setSize(size);
        vmTemplateDao.update(template.getId(), template);

        TemplateDataStoreVO templateDataStoreVO = templateDataStoreDao.findByStoreTemplate(storeId, template.getId());
        templateDataStoreVO.setSize(size);
        templateDataStoreVO.setPhysicalSize(physicalSize);
        templateDataStoreVO.setLastUpdated(new Date(DateUtil.currentGMTTime().getTime()));
        boolean updated = templateDataStoreDao.update(templateDataStoreVO.getId(), templateDataStoreVO);
        if (!updated) {
            throw new CloudRuntimeException("Failed to update template_store_ref entry for seeded systemVM template");
        }
    }

    public void updateSystemVMEntries(Long templateId, Hypervisor.HypervisorType hypervisorType) {
        vmInstanceDao.updateSystemVmTemplateId(templateId, hypervisorType);
    }

    private void updateSystemVmTemplateGuestOsId() {
        String systemVmGuestOsName = "Debian GNU/Linux 12 (64-bit)"; // default
        try {
            GuestOSVO guestOS = guestOSDao.findOneByDisplayName(systemVmGuestOsName);
            if (guestOS != null) {
                LOGGER.debug("Updating SystemVM Template Guest OS [{}] id", systemVmGuestOsName);
                SystemVmTemplateRegistration.LINUX_12_ID = Math.toIntExact(guestOS.getId());
                hypervisorGuestOsMap.put(Hypervisor.HypervisorType.KVM, LINUX_12_ID);
                hypervisorGuestOsMap.put(Hypervisor.HypervisorType.Hyperv, LINUX_12_ID);
                hypervisorGuestOsMap.put(Hypervisor.HypervisorType.LXC, LINUX_12_ID);
                hypervisorGuestOsMap.put(Hypervisor.HypervisorType.Ovm3, LINUX_12_ID);
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't update SystemVM Template Guest OS id, due to {}", e.getMessage());
        }
    }

    public void updateConfigurationParams(Map<String, String> configParams) {
        for (Map.Entry<String, String> config : configParams.entrySet()) {
            boolean updated = configurationDao.update(config.getKey(), config.getValue());
            if (!updated) {
                throw new CloudRuntimeException(String.format("Failed to update configuration parameter %s", config.getKey()));
            }
        }
    }

    private static Pair<Long, Long> readTemplatePropertiesSizes(String path) {
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
        return new Pair<>(size, physicalSize);
    }

    public static void readTemplateProperties(String path, SystemVMTemplateDetails details) {
        Pair<Long, Long> templateSizes = readTemplatePropertiesSizes(path);
        details.setSize(templateSizes.first());
        details.setPhysicalSize(templateSizes.second());
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

    private void setupTemplate(String templateName, Hypervisor.HypervisorType hypervisor, CPU.CPUArch arch,
               String destTempFolder) throws CloudRuntimeException {
        String setupTmpltScript = Script.findScript(storageScriptsDir, "setup-sysvm-tmplt");
        if (setupTmpltScript == null) {
            throw new CloudRuntimeException("Unable to find the createtmplt.sh");
        }
        Script scr = new Script(setupTmpltScript, SCRIPT_TIMEOUT, LOGGER);
        scr.add("-u", templateName);
        MetadataTemplateDetails templateDetails = NewTemplateMap.get(getHypervisorArchKey(hypervisor, arch));
        String filePath = StringUtils.isNotBlank(templateDetails.getDownloadedFilePath()) ?
                templateDetails.getDownloadedFilePath() :
                templateDetails.getDefaultFilePath();
        scr.add("-f", filePath);
        scr.add("-h", hypervisor.name().toLowerCase(Locale.ROOT));
        scr.add("-d", destTempFolder);
        String result = scr.execute();
        if (result != null) {
            String errMsg = String.format("failed to create template: %s ", result);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    private Long performTemplateRegistrationOperations(Hypervisor.HypervisorType hypervisor,
           String name, CPU.CPUArch arch, String url, String checksum, ImageFormat format, long guestOsId,
           Long storeId, Long templateId, String filePath, TemplateDataStoreVO templateDataStoreVO) {
        String templateName = UUID.randomUUID().toString();
        Date created = new Date(DateUtil.currentGMTTime().getTime());
        SystemVMTemplateDetails details = new SystemVMTemplateDetails(templateName, name, created,
                url, checksum, format, (int) guestOsId, hypervisor, arch, storeId);
        if (templateId == null) {
            VMTemplateVO template = createTemplateObjectInDB(details);
            if (template == null) {
                throw new CloudRuntimeException(String.format("Failed to register template for hypervisor: %s", hypervisor.name()));
            }
            templateId = template.getId();
        }
        createCrossZonesTemplateZoneRefEntries(templateId);

        details.setId(templateId);
        String destTempFolderName = String.valueOf(templateId);
        String destTempFolder = filePath + PARTIAL_TEMPLATE_FOLDER + destTempFolderName;
        details.setInstallPath(PARTIAL_TEMPLATE_FOLDER + destTempFolderName + File.separator + templateName + "." + hypervisorImageFormat.get(hypervisor).getFileExtension());
        if (templateDataStoreVO == null) {
            createTemplateStoreRefEntry(details);
        }
        setupTemplate(templateName, hypervisor, arch, destTempFolder);
        readTemplateProperties(destTempFolder + "/template.properties", details);
        details.setUpdated(new Date(DateUtil.currentGMTTime().getTime()));
        updateTemplateDetails(details);
        return templateId;
    }

    public void registerTemplate(Hypervisor.HypervisorType hypervisor, String name, Long storeId,
                 VMTemplateVO templateVO, TemplateDataStoreVO templateDataStoreVO, String filePath) {
        try {
            performTemplateRegistrationOperations(hypervisor, name, templateVO.getArch(), templateVO.getUrl(),
                    templateVO.getChecksum(), templateVO.getFormat(), templateVO.getGuestOSId(), storeId,
                    templateVO.getId(), filePath, templateDataStoreVO);
        } catch (Exception e) {
            String errMsg = String.format("Failed to register template for hypervisor: %s", hypervisor);
            LOGGER.error(errMsg, e);
            updateTemplateTablesOnFailure(templateVO.getId());
            cleanupStore(templateVO.getId(), filePath);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    public void registerTemplateForNonExistingEntries(Hypervisor.HypervisorType hypervisor, CPU.CPUArch arch,
              String name, Pair<String, Long> storeUrlAndId, String filePath) {
        Long templateId = null;
        try {
            MetadataTemplateDetails templateDetails = getMetadataTemplateDetails(hypervisor, arch);
            templateId = performTemplateRegistrationOperations(hypervisor, name,
                    templateDetails.getArch(), templateDetails.getUrl(),
                    templateDetails.getChecksum(), hypervisorImageFormat.get(hypervisor),
                    hypervisorGuestOsMap.get(hypervisor), storeUrlAndId.second(), null, filePath, null);
            Map<String, String> configParams = new HashMap<>();
            configParams.put(RouterTemplateConfigurationNames.get(hypervisor), templateDetails.getName());
            configParams.put("minreq.sysvmtemplate.version", getSystemVmTemplateVersion());
            updateConfigurationParams(configParams);
            updateSystemVMEntries(templateId, hypervisor);
        } catch (Exception e) {
            String errMsg = String.format("Failed to register template for hypervisor: %s", hypervisor);
            LOGGER.error(errMsg, e);
            if (templateId != null) {
                updateTemplateTablesOnFailure(templateId);
                cleanupStore(templateId, filePath);
            }
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    protected void validateTemplateFileForHypervisorAndArch(Hypervisor.HypervisorType hypervisor, CPU.CPUArch arch) {
        MetadataTemplateDetails templateDetails = getMetadataTemplateDetails(hypervisor, arch);
        File templateFile = getTemplateFile(templateDetails);
        if (templateFile == null) {
            throw new CloudRuntimeException("Failed to find local template file");
        }
        if (isTemplateFileChecksumDifferent(templateDetails, templateFile)) {
            throw new CloudRuntimeException("Checksum failed for local template file");
        }
    }

    public void validateAndRegisterTemplate(Hypervisor.HypervisorType hypervisor, String name, Long storeId,
                VMTemplateVO templateVO, TemplateDataStoreVO templateDataStoreVO, String filePath) {
        validateTemplateFileForHypervisorAndArch(hypervisor, templateVO.getArch());
        registerTemplate(hypervisor, name, storeId, templateVO, templateDataStoreVO, filePath);
    }

    public void validateAndRegisterTemplateForNonExistingEntries(Hypervisor.HypervisorType hypervisor,
             CPU.CPUArch arch, String name, Pair<String, Long> storeUrlAndId, String filePath) {
        validateTemplateFileForHypervisorAndArch(hypervisor, arch);
        registerTemplateForNonExistingEntries(hypervisor, arch, name, storeUrlAndId, filePath);
    }

    protected static String getMetadataFilePath() {
        return METADATA_FILE;
    }

    /**
     * This method parses the metadata file consisting of the systemVM templates information
     * @return the version of the systemvm template that is to be used. This is done in order
     * to fallback on the latest available version of the systemVM template when there doesn't
     * exist a template corresponding to the current code version.
     */
    public static String parseMetadataFile() {
        String metadataFilePath = getMetadataFilePath();
        String errMsg = String.format("Failed to parse systemVM template metadata file: %s", metadataFilePath);
        final Ini ini = new Ini();
        try (FileReader reader = new FileReader(metadataFilePath)) {
            ini.load(reader);
        } catch (IOException e) {
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
        if (!ini.containsKey("default")) {
            errMsg = String.format("%s as unable to default section", errMsg);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        for (Pair<Hypervisor.HypervisorType, CPU.CPUArch> hypervisorType : hypervisorList) {
            String key = getHypervisorArchKey(hypervisorType.first(), hypervisorType.second());
            Ini.Section section = ini.get(key);
            if (section == null) {
                LOGGER.error("Failed to find details for {} in template metadata file: {}",
                        key, metadataFilePath);
                continue;
            }
            NewTemplateMap.put(key, new MetadataTemplateDetails(
                    hypervisorType.first(),
                    section.get("templatename"),
                    section.get("filename"),
                    section.get("downloadurl"),
                    section.get("checksum"),
                    hypervisorType.second(),
                    section.get("guestos")));
        }
        Ini.Section defaultSection = ini.get("default");
        return defaultSection.get("version").trim();
    }


    private static void cleanupStore(Long templateId, String filePath) {
        String destTempFolder = filePath + PARTIAL_TEMPLATE_FOLDER + String.valueOf(templateId);
        try {
            Files.deleteIfExists(Paths.get(destTempFolder));
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to cleanup mounted store at: %s", filePath), e);
        }
    }

    protected File getTemplateFile(MetadataTemplateDetails templateDetails) {
        File templateFile = new File(templateDetails.getDefaultFilePath());
        if (templateFile.exists()) {
            return templateFile;
        }
        LOGGER.debug("{} is not present", templateFile.getAbsolutePath());
        if (DOWNLOADABLE_TEMPLATE_ARCH_TYPES.contains(templateDetails.getArch()) &&
                StringUtils.isNotBlank(templateDetails.getUrl())) {
            LOGGER.debug("Downloading the template file {} for {}",
                    templateDetails.getUrl(), templateDetails.getHypervisorArchLog());
            Path path = Path.of(TEMPLATES_PATH);
            if (!Files.isWritable(path)) {
                templateFile = new File(tempDownloadDir, templateDetails.getFilename());
            }
            if (!templateFile.exists() &&
                    !HttpUtils.downloadFileWithProgress(templateDetails.getUrl(), templateFile.getAbsolutePath(),
                            LOGGER)) {
                return null;
            }
            templateDetails.setDownloadedFilePath(templateFile.getAbsolutePath());
        }
        return templateFile;
    }

    protected boolean isTemplateFileChecksumDifferent(MetadataTemplateDetails templateDetails, File templateFile) {
        String templateChecksum = DigestHelper.calculateChecksum(templateFile);
        if (!templateChecksum.equals(templateDetails.getChecksum())) {
            LOGGER.error("Checksum {} for file {} does not match checksum {} from metadata",
                    templateChecksum, templateFile, templateDetails.getChecksum());
            return true;
        }
        return false;
    }

    protected void validateTemplates(List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsArchInUse) {
        boolean templatesFound = true;
        for (Pair<Hypervisor.HypervisorType, CPU.CPUArch> hypervisorArch : hypervisorsArchInUse) {
            MetadataTemplateDetails matchedTemplate = getMetadataTemplateDetails(hypervisorArch.first(),
                    hypervisorArch.second());
            if (matchedTemplate == null) {
                templatesFound = false;
                break;
            }
            File tempFile = getTemplateFile(matchedTemplate);
            if (tempFile == null) {
                LOGGER.warn("Failed to download template for {}, moving ahead",
                        matchedTemplate.getHypervisorArchLog());
                continue;
            }
            if (isTemplateFileChecksumDifferent(matchedTemplate, tempFile)) {
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

    protected void registerTemplatesForZone(long zoneId, String filePath) {
        Pair<String, Long> storeUrlAndId = getNfsStoreInZone(zoneId);
        String nfsVersion = getNfsVersion(storeUrlAndId.second());
        mountStore(storeUrlAndId.first(), filePath, nfsVersion);
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList =
                clusterDao.listDistinctHypervisorsAndArchExcludingExternalType(zoneId);
        for (Pair<Hypervisor.HypervisorType, CPU.CPUArch> hypervisorArch : hypervisorArchList) {
            Hypervisor.HypervisorType hypervisorType = hypervisorArch.first();
            MetadataTemplateDetails templateDetails = getMetadataTemplateDetails(hypervisorType,
                    hypervisorArch.second());
            if (templateDetails == null) {
                continue;
            }
            VMTemplateVO templateVO  = getRegisteredTemplate(templateDetails.getName(), templateDetails.getArch());
            if (templateVO != null) {
                TemplateDataStoreVO templateDataStoreVO =
                        templateDataStoreDao.findByStoreTemplate(storeUrlAndId.second(), templateVO.getId());
                if (templateDataStoreVO != null) {
                    String installPath = templateDataStoreVO.getInstallPath();
                    if (validateIfSeeded(templateDataStoreVO, storeUrlAndId.first(), installPath, nfsVersion)) {
                        continue;
                    }
                }
                registerTemplate(hypervisorType, templateDetails.getName(), storeUrlAndId.second(), templateVO,
                        templateDataStoreVO, filePath);
                updateRegisteredTemplateDetails(templateVO.getId(), templateDetails);
                continue;
            }
            registerTemplateForNonExistingEntries(hypervisorType, templateDetails.getArch(), templateDetails.getName(),
                    storeUrlAndId, filePath);
        }
    }

    public void registerTemplates(List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsArchInUse) {
        GlobalLock lock = GlobalLock.getInternLock("UpgradeDatabase-Lock");
        try {
            LOGGER.info("Grabbing lock to register templates.");
            if (!lock.lock(LOCK_WAIT_TIMEOUT)) {
                throw new CloudRuntimeException("Unable to acquire lock to register SystemVM template.");
            }
            try {
                validateTemplates(hypervisorsArchInUse);
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
                                registerTemplatesForZone(zoneId, filePath);
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

    private void updateRegisteredTemplateDetails(Long templateId, MetadataTemplateDetails templateDetails) {
        VMTemplateVO templateVO = vmTemplateDao.findById(templateId);
        templateVO.setTemplateType(Storage.TemplateType.SYSTEM);
        GuestOSVO guestOS = guestOSDao.findOneByDisplayName(templateDetails.getGuestOs());
        if (guestOS != null) {
            templateVO.setGuestOSId(guestOS.getId());
        }
        boolean updated = vmTemplateDao.update(templateVO.getId(), templateVO);
        if (!updated) {
            String errMsg = String.format("updateSystemVmTemplates:Exception while updating template with id %s to be marked as 'system'", templateId);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        Hypervisor.HypervisorType hypervisorType = templateDetails.getHypervisorType();
        updateSystemVMEntries(templateId, hypervisorType);
        // Change value of global configuration parameter router.template.* for the corresponding hypervisor and minreq.sysvmtemplate.version for the ACS version
        Map<String, String> configParams = new HashMap<>();
        configParams.put(RouterTemplateConfigurationNames.get(hypervisorType), templateDetails.getName());
        configParams.put("minreq.sysvmtemplate.version", getSystemVmTemplateVersion());
        updateConfigurationParams(configParams);
    }

    private void updateTemplateUrlChecksumAndGuestOsId(VMTemplateVO templateVO, MetadataTemplateDetails templateDetails) {
        templateVO.setUrl(templateDetails.getUrl());
        templateVO.setChecksum(templateDetails.getChecksum());
        GuestOSVO guestOS = guestOSDao.findOneByDisplayName(templateDetails.getGuestOs());
        if (guestOS != null) {
            templateVO.setGuestOSId(guestOS.getId());
        }
        boolean updated = vmTemplateDao.update(templateVO.getId(), templateVO);
        if (!updated) {
            String errMsg = String.format("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type %s", templateDetails.getHypervisorType());
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    protected boolean registerOrUpdateSystemVmTemplate(MetadataTemplateDetails templateDetails,
                   List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsInUse) {
        LOGGER.debug("Updating System VM template for {}", templateDetails.getHypervisorArchLog());
        VMTemplateVO registeredTemplate = getRegisteredTemplate(templateDetails.getName(), templateDetails.getArch());
        // change template type to SYSTEM
        if (registeredTemplate != null) {
            updateRegisteredTemplateDetails(registeredTemplate.getId(), templateDetails);
        } else {
            boolean isHypervisorArchMatchMetadata = hypervisorsInUse.stream()
                    .anyMatch(p -> p.first().equals(templateDetails.getHypervisorType())
                            && Objects.equals(p.second(), templateDetails.getArch()));
            if (isHypervisorArchMatchMetadata) {
                try {
                    registerTemplates(hypervisorsInUse);
                    return true;
                } catch (final Exception e) {
                    throw new CloudRuntimeException(String.format("Failed to register %s templates for hypervisors: [%s]. " +
                                    "Cannot upgrade system VMs",
                            getSystemVmTemplateVersion(),
                            StringUtils.join(hypervisorsInUse.stream()
                                    .map(x -> getHypervisorArchKey(x.first(), x.second()))
                                    .collect(Collectors.toList()), ",")), e);
                }
            } else {
                LOGGER.warn("Cannot upgrade {} system VM template for {} as it is not used, not failing upgrade",
                        getSystemVmTemplateVersion(), templateDetails.getHypervisorArchLog());
                VMTemplateVO templateVO = vmTemplateDao.findLatestTemplateByTypeAndHypervisorAndArch(
                        templateDetails.getHypervisorType(), templateDetails.getArch(), Storage.TemplateType.SYSTEM);
                if (templateVO != null) {
                    updateTemplateUrlChecksumAndGuestOsId(templateVO, templateDetails);
                }
            }
        }
        return false;
    }

    public void updateSystemVmTemplates(final Connection conn) {
        LOGGER.debug("Updating System Vm template IDs");
        updateSystemVmTemplateGuestOsId();
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsInUse;
                try {
                    hypervisorsInUse = clusterDao.listDistinctHypervisorsAndArchExcludingExternalType(null);
                } catch (final Exception e) {
                    throw new CloudRuntimeException("Exception while getting hypervisor types from clusters", e);
                }
                Collection<MetadataTemplateDetails> templateEntries = NewTemplateMap.values();
                for (MetadataTemplateDetails templateDetails : templateEntries) {
                    try {
                        if (registerOrUpdateSystemVmTemplate(templateDetails, hypervisorsInUse)) {
                            break;
                        }
                    } catch (final Exception e) {
                        String errMsg = "Exception while registering/updating system VM templates for hypervisors in metadata";
                        LOGGER.error(errMsg, e);
                        throw new CloudRuntimeException(errMsg, e);
                    }
                }
                LOGGER.debug("Updating System Vm Template IDs Complete");
            }
        });
    }

    public String getNfsVersion(long storeId) {
        final String configKey = "secstorage.nfs.version";
        final Map<String, String> storeDetails = imageStoreDetailsDao.getDetails(storeId);
        if (storeDetails != null && storeDetails.containsKey(configKey)) {
            return storeDetails.get(configKey);
        }
        ConfigurationVO globalNfsVersion = configurationDao.findByName(configKey);
        if (globalNfsVersion != null) {
            return globalNfsVersion.getValue();
        }
        return null;
    }

    protected static class MetadataTemplateDetails {
        private final Hypervisor.HypervisorType hypervisorType;
        private final String name;
        private final String filename;
        private final String url;
        private final String checksum;
        private final CPU.CPUArch arch;
        private String downloadedFilePath;
        private final String guestOs;

        MetadataTemplateDetails(Hypervisor.HypervisorType hypervisorType, String name, String filename, String url,
                                String checksum, CPU.CPUArch arch, String guestOs) {
            this.hypervisorType = hypervisorType;
            this.name = name;
            this.filename = filename;
            this.url = url;
            this.checksum = checksum;
            this.arch = arch;
            this.guestOs = guestOs;
        }

        public Hypervisor.HypervisorType getHypervisorType() {
            return hypervisorType;
        }

        public String getName() {
            return name;
        }

        public String getFilename() {
            return filename;
        }

        public String getUrl() {
            return url;
        }

        public String getChecksum() {
            return checksum;
        }

        public CPU.CPUArch getArch() {
            return arch;
        }

        public String getGuestOs() {
            return guestOs;
        }

        public String getDownloadedFilePath() {
            return downloadedFilePath;
        }

        public void setDownloadedFilePath(String downloadedFilePath) {
            this.downloadedFilePath = downloadedFilePath;
        }

        public String getDefaultFilePath() {
            return TEMPLATES_PATH + filename;
        }

        public String getHypervisorArchLog() {
            return SystemVmTemplateRegistration.getHypervisorArchLog(hypervisorType, arch);
        }
    }
}
