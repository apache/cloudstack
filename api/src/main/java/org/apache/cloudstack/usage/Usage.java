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
package org.apache.cloudstack.usage;

import java.util.Date;

public interface Usage {

    public long getId();

    public Long getZoneId();

    public Long getAccountId();

    public Long getDomainId();

    public String getDescription();

    public String getUsageDisplay();

    public int getUsageType();

    public Double getRawUsage();

    public Long getVmInstanceId();

    public String getVmName();

    public Long getCpuCores();

    public Long getCpuSpeed();

    public Long getMemory();

    public Long getOfferingId();

    public Long getTemplateId();

    public Long getUsageId();

    public String getType();

    public Long getNetworkId();

    public Long getSize();

    public Date getStartDate();

    public Date getEndDate();

    public Long getVirtualSize();
}
