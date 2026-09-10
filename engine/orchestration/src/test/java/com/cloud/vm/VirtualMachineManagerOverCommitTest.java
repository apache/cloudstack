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
package com.cloud.vm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * A service offering may override its cluster's overcommit ratio. These cover what gets recorded on
 * the VM as a result, which is what capacity accounting and the hypervisor then read.
 */
@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineManagerOverCommitTest {

    private static final long VM_ID = 1L;
    private static final long OFFERING_ID = 5L;
    private static final String CPU = VmDetailConstants.CPU_OVER_COMMIT_RATIO;
    private static final String MEMORY = VmDetailConstants.MEMORY_OVER_COMMIT_RATIO;
    private static final String RECLAIM = VmDetailConstants.MEMORY_RECLAIM_DISABLED;

    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    @Mock
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @InjectMocks
    private VirtualMachineManagerImpl vmManager = new VirtualMachineManagerImpl();

    private VirtualMachineProfile profile() {
        VirtualMachineProfile profile = Mockito.mock(VirtualMachineProfile.class);
        Mockito.lenient().when(profile.getId()).thenReturn(VM_ID);
        Mockito.lenient().when(profile.getServiceOfferingId()).thenReturn(OFFERING_ID);
        return profile;
    }

    private void offeringSays(String key, String value) {
        Mockito.when(serviceOfferingDetailsDao.getDetail(OFFERING_ID, key)).thenReturn(value);
    }

    private void vmDetailIs(String key, String value) {
        VMInstanceDetailVO detail = value == null ? null : Mockito.mock(VMInstanceDetailVO.class);
        if (detail != null) {
            Mockito.lenient().when(detail.getValue()).thenReturn(value);
        }
        Mockito.lenient().when(vmInstanceDetailsDao.findDetail(VM_ID, key)).thenReturn(detail);
    }

    // --- what the offering asks for ---

    @Test
    public void testOfferingWithNoDetailInheritsTheCluster() {
        offeringSays(CPU, null);
        assertNull(vmManager.offeringOverCommitRatio(profile(), CPU));
    }

    @Test
    public void testOfferingRatioIsUsed() {
        offeringSays(CPU, "1");
        assertEquals(1.0f, vmManager.offeringOverCommitRatio(profile(), CPU), 1e-6);
    }

    @Test
    public void testNonsenseOfferingRatioInheritsTheCluster() {
        offeringSays(CPU, "not a number");
        assertNull(vmManager.offeringOverCommitRatio(profile(), CPU));
    }

    @Test
    public void testNonPositiveOfferingRatioInheritsTheCluster() {
        offeringSays(CPU, "0");
        assertNull(vmManager.offeringOverCommitRatio(profile(), CPU));
    }

    // --- what gets recorded on the VM ---

    @Test
    public void testAnExemptOfferingRecordsItsRatioSoCapacityCanScaleIt() {
        // ratio 1 in a cluster overcommitted 10 times: without the detail the VM would be charged
        // a tenth of what it actually holds
        vmDetailIs(MEMORY, null);

        vmManager.persistOverCommitRatio(VM_ID, MEMORY, 1.0f, 10.0f);

        Mockito.verify(vmInstanceDetailsDao).addDetail(VM_ID, MEMORY, "1.0", true);
    }

    @Test
    public void testAnOvercommittedClusterRecordsItsRatio() {
        vmDetailIs(CPU, null);

        vmManager.persistOverCommitRatio(VM_ID, CPU, 10.0f, 10.0f);

        Mockito.verify(vmInstanceDetailsDao).addDetail(VM_ID, CPU, "10.0", true);
    }

    @Test
    public void testNothingIsRecordedWhenNobodyOvercommits() {
        vmDetailIs(CPU, null);

        vmManager.persistOverCommitRatio(VM_ID, CPU, 1.0f, 1.0f);

        Mockito.verify(vmInstanceDetailsDao, Mockito.never())
                .addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testAStaleRatioIsRewritten() {
        vmDetailIs(CPU, "4.0");

        vmManager.persistOverCommitRatio(VM_ID, CPU, 10.0f, 10.0f);

        Mockito.verify(vmInstanceDetailsDao).addDetail(VM_ID, CPU, "10.0", true);
    }

    @Test
    public void testAnUnreadableStoredRatioIsRewritten() {
        vmDetailIs(CPU, "rubbish");

        vmManager.persistOverCommitRatio(VM_ID, CPU, 10.0f, 10.0f);

        Mockito.verify(vmInstanceDetailsDao).addDetail(VM_ID, CPU, "10.0", true);
    }

    // --- memory reclaim ---

    @Test
    public void testReclaimIsDisabledOnlyWhenTheOfferingAsksForIt() {
        vmDetailIs(RECLAIM, null);

        vmManager.persistMemoryReclaimFlag(VM_ID, 1.0f);

        Mockito.verify(vmInstanceDetailsDao).addDetail(VM_ID, RECLAIM, "true", true);
    }

    @Test
    public void testReclaimIsUntouchedWhenTheOfferingSaysNothing() {
        // the default everywhere: no offering override, and clusters ship with a ratio of 1.
        // reading that as "pin every VM's memory" would change behaviour for installations that
        // opted into nothing at all
        vmDetailIs(RECLAIM, null);

        vmManager.persistMemoryReclaimFlag(VM_ID, null);

        Mockito.verify(vmInstanceDetailsDao, Mockito.never())
                .addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testReclaimStaysOnForAnOfferingThatStillOvercommits() {
        vmDetailIs(RECLAIM, null);

        vmManager.persistMemoryReclaimFlag(VM_ID, 4.0f);

        Mockito.verify(vmInstanceDetailsDao, Mockito.never())
                .addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testReclaimIsRestoredWhenAnOfferingStopsExemptingItself() {
        vmDetailIs(RECLAIM, "true");

        vmManager.persistMemoryReclaimFlag(VM_ID, null);

        Mockito.verify(vmInstanceDetailsDao).removeDetail(VM_ID, RECLAIM);
    }
}
