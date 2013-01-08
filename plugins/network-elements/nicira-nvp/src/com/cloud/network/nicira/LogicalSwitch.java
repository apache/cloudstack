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

import java.util.List;

public class LogicalSwitch {
    private String display_name;
    private boolean port_isolation_enabled;
    private List<NiciraNvpTag> tags;
    private List<TransportZoneBinding> transport_zones;
    private String type;
    private String uuid;
    private String _href;
    //private RequestQueryParameters _query;
    //private LogicalSwitchRelations _relations;
    private String _schema;
    
    public String getDisplay_name() {
        return display_name;
    }
    
    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }
    
    public boolean isPort_isolation_enabled() {
        return port_isolation_enabled;
    }
    
    public void setPort_isolation_enabled(boolean port_isolation_enabled) {
        this.port_isolation_enabled = port_isolation_enabled;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String get_href() {
        return _href;
    }
    
    public void set_href(String _href) {
        this._href = _href;
    }
    
    public String get_schema() {
        return _schema;
    }
    
    public void set_schema(String _schema) {
        this._schema = _schema;
    }

    public List<NiciraNvpTag> getTags() {
        return tags;
    }

    public void setTags(List<NiciraNvpTag> tags) {
        this.tags = tags;
    }

    public List<TransportZoneBinding> getTransport_zones() {
        return transport_zones;
    }

    public void setTransport_zones(List<TransportZoneBinding> transport_zones) {
        this.transport_zones = transport_zones;
    }
    
    
}
