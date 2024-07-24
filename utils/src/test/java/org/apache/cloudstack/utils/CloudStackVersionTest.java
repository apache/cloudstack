//
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
//
package org.apache.cloudstack.utils;

import com.google.common.testing.EqualsTester;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(DataProviderRunner.class)
public final class CloudStackVersionTest {

    @Test
    @DataProvider({
        "1.2.3, 1.2.3",
        "1.2.3.4, 1.2.3.4",
        "1.2.3-12, 1.2.3",
        "1.2.3.4-14, 1.2.3.4"
    })
    public void testValidParse(final String inputValue, final String expectedVersion) {
        final CloudStackVersion version = CloudStackVersion.parse(inputValue);
        assertNotNull(version);
        assertEquals(expectedVersion, version.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    @DataProvider({
        "1.2",
        "1",
        "1.2.3.4.5",
        "aaaa",
        "",
        "  ",
        "1.2.3.4.5"
    })
    public void testInvalidParse(final String invalidValue) {
        CloudStackVersion.parse(invalidValue);
    }

    @Test
    @DataProvider({
        "1.0.0",
        "1.0.0.0",
        "1.2.3",
        "1.2.3.4"
    })
    public void testEquals(final String value) {

        final CloudStackVersion version = CloudStackVersion.parse(value);
        assertNotNull(version);

        final CloudStackVersion thatVersion = CloudStackVersion.parse(value);
        assertNotNull(thatVersion);

        new EqualsTester()
                .addEqualityGroup(version, thatVersion)
                .testEquals();

    }

    @Test
    @DataProvider({
        "1.0.0.0, 1.0.0.0",
        "1.0.0, 1.0.0",
        "1.0.0.0, 1.0.0",
        "1.0.0-10, 1.0.0-10",
        "1.0.0-10, 1.0.0",
        "1.0.0.0, 1.0.0-10",
        "1.0.0.0, 1.0.0.0-10",
        "1.0.0-10, 1.0.0-11",
        "1.0.0-10, 1.0.0.0-14",
        "1.0.0.0-14, 1.0.0.0-15",
        "1.0.0.0-SNAPSHOT, 1.0.0.0-SNAPSHOT",
        "1.0.0.0-branding, 1.0.0.0-branding",
        "1.0.0.0-1518453362, 1.0.0.0-1519453362"
    })
    public void testEqualCompareTo(final String value, final String thatValue) {

        final CloudStackVersion version = CloudStackVersion.parse(value);
        assertNotNull(version);

        final CloudStackVersion thatVersion = CloudStackVersion.parse(thatValue);
        assertNotNull(thatVersion);

        assertEquals(0, version.compareTo(thatVersion));
        assertEquals(0, thatVersion.compareTo(version));

    }

    @Test
    @DataProvider({
        "1.0.0.0, 1.0.0.0",
        "1.0.0, 1.0.0",
        "1.0.0.0, 1.0.0",
        "1.0.0-10, 1.0.0-10",
        "1.0.0-10, 1.0.0",
        "1.0.0.0, 1.0.0-10",
        "1.0.0.0, 1.0.0.0-10",
        "1.0.0-10, 1.0.0-11",
        "1.0.0-10, 1.0.0.0-14",
        "1.0.0.0-14, 1.0.0.0-15",
        "1.0.0.0-SNAPSHOT, 1.0.0.0-SNAPSHOT",
        "1.0.0.0-branding, 1.0.0.0-branding",
        "1.0.0.0-1518453362, 1.0.0.0-1519453362"
    })
    public void testEqualCompareDirect(final String value, final String thatValue) {

        assertEquals(0, CloudStackVersion.compare(value, thatValue));
        assertEquals(0, CloudStackVersion.compare(thatValue, value));

    }

    @Test
    @DataProvider({
        "1.2.3.4, 1.2.3",
        "1.2.3, 1.0.0.0",
        "1.2.3.4, 1.0.0",
        "2.0.0, 1.2.3",
        "2.0.0, 1.2.3.4",
        "2.0.0.0, 1.2.3",
        "2.0.0.0, 1.2.3.4",
        "2.0.0.0, 1.2.3",
        "1.3.0, 1.2.3.4",
        "1.3.0.0, 1.2.3.4",
        "1.3.0.0, 1.2.3",
        "1.2.3.4-10, 1.0.0.0-5",
        "1.2.3-10, 1.0.0-5",
        "1.2.3.4, 1.0.0.0-5",
        "1.2.3.4-10, 1.0.0"
    })
    public void testGreaterThanAndLessThanCompareTo(final String value, final String thatValue) {

        final CloudStackVersion version = CloudStackVersion.parse(value);
        assertNotNull(version);

        final CloudStackVersion thatVersion = CloudStackVersion.parse(thatValue);
        assertNotNull(thatVersion);

        assertEquals(1, version.compareTo(thatVersion));
        assertEquals(-1, thatVersion.compareTo(version));

    }

    @Test
    @DataProvider({
        "1.2.3.4, 1.2.3",
        "1.2.3, 1.0.0.0",
        "1.2.3.4, 1.0.0",
        "2.0.0, 1.2.3",
        "2.0.0, 1.2.3.4",
        "2.0.0.0, 1.2.3",
        "2.0.0.0, 1.2.3.4",
        "2.0.0.0, 1.2.3",
        "1.3.0, 1.2.3.4",
        "1.3.0.0, 1.2.3.4",
        "1.3.0.0, 1.2.3",
        "1.2.3.4-10, 1.0.0.0-5",
        "1.2.3-10, 1.0.0-5",
        "1.2.3.4, 1.0.0.0-5",
        "1.2.3.4-10, 1.0.0"
    })
    public void testGreaterThanAndLessThanCompareDirect(final String value, final String thatValue) {

        assertEquals(1, CloudStackVersion.compare(value, thatValue));
        assertEquals(-1, CloudStackVersion.compare(thatValue, value));

    }

    @Test
    @DataProvider({
        "Cloudstack Release 1.2.3 Mon Jan  1 10:10:10 UTC 2018, 1.2.3",
        "Cloudstack Release 1.2.3.4 Mon Jan  1 10:10:10 UTC 2018, 1.2.3.4",
        "Cloudstack Release 1.2.3-SNAPSHOT Mon Jan  1 10:10:10 UTC 2018, 1.2.3-SNAPSHOT",
        "Cloudstack Release 1.2.3.4-SNAPSHOT Mon Jan  1 10:10:10 UTC 2018, 1.2.3.4-SNAPSHOT",
        "Cloudstack Release 1.2.3.4-1519453362 Mon Jan  1 10:10:10 UTC 2018, 1.2.3.4-1519453362",
        "Cloudstack Release 1.2.3.4-brnading-SNAPSHOT Mon Jan  1 10:10:10 UTC 2018, 1.2.3.4-brnading-SNAPSHOT",
        "Cloudstack Release 1.2.3.4-brnading-1519453362 Mon Jan  1 10:10:10 UTC 2018, 1.2.3.4-brnading-1519453362",
        "Cloudstack Release 1.2 Mon Jan  1 10:10:10 UTC 2018, 0",
        "Cloudstack Release 1.2-SNAPSHOT Mon Jan  1 10:10:10 UTC 2018, 0",
        "Cloud stack Release 1.2.3.4 Mon Jan  1 10:10:10 UTC 2018, 0"
    })
    public void testTrimRouterVersion(final String value, final String expected) {

        assertEquals(expected, CloudStackVersion.trimRouterVersion(value));

    }

    private void verifyGetVMwareParentVersion(String hypervisorVersion, String expectedParentVersion) {
        if (expectedParentVersion == null) {
            Assert.assertNull(CloudStackVersion.getVMwareParentVersion(hypervisorVersion));
        } else {
            Assert.assertEquals(CloudStackVersion.getVMwareParentVersion(hypervisorVersion), expectedParentVersion);
        }
    }
    @Test
    public void testGetParentVersion() {
        verifyGetVMwareParentVersion(null, null);
        verifyGetVMwareParentVersion("6.5", null);
        verifyGetVMwareParentVersion("6.7.3", "6.7.3");
        verifyGetVMwareParentVersion("7.0.3.0", "7.0.3");
        verifyGetVMwareParentVersion("8.0", null);
        verifyGetVMwareParentVersion("8.0.0", "8.0");
        verifyGetVMwareParentVersion("8.0.0.2", "8.0");
        verifyGetVMwareParentVersion("8.0.1.0", "8.0.1");
    }
}
