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
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    private static final String templatesPath = "/usr/share/cloudstack-management/templates/";
    private static final String TEMPORARY_SECONDARY_STORE = "/tmp/tmpSecStorage";
    private static final String PARENT_TEMPLATE_FOLDER = TEMPORARY_SECONDARY_STORE;
    private static final String PARTIAL_TEMPLATE_FOLDER = "/template/tmpl/1/";
    private static final String FETCH_FOLDER_NAME = "SELECT id FROM vm_template ORDER BY id DESC LIMIT 1;";
    private static final String FETCH_DISTINCT_ELIGIBLE_ZONES = "SELECT DISTINCT(data_center_id) FROM `cloud`.`image_store` WHERE protocol = \"nfs\" AND removed is null";
    private static final String FETCH_DISTINCT_HYPERVISORS_IN_ZONE = "SELECT DISTINCT(hypervisor_type) FROM `cloud`.`cluster` where removed is null and data_center_id=?";
    private static final String FETCH_IMAGE_STORE_PER_ZONE = "SELECT url,id FROM `cloud`.`image_store` WHERE data_center_id=? AND removed IS NULL LIMIT 1";
    private static final String UPDATE_VM_TEMPLATE_TABLE = "INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, featured, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, cross_zones, hypervisor_type, state)" +
            "VALUES (?, ?, ?, ?, 0, 0, ?, 'SYSTEM', 0, 64, 1, ?, ?, 0, ?, ?, ?, 1, ?, 'Active')";
    private static final String UPDATE_TEMPLATE_STORE_REF_TABLE = "INSERT INTO `cloud`.`template_store_ref` (store_id,  template_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, error_str, local_path, install_path, url, state, destroyed, is_copy," +
            " update_count, ref_cnt, store_role) VALUES (?, ?, ?, ?, NULL, 100, ?, ?, 'DOWNLOADED', NULL, NULL, ?, ?, 'READY', 0, 0, 0, 0, 'Image')";
    private static final String UPDATE_CONFIGURATION_TABLE = "UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?";
    public static final String CS_MAJOR_VERSION = "4.16";
    public static final String CS_MINOR_VERSION = "0";


    private static class SystemVMTemplateDetails {
        Long id;
        String uuid;
        String name;
        String uniqueName;
        Date created;
        String url;
        String checksum;
        TemplateFormat format;
        Integer guestOsId;
        Hypervisor.HypervisorType hypervisorType;
        Long storeId;
        Long size;
        Long physicalSize;
        String installPath;

        SystemVMTemplateDetails() {
        }

        SystemVMTemplateDetails(Long id, String uuid, String name, Date created, String url, String checksum,
                                TemplateFormat format, Integer guestOsId, Hypervisor.HypervisorType hypervisorType,
                                Long storeId) {
            this.id = id;
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

        public TemplateFormat getFormat() {
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
    }

    public static final Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-" + CS_MAJOR_VERSION + "." + CS_MINOR_VERSION);
            put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-" + CS_MAJOR_VERSION + "." + CS_MINOR_VERSION);
            put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-" + CS_MAJOR_VERSION + "." + CS_MINOR_VERSION);
            put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-" + CS_MAJOR_VERSION + "." + CS_MINOR_VERSION);
            put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-" + CS_MAJOR_VERSION + "." + CS_MINOR_VERSION);
            put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-" + CS_MAJOR_VERSION + "." + CS_MINOR_VERSION);
        }
    };

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

    public static final Map<Hypervisor.HypervisorType, String> fileNames = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "systemvmtemplate-4.16.0-kvm.qcow2.bz2");
            put(Hypervisor.HypervisorType.VMware, "systemvmtemplate-4.16.0-vmware.ova");
            put(Hypervisor.HypervisorType.XenServer, "systemvmtemplate-4.16.0-xen.vhd.bz2");
            put(Hypervisor.HypervisorType.Hyperv, "systemvmtemplate-4.16.0-hyperv.vhd.zip");
            put(Hypervisor.HypervisorType.LXC, "systemvmtemplate-4.16.0-kvm.qcow2.bz2");
            put(Hypervisor.HypervisorType.Ovm3, "systemvmtemplate-4.16.0-ovm.raw.bz2");
        }
    };

    public static final Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "https://download.cloudstack.org/systemvm/4.16/" + fileNames.get(Hypervisor.HypervisorType.KVM));
            put(Hypervisor.HypervisorType.VMware, "https://download.cloudstack.org/systemvm/4.16/" + fileNames.get(Hypervisor.HypervisorType.VMware));
            put(Hypervisor.HypervisorType.XenServer, "https://download.cloudstack.org/systemvm/4.16/" + fileNames.get(Hypervisor.HypervisorType.XenServer));
            put(Hypervisor.HypervisorType.Hyperv, "https://download.cloudstack.org/systemvm/4.16/" + fileNames.get(Hypervisor.HypervisorType.Hyperv));
            put(Hypervisor.HypervisorType.LXC, "https://download.cloudstack.org/systemvm/4.16/" + fileNames.get(Hypervisor.HypervisorType.LXC));
            put(Hypervisor.HypervisorType.Ovm3, "https://download.cloudstack.org/systemvm/4.16/" + fileNames.get(Hypervisor.HypervisorType.Ovm3));
        }
    };

    public static final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "07268f267dc4316dc5f86150346bb8d7");
            put(Hypervisor.HypervisorType.XenServer, "71d8adb40baa609997acdc3eae15fbde");
            put(Hypervisor.HypervisorType.VMware, "b356cbbdef67c4eefa8c336328e2b202");
            put(Hypervisor.HypervisorType.Hyperv, "0982aa1461800ce1538e0cae07e00770");
            put(Hypervisor.HypervisorType.LXC, "07268f267dc4316dc5f86150346bb8d7");
            put(Hypervisor.HypervisorType.Ovm3, "8c643d146c82f92843b8a48c7661f800");
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

    public static enum TemplateFormat{
        QCOW2("qcow2"),
        RAW("raw"),
        VHD("vhd"),
        OVA("ova");

        private final String fileExtension;

        TemplateFormat(String fileExtension) {
            this.fileExtension = fileExtension;
        }
    }

    public static final Map<Hypervisor.HypervisorType, TemplateFormat> hypervisorImageFormat = new HashMap<Hypervisor.HypervisorType, TemplateFormat>() {
        {
            put(Hypervisor.HypervisorType.KVM, TemplateFormat.QCOW2);
            put(Hypervisor.HypervisorType.XenServer, TemplateFormat.VHD);
            put(Hypervisor.HypervisorType.VMware, TemplateFormat.OVA);
            put(Hypervisor.HypervisorType.Hyperv, TemplateFormat.VHD);
            put(Hypervisor.HypervisorType.LXC, TemplateFormat.QCOW2);
            put(Hypervisor.HypervisorType.Ovm3, TemplateFormat.RAW);
        }
    };

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
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    static long isTemplateAlreadyRegistered(Connection conn, Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
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
                String errMsg = String.format("updateSystemVmTemplates: Exception caught while getting ids of SystemVM templates: %s ", e.getMessage());
                LOGGER.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (SQLException e) {
            String errorMessage = "Unable to upgrade the database";
            LOGGER.error(errorMessage, e);
            throw new CloudRuntimeException(errorMessage, e);
        }
        return templateId;
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
            throw new CloudRuntimeException("Failed to fetch eligible zones for SystemVM template registration");
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
            throw new CloudRuntimeException("Failed to fetch eligible zones for SystemVM template registration");
        }
        return new Pair<>(url, storeId);
    }

    public static void mountStore(String storeUrl) {
        try {
            if (storeUrl != null) {
                String path = storeUrl.split("://")[1];
                int index = path.indexOf('/');
                String host = path.substring(0, index);
                String mountPath = path.substring(index);
                Script.runSimpleBashScript("mkdir -p " + TEMPORARY_SECONDARY_STORE);
                String mount = String.format(mountCommand, host + ":" + mountPath, TEMPORARY_SECONDARY_STORE);
                Script.runSimpleBashScript(mount);
            }
        } catch (Exception e) {
            String msg = "NFS Store URL is not in the correct format";
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);

        }
    }
    private static String getTemplateFolderName(Connection conn) {
        Long templateId = null;
        try {
            PreparedStatement pstmt = conn.prepareStatement(FETCH_FOLDER_NAME);
            if(pstmt != null) {
                ResultSet resultSet = pstmt.executeQuery();
                while (resultSet.next()) {
                    templateId = resultSet.getLong(1);
                }
            }
            templateId += 1L;
        } catch (SQLException e) {
            String errMsg = "Failed to get folder name";
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        return String.valueOf(templateId);
    }

    private static String getTemplateFolder(Connection conn) {
        String folderName = getTemplateFolderName(conn);
        if (folderName != null || !folderName.equals(0)) {
            return folderName;
        } else {
            return "202";
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
            throw new CloudRuntimeException("Failed to fetch eligible zones for SystemVM template registration");
        }
        return hypervisorList;
    }

    public static void updateDb(Connection conn, SystemVMTemplateDetails details) {
        try {
            PreparedStatement pstmt = conn.prepareStatement(UPDATE_VM_TEMPLATE_TABLE);
            if (pstmt != null) {
                pstmt.setLong(1, details.getId());
                pstmt.setString(2, details.getUuid());
                pstmt.setString(3, details.getUniqueName());
                pstmt.setString(4, details.getName());
                pstmt.setDate(5, details.getCreated());
                pstmt.setString(6, details.getUrl());
                pstmt.setString(7, details.getChecksum());
                pstmt.setString(8, details.getName());
                pstmt.setString(9, details.getFormat().toString());
                pstmt.setLong(10, details.getGuestOsId());
                pstmt.setString(11, details.getHypervisorType().toString());
                pstmt.executeUpdate();
            }

            PreparedStatement pstmt1 = conn.prepareStatement(UPDATE_TEMPLATE_STORE_REF_TABLE);
            if (pstmt1 != null) {
                pstmt1.setLong(1, details.getStoreId());
                pstmt1.setLong(2, details.getId());
                pstmt1.setDate(3, details.getCreated());
                pstmt1.setDate(4, details.getCreated());
                pstmt1.setLong(5, details.getSize());
                pstmt1.setLong(6, details.getPhysicalSize());
                pstmt1.setString(7, details.getInstallPath());
                pstmt1.setString(8, details.getUrl());
                pstmt1.executeUpdate();
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to fetch eligible zones for SystemVM template registration: " + e.getMessage());
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
            String errMsg = String.format("updateSystemVmTemplates:Exception while setting template for %s to %s : %s",hypervisorAndTemplateName.first().toString(), templateId,
                    e.getMessage());
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
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
            String errMsg = String.format("updateSystemVmTemplates: Exception while setting %s to %s: %s ", key, value, e.getMessage());
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    private static void readTemplateProperties(String path, SystemVMTemplateDetails details) {
        File tmpFile = new File(path);
        String uniqName = null;
        Long size = null;
        Long physicalSize = 0L;
        try (FileReader fr = new FileReader(tmpFile); BufferedReader brf = new BufferedReader(fr);) {
            String line = null;
            while ((line = brf.readLine()) != null) {
                if (line.startsWith("uniquename=")) {
                    uniqName = line.split("=")[1];
                } else if (line.startsWith("size=")) {
                    physicalSize = Long.parseLong(line.split("=")[1]);
                } else if (line.startsWith("virtualsize=")) {
                    size = Long.parseLong(line.split("=")[1]);
                }
                if (size == null) {
                    size = physicalSize;
                }
            }
        } catch (IOException ex) {
            LOGGER.debug(String.format("Failed to read from template.properties due to: %s ", ex.getMessage()));
        }
        details.setSize(size);
        details.setPhysicalSize(physicalSize);
        details.setUniqueName(uniqName);
    }

    private static void unmountStore() {
        try {
            LOGGER.info("Unmounting store");
            String umountCmd = String.format(umountCommand, TEMPORARY_SECONDARY_STORE);
            Script.runSimpleBashScript(umountCmd);
        } catch (Exception e) {
            String msg = String.format("Failed to unmount store mounted at %s", TEMPORARY_SECONDARY_STORE);
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }
    public static void registerTemplate(Connection conn, Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName, Long zoneId) {
        try {
            Hypervisor.HypervisorType hypervisor = hypervisorAndTemplateName.first();
            Pair<String, Long> storeUrlAndId = getNfsStoreInZone(conn, zoneId);
            mountStore(storeUrlAndId.first());
            String destTempFolderName = getTemplateFolder(conn);
            String destTempFolder = PARENT_TEMPLATE_FOLDER + PARTIAL_TEMPLATE_FOLDER + destTempFolderName;
            Script.runSimpleBashScript("mkdir -p " + destTempFolder);
            String storageScriptsDir = "scripts/storage/secondary";
            String setupTmpltScript = Script.findScript(storageScriptsDir, "setup-sysvm-tmplt");
            if (setupTmpltScript == null) {
                throw new ConfigurationException("Unable to find the createtmplt.sh");
            }

            Script scr = new Script(setupTmpltScript, 120000, LOGGER);
            final String templateName = UUID.randomUUID().toString();
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
            Date created = new Date(DateUtil.currentGMTTime().getTime());
            SystemVMTemplateDetails details = new SystemVMTemplateDetails(Long.parseLong(destTempFolderName), templateName, NewTemplateNameList.get(hypervisor), created,
                    newTemplateUrl.get(hypervisor), newTemplateChecksum.get(hypervisor), hypervisorImageFormat.get(hypervisor), hypervisorGuestOsMap.get(hypervisor), hypervisor, storeUrlAndId.second());
            details.setInstallPath(PARTIAL_TEMPLATE_FOLDER + destTempFolderName + "/" + templateName + "." + hypervisorImageFormat.get(hypervisor).fileExtension);
            readTemplateProperties(destTempFolder + "/template.properties", details);
            updateDb(conn, details);
            Map<String, String> configParams = new HashMap<>();
            configParams.put(SystemVmTemplateRegistration.routerTemplateConfigurationNames.get(hypervisorAndTemplateName.first()), hypervisorAndTemplateName.second());
            configParams.put("minreq.sysvmtemplate.version", CS_MAJOR_VERSION + "." + CS_MINOR_VERSION);
            updateConfigurationParams(conn, configParams);
            updateSystemVMEntries(conn, Long.valueOf(destTempFolderName), hypervisorAndTemplateName);
        } catch (Exception e) {
            String errMsg = String.format("Failed to register template for hypervisor: %s ", hypervisorAndTemplateName.first());
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    public static void registerTemplates(Connection conn, Set<Hypervisor.HypervisorType> hypervisorsInUse) {
        GlobalLock lock = GlobalLock.getInternLock("UpgradeDatabase-Lock");
        try {
            LOGGER.info("Grabbing lock to register templates.");
            if (!lock.lock(20 * 60)) {
                throw new CloudRuntimeException("Unable to acquire lock to register systemvm template.");
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
                    LOGGER.info("SystemVm template not found. Cannot upgrade system Vms");
                    throw new CloudRuntimeException("SystemVm template not found. Cannot upgrade system Vms");
                }

                // Perform Registration if templates not already registered
                List<Long> zoneIds = getEligibleZoneIds(conn);
                for (Long zoneId : zoneIds) {
                    List<String> hypervisorList = fetchAllHypervisors(conn, zoneId);
                    for (String hypervisor : hypervisorList) {
                        Hypervisor.HypervisorType name = Hypervisor.HypervisorType.getType(hypervisor);
                        String templateName = NewTemplateNameList.get(name);
                        Pair<Hypervisor.HypervisorType, String> hypervisorAndTemplateName = new Pair<Hypervisor.HypervisorType, String>(name, templateName);
                        long templateId = isTemplateAlreadyRegistered(conn, hypervisorAndTemplateName);
                        if (templateId != -1) {
                            continue;
                        }
                        registerTemplate(conn, hypervisorAndTemplateName, zoneId);
                    }
                    unmountStore();
                }
            } catch (Exception e) {
                throw new CloudRuntimeException("Failed to register systemVM template. Upgrade Failed");
            }
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }
}
