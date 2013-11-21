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
package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;

public interface DataObjectManager {
    public void createAsync(DataObject data, DataStore store, AsyncCompletionCallback<CreateCmdResult> callback, boolean noCopy);

    /*
     * Only create internal state, without actually send down create command.
     * It's up to device driver decides whether to create object before copying
     */
    public DataObject createInternalStateOnly(DataObject data, DataStore store);

    public void update(DataObject data, String path, Long size);

    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CreateCmdResult> callback);

    public void deleteAsync(DataObject data, AsyncCompletionCallback<CommandResult> callback);
}
