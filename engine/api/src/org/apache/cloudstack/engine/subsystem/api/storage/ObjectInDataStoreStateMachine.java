/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.utils.fsm.StateObject;

public interface ObjectInDataStoreStateMachine extends StateObject<ObjectInDataStoreStateMachine.State> {
    enum State {
        Allocated("The initial state"),
        Creating2("This is only used with createOnlyRequested event"),
        Creating("The object is being creating on data store"),
        Created("The object is created"),
        Ready("Template downloading is accomplished"),
        Copying("The object is being coping"),
        Migrating("The object is being migrated"),
        Destroying("Template is destroying"),
        Destroyed("Template is destroyed"),
        Failed("Failed to download template");
        String _description;

        private State(String description) {
            _description = description;
        }

        public String getDescription() {
            return _description;
        }
    }

    enum Event {
        CreateRequested,
        CreateOnlyRequested,
        DestroyRequested,
        OperationSuccessed,
        OperationFailed,
        CopyingRequested,
        MigrationRequested,
        MigrationCopyRequested,
        MigrationCopySucceeded,
        MigrationCopyFailed,
        ResizeRequested,
        ExpungeRequested
    }
}
