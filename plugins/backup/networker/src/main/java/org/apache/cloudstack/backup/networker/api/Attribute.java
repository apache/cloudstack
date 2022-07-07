package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "key",
        "values"
})
@Generated("jsonschema2pojo")
public class Attribute implements Serializable {

    private final static long serialVersionUID = -8899380112144428567L;
    @JsonProperty("key")
    private String key;
    @JsonProperty("values")
    private List<String> values = null;

    /**
     * No args constructor for use in serialization
     */
    public Attribute() {
    }

    /**
     * @param values
     * @param key
     */
    public Attribute(String key, List<String> values) {
        super();
        this.key = key;
        this.values = values;
    }

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    @JsonProperty("key")
    public void setKey(String key) {
        this.key = key;
    }

    @JsonProperty("values")
    public List<String> getValues() {
        return values;
    }

    @JsonProperty("values")
    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Attribute.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("key");
        sb.append('=');
        sb.append(((this.key == null) ? "<null>" : this.key));
        sb.append(',');
        sb.append("values");
        sb.append('=');
        sb.append(((this.values == null) ? "<null>" : this.values));
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
        result = ((result * 31) + ((this.key == null) ? 0 : this.key.hashCode()));
        result = ((result * 31) + ((this.values == null) ? 0 : this.values.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Attribute) == false) {
            return false;
        }
        Attribute rhs = ((Attribute) other);
        return (((this.key == rhs.key) || ((this.key != null) && this.key.equals(rhs.key))) && ((this.values == rhs.values) || ((this.values != null) && this.values.equals(rhs.values))));
    }

}
