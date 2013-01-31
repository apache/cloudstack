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
package org.apache.cloudstack.storage.test;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.motion.DataMotionStrategy;

public class MockStorageMotionStrategy implements DataMotionStrategy {

    @Override
    public boolean canHandle(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Void copyAsync(DataObject srcData, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCommandResult result = new CopyCommandResult("something");
        callback.complete(result);
        return null;
    }

}
