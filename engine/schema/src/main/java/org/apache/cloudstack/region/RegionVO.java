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
package org.apache.cloudstack.region;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "region")
public class RegionVO implements Region {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "end_point")
    private String endPoint;

    @Column(name = "gslb_service_enabled")
    private boolean gslbEnabled;

    @Column(name = "portableip_service_enabled")
    private boolean portableipEnabled;

    public boolean getGslbEnabled() {
        return gslbEnabled;
    }

    public void setGslbEnabled(boolean gslbEnabled) {
        this.gslbEnabled = gslbEnabled;
    }

    public RegionVO() {
    }

    public RegionVO(int id, String name, String endPoint) {
        this.id = id;
        this.name = name;
        this.endPoint = endPoint;
        this.gslbEnabled = true;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public boolean checkIfServiceEnabled(Service service) {
        if (Service.Gslb.equals(service)) {
            return gslbEnabled;
        } else if (Service.PortableIp.equals(service)) {
            return portableipEnabled;
        } else {
            assert false : "Unknown Region level Service";
            return false;
        }
    }

    @Override
    public void enableService(org.apache.cloudstack.region.Region.Service service) {
        if (Service.Gslb.equals(service)) {
            this.gslbEnabled = true;
        } else if (Service.PortableIp.equals(service)) {
            this.portableipEnabled = true;
        } else {
            assert false : "Unknown Region level Service";
            return;
        }
    }

    public boolean getPortableipEnabled() {
        return portableipEnabled;
    }

    public void setPortableipEnabled(boolean portableipEnabled) {
        this.portableipEnabled = portableipEnabled;
    }
}
