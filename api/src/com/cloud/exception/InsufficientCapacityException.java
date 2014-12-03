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

import com.cloud.utils.SerialVersionUID;

/**
 * Generic parent exception class for capacity being reached.
 *
 */
public abstract class InsufficientCapacityException extends CloudException {
    private static final long serialVersionUID = SerialVersionUID.InsufficientCapacityException;

    Long id;
    Class<?> scope;

    protected InsufficientCapacityException() {
        super();
    }

    public InsufficientCapacityException(String msg, Class<?> scope, Long id) {
        super(msg);
        this.scope = scope;
        this.id = id;
    }

    /**
     * @return scope where we are insufficient.  The possible classes are
     *         Host, StoragePool, Cluster, Pod, DataCenter, NetworkConfiguration.
     */
    public Class<?> getScope() {
        return scope;
    }

    /**
     * @return the id of the object that it is insufficient in.  Note that this method is
     *         marked such that if the id is not set, then it will throw NullPointerException.
     *         This is intended as you should check to see if the Scope is present before
     *         accessing this method.
     */
    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        String str = super.toString();
        return str + "Scope=" + scope + "; id=" + id;
    }
}
