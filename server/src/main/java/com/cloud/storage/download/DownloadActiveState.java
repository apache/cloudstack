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

public abstract class DownloadActiveState extends DownloadState {

    public DownloadActiveState(DownloadListener dl) {
        super(dl);
    }

    @Override
    public String handleAnswer(DownloadAnswer answer) {
        if (logger.isTraceEnabled()) {
            logger.trace("handleAnswer, answer status=" + answer.getDownloadStatus() + ", curr state=" + getName());
        }
        switch (answer.getDownloadStatus()) {
        case DOWNLOAD_IN_PROGRESS:
            getDownloadListener().scheduleStatusCheck(RequestType.GET_STATUS);
            return Status.DOWNLOAD_IN_PROGRESS.toString();
        case DOWNLOADED:
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
    public void onEntry(String prevState, DownloadEvent event, Object evtObj) {
        super.onEntry(prevState, event, evtObj);

        if (event == DownloadEvent.DOWNLOAD_ANSWER) {
            getDownloadListener().setLastUpdated();
        }
    }

    @Override
    public void onExit() {
    }

    @Override
    public String handleTimeout(long updateMs) {
        if (logger.isTraceEnabled()) {
            getDownloadListener().log("handleTimeout, updateMs=" + updateMs + ", curr state= " + getName(), Level.TRACE);
        }
        String newState = getName();
        if (updateMs > 5 * DownloadListener.STATUS_POLL_INTERVAL) {
            newState = Status.DOWNLOAD_ERROR.toString();
            getDownloadListener().log("timeout: transitioning to download error state, currstate=" + getName(), Level.DEBUG);
        } else if (updateMs > 3 * DownloadListener.STATUS_POLL_INTERVAL) {
            getDownloadListener().cancelStatusTask();
            getDownloadListener().scheduleImmediateStatusCheck(RequestType.GET_STATUS);
            getDownloadListener().scheduleTimeoutTask(3 * DownloadListener.STATUS_POLL_INTERVAL);
            getDownloadListener().log(getName() + " first timeout: checking again ", Level.DEBUG);
        } else {
            getDownloadListener().scheduleTimeoutTask(3 * DownloadListener.STATUS_POLL_INTERVAL);
        }
        return newState;
    }

    @Override
    public String handleAbort() {
        return Status.ABANDONED.toString();
    }

    @Override
    public String handleDisconnect() {

        return Status.DOWNLOAD_ERROR.toString();
    }

}
