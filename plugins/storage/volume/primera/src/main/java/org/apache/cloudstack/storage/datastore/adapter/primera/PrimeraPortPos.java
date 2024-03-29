package org.apache.cloudstack.storage.datastore.adapter.primera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraPortPos {
    private Integer cardPort;
    private Integer node;
    private Integer slot;
    public Integer getCardPort() {
        return cardPort;
    }
    public void setCardPort(Integer cardPort) {
        this.cardPort = cardPort;
    }
    public Integer getNode() {
        return node;
    }
    public void setNode(Integer node) {
        this.node = node;
    }
    public Integer getSlot() {
        return slot;
    }
    public void setSlot(Integer slot) {
        this.slot = slot;
    }
}
