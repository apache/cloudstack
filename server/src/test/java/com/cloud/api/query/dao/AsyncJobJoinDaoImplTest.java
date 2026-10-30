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
package com.cloud.api.query.dao;

import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.junit.Assert;
import org.junit.Test;

public class AsyncJobJoinDaoImplTest {

    @Test
    public void commandsThatSomeHypervisorCanStopAreCancellable() {
        Assert.assertTrue(AsyncJobJoinDaoImpl.isCancellableCommand(CreateVolumeCmd.class.getName()));
        Assert.assertTrue(AsyncJobJoinDaoImpl.isCancellableCommand(StartVMCmd.class.getName()));
    }

    @Test
    public void commandsNotAnnotatedAreNot() {
        Assert.assertFalse(AsyncJobJoinDaoImpl.isCancellableCommand(DeployVMCmd.class.getName()));
    }

    @Test
    public void unknownOrMissingCommandsAreNotCancellable() {
        Assert.assertFalse(AsyncJobJoinDaoImpl.isCancellableCommand("org.apache.cloudstack.api.command.NoSuchCmd"));
        Assert.assertFalse(AsyncJobJoinDaoImpl.isCancellableCommand(null));
    }
}
