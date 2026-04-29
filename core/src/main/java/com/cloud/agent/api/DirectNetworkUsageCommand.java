//
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
//

package com.cloud.agent.api;

import java.util.Date;
import java.util.List;

public class DirectNetworkUsageCommand extends Command {

    private List<String> publicIps;
    private Date start;
    private Date end;
    private String includeZones;
    private String excludeZones;

    public DirectNetworkUsageCommand(List<String> publicIps, Date start, Date end, String includeZones, String excludeZones) {
        this.setPublicIps(publicIps);
        this.setStart(start);
        this.setEnd(end);
        this.setIncludeZones(includeZones);
        this.setExcludeZones(excludeZones);
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public void setPublicIps(List<String> publicIps) {
        this.publicIps = publicIps;
    }

    public List<String> getPublicIps() {
        return publicIps;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getStart() {
        return start;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Date getEnd() {
        return end;
    }

    public String getIncludeZones() {
        return includeZones;
    }

    public void setIncludeZones(String includeZones) {
        this.includeZones = includeZones;
    }

    public String getExcludeZones() {
        return excludeZones;
    }

    public void setExcludeZones(String excludeZones) {
        this.excludeZones = excludeZones;
    }

}
