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

package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "clone",
        "id",
        "status",
        "volumeIds"
})
@Generated("jsonschema2pojo")
public class Instance implements Serializable {

    private final static long serialVersionUID = -5708111855130556342L;
    @JsonProperty("clone")
    private Boolean clone;
    @JsonProperty("id")
    private String id;
    @JsonProperty("status")
    private String status;
    @JsonProperty("volumeIds")
    private List<String> volumeIds = null;

    /**
     * No args constructor for use in serialization
     */
    public Instance() {
    }

    /**
     * @param clone
     * @param volumeIds
     * @param id
     * @param status
     */
    public Instance(Boolean clone, String id, String status, List<String> volumeIds) {
        super();
        this.clone = clone;
        this.id = id;
        this.status = status;
        this.volumeIds = volumeIds;
    }

    @JsonProperty("clone")
    public Boolean getClone() {
        return clone;
    }

    @JsonProperty("clone")
    public void setClone(Boolean clone) {
        this.clone = clone;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("volumeIds")
    public List<String> getVolumeIds() {
        return volumeIds;
    }

    @JsonProperty("volumeIds")
    public void setVolumeIds(List<String> volumeIds) {
        this.volumeIds = volumeIds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Instance.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("clone");
        sb.append('=');
        sb.append(((this.clone == null) ? "<null>" : this.clone));
        sb.append(',');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null) ? "<null>" : this.id));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null) ? "<null>" : this.status));
        sb.append(',');
        sb.append("volumeIds");
        sb.append('=');
        sb.append(((this.volumeIds == null) ? "<null>" : this.volumeIds));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.clone == null) ? 0 : this.clone.hashCode()));
        result = ((result * 31) + ((this.volumeIds == null) ? 0 : this.volumeIds.hashCode()));
        result = ((result * 31) + ((this.id == null) ? 0 : this.id.hashCode()));
        result = ((result * 31) + ((this.status == null) ? 0 : this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Instance) == false) {
            return false;
        }
        Instance rhs = ((Instance) other);
        return (((((this.clone == rhs.clone) || ((this.clone != null) && this.clone.equals(rhs.clone))) && ((this.volumeIds == rhs.volumeIds) || ((this.volumeIds != null) && this.volumeIds.equals(rhs.volumeIds)))) && ((this.id == rhs.id) || ((this.id != null) && this.id.equals(rhs.id)))) && ((this.status == rhs.status) || ((this.status != null) && this.status.equals(rhs.status))));
    }

}
