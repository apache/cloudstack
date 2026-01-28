//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import java.util.List;
import java.util.Map;

import com.cloud.agent.api.Command;

public class GetImageTransferProgressCommand extends Command {
    private List<String> transferIds;
    private Map<String, String> volumePaths; // transferId -> volume path
    private Map<String, Long> volumeSizes; // transferId -> volume size

    public GetImageTransferProgressCommand() {
    }

    public GetImageTransferProgressCommand(List<String> transferIds, Map<String, String> volumePaths, Map<String, Long> volumeSizes) {
        this.transferIds = transferIds;
        this.volumePaths = volumePaths;
        this.volumeSizes = volumeSizes;
    }

    public List<String> getTransferIds() {
        return transferIds;
    }

    public void setTransferIds(List<String> transferIds) {
        this.transferIds = transferIds;
    }

    public Map<String, String> getVolumePaths() {
        return volumePaths;
    }

    public void setVolumePaths(Map<String, String> volumePaths) {
        this.volumePaths = volumePaths;
    }

    public Map<String, Long> getVolumeSizes() {
        return volumeSizes;
    }

    public void setVolumeSizes(Map<String, Long> volumeSizes) {
        this.volumeSizes = volumeSizes;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
