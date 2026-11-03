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

/**
 * A LUN is the logical representation of storage in a storage area network (SAN).&lt;br/&gt; In ONTAP, a LUN is located within a volume. Optionally, it can be located within a qtree in a volume.&lt;br/&gt; A LUN can be created to a specified size using thin or thick provisioning. A LUN can then be renamed, resized, cloned, and moved to a different volume. LUNs support the assignment of a quality of service (QoS) policy for performance management or a QoS policy can be assigned to the volume containing the LUN. See the LUN object model to learn more about each of the properties supported by the LUN REST API.&lt;br/&gt; A LUN must be mapped to an initiator group to grant access to the initiator group&#39;s initiators (client hosts). Initiators can then access the LUN and perform I/O over a Fibre Channel (FC) fabric using the Fibre Channel Protocol or a TCP/IP network using iSCSI.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Lun {

    @JsonProperty("auto_delete")
    private Boolean autoDelete = null;

    /**
     * The class of LUN.&lt;br/&gt; Optional in POST.
     */
    public enum PropertyClassEnum {
        REGULAR("regular");

        private String value;

        PropertyClassEnum(String value) {
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
        public static PropertyClassEnum fromValue(String value) {
            for (PropertyClassEnum b : PropertyClassEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("class")
    private PropertyClassEnum propertyClass = null;

    @JsonProperty("enabled")
    private Boolean enabled = null;

    @JsonProperty("lun_maps")
    private List<LunMap> lunMaps = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("clone")
    private Clone clone = null;

    /**
     * The operating system type of the LUN.&lt;br/&gt; Required in POST when creating a LUN that is not a clone of another. Disallowed in POST when creating a LUN clone.
     */
    public enum OsTypeEnum {
        HYPER_V("hyper_v"),

        LINUX("linux"),

        VMWARE("vmware"),

        WINDOWS("windows"),

        XEN("xen");

        private String value;

        OsTypeEnum(String value) {
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
        public static OsTypeEnum fromValue(String value) {
            for (OsTypeEnum b : OsTypeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("os_type")
    private OsTypeEnum osType = null;

    @JsonProperty("serial_number")
    private String serialNumber = null;

    @JsonProperty("space")
    private LunSpace space = null;

    @JsonProperty("svm")
    private Svm svm = null;

    @JsonProperty("uuid")
    private String uuid = null;

    public Lun autoDelete(Boolean autoDelete) {
        this.autoDelete = autoDelete;
        return this;
    }

    public Boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(Boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public Lun propertyClass(PropertyClassEnum propertyClass) {
        this.propertyClass = propertyClass;
        return this;
    }

    public PropertyClassEnum getPropertyClass() {
        return propertyClass;
    }

    public void setPropertyClass(PropertyClassEnum propertyClass) {
        this.propertyClass = propertyClass;
    }

    public Lun enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<LunMap> getLunMaps() {
        return lunMaps;
    }

    public void setLunMaps(List<LunMap> lunMaps) {
        this.lunMaps = lunMaps;
    }

    public Lun name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Lun osType(OsTypeEnum osType) {
        this.osType = osType;
        return this;
    }

    public OsTypeEnum getOsType() {
        return osType;
    }

    public void setOsType(OsTypeEnum osType) {
        this.osType = osType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public Lun space(LunSpace space) {
        this.space = space;
        return this;
    }

    public LunSpace getSpace() {
        return space;
    }

    public void setSpace(LunSpace space) {
        this.space = space;
    }

    public Lun svm(Svm svm) {
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
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Clone getClone() {
        return clone;
    }

    public void setClone(Clone clone) {
        this.clone = clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Lun lun = (Lun) o;
        return Objects.equals(this.name, lun.name) && Objects.equals(this.uuid, lun.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uuid);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Lun {\n");
        sb.append("    autoDelete: ").append(toIndentedString(autoDelete)).append("\n");
        sb.append("    propertyClass: ").append(toIndentedString(propertyClass)).append("\n");
        sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
        sb.append("    lunMaps: ").append(toIndentedString(lunMaps)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    osType: ").append(toIndentedString(osType)).append("\n");
        sb.append("    serialNumber: ").append(toIndentedString(serialNumber)).append("\n");
        sb.append("    space: ").append(toIndentedString(space)).append("\n");
        sb.append("    svm: ").append(toIndentedString(svm)).append("\n");
        sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }


    public static class Clone {
        @JsonProperty("source")
        private Source source = null;
        public Source getSource() {
            return source;
        }
        public void setSource(Source source) {
            this.source = source;
        }
    }

    public static class Source {
        @JsonProperty("name")
        private String name = null;
        @JsonProperty("uuid")
        private String uuid = null;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getUuid() {
            return uuid;
        }
        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
}
