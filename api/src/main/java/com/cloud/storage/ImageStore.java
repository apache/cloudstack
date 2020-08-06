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
package com.cloud.storage;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface ImageStore extends Identity, InternalIdentity {

    String ACS_PROPERTY_PREFIX = "ACS-property-";
    String REQUIRED_NETWORK_PREFIX = "ACS-network-";
    String DISK_DEFINITION_PREFIX = "ACS-disk-";
    String OVF_HARDWARE_CONFIGURATION_PREFIX = "ACS-configuration-";
    String OVF_HARDWARE_ITEM_PREFIX = "ACS-hardware-item-";
    String OVF_EULA_SECTION_PREFIX = "ACS-eula-";

    /**
     * @return name of the object store.
     */
    String getName();

    /**
     * @return availability zone.
     */
    Long getDataCenterId();

    /**
     * @return object store provider name
     */
    String getProviderName();

    /**
     *
     * @return data store protocol
     */
    String getProtocol();

    /**
     *
     * @return uri
     */
    String getUrl();

}
