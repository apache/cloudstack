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

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Igroup {
    @JsonProperty("delete_on_unmap")
    private Boolean deleteOnUnmap = null;
    @JsonProperty("initiators")
    private List<Initiator> initiators = null;
    @JsonProperty("lun_maps")
    private List<LunMap> lunMaps = null;
    @JsonProperty("os_type")
    private OsTypeEnum osType = null;

    @JsonProperty("parent_igroups")
    private List<Igroup> parentIgroups = null;

    @JsonProperty("igroups")
    private List<Igroup> igroups = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("protocol")
    private ProtocolEnum protocol = null;
    @JsonProperty("svm")
    private Svm svm = null;
    @JsonProperty("uuid")
    private String uuid = null;

    public enum OsTypeEnum {
        hyper_v("hyper_v"),

        linux("linux"),

        vmware("vmware"),

        windows("windows"),

        xen("xen");

        private String value;

        OsTypeEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static OsTypeEnum fromValue(String text) {
            for (OsTypeEnum b : OsTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public List<Igroup> getParentIgroups() {
        return parentIgroups;
    }

    public void setParentIgroups(List<Igroup> parentIgroups) {
        this.parentIgroups = parentIgroups;
    }

    public Igroup igroups(List<Igroup> igroups) {
        this.igroups = igroups;
        return this;
    }

   public List<Igroup> getIgroups() {
        return igroups;
    }

    public void setIgroups(List<Igroup> igroups) {
        this.igroups = igroups;
    }

    public enum ProtocolEnum {
        iscsi("iscsi"),

        mixed("mixed");

        private String value;

        ProtocolEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static ProtocolEnum fromValue(String text) {
            for (ProtocolEnum b : ProtocolEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }
    public Igroup deleteOnUnmap(Boolean deleteOnUnmap) {
        this.deleteOnUnmap = deleteOnUnmap;
        return this;
    }

   public Boolean isDeleteOnUnmap() {
        return deleteOnUnmap;
    }

    public void setDeleteOnUnmap(Boolean deleteOnUnmap) {
        this.deleteOnUnmap = deleteOnUnmap;
    }

    public Igroup initiators(List<Initiator> initiators) {
        this.initiators = initiators;
        return this;
    }
    public List<Initiator> getInitiators() {
        return initiators;
    }

    public void setInitiators(List<Initiator> initiators) {
        this.initiators = initiators;
    }

    public Igroup lunMaps(List<LunMap> lunMaps) {
        this.lunMaps = lunMaps;
        return this;
    }
    public List<LunMap> getLunMaps() {
        return lunMaps;
    }

    public void setLunMaps(List<LunMap> lunMaps) {
        this.lunMaps = lunMaps;
    }

    public Igroup name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Igroup osType(OsTypeEnum osType) {
        this.osType = osType;
        return this;
    }
    public OsTypeEnum getOsType() {
        return osType;
    }

    public void setOsType(OsTypeEnum osType) {
        this.osType = osType;
    }

    public Igroup protocol(ProtocolEnum protocol) {
        this.protocol = protocol;
        return this;
    }

    public ProtocolEnum getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolEnum protocol) {
        this.protocol = protocol;
    }

    public Igroup svm(Svm svm) {
        this.svm = svm;
        return this;
    }
    public Svm getSvm() {
        return svm;
    }

    public void setSvm(Svm svm) {
        this.svm = svm;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Igroup other = (Igroup) obj;
        return Objects.equals(name, other.name) && Objects.equals(uuid, other.uuid);
    }

    @Override
    public String toString() {
        return "Igroup [deleteOnUnmap=" + deleteOnUnmap + ", initiators=" + initiators + ", lunMaps=" + lunMaps
                + ", name=" + name + ", replication="  + ", osType=" + osType + ", parentIgroups="
                + parentIgroups + ", igroups=" + igroups + ", protocol=" + protocol + ", svm=" + svm + ", uuid=" + uuid
                + ", portset=" + "]";
    }
}
