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
package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2ImageLaunchPermission {

    private Operation launchPermOperation;
    public enum Operation{
        add,
        remove,
        reset;
    }

    private List<String>  launchPermissionList = new ArrayList<String>();

    public EC2ImageLaunchPermission() {
        launchPermOperation = null;
    }

    public void addLaunchPermission(String launchPermission) {
        launchPermissionList.add(launchPermission);
    }

    public List<String> getLaunchPermissionList() {
        return launchPermissionList;
    }

    public void setLaunchPermOp( Operation launchPermOperation ) {
        this.launchPermOperation = launchPermOperation;
    }

    public Operation getLaunchPermOp() {
        return this.launchPermOperation;
    }

}
