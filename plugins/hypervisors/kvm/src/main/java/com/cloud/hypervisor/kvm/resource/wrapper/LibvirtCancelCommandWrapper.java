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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

/** Can another request in flight on this agent be stopped (checkOnly), or stop it. Out of sequence, so it is not queued behind that request. */
@ResourceWrapper(handles = CancelCommand.class)
public final class LibvirtCancelCommandWrapper extends CommandWrapper<CancelCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final CancelCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final long sequence = command.getSequence();
        if (command.isCheckOnly()) {
            final boolean cancellable = libvirtComputingResource.isRequestSequenceCancellable(sequence);
            return new Answer(command, cancellable, cancellable
                    ? "request sequence " + sequence + " can be cancelled"
                    : "request sequence " + sequence + " has no work that can be stopped on this host");
        }

        final boolean cancelled = libvirtComputingResource.cancelRequestSequence(sequence);
        return new Answer(command, cancelled, cancelled
                ? "request sequence " + sequence + " cancelled (" + command.getReason() + ")"
                : "request sequence " + sequence + " could not be cancelled on this host");
    }
}
