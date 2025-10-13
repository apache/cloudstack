package org.apache.cloudstack.storage.feign.model.response;


import org.apache.cloudstack.storage.feign.model.Aggregate;
import org.apache.cloudstack.storage.feign.model.Nas;
import org.apache.cloudstack.storage.feign.model.Qos;

import java.util.List;

public class VolumeDetailsResponseDTO {
    private String uuid;
    private String createTime;
    private String name;
    private long size;
    private String state;
    private String style;
    private List<Aggregate> aggregates;
    private Nas nas;
    private Qos qos;
    private Svm svm;
    private String antiRansomwareState;

    // getters and setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public List<Aggregate> getAggregates() { return aggregates; }
    public void setAggregates(List<Aggregate> aggregates) { this.aggregates = aggregates; }
    public Nas getNas() { return nas; }
    public void setNas(Nas nas) { this.nas = nas; }
    public Qos getQos() { return qos; }
    public void setQos(Qos qos) { this.qos = qos; }
    public Svm getSvm() { return svm; }
    public void setSvm(Svm svm) { this.svm = svm; }
    public String getAntiRansomwareState() { return antiRansomwareState; }
    public void setAntiRansomwareState(String antiRansomwareState) { this.antiRansomwareState = antiRansomwareState; }

    public static class Svm {
        private String name;
        private String uuid;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
    }
}

