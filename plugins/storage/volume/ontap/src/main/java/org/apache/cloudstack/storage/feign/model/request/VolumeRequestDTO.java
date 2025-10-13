package org.apache.cloudstack.storage.feign.model.request;

import java.util.List;

public class VolumeRequestDTO {
    private String name;
    private List<AggregateDTO> aggregates;
    private SvmDTO svm;

    // getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<AggregateDTO> getAggregates() { return aggregates; }
    public void setAggregates(List<AggregateDTO> aggregates) { this.aggregates = aggregates; }
    public SvmDTO getSvm() { return svm; }
    public void setSvm(SvmDTO svm) { this.svm = svm; }

    public static class AggregateDTO {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class SvmDTO {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}