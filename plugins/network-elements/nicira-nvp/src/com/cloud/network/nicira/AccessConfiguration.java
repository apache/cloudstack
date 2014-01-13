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

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@SuppressWarnings("serial")
public abstract class AccessConfiguration<T extends AccessRule> implements Serializable {

    protected String displayName;
    protected List<T> logicalPortEgressRules;
    protected List<T> logicalPortIngressRules;
    protected List<NiciraNvpTag> tags;
    protected String uuid;
    protected String href;
    protected String schema;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public List<T> getLogicalPortEgressRules() {
        return logicalPortEgressRules;
    }

    public void setLogicalPortEgressRules(final List<T> logicalPortEgressRules) {
        this.logicalPortEgressRules = logicalPortEgressRules;
    }

    public List<T> getLogicalPortIngressRules() {
        return logicalPortIngressRules;
    }

    public void setLogicalPortIngressRules(final List<T> logicalPortIngressRules) {
        this.logicalPortIngressRules = logicalPortIngressRules;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getHref() {
        return href;
    }

    public void setHref(final String href) {
        this.href = href;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public List<NiciraNvpTag> getTags() {
        return tags;
    }

    public void setTags(final List<NiciraNvpTag> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE, false);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
            .append(displayName).append(logicalPortEgressRules)
            .append(logicalPortIngressRules).append(tags)
            .append(uuid).append(href).append(schema)
            .toHashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(this.getClass().isInstance(obj))) {
            return false;
        }
        final AccessConfiguration<? extends AccessRule> another =
                (AccessConfiguration<? extends AccessRule>) obj;
        return new EqualsBuilder()
                .append(displayName, another.displayName)
                .append(uuid, another.uuid)
                .append(href, another.href)
                .append(schema, another.schema)
                .isEquals();
    }
}
