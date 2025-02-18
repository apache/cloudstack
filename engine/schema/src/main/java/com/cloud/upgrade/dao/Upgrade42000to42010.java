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

import java.io.InputStream;
import java.sql.Connection;

import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42000to42010 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.20.0.0", "4.20.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.20.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42000to42010.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        addIndexes(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-42000to42010-cleanup.sql";
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
        logger.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    private void addIndexes(Connection conn) {
        DbUpgradeUtils.addIndexIfNeeded(conn, "host", "mgmt_server_id");
        DbUpgradeUtils.addIndexIfNeeded(conn, "host", "resource");
        DbUpgradeUtils.addIndexIfNeeded(conn, "host", "resource_state");
        DbUpgradeUtils.addIndexIfNeeded(conn, "host", "type");

        DbUpgradeUtils.renameIndexIfNeeded(conn, "user_ip_address", "public_ip_address", "uk_public_ip_address");
        DbUpgradeUtils.addIndexIfNeeded(conn, "user_ip_address", "public_ip_address");
        DbUpgradeUtils.addIndexIfNeeded(conn, "user_ip_address", "data_center_id");
        DbUpgradeUtils.addIndexIfNeeded(conn, "user_ip_address", "vlan_db_id");
        DbUpgradeUtils.addIndexIfNeeded(conn, "user_ip_address", "removed");

        DbUpgradeUtils.addIndexIfNeeded(conn, "vlan", "vlan_type");
        DbUpgradeUtils.addIndexIfNeeded(conn, "vlan", "data_center_id");
        DbUpgradeUtils.addIndexIfNeeded(conn, "vlan", "removed");

        DbUpgradeUtils.addIndexIfNeeded(conn, "network_offering_details", "name");

        DbUpgradeUtils.addIndexIfNeeded(conn, "network_offering_details", "resource_id", "resource_type");

        DbUpgradeUtils.addIndexIfNeeded(conn, "service_offering", "cpu");
        DbUpgradeUtils.addIndexIfNeeded(conn, "service_offering", "speed");
        DbUpgradeUtils.addIndexIfNeeded(conn, "service_offering", "ram_size");

        DbUpgradeUtils.addIndexIfNeeded(conn, "op_host_planner_reservation", "resource_usage");

        DbUpgradeUtils.addIndexIfNeeded(conn, "storage_pool", "pool_type");
        DbUpgradeUtils.addIndexIfNeeded(conn, "storage_pool", "data_center_id", "status", "scope", "hypervisor");

        DbUpgradeUtils.addIndexIfNeeded(conn, "router_network_ref", "guest_type");

        DbUpgradeUtils.addIndexIfNeeded(conn, "domain_router", "role");

        DbUpgradeUtils.addIndexIfNeeded(conn, "async_job", "instance_type", "job_status");

        DbUpgradeUtils.addIndexIfNeeded(conn, "cluster", "managed_state");
    }
}
