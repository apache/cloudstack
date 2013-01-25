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

public class LogicalSwitchPort {
    private String display_name;
    private List<NiciraNvpTag> tags;
    private Integer portno;
    private boolean admin_status_enabled;
    //private List<AddressPairs> allowed_address_pairs;
    private String queue_uuid;
    private List<String> security_profiles;
    private List<String> mirror_targets;
    private String type;
    private String uuid;
    
    public LogicalSwitchPort() {
        super();
    }

    public LogicalSwitchPort(String display_name, List<NiciraNvpTag> tags,
            boolean admin_status_enabled) {
        super();
        this.display_name = display_name;
        this.tags = tags;
        this.admin_status_enabled = admin_status_enabled;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public List<NiciraNvpTag> getTags() {
        return tags;
    }

    public void setTags(List<NiciraNvpTag> tags) {
        this.tags = tags;
    }

    public Integer getPortno() {
        return portno;
    }

    public void setPortno(Integer portno) {
        this.portno = portno;
    }

    public boolean isAdmin_status_enabled() {
        return admin_status_enabled;
    }

    public void setAdmin_status_enabled(boolean admin_status_enabled) {
        this.admin_status_enabled = admin_status_enabled;
    }

    public String getQueue_uuid() {
        return queue_uuid;
    }

    public void setQueue_uuid(String queue_uuid) {
        this.queue_uuid = queue_uuid;
    }

    public List<String> getSecurity_profiles() {
        return security_profiles;
    }

    public void setSecurity_profiles(List<String> security_profiles) {
        this.security_profiles = security_profiles;
    }

    public List<String> getMirror_targets() {
        return mirror_targets;
    }

    public void setMirror_targets(List<String> mirror_targets) {
        this.mirror_targets = mirror_targets;
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
    
}
