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
package com.cloud.network.router;

import com.cloud.agent.api.routing.VmDataCommand;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
public class CommandSetupHelperTest {

    @InjectMocks
    protected CommandSetupHelper commandSetupHelper = new CommandSetupHelper();

    @Test
    public void testUserDataDetails() {
        VmDataCommand vmDataCommand = new VmDataCommand("testVMname");
        String testUserDataDetails = new String("{test1=value1,test2=value2}");
        commandSetupHelper.addUserDataDetailsToCommand(vmDataCommand, testUserDataDetails);

        List<String[]> metadata = vmDataCommand.getVmData();
        String[] metadataFile1 = metadata.get(0);
        String[] metadataFile2 = metadata.get(1);

        Assert.assertEquals("metadata", metadataFile1[0]);
        Assert.assertEquals("metadata", metadataFile2[0]);

        Assert.assertEquals("test1", metadataFile1[1]);
        Assert.assertEquals("test2", metadataFile2[1]);

        Assert.assertEquals("value1", metadataFile1[2]);
        Assert.assertEquals("value2", metadataFile2[2]);
    }

    @Test
    public void testNullUserDataDetails() {
        VmDataCommand vmDataCommand = new VmDataCommand("testVMname");
        String testUserDataDetails = null;
        commandSetupHelper.addUserDataDetailsToCommand(vmDataCommand, testUserDataDetails);
        Assert.assertEquals(new ArrayList<>(), vmDataCommand.getVmData());
    }

    @Test
    public void testUserDataDetailsWithWhiteSpaces() {
        VmDataCommand vmDataCommand = new VmDataCommand("testVMname");
        String testUserDataDetails = new String("{test1 =value1,test2= value2 }");
        commandSetupHelper.addUserDataDetailsToCommand(vmDataCommand, testUserDataDetails);

        List<String[]> metadata = vmDataCommand.getVmData();
        String[] metadataFile1 = metadata.get(0);
        String[] metadataFile2 = metadata.get(1);

        Assert.assertEquals("metadata", metadataFile1[0]);
        Assert.assertEquals("metadata", metadataFile2[0]);

        Assert.assertEquals("test1", metadataFile1[1]);
        Assert.assertEquals("test2", metadataFile2[1]);

        Assert.assertEquals("value1", metadataFile1[2]);
        Assert.assertEquals("value2", metadataFile2[2]);
    }
}
