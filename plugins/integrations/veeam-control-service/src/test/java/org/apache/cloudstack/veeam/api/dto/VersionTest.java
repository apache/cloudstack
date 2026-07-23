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

package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.cloudstack.utils.CloudStackVersion;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class VersionTest {

    @Test
    public void fromPackageAndCSVersion_CompleteVersion_IncludesPackageAndCsFields() {
        CloudStackVersion csVersion = CloudStackVersion.parse("4.23.1.2");
        try (MockedStatic<VeeamControlService> mocked = Mockito.mockStatic(VeeamControlService.class)) {
            mocked.when(VeeamControlService::getPackageVersion).thenReturn("4.23.1.2");
            mocked.when(VeeamControlService::getCSVersion).thenReturn(csVersion);

            Version version = Version.fromPackageAndCSVersion(true);

            assertEquals("4.23.1.2", version.getFullVersion());
            assertEquals("4", version.getMajor());
            assertEquals("23", version.getMinor());
            assertEquals("1", version.getBuild());
            assertEquals("2", version.getRevision());
        }
    }

    @Test
    public void fromPackageAndCSVersion_IncompleteVersion_DoesNotSetFullVersion() {
        CloudStackVersion csVersion = CloudStackVersion.parse("4.23.1.2");
        try (MockedStatic<VeeamControlService> mocked = Mockito.mockStatic(VeeamControlService.class)) {
            mocked.when(VeeamControlService::getPackageVersion).thenReturn("4.23.1.2");
            mocked.when(VeeamControlService::getCSVersion).thenReturn(csVersion);

            Version version = Version.fromPackageAndCSVersion(false);

            assertNull(version.getFullVersion());
            assertEquals("4", version.getMajor());
            assertEquals("23", version.getMinor());
            assertEquals("1", version.getBuild());
            assertEquals("2", version.getRevision());
        }
    }

    @Test
    public void fromPackageAndCSVersion_NullCloudStackVersion_ReturnsWithoutNumericParts() {
        try (MockedStatic<VeeamControlService> mocked = Mockito.mockStatic(VeeamControlService.class)) {
            mocked.when(VeeamControlService::getPackageVersion).thenReturn("4.23.1.2");
            mocked.when(VeeamControlService::getCSVersion).thenReturn(null);

            Version version = Version.fromPackageAndCSVersion(true);

            assertEquals("4.23.1.2", version.getFullVersion());
            assertNull(version.getMajor());
            assertNull(version.getMinor());
            assertNull(version.getBuild());
            assertNull(version.getRevision());
        }
    }
}
