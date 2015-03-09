//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.network.as;

import java.util.Date;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.InternalIdentity;

public interface AutoScaleVmGroup extends ControlledEntity, InternalIdentity, Displayable {

    String State_New = "new";
    String State_Revoke = "revoke";
    String State_Enabled = "enabled";
    String State_Disabled = "disabled";

    @Override
    long getId();

    @Override
    long getAccountId();

    Long getLoadBalancerId();

    long getProfileId();

    int getMinMembers();

    int getMaxMembers();

    int getMemberPort();

    int getInterval();

    Date getLastInterval();

    String getState();

    String getUuid();

    @Override
    boolean isDisplay();

}
