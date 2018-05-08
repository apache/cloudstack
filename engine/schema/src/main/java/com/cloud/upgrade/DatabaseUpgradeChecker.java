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
import com.cloud.upgrade.dao.Upgrade41110to41200;
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
import com.cloud.upgrade.dao.Upgrade461to470;
import com.cloud.upgrade.dao.Upgrade470to471;
import com.cloud.upgrade.dao.Upgrade471to480;
import com.cloud.upgrade.dao.Upgrade480to481;
import com.cloud.upgrade.dao.Upgrade481to490;
import com.cloud.upgrade.dao.Upgrade490to4910;
import com.cloud.upgrade.dao.Upgrade4910to4920;
import com.cloud.upgrade.dao.Upgrade4920to4930;
import com.cloud.upgrade.dao.Upgrade4930to41000;
import com.cloud.upgrade.dao.Upgrade41000to41100;
import com.cloud.upgrade.dao.Upgrade41100to41110;
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
import com.google.common.collect.ImmutableList;
import org.apache.cloudstack.utils.CloudStackVersion;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.reverse;
import static com.google.common.collect.ObjectArrays.concat;
import static java.util.Collections.sort;

public class DatabaseUpgradeChecker implements SystemIntegrityChecker {
    private static final Logger s_logger = Logger.getLogger(DatabaseUpgradeChecker.class);
    private final ImmutableList<CloudStackVersion> availableVersions;
    protected Map<CloudStackVersion, DbUpgrade[]> _upgradeMap = new HashMap<>();

    @Inject
    VersionDao _dao;

    public DatabaseUpgradeChecker() {
        _dao = new VersionDaoImpl();

        _upgradeMap.put(CloudStackVersion.parse("2.1.7"),
            new DbUpgrade[] {new Upgrade217to218(), new Upgrade218to22(), new Upgrade221to222(), new UpgradeSnapshot217to224(), new Upgrade222to224(), new Upgrade224to225(),
                new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(),
                new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(),
                new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(),
                new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(),
                new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(),
                new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.1.8"),
            new DbUpgrade[] {new Upgrade218to22(), new Upgrade221to222(), new UpgradeSnapshot217to224(), new Upgrade222to224(), new Upgrade218to224DomainVlans(),
                new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
                new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(),
                new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
                new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(),
                new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.1.9"),
            new DbUpgrade[] {new Upgrade218to22(), new Upgrade221to222(), new UpgradeSnapshot217to224(), new Upgrade222to224(), new Upgrade218to224DomainVlans(),
                new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
                new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(),
                new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
                new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(),
                new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.1"),
            new DbUpgrade[] {new Upgrade221to222(), new UpgradeSnapshot223to224(), new Upgrade222to224(), new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(),
                new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(),
                new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
                new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(),
                new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(),
                new Upgrade41100to41110(),  new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.2"),
            new DbUpgrade[] {new Upgrade222to224(), new UpgradeSnapshot223to224(), new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
                new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
                new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(),
                new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
                new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.3"),
            new DbUpgrade[] {new Upgrade222to224(), new UpgradeSnapshot223to224(), new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
                new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
                new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(),
                new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
               new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.4"),
            new DbUpgrade[] {new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
                new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(),
                new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
                new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(),
                new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.5"),
            new DbUpgrade[] {new Upgrade225to226(), new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(),
                new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(),
                new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(),
                new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(),
                new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(),
                new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.6"),
            new DbUpgrade[] {new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(),
                new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(),
                new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(),
                new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.7"),
            new DbUpgrade[] {new Upgrade227to228(), new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(),
                new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(),
                new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(),
                new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.8"),
            new DbUpgrade[] {new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(),
                new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
                new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(),
                new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.9"),
            new DbUpgrade[] {new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
                new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
                new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(),
                new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
                new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.10"),
            new DbUpgrade[] {new Upgrade2210to2211(), new Upgrade2211to2212(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(),
                new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(),
                new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(),
                new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.12"),
            new DbUpgrade[] {new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(),
                new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
                new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(),
                new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.13"),
            new DbUpgrade[] {new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(),
                new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(),
                new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(),
                new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(),
                new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.14"),
            new DbUpgrade[] {new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(),
                new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(),
                new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(),
                new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("3.0.0"),
            new DbUpgrade[] {new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(),
                new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(),
                new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("3.0.1"),
            new DbUpgrade[] {new Upgrade301to302(), new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
                new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(),
                new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
                new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("3.0.2"),
            new DbUpgrade[] {new Upgrade302to40(), new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(),
                new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(),
                new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.0.0"),
            new DbUpgrade[] {new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(),
                new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(),
                new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.0.1"),
            new DbUpgrade[] {new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(),
                new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(),
                new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.0.2"),
            new DbUpgrade[] {new Upgrade40to41(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(),
                new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(),
                new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.1.0"),
            new DbUpgrade[] {new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
                new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(),
                new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.1.1"),
            new DbUpgrade[] {new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(),
                new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(),
                new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.2.0"),
            new DbUpgrade[] {new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(),
                new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(),
                new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(),
                new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.2.1"),
            new DbUpgrade[] {new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(),
                new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(),
                new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.3.0"),
            new DbUpgrade[] {new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(),
                new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.3.1"),
            new DbUpgrade[] {new Upgrade431to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(),
                new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.3.2"),
            new DbUpgrade[] {new Upgrade432to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(),
                new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.4.0"),
            new DbUpgrade[] {new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(),
                new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
                new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.4.1"),
            new DbUpgrade[] {new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(),
                new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.4.2"),
            new DbUpgrade[] {new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(),
                new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.4.3"),
            new DbUpgrade[] {new Upgrade443to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(),
                new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.4.4"),
            new DbUpgrade[] {new Upgrade444to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(),
                new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.5.0"),
            new DbUpgrade[] {new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(),
                new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.5.1"),
            new DbUpgrade[] {new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(),
                new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(),
                new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.5.2"),
            new DbUpgrade[] {new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(),
                new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.5.3"),
            new DbUpgrade[] {new Upgrade453to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(),
                new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.6.0"),
            new DbUpgrade[] {new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.6.1"),
            new DbUpgrade[] {new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
                new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.6.2"),
            new DbUpgrade[] {new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
                new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.7.0"),
            new DbUpgrade[] {new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.7.1"),
            new DbUpgrade[] {new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.7.2"),
            new DbUpgrade[] {new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(),
                new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.8.0"),
            new DbUpgrade[] {new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(),
                new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.8.1"),
            new DbUpgrade[] {new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.8.2.0"),
            new DbUpgrade[] {new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.9.0"),
            new DbUpgrade[] {new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.9.1.0"),
            new DbUpgrade[] {new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.9.2.0"),
            new DbUpgrade[] {new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.9.3.0"),
            new DbUpgrade[] {new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.9.3.1"),
            new DbUpgrade[] {new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.10.0.0"),
            new DbUpgrade[] {new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.11.0.0"),
                new DbUpgrade[] {new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("4.11.1.0"),
                new DbUpgrade[] {new Upgrade41110to41200()});

        //CP Upgrades
        _upgradeMap.put(CloudStackVersion.parse("3.0.3"),
            new DbUpgrade[] {new Upgrade303to304(), new Upgrade304to305(), new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(),
                new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(),
                new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(),
                new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("3.0.4"),
            new DbUpgrade[] {new Upgrade304to305(), new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(),
                new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(),
                new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(),
                new Upgrade490to4910(), new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("3.0.5"),
            new DbUpgrade[] {new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(),
                new Upgrade430to440(), new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(),
                new Upgrade460to461(), new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(),
                new Upgrade4910to4920(), new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("3.0.6"),
            new DbUpgrade[] {new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(),
                new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(),
                new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("3.0.7"),
            new DbUpgrade[] {new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(), new Upgrade440to441(),
                new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(), new Upgrade461to470(),
                new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.15"),
            new DbUpgrade[] {new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to303(), new Upgrade303to304(), new Upgrade304to305(),
                new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(),
                new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(),
                new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        _upgradeMap.put(CloudStackVersion.parse("2.2.16"),
            new DbUpgrade[] {new Upgrade2214to30(), new Upgrade30to301(), new Upgrade301to302(), new Upgrade302to303(), new Upgrade303to304(), new Upgrade304to305(),
                new Upgrade305to306(), new Upgrade306to307(), new Upgrade307to410(), new Upgrade410to420(), new Upgrade420to421(), new Upgrade421to430(), new Upgrade430to440(),
                new Upgrade440to441(), new Upgrade441to442(), new Upgrade442to450(), new Upgrade450to451(), new Upgrade451to452(), new Upgrade452to460(), new Upgrade460to461(),
                new Upgrade461to470(), new Upgrade470to471(), new Upgrade471to480(), new Upgrade480to481(), new Upgrade481to490(), new Upgrade490to4910(), new Upgrade4910to4920(),
                new Upgrade4920to4930(), new Upgrade4930to41000(), new Upgrade41000to41100(), new Upgrade41100to41110(), new Upgrade41110to41200()});

        final List<CloudStackVersion> sortedVersions = newArrayList(_upgradeMap.keySet());
        sort(sortedVersions);

        availableVersions = ImmutableList.copyOf(sortedVersions);
    }

    protected void runScript(Connection conn, InputStream file) {

        try (InputStreamReader reader = new InputStreamReader(file)) {
            ScriptRunner runner = new ScriptRunner(conn, false, true);
            runner.runScript(reader);
        } catch (IOException e) {
            s_logger.error("Unable to read upgrade script", e);
            throw new CloudRuntimeException("Unable to read upgrade script", e);
        } catch (SQLException e) {
            s_logger.error("Unable to execute upgrade script", e);
            throw new CloudRuntimeException("Unable to execute upgrade script", e);
        }

    }

    /**
     *
     * Calculates an upgrade path for the passed <code>dbVersion</code>.  The calculation assumes that the
     * <code>dbVersion</code> required no schema migrations or data conversions and no upgrade path was defined
     * for it.  Therefore, we find the most recent version with database migrations before the <code>dbVersion</code>
     * and adopt that list.
     *
     * @param dbVersion The version from which the upgrade will occur
     *
     * @return The upgrade path from <code>dbVersion</code> to <code>currentVersion</code>
     *
     * @since 4.8.2.0
     *
     */
    private DbUpgrade[] findMostRecentUpgradePath(final CloudStackVersion dbVersion) {

        // Find the most recent version before dbVersion
        for (CloudStackVersion version : reverse(availableVersions)) {
            if (dbVersion.compareTo(version) < 0) {
                return _upgradeMap.get(version);
            }
        }

        // The current version was the latest and didn't have any migrations ...
        return new DbUpgrade[0];

    }

    // Default visibility to support unit testing ...
    DbUpgrade[] calculateUpgradePath(final CloudStackVersion dbVersion, final CloudStackVersion currentVersion) {

        checkArgument(dbVersion != null);
        checkArgument(currentVersion != null);
        checkArgument(currentVersion.compareTo(dbVersion) > 0);

        final DbUpgrade[] upgrades = _upgradeMap.containsKey(dbVersion) ? _upgradeMap.get(dbVersion) : findMostRecentUpgradePath(dbVersion);

        // When there is no upgrade defined for the target version, we assume that there were no schema changes or
        // data migrations required.  Based on that assumption, we add a noop DbUpgrade to the end of the list ...
        final CloudStackVersion tailVersion = upgrades.length > 0 ? CloudStackVersion.parse(upgrades[upgrades.length - 1].getUpgradedVersion()) : dbVersion;

        if (currentVersion.compareTo(tailVersion) != 0) {
            return concat(upgrades, new NoopDbUpgrade(tailVersion, currentVersion));
        }

        return upgrades;

    }

    protected void upgrade(CloudStackVersion dbVersion, CloudStackVersion currentVersion) {
        s_logger.info("Database upgrade must be performed from " + dbVersion + " to " + currentVersion);

        final DbUpgrade[] upgrades = calculateUpgradePath(dbVersion, currentVersion);

        for (DbUpgrade upgrade : upgrades) {
            VersionVO version;
            s_logger.debug("Running upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" + upgrade
                .getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());
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
                InputStream[] scripts = upgrade.getPrepareScripts();
                if (scripts != null) {
                    for (InputStream script : scripts) {
                        runScript(conn, script);
                    }
                }

                upgrade.performDataMigration(conn);

                version = new VersionVO(upgrade.getUpgradedVersion());
                version = _dao.persist(version);

                txn.commit();
            } catch (CloudRuntimeException e) {
                String errorMessage = "Unable to upgrade the database";
                s_logger.error(errorMessage, e);
                throw new CloudRuntimeException(errorMessage, e);
            } finally {
                txn.close();
            }

            // Run the corresponding '-cleanup.sql' script
            txn = TransactionLegacy.open("Cleanup");
            try {
                s_logger.info("Cleanup upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" + upgrade
                    .getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());

                txn.start();
                Connection conn;
                try {
                    conn = txn.getConnection();
                } catch (SQLException e) {
                    s_logger.error("Unable to cleanup the database", e);
                    throw new CloudRuntimeException("Unable to cleanup the database", e);
                }

                InputStream[] scripts = upgrade.getCleanupScripts();
                if (scripts != null) {
                    for (InputStream script : scripts) {
                        runScript(conn, script);
                        s_logger.debug("Cleanup script " + upgrade.getClass().getSimpleName() + " is executed successfully");
                    }
                }
                txn.commit();

                txn.start();
                version.setStep(Step.Complete);
                version.setUpdated(new Date());
                _dao.update(version.getId(), version);
                txn.commit();
                s_logger.debug("Upgrade completed for version " + version.getVersion());
            } finally {
                txn.close();
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

                final CloudStackVersion dbVersion = CloudStackVersion.parse(_dao.getCurrentVersion());
                final String currentVersionValue = this.getClass().getPackage().getImplementationVersion();

                if (StringUtils.isBlank(currentVersionValue)) {
                    return;
                }

                final CloudStackVersion currentVersion = CloudStackVersion.parse(currentVersionValue);
                s_logger.info("DB version = " + dbVersion + " Code Version = " + currentVersion);

                if (dbVersion.compareTo(currentVersion) > 0) {
                    throw new CloudRuntimeException("Database version " + dbVersion + " is higher than management software version " + currentVersionValue);
                }

                if (dbVersion.compareTo(currentVersion) == 0) {
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

    private static final class NoopDbUpgrade implements DbUpgrade {

        private final String upgradedVersion;
        private final String[] upgradeRange;

        private NoopDbUpgrade(final CloudStackVersion fromVersion, final CloudStackVersion toVersion) {

            super();

            upgradedVersion = toVersion.toString();
            upgradeRange = new String[] {fromVersion.toString(), toVersion.toString()};

        }

        @Override
        public String[] getUpgradableVersionRange() {
            return Arrays.copyOf(upgradeRange, upgradeRange.length);
        }

        @Override
        public String getUpgradedVersion() {
            return upgradedVersion;
        }

        @Override
        public boolean supportsRollingUpgrade() {
            return false;
        }

        @Override
        public InputStream[] getPrepareScripts() {
            return new InputStream[0];
        }

        @Override
        public void performDataMigration(Connection conn) {

        }

        @Override
        public InputStream[] getCleanupScripts() {
            return new InputStream[0];
        }

    }
}
