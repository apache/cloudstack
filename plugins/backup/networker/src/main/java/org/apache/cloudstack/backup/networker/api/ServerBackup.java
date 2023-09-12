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
        "performBootstrap",
        "performClientFileIndexing",
        "destinationStorageNode",
        "retentionPeriod",
        "destinationPool"
})
@Generated("jsonschema2pojo")
public class ServerBackup implements Serializable {

    private final static long serialVersionUID = -542556595701706880L;
    @JsonProperty("performBootstrap")
    private Boolean performBootstrap;
    @JsonProperty("performClientFileIndexing")
    private Boolean performClientFileIndexing;
    @JsonProperty("destinationStorageNode")
    private String destinationStorageNode;
    @JsonProperty("retentionPeriod")
    private String retentionPeriod;
    @JsonProperty("destinationPool")
    private String destinationPool;

    /**
     * No args constructor for use in serialization
     */
    public ServerBackup() {
    }

    /**
     * @param destinationPool
     * @param performBootstrap
     * @param performClientFileIndexing
     * @param destinationStorageNode
     * @param retentionPeriod
     */
    public ServerBackup(Boolean performBootstrap, Boolean performClientFileIndexing, String destinationStorageNode, String retentionPeriod, String destinationPool) {
        super();
        this.performBootstrap = performBootstrap;
        this.performClientFileIndexing = performClientFileIndexing;
        this.destinationStorageNode = destinationStorageNode;
        this.retentionPeriod = retentionPeriod;
        this.destinationPool = destinationPool;
    }

    @JsonProperty("performBootstrap")
    public Boolean getPerformBootstrap() {
        return performBootstrap;
    }

    @JsonProperty("performBootstrap")
    public void setPerformBootstrap(Boolean performBootstrap) {
        this.performBootstrap = performBootstrap;
    }

    @JsonProperty("performClientFileIndexing")
    public Boolean getPerformClientFileIndexing() {
        return performClientFileIndexing;
    }

    @JsonProperty("performClientFileIndexing")
    public void setPerformClientFileIndexing(Boolean performClientFileIndexing) {
        this.performClientFileIndexing = performClientFileIndexing;
    }

    @JsonProperty("destinationStorageNode")
    public String getDestinationStorageNode() {
        return destinationStorageNode;
    }

    @JsonProperty("destinationStorageNode")
    public void setDestinationStorageNode(String destinationStorageNode) {
        this.destinationStorageNode = destinationStorageNode;
    }

    @JsonProperty("retentionPeriod")
    public String getRetentionPeriod() {
        return retentionPeriod;
    }

    @JsonProperty("retentionPeriod")
    public void setRetentionPeriod(String retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    @JsonProperty("destinationPool")
    public String getDestinationPool() {
        return destinationPool;
    }

    @JsonProperty("destinationPool")
    public void setDestinationPool(String destinationPool) {
        this.destinationPool = destinationPool;
    }

    @Override
    public String toString() {
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"performBootstrap","performClientFileIndexing","destinationStorageNode",
                "retentionPeriod","destinationPool");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.performBootstrap == null) ? 0 : this.performBootstrap.hashCode()));
        result = ((result * 31) + ((this.performClientFileIndexing == null) ? 0 : this.performClientFileIndexing.hashCode()));
        result = ((result * 31) + ((this.destinationPool == null) ? 0 : this.destinationPool.hashCode()));
        result = ((result * 31) + ((this.destinationStorageNode == null) ? 0 : this.destinationStorageNode.hashCode()));
        result = ((result * 31) + ((this.retentionPeriod == null) ? 0 : this.retentionPeriod.hashCode()));
        return result;
    }
}
