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

import com.cloud.org.Cluster;
import com.cloud.utils.SerialVersionUID;

/**
 * virtual machine.  The type gives the type of virtual machine we are
 * trying to start.
 */
public class InsufficientServerCapacityException extends InsufficientCapacityException {

    private static final long serialVersionUID = SerialVersionUID.InsufficientServerCapacityException;

    private boolean affinityGroupsApplied = false;

    public InsufficientServerCapacityException(String msg, Long clusterId) {
        this(msg, Cluster.class, clusterId);
    }

    public InsufficientServerCapacityException(String msg, Class<?> scope, Long id) {
        super(msg, scope, id);
    }

    public InsufficientServerCapacityException(String msg, Class<?> scope, Long id, boolean affinityGroupsApplied) {
        super(msg, scope, id);
        this.affinityGroupsApplied = affinityGroupsApplied;
    }

    public boolean isAffinityApplied() {
        return affinityGroupsApplied;
    }
}
