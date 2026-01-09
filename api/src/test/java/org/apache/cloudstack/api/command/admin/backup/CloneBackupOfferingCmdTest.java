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

package org.apache.cloudstack.api.command.admin.backup;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.backup.BackupManager;
import org.apache.cloudstack.backup.BackupOffering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloneBackupOfferingCmdTest {

    private CloneBackupOfferingCmd cloneBackupOfferingCmd;

    @Mock
    private BackupManager backupManager;

    @Mock
    private ResponseGenerator responseGenerator;

    @Mock
    private BackupOffering mockBackupOffering;

    @Mock
    private BackupOfferingResponse mockBackupOfferingResponse;

    @Before
    public void setUp() {
        cloneBackupOfferingCmd = new CloneBackupOfferingCmd();
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "backupManager", backupManager);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "_responseGenerator", responseGenerator);
    }

    @Test
    public void testGetSourceOfferingId() {
        Long sourceOfferingId = 999L;
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", sourceOfferingId);
        assertEquals(sourceOfferingId, cloneBackupOfferingCmd.getSourceOfferingId());
    }

    @Test
    public void testGetName() {
        String name = "ClonedBackupOffering";
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", name);
        assertEquals(name, cloneBackupOfferingCmd.getName());
    }

    @Test
    public void testGetDescription() {
        String description = "Cloned Backup Offering Description";
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", description);
        assertEquals(description, cloneBackupOfferingCmd.getDescription());
    }

    @Test
    public void testGetZoneId() {
        Long zoneId = 123L;
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "zoneId", zoneId);
        assertEquals(zoneId, cloneBackupOfferingCmd.getZoneId());
    }

    @Test
    public void testGetExternalId() {
        String externalId = "external-backup-123";
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "externalId", externalId);
        assertEquals(externalId, cloneBackupOfferingCmd.getExternalId());
    }

    @Test
    public void testGetAllowUserDrivenBackups() {
        Boolean allowUserDrivenBackups = true;
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "userDrivenBackups", allowUserDrivenBackups);
        assertEquals(allowUserDrivenBackups, cloneBackupOfferingCmd.getUserDrivenBackups());
    }

    @Test
    public void testAllowUserDrivenBackupsDefaultTrue() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "userDrivenBackups", null);
        Boolean result = cloneBackupOfferingCmd.getUserDrivenBackups();
        assertTrue(result == null || result);
    }

    @Test
    public void testAllowUserDrivenBackupsFalse() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "userDrivenBackups", false);
        assertEquals(Boolean.FALSE, cloneBackupOfferingCmd.getUserDrivenBackups());
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        Long sourceOfferingId = 999L;
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", sourceOfferingId);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");

        when(backupManager.cloneBackupOffering(any(CloneBackupOfferingCmd.class))).thenReturn(mockBackupOffering);
        when(responseGenerator.createBackupOfferingResponse(mockBackupOffering)).thenReturn(mockBackupOfferingResponse);

        cloneBackupOfferingCmd.execute();

        assertNotNull(cloneBackupOfferingCmd.getResponseObject());
        assertEquals(mockBackupOfferingResponse, cloneBackupOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteFailure() throws Exception {
        Long sourceOfferingId = 999L;
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(backupManager.cloneBackupOffering(any(CloneBackupOfferingCmd.class))).thenReturn(null);

        try {
            cloneBackupOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Failed to clone backup offering", e.getMessage());
        }
    }

    @Test
    public void testExecuteWithInvalidParameterException() throws Exception {
        Long sourceOfferingId = 999L;
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(backupManager.cloneBackupOffering(any(CloneBackupOfferingCmd.class)))
            .thenThrow(new InvalidParameterValueException("Invalid source offering ID"));

        try {
            cloneBackupOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.PARAM_ERROR, e.getErrorCode());
            assertEquals("Invalid source offering ID", e.getMessage());
        }
    }

    @Test
    public void testExecuteWithCloudRuntimeException() throws Exception {
        Long sourceOfferingId = 999L;
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(backupManager.cloneBackupOffering(any(CloneBackupOfferingCmd.class)))
            .thenThrow(new CloudRuntimeException("Runtime error during clone"));

        try {
            cloneBackupOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Runtime error during clone", e.getMessage());
        }
    }

    @Test
    public void testExecuteSuccessWithAllParameters() throws Exception {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", 999L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", "Test Description");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "zoneId", 123L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "externalId", "ext-123");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "userDrivenBackups", true);

        when(backupManager.cloneBackupOffering(any(CloneBackupOfferingCmd.class))).thenReturn(mockBackupOffering);
        when(responseGenerator.createBackupOfferingResponse(mockBackupOffering)).thenReturn(mockBackupOfferingResponse);

        cloneBackupOfferingCmd.execute();

        assertNotNull(cloneBackupOfferingCmd.getResponseObject());
        assertEquals(mockBackupOfferingResponse, cloneBackupOfferingCmd.getResponseObject());
    }

    @Test
    public void testCloneWithAllParameters() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", 999L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", "Cloned backup offering for testing");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "zoneId", 123L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "externalId", "external-backup-123");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "userDrivenBackups", true);

        assertEquals(Long.valueOf(999L), cloneBackupOfferingCmd.getSourceOfferingId());
        assertEquals("ClonedBackupOffering", cloneBackupOfferingCmd.getName());
        assertEquals("Cloned backup offering for testing", cloneBackupOfferingCmd.getDescription());
        assertEquals(Long.valueOf(123L), cloneBackupOfferingCmd.getZoneId());
        assertEquals("external-backup-123", cloneBackupOfferingCmd.getExternalId());
        assertEquals(Boolean.TRUE, cloneBackupOfferingCmd.getUserDrivenBackups());
    }

    @Test
    public void testCloneWithMinimalParameters() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", 999L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", "Description");

        assertEquals(Long.valueOf(999L), cloneBackupOfferingCmd.getSourceOfferingId());
        assertEquals("ClonedBackupOffering", cloneBackupOfferingCmd.getName());
        assertEquals("Description", cloneBackupOfferingCmd.getDescription());

        assertNull(cloneBackupOfferingCmd.getZoneId());
        assertNull(cloneBackupOfferingCmd.getExternalId());
    }

    @Test
    public void testSourceOfferingIdNullByDefault() {
        assertNull(cloneBackupOfferingCmd.getSourceOfferingId());
    }

    @Test
    public void testNameNullByDefault() {
        assertNull(cloneBackupOfferingCmd.getName());
    }

    @Test
    public void testDescriptionNullByDefault() {
        assertNull(cloneBackupOfferingCmd.getDescription());
    }

    @Test
    public void testZoneIdNullByDefault() {
        assertNull(cloneBackupOfferingCmd.getZoneId());
    }

    @Test
    public void testExternalIdNullByDefault() {
        assertNull(cloneBackupOfferingCmd.getExternalId());
    }

    @Test
    public void testCloneBackupOfferingInheritingZone() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", 999L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", "Clone with inherited zone");

        assertEquals(Long.valueOf(999L), cloneBackupOfferingCmd.getSourceOfferingId());
        assertNull(cloneBackupOfferingCmd.getZoneId());
    }

    @Test
    public void testCloneBackupOfferingInheritingExternalId() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", 999L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", "Clone with inherited external ID");

        assertEquals(Long.valueOf(999L), cloneBackupOfferingCmd.getSourceOfferingId());
        assertNull(cloneBackupOfferingCmd.getExternalId());
    }

    @Test
    public void testCloneBackupOfferingOverridingZone() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", 999L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", "Clone with new zone");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "zoneId", 456L);

        assertEquals(Long.valueOf(999L), cloneBackupOfferingCmd.getSourceOfferingId());
        assertEquals(Long.valueOf(456L), cloneBackupOfferingCmd.getZoneId());
    }

    @Test
    public void testCloneBackupOfferingDisallowUserDrivenBackups() {
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "sourceOfferingId", 999L);
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "name", "ClonedBackupOffering");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "description", "Clone without user-driven backups");
        ReflectionTestUtils.setField(cloneBackupOfferingCmd, "userDrivenBackups", false);

        assertEquals(Boolean.FALSE, cloneBackupOfferingCmd.getUserDrivenBackups());
    }
}
