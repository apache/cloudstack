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
package org.apache.cloudstack.storage.datastore.adapter.primera;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraHostset {

    private String comment;
    private Integer id;
    private String name;
    private List<String> setmembers = new ArrayList<String>();
    private String uuid;
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();



    public String getComment() {
        return comment;
    }



    public void setComment(String comment) {
        this.comment = comment;
    }



    public Integer getId() {
        return id;
    }



    public void setId(Integer id) {
        this.id = id;
    }



    public String getName() {
        return name;
    }



    public void setName(String name) {
        this.name = name;
    }



    public List<String> getSetmembers() {
        return setmembers;
    }



    public void setSetmembers(List<String> setmembers) {
        this.setmembers = setmembers;
    }



    public String getUuid() {
        return uuid;
    }



    public void setUuid(String uuid) {
        this.uuid = uuid;
    }



    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }



    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }



    // adds members to a hostset
    public static class PrimeraHostsetVLUNRequest {
        private String volumeName;
        private Boolean autoLun = true;
        private Integer lun = 0;
        private Integer maxAutoLun = 0;
        // hostset format: "set:<hostset>"
        private String hostname;
        public String getVolumeName() {
            return volumeName;
        }
        public void setVolumeName(String volumeName) {
            this.volumeName = volumeName;
        }
        public Boolean getAutoLun() {
            return autoLun;
        }
        public void setAutoLun(Boolean autoLun) {
            this.autoLun = autoLun;
        }
        public Integer getLun() {
            return lun;
        }
        public void setLun(Integer lun) {
            this.lun = lun;
        }
        public Integer getMaxAutoLun() {
            return maxAutoLun;
        }
        public void setMaxAutoLun(Integer maxAutoLun) {
            this.maxAutoLun = maxAutoLun;
        }
        public String getHostname() {
            return hostname;
        }
        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

    }
}
