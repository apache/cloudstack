/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * An iSCSI service defines the properties of the iSCSI target for an SVM.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IscsiService {
    @JsonProperty("enabled")
    private Boolean enabled = null;

    @JsonProperty("svm")
    private Svm svm = null;

    @JsonProperty("target")
    private IscsiServiceTarget target = null;

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Svm getSvm() {
        return svm;
    }

    public void setSvm(Svm svm) {
        this.svm = svm;
    }

    public IscsiServiceTarget getTarget() {
        return target;
    }

    public void setTarget(IscsiServiceTarget target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "IscsiService{" +
                "enabled=" + enabled +
                ", svm=" + svm +
                ", target=" + target +
                '}';
    }

    /**
     * iSCSI target information
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IscsiServiceTarget {
        @JsonProperty("alias")
        private String alias = null;

        @JsonProperty("name")
        private String name = null;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "IscsiServiceTarget{" +
                    "alias='" + alias + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
