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

import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.DownloadAnswer;

public abstract class DownloadState {
    public static enum DownloadEvent {
        DOWNLOAD_ANSWER, ABANDON_DOWNLOAD, TIMEOUT_CHECK, DISCONNECT
    };

    protected static final Logger s_logger = Logger.getLogger(DownloadState.class.getName());

    private DownloadListener dl;

    public DownloadState(DownloadListener dl) {
        this.dl = dl;
    }

    protected DownloadListener getDownloadListener() {
        return dl;
    }

    public String handleEvent(DownloadEvent event, Object eventObj) {
        if (s_logger.isTraceEnabled()) {
            getDownloadListener().log("handleEvent, event type=" + event + ", curr state=" + getName(), Level.TRACE);
        }
        switch (event) {
            case DOWNLOAD_ANSWER:
                DownloadAnswer answer = (DownloadAnswer)eventObj;
                return handleAnswer(answer);
            case ABANDON_DOWNLOAD:
                return handleAbort();
            case TIMEOUT_CHECK:
                Date now = new Date();
                long update = now.getTime() - dl.getLastUpdated().getTime();
                return handleTimeout(update);
            case DISCONNECT:
                return handleDisconnect();
        }
        return null;
    }

    public void onEntry(String prevState, DownloadEvent event, Object evtObj) {
        if (s_logger.isTraceEnabled()) {
            getDownloadListener().log("onEntry, event type=" + event + ", curr state=" + getName(), Level.TRACE);
        }
        if (event == DownloadEvent.DOWNLOAD_ANSWER) {
            getDownloadListener().callback((DownloadAnswer)evtObj);
        }
    }

    public void onExit() {

    }

    public abstract String handleTimeout(long updateMs);

    public abstract String handleAbort();

    public abstract String handleDisconnect();

    public abstract String handleAnswer(DownloadAnswer answer);

    public abstract String getName();

}
