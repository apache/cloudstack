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
package org.apache.cloudstack.command;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.to.VirtualMachineTO;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

public class ReconcileCommandUtilsTest {

    final static String COMMANDS_LOG_PATH = "/tmp";

    @Test
    public void createAndReadAndDeleteLogFilesForCommand() {
        final String vmName = "TestVM";
        final String destIp = "DestinationHost";
        final VirtualMachineTO vmTO = Mockito.mock(VirtualMachineTO.class);
        final MigrateCommand command = new MigrateCommand(vmName, destIp, false, vmTO, false);
        long requestSequence = 10000L;
        command.setRequestSequence(requestSequence);

        String logFile = ReconcileCommandUtils.getLogFileNameForCommand(COMMANDS_LOG_PATH, command);

        ReconcileCommandUtils.createLogFileForCommand(COMMANDS_LOG_PATH, command);
        Assert.assertTrue((new File(logFile).exists()));

        CommandInfo commandInfo = ReconcileCommandUtils.readLogFileForCommand(logFile);
        Assert.assertNotNull(commandInfo);
        Assert.assertEquals(command.getClass().getName(), commandInfo.getCommandName());
        Assert.assertEquals(Command.State.CREATED, commandInfo.getState());

        Command parseCommand = ReconcileCommandUtils.parseCommandInfo(commandInfo);
        System.out.println("command state is " + commandInfo);
        Assert.assertNotNull(parseCommand);
        Assert.assertTrue(parseCommand instanceof MigrateCommand);
        Assert.assertEquals(vmName,((MigrateCommand) parseCommand).getVmName());
        Assert.assertEquals(destIp,((MigrateCommand) parseCommand).getDestinationIp());

        ReconcileCommandUtils.updateLogFileForCommand(COMMANDS_LOG_PATH, command, Command.State.PROCESSING);
        CommandInfo newCommandInfo = ReconcileCommandUtils.readLogFileForCommand(logFile);
        System.out.println("new command state is " + newCommandInfo);
        Assert.assertNotNull(newCommandInfo);
        Assert.assertEquals(command.getClass().getName(), newCommandInfo.getCommandName());
        Assert.assertEquals(Command.State.PROCESSING, newCommandInfo.getState());

        ReconcileCommandUtils.deleteLogFileForCommand(COMMANDS_LOG_PATH, command);
        Assert.assertFalse((new File(logFile).exists()));
    }
}
