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
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import javax.annotation.Generated;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "backups",
        "count"
})
@Generated("jsonschema2pojo")
public class NetworkerBackups implements Serializable {

    private final static long serialVersionUID = -3805021350250865454L;
    @JsonProperty("backups")
    private List<NetworkerBackup> backups = null;
    @JsonProperty("count")
    private Integer count;

    /**
     * No args constructor for use in serialization
     */
    public NetworkerBackups() {
    }

    /**
     * @param count
     * @param backups
     */
    public NetworkerBackups(List<NetworkerBackup> backups, Integer count) {
        super();
        this.backups = backups;
        this.count = count;
    }

    @JsonProperty("backups")
    public List<NetworkerBackup> getBackups() {
        return backups;
    }

    @JsonProperty("backups")
    public void setBackups(List<NetworkerBackup> backups) {
        this.backups = backups;
    }

    @JsonProperty("count")
    public Integer getCount() {
        return count;
    }

    @JsonProperty("count")
    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"backups","count");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.count == null) ? 0 : this.count.hashCode()));
        result = ((result * 31) + ((this.backups == null) ? 0 : this.backups.hashCode()));
        return result;
    }
}
