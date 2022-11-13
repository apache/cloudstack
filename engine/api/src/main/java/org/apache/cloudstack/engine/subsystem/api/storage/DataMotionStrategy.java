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

import java.util.Map;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.Host;

/**
 * Interface to query how to move data around and to commission the moving
 */
public interface DataMotionStrategy {
    /**
     * Reports whether this instance can do a move from source to destination
     * @param srcData object to move
     * @param destData location to move it to
     * @return the expertise level with which this instance knows how to handle the move
     */
    StrategyPriority canHandle(DataObject srcData, DataObject destData);

    StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost);

    /**
     * Copy the source volume to its destination (on a host if not null)
     *
     * @param srcData volume to move
     * @param destData volume description as intended after the move
     * @param destHost if not null destData should be reachable from here
     * @param callback where to report completion or failure to
     */
    void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback);

    void copyAsync(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback);
}
