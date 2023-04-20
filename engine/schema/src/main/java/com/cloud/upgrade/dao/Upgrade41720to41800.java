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
package com.cloud.upgrade.dao;

import com.cloud.storage.GuestOSHypervisorMapping;
import com.cloud.upgrade.ConfigurationGroupsAggregator;
import com.cloud.upgrade.GuestOsMapper;
import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.response.UsageTypeResponse;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Upgrade41720to41800 implements DbUpgrade, DbUpgradeSystemVmTemplate {

    final static Logger LOG = Logger.getLogger(Upgrade41720to41800.class);

    private GuestOsMapper guestOsMapper = new GuestOsMapper();

    private SystemVmTemplateRegistration systemVmTemplateRegistration;
    private ConfigurationGroupsAggregator configGroupsAggregator = new ConfigurationGroupsAggregator();

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.17.2.0", "4.18.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.18.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41720to41800.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        convertQuotaTariffsToNewParadigm(conn);
        convertVmResourcesQuotaTypesToRunningVmQuotaType(conn);
        correctGuestOsNames();
        updateGuestOsMappings();
        correctGuestOsIdsInHypervisorMapping(conn);
        updateConfigurationGroups();
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41720to41800-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void initSystemVmTemplateRegistration() {
        systemVmTemplateRegistration = new SystemVmTemplateRegistration("");
    }

    @Override
    public void updateSystemVmTemplates(Connection conn) {
        LOG.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    protected void convertQuotaTariffsToNewParadigm(Connection conn) {
        LOG.info("Converting quota tariffs to new paradigm.");

        List<UsageTypeResponse> usageTypeResponses = UsageTypes.listUsageTypes();

        for (UsageTypeResponse usageTypeResponse : usageTypeResponses) {
            Integer usageType = usageTypeResponse.getUsageType();

            String tariffTypeDescription = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(usageTypeResponse, "description", "usageType");

            LOG.info(String.format("Converting quota tariffs of type %s to new paradigm.", tariffTypeDescription));

            for (boolean previousTariff : Arrays.asList(true, false)) {
                Map<Long, Date> tariffs = selectTariffs(conn, usageType, previousTariff, tariffTypeDescription);

                int tariffsSize = tariffs.size();
                if (tariffsSize <  2) {
                    LOG.info(String.format("Quota tariff of type %s has [%s] %s register(s). Tariffs with less than 2 register do not need to be converted to new paradigm.",
                            tariffTypeDescription, tariffsSize, previousTariff ? "previous of current" : "next to current"));
                    continue;
                }

                executeUpdateQuotaTariffSetEndDateAndRemoved(conn, usageType, tariffs, previousTariff, tariffTypeDescription);
            }
        }
    }

    protected Map<Long, Date> selectTariffs(Connection conn, Integer usageType, boolean previousTariff, String tariffTypeDescription) {
        Map<Long, Date> quotaTariffs = new LinkedHashMap<>();

        String selectQuotaTariffs = String.format("SELECT id, effective_on FROM cloud_usage.quota_tariff WHERE %s AND usage_type = ? ORDER BY effective_on, updated_on;",
                previousTariff ? "usage_name = name" : "removed is null");

        LOG.info(String.format("Selecting %s quota tariffs of type [%s] according to SQL [%s].", previousTariff ? "previous of current" : "next to current",
                tariffTypeDescription, selectQuotaTariffs));

        try (PreparedStatement pstmt = conn.prepareStatement(selectQuotaTariffs)) {
            pstmt.setInt(1, usageType);

            try (ResultSet result = pstmt.executeQuery()) {
                while (result.next()) {
                    quotaTariffs.put(result.getLong("id"), result.getDate("effective_on"));
                }
            }
            return quotaTariffs;
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve %s quota tariffs of type [%s] due to [%s].", previousTariff ? "previous" : "next", tariffTypeDescription,
                    e.getMessage());
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    protected void executeUpdateQuotaTariffSetEndDateAndRemoved(Connection conn, Integer usageType, Map<Long, Date> tariffs, boolean setRemoved, String tariffTypeDescription) {
        String updateQuotaTariff = String.format("UPDATE cloud_usage.quota_tariff SET end_date = ? %s WHERE id = ?;", setRemoved ? ", removed = ?" : "");

        Object[] ids = tariffs.keySet().toArray();

        LOG.info(String.format("Updating %s registers of %s quota tariffs of type [%s] with SQL [%s].", tariffs.size() - 1, setRemoved ? "previous of current" :
                "next to current", tariffTypeDescription, updateQuotaTariff));

        for (int i = 0; i < tariffs.size() - 1; i++) {
            Long id = Long.valueOf(String.valueOf(ids[i]));
            Long nextId = Long.valueOf(String.valueOf(ids[i + 1]));

            Date endDate = tariffs.get(nextId);

            if (!DateUtils.isSameDay(endDate, tariffs.get(id))) {
                endDate = DateUtils.addDays(endDate, -1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateQuotaTariff)) {
                java.sql.Date sqlEndDate = new java.sql.Date(endDate.getTime());
                pstmt.setDate(1, sqlEndDate);

                String updateRemoved = "";
                if (setRemoved) {
                    pstmt.setDate(2, sqlEndDate);
                    pstmt.setLong(3, id);

                    updateRemoved = String.format("and \"removed\" to [%s] ", sqlEndDate);
                } else {
                    pstmt.setLong(2, id);
                }

                LOG.info(String.format("Updating \"end_date\" to [%s] %sof quota tariff with ID [%s].", sqlEndDate, updateRemoved, id));
                pstmt.executeUpdate();
            } catch (SQLException e) {
                String message = String.format("Unable to update \"end_date\" %s of quota tariffs of usage type [%s] due to [%s].", setRemoved ? "and \"removed\"" : "",
                        usageType, e.getMessage());
                LOG.error(message, e);
                throw new CloudRuntimeException(message, e);
            }
        }
    }

    protected void convertVmResourcesQuotaTypesToRunningVmQuotaType(Connection conn) {
        LOG.info("Converting quota tariffs of type \"vCPU\", \"CPU_SPEED\" and \"MEMORY\" to \"RUNNING_VM\".");

        String insertSql = "INSERT INTO cloud_usage.quota_tariff (usage_type, usage_name, usage_unit, usage_discriminator, currency_value, effective_on, updated_on,"
                + " updated_by, uuid, name, description, removed, end_date, activation_rule)\n"
                + "SELECT  1, 'RUNNING_VM', usage_unit, '', 0, effective_on, updated_on, updated_by, UUID(), name, description, removed, end_date,\n"
                + "        CASE\n"
                + "            WHEN usage_type = 15 THEN CONCAT('((value.computingResources ? (value.computingResources.cpuSpeed * value.computingResources.cpuNumber) : 0) / 100) * ', currency_value)\n"
                + "            WHEN usage_type = 16 THEN CONCAT('(value.computingResources ? value.computingResources.cpuNumber : 0) * ', currency_value)\n"
                + "            WHEN usage_type = 17 THEN CONCAT('(value.computingResources ? value.computingResources.memory : 0) * ', currency_value)\n"
                + "        END\n"
                + "FROM    cloud_usage.quota_tariff \n"
                + "WHERE   usage_type in (15, 16, 17) \n"
                + "AND     currency_value > 0.0;";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            String message = String.format("Failed to convert quota tariffs of type \"vCPU\", \"CPU_SPEED\" and \"MEMORY\" to \"RUNNING_VM\" due to [%s].", e.getMessage());
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        LOG.info("Disabling unused quota tariffs of type \"vCPU\", \"CPU_SPEED\" and \"MEMORY\".");

        String updateSql = "UPDATE cloud_usage.quota_tariff SET removed = now() WHERE usage_type in (15, 16, 17) and removed is null;";

        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            String message = String.format("Failed disable quota tariffs of type \"vCPU\", \"CPU_SPEED\" and \"MEMORY\" due to [%s].", e.getMessage());
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    private void correctGuestOsNames() {
        guestOsMapper.updateGuestOsName(7, "Fedora Linux", "Fedora Linux (32 bit)");
        guestOsMapper.updateGuestOsName(7, "Mandriva Linux", "Mandriva Linux (32 bit)");

        GuestOSHypervisorMapping mapping = new GuestOSHypervisorMapping("VMware", "6.7.3", "opensuseGuest");
        guestOsMapper.updateGuestOsNameFromMapping("OpenSUSE Linux (32 bit)", mapping);
    }

    private void updateGuestOsMappings() {
        LOG.debug("Updating guest OS mappings");

        // Add support for SUSE Linux Enterprise Desktop 12 SP3 (64-bit) for Xenserver 8.1.0
        List<GuestOSHypervisorMapping> mappings = new ArrayList<GuestOSHypervisorMapping>();
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise Desktop 12 SP3 (64-bit)"));
        guestOsMapper.addGuestOsAndHypervisorMappings(5, "SUSE Linux Enterprise Desktop 12 SP3 (64-bit)", mappings);
        mappings.clear();

        // Add support for SUSE Linux Enterprise Desktop 12 SP4 (64-bit) for Xenserver 8.1.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise Desktop 12 SP4 (64-bit)"));
        guestOsMapper.addGuestOsAndHypervisorMappings(5, "SUSE Linux Enterprise Desktop 12 SP4 (64-bit)", mappings);
        mappings.clear();

        // Add support for SUSE Linux Enterprise Server 12 SP4 (64-bit) and NeoKylin Linux Server 7 for Xenserver 8.1.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise Server 12 SP4 (64-bit)"));
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "NeoKylin Linux Server 7"));
        guestOsMapper.addGuestOsAndHypervisorMappings(5, "SUSE Linux Enterprise Server 12 SP4 (64-bit)", mappings);
        mappings.clear();

        // Add support for Scientific Linux 7 and NeoKylin Linux Server 7 for Xenserver 8.1.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "Scientific Linux 7"));
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "NeoKylin Linux Server 7"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "Scientific Linux 7", mappings);
        mappings.clear();

        // Add support for NeoKylin Linux Server 7 for Xenserver 8.1.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "NeoKylin Linux Server 7"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "NeoKylin Linux Server 7", mappings); //334
        mappings.clear();

        // Pass Guest OS Ids to update pre-4.14 mappings
        // Add support CentOS 8 for Xenserver 8.1.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "CentOS 8"),
                1, "CentOS 8");

        // Add support for Debian Buster 10 for Xenserver 8.1.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "Debian Buster 10"),
                2, "Debian GNU/Linux 10 (32-bit)");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "Debian Buster 10"),
                2, "Debian GNU/Linux 10 (64-bit)");

        // Add support for SUSE Linux Enterprise 15 (64-bit) for Xenserver 8.1.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise 15 (64-bit)"),
                5, "SUSE Linux Enterprise Server 15 (64-bit)");

        // Add support for Ubuntu Focal Fossa 20.04 for Xenserver 8.2.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.2.0", "Ubuntu Focal Fossa 20.04"));
        guestOsMapper.addGuestOsAndHypervisorMappings(10, "Ubuntu 20.04 LTS", mappings);
        mappings.clear();

        // Add support for darwin19_64Guest from VMware 7.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "darwin19_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 10.15 (64 bit)", mappings);
        mappings.clear();

        // Add support for debian11_64Guest from VMware 7.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "debian11_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Debian GNU/Linux 11 (64-bit)", mappings);
        mappings.clear();

        // Add support for debian11Guest from VMware 7.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "debian11Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Debian GNU/Linux 11 (32-bit)", mappings);
        mappings.clear();

        // Add support for windows2019srv_64Guest from VMware 7.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0", "windows2019srv_64Guest"),
                6, "Windows Server 2019 (64-bit)");

        // Add support for amazonlinux3_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "amazonlinux3_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Amazon Linux 3 (64 bit)", mappings);
        mappings.clear();

        // Add support for asianux9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 9 (64 bit)", mappings);
        mappings.clear();

        // Add support for centos9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "centos9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(1, "CentOS 9", mappings);
        mappings.clear();

        // Add support for darwin20_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "darwin20_64Guest"));
        // Add support for darwin21_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "darwin21_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 11 (64 bit)", mappings);
        mappings.clear();

        // Add support for freebsd13_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "freebsd13_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "FreeBSD 13 (64-bit)", mappings);
        mappings.clear();

        // Add support for freebsd13Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "freebsd13Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "FreeBSD 13 (32-bit)", mappings);
        mappings.clear();

        // Add support for oracleLinux9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "oracleLinux9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(3, "Oracle Linux 9", mappings);
        mappings.clear();

        // Add support for other5xLinux64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "other5xLinux64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 5.x Kernel (64-bit)", mappings);
        mappings.clear();

        // Add support for other5xLinuxGuest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "other5xLinuxGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 5.x Kernel (32-bit)", mappings);
        mappings.clear();

        // Add support for rhel9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "rhel9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(4, "Red Hat Enterprise Linux 9.0", mappings);
        mappings.clear();

        // Add support for sles16_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "sles16_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(5, "SUSE Linux Enterprise Server 16 (64-bit)", mappings);
        mappings.clear();

        // Add support for windows2019srvNext_64Guest from VMware 7.0.1.0 - Pass Guest OS Ids to update pre-4.14 mappings
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "windows2019srvNext_64Guest"),
                6, "Windows Server 2019 (64-bit)");

        // The below existing Guest OS Ids must be used for updating the guest OS hypervisor mappings
        // CentOS - 1, Debian - 2, Oracle - 3, RedHat - 4, SUSE - 5, Windows - 6, Other - 7, Novel - 8, Unix - 9, Ubuntu - 10, None - 11

        // OVF configured OS while registering deploy-as-is templates Linux 3.x Kernel OS
        guestOsMapper.addGuestOsAndHypervisorMappings(11, "OVF Configured OS", null);

        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "other3xLinux64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 3.x Kernel (64 bit)", mappings);
        mappings.clear();

        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "other3xLinuxGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 3.x Kernel (32 bit)", mappings);
        mappings.clear();

        // Add Amazonlinux as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "amazonlinux2_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Amazon Linux 2 (64 bit)", mappings);
        mappings.clear();

        // Add asianux4 32 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "asianux4Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 4 (32 bit)", mappings);
        mappings.clear();

        // Add asianux4 64 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "asianux4_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 4 (64 bit)", mappings);
        mappings.clear();

        // Add asianux5 32 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "asianux5Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 5 (32 bit)", mappings);
        mappings.clear();

        // Add asianux5 64 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "asianux5_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 5 (64 bit)", mappings);
        mappings.clear();

        // Add asianux7 32 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "asianux7Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 7 (32 bit)", mappings);
        mappings.clear();

        // Add asianux7 64 as support guest os, and  VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "asianux7_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 7 (64 bit)", mappings);
        mappings.clear();

        // Add asianux8 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "asianux8_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 8 (64 bit)", mappings);
        mappings.clear();

        // Add eComStation 2.0 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "eComStation2Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "eComStation 2.0", mappings);
        mappings.clear();

        // Add macOS 10.13 (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "darwin17_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 10.13 (64 bit)", mappings);
        mappings.clear();

        // Add macOS 10.14 (64 bit) as support guest os, and VMWare guest os mapping
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "darwin18_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 10.14 (64 bit)", mappings);
        mappings.clear();

        // Add Fedora Linux (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "fedora64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Fedora Linux (64 bit)", mappings);
        mappings.clear();

        // Add Fedora Linux as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "fedoraGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Fedora Linux", mappings);
        mappings.clear();

        // Add Mandrake Linux as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "mandrakeGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Mandrake Linux", mappings);
        mappings.clear();

        // Add Mandriva Linux (64 bit)  as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "mandriva64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Mandriva Linux (64 bit)", mappings);
        mappings.clear();

        // Add Mandriva Linux  as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "mandrivaGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Mandriva Linux", mappings);
        mappings.clear();

        // Add SCO OpenServer 5   as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "openServer5Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "SCO OpenServer 5", mappings);
        mappings.clear();

        // Add SCO OpenServer 6 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "openServer6Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "SCO OpenServer 6", mappings);
        mappings.clear();

        // Add OpenSUSE Linux (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "opensuse64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "OpenSUSE Linux (64 bit)", mappings);
        mappings.clear();

        // Add OpenSUSE Linux (32 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "opensuseGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "OpenSUSE Linux (32 bit)", mappings);
        mappings.clear();

        // Add Solaris 11 (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "solaris11_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Solaris 11 (64 bit)", mappings);
        mappings.clear();

        // Add  VMware Photon (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "vmwarePhoton64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "VMware Photon (64 bit)", mappings);
    }

    private void correctGuestOsIdsInHypervisorMapping(final Connection conn) {
        LOG.debug("Correcting guest OS ids in hypervisor mappings");
        guestOsMapper.updateGuestOsIdInHypervisorMapping(conn, 10, "Ubuntu 20.04 LTS", new GuestOSHypervisorMapping("Xenserver", "8.2.0", "Ubuntu Focal Fossa 20.04"));
    }

    private void updateConfigurationGroups() {
        configGroupsAggregator.updateConfigurationGroups();
    }
}
