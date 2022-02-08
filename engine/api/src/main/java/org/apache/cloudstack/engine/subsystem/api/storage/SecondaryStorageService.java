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

package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.utils.Pair;

public interface SecondaryStorageService {
    class DataObjectResult extends CommandResult {
        private final DataObject data;

        public DataObjectResult(DataObject data) {
            super();
            this.data = data;
        }

        public DataObject getData() {
            return this.data;
        }

    }
    AsyncCallFuture<DataObjectResult> migrateData(DataObject srcDataObject, DataStore srcDatastore, DataStore destDatastore, Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChain,
                                                  Map<DataObject, Pair<List<TemplateInfo>, Long>> templateChain);
}
