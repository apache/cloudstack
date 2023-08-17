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
package com.cloud.hypervisor.guru;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.vm.VmDetailConstants;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VmwareVmImplementerTest {

    @Spy
    @InjectMocks
    private VmwareVmImplementer implementer = new VmwareVmImplementer();

    @Mock
    VirtualMachineTO vmTO;

    private Map<String,String> vmDetails = new HashMap<String, String>();

    AutoCloseable closeable;

    @Before
    public void testSetUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private void setConfigValues(Boolean globalNV, Boolean globalNVPVM, String localNV){
        implementer.setGlobalNestedVirtualisationEnabled(globalNV.booleanValue());
        implementer.setGlobalNestedVPerVMEnabled(globalNVPVM.booleanValue());
        if (localNV != null) {
            vmDetails.put(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG, localNV);
        }
    }

    private void executeAndVerifyTest(Boolean globalNV, Boolean globalNVPVM, String localNV, Boolean expectedResult){
        Boolean result = implementer.shouldEnableNestedVirtualization(globalNV, globalNVPVM, localNV);
        assertEquals(expectedResult, result);
    }

    @Test
    public void testConfigureNestedVirtualization(){
        setConfigValues(true, true, null);
        implementer.configureNestedVirtualization(vmDetails, vmTO);
        Map<String,String> spyDetails = Mockito.spy(vmDetails);

        verify(implementer).shouldEnableNestedVirtualization(true, true, null);

        assertTrue(vmDetails.containsKey(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG));
        assertEquals(Boolean.toString(true), vmDetails.get(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG));

    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVTrueGlobalNVPVMTrueLocalNVNull(){
        executeAndVerifyTest(true, true, null, true);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVTrueGlobalNVPVMTrueLocalNVTrue(){
        executeAndVerifyTest(true, true, "true", true);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVTrueGlobalNVPVMTrueLocalNVFalse(){
        executeAndVerifyTest(true, true, "false", false);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVTrueGlobalNVPVMFalseLocalNVNull(){
        executeAndVerifyTest(true, false, null, true);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVTrueGlobalNVPVMFalseLocalNVTrue(){
        executeAndVerifyTest(true, false, "true", true);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVTrueGlobalNVPVMFalseLocalNVNFalse(){
        executeAndVerifyTest(true, false, "false", true);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVFalseGlobalNVPVMTrueLocalNVNull(){
        executeAndVerifyTest(false, true, null, false);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVFalseGlobalNVPVMTrueLocalNVTrue(){
        executeAndVerifyTest(false, true, "true", true);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVFalseGlobalNVPVMTrueLocalNVFalse(){
        executeAndVerifyTest(false, true, "false", false);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVFalseGlobalNVPVMFalseLocalNVNull(){
        executeAndVerifyTest(false, false, null, false);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVFalseGlobalNVPVMFalseLocalNVTrue(){
        executeAndVerifyTest(false, false, "true", false);
    }

    @Test
    public void testEnableNestedVirtualizationCaseGlobalNVFalseGlobalNVPVMFalseLocalNVFalse(){
        executeAndVerifyTest(false, false, "false", false);
    }

}
