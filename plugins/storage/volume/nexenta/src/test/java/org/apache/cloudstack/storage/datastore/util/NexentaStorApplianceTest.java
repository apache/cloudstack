/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.util;

import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.IscsiTarget;
import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.ListOfIscsiTargetsNmsResponse;
import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.CreateIscsiTargetRequestParams;
import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.ListOfStringsNmsResponse;
import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.IntegerNmsResponse;
import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.LuParams;
import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.MappingEntry;
import static org.apache.cloudstack.storage.datastore.util.NexentaStorAppliance.AddMappingEntryNmsResponse;
import static org.apache.cloudstack.storage.datastore.util.NexentaNmsClient.NmsResponse;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class NexentaStorApplianceTest {
    private NexentaNmsClient client;

    private NexentaStorAppliance appliance;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void init() {
        final String url = "nmsUrl=https://admin:nexenta@10.1.3.182:8457;volume=cloudstack;storageType=iscsi";
        NexentaUtil.NexentaPluginParameters parameters = NexentaUtil.parseNexentaPluginUrl(url);
        //client = new NexentaNmsClient(parameters.getNmsUrl());
        client = mock(NexentaNmsClient.class);
        appliance = new NexentaStorAppliance(client, parameters);
    }

    @Test
    public void testIsIscsiTargetExists() {
        final String targetName = NexentaStorAppliance.getTargetName("volume1");

        when(client.execute(ListOfIscsiTargetsNmsResponse.class, "stmf", "list_targets")).thenReturn(null);
        assertFalse(appliance.isIscsiTargetExists(targetName));

        when(client.execute(ListOfIscsiTargetsNmsResponse.class, "stmf", "list_targets")).thenReturn(new ListOfIscsiTargetsNmsResponse());
        assertFalse(appliance.isIscsiTargetExists(targetName));

        final HashMap<String, IscsiTarget> result = new HashMap<String, IscsiTarget>();

        result.put("any", new IscsiTarget("Online", "iSCSI", "any", "0", "-", "iscsit"));
        when(client.execute(ListOfIscsiTargetsNmsResponse.class, "stmf", "list_targets")).thenReturn(new ListOfIscsiTargetsNmsResponse(result));
        assertFalse(appliance.isIscsiTargetExists(targetName));

        result.put(targetName, new IscsiTarget("Online", "iSCSI", targetName, "0", "-", "iscsit"));
        when(client.execute(ListOfIscsiTargetsNmsResponse.class, "stmf", "list_targets")).thenReturn(new ListOfIscsiTargetsNmsResponse(result));
        assertTrue(appliance.isIscsiTargetExists(targetName));
    }

    final static String ISCSI_TARGET_ALREADY_CONFIGURED_ERROR = "Unable to create iscsi target\\n iSCSI target %s already configured\\n itadm create-target failed with error " +
            "17\\n";

    @Test
    public void testCreateIscsiTarget() {
        final String targetName = NexentaStorAppliance.getTargetName("volume1");
        final CreateIscsiTargetRequestParams p = new CreateIscsiTargetRequestParams(targetName);

        appliance.createIscsiTarget(targetName);
        verify(client).execute(NmsResponse.class, "iscsitarget", "create_target", p);

        final String error = String.format(ISCSI_TARGET_ALREADY_CONFIGURED_ERROR, targetName);
        when(client.execute(NmsResponse.class, "iscsitarget", "create_target", p)).thenThrow(new CloudRuntimeException(error));
        appliance.createIscsiTarget(targetName);
    }

    @Test
    public void testCreateIscsiTargetFails() {
        final String targetName = NexentaStorAppliance.getTargetName("volume1");
        final CreateIscsiTargetRequestParams p = new CreateIscsiTargetRequestParams(targetName);
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        when(client.execute(NmsResponse.class, "iscsitarget", "create_target", p)).thenThrow(new CloudRuntimeException("any exception"));
        appliance.createIscsiTarget(targetName);
    }

    @Test
    public void testIsIscsiTargetGroupExists() {
        final String targetGroup = NexentaStorAppliance.getTargetGroupName("volume1");

        when(client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroups")).thenReturn(null);
        assertFalse(appliance.isIscsiTargetGroupExists(targetGroup));

        assertFalse(appliance.isIscsiTargetGroupExists(targetGroup));

        LinkedList<String> result = new LinkedList<String>();

        result.add("any");
        when(client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroups")).thenReturn(new ListOfStringsNmsResponse(result));
        assertFalse(appliance.isIscsiTargetGroupExists(targetGroup));

        result.add(targetGroup);
        when(client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroups")).thenReturn(new ListOfStringsNmsResponse(result));
        assertTrue(appliance.isIscsiTargetGroupExists(targetGroup));
    }

    final static String ISCSI_TARGET_GROUP_EXISTS_ERROR = "Unable to create targetgroup: stmfadm: %s: already exists\\n";

    @Test
    public void testCreateIscsiTargetGroup() {
        final String targetGroupName = NexentaStorAppliance.getTargetGroupName("volume1");

        appliance.createIscsiTargetGroup(targetGroupName);
        verify(client).execute(NmsResponse.class, "stmf", "create_targetgroup", targetGroupName);

        final String error = String.format(ISCSI_TARGET_GROUP_EXISTS_ERROR, targetGroupName);
        when(client.execute(NmsResponse.class, "stmf", "create_targetgroup", targetGroupName)).thenThrow(new CloudRuntimeException(error));
        appliance.createIscsiTargetGroup(targetGroupName);
    }

    @Test
    public void testCreateIscsiTargetGroupFails() {
        final String targetGroupName = NexentaStorAppliance.getTargetGroupName("volume1");
        when(client.execute(NmsResponse.class, "stmf", "create_targetgroup", targetGroupName)).thenThrow(new CloudRuntimeException("any exception"));
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        appliance.createIscsiTargetGroup(targetGroupName);
    }

    @Test
    public void testIsMemberOfTargetGroup() {
        final String targetName = NexentaStorAppliance.getTargetName("volume1");
        final String targetGroupName = NexentaStorAppliance.getTargetGroupName("volume1");

        when(client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroup_members", targetGroupName)).thenReturn(null);
        assertFalse(appliance.isTargetMemberOfTargetGroup(targetGroupName, targetName));

        when(client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroup_members", targetGroupName)).thenReturn(new ListOfStringsNmsResponse());
        assertFalse(appliance.isTargetMemberOfTargetGroup(targetGroupName, targetName));

        LinkedList<String> result = new LinkedList<String>();

        result.add("any");
        when(client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroup_members", targetGroupName)).thenReturn(new ListOfStringsNmsResponse(result));
        assertFalse(appliance.isTargetMemberOfTargetGroup(targetGroupName, targetName));

        result.add(targetName);
        when(client.execute(ListOfStringsNmsResponse.class, "stmf", "list_targetgroup_members", targetGroupName)).thenReturn(new ListOfStringsNmsResponse(result));
        assertTrue(appliance.isTargetMemberOfTargetGroup(targetGroupName, targetName));
    }

    @Test
    public void testAddTargetGroupMember() {
        final String targetName = NexentaStorAppliance.getTargetName("volume1");
        final String targetGroupName = NexentaStorAppliance.getTargetGroupName("volume1");

        appliance.addTargetGroupMember(targetGroupName, targetName);
        verify(client).execute(NmsResponse.class, "stmf", "add_targetgroup_member", targetGroupName, targetName);

        String error = String.format(ISCSI_TARGET_ALREADY_EXISTS_IN_TARGET_GROUP_ERROR, targetName);
        when(client.execute(NmsResponse.class, "stmf", "add_targetgroup_member", targetGroupName, targetName)).thenThrow(new CloudRuntimeException(error));
        appliance.addTargetGroupMember(targetGroupName, targetName);
    }

    final static String ISCSI_TARGET_ALREADY_EXISTS_IN_TARGET_GROUP_ERROR = "Unable to add member to targetgroup: stmfadm: %s: already exists\\n";

    @Test
    public void testAddTargetGroupMemberFails() {
        final String targetName = NexentaStorAppliance.getTargetName("volume1");
        final String targetGroupName = NexentaStorAppliance.getTargetGroupName("volume1");

        when(client.execute(NmsResponse.class, "stmf", "add_targetgroup_member", targetGroupName, targetName)).thenThrow(new CloudRuntimeException("any exception"));
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        appliance.addTargetGroupMember(targetGroupName, targetName);
    }

    @Test
    public void testIsLuExists() {
        final String volumeName = appliance.getVolumeName("volume1");
        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_exists", volumeName)).thenReturn(null);
        assertFalse(appliance.isLuExists(volumeName));

        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_exists", volumeName)).thenReturn(new IntegerNmsResponse(0));
        assertFalse(appliance.isLuExists(volumeName));

        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_exists", volumeName)).thenReturn(new IntegerNmsResponse(1));
        assertTrue(appliance.isLuExists(volumeName));

        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_exists", volumeName)).thenThrow(new CloudRuntimeException("does not exist"));
        assertFalse(appliance.isLuExists(volumeName));
    }

    @Test
    public void testIsLuExistsFails() {
        final String volumeName = appliance.getVolumeName("volume1");
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_exists", volumeName)).thenThrow(new CloudRuntimeException("any exception"));
        assertTrue(appliance.isLuExists(volumeName));
    }

    final static String CREATE_LU_IN_USE_ERROR = "Unable to create lu with " +
            "zvol '%s':\\n stmfadm: filename /dev/zvol/rdsk/%s: in use\\n";

    @Test
    public void testCreateLu() {
        final String luName = appliance.getVolumeName("volume1");
        final LuParams p = new LuParams();

        appliance.createLu(luName);
        verify(client).execute(NmsResponse.class, "scsidisk", "create_lu", luName, p);

        String error = String.format(CREATE_LU_IN_USE_ERROR, luName, luName);
        when(client.execute(NmsResponse.class, "scsidisk", "create_lu", luName, p)).thenThrow(new CloudRuntimeException(error));
        appliance.createLu(luName);
    }

    @Test
    public void testCreateLuFails() {
        final String luName = appliance.getVolumeName("volume1");
        when(client.execute(NmsResponse.class, "scsidisk", "create_lu", luName, new LuParams())).thenThrow(new CloudRuntimeException("any exception"));
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        appliance.createLu(luName);
    }

    final static String ZVOL_DOES_NOT_EXISTS_ERROR = "Zvol '%s' does not exist";

    @Test
    public void testIsLuShared() {
        final String luName = appliance.getVolumeName("volume1");
        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_shared", luName)).thenReturn(null);
        assertFalse(appliance.isLuShared(luName));

        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_shared", luName)).thenReturn(new IntegerNmsResponse(0));
        assertFalse(appliance.isLuShared(luName));

        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_shared", luName)).thenReturn(new IntegerNmsResponse(1));
        assertTrue(appliance.isLuShared(luName));

        final String error = String.format(ZVOL_DOES_NOT_EXISTS_ERROR, luName);
        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_shared", luName)).thenThrow(new CloudRuntimeException(error));
        assertFalse(appliance.isLuShared(luName));
    }

    @Test
    public void testIsLuSharedFails() {
        final String luName = appliance.getVolumeName("volume1");
        when(client.execute(IntegerNmsResponse.class, "scsidisk", "lu_shared", luName)).thenThrow(new CloudRuntimeException("any exception"));
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        appliance.isLuShared(luName);
    }

    final static String ADD_LUN_MAPPING_ENTRY_ERROR = "(rc: 256) Unable to " +
            "add view to zvol '%s':\\n add-view: view already exists\\n";

    @Test
    public void testAddLuMappingEntry() {
        final String luName = appliance.getVolumeName("volume1");
        final String targetGroupName = NexentaStorAppliance.getTargetGroupName("volume1");
        final MappingEntry mappingEntry = new MappingEntry(targetGroupName, "0");
        appliance.addLuMappingEntry(luName, targetGroupName);
        verify(client).execute(AddMappingEntryNmsResponse.class, "scsidisk", "add_lun_mapping_entry", luName, mappingEntry);

        String error = String.format(ADD_LUN_MAPPING_ENTRY_ERROR, luName);
        when(client.execute(AddMappingEntryNmsResponse.class, "scsidisk", "add_lun_mapping_entry", luName, mappingEntry)).thenThrow(new CloudRuntimeException(error));
        appliance.addLuMappingEntry(luName, targetGroupName);
    }

    @Test
    public void testAddLuMappingEntryTest() {
        final String luName = appliance.getVolumeName("volume1");
        final String targetGroupName = NexentaStorAppliance.getTargetGroupName("volume1");
        final MappingEntry mappingEntry = new MappingEntry(targetGroupName, "0");
        when(client.execute(AddMappingEntryNmsResponse.class, "scsidisk", "add_lun_mapping_entry", luName, mappingEntry)).thenThrow(new CloudRuntimeException("any exception"));
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        appliance.addLuMappingEntry(luName, targetGroupName);
    }

    @Test
    public void testCreateIscsiVolume() {
        final String volumeName = "volume1";
        final Long volumeSize = Long.valueOf(1);
        appliance.createIscsiVolume(volumeName, volumeSize);
    }

    @Test
    public void testDeleteIscsiVolume() {
        final String volumeName = appliance.getVolumeName("volume1");
        appliance.deleteIscsiVolume(volumeName);
        verify(client).execute(NmsResponse.class, "zvol", "destroy", volumeName, "");

        when(client.execute(NmsResponse.class, "zvol", "destroy", volumeName, "")).thenThrow(new CloudRuntimeException(String.format("Zvol '%s' does not exist", volumeName)));
        appliance.deleteIscsiVolume(volumeName);
    }

    @Test
    public void testDeleteIscsiVolumeFails() {
        final String volumeName = appliance.getVolumeName("volume1");
        exception.expect(CloudRuntimeException.class);
        exception.expectMessage("any exception");
        when(client.execute(NmsResponse.class, "zvol", "destroy", volumeName, "")).thenThrow(new CloudRuntimeException("any exception"));
        appliance.deleteIscsiVolume(volumeName);
    }
}
