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
package com.cloud.simulator;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "mockconfiguration")
public class MockConfigurationVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "data_center_id", nullable = false)
    private Long dataCenterId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "name")
    private String name;

    @Column(name = "values")
    private String values;

    @Column(name="count")
    private Integer count;

    @Column(name="json_response", length=4096)
    private String jsonResponse;

    @Column(name="removed")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date removed;

    @Override
    public long getId() {
        return this.id;
    }

    public Long getDataCenterId() {
        return this.dataCenterId;
    }

    public void setDataCenterId(Long dcId) {
        this.dataCenterId = dcId;
    }

    public Long getPodId() {
        return this.podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    public Long getClusterId() {
        return this.clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getHostId() {
        return this.hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValues() {
        return this.values;
    }

    public Map<String, String> getParameters() {
        Map<String, String> maps = new HashMap<String, String>();
        if (this.values == null) {
            return maps;
        }

        String[] vals = this.values.split("\\|");
        for (String val : vals) {
            String[] paras = val.split(":");
            maps.put(paras[0], paras[1]);
        }
        return maps;
    }

    public void setValues(String values) {
        this.values = values;
    }

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getJsonResponse() {
        return this.jsonResponse;
    }

    public void setJsonResponse(String jsonResponse) {
        this.jsonResponse = jsonResponse;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
