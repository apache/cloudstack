/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering.DiskCacheMode;
import com.cloud.storage.Volume;
import com.cloud.vm.VirtualMachine;

public interface VolumeInfo extends DataObject, Volume {
    boolean isAttachedVM();

    void addPayload(Object data);

    Object getpayload();

    HypervisorType getHypervisorType();

    Long getLastPoolId();

    String getAttachedVmName();
    VirtualMachine getAttachedVM();

    void processEventOnly(ObjectInDataStoreStateMachine.Event event);

    void processEventOnly(ObjectInDataStoreStateMachine.Event event, Answer answer);

    boolean stateTransit(Volume.Event event);

    Long getBytesReadRate();

    Long getBytesWriteRate();

    Long getIopsReadRate();

    Long getIopsWriteRate();

    DiskCacheMode getCacheMode();
}
