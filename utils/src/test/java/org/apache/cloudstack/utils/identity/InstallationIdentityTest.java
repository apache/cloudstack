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
package org.apache.cloudstack.utils.identity;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InstallationIdentityTest {

    private static final String VERSION = "4.23.0.0";
    private static final Date CREATED = Date.from(Instant.parse("2024-01-15T10:30:00Z"));

    private TimeZone defaultTimeZone;

    @Before
    public void setUp() {
        defaultTimeZone = TimeZone.getDefault();
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testIdentityIsA64CharacterLowerCaseHexSha256() {
        String identity = InstallationIdentity.generate(VERSION, CREATED);

        Assert.assertNotNull(identity);
        Assert.assertEquals(64, identity.length());
        Assert.assertTrue(identity, identity.matches("[0-9a-f]{64}"));
    }

    @Test
    public void testSameInputAlwaysYieldsTheSameIdentity() {
        Assert.assertEquals(InstallationIdentity.generate(VERSION, CREATED),
                InstallationIdentity.generate(VERSION, new Date(CREATED.getTime())));
    }

    /**
     * Two installations created at the same moment but from different versions, and
     * two installations of the same version created at different moments, must not
     * collide. The latter is what separates a reinstall from the cloud it replaced.
     */
    @Test
    public void testDifferentVersionOrTimestampYieldsADifferentIdentity() {
        String base = InstallationIdentity.generate(VERSION, CREATED);

        Assert.assertNotEquals(base, InstallationIdentity.generate("4.22.0.0", CREATED));
        Assert.assertNotEquals(base,
                InstallationIdentity.generate(VERSION, Date.from(Instant.parse("2024-01-15T10:30:01Z"))));
    }

    /**
     * The timestamp is formatted as UTC before hashing, so a Management Server in
     * Amsterdam and one in Tokyo sharing a database derive the same identity.
     */
    @Test
    public void testIdentityDoesNotDependOnTheDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        String utc = InstallationIdentity.generate(VERSION, CREATED);

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        String tokyo = InstallationIdentity.generate(VERSION, CREATED);

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        String losAngeles = InstallationIdentity.generate(VERSION, CREATED);

        Assert.assertEquals(utc, tokyo);
        Assert.assertEquals(utc, losAngeles);
    }

    /**
     * Sub-second precision is deliberately not part of the identity: the version
     * table stores a DATETIME, and rendering it to the second keeps the value stable
     * regardless of what the JDBC driver hands back.
     */
    @Test
    public void testIdentityIgnoresMilliseconds() {
        Assert.assertEquals(InstallationIdentity.generate(VERSION, CREATED),
                InstallationIdentity.generate(VERSION, new Date(CREATED.getTime() + 999)));
    }

    @Test
    public void testMissingInputYieldsNoIdentity() {
        Assert.assertNull(InstallationIdentity.generate(null, CREATED));
        Assert.assertNull(InstallationIdentity.generate("", CREATED));
        Assert.assertNull(InstallationIdentity.generate("   ", CREATED));
        Assert.assertNull(InstallationIdentity.generate(VERSION, null));
    }
}
