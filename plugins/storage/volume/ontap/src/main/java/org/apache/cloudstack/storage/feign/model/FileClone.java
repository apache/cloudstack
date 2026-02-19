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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileClone {
    @JsonProperty("source_path")
    private String sourcePath;
    @JsonProperty("destination_path")
    private String destinationPath;
    @JsonProperty("volume")
    private VolumeConcise volume;
    public VolumeConcise getVolume() {
        return volume;
    }
    public void setVolume(VolumeConcise volume) {
        this.volume = volume;
    }
    public String getSourcePath() {
        return sourcePath;
    }
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
    public String getDestinationPath() {
        return destinationPath;
    }
    public void setDestinationPath(String destinationPath) {}
}
