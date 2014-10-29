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
package com.cloud.storage.download;

import org.apache.cloudstack.storage.command.DownloadProgressCommand.RequestType;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public class DownloadAbandonedState extends DownloadInactiveState {

    public DownloadAbandonedState(DownloadListener dl) {
        super(dl);
    }

    @Override
    public String getName() {
        return Status.ABANDONED.toString();
    }

    @Override
    public void onEntry(String prevState, DownloadEvent event, Object evtObj) {
        super.onEntry(prevState, event, evtObj);
        if (!prevState.equalsIgnoreCase(getName())) {
            DownloadAnswer answer = new DownloadAnswer("Download canceled", Status.ABANDONED);
            getDownloadListener().callback(answer);
            getDownloadListener().cancelStatusTask();
            getDownloadListener().cancelTimeoutTask();
            getDownloadListener().sendCommand(RequestType.ABORT);
        }
    }

}
