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
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
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
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.dc.dao.DataCenterDetailsDaoImpl;
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
    protected static final String STORAGE_SCRIPTS_DIR = "scripts/storage/secondary";
    private static final Integer OTHER_LINUX_ID = 99;
    protected static Integer LINUX_12_ID = 363;
    private static final Integer SCRIPT_TIMEOUT = 1800000;
    private static final Integer LOCK_WAIT_TIMEOUT = 1200;
    protected static final String TEMPLATE_DOWNLOAD_URL_KEY = "downloadurl";
    protected static final String TEMPLATES_DOWNLOAD_REPOSITORY_KEY = "downloadrepository";
    protected static final String TEMPLATES_CUSTOM_DOWNLOAD_REPOSITORY_KEY = "system.vm.templates.download.repository";
    protected static final List<CPU.CPUArch> DOWNLOADABLE_TEMPLATE_ARCH_TYPES = Arrays.asList(
            CPU.CPUArch.amd64,
            CPU.CPUArch.arm64
    );
    protected static final String MINIMUM_SYSTEM_VM_VERSION_KEY = "minreq.sysvmtemplate.version";
    protected static final String DEFAULT_SYSTEM_VM_GUEST_OS_NAME = "Debian GNU/Linux 12 (64-bit)";

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
    DataCenterDetailsDao dataCenterDetailsDao;
    @Inject
    GuestOSDao guestOSDao;

    private String systemVmTemplateVersion;

    private final File tempDownloadDir;

    public SystemVmTemplateRegistration() {
        dataCenterDao = new DataCenterDaoImpl();
        dataCenterDetailsDao = new DataCenterDetailsDaoImpl();
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
     * Convenience constructor method to use when there is no system VM Template change for a new version.
     */
    public SystemVmTemplateRegistration(String systemVmTemplateVersion) {
        this();
        this.systemVmTemplateVersion = systemVmTemplateVersion;
    }

    protected static class SystemVMTemplateDetails {
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

    protected static final List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> AVAILABLE_SYSTEM_TEMPLATES_HYPERVISOR_ARCH_LIST = Arrays.asList(
            new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64),
            new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.arm64),
            new Pair<>(Hypervisor.HypervisorType.VMware, CPU.CPUArch.amd64),
            new Pair<>(Hypervisor.HypervisorType.XenServer, CPU.CPUArch.amd64),
            new Pair<>(Hypervisor.HypervisorType.Hyperv, CPU.CPUArch.amd64),
            new Pair<>(Hypervisor.HypervisorType.LXC, CPU.CPUArch.amd64),
            new Pair<>(Hypervisor.HypervisorType.Ovm3, CPU.CPUArch.amd64)
    );

    protected static final List<MetadataTemplateDetails> METADATA_TEMPLATE_LIST = new ArrayList<>();

    protected static final Map<Hypervisor.HypervisorType, String> ROUTER_TEMPLATE_CONFIGURATION_NAMES = new HashMap<>() {
        {
            put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
            put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
            put(Hypervisor.HypervisorType.XenServer, "router.template.xenserver");
            put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
            put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
            put(Hypervisor.HypervisorType.Ovm3, "router.template.ovm3");
        }
    };

    protected static final Map<Hypervisor.HypervisorType, ImageFormat> HYPERVISOR_IMAGE_FORMAT_MAP = new HashMap<>() {
        {
            put(Hypervisor.HypervisorType.KVM, ImageFormat.QCOW2);
            put(Hypervisor.HypervisorType.XenServer, ImageFormat.VHD);
            put(Hypervisor.HypervisorType.VMware, ImageFormat.OVA);
            put(Hypervisor.HypervisorType.Hyperv, ImageFormat.VHD);
            put(Hypervisor.HypervisorType.LXC, ImageFormat.QCOW2);
            put(Hypervisor.HypervisorType.Ovm3, ImageFormat.RAW);
        }
    };

    protected static Map<Hypervisor.HypervisorType, Integer> hypervisorGuestOsMap = new HashMap<>() {
        {
            put(Hypervisor.HypervisorType.KVM, LINUX_12_ID);
            put(Hypervisor.HypervisorType.XenServer, OTHER_LINUX_ID);
            put(Hypervisor.HypervisorType.VMware, OTHER_LINUX_ID);
            put(Hypervisor.HypervisorType.Hyperv, LINUX_12_ID);
            put(Hypervisor.HypervisorType.LXC, LINUX_12_ID);
            put(Hypervisor.HypervisorType.Ovm3, LINUX_12_ID);
        }
    };

    private static boolean isRunningInTest() {
        return "true".equalsIgnoreCase(System.getProperty("test.mode"));
    }

    private static String getHypervisorArchLog(Hypervisor.HypervisorType hypervisorType, CPU.CPUArch arch) {
        StringBuilder sb = new StringBuilder("hypervisor: ").append(hypervisorType.name());
        sb.append(", arch: ").append(arch == null ? CPU.CPUArch.amd64.getType() : arch.getType());
        return sb.toString();
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

    protected static void cleanupStore(Long templateId, String filePath) {
        String destTempFolder = filePath + PARTIAL_TEMPLATE_FOLDER + String.valueOf(templateId);
        try {
            Files.deleteIfExists(Paths.get(destTempFolder));
        } catch (IOException e) {
            LOGGER.error("Failed to cleanup mounted store at: {}", filePath, e);
        }
    }

    protected static Pair<Long, Long> readTemplatePropertiesSizes(String path) {
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

    protected static MetadataTemplateDetails getMetadataTemplateDetails(Hypervisor.HypervisorType hypervisorType,
                CPU.CPUArch arch) {
        return METADATA_TEMPLATE_LIST
                .stream()
                .filter(x -> Objects.equals(x.getHypervisorType(), hypervisorType) &&
                        Objects.equals(x.getArch(), arch))
                .findFirst()
                .orElse(null);
    }

    protected static String getMetadataFilePath() {
        return METADATA_FILE;
    }

    protected static Ini.Section getMetadataSectionForHypervisorAndArch(Ini ini,
                Hypervisor.HypervisorType hypervisorType, CPU.CPUArch arch) {
        String key = String.format("%s-%s", hypervisorType.name().toLowerCase(),
                arch.getType().toLowerCase());
        Ini.Section section = ini.get(key);
        if (section == null && !Hypervisor.HypervisorType.KVM.equals(hypervisorType)) {
            key = String.format("%s", hypervisorType.name().toLowerCase());
            section = ini.get(key);
        }
        return section;
    }

    protected static String getMountCommand(String nfsVersion, String device, String dir) {
        String cmd = MOUNT_COMMAND_BASE;
        if (StringUtils.isNotBlank(nfsVersion)) {
            cmd = String.format("%s -o vers=%s", cmd, nfsVersion);
        }
        return String.format("%s %s %s", cmd, device, dir);
    }

    /**
     * This method parses the metadata file consisting of the system VM Templates information
     * @return the version of the system VM Template that is to be used. This is done in order
     * to fallback on the latest available version of the system VM Template when there doesn't
     * exist a template corresponding to the current code version.
     */
    public static String parseMetadataFile() {
        String metadataFilePath = getMetadataFilePath();
        String errMsg = String.format("Failed to parse system VM Template metadata file: %s", metadataFilePath);
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
        Ini.Section defaultSection = ini.get("default");
        String defaultDownloadRepository = defaultSection.get(TEMPLATES_DOWNLOAD_REPOSITORY_KEY);
        String customDownloadRepository = ServerPropertiesUtil.getProperty(TEMPLATES_CUSTOM_DOWNLOAD_REPOSITORY_KEY);
        boolean updateCustomDownloadRepository = StringUtils.isNotBlank(customDownloadRepository) &&
                StringUtils.isNotBlank(defaultDownloadRepository);
        for (Pair<Hypervisor.HypervisorType, CPU.CPUArch> hypervisorTypeArchPair : AVAILABLE_SYSTEM_TEMPLATES_HYPERVISOR_ARCH_LIST) {
            String key = String.format("%s-%s", hypervisorTypeArchPair.first().name().toLowerCase(),
                    hypervisorTypeArchPair.second().getType().toLowerCase());
            Ini.Section section = getMetadataSectionForHypervisorAndArch(ini, hypervisorTypeArchPair.first(),
                    hypervisorTypeArchPair.second());
            if (section == null) {
                LOGGER.error("Failed to find details for {} in template metadata file: {}",
                        getHypervisorArchLog(hypervisorTypeArchPair.first(), hypervisorTypeArchPair.second()),
                        metadataFilePath);
                continue;
            }
            String url = section.get(TEMPLATE_DOWNLOAD_URL_KEY);
            if (StringUtils.isNotBlank(url) && updateCustomDownloadRepository) {
                url = url.replaceFirst(defaultDownloadRepository.trim(),
                        customDownloadRepository.trim());
                LOGGER.debug("Updated download URL for {} using custom repository to {}", key, url);
            }
            METADATA_TEMPLATE_LIST.add(new MetadataTemplateDetails(
                    hypervisorTypeArchPair.first(),
                    section.get("templatename"),
                    section.get("filename"),
                    url,
                    section.get("checksum"),
                    hypervisorTypeArchPair.second(),
                    section.get("guestos")));
        }
        return defaultSection.get("version").trim();
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

    protected File getTempDownloadDir() {
        return tempDownloadDir;
    }

    protected void readTemplateProperties(String path, SystemVMTemplateDetails details) {
        Pair<Long, Long> templateSizes = readTemplatePropertiesSizes(path);
        details.setSize(templateSizes.first());
        details.setPhysicalSize(templateSizes.second());
    }

    protected List<Long> getEligibleZoneIds() {
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
            String errMsg = String.format("Failed to fetch NFS store in zone = %s for SystemVM Template registration",
                    zoneId);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        String url = storeVO.getUrl();
        Long storeId = storeVO.getId();
        return new Pair<>(url, storeId);
    }

    protected String getSystemVmTemplateVersion() {
        if (StringUtils.isEmpty(systemVmTemplateVersion)) {
            return String.format("%s.%s", CS_MAJOR_VERSION, CS_TINY_VERSION);
        }
        return systemVmTemplateVersion;
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
        template.setChecksum(DigestHelper.prependAlgorithm(details.getChecksum()));
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

    protected VMTemplateZoneVO createOrUpdateTemplateZoneEntry(long zoneId, long templateId) {
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

    protected void createCrossZonesTemplateZoneRefEntries(Long templateId) {
        List<DataCenterVO> dcs = dataCenterDao.listAll();
        for (DataCenterVO dc : dcs) {
            VMTemplateZoneVO templateZoneVO = createOrUpdateTemplateZoneEntry(dc.getId(), templateId);
            if (templateZoneVO == null) {
                throw new CloudRuntimeException(String.format("Failed to create template-zone record for the system " +
                        "VM Template (ID : %d) and zone: %s", templateId, dc));
            }
        }
    }

    protected void createTemplateStoreRefEntry(SystemVMTemplateDetails details) {
        TemplateDataStoreVO templateDataStoreVO = new TemplateDataStoreVO(details.getStoreId(), details.getId(),
                details.getCreated(), 0, VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED,
                null, null, null, details.getInstallPath(), details.getUrl());
        templateDataStoreVO.setDataStoreRole(DataStoreRole.Image);
        templateDataStoreVO = templateDataStoreDao.persist(templateDataStoreVO);
        if (templateDataStoreVO == null) {
            throw new CloudRuntimeException(String.format("Failed to create template-store record for the system VM " +
                    "template (ID : %d) and store (ID: %d)", details.getId(), details.getStoreId()));
        }
    }

    protected void updateTemplateDetails(SystemVMTemplateDetails details) {
        VMTemplateVO template = vmTemplateDao.findById(details.getId());
        template.setSize(details.getSize());
        template.setState(VirtualMachineTemplate.State.Active);
        vmTemplateDao.update(template.getId(), template);

        TemplateDataStoreVO templateDataStoreVO = templateDataStoreDao.findByStoreTemplate(details.getStoreId(),
                template.getId());
        templateDataStoreVO.setSize(details.getSize());
        templateDataStoreVO.setPhysicalSize(details.getPhysicalSize());
        templateDataStoreVO.setDownloadPercent(100);
        templateDataStoreVO.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        templateDataStoreVO.setLastUpdated(details.getUpdated());
        templateDataStoreVO.setState(ObjectInDataStoreStateMachine.State.Ready);
        boolean updated = templateDataStoreDao.update(templateDataStoreVO.getId(), templateDataStoreVO);
        if (!updated) {
            throw new CloudRuntimeException("Failed to update template-store record for registered system VM Template");
        }
    }

    protected void updateSeededTemplateDetails(long templateId, long storeId, long size, long physicalSize) {
        VMTemplateVO template = vmTemplateDao.findById(templateId);
        template.setSize(size);
        vmTemplateDao.update(template.getId(), template);

        TemplateDataStoreVO templateDataStoreVO = templateDataStoreDao.findByStoreTemplate(storeId, template.getId());
        templateDataStoreVO.setSize(size);
        templateDataStoreVO.setPhysicalSize(physicalSize);
        templateDataStoreVO.setLastUpdated(new Date(DateUtil.currentGMTTime().getTime()));
        boolean updated = templateDataStoreDao.update(templateDataStoreVO.getId(), templateDataStoreVO);
        if (!updated) {
            throw new CloudRuntimeException("Failed to update template-store record for seeded system VM Template");
        }
    }

    protected void updateSystemVMEntries(Long templateId, Hypervisor.HypervisorType hypervisorType) {
        vmInstanceDao.updateSystemVmTemplateId(templateId, hypervisorType);
    }

    protected void updateHypervisorGuestOsMap() {
        try {
            GuestOSVO guestOS = guestOSDao.findOneByDisplayName(DEFAULT_SYSTEM_VM_GUEST_OS_NAME);
            if (guestOS == null) {
                LOGGER.warn("Couldn't find Guest OS by name [{}] to update system VM Template guest OS ID",
                        DEFAULT_SYSTEM_VM_GUEST_OS_NAME);
                return;
            }
            LOGGER.debug("Updating system VM Template guest OS [{}] ID", DEFAULT_SYSTEM_VM_GUEST_OS_NAME);
            SystemVmTemplateRegistration.LINUX_12_ID = Math.toIntExact(guestOS.getId());
            hypervisorGuestOsMap.put(Hypervisor.HypervisorType.KVM, LINUX_12_ID);
            hypervisorGuestOsMap.put(Hypervisor.HypervisorType.Hyperv, LINUX_12_ID);
            hypervisorGuestOsMap.put(Hypervisor.HypervisorType.LXC, LINUX_12_ID);
            hypervisorGuestOsMap.put(Hypervisor.HypervisorType.Ovm3, LINUX_12_ID);
        } catch (Exception e) {
            LOGGER.warn("Couldn't update System VM template guest OS ID, due to {}", e.getMessage());
        }
    }

    protected void updateConfigurationParams(Hypervisor.HypervisorType hypervisorType, String templateName, Long zoneId) {
        String configName = ROUTER_TEMPLATE_CONFIGURATION_NAMES.get(hypervisorType);
        boolean updated = configurationDao.update(configName, templateName);
        if (!updated) {
            throw new CloudRuntimeException(String.format("Failed to update configuration parameter %s", configName));
        }
        if (zoneId != null) {
            dataCenterDetailsDao.removeDetail(zoneId, configName);
        }
        updated = configurationDao.update(MINIMUM_SYSTEM_VM_VERSION_KEY, getSystemVmTemplateVersion());
        if (!updated) {
            throw new CloudRuntimeException(String.format("Failed to update configuration parameter %s", configName));
        }
        if (zoneId != null) {
            dataCenterDetailsDao.removeDetail(zoneId, MINIMUM_SYSTEM_VM_VERSION_KEY);
        }
    }

    protected void updateTemplateEntriesOnFailure(long templateId) {
        VMTemplateVO template = vmTemplateDao.createForUpdate(templateId);
        template.setState(VirtualMachineTemplate.State.Inactive);
        vmTemplateDao.update(template.getId(), template);
        vmTemplateDao.remove(templateId);
        TemplateDataStoreVO templateDataStoreVO = templateDataStoreDao.findByTemplate(template.getId(),
                DataStoreRole.Image);
        if (templateDataStoreVO == null) {
            return;
        }
        templateDataStoreDao.remove(templateDataStoreVO.getId());
    }

    protected void setupTemplateOnStore(String templateName, MetadataTemplateDetails templateDetails,
                  String destTempFolder) throws CloudRuntimeException {
        String setupTmpltScript = Script.findScript(STORAGE_SCRIPTS_DIR, "setup-sysvm-tmplt");
        if (setupTmpltScript == null) {
            throw new CloudRuntimeException("Unable to find the setup-sysvm-tmplt script");
        }
        Script scr = new Script(setupTmpltScript, SCRIPT_TIMEOUT, LOGGER);
        scr.add("-u", templateName);
        String filePath = StringUtils.isNotBlank(templateDetails.getDownloadedFilePath()) ?
                templateDetails.getDownloadedFilePath() :
                templateDetails.getDefaultFilePath();
        scr.add("-f", filePath);
        scr.add("-h", templateDetails.getHypervisorType().name().toLowerCase(Locale.ROOT));
        scr.add("-d", destTempFolder);
        String result = scr.execute();
        if (result != null) {
            String errMsg = String.format("Failed to create Template: %s ", result);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    /**
     * Register or update a system VM Template record and seed it on the target store.
     *
     * @param name display name of the template
     * @param templateDetails metadata for the template
     * @param url download URL of the template
     * @param checksum expected checksum of the template file
     * @param format image format of the template
     * @param guestOsId guest OS id
     * @param storeId target image store id
     * @param templateId existing template id if present, otherwise {@code null}
     * @param filePath temporary mount path for the store
     * @param templateDataStoreVO existing template-store mapping; may be {@code null}
     * @return the id of the template that was created or updated
     */
    protected Long performTemplateRegistrationOperations(String name, MetadataTemplateDetails templateDetails,
           String url, String checksum, ImageFormat format, long guestOsId, Long storeId, Long templateId,
           String filePath, TemplateDataStoreVO templateDataStoreVO) {
        String templateName = UUID.randomUUID().toString();
        Date created = new Date(DateUtil.currentGMTTime().getTime());
        SystemVMTemplateDetails details = new SystemVMTemplateDetails(templateName, name, created, url, checksum,
                format, (int) guestOsId, templateDetails.getHypervisorType(), templateDetails.getArch(), storeId);
        if (templateId == null) {
            VMTemplateVO template = createTemplateObjectInDB(details);
            if (template == null) {
                throw new CloudRuntimeException(String.format("Failed to register Template for hypervisor: %s",
                        templateDetails.getHypervisorType().name()));
            }
            templateId = template.getId();
        }
        createCrossZonesTemplateZoneRefEntries(templateId);

        details.setId(templateId);
        String destTempFolderName = String.valueOf(templateId);
        String destTempFolder = filePath + PARTIAL_TEMPLATE_FOLDER + destTempFolderName;
        details.setInstallPath(String.format("%s%s%s%s.%s", PARTIAL_TEMPLATE_FOLDER, destTempFolderName,
                File.separator, templateName,
                HYPERVISOR_IMAGE_FORMAT_MAP.get(templateDetails.getHypervisorType()).getFileExtension()));
        if (templateDataStoreVO == null) {
            createTemplateStoreRefEntry(details);
        }
        setupTemplateOnStore(templateName, templateDetails, destTempFolder);
        readTemplateProperties(destTempFolder + "/template.properties", details);
        details.setUpdated(new Date(DateUtil.currentGMTTime().getTime()));
        updateTemplateDetails(details);
        return templateId;
    }

    /**
     * Add an existing system VM Template to a secondary image store and update related DB entries.
     *
     * @param templateVO the existing VM template (must not be null)
     * @param templateDetails the metadata details of the template to be added
     * @param templateDataStoreVO optional existing template-store mapping; may be null
     * @param zoneId zone id where the operation is performed
     * @param storeId target image store id
     * @param filePath temporary mount path for the store
     * @throws CloudRuntimeException on failure; the method attempts rollback/cleanup
     */
    protected void addExistingTemplateToStore(VMTemplateVO templateVO, MetadataTemplateDetails templateDetails,
                   TemplateDataStoreVO templateDataStoreVO, long zoneId, Long storeId, String filePath) {
        try {
            performTemplateRegistrationOperations(templateVO.getName(), templateDetails, templateVO.getUrl(),
                    templateVO.getChecksum(), templateVO.getFormat(), templateVO.getGuestOSId(), storeId,
                    templateVO.getId(), filePath, templateDataStoreVO);
        } catch (Exception e) {
            String errMsg = String.format("Failed to add %s to store ID: %d, zone ID: %d", templateVO, storeId, zoneId);
            LOGGER.error(errMsg, e);
            cleanupStore(templateVO.getId(), filePath);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    /**
     * Registers a new system VM Template for the given hypervisor/arch when no existing template is present.
     *
     * @param name the name of the new template
     *  @param templateDetails the metadata details of the template to be registered
     * @param zoneId the zone id for which the new template should be seeded
     * @param storeId the store id on which the new template will be seeded
     * @param filePath temporary mount path for the store
     * @throws CloudRuntimeException on failure; the method attempts rollback/cleanup
     */
    protected void registerNewTemplate(String name, MetadataTemplateDetails templateDetails, long zoneId, Long storeId,
               String filePath) {
        Long templateId = null;
        Hypervisor.HypervisorType hypervisor = templateDetails.getHypervisorType();
        try {
            templateId = performTemplateRegistrationOperations(name, templateDetails, templateDetails.getUrl(),
                    templateDetails.getChecksum(), HYPERVISOR_IMAGE_FORMAT_MAP.get(hypervisor),
                    hypervisorGuestOsMap.get(hypervisor), storeId, null, filePath, null);
            updateConfigurationParams(hypervisor, name, zoneId);
            updateSystemVMEntries(templateId, hypervisor);
        } catch (Exception e) {
            String errMsg = String.format("Failed to register Template for hypervisor: %s", hypervisor);
            LOGGER.error(errMsg, e);
            if (templateId != null) {
                updateTemplateEntriesOnFailure(templateId);
                cleanupStore(templateId, filePath);
            }
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    /**
     * Validate presence and integrity of metadata and local template file for the given hypervisor/arch.
     *
     * @param hypervisor target hypervisor type
     * @param arch target CPU architecture
     * @return validated MetadataTemplateDetails
     * @throws CloudRuntimeException if template is not available, missing, or checksum validation fails
     */
    protected MetadataTemplateDetails getValidatedTemplateDetailsForHypervisorAndArch(
            Hypervisor.HypervisorType hypervisor, CPU.CPUArch arch) {
        if (!AVAILABLE_SYSTEM_TEMPLATES_HYPERVISOR_ARCH_LIST.contains(new Pair<>(hypervisor, arch))) {
            throw new CloudRuntimeException("No system VM Template available for the given hypervisor and arch");
        }
        MetadataTemplateDetails templateDetails = getMetadataTemplateDetails(hypervisor, arch);
        if (templateDetails == null) {
            throw new CloudRuntimeException("No template details found for the given hypervisor and arch");
        }
        File templateFile = getTemplateFile(templateDetails);
        if (templateFile == null) {
            throw new CloudRuntimeException("Failed to find local template file");
        }
        if (templateDetails.isFileChecksumDifferent(templateFile)) {
            throw new CloudRuntimeException("Checksum failed for local template file");
        }
        return templateDetails;
    }

    /**
     * Return the local template file. Downloads it if not present locally and url is present.
     *
     * @param templateDetails template metadata; may set `downloadedFilePath`
     * @return the template {@code File} on disk, or {@code null} if not found/downloaded
     */
    protected File getTemplateFile(MetadataTemplateDetails templateDetails) {
        File templateFile = new File(templateDetails.getDefaultFilePath());
        if (templateFile.exists()) {
            return templateFile;
        }
        LOGGER.debug("{} is not present", templateFile.getAbsolutePath());
        if (StringUtils.isNotBlank(templateDetails.getUrl())) {
            LOGGER.debug("Downloading the template file {} for {}",
                    templateDetails.getUrl(), templateDetails.getHypervisorArchLog());
            Path path = Path.of(TEMPLATES_PATH);
            if (!Files.isWritable(path)) {
                templateFile = new File(getTempDownloadDir(), templateDetails.getFilename());
            }
            if (!templateFile.exists() &&
                    !HttpUtils.downloadFileWithProgress(templateDetails.getUrl(), templateFile.getAbsolutePath(),
                            LOGGER)) {
                LOGGER.error("Failed to download template for {} using url: {}",
                         templateDetails.getHypervisorArchLog(), templateDetails.getUrl());
                return null;
            }
            templateDetails.setDownloadedFilePath(templateFile.getAbsolutePath());
        }
        return templateFile;
    }

    /**
     * Validate that templates for the provided hypervisor/architecture pairs which are in use and are valid.
     *
     * If a template is missing or validation fails for any required pair, a
     * {@link CloudRuntimeException} is thrown to abort the upgrade. If system VM Template for a hypervisor/arch is
     * not considered available then validation is skipped for that pair.
     *
     * @param hypervisorArchList list of hypervisor/architecture pairs to validate
     */
    protected void validateTemplates(List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList) {
        boolean templatesFound = true;
        for (Pair<Hypervisor.HypervisorType, CPU.CPUArch> hypervisorArch : hypervisorArchList) {
            if (!AVAILABLE_SYSTEM_TEMPLATES_HYPERVISOR_ARCH_LIST.contains(hypervisorArch)) {
                LOGGER.info("No system VM Template available for {}. Skipping validation.",
                        getHypervisorArchLog(hypervisorArch.first(), hypervisorArch.second()));
                continue;
            }
            try {
                getValidatedTemplateDetailsForHypervisorAndArch(hypervisorArch.first(), hypervisorArch.second());
            } catch (CloudRuntimeException e) {
                LOGGER.error("Validation failed for {}: {}",
                        getHypervisorArchLog(hypervisorArch.first(), hypervisorArch.second()), e.getMessage());
                templatesFound = false;
                break;
            }
        }
        if (!templatesFound) {
            String errMsg = "SystemVM Template not found. Cannot upgrade system VMs";
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    /**
     * Register or ensure system VM Templates are present on the NFS store for a given zone.
     *
     * Mounts the zone image store, enumerates hypervisors and architectures in the zone,
     * and for each template either adds an existing template to the store or registers
     * a new template as required.
     *
     * @param zoneId the zone id
     * @param storeMountPath temporary mount path for the store
     */
    protected void registerTemplatesForZone(long zoneId, String storeMountPath) {
        Pair<String, Long> storeUrlAndId = getNfsStoreInZone(zoneId);
        String nfsVersion = getNfsVersion(storeUrlAndId.second());
        mountStore(storeUrlAndId.first(), storeMountPath, nfsVersion);
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchList =
                clusterDao.listDistinctHypervisorsAndArchExcludingExternalType(zoneId);
        for (Pair<Hypervisor.HypervisorType, CPU.CPUArch> hypervisorArch : hypervisorArchList) {
            Hypervisor.HypervisorType hypervisorType = hypervisorArch.first();
            MetadataTemplateDetails templateDetails = getMetadataTemplateDetails(hypervisorType,
                    hypervisorArch.second());
            if (templateDetails == null) {
                continue;
            }
            VMTemplateVO templateVO  = getRegisteredTemplate(templateDetails.getName(),
                    templateDetails.getHypervisorType(), templateDetails.getArch(), templateDetails.getUrl());
            if (templateVO != null) {
                TemplateDataStoreVO templateDataStoreVO =
                        templateDataStoreDao.findByStoreTemplate(storeUrlAndId.second(), templateVO.getId());
                if (templateDataStoreVO != null) {
                    String installPath = templateDataStoreVO.getInstallPath();
                    if (validateIfSeeded(templateDataStoreVO, storeUrlAndId.first(), installPath, nfsVersion)) {
                        continue;
                    }
                }
                addExistingTemplateToStore(templateVO, templateDetails, templateDataStoreVO, zoneId,
                        storeUrlAndId.second(), storeMountPath);
                updateRegisteredTemplateDetails(templateVO.getId(), templateDetails, zoneId);
                continue;
            }
            registerNewTemplate(templateDetails.getName(), templateDetails, zoneId, storeUrlAndId.second(),
                    storeMountPath);
        }
    }

    protected void registerTemplates(List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsArchInUse) {
        GlobalLock lock = GlobalLock.getInternLock("UpgradeDatabase-Lock");
        try {
            LOGGER.info("Grabbing lock to register Templates.");
            if (!lock.lock(LOCK_WAIT_TIMEOUT)) {
                throw new CloudRuntimeException("Unable to acquire lock to register system VM Template.");
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
                                throw new CloudRuntimeException("Failed to register system VM Template. Upgrade Failed");
                            }
                        }
                    }
                });
            } catch (Exception e) {
                throw new CloudRuntimeException("Failed to register system VM Template. Upgrade Failed");
            }
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    /**
     * Update the DB record for an existing template to mark it as a system template,
     * set the guest OS (if resolvable), and propagate the change to system VM entries
     * and related configuration for the template's hypervisor.
     *
     * @param templateId       id of the template to update
     * @param templateDetails  metadata used to update the template record
     * @param zoneId           zone id whose per-zone details (if any) should be cleared; may be null
     * @throws CloudRuntimeException if updating the template record fails
     */
    protected void updateRegisteredTemplateDetails(Long templateId, MetadataTemplateDetails templateDetails,
               Long zoneId) {
        VMTemplateVO templateVO = vmTemplateDao.findById(templateId);
        templateVO.setTemplateType(Storage.TemplateType.SYSTEM);
        GuestOSVO guestOS = guestOSDao.findOneByDisplayName(templateDetails.getGuestOs());
        if (guestOS != null) {
            templateVO.setGuestOSId(guestOS.getId());
        }
        boolean updated = vmTemplateDao.update(templateVO.getId(), templateVO);
        if (!updated) {
            String errMsg = String.format("Exception while updating template with id %s to be marked as 'system'",
                    templateId);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        Hypervisor.HypervisorType hypervisorType = templateDetails.getHypervisorType();
        updateSystemVMEntries(templateId, hypervisorType);
        updateConfigurationParams(hypervisorType, templateDetails.getName(), zoneId);
    }

    protected void updateTemplateUrlChecksumAndGuestOsId(VMTemplateVO templateVO,
               MetadataTemplateDetails templateDetails) {
        templateVO.setUrl(templateDetails.getUrl());
        templateVO.setChecksum(DigestHelper.prependAlgorithm(templateDetails.getChecksum()));
        GuestOSVO guestOS = guestOSDao.findOneByDisplayName(templateDetails.getGuestOs());
        if (guestOS != null) {
            templateVO.setGuestOSId(guestOS.getId());
        }
        boolean updated = vmTemplateDao.update(templateVO.getId(), templateVO);
        if (!updated) {
            String errMsg = String.format("Exception while updating 'url' and 'checksum' for hypervisor type %s",
                    templateDetails.getHypervisorType());
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    /**
     * Updates or registers the system VM Template for the given hypervisor/arch if not already present.
     * Returns true if a new template was registered.
     * If there is an existing system VM Template for the given hypervisor/arch, its details are updated.
     * If no existing template is found, new templates are registered for the valid hypervisor/arch which are in use.
     */
    protected boolean updateOrRegisterSystemVmTemplate(MetadataTemplateDetails templateDetails,
               List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorArchInUse) {
        String systemVmTemplateLog = String.format("%s system VM Template for %s", getSystemVmTemplateVersion(),
                templateDetails.getHypervisorArchLog());
        LOGGER.debug("Registering or updating {}", systemVmTemplateLog,
                templateDetails.getHypervisorArchLog());
        VMTemplateVO registeredTemplate = getRegisteredTemplate(templateDetails.getName(),
                templateDetails.getHypervisorType(), templateDetails.getArch(), templateDetails.getUrl());
        if (registeredTemplate != null) {
            LOGGER.info("{} is already registered, updating details for: {}",
                    systemVmTemplateLog, templateDetails.getHypervisorArchLog(), registeredTemplate);
            updateRegisteredTemplateDetails(registeredTemplate.getId(), templateDetails, null);
            return false;
        }
        boolean isHypervisorArchMatchMetadata = hypervisorArchInUse.stream()
                .anyMatch(p -> p.first().equals(templateDetails.getHypervisorType())
                        && Objects.equals(p.second(), templateDetails.getArch()));
        if (!isHypervisorArchMatchMetadata) {
            LOGGER.warn("Skipping upgrading {} as it is not used, not failing upgrade",
                    getSystemVmTemplateVersion(), templateDetails.getHypervisorArchLog());
            VMTemplateVO templateVO = vmTemplateDao.findLatestTemplateByTypeAndHypervisorAndArch(
                    templateDetails.getHypervisorType(), templateDetails.getArch(), Storage.TemplateType.SYSTEM);
            if (templateVO != null) {
                updateTemplateUrlChecksumAndGuestOsId(templateVO, templateDetails);
            }
            return false;
        }
        try {
            registerTemplates(hypervisorArchInUse);
            return true;
        } catch (final Exception e) {
            throw new CloudRuntimeException(String.format("Failed to register %s templates for hypervisors: [%s]. " +
                            "Cannot upgrade system VMs",
                    getSystemVmTemplateVersion(),
                    StringUtils.join(hypervisorArchInUse.stream()
                            .map(x -> String.format("%s-%s", x.first().name(), x.second().name()))
                            .collect(Collectors.toList()), ",")), e);
        }
    }

    /**
     * Return NFS version for the store: store-specific config if present
     * or global config if absent. Returns null if not set.
     */
    protected String getNfsVersion(long storeId) {
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

    /**
     * Validate metadata for the given template's hypervisor/arch and add the existing template
     * to the specified secondary store. On success, database entries are created/updated.
     *
     * @param templateVO template to add
     * @param templateDataStoreVO existing template-store mapping; may be null
     * @param zoneId zone id where the operation is performed
     * @param storeId target image store id
     * @param filePath temporary mount path for the store
     * @throws CloudRuntimeException on failure; the method attempts rollback/cleanup
     */
    public void validateAndAddTemplateToStore(VMTemplateVO templateVO, TemplateDataStoreVO templateDataStoreVO,
                  long zoneId, long storeId, String filePath) {
        MetadataTemplateDetails templateDetails = getValidatedTemplateDetailsForHypervisorAndArch(
                templateVO.getHypervisorType(), templateVO.getArch());
        addExistingTemplateToStore(templateVO, templateDetails, templateDataStoreVO, zoneId, storeId, filePath);
    }

    /**
     * Validate metadata for the given hypervisor/arch and register a new system VM Template
     * on the specified store and zone. Creates DB entries and seeds the template on the store.
     *
     * @param hypervisor hypervisor type
     * @param arch cpu architecture
     * @param name template name to register
     * @param zoneId zone id where the operation is performed
     * @param storeId target image store id
     * @param filePath temporary mount path for the store
     * @throws CloudRuntimeException on failure; the method attempts rollback/cleanup
     */
    public void validateAndRegisterNewTemplate(Hypervisor.HypervisorType hypervisor, CPU.CPUArch arch, String name,
                   long zoneId, long storeId, String filePath) {
        MetadataTemplateDetails templateDetails = getValidatedTemplateDetailsForHypervisorAndArch(hypervisor, arch);
        registerNewTemplate(name, templateDetails, zoneId, storeId, filePath);
    }

    /**
     * Check whether the template at the given `path` on NFS `url` is already seeded.
     * If found, updates DB with sizes and returns true; otherwise returns false.
     *
     * @throws CloudRuntimeException on any error
     */
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
                LOGGER.info("System VM template already seeded, skipping registration");
                return true;
            }
            LOGGER.info("System VM template not seeded");
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to verify if the template is seeded", e);
            throw new CloudRuntimeException("Failed to verify if the template is seeded", e);
        } finally {
            unmountStore(filePath);
            try {
                Files.delete(Path.of(filePath));
            } catch (IOException e) {
                LOGGER.error("Failed to delete temporary directory: {}", filePath);
            }
        }
    }

    /**
     * Finds a registered system VM Template matching the provided criteria.
     *
     * <p>The method first attempts to locate the latest template by {@code templateName},
     * {@code hypervisorType} and {@code arch}. If none is found and a non-blank {@code url}
     * is provided, it falls back to searching for an active system template by the
     * URL path segment (the substring after the last '/' in the URL).</p>
     *
     * @param templateName the template name to search for
     * @param hypervisorType the hypervisor type
     * @param arch the CPU architecture
     * @param url optional download URL used as a fallback; may be {@code null} or blank
     * @return the matching {@code VMTemplateVO} if found; {@code null} otherwise
     */
    public VMTemplateVO getRegisteredTemplate(String templateName, Hypervisor.HypervisorType hypervisorType,
                  CPU.CPUArch arch, String url) {
        VMTemplateVO registeredTemplate = vmTemplateDao.findLatestTemplateByName(templateName, hypervisorType, arch);
        if (registeredTemplate == null && StringUtils.isNotBlank(url)) {
            String urlPath = url.substring(url.lastIndexOf("/") + 1);
            LOGGER.debug("No template found by name, falling back to search existing SYSTEM template by " +
                    "urlPath: {}, hypervisor: {}, arch:{}", urlPath, hypervisorType, arch);
            registeredTemplate = vmTemplateDao.findActiveSystemTemplateByHypervisorArchAndUrlPath(hypervisorType, arch,
                    urlPath);
        }
        LOGGER.debug("Found existing registered template for hypervisor: {}, arch: {}: {}", hypervisorType,
                arch, registeredTemplate);
        return registeredTemplate;
    }

    /**
     * Update or register system VM Templates based on metadata.
     *
     * Runs the registration logic inside a database transaction: obtains the
     * set of hypervisors/architectures in use, iterates over metadata entries
     * and attempts to register or update each template.
     *
     * @param conn retained for compatibility with callers (not used directly)
     */
    public void updateSystemVmTemplates(final Connection conn) {
        LOGGER.debug("Updating System VM templates");
        updateHypervisorGuestOsMap();
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> hypervisorsInUse;
                try {
                    hypervisorsInUse = clusterDao.listDistinctHypervisorsAndArchExcludingExternalType(null);
                } catch (final Exception e) {
                    throw new CloudRuntimeException("Exception while getting hypervisor types from clusters", e);
                }
                for (MetadataTemplateDetails templateDetails : METADATA_TEMPLATE_LIST) {
                    try {
                        if (updateOrRegisterSystemVmTemplate(templateDetails, hypervisorsInUse)) {
                            break;
                        }
                    } catch (final Exception e) {
                        String errMsg = "Exception while registering/updating system VM Templates for hypervisors in metadata";
                        LOGGER.error(errMsg, e);
                        throw new CloudRuntimeException(errMsg, e);
                    }
                }
                LOGGER.debug("Updating System VM Templates Complete");
            }
        });
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

        public boolean isFileChecksumDifferent(File file) {
            String fileChecksum = DigestHelper.calculateChecksum(file);
            if (!fileChecksum.equals(getChecksum())) {
                LOGGER.error("Checksum {} for file {} does not match checksum {} from metadata",
                        fileChecksum, file, getChecksum());
                return true;
            }
            return false;
        }

        public String getHypervisorArchLog() {
            return SystemVmTemplateRegistration.getHypervisorArchLog(hypervisorType, arch);
        }
    }
}
