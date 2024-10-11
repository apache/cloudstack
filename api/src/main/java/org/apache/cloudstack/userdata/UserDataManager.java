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
package org.apache.cloudstack.userdata;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.utils.component.Manager;

public interface UserDataManager extends Manager, Configurable {
    String VM_USERDATA_MAX_LENGTH_STRING = "vm.userdata.max.length";
    ConfigKey<Integer> VM_USERDATA_MAX_LENGTH = new ConfigKey<>("Advanced", Integer.class, VM_USERDATA_MAX_LENGTH_STRING, "32768",
            "Max length of vm userdata after base64 encoding. Default is 32768 and maximum is 1048576", true);

    String concatenateUserData(String userdata1, String userdata2, String userdataProvider);
    String validateUserData(String userData, BaseCmd.HTTPMethod httpmethod);
}
