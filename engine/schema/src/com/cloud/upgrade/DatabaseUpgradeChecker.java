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

import com.cloud.maint.Version;
import com.cloud.upgrade.dao.DbUpgrade;
import com.cloud.upgrade.dao.Upgrade217to218;
import com.cloud.upgrade.dao.Upgrade218to22;
import com.cloud.upgrade.dao.Upgrade218to224DomainVlans;
import com.cloud.upgrade.dao.Upgrade2210to2211;
import com.cloud.upgrade.dao.Upgrade2211to2212;
import com.cloud.upgrade.dao.Upgrade2212to2213;
import com.cloud.upgrade.dao.Upgrade2213to2214;
import com.cloud.upgrade.dao.Upgrade2214to30;
import com.cloud.upgrade.dao.Upgrade221to222;
import com.cloud.upgrade.dao.Upgrade222to224;
import com.cloud.upgrade.dao.Upgrade224to225;
import com.cloud.upgrade.dao.Upgrade225to226;
import com.cloud.upgrade.dao.Upgrade227to228;
import com.cloud.upgrade.dao.Upgrade228to229;
import com.cloud.upgrade.dao.Upgrade229to2210;
import com.cloud.upgrade.dao.Upgrade301to302;
import com.cloud.upgrade.dao.Upgrade302to303;
import com.cloud.upgrade.dao.Upgrade302to40;
import com.cloud.upgrade.dao.Upgrade303to304;
import com.cloud.upgrade.dao.Upgrade304to305;
import com.cloud.upgrade.dao.Upgrade305to306;
import com.cloud.upgrade.dao.Upgrade306to307;
import com.cloud.upgrade.dao.Upgrade307to410;
import com.cloud.upgrade.dao.Upgrade30to301;
import com.cloud.upgrade.dao.Upgrade40to41;
import com.cloud.upgrade.dao.Upgrade410to420;
import com.cloud.upgrade.dao.Upgrade420to421;
import com.cloud.upgrade.dao.Upgrade421to430;
import com.cloud.upgrade.dao.Upgrade430to440;
import com.cloud.upgrade.dao.Upgrade431to440;
import com.cloud.upgrade.dao.Upgrade432to440;
import com.cloud.upgrade.dao.Upgrade440to441;
import com.cloud.upgrade.dao.Upgrade441to442;
import com.cloud.upgrade.dao.Upgrade442to450;
import com.cloud.upgrade.dao.Upgrade443to450;
import com.cloud.upgrade.dao.Upgrade444to450;
import com.cloud.upgrade.dao.Upgrade450to451;
import com.cloud.upgrade.dao.Upgrade451to452;
import com.cloud.upgrade.dao.Upgrade452to460;
import com.cloud.upgrade.dao.Upgrade453to460;
import com.cloud.upgrade.dao.Upgrade460to461;
import com.cloud.upgrade.dao.Upgrade461to462;
import com.cloud.upgrade.dao.UpgradeSnapshot217to224;
import com.cloud.upgrade.dao.UpgradeSnapshot223to224;
import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionDaoImpl;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

@Local(value = {SystemIntegrityChecker.class})
public class DatabaseUpgradeChecker implements SystemIntegrityChecker {
    private static final Logger s_logger = Logger.getLogger(DatabaseUpgradeChecker.class);

    protected HashMap<String, DbUpgrade[]> _upgradeMap = new HashMap<String, DbUpgrade[]>();

    @Inject
    VersionDao _dao;

    public DatabaseUpgradeChecker() {
        _dao = new VersionDaoImpl();

        _upgradeMap.put("2.1.7", new DbUpgrade[] {new Upgrade217to218(), new Upgrade218to22(), new Upgrade221to222(),
            new UpgradeSnapshot217to224(), new Upgrade222to224(), new Upgrade224to225(), new Upgrade225to226(),
            new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
            new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
            new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(),
            new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
            new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.1.8", new DbUpgrade[] {new Upgrade218to22(), new Upgrade221to222(), new UpgradeSnapshot217to224(),
            new Upgrade222to224(), new Upgrade218to224DomainVlans(), new Upgrade224to225(), new Upgrade225to226(),
            new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
            new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(),
            new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(),
            new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.1.9", new DbUpgrade[] {new Upgrade218to22(), new Upgrade221to222(), new UpgradeSnapshot217to224(),
            new Upgrade222to224(), new Upgrade218to224DomainVlans(), new Upgrade224to225(), new Upgrade225to226(),
            new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
            new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
            new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(),
            new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
            new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.1", new DbUpgrade[] {new Upgrade221to222(), new UpgradeSnapshot223to224(), new Upgrade222to224(),
            new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(),
            new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(),
            new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(),
            new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(),
            new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.2", new DbUpgrade[] {new Upgrade222to224(), new UpgradeSnapshot223to224(), new Upgrade224to225(),
            new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(),
            new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(),
            new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.3", new DbUpgrade[] {new Upgrade222to224(), new UpgradeSnapshot223to224(), new Upgrade224to225(),
            new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(),
            new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(),
            new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.4", new DbUpgrade[] {new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(),
            new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(),
            new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(),
            new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.5", new DbUpgrade[] {new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(),
            new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(),
            new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(),
            new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
            new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.6", new DbUpgrade[] {new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(),
            new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(),
            new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(),
            new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.7", new DbUpgrade[] {new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(),
            new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(),
            new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(),
            new Upgrade410to420(),
            new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
            new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.8", new DbUpgrade[] {new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
            new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30()
            , new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.9", new DbUpgrade[] {new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(),
            new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(),
            new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.10", new DbUpgrade[] {new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(),
            new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(),
            new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(),
            new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.12", new DbUpgrade[] {new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
            new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
            new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.13", new DbUpgrade[] {new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(),
            new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.14", new DbUpgrade[] {new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(),
            new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
            new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("3.0.0", new DbUpgrade[] {new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(),
            new Upgrade40to41(), new Upgrade410to420(),
            new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("3.0.1", new DbUpgrade[] {new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("3.0.2", new DbUpgrade[] {new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
            new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.0.0", new DbUpgrade[] {new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.0.1", new DbUpgrade[] {new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.0.2", new DbUpgrade[] {new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.1.0", new DbUpgrade[] {new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.1.1", new DbUpgrade[] {new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.2.0", new DbUpgrade[] {new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.2.1", new DbUpgrade[] {new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.3.0", new DbUpgrade[] {new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.3.1", new DbUpgrade[] {new Upgrade431to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.3.2", new DbUpgrade[] {new Upgrade432to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.4.0", new DbUpgrade[] {new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.4.1", new DbUpgrade[] {new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462() });

        _upgradeMap.put("4.4.2", new DbUpgrade[] {new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.4.3", new DbUpgrade[] {new Upgrade443to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.4.4", new DbUpgrade[] {new Upgrade444to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.5.0", new DbUpgrade[] {new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.5.1", new DbUpgrade[] {new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.5.2", new DbUpgrade[] {new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.5.3", new DbUpgrade[] {new Upgrade453to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.6.0", new DbUpgrade[] {new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("4.6.1", new DbUpgrade[] {new Upgrade461to462()});

        //CP Upgrades
        _upgradeMap.put("3.0.3", new DbUpgrade[] {new Upgrade303to304(), new Upgrade304to305(), new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(),
            new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("3.0.4", new DbUpgrade[] {new Upgrade304to305(), new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(),
            new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("3.0.5", new DbUpgrade[] {new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(),
            new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("3.0.6", new DbUpgrade[] {new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
            new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("3.0.7", new DbUpgrade[] {new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.15", new DbUpgrade[] {new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(),
            new Upgrade302to303(), new Upgrade303to304(), new Upgrade304to305(), new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(),
            new Upgrade410to420(),
            new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});

        _upgradeMap.put("2.2.16", new DbUpgrade[] {new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(),
            new Upgrade302to303(), new Upgrade303to304(), new Upgrade304to305(), new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(),
            new Upgrade410to420(),
            new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to462()});
    }

    protected void runScript(Connection conn, File file) {

        try(FileReader reader = new FileReader(file);) {
            ScriptRunner runner = new ScriptRunner(conn, false, true);
            runner.runScript(reader);
        } catch (FileNotFoundException e) {
            s_logger.error("Unable to find upgrade script: " + file.getAbsolutePath(), e);
            throw new CloudRuntimeException("Unable to find upgrade script: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            s_logger.error("Unable to read upgrade script: " + file.getAbsolutePath(), e);
            throw new CloudRuntimeException("Unable to read upgrade script: " + file.getAbsolutePath(), e);
        } catch (SQLException e) {
            s_logger.error("Unable to execute upgrade script: " + file.getAbsolutePath(), e);
            throw new CloudRuntimeException("Unable to execute upgrade script: " + file.getAbsolutePath(), e);
        }

    }

    protected void upgrade(String dbVersion, String currentVersion) {
        s_logger.info("Database upgrade must be performed from " + dbVersion + " to " + currentVersion);

        String trimmedDbVersion = Version.trimToPatch(dbVersion);
        String trimmedCurrentVersion = Version.trimToPatch(currentVersion);

        DbUpgrade[] upgrades = _upgradeMap.get(trimmedDbVersion);
        if (upgrades == null) {
            s_logger.error("There is no upgrade path from " + dbVersion + " to " + currentVersion);
            throw new CloudRuntimeException("There is no upgrade path from " + dbVersion + " to " + currentVersion);
        }

        if (Version.compare(trimmedCurrentVersion, upgrades[upgrades.length - 1].getUpgradedVersion()) != 0) {
            String errorMessage = "The end upgrade version is actually at " + upgrades[upgrades.length - 1].getUpgradedVersion() +
                    " but our management server code version is at " + currentVersion;
            s_logger.error(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }

        boolean supportsRollingUpgrade = true;
        for (DbUpgrade upgrade : upgrades) {
            if (!upgrade.supportsRollingUpgrade()) {
                supportsRollingUpgrade = false;
                break;
            }
        }

        if (!supportsRollingUpgrade && false) { // FIXME: Needs to detect if there are management servers running
            // ClusterManagerImpl.arePeersRunning(null)) {
            String errorMessage =
                "Unable to run upgrade because the upgrade sequence does not support rolling update and there are other management server nodes running";
            s_logger.error(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }

        for (DbUpgrade upgrade : upgrades) {
            s_logger.debug("Running upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" +
                    upgrade.getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());
            TransactionLegacy txn = TransactionLegacy.open("Upgrade");
            txn.start();
            try {
                Connection conn;
                try {
                    conn = txn.getConnection();
                } catch (SQLException e) {
                    String errorMessage = "Unable to upgrade the database";
                    s_logger.error(errorMessage, e);
                    throw new CloudRuntimeException(errorMessage, e);
                }
                File[] scripts = upgrade.getPrepareScripts();
                if (scripts != null) {
                    for (File script : scripts) {
                        runScript(conn, script);
                    }
                }

                upgrade.performDataMigration(conn);
                boolean upgradeVersion = true;

                if (upgrade.getUpgradedVersion().equals("2.1.8")) {
                    // we don't have VersionDao in 2.1.x
                    upgradeVersion = false;
                } else if (upgrade.getUpgradedVersion().equals("2.2.4")) {
                    try(PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM version WHERE version='2.2.4'");
                            ResultSet rs = pstmt.executeQuery();) {
                        // specifically for domain vlan update from 2.1.8 to 2.2.4
                        if (rs.next()) {
                            upgradeVersion = false;
                        }
                    } catch (SQLException e) {
                        throw new CloudRuntimeException("Unable to update the version table", e);
                    }
                }

                if (upgradeVersion) {
                    VersionVO version = new VersionVO(upgrade.getUpgradedVersion());
                    _dao.persist(version);
                }

                txn.commit();
            } catch (CloudRuntimeException e) {
                String errorMessage = "Unable to upgrade the database";
                s_logger.error(errorMessage, e);
                throw new CloudRuntimeException(errorMessage, e);
            } finally {
                txn.close();
            }
        }

        if (true) { // FIXME Needs to detect if management servers are running
            // !ClusterManagerImpl.arePeersRunning(trimmedCurrentVersion)) {
            s_logger.info("Cleaning upgrades because all management server are now at the same version");
            TreeMap<String, List<DbUpgrade>> upgradedVersions = new TreeMap<String, List<DbUpgrade>>();

            for (DbUpgrade upgrade : upgrades) {
                String upgradedVerson = upgrade.getUpgradedVersion();
                List<DbUpgrade> upgradeList = upgradedVersions.get(upgradedVerson);
                if (upgradeList == null) {
                    upgradeList = new ArrayList<DbUpgrade>();
                }
                upgradeList.add(upgrade);
                upgradedVersions.put(upgradedVerson, upgradeList);
            }

            for (String upgradedVersion : upgradedVersions.keySet()) {
                List<DbUpgrade> versionUpgrades = upgradedVersions.get(upgradedVersion);
                VersionVO version = _dao.findByVersion(upgradedVersion, Step.Upgrade);
                s_logger.debug("Upgrading to version " + upgradedVersion + "...");

                TransactionLegacy txn = TransactionLegacy.open("Cleanup");
                try {
                    if (version != null) {
                        for (DbUpgrade upgrade : versionUpgrades) {
                            s_logger.info("Cleanup upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" +
                                    upgrade.getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());

                            txn.start();

                            Connection conn;
                            try {
                                conn = txn.getConnection();
                            } catch (SQLException e) {
                                String errorMessage = "Unable to cleanup the database";
                                s_logger.error(errorMessage, e);
                                throw new CloudRuntimeException(errorMessage, e);
                            }

                            File[] scripts = upgrade.getCleanupScripts();
                            if (scripts != null) {
                                for (File script : scripts) {
                                    runScript(conn, script);
                                    s_logger.debug("Cleanup script " + script.getAbsolutePath() + " is executed successfully");
                                }
                            }
                            txn.commit();
                        }

                        txn.start();
                        version.setStep(Step.Complete);
                        s_logger.debug("Upgrade completed for version " + upgradedVersion);
                        version.setUpdated(new Date());
                        _dao.update(version.getId(), version);
                        txn.commit();
                    }
                } finally {
                    txn.close();
                }
            }
        }

    }

    @Override
    public void check() {
        GlobalLock lock = GlobalLock.getInternLock("DatabaseUpgrade");
        try {
            s_logger.info("Grabbing lock to check for database upgrade.");
            if (!lock.lock(20 * 60)) {
                throw new CloudRuntimeException("Unable to acquire lock to check for database integrity.");
            }

            try {
                String dbVersion = _dao.getCurrentVersion();
                String currentVersion = this.getClass().getPackage().getImplementationVersion();

                if (currentVersion == null)
                    return;

                s_logger.info("DB version = " + dbVersion + " Code Version = " + currentVersion);

                if (Version.compare(Version.trimToPatch(dbVersion), Version.trimToPatch(currentVersion)) > 0) {
                    throw new CloudRuntimeException("Database version " + dbVersion + " is higher than management software version " + currentVersion);
                }

                if (Version.compare(Version.trimToPatch(dbVersion), Version.trimToPatch(currentVersion)) == 0) {
                    s_logger.info("DB version and code version matches so no upgrade needed.");
                    return;
                }

                upgrade(dbVersion, currentVersion);
            } finally {
                lock.unlock();
            }
        } finally {
            lock.releaseRef();
        }
    }
}
