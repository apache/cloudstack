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
package com.cloud.bridge.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "meta")
public class SMetaVO implements Serializable {
    private static final long serialVersionUID = 7459503272337054283L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "Target")
    private String target;

    @Column(name = "TargetID")
    private long targetId;

    @Column(name = "Name")
    private String name;

    @Column(name = "Value")
    private String value;

    public SMetaVO() {
    }

    public Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof SMetaVO))
            return false;

        return getTarget().equals(((SMetaVO)other).getTarget()) && getTargetId() == ((SMetaVO)other).getTargetId() && getName().equals(((SMetaVO)other).getName());
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = hashCode * 17 + getTarget().hashCode();
        hashCode = hashCode * 17 + (int)getTargetId();
        hashCode = hashCode * 17 + getName().hashCode();
        return hashCode;
    }
}
