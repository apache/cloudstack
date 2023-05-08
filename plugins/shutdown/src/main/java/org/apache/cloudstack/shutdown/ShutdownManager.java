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

package org.apache.cloudstack.shutdown;

import org.apache.cloudstack.api.command.CancelShutdownCmd;
import org.apache.cloudstack.api.command.PrepareForShutdownCmd;
import org.apache.cloudstack.api.command.ReadyForShutdownCmd;
import org.apache.cloudstack.api.command.TriggerShutdownCmd;
import org.apache.cloudstack.api.response.ReadyForShutdownResponse;

public interface ShutdownManager {
    // Returns the number of pending jobs for the given Management server msids.
    // NOTE: This is the msid and NOT the id
    long countPendingJobs(Long... msIds);

    // Indicates whether a shutdown has been triggered on the current management server
    boolean isShutdownTriggered();

    // Indicates whether the current management server is preparing to shutdown
    boolean isPreparingForShutdown();

    // Triggers a shutdown on the current management server by not accepting any more async jobs and shutting down when there are no pending jobs
    void triggerShutdown();

    // Prepares the current management server to shutdown by not accepting any more async jobs
    void prepareForShutdown();

    // Cancels the shutdown on the current management server
    void cancelShutdown();

    // Returns whether the given ms can be shut down
    ReadyForShutdownResponse readyForShutdown(Long managementserverid);

    // Returns whether the any of the ms can be shut down and if a shutdown has been triggered on any running ms
    ReadyForShutdownResponse readyForShutdown(ReadyForShutdownCmd cmd);

    // Prepares the specified management server to shutdown by not accepting any more async jobs
    ReadyForShutdownResponse prepareForShutdown(PrepareForShutdownCmd cmd);

    // Cancels the shutdown on the specified management server
    ReadyForShutdownResponse cancelShutdown(CancelShutdownCmd cmd);

    // Triggers a shutdown on the specified management server by not accepting any more async jobs and shutting down when there are no pending jobs
    ReadyForShutdownResponse triggerShutdown(TriggerShutdownCmd cmd);
}
