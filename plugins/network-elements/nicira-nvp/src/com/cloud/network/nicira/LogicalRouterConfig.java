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

/**
 * 
 */
public class LogicalRouterConfig {
	private String display_name;
	private RoutingConfig routing_config;
	private String type = "LogicalRouterConfig";
	private String uuid;
    private List<NiciraNvpTag> tags;
	
	public RoutingConfig getRoutingConfig() {
		return routing_config;
	}
	
	public void setRoutingConfig(RoutingConfig routing_config) {
		this.routing_config = routing_config;
	}
	
	public String getDisplayName() {
		return display_name;
	}
	
	public void setDisplayName(String display_name) {
		this.display_name = display_name;
	}
	
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public List<NiciraNvpTag> getTags() {
        return tags;
    }

    public void setTags(List<NiciraNvpTag> tags) {
        this.tags = tags;
    }
	
	
}
