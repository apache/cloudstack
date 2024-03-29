package org.apache.cloudstack.storage.datastore.adapter.flasharray;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlashArrayHost {
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<String> getWwns() {
        return wwns;
    }
    public void setWwns(List<String> wwns) {
        this.wwns = wwns;
    }
    @JsonProperty("name")
    private String name;
    @JsonProperty("wwns")
    private List<String> wwns;

}
