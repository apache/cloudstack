//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.api.to.NetworkTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase.NfsStoragePool;
import com.cloud.hypervisor.kvm.resource.KVMHAChecker;
import com.cloud.hypervisor.kvm.resource.KVMHAMonitor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  CheckOnHostCommand.class)
public final class LibvirtCheckOnHostCommandWrapper extends CommandWrapper<CheckOnHostCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CheckOnHostCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final ExecutorService executors = Executors.newSingleThreadExecutor();
        final KVMHAMonitor monitor = libvirtComputingResource.getMonitor();

        final List<NfsStoragePool> pools = monitor.getStoragePools();
        final HostTO host = command.getHost();
        final NetworkTO privateNetwork = host.getPrivateNetwork();
        final KVMHAChecker ha = new KVMHAChecker(pools, privateNetwork.getIp());

        final Future<Boolean> future = executors.submit(ha);
        try {
            final Boolean result = future.get();
            if (result) {
                return new Answer(command, false, "Heart is beating...");
            } else {
                return new Answer(command);
            }
        } catch (final InterruptedException e) {
            return new Answer(command, false, "CheckOnHostCommand: can't get status of host: InterruptedException");
        } catch (final ExecutionException e) {
            return new Answer(command, false, "CheckOnHostCommand: can't get status of host: ExecutionException");
        }
    }
}
