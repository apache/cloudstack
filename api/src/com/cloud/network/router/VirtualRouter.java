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

import com.cloud.vm.VirtualMachine;

/**
 *  bridge internal and external traffic.
 */
public interface VirtualRouter extends VirtualMachine {
    public enum Role {
        VIRTUAL_ROUTER, LB, INTERNAL_LB_VM
    }

    public enum UpdateState {
        UPDATE_NEEDED, UPDATE_IN_PROGRESS, UPDATE_COMPLETE, UPDATE_FAILED
    }

    Role getRole();

    boolean getIsRedundantRouter();

    public enum RedundantState {
        UNKNOWN, MASTER, BACKUP, FAULT
    }

    RedundantState getRedundantState();

    String getPublicIpAddress();

    boolean isStopPending();

    void setStopPending(boolean stopPending);

    /**
     * @return
     */
    Long getVpcId();

    String getTemplateVersion();
}
