//
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
//

package org.apache.cloudstack.storage.command;

import java.util.HashMap;
import java.util.Map;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;

public final class CopyCommand extends Command implements StorageSubSystemCommand {
    private DataTO srcTO;
    private DataTO destTO;
    private DataTO cacheTO;
    private boolean executeInSequence = false;
    private Map<String, String> options = new HashMap<String, String>();
    private Map<String, String> options2 = new HashMap<String, String>();

    public CopyCommand(DataTO srcData, DataTO destData, int timeout, boolean executeInSequence) {
        super();
        this.srcTO = srcData;
        this.destTO = destData;
        this.setWait(timeout);
        this.executeInSequence = executeInSequence;
    }

    public DataTO getDestTO() {
        return this.destTO;
    }

    public void setSrcTO(DataTO srcTO) {
        this.srcTO = srcTO;
    }

    public void setDestTO(DataTO destTO) {
        this.destTO = destTO;
    }

    public DataTO getSrcTO() {
        return this.srcTO;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

    public DataTO getCacheTO() {
        return cacheTO;
    }

    public void setCacheTO(DataTO cacheTO) {
        this.cacheTO = cacheTO;
    }

    public int getWaitInMillSeconds() {
        return this.getWait() * 1000;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions2(Map<String, String> options2) {
        this.options2 = options2;
    }

    public Map<String, String> getOptions2() {
        return options2;
    }

    @Override
    public void setExecuteInSequence(boolean inSeq) {
        this.executeInSequence = inSeq;
    }
}
