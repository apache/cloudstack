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
package com.cloud.netapp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "netapp_lun")
public class LunVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "lun_name")
    private String lunName;

    @Column(name = "target_iqn")
    private String targetIqn;

    @Column(name = "path")
    private String path;

    @Column(name = "volume_id")
    private Long volumeId;

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Column(name = "size")
    private Long size;

    public LunVO() {

    }

    public LunVO(String path, Long volumeId, Long size, String lunName, String targetIqn) {
        this.path = path;
        this.volumeId = volumeId;
        this.size = size;
        this.lunName = lunName;
        this.targetIqn = targetIqn;
    }

    public String getLunName() {
        return lunName;
    }

    public void setLunName(String lunName) {
        this.lunName = lunName;
    }

    public LunVO(Long id, String path, Long volumeId, Long size, String lunName, String targetIqn) {
        this.id = id;
        this.path = path;
        this.volumeId = volumeId;
        this.size = size;
        this.lunName = lunName;
        this.targetIqn = targetIqn;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public void setTargetIqn(String iqn) {
        this.targetIqn = iqn;
    }

    public String getTargetIqn() {
        return targetIqn;
    }

}
