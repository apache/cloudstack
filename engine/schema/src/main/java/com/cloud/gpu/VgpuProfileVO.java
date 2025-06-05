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

package com.cloud.gpu;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.gpu.VgpuProfile;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "vgpu_profile")
public class VgpuProfileVO implements VgpuProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "max_vgpu_per_pgpu")
    private Long maxVgpuPerPgpu;

    @Column(name = "video_ram")
    private Long videoRam;

    @Column(name = "max_heads")
    private Long maxHeads;

    @Column(name = "max_resolution_x")
    private Long maxResolutionX;

    @Column(name = "max_resolution_y")
    private Long maxResolutionY;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public VgpuProfileVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public VgpuProfileVO(String name, String description, Long gpuCardId, Long maxVgpuPerPgpu) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.cardId = gpuCardId;
        this.maxVgpuPerPgpu = maxVgpuPerPgpu;
        this.created = new Date();
    }


    public VgpuProfileVO(String name, String description, Long gpuCardId, Long maxVgpuPerPgpu, Long videoRam, Long maxHeads, Long maxResolutionX, Long maxResolutionY) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.cardId = gpuCardId;
        this.maxVgpuPerPgpu = maxVgpuPerPgpu;
        this.videoRam = videoRam;
        this.maxHeads = maxHeads;
        this.maxResolutionX = maxResolutionX;
        this.maxResolutionY = maxResolutionY;
        this.created = new Date();
    }

    @Override
    public String toString() {
        return String.format("VgpuProfile %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "name", "cardId"));
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    @Override
    public Long getMaxVgpuPerPgpu() {
        return maxVgpuPerPgpu;
    }

    public void setMaxVgpuPerPgpu(Long maxVgpuPerPgpu) {
        this.maxVgpuPerPgpu = maxVgpuPerPgpu;
    }

    @Override
    public Long getVideoRam() {
        return videoRam;
    }

    public void setVideoRam(Long videoRam) {
        this.videoRam = videoRam;
    }

    @Override
    public Long getMaxHeads() {
        return maxHeads;
    }

    public void setMaxHeads(Long maxHeads) {
        this.maxHeads = maxHeads;
    }

    @Override
    public Long getMaxResolutionX() {
        return maxResolutionX;
    }

    public void setMaxResolutionX(Long maxResolutionX) {
        this.maxResolutionX = maxResolutionX;
    }

    @Override
    public Long getMaxResolutionY() {
        return maxResolutionY;
    }

    public void setMaxResolutionY(Long maxResolutionY) {
        this.maxResolutionY = maxResolutionY;
    }
}
