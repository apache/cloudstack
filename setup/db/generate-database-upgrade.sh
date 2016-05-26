#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

if [[ $# -ne 2 ]]
then
	echo "Usage: $0 <old-version> <new-version>"
	echo "Example:"
	echo "        $0 4.7.1 4.7.2"
	exit 1
fi

if ! [[ `pwd` == */cloudstack/setup/db ]]
then
	echo "Please run this script from within cloudstack/setup/db"
	exit 1
fi

oldVersion=$1
newVersion=$2

oldVersionDotless=${oldVersion//\./}
newVersionDotless=${newVersion//\./}

# Next check might break if minor or patch version exceed 9 for one version but
# not the other... but who cares
if (( oldVersionDotless >= newVersionDotless ))
then
	echo "Specified old-version is newer or equal to specified new-version:"
	echo "        old-version: $oldVersion"
	echo "        new-version: $newVersion"
	exit 1
fi

echo "Generating database upgrade from version $oldVersion to version $newVersion"

oldToNew="${oldVersionDotless}to${newVersionDotless}"

read -r -d '' sql << EOF
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema SQLFILE from $oldVersion to $newVersion;
--;
EOF

echo "${sql/SQLFILE/cleanup}" > db/schema-${oldToNew}-cleanup.sql
echo "${sql/SQLFILE/upgrade}" > db/schema-${oldToNew}.sql

read -r -d '' java << EOF
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
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;

public class Upgrade${oldToNew} implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade${oldToNew}.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"${oldVersion}", "${newVersion}"};
    }

    @Override
    public String getUpgradedVersion() {
        return "${newVersion}";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-${oldToNew}.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-${oldToNew}.sql");
        }
        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-${oldToNew}-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-${oldToNew}-cleanup.sql");
        }

        return new File[] {new File(script)};
    }
}
EOF

echo "$java" > ../../engine/schema/src/com/cloud/upgrade/dao/Upgrade${oldToNew}.java

sed -i -e 's/import com.cloud.upgrade.dao.UpgradeSnapshot217to224;/import com.cloud.upgrade.dao.Upgrade'${oldToNew}';\
import com.cloud.upgrade.dao.UpgradeSnapshot217to224;/' ../../engine/schema/src/com/cloud/upgrade/DatabaseUpgradeChecker.java
sed -i -e 's/});/, new Upgrade'${oldToNew}'()});/' ../../engine/schema/src/com/cloud/upgrade/DatabaseUpgradeChecker.java
sed -i -e 's/\/\/CP Upgrades/_upgradeMap.put("'${oldVersion}'", new DbUpgrade[] {new Upgrade'${oldToNew}'()});\
\
        \/\/CP Upgrades/' ../../engine/schema/src/com/cloud/upgrade/DatabaseUpgradeChecker.java

rm ../../engine/schema/src/com/cloud/upgrade/DatabaseUpgradeChecker.java-e

echo "Finished generating database upgrade"
