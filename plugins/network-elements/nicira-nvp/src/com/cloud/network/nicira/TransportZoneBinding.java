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
package com.cloud.network.nicira;

public class TransportZoneBinding {
    private String zone_uuid;
    private String transport_type;
    
    public TransportZoneBinding() {}
    
    public TransportZoneBinding(String zone_uuid, String transport_type) {
        this.zone_uuid = zone_uuid;
        this.transport_type = transport_type;
    }

    public String getZone_uuid() {
        return zone_uuid;
    }

    public void setZone_uuid(String zone_uuid) {
        this.zone_uuid = zone_uuid;
    }

    public String getTransport_type() {
        return transport_type;
    }

    public void setTransport_type(String transport_type) {
        this.transport_type = transport_type;
    }
    
}
