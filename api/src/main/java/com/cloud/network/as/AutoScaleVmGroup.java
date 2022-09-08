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
import org.apache.commons.lang3.StringUtils;

public interface AutoScaleVmGroup extends ControlledEntity, InternalIdentity, Displayable {

    enum State {
        NEW, REVOKE, ENABLED, DISABLED, SCALING;

        public static State fromValue(String state) {
            if (StringUtils.isBlank(state)) {
                return null;
            } else if (state.equalsIgnoreCase("new")) {
                return NEW;
            } else if (state.equalsIgnoreCase("revoke")) {
                return REVOKE;
            } else if (state.equalsIgnoreCase("enabled")) {
                return ENABLED;
            } else if (state.equalsIgnoreCase("disabled")) {
                return DISABLED;
            } else if (state.equalsIgnoreCase("scaling")) {
                return SCALING;
            } else {
                throw new IllegalArgumentException("Unexpected AutoScale VM group state : " + state);
            }
        }
    }

    @Override
    long getId();

    @Override
    long getAccountId();

    Long getLoadBalancerId();

    long getProfileId();

    String getName();

    int getMinMembers();

    int getMaxMembers();

    int getMemberPort();

    int getInterval();

    Date getLastInterval();

    State getState();

    String getUuid();

    @Override
    boolean isDisplay();

    Date getCreated();
}
