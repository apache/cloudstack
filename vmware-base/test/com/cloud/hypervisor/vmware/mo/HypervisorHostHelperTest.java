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
package com.cloud.hypervisor.vmware.mo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.BoolPolicy;
import com.vmware.vim25.DVPortgroupConfigInfo;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DVSTrafficShapingPolicy;
import com.vmware.vim25.LongPolicy;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanIdSpec;

import com.cloud.hypervisor.vmware.util.VmwareContext;

public class HypervisorHostHelperTest {
    @Mock
    VmwareContext context;
    @Mock
    DVPortgroupConfigInfo currentDvPortgroupInfo;
    @Mock
    DVPortgroupConfigSpec dvPortgroupConfigSpec;
    @Mock
    ServiceContent serviceContent;
    @Mock
    AboutInfo aboutInfo;

    String vSwitchName;
    Integer networkRateMbps;
    String vlanId;
    String prefix;
    String svlanId = null;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(context.getServiceContent()).thenReturn(serviceContent);
        when(serviceContent.getAbout()).thenReturn(aboutInfo);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetPublicNetworkNamePrefixUnTaggedVlan() throws Exception {
        vlanId = "untagged";
        String publicNetworkPrefix = HypervisorHostHelper.getPublicNetworkNamePrefix(vlanId);
        assertEquals("cloud.public.untagged", publicNetworkPrefix);
    }

    @Test
    public void testGetVcenterApiVersion() throws Exception {
        when(aboutInfo.getApiVersion()).thenReturn("5.5");
        assertEquals("5.5", HypervisorHostHelper.getVcenterApiVersion(context));
        verify(aboutInfo).getApiVersion();
        verify(serviceContent).getAbout();
        verify(context).getServiceContent();
    }

    @Test
    public void testGetVcenterApiVersionWithNullContextObject() throws Exception {
        assertNull(HypervisorHostHelper.getVcenterApiVersion(null));
        verifyZeroInteractions(aboutInfo);
        verifyZeroInteractions(serviceContent);
    }

    @Test
    public void testIsFeatureSupportedInVcenterApiVersionUnSupported() throws Exception {
        when(aboutInfo.getApiVersion()).thenReturn("5.0");
        String featureRequiresVersion = "6.0";
        String actualVersion = HypervisorHostHelper.getVcenterApiVersion(context);
        Boolean featureSupportedVersion = HypervisorHostHelper.isFeatureSupportedInVcenterApiVersion(
                actualVersion, featureRequiresVersion);
        assertFalse(featureSupportedVersion);
        verify(aboutInfo).getApiVersion();
        verify(serviceContent).getAbout();
        verify(context).getServiceContent();
    }

    @Test
    public void testIsFeatureSupportedInVcenterApiVersionSupported() throws Exception {
        when(aboutInfo.getApiVersion()).thenReturn("5.5");
        String featureRequiresVersion = "5.0";
        String actualVersion = HypervisorHostHelper.getVcenterApiVersion(context);
        boolean featureSupportedVersion = HypervisorHostHelper.isFeatureSupportedInVcenterApiVersion(
                actualVersion, featureRequiresVersion);
        assertTrue(featureSupportedVersion);
        verify(aboutInfo).getApiVersion();
        verify(serviceContent).getAbout();
        verify(context).getServiceContent();
    }

    @Test
    public void testIsSpecMatch() throws Exception {
        int currentNumPorts = 256;
        int currentvlanId = 100;
        boolean currentAutoExpand = true;
        DVSTrafficShapingPolicy currentTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy currentIsEnabled = new BoolPolicy();
        currentIsEnabled.setValue(true);
        LongPolicy currentAvgBw = new LongPolicy();
        currentAvgBw.setValue(200L);
        LongPolicy currentBurstSize = new LongPolicy();
        currentBurstSize.setValue(400L);
        LongPolicy currentPeakBw = new LongPolicy();
        currentPeakBw.setValue(2000L);

        VMwareDVSPortSetting currentVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec currentVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        currentVlanIdSpec.setVlanId(currentvlanId);
        currentVmwareDvsPortSetting.setVlan(currentVlanIdSpec);
        currentTrafficShapingPolicy.setAverageBandwidth(currentAvgBw);
        currentTrafficShapingPolicy.setBurstSize(currentBurstSize);
        currentTrafficShapingPolicy.setPeakBandwidth(currentPeakBw);
        currentTrafficShapingPolicy.setEnabled(currentIsEnabled);
        currentVmwareDvsPortSetting.setInShapingPolicy(currentTrafficShapingPolicy);

        when(currentDvPortgroupInfo.getNumPorts()).thenReturn(currentNumPorts);
        when(currentDvPortgroupInfo.isAutoExpand()).thenReturn(currentAutoExpand);
        when(currentDvPortgroupInfo.getDefaultPortConfig()).thenReturn(currentVmwareDvsPortSetting);

        int newNumPorts = 256;
        int newvlanId = 100;
        boolean newAutoExpand = true;
        DVSTrafficShapingPolicy newTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy newIsEnabled = new BoolPolicy();
        newIsEnabled.setValue(true);
        LongPolicy newAvgBw = new LongPolicy();
        newAvgBw.setValue(200L);
        LongPolicy newBurstSize = new LongPolicy();
        newBurstSize.setValue(400L);
        LongPolicy newPeakBw = new LongPolicy();
        newPeakBw.setValue(2000L);
        VMwareDVSPortSetting newVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec newVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        newVlanIdSpec.setVlanId(newvlanId);
        newVmwareDvsPortSetting.setVlan(newVlanIdSpec);
        newTrafficShapingPolicy.setAverageBandwidth(newAvgBw);
        newTrafficShapingPolicy.setBurstSize(newBurstSize);
        newTrafficShapingPolicy.setPeakBandwidth(newPeakBw);
        newTrafficShapingPolicy.setEnabled(newIsEnabled);
        newVmwareDvsPortSetting.setInShapingPolicy(newTrafficShapingPolicy);

        when(dvPortgroupConfigSpec.getNumPorts()).thenReturn(newNumPorts);
        when(dvPortgroupConfigSpec.isAutoExpand()).thenReturn(newAutoExpand);
        when(dvPortgroupConfigSpec.getDefaultPortConfig()).thenReturn(newVmwareDvsPortSetting);

        boolean specCompareResult = HypervisorHostHelper.isSpecMatch(currentDvPortgroupInfo, dvPortgroupConfigSpec);
        assertTrue(specCompareResult);
    }

    @Test
    public void testIsSpecMatchConfigSpecWithHighBandwidthShapingPolicy() throws Exception {
        // Tests case of network offering upgrade in terms of bandwidth
        int currentNumPorts = 256;
        int currentvlanId = 100;
        boolean currentAutoExpand = true;
        DVSTrafficShapingPolicy currentTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy currentIsEnabled = new BoolPolicy();
        currentIsEnabled.setValue(true);
        LongPolicy currentAvgBw = new LongPolicy();
        currentAvgBw.setValue(200L);
        LongPolicy currentBurstSize = new LongPolicy();
        currentBurstSize.setValue(400L);
        LongPolicy currentPeakBw = new LongPolicy();
        currentPeakBw.setValue(2000L);

        VMwareDVSPortSetting currentVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec currentVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        currentVlanIdSpec.setVlanId(currentvlanId);
        currentVmwareDvsPortSetting.setVlan(currentVlanIdSpec);
        currentTrafficShapingPolicy.setAverageBandwidth(currentAvgBw);
        currentTrafficShapingPolicy.setBurstSize(currentBurstSize);
        currentTrafficShapingPolicy.setPeakBandwidth(currentPeakBw);
        currentTrafficShapingPolicy.setEnabled(currentIsEnabled);
        currentVmwareDvsPortSetting.setInShapingPolicy(currentTrafficShapingPolicy);

        when(currentDvPortgroupInfo.getNumPorts()).thenReturn(currentNumPorts);
        when(currentDvPortgroupInfo.isAutoExpand()).thenReturn(currentAutoExpand);
        when(currentDvPortgroupInfo.getDefaultPortConfig()).thenReturn(currentVmwareDvsPortSetting);

        int newNumPorts = 256;
        int newvlanId = 100;
        boolean newAutoExpand = true;
        DVSTrafficShapingPolicy newTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy newIsEnabled = new BoolPolicy();
        newIsEnabled.setValue(true);
        LongPolicy newAvgBw = new LongPolicy();
        newAvgBw.setValue(400L);
        LongPolicy newBurstSize = new LongPolicy();
        newBurstSize.setValue(800L);
        LongPolicy newPeakBw = new LongPolicy();
        newPeakBw.setValue(4000L);
        VMwareDVSPortSetting newVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec newVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        newVlanIdSpec.setVlanId(newvlanId);
        newVmwareDvsPortSetting.setVlan(newVlanIdSpec);
        newTrafficShapingPolicy.setAverageBandwidth(newAvgBw);
        newTrafficShapingPolicy.setBurstSize(newBurstSize);
        newTrafficShapingPolicy.setPeakBandwidth(newPeakBw);
        newTrafficShapingPolicy.setEnabled(newIsEnabled);
        newVmwareDvsPortSetting.setInShapingPolicy(newTrafficShapingPolicy);

        when(dvPortgroupConfigSpec.getNumPorts()).thenReturn(newNumPorts);
        when(dvPortgroupConfigSpec.isAutoExpand()).thenReturn(newAutoExpand);
        when(dvPortgroupConfigSpec.getDefaultPortConfig()).thenReturn(newVmwareDvsPortSetting);

        boolean specCompareResult = HypervisorHostHelper.isSpecMatch(currentDvPortgroupInfo, dvPortgroupConfigSpec);
        assertFalse(specCompareResult);
    }

    @Test
    public void testIsSpecMatchConfigSpecWithMoreDvPortsAndAutoExpandEnabled() throws Exception {
        int currentNumPorts = 512;
        int currentvlanId = 100;
        boolean currentAutoExpand = true;
        DVSTrafficShapingPolicy currentTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy currentIsEnabled = new BoolPolicy();
        currentIsEnabled.setValue(true);
        LongPolicy currentAvgBw = new LongPolicy();
        currentAvgBw.setValue(200L);
        LongPolicy currentBurstSize = new LongPolicy();
        currentBurstSize.setValue(400L);
        LongPolicy currentPeakBw = new LongPolicy();
        currentPeakBw.setValue(2000L);

        VMwareDVSPortSetting currentVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec currentVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        currentVlanIdSpec.setVlanId(currentvlanId);
        currentVmwareDvsPortSetting.setVlan(currentVlanIdSpec);
        currentTrafficShapingPolicy.setAverageBandwidth(currentAvgBw);
        currentTrafficShapingPolicy.setBurstSize(currentBurstSize);
        currentTrafficShapingPolicy.setPeakBandwidth(currentPeakBw);
        currentTrafficShapingPolicy.setEnabled(currentIsEnabled);
        currentVmwareDvsPortSetting.setInShapingPolicy(currentTrafficShapingPolicy);

        when(currentDvPortgroupInfo.getNumPorts()).thenReturn(currentNumPorts);
        when(currentDvPortgroupInfo.isAutoExpand()).thenReturn(currentAutoExpand);
        when(currentDvPortgroupInfo.getDefaultPortConfig()).thenReturn(currentVmwareDvsPortSetting);

        int newNumPorts = 256;
        int newvlanId = 100;
        boolean newAutoExpand = true;
        DVSTrafficShapingPolicy newTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy newIsEnabled = new BoolPolicy();
        newIsEnabled.setValue(true);
        LongPolicy newAvgBw = new LongPolicy();
        newAvgBw.setValue(200L);
        LongPolicy newBurstSize = new LongPolicy();
        newBurstSize.setValue(400L);
        LongPolicy newPeakBw = new LongPolicy();
        newPeakBw.setValue(2000L);
        VMwareDVSPortSetting newVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec newVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        newVlanIdSpec.setVlanId(newvlanId);
        newVmwareDvsPortSetting.setVlan(newVlanIdSpec);
        newTrafficShapingPolicy.setAverageBandwidth(newAvgBw);
        newTrafficShapingPolicy.setBurstSize(newBurstSize);
        newTrafficShapingPolicy.setPeakBandwidth(newPeakBw);
        newTrafficShapingPolicy.setEnabled(newIsEnabled);
        newVmwareDvsPortSetting.setInShapingPolicy(newTrafficShapingPolicy);

        when(dvPortgroupConfigSpec.getNumPorts()).thenReturn(newNumPorts);
        when(dvPortgroupConfigSpec.isAutoExpand()).thenReturn(newAutoExpand);
        when(dvPortgroupConfigSpec.getDefaultPortConfig()).thenReturn(newVmwareDvsPortSetting);

        boolean specCompareResult = HypervisorHostHelper.isSpecMatch(currentDvPortgroupInfo, dvPortgroupConfigSpec);
        assertTrue(specCompareResult);
    }

    @Test
    public void testIsSpecMatchConfigSpecWithMoreDvPortsAndAutoExpandDisabled() throws Exception {
        int currentNumPorts = 512;
        int currentvlanId = 100;
        boolean currentAutoExpand = false;
        DVSTrafficShapingPolicy currentTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy currentIsEnabled = new BoolPolicy();
        currentIsEnabled.setValue(true);
        LongPolicy currentAvgBw = new LongPolicy();
        currentAvgBw.setValue(200L);
        LongPolicy currentBurstSize = new LongPolicy();
        currentBurstSize.setValue(400L);
        LongPolicy currentPeakBw = new LongPolicy();
        currentPeakBw.setValue(2000L);

        VMwareDVSPortSetting currentVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec currentVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        currentVlanIdSpec.setVlanId(currentvlanId);
        currentVmwareDvsPortSetting.setVlan(currentVlanIdSpec);
        currentTrafficShapingPolicy.setAverageBandwidth(currentAvgBw);
        currentTrafficShapingPolicy.setBurstSize(currentBurstSize);
        currentTrafficShapingPolicy.setPeakBandwidth(currentPeakBw);
        currentTrafficShapingPolicy.setEnabled(currentIsEnabled);
        currentVmwareDvsPortSetting.setInShapingPolicy(currentTrafficShapingPolicy);

        when(currentDvPortgroupInfo.getNumPorts()).thenReturn(currentNumPorts);
        when(currentDvPortgroupInfo.isAutoExpand()).thenReturn(currentAutoExpand);
        when(currentDvPortgroupInfo.getDefaultPortConfig()).thenReturn(currentVmwareDvsPortSetting);

        int newNumPorts = 256;
        int newvlanId = 100;
        boolean newAutoExpand = false;
        DVSTrafficShapingPolicy newTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy newIsEnabled = new BoolPolicy();
        newIsEnabled.setValue(true);
        LongPolicy newAvgBw = new LongPolicy();
        newAvgBw.setValue(200L);
        LongPolicy newBurstSize = new LongPolicy();
        newBurstSize.setValue(400L);
        LongPolicy newPeakBw = new LongPolicy();
        newPeakBw.setValue(2000L);
        VMwareDVSPortSetting newVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec newVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        newVlanIdSpec.setVlanId(newvlanId);
        newVmwareDvsPortSetting.setVlan(newVlanIdSpec);
        newTrafficShapingPolicy.setAverageBandwidth(newAvgBw);
        newTrafficShapingPolicy.setBurstSize(newBurstSize);
        newTrafficShapingPolicy.setPeakBandwidth(newPeakBw);
        newTrafficShapingPolicy.setEnabled(newIsEnabled);
        newVmwareDvsPortSetting.setInShapingPolicy(newTrafficShapingPolicy);

        when(dvPortgroupConfigSpec.getNumPorts()).thenReturn(newNumPorts);
        when(dvPortgroupConfigSpec.isAutoExpand()).thenReturn(newAutoExpand);
        when(dvPortgroupConfigSpec.getDefaultPortConfig()).thenReturn(newVmwareDvsPortSetting);

        boolean specCompareResult = HypervisorHostHelper.isSpecMatch(currentDvPortgroupInfo, dvPortgroupConfigSpec);
        assertFalse(specCompareResult);
    }

    @Test
    public void testIsSpecMatchConfigSpecWithAutoExpandUpdate() throws Exception {
        int currentNumPorts = 512;
        int currentvlanId = 100;
        boolean currentAutoExpand = false;
        DVSTrafficShapingPolicy currentTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy currentIsEnabled = new BoolPolicy();
        currentIsEnabled.setValue(true);
        LongPolicy currentAvgBw = new LongPolicy();
        currentAvgBw.setValue(200L);
        LongPolicy currentBurstSize = new LongPolicy();
        currentBurstSize.setValue(400L);
        LongPolicy currentPeakBw = new LongPolicy();
        currentPeakBw.setValue(2000L);

        VMwareDVSPortSetting currentVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec currentVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        currentVlanIdSpec.setVlanId(currentvlanId);
        currentVmwareDvsPortSetting.setVlan(currentVlanIdSpec);
        currentTrafficShapingPolicy.setAverageBandwidth(currentAvgBw);
        currentTrafficShapingPolicy.setBurstSize(currentBurstSize);
        currentTrafficShapingPolicy.setPeakBandwidth(currentPeakBw);
        currentTrafficShapingPolicy.setEnabled(currentIsEnabled);
        currentVmwareDvsPortSetting.setInShapingPolicy(currentTrafficShapingPolicy);

        when(currentDvPortgroupInfo.getNumPorts()).thenReturn(currentNumPorts);
        when(currentDvPortgroupInfo.isAutoExpand()).thenReturn(currentAutoExpand);
        when(currentDvPortgroupInfo.getDefaultPortConfig()).thenReturn(currentVmwareDvsPortSetting);

        int newNumPorts = 256;
        int newvlanId = 100;
        boolean newAutoExpand = true;
        DVSTrafficShapingPolicy newTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy newIsEnabled = new BoolPolicy();
        newIsEnabled.setValue(true);
        LongPolicy newAvgBw = new LongPolicy();
        newAvgBw.setValue(200L);
        LongPolicy newBurstSize = new LongPolicy();
        newBurstSize.setValue(400L);
        LongPolicy newPeakBw = new LongPolicy();
        newPeakBw.setValue(2000L);
        VMwareDVSPortSetting newVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec newVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        newVlanIdSpec.setVlanId(newvlanId);
        newVmwareDvsPortSetting.setVlan(newVlanIdSpec);
        newTrafficShapingPolicy.setAverageBandwidth(newAvgBw);
        newTrafficShapingPolicy.setBurstSize(newBurstSize);
        newTrafficShapingPolicy.setPeakBandwidth(newPeakBw);
        newTrafficShapingPolicy.setEnabled(newIsEnabled);
        newVmwareDvsPortSetting.setInShapingPolicy(newTrafficShapingPolicy);

        when(dvPortgroupConfigSpec.getNumPorts()).thenReturn(newNumPorts);
        when(dvPortgroupConfigSpec.isAutoExpand()).thenReturn(newAutoExpand);
        when(dvPortgroupConfigSpec.getDefaultPortConfig()).thenReturn(newVmwareDvsPortSetting);

        boolean specCompareResult = HypervisorHostHelper.isSpecMatch(currentDvPortgroupInfo, dvPortgroupConfigSpec);
        assertFalse(specCompareResult);
    }

    @Test
    public void testIsSpecMatchConfigSpecWithCurrentShapingPolicyDisabled() throws Exception {
        int currentNumPorts = 512;
        int currentvlanId = 100;
        boolean currentAutoExpand = true;
        DVSTrafficShapingPolicy currentTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy currentIsEnabled = new BoolPolicy();
        currentIsEnabled.setValue(false);

        VMwareDVSPortSetting currentVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec currentVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        currentVlanIdSpec.setVlanId(currentvlanId);
        currentVmwareDvsPortSetting.setVlan(currentVlanIdSpec);
        currentTrafficShapingPolicy.setEnabled(currentIsEnabled);
        currentVmwareDvsPortSetting.setInShapingPolicy(currentTrafficShapingPolicy);

        when(currentDvPortgroupInfo.getNumPorts()).thenReturn(currentNumPorts);
        when(currentDvPortgroupInfo.isAutoExpand()).thenReturn(currentAutoExpand);
        when(currentDvPortgroupInfo.getDefaultPortConfig()).thenReturn(currentVmwareDvsPortSetting);

        int newNumPorts = 256;
        int newvlanId = 100;
        boolean newAutoExpand = true;
        DVSTrafficShapingPolicy newTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy newIsEnabled = new BoolPolicy();
        newIsEnabled.setValue(true);
        LongPolicy newAvgBw = new LongPolicy();
        newAvgBw.setValue(200L);
        LongPolicy newBurstSize = new LongPolicy();
        newBurstSize.setValue(400L);
        LongPolicy newPeakBw = new LongPolicy();
        newPeakBw.setValue(2000L);
        VMwareDVSPortSetting newVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec newVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        newVlanIdSpec.setVlanId(newvlanId);
        newVmwareDvsPortSetting.setVlan(newVlanIdSpec);
        newTrafficShapingPolicy.setAverageBandwidth(newAvgBw);
        newTrafficShapingPolicy.setBurstSize(newBurstSize);
        newTrafficShapingPolicy.setPeakBandwidth(newPeakBw);
        newTrafficShapingPolicy.setEnabled(newIsEnabled);
        newVmwareDvsPortSetting.setInShapingPolicy(newTrafficShapingPolicy);

        when(dvPortgroupConfigSpec.getNumPorts()).thenReturn(newNumPorts);
        when(dvPortgroupConfigSpec.isAutoExpand()).thenReturn(newAutoExpand);
        when(dvPortgroupConfigSpec.getDefaultPortConfig()).thenReturn(newVmwareDvsPortSetting);

        boolean specCompareResult = HypervisorHostHelper.isSpecMatch(currentDvPortgroupInfo, dvPortgroupConfigSpec);
        assertFalse(specCompareResult);
    }

    @Test
    public void testIsSpecMatchConfigSpecWithCurrentShapingPolicyAndNewShapingPolicyDisabled() throws Exception {
        int currentNumPorts = 512;
        int currentvlanId = 100;
        boolean currentAutoExpand = true;
        DVSTrafficShapingPolicy currentTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy currentIsEnabled = new BoolPolicy();
        currentIsEnabled.setValue(false);
        VMwareDVSPortSetting currentVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec currentVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        currentVlanIdSpec.setVlanId(currentvlanId);
        currentVmwareDvsPortSetting.setVlan(currentVlanIdSpec);
        currentTrafficShapingPolicy.setEnabled(currentIsEnabled);
        currentVmwareDvsPortSetting.setInShapingPolicy(currentTrafficShapingPolicy);

        when(currentDvPortgroupInfo.getNumPorts()).thenReturn(currentNumPorts);
        when(currentDvPortgroupInfo.isAutoExpand()).thenReturn(currentAutoExpand);
        when(currentDvPortgroupInfo.getDefaultPortConfig()).thenReturn(currentVmwareDvsPortSetting);

        int newNumPorts = 256;
        int newvlanId = 100;
        boolean newAutoExpand = true;
        DVSTrafficShapingPolicy newTrafficShapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy newIsEnabled = new BoolPolicy();
        newIsEnabled.setValue(false);
        VMwareDVSPortSetting newVmwareDvsPortSetting = new VMwareDVSPortSetting();
        VmwareDistributedVirtualSwitchVlanIdSpec newVlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        newVlanIdSpec.setVlanId(newvlanId);
        newVmwareDvsPortSetting.setVlan(newVlanIdSpec);
        newTrafficShapingPolicy.setEnabled(newIsEnabled);
        newVmwareDvsPortSetting.setInShapingPolicy(newTrafficShapingPolicy);

        when(dvPortgroupConfigSpec.getNumPorts()).thenReturn(newNumPorts);
        when(dvPortgroupConfigSpec.isAutoExpand()).thenReturn(newAutoExpand);
        when(dvPortgroupConfigSpec.getDefaultPortConfig()).thenReturn(newVmwareDvsPortSetting);

        boolean specCompareResult = HypervisorHostHelper.isSpecMatch(currentDvPortgroupInfo, dvPortgroupConfigSpec);
        assertTrue(specCompareResult);
    }

    @Test
    public void testGetPublicNetworkNamePrefixTaggedVlan() throws Exception {
        vlanId = "1234";
        String publicNetworkPrefix = HypervisorHostHelper.getPublicNetworkNamePrefix(vlanId);
        assertEquals("cloud.public.1234", publicNetworkPrefix);
    }

    @Test
    public void testComposeCloudNetworkNameTaggedVlanPublicTraffic() throws Exception {
        vlanId = "100";
        networkRateMbps = 200;
        prefix = "cloud.public";
        vSwitchName = "vSwitch0";
        String cloudNetworkName = HypervisorHostHelper.composeCloudNetworkName(prefix, vlanId, svlanId, networkRateMbps, vSwitchName);
        assertEquals("cloud.public.100.200.1-vSwitch0", cloudNetworkName);
    }

    @Test
    public void testComposeCloudNetworkNameUnTaggedVlanStorageTraffic() throws Exception {
        vlanId = null;
        networkRateMbps = null;
        prefix = "cloud.storage";
        vSwitchName = "vSwitch1";
        String cloudNetworkName = HypervisorHostHelper.composeCloudNetworkName(prefix, vlanId, svlanId, networkRateMbps, vSwitchName);
        assertEquals("cloud.storage.untagged.0.1-vSwitch1", cloudNetworkName);
    }

    @Test
    public void testComposeCloudNetworkNameUnTaggedVlanGuestTraffic() throws Exception {
        vlanId = "400";
        svlanId = "123";
        networkRateMbps = 512;
        prefix = "cloud.guest";
        vSwitchName = "vSwitch2";
        String cloudNetworkName = HypervisorHostHelper.composeCloudNetworkName(prefix, vlanId, svlanId, networkRateMbps, vSwitchName);
        assertEquals("cloud.guest.400.s123.512.1-vSwitch2", cloudNetworkName);
    }
}
