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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "backup",
        "serverBackup",
        "expire"
})
@Generated("jsonschema2pojo")
public class ActionSpecificData implements Serializable {

    private final static long serialVersionUID = 2969226417055065194L;
    @JsonProperty("backup")
    private NetworkerBackup backup;
    @JsonProperty("serverBackup")
    private ServerBackup serverBackup;
    @JsonProperty("expire")
    private Expire expire;

    /**
     * No args constructor for use in serialization
     */
    public ActionSpecificData() {
    }

    /**
     * @param backup
     * @param expire
     * @param serverBackup
     */
    public ActionSpecificData(NetworkerBackup backup, ServerBackup serverBackup, Expire expire) {
        super();
        this.backup = backup;
        this.serverBackup = serverBackup;
        this.expire = expire;
    }

    @JsonProperty("backup")
    public NetworkerBackup getBackup() {
        return backup;
    }

    @JsonProperty("backup")
    public void setBackup(NetworkerBackup backup) {
        this.backup = backup;
    }

    @JsonProperty("serverBackup")
    public ServerBackup getServerBackup() {
        return serverBackup;
    }

    @JsonProperty("serverBackup")
    public void setServerBackup(ServerBackup serverBackup) {
        this.serverBackup = serverBackup;
    }

    @JsonProperty("expire")
    public Expire getExpire() {
        return expire;
    }

    @JsonProperty("expire")
    public void setExpire(Expire expire) {
        this.expire = expire;
    }

    @Override
    public String toString() {
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"backup","serverBackup","expire");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.backup == null) ? 0 : this.backup.hashCode()));
        result = ((result * 31) + ((this.serverBackup == null) ? 0 : this.serverBackup.hashCode()));
        result = ((result * 31) + ((this.expire == null) ? 0 : this.expire.hashCode()));
        return result;
    }
}
