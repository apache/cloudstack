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
package org.apache.cloudstack.storage.datastore.adapter;

public interface ProviderVolume {

    public Boolean isDestroyed();
    public String getId();
    public void setId(String id);
    public String getName();
    public void setName(String name);
    public Integer getPriority();
    public void setPriority(Integer priority);
    public String getState();
    public AddressType getAddressType();
    public void setAddressType(AddressType addressType);
    public String getAddress();
    public Long getAllocatedSizeInBytes();
    public Long getUsedBytes();
    public String getExternalUuid();
    public String getExternalName();
    public String getExternalConnectionId();
    public enum AddressType {
        FIBERWWN
    }
}
