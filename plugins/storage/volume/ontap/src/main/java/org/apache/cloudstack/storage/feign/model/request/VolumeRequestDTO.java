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
package org.apache.cloudstack.storage.feign.model.request;

import java.util.List;

public class VolumeRequestDTO {
    private String name;
    private List<AggregateDTO> aggregates;
    private SvmDTO svm;

    // getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<AggregateDTO> getAggregates() { return aggregates; }
    public void setAggregates(List<AggregateDTO> aggregates) { this.aggregates = aggregates; }
    public SvmDTO getSvm() { return svm; }
    public void setSvm(SvmDTO svm) { this.svm = svm; }

    public static class AggregateDTO {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class SvmDTO {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
