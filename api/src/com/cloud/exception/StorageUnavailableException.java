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
package com.cloud.exception;

import com.cloud.storage.StoragePool;
import com.cloud.utils.SerialVersionUID;

/**
 * If the cause is due to storage pool unavailable, calling
 * problem with.
 *
 */
public class StorageUnavailableException extends ResourceUnavailableException {
    private static final long serialVersionUID = SerialVersionUID.StorageUnavailableException;

    public StorageUnavailableException(String msg, long poolId) {
        this(msg, poolId, null);
    }

    public StorageUnavailableException(String msg, long poolId, Throwable cause) {
        this(msg, StoragePool.class, poolId, cause);
    }

    public StorageUnavailableException(String msg, Class<?> scope, long resourceId) {
        this(msg, scope, resourceId, null);
    }

    public StorageUnavailableException(String msg, Class<?> scope, long resourceId, Throwable th) {
        super(msg, scope, resourceId, th);
    }
}
