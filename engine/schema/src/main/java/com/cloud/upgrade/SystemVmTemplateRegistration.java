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
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SystemVmTemplateRegistration {
    private static final Logger LOGGER = Logger.getLogger(SystemVmTemplateRegistration.class);
    private static final String mountCommand = "mount -t nfs %s %s";
    private static final String templatesPath = "/usr/share/cloudstack-management/templates";
    private static final String TEMPORARY_SECONDARY_STORE = "/tmp/tmpSecStorage";
    private static final String FETCH_CLOUDSTACK_VERSION = "SELECT version FROM version ORDER BY id DESC LIMIT 1";
    private static final String FETCH_DISTINCT_ELIGILBLE_ZONES = "SELECT DISTINCT(data_center_id) FROM image_store WHERE protocol = \"nfs\" AND removed is null";
    private static final String FETCH_IMAGE_STORE_PER_ZONE = "SELECT url FROM image_store WHERE data_center_id=? AND removed IS NULL LIMIT 1";
    private static final String CS_MAJOR_VERSION = "4.16";
    private static final String CS_MINOR_VERSION = "0";


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
            put(Hypervisor.HypervisorType.KVM, "81b3e48bb934784a13555a43c5ef5ffb");
            put(Hypervisor.HypervisorType.XenServer, "1b178a5dbdbe090555515340144c6017");
            put(Hypervisor.HypervisorType.VMware, "e6a88e518c57d6f36c096c4204c3417f");
            put(Hypervisor.HypervisorType.Hyperv, "5c94da45337cf3e1910dcbe084d4b9ad");
            put(Hypervisor.HypervisorType.LXC, "81b3e48bb934784a13555a43c5ef5ffb");
            put(Hypervisor.HypervisorType.Ovm3, "875c5c65455fc06c4a012394410db375");
        }
    };

    public static final Map<Hypervisor.HypervisorType, String> templateFiles = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, templatesPath + "/" + fileNames.get(Hypervisor.HypervisorType.KVM));
            put(Hypervisor.HypervisorType.XenServer, templatesPath + "/" + fileNames.get(Hypervisor.HypervisorType.XenServer));
            put(Hypervisor.HypervisorType.VMware, templatesPath + "/" + fileNames.get(Hypervisor.HypervisorType.VMware));
            put(Hypervisor.HypervisorType.Hyperv, templatesPath + "/" + fileNames.get(Hypervisor.HypervisorType.Hyperv));
            put(Hypervisor.HypervisorType.LXC, templatesPath + "/" + fileNames.get(Hypervisor.HypervisorType.LXC));
            put(Hypervisor.HypervisorType.Ovm3, templatesPath + "/" + fileNames.get(Hypervisor.HypervisorType.Ovm3));
        }
    };


    static long isTemplateAlreadyRegistered(Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
        final TransactionLegacy txn = TransactionLegacy.open("TemplateValidation");
        long templateId = -1;
        Connection conn;
        try {
            conn = txn.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1");
            // Get systemvm template id for corresponding hypervisor
            pstmt.setString(1, hypervisorAndTemplateName.getValue());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    templateId = rs.getLong(1);
                }
            } catch (final SQLException e) {
                LOGGER.error("updateSystemVmTemplates: Exception caught while getting ids of SystemVM templates: " + e.getMessage());
                throw new CloudRuntimeException("updateSystemVmTemplates: Exception caught while getting ids of SystemVM templates", e);
            }
        } catch (SQLException e) {
            String errorMessage = "Unable to upgrade the database";
            LOGGER.error(errorMessage, e);
            throw new CloudRuntimeException(errorMessage, e);
        } finally {
            txn.close();
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

    private static List<Long> getEligibleZoneIds() {
        final TransactionLegacy txn = TransactionLegacy.open("FetchZones");
        List<Long> zones = new ArrayList<Long>();
        Connection conn;
        try {
            conn = txn.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(FETCH_DISTINCT_ELIGILBLE_ZONES);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                zones.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to fetch eligible zones for SystemVM template registration");
        } finally {
            txn.close();
        }
        return zones;
    }

    private static String getNfsStoreInZone(Long zoneId) {
        final TransactionLegacy txn = TransactionLegacy.open("FetchStore");
        String url = null;
        Connection conn;
        try {
            conn = txn.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(FETCH_IMAGE_STORE_PER_ZONE);
            if(pstmt != null) {
                pstmt.setLong(1, zoneId);
                ResultSet resultSet = pstmt.executeQuery();
                while (resultSet.next()) {
                    url = resultSet.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to fetch eligible zones for SystemVM template registration");
        } finally {
            txn.close();
        }
        return url;
    }

    public static void mountStore(String storeUrl) {
        try {
            if (storeUrl != null) {
                String path = storeUrl.split("://")[1];
                int index = path.indexOf('/');
                String host = path.substring(0, index);
                String mountPath = path.substring(index);
                String mount = String.format(mountCommand, host + ":" + mountPath, TEMPORARY_SECONDARY_STORE);
                Script.runSimpleBashScript(mount);
            }
        } catch (Exception e) {
            String msg = "NFS Store URL is not in the correct format";
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);

        }
    }

    private static String getTemplateFolder() {
        String path = TEMPORARY_SECONDARY_STORE + "/template/tmpl/1/";
        File templatesDirectory = new File(path);
        List<String> templates = Arrays.asList(Objects.requireNonNull(templatesDirectory.list()));
        if (templates != null && templates.size() > 0) {
            Collections.sort(templates);
            return path + String.valueOf(Long.parseLong(templates.get(templates.size() -1)) + 1L);
        } else {
            return path + "9";
        }
    }


    public static void registerTemplate(Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName) {
        try {
            List<Long> zoneIds = getEligibleZoneIds();
            for (Long zoneId : zoneIds) {
                String storeUrl = getNfsStoreInZone(zoneId);
                mountStore(storeUrl);
                String destTempFolder = getTemplateFolder();
                Script.runSimpleBashScript("cp " + templateFiles.get(hypervisorAndTemplateName.getKey()) + " " + destTempFolder);
                String storageScriptsDir = "scripts/storage/secondary";
                String createTmplPath = Script.findScript(storageScriptsDir, "createtmplt.sh");
                if (createTmplPath == null) {
                    throw new ConfigurationException("Unable to find the createtmplt.sh");
                }
                String templateName = UUID.randomUUID().toString();
                // TODO: need to add extension
                String templateFilename = templateName;
                Script scr = new Script(createTmplPath, 120, LOGGER);
                scr.add("-n", templateFilename);

                scr.add("-t", destTempFolder);
                scr.add("-f", destTempFolder); // this is the temporary
                // template file downloaded
                String result = scr.execute();
            }
        } catch (Exception e) {
            String errMsg = "Failed to register template for hypervisor: " + hypervisorAndTemplateName.getKey();
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
    }

    public static void registerTemplates(Set<Hypervisor.HypervisorType> hypervisorsInUse) {
        // Check if templates path exists
        Set<String> hypervisors = hypervisorsInUse.stream().map(Enum::name).
                map(name -> name.toLowerCase(Locale.ROOT)).map(SystemVmTemplateRegistration::getHypervisorName).collect(Collectors.toSet());
        File templatesDirectory = new File(templatesPath);
        List<String> templatePaths = new ArrayList<>();
        String[] templates = Objects.requireNonNull(templatesDirectory.list());
        for (String template : templates) {
            if (hypervisors.stream().anyMatch(template::contains)) {
                templatePaths.add(templatesDirectory.getPath() + "/" + template);
            } else {
                throw new CloudRuntimeException("SystemVm template " + template + " not found. Cannot upgrade system Vms");
            }
        }

        // Perform Registration if templates not already registered
        for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : SystemVmTemplateRegistration.NewTemplateNameList.entrySet()) {
           long templateId = isTemplateAlreadyRegistered(hypervisorAndTemplateName);
           if (templateId != -1) {
               continue;
           }
           // TODO: Concurrency??
           registerTemplate(hypervisorAndTemplateName);
        }
    }
}
