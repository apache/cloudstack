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
import org.apache.logging.log4j.Level;

public class DownloadErrorState extends DownloadInactiveState {

    public DownloadErrorState(DownloadListener dl) {
        super(dl);
    }

    @Override
    public String handleAnswer(DownloadAnswer answer) {
        switch (answer.getDownloadStatus()) {
            case DOWNLOAD_IN_PROGRESS:
                getDownloadListener().scheduleStatusCheck(RequestType.GET_STATUS);
                return Status.DOWNLOAD_IN_PROGRESS.toString();
            case DOWNLOADED:
                getDownloadListener().scheduleImmediateStatusCheck(RequestType.PURGE);
                getDownloadListener().cancelTimeoutTask();
                return Status.DOWNLOADED.toString();
            case NOT_DOWNLOADED:
                getDownloadListener().scheduleStatusCheck(RequestType.GET_STATUS);
                return Status.NOT_DOWNLOADED.toString();
            case DOWNLOAD_ERROR:
                getDownloadListener().cancelStatusTask();
                getDownloadListener().cancelTimeoutTask();
                return Status.DOWNLOAD_ERROR.toString();
            case UNKNOWN:
                getDownloadListener().cancelStatusTask();
                getDownloadListener().cancelTimeoutTask();
                return Status.DOWNLOAD_ERROR.toString();
            default:
                return null;
        }
    }

    @Override
    public String handleAbort() {
        return Status.ABANDONED.toString();
    }

    @Override
    public String getName() {
        return Status.DOWNLOAD_ERROR.toString();
    }

    @Override
    public void onEntry(String prevState, DownloadEvent event, Object evtObj) {
        super.onEntry(prevState, event, evtObj);
        if (event == DownloadEvent.DISCONNECT) {
            getDownloadListener().logDisconnect();
            getDownloadListener().cancelStatusTask();
            getDownloadListener().cancelTimeoutTask();
            DownloadAnswer answer = new DownloadAnswer("Storage agent or storage VM disconnected", Status.DOWNLOAD_ERROR);
            getDownloadListener().callback(answer);
            getDownloadListener().log("Entering download error state because the storage host disconnected", Level.WARN);
        } else if (event == DownloadEvent.TIMEOUT_CHECK) {
            DownloadAnswer answer = new DownloadAnswer("Timeout waiting for response from storage host", Status.DOWNLOAD_ERROR);
            getDownloadListener().callback(answer);
            getDownloadListener().log("Entering download error state: timeout waiting for response from storage host", Level.WARN);
        }
        getDownloadListener().setDownloadInactive(Status.DOWNLOAD_ERROR);
    }

}
