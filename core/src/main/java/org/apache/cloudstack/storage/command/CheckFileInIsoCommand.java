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

package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.to.DataStoreTO;

/**
 * Asks the secondary storage VM whether an ISO on the given image store contains a file.
 */
public class CheckFileInIsoCommand extends StorageSubSystemCommand {

    private DataStoreTO store;
    private String isoPath;
    private String filePath;

    public CheckFileInIsoCommand() {
        super();
    }

    public CheckFileInIsoCommand(final DataStoreTO store, final String isoPath, final String filePath) {
        super();
        this.store = store;
        this.isoPath = isoPath;
        this.filePath = filePath;
    }

    public DataStoreTO getStore() {
        return store;
    }

    /**
     * @return path of the ISO, relative to the root of the image store
     */
    public String getIsoPath() {
        return isoPath;
    }

    /**
     * @return path of the file to look for, relative to the root of the ISO
     */
    public String getFilePath() {
        return filePath;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public void setExecuteInSequence(boolean inSeq) {}
}
