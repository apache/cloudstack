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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

/**
 * GPU card interface representing a physical GPU card model
 */
public interface GpuCard extends InternalIdentity, Identity {
    /**
     * @return the UUID of the GPU card
     */
    String getUuid();

    /**
     * @return the device ID of the GPU card
     */
    String getDeviceId();

    /**
     * @return the device name of the GPU card
     */
    String getDeviceName();

    /**
     * @return the name of the GPU card
     */
    String getName();

    /**
     * @return the vendor name of the GPU card
     */
    String getVendorName();

    /**
     * @return the vendor ID of the GPU card
     */
    String getVendorId();

    /**
     * @return the date when the GPU card was created
     */
    Date getCreated();


    /**
     * @return the group name of the GPU card based on how the XenServer expects it.
     */
    String getGroupName();

}
