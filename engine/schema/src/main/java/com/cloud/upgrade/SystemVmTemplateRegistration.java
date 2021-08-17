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

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;
import org.ini4j.Ini;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SystemVmTemplateRegistration {
    private static final Logger LOGGER = Logger.getLogger(SystemVmTemplateRegistration.class);
    private static final String mountCommand = "sudo mount -t nfs %s %s";
    private static final String umountCommand = "sudo umount %s";
    private static final  String hashAlgorithm = "MD5";
    private static final String relativeTemplatePath = "./engine/schema/dist/systemvm-templates/";
    private static final String AbsolutetemplatesPath = "/usr/share/cloudstack-management/templates/";
    private static final String templatesPath = fetchTemplatesPath();
    private static final String metadataFileName = "metadata.ini";
    private static final String metadataFile = templatesPath + metadataFileName;
    private static final String TEMPORARY_SECONDARY_STORE = "/tmp/tmpSecStorage";
    private static final String PARENT_TEMPLATE_FOLDER = TEMPORARY_SECONDARY_STORE;
    private static final String PARTIAL_TEMPLATE_FOLDER = "/template/tmpl/1/";
    private static final String FETCH_DISTINCT_ELIGIBLE_ZONES = "SELECT DISTINCT(data_center_id) FROM `cloud`.`image_store` WHERE protocol = \"nfs\"  AND role = \"Image\" AND removed is null";
    private static final String FETCH_DISTINCT_HYPERVISORS_IN_ZONE = "SELECT DISTINCT(hypervisor_type) FROM `cloud`.`cluster` where  data_center_id=? AND role = \"Image\" AND image_provider_name = \"NFS\" AND removed is null";
    private static final String FETCH_IMAGE_STORE_PER_ZONE = "SELECT url,id FROM `cloud`.`image_store` WHERE data_center_id=? AND removed IS NULL LIMIT 1";
    private static final String INSERT_VM_TEMPLATE_TABLE = "INSERT INTO `cloud`.`vm_template` (uuid, unique_name, name, public, featured, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, cross_zones, hypervisor_type, state, deploy_as_is)" +
        "VALUES (?, ?, ?, 0, 0, ?, 'SYSTEM', 0, 64, 1, ?, ?, 0, ?, ?, ?, 1, ?, 'Inactive', ?)";
    private static final String INSERT_TEMPLATE_STORE_REF_TABLE = "INSERT INTO `cloud`.`template_store_ref` (store_id,  template_id, created, last_updated, job_id, download_pct, download_state, error_str, local_path, install_path, url, state, destroyed, is_copy," +
            " update_count, ref_cnt, store_role) VALUES (?, ?, ?, ?, NULL, 0, 'NOT_DOWNLOADED', NULL, NULL, ?, ?, 'Allocated', 0, 0, 0, 0, 'Image')";
    private static final String UPDATE_TEMPLATE_STORE_REF_TABLE = "UPDATE `cloud`.`template_store_ref` SET download_pct=100, download_state='DOWNLOADED', " +
            "state='Ready', size=?, physical_size=?, last_updated=?, updated=? where template_id=?";
    private static final String UPDATE_VM_TEMPLATE_ENTRY = "UPDATE `cloud`.`vm_template` set size = ?, state = 'Active' where id = ?";
    private static final String UPDATE_CONFIGURATION_TABLE = "UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?";
    private static final String UPDATE_TEMPLATE_TABLE_ON_FAILURE = "UPDATE `cloud`.`vm_template` set removed = ?, state = 'Inactive' where id = ?";
    private static final String DELETE_TEMPLATE_REF_RECORD_ON_FAILURE = "DELETE from `cloud`.`template_store_ref` where template_id = ?";
    private static final Integer SCRIPT_TIMEOUT = 1800000;
    private static final Integer LOCK_WAIT_TIMEOUT = 1200;
    public static String CS_MAJOR_VERSION = "4.16";
    public static String CS_TINY_VERSION = "0";

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

        SystemVMTemplateDetails() {
        }

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
    public static final Map<Hypervisor.HypervisorType, String> fileNames = new HashMap<Hypervisor.HypervisorType, String>();
    public static final Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>();
    public static final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>();

    public static final Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>() {
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
            put(Hypervisor.HypervisorType.KVM, 15);
            put(Hypervisor.HypervisorType.XenServer, 99);
            put(Hypervisor.HypervisorType.VMware, 99);
            put(Hypervisor.HypervisorType.Hyperv, 15);
            put(Hypervisor.HypervisorType.LXC, 15);
            put(Hypervisor.HypervisorType.Ovm3, 183);
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
        try {
            mountStore(url);
            int lastIdx = path.lastIndexOf(File.separator);
            String partialDirPath = path.substring(0, lastIdx);
            String templatePath = TEMPORARY_SECONDARY_STORE + File.separator + partialDirPath;
            File templateProps = new File(templatePath + "/template.properties");
            if (templateProps.exists()) {
                LOGGER.info("SystemVM template already seeded, skipping registration");
                return true;
            }
            LOGGER.info("SystemVM template not seeded");
            return false;
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to verify if the template is seeded", e);
        } finally {
            unmountStore();
        }
    }

    private static String calculateChecksum(MessageDigest digest, File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            fis.close();
            byte[] bytes = digest.digest();

            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer
                        .toString((aByte & 0xff) + 0x100, 16)
                        .substring(1));
            }
            return sb.toString();
        } catch (IOException e) {
            String errMsg = String.format("Failed to calculate Checksum of template file: %s ", file.getName());
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    public static long isTemplateAlreadyRegistered(Connection conn, Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
        long templateId = -1;
        try {
            PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1");
            // Get systemvm template id for corresponding hypervisor
            pstmt.setString(1, hypervisorAndTemplateName.second());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    templateId = rs.getLong(1);
                }
            } catch (final SQLException e) {
                String errMsg = "updateSystemVmTemplates: Exception caught while getting ids of SystemVM templates";
                LOGGER.error(errMsg, e);
                throw new CloudRuntimeException(errMsg, e);
            }
        } catch (SQLException e) {
            String errorMessage = "Unable to upgrade the database";
            LOGGER.error(errorMessage, e);
            throw new CloudRuntimeException(errorMessage, e);
        }
        return templateId;
    }

    private static String fetchTemplatesPath() {
            String filePath = relativeTemplatePath + metadataFileName;
            LOGGER.debug(String.format("Looking for file [ %s ] in the classpath.", filePath));
            File metaFile = new File(filePath);
            String templatePath = null;
            if (metaFile.exists()) {
                templatePath = relativeTemplatePath;
            }
            if (templatePath == null) {
                filePath = AbsolutetemplatesPath + metadataFileName;
                metaFile = new File(filePath);
                templatePath = AbsolutetemplatesPath;
                LOGGER.debug(String.format("Looking for file [ %s ] in the classpath.", filePath));
                if (!metaFile.exists()) {
                    String errMsg = String.format("Unable to locate metadata file in your setup at %s", filePath.toString());
                    LOGGER.error(errMsg);
                    throw new CloudRuntimeException(errMsg);
                }
            }
        return templatePath;
    }

    private static String getHypervisorName(String name) {
        if (name.equals("xenserver")) {
            return "xen";
        }
        if (name.equals("ovm3")) {
            return "ovm";
        }
        return name;

    }

    private static Hypervisor.HypervisorType getHypervisorType(String hypervisor) {
        if (hypervisor.equalsIgnoreCase("xen")) {
            hypervisor = "xenserver";
        } else if (hypervisor.equalsIgnoreCase("ovm")) {
            hypervisor = "ovm3";
        }
        return Hypervisor.HypervisorType.getType(hypervisor);
    }

    private static List<Long> getEligibleZoneIds(Connection conn) {
        List<Long> zones = new ArrayList<Long>();
        try {
            PreparedStatement pstmt = conn.prepareStatement(FETCH_DISTINCT_ELIGIBLE_ZONES);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                zones.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            String errMsg = "Failed to fetch eligible zones for SystemVM template registration";
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
        return zones;
    }

    private static Pair<String, Long> getNfsStoreInZone(Connection conn, Long zoneId) {
        String url = null;
        Long storeId = null;
        try {
            PreparedStatement pstmt = conn.prepareStatement(FETCH_IMAGE_STORE_PER_ZONE);
            if(pstmt != null) {
                pstmt.setLong(1, zoneId);
                ResultSet resultSet = pstmt.executeQuery();
                while (resultSet.next()) {
                    url = resultSet.getString(1);
                    storeId = resultSet.getLong(2);
                }
            }
        } catch (SQLException e) {
            String errMsg = String.format("Failed to fetch NFS store in zone = %s for SystemVM template registration", zoneId);
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
        return new Pair<>(url, storeId);
    }

    public static void mountStore(String storeUrl) {
        try {
            if (storeUrl != null) {
                URI uri = new URI(UriUtils.encodeURIComponent(storeUrl));
                String host = uri.getHost();
                String mountPath = uri.getPath();
                Script.runSimpleBashScript("mkdir -p " + TEMPORARY_SECONDARY_STORE);
                String mount = String.format(mountCommand, host + ":" + mountPath, TEMPORARY_SECONDARY_STORE);
                Script.runSimpleBashScript(mount);
            }
        } catch (Exception e) {
            String msg = "NFS Store URL is not in the correct format";
            LOGGER.error(msg, e);
            throw new CloudRuntimeException(msg, e);

        }
    }

    private static List<String> fetchAllHypervisors(Connection conn, Long zoneId) {
        List<String> hypervisorList = new ArrayList<>();
        try {
            PreparedStatement pstmt = conn.prepareStatement(FETCH_DISTINCT_HYPERVISORS_IN_ZONE);
            if(pstmt != null) {
                pstmt.setLong(1, zoneId);
                ResultSet resultSet = pstmt.executeQuery();
                while (resultSet.next()) {
                    hypervisorList.add(resultSet.getString(1));
                }
            }
        } catch (SQLException e) {
            String errMsg = String.format("Failed to fetch distinct hypervisors in zone: %s for SystemVM template registration", zoneId);
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
        return hypervisorList;
    }

    private static Long createTemplateObjectInDB(Connection conn, SystemVMTemplateDetails details) {
        Long id = null;
        try {
            PreparedStatement pstmt = conn.prepareStatement(INSERT_VM_TEMPLATE_TABLE);
            if (pstmt != null) {
                int i = 1;
                pstmt.setString(i++, details.getUuid());
                pstmt.setString(i++, details.getUuid());
                pstmt.setString(i++, details.getName());
                pstmt.setDate(i++, details.getCreated());
                pstmt.setString(i++, details.getUrl());
                pstmt.setString(i++, details.getChecksum());
                pstmt.setString(i++, details.getName());
                pstmt.setString(i++, details.getFormat().toString());
                pstmt.setLong(i++, details.getGuestOsId());
                pstmt.setString(i++, details.getHypervisorType().toString());
                pstmt.setBoolean(i++, details.getHypervisorType() == Hypervisor.HypervisorType.VMware);
                pstmt.executeUpdate();

                pstmt = conn.prepareStatement("SELECT id FROM vm_template ORDER BY id DESC LIMIT 1");
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getLong(1);
                    }
                } catch (final SQLException e) {
                    String errMsg = "Failed to fetch template id ";
                    LOGGER.error(errMsg, e);
                    throw new CloudRuntimeException(errMsg, e);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to create vm_template record for the systemVM template for hypervisor: %s", details.getHypervisorType().name()), e);
        }
        return id;
    }

    private static void createTemplateStoreRefEntry(Connection conn, SystemVMTemplateDetails details) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(INSERT_TEMPLATE_STORE_REF_TABLE);
            if (pstmt != null) {
                int i = 1;
                pstmt.setLong(i++, details.getStoreId());
                pstmt.setLong(i++, details.getId());
                pstmt.setDate(i++, details.getCreated());
                pstmt.setDate(i++, details.getCreated());
                pstmt.setString(i++, details.getInstallPath());
                pstmt.setString(i++, details.getUrl());
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to create template_store_ref record for the systemVM template for hypervisor: %s", details.getHypervisorType().name()), e);
        }
    }

    public static void updateDb(Connection conn,  SystemVMTemplateDetails details) {
        try {
            int i = 1;
            PreparedStatement pstmt = conn.prepareStatement(UPDATE_VM_TEMPLATE_ENTRY);
            if (pstmt != null) {
                pstmt.setLong(i++, details.getSize());
                pstmt.setLong(i++, details.getId());
                pstmt.executeUpdate();
            }
            i = 1;
            pstmt = conn.prepareStatement(UPDATE_TEMPLATE_STORE_REF_TABLE);
            if (pstmt != null) {
                pstmt.setLong(i++, details.getSize());
                pstmt.setLong(i++, details.getPhysicalSize());
                pstmt.setDate(i++, details.getUpdated());
                pstmt.setDate(i++, details.getUpdated());
                pstmt.setLong(i++, details.getId());
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to update template_store_ref record for the systemVM template registered for hypervisor: %s", details.getHypervisorType().name()), e);
        }
    }

    public static void updateSystemVMEntries(Connection conn, Long templateId, Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
        // update template ID of system Vms
        try {
            PreparedStatement update_templ_id_pstmt = conn
                    .prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ? and removed is NULL");
            update_templ_id_pstmt.setLong(1, templateId);
            update_templ_id_pstmt.setString(2, hypervisorAndTemplateName.first().toString());
            update_templ_id_pstmt.executeUpdate();
        } catch (SQLException e) {
            String errMsg = String.format("updateSystemVmTemplates:Exception while setting template for %s to %s",hypervisorAndTemplateName.first().toString(), templateId);
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    public static void updateConfigurationParams(Connection conn, Map<String, String> configParams) {
        String key = null;
        String value = null;
        try {
            PreparedStatement pstmt = conn.prepareStatement(UPDATE_CONFIGURATION_TABLE);
            for (Map.Entry<String, String> config : configParams.entrySet()) {
                key = config.getKey();
                value = config.getValue();
                pstmt.setString(1, value);
                pstmt.setString(2, key);
                pstmt.executeUpdate();
            }

        } catch (final SQLException e) {
            String errMsg = String.format("updateSystemVmTemplates: Exception while setting %s to %s ", key, value);
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
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

    private static  void updateTemplateTablesOnFailure(Connection conn, long templateId) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(UPDATE_TEMPLATE_TABLE_ON_FAILURE);
            if (pstmt != null) {
                Date removedTime = new Date(DateUtil.currentGMTTime().getTime());
                pstmt.setDate(1, removedTime);
                pstmt.setLong(2, templateId);
                pstmt.executeUpdate();
            }

            PreparedStatement pstmt1 = conn.prepareStatement(DELETE_TEMPLATE_REF_RECORD_ON_FAILURE);
            if (pstmt1 != null) {
                pstmt1.setLong(1, templateId);
                pstmt1.executeUpdate();
            }
        } catch (Exception e) {
            String errMsg = "updateSystemVmTemplates: Exception while updating vm_template and template_store_ref tables on failure";
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    public static void unmountStore() {
        try {
            LOGGER.info("Unmounting store");
            String umountCmd = String.format(umountCommand, TEMPORARY_SECONDARY_STORE);
            Script.runSimpleBashScript(umountCmd);
        } catch (Exception e) {
            String msg = String.format("Failed to unmount store mounted at %s", TEMPORARY_SECONDARY_STORE);
            LOGGER.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }
    public static void registerTemplate(Connection conn, Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName, Pair<String, Long> storeUrlAndId) {
        Long templateId = null;
        try {
            Hypervisor.HypervisorType hypervisor = hypervisorAndTemplateName.first();
            mountStore(storeUrlAndId.first());
            final String templateName = UUID.randomUUID().toString();
            Date created = new Date(DateUtil.currentGMTTime().getTime());
            SystemVMTemplateDetails details = new SystemVMTemplateDetails(templateName, NewTemplateNameList.get(hypervisor), created,
                    newTemplateUrl.get(hypervisor), newTemplateChecksum.get(hypervisor), hypervisorImageFormat.get(hypervisor), hypervisorGuestOsMap.get(hypervisor), hypervisor, storeUrlAndId.second());
            templateId = createTemplateObjectInDB(conn, details);
            if (templateId == null) {
                throw new CloudRuntimeException(String.format("Failed to register template for hypervisor: %s", hypervisor.name()));
            }
            details.setId(templateId);
            String destTempFolderName = String.valueOf(templateId);
            String destTempFolder = PARENT_TEMPLATE_FOLDER + PARTIAL_TEMPLATE_FOLDER + destTempFolderName;
            details.setInstallPath(PARTIAL_TEMPLATE_FOLDER + destTempFolderName + File.separator + templateName + "." + hypervisorImageFormat.get(hypervisor).getFileExtension());
            createTemplateStoreRefEntry(conn, details);
            String storageScriptsDir = "scripts/storage/secondary";
            String setupTmpltScript = Script.findScript(storageScriptsDir, "setup-sysvm-tmplt");
            if (setupTmpltScript == null) {
                throw new ConfigurationException("Unable to find the createtmplt.sh");
            }
            Script scr = new Script(setupTmpltScript, SCRIPT_TIMEOUT, LOGGER);
            scr.add("-u", templateName);
            scr.add("-f", templatesPath + fileNames.get(hypervisorAndTemplateName.first()));
            scr.add("-h", hypervisorAndTemplateName.first().name().toLowerCase(Locale.ROOT));
            scr.add("-d", destTempFolder);
            String result = scr.execute();
            if (result != null) {
                String errMsg = String.format("failed to create template: %s ", result);
                LOGGER.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
            details.setInstallPath(PARTIAL_TEMPLATE_FOLDER + destTempFolderName + File.separator + templateName + "." + hypervisorImageFormat.get(hypervisor).getFileExtension());
            readTemplateProperties(destTempFolder + "/template.properties", details);
            details.setUpdated(new Date(DateUtil.currentGMTTime().getTime()));
            updateDb(conn, details);
            Map<String, String> configParams = new HashMap<>();
            configParams.put(SystemVmTemplateRegistration.routerTemplateConfigurationNames.get(hypervisorAndTemplateName.first()), hypervisorAndTemplateName.second());
            configParams.put("minreq.sysvmtemplate.version", CS_MAJOR_VERSION + "." + CS_TINY_VERSION);
            updateConfigurationParams(conn, configParams);
            updateSystemVMEntries(conn, templateId, hypervisorAndTemplateName);
        } catch (Exception e) {
            String errMsg = String.format("Failed to register template for hypervisor: %s", hypervisorAndTemplateName.first());
            LOGGER.error(errMsg, e);
            if (templateId != null) {
                updateTemplateTablesOnFailure(conn, templateId);
                cleanupStore(templateId);
            }
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    public static void parseMetadataFile() {
        try {
            Ini ini = new Ini();
            ini.load(new FileReader(metadataFile));
            for (Hypervisor.HypervisorType hypervisorType : hypervisorList) {
                String hypervisor = hypervisorType.name().toLowerCase(Locale.ROOT);
                Ini.Section section = ini.get(hypervisor);
                NewTemplateNameList.put(hypervisorType, section.get("templatename"));
                fileNames.put(hypervisorType, section.get("filename"));
                newTemplateChecksum.put(hypervisorType, section.get("checksum"));
                newTemplateUrl.put(hypervisorType, section.get("downloadurl"));
            }
        } catch (Exception e) {
            String errMsg = String.format("Failed to parse systemVM template metadata file: %s", metadataFile);
            LOGGER.error(errMsg, e);
            throw new CloudRuntimeException(errMsg, e);
        }
    }

    private static void cleanupStore(Long templateId) {
        String destTempFolder = PARENT_TEMPLATE_FOLDER + PARTIAL_TEMPLATE_FOLDER + String.valueOf(templateId);
        Script.runSimpleBashScript("rm -rf " + destTempFolder);
    }

    public static void registerTemplates(Connection conn, Set<Hypervisor.HypervisorType> hypervisorsInUse) {
        GlobalLock lock = GlobalLock.getInternLock("UpgradeDatabase-Lock");
        try {
            LOGGER.info("Grabbing lock to register templates.");
            if (!lock.lock(LOCK_WAIT_TIMEOUT)) {
                throw new CloudRuntimeException("Unable to acquire lock to register SystemVM template.");
            }
            // Check if templates path exists
            try {
                Set<String> hypervisors = hypervisorsInUse.stream().map(Enum::name).
                        map(name -> name.toLowerCase(Locale.ROOT)).map(SystemVmTemplateRegistration::getHypervisorName).collect(Collectors.toSet());
                List<String> templates = new ArrayList<>();
                for (Hypervisor.HypervisorType hypervisorType : hypervisorsInUse) {
                    templates.add(fileNames.get(hypervisorType));
                }

                boolean templatesFound = true;
                for (String hypervisor : hypervisors) {
                    String matchedTemplate = templates.stream().filter(x -> x.contains(hypervisor)).findAny().orElse(null);
                    if (matchedTemplate == null) {
                        templatesFound = false;
                        break;
                    }
                    MessageDigest mdigest = MessageDigest.getInstance(hashAlgorithm);
                    File tempFile = new File(templatesPath + matchedTemplate);
                    String templateChecksum = calculateChecksum(mdigest, tempFile);
                    if (!templateChecksum.equals(newTemplateChecksum.get(getHypervisorType(hypervisor)))) {
                        LOGGER.error(String.format("Checksum mismatch: %s != %s ", templateChecksum, newTemplateChecksum.get(getHypervisorType(hypervisor))));
                        templatesFound = false;
                        break;
                    }
                }

                if (!templatesFound) {
                    String errMsg = "SystemVm template not found. Cannot upgrade system Vms";
                    LOGGER.error(errMsg);
                    throw new CloudRuntimeException(errMsg);
                }

                // Perform Registration if templates not already registered
                List<Long> zoneIds = getEligibleZoneIds(conn);
                for (Long zoneId : zoneIds) {
                    Pair<String, Long> storeUrlAndId = getNfsStoreInZone(conn, zoneId);
                    List<String> hypervisorList = fetchAllHypervisors(conn, zoneId);
                    for (String hypervisor : hypervisorList) {
                        Hypervisor.HypervisorType name = Hypervisor.HypervisorType.getType(hypervisor);
                        String templateName = NewTemplateNameList.get(name);
                        Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName = new Pair<Hypervisor.HypervisorType, String>(name, templateName);
                        long templateId = isTemplateAlreadyRegistered(conn, hypervisorAndTemplateName);
                        if (templateId != -1) {
                            continue;
                        }
                        registerTemplate(conn, hypervisorAndTemplateName, storeUrlAndId);
                    }
                    unmountStore();
                }
            } catch (Exception e) {
                unmountStore();
                throw new CloudRuntimeException("Failed to register systemVM template. Upgrade Failed");
            }
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }
}
