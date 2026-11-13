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
package com.cloud.host;

/**
 * A smoothed view of what each host is actually doing, as opposed to what has been allocated on it.
 *
 * Allocation figures say what was promised; under overprovisioning they can be far from what a host
 * is really carrying. Placement and rebalancing both need the second view.
 */
public interface HostLoadService {

    /**
     * @return the host's smoothed load, or a value reporting itself unusable when the host has not
     *         been sampled recently enough to rank on
     */
    HostLoad getLoad(long hostId);
}
