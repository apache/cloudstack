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
package org.apache.cloudstack.gpu;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * GPU offering interface representing a collection of vGPU profiles and GPU cards
 */
public interface GpuOffering extends InternalIdentity, Identity {

    enum State {
        Inactive, Active,
    }

    /**
     * @return the name of the GPU offering
     */
    String getName();

    /**
     * @return the description of the GPU offering
     */
    String getDescription();

    State getState();

    /**
     * @return list of vGPU profiles included in this offering
     */
    List<VgpuProfile> getVgpuProfiles();

    /**
     * @return the date when the GPU offering was created
     */
    Date getCreated();
}
