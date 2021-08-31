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
package com.cloud.network;

import java.util.Date;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Displayable;
import org.apache.cloudstack.api.InternalIdentity;

public interface Site2SiteVpnConnection extends ControlledEntity, InternalIdentity, Displayable {
    enum State {
        Pending, Connecting, Connected, Disconnected, Error,
    }

    @Override
    public long getId();

    public String getUuid();

    public long getVpnGatewayId();

    public long getCustomerGatewayId();

    public State getState();

    public Date getCreated();

    public Date getRemoved();

    public boolean isPassive();

    @Override
    boolean isDisplay();
}
