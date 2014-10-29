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

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade2211to2212Premium extends Upgrade2211to2212 {
    final static Logger s_logger = Logger.getLogger(Upgrade2211to2212Premium.class);

    @Override
    public File[] getPrepareScripts() {
        File[] scripts = super.getPrepareScripts();
        File[] newScripts = new File[2];
        newScripts[0] = scripts[0];

        String file = Script.findScript("", "db/schema-2211to2212-premium.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-2211to2212-premium.sql");
        }

        newScripts[1] = new File(file);

        return newScripts;
    }

    @Override
    public void performDataMigration(Connection conn) {
        super.performDataMigration(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
}
