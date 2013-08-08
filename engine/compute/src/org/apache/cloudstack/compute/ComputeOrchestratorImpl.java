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
package org.apache.cloudstack.compute;

import java.util.logging.Handler;


public class ComputeOrchestratorImpl implements ComputeOrchestrator {

    @Override
    public void cancel(String reservationId) {
    }

    @Override
    public void stop(String vm, String reservationId) {
        // Retrieve the VM
        // Locate the HypervisorGuru based on the VM type
        // Call HypervisorGuru to stop the VM
    }

    @Override
    public void start(String vm, String reservationId, Handler handler) {
        // TODO Auto-generated method stub

    }
}
