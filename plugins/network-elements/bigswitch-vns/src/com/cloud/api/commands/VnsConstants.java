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

package com.cloud.api.commands;


import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;

import com.cloud.network.Network.Provider;

public class VnsConstants {
    public static final String BIGSWITCH_VNS_DEVICE_ID = "vnsdeviceid";
    public static final String BIGSWITCH_VNS_DEVICE_NAME = "bigswitchdevicename";

    public static final String EVENT_EXTERNAL_VNS_CONTROLLER_ADD = "PHYSICAL.VNSCONTROLLER.ADD";
    public static final String EVENT_EXTERNAL_VNS_CONTROLLER_DELETE = "PHYSICAL.VNSCONTROLLER.DELETE";

    public static final Provider BigSwitchVns = new Provider("BigSwitchVns", true);

    public static final NetworkDevice BigSwitchVnsDevice = new NetworkDevice("BigSwitchVns", BigSwitchVns.getName());

}
