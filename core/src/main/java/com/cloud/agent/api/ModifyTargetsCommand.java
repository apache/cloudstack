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

package com.cloud.agent.api;

import java.util.List;
import java.util.Map;

public class ModifyTargetsCommand extends Command {
    public enum TargetTypeToRemove { BOTH, NEITHER, STATIC, DYNAMIC }

    public static final String IQN = "iqn";
    public static final String STORAGE_TYPE = "storageType";
    public static final String STORAGE_UUID = "storageUuid";
    public static final String STORAGE_HOST = "storageHost";
    public static final String STORAGE_PORT = "storagePort";
    public static final String CHAP_NAME = "chapName";
    public static final String CHAP_SECRET = "chapSecret";
    public static final String MUTUAL_CHAP_NAME = "mutualChapName";
    public static final String MUTUAL_CHAP_SECRET = "mutualChapSecret";

    private boolean add;
    private boolean applyToAllHostsInCluster;
    private TargetTypeToRemove targetTypeToRemove = TargetTypeToRemove.BOTH;
    private boolean removeAsync;
    private List<Map<String, String>> targets;

    public void setAdd(boolean add) {
        this.add = add;
    }

    public boolean getAdd() {
        return add;
    }

    public void setApplyToAllHostsInCluster(boolean applyToAllHostsInCluster) {
        this.applyToAllHostsInCluster = applyToAllHostsInCluster;
    }

    public boolean getApplyToAllHostsInCluster() {
        return applyToAllHostsInCluster;
    }

    public void setTargetTypeToRemove(TargetTypeToRemove targetTypeToRemove) {
        this.targetTypeToRemove = targetTypeToRemove;
    }

    public TargetTypeToRemove getTargetTypeToRemove() {
        return targetTypeToRemove;
    }

    public void setRemoveAsync(boolean removeAsync) {
        this.removeAsync = removeAsync;
    }

    public boolean isRemoveAsync() {
        return removeAsync;
    }

    public void setTargets(List<Map<String, String>> targets) {
        this.targets = targets;
    }

    public List<Map<String, String>> getTargets() {
        return targets;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
