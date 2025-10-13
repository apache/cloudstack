package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Aggregate {

    @SerializedName("name")
    private String name = null;

    @SerializedName("uuid")
    private String uuid = null;

    public Aggregate name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get name
     *
     * @return name
     **/
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

    /**
     * Get uuid
     *
     * @return uuid
     **/
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

}
