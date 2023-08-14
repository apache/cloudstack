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
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Upgrade41800to41810 implements DbUpgrade, DbUpgradeSystemVmTemplate {
    final static Logger LOG = Logger.getLogger(Upgrade41800to41810.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.18.0.0", "4.18.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.18.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41800to41810.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        fixForeignKeyNames(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41800to41810-cleanup.sql";
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

    private void fixForeignKeyNames(Connection conn) {
        //Alter foreign key name for user_vm table from fk_user_data_id to fk_user_vm__user_data_id (if exists)
        List<String> keys = new ArrayList<String>();
        keys.add("fk_user_data_id");
        keys.add("fk_user_vm__user_data_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_vm", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_vm", keys, false);
        DbUpgradeUtils.addForeignKey(conn, "user_vm", "user_data_id", "user_data", "id");

        //Alter foreign key name for vm_template table from fk_user_data_id to fk_vm_template__user_data_id (if exists)
        keys = new ArrayList<>();
        keys.add("fk_user_data_id");
        keys.add("fk_vm_template__user_data_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_template", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_template", keys, false);
        DbUpgradeUtils.addForeignKey(conn, "vm_template", "user_data_id", "user_data", "id");

        //Alter foreign key name for volumes table from fk_passphrase_id to fk_volumes__passphrase_id (if exists)
        keys = new ArrayList<>();
        keys.add("fk_passphrase_id");
        keys.add("fk_volumes__passphrase_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.volumes", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.volumes", keys, false);
        DbUpgradeUtils.addForeignKey(conn, "volumes", "passphrase_id","passphrase", "id");
    }
}
