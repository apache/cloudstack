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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Upgrade41700to41800 implements DbUpgrade, DbUpgradeSystemVmTemplate {

    final static Logger LOG = Logger.getLogger(Upgrade41700to41800.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.17.0.0", "4.18.0.0"};
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
        final String scriptFile = "META-INF/db/schema-41700to41800.sql";
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
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41700to41800-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void initSystemVmTemplateRegistration() {
        systemVmTemplateRegistration = new SystemVmTemplateRegistration();
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

        LOG.info(String.format("Updating %s registers of %s quota tariffs of type [%s] with SQL [%s].", tariffs.size() -1, setRemoved ? "previous of current" :
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

        String insertSql = String.format("INSERT INTO cloud_usage.quota_tariff (usage_type, usage_name, usage_unit, usage_discriminator, currency_value, effective_on, updated_on,"
                + " updated_by, uuid, name, description, removed, end_date, activation_rule)\n"
                + "SELECT  1, 'RUNNING_VM', usage_unit, '', 0, effective_on, updated_on, updated_by, UUID(), name, description, removed, end_date,\n"
                + "        CASE\n"
                + "            WHEN usage_type = 15 THEN CONCAT('((value.computingResources ? (value.computingResources.cpuSpeed * value.computingResources.cpuNumber) : 0) / 100) * ', currency_value)\n"
                + "            WHEN usage_type = 16 THEN CONCAT('(value.computingResources ? value.computingResources.cpuNumber : 0) * ', currency_value)\n"
                + "            WHEN usage_type = 17 THEN CONCAT('(value.computingResources ? value.computingResources.memory : 0)* ', currency_value)\n"
                + "        END\n"
                + "FROM    cloud_usage.quota_tariff \n"
                + "WHERE   usage_type in (15, 16, 17) \n"
                + "AND     currency_value > 0.0;");

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
}
