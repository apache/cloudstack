package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "unit",
        "value"
})
@Generated("jsonschema2pojo")
public class Size implements Serializable {

    private final static long serialVersionUID = -8270448162523429622L;
    @JsonProperty("unit")
    private String unit;
    @JsonProperty("value")
    private Integer value;

    /**
     * No args constructor for use in serialization
     */
    public Size() {
    }

    /**
     * @param unit
     * @param value
     */
    public Size(String unit, Integer value) {
        super();
        this.unit = unit;
        this.value = value;
    }

    @JsonProperty("unit")
    public String getUnit() {
        return unit;
    }

    @JsonProperty("unit")
    public void setUnit(String unit) {
        this.unit = unit;
    }

    @JsonProperty("value")
    public Integer getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Size.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("unit");
        sb.append('=');
        sb.append(((this.unit == null) ? "<null>" : this.unit));
        sb.append(',');
        sb.append("value");
        sb.append('=');
        sb.append(((this.value == null) ? "<null>" : this.value));
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
        result = ((result * 31) + ((this.value == null) ? 0 : this.value.hashCode()));
        result = ((result * 31) + ((this.unit == null) ? 0 : this.unit.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Size) == false) {
            return false;
        }
        Size rhs = ((Size) other);
        return (((this.value == rhs.value) || ((this.value != null) && this.value.equals(rhs.value))) && ((this.unit == rhs.unit) || ((this.unit != null) && this.unit.equals(rhs.unit))));
    }

}
