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
package org.apache.cloudstack.api.command.test;

import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmService;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class ScaleVMCmdTest extends TestCase{

    private ScaleVMCmd scaleVMCmd;
    private ResponseGenerator responseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {

        scaleVMCmd = new ScaleVMCmd(){
            @Override
            public Long getId() {
                return 2L;
            }
            @Override
            public String getCommandName() {
                return "scalevirtualmachineresponse";
            }
        };
    }


    @Test
    public void testCreateSuccess() {

        UserVmService userVmService = Mockito.mock(UserVmService.class);

        try {
            Mockito.when(
                    userVmService.upgradeVirtualMachine(scaleVMCmd))
                    .thenReturn(true);
        }catch (Exception e){
            Assert.fail("Received exception when success expected " +e.getMessage());
        }

        scaleVMCmd._userVmService = userVmService;
        responseGenerator = Mockito.mock(ResponseGenerator.class);

        scaleVMCmd._responseGenerator = responseGenerator;
        scaleVMCmd.execute();

    }

    @Test
    public void testCreateFailure() {

        UserVmService userVmService = Mockito.mock(UserVmService.class);

        try {
            Mockito.when(
                    userVmService.upgradeVirtualMachine(scaleVMCmd))
                    .thenReturn(false);
        }catch (Exception e){
            Assert.fail("Received exception when success expected " +e.getMessage());
        }

        scaleVMCmd._userVmService = userVmService;

        try {
            scaleVMCmd.execute();
        } catch (ServerApiException exception) {
            Assert.assertEquals("Failed to scale vm",
                    exception.getDescription());
        }

    }
}
