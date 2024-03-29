package org.apache.cloudstack.storage.datastore.adapter.primera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraPort {
    private String wwn;
    private PrimeraPortPos portPos;
    public String getWwn() {
        return wwn;
    }
    public void setWwn(String wwn) {
        this.wwn = wwn;
    }
    public PrimeraPortPos getPortPos() {
        return portPos;
    }
    public void setPortPos(PrimeraPortPos portPos) {
        this.portPos = portPos;
    }
}
