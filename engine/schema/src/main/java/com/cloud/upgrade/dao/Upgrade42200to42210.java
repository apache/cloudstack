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

import org.apache.cloudstack.vm.UnmanagedVMsManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Upgrade42200to42210 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.22.0.0", "4.22.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.22.1.0";
    }

    @Override
    public void performDataMigration(Connection conn) {
        removeDuplicateDummyTemplates(conn);
    }

    private void removeDuplicateDummyTemplates(Connection conn) {
        List<Long> templateIds = new ArrayList<>();
        try (PreparedStatement selectStmt = conn.prepareStatement(String.format("SELECT id FROM cloud.vm_template WHERE name='%s' ORDER BY id ASC", UnmanagedVMsManager.KVM_VM_IMPORT_DEFAULT_TEMPLATE_NAME))) {
            ResultSet rs = selectStmt.executeQuery();
            while (rs.next()) {
                templateIds.add(rs.getLong(1));
            }

            if (templateIds.size() <= 1) {
                return;
            }

            Long firstTemplateId = templateIds.get(0);

            String updateTemplateSql = "UPDATE cloud.vm_instance SET vm_template_id = ? WHERE vm_template_id = ?";
            String deleteTemplateSql = "DELETE FROM cloud.vm_template WHERE id = ?";

            try (PreparedStatement updateTemplateStmt = conn.prepareStatement(updateTemplateSql);
                 PreparedStatement deleteTemplateStmt = conn.prepareStatement(deleteTemplateSql)) {
                for (int i = 1; i < templateIds.size(); i++) {
                    Long duplicateTemplateId = templateIds.get(i);

                    // Update VM references
                    updateTemplateStmt.setLong(1, firstTemplateId);
                    updateTemplateStmt.setLong(2, duplicateTemplateId);
                    updateTemplateStmt.executeUpdate();

                    // Delete duplicate dummy template
                    deleteTemplateStmt.setLong(1, duplicateTemplateId);
                    deleteTemplateStmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to clean duplicate kvm-default-vm-import-dummy-template entries", e);
        }
    }
}
