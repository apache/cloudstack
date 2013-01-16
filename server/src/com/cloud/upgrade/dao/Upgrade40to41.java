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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

import java.io.File;
import java.sql.Connection;

/**
 * @author htrippaers
 *
 */
public class Upgrade40to41 implements DbUpgrade {

	/**
	 *
	 */
	public Upgrade40to41() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getUpgradableVersionRange()
	 */
	@Override
	public String[] getUpgradableVersionRange() {
		return new String[] { "4.0.0", "4.1.0" };
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getUpgradedVersion()
	 */
	@Override
	public String getUpgradedVersion() {
		return "4.1.0";
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#supportsRollingUpgrade()
	 */
	@Override
	public boolean supportsRollingUpgrade() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getPrepareScripts()
	 */
	@Override
	public File[] getPrepareScripts() {
		String script = Script.findScript("", "db/schema-40to410.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-40to410.sql");
        }

        return new File[] { new File(script) };
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#performDataMigration(java.sql.Connection)
	 */
	@Override
	public void performDataMigration(Connection conn) {

	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getCleanupScripts()
	 */
	@Override
	public File[] getCleanupScripts() {
		return new File[0];
	}

}
