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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.offering.NetworkOffering;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.BoolPolicy;
import com.vmware.vim25.ClusterConfigInfoEx;
import com.vmware.vim25.DatacenterConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.DVPortgroupConfigInfo;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DVSSecurityPolicy;
import com.vmware.vim25.DVSTrafficShapingPolicy;
import com.vmware.vim25.HostNetworkSecurityPolicy;
import com.vmware.vim25.LongPolicy;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VmwareDistributedVirtualSwitchTrunkVlanSpec;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanIdSpec;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanSpec;

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
    @Mock
    private VirtualMachineConfigSpec vmSpec;
    @Mock
    private ClusterMO clusterMO;
    @Mock
    private DatacenterMO datacenterMO;
    @Mock
    private ClusterConfigInfoEx clusterConfigInfo;
    @Mock
    private DatacenterConfigInfo datacenterConfigInfo;

    String vSwitchName;
    Integer networkRateMbps;
    String vlanId;
    String prefix;
    String svlanId = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(context.getServiceContent()).thenReturn(serviceContent);
        when(serviceContent.getAbout()).thenReturn(aboutInfo);
        when(clusterMO.getClusterConfigInfo()).thenReturn(clusterConfigInfo);
        when(datacenterMO.getDatacenterConfigInfo()).thenReturn(datacenterConfigInfo);
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
        assertEquals("cloud.public.1234.", publicNetworkPrefix);
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

    @Test
    public void testOvfDomRewriter() {
        final String ovfString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!--Generated by VMware ovftool 3.5.0 (build-1274719), UTC time: 2016-10-03T12:49:55.591821Z-->" +
                "<Envelope xmlns=\"http://schemas.dmtf.org/ovf/envelope/1\" xmlns:cim=\"http://schemas.dmtf.org/wbem/wscim/1/common\" xmlns:ovf=\"http://schemas.dmtf.org/ovf/envelope/1\" xmlns:rasd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData\" xmlns:vmw=\"http://www.vmware.com/schema/ovf\" xmlns:vssd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <References>\n" +
                "    <File ovf:href=\"macchinina-vmware-disk1.vmdk\" ovf:id=\"file1\" ovf:size=\"23303168\"/>\n" +
                "  </References>\n" +
                "  <DiskSection>\n" +
                "    <Info>Virtual disk information</Info>\n" +
                "    <Disk ovf:capacity=\"50\" ovf:capacityAllocationUnits=\"byte * 2^20\" ovf:diskId=\"vmdisk1\" ovf:fileRef=\"file1\" ovf:format=\"http://www.vmware.com/interfaces/specifications/vmdk.html#streamOptimized\" ovf:populatedSize=\"43319296\"/>\n" +
                "  </DiskSection>\n" +
                "  <NetworkSection>\n" +
                "    <Info>The list of logical networks</Info>\n" +
                "    <Network ovf:name=\"bridged\">\n" +
                "      <Description>The bridged network</Description>\n" +
                "    </Network>\n" +
                "  </NetworkSection>\n" +
                "  <VirtualSystem ovf:id=\"vm\">\n" +
                "    <Info>A virtual machine</Info>\n" +
                "    <Name>macchinina-vmware</Name>\n" +
                "    <OperatingSystemSection ovf:id=\"101\" vmw:osType=\"otherLinux64Guest\">\n" +
                "      <Info>The kind of installed guest operating system</Info>\n" +
                "    </OperatingSystemSection>\n" +
                "    <VirtualHardwareSection>\n" +
                "      <Info>Virtual hardware requirements</Info>\n" +
                "      <System>\n" +
                "        <vssd:ElementName>Virtual Hardware Family</vssd:ElementName>\n" +
                "        <vssd:InstanceID>0</vssd:InstanceID>\n" +
                "        <vssd:VirtualSystemIdentifier>macchinina-vmware</vssd:VirtualSystemIdentifier>\n" +
                "        <vssd:VirtualSystemType>vmx-07</vssd:VirtualSystemType>\n" +
                "      </System>\n" +
                "      <Item>\n" +
                "        <rasd:AllocationUnits>hertz * 10^6</rasd:AllocationUnits>\n" +
                "        <rasd:Description>Number of Virtual CPUs</rasd:Description>\n" +
                "        <rasd:ElementName>1 virtual CPU(s)</rasd:ElementName>\n" +
                "        <rasd:InstanceID>1</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>3</rasd:ResourceType>\n" +
                "        <rasd:VirtualQuantity>1</rasd:VirtualQuantity>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:AllocationUnits>byte * 2^20</rasd:AllocationUnits>\n" +
                "        <rasd:Description>Memory Size</rasd:Description>\n" +
                "        <rasd:ElementName>256MB of memory</rasd:ElementName>\n" +
                "        <rasd:InstanceID>2</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>4</rasd:ResourceType>\n" +
                "        <rasd:VirtualQuantity>256</rasd:VirtualQuantity>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:Address>0</rasd:Address>\n" +
                "        <rasd:Description>SCSI Controller</rasd:Description>\n" +
                "        <rasd:ElementName>scsiController0</rasd:ElementName>\n" +
                "        <rasd:InstanceID>3</rasd:InstanceID>\n" +
                "        <rasd:ResourceSubType>lsilogic</rasd:ResourceSubType>\n" +
                "        <rasd:ResourceType>6</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:Address>0</rasd:Address>\n" +
                "        <rasd:Description>IDE Controller</rasd:Description>\n" +
                "        <rasd:ElementName>ideController0</rasd:ElementName>\n" +
                "        <rasd:InstanceID>4</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>5</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <Item ovf:required=\"false\">\n" +
                "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
                "        <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>\n" +
                "        <rasd:ElementName>cdrom0</rasd:ElementName>\n" +
                "        <rasd:InstanceID>5</rasd:InstanceID>\n" +
                "        <rasd:Parent>4</rasd:Parent>\n" +
                "        <rasd:ResourceType>15</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
                "        <rasd:ElementName>disk0</rasd:ElementName>\n" +
                "        <rasd:HostResource>ovf:/disk/vmdisk1</rasd:HostResource>\n" +
                "        <rasd:InstanceID>6</rasd:InstanceID>\n" +
                "        <rasd:Parent>3</rasd:Parent>\n" +
                "        <rasd:ResourceType>17</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:AddressOnParent>2</rasd:AddressOnParent>\n" +
                "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
                "        <rasd:Connection>bridged</rasd:Connection>\n" +
                "        <rasd:Description>E1000 ethernet adapter on &quot;bridged&quot;</rasd:Description>\n" +
                "        <rasd:ElementName>ethernet0</rasd:ElementName>\n" +
                "        <rasd:InstanceID>7</rasd:InstanceID>\n" +
                "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
                "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
                "        <vmw:Config ovf:required=\"false\" vmw:key=\"wakeOnLanEnabled\" vmw:value=\"false\"/>\n" +
                "      </Item>\n" +
                "      <Item ovf:required=\"false\">\n" +
                "        <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>\n" +
                "        <rasd:ElementName>video</rasd:ElementName>\n" +
                "        <rasd:InstanceID>8</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>24</rasd:ResourceType>\n" +
                "        <vmw:Config ovf:required=\"false\" vmw:key=\"enable3DSupport\" vmw:value=\"false\"/>\n" +
                "        <vmw:Config ovf:required=\"false\" vmw:key=\"useAutoDetect\" vmw:value=\"false\"/>\n" +
                "        <vmw:Config ovf:required=\"false\" vmw:key=\"videoRamSizeInKB\" vmw:value=\"4096\"/>\n" +
                "      </Item>\n" +
                "      <Item ovf:required=\"false\">\n" +
                "        <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>\n" +
                "        <rasd:ElementName>vmci</rasd:ElementName>\n" +
                "        <rasd:InstanceID>9</rasd:InstanceID>\n" +
                "        <rasd:ResourceSubType>vmware.vmci</rasd:ResourceSubType>\n" +
                "        <rasd:ResourceType>1</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"cpuHotAddEnabled\" vmw:value=\"false\"/>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"cpuHotRemoveEnabled\" vmw:value=\"false\"/>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"firmware\" vmw:value=\"bios\"/>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"memoryHotAddEnabled\" vmw:value=\"false\"/>\n" +
                "    </VirtualHardwareSection>\n" +
                "    <AnnotationSection ovf:required=\"false\">\n" +
                "      <Info>A human-readable annotation</Info>\n" +
                "      <Annotation>macchinina-vmware</Annotation>\n" +
                "    </AnnotationSection>\n" +
                "  </VirtualSystem>\n" +
                "</Envelope>";

        final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!--Generated by VMware ovftool 3.5.0 (build-1274719), UTC time: 2016-10-03T12:49:55.591821Z-->" +
                "<Envelope xmlns=\"http://schemas.dmtf.org/ovf/envelope/1\" xmlns:cim=\"http://schemas.dmtf.org/wbem/wscim/1/common\" xmlns:ovf=\"http://schemas.dmtf.org/ovf/envelope/1\" xmlns:rasd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData\" xmlns:vmw=\"http://www.vmware.com/schema/ovf\" xmlns:vssd=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <References>\n" +
                "    <File ovf:href=\"macchinina-vmware-disk1.vmdk\" ovf:id=\"file1\" ovf:size=\"23303168\"/>\n" +
                "  </References>\n" +
                "  <DiskSection>\n" +
                "    <Info>Virtual disk information</Info>\n" +
                "    <Disk ovf:capacity=\"50\" ovf:capacityAllocationUnits=\"byte * 2^20\" ovf:diskId=\"vmdisk1\" ovf:fileRef=\"file1\" ovf:format=\"http://www.vmware.com/interfaces/specifications/vmdk.html#streamOptimized\" ovf:populatedSize=\"43319296\"/>\n" +
                "  </DiskSection>\n  \n" +
                "  <VirtualSystem ovf:id=\"vm\">\n" +
                "    <Info>A virtual machine</Info>\n" +
                "    <Name>macchinina-vmware</Name>\n" +
                "    <OperatingSystemSection ovf:id=\"101\" vmw:osType=\"otherLinux64Guest\">\n" +
                "      <Info>The kind of installed guest operating system</Info>\n" +
                "    </OperatingSystemSection>\n" +
                "    <VirtualHardwareSection>\n" +
                "      <Info>Virtual hardware requirements</Info>\n" +
                "      <System>\n" +
                "        <vssd:ElementName>Virtual Hardware Family</vssd:ElementName>\n" +
                "        <vssd:InstanceID>0</vssd:InstanceID>\n" +
                "        <vssd:VirtualSystemIdentifier>macchinina-vmware</vssd:VirtualSystemIdentifier>\n" +
                "        <vssd:VirtualSystemType>vmx-07</vssd:VirtualSystemType>\n" +
                "      </System>\n" +
                "      <Item>\n" +
                "        <rasd:AllocationUnits>hertz * 10^6</rasd:AllocationUnits>\n" +
                "        <rasd:Description>Number of Virtual CPUs</rasd:Description>\n" +
                "        <rasd:ElementName>1 virtual CPU(s)</rasd:ElementName>\n" +
                "        <rasd:InstanceID>1</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>3</rasd:ResourceType>\n" +
                "        <rasd:VirtualQuantity>1</rasd:VirtualQuantity>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:AllocationUnits>byte * 2^20</rasd:AllocationUnits>\n" +
                "        <rasd:Description>Memory Size</rasd:Description>\n" +
                "        <rasd:ElementName>256MB of memory</rasd:ElementName>\n" +
                "        <rasd:InstanceID>2</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>4</rasd:ResourceType>\n" +
                "        <rasd:VirtualQuantity>256</rasd:VirtualQuantity>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:Address>0</rasd:Address>\n" +
                "        <rasd:Description>SCSI Controller</rasd:Description>\n" +
                "        <rasd:ElementName>scsiController0</rasd:ElementName>\n" +
                "        <rasd:InstanceID>3</rasd:InstanceID>\n" +
                "        <rasd:ResourceSubType>lsilogic</rasd:ResourceSubType>\n" +
                "        <rasd:ResourceType>6</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:Address>0</rasd:Address>\n" +
                "        <rasd:Description>IDE Controller</rasd:Description>\n" +
                "        <rasd:ElementName>ideController0</rasd:ElementName>\n" +
                "        <rasd:InstanceID>4</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>5</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <Item ovf:required=\"false\">\n" +
                "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
                "        <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>\n" +
                "        <rasd:ElementName>cdrom0</rasd:ElementName>\n" +
                "        <rasd:InstanceID>5</rasd:InstanceID>\n" +
                "        <rasd:Parent>4</rasd:Parent>\n" +
                "        <rasd:ResourceType>15</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <Item>\n" +
                "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
                "        <rasd:ElementName>disk0</rasd:ElementName>\n" +
                "        <rasd:HostResource>ovf:/disk/vmdisk1</rasd:HostResource>\n" +
                "        <rasd:InstanceID>6</rasd:InstanceID>\n" +
                "        <rasd:Parent>3</rasd:Parent>\n" +
                "        <rasd:ResourceType>17</rasd:ResourceType>\n" +
                "      </Item>\n      \n" +
                "      <Item ovf:required=\"false\">\n" +
                "        <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>\n" +
                "        <rasd:ElementName>video</rasd:ElementName>\n" +
                "        <rasd:InstanceID>8</rasd:InstanceID>\n" +
                "        <rasd:ResourceType>24</rasd:ResourceType>\n" +
                "        <vmw:Config ovf:required=\"false\" vmw:key=\"enable3DSupport\" vmw:value=\"false\"/>\n" +
                "        <vmw:Config ovf:required=\"false\" vmw:key=\"useAutoDetect\" vmw:value=\"false\"/>\n" +
                "        <vmw:Config ovf:required=\"false\" vmw:key=\"videoRamSizeInKB\" vmw:value=\"4096\"/>\n" +
                "      </Item>\n" +
                "      <Item ovf:required=\"false\">\n" +
                "        <rasd:AutomaticAllocation>false</rasd:AutomaticAllocation>\n" +
                "        <rasd:ElementName>vmci</rasd:ElementName>\n" +
                "        <rasd:InstanceID>9</rasd:InstanceID>\n" +
                "        <rasd:ResourceSubType>vmware.vmci</rasd:ResourceSubType>\n" +
                "        <rasd:ResourceType>1</rasd:ResourceType>\n" +
                "      </Item>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"cpuHotAddEnabled\" vmw:value=\"false\"/>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"cpuHotRemoveEnabled\" vmw:value=\"false\"/>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"firmware\" vmw:value=\"bios\"/>\n" +
                "      <vmw:Config ovf:required=\"false\" vmw:key=\"memoryHotAddEnabled\" vmw:value=\"false\"/>\n" +
                "    </VirtualHardwareSection>\n" +
                "    <AnnotationSection ovf:required=\"false\">\n" +
                "      <Info>A human-readable annotation</Info>\n" +
                "      <Annotation>macchinina-vmware</Annotation>\n" +
                "    </AnnotationSection>\n" +
                "  </VirtualSystem>\n" +
                "</Envelope>";
        assertEquals(expected, HypervisorHostHelper.removeOVFNetwork(ovfString));
    }

    private Map<NetworkOffering.Detail, String> getSecurityDetails() {
        final Map<NetworkOffering.Detail, String> details = new HashMap<>();
        details.put(NetworkOffering.Detail.PromiscuousMode, "false");
        details.put(NetworkOffering.Detail.ForgedTransmits, "false");
        details.put(NetworkOffering.Detail.MacAddressChanges, "false");
        return details;
    }

    @Test
    public void testVSSecurityPolicyDefault() {
        HostNetworkSecurityPolicy secPolicy = HypervisorHostHelper.createVSSecurityPolicy(null);
        assertFalse(secPolicy.isAllowPromiscuous());
        assertTrue(secPolicy.isForgedTransmits());
        assertTrue(secPolicy.isMacChanges());
    }

    @Test
    public void testVSSecurityPolicyDefaultWithDetail() {
        HostNetworkSecurityPolicy secPolicy = HypervisorHostHelper.createVSSecurityPolicy(getSecurityDetails());
        assertFalse(secPolicy.isAllowPromiscuous());
        assertFalse(secPolicy.isForgedTransmits());
        assertFalse(secPolicy.isMacChanges());
    }

    @Test
    public void testVSSecurityPolicyWithDetail() {
        Map<NetworkOffering.Detail, String> details = getSecurityDetails();
        details.put(NetworkOffering.Detail.MacAddressChanges, "true");
        HostNetworkSecurityPolicy secPolicy = HypervisorHostHelper.createVSSecurityPolicy(details);
        assertFalse(secPolicy.isAllowPromiscuous());
        assertFalse(secPolicy.isForgedTransmits());
        assertTrue(secPolicy.isMacChanges());
    }

    @Test
    public void testDVSSecurityPolicyDefault() {
        DVSSecurityPolicy secPolicy = HypervisorHostHelper.createDVSSecurityPolicy(null);
        assertFalse(secPolicy.getAllowPromiscuous().isValue());
        assertTrue(secPolicy.getForgedTransmits().isValue());
        assertTrue(secPolicy.getMacChanges().isValue());
    }

    @Test
    public void testDVSSecurityPolicyDefaultWithDetail() {
        Map<NetworkOffering.Detail, String> details = getSecurityDetails();
        details.remove(NetworkOffering.Detail.ForgedTransmits);
        details.remove(NetworkOffering.Detail.PromiscuousMode);
        DVSSecurityPolicy secPolicy = HypervisorHostHelper.createDVSSecurityPolicy(details);
        assertFalse(secPolicy.getAllowPromiscuous().isValue());
        assertFalse(secPolicy.getMacChanges().isValue());
        assertTrue(secPolicy.getForgedTransmits().isValue());
    }

    @Test
    public void testDVSSecurityPolicyWithDetail() {
        Map<NetworkOffering.Detail, String> details = getSecurityDetails();
        details.put(NetworkOffering.Detail.ForgedTransmits, "true");
        DVSSecurityPolicy secPolicy = HypervisorHostHelper.createDVSSecurityPolicy(details);
        assertFalse(secPolicy.getAllowPromiscuous().isValue());
        assertTrue(secPolicy.getForgedTransmits().isValue());
        assertFalse(secPolicy.getMacChanges().isValue());
    }

    @Test
    public void testCreateDVPortVlanSpecNullVlanId() {
        VmwareDistributedVirtualSwitchVlanSpec spec = HypervisorHostHelper.createDVPortVlanSpec(null, null);
        assertTrue(spec instanceof VmwareDistributedVirtualSwitchVlanIdSpec);
        assertTrue(((VmwareDistributedVirtualSwitchVlanIdSpec) spec).getVlanId() == 0);
    }

    @Test
    public void testCreateDVPortVlanSpecValidVlanId() {
        VmwareDistributedVirtualSwitchVlanSpec spec = HypervisorHostHelper.createDVPortVlanSpec(100, "400");
        assertTrue(spec instanceof VmwareDistributedVirtualSwitchVlanIdSpec);
        assertTrue(((VmwareDistributedVirtualSwitchVlanIdSpec) spec).getVlanId() == 100);
    }

    @Test
    public void testCreateDVPortVlanSpecValidVlanRange() {
        VmwareDistributedVirtualSwitchVlanSpec spec = HypervisorHostHelper.createDVPortVlanSpec(null, "200-250");
        assertTrue(spec instanceof VmwareDistributedVirtualSwitchTrunkVlanSpec);
        assertTrue(((VmwareDistributedVirtualSwitchTrunkVlanSpec) spec).getVlanId().get(0).getStart() == 200);
        assertTrue(((VmwareDistributedVirtualSwitchTrunkVlanSpec) spec).getVlanId().get(0).getEnd() == 250);
    }

    @Test
    public void testCreateDVPortVlanSpecInvalidMissingVlanRange() {
        VmwareDistributedVirtualSwitchVlanSpec spec = HypervisorHostHelper.createDVPortVlanSpec(null, "200-");
        assertTrue(spec instanceof VmwareDistributedVirtualSwitchVlanIdSpec);
        assertTrue(((VmwareDistributedVirtualSwitchVlanIdSpec) spec).getVlanId() == 0);
    }

    @Test
    public void testCreateDVPortVlanSpecInvalidInputVlanRange() {
        VmwareDistributedVirtualSwitchVlanSpec spec = HypervisorHostHelper.createDVPortVlanSpec(null, "a-b");
        assertTrue(spec instanceof VmwareDistributedVirtualSwitchTrunkVlanSpec);
        assertTrue(((VmwareDistributedVirtualSwitchTrunkVlanSpec) spec).getVlanId().get(0).getStart() == 0);
        assertTrue(((VmwareDistributedVirtualSwitchTrunkVlanSpec) spec).getVlanId().get(0).getEnd() == 0);
    }

    @Test
    public void testSetVMHardwareVersionClusterLevel() throws Exception {
        when(clusterConfigInfo.getDefaultHardwareVersionKey()).thenReturn("vmx-11");
        when(datacenterConfigInfo.getDefaultHardwareVersionKey()).thenReturn("vmx-9");
        HypervisorHostHelper.setVMHardwareVersion(vmSpec, clusterMO, datacenterMO);
        verify(vmSpec).setVersion("vmx-11");
        verify(vmSpec, never()).setVersion("vmx-9");
    }

    @Test
    public void testSetVMHardwareVersionDatacenterLevel() throws Exception {
        when(clusterConfigInfo.getDefaultHardwareVersionKey()).thenReturn(null);
        when(datacenterConfigInfo.getDefaultHardwareVersionKey()).thenReturn("vmx-9");
        HypervisorHostHelper.setVMHardwareVersion(vmSpec, clusterMO, datacenterMO);
        verify(vmSpec).setVersion("vmx-9");
    }

    @Test
    public void testSetVMHardwareVersionUnset() throws Exception {
        when(clusterConfigInfo.getDefaultHardwareVersionKey()).thenReturn(null);
        when(datacenterConfigInfo.getDefaultHardwareVersionKey()).thenReturn(null);
        HypervisorHostHelper.setVMHardwareVersion(vmSpec, clusterMO, datacenterMO);
        verify(vmSpec, never()).setVersion(any());
    }
}
