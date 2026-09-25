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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Volume {
    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("state")
    private String state;

    @JsonProperty("nas")
    private Nas nas;

    @JsonProperty("svm")
    private Svm svm;

    @JsonProperty("qos")
    private Qos qos;

    @JsonProperty("space")
    private VolumeSpace space;

    @JsonProperty("guarantee")
    private Guarantee guarantee;

    @JsonProperty("anti_ransomware")
    private AntiRansomware antiRansomware;

    @JsonProperty("aggregates")
    private List<Aggregate> aggregates = null;

    @JsonProperty("size")
    private Long size = null;

    // Getters and setters
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Nas getNas() {
        return nas;
    }

    public void setNas(Nas nas) {
        this.nas = nas;
    }

    public Svm getSvm() {
        return svm;
    }

    public void setSvm(Svm svm) {
        this.svm = svm;
    }

    public Qos getQos() {
        return qos;
    }

    public void setQos(Qos qos) {
        this.qos = qos;
    }

    public VolumeSpace getSpace() {
        return space;
    }

    public void setSpace(VolumeSpace space) {
        this.space = space;
    }

    public Guarantee getGuarantee() {
        return guarantee;
    }

    public void setGuarantee(Guarantee guarantee) {
        this.guarantee = guarantee;
    }

    public AntiRansomware getAntiRansomware() {
        return antiRansomware;
    }

    public void setAntiRansomware(AntiRansomware antiRansomware) {
        this.antiRansomware = antiRansomware;
    }

    public List<Aggregate> getAggregates () { return aggregates; }

    public void setAggregates (List<Aggregate> aggregates) { this.aggregates = aggregates; }

    public Long getSize () { return size; }

    public void setSize (Long size) { this.size = size; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Volume volume = (Volume) o;
        return Objects.equals(uuid, volume.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    public static class Guarantee {

        /**
         * ONTAP FlexVolume space guarantee (provisioning) type.
         * <ul>
         *   <li>{@link #NONE}   - thin provisioning (space is not reserved up front)</li>
         *   <li>{@link #VOLUME} - thick provisioning (full volume size is reserved on the aggregate)</li>
         * </ul>
         */
        public enum TypeEnum {
            NONE("none"),

            VOLUME("volume");

            private String value;

            TypeEnum(String value) {
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
            public static TypeEnum fromValue(String text) {
                if (text == null) return null;
                for (TypeEnum b : TypeEnum.values()) {
                    if (text.equalsIgnoreCase(b.value)) {
                        return b;
                    }
                }
                return null;
            }
        }

        @JsonProperty("type")
        private TypeEnum type;

        public Guarantee() {
        }

        public Guarantee(TypeEnum type) {
            this.type = type;
        }

        public TypeEnum getType() {
            return type;
        }

        public void setType(TypeEnum type) {
            this.type = type;
        }
    }

}
