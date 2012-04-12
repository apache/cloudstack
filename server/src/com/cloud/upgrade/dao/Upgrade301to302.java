// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.cloud.upgrade.dao;

/**
 * @author Alena Prokharchyk
 */
import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade301to302 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade301to302.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "3.0.1", "3.0.2" };
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.2";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-301to302.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-301to302.sql");
        }

        return new File[] { new File(script) };
    }

    private void dropKeysIfExists(Connection conn) {
        HashMap<String, List<String>> uniqueKeys = new HashMap<String, List<String>>();
        List<String> keys = new ArrayList<String>();
        
        keys.add("i_host__allocation_state");
        uniqueKeys.put("host", keys);
        
        s_logger.debug("Droping i_host__allocation_state key in host table");
        for (String tableName : uniqueKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, uniqueKeys.get(tableName), false);
        }
    }
    
    @Override
    public void performDataMigration(Connection conn) {
        dropKeysIfExists(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-301to302-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-301to302-cleanup.sql");
        }

        return new File[] { new File(script) };
    }
}
