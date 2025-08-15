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

package com.cloud.hypervisor.kvm.storage;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.apache.cloudstack.storage.datastore.util.ScaleIOUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.StorageLayer;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOStorageAdaptorTest {

    @Mock
    StorageLayer storageLayer;
    ScaleIOStorageAdaptor scaleIOStorageAdaptor;

    private final static String poolUuid = "345fc603-2d7e-47d2-b719-a0110b3732e6";
    private static MockedStatic<Script> mockedScript;

    @Before
    public void setUp() {
        mockedScript = Mockito.mockStatic(Script.class);
        scaleIOStorageAdaptor = Mockito.spy(ScaleIOStorageAdaptor.class);
    }

    @After
    public void tearDown() {
        mockedScript.close();
    }

    @Test
    public void getUsableBytesFromRawBytesTest() {
        Assert.assertEquals("Overhead calculated for 8Gi size", 8454111232L, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(8L << 30));
        Assert.assertEquals("Overhead calculated for 4Ti size", 4294030262272L, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(4000L << 30));
        Assert.assertEquals("Overhead calculated for 500Gi size", 536636342272L, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(500L << 30));
        Assert.assertEquals("Unsupported small size", 0, ScaleIOStorageAdaptor.getUsableBytesFromRawBytes(1L));
    }

    @Test
    public void testPrepareStorageClient_SDCServiceNotInstalled() {
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl status scini"))).thenReturn(4);

        Ternary<Boolean, Map<String, String>, String> result = scaleIOStorageAdaptor.prepareStorageClient(poolUuid, new HashMap<>());

        Assert.assertFalse(result.first());
        Assert.assertNull(result.second());
        Assert.assertEquals("SDC service not installed on host", result.third());
    }

    @Test
    public void testPrepareStorageClient_SDCServiceNotEnabled() {
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl status scini"))).thenReturn(3);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl is-enabled scini"))).thenReturn(1);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl enable scini"))).thenReturn(1);

        Ternary<Boolean, Map<String, String>, String> result = scaleIOStorageAdaptor.prepareStorageClient(poolUuid, new HashMap<>());

        Assert.assertFalse(result.first());
        Assert.assertNull(result.second());
        Assert.assertEquals("SDC service not enabled on host", result.third());
    }

    @Test
    public void testPrepareStorageClient_SDCServiceNotStarted() {
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl status scini"))).thenReturn(3);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl is-enabled scini"))).thenReturn(0);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl is-active scini"))).thenReturn(1);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl start scini"))).thenReturn(1);

        Ternary<Boolean, Map<String, String>, String> result = scaleIOStorageAdaptor.prepareStorageClient(poolUuid, new HashMap<>());

        Assert.assertFalse(result.first());
        Assert.assertNull(result.second());
        Assert.assertEquals("Couldn't start SDC service on host", result.third());
    }

    @Test
    public void testPrepareStorageClient_SDCServiceStartedReturnSDCId() {
        Map<String, String> details = new HashMap<>();
        String systemId = "218ce1797566a00f";
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);

        try (MockedStatic<ScaleIOUtil> ignored = Mockito.mockStatic(ScaleIOUtil.class)) {
            when(ScaleIOUtil.isSDCServiceInstalled()).thenReturn(true);
            when(ScaleIOUtil.isSDCServiceEnabled()).thenReturn(true);
            when(ScaleIOUtil.isSDCServiceActive()).thenReturn(false);
            when(ScaleIOUtil.startSDCService()).thenReturn(true);
            String sdcId = "301b852c00000003";
            when(ScaleIOUtil.getSdcId(systemId)).thenReturn(sdcId);

            Ternary<Boolean, Map<String, String>, String> result = scaleIOStorageAdaptor.prepareStorageClient(poolUuid, details);

            Assert.assertTrue(result.first());
            Assert.assertNotNull(result.second());
            Assert.assertEquals(sdcId, result.second().get(ScaleIOGatewayClient.SDC_ID));
        }
    }

    @Test
    public void testPrepareStorageClient_SDCServiceStartedReturnSDCGuid() {
        Map<String, String> details = new HashMap<>();
        String systemId = "218ce1797566a00f";
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);

        String sdcGuid = "B0E3BFB8-C20B-43BF-93C8-13339E85AA50";
        try (MockedStatic<ScaleIOUtil> ignored = Mockito.mockStatic(ScaleIOUtil.class)) {
            when(ScaleIOUtil.isSDCServiceInstalled()).thenReturn(true);
            when(ScaleIOUtil.isSDCServiceEnabled()).thenReturn(true);
            when(ScaleIOUtil.isSDCServiceActive()).thenReturn(false);
            when(ScaleIOUtil.startSDCService()).thenReturn(true);
            when(ScaleIOUtil.getSdcId(systemId)).thenReturn(null);
            when(ScaleIOUtil.getSdcGuid()).thenReturn(sdcGuid);

            Ternary<Boolean, Map<String, String>, String> result = scaleIOStorageAdaptor.prepareStorageClient(poolUuid, details);

            Assert.assertTrue(result.first());
            Assert.assertNotNull(result.second());
            Assert.assertEquals(sdcGuid, result.second().get(ScaleIOGatewayClient.SDC_GUID));
        }
    }

    @Test
    public void testUnprepareStorageClient_SDCServiceNotInstalled() {
        Map<String, String> details = new HashMap<>();
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl status scini"))).thenReturn(4);

        Pair<Boolean, String> result = scaleIOStorageAdaptor.unprepareStorageClient(poolUuid, details);

        Assert.assertTrue(result.first());
        Assert.assertEquals("SDC service not installed on host, no need to unprepare the SDC client", result.second());
    }

    @Test
    public void testUnprepareStorageClient_SDCServiceNotEnabled() {
        Map<String, String> details = new HashMap<>();
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl status scini"))).thenReturn(3);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl is-enabled scini"))).thenReturn(1);

        Pair<Boolean, String> result = scaleIOStorageAdaptor.unprepareStorageClient(poolUuid, details);

        Assert.assertTrue(result.first());
        Assert.assertEquals("SDC service not enabled on host, no need to unprepare the SDC client", result.second());
    }

    @Test
    public void testUnprepareStorageClient_MDMNotAdded() {
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, "1.1.1.1,2.2.2.2");
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl status scini"))).thenReturn(3);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl is-enabled scini"))).thenReturn(0);
        when(Script.runSimpleBashScript(Mockito.eq("/opt/emc/scaleio/sdc/bin/drv_cfg --query_mdms|grep 1.1.1.1"))).thenReturn("MDM-ID 71fd458f0775010f SDC ID 4421a91a00000000 INSTALLATION ID 204930df2cbcaf8e IPs [0]-3.3.3.3 [1]-4.4.4.4");

        Pair<Boolean, String> result = scaleIOStorageAdaptor.unprepareStorageClient(poolUuid, details);

        Assert.assertTrue(result.first());
        Assert.assertEquals("MDM not added, no need to unprepare the SDC client", result.second());
    }

    @Test
    public void testUnprepareStorageClient_RemoveMDMFailed() {
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_MDMS, "1.1.1.1,2.2.2.2");
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl status scini"))).thenReturn(3);
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl is-enabled scini"))).thenReturn(0);
        when(Script.executeCommand(Mockito.eq("sed -i '/1.1.1.1\\,/d' /etc/emc/scaleio/drv_cfg.txt"))).thenReturn(new Pair<>(null, null));
        when(Script.runSimpleBashScriptForExitValue(Mockito.eq("systemctl restart scini"))).thenReturn(0);
        when(Script.runSimpleBashScript(Mockito.eq("/opt/emc/scaleio/sdc/bin/drv_cfg --query_mdms|grep 1.1.1.1"))).thenReturn("MDM-ID 71fd458f0775010f SDC ID 4421a91a00000000 INSTALLATION ID 204930df2cbcaf8e IPs [0]-1.1.1.1 [1]-2.2.2.2");
        when(Script.executeCommand(Mockito.eq("/opt/emc/scaleio/sdc/bin/drv_cfg"))).thenReturn(new Pair<>(null, null));
        when(Script.executeCommand(Mockito.eq("/opt/emc/scaleio/sdc/bin/drv_cfg --query_vols"))).thenReturn(new Pair<>("", null));


        Pair<Boolean, String> result = scaleIOStorageAdaptor.unprepareStorageClient(poolUuid, details);

        Assert.assertFalse(result.first());
        Assert.assertEquals("Failed to remove MDMs, unable to unprepare the SDC client", result.second());
    }
}
