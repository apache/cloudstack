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

import java.util.Map;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.host.Host;

public interface DataStoreDriver {
    Map<String, String> getCapabilities();

    DataTO getTO(DataObject data);

    DataStoreTO getStoreTO(DataStore store);

    void createAsync(DataStore store, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback);

    void deleteAsync(DataStore store, DataObject data, AsyncCompletionCallback<CommandResult> callback);

    void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback);

    void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback);

    boolean canCopy(DataObject srcData, DataObject destData);

    void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback);
}
