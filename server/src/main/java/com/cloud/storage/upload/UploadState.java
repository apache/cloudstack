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
package com.cloud.storage.upload;

import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.agent.api.storage.UploadAnswer;

public abstract class UploadState {

    public static enum UploadEvent {
        UPLOAD_ANSWER, ABANDON_UPLOAD, TIMEOUT_CHECK, DISCONNECT
    };

    protected static final Logger s_logger = Logger.getLogger(UploadListener.class.getName());

    private UploadListener ul;

    public UploadState(UploadListener ul) {
        this.ul = ul;
    }

    protected UploadListener getUploadListener() {
        return ul;
    }

    public String handleEvent(UploadEvent event, Object eventObj) {
        if (s_logger.isTraceEnabled()) {
            getUploadListener().log("handleEvent, event type=" + event + ", curr state=" + getName(), Level.TRACE);
        }
        switch (event) {
            case UPLOAD_ANSWER:
                UploadAnswer answer = (UploadAnswer)eventObj;
                return handleAnswer(answer);
            case ABANDON_UPLOAD:
                return handleAbort();
            case TIMEOUT_CHECK:
                Date now = new Date();
                long update = now.getTime() - ul.getLastUpdated().getTime();
                return handleTimeout(update);
            case DISCONNECT:
                return handleDisconnect();
        }
        return null;
    }

    public void onEntry(String prevState, UploadEvent event, Object evtObj) {
        if (s_logger.isTraceEnabled()) {
            getUploadListener().log("onEntry, event type=" + event + ", curr state=" + getName(), Level.TRACE);
        }
        if (event == UploadEvent.UPLOAD_ANSWER) {
            getUploadListener().updateDatabase((UploadAnswer)evtObj);
        }
    }

    public void onExit() {

    }

    public abstract String handleTimeout(long updateMs);

    public abstract String handleAbort();

    public abstract String handleDisconnect();

    public abstract String handleAnswer(UploadAnswer answer);

    public abstract String getName();

}
