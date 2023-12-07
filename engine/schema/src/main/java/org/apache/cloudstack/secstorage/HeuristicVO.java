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
package org.apache.cloudstack.secstorage;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.secstorage.heuristics.Heuristic;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "heuristics")
public class HeuristicVO implements Heuristic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "heuristic_rule", nullable = false, length = 65535)
    private String heuristicRule;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    public HeuristicVO() {
    }

    public HeuristicVO(String name, String description, Long zoneId, String type, String heuristicRule) {
        this.name = name;
        this.description = description;
        this.zoneId = zoneId;
        this.type = type;
        this.heuristicRule = heuristicRule;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getType() {
        return type;
    }

    public String getHeuristicRule() {
        return heuristicRule;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setHeuristicRule(String heuristicRule) {
        this.heuristicRule = heuristicRule;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "name", "heuristicRule", "type");
    }
}
