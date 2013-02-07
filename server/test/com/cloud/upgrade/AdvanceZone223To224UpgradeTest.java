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
package com.cloud.upgrade;

import java.sql.SQLException;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.cloud.upgrade.dao.VersionDaoImpl;


public class AdvanceZone223To224UpgradeTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(AdvanceZone223To224UpgradeTest.class);
    @Inject VersionDaoImpl dao;
    @Inject DatabaseUpgradeChecker checker;

    @Override
    @Before
    public void setUp() throws Exception {
//        DbTestUtils.executeScript("PreviousDatabaseSchema/clean-db.sql", false, true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
    }

    public void test223to224Upgrade() throws SQLException {


        String version = dao.getCurrentVersion();
        assert version.equals("2.2.3") : "Version returned is not 2.2.3 but " + version;

        checker.upgrade("2.2.3", "2.2.4");
    }

}
