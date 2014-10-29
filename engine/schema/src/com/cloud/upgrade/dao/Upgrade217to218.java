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

import java.io.File;
import java.sql.Connection;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade217to218 implements DbUpgrade {

    @Override
    public File[] getPrepareScripts() {
        String schemaFile = Script.findScript("", "db/schema-217to218.sql");
        if (schemaFile == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-217to218.sql");
        }

        String dataFile = Script.findScript("", "db/data-217to218.sql");
        if (dataFile == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, data-217to218.sql");
        }

        return new File[] {new File(schemaFile), new File(dataFile)};
    }

    @Override
    public void performDataMigration(Connection conn) {

    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.1.7", "2.1.7"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.1.8";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }
}
