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
package com.cloud.kubernetes.cluster;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "kubernetes_cluster_node_group")
public class KubernetesClusterNodeGroupVO implements KubernetesClusterNodeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private boolean defaultGroup;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "service_offering_id")
    private long serviceOfferingId;

    @Column(name = "template_id")
    private long templateId;

    @Column(name = "node_count")
    private long nodeCount;

    @Column(name = "labels", length = 4096)
    private String labels;

    @Column(name = "taints", length = 4096)
    private String taints;

    @Column(name = "autoscaling_enabled")
    private boolean autoscalingEnabled;

    @Column(name = "min_size")
    private Long minSize;

    @Column(name = "max_size")
    private Long maxSize;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public KubernetesClusterNodeGroupVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public KubernetesClusterNodeGroupVO(long clusterId, String name, long serviceOfferingId, long templateId,
                                        String labels, String taints) {
        this();
        this.clusterId = clusterId;
        this.name = name;
        this.serviceOfferingId = serviceOfferingId;
        this.templateId = templateId;
        this.labels = labels;
        this.taints = taints;
        this.nodeCount = 0;
        this.state = State.Ready;
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
    public long getClusterId() {
        return clusterId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isDefault() {
        return defaultGroup;
    }

    /**
     * Same as {@link #isDefault()}, named after the field for the search builders
     */
    public boolean isDefaultGroup() {
        return defaultGroup;
    }

    public void setDefault(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    @Override
    public long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }

    @Override
    public long getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(long nodeCount) {
        this.nodeCount = nodeCount;
    }

    @Override
    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    @Override
    public String getTaints() {
        return taints;
    }

    public void setTaints(String taints) {
        this.taints = taints;
    }

    @Override
    public boolean isAutoscalingEnabled() {
        return autoscalingEnabled;
    }

    public void setAutoscalingEnabled(boolean autoscalingEnabled) {
        this.autoscalingEnabled = autoscalingEnabled;
    }

    @Override
    public Long getMinSize() {
        return minSize;
    }

    public void setMinSize(Long minSize) {
        this.minSize = minSize;
    }

    @Override
    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return String.format("KubernetesClusterNodeGroup %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "name", "clusterId", "defaultGroup", "state"));
    }
}
