// ExportPolicy.java
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportPolicy {
    private String name;
    private long id;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
}

