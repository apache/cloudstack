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

package org.apache.cloudstack.vm.bootgroup;

import com.cloud.utils.component.Manager;

/**
 * Backend/orchestration counterpart to {@link InstanceBootGroupService} (the API-facing contract,
 * implemented by {@code InstanceBootGroupApiServiceImpl}). Deliberately takes domain objects, never
 * API {@code Cmd} types, so it has no API-layer dependency and can be invoked outside a request
 * context (e.g. a background job) without fabricating a CallContext.
 */
public interface InstanceBootGroupManager extends Manager {

    void startInstanceBootGroup(InstanceBootGroupVO group);

    void stopInstanceBootGroup(InstanceBootGroupVO group);

    void rebootInstanceBootGroup(InstanceBootGroupVO group);
}
