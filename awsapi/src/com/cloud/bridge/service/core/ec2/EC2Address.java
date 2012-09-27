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

public class EC2Address {

    private String ipAddress = null;
    private String associatedInstanceId = null;

    public EC2Address() { }

    public void setIpAddress( String ipAddress ) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setAssociatedInstanceId( String associatedInstanceId ) {
        this.associatedInstanceId = associatedInstanceId;
    }

    public String getAssociatedInstanceId() {
        return this.associatedInstanceId;
    }
}
