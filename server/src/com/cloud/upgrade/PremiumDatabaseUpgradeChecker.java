/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 *
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.upgrade;

import javax.ejb.Local;

import com.cloud.upgrade.dao.DbUpgrade;
import com.cloud.upgrade.dao.Upgrade217to218;
import com.cloud.upgrade.dao.Upgrade218to224DomainVlans;
import com.cloud.upgrade.dao.Upgrade218to22Premium;
import com.cloud.upgrade.dao.Upgrade2210to2211;
import com.cloud.upgrade.dao.Upgrade2211to2212Premium;
import com.cloud.upgrade.dao.Upgrade2212to2213;
import com.cloud.upgrade.dao.Upgrade2213to2214;
import com.cloud.upgrade.dao.Upgrade2214to30;
import com.cloud.upgrade.dao.Upgrade221to222Premium;
import com.cloud.upgrade.dao.Upgrade222to224Premium;
import com.cloud.upgrade.dao.Upgrade224to225;
import com.cloud.upgrade.dao.Upgrade225to226;
import com.cloud.upgrade.dao.Upgrade227to228Premium;
import com.cloud.upgrade.dao.Upgrade228to229;
import com.cloud.upgrade.dao.Upgrade229to2210;
import com.cloud.upgrade.dao.Upgrade30to301;
import com.cloud.upgrade.dao.UpgradeSnapshot217to224;
import com.cloud.upgrade.dao.UpgradeSnapshot223to224;
import com.cloud.upgrade.dao.VersionDaoImpl;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.SystemIntegrityChecker;

@Local(value = { SystemIntegrityChecker.class })
public class PremiumDatabaseUpgradeChecker extends DatabaseUpgradeChecker {
    public PremiumDatabaseUpgradeChecker() {
        _dao = ComponentLocator.inject(VersionDaoImpl.class);
        _upgradeMap.put("2.1.7", new DbUpgrade[] { new Upgrade217to218(), new Upgrade218to22Premium(),
                new Upgrade221to222Premium(), new UpgradeSnapshot217to224(), new Upgrade222to224Premium(),
                new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228Premium(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(),
                new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.1.8", new DbUpgrade[] { new Upgrade218to22Premium(), new Upgrade221to222Premium(),
                new UpgradeSnapshot217to224(), new Upgrade222to224Premium(), new Upgrade218to224DomainVlans(),
                new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228Premium(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(), new Upgrade2212to2213()
                , new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.1.9", new DbUpgrade[] { new Upgrade218to22Premium(), new Upgrade221to222Premium(),
                new UpgradeSnapshot217to224(), new Upgrade222to224Premium(), new Upgrade218to224DomainVlans(),
                new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228Premium(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.1", new DbUpgrade[] { new Upgrade221to222Premium(), new Upgrade222to224Premium(),
                new UpgradeSnapshot223to224(), new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228Premium(),
                new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(),
                new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.2", new DbUpgrade[] { new Upgrade222to224Premium(), new UpgradeSnapshot223to224(),
                new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228Premium(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.3", new DbUpgrade[] { new Upgrade222to224Premium(), new UpgradeSnapshot223to224(),
                new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228Premium(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.4", new DbUpgrade[] { new Upgrade224to225(), new Upgrade225to226(), new Upgrade227to228Premium(),
                new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(),
                new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.5", new DbUpgrade[] { new Upgrade225to226(), new Upgrade227to228Premium(), new Upgrade228to229(),
                new Upgrade229to2210(), new Upgrade2210to2211(), new Upgrade2211to2212Premium(),
                new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.6", new DbUpgrade[] { new Upgrade227to228Premium(), new Upgrade228to229(), new Upgrade229to2210(),
                new Upgrade2210to2211(), new Upgrade2211to2212Premium(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.7", new DbUpgrade[] { new Upgrade227to228Premium(), new Upgrade228to229(), new Upgrade229to2210(),
                new Upgrade2210to2211(), new Upgrade2211to2212Premium(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.8", new DbUpgrade[] { new Upgrade228to229(), new Upgrade229to2210(), new Upgrade2210to2211(),
                new Upgrade2211to2212Premium(), new Upgrade2212to2213(), new Upgrade2213to2214(),
                new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.9", new DbUpgrade[] { new Upgrade229to2210(), new Upgrade2210to2211(),
                new Upgrade2211to2212Premium(), new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
                new Upgrade30to301() });

        _upgradeMap.put("2.2.10", new DbUpgrade[] { new Upgrade2210to2211(), new Upgrade2211to2212Premium(),
                new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.11", new DbUpgrade[] { new Upgrade2211to2212Premium(), new Upgrade2212to2213(),
                new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.12", new DbUpgrade[] { new Upgrade2212to2213(), new Upgrade2213to2214(), new Upgrade2214to30(),
                new Upgrade30to301() });

        _upgradeMap.put("2.2.13", new DbUpgrade[] { new Upgrade2213to2214(), new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("2.2.14", new DbUpgrade[] { new Upgrade2214to30(), new Upgrade30to301() });

        _upgradeMap.put("3.0.0", new DbUpgrade[] { new Upgrade30to301() });
    }
}
