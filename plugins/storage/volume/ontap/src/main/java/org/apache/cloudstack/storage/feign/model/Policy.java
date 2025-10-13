
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Policy {
    private int minThroughputIops;
    private int minThroughputMbps;
    private int maxThroughputIops;
    private int maxThroughputMbps;
    private String uuid;
    private String name;
    public int getMinThroughputIops() { return minThroughputIops; }
    public void setMinThroughputIops(int minThroughputIops) { this.minThroughputIops = minThroughputIops; }
    public int getMinThroughputMbps() { return minThroughputMbps; }
    public void setMinThroughputMbps(int minThroughputMbps) { this.minThroughputMbps = minThroughputMbps; }
    public int getMaxThroughputIops() { return maxThroughputIops; }
    public void setMaxThroughputIops(int maxThroughputIops) { this.maxThroughputIops = maxThroughputIops; }
    public int getMaxThroughputMbps() { return maxThroughputMbps; }
    public void setMaxThroughputMbps(int maxThroughputMbps) { this.maxThroughputMbps = maxThroughputMbps; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
