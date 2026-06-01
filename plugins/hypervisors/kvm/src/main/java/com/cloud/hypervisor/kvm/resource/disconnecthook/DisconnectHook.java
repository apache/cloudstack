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
package com.cloud.hypervisor.kvm.resource.disconnecthook;

/**
DisconnectHooks can be used to cleanup/cancel long running commands when
connection to the management server is interrupted (which results in job
failure). Agent CommandWrappers can register a hook with the
libvirtComputingResource at the beginning of processing, and
libvirtComputingResource will call it upon disconnect. The CommandWrapper can
also remove the hook upon completion of the command.

DisconnectHooks should implement a run() method that is safe to call and will
fail cleanly if there is no cleanup to do. Otherwise the CommandWrapper
registering/deregistering the hook should account for any race conditions
introduced by the ordering of when the command is processed and when the hook
is registered/deregistered.

If a timeout is set, the hook's run() will be interrupted. It will be up to
run() to determine what to do with the InterruptedException, but the hook
processing will not wait any longer for the hook to complete.

Avoid doing anything time intensive as DisconnectHooks will delay agent
shutdown.
*/

public abstract class DisconnectHook extends Thread {
    // Default timeout is 10 seconds
    long timeoutMs = 10000;

    public DisconnectHook(String name) {
        super();
        this.setName(this.getClass().getName() + "-" + name);
    }

    public DisconnectHook(String name, long timeout) {
        this(name);
        this.timeoutMs = timeout;
    }

    public long getTimeoutMs(){ return timeoutMs; }

}
