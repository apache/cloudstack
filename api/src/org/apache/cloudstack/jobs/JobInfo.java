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
package org.apache.cloudstack.jobs;

import java.util.Date;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface JobInfo extends Identity, InternalIdentity {
    public enum Status {
        IN_PROGRESS(false), SUCCEEDED(true), FAILED(true), CANCELLED(true);

        private final boolean done;

        private Status(boolean done) {
            this.done = done;
        }

        public boolean done() {
            return done;
        }
    }

    String getType();

    String getDispatcher();

    int getPendingSignals();

    long getUserId();

    long getAccountId();

    String getCmd();

    int getCmdVersion();

    String getCmdInfo();

    Status getStatus();

    int getProcessStatus();

    int getResultCode();

    String getResult();

    Long getInitMsid();

    Long getExecutingMsid();

    Long getCompleteMsid();

    Date getCreated();

    Date getLastUpdated();

    Date getLastPolled();

    String getInstanceType();

    Long getInstanceId();
}
