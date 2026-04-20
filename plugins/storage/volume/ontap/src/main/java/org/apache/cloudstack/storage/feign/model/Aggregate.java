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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Aggregate {
    // Replace previous enum with case-insensitive mapping
    public enum StateEnum {
        ONLINE("online");
        private final String value;

        StateEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StateEnum fromValue(String text) {
            for (StateEnum b : StateEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("name")
    private String name = null;

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getUuid());
    }

    @JsonProperty("uuid")
    private String uuid = null;

    @JsonProperty("state")
    private StateEnum state = null;

    @JsonProperty("space")
    private AggregateSpace space = null;


    public Aggregate name(String name) {
        this.name = name;
        return this;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Aggregate uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public StateEnum getState() {
        return state;
    }

    public AggregateSpace getSpace() {
        return space;
    }

    public Double getAvailableBlockStorageSpace() {
        if (space != null && space.blockStorage != null) {
            return space.blockStorage.available;
        }
        return null;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Aggregate diskAggregates = (Aggregate) o;
        return Objects.equals(this.name, diskAggregates.name) &&
                Objects.equals(this.uuid, diskAggregates.uuid);
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    @Override
    public String toString() {
        return "DiskAggregates [name=" + name + ", uuid=" + uuid + "]";
    }

    public static class AggregateSpace {
        @JsonProperty("block_storage")
        private AggregateSpaceBlockStorage blockStorage = null;
    }

    public static class AggregateSpaceBlockStorage {
        @JsonProperty("available")
        private Double available = null;
        @JsonProperty("size")
        private Double size = null;
        @JsonProperty("used")
        private Double used = null;
    }

}
